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

/**
 * Starts a new coroutine.
 *
 * The coroutine will be executed on [Schedulers.global] and
 * [kotlinx.coroutines.Dispatchers.Default].
 *
 * @param sync The synchronization context to start the coroutine with
 * @param block The coroutine to execute
 * @return The Job linked to the coroutine
 * @receiver The plugin to register the tasks with
 */
fun Plugin.skedule(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> Unit,
): Job = Schedulers.global(this).skedule(sync, block)

/**
 * Starts a new coroutine.
 *
 * The coroutine will be executed on [Schedulers.entity] and
 * [kotlinx.coroutines.Dispatchers.Default]
 *
 * @param entity The entity to execute the tasks with
 * @param sync The synchronization context to start the coroutine with
 * @param block The coroutine to execute
 * @return The Job linked to the coroutine
 * @receiver The plugin to register the tasks with
 */
fun Plugin.skedule(
    entity: Entity,
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> Unit,
): Job = Schedulers.entity(this, entity).skedule(sync, block)

/**
 * Starts a new coroutine.
 *
 * The coroutine will be executed on the scheduler and [kotlinx.coroutines.Dispatchers.Default]
 *
 * @param sync The synchronization context to start the coroutine with
 * @param block The coroutine to execute
 * @return The Job linked to the coroutine
 * @receiver The scheduler to execute the tasks with
 */
fun AbstractScheduler.skedule(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> Unit,
): Job =
    CoroutineScope(BukkitDispatcher(this))
        .launch(context = BukkitSchedulerSynchronizationContext(sync), block = block)

/**
 * Starts a new coroutine and returns its result as an implementation of [Deferred].
 *
 * The coroutine will be executed on [Schedulers.global] and
 * [kotlinx.coroutines.Dispatchers.Default].
 *
 * @param sync The synchronization context to start the coroutine with
 * @param block The coroutine to execute
 * @return The result of the coroutine
 * @receiver The plugin to register the tasks with
 */
fun <T> Plugin.async(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> = Schedulers.global(this).async(sync, block)

/**
 * Starts a new coroutine and returns its result as an implementation of [Deferred].
 *
 * The coroutine will be executed on [Schedulers.entity] and
 * [kotlinx.coroutines.Dispatchers.Default]
 *
 * @param entity The entity to execute the tasks with
 * @param sync The synchronization context to start the coroutine with
 * @param block The coroutine to execute
 * @return The result of the coroutine
 * @receiver The plugin to register the tasks with
 */
fun <T> Plugin.async(
    entity: Entity,
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> = Schedulers.entity(this, entity).async(sync, block)

/**
 * Starts a new coroutine and returns its result as an implementation of [Deferred].
 *
 * The coroutine will be executed on the scheduler and [kotlinx.coroutines.Dispatchers.Default]
 *
 * @param sync The synchronization context to start the coroutine with
 * @param block The coroutine to execute
 * @return The result of the coroutine
 * @receiver The scheduler to execute the tasks with
 */
fun <T> AbstractScheduler.async(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): Deferred<T> =
    CoroutineScope(BukkitDispatcher(this))
        .async(context = BukkitSchedulerSynchronizationContext(sync), block = block)

/**
 * Starts a new coroutine and returns its result as an implementation of [CompletableFuture].
 *
 * The coroutine will be executed on [Schedulers.global] and
 * [kotlinx.coroutines.Dispatchers.Default].
 *
 * @param sync The synchronization context to start the coroutine with
 * @param block The coroutine to execute
 * @return The result of the coroutine
 * @receiver The plugin to register the tasks with
 */
fun <T> Plugin.future(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): CompletableFuture<T> = Schedulers.global(this).future(sync, block)

/**
 * Starts a new coroutine and returns its result as an implementation of [CompletableFuture].
 *
 * The coroutine will be executed on [Schedulers.entity] and
 * [kotlinx.coroutines.Dispatchers.Default]
 *
 * @param entity The entity to execute the tasks with
 * @param sync The synchronization context to start the coroutine with
 * @param block The coroutine to execute
 * @return The result of the coroutine
 * @receiver The plugin to register the tasks with
 */
fun <T> Plugin.future(
    entity: Entity,
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): CompletableFuture<T> = Schedulers.entity(this, entity).future(sync, block)

/**
 * Starts a new coroutine and returns its result as an implementation of [CompletableFuture].
 *
 * The coroutine will be executed on the scheduler and [kotlinx.coroutines.Dispatchers.Default]
 *
 * @param sync The synchronization context to start the coroutine with
 * @param block The coroutine to execute
 * @return The result of the coroutine
 * @receiver The scheduler to execute the tasks with
 */
fun <T> AbstractScheduler.future(
    sync: SynchronizationContext = SynchronizationContext.ASYNC,
    block: suspend CoroutineScope.() -> T,
): CompletableFuture<T> =
    CoroutineScope(BukkitDispatcher(this))
        .future(context = BukkitSchedulerSynchronizationContext(sync), block = block)
