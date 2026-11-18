package dev.anvilcraft.thenextforge;

import dev.anvilcraft.lib.v2.config.Comment;
import dev.anvilcraft.lib.v2.config.Config;

/**
 * 本模组的配置。
 *
 * <p>模型放大和品质词缀均为实验性开关，默认关闭；
 * 关闭时一律不放大，但锻造的数值与效果完全不受影响。
 */
@Config(name = TheNextForge.MOD_ID)
public class AddonConfig {
    @Comment("Experimental: scale forged items in hand, inventory, third person and as dropped items. Off means no scaling at all; forging stats and effects are unaffected.")
    public boolean experimentalModelScaling = false;

    @Comment("Experimental: replace the name with a quality affix on each upgrade. Anvils can rename it until the next upgrade. Disabling leaves existing names unchanged.")
    public boolean experimentalQualityAffixes = false;
}
