# 实机验收清单（26.1.2）

> 代码层面已完成：编译通过、GameTest 113/113、与 1.21.1 的方法/常量/分支三项审计一致。
> 本清单只覆盖**必须进游戏才能确认**的部分。

## 准备

实例：`PCL/.minecraft/versions/TNF-26.1.2`，模组已就位：

| 文件 | 说明 |
|---|---|
| `thenextforge-neoforge-26.1.2-0.1.0.jar` | 本模组（193 KB） |
| `anvilcraft-neoforge-26.1.2-1.6.0+snapshot.2749.jar` | 前置 |
| `anvillib-neoforge-26.1.2-2.0.0+snapshot.601.jar` | 前置 |
| `ageratum-neoforge-26.1.2-0.0.1+build.130.jar` | 指导书前置 |
| `jei-26.1.2-neoforge-29.53.0.115.jar` | JEI |

## 验收项

### A. 能否启动（最高优先级）

- [ ] 实例正常进入主菜单，没有崩溃
- [ ] 日志里出现 `[TNF] loaded on 26.1.2`

**若崩溃**：把 `crash-reports/` 里最新的日志给我。最可能的原因是自建渲染管线被拒——
回退方式是把 `client/GlowHighlight.java` 里 `var renderType = glowLines();` 改成
`net.minecraft.client.renderer.rendertype.RenderTypes.lines();`（一行）。

### B. JEI「嵌入材料」统合页

- [ ] JEI 里能找到一个叫「嵌入材料」的分类
- [ ] 每一页：左上角是材料，底部一排是它允许嵌入的对象
- [ ] 文字区在上方，**不压到底部槽位**
- [ ] 点底部的对象槽，能跳到该装备的用途页（互相索引）

预期材料条目数：**39**（`EmbedMaterials` 表里的非分组条目）。

### C. 发光高亮

- [ ] 主手拿「带 `minecraft:glow_ink_sac` 嵌入的工具」，副手拿任意**方块**
- [ ] 13 格内同类型方块出现**黄色线框**
- [ ] 线框**能穿过**其它方块看到（这是自建管线的目的）
- [ ] 副手换成非方块物品 → 线框消失

### D. 实验性放大

- [ ] 模组配置页能看到 `experimentalModelScaling`（默认关闭，中文说明正常显示）
- [ ] 关闭时：锻造过的物品在手上/物品栏里大小不变
- [ ] 打开后：锻造过的物品**第一人称手持**变大（每级 +10%，砧+砂轮加算）
- [ ] 打开后：砧上展示的实体物品也变大，且放置后升级/降级**当场**改变大小

> 放大由 mixin 统一处理：注入 `ItemInHandRenderer#renderItem`，只做 `PoseStack.scale`，
> 因此**手持（第一/第三人称）、物品栏图标、掉落物、展示框**都会放大。
>
> 26.1.2 没有 `ItemRenderer#render`（物品渲染拆成 render state + submit，state 不含 ItemStack），
> `ItemInHandRenderer#renderItem` 是唯一同时拿得到「物品 + 姿态」的统一入口。

### E. 基础玩法

- [ ] 铁砧砸在鱼缸上，能把鱼缸里的物品锻造升级
- [ ] 嵌入材料消耗副手材料，tooltip 显示等级与嵌入
- [ ] 指导书（Ageratum）里有「铁砧工艺：下一锤」大章节（手动升级 / 自动升级 / 嵌入材料）

## 反馈什么

| 情况 | 需要提供 |
|---|---|
| 崩溃 | `crash-reports/` 最新文件 |
| 显示不对 | 截图 + 描述（「线框不穿透」/「文字压到槽位」等） |
| 全部正常 | 一句话即可，我把移植标记为完成 |