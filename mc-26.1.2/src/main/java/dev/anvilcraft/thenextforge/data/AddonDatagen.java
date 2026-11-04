package dev.anvilcraft.thenextforge.data;

import dev.anvilcraft.thenextforge.TheNextForge;
import dev.anvilcraft.thenextforge.data.lang.AddonLangHandler;
import dev.anvilcraft.lib.v2.registrum.providers.ProviderType;

import static dev.anvilcraft.thenextforge.TheNextForge.REGISTRUM;

public class AddonDatagen {
    /**
     * 初始化生成器
     */
    public static void init() {
        REGISTRUM.addDataGenerator(ProviderType.LANG, AddonLangHandler::init);
    }
}
