# AnvilCraft: TheNextForge — Minecraft 26.1.2 移植工程

本目录是 `AnvilCraft-TheNextForge`（1.21.1）的 **Minecraft 26.1.2 / NeoForge 26.1.2.114** 移植工程，
独立构建、独立测试，与 1.21.1 分支保持行为一致。

## 目标环境

| 项 | 版本 |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.114 |
| Java | 25 |
| AnvilCraft | 1.6.0+snapshot.2749 |
| AnvilLib | 2.0.0+snapshot.601（含 `anvillib-config`、`anvillib-recipe`） |
| Ageratum | 0.0.1+build.130 |
| JEI | 29.43.0.106（compileOnly + runtimeOnly） |

## 快速开始

```powershell
$env:JAVA_HOME="C:\Users\12252\.gradle\jdks\eclipse_adoptium-25-amd64-windows.2"
cd AnvilCraft-TheNextForge/mc-26.1.2
.\gradlew.bat runGameTestServer   # 跑全部 GameTest
.\gradlew.bat build               # 产出 build/libs/thenextforge-neoforge-0.1.0.jar
```

## 历史状态（2026-11-04）

| 项 | 状态 |
|---|---|
| 编译 | ✅ |
| GameTest | ✅ **113/113** 通过 |
| 1.21.1 测试覆盖 | 109 条全部迁入并通过 |
| 一致性审计 | ✅ 方法/常量/分支三条比对通过 |
| 制品 | `build/libs/thenextforge-neoforge-0.1.0.jar` |

```
========= 113 GAME TESTS COMPLETE IN 4.460 s ======================
All 113 required tests passed :)
BUILD SUCCESSFUL
```

## 工程结构

```
src/main/java/dev/anvilcraft/thenextforge/
├── TheNextForge.java          模组入口（注册数据组件/附着/配方结果/配置/GameTest 监听）
├── AddonConfig.java           模组配置（含实验性放大开关）
├── forging/                   锻造核心：规则、材料、数据组件、展示实体、结算服务
│   ├── ModelScaling.java      ← 26.1.2 新增：放大倍率判定（配置 + 数据驱动）
│   └── EmbedMaterials.java    39 种嵌入材料表（与 1.21.1 逐字一致）
├── event/                     事件层
│   ├── FishTankSlots.java     ← 26.1.2 新增：Transfer API 物品适配层
│   ├── FishTankFluids.java    ← 26.1.2 新增：Transfer API 流体适配层
│   └── AutomationHandler.java 自动化锤击（含邻接流体供能）
├── recipe/                    世界内配方（用 AnvilLib 的 InWorldRecipe）
├── client/                    客户端：tooltip、发光高亮、手持变换、JEI
│   └── ForgeJeiData.java      ← 26.1.2 新增：JEI 文本数据层（零渲染依赖）
├── data/                      数据生成（配置页语言）
└── gametest/                  GameTest（113 条）

src/main/resources/
├── META-INF/neoforge.mods.toml
├── thenextforge.mixins.json   渲染放大 mixin（client: ItemInHandRendererMixin）
└── assets/anvilcraft/ageratum/ 指南书章节（中英各 4 篇 + 结构 snbt）
```

## 与原版的差异（全部有意）

| 项 | 说明 |
|---|---|
| `recipe/ForgeUpgradeRecipe` | 删除：26.1.2 的 `RecipeSerializer` 是 record，改用 AnvilLib 的 `InWorldRecipe` |
| `RenderPipeline` | 自建「无深度测试」线段管线（穿透高亮），在 `RegisterRenderPipelinesEvent` 注册 |
| JEI category | 用 `GuiGraphicsExtractor` 重写（26.1.2 没有 `GuiGraphics`） |

## 已知限制

| 项 | 状态 |
|---|---|
| 发光高亮穿透 | 已按官方管线注册实现，**渲染观感未实机验证** |
| JEI 页面排版 | 已实现，**观感未实机验证** |
| 实验性放大 | 已实现（第一人称 + 砧上展示实体），**观感未实机验证** |

## 文档

- [2026-11-08 两代对齐与性能报告](../docs/design/07-generation-alignment-performance.md) —— 当前规则修正、合同测试与性能验证边界；实机清单仍独立验收。

- [PORTING.md](PORTING.md) —— API 变更表、踩坑记录、已知差异
- [ACCEPTANCE.md](ACCEPTANCE.md) —— **实机验收清单**（需要进游戏确认的部分）
