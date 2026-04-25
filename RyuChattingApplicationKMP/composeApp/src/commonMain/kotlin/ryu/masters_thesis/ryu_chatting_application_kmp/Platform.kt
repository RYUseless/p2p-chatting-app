package ryu.masters_thesis.ryu_chatting_application_kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform