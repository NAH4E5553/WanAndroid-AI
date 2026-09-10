# CI / OCR 本地适配记录

日期：2026-09-10。

## 本轮变更

- 在原 android.yml 上整合，没有新增重复 Android 工作流。
- 五项 Android 检查独立矩阵执行，新增始终运行的 CI Configuration。
- 增加保守分类脚本，覆盖 merge-base、首次 push、删除、重命名和特殊文件名；分类失败不放行。
- 复制并适配 OCR 机制：固定 Action SHA 与 CLI 1.11.1，仅同仓库非 Draft PR，默认不启用。
- 规则聚焦 Cookie、分类/搜索请求竞争、收藏、Room 与 WebView；去掉商城支付专属和强制 Jetifier 约束，不强制或禁止 MMKV。
- PyYAML 6.0.2 与 actionlint 1.7.7 仅为配置验证工具，安装在被忽略的 build/ci-tools，不进入 App。
- actionlint 官方发布包经固定 SHA-256 校验，未修改系统 Python。

## 实际验证结果

- 配置回归测试 27 项通过：分类 15 项、工作流/规则/工具校验 12 项。
- actionlint 1.7.7 检查两份工作流，通过；Bash 语法检查通过。
- `./gradlew verifyArchitecture spotlessCheck :app:assembleDebug testDebugUnitTest lintDebug --no-daemon --stacktrace --console=plain --quiet` 成功，退出码 0。
- 原有 13 个 Kotlin 单元测试保留并通过；业务源码未修改。
- 本轮没有重新执行 Release 或真机回归，因为未改变业务源码、构建变体和生产依赖。
- 分类测试在新项目 build/ci-tests 中使用并清理虚构 Git fixture，未提交当前项目。

## 尚未执行与启用前核查（历史状态）

- 未提交、推送、建立 PR、配置仓库 Secrets/Variables 或分支保护。
- GitHub Runner 上的实际检查、OCR 真实模型调用、评论及增量范围尚未验证。
- 未设置 OCR_ENABLED；缺省条件不满足，不调用模型。
- 固定的上游 OCR Action 仍输出原始审查结果/stderr 到 Actions 日志，关闭 Artifact 不能关闭日志。启用前核查日志可见性和错误响应脱敏；不满足要求时先修正集成再启用。详见 .github/CI_GUIDE.md。
- 原商城保持只读；不复制、读取或显示其凭据。

## 后续：OCR 日志安全修复（2026-09-10）

上方为首次适配时的历史状态。本次在用户授权后完成以下本地修复，取代上方“原始 Action 日志风险待处理”的状态：

- 新增 prepare_ocr_action.py，固定来源提交不变，分别校验 Action 与评论辅助脚本的 SHA-256，再生成日志安全版本。下载或适配失败不回退。
- 适配器和审查规则仅从 PR base.sha 加载，head 仅作为 Git 审查对象；不执行 head 上的准备脚本。首次启用前必须让本配置进入 main。
- 抑制配置输出、review stderr、原始 JSON、API 异常正文及警告正文的日志/错误评论输出。正常审查结论继续发布。
- 结果放入 runner 私有临时目录；成功后 always 清理，失败及可捕获取消时清理，不上传原始 Artifact。
- JSON 无效或审查未完成时报固定失败标签，不生成误导性的成功总结、不推进检查点。
- 保留来源许可与本地修改说明；不更新生产依赖、不改变业务源码。

实际验证：

- 公开固定版本源码在线下载、SHA-256 校验和适配生成成功；离线同哈希路径也执行成功。
- Python 配置回归共 37 项通过（原 27 项 + 新增 10 项 OCR 安全测试）。
- 安全测试包含正常响应、非零退出码/超时退出码、TERM 取消、0600/0700 权限、清理越界拒绝、配置失败和无效 Git 对象。
- Node.js 22 内存 Fake GitHub 覆盖无效 JSON、未完成结果、正常评论、错误/warnings 隐藏、评论 API 失败、检查点推进与不推进、同 head 不重写总结。测试无网络或真实 API 写入。
- 生成的复合 Action 通过 YAML 重复键检查、各 Bash 步骤语法、内联 JavaScript 与评论辅助脚本语法检查。
- 两份工作流通过 actionlint 1.7.7；分类脚本通过 bash -n。
- `./gradlew verifyArchitecture spotlessCheck :app:assembleDebug testDebugUnitTest lintDebug --no-daemon --stacktrace --console=plain --quiet` 退出码 0。
- 原商城只读 Git 状态为空，未修改原项目；未执行提交、推送、PR 创建、远端配置或 DeepSeek 调用。

用户已反馈自行配置仓库变量与凭据；本地适配阶段未读取或核验具体值，也未更改启用开关。
当时剩余验证为实际 GitHub Runner、真实模型 API、评论及增量范围，待配置进入 main 并获得启用授权后执行。
限制：不保证进程被 SIGKILL/机器故障时能执行清理；不宣称模型结论具备完整敏感信息检测能力。详细边界见 .github/CI_GUIDE.md。

## 后续：首次远端 PR 验证（2026-09-10）

- 仅核对 OCR_LLM_URL、OCR_LLM_AUTH_TOKEN、OCR_LLM_MODEL、OCR_LLM_USE_ANTHROPIC、OCR_ENABLED 的名称存在；未读取或输出 Secret 值。
- 用户确认代码上下文外发和费用边界后，将 OCR_ENABLED 设为 true。
- PR #1 的 Android CI 六项必需检查全部成功；OpenCodeReview 真实调用成功。
- OCR 发布正常汇总，结论为无发现且没有行级评论；工作流配置仍禁止上传原始结果 Artifact，并保持固定错误标签与临时结果清理约束。
- 同一 PR 首次文档同步时，检查点正确选择上一提交到新提交的增量范围，模型命令退出码为 0，但评论发布以 `OCR_COMMENT_POST_FAILED` 保守失败；日志未打印原始输出，清理步骤成功。
- 该同步只包含 Markdown 验证记录。工作流现排除 `.github/**/*.md` 单独触发，同时继续覆盖 `.github` 下的工作流、脚本、测试及其他非 Markdown 配置；配置回归测试锁定此边界。
- OCR 仍未设为必需检查，且模型无发现不等于代码已被证明无缺陷；合并仍以确定性 CI、人工判断和分支保护为准。
