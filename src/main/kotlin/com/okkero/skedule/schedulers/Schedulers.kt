package com.okkero.skedule.schedulers

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

object Schedulers {
  private val isFolia =
      try {
        Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler")
        true
      } catch (e: ClassNotFoundException) {
        false
      }

  fun global(plugin: Plugin) =
      if (isFolia) {
        FoliaGlobalScheduler(plugin)
      } else {
        BukkitScheduler(plugin)
      }

  fun region(plugin: Plugin, location: Location) =
      if (isFolia) {
        FoliaRegionScheduler(plugin, location)
      } else {
        BukkitScheduler(plugin)
      }

  fun entity(plugin: Plugin, entity: Entity) =
      if (isFolia) {
        FoliaEntityScheduler(plugin, entity)
      } else {
        BukkitScheduler(plugin)
      }
}

sealed interface AbstractScheduler {

  fun schedule(block: Runnable, retired: Runnable): AbstractScheduledTask?

  fun scheduleAsync(block: Runnable, retired: Runnable): AbstractScheduledTask?

  fun scheduleLater(block: Runnable, delay: Long, retired: Runnable): AbstractScheduledTask?

  fun scheduleLaterAsync(block: Runnable, delay: Long, retired: Runnable): AbstractScheduledTask?

  fun scheduleTimer(block: Runnable, interval: Long, retired: Runnable): AbstractScheduledTask?

  fun scheduleTimerAsync(block: Runnable, interval: Long, retired: Runnable): AbstractScheduledTask?
}

private class BukkitScheduler(private val plugin: Plugin) : AbstractScheduler {

  private val scheduler
    get() = plugin.server.scheduler

  override fun schedule(block: Runnable, retired: Runnable) =
      BukkitScheduledTask(scheduler.runTask(plugin, block))

  override fun scheduleAsync(block: Runnable, retired: Runnable) =
      BukkitScheduledTask(scheduler.runTaskAsynchronously(plugin, block))

  override fun scheduleLater(block: Runnable, delay: Long, retired: Runnable) =
      BukkitScheduledTask(scheduler.runTaskLater(plugin, block, delay))

  override fun scheduleLaterAsync(block: Runnable, delay: Long, retired: Runnable) =
      BukkitScheduledTask(scheduler.runTaskLaterAsynchronously(plugin, block, delay))

  override fun scheduleTimer(block: Runnable, interval: Long, retired: Runnable) =
      BukkitScheduledTask(scheduler.runTaskTimer(plugin, block, 0L, interval))

  override fun scheduleTimerAsync(block: Runnable, interval: Long, retired: Runnable) =
      BukkitScheduledTask(scheduler.runTaskTimerAsynchronously(plugin, block, 0L, interval))

  override fun toString() = "BukkitScheduler(plugin=$plugin)"
}

private sealed class FoliaSchedulerBase(protected val plugin: Plugin) : AbstractScheduler {

  private val scheduler
    get() = plugin.server.asyncScheduler

  override fun scheduleAsync(block: Runnable, retired: Runnable): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runNow(plugin, ConsumerRunner(block)))

  override fun scheduleLaterAsync(
      block: Runnable,
      delay: Long,
      retired: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(
          scheduler.runDelayed(plugin, ConsumerRunner(block), delay * 50, TimeUnit.MILLISECONDS))

  override fun scheduleTimerAsync(
      block: Runnable,
      interval: Long,
      retired: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(
          scheduler.runAtFixedRate(
              plugin, ConsumerRunner(block), 0L, interval * 50, TimeUnit.MILLISECONDS),
      )
}

private class FoliaGlobalScheduler(plugin: Plugin) : FoliaSchedulerBase(plugin) {

  private val scheduler
    get() = plugin.server.globalRegionScheduler

  override fun schedule(block: Runnable, retired: Runnable): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.run(plugin, ConsumerRunner(block)))

  override fun scheduleLater(
      block: Runnable,
      delay: Long,
      retired: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runDelayed(plugin, ConsumerRunner(block), delay))

  override fun scheduleTimer(
      block: Runnable,
      interval: Long,
      retired: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runAtFixedRate(plugin, ConsumerRunner(block), 0L, interval))

  override fun toString() = "FoliaGlobalScheduler(plugin=$plugin)"
}

private class FoliaRegionScheduler(
    plugin: Plugin,
    private val location: Location,
) : FoliaSchedulerBase(plugin) {

  private val scheduler
    get() = plugin.server.regionScheduler

  override fun schedule(block: Runnable, retired: Runnable): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.run(plugin, location, ConsumerRunner(block)))

  override fun scheduleLater(
      block: Runnable,
      delay: Long,
      retired: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runDelayed(plugin, location, ConsumerRunner(block), delay))

  override fun scheduleTimer(
      block: Runnable,
      interval: Long,
      retired: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(
          scheduler.runAtFixedRate(plugin, location, ConsumerRunner(block), 0L, interval))

  override fun toString() = "FoliaRegionScheduler(plugin=$plugin, location=$location)"
}

private class FoliaEntityScheduler(
    plugin: Plugin,
    private val entity: Entity,
) : FoliaSchedulerBase(plugin) {

  private val scheduler
    get() = entity.scheduler

  override fun schedule(block: Runnable, retired: Runnable): AbstractScheduledTask? =
      scheduler.run(plugin, ConsumerRunner(block), retired)?.let { FoliaScheduledTask(it) }

  override fun scheduleLater(
      block: Runnable,
      delay: Long,
      retired: Runnable
  ): AbstractScheduledTask? =
      scheduler.runDelayed(plugin, ConsumerRunner(block), retired, delay)?.let {
        FoliaScheduledTask(it)
      }

  override fun scheduleTimer(
      block: Runnable,
      interval: Long,
      retired: Runnable
  ): AbstractScheduledTask? =
      scheduler.runAtFixedRate(plugin, ConsumerRunner(block), retired, 0L, interval)?.let {
        FoliaScheduledTask(it)
      }

  override fun toString() = "FoliaEntityScheduler(plugin=$plugin, entity=$entity)"
}

sealed interface AbstractScheduledTask {
  fun cancel()
}

private class BukkitScheduledTask(
    private val task: BukkitTask,
) : AbstractScheduledTask {
  override fun cancel() {
    task.cancel()
  }

  override fun toString() = "BukkitScheduledTask(task=$task)"
}

private class FoliaScheduledTask(
    private val task: ScheduledTask,
) : AbstractScheduledTask {
  override fun cancel() {
    task.cancel()
  }

  override fun toString() = "FoliaScheduledTask(task=$task)"
}

private class ConsumerRunner(
    private val block: Runnable,
) : Consumer<ScheduledTask> {
  override fun accept(t: ScheduledTask) {
    block.run()
  }
}
