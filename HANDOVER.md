# 泉城轨迹 Android App 交接文档

文档日期：2026-11-08  
项目目录：`D:\Users\Lenovo\Desktop\JNSUB\QuanchenTrail`  
应用版本：`versionCode = 1`，`versionName = 1.0`  
适用平台：小米澎湃 OS / HyperOS 3，Android 8.0 及以上  
当前构建状态：Debug / Release 均可构建，Lint 0 errors

---

## 0. 给下一位 Agent 的快速入口

如果你刚刚接手，请按以下顺序阅读：

1. `README.md`：功能说明、构建方式、已知数据冲突；
2. `docs/agent-prompt-v1.0.txt`：项目最初的需求提示词；
3. `app/src/main/java/com/jnsub/quanchentrail/agent/CheckinAgent.java`：核心打卡 Agent；
4. `app/src/main/java/com/jnsub/quanchentrail/data/AppDb.java`：数据库、迁移、进度与图库逻辑；
5. `app/src/main/java/com/jnsub/quanchentrail/MainActivity.java`：定位、通知、页面切换、开发者测试；
6. `app/src/main/assets/`：线路、车站、换乘、POI、成就、官方车站顺序；
7. 本文档“待办事项”和“风险与冲突”两节。

不要先直接修改 UI。当前用户非常关注以下问题：

- 打卡必须在首页“最近车站”下方的页面内卡片完成；
- 最近车站下方的长条区域只是提示，不可点击打卡；
- 不要再弹出重复的“打卡确认”弹窗；
- 同一站点允许多次打卡；
- 一次打卡可上传多张照片；
- 成就必须有中文说明、状态、徽章和组成车站；
- 首末班车必须按最新运营时刻；
- `versionCode` 必须保持 `1`；
- 正式 Release 不能显示开发者测试入口；
- 所有项目文件放在 D 盘工作区，不要放回 C 盘。

---

# 1. 项目目标

「泉城轨迹」是一款济南城市轨道交通打卡应用，目标是：

- 根据用户实时位置识别最近轨道车站；
- 以车站坐标 100 m 默认半径判断是否可打卡；
- 支持地铁、济阳线、云巴 1 号线；
- 支持换乘站跨线路原子联动；
- 记录打卡时间、站点、线路和照片；
- 计算线路、枢纽、景区、医院、高校、换乘、多制式、摄影等成就；
- 支持用户在济南以外地区通过开发者模拟位置测试；
- 界面强调“泉城”“泉水”“轨道交通”视觉特征；
- 保持稳定、易用、适合小米 HyperOS 设备。

---

# 2. 当前目录结构

```text
D:\Users\Lenovo\Desktop\JNSUB\
├── .gradle-user\                 # 本地 Gradle 用户缓存
├── .tools\                       # JDK 17、Android SDK、Gradle、adb、emulator
└── QuanchenTrail\
    ├── app\
    │   ├── build.gradle
    │   └── src\
    │       ├── main\
    │       │   ├── AndroidManifest.xml
    │       │   ├── assets\
    │       │   │   ├── achievements.json
    │       │   │   ├── line_station_order.json
    │       │   │   ├── lines.json
    │       │   │   ├── poi_mapping.json
    │       │   │   ├── stations.json
    │       │   │   └── transfers.json
    │       │   ├── java/com/jnsub/quanchentrail\
    │       │   │   ├── MainActivity.java
    │       │   │   ├── QuanchenTrailApp.java
    │       │   │   ├── CrashLog.java
    │       │   │   ├── agent\
    │       │   │   │   ├── CheckinAgent.java
    │       │   │   │   └── Geo.java
    │       │   │   ├── data\
    │       │   │   │   ├── AppDb.java
    │       │   │   │   ├── Station.java
    │       │   │   │   ├── Poi.java
    │       │   │   │   ├── LineProgress.java
    │       │   │   │   ├── AchievementProgress.java
    │       │   │   │   ├── CheckinRecord.java
    │       │   │   │   └── GalleryItem.java
    │       │   │   └── ui\
    │       │   │       ├── HomeFragment.java
    │       │   │       ├── ListFragment.java
    │       │   │       ├── LineDetailFragment.java
    │       │   │       ├── GalleryFragment.java
    │       │   │       ├── AboutFragment.java
    │       │   │       ├── DeveloperFragment.java
    │       │   │       └── Adapters.java
    │       │   └── res\
    │       │       ├── layout\
    │       │       ├── drawable\
    │       │       ├── values\
    │       │       └── xml\
    │       └── test/java/com/jnsub/quanchentrail/GeoTest.java
    ├── docs\
    │   ├── agent-prompt-v1.0.txt
    │   ├── osm-source\
    │   ├── timetable-source\
    │   ├── qa\
    │   ├── research\
    │   └── screenshots\
    ├── dist\
    │   └── 泉城轨迹-debug.apk
    ├── gradle\
    ├── local.properties
    ├── README.md
    └── HANDOVER.md
```

