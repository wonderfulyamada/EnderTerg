package dev.endertag.mod;

import net.minecraftforge.common.config.Config;

@Config(modid = Tags.MOD_ID)
public final class EnderTagConfig {

    @Config.Comment("Entity registry IDs that cannot be bound by an Ender Terg (for example, minecraft:horse).")
    public static String[] entityBlacklist = new String[0];

    private EnderTagConfig() {
    }
}
