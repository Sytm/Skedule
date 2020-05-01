package me.ddevil.skedule

import com.okkero.skedule.BukkitSchedulerController
import com.okkero.skedule.SynchronizationContext
import com.okkero.skedule.currentContext

suspend inline fun <T> BukkitSchedulerController.runWithContext(context: SynchronizationContext, block: BukkitSchedulerController.() -> T): T {
    val before = currentContext()
    if (before != context) {
        switchContext(context)
    }
    val r = block()
    if (before != context) {
        switchContext(before)
    }
    return r
}