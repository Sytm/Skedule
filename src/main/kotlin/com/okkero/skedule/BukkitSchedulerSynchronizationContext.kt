package com.okkero.skedule

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.yield

internal class BukkitSchedulerSynchronizationContext(
    var sync: SynchronizationContext,
) : CoroutineContext.Element {

  object Key : CoroutineContext.Key<BukkitSchedulerSynchronizationContext>

  override val key: CoroutineContext.Key<BukkitSchedulerSynchronizationContext>
    get() = Key
}

internal val CoroutineContext.bukkitSchedulerSynchronizationContext
  get() =
      this[BukkitSchedulerSynchronizationContext.Key]
          ?: throw IllegalStateException(
              "Synchronization state access and switches can only be performed on Skedule-launched coroutines")

/**
 * Retrieves the current synchronization context of the provided coroutine context
 *
 * @throws IllegalStateException If coroutine is not a Skedule coroutine
 */
val CoroutineContext.synchronizationContext
  get() = bukkitSchedulerSynchronizationContext.sync

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
 * @throws IllegalStateException If coroutine is not a Skedule coroutine
 */
suspend fun switchContext(newContext: SynchronizationContext) {
  val context = coroutineContext.bukkitSchedulerSynchronizationContext

  if (context.sync !== newContext) {
    context.sync = newContext
    yield()
  }
}
