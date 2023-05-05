package com.okkero.skedule.schedulers

import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.scheduler.BukkitTask

sealed interface AbstractScheduledTask {
    fun cancel()
}

internal class BukkitScheduledTask(
    private val task: BukkitTask,
) : AbstractScheduledTask {
    override fun cancel() {
        task.cancel()
    }
}

internal class FoliaScheduledTask(
    private val task: ScheduledTask,
) : AbstractScheduledTask {
    override fun cancel() {
        task.cancel()
    }
}