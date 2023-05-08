package com.okkero.skedule

import de.md5lukas.schedulers.AbstractScheduledTask
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

@OptIn(InternalCoroutinesApi::class)
internal object BukkitDispatcher : CoroutineDispatcher(), Delay {

  private val asyncDelegate
    get() = Dispatchers.Default

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun scheduleResumeAfterDelay(
      timeMillis: Long,
      continuation: CancellableContinuation<Unit>
  ) {
    val bukkitContext = continuation.context.bukkitContextNullable

    if (bukkitContext === null) {
      continuation.context.cancel(MissingBukkitContextException())
      return
    }

    val task =
        bukkitContext.runTaskLater(
            { continuation.apply { resumeUndispatched(Unit) } },
            timeMillis / 50,
            { continuation.context.cancel(RetiredEntityException()) },
        )
    if (task === null) {
      continuation.context.cancel(RemovedEntityException())
      return
    }
    continuation.invokeOnCancellation { task.cancel() }
  }

  override fun dispatchYield(context: CoroutineContext, block: Runnable) {
    val bukkitContext = context.bukkitContextNullable

    if (bukkitContext === null) {
      context.cancel(MissingBukkitContextException())
      return
    }

    if (bukkitContext.sync === SynchronizationContext.ASYNC) {
      asyncDelegate.dispatchYield(context, block)
    } else {
      super.dispatchYield(context, block)
    }
  }

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    if (!context.isActive) {
      return
    }

    val bukkitContext = context.bukkitContextNullable

    if (bukkitContext === null) {
      context.cancel(MissingBukkitContextException())
      return
    }

    if (bukkitContext.sync === SynchronizationContext.ASYNC) {
      return asyncDelegate.dispatch(context, block)
    }

    bukkitContext.runTask(
        block,
    ) {
      context.cancel(RetiredEntityException())
    }
        ?: context.cancel(RemovedEntityException())
  }

  private fun BukkitContext.runTaskLater(
      block: Runnable,
      delay: Long,
      retired: Runnable
  ): AbstractScheduledTask? =
      when (sync) {
        SynchronizationContext.SYNC -> scheduler.scheduleDelayed(delay, retired, block)
        SynchronizationContext.ASYNC -> scheduler.scheduleDelayedAsync(delay, block)
      }

  private fun BukkitContext.runTask(block: Runnable, retired: Runnable): AbstractScheduledTask? =
      when (sync) {
        SynchronizationContext.SYNC -> scheduler.schedule(retired, block)
        SynchronizationContext.ASYNC -> scheduler.scheduleAsync(block)
      }

  class MissingBukkitContextException : CancellationException()
  class RetiredEntityException : CancellationException()
  class RemovedEntityException : CancellationException()
}
