package com.okkero.skedule

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.yield

class BukkitSchedulerSynchronizationContext(
    var sync: SynchronizationContext,
) : CoroutineContext.Element {
  object Key : CoroutineContext.Key<BukkitSchedulerSynchronizationContext>

  override val key: CoroutineContext.Key<BukkitSchedulerSynchronizationContext>
    get() = Key
}

/**
 * Runs the given block with the provided synchronization context and reverts to the previous
 * context after execution
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

val CoroutineContext.synchronizationContext
  get() = get(BukkitSchedulerSynchronizationContext.Key)!!.sync

suspend fun switchContext(newContext: SynchronizationContext) {
  val context = coroutineContext[BukkitSchedulerSynchronizationContext.Key]!!

  if (context.sync !== newContext) {
    context.sync = newContext
    yield()
  }
}
