# Mixin 的用途和验证范围

当前共 7 个 Mixin：3 个访问接口，4 个行为适配类。不替换控制器世界 Tick，不添加多方块扫描，也不覆盖原生配方或产物算法。

| Mixin | 用途 | 生效范围 |
| --- | --- | --- |
| `HeatingControllerAccess` | 读取结构及燃料消耗率，调用实际 `setStructure` / `heat`，接入便携燃料模块和阶段计数 | 访问接口，供便携实例使用；保留子类动态分派 |
| `HeatingMaterialsAccess` | 调用实际结构的材料、底板、墙及储罐判断 | 访问接口，本身不改变行为 |
| `MultiblockShapeAccess` | 读取原生几何限制 | 访问接口，本身不改变行为 |
| `HeatingControllerMixin` | 液体变化写回升级，避免向世界坐标发送同步 | 仅已通过 `attach` 标记的便携实例 |
| `MantleBlockEntityMixin` | `setChangedFast` 转为升级保存通知 | 仅实现便携接口且已绑定保存回调的实例，其余 Mantle 方块实体照常执行 |
| `MeltingInventoryMixin` | 计算尺寸时阻止按虚拟体积分配熔炼模块 | 仅便携实例计算结构尺寸期间；实际输入槽的 resize 正常执行 |
| `MeltingModuleMixin` | 抑制模块物品的世界同步；原生产物成功后从背包扣料；清理配方缓存 | 仅绑定背包输入的模块；未绑定模块的两处 Redirect 执行原表达式 |

行为注入及 Redirect 使用必需匹配检查。当前验证版本为 TiC 3.12.1.231 / Mantle 1.11.117；升级依赖时应重新执行 GameTest，检查注入匹配以及实际产物。Redirect 与其他修改同一调用点的模组仍可能冲突，当前测试不代表验证了所有模组组合。

验证包含原生 Foundry 与便携 Foundry 的实际产物对照，以及控制器子类覆盖容量和 `tryFillTank()` 的测试。测试子类不能替代实际 Transmute 的运行验证。动态图标与背包槽标记通过客户端模型/GUI 注册实现，没有新增客户端 Mixin。
