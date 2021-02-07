package net.kunmc.lab.findwithsound.sound

import org.bukkit.Instrument
import org.bukkit.Note
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.entity.Player

class SoundUtils {
    companion object {
        fun playSound(p: Player, sound: Sound, category: SoundCategory, vol: Float = 0.0f, pitch: Float = 0.0f) {
            p.playSound(p.location, sound, category, vol, pitch)
        }

        fun playSound(p: Player, sound: Sound, vol: Float = 0.0f, pitch: Float = 0.0f) {
            playSound(p, sound, SoundCategory.MASTER, vol, pitch)
        }

        fun playNote(p: Player, ins: Instrument, note: Note) {
            p.playNote(p.location, ins, note)
        }
    }
}