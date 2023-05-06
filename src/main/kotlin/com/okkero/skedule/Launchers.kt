package com.okkero.skedule

import de.md5lukas.schedulers.AbstractScheduler
import de.md5lukas.schedulers.Schedulers
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin

fun Plugin.skedule(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> Unit,
): Job = Schedulers.global(this).skedule(sync, block)

fun Plugin.skedule(
    entity: Entity,
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> Unit,
): Job = Schedulers.entity(this, entity).skedule(sync, block)

fun AbstractScheduler.skedule(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> Unit,
): Job =
    CoroutineScope(BukkitDispatcher(this))
        .launch(context = BukkitSchedulerSynchronizationContext(sync), block = block)

fun <T> Plugin.async(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> = Schedulers.global(this).async(sync, block)

fun <T> Plugin.async(
    entity: Entity,
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> = Schedulers.entity(this, entity).async(sync, block)

fun <T> AbstractScheduler.async(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> =
    CoroutineScope(BukkitDispatcher(this))
        .async(context = BukkitSchedulerSynchronizationContext(sync), block = block)

fun <T> Plugin.future(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): CompletableFuture<T> = Schedulers.global(this).future(sync, block)

fun <T> Plugin.future(
    entity: Entity,
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): CompletableFuture<T> = Schedulers.entity(this, entity).future(sync, block)

fun <T> AbstractScheduler.future(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): CompletableFuture<T> =
    CoroutineScope(BukkitDispatcher(this))
        .future(context = BukkitSchedulerSynchronizationContext(sync), block = block)
