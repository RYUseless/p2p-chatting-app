package ryu.masters_thesis.feature.lifecycle.implementation

import android.util.Log
import ryu.masters_thesis.feature.lifecycle.domain.Terminable

object AppTerminationRegistry {

    private const val TAG = "AppTermination"

    private val registered = mutableListOf<Terminable>()

    fun register(component: Terminable) {
        synchronized(registered) {
            registered.add(component)
            Log.d(TAG, "registered: ${component::class.simpleName} total=${registered.size}")
        }
    }

    fun unregister(component: Terminable) {
        synchronized(registered) {
            registered.remove(component)
            Log.d(TAG, "unregistered: ${component::class.simpleName} total=${registered.size}")
        }
    }

    fun terminateAll() {
        synchronized(registered) {
            Log.i(TAG, "terminateAll: ${registered.size} components")
            registered.forEach { component ->
                try {
                    component.onTerminate()
                    Log.d(TAG, "terminated: ${component::class.simpleName}")
                } catch (e: Exception) {
                    Log.e(TAG, "terminate failed for ${component::class.simpleName}: ${e.message}")
                }
            }
            registered.clear()
        }
    }
}