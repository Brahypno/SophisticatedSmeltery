# 基础熔炼联动实施记录

设计约束与用户决定见 [Integration_Questions_and_Decisions.md](Integration_Questions_and_Decisions.md)。

## 功能状态与验收（2026-10-25）

### 三种炉型共用的容器行排布

冶炼炉、熔铸炉、嬗变炉共用新的横向容器行：输入槽保持原位，输出槽使用相同的 18×18 槽框并与燃料槽右边框对齐，桶模式按钮位于箭杆上。按钮左侧留 2 像素、右侧留 12 像素，右侧完整显示未拉伸的原生箭头头部；只延长箭杆，避免头部被按钮遮挡。实际槽位、槽框和点击区域同步移动。这项通用布局调整独立于高级嬗变炉功能提交。

三种炉型的实际客户端均已通过，日志分别为 `build/container-align-transmute.log`、`build/container-align-smeltery.log`、`build/container-align-foundry.log`。均检查滚动/非滚动界面的箭头空间、槽位对齐及输出槽与燃料槽右边框对齐；嬗变炉继续验证高级开关同步，冶炼炉/熔铸炉实际点击桶的三种模式、堆叠输入、连续取出输出，并核对全部容器守恒。三份日志均为 `BUILD SUCCESSFUL`，最后一轮完成正式构建；截图 `build/transmute-controls-alloy-off.png`、`build/bucket-ui-first-output.png` 已核对，确认输入/输出槽框同为 18×18，箭头头部完整可见。

### 缺失控制器时回退为空白停用状态

卸载 estinker 或控制器无法解析时，不再创建 `NativeFurnace`，已安装的升级显示为空白并停用：无熔炼槽、无流体容量、不处理物品/生物、不接受新的材料或桶，可以取下。仍有保存内容的升级保持不可堆叠。原 `controller` / `furnace` NBT 原样保留，包含未注册的物品/流体 ID；停用时不反序列化内部库存，也不写回空状态。控制器重新可用后可从保存内容恢复。

无 estinker 的 **36 项 GameTest 与构建通过**，日志 `build/missing-controller-tests.log`；有 estinker 的 **36 项回归与构建通过**，日志 `build/missing-controller-present.log`。新增测试覆盖空白名称与停用、移除、槽位/流体接口、容器消息、精确 NBT 保留、已注册控制器恢复、真实携带/放置背包。此前实际崩溃的 `run/world` 也已在不加载 estinker 时重开并跑完 **36 项测试**，记录在 `build/missing-controller-world-client.log`。

同一流程中的实际客户端 `runClient -PheatingSwitchSmoke -PmissingControllerSmoke --offline` 也已通过：背包可正常打开并展开空白页，无炉体或加热，日志含 `MISSING CONTROLLER UI PASSED` 和最终 `BUILD SUCCESSFUL`。截图 `build/missing-controller-empty.png` 已核对。

### 全部支持炉型的生物熔炼

物品与生物熔炼统一覆盖全部已支持的控制器，生物路径已去掉仅允许冶炼炉/熔铸炉的白名单。嬗变炉的可选适配使用原生专用生物配方缓存、优先级、伤害与全部副产物，回退到匠魂配方时使用 estinker 配置倍率；九格加热器可供热。仍遵守总开关、捕获槽范围、免疫/黑名单、完整产物容量保护和实际死亡/掉落。

有 estinker 的 **35 项 GameTest 与构建通过**，日志 `build/all-furnaces-mob-tests.log`；三种炉型均覆盖原生伤害/流体对照、范围与免疫、燃料/容量/总开关、死亡掉落与满包重载、携带/放置宿主。新增嬗变炉专用配方优先级、多副产物、加热器供热、炉液恢复和配方缓存重载检查。无 estinker 的 **35 项 GameTest 与构建通过**，日志 `build/all-furnaces-mob-absent.log`。

实际嬗变炉客户端 `runClient -PwithEstinker=true -PmobMeltingReloadSmoke -PtransmuteMobSmoke --offline` **通过**，日志 `build/transmute-mob-client.log`：滚动/非滚动界面生物血量分别从 100→98、98→96，实际开关同步通过；保持加热保存退出后从磁盘重进，原捕获 UUID 可见并继续受伤，关闭后血量稳定为 92。日志含 `ACTUAL MOB MELTING UI PASSED`、`ACTUAL MOB RELOAD UI PASSED` 与 `BUILD SUCCESSFUL`；截图 `build/mob-reload-{loaded,heating}.png` 已核对。

有/无 estinker 的 GameTest 分别使用 `build/gametest-estinker` 与 `build/gametest-base` 存档。此前共用测试世界卸载 estinker 后曾复现 `Not a supported heating controller`，现已通过上文的空白停用回退修复，并实际重开原测试存档验证。

### 嬗变炉可选依赖与测试基线

