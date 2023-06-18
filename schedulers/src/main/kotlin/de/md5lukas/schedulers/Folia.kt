package de.md5lukas.schedulers

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.plugin.Plugin

internal sealed class FoliaSchedulerBase(override val plugin: Plugin) : AbstractScheduler {

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
      block: Runnable,
  ): AbstractScheduledTask =
      FoliaScheduledTask(
          scheduler.runAtFixedRate(
              plugin, ConsumerRunner(block), delay * 50, interval * 50, TimeUnit.MILLISECONDS),
      )
}

internal class FoliaGlobalScheduler(plugin: Plugin) : FoliaSchedulerBase(plugin) {

  private val scheduler
    get() = plugin.server.globalRegionScheduler

  override fun schedule(retired: Runnable?, block: Runnable): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.run(plugin, ConsumerRunner(block)))

  override fun scheduleDelayed(
      delay: Long,
      retired: Runnable?,
      block: Runnable,
  ): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runDelayed(plugin, ConsumerRunner(block), delay))

  override fun scheduleAtFixedRate(
      interval: Long,
      delay: Long,
      retired: Runnable?,
      block: Runnable,
  ): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runAtFixedRate(plugin, ConsumerRunner(block), delay, interval))

  override fun toString() = "FoliaGlobalScheduler(plugin=$plugin)"
}

internal class FoliaRegionScheduler(
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
      block: Runnable,
  ): AbstractScheduledTask =
      FoliaScheduledTask(scheduler.runDelayed(plugin, location, ConsumerRunner(block), delay))

  override fun scheduleAtFixedRate(
      interval: Long,
      delay: Long,
      retired: Runnable?,
      block: Runnable,
  ): AbstractScheduledTask =
      FoliaScheduledTask(
          scheduler.runAtFixedRate(plugin, location, ConsumerRunner(block), delay, interval))

  override fun toString() = "FoliaRegionScheduler(plugin=$plugin, location=$location)"
}

internal class FoliaEntityScheduler(
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
      block: Runnable,
  ): AbstractScheduledTask? =
      scheduler.runDelayed(plugin, ConsumerRunner(block), retired, delay)?.let {
        FoliaScheduledTask(it)
      }

  override fun scheduleAtFixedRate(
      interval: Long,
      delay: Long,
      retired: Runnable?,
      block: Runnable,
  ): AbstractScheduledTask? =
      scheduler.runAtFixedRate(plugin, ConsumerRunner(block), retired, delay, interval)?.let {
        FoliaScheduledTask(it)
      }

  override fun toString() = "FoliaEntityScheduler(plugin=$plugin, entity=$entity)"
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
