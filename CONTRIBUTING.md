# Contributing Guide

Thank you for your interest in Spring AI Lab! This document will help you get started with contributing.

## Code of Conduct

- Respect all contributors and maintain a friendly, professional environment
- Focus on technical discussions; avoid personal attacks
- Newcomer questions are welcome — answer with patience

## How to Contribute

### Reporting Bugs

1. Search [Issues](../../issues) to confirm the bug hasn't been reported
2. Describe clearly:
   - Versions used (Spring AI Lab, Spring Boot, JDK)
   - Steps to reproduce
   - Expected behavior vs. actual behavior
   - Relevant logs or screenshots

### Suggesting Features

1. Search existing Issues first to avoid duplicates
2. Clearly describe the feature and its use case
3. If possible, provide pseudo-code or a draft API design

### Code Contributions

#### Development Environment

**Prerequisites:**
- JDK 17+
- Maven 3.9+
- IDE: IntelliJ IDEA (recommended)

```bash
# Clone the repository
git clone https://github.com/Ziye-0911/spring-ai-lab.git
cd spring-ai-lab

# Compile
mvn clean compile

# Run tests
mvn test
```

#### Project Structure

```text
spring-ai-lab/
├── spring-ai-lab-core/          # Core module — interfaces and abstract implementations
├── spring-ai-lab-document/      # Document processing (PDF/Word/Markdown/HTML/TXT)
├── spring-ai-lab-test/          # Test utilities (Mock components + test base classes)
├── spring-ai-lab-scenario-*/    # Scenario starters (chat/rag/multi-agent/...)
├── spring-ai-lab-bom/           # BOM — unified version control
└── docs/                        # Project documentation
```

#### Coding Standards

1. **Package Naming**: `com.liziye.spring.ai.lab.{module}`
2. **Code Style**:
   - Use Lombok (`@Slf4j`, `@Data`, `@RequiredArgsConstructor`)
   - Interface-first: the core module defines only abstractions; implementations are independent
   - Clear naming with full business-semantic words
3. **Comments (JDK Javadoc style)**:
   - Class comments include `@author` / `@since`; use `<p>` to separate paragraphs, `{@code}` / `{@link}` for references
   - All public/protected methods must have `@param` / `@return` / `@throws`
   - Field comments use `/** xxx */` format
   - `@Override` methods do not repeat comments (inherit from parent)
   - Design decisions explained with inline comments
4. **Testing Requirements**:
   - Core module interface implementations must have unit tests
   - Scenario starters are recommended to provide integration tests

#### Pull Request Workflow

1. Fork this repository
2. Create a feature branch: `feature/your-feature-name` or `fix/your-bug-fix`
3. Write code + tests
4. Ensure all tests pass: `mvn test`
5. Submit a PR, using the PR template to describe changes
6. Wait for Code Review

#### Commit Convention

```
feat: new feature
fix: bug fix
docs: documentation changes
refactor: refactoring (no functional change)
test: testing related
chore: build/tooling changes
style: code formatting
```

Examples:
```
feat: add customer service intent classifier
fix: resolve ConversationMemory TTL expiry cleanup issue
docs: update README with MCP Server module description
```

## Module Partitioning Principles

- **Core module** defines only interfaces and abstractions; no third-party implementation libraries (e.g., PDFBox, POI)
- **Document module** contains all document loader implementations
- **Scenario starters** are independently runnable; no cross-scenario dependencies
- **Third-party dependencies** are marked `<optional>true</optional>`, with `@ConditionalOnClass` conditional assembly

## Version Management

- All dependency versions are centrally managed by the root POM `<dependencyManagement>`
- `spring-ai-lab-bom` provides unified version control for external projects

## Getting Help

- Ask questions in [Issues](../../issues)
- Check [README.md](README.md) for the full architecture design and usage guide

---

Thanks again for your contribution! 🎉
