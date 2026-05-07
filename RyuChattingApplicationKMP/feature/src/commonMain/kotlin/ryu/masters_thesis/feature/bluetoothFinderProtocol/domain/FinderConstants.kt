package ryu.masters_thesis.feature.bluetoothFinderProtocol.domain

// Dedikovaný UUID pro BFP — odlišný od chat UUID
// perhaps migrate do BFP/data?
const val BFP_UUID            = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
const val BFP_MSG_QUERY       = "BFP_QUERY"
const val BFP_MSG_HOSTING     = "BFP_HOSTING"
const val BFP_MSG_NOT_HOSTING = "BFP_NOT_HOSTING"
const val BFP_CONNECT_TIMEOUT = 4000L   // ms per peer connect attempt
const val BFP_READ_TIMEOUT    = 2000L   // ms čekání na odpověď