package ryu.masters_thesis.core.performance_tests

fun measureMs(label: String, warmups: Int = 3, runs: Int = 10, block: () -> Unit): Long {
    repeat(warmups) { block() }
    val times = LongArray(runs) { kotlin.system.measureTimeMillis { block() } }
    val avg = times.average().toLong()
    println("[PERF] [$label] avg=${avg}ms  min=${times.min()}ms  max=${times.max()}ms")
    return avg
}