已从本地 Maven 接入并更新到 `org.brahypno:esotericism_tinker:1.6.2fix`。编译使用 `compileOnly fg.deobf(...)`，`-PwithEstinker=true` 才加入运行依赖；`mods.toml` 使用 `mandatory=false` 与 `ordering="AFTER"`。正式 JAR 已核对不包含 estinker、其嵌套依赖或测试类。

- 接入前：实际 **30 项 GameTest 全过**，`runGameTestServer build --offline` 成功，日志 `build/estinker-before-baseline.log`。
- 接入后无 estinker：**31 项 GameTest 全过**，构建成功，日志 `build/estinker-absent-baseline.log`；新增测试确认嬗变炉绑定配方未加载。
- `1.6.2fix` 有模组基线：**31 项 GameTest 全过**，构建成功，日志 `build/estinker-fix-baseline.log`。已执行真实嬗变炉控制器、原生熔炼模块、容量、燃料驱动的双物品批次与炉液保存恢复。
- `1.6.2fix` 合成/熔炼规则扩展：**33 项 GameTest 全过**，构建成功，日志 `build/estinker-fix-rules.log`。覆盖 2×2/3×3 绑定及换绑、控制器 NBT/剩余物、配方网络同步；铁锭/粗铁/粗铜/粗金的单个和双个批次，以及多副产物比例、整数余量和容量边界与原生模块对照。真实流体 ID、数量和容量截断记录在该日志。
- `1.6.2fix` 引用下无模组回归：**33 项 GameTest 全过**，构建成功，日志 `build/estinker-fix-absent.log`；条件配方保持禁用，可选模组测试分支不加载第三方类。
- 旧 SNAPSHOT 的客户端：`runClient -PwithEstinker=true -PheatingSwitchSmoke --offline` **通过**，日志 `build/estinker-client-baseline.log`；实际进入集成世界并完成滚动/非滚动背包界面、加热开关服务端同步、原生图标/展开页与 JEI 升阶配方检查。该记录不代表 `1.6.2fix` 的客户端专项验收。

加热器与加速器已按用户要求接入原九格方块槽，按槽物品数量生效，继续调用原生加热方法与配置数值；同数量替换、增减、结构变化及恢复后均重算。最低够用燃料选择和热量条判断已计入加热器温度。含 estinker 的 **34 项 GameTest 全部通过**并构建成功，日志 `build/estinker-components-tests.log`；新增流程通过真实背包验证九格接收、堆叠计数、即时替换、原生速率、移除、总开关、重载，以及加热器辅助下优先使用低温燃料。

无 estinker 的 **34 项 GameTest 与构建也通过**，日志 `build/estinker-components-absent.log`。`1.6.2fix` 客户端的现有界面冒烟检查通过，日志 `build/estinker-components-client.log`，包含滚动/非滚动界面、总加热开关服务端同步、图标/展开页和 JEI 升阶配方检查及最终 `ACTUAL BACKPACK UI PASSED`、`BUILD SUCCESSFUL`；组件效果本身由上述服务端测试验证，尚未录制嬗变炉组件的客户端专项操作流程。

旧 SNAPSHOT 曾在专用服务端网络包注册时加载 `ClientLevel` 导致启动失败，见 `build/estinker-present-baseline.log`；`1.6.2fix` 已解除该阻塞。用户确认正常产出和容量边界均与原生一致，说明正确调用了嬗变炉提供的熔炼模块，本轮保留原生行为。详细规则见 [Transmute_Verification.md](Transmute_Verification.md)。模式/合金开关仍待后续实现，计划见 [Plan.md](Plan.md)。下文保留历史功能验收记录。

mod ID 已统一为 `sophisticated_smeltery`，同步 Java 入口、事件订阅、Mixin 配置与 refmap、资源目录、配方/进度/标签、语言键及测试模板。重新编译与正式构建通过，29 项 GameTest 全部通过，日志 `build/mod-id-verification.log`；`gradlew.bat runClient -PheatingSwitchSmoke --offline` 通过滚动/非滚动背包界面的开关同步及 JEI 升阶配方检查，日志 `build/mod-id-client-verification.log`。产物 `build/libs/sophisticated_smeltery-0.5-SNAPSHOT.jar` 已核对元数据、Mixin 清单、新命名空间资源及测试文件隔离。旧命名空间存档迁移未包含在本次修改中。

“已实现”表示代码已接入；“自动验证”只覆盖所列测试；“用户已确认”仅记录对话中明确确认的部分。其余人工操作仍待验收，不以编译或单独控件截图替代。

用户反馈随身背包重进世界后牛不可见、仍有受伤声音及掉落。新增显示断言后已复现：实际菜单中存在捕获记录，但 `storageInventoryControls` 未创建原生捕获器显示控件，见 `build/mob-reload-repro.log` 的 `The actual screen is missing its captured-mob display control`。此前客户端检查只核对捕获数据和血量，没有覆盖控件缺失。

