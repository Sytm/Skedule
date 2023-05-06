package com.okkero.skedule

import com.okkero.skedule.schedulers.AbstractScheduledTask
import com.okkero.skedule.schedulers.AbstractScheduler
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive

@OptIn(InternalCoroutinesApi::class)
class BukkitDispatcher(private val scheduler: AbstractScheduler) : CoroutineDispatcher(), Delay {

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun scheduleResumeAfterDelay(
      timeMillis: Long,
      continuation: CancellableContinuation<Unit>
  ) {
    val task =
        runTaskLater(
            continuation.context.synchronizationContext,
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

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    if (!context.isActive) {
      return
    }

    runTask(
        context.synchronizationContext,
        block,
    ) {
      context.cancel(RetiredEntityException())
    }
        ?: context.cancel(RemovedEntityException())
  }

  private fun runTaskLater(
      sync: SynchronizationContext,
      block: Runnable,
      delay: Long,
      retired: Runnable
  ): AbstractScheduledTask? =
      when (sync) {
        SynchronizationContext.SYNC -> scheduler.scheduleLater(block, delay, retired)
        SynchronizationContext.ASYNC -> scheduler.scheduleLaterAsync(block, delay, retired)
      }

  private fun runTask(
      sync: SynchronizationContext,
      block: Runnable,
      retired: Runnable
  ): AbstractScheduledTask? =
      when (sync) {
        SynchronizationContext.SYNC -> scheduler.schedule(block, retired)
        SynchronizationContext.ASYNC -> scheduler.scheduleAsync(block, retired)
      }

  class RetiredEntityException : CancellationException()
  class RemovedEntityException : CancellationException()
}
