package de.md5lukas.schedulers

import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

internal class BukkitScheduler(override val plugin: Plugin) : AbstractScheduler {

  private val scheduler
    get() = plugin.server.scheduler

  override fun schedule(retired: Runnable?, block: Runnable): AbstractScheduledTask =
    BukkitScheduledTask(scheduler.runTask(plugin, block))

  override fun scheduleAsync(block: Runnable): AbstractScheduledTask =
    BukkitScheduledTask(scheduler.runTaskAsynchronously(plugin, block))

  override fun scheduleDelayed(delay: Long, retired: Runnable?, block: Runnable): AbstractScheduledTask =
    BukkitScheduledTask(scheduler.runTaskLater(plugin, block, delay))

  override fun scheduleDelayedAsync(delay: Long, block: Runnable): AbstractScheduledTask =
    BukkitScheduledTask(scheduler.runTaskLaterAsynchronously(plugin, block, delay))

  override fun scheduleAtFixedRate(
    interval: Long,
    delay: Long,
    retired: Runnable?,
    block: Runnable
  ): AbstractScheduledTask = BukkitScheduledTask(scheduler.runTaskTimer(plugin, block, delay, interval))

  override fun scheduleAtFixedRateAsync(interval: Long, delay: Long, block: Runnable): AbstractScheduledTask =
    BukkitScheduledTask(scheduler.runTaskTimerAsynchronously(plugin, block, delay, interval))

  override fun toString() = "BukkitScheduler(plugin=$plugin)"
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