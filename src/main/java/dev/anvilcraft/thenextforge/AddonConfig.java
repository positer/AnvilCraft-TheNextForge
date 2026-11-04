package dev.anvilcraft.thenextforge;

import dev.anvilcraft.lib.v2.config.BoundedDiscrete;
import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;

@Config(name = TheNextForge.MOD_ID)
public class AddonConfig {
    @Comment("Whether to log the dirt block on common setup")
    public boolean logDirtBlock = false;

    @Comment("实验性：被锻造的物品在手持、物品栏、第三人称与掉落物形态下统一放大（关闭后一律不放大，不影响任何数值效果）")
    public boolean experimentalModelScaling = false;

    @Comment("A magic number")
    @BoundedDiscrete(max = 24, min = 2)
    public int magicNumber = 2;

    @Comment("What you want the introduction message to be for the magic number")
    public String magicNumberIntroduction = "";
}