现通过 `MobCatcherDisplayRefresh` 在熔炼背包界面的客户端 Tick 中，仅在原生捕获器显示控件缺失时重试原生工厂；原有控件和模型缓存保持原生行为。`gradle runGameTestServer runClient build -PmobMeltingReloadSmoke --offline` 已通过，日志 `build/mob-reload-verification.log`：29 项 GameTest 全过，有/无滚动区域均断言真实控件、占位渲染和模型存在；保持总加热开启保存退出、从磁盘重新加载同一世界，通过手持物品入口重开原背包，同一捕获 UUID 可见且继续受伤，关闭后血量稳定为 92。截图 `build/mob-reload-{loaded,heating}.png` 已检查，最终 `BUILD SUCCESSFUL`。修复版 JAR 已生成并核对包含客户端补建类和控件访问接口，测试类不入包。

生物熔炼已接入原生冶炼炉和熔铸炉，跟随总加热开关、自动处理占位与熔炼区部分重叠的全部捕获生物。使用实际伤害、死亡和掉落路径，生命值写回捕获记录；死亡释放占位，掉落普通槽优先/熔炼槽回退，满包残余随控制器保存并纳入合成返还。

新增 **4 项 `MobMeltingGameTests`**，当前完整 **29 项 GameTest 全部通过**，日志 `build/mob-melting-tests.log`。覆盖两种炉子的原生伤害/产液对照、默认产物、耐火生物、部分占位、区域外及免疫、总开关、无燃料和炉满、真实死亡/掉落事件及取消、满包重载/返还、实际放置与携带入口和受损 NBT 恢复。测试资源中的黑名单条目仅用于验证 tag 生效，不随正式 JAR 打包，不添加生产默认黑名单。

实际客户端 `gradle runClient build -PmobMeltingSmoke --offline` 已通过并完成重混淆打包，日志 `build/mob-melting-client-build.log`。有/无滚动区域的真实屏幕点击总开关后，血量分别从 10→8、8→6 同步，精确 NBT 与界面血量一致，关闭后保持血量；两次 `ACTUAL MOB MELTING UI PASSED`、最终 `ACTUAL BACKPACK UI PASSED` 和 `BUILD SUCCESSFUL` 均已确认。截图 `build/mob-melting-{0,1}.png` 已检查。产物 `build/libs/sophisticatedsmeltery-1.0-SNAPSHOT.jar` 已核对包含新模块及访问接口，不包含测试类或测试黑名单。嬗变炉/未知第三方生物模块和真实长期存档重进仍未在本轮验证。

生物联动前，测试精简的实际基线为 **54 项**（原 53 项加自定义清空配方回归），已全部通过，日志 `build/test-consolidation-baseline.log`。当时合并为 **25 项 GameTest，全部通过**，日志 `build/test-consolidation-tests.log`；加入生物熔炼后为上文所述的 29 项，分组及行为覆盖见下表。合并使用同一库存、炉子或宿主继续检查状态转换，减少重复建炉和已由完整操作覆盖的局部断言；原生配方、材质和动态结果矩阵继续执行。

运行代码优化限于浇筑：手动、关闭和冷却期间跳过自动供液来源快照；自动浇筑复用本次来源列表；有液体或已有模具时不复制锁定模具；冷却完成时复用已生成的动态产物作容量检查，避免再次执行配方 `assemble()`。不改变流体来源顺序、产物去向和开关行为。

合成返还的合并测试另外检出控制器物品 NBT 与输入升级共享引用的问题：直接修改返还物会改动输入。`ControllerBinding.getController()` 现从复制后的存储标签构造物品，返还物与输入独立；同一测试同时核对 2×2/3×3 换绑和解绑的实际取出与物品守恒。

浇筑优化后的完整客户端流程也已通过：`gradle runClient -PcastingExpansionSmoke --offline`，日志 `build/test-consolidation-client.log` 包含 `ACTUAL BACKPACK UI PASSED`、`ACTUAL CASTING UI PASSED`、`ACTUAL CASTING EXPANSION UI PASSED` 和最终 `BUILD SUCCESSFUL`。覆盖真实屏幕的手动/自动、独立储罐、冷却中关闭、产物入包/堆积、模具锁补料及精确数量守恒。

