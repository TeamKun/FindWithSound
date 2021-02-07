package net.kunmc.lab.findwithsound.effect

import org.bukkit.Location
import org.bukkit.Particle


class SimpleEffect{
    companion object{
        fun spawnParticle(loc: Location,pat: Particle,count:Int = 1){
            loc.world.spawnParticle(pat,loc,count)
        }
    }
}