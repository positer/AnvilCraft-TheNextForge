# AnvilCraft: TheNextForge | 铁砧工艺：下一锤

> An AnvilCraft addon. Put your gear on the anvil, then hit it.
> 铁砧工艺的附属模组。把装备放上铁砧，然后砸下去。

## English

Vanilla anvils only repair. **TheNextForge** turns the anvil into the way you grow stronger: place a weapon, a tool, an armor piece or an amulet on an anvil or a grindstone, hold an anvil hammer, and strike. Every strike spends experience and rolls for luck. A success adds a level and a stat. A failure can leave the item alone, pull levels off it, or destroy it.

There is no level cap. The only way up is to keep gambling.

### First steps

1. **Place the item.** Right-click a forgeable item onto an anvil or a grindstone; it lies flat on the surface. Shift + right-click, or an empty hand, takes it back. One item per block.
2. **Strike it.** Hold an **anvil hammer** — the plain one, or its royal, frost, ember or transcendence variant — and right-click the placed item. A dragon rod or an anvil held in the hand is not a hammer and does nothing.
3. **Read the result.** The line above the hotbar shows the level and the quality; chat prints every stat gained, every extra level and the experience paid.
4. **Pick your risk.** Royal and transcendence keep the item alive; frost and ember improve the deal. Carry armor to a grindstone to open the second line.

### Two lines, two sets of rewards

| | Anvil line | Grindstone line |
|---|---|---|
| What it upgrades | Weapons, tools, armor, amulets | Weapons and armor |
| What a level gives | Damage, reach, mining speed, durability, armor, amulet damage | Attack cooldown, sweep damage, movement speed |
| Levels and chance | Tracked separately | Tracked separately |

Tools and amulets can be placed on a grindstone but cannot be upgraded there; the only useful action is taking embedded materials back out.

| Item | Anvil line, per level | Grindstone line, per level |
|---|---|---|
| Tool | +3–8% mining speed | — |
| Weapon | 70%: +5–10% attack damage; 30%: +10% entity reach | 70%: +5–10% attack cooldown recovery; 30%: +10% sweep damage |
| Armor | 50%: +5–10% durability; 50%: +1 armor | +5–10% movement speed, −5% durability |
| Amulet | +1–5% damage | — |

Axes, dragon rods and resonators count as both tool and weapon: they roll from both pools, and their embed set is the union of both sides.

### Chance and cost

| Current level | Success chance |
|---|---|
| 0–3 | 90% |
| 4–6 | 76.5% |
| 7–9 | 65% |
| 10 | 55.3% |
| 11 and above | ×0.85 per level |
| 14 and above | never below 30% |

Every strike costs experience before the roll: 6 + 4 × current level. From level 20 the cost is multiplied by 1.5 every five levels (20–24 ×1.5, 25–29 ×2.25, and so on). Creative mode pays nothing. If you cannot pay, the hammer refuses and tells you how much is missing.

### Failure

A failed roll starts with a coin flip.

- **50%** (30% with the feather amulet): nothing happens and the level stays.
- Otherwise the materials decide. A plain anvil with a plain hammer destroys the item. If either side is transcendence the item is never destroyed: it loses up to 3 levels, or up to 1 level when both sides are transcendence.
- An embedded transcendium nugget turns destruction into a level reset: the item survives, the work is lost.

Reaching level 0 restores the guaranteed successes from royal materials and clears every amulet protection.

### Five material tiers

| Tier | Block | Hammer | Effect |
|---|---|---|---|
| Plain | Anvil, grindstone | Anvil hammer | Nothing extra |
| Royal | Royal anvil, royal grindstone | Royal anvil hammer | One guaranteed success per source; block and hammer each hold one |
| Frost | Frost anvil, frost grindstone | Frost anvil hammer | Halves experience cost; both sides stack to one quarter |
| Ember | Ember anvil, ember grindstone | Ember anvil hammer | 50% chance per side to add one extra level, with no extra experience |
| Transcendence | Transcendence anvil, transcendence grindstone | Transcendence anvil hammer | Never destroys; degrades instead |

Mix them freely: the safer side wins, so any transcendence block or hammer prevents destruction. In automation the falling anvil is the hammer, and a spectral anvil counts as a plain one.

