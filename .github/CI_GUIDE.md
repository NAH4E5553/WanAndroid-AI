# WanAndroid-AI CI 与 OpenCodeReview

## 当前状态和范围

已在本地适配：Android CI、分类脚本、配置回归测试、OCR 工作流和审查规则。
本配置只针对新项目，商城保持只读；首次提交包含工程骨架及 CI/OCR 配置，首次远端 Android CI 六项通过，main 分支保护已配置。
GitHub Secrets/Variables 的名称已核对齐全，未读取或输出 Secret 值。经用户确认代码上下文外发和费用边界后，仓库变量 OCR_ENABLED 已设为 true。
origin 已关联并上传至 https://github.com/NAH4E5553/WanAndroid-AI.git。
GitHub 首次 push 的 Android CI 六项检查已通过；PR #1 的六项必需检查及首次 OCR 真实模型调用也已通过，OCR 汇总为无发现、无行级评论。增量范围仍由后续 synchronize 运行持续核对。

## Android CI

唯一 Android 工作流为 `.github/workflows/android.yml`，不额外保留一份 android-ci.yml。

| 检查名称 | 执行内容 |
|---|---|
| CI Configuration | YAML/工作流契约测试、分类回归测试、actionlint、Bash 语法、变更分类 |
| Build | :app:assembleDebug |
| Unit Tests | testDebugUnitTest |
| Android Lint | lintDebug |
| Code Format | spotlessCheck |
| Architecture | verifyArchitecture |

五项 Android 检查以矩阵独立运行，fail-fast=false；单项失败不会取消其他诊断。
PR 指向 main、push main 或手动运行触发 CI。整条必需工作流不用路径过滤，避免长期 Pending。
配置检查始终执行。只有已知的 README.md、NOTICE.md、docs/**/*.md、.github/**/*.md 可以跳过 Android Gradle 任务；任何位置的 AGENTS.md 仍执行全检查。
源码、Gradle、规则、脚本、工作流、未知文件改动保守运行 Android 检查。纯 OCR 规则变更也运行，不把规则 JSON 当成普通说明文件。
首次 push 的空基线和手动运行执行全检查；无效 SHA、无法求 merge-base、空 diff 或分类失败不会被伪装成跳过成功。
PR 分类基于 merge-base 到 head；push 分类基于 before 到 head。关闭重命名识别，避免代码改名为文档后漏检；路径以 NUL 分隔。
CI Configuration 失败时，其余五项检查使用 always() 和显式状态校验一起失败，不静默跳过。

JDK 17；保留 setup-gradle 缓存，PR 只读缓存，main 可写。checkout 不持久化 Git 凭据。
测试、Lint 报告以及 Debug APK 可作为 Artifact 保留 7 天；这些不是正式签名发布产物。
main 已按 GitHub 实际显示名称将上述六项配置为必需检查，并要求 PR、分支同步和讨论解决；审批人数为 0，管理员不可绕过，禁止强推和删除。OCR 不设为必需检查。
目前不启用 merge queue；如果以后启用，需要补充 merge_group 事件和分类策略再验证。

## 本地验证

只在 WanAndroid-AI 目录执行以下命令。测试工具均安装在被忽略的 build/ci-tools 下，不进入 App。

```bash
python3 -m venv build/ci-tools/venv
build/ci-tools/venv/bin/python -m pip install -r .github/tests/requirements.txt
build/ci-tools/venv/bin/python .github/scripts/install_actionlint.py
build/ci-tools/venv/bin/python .github/scripts/prepare_ocr_action.py
build/ci-tools/venv/bin/python -m unittest discover -s .github/tests -v
build/ci-tools/actionlint -shellcheck= -pyflakes= .github/workflows/*.yml
bash -n .github/scripts/should-run-android-ci.sh
./gradlew verifyArchitecture spotlessCheck :app:assembleDebug testDebugUnitTest lintDebug --no-daemon --stacktrace
```