| 功能组 | 实现状态 | 已有证据 | 用户验收状态                                |
| --- | --- | --- |---------------------------------------|
| 空白/绑定/无序换绑、控制器唯一、原控制器 NBT 返还 | 已实现 | GameTest：合成与安装限制；可见绑定配方 | 工作台、JEI、已                             |
| 换绑/解绑返还全部内部物品、解绑返还旧控制器 | 已修复 | 2×2/3×3 实际 ResultSlot 取出，验证 11 份内部物品及原控制器 NBT；含燃料罐液体 | 用户确认“都返还”；自动路径通过 |
| Upgrade Base 底板与控制器正面图标 | 已实现 | 实际客户端截图 | 用户已确认图标符合需求                           |
| 控制器原生名称、空白升级 tooltip 中英文 | 已实现 | 名称组件测试、实际客户端 tooltip 检查 | 新名称与资源包组合已验收                          |
| 数值结构、前 X 个真实背包槽、容量与原生模块 | 已实现 | GameTest：最小结构、容量、原槽扣料、Foundry 副产物与原生对照 | 连续批次和大型背包待验收                          |
| 排序/替换后进度重算、携带与放置背包调度 | 已实现，自动验证通过 | 实际调用背包原生排序，移动后重置进度并产出正确流体，正常输入接口仍可用；真实两种入口调度 | 自动路径通过；拾取/磁铁组合及重进世界待用户复验              |
| 炉满保护、拆卸条件、堆叠升级降级保护 | 已实现 | GameTest：不提前扣料、不溢出及移除限制 | 实际操作待验收                               |
| 冶炼炉/熔铸炉原生加热、合金和副产物策略 | 已实现 | GameTest：实际 heat 覆写、玫瑰金合金、Foundry 无合金 | 实际两种炉子连续运行待验收                         |
| 燃料配方消耗、温度和速率换算 | 已实现 | 原生配方及温度测试 | 用户已实测确认消耗转化规则；切换延迟另列                  |
| 最低够用温度、不可达高温输入不阻塞 | 已实现 | 实际加载 800/1300/2000 度配方测试 | 混合燃料长时间操作待验收                          |
| 立即切换并保存旧燃料剩余燃烧量 | 已实现，自动验证通过 | 28 项 GameTest 通过，新增切高温/切低温、精确余量、保存重载及关机测试 | 用户已确认规则；本轮效果待验收                       |
| 模板旁加热开关 | 已修正，真实客户端验证通过 | 有/无库存滚动区域均通过屏幕点击与服务端关→开→关同步，截图中红绿状态可见 | 本轮用户复验待完成                             |
| 与高级磁铁一致的页签图标及展开背景 | 已修正，真实客户端验证通过 | 展开/收起均保留原版图标锚点；两种展开背景左侧边框像素对照一致 | 本轮展开/收起截图待用户验收                        |
| 分层炉液、少量液体可见、原生单位和 Shift mB | 已实现 | 原生 GuiSmelteryTank、流体容器服务端测试 | 少量铜液显示、悬停和层次切换待验收                     |
| 桶输入/输出、三模式图标、手持桶交互 | 已实现，整页客户端交互通过 | 真实屏幕点击完成液层选择、堆叠取液、右键注液、三模式切换及连续桶输出，精确核对数量 | 横排方案已确认；自动验证通过，用户已复验                  |
| 先放桶、后到炉液的自动重试 | 已修复，自动验证通过 | 新回归先复现旧问题；修复后 29 项 GameTest 全过，取走输出后连续处理下一桶 | 待用户复验                                 |
| 固定宽度左侧热量条与状态提示 | 已实现 | 六种状态截图、整数像素边界检查 | 用户认可区分效果；滚动和提示交界待验收                   |
| 移除橙色/灰色顶部横条 | 已实现 | 绘制路径已移除 | 本轮游戏效果待验收                             |
| 22×18 加宽槽框、保留 16×16 物品 | 暂缓，未接入正式库存布局 | 临时试排曾生成局部对照，不代表正式实现；现已清理临时改动 | 用户确认需要额外客户端 Mixin 后先不做；后续范围见 [Plan.md](Plan.md) |
| Transmute 通用路径与热组件 | 可选联动基线及九格加热器/加速器通过 | `1.6.2fix` 真实控制器与原生对照；组件计数/加热/恢复与燃料选择 | 生物熔炼已接入；模式/合金开关待映射 |
| 浇筑核心 | 已实现，原生对照测试通过 | 原生台/盆、动态 NBT、保存/退液、预览失效及完整 45 项 GameTest 通过 | 已接入升级，待整体用户验收                         |
| 浇筑升级物品、GUI 与宿主 | 已实现，真实客户端验证通过 | 45 项服务端回归通过；实际屏幕完成手动/自动、台/盆、冷却拆卸拦截、半满退液、干净移除和精确守恒 | 规则已确认，整体界面待用户验收                       |
| 普通/高级浇筑分级与配方 | 已实现，服务端及实际客户端通过 | 49 项回归通过；普通页无自动按钮，高级自动正常，俯视图标与金边可见；两配方实际进入 JEI 可见列表和客户端配方书 | 新分级、图标和布局待用户验收                        |
| 浇筑展开背景重复左边框 | 已修正，真实像素对照通过 | 旧实现 128 像素边框对照失败；修正后普通/高级与磁铁一致，熔炼文件保持原样，见 casting-tab-frame-tests.log | 待用户复验                                 |
| 浇筑开关与独立储罐来源 | 已实现，服务端和实际客户端通过 | 关闭仅停止新浇筑、冷却和交付继续；无熔炼升级的储罐供液/退液、炉底优先及同种补液；实际点击浇筑开关并核对服务端结果 | 关闭行为已由用户确认；游戏内效果待验收                   |
| 浇筑产物去向与输出堆积 | 已实现，服务端和实际客户端通过 | 同物品/NBT/容量检查、冷却期间取旧产物、普通槽优先/熔炼槽回退、背包满后恢复；实际点击切换去向，产出一锭入包、两锭留槽堆积 | 待用户验收                                 |
| 焦褐浇筑与模具锁补料 | 已实现，服务端和实际客户端通过 | 原生焦褐台/盆逐 Tick 对照、金平台与无模具限制、精确匹配补料、缺料恢复及重建保存；实际点击锁定/解除，三份沙模恰好消耗并产出三锭 | 待用户验收                                 |
| 焦黑命名、焦褐两级配方与图标 | 已实现，服务端和实际客户端通过 | 中文核对匠魂原文，焦黑 ID 已包含 seared（开发阶段不兼容旧 ID）；升阶独立 NBT 与配方包；四种实际模型、两级焦褐配方真实进入配方书/JEI，页边框像素对照通过 | 待用户验收                                 |
| 炉液外部接口、手持背包右键储罐 | 已实现，服务端验证通过 | 51 项 GameTest 全过；新增 5 项覆盖模拟与立即保存、炉液优先/普通储罐回退、方向选择、六面能力及安装拆卸刷新、真实右键事件 | 具体管道模组组合待实机复验                         |
| 背包下部控制器凸起 | 已实现，实际客户端验证通过 | 原生控制器贴图；物品与穿戴上下文、四个实际方块模型数据和朝向、普通包复位及原模型类型检查通过，截图 `build/backpack-controller-smoke.png` | 外观待用户验收                               |
| 捕获生物熔炼 | 已实现，服务端及实际客户端通过 | 4 项新增回归；两种炉子原生对照、真实宿主、死亡掉落和满包重载；真实血量同步和共享开关 | 规则已确认，待用户游戏内复验 |
| 背面定向管道、物品黑白名单 | 后续版本 | 设计边界已记录 | 未实现 |

