package de.md5lukas.schedulers

import de.md5lukas.schedulers.Schedulers.entity
import de.md5lukas.schedulers.Schedulers.global
import de.md5lukas.schedulers.Schedulers.region
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler
import io.papermc.paper.threadedregions.scheduler.EntityScheduler
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler
import io.papermc.paper.threadedregions.scheduler.RegionScheduler
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

/**
 * Helper Object to provide generic access to the Bukkit and Folia schedulers.
 *
 * On non-Folia servers everything will be handled by the same scheduler, the [org.bukkit.scheduler.BukkitScheduler].
 * On Folia servers all async schedules are handled by the [AsyncScheduler], while the sync variants
 * depend on which function is selected to retrieve the [AbstractScheduler].
 * - [global] uses the [GlobalRegionScheduler]
 * - [region] uses the [RegionScheduler]
 * - [entity] uses the [EntityScheduler]
 */
object Schedulers {

  /**
   * <code>true</code> if the current server implementation is Folia, <code>false</code> otherwise
   */
  val isFolia =
      try {
        Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler")
        true
      } catch (e: ClassNotFoundException) {
        false
      }

  /**
   * Get an instance for the global scheduler
   *
   * @param plugin The plugin to register the tasks for
   */
  fun global(plugin: Plugin) =
      if (isFolia) {
        FoliaGlobalScheduler(plugin)
      } else {
        BukkitScheduler(plugin)
      }

  /**
   * Get an instance to a scheduler for a specific region
   *
   * @param plugin The plugin to register the tasks for
   * @param location The location of the region to register the tasks for
   */
  fun region(plugin: Plugin, location: Location) =
      if (isFolia) {
        FoliaRegionScheduler(plugin, location)
      } else {
        BukkitScheduler(plugin)
      }

  /**
   * Get an instance to a scheduler for a specific entity
   *
   * @param plugin The plugin to register the tasks for
   * @param entity The entity to register the tasks for
   */
  fun entity(plugin: Plugin, entity: Entity) =
      if (isFolia) {
        FoliaEntityScheduler(plugin, entity)
      } else {
        BukkitScheduler(plugin)
      }
}

/**
 * An encapsulation of the various schedulers.
 *
 * The retired callback is only possibly executed on folia servers when scheduling on an entity
 */
sealed interface AbstractScheduler {

  /**
   * The plugin that the scheduler registers tasks with
   */
  val plugin: Plugin

  /**
   * Provides a view of this scheduler as an [Executor]. The executor either delegates to [schedule]
   * or [scheduleAsync], depending on the provided argument
   */
  fun asExecutor(async: Boolean = true) =
      if (async) {
        Executor { scheduleAsync(it) }
      } else {
        Executor { schedule(null, it) }
      }

  /**
   * Schedules the [Runnable] to execute at the next possible moment in sync with the server thread.
   *
   * Only the [EntityScheduler] *may* return a <code>null</code> scheduled task.
   *
   * @param retired Callback that only gets called by the Folia [EntityScheduler] if that entity is no longer valid
   * @param block Runnable to execute
   * @return The scheduled task or <code>null</code> if the entity is no longer valid
   */
  fun schedule(retired: Runnable? = null, block: Runnable): AbstractScheduledTask?

  /**
   * Schedules the [Runnable] to execute at the next possible moment async to any server thread.
   *
   * @param block Runnable to execute
   * @return The scheduled task
   */
  fun scheduleAsync(block: Runnable): AbstractScheduledTask

  /**
   * Schedules the [Runnable] to execute with the given delay in sync with the server thread.
   *
   * Only the [EntityScheduler] *may* return a <code>null</code> scheduled task.
   *
   * @param delay The delay of execution in ticks
   * @param retired Callback that only gets called by the Folia [EntityScheduler] if that entity is no longer valid
   * @param block Runnable to execute
   * @return The scheduled task or <code>null</code> if the entity is no longer valid
   */
  fun scheduleDelayed(
      delay: Long,
      retired: Runnable? = null,
      block: Runnable
  ): AbstractScheduledTask?

