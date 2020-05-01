package me.ddevil.skedule

import com.okkero.skedule.BukkitSchedulerController
import com.okkero.skedule.SynchronizationContext
import com.okkero.skedule.currentContext

suspend inline fun BukkitSchedulerController.runWithContext(context: SynchronizationContext, block: BukkitSchedulerController.() -> Unit) {
    val before = currentContext()
    switchContext(context)
    block()
    switchContext(before)
}