浇筑扩展原先通过完整 **62 项 GameTest**，历史日志 `build/casting-expansion-tests.log`。按用户要求精简后，浇筑由 32 项降为 **16 项**，全项目为 **46 项**。同一流程的检查合并，焦黑/焦褐同类规则用数据表对照；删除普通 JSON 配方排布、缺材料和错误材料等依赖原版匹配器的重复断言，升阶仅保留一项覆盖两种材质的自定义状态转移与客户端配方包回归。四份浇筑测试源码由 794 行减至 624 行；未修改浇筑运行逻辑。

炉液外部访问新增 5 项后，当时全项目为 **51 项 GameTest**，历史日志 `build/fluid-access-tests.log`。对外接口使用精妙背包 `IFluidHandlerWrapperUpgrade`，右键使用 Forge 事件，没有新增服务端 Mixin。背包下部凸起使用一个客户端模型 Mixin，具体边界见 Mixin_Boundaries.md。

数量指 `@GameTest` 入口数，矩阵中仍执行各模具/材质场景；这里的覆盖指下列行为，项目没有百分比覆盖率统计。56、62 项是精简前不同轮次的记录。当前为：

| 测试组 | 数量 | 主要覆盖 |
| --- | ---: | --- |
| `FurnaceGameTests` | 6 | 结构与原生覆写；输入、进度重载、替换、炉满和关机至配方失效；燃料优先/暂停/空闲保存；原生合金与副产物；温度选择；余量切换重载 |
| `UpgradeGameTests` | 8 | 安装与原生名称；缺失控制器空白停用及 NBT 保留；容量/燃料/拆卸保护；真实槽位与排序；携带/放置宿主和保存；合成返还；清空配方与桶交互 |
| `FluidAccessGameTests` | 3 | 真实放置能力的安装、模拟、立即保存、储罐优先、六面访问和拆卸刷新；侧面燃料优先；真实右键事件的双向转移及满炉拦截 |
| `CastingGameTests` | 3 | 原生台/盆、材质、模具和动态产物矩阵；半满退液与冷却重载；配方重载与预览失效 |
| `CastingUpgradeGameTests` | 2 | 普通版安装、手动与越级拦截；高级版半满自动补液、拆卸/重建生命周期和多升级共享炉液守恒 |
| `CastingExpansionGameTests` | 4 | 独立储罐与开关保存；炉底优先及输出物品/NBT/容量；满包恢复及动态/换槽结果交付矩阵；模具锁补料与缺料恢复 |
| `CastingRecipeGameTests` | 1 | 两种系列自定义升阶的独立 NBT 复制、锁/开关/去向保留、无重复返还、客户端配方序列化 |
| `MobMeltingGameTests` | 5 | 三炉原生伤害/产液；范围与免疫、燃料、容量及总开关；死亡掉落、取消、满包重载/返还及携带/放置；嬗变专用优先级/副产物/加热器与配方重载 |
| `EstinkerBaselineGameTests` | 4 | 可选加载、合成/换绑及网络配方；真实控制器批次/容量/炉液恢复；原生主副产物对照；九格加热器/加速器及燃料选择 |