  /**
   * Schedules the [Runnable] to execute with the given delay async to any server thread.
   *
   * On Folia the delay is not coupled to server ticks, but each tick is converted to exactly 50ms
   *
   * @param delay The delay of execution in ticks
   * @param block Runnable to execute
   * @return The scheduled task or <code>null</code> if the entity is no longer valid
   */
  fun scheduleDelayedAsync(delay: Long, block: Runnable): AbstractScheduledTask

  /**
   * Schedules the [Runnable] to execute with the given delay in sync with the server thread.
   * After the first execution the task is repeatedly called with in the given interval until cancellation.
   *
   * Only the [EntityScheduler] *may* return a <code>null</code> scheduled task.
   *
   * @param interval The interval of execution in ticks
   * @param delay The delay of execution in ticks
   * @param retired Callback that only gets called by the Folia [EntityScheduler] if that entity is no longer valid
   * @param block Runnable to execute
   * @return The scheduled task or <code>null</code> if the entity is no longer valid
   */
  fun scheduleAtFixedRate(
      interval: Long,
      delay: Long = 0,
      retired: Runnable? = null,
      block: Runnable
  ): AbstractScheduledTask?

  /**
   * Schedules the [Runnable] to execute with the given delay in async to any server thread.
   * After the first execution the task is repeatedly called with in the given interval until cancellation.
   *
   * On Folia the delay is not coupled to server ticks, but each tick is converted to exactly 50ms
   *
   * @param interval The interval of execution in ticks
   * @param delay The delay of execution in ticks
   * @param block Runnable to execute
   * @return The scheduled task or <code>null</code> if the entity is no longer valid
   */
  fun scheduleAtFixedRateAsync(
      interval: Long,
      delay: Long = 0,
      block: Runnable
  ): AbstractScheduledTask
}

private class BukkitScheduler(override val plugin: Plugin) : AbstractScheduler {

  private val scheduler
    get() = plugin.server.scheduler

  override fun schedule(retired: Runnable?, block: Runnable) =
      BukkitScheduledTask(scheduler.runTask(plugin, block))

  override fun scheduleAsync(block: Runnable) =
      BukkitScheduledTask(scheduler.runTaskAsynchronously(plugin, block))

  override fun scheduleDelayed(delay: Long, retired: Runnable?, block: Runnable) =
      BukkitScheduledTask(scheduler.runTaskLater(plugin, block, delay))

  override fun scheduleDelayedAsync(delay: Long, block: Runnable) =
      BukkitScheduledTask(scheduler.runTaskLaterAsynchronously(plugin, block, delay))

  override fun scheduleAtFixedRate(
    interval: Long,
    delay: Long,
    retired: Runnable?,
    block: Runnable
  ) = BukkitScheduledTask(scheduler.runTaskTimer(plugin, block, delay, interval))

  override fun scheduleAtFixedRateAsync(interval: Long, delay: Long, block: Runnable) =
      BukkitScheduledTask(scheduler.runTaskTimerAsynchronously(plugin, block, delay, interval))

  override fun toString() = "BukkitScheduler(plugin=$plugin)"
}

private sealed class FoliaSchedulerBase(override val plugin: Plugin) : AbstractScheduler {

  private val scheduler
    get() = plugin.server.asyncScheduler

  override fun scheduleAsync(block: Runnable): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runNow(plugin, ConsumerRunner(block)))

  override fun scheduleDelayedAsync(delay: Long, block: Runnable): AbstractScheduledTask =
      FoliaScheduledTask(
          scheduler.runDelayed(plugin, ConsumerRunner(block), delay * 50, TimeUnit.MILLISECONDS))

  override fun scheduleAtFixedRateAsync(
      interval: Long,
      delay: Long,
      block: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(
          scheduler.runAtFixedRate(
              plugin, ConsumerRunner(block), delay * 50, interval * 50, TimeUnit.MILLISECONDS),
      )
}