---

# 3. 构建环境

## 3.1 本地工具链

本地工具链位于：

```text
D:\Users\Lenovo\Desktop\JNSUB\.tools
```

推荐构建命令：

```powershell
$env:JAVA_HOME = 'D:\Users\Lenovo\Desktop\JNSUB\.tools\zulu17\zulu17.54.21-ca-jdk17.0.13-win_x64'
$env:ANDROID_HOME = 'D:\Users\Lenovo\Desktop\JNSUB\.tools\android-sdk'
$env:GRADLE_USER_HOME = 'D:\Users\Lenovo\Desktop\JNSUB\.gradle-user'

cd D:\Users\Lenovo\Desktop\JNSUB\QuanchenTrail
.\gradlew.bat test assembleDebug
```

调试 APK：

```text
D:\Users\Lenovo\Desktop\JNSUB\QuanchenTrail\app\build\outputs\apk\debug\app-debug.apk
```

交付 APK：

```text
D:\Users\Lenovo\Desktop\JNSUB\QuanchenTrail\dist\泉城轨迹-debug.apk
```

当前 APK SHA-256：

```text
DAA0CAE3D824E2702CFBBBB29B385B8450143C8F0EC4CAF15416A3BA6FA87060
```

## 3.2 技术栈

- 原生 Android Java
- AndroidX AppCompat
- Material Design 3 风格
- SQLiteOpenHelper
- Android LocationManager
- Android Photo Picker
- Android FileProvider
- JUnit 4
- compileSdk / targetSdk 35
- minSdk 26
- Java 17

不依赖 Google Play Services 地图或定位。

## 3.3 版本约束

用户明确要求：

```text
versionCode = 1
versionName = 1.0
```

除非用户再次明确要求，否则不要提升版本号。

---

# 4. 核心业务规则

## 4.1 定位与半径

输入：

```json
{
  "latitude": 36.65,
  "longitude": 117.10,
  "accuracy": 10,
  "timestamp": 1730000000000
}
```

处理：

1. 从 `stations` 表获取 `active` 车站；
2. 对所有活跃车站计算球面距离；
3. 取最近车站；
4. 默认半径 100 m；
5. 半径可在关于页调整，范围 50–500 m；
6. 只有最近车站进入半径时才生成打卡提示；
7. 济阳线和云巴与地铁采用同一套半径规则。

## 4.2 打卡入口

首页结构必须是：

```text
最近车站卡片
    ↓
长条“打卡区域”提示
    ↓
页面内打卡卡片
    ↓
确认打卡 / 带图打卡 / 稍后
```

重要规则：

- 长条“打卡区域”不能触发打卡；
- 不使用重复的打卡确认弹窗；
- 页面内卡片是唯一确认入口；
- 进入范围后自动滚动到页面内卡片；
- 系统通知点击后也只进入首页卡片；
- “稍后”会清除当前待确认动作；
- “确认打卡”执行正式 Agent 写入；
- “带图打卡”先选择现场拍摄或图库，再执行写入。

## 4.3 重复打卡

当前版本允许同一个站点重复打卡：

- 每次打卡生成新的 `checkin_group`；
- 一个 `checkin_group` 可对应多个换乘线路；
- 历史记录不被覆盖；
- 成就进度仍按站点去重计算；
- 图库按独立打卡组展示。

