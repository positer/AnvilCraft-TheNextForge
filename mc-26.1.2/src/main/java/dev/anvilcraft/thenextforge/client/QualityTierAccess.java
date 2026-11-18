package dev.anvilcraft.thenextforge.client;

/** 帧内渲染状态只携带品质整数，不保留源 ItemStack。 */
public interface QualityTierAccess {
    int tnf$getQualityTier();
    void tnf$setQualityTier(int tier);
    record Identity(int tier) { }
}