### Amulets around the base

Up to four item frames on the four sides of the base. Each amulet counts once, and every forged level on an amulet adds +0.5% success chance.

| Amulet | Effect |
|---|---|
| Armadillo | Blocks one destruction and protects the next strike; also blocks a transcendence degrade |
| Emerald | With a villager within 3×3×3 of the base: blocks one destruction, raises that villager's trade level, then consumes it |
| Topaz | Blocks one destruction, calls down lightning, raises the item by one level and seals it so it can never be struck again |
| Feather | Lowers the destruction chance from 50% to 30% |
| Cat | After two consecutive successes, the next success adds one extra level |
| Dog | Returns 50% of the experience spent when the item is destroyed, degraded or reset |
| Ruby | While you are on fire, +10% success chance; costs 1 health |
| Sapphire | With lava within 3×1×3: one extra level, and one lava source becomes obsidian |
| Anvil | Every stat gained is 10% larger |
| Silence | Mutes strike sounds and particles; text still appears |
| Abnormal | On success, plays a random tier's effect regardless of level |
| Gem | Ruby + sapphire + topaz at once |
| Nature | Armadillo + feather + cat + dog at once |

Destruction is blocked in this order: armadillo, then emerald, then topaz.

### Embedding materials

Hold a material in the offhand and land a successful strike: the material is consumed and embedded. A failed strike consumes nothing. In automation, put one material in the tank next to the target.

| Item | Base slots |
|---|---|
| Weapon | 3 |
| Tool | 3 |
| Armor | 1 |
| Amulet | 1 |

Paper adds 3 slots (unique, halves maximum durability) and a transcendium nugget adds 1. A full item still levels up; the material is simply not consumed. The same material stacks linearly, and unique materials (glow ink sac, echo shard, transcendium nugget, paper, golden carrot) can appear once per item.

Removal: put the item on a grindstone, hold a sponge gemmule in the offhand and strike (or put the gemmule in a grindstone-based tank). It always returns the most recently embedded material, consumes one gemmule, costs no experience and needs no roll. With nothing embedded, nothing is consumed.