实际客户端点击、截图、图标和边框像素对照、配方书/JEI 显示另行验证，不计入上述 25 项；其中两种普通 JSON 配方仍由现有客户端流程检查可见性。完整第三方 Transmute/嬗变炉、不同资源包组合、长时间运行及真实存档重新进入仍待对应环境验收。

精简后的完整 46 项已通过，日志 `build/casting-consolidated-tests.log`。同时按用户最终确认的布局，仅将浇筑按钮移至独立产物预览下方 `x+100,y+74`；预览、台/盆切换、模具锁和底部三个按钮不移动。实际客户端完整流程已通过，日志 `build/casting-pour-position-ui-test.log`，截图已检查；手动浇筑、新位置点击、退液、自动、输出去向及模具锁均正常，最终 `BUILD SUCCESSFUL`。

随后按用户要求将底部退液、自动、产物去向等距排在 `x+8/54/100,y+92`，按钮间空隙均为 28 像素；浇筑按钮继续位于预览下方。现有客户端流程已通过，日志 `build/casting-even-buttons-ui-test.log`，实际截图已检查；没有新增测试或改变服务端逻辑。当前安装包的打包日志为 `build/casting-even-buttons-build.log`。

实际客户端 `gradle runClient -PcastingExpansionSmoke --offline` 已通过，日志 `build/casting-expansion-ui-test.log` 包含 `ACTUAL BACKPACK UI PASSED`、`ACTUAL CASTING UI PASSED`、`ACTUAL CASTING EXPANSION UI PASSED` 及最终 `BUILD SUCCESSFUL`。新增测试在独立世界使用无熔炼控制器的背包，实际屏幕点击完成模具锁、优先包内、浇筑中关闭、冷却后交付、重新开启、切回留槽、自动连续消耗模具、解除锁定及普通焦褐页检查。270 mB 铁液最终为包内一锭与输出槽两锭，三份一次性沙模全部正确消耗。没有直接改客户端库存或游标。

已检查截图 `build/casting-expansion-{manual,off-output-storage,stacked,basic,icons}.png`。四种图标取对应原生焦黑/焦褐材质与高级外框；全部操作改用图标后，浇筑页统一为 115 像素高、128 像素宽，左边框继续与原版磁铁匹配。客户端实际验证为英文界面，中文名称已核对依赖语言文件；中文整页、不同资源包和重新进入真实存档的组合仍待用户验收。

浇筑输入/输出空槽现提供用途悬停提示，有物品时保留普通物品提示；两个原生箭头指引输入→液体区→输出。焦黑物品、模型、配方与解锁 ID 直接改为 seared_casting_upgrade / advanced_seared_casting_upgrade，用户明确开发阶段不做旧 ID 兼容。焦褐无法安装的原因是遗漏 sophisticatedbackpacks:upgrade 标签，现已补齐四种浇筑升级；安装检查并入既有回归，浇筑仍为 16 项。服务端日志 build/casting-io-registration-tests.log；实际客户端日志 build/casting-io-ui-test.log 已验证两种焦褐升级的正常槽位取出/重新安装，以及空槽提示和有物品时原提示，截图已检查。

模具锁使用 Minecraft 原生锁头按钮，放在台/盆图标旁，替代底部整行文字按钮；悬停保留说明和锁定物品名称。`build/casting-lock-icon-ui-test.log` 已通过完整实际客户端流程，三次 `CAST LOCK ICON MATCH` 分别验证开锁、闭锁、空模具禁用状态与原版逐像素一致，并完成真实点击锁定和解除、自动补料及最终数量守恒。本次只改界面，服务端逻辑与上述 62 项基线保持一致。

其余浇筑动作随后也按用户要求改为图标：底部单行浇注口、返回箭头、绿/灰循环箭头、背包/槽位，保留动作及当前状态的本地化悬停提示。`build/casting-action-icons-ui-test.log` 已通过完整客户端流程，覆盖新位置的手动浇筑、退液、自动开/关、产物去向切换和锁定；三种锁头状态及 128×115 面板的边框像素对照也通过，最终 `BUILD SUCCESSFUL`。没有新增纹理或修改服务端逻辑，62 项服务端基线无需因纯界面改动重复运行。

`gradle build --offline` 已通过并完成重混淆打包，产物为 `build/libs/sophisticatedsmeltery-1.0-SNAPSHOT.jar`，当前日志 `build/casting-io-build.log`。测试夹具与客户端探针不随正式 JAR 打包。

