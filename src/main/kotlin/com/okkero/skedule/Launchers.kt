package com.okkero.skedule

import com.okkero.skedule.schedulers.AbstractScheduler
import com.okkero.skedule.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin

fun Plugin.schedule(
    sync: SynchronizationContext = SynchronizationContext.SYNC,
    block: suspend CoroutineScope.() -> Unit,
) {
    Schedulers.global(this).schedule(sync, block)
}

fun Plugin.schedule(
    entity: Entity,
    sync: SynchronizationContext = SynchronizationContext.SYNC,
    block: suspend CoroutineScope.() -> Unit,
) {
    Schedulers.entity(this, entity).schedule(sync, block)
}

fun AbstractScheduler.schedule(
    sync: SynchronizationContext = SynchronizationContext.SYNC,
    block: suspend CoroutineScope.() -> Unit,
) {
    CoroutineScope(BukkitDispatcher(this)).launch(context = BukkitSchedulerSynchronizationContext(sync), block = block)
}