| Material | Tool | Weapon | Armor | Amulet |
|---|---|---|---|---|
| Quartz | — | +20% damage | — | +20% damage |
| Redstone | Haste I | -20% cooldown | Speed I | Haste I and Speed I |
| Lapis Lazuli | +20% experience from kills | +20% experience from kills | +20% experience gained | +20% experience gained |
| Iron Ingot | +1 block interaction range | +1 entity interaction range | — | +1 block interaction range; +1 entity interaction range |
| Gold Nugget | Fortune +1 (-20% maximum durability) | +30% damage (-20% maximum durability) | — | Fortune +1 |
| Gold Ingot | Fortune +3 (-20% maximum durability) | +50% damage (-20% maximum durability) | — | Fortune +3 |
| Diamond | Combine mining tiers with a diamond pickaxe; +30% durability | +30% durability | +30% durability | +2 armor |
| Netherite Scrap | +40% durability | +40% durability | +1 armor; +40% durability | — |
| Netherite Ingot | +100% durability | +100% durability | +3 armor; +100% durability | — |
| Glow Ink Sac | Highlight offhand blocks within 13 blocks | Inflict Glowing for 5s on hit | Night Vision | Night Vision |
| Echo Shard | — | Melee hits deal 50% additional area damage | — | — |
| Fermented Spider Eye | — | Inflict Poison for 5s on hit (+1 level per embedding) | Poison attackers when hit | Inflict Poison for 5s on hit (+1 level per embedding) |
| Slimeball | — | Reduce target movement speed by 50% on hit | — | — |
| Ender Pearl | Send mined drops to inventory (shulker boxes first) | Teleport hit targets randomly within 13 blocks | Teleport attackers randomly within 13 blocks when hit | Send mined drops to inventory (shulker boxes first) |
| Heart of the Sea | Ignore underwater mining penalty | +75% damage in water | Conduit Power in water | Conduit Power in water |
| Nether Star | — | +100% damage | Resistance +3 (max 4) | +100% damage; Resistance +3 (max 4) |
| Royal Steel Ingot | +20% durability | +10% damage; +20% durability | +1 armor; +20% durability | +10% damage; +1 armor; +20% durability |
| Frost Metal Ingot | Convert mined drops to experience (block experience x4; at least 1) | +5% damage per enchantment | — | +1% damage per level |
| Ember Metal Ingot | Auto-smelt mined drops (affected by Fortune) | Fire Aspect | Set attackers on fire when hit | Set attackers on fire when hit |
| Transcendium Nugget | Unbreakable; +1 embedding slot; destruction resets levels to 0 | Unbreakable; +1 embedding slot; destruction resets levels to 0 | Unbreakable; +1 embedding slot; destruction resets levels to 0 | Creative flight; ignore airborne mining penalty; +1 embedding slot; destruction resets levels to 0 |
| Cursed Gold Ingot | — | — | — | +5% damage per curse across equipped gear and amulets |
| Neutronium Ingot | — | +80% damage; +30% cooldown | +2 armor; Resistance +1 (max 4) | +2 armor; Resistance +1 (max 4) |
| Negative Matter Nugget | Inflict Levitation and vulnerability on hit | Inflict Levitation and vulnerability on hit | Inflict Levitation and vulnerability on attackers when hit | Inflict Levitation and vulnerability on attackers when hit |
| Void Matter | — | Convert 30% of damage to void damage | — | Convert 10% of damage to void damage |
| Amber | — | +20% base resentment on hit (max 100%) | — | — |
| Emerald | Mending | Mending | +3 maximum health | +3 maximum health |
| Sapphire | Restore 2 durability every 5s in water | Restore 2 durability every 5s in water | Restore 2 durability every 5s in water | Water Breathing |
| Ruby | Restore 2 durability every 5s while burning | Restore 2 durability every 5s while burning | Restore 2 durability every 5s while burning | Fire Resistance |
| Topaz | Restore 2 durability every 5s in a power grid; restore 10 on lightning strike | Restore 2 durability every 5s in a power grid; restore 10 on lightning strike | Restore 2 durability every 5s in a power grid; restore 10 on lightning strike | — |
| Any Log | Restore 1 durability every 5s | Restore 1 durability every 5s | +1 maximum health; restore 1 durability every 5s | +1 maximum health; restore 1 durability to held items every 5s |
| Feather | — | -75% attack damage; -75% weapon cooldown | — | Slow Falling |
| Golden Carrot | — | — | Gain Saturation for 5s every 20s | Gain Saturation for 5s every 20s |
| Enchanted Golden Apple | — | — | Fire Resistance, Resistance +1, Regeneration +1 | Fire Resistance, Resistance +1, Regeneration +1 |
| Breeze Rod | Create one wind-charge explosion at the hit position | Create one wind-charge explosion at the hit position | — | — |
| Heavy Core | — | +150% knockback; +50% weapon cooldown | Resistance +2 (max 4); -20% movement speed; cannot swim | Resistance +2 (max 4); -20% movement speed; cannot swim |
| Totem of Undying | — | Deal extra magic damage equal to 5% of target maximum health | +5 maximum health; Regeneration +2 | +5 maximum health; Regeneration +2; trigger a Totem of Undying on lethal damage (240s cooldown) |
| Dragon Egg | — | Deal 25% additional magic damage | Deal 10% additional magic damage; immune to magic damage | Deal 10% additional magic damage; immune to magic damage |
| Paper | +3 embedding slots; -50% maximum durability | +3 embedding slots; -50% maximum durability | +3 embedding slots; -50% maximum durability | — |
| Wither Rose | Inflict Wither for 5s on hit | Inflict Wither for 5s on hit | — | — |

### Quality

The higher of the two lines sets the quality: ordinary 0–3, excellent 4–6, rare 7–9, legendary 10 and above. Quality changes the name colour, the enchantment glint and the shimmer over the model: blue for excellent, purple for rare, gold for legendary. Strikes keep their tiered particles and sounds.

With **experimental quality affixes** on (off by default), each upgrade rolls one of five prefixes for the current tier: Decayed, Crude, Worn, Plain, Ordinary; Fine, Spirited, Resilient, Tempered, Excellent; Radiant, Arcane, Starforged, Celestial, Flawless; Legendary, Immortal, Divine, Eternal, Primordial. Affixes are names only; they change no stat. An anvil can rename the item until the next upgrade.

