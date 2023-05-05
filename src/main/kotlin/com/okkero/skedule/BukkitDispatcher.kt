package com.okkero.skedule

import com.okkero.skedule.schedulers.AbstractScheduledTask
import com.okkero.skedule.schedulers.AbstractScheduler
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@OptIn(InternalCoroutinesApi::class)
class BukkitDispatcher(private val scheduler: AbstractScheduler) :
    CoroutineDispatcher(), Delay {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
        val task = runTaskLater(
            continuation.context.synchronizationContext,
            Runnable {
                continuation.apply { resumeUndispatched(Unit) }
            },
            timeMillis / 50,
            Runnable {
                continuation.context.cancel(RetiredEntityException())
            },
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
            Runnable {
                context.cancel(RetiredEntityException())
            },
        ) ?: context.cancel(RemovedEntityException())
    }

    private fun runTaskLater(
        sync: SynchronizationContext,
        block: Runnable,
        delay: Long,
        retired: Runnable
    ): AbstractScheduledTask? = when (sync) {
        SynchronizationContext.SYNC -> scheduler.scheduleLater(block, delay, retired)
        SynchronizationContext.ASYNC -> scheduler.scheduleLaterAsync(block, delay, retired)
    }

    private fun runTask(sync: SynchronizationContext, block: Runnable, retired: Runnable): AbstractScheduledTask? =
        when (sync) {
            SynchronizationContext.SYNC -> scheduler.schedule(block, retired)
            SynchronizationContext.ASYNC -> scheduler.scheduleAsync(block, retired)
        }

    class RetiredEntityException : CancellationException()
    class RemovedEntityException : CancellationException()
}