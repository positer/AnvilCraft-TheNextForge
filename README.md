# 铁砧工艺：下一锤 | AnvilCraft: TheNextForge

> [铁砧工艺（AnvilCraft）](https://github.com/Anvil-Dev/AnvilCraft) 的附属模组，同时维护 **NeoForge 1.21.1** 与 **NeoForge 26.1.2** 两代。

## 两代状态

| 代 | 目录 | GameTest |
|---|---|---|
| **1.21.1** | 仓库根 | **110/110** |
| **26.1.2** | [`mc-26.1.2/`](mc-26.1.2/) | **114/114** |

两代共用同一份 git 历史，各自独立构建、独立测试。

## 构建与测试

### 1.21.1（仓库根）

需要 **JDK 21**（Gradle 通过 toolchain 解析）。

```powershell
.\gradlew.bat build                # 产出 build/libs/*.jar
.\gradlew.bat runGameTestServer    # 跑全部 GameTest
.\gradlew.bat runClient            # 开发客户端
```

### 26.1.2（`mc-26.1.2/`）

需要 **JDK 25**。

```powershell
cd mc-26.1.2
$env:JAVA_HOME="<JDK 25 安装目录>"
.\gradlew.bat build
.\gradlew.bat runGameTestServer
.\gradlew.bat runClient
```

两代都是自包含的 Gradle 工程：`gradle/wrapper` 已随仓库提供，**不需要预装 Gradle**，首次构建由 wrapper 自行下载对应发行版。

## 依赖版本（已核实）

| 代 | Minecraft | NeoForge | Java | 关键前置 |
|---|---|---|---|---|
| 1.21.1 | 1.21.1 | 21.1.241 | 21 | AnvilCraft 1.6.0+snapshot.2731、AnvilLib 2.0.0、Ageratum、JEI 19.x（compileOnly） |
| 26.1.2 | 26.1.2 | 26.1.2.114 | 25 | AnvilCraft 1.6.0+snapshot.2749、AnvilLib 2.0.0+snapshot.601、Ageratum 0.0.1+build.130、JEI 29.53.0.115 |

完整坐标见各代 `gradle/libs.versions.toml`。

## 目录结构

```
├── src/                 1.21.1 源码、资源与测试
├── mc-26.1.2/           26.1.2 独立工程（同结构）
├── gradle/              构建脚本与 wrapper
├── build.gradle
└── README.md
```

## 许可

MIT。