这是用户明确要求，虽然原始提示词中的 Agent 协议包含 `ALREADY_CHECKED_IN`，当前实现以用户最新需求为准。

## 4.4 换乘联动

`transfers.json` 中共 14 条换乘关系。确认一次换乘站打卡后，所有关联线路均写入同一 `checkin_group`。

例如：

- 彭家庄：1、2、4、云巴 1 号线；
- 济南东站：3、6、济阳线；
- 春博路：8、云巴 1 号线。

---

# 5. Agent 协议

核心实现位于：

```text
app\src\main\java\com\jnsub\quanchentrail\agent\CheckinAgent.java
```

Agent 六种结构化响应：

1. `NO_ELIGIBLE_STATION`
2. `ALREADY_CHECKED_IN`（当前重复打卡模式下基本保留协议兼容，但不阻止新打卡）
3. `SHOW_CHECKIN_PROMPT`
4. `CHECKIN_CONFIRMED`
5. `CHECKIN_CANCELLED`
6. `DATA_MISSING`

关键输出字段：

- `station`
- `line`
- `line_color`
- `distance_m`
- `nearby_pois`
- `transfer_note`
- `soft_warning`
- `checkin_radius_m`
- `affected_lines`
- `checkin_group`
- `newly_unlocked`
- `achievements`
- `photo_count`

软性提醒文字来源于系统提示词，不要随意删改。

---

# 6. 数据库

数据库类：

```text
app\src\main\java\com\jnsub\quanchentrail\data\AppDb.java
```

数据库名称：

```text
quanchen_trail.db
```

当前 `DB_VERSION = 5`。

## 6.1 主要表

### stations

```text
station_id
name
lines_json
lat
lng
system_type
operational_status
```

### transfers

```text
station_name
lines_json
```

### poi_mapping

```text
station_name
poi_name
category
```

### achievements

```text
achievement_id
name
condition_type
description
line
target
target_set_json
```

### lines

```text
line
name
system_type
color
route
target
status
sort_order
```

### checkins

```text
checkin_id INTEGER PRIMARY KEY AUTOINCREMENT
checkin_group TEXT NOT NULL
user_id TEXT NOT NULL
station_name TEXT NOT NULL
line TEXT NOT NULL
checked_in_at INTEGER NOT NULL
photo_count INTEGER NOT NULL DEFAULT 0
has_photo INTEGER NOT NULL DEFAULT 0
photo_uris_json TEXT NOT NULL DEFAULT '[]'
```

索引：

```text
idx_checkins_group
idx_checkins_station_time
idx_checkins_station
```

## 6.2 数据库迁移

重要：修改 `checkins` 结构时必须同步完成：

1. 新建迁移逻辑；
2. 增加 `DB_VERSION`；
3. 更新 `CREATE TABLE`；
4. 测试旧数据库升级；
5. 不要只更新建表 SQL，否则已有用户升级时会崩溃。

已有迁移历史：

- v1：基础表；
- v2：成就 `description`；
- v3：照片 `photo_uris_json`；
- v4：照片 `has_photo`；
- v5：`checkin_id` / `checkin_group`，支持重复打卡。

---

# 7. 首页与 UI 说明

## 7.1 首页

`HomeFragment.java` 负责：

- 定位状态；
- 最近 3 个车站；
- 车站周边 POI；
- 长条打卡区域；
- 页面内打卡卡片；
- Agent JSON 可选展示；
- 图片来源选择入口。

最近 3 个车站每项显示：

```text
序号
车站名称
周边地标 / POI
关联线路
距离
可打卡 / 已打卡 / 附近
```

## 7.2 页面内打卡卡片

卡片按钮：

- `确认打卡`
- `带图打卡`
- `稍后`

不要再调用旧的 `showCheckinDialog`。目前代码中已不存在该方法，不要恢复。

## 7.3 线路页面

`ListFragment.java` + `Adapters.LineAdapter`：

- 线路颜色；
- 起终点；
- 进度；
- 运营中 / 休息中 / 未开通；
- 首末班时间；
- 运营备注。

点击线路进入：

