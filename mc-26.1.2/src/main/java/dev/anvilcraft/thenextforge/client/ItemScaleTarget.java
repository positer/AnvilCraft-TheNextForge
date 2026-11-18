package dev.anvilcraft.thenextforge.client;

/** 渲染状态与层共享的内部缩放接口；不保留 ItemStack 或世界引用。 */
public interface ItemScaleTarget {
    record Identity(float scale) { }
    void tnf$setScale(float scale);
}
