package com.okkero.skedule

import de.md5lukas.schedulers.AbstractScheduler
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

/**
 * The BukkitContext holds information for the [BukkitDispatcher] to know what to do.
 *
 * Only useful inside coroutines that have been started inside another coroutine, for example when
 * using [launch]
 *
 * @property scheduler The scheduler the coroutine is dispatched to
 * @property sync The synchronization context used by the dispatcher
 */
class BukkitContext(
    var scheduler: AbstractScheduler,
    var sync: SynchronizationContext,
) : CoroutineContext.Element {

  companion object Key : CoroutineContext.Key<BukkitContext>

  override val key: CoroutineContext.Key<BukkitContext>
    get() = Key
}

internal val CoroutineContext.bukkitContextNullable
  get() = this[BukkitContext]

internal val CoroutineContext.bukkitContext
  get() =
      bukkitContextNullable
          ?: throw IllegalStateException(
              "Synchronization state access and switches can only be performed on Skedule-launched coroutines")

/**
 * Retrieves the current synchronization context of the provided coroutine context
 *
 * @throws IllegalStateException If coroutine is not a Skedule coroutine
 */
val CoroutineContext.synchronizationContext
  get() = bukkitContext.sync

/**
 * Runs the given block with the provided synchronization context and reverts to the previous
 * context after execution
 *
 * @param newContext The context to execute the block in
 * @param block The code to execute
 * @return The result of the code block
 * @throws IllegalStateException If coroutine is not a Skedule coroutine
 */
suspend inline fun <T> withSynchronizationContext(
    newContext: SynchronizationContext,
    block: () -> T
): T {
  val oldContext = coroutineContext.synchronizationContext

  if (oldContext !== newContext) {
    switchContext(newContext)
  }

  val result = block()

  if (coroutineContext.synchronizationContext !== oldContext) {
    switchContext(oldContext)
  }

  return result
}

/**
 * Switches the synchronization context of this coroutine to the new one
 *
 * @param newContext The context to switch to
 * @param immediate Perform the context switch immediately. Can be used before [delay] to avoid
 *   double dispatch.
 * @throws IllegalStateException If coroutine is not a Skedule coroutine
 */
suspend fun switchContext(newContext: SynchronizationContext, immediate: Boolean = true) {
  val context = coroutineContext.bukkitContext

  if (context.sync !== newContext) {
    context.sync = newContext
    if (immediate) {
      yield()
    }
  }
}

/**
 * Switches the scheduler executing the coroutine
 *
 * @param newScheduler The scheduler to switch to
 * @param immediate Perform the scheduler switch immediately. Can be used before [delay] to avoid
 *   double dispatch.
 * @throws IllegalStateException If coroutine is not a Skedule coroutine
 */
suspend fun switchScheduler(newScheduler: AbstractScheduler, immediate: Boolean = true) {
  coroutineContext.bukkitContext.scheduler = newScheduler

  if (immediate) {
    yield()
  }
}
