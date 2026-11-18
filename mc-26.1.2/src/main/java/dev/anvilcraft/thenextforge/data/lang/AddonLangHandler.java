package dev.anvilcraft.thenextforge.data.lang;

import dev.anvilcraft.lib.v2.registrum.providers.RegistrumLangProvider;

/**
 * 语言文件初始化。
 *
 * <p>**不调用 {@code ConfigData.readConfigClass}**：它生成的标题写死了模板名，
 * 而 NeoForge 的 {@code LanguageProvider.add} 对重复 key 直接抛异常，无法事后覆盖。
 */
public class AddonLangHandler {
    private static final String CONFIG = "thenextforge.configuration.";
    private static final String SECTION = CONFIG + "section.thenextforge.common.toml";

    /**
     * 语言文件初始化。
     *
     * @param provider 提供器
     */
    public static void init(RegistrumLangProvider provider) {
        AdventureLang.add(provider);
        provider.add(CONFIG + "title", "AnvilCraft: TheNextForge Configuration");
        provider.add(SECTION, "AnvilCraft: TheNextForge Common Configuration");
        provider.add(SECTION + ".title", "AnvilCraft: TheNextForge Common Configuration");
        provider.add(CONFIG + "experimental_model_scaling", "Experimental Model Scaling");
        provider.add(CONFIG + "experimental_model_scaling.tooltip",
            "Experimental: scale forged items in hand, inventory, third person and as dropped items. "
                + "Off means no scaling at all; forging stats and effects are unaffected.");

        provider.add(CONFIG + "experimental_quality_affixes", "Experimental Quality Affixes");
        provider.add(CONFIG + "experimental_quality_affixes.tooltip", "Replace the name on each upgrade. Anvils can rename it until the next upgrade. Disabling leaves existing names unchanged.");
        provider.add("thenextforge.affix.0.0", "Decayed ");
        provider.add("thenextforge.affix.0.1", "Crude ");
        provider.add("thenextforge.affix.0.2", "Worn ");
        provider.add("thenextforge.affix.0.3", "Plain ");
        provider.add("thenextforge.affix.0.4", "Ordinary ");
        provider.add("thenextforge.affix.1.0", "Fine ");
        provider.add("thenextforge.affix.1.1", "Spirited ");
        provider.add("thenextforge.affix.1.2", "Resilient ");
        provider.add("thenextforge.affix.1.3", "Tempered ");
        provider.add("thenextforge.affix.1.4", "Excellent ");
        provider.add("thenextforge.affix.2.0", "Radiant ");
        provider.add("thenextforge.affix.2.1", "Arcane ");
        provider.add("thenextforge.affix.2.2", "Starforged ");
        provider.add("thenextforge.affix.2.3", "Celestial ");
        provider.add("thenextforge.affix.2.4", "Flawless ");
        provider.add("thenextforge.affix.3.0", "Legendary ");
        provider.add("thenextforge.affix.3.1", "Immortal ");
        provider.add("thenextforge.affix.3.2", "Divine ");
        provider.add("thenextforge.affix.3.3", "Eternal ");
        provider.add("thenextforge.affix.3.4", "Primordial ");
    }
}