### Automation

Build from the bottom up: a base anvil or grindstone, a fish tank on top of it, and an anvil falling onto the tank. One landing is one strike, and the falling anvil's material is the hammer's.

- The tank must hold experience fluid. It holds 1,000 mB and pulls more from adjacent tanks and pipes; one point of experience costs 35 mB. Any other fluid is refused.
- The tank's outlet is the master switch. Closed: the item keeps upgrading in place, up to level 50. Open: the item is ejected once it reaches the redstone threshold, which is 2 × signal + 1 (no signal ejects at level 1).
- One tank slot holds the target and a second holds an embedding material. A sponge gemmule in the tank removes the most recent embed instead.
- Amulets in the frames still apply, and the silence amulet mutes the machine.

### Reading the results

Every strike reports on the action bar, and chat keeps the detail. Item tooltips aggregate each line and each embed into a single line, for example "Anvil Lv.4 attack +32.5%". With JEI installed, one category lists every embed material against the workstations it fits. The in-game guide has chapters for manual upgrading, automation, and embedding.

### Experimental toggles

Both are off by default and change presentation only.

- **Model scaling**: reach and sweep levels make the model 5% larger per level, in hand, in the inventory, in third person and as a dropped item.
- **Quality affixes**: the five-per-tier prefixes above.

---

## 中文

原版铁砧只会修东西。**《下一锤》**把铁砧变成唯一变强的途径：把武器、工具、装甲或护符放到铁砧或砂轮上，手持铁砧锤砸下去。每一锤先扣经验、再掷运气——成功就升一级并随机涨一项属性，失败可能什么都不发生，可能掉级，也可能连装备一起没。

等级没有上限，想更强，只能继续赌。

### 三步开始

1. **放下装备。** 手持可锻造的物品右键铁砧或砂轮，它会平放在台面上；Shift + 右键或空手右键取回。一个方块只放一件。
2. **砸下去。** 主手持**铁砧锤**（普通、皇家、浮霜、余烬或超限），右键台面上的物品。龙杖和铁砧本体都不是锤，拿在手里右键不会触发。
3. **看结果。** 快捷栏上方的文字显示等级与品质，聊天记录列出每一项提升、每一次额外升级和扣掉的经验。
4. **选风险。** 想稳，用皇家或超限；想多赚，用浮霜或余烬；想练第二条线，把装甲搬到砂轮上。

### 两条线，两套回报

| | 铁砧线 | 砂轮线 |
|---|---|---|
| 能升级什么 | 武器、工具、装甲、护符 | 武器、装甲 |
| 每级给什么 | 攻击力、触及、挖掘速度、耐久、护甲、护符伤害 | 攻击冷却、横扫、移速 |
| 等级与成功率 | 各自独立计算 | 各自独立计算 |

工具与护符可以放上砂轮，但不能在砂轮上升级；放在上面的唯一用途是取出嵌入材料。

| 对象 | 铁砧线每级 | 砂轮线每级 |
|---|---|---|
| 工具 | 挖掘速度 +3%~8% | — |
| 武器 | 70%：攻击力 +5%~10%；30%：实体触及 +10% | 70%：攻击冷却回复 +5%~10%；30%：横扫伤害 +10% |
| 装甲 | 50%：耐久 +5%~10%；50%：护甲 +1 | 移速 +5%~10%，耐久 -5% |
| 护符 | 伤害 +1%~5% | — |

斧头、龙杖、共振器既是工具又是武器：两条材料池都能用，效果取并集。

### 成功率与经验

| 当前等级 | 成功率 |
|---|---|
| 0–3 | 90% |
| 4–6 | 76.5% |
| 7–9 | 65% |
| 10 | 55.3% |
| 11 级起 | 每级再乘 0.85 |
| 14 级起 | 不低于 30% |

每一锤都在掷骰前扣经验：6 + 4 × 当前等级。20 级起每 5 级再乘 1.5（20–24 级 ×1.5，25–29 级 ×2.25，依此类推）。创造模式不扣经验；经验不够时锤子不会挥下，并提示还差多少。

