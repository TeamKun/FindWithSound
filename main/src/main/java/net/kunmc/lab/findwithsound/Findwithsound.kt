package net.kunmc.lab.findwithsound

import net.kunmc.lab.findwithsound.effect.SimpleEffect
import net.kunmc.lab.findwithsound.flylib.SmartTabCompleter
import net.kunmc.lab.findwithsound.flylib.TabChain
import net.kunmc.lab.findwithsound.flylib.TabObject
import net.kunmc.lab.findwithsound.sound.SoundUtils
import org.bukkit.*
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class Findwithsound : JavaPlugin() {
    lateinit var command: net.kunmc.lab.findwithsound.Command
    lateinit var manager: GameManager
    override fun onEnable() {
        // Plugin startup logic
        manager = GameManager(this)
        command = Command(this)
        getCommand("fis")!!.setExecutor(command)
        getCommand("fis")!!.tabCompleter = command.genTabCompleter()
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}

class Command(val plugin: Findwithsound) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        return if (sender is Player) {
            if (sender.isOp) {
                run(sender, command, label, args)
            } else false
        } else run(sender, command, label, args)
    }

    fun run(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.size != 1) return false
        when (args[0]) {
            "s", "start" -> {
                plugin.manager.isGoing = true
            }
            "e", "end" -> {
                plugin.manager.isGoing = false
            }
            "set" -> {
                if (sender is Player) {
                    plugin.manager.setMode(sender)
                }
            }
            else -> {
                return false
            }
        }
        return true
    }

    fun genTabCompleter(): SmartTabCompleter {
        return SmartTabCompleter(
            mutableListOf(
                TabChain(
                    arrayOf(
                        TabObject(
                            arrayOf(
                                "s", "start"
                            )
                        ),
                        TabObject(
                            arrayOf(
                                "e", "end"
                            )
                        ),
                        TabObject(
                            arrayOf(
                                "set"
                            )
                        )
                    )
                )
            )
        )
    }
}

class GameManager(plugin: JavaPlugin) : Listener {
    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { onTick() }, 10, 1)
        plugin.server.pluginManager.registerEvents(this, plugin)
    }

    // Main Logic
    var isGoing = false

    fun onTick() {
        if (isGoing) {
            playSound()
        }
    }

    fun playSound() {
        treasures
            .filter { !it.isFounded }
            .forEach {
                it.showEffect()
                it.playSound()
            }
    }

    //Set Logic
    val treasures = mutableListOf<Treasure>()
    val settingPlayer = mutableListOf<Player>()
    fun setMode(p: Player) {
        if (settingPlayer.contains(p)) {
            settingPlayer.remove(p)
        } else settingPlayer.add(p)
    }

    @EventHandler
    fun onBlockPlaced(e: BlockPlaceEvent) {
        if (settingPlayer.contains(e.player)) {
            e.player.sendMessage("お宝を配置しました!")
            e.player.sendMessage("続けてお宝を配置できます!")
            e.player.sendMessage("終了する場合は/fis set")
            treasures.add(Treasure(e.blockPlaced.location))
        }
    }
}

class Treasure(val loc: Location) {
    companion object {
        const val volume = 7.0f
        val sound = Sound.ENTITY_ITEM_PICKUP
        val effect = Particle.FIREWORKS_SPARK
        fun getDistance(loc: Location, loc2: Location): Double {
            return sqrt(
                (max(loc.x, loc2.x) - min(loc.x, loc2.x)) * (max(loc.x, loc2.x) - min(loc.x, loc2.x)) +
                        (max(loc.y, loc2.y) - min(loc.y, loc2.y)) * (max(loc.y, loc2.y) - min(loc.y, loc2.y)) +
                        (max(loc.z, loc2.z) - min(loc.z, loc2.z)) * (max(loc.z, loc2.z) - min(loc.z, loc2.z))
            )
        }
    }

    var isFounded = false

    fun playSound() {
        val dis = mutableMapOf<Player, Double>()
        Bukkit.getOnlinePlayers().filter {
            dis[it] = getDistance(it.location, loc)
            dis[it]!! < 50.0
        }.forEach {
            if (dis[it]!! < 2.0) {
                Bukkit.broadcastMessage("${it.displayName}がお宝を発見した!")
                isFounded = true
            } else {
                SoundUtils.playSound(it, sound, (1 / dis[it]!! * volume).toFloat())
            }
        }
    }

    fun showEffect() {
        SimpleEffect.spawnParticle(loc, effect)
    }
}