PyYAML 固定为 6.0.2，用于测试安全解析及重复键检测。actionlint 固定 1.7.7，下载包以代码中记录的官方 SHA-256 校验后，只提取名为 actionlint 的普通文件。
actionlint 的可选 shellcheck/pyflakes 集成关闭以保持本地和 CI 一致；Bash 另做语法检查，关键脚本逻辑由可执行回归测试覆盖。这不宣称完成所有 Shell/Python 静态分析。
分类测试在本项目 build/ci-tests 下创建临时 Git fixture 并清理，只提交虚构测试文件；不会提交当前工程，也不会操作商城 Git。
需要 Node.js 22 运行内存 Fake GitHub 评论测试；CI 显式安装 Node.js 22，均不进入 App。
prepare_ocr_action.py 只下载固定提交的两个公开源码文件并校验 SHA-256，输出到忽略的 build/ci-tools/ocr-action。
下载/校验/适配失败即停止，不执行原始上游 Action 兜底。离线验证可传 --source-dir，目录内的 action.yml 和 post-review-comments.js 仍必须通过同一哈希校验。
测试本身不访问真实账号、收藏、GitHub API 或模型；准备工具下载公开源码与 Action 安装需要网络。

## OCR 触发与安全边界

名称为 OpenCodeReview，仅在目标 main 的同仓库、非 Draft PR 创建/更新/重开/转 Ready 时考虑运行。
必须显式设置 OCR_ENABLED=true；变量未设置或 false 时不调用模型、不花费模型额度。
源码、Gradle、Manifest/资源、Schema、审查规则及 `.github` 非 Markdown 配置会触发 OCR；普通说明文档和 `.github/**/*.md` 不单独触发，避免无有效审查对象时产生无意义的评论发布失败。若同一提交还包含可审查代码或配置，仍正常触发。
不使用 pull_request_target，不接收评论命令，不审查外部 Fork，不添加历史审查工作流，不在 main push 上触发。
工作流默认 contents:read；只有 review job 获得 pull-requests:write，用于审查评论。
保持上游 Action 来源提交 8d023aafcec05f8ba5628fca3eaba88078e5d201 和 CLI 1.11.1，不使用 latest。
Action 自己负责安装并固定本次运行的 CLI；生成的 Install、Configure、Run 三个 OCR 调用步骤均设置 `OCR_NO_UPDATE=1`，禁止启动器在同一审查期间异步执行全局 npm 自升级。配置回归测试会扫描所有实际调用 `ocr` 的步骤，新增调用未携带该环境变量时直接失败。
不再直接执行原始远程 Action，而是运行经固定 SHA-256 校验与日志安全适配后生成的本地 Action。
工作流显式检出 PR 的 base.sha，适配脚本、哈希和规则只能来自该基线；不能执行 PR head 的准备脚本或本地 Action。
检出保留完整 Git 历史、不持久化 Git 凭据；审查读取 head SHA 的 Git 对象，不检出或构建 PR head。缺少对象或 merge-base 无法计算直接失败，不退回空范围。
这意味着第一次启用前须先把适配器及规则纳入 main。PR 对这些文件的修改，本轮只作为审查内容，合并后后续 PR 才使用新版本。
此边界不替代 main 分支保护、工作流变更人工审查及同仓库写权限管理。
审查语言中文，并发 2；启用汇总评论、增量审查和检查点范围，不上传原始 OCR 结果/错误 Artifact。
原始上游版本会输出审查 JSON/stderr，解析失败时还可能把 stderr 放入 PR 评论；只设置 upload_artifacts=false 不足以解决。
当前本地适配：配置命令输出和 review stderr 直接丢弃；review stdout 只写入 RUNNER_TEMP 下 mktemp 生成的私有目录（0700），JSON 文件权限 0600，不打印、不上传。
正常发布后由 always 步骤清理；review 失败/超时退出和可捕获的取消信号清理临时结果。只删除已验证位置的单个文件及空目录，不递归清理。
机器宕机/SIGKILL 不能保证清理逻辑运行；当前只用临时 GitHub-hosted runner，不据此宣称自托管机器无残留。OCR CLI 自身可能产生会话文件，也不上传或缓存这些文件。
JSON 解析错误、缺少完成标记、未完成审查只返回固定失败标签，不发布原始 stderr、result.message 或 warnings 正文；正常代码问题仍发布，warnings 保留数量和通用提示。
GitHub 评论 API 错误的正文也不进入日志或失败评论。真正的代码审查结论仍会进入 PR，这是 OCR 的功能，不是任意敏感信息的自动检测或全面脱敏保证。
固定版本升级时必须重新核对源码、哈希及适配点，并重跑回归测试；不允许只更新哈希跳过审核。
OCR 找到问题时不一定导致检查失败，工具成功不代表代码无缺陷。
配置缺失、非 HTTPS 模型地址、协议未明确 true/false 时在调用模型前失败，不打印配置值。

