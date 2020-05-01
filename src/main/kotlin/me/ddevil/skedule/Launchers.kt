package me.ddevil.skedule

import com.okkero.skedule.*
import com.okkero.skedule.bukkitScheduler
import org.bukkit.plugin.Plugin

/**
 * Sugar function to allow for easier creation of coroutines.
 *
 * For example, before:
 * ```kotlin
 * Bukkit.getScheduler().schedule(myPlugin) {
 *     //...
 * }
 * ```
 * After:
 * ```kotlin
 * skeduleSync(myPlugin) {
 *     //...
 * }
 * ```
 */
fun skeduleSync(plugin: Plugin, block: suspend BukkitSchedulerController.() -> Unit): CoroutineTask {
    return bukkitScheduler.schedule(plugin, SynchronizationContext.SYNC, block)
}

/**
 * Sugar function to allow for easier creation of coroutines.
 *
 * For example, before:
 * ```kotlin
 * Bukkit.getScheduler().schedule(myPlugin, SynchronizationContext.ASYNC) {
 *     //...
 * }
 * ```
 * After:
 * ```kotlin
 * skeduleAsync(myPlugin) {
 *     //...
 * }
 * ```
 */
fun skeduleAsync(plugin: Plugin, block: suspend BukkitSchedulerController.() -> Unit): CoroutineTask {
    return bukkitScheduler.schedule(plugin, SynchronizationContext.ASYNC, block)
}