### 失败

失败先掷一枚硬币：

- **50%**（带羽毛护符时 30%）：什么都不发生，等级不变。
- 其余情况按材质结算。普通铁砧配普通锤会直接损毁物品；任一侧是超限材质就不会损毁，改为掉级——单超限最多 3 级，双超限最多 1 级。
- 内嵌超限合金粒的装备被判定损毁时改为**等级归零**：装备还在，之前的积累归零。

降到 0 级会刷新皇家材质的必定成功次数，并清掉所有护符保护。

### 五档材质

| 档位 | 台面 | 锤 | 效果 |
|---|---|---|---|
| 普通 | 铁砧、砂轮 | 铁砧锤 | 没有额外效果 |
| 皇家 | 皇家铁砧、皇家砂轮 | 皇家铁砧锤 | 每个来源各一次必定成功；台面与锤各存一次 |
| 浮霜 | 浮霜铁砧、浮霜砂轮 | 浮霜铁砧锤 | 经验消耗减半；两侧叠乘为四分之一 |
| 余烬 | 余烬铁砧、余烬砂轮 | 余烬铁砧锤 | 每侧 50% 概率额外 +1 级，不额外扣经验 |
| 超限 | 超限铁砧、超限砂轮 | 超限铁砧锤 | 永不损毁，失败改为掉级 |

可以混搭：后果取更宽松的一方，所以只要台面或锤有一侧是超限，装备就不会被摧毁。自动化里落下的铁砧就是锤，幻灵铁砧按普通铁砧计算。

### 砧边的护符

台面四个侧面最多挂四个物品展示框，同名护符只生效一次；护符自身每级强化给 +0.5% 成功率。

| 护符 | 效果 |
|---|---|
| 犰狳 | 抵挡一次损毁，并保护下一次锤击；超限材质时改为抵挡降级 |
| 绿宝石 | 台面 3×3×3 内有村民时：抵挡一次损毁，把该村民交易等级 +1 后消耗 |
| 黄玉 | 抵挡一次损毁，召来雷击，装备 +1 级并封印，之后不能再锤 |
| 羽毛 | 损毁概率从 50% 降到 30% |
| 猫 | 连续成功两次后，下一次成功额外 +1 级 |
| 狗 | 装备损毁、降级或归零时，返还 50% 已消耗经验 |
| 红宝石 | 玩家着火时成功率固定 +10%，并扣 1 点生命 |
| 蓝宝石 | 3×1×3 内有岩浆源时额外 +1 级，并把一块岩浆源变成黑曜石 |
| 铁砧 | 每次升级的数值提升额外 +10% |
| 寂静 | 屏蔽锤击的声音与粒子，只保留文字 |
| 异常 | 成功时随机播放一档等级的升级特效 |
| 宝石 | 红宝石 + 蓝宝石 + 黄玉，同时生效 |
| 自然 | 犰狳 + 羽毛 + 猫 + 狗，同时生效 |

损毁抵挡顺序：犰狳 → 绿宝石 → 黄玉。

### 嵌入材料

副手拿着材料锤出一次成功：材料被消耗并嵌入。失败不消耗材料。自动化里把一份材料放进鱼缸。

| 对象 | 基础槽位 |
|---|---|
| 武器 | 3 |
| 工具 | 3 |
| 装甲 | 1 |
| 护符 | 1 |

纸 +3 槽（唯一嵌入，最大耐久 -50%），超限合金粒 +1 槽。槽位满了仍能继续升级，只是材料不嵌入、也不消耗。同种材料线性叠加；唯一材料（发光墨囊、回响碎片、超限合金粒、纸、金胡萝卜）每件装备只能有一枚。

取出：把装备放上砂轮，副手拿海绵芽锤击（或把海绵芽放进砂轮上方的鱼缸）。必定取回**最近嵌入**的一枚材料，消耗 1 个海绵芽，不判成败、不扣经验、不掉级；没有可取的嵌入时不消耗海绵芽。

