# 26.1.2 移植记录

**状态（2026-11-04）：移植完成。** 编译通过、GameTest **113/113** 通过、与 1.21.1 的逻辑/数值一致（方法、常量、分支三条审计通过）。

仍开放的只有三项**渲染观感**，已按 26.1.2 的新 API 实现但未实机验证：发光高亮的穿透效果、JEI 分类页排版、实验性手持放大的观感。

## 环境

| 项 | 版本 |
|---|---|
| Minecraft | 26.1.2 |
| NeoForge | 26.1.2.114 |
| Java | 25 |
| AnvilLib | 2.0.0+snapshot.601（含 `anvillib-config`、`anvillib-recipe`） |
| Ageratum | 0.0.1+build.130 |
| AnvilCraft | 1.6.0+snapshot.2749 |
| JEI | 29.53.0.115 |

依赖版本已对齐到部署实例 `TNF-26.1.2` 的实际版本。

## 阶段进度（全部完成）

| 阶段 | 状态 |
|---|---|
| 1 骨架 | ✅ |
| 2 数据模型 | ✅ |
| 3 规则 | ✅ |
| 4 表现（展示实体 / 客户端 / 发光高亮 / JEI） | ✅ |
| 5 事件（含鱼缸 Transfer API 适配层） | ✅ |
| 6 配方（AnvilLib `InWorldRecipe`） | ✅ |
| 7 全部 GameTest | ✅ 113/113 |

## 26.1.2 API 变更清单（迁移时逐条对照）

| 1.21.1 | 26.1.2 |
|---|---|
| `net.minecraft.resources.ResourceLocation` | `net.minecraft.resources.Identifier` |
| `BuiltInRegistries.X.get(id)` 返回 `T` | 返回 `Optional<Reference<T>>`（`.map(Holder.Reference::value).orElse(null)`） |
| `DeferredRegister.createDataComponents(String)` | `createDataComponents(Registries.DATA_COMPONENT_TYPE, String)` |
| `net.minecraft.world.item.ArmorItem` | **已移除**；护甲 = `Item` + `DataComponents.EQUIPPABLE`，`equippable.slot().isArmor()` |
| `Inventory.armor` / `Inventory.items` | `getItemBySlot(EquipmentSlot)` / `getNonEquipmentItems()` |
| `net.minecraft.world.item.component.Unbreakable` | 已移除（待确认替代：`DataComponents.UNBREAKABLE` 的新类型） |
| `BlockTags.getOrCreateTag(...)` | 已移除（改用 tag 查询） |
| `new Tool(rules, speed, damage, correctDrop)` 4 参 | 构造器已变（待确认） |
| `ItemAttributeModifiers(list, showInTooltip)` | `record (List<Entry> modifiers)` 单参，`showInTooltip()`/`withTooltip()` 全部移除 |
| `AttachmentType.Builder.serialize(Codec<T>)` | 只接受 `IAttachmentSerializer<T>` 或 **`MapCodec<T>`**（用 `Codec.INT.fieldOf("value")`） |
| `Item#getDefaultAttributeModifiers()` | 需要 `getDefaultAttributeModifiers(ItemStack)` |
| `BlockTags.getOrCreateTag(tag)` | `Registry.get(tag)` 返回 Optional，需显式 `HolderSet<T>` 类型 |
| `Unbreakable(true)` | `DataComponents.UNBREAKABLE` 类型是 `Unit`，写 `Unit.INSTANCE` |
| `Tool(rules, speed, damage)` 3 参 | 4 参（多 `canDestroyBlocksInCreative`） |
| `MobEffects.DIG_SPEED` / `DAMAGE_RESISTANCE` / `MOVEMENT_SPEED` / `MOVEMENT_SLOWDOWN` | `HASTE` / `RESISTANCE` / `SPEED` / `SLOWNESS` |
| `net.neoforged...level.BlockEvent.BreakEvent` | `net.neoforged...event.level.block.BreakBlockEvent` |
| `ServerLevel#getRecipeManager().getRecipeFor(...)` | `((RecipeManager) level.recipeAccess()).getRecipeFor(type, input, level)` |
| `Recipe#getResultItem(Provider)` | `Recipe#assemble(input)` |
| `ItemContainerContents#nonEmptyItems()` 返回 `ItemStack` | 返回 **`ItemStackTemplate`**，需 `.create()` |
| `RecipeSerializer` 接口 | 26.1.2 变成 **`record(MapCodec, StreamCodec)`**，不能再 `implements`；改用 AnvilLib 自带序列化器 |
| `RecipeHolder(Identifier, recipe)` | `RecipeHolder(ResourceKey<Recipe<?>>, recipe)` |
| `InWorldRecipe` 的 icon | `ItemStack` → **`ItemStackTemplate`** |
| `HasBlock.of(TagKey)` | `of(HolderGetter<Block>, TagKey)`；`Registry` 本身即 HolderGetter |
| `@EventBusSubscriber(bus = Bus.MOD)` | 26.1.2 移除 `bus` 参数，只留 `modid` 与 `value = Dist.CLIENT` |
| `Screen.hasShiftDown()` | 已移除；用 `Minecraft.getInstance().options.keyShift.isDown()` |
| `ItemAttributeModifiers#showInTooltip()` | 已移除（可见性交给 `TooltipDisplay` 组件） |
| `RenderType.create` + `RenderStateShard` | 26.1.2 用 `RenderPipeline.builder(LINES_SNIPPET)` + `RenderSetup.builder(pipeline)` + `RenderType.create(name, setup)`；穿透靠 `withDepthStencilState(Optional.empty())` |
| `ResourceKey#location()` | 改为 **`identifier()`** |
| `LivingDamageEvent.Post#getNewDamage()` | 用 **`getInflictedDamage()`**（Pre 仍是 `getNewDamage()`/`setNewDamage`） |
| `Player#serverLevel()` | 已移除；用 `if (!(player.level() instanceof ServerLevel lvl)) return;` |
| 鱼缸 `getItemHandler()` | 返回 **`ItemStacksResourceHandler`**（Transfer API）：`getResource(i)`/`getAmountAsInt(i)`/`set(i, ItemResource, n)`，用 `IDENTITY` 适配层 `FishTankSlots` 包回 `getStack`/`setStack` |
| 鱼缸 `getFluidHandler()` | 返回 **`FluidStackResourceHandler`**，用 `getStack()`/`insert`/`extract`（包在 `FishTankFluids`）+ `Transaction.openRoot()` |
| `Inventory.armor` / `Inventory.items` | 已移除/私有：`getItemBySlot` / `getNonEquipmentItems()` |

