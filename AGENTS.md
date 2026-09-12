# WanAndroid-AI 开发规则

## 操作边界（最高优先级）

- 唯一开发目录：`/Users/sn/Desktop/workplace/WanAndroid-AI`。
- CoolMallKotlin 仅作只读参考：禁止修改、删除、移动、格式化、构建或执行 Git 写操作。复用文件先复制到本项目再修改。
- 不复制来源项目的 `.git`、签名、凭据、local.properties、build/、.gradle/、支付或社交登录 SDK。
- 操作前确认工作目录；保护已有文件，不执行强制推送或破坏性清理。

## 基线与复用

- 开发前查询 docs/模块使用手册.md、docs/基线差异清单.md 及已有调用方；现有能力适用时必须复用。
- 优先扩展或优化已有封装；新公共抽象必须有生产需求和实际调用方。同职责只保留一套权威实现。
- core:result 为纯 Kotlin/JVM 契约；core:common 只承载分页/状态，不依赖 data/network/database/navigation/ui。
- 固定上下文分页使用 PagingController；复杂页面组合控制器，禁止另存第二套列表、页码或请求任务。
- Feature Graph 注册自己的页面。普通导航携带捕获的 Host token 与来源 entry ID，不能绕开 Dispatcher 操作栈。

## 架构

- app 只负责 Application、Activity、根导航和 Feature 组装。
- feature 不依赖其他 feature 的实现，不允许 core 反向依赖 app/feature。
- 请求链路：Route/Screen → ViewModel → Repository → DataSource → Service。
- core:model 为业务契约；网络 DTO 在 core:network；Room Entity 在 core:database；Repository 负责转换。
- core:designsystem 不依赖业务、网络、导航或数据库。core:ui 只提供跨页面视觉组件。
- 不为保持商城目录一致而引入空模块、无用 SDK 或无约束公共工具。

## 代码分组与文件组织

新增和修改代码必须遵守以下约定，以实际职责决定归属，文件名后缀只作辅助判断。CoolMallKotlin 仅作组织方式参考，本项目的架构边界优先。

### Feature

| 包目录 | 职责与约定 |
|---|---|
| `view` | Screen 和对应 Route；Route 获取 ViewModel 并收集状态，Screen 负责渲染和事件回调 |
| `viewmodel` | 页面状态管理与事件处理；每个主要 ViewModel 独立文件 |
| `navigation` | Feature Graph 和页面路由注册，不承载页面业务逻辑 |
| `state` | UiState、页面事件及紧密相关的状态类型，不执行请求或操作 UI |
| `component` | Feature 内可独立使用或具有独立生命周期的 UI 组件 |
| `policy` | Feature 专属的判断、校验与安全策略 |

- Route 默认与对应 Screen 同文件；仅服务当前页面的短小私有组件可留在 Screen 文件，不要求每个 Composable 单独拆文件。
- 跨 Feature 的公共视觉组件放 `core:ui`；业务契约放 `core:model`，不得通过复制代码或 Feature 互相依赖实现复用。

### Core

| 模块 | 包目录与职责 |
|---|---|
| `core:data` | `repository` 仓储接口及实现；`datasource` 本机存储；`mapper` 响应与模型转换；`model` 存储专属类型；`di` 注入绑定 |
| `core:network` | `service` Retrofit 接口；`datasource` 网络数据源；`dto` 网络传输模型；`di` 网络配置及绑定 |
| `core:database` | `dao` 数据访问；`entity` Room 表实体；`model` 查询投影；`di` 数据库注入；数据库入口类保留根包 |
| `core:common` | `base/viewmodel` 列表基类；`base/state` 分页状态；`paging` 分页控制器 |
| `core:designsystem` | `theme` 颜色、形状、间距及主题入口 |
| `core:ui` | `component` 下按组件职责分组，如 `card`、`list`、`network`、`placeholder`、`scaffold` |
| `core:model` | 业务契约按类型独立文件；规模较小时保留根包，增长后按业务领域分组 |
| `core:navigation`、`core:result` | 职责集中且文件较少时保留根包，不为对齐其他模块强行增加层级 |

- `core:data/model` 只承载存储专属契约（如现有主题偏好），不能成为通用业务模型目录；网络 DTO、Room 实体和业务契约仍分别归属各自模块。
- DTO、Entity、DAO 等主要类型各自独立文件；仓储接口与对应默认实现、状态及紧密相关的小类型可以同文件，不机械拆分所有声明。

### 执行与验收