本轮真实客户端测试日志为 `build/actual-ui-test.log`，包含 `ACTUAL BACKPACK UI PASSED`。在独立测试世界实际打开同时安装控制器及高级磁铁的下界合金背包，使用 1280×480 和 1280×900 窗口、GUI 比例 2，分别覆盖有/无库存滚动区域。8 张截图为 `build/switch-{0,1}-{closed,on,melting-open,magnet-open}.png`；像素检查结果为 `build/ui-pixel-check.txt`。确认开关红/绿实际可见、真实屏幕点击后服务器同步状态，以及页签收起/展开的原版图标锚点；展开背景左边框的 6×32 像素对照一致。

客户端测试执行测试命令后，先同步背包物品再从物品正常使用入口打开菜单，避免测试夹具在背包尚未同步时打开空界面。只有完整走到验证成功和正常保存退出才算通过。自动化通过的条目仍保留用户复验状态。

整页流体交互测试使用 `-PmeltingInteractionSmoke`，日志 `build/melting-interactions-test.log` 同时包含 `ACTUAL BACKPACK UI PASSED` 和 `ACTUAL FLUID UI PASSED`，最终 `BUILD SUCCESSFUL`。25 个操作阶段都使用真实屏幕点击、正常库存槽和服务器同步结果，不直接改客户端游标/库存。先切换金液到底层，以 8 个空桶取液、注回，再经桶输入/输出槽连续取出金液桶和铁液桶，最后通过倒空模式将金液注回，得到 7 个空桶、1 个铁液桶和炉内 1000 mB 金液；输入/输出槽为空。实际截图为 `build/bucket-ui-first-output.png` 和 `build/bucket-ui-injection-complete.png`。

## 已验证的原生引擎

浇筑扩展前的基线在 Minecraft 1.20.1 / Forge 47.4.28 / TiC 3.12.1.231 的 Forge GameTest 服务端通过全部 49 项测试，历史日志为本地 `build/casting-tier-tests.log`，包含之前 45 项以及普通版越级限制、图示普通配方、高级升阶状态和客户端配方包测试。上一轮基础版本的 20 项测试已包含在这些回归检查中；当前 29 项清单以上文为准。

原生引擎验证包含：

1. 未满足最小结构用量时容量为零；计算大结构容量不按体积分配模块。
2. 输入始终保留在背包，仅原生熔炼产出成功后扣除本批数量。
3. 炉满时不扣料；关闭后不会通过冷却重试产出已完成的批次。
4. 替换槽内物品会重置进度并使用新物品配方。
5. 进度保存及恢复不会创建、迁移或重复消耗输入。
6. 关闭时暂停剩余燃烧量，再开启后按原生规则消耗。
7. Foundry 矿物产物、副产物及容量与原生实例一致。
8. 测试控制器子类覆盖的容量计算及自定义 `tryFillTank()` 被保留并执行。
9. 原本使用背包燃料后，再填入内部燃料，下一次补充同温度新燃料时按内部储罐优先，不继续粘着上次来源。
10. 使用原生玫瑰金合金配方，检查控制器加热开关及产量；移除了额外合金开关，Foundry 不添加合金能力。
11. 从实际 RecipeManager 移除配方后，已加热输入不能继续通过缓存产出。
12. 调用实际控制器 `heat()` 覆写，跳过世界 ACTIVE 更新阶段；子类可定义不同加热行为。
13. 真正加载 800/1300/2000 度测试配方，检查岩浆与烈焰血的最低够用温度选择，以及不可达任务不阻挡其他输入。
14. 空闲原生加热不请求周期性 NBT 保存。
15. 岩浆尚未燃尽时切换到高温配方，在下一个原生补燃料阶段开始加工；保存重载并排空所有燃料液体后，仍可精确恢复两种燃料的原余量、温度与速率。关机全部暂停；按最新返还规则，换绑会清除旧炉燃烧状态并返还内部物品。
16. 空炉先放入 8 个桶，再注入 2000 mB 炉液，下一次正常升级 Tick 填充一桶；输出未拿走时不继续扣液体，取走后恰好再填一桶，剩余输入、输出及液体状态正确保存。临时未修正版本只在该新增测试失败，见 `build/waiting-bucket-negative.log`。
17. 真实调用背包 `sort()`，按数量将 64 个铁锭从普通槽移到正在加热金锭的首槽。金锭进度清零，随后正确产出 90 mB 铁液，仅扣除一个铁锭，金锭保留；通过背包公共自动输入接口向同一熔炼槽再插入一个铁锭成功。

背包集成覆盖：空白不能安装、控制器唯一、无序换绑返还原物品 NBT、炉液和燃料移除限制、堆叠升级降级保护、使用前 X 个真实槽位、完整升级 Tick 的熔炼及保存，以及实际放置背包和背包物品入口的调度；本轮补充可见绑定配方、控制器原生名称组件、真实容器消息中的堆叠桶、GUI 桶槽保存及拿走燃料罐后立即清空燃料显示。

