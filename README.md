# WanAndroid-AI

包名：`com.personal.wanandroid`。独立于 CoolMallKotlin 的 Android 多模块项目。

## 当前阶段

**阶段 4：登录会话已通过 PR #10 合并；收藏功能本地实现与自动验证完成，待真机验收。不是第一版完整功能交付。**

已建立：版本目录、Convention Plugins、Hilt、Navigation3 根导航、四套浅深主题、主题偏好持久化、首页/专题/我的入口、网络 DTO/Service/DataSource/Repository、Room 分表与导出 Schema、基础架构检查和 CI 配置。首页已接入真实文章列表与最新 5 条问答轮播；“查看更多”进入支持分页的每日一问列表。已完成 CoolMallKotlin 基线封装迁移，统一结果、分页、公共 UI 和 Feature Graph。文章路由已接入 HTTPS 在线阅读、进度、错误重试、网页历史返回及外部跳转确认。

基线迁移已合并，用户已确认其真机专项验收完成。本次在线阅读新增 12 个单元测试，9 个 Android 15 模拟器测试全部通过，覆盖固定虚构网页、返回分发、外部 Intent 分类与确认界面。2026-09-12 用户确认本次在线阅读真机验收完成；该确认独立于此前基线迁移验收。Release 尚未针对本次改动复验。

搜索功能已完成本地实现：输入与键盘提交、关键词切换隔离、分页/空结果/失败重试，以及阅读返回保留已提交关键词和列表位置。编辑输入期间隐藏旧结果，提交后再显示。搜索页按参考布局展示圆角搜索栏及历史/推荐标签；本机持久保留最近 20 条去重历史，支持清空；推荐词来自 hotkey/json。搜索与首页共用文章卡片和分页控制器；搜索相关新增 23 个单元测试及 13 个 Android 15 固定响应设备测试均通过，包含深色大字体布局，待本次搜索真机验收。

专题页面已完成本地实现：左侧只显示一级分类，点击选中及圆角背景参考 CoolMallKotlin；右侧顶部仅展示接口返回的真实二级分类标签，支持点击与文章区域横滑切换，到底不会切换下一个一级分类。默认加载第一个二级分类，以子分类 cid 请求文章；分类按真实 ID 独立保存分页和滚动位置，阅读/Tab 返回保留状态。二级标签离开第一个真实分类后侧栏左滑淡出、右侧扩展全宽，回到第一个真实分类时反向恢复，保留两侧滚动位置。11 个专题单元测试和 19 个 Android 15 固定响应专题 UI 测试通过；专题真机验收待完成。

当前完整验证入口已通过：Debug 构建、179 个单元测试、Spotless、架构检查及 Lint。登录会话阶段 Android 15 固定数据 UI 回归 56 条通过；登录页改版及手机号校验后 8 条登录 UI 回归通过；PR 审查修复后登录 8、个人中心 3、加密存储 5，共 16 条受影响设备回归再次通过。收藏阶段新增 24 个单元测试；设备回归覆盖个人中心 7、阅读器 12、登录 8，共 27 条，包含深色大字体。阅读页三点菜单调整后，15 条阅读器设备回归通过；审查修正后个人中心 7 条、阅读器 15 条再次通过。

界面上的“待接入”是明确的占位状态，不是网络加载失败或真实数据。
登录与会话管理本轮接入：账号密码表单、会话恢复与校验、退出和失效处理；阅读历史页面和正文缓存尚未完成。在线阅读只恢复安全当前 URL，不保证重建后的完整网页历史、表单或滚动位置；第三方网页暗色效果受页面与 WebView 能力限制。搜索进程恢复会重新请求已提交关键词的首屏，不保证恢复所有已加载分页。

登录账号按用户要求使用中国大陆手机号，校验归属 `feature/auth/policy`，由 UiState 和 ViewModel 复用；进入密码框及提交前检查格式，仍通过服务端 username 字段传输。注册、忘记密码和协议页面目前仅提供占位提示。
多配色主题选择提供四套配色与三种显示模式，默认“石板蓝 + 跟随系统”。

收藏入口位于“我的 → 我的收藏”，支持分页、刷新、取消收藏及阅读跳转；文章阅读页右上角三点菜单依次提供刷新、外部打开、收藏/取消收藏。状态由账号会话版本隔离的 CollectionRepository 统一管理，列表与阅读页共享变更；取消收藏后重新从第一页加载，避免删除引起分页偏移。游客登录成功后进入目标页面或返回原文章，不自动重放收藏写操作。

首次确认文章收藏状态及写入结果不确定时，只读查询收藏列表；最多查询 50 页、30 秒，未确认仍显示“确认收藏状态”，不会将部分查询中未找到的文章当作未收藏。内部文章取消使用文章 ID，收藏列表取消使用收藏记录 ID 与 originId；已有外部收藏可阅读和取消，但本版不新增外部收藏。

## 开发入口

