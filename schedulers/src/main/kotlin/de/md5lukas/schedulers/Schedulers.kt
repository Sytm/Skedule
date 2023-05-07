package de.md5lukas.schedulers

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.util.concurrent.Executor
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

  fun asExecutor(async: Boolean = true) =
      if (async) {
        Executor { scheduleAsync(NOOP, it) }
      } else {
        Executor { schedule(NOOP, it) }
      }

  fun schedule(retired: Runnable = NOOP, block: Runnable): AbstractScheduledTask?

  fun scheduleAsync(block: Runnable): AbstractScheduledTask

  fun scheduleDelayed(
      delay: Long,
      retired: Runnable = NOOP,
      block: Runnable
  ): AbstractScheduledTask?

  fun scheduleDelayedAsync(
      delay: Long,
      block: Runnable
  ): AbstractScheduledTask

  fun scheduleAtFixedRate(
      interval: Long,
      delay: Long = 0,
      retired: Runnable = NOOP,
      block: Runnable
  ): AbstractScheduledTask?

  fun scheduleAtFixedRateAsync(
      interval: Long,
      delay: Long = 0,
      block: Runnable
  ): AbstractScheduledTask
}

private class BukkitScheduler(private val plugin: Plugin) : AbstractScheduler {

  private val scheduler
    get() = plugin.server.scheduler

  override fun schedule(retired: Runnable, block: Runnable) =
      BukkitScheduledTask(scheduler.runTask(plugin, block))

  override fun scheduleAsync(block: Runnable) =
      BukkitScheduledTask(scheduler.runTaskAsynchronously(plugin, block))

  override fun scheduleDelayed(delay: Long, retired: Runnable, block: Runnable) =
      BukkitScheduledTask(scheduler.runTaskLater(plugin, block, delay))

  override fun scheduleDelayedAsync(delay: Long, block: Runnable) =
      BukkitScheduledTask(scheduler.runTaskLaterAsynchronously(plugin, block, delay))

  override fun scheduleAtFixedRate(
      interval: Long,
      delay: Long,
      retired: Runnable,
      block: Runnable
  ) = BukkitScheduledTask(scheduler.runTaskTimer(plugin, block, delay, interval))

  override fun scheduleAtFixedRateAsync(
    interval: Long,
    delay: Long,
    block: Runnable
  ) = BukkitScheduledTask(scheduler.runTaskTimerAsynchronously(plugin, block, delay, interval))

  override fun toString() = "BukkitScheduler(plugin=$plugin)"
}

private sealed class FoliaSchedulerBase(protected val plugin: Plugin) : AbstractScheduler {

  private val scheduler
    get() = plugin.server.asyncScheduler

  override fun scheduleAsync(block: Runnable): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runNow(plugin, ConsumerRunner(block)))

  override fun scheduleDelayedAsync(
    delay: Long,
    block: Runnable
  ): AbstractScheduledTask =
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

  override fun schedule(retired: Runnable, block: Runnable): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.run(plugin, ConsumerRunner(block)))

  override fun scheduleDelayed(
      delay: Long,
      retired: Runnable,
      block: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runDelayed(plugin, ConsumerRunner(block), delay))

  override fun scheduleAtFixedRate(
      interval: Long,
      delay: Long,
      retired: Runnable,
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

  override fun schedule(retired: Runnable, block: Runnable): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.run(plugin, location, ConsumerRunner(block)))

  override fun scheduleDelayed(
      delay: Long,
      retired: Runnable,
      block: Runnable
  ): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runDelayed(plugin, location, ConsumerRunner(block), delay))

  override fun scheduleAtFixedRate(
      interval: Long,
      delay: Long,
      retired: Runnable,
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

  override fun schedule(retired: Runnable, block: Runnable): AbstractScheduledTask? =
      scheduler.run(plugin, ConsumerRunner(block), retired)?.let { FoliaScheduledTask(it) }

  override fun scheduleDelayed(
      delay: Long,
      retired: Runnable,
      block: Runnable
  ): AbstractScheduledTask? =
      scheduler.runDelayed(plugin, ConsumerRunner(block), retired, delay)?.let {
        FoliaScheduledTask(it)
      }

  override fun scheduleAtFixedRate(
      interval: Long,
      delay: Long,
      retired: Runnable,
      block: Runnable
  ): AbstractScheduledTask? =
      scheduler.runAtFixedRate(plugin, ConsumerRunner(block), retired, delay, interval)?.let {
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

object NOOP : Runnable {
  override fun run() {}
}