- 目录与 Kotlin `package` 一致，包目录采用小写；`test`、`androidTest` 按被测代码职责同步分组。
- 只在有实际代码时创建分组；优先使用已有明确职责的目录，不新建无边界的 `utils`、`manager` 或 `helper` 杂物包。
- 包迁移同步检查调用方、测试、完整类名引用、Manifest 和混淆配置；不得为解决引用问题扩大类型可见性或增加不必要的模块依赖。
- 纯组织重构保持行为、导航序列化名称、Room Schema 和 DataStore 文件名/键值兼容；涉及持久化标识变化时，必须另行提供兼容或迁移方案及验证。
- 代码变更运行本文件规定的完整验证入口；UI/导航相关迁移还需运行受影响的现有 UI 回归。纯文档变更检查差异和链接即可。
- 新增职责分组或采用例外时，在变更说明中解释原因，并同步更新本节和 README 的分组说明；不要让实现与规则长期不一致。

## 状态与并发

- Route 获取 ViewModel、使用 collectAsStateWithLifecycle；Screen 通过参数和事件渲染，不访问 Repository。
- 私有 MutableStateFlow，对外只读；需要原子更新的事实合并为不可变 UiState。
- 查询刷新取消旧工作，并用请求标识防止过期结果提交；分类状态按真实分类 ID 隔离。
- 页码只在成功后推进；加载更多去重；问答轮播只切换现有数据，不发网络请求。
- 重抛 CancellationException。禁止 GlobalScope、无主协程及在 ViewModel 中持有 Activity、WebView 或 Context。
- 收藏归账号；退出/切换账号使旧请求失效。历史及离线内容属于本机数据，退出不删除。
- 登录后回到原页，不自动重放收藏等写操作。

## 网络与安全

- HTTPS API 域名固定为 wanandroid.com；API Cookie 不发送到第三方阅读域名。
- errorCode 必须显式解析，不给成功码默认值。非零为业务失败，-1001 统一处理会话失效。
- 不记录密码、Cookie、Set-Cookie 或完整登录请求体；Release 禁用网络内容日志。
- 不默认启用 JavaScript bridge、文件访问或忽略 TLS 错误；外部 scheme 跳转需用户确认。
- 自动测试只用 Fake/固定虚构响应，不操作真实收藏或账户。

## Room 与离线

- 阅读历史与离线内容分表，保存正文到数据库；历史存在不表示离线可用。
- 在线阅读可用后尝试缓存，不阻塞阅读；仅保证成功缓存的正文，图片/视频/脚本不保证离线。
- 缓存正文净化，限制来源、字节数、并发、超时和总容量；不绕过登录或付费限制。
- 分页查询历史，按需加载正文；任务中断可恢复，禁止永久停留在缓存中。
- 独立删除历史/缓存。Schema 导出入库，升级提供 Migration 和测试，不使用破坏性迁移兜底。
- 凭据及敏感本地数据不进入系统备份。

## 构建与防劣化

- 版本统一在 gradle/libs.versions.toml，配置复用 build-logic；最低层使用 API 的模块声明 implementation。
- 新依赖必须说明用途、所属模块、替代方案和风险，不随迁移升级整个技术栈。
- 字符串资源化；支持深色、大字体、系统 Insets、触摸目标与无障碍。
- 功能修改配套成功、失败、空、取消、竞争测试；占位页面不计作已完成功能。
- 本地与 CI 同一入口：`./gradlew verifyArchitecture spotlessCheck :app:assembleDebug testAll lintDebug`。
- 准备发布额外验证 `:app:assembleRelease`，不复用商城签名。
- 只报告实际验证结果；未验证、环境受阻、待用户确认必须分别记录。

## CI 与自动审查

- CI/OCR 工作流、分类脚本、审查规则修改后，额外执行 .github/CI_GUIDE.md 中的配置回归测试、actionlint 和 Bash 语法检查。
- 唯一 Android CI 为 .github/workflows/android.yml；配置失败和未知变更必须保守处理，不让必需检查静默放行。
- OCR 默认关闭，仅作为同仓库非草稿 PR 的辅助审查，不设为强制合并门槛，也不自动修改代码。
- 移除商城支付专属约束，不要求保留 Jetifier；MMKV 可以按实际需要引入，不强制也不禁止，通用存储安全规则仍适用。
- OCR 模型凭据只配置到新仓库 Secrets/Variables，禁止复制商城凭据或在日志中输出；启用前确认代码上下文外发和费用边界。
- OCR 适配器与规则从 PR 基线 SHA 加载，不能执行 PR head 的适配脚本；固定上游源码 SHA-256，校验/准备失败必须停止，禁止退回原始 Action。
- OCR 原始响应和 stderr 不进入日志、Artifact 或错误评论；只发布正常审查结论和固定失败标签，生成结果文件在 always 清理步骤删除。

## Git

- 独立仓库、不关联商城远端。未明确要求不创建远端仓库或推送。
- 提交前检查 status、diff --check、暂存差异；不暂存密钥和无关内容。
- 提交类型：feat/fix/refactor/test/docs/chore(scope)。

## 第一版范围与约定

以 docs/第一版验收清单.md 为准。注册、找回密码、资料编辑、推送、批量后台下载和云端历史同步不在第一版。
遇到专题父分类 API 不可直接查询或正文缓存无法可靠支持时，先记录证据并确认方案，不能静默改变业务含义。
