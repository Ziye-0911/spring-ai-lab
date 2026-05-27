# 贡献指南 / Contributing Guide

感谢你对 Spring AI Lab 的关注！本文档将帮助你了解如何参与贡献。

## 行为准则

- 尊重所有贡献者，保持友好、专业的交流环境
- 专注于技术讨论，避免人身攻击
- 欢迎新手提问，耐心解答

## 如何贡献

### 报告 Bug

1. 在 [Issues](../../issues) 中搜索，确认 Bug 未被报告过
2. 清晰描述：
   - 使用的版本（Spring AI Lab 版本、Spring Boot 版本、JDK 版本）
   - 复现步骤
   - 期望行为 vs 实际行为
   - 相关日志或截图

### 提交功能建议

1. 先搜索现有 Issues，确认未有人提过
2. 清晰描述功能需求和使用场景
3. 如有可能，提供伪代码或 API 设计草案

### 代码贡献

#### 开发环境

**环境要求：**
- JDK 17+
- Maven 3.9+
- IDE: IntelliJ IDEA (推荐)

```bash
# 克隆仓库
git clone https://gitee.com/sjz_zy/spring-ai-lab.git
cd spring-ai-lab

# 编译
mvn clean compile

# 运行测试
mvn test
```

#### 项目结构

```text
spring-ai-lab/
├── spring-ai-lab-core/          # 核心模块 — 接口定义和抽象实现
├── spring-ai-lab-document/      # 文档处理（PDF/Word/Markdown/HTML/TXT）
├── spring-ai-lab-test/          # 测试工具（Mock 组件 + 测试基类）
├── spring-ai-lab-scenario-*/    # 各场景模板（chat/rag/multi-agent/...）
├── spring-ai-lab-bom/           # BOM — 统一版本控制
└── docs/                        # 项目文档
```

#### 编码规范

1. **包命名**：`com.liziye.spring.ai.lab.{module}`
2. **代码风格**：
   - 使用 Lombok（`@Slf4j`、`@Data`、`@RequiredArgsConstructor`）
   - 接口优先，核心模块只定义抽象，实现可独立于接口模块
   - 命名清晰，使用完整的业务语义单词
3. **注释要求（JDK Javadoc 风格）**：
   - 类注释含 `@author` / `@since`，使用 `<p>` 分隔段落，`{@code}` / `{@link}` 引用
   - 所有 public/protected 方法必须有 `@param` / `@return` / `@throws`
   - 字段注释统一 `/** xxx */` 格式
   - `@Override` 方法不重复写注释（继承父类文档）
   - 设计决策用行内注释说明原因
4. **测试要求**：
   - 核心模块的接口实现必须有单元测试
   - 场景模板建议提供集成测试

#### Pull Request 流程

1. Fork 本仓库
2. 创建特性分支：`feature/your-feature-name` 或 `fix/your-bug-fix`
3. 编写代码 + 测试
4. 确保所有测试通过：`mvn test`
5. 提交 PR，使用 PR 模板描述变更
6. 等待 Code Review

#### Commit 规范

```
feat: 新功能
fix: 修复Bug
docs: 文档变更
refactor: 重构（不改变功能）
test: 测试相关
chore: 构建/工具变更
style: 代码格式
```

示例：
```
feat: 添加智能客服意图分类器
fix: 修复 ConversationMemory TTL 过期不清理的问题
docs: 更新 README 添加 MCP Server 模块说明
```

## 模块划分原则

- **Core 模块**只定义接口和抽象，不引入第三方实现库（如 pdfbox、poi）
- **Document 模块**包含所有文档加载器实现
- **场景模板**可独立运行，不依赖其他场景模块
- **第三方依赖**标记 `<optional>true</optional>`，通过 `@ConditionalOnClass` 条件装配

## 版本管理

- 所有依赖版本由根 POM 的 `<dependencyManagement>` 统一管理
- `spring-ai-lab-bom` 对外提供统一的版本控制

## 获取帮助

- 在 [Issues](../../issues) 中提问
- 查阅 [README.md](README.md) 了解完整的架构设计和使用指南

---

再次感谢你的贡献！🎉
