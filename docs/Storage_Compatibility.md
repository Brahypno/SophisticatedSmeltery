# 精妙存储兼容

目标版本：Sophisticated Storage `1.5.9.2197`（CurseForge 文件 `9296118`），Sophisticated Core `1.5.15.2427`，Minecraft Forge 1.20.1。

背包和存储都是可选依赖，精妙核心仍为必需依赖。开发环境默认加载两者；`-PwithBackpacks=false` / `-PwithStorage=false` 可分别关闭运行时宿主。

## 独立物品及条件注册

背包版保留已有 ID；存储版使用 `storage_` 前缀。两个模组都安装时同时注册两套物品，缺少某个模组时不注册其物品。

| 升级 | 背包版 ID | 存储版 ID |
| --- | --- | --- |
| 熔炼 | `melting_upgrade` | `storage_melting_upgrade` |
| 合金 | `scorched_alloyer_upgrade` | `storage_scorched_alloyer_upgrade` |
| 刷怪 | `spawner_upgrade` | `storage_spawner_upgrade` |
| 高级刷怪 | `advanced_spawner_upgrade` | `storage_advanced_spawner_upgrade` |
| 高级嬗变 | `advanced_transmute_upgrade` | `storage_advanced_transmute_upgrade` |
| 焦黑浇筑 | `seared_casting_upgrade` | `storage_seared_casting_upgrade` |
| 高级焦黑浇筑 | `advanced_seared_casting_upgrade` | `storage_advanced_seared_casting_upgrade` |
| 焦褐浇筑 | `scorched_casting_upgrade` | `storage_scorched_casting_upgrade` |
| 高级焦褐浇筑 | `advanced_scorched_casting_upgrade` | `storage_advanced_scorched_casting_upgrade` |

两套物品分别列入背包和存储的升级标签。熔炼、合金、浇筑、刷怪的运行代码共用，物品身份和外观不共用。

每个宿主有独立的基础合成、升阶、熔炼厂/冶炼炉/嬗变炉绑定配方和配方解锁进度，均有对应宿主的 `forge:mod_loaded` 条件。高级嬗变物品、嬗变配方及解锁还要求 `esotericism_tinker`。背包版只接受背包底板，存储版只接受存储底板，不能用另一种底板合成。

可见控制器绑定配方在 JSON 和网络同步中携带对应升级物品，绑定和升阶保持物品所属宿主。通用解绑配方继续处理两种升级。

## 外观和创造模式 Tab

- 背包版始终使用背包底板和高级边框；存储版始终使用存储底板和高级边框。两者共存时也不会用优先级把存储版画成背包版。
- 绑定控制器后的图标保留所属升级的底板，再叠加控制器正面。
- 精妙匠魂 Tab 只收录本模组升级，包括已安装宿主的对应版本及绑定变体，不导入原版箱桶或染色变体。
- Tab 图标优先为本模组 `melting_upgrade`；没有背包时使用本模组 `storage_melting_upgrade`。不使用原版背包或木桶作为图标。

## 存储功能与链接存储

普通箱子、木桶、潜影盒及有限木桶接入本模组熔炼、合金、浇筑、刷怪升级。存储没有普通储罐升级，炉内容量按库存堆叠倍率增长；面板桶交互、浇筑和管道输入/输出继续可用。

流体处理器建立在原生共享宿主上，各端点访问同一份储罐，升级只由主端 tick。原生通知处理共享数据替换和端点移除；打包和区块卸载使能力缓存失效。没有增加副端自动接管 tick 的机制。见 [Mixin_Boundaries.md](Mixin_Boundaries.md)。

生物捕捉、捕获生物熔化和背部控制器外观仍属于背包适配；未新增移动存储宿主或箱桶物品的随身运行能力。

存储版刷怪笼升级向世界生成生物，不依赖捕捉升级，也不把生物存进箱子。普通版保留玩家距离等原生限制；高级版放宽玩家距离和自然生成条件，开关仍可暂停运行。

## 验证

2026-10-25 完成以下验证：

| 环境 | 游戏测试 | 客户端检查 | 日志 |
| --- | --- | --- | --- |
| 背包 + 存储 + 秘术匠心 | 61 项通过 | 两套外观固定，Tab 使用背包版熔炼升级 | `build/storage-compat/host-split-both-final.log`、`host-split-both.log` |
| 仅存储 | 8 项通过 | 背包版不注册，Tab 使用存储版熔炼升级 | `build/storage-compat/host-split-storage-spawner.log`、`host-split-storage-only.log` |
| 仅背包 | 52 项通过 | 存储版不注册，Tab 使用背包版熔炼升级 | `build/storage-compat/host-split-backpacks-only.log` |

三种环境均构建成功。客户端对照截图为 `build/storage-compat/host-split-icons-both.png`、`host-split-icons-storage-only.png` 和 `host-split-icons-backpacks-only.png`；图中底部单独显示实际 Tab 图标。

`StorageCompatibilityGameTests` 检查物品/配方/解锁条件、各宿主配方实际合成、底板不能混用、绑定配方网络同步、真实存储安装、桶和浇筑、管道能力、链接共享及主端 tick。

`StorageCrossHostGameTests` 检查背包和存储分别作为共享宿主时的流体访问。`-PstorageCompatSmoke` 在客户端检查两套物品的注册、Tab 内容与熔炼图标，并核对普通、绑定、高级模型中的底板和边框贴图所属模组，保存对照截图。

测试目录预填了上游核心的默认物品开关配置，以避免本机首次加载时的 NightConfig 配置写入冲突。仅存储环境中，上游存储的跨模组配方进度仍可能记录缺失背包物品的错误，本模组的配方和进度由宿主条件保护。