private class FoliaGlobalScheduler(plugin: Plugin) : FoliaSchedulerBase(plugin) {

  private val scheduler
    get() = plugin.server.globalRegionScheduler

  override fun schedule(retired: Runnable?, block: Runnable): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.run(plugin, ConsumerRunner(block)))

  override fun scheduleDelayed(
    delay: Long,
    retired: Runnable?,
    block: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runDelayed(plugin, ConsumerRunner(block), delay))

  override fun scheduleAtFixedRate(
    interval: Long,
    delay: Long,
    retired: Runnable?,
    block: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runAtFixedRate(plugin, ConsumerRunner(block), delay, interval))

  override fun toString() = "FoliaGlobalScheduler(plugin=$plugin)"
}

private class FoliaRegionScheduler(
    plugin: Plugin,
    private val location: Location,
) : FoliaSchedulerBase(plugin) {

  private val scheduler
    get() = plugin.server.regionScheduler

  override fun schedule(retired: Runnable?, block: Runnable): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.run(plugin, location, ConsumerRunner(block)))

  override fun scheduleDelayed(
    delay: Long,
    retired: Runnable?,
    block: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runDelayed(plugin, location, ConsumerRunner(block), delay))

  override fun scheduleAtFixedRate(
    interval: Long,
    delay: Long,
    retired: Runnable?,
    block: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(
          scheduler.runAtFixedRate(plugin, location, ConsumerRunner(block), delay, interval))

  override fun toString() = "FoliaRegionScheduler(plugin=$plugin, location=$location)"
}

private class FoliaEntityScheduler(
    plugin: Plugin,
    private val entity: Entity,
) : FoliaSchedulerBase(plugin) {

  private val scheduler
    get() = entity.scheduler

  override fun schedule(retired: Runnable?, block: Runnable): AbstractScheduledTask? =
      scheduler.run(plugin, ConsumerRunner(block), retired)?.let { FoliaScheduledTask(it) }

  override fun scheduleDelayed(
    delay: Long,
    retired: Runnable?,
    block: Runnable
  ): AbstractScheduledTask? =
      scheduler.runDelayed(plugin, ConsumerRunner(block), retired, delay)?.let {
        FoliaScheduledTask(it)
      }

  override fun scheduleAtFixedRate(
    interval: Long,
    delay: Long,
    retired: Runnable?,
    block: Runnable
  ): AbstractScheduledTask? =
      scheduler.runAtFixedRate(plugin, ConsumerRunner(block), retired, delay, interval)?.let {
        FoliaScheduledTask(it)
      }

  override fun toString() = "FoliaEntityScheduler(plugin=$plugin, entity=$entity)"
}

/**
 * An encapsulation of an scheduled task
 */
sealed interface AbstractScheduledTask {

  /**
   * Attempt to cancel the task.
   */
  fun cancel()

  /**
   * Check if the task has already been cancelled
   */
  val isCancelled: Boolean
}

private class BukkitScheduledTask(
    private val task: BukkitTask,
) : AbstractScheduledTask {
  override fun cancel() {
    task.cancel()
  }

  override val isCancelled: Boolean
    get() = task.isCancelled

  override fun toString() = "BukkitScheduledTask(task=$task)"
}

private class FoliaScheduledTask(
    private val task: ScheduledTask,
) : AbstractScheduledTask {
  override fun cancel() {
    task.cancel()
  }

  override val isCancelled: Boolean
    get() = task.isCancelled

  override fun toString() = "FoliaScheduledTask(task=$task)"
}

private class ConsumerRunner(
    private val block: Runnable,
) : Consumer<ScheduledTask> {
  override fun accept(t: ScheduledTask) {
    block.run()
  }
}