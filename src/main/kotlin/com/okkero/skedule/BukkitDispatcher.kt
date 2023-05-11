package com.okkero.skedule

import de.md5lukas.schedulers.AbstractScheduledTask
import de.md5lukas.schedulers.AbstractScheduler
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import org.bukkit.plugin.Plugin

/**
 * Dispatcher that dispatches [SynchronizationContext.SYNC] coroutines on the
 * [BukkitContext.scheduler] and [SynchronizationContext.ASYNC] coroutines on [Dispatchers.Default].
 * Calling [kotlinx.coroutines.delay] in [SynchronizationContext.ASYNC] will also schedule on
 * [BukkitContext.scheduler].
 *
 * If the Dispatcher decides to cancel the coroutine, the coroutine will be dispatched to
 * [Dispatchers.IO] for cleanup. Reasons for cancellation:
 * - The [Plugin] is no longer enabled
 * - The [CoroutineContext] does not contain [BukkitContext]
 * - The Entity is removed before the dispatch could happen
 * - The Entity has been retired after the dispatch
 */
@OptIn(InternalCoroutinesApi::class)
object BukkitDispatcher : CoroutineDispatcher(), Delay {

  private val asyncDelegate
    get() = Dispatchers.Default

  private val cancelDelegate
    get() = Dispatchers.IO

  @OptIn(ExperimentalCoroutinesApi::class)
  override fun scheduleResumeAfterDelay(
      timeMillis: Long,
      continuation: CancellableContinuation<Unit>
  ) {
    val bukkitContext = continuation.context.bukkitContextNullable

    if (bukkitContext === null) {
      continuation.apply { resumeUndispatchedWithException(missingBukkitContextException()) }
      return
    } else if (!bukkitContext.scheduler.plugin.isEnabled) {
      cancelDelegate.dispatch(EmptyCoroutineContext) {
        continuation.apply {
          resumeUndispatchedWithException(disabledPluginException(bukkitContext.scheduler))
        }
      }
      return
    }

    val task =
        bukkitContext.runTaskLater(
            { continuation.apply { resumeUndispatched(Unit) } },
            timeMillis / 50,
        ) {
          cancelDelegate.dispatch(EmptyCoroutineContext) {
            continuation.apply {
              resumeUndispatchedWithException(retiredEntityException(bukkitContext.scheduler))
            }
          }
        }

    if (task === null) {
      cancelDelegate.dispatch(EmptyCoroutineContext) {
        continuation.apply {
          resumeUndispatchedWithException(removedEntityException(bukkitContext.scheduler))
        }
      }
      return
    }

    continuation.invokeOnCancellation { task.cancel() }
  }

  override fun dispatchYield(context: CoroutineContext, block: Runnable) {
    val bukkitContext = context.bukkitContextNullable

    if (bukkitContext === null) {
      context.cancel(missingBukkitContextException())
      cancelDelegate.dispatch(context, block)
      return
    }

    if (bukkitContext.sync === SynchronizationContext.ASYNC) {
      asyncDelegate.dispatchYield(context, block)
    } else {
      dispatch(context, block)
    }
  }

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    if (!context.isActive) {
      return
    }

    val bukkitContext = context.bukkitContextNullable

    if (bukkitContext === null) {
      context.cancel(missingBukkitContextException())
      cancelDelegate.dispatch(context, block)
      return
    } else if (!bukkitContext.scheduler.plugin.isEnabled) {
      context.cancel(disabledPluginException(bukkitContext.scheduler))
      cancelDelegate.dispatch(context, block)
      return
    }

    if (bukkitContext.sync === SynchronizationContext.ASYNC) {
      asyncDelegate.dispatch(context, block)
    } else {
      val task =
          bukkitContext.runTask(block) {
            context.cancel(retiredEntityException(bukkitContext.scheduler))
            cancelDelegate.dispatch(context, block)
          }
      if (task === null) {
        context.cancel(removedEntityException(bukkitContext.scheduler))
        cancelDelegate.dispatch(context, block)
      }
    }
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

  private fun disabledPluginException(scheduler: AbstractScheduler) =
      CancellationException("The plugin for the $scheduler is not enabled")
  private fun missingBukkitContextException() =
      CancellationException(
          "The BukkitDispatcher requires the BukkitContext to be available in the coroutine context")
  private fun retiredEntityException(scheduler: AbstractScheduler) =
      CancellationException(
          "The entity has been retired after the coroutine has been dispatched on $scheduler")
  private fun removedEntityException(scheduler: AbstractScheduler) =
      CancellationException(
          "The entity has been removed before the coroutine could be dispatched on $scheduler")
}