测试源代码：`src/test/java/org/brahypno/sophisticated_smeltery/FurnaceGameTests.java` 和 `UpgradeGameTests.java`。

已做一次反向验证：临时恢复“始终优先上次燃料源”的旧错误后，18 项测试中仅燃料优先级测试失败，恢复修正后全部通过。日志为本地忽略目录下的 `build/fuel-negative-control.log`。这仅验证该测试对已知错误的检测能力，不代表其他测试或全部模组组合都得到证明。

运行：`gradle runGameTestServer --offline`。测试源集默认接入 GameTest，开发客户端可通过 `-PmanualTest` 或 `-PclientSmokeTest` 显式启用，不随主模组源集打包。

## 实现方式

- 每个安装的控制器升级复用一个脱离世界注册的原生控制器及其实际熔炼模块库存。
- 加热的 0/1/2 阶段使用实际控制器 `heat()`；只替换燃料来源和持久化，世界 ACTIVE 更新及实体扫描不执行。区别和边界详见 [Furnace_Behavior.md](Furnace_Behavior.md)。
- 结构材料变化时计算尺寸数值，并调用控制器的容量规则；不放置或扫描虚拟方块，不调用控制器的世界 Tick。
- 仅为实际背包加工槽分配模块。模块中的物品仅作为配方计算视图，所有真实输入仍属于背包。
- 原生物品模块的世界坐标同步被便携适配替换；持久化通过升级物品进行。
- 使用原生燃料配方与燃烧记账，实际燃料液体保存在匠魂燃料储罐或背包储罐中。
- 背包设置页复用原生 106×106 储液 GUI、流体单位提示和燃料 GUI，提供材料槽和桶输入/输出槽；加热按钮放在升级模板旁，不提供额外合金开关。普通背包前 X 格作输入，保留排序和输入行为。
- Mixin 的用途和生效条件见 [Mixin_Boundaries.md](Mixin_Boundaries.md)。
- 已检查有效的 854×480 客户端截图：绑定图标采用精妙背包 `Upgrade Base` 底板和原生控制器正面，用户确认图像符合要求。升级名称进一步改为引用控制器原生名称组件；不维护炉名翻译表。没有新增客户端 Mixin；资源包替换尚未实际测试。
- 桶区保持横排槽位，下面使用原生 18×18 模式图标按钮及原生三行提示；三模式独立截图与背包整页点击/服务端同步已验证。原生热栏返还位置也纳入操作步骤，不假设装好的桶总进入某个固定空槽。
- 热量条改为固定底槽和整数像素绘制。六种状态的客户端截图（`build/heat-bars-smoke.png`）已逐行检查左右边界：在 6 倍展示比例下均为 12 像素宽，即实际统一为 2 像素；没有改格子间距或物品大小。
- 空白升级 tooltip 的中英文功能说明已补齐；实际客户端物品 tooltip 路径检查通过。方案 2 的 22×18 槽框与现有 18×18 槽位对照已生成（`build/wide-slots-smoke.png`），两者均保留 16×16 物品，仅用于外观评审，尚未接入实际背包排布。
- 加热开关从库存绘制控件移到独立屏幕控件，避免大背包的滚动剪裁；沿用原版按钮纹理、位置和左侧背景。展开面板宽 138 像素，精妙核心在宽度超过 128 时使用不同左边框起点；此处修正背景起点，保留和高级磁铁相同的图标/标题坐标，不通过额外移动图标补偿背景差异。使用 Forge 屏幕初始化与绘制事件，没有增加 Mixin。
- 待处理的桶只在炉液内容/层次变化时重试；复用现有控制器流体通知记录版本号，加热进度和燃料余量变化不会触发容器配方查找，没有增加 Mixin 类。

## 待完成及验证

- 本轮开关及页签背景已通过真实客户端测试，仍待用户复验；其他新布局、原生热量条、流体单位及桶交互的剩余人工项按上表验收。手工运行首次打开熔炼设置页会保存 GUI 截图；检查步骤见 [Manual_Verification.md](Manual_Verification.md)。
- 嬗变炉模式/合金开关、物品输入替换与配方重载专项验证，以及完整世界多方块运行对照；当前已完成的真实加载和生物验收见本文开头。
- 浇筑核心、普通/高级物品、GUI、配方与宿主策略已接入，49 项 GameTest 和实际客户端交互通过。最新日志 `build/casting-tier-tests.log`、`build/casting-tier-ui-test.log`，详细步骤见 [Casting_Implementation.md](Casting_Implementation.md)。用户确认关闭加热仍可浇筑/冷却，有残液或仍冷却不能拆下；自动仅高级版，规则不再待定，整体外观/操作仍待用户验收。
- 背面定向管道访问、物品黑白名单和第三方专用生物模块仍按后续版本边界保留；原生两种炉子的捕获生物熔炼状态见本文开头。