```text
LineDetailFragment
```

详情页显示：

- 线路状态；
- 首末班；
- 线路总进度；
- 每个站点打卡状态；
- 车站按官方先来后到顺序。

顺序来源：

```text
app\src\main\assets\line_station_order.json
```

## 7.4 成就页面

规则：

- `current = 0`：`未开始`
- `0 < current < target`：`进行中`
- `current >= target`：`已解锁`

已解锁成就：

- 排在最前；
- 在顶部显示徽章；
- 徽章包含成就图标与名称；
- 点击后显示：
  - 中文说明；
  - 进度；
  - 百分比；
  - 组成车站。

成就组成车站由 `AppDb.achievementStations()` 计算。

## 7.5 记录与图库

记录页入口按钮：

```text
记录 → 打开图库 → 打卡图库
```

图库显示：

- 站点；
- 打卡时间；
- 线路；
- 图片；
- 图片总数；
- 同一打卡组的多张照片；
- 点击图片可放大。

图片保存位置：

```text
files/checkin_photos/
```

## 7.6 关于页

包含：

- 版本号；
- 构建号；
- 数据时点；
- 定位权限；
- 通知权限；
- 打卡提醒开关；
- Agent JSON 开关；
- 打卡半径；
- 隐私说明；
- 时刻表来源；
- 清除打卡记录；
- Debug 开发者测试入口。

开发者入口只在 `BuildConfig.DEBUG == true` 时显示。

---

# 8. 开发者测试模块

入口：

```text
关于 → 开发者测试
```

正式 Release 中必须隐藏。

功能：

1. 测试通知；
2. 自定义经纬度；
3. 自定义定位精度；
4. 应用模拟位置；
5. 自动返回首页；
6. 模拟打卡；
7. 真实定位锁；
8. 点击首页状态卡恢复真实定位。

模拟位置会调用：

```java
handleLocation()
```

模拟打卡会调用：

```java
CheckinAgent.confirm()
```

因此开发者模式不能有另一套业务逻辑。若发现“开发者环境正常、真实环境异常”，必须优先排查：

- 权限；
- 时间；
- 数据版本；
- 真实定位更新；
- HyperOS 后台限制；
- 半径设置；
- 通知开关；
- 旧数据库迁移。

---

# 9. 通知规则

通知由 `MainActivity.notifyPrompt()` 生成。

触发条件：

```text
Agent 返回 SHOW_CHECKIN_PROMPT
```

控制因素：

- 系统通知权限；
- 关于页“打卡提醒”开关；
- 当前站点通知去重；
- 开发者模拟定位锁。

通知点击后：

- 打开首页；
- 显示页面内打卡卡片；
- 不弹确认弹窗。

---

# 10. 时刻表

时刻表数据位于：

```text
app\src\main\assets\lines.json
```

原始图片位于：

```text
docs\timetable-source\
```

主要参考来源：

```text
https://k.sina.com.cn/article_2287707595_885ba5cb02001k4hy.html
```

来源文章更新时间：2026-08-29。  
另有 2026-08-28 起延时运营说明：

```text
https://k.sina.com.cn/article_5328858693_13d9fee4502002hab8.html
```

当前关键时间：

- 1 号线：周五至周日往工研院方向 22:30；
- 2 号线：往彭家庄 22:19，往王府庄方向部分车站末班 23:11；
- 3 号线：每日往滩头 22:39，全程往机场南 22:06，往龙洞 23:10；
- 4 号线：06:00–22:30；
- 6 号线东段：06:00–22:30，部分反向站点至 23:04；
- 8 号线：周五至周日往清源大街 22:30；
- 济阳线：往济南东 20:50，往济阳北 21:00；
- 云巴：06:00–22:00。

时刻表是方向相关的。修改时不要只改一个 `last_train` 字段，必须同步：

- `schedule_note`；
- `last_train_weekend`；
- `LineDetailFragment`；
- 线路卡片；
- `lines.json`。

---

# 11. 车站数据

车站坐标来源：

```text
docs\osm-source\
```

使用 OpenStreetMap / OSM 对应线路站点节点整理，并根据提示词中的站名进行对齐。