| `Player#serverLevel()` | 用 `level() instanceof ServerLevel` |
| `LivingDamageEvent.Post#getNewDamage()` | `getInflictedDamage()`（`Pre` 仍是 `getNewDamage/setNewDamage`） |
| `MobEffects.DIG_SPEED` / `DAMAGE_RESISTANCE` / `MOVEMENT_SPEED` / `MOVEMENT_SLOWDOWN` | `HASTE` / `RESISTANCE` / `SPEED` / `SLOWNESS` |
| `BlockEvent.BreakEvent` | `event.level.block.BreakBlockEvent` |
| `Recipe#getResultItem(Provider)` | `Recipe#assemble(input)` |
| `ItemContainerContents#nonEmptyItems()` | 返回 `ItemStackTemplate`；要 `ItemStack` 用 `nonEmptyItemCopyStream()` |
| `RecipeSerializer` 接口 | `record(MapCodec, StreamCodec)`，不能再 `implements` |
| `RecipeHolder(Identifier, recipe)` | `(ResourceKey<Recipe<?>>, recipe)` |
| `HasBlock.of(TagKey)` | `of(HolderGetter<Block>, TagKey)`（`Registry` 本身即 HolderGetter） |
| `@EventBusSubscriber(bus = Bus.MOD)` | 移除 `bus` 参数 |
| `Screen.hasShiftDown()` | `Minecraft.getInstance().options.keyShift.isDown()` |
| `ItemAttributeModifiers#showInTooltip()` | 已移除 |
| `entity.moveTo(x,y,z)` | `entity.snapTo(x,y,z,yaw,pitch)` |
| `Player#getInventory().armor` / `.items` | `getItemBySlot(...)` / `getNonEquipmentItems()` |
| `monster.Zombie` | `monster.zombie.Zombie` |
| `VillagerData#getLevel/setLevel` | record：`level()` / `withLevel(n)` |
| `CreativeFluidTankBlockEntity#getFluidHandler()` | 返回 `CreativeFluidHandler`（不再是 `FluidTank`） |
| `Capabilities.FluidHandler.BLOCK` | `Capabilities.Fluid.BLOCK`（`ResourceHandler<FluidResource>`） |
| `IFluidHandlerHolder` | `IFluidResourceHandlerHolder` |
| `RenderType.create(... CompositeState)` | `RenderPipeline.builder(...)` + `RenderSetup.builder(pipeline)` + `RenderType.create(name, setup)` |
| `RenderStateShard.NO_DEPTH_TEST` | `RenderPipeline.Builder#withDepthStencilState(Optional.empty())` |
| `GuiGraphics` | `GuiGraphicsExtractor`（JEI 29 的 `draw` 第三参） |
| `gui.drawString(font, FormattedCharSequence, ...)` | `gui.text(font, String, x, y, ARGB)` |
| `RenderLevelStageEvent.Stage` / `getCamera()` | 无 `Stage`；相机取 `LevelRenderState.cameraRenderState.pos` |
| `entity.load/saveWithoutId(CompoundTag)` | `TagValueInput.create(...)` / `TagValueOutput.createWithContext(...).buildResult()` |

