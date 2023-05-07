# Skedule
![](https://repo.md5lukas.de/api/badge/latest/releases/de/md5lukas/skedule)
![](https://img.shields.io/github/license/Sytm/Skedule)  
Please note this is a fork from the forked [Skedule](https://github.com/BrunoSilvaFreire/Skedule).  
All instructions in this ReadMe are update with new repositories and artifacts.

The objetives of this fork is:
* Don't use the continuation shenanigans of the original and instead control the dispatcher via coroutine context elements

Skedule is a small coroutine library for the [AbstractSchedulers](https://github.com/Sytm/Skedule/tree/master/schedulers)
for Bukkit/Spigot/Folia plugin developers using Kotlin.

Tired of designing complex BukkitRunnables to meet your needs? Do you find yourself in [Callback Hell](http://callbackhell.com/) a tad too often?
Fret no more, for with Kotlin's coroutines and this nifty little utility, you will be scheduling tasks like never before!

## How to use Skedule?

To get an overview of the API take a look at the [KDocs](https://repo.md5lukas.de/javadoc/releases/de/md5lukas/skedule/2.0.0/raw/index.html)

### Asynchronous tasks
We often find ourselves
having to do I/O or query a database, or we might have to do some long and costly operations. In all of
these cases, so as to not block the game thread, we want to schedule an asynchronous task. Skedule supports
this. To schedule any task with Skedule, a SynchronizationContext needs to be provided. If you do not provide
a SynchronizationContext, `ASYNC` is inferred. If you want to schedule synchronous tasks with Skedule, you
need to explicitly pass `SYNC`:
```kotlin
plugin.skedule {
    Bukkit.broadcastMessage("Doing some heavy work off the main thread")
    //Do costly operation
}
```
You can also switch back and forth between sync and async execution:
```kotlin
plugin.skedule {
    Bukkit.broadcastMessage("Doing some heavy work off the main thread")
    //Do costly operation off the main thread
    switchContext(SynchronizationContext.SYNC)
    //Do stuff on the main thread
    switchContext(SynchronizationContext.ASYNC)
    //Do more costly stuff off the main thread
}
```

### Delays
To suspend the coroutine for a given amount of time we use the default `delay(t)` implementation of the
coroutines library. Due to the fact that this function takes milliseconds as time delay, we must convert our timings
to milliseconds. Internally this value is again divided by 50 and submitted to the BukkitScheduler.

The simplest example looks like this:
```kotlin
plugin.skedule(SynchronizationContext.SYNC) {
    delay(40 * 50) // or 2000
    Bukkit.broadcastMessage("Waited 40 ticks or 2 seconds")
}
```
Of course, this isn't very useful, and doesn't really showcase what Skedule is capable of.
So here is a more useful example:
```kotlin
plugin.skedule(SynchronizationContext.SYNC) {
    Bukkit.broadcastMessage("Waited 0 ticks")
    delay(1000)
    Bukkit.broadcastMessage("Waited 20 ticks")
    delay(1000)
    Bukkit.broadcastMessage("Waited 40 ticks")
    delay(1000)
    Bukkit.broadcastMessage("Waited 60 ticks")
}
```
This may look like procedural code that will block the main server thread, but it really isn't.
The extension method `schedule` starts a coroutine. At each of the delay calls the coroutine is suspended,
a task is scheduled, and the rest of the coroutine is set aside for continuation at a later point
(20 game ticks in the future in this case). After this, control is yielded back to the caller (your plugin).
From there, the server carries on doing whatever it was doing, until the 40 ticks have passed, after which
the coroutine will continue until suspended again, or finished.

### A more useful example
A great real-world example of when Skedule would be useful, is when you need a countdown of some sort.
Say you wanted to start a game countdown of 10 seconds, and each second you wanted to display the
remaining time. With Skedule, this is super easy. No need to create an entirely new class that implements
Runnable and uses mutable state to track how many seconds are left. All you have to do, is use a regular
for-loop:
```kotlin
scheduler.schedule(plugin) {
    for (i in 10 downTo 1) {
        Bukkit.broadcastMessage("Time left: $i sec...")
        delay(1000)
    }
    Bukkit.broadcastMessage("Game starts now!")
}
```
This example really shows where Skedule is at its most powerful.

### Adding the dependencies to your project

```kotlin
repositories {
  maven("https://repo.md5lukas.de/releases")
}

dependencies {
  // Both need to be shadowed separately
  implementation("de.md5lukas:skedule:2.0.0")
  implementation("de.md5lukas:schedulers:1.0.0")
}
```