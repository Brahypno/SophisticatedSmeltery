# 刷怪笼升级

## 玩法

- 普通版 `spawner_upgrade`：8 个铁栏杆围住精妙背包空白升级（Upgrade Base）。
- 高级版 `advanced_spawner_upgrade`：沿用标准升阶配方，上排中央钻石、中排金锭/普通版/金锭、下排三个红石；完整复制普通版的刷怪笼和设置 NBT。
- 每个升级只有一个刷怪笼槽位，只接受一个 `minecraft:spawner`。使用精妙背包原生模块开关暂停/恢复，不增加第二个开关。
- 普通版遵循所放刷怪笼的原生条件。高级版忽略玩家距离和自然生成规则（包括光照、群系、动物地面等），仍保留碰撞空间、附近同类数量、生成间隔、和平难度限制及神化红石控制。
- 支持携带和放置背包。关闭后计时暂停；取出刷怪笼、移动背包和保存恢复保留当前计时与刷怪笼参数。升级可带着内部刷怪笼取下。
- 放置背包中的高级升级开启时，也为六面紧邻的世界刷怪笼提供同样的条件豁免，内部刷怪笼槽为空仍生效，不要求安装捕捉升级。普通升级、斜角和隔一格均不提供豁免。关闭、拆下高级升级或移走背包后，下次刷怪笼 Tick 立即恢复其自身条件；不停止本来就满足条件的世界刷怪笼。

## 生物捕捉联动

内部刷怪笼优先把新生生物存进同背包的捕捉升级。世界中的刷怪笼则尝试六个紧邻方块上的背包；这些背包只需安装捕捉升级，不需要刷怪笼升级。没有合适的捕捉升级、背包空间不足或生物不满足捕捉条件时，继续原生世界生成。

遵守捕捉升级的普通/高级生物限制、黑名单、配置的槽位成本上限及连续矩形空间；使用原生费用、占位计算及保存 API。自动捕捉没有操作者，不接受已归属玩家的生物；玩家、Boss、骑乘关系和禁止的库存生物沿用捕捉限制。自然刷新、刷怪蛋及读档实体不会因邻近背包而被截获。

生成出口使用 Forge 事件：`MobSpawnEvent.FinalizeSpawn` 仅记录来源；`EntityJoinLevelEvent` 在初始化完成后保存生物并取消入世。失败则不取消。原生 `tryAddFreshEntityWithPassengers()` 在该事件取消后仍返回成功，完整批次和冷却继续由原生刷怪笼处理。记录用弱键避免保留被其他模组取消的临时实体；仅服务端访问。

## 原生与神化

通过 `BlockEntityType.MOB_SPAWNER.create()` 创建未放入世界的实际实体，直接调用其 `BaseSpawner.serverTick()`。神化替换注册表工厂后，自动使用其 `ApothSpawnerTile` 和 `SpawnerLogicExt`，不另抄一套刷怪逻辑。原物品及未知 NBT 保留；每次运行把原生状态合并回槽内物品的 `BlockEntityTag`。

`BaseSpawner.isNearPlayer()` 的玩家距离豁免，以及 `SpawnData.getCustomSpawnRules()` 的临时全光照范围规则，共用高级运行上下文。`WorldSpawnerTickMixin` 仅包住 `SpawnerBlockEntity.serverTick()` 对原生 `BaseSpawner.serverTick()` 的调用：先检查六面紧邻的放置背包，再调用原版或神化自己的刷怪逻辑。作用域在 `finally` 中恢复，不改写世界刷怪笼或物品中的权限 NBT。生成出口仍使用事件，没有针对神化类的 Mixin。

世界刷怪笼的条件豁免与捕捉来源独立：一个背包提供高级条件豁免，另一个紧邻背包提供捕捉也可生效。全部紧邻捕捉背包均无法接收时，继续原生世界生成。

神化是可选依赖，验证版本为 1.20.1 的 Apotheosis 7.4.8。正式 JAR 不包含神化、Placebo、AttributesLib 或测试类。

## 验证入口

- `runGameTestServer build --offline`：无神化基线。
- `runGameTestServer build -PwithApotheosis=true -PwithEstinker=true --offline`：神化和嬗变炉共同运行。
- `runClient build -PspawnerSmoke --offline`：实际界面放入/取出刷怪笼、模块开关、服务端刷怪/捕捉及客户端生物显示。
- `runClient -PmanualTest` 后执行 `/ssmeltery_test spawner`：提供关闭的高级刷怪笼升级、普通捕捉升级、刷怪笼及普通刷怪笼升级，供人工操作。

验证结果见 [Implementation_Status.md](Implementation_Status.md)。
