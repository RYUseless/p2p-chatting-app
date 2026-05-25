# Ryu's Peer-to-Peer Bluetooth Chatting Application
### Master's Thesis · Kotlin Multiplatform · Compose Multiplatform

![Platform](https://img.shields.io/badge/platform-Android%20%7C%20iOS%20(stub)-informational?style=flat-square)
![KMP](https://img.shields.io/badge/Kotlin%20Multiplatform-2.x-7F52FF?style=flat-square&logo=kotlin)
![Compose](https://img.shields.io/badge/Compose%20Multiplatform-UI-4285F4?style=flat-square)
![License](https://img.shields.io/badge/license-MIT-green?style=flat-square)
![Status](https://img.shields.io/badge/status-thesis%20freeze-orange?style=flat-square)

> A decentralised, end-to-end encrypted peer-to-peer chat application operating entirely over Bluetooth Classic (RFCOMM), built as a master's thesis project. No internet connection, no central server, no third-party infrastructure.

---

## Table of Contents

1. [Overview](#overview)
2. [How to Run](#how-to-run)
3. [Architecture](#architecture)
   - [Module Graph](#module-graph)
   - [Layer Breakdown](#layer-breakdown)
   - [Feature Modules](#feature-modules)
4. [Protocol Stack](#protocol-stack)
   - [Transport — BTP](#transport--btp-bluetooth-transport-protocol)
   - [Discovery — BNP](#discovery--bnp-bluetooth-neighbour-protocol)
   - [Application — BFP](#application--bfp-bluetooth-find-protocol)
   - [Authentication](#authentication--schnorr--fiat-shamir)
5. [Security](#security)
6. [Technology Stack](#technology-stack)
7. [Repository Structure](#repository-structure)

---

## Overview

Chatting App establishes direct Bluetooth RFCOMM connections between devices, enabling encrypted messaging without relying on any external network infrastructure. Rooms are created by one device acting as a server; other devices discover and join via a custom peer-discovery protocol.

The project explores the feasibility of a fully offline, cryptographically authenticated mesh-adjacent chat system on mobile hardware.

**Key properties:**
- Zero-server architecture — all communication is device-to-device
- AES-256-GCM encrypted message payloads
- Zero-knowledge authentication via Fiat-Shamir transformed Schnorr proofs on secp256k1 curve
- Multi-hop room advertisement propagation via a custom routing protocol
- Android-first implementation; iOS platform layer is stub-only

---

## How to Run

No release build is available — this project is a proof of concept developed as part of a master's thesis.

To run the app:

1. Clone the repository: `git clone https://github.com/RYUseless/p2p-chatting-app.git`
2. Open the project in [Android Studio](https://developer.android.com/studio) (Hedgehog or newer)
3. Connect an Android device with Bluetooth enabled — USB or Wi-Fi debugging must be enabled for the device to be recognised
4. Run the `app` configuration directly on the device — Android Studio will compile and deploy the app automatically

## Architecture

The project follows **Clean Architecture** with a strict unidirectional dependency rule and **MVI** (Model–View–Intent) at the presentation layer, implemented using **Voyager ScreenModel**.

### Module Graph

```
:app
 └── :feature
      ├── :feature:bluetooth      ← room lifecycle, BLE controller
      ├── :feature:bnp            ← neighbour & routing protocol
      ├── :feature:btp            ← transport session management
      └── :feature:bfp            ← device/room discovery (FinderResponder)
          └── :presentation       ← screens, ViewModels, navigation
              └── :data           ← repositories, local persistence
                  └── :core       ← domain models, interfaces, crypto utils
```

Dependencies flow **inward only** — `:core` has no dependency on any other module.

### Layer Breakdown

| Layer | Module | Responsibility |
|---|---|---|
| **Domain** | `:core` | Entities, use-case interfaces, `CryptoManager`, `KeyManager`, `RoutingTable` |
| **Data** | `:data` | Repository implementations, Room DB, `EncryptedSharedPreferences`, `DatabaseKeyProvider` |
| **Presentation** | `:presentation` | Compose screens, `ScreenModel` (MVI), `AppNavigation`, `AppThemeWrapper` |
| **Feature** | `:feature:*` | Protocol state machines, Bluetooth controllers, business orchestration |

### Feature Modules

#### `:feature:bluetooth`
Manages the full Bluetooth session lifecycle. Splits responsibility into:
- **`ControllerServer`** — accepts incoming RFCOMM connections, runs the verifier side of Schnorr authentication, manages room state
- **`ControllerClient`** — initiates RFCOMM connections, executes the prover side of Schnorr authentication
- **`ControllerBase`** — shared logic: AES-GCM framing, packet dispatch, keepalive

#### `:feature:bnp`
Implements the **Bluetooth Neighbour Protocol** (see [Protocol Stack](#discovery--bnp-bluetooth-neighbour-protocol)).

#### `:feature:btp`
Manages raw RFCOMM socket lifecycle, write queuing, and read loops as a thin transport abstraction.

#### `:feature:bfp`
`FinderResponder` runs persistently from `MainActivity` start to app termination (via `AppTerminationRegistry`), advertising local room presence and responding to discovery queries from remote peers.

---

## Protocol Stack

Four custom application-layer protocols operate over Bluetooth Classic RFCOMM:

```
┌─────────────────────────────────────────────┐
│              Application (Chat)             │  Message, Reaction, Presence
├─────────────────────────────────────────────┤
│        BFP — Find Protocol                  │  Room advertisement & discovery
├─────────────────────────────────────────────┤
│        BNP — Neighbour Protocol             │  Topology, routing, flooding
├─────────────────────────────────────────────┤
│        BTP — Transport Protocol             │  Framing, sequencing, sessions
├─────────────────────────────────────────────┤
│        Bluetooth Classic RFCOMM             │  L2CAP / RF layer
└─────────────────────────────────────────────┘
```

### Transport — BTP (Bluetooth Transport Protocol)

Wraps raw RFCOMM sockets with:
- Length-prefixed packet framing
- Sequential write queue
- Session state machine (`CONNECTING → HANDSHAKING → ESTABLISHED → CLOSED`)

### Discovery — BNP (Bluetooth Neighbour Protocol)

An OSPF-inspired link-state routing protocol operating over Bluetooth adjacencies.

| Parameter | Value |
|---|---|
| HELLO interval | 15 s |
| Dead threshold | 45 s |
| LSA flood interval | 60 s |
| Max TTL | 5 hops |
| Cost function | hop count + RSSI penalty |
| Routing algorithm | Dijkstra |

Packet types: `HELLO`, `NBR_LSA`, `NBR_LSA_REQUEST`, `NBR_ACK`

### Application — BFP (Bluetooth Find Protocol)

Provides room advertisement and peer discovery on top of BNP topology. `FinderResponder` answers incoming discovery queries with room metadata (name, capacity, presence). Clients use BFP to populate the room list on `HomeScreen` without needing a central registry.

### Authentication — Schnorr + Fiat-Shamir

Authentication replaces a naive key-exchange handshake with a **zero-knowledge proof of knowledge** of the room password:

```
Client (Prover)                         Server (Verifier)
───────────────                         ─────────────────
  k ← random scalar
  R = k·G                 ──── R ────►
                          ◄─── e ────  e ← H(R ‖ context)
  s = k + e·x             ──── s ────►
                                        verify: s·G == R + e·X
```

- Curve: **secp256k1**
- Hash: **SHA-256** (injectable `hashFn` for testability)
- Types: `SchnorrProof`, `SchnorrProtocol`, `SchnorrProtocolImpl`
- Dependency: `com.ionspin.kotlin:bignum:0.3.10`

No password material is ever transmitted; the server only learns that the client possesses the correct secret.

---

## Security

| Property | Mechanism |
|---|---|
| Authentication | Schnorr ZKP (Fiat–Shamir) on secp256k1 |
| Message confidentiality | AES-256-GCM per session |
| Key derivation | PBKDF2-HMAC-SHA256, **600 000 iterations** (OWASP 2023) |
| Key storage | Android Keystore + `EncryptedSharedPreferences` |
| Room identity | QR code (`roomId\|password`), scanned via CameraX + MLKit |

---

## Technology Stack

| Area | Library / Tool |
|---|---|
| Language | Kotlin 2.x (Multiplatform) |
| UI | Compose Multiplatform |
| Navigation | Voyager (`ScreenModel`, `Navigator`) |
| State management | MVI via `ScreenModel` + `StateFlow` |
| Dependency injection | Koin |
| Persistence | Room (KMP) |
| Cryptography | `javax.crypto`, Android Keystore, `bignum:0.3.10` |
| QR | ZXing + CameraX + MLKit (Android) |
| Bluetooth | Android `BluetoothAdapter`, RFCOMM `BluetoothSocket` |
| Build | Gradle 8.x, Convention plugins |

---

## Repository Structure

- Masters Thesis `src`: [folder link](RyuChattingApplicationKMP/)
- Semestral Thesis `src`: [folder link](RyuP2P/)
- Dead attempt `src`: [folder link](Ryus-Chatting-Application/)

- Masters thesis folder structure, only relevant parts:
```
.
├── app/                        # Application entry point, DI graph root
├── core/
│   ├── commonMain/             # Domain models, interfaces, constants
│   ├── androidMain/            # Android implementations (crypto, expect/actual)
│   └── iosMain/                # iOS stubs (expect/actual)
├── data/                       # Repository impls, DB schema, key provider
├── presentation/               # Compose screens, navigation, theme
│   ├── AppNavigation.kt
│   ├── AppThemeWrapper.kt
│   └── AppTranslations.kt      # i18n: CS / EN / DE / PL
└── feature/
    ├── bluetooth/              # Controller{Base,Server,Client}, packet types
    ├── bnp/                    # NeighbourProtocol, RoutingTable, LSA flooding
    ├── btp/                    # RFCOMM session, framing, write queue
    └── bfp/                    # FinderResponder, room advertisement
```

---

<p align="center">
  <sub>Master's Thesis 2025/2026</sub>
</p>