| 材料 | 工具 | 武器 | 装甲 | 护符 |
|---|---|---|---|---|
| 石英 | — | 伤害 +20% | — | 伤害 +20% |
| 红石 | 急迫 I | 冷却 -20% | 速度 I | 急迫 I 与速度 I |
| 青金石 | 击杀经验 +20% | 击杀经验 +20% | 获得经验 +20% | 获得经验 +20% |
| 铁锭 | 方块触及 +1 格 | 实体触及 +1 格 | — | 方块触及 +1 格，实体触及 +1 格 |
| 金粒 | 时运 +1（最大耐久 -20%） | 伤害 +30%（最大耐久 -20%） | — | 时运 +1 |
| 金锭 | 时运 +3（最大耐久 -20%） | 伤害 +50%（最大耐久 -20%） | — | 时运 +3 |
| 钻石 | 挖掘等级与钻石镐取并集，耐久 +30% | 耐久 +30% | 耐久 +30% | 护甲 +2 |
| 下界合金碎片 | 耐久 +40% | 耐久 +40% | 护甲 +1，耐久 +40% | — |
| 下界合金锭 | 耐久 +100% | 耐久 +100% | 护甲 +3，耐久 +100% | — |
| 发光墨囊 | 副手方块高亮 13 格 | 命中发光 5s | 夜视 | 夜视 |
| 回响碎片 | — | 近战命中额外 50% 范围伤害 | — | — |
| 发酵蜘蛛眼 | — | 命中附加中毒 5s（每嵌入 +1 级） | 被攻击时攻击者中毒 | 命中附加中毒 5s（每嵌入 +1 级） |
| 黏液球 | — | 命中降低 50% 移速 | — | — |
| 末影珍珠 | 挖掘掉落直接进入背包（优先潜影盒） | 命中后目标随机 13 格传送 | 被攻击后攻击者随机 13 格传送 | 挖掘掉落直接进入背包（优先潜影盒） |
| 海洋之心 | 水下无视挖掘惩罚 | 水中伤害 +75% | 水中获得海洋庇佑 | 水中获得海洋庇佑 |
| 下界之星 | — | 伤害 +100% | 抗性提升 +3（最高 4） | 伤害 +100%，抗性提升 +3（最高 4） |
| 皇家钢锭 | 耐久 +20% | 伤害 +10%，耐久 +20% | 护甲 +1，耐久 +20% | 伤害 +10%，护甲 +1，耐久 +20% |
| 浮霜金属锭 | 挖掘掉落转化为经验（方块经验 ×4，至少 1 点） | 每附魔 +5% 伤害 | — | 每级 +1% 伤害 |
| 余烬金属锭 | 挖掘自动熔炼（受时运影响） | 火焰附加 | 被攻击后攻击者燃烧 | 被攻击后攻击者燃烧 |
| 超限合金粒 | 不毁，嵌入槽 +1，损毁降为等级 0 | 不毁，嵌入槽 +1，损毁降为等级 0 | 不毁，嵌入槽 +1，损毁降为等级 0 | 创造飞行，无视飞行挖掘惩罚，嵌入槽 +1，损毁降为等级 0 |
| 诅咒金锭 | — | — | — | 全身装备/护符每诅咒 +5% 伤害 |
| 中子锭 | — | 伤害 +80%，冷却 +30% | 护甲 +2，抗性提升 +1（最高 4） | 护甲 +2，抗性提升 +1（最高 4） |
| 负物质粒 | 命中漂浮且易伤 | 命中漂浮且易伤 | 被攻击后攻击者漂浮且易伤 | 被攻击后攻击者漂浮且易伤 |
| 虚空物质 | — | 30% 伤害转虚空 | — | 10% 伤害转虚空 |
| 琥珀 | — | 命中后基础怨恨值 +20%（上限 100%） | — | — |
| 绿宝石 | 经验修补 | 经验修补 | 最大生命值 +3 | 最大生命值 +3 |
| 蓝宝石 | 水中每 5s 恢复 2 点耐久 | 水中每 5s 恢复 2 点耐久 | 水中每 5s 恢复 2 点耐久 | 水下呼吸 |
| 红宝石 | 燃烧时每 5s 恢复 2 点耐久 | 燃烧时每 5s 恢复 2 点耐久 | 燃烧时每 5s 恢复 2 点耐久 | 抗火 |
| 黄玉 | 电网内每 5s 恢复 2 点耐久，遭雷劈恢复 10 点 | 电网内每 5s 恢复 2 点耐久，遭雷劈恢复 10 点 | 电网内每 5s 恢复 2 点耐久，遭雷劈恢复 10 点 | — |
| 任意原木 | 每 5s 恢复 1 点耐久 | 每 5s 恢复 1 点耐久 | 最大生命值 +1，每 5s 恢复 1 点耐久 | 最大生命值 +1，每 5s 恢复手持物品 1 点耐久 |
| 羽毛 | — | 攻击力 -75%，武器冷却 -75% | — | 缓降 |
| 金胡萝卜 | — | — | 每 20s 获得 5s 饱和 | 每 20s 获得 5s 饱和 |
| 附魔金苹果 | — | — | 抗火、抗性提升 +1、生命恢复 +1 | 抗火、抗性提升 +1、生命恢复 +1 |
| 旋风棒 | 命中后原地生成一次风弹爆炸 | 命中后原地生成一次风弹爆炸 | — | — |
| 沉重核心 | — | 击退 +150%，武器冷却 +50% | 抗性 +2（最高 4），移速 -20%，水中无法游泳 | 抗性 +2（最高 4），移速 -20%，水中无法游泳 |
| 不死图腾 | — | 额外造成目标最大生命 5% 的魔法伤害 | 最大生命 +5，生命恢复 +2 | 最大生命 +5，生命恢复 +2；致命伤害时触发不死图腾（冷却 240s） |
| 龙蛋 | — | 额外附加 25% 魔法伤害 | 额外附加 10% 魔法伤害，且免疫魔法伤害 | 额外附加 10% 魔法伤害，且免疫魔法伤害 |
| 纸 | 可嵌入槽位 +3，最大耐久 -50% | 可嵌入槽位 +3，最大耐久 -50% | 可嵌入槽位 +3，最大耐久 -50% | — |
| 凋零玫瑰 | 命中附加凋零 5s | 命中附加凋零 5s | — | — |