当前数据规模：

- 139 个去重后的活跃车站；
- 1 号线 11 个官方站点；
- 2 号线 19 个；
- 3 号线 18 个；
- 4 号线 33 个；
- 6 号线东段 17 个；
- 8 号线 14 个；
- 济阳线 11 个；
- 云巴 1 号线 32 个。

---

# 12. 重要数据冲突

## 12.1 彭家庄与 1 号线

系统提示词中存在冲突：

- `transfers.json` 与换乘联动要求彭家庄属于 1 号线；
- 1 号线官方站点清单只有 11 站，且不含彭家庄；
- 若把彭家庄计入 1 号线，会形成“目标 11、实际 12”的矛盾。

当前处理：

- 1 号线线路详情使用官方 11 站顺序；
- 1 号线进度分母为 11；
- 换乘表仍保留彭家庄与 1 号线的联动；
- 代码中 `lineStations("1")` 使用 `line_station_order.json` 排除彭家庄；
- 换乘写入仍按 `transfers.json`。

如果后续需要彻底消除冲突，必须由用户确认采用哪一种业务规则。

## 12.2 时刻表方向差异

单个 `last_train` 字段无法完整表达方向差异。目前通过 `schedule_note` 展示方向细节。

---

# 13. 已知风险与限制

1. 项目当前主要在 Android 15 模拟器上验证，尚未完成真实小米 HyperOS 设备全流程验证。
2. 定位更新主要在 Activity 生命周期内运行，没有独立前台定位服务。
3. HyperOS 对后台定位、通知、自启动和省电策略可能影响实际表现。
4. 当前版本不收集网络数据，定位和照片均存本机。
5. `dialog_checkin.xml` 目前已无代码引用，属于历史遗留资源，可清理。
6. `docs/qa` 中存在少量临时数据库和 XML 命名不规范文件，不影响 App 运行。
7. Release 当前未配置正式签名密钥。
8. 图片 URI 已复制到应用私有目录，但尚未实现删除单张图片、图片压缩、云同步、缩略图缓存。
9. 当前没有滑动刷新、搜索、排序、导出图片等高级图库功能。
10. 尚未实现完整站点级逐站首末班列表；目前展示线路级/方向级关键时间和备注。

---

# 14. 测试状态

## 14.1 已完成

- `GeoTest` 单元测试通过；
- Debug 构建通过；
- Release 构建通过；
- Lint 0 errors；
- Android 15 模拟器启动无闪退；
- 最近 3 车站显示正常；
- POI 显示正常；
- 长条打卡区域不可直接打卡；
- 页面内打卡卡片显示正常；
- 重复打卡生成独立 `checkin_group`；
- 换乘联动写入；
- 图片来源弹窗；
- 图库显示；
- 成就中文说明；
- 成就状态；
- 成就徽章；
- 线路详情官方顺序；
- 通知权限设置；
- 开发者模拟定位；
- 开发者模拟打卡；
- Release 中 `DEBUG = false`；
- `versionCode = 1`，`versionName = 1.0`；
- 首末班车按方向分条显示，与时刻表原图一致；
- 多站同时在打卡范围内时，页面内多张打卡卡片同时显示，UI 结构一致；
- 确认其中一张卡片后，其他仍在范围内的卡片不被清空；
- 成就状态标签颜色区分已解锁（绿色）/未解锁（灰蓝色），RecyclerView 复用不串色；
- `全制式行者`（`system_types`）跨制式换乘联动打卡不计入该成就制式计数（如济南东站地铁+济阳线换乘，不会单独解锁济阳线制式）。

## 14.2 推荐下一轮测试

建议在真实设备完成：

1. 首次安装权限授权；
2. 离开再进入车站，检查系统通知；
3. 点击通知，确认只进入页面卡片；
4. 页面卡片完成打卡；
5. 同一站点重复打卡；
6. 换乘站联动；
7. 相机拍照 1–3 张；
8. 图库选择 1–3 张；
9. 图库查看和放大；
10. 杀死 App 后重新打开；
11. 锁屏 / 后台切换；
12. HyperOS 省电模式；
13. 关闭定位权限；
14. 关闭通知权限；
15. 修改打卡半径；
16. 线路运营中 / 休息中状态。