## GameTest 注册的三条硬要求（踩过）

1. `RegisterGameTestsEvent` 是 **mod bus** 事件，类上没有 `@EventBusSubscriber` 就**不会自动触发** —— 必须在模组构造器里 `modBus.addListener(...)`。
2. `registerEnvironment` 的 id **不能重复**（重复会中断整批注册，表现为「只跑了 1 个测试」）。做法：每个测试用自己的 `<name>_env`。
3. 数据组件/附着/配方结果类型都要在模组构造器里注册，否则测试里访问会是 `unbound value`。

## 26.1.2 踩到的坑（已解决，后续移植都要遵守）

| 坑 | 现象 | 解决 |
|---|---|---|
| `ResourceLocation` 改名 | `找不到符号 ResourceLocation` | 26.1.2 用 **`net.minecraft.resources.Identifier`**，`fromNamespaceAndPath` 仍在 |
| `@GameTest` 注解不存在 | gametest 包里有 `FunctionGameTestInstance` 等，但没有注解类 | 26.1.2 改为**数据驱动 + 注册表**；本工程用 `RegisterGameTestsEvent` + 自定义 `MethodTestInstance`（直接持有 lambda），用法与旧注解等价 |
| 测试环境必须显式给 | `TestEnvironmentDefinition` 无内置列表 | 注册 `new AllOf(List.of())` 作为我们的 `thenextforge:default` 环境 |
| `accesstransformer.cfg` / `interface_injections.json` 必须存在 | `createMinecraftArtifacts` 因缺文件失败 | 各放一个空文件即可 |
| Lombok 版本混淆 | 找不到 `org.projectlombok:lombok:8.7.1` | 本模组源码不用 Lombok，直接把插件去掉 |
| 缺 AnvilCraft 本体 | 启动报 `Mod thenextforge requires anvilcraft` | 运行时依赖 `dev.dubhe:anvilcraft-neoforge-26.1.2:1.6.0+snapshot.2750` |
| JEI maven 握手失败 | `maven.blamejared.com` TLS 断连 | 骨架阶段先不引入 JEI；移植客户端时再解决 |


## 已知差异与限制

| 项 | 说明 |
|---|---|
| `recipe/ForgeUpgradeRecipe` | 删除（`RecipeSerializer` 变 record）；改用 AnvilLib 的 `InWorldRecipe` |
| `GlowHighlight` 穿透 | 自建 `RenderPipeline`（`withDepthStencilState(Optional.empty())`），按官方事件注册；观感未实机验证 |
| JEI 分类页 | 用 `GuiGraphicsExtractor` 重写；排版未实机验证 |
| 实验性放大 | 与 1.21.1 一致（第一人称手持 + 砧上展示实体，受配置开关控制）；观感未实机验证 |
| 渲染放大 mixin | `ItemInHandRendererMixin` 注入 `renderItem`，只做 `PoseStack.scale`；**需在 `neoforge.mods.toml` 声明 `[[mixins]]`**，否则不会加载 |

## 历史更正记录（append-only）

- **`netherite_scrap` ARMOR 9.0**：不是行为差异。26.1.2 把物品默认属性写进了 `ATTRIBUTE_MODIFIERS` 组件，
  测试统计「总量」把胸甲自带的 +8 也算进去了。改为统计增量后通过。
- **JEI 运行期依赖「不存在」**：不是外部阻塞。`net.mezzdev.config` 就在 BlameJared maven 上，
  是仓库的 `content { includeGroup }` 白名单漏了它。
- **「高亮不穿透」是既定事实**：不是。26.1.2 的管线可编程，用 `withDepthStencilState(Optional.empty())` 即可恢复。