package net.kunmc.lab.findwithsound

import com.destroystokyo.paper.Title
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
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.plugin.java.JavaPlugin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class Findwithsound : JavaPlugin() {
    lateinit var command: net.kunmc.lab.findwithsound.Command
    lateinit var manager: GameManager
    lateinit var noticer: InformationNoticer

    override fun onEnable() {
        // Plugin startup logic
        manager = GameManager(this)
        command = Command(this)
        noticer = InformationNoticer(this)
        getCommand("fws")!!.setExecutor(command)
        getCommand("fws")!!.tabCompleter = command.genTabCompleter()
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
                Bukkit.broadcastMessage("お宝さがし開始!")
                plugin.manager.isGoing = true
            }
            "e", "end" -> {
                Bukkit.broadcastMessage("お宝さがし終了!")
                plugin.manager.treasures.clear()
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
                                "s", "start",
                                "e", "end",
                                "set"
                            )
                        )
                    )
                )
            )
        )
    }
}

class GameManager(val plugin: Findwithsound) : Listener {
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
            p.sendMessage("設置モードが解除されました")
            settingPlayer.remove(p)
        } else {
            p.sendMessage("設置モードが有効化されました")
            settingPlayer.add(p)
        }
    }

    @EventHandler
    fun onBlockPlaced(e: BlockPlaceEvent) {
        if (settingPlayer.contains(e.player)) {
            e.player.sendMessage("お宝を配置しました!")
            e.player.sendMessage("続けてお宝を配置できます!")
            e.player.sendMessage("終了する場合は/fws set")
            val loc = e.blockPlaced.location
            loc.add(0.5, 0.5, 0.5)
            treasures.add(Treasure(loc, plugin))
        }
    }

    @EventHandler
    fun onBlockBreakEvent(e: BlockBreakEvent) {
        if (settingPlayer.contains(e.player)) {
            val loc = e.block.location.add(0.5, 0.5, 0.5)
            val r = mutableListOf<Treasure>()
            treasures.filter {
                it.loc.blockX == loc.blockX &&
                        it.loc.blockY == loc.blockY &&
                        it.loc.blockZ == loc.blockZ
            }.forEach {
                r.add(it)
            }

            r.forEach { treasures.remove(it);e.player.sendMessage("お宝を破壊した!") }
        }
    }
}

class Treasure(val loc: Location, val plugin: Findwithsound) {
    companion object {
        const val volume = 0.3f
        val sound = Sound.ENTITY_ITEM_PICKUP
        val foundSound = Sound.ENTITY_COW_MILK
        val effect = Particle.FIREWORKS_SPARK
        const val max_distance = 64
        const val interval = 64

        fun getDistance(loc: Location, loc2: Location): Double {
            return sqrt(
                (max(loc.x, loc2.x) - min(loc.x, loc2.x)) * (max(loc.x, loc2.x) - min(loc.x, loc2.x)) +
                        (max(loc.y, loc2.y) - min(loc.y, loc2.y)) * (max(loc.y, loc2.y) - min(loc.y, loc2.y)) +
                        (max(loc.z, loc2.z) - min(loc.z, loc2.z)) * (max(loc.z, loc2.z) - min(loc.z, loc2.z))
            )
        }

        fun getVolume(dis: Double): Float {
            return (1 * volume / dis).toFloat()
        }

        fun getIntervalTick(dis: Double): Int {
            return max(1, ((dis * dis) / interval).roundToInt())
        }
    }

    var isFounded = false
    val soundList = mutableMapOf<Player, Int>()

    fun playSound() {
        val dis = mutableMapOf<Player, Double>()
        Bukkit.getOnlinePlayers()
            .filter { !plugin.manager.settingPlayer.contains(it) }
            .filter {
                dis[it] = getDistance(it.location, loc)
                dis[it]!! < max_distance
            }.forEach {
                if (dis[it]!! < 2.0) {
                    onFound(it)
                } else {
                    invokeSound()
//                    SoundUtils.playSound(it, sound, (1 * volume / dis[it]!!).toFloat())
                }
            }
    }

    fun invokeSound() {
        Bukkit.getOnlinePlayers().filter { !plugin.manager.settingPlayer.contains(it) }
            .forEach {
                if (!soundList.containsKey(it)) {
                    soundList[it] = getIntervalTick(getDistance(loc, it.location))
                }
                soundList[it] = soundList[it]!! - 1
                if (soundList[it]!! <= 0) {
                    SoundUtils.playSound(it, sound, getVolume(getDistance(it.location, loc)))
                    soundList[it] = getIntervalTick(getDistance(loc, it.location))
                }
            }
    }

    fun getNotFounded(): Int {
        return plugin.manager.treasures.filter { !it.isFounded }.size
    }

    fun getFounded(): Int {
        return plugin.manager.treasures.filter { it.isFounded }.size
    }

    private fun onFound(it: Player) {
        isFounded = true
        if (plugin.manager.treasures.size <= getFounded()) {
            it.sendTitle(Title("${"" + ChatColor.GOLD + it.displayName + ChatColor.RESET}が最後の宝箱を発見!!", "${getFounded()}個の宝箱が発見済み", 2, 20 * 5, 2))
            plugin.manager.isGoing = false
            plugin.manager.treasures.clear()
            Bukkit.getOnlinePlayers().forEach { p ->
                val dis = getDistance(p.location, this.loc)
                SoundUtils.playSound(p, foundSound, getVolume(dis))
            }
        } else {
            Bukkit.getOnlinePlayers().forEach {
                it.sendTitle(
                    Title(
                        "${"" + ChatColor.GOLD + it.displayName + ChatColor.RESET}がお宝を発見した!",
                        "残り${getNotFounded()}個",
                        2,
                        20 * 4,
                        2
                    )
                )
                Bukkit.getOnlinePlayers().forEach { p ->
                    val dis = getDistance(p.location, this.loc)
                    SoundUtils.playSound(p, foundSound, getVolume(dis))
                }
            }
        }
    }

    fun showEffect() {
        SimpleEffect.spawnParticle(loc, effect)
    }
}

class InformationNoticer(val plugin: Findwithsound) {
    init {
        plugin.server.scheduler.runTaskTimer(plugin, Runnable { tick() }, 1, 1)
    }

    fun getFounded(): Int {
        return plugin.manager.treasures.filter { it.isFounded }.size
    }

    fun tick() {
        if (plugin.manager.isGoing) {
            if (plugin.manager.treasures.size <= getFounded()) {
                Bukkit.getOnlinePlayers().forEach {
                    it.sendActionBar("宝箱全発見!")
                }
            } else {
                Bukkit.getOnlinePlayers().forEach {
                    it.sendActionBar("宝箱${plugin.manager.treasures.size}個中${getFounded()}個発見済み")
                }
            }
        }

    }
}