日志只提供固定诊断标签：

| 标签 | 处理方向 |
|---|---|
| OCR_PREPARE_FAILED | 检查公开源码下载、固定哈希及适配脚本；不要退回原始 Action |
| OCR_RANGE_FAILED | 检查 base/head Git 对象与完整历史 |
| OCR_CONFIG_FAILED | 检查配置和 CLI 版本，不打印配置值 |
| OCR_REVIEW_FAILED | 根据退出码检查配额、网络及超时；必要时在受控环境诊断，不能重新开启原始日志 |
| OCR_COMMENT_POST_FAILED | 检查审查是否完整、JSON 格式及 GitHub 评论权限 |
| OCR_CLEANUP_FAILED | 检查临时结果位置，不扩大删除范围 |

日志抑制的代价是故障详情更少；本地验证不能替代真实 PR 的首次接入验证。

## 新仓库启用（当前已启用）

在新仓库 Settings → Secrets and variables → Actions 单独设置：

| 类型 | 名称 | 说明 |
|---|---|---|
| Secret | OCR_LLM_URL | HTTPS 模型端点 |
| Secret | OCR_LLM_AUTH_TOKEN | 新项目模型凭据 |
| Variable | OCR_LLM_MODEL | 模型名称 |
| Variable | OCR_LLM_USE_ANTHROPIC | Anthropic 协议 true；OpenAI 兼容协议 false |
| Variable | OCR_ENABLED | 当前为 true；适配器已进入 main，且已确认外发/费用边界 |

优先使用新项目独立凭据，不从商城读取/复制 Secret；不要在聊天、Git、PR 或日志贴凭据。
启用前确认允许把审查所需 Diff/代码上下文发送给所配置的模型服务，设置额度并观察耗时。
停用只需将 OCR_ENABLED 改回 false；模型配置只能通过 GitHub Secrets/Variables，不在源码设置备用端点。
首次接入后需真实 PR 验证：规则读取、首次完整范围、后续增量、评论权限、失败诊断、Draft/Fork/开关跳过；不能假定本地测试已验证上游内部行为。
建议首次 OCR 完整审查前确保规则文件已可被该 PR 工作流所读取；若需要基线初始化，先核对远端现状，不覆盖已有提交。

## 审查规则调整

规则位于 `.opencodereview/rule.json`：只报告可证明的缺陷，不为消除评论盲目改代码。
移除商城支付专属规则和强制保留 Jetifier 的要求；不强制或禁止 MMKV，实际引入后按用途审查敏感数据保护。
重点覆盖 Cookie/换号隔离、分类/搜索/分页竞争、收藏 ID/状态一致性、Room 分表和升级、离线正文边界、WebView 安全。
只要求维护已存在的翻译，不强迫当前中文项目新增英文；不把明确的骨架占位当成新增缺陷。
证据充分的真实问题在同一 PR 修复并测试；误报记录理由。OCR 不替代确定性 CI、人工判断或设备回归。

## 后续交付顺序

1. 本地适配和验证（已完成）。
2. 工程骨架及安全适配器进入 main，首次 Android CI 六项通过（已完成）。
3. main 分支保护配置并回查（已完成）。
4. 通过真实功能 PR 验证 PR 必需检查（PR #1 已完成）。
5. 再次确认代码外发和费用边界后启用 OCR，并单独验证真实评论链路（PR #1 首次完整审查已完成；后续继续验证增量和失败路径）。

参考：

- https://github.com/alibaba/open-code-review/blob/8d023aafcec05f8ba5628fca3eaba88078e5d201/action.yml
- https://github.com/rhysd/actionlint/releases/tag/v1.7.7
- https://docs.github.com/en/pull-requests/how-tos/merge-and-close-pull-requests/troubleshooting-required-status-checks
