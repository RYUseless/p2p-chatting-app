package ryu.masters_thesis.feature.bluetoothNeighbourProtokol.domain

const val MSG_NBR_HELLO       = "NBR_YELLO"
const val MSG_NBR_LSA         = "NBR_LSA"
const val MSG_NBR_LSA_REQUEST = "NBR_LSA_REQUEST"
const val MSG_NBR_ROOM_ADV    = "NBR_ROOM_ADV"
const val MSG_NBR_TUNNEL      = "NBR_TUNNEL"

const val HELLO_INTERVAL_MS   = 15_000L
const val LSA_INTERVAL_MS     = 60_000L
const val NEIGHBOUR_DEAD_MS   = 45_000L

const val MAX_LSA_TTL         = 5
const val MAX_ROOM_ADV_HOPS   = 3
const val MAX_TUNNEL_HOPS     = 6