### 品质

两条线里更高的等级决定品质：普通 0–3、优秀 4–6、稀有 7–9、传说 10 起。品质改变名称颜色、附魔光效和模型表面的流光：优秀蓝、稀有紫、传说金。每次升级的粒子与音效按等级分层。

开启**实验性品质词缀**（默认关闭）后，每次升级按当前品质从五个词缀里抽一个：

- 普通：朽坏的、粗制的、陈旧的、朴素的、平凡的
- 优秀：精良的、锐意的、坚韧的、淬炼的、卓越的
- 稀有：辉耀的、秘藏的、星铸的、苍穹的、无瑕的
- 传说：传奇的、不朽的、神铸的、永恒的、创世的

词缀只是名字，不改变任何数值；铁砧可以改名，直到下一次升级。

### 自动化

从下往上：基底铁砧或砂轮、鱼缸、落下的铁砧。每落一次砧就是一次锤击，落砧的材质就是锤的材质。

- 鱼缸里必须是经验流体。缸容量 1000 mB，不足时从相邻的储罐与管道抽取；1 点经验折合 35 mB。其它流体会被拒绝。
- 鱼缸的开口是总开关。开口关闭：原地持续升级，最高 50 级；开口打开：达到红石阈值就出货，阈值为 2 × 信号强度 + 1（无信号 1 级即出）。
- 鱼缸一个槽放锻造对象，第二个槽放嵌入材料；放海绵芽则改为取出最近嵌入的材料。
- 台面周围展示框里的护符照常生效；寂静护符同样会让机器静音。

### 看懂结果

每次锤击在快捷栏上方给摘要，聊天里留明细。物品 tooltip 会把每条线与每种嵌入聚合成一行，例如「铁砧 Lv.4 攻击力 +32.5%」。装了 JEI 后有一个分类翻完所有嵌入材料与可用的工作方块；游戏内指导书分「手动升级」「自动升级」「嵌入材料与取出」三章。

### 实验性开关

两个开关默认关闭，只影响表现，不影响数值与判定。

- **模型放大**：触及与横扫每升一级，模型放大 5%，覆盖手持、物品栏、第三人称与掉落物。
- **品质词缀**：上文四档共二十个词缀。