1. Android Studio 打开本目录（不要打开或改动来源商城工程）。
2. 使用 JDK 17、Android SDK 36；本机 SDK 路径在被 Git 忽略的 local.properties。
3. 选择 app，运行 debug 变体。

```bash
./gradlew verifyArchitecture spotlessCheck :app:assembleDebug testAll lintDebug
```

仅格式化新工程：`./gradlew spotlessApply`。
Release 验证：`./gradlew :app:assembleRelease`，默认不配置正式签名。

CI 使用相同 Gradle 检查，另有配置回归测试和工作流语法校验；见 [.github/CI_GUIDE.md](.github/CI_GUIDE.md)。
origin 指向 https://github.com/NAH4E5553/WanAndroid-AI.git；初始版本包含工程骨架、CI/OCR 配置及验证文档。
首次远端 CI 的六项检查均已通过；main 已要求 PR、分支同步、讨论解决及六项必需检查，并禁止管理员绕过、强推和删除。
OCR 工作流、规则与日志安全适配器已就绪；经用户确认代码上下文外发和费用边界后，仓库已启用 OCR，并在 PR #1 完成首次真实模型审查，结果为无发现。OCR 仍是辅助检查，不是必需合并门槛。
本工程只保留 debug/release；不使用来源商城的 devDebug/prodRelease 任务。

## Feature 代码分组

后续开发须遵守 [AGENTS.md 的代码分组与文件组织规则](AGENTS.md#代码分组与文件组织)，下文为现有结构说明。

各 feature 在自己的 Kotlin 包下按职责组织，目录与 package 保持一致：

| 目录 | 职责 |
|---|---|
| `view` | 页面及对应 Route，保留页面私有的小组件 |
| `viewmodel` | 页面状态管理与事件处理，一个主要 ViewModel 对应一个文件 |
| `navigation` | Feature Graph 和页面路由注册 |
| `state` | 页面 UiState、页面事件及相关状态类型 |
| `component` | 模块内部独立 UI 组件，如阅读器 WebView |
| `policy` | 模块专属策略，如阅读链接的安全校验 |

仅在有对应代码时创建目录；`test`、`androidTest` 的包路径与被测代码职责对应。
跨模块公共 UI 继续放 `core:ui`，业务契约继续放 `core:model`，不因分组搬入 feature。

## Core 代码分组

参考 CoolMallKotlin 按职责组织，保留本项目的模块依赖和调用链：

| 模块 | 内部分组 |
|---|---|
| `core:data` | `repository` 仓储接口及实现、`datasource` 本机存储、`mapper` 响应转换、`model` 主题偏好存储契约、`di` 注入绑定 |
| `core:network` | `service` Retrofit 接口、`datasource` 网络数据源、`dto` 网络传输模型、`session` 会话快照与存储接口、`interceptor` 会话拦截、`di` 网络配置及绑定 |
| `core:database` | `dao` 数据访问、`entity` 表实体、`model` 查询投影、`di` 数据库注入；数据库入口留在根包 |
| `core:common` | `base/viewmodel` 列表基类、`base/state` 分页状态、`paging` 分页控制器 |
| `core:designsystem` | `theme` 颜色、形状、间距及主题入口 |
| `core:ui` | `component` 下按 `card`、`list`、`network`、`placeholder`、`scaffold` 组织公共组件 |
| `core:model` | Article、Topic、PageResult、SearchHistory、CollectionItem/Target/Snapshot 保留根包，账号契约按领域放 `auth` |
| `core:navigation`、`core:result` | 职责集中且文件较少，保留根包，不创建空分组 |

测试包按被测职责同步分组。导航序列化类型的包名、数据库类名、Room 表结构、DataStore 文件名及键值保持不变。
网络 DTO 仍属于 `core:network`，Room 实体仍属于 `core:database`，不复制商城的请求/响应模型到业务契约模块。

## 文档

`docs/` 为本地开发与验收资料，已从 Git 跟踪中移除；下列 docs 链接仅在本机资料存在时可用。

- [开发规则](AGENTS.md)
- [架构与迁移方案](docs/架构与迁移方案.md)
- [基线差异清单](docs/基线差异清单.md)
- [模块使用手册](docs/模块使用手册.md)
- [新增功能开发指南](docs/新增功能开发指南.md)
- [基线迁移验证记录](docs/基线迁移验证记录.md)
- [第一版验收清单](docs/第一版验收清单.md)
- [验证记录](docs/验证记录.md)
- [主题选择开发方案](docs/主题选择开发方案.md)
- [来源说明](NOTICE.md)
- [CI 与 OCR 配置及验证指南](.github/CI_GUIDE.md)
- [CI/OCR 本地适配验证记录](docs/CI-OCR本地适配记录.md)

## 下一阶段

按用户授权提交收藏功能 PR；收藏真实账号专项，以及登录、搜索和专题此前未记录的真机验收项仍需补齐。历史和离线缓存按独立阶段实施；不要将占位页面算作已完成的功能。