---

# 15. 调试方法

## 15.1 查看日志

```powershell
$adb = 'D:\Users\Lenovo\Desktop\JNSUB\.tools\android-sdk\platform-tools\adb.exe'
& $adb logcat -d AndroidRuntime:E *:F
```

查看应用自己的调试日志：

```powershell
& $adb logcat -d -s QuanchenTrail:D
```

## 15.2 查看数据库

Debug APK 可以通过 `run-as` 导出：

```powershell
python -c "import subprocess; adb=r'D:\Users\Lenovo\Desktop\JNSUB\.tools\android-sdk\platform-tools\adb.exe'; r=subprocess.run([adb,'exec-out','run-as','com.jnsub.quanchentrail','cat','databases/quanchen_trail.db'],capture_output=True); open(r'D:\Users\Lenovo\Desktop\JNSUB\test.db','wb').write(r.stdout); print(len(r.stdout))"
```

然后使用 Python `sqlite3` 查看。

## 15.3 启动错误页

如果 `MainActivity` 启动异常，会进入：

```text
activity_fallback.xml
```

错误同时写入应用内部：

```text
files/logs/crash.log
```

---

# 16. 新 Agent 工作守则

1. 先完整阅读 `README.md` 和本文档；
2. 修改数据库结构时必须写迁移；
3. 修改 Agent 协议时保持六种响应格式；
4. 不要恢复重复的打卡确认弹窗；
5. 不要把长条打卡区域改回可点击打卡；
6. 不要提升 `versionCode`；
7. 不要把项目移动到 C 盘；
8. 正式 Release 隐藏开发者测试入口；
9. 开发者测试必须调用真实业务逻辑；
10. 修改 UI 后至少执行：
    ```powershell
    .\gradlew.bat test assembleDebug lintDebug
    ```
11. 修改图片功能时测试相机与图库两条路径；
12. 修改打卡功能时测试首次打卡、重复打卡、换乘打卡；
13. 修改成就时测试 `0`、部分进度、已解锁三种状态；
14. 修改通知时测试权限开启和关闭；
15. 每次重大改动更新 `README.md` 和本文档。

---

# 17. 建议后续开发优先级

## P0：稳定性

- 真实小米 HyperOS 设备完整测试；
- 后台定位策略；
- 通知权限和电池限制；
- 数据库迁移回归；
- 重复打卡和图片写入压力测试。

## P1：产品完善

- 站点级逐站首末班车；
- 图片压缩、缩略图、删除单张；
- 打卡详情页；
- 图库搜索与按站点筛选；
- 导出图片或分享；
- 打卡记录编辑/删除；
- 成就完成动画与声音；
- 线路图页面。

## P1：数据质量

- 核实最新时刻表；
- 处理彭家庄与 1 号线冲突；
- 核实济阳线和云巴完整车站坐标；
- 建立数据版本和来源记录；
- 后续线路开通后更新 `stations`、`transfers`、`line_station_order`、`achievements`。

## P2：发布准备

- 正式签名；
- 隐私政策；
- 用户协议；
- 图标与商店截图；
- Release 测试；
- 版本发布说明。

---

# 18. 交接检查清单

新 Agent 开始工作前，请确认：

- [ ] 能在 `D:\Users\Lenovo\Desktop\JNSUB\QuanchenTrail` 打开项目；
- [ ] 能使用 `.\gradlew.bat test assembleDebug` 构建；
- [ ] 知道当前版本必须保持 `1.0 / 1`；
- [ ] 知道打卡只能在页面卡片完成；
- [ ] 知道同一站点允许多次打卡；
- [ ] 知道图片可由相机或图库选择；
- [ ] 知道开发者入口只在 Debug 显示；
- [ ] 知道数据库当前为 v5；
- [ ] 知道彭家庄与 1 号线存在原始数据冲突；
- [ ] 知道时刻表是方向相关数据；
- [ ] 知道所有新改动都要执行构建和 Lint；
- [ ] 知道所有项目文件继续放在 D 盘。

---

文档结束
