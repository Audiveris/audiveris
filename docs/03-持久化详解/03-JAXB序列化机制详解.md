# JAXB 序列化机制深度解析

## 一、JAXB 上下文初始化

### Book 的 JAXB 上下文

**文件**: `Book.java`，行号 2886-2895

```java
public static JAXBContext getJaxbContext () throws JAXBException
{
    // Lazy creation - 懒加载，线程安全由 volatile 保证
    if (jaxbContext == null) {
        jaxbContext = JAXBContext.newInstance(Book.class);
    }
    return jaxbContext;
}

private static volatile JAXBContext jaxbContext;  // 行145：volatile 关键字
```

- **初始化时机**: 首次调用 `Book.loadBook()` 或 `Book.storeBookInfo()` 时初始化
- **扫描范围**: 以 `Book.class` 为根入口，JAXB 自动发现所有通过 `@XmlElement`/`@XmlAttribute`/`@XmlElementRef` 引用的类型
- **上下文隔离**: Book 和 Sheet 各自有独立的 `JAXBContext` 实例

### Sheet 的 JAXB 上下文

**文件**: `Sheet.java`，行号 1611-1620

```java
public static JAXBContext getJaxbContext () throws JAXBException
{
    if (jaxbContext == null) {
        jaxbContext = JAXBContext.newInstance(Sheet.class);
    }
    return jaxbContext;
}
```

- 与 Book 的 JAXBContext **完全独立**，各自管理自己的类型层级

---

## 二、所有参与持久化序列化的核心实体类

### Book.xml 序列化链（JAXBContext: Book.class）

| 类 | 字段/元素名 | 说明 |
|------|------------|------|
| `Book` | `<book>` | 根元素 |
| `→ Book.version` | `@software-version` | 版本号属性 |
| `→ Book.build` | `@software-build` | 构建号属性 |
| `→ Book.path` | `@path` | 输入路径（通过 PathAdapter） |
| `→ Book.dirty` | `@dirty` | 脏标记（仅 true 时输出） |
| `→ Book.parameters` | `<parameters>` | BookParams 元素 |
| `→ Book.sheetsSelection` | `<sheets-selection>` | 页面选择字符串 |
| `→ Book.stubs` | `<sheet>` | SheetStub 列表（每个 stub 一个 `<sheet>` 元素） |
| `→ Book.scores` | `<score>` | Score 列表 |
| `SheetStub` | `<sheet>` | Sheet 存根 |
| `→ SheetStub.number` | `@number` | 编号 |
| `→ SheetStub.versionValue` | `@version` | 版本 |
| `→ SheetStub.invalid` | `@invalid` | 无效标记 |
| `→ SheetStub.sheetInput` | `<input>` | SheetInput 元素 |
| `→ SheetStub.parameters` | `<parameters>` | SheetParams |
| `→ SheetStub.doneSteps` | `<steps>` | 已执行步骤列表（`@XmlList` 空格分隔） |
| `→ SheetStub.pageRefs` | `<page>` | 页面引用列表 |
| `Score` | `<score>` | 乐谱 |
| `→ Score.id` | `@id` | ID |
| `→ Score.name` | `@name` | 名称 |

### sheetNNN.xml 序列化链（JAXBContext: Sheet.class）

| 类 | XML 映射 | 说明 |
|------|----------|------|
| `Sheet` | `<sheet>` | 根元素 |
| `→ Sheet.id` | `@id` | Sheet ID |
| `→ Sheet.picture` | `<picture>` | 图片元数据 |
| `→ Sheet.pages` | `<page>` | 页面列表 |
| `Picture` | `<picture>` | 图片信息（宽/高/缩放等，**不包含像素数据**） |
| `Page` | `<page>` | 逻辑页面 |
| `→ Page.systems` | `<system>` | SystemInfo 列表 |
| `SystemInfo` | `<system>` | 乐谱系统 |

---

## 三、序列化注解的使用规则

### `@XmlRootElement`

| 用法 | 代码 | 作用 |
|------|------|------|
| Book 类 | `@XmlRootElement(name = "book")` | 声明 `<book>` 为 XML 根元素 |
| Sheet 类 | `@XmlRootElement` | 声明 `<sheet>` 为 XML 根元素（无显式 name 则使用类名小写） |

### `@XmlAccessorType`

```java
@XmlAccessorType(XmlAccessType.NONE)
```

- **作用**: 严格控制序列化范围——只有显式标注了 `@XmlAttribute`/`@XmlElement`/`@XmlElementRef` 的字段才参与序列化
- **设计意图**: 防止误序列化瞬态字段或内部对象，同时允许某些字段 **故意不序列化**（如 Book 的 `lock`、`modified`）

### `@XmlAttribute`

用于**简单属性**的序列化，生成 XML 属性而非子元素：

```java
@XmlAttribute(name = "software-version")  // → <book software-version="5.7.0">
private String version;

@XmlAttribute(name = "number")            // → <sheet number="1">
private final int number;
```

### `@XmlElement`

用于**复杂对象或列表**的序列化，生成 XML 子元素：

```java
@XmlElement(name = "sheet")               // → <sheet>...</sheet><sheet>...</sheet>
private final List<SheetStub> stubs = new ArrayList<>();
```

### `@XmlList`

用于**枚举集合**的紧凑序列化：

```java
@XmlList                                    // → <steps>LOAD BINARY SCALE</steps>
@XmlElement(name = "steps")
private final EnumSet<OmrStep> doneSteps = EnumSet.noneOf(OmrStep.class);
```
- 不展开为多个子元素，而是空格分隔的列表
- 显著缩小 XML 文件体积

---

## 四、字段序列化的忽略规则与特殊处理

### 忽略规则

| 机制 | 说明 | 示例 |
|------|------|------|
| `@XmlAccessorType(NONE)` | 无注解的字段全不序列化 | `Book.lock`（ReentrantLock）不序列化 |
| `@XmlTransient` | 显式标记不序列化 | `Sheet.stub`（反向引用）不序列化 |
| `@Deprecated` + `@XmlElement` | 过期字段被保留用于向后兼容 | `Book.old_musicFamily` |
| `BooleanPositiveAdapter` | false/null 值不输出 | `dirty=false` 时 `<book>` 无 `dirty` 属性 |
| `beforeMarshal 剪枝` | 默认值参数设为 null 不输出 | `parameters` 全为默认时消失 |

### `beforeMarshal` / `afterMarshal` 钩子

Book 和 SheetStub 都实现了 JAXB 的序列化回调：

```java
// Book.java 行398-404
@SuppressWarnings("unused")
private void beforeMarshal (Marshaller m) {
    if ((parameters != null) && parameters.prune()) {  // 剪枝：删除默认值参数
        parameters = null;
    }
}

@SuppressWarnings("unused")
private void afterMarshal (Marshaller m) {
    parameters = parametersMirror.duplicate();           // 恢复镜像
}
```

- `beforeMarshal`: 序列化前执行，"剪枝"操作——如果所有参数都是默认值，将 `parameters` 设为 null，使其不被序列化
- `afterMarshal`: 序列化后执行，从 `parametersMirror` 恢复参数对象
- `parametersMirror`: 在 `setParamParents()` 时创建的参数副本（`Book.java:2325`）

---

## 五、引用处理与循环依赖解决方案

### 循环依赖结构

```
Book ←─────────────────────┐
  │                         │
  ├── List<SheetStub>       │
  │     └── Book book ──────┘  (反向引用, @XmlTransient)
  │
  └── SheetStub
        └── Sheet
              └── SheetStub stub ──┘  (反向引用, @XmlTransient)
```

### 解决方案

Audiveris 采用 **双向引用标记瞬态** 的策略：

1. **正向引用**（序列化方向）：Book → SheetStub（`@XmlElement`）
2. **反向引用**（不序列化）：SheetStub → Book（无注解，因 `@XmlAccessorType(NONE)` 而不序列化）
3. **重建方式**：通过 `initTransients()` / `afterReload()` 方法在加载完成后重建反向引用

具体代码示例：
```java
// Book.loadBook() → initTransients() → for each stub:
stub.initTransients(this);   // 设置 stub.book = this（重建反向引用）

// SheetStub.getSheet() → afterReload():
sheet.afterReload(this);     // 设置 sheet.stub = this（重建反向引用）
```

### JAXB 上下文隔离

- Book 和 Sheet 各自使用独立的 `JAXBContext`
- 这意味着 JAXB 在序列化 Book 时不会尝试解析 Sheet 的复杂结构
- Sheet 只在需要时才被单独序列化/反序列化

---

## 六、JAXB Adapter 机制

### `Jaxb.PathAdapter` — Path ↔ String 互转

```java
public static class PathAdapter extends XmlAdapter<String, Path> {
    public String marshal(Path path) { return path.toString(); }
    public Path unmarshal(String str) { return Paths.get(str); }
}
```

### `Jaxb.BooleanPositiveAdapter` — 仅输出 true

```java
public static class BooleanPositiveAdapter extends XmlAdapter<Boolean, Boolean> {
    public Boolean marshal(Boolean b) { return b ? true : null; }  // false→null 不输出
    public Boolean unmarshal(Boolean s) { return (s == null) ? false : s; }
}
```

### 精度控制适配器

| 适配器 | 精度 | 适用场景 |
|--------|------|----------|
| `Double1Adapter` | 1位小数 | 点坐标、曲线控制点 |
| `Double3Adapter` | 3位小数 | 矩形坐标、尺寸 |
| `Double5Adapter` | 5位小数 | 高精度测量数据 |
| `IntegerPositiveAdapter` | 仅正数 | 仅输出正整数值 |

---

## 七、JAXB 版本与依赖

```groovy
// app/build.gradle 行 77-81
com.sun.xml.bind:jaxb-core:2.3.0.1
com.sun.xml.bind:jaxb-impl:2.3.1
javax.xml.bind:jaxb-api:2.3.1
```

- **版本 2.3**: JAXB 2.x 是 Java EE 标准版，2.3.x 是最新的 JAXB 2 系列的维护版本
- **为何使用外部 JAXB**: Java 9+ 模块化系统（Jigsaw）将 JAXB 从 JDK 中移除，所以项目显式引入
- **OpenJFX 兼容性**: 使用 `com.sun.xml.bind` 而非 `org.glassfish.jaxb`（GlassFish 的 JAXB 实现）

---

## 八、序列化流程中的数据流图

```
保存时：
  Java对象 (Book/Sheet)
    → JAXB Marshaller
      → 遍历 @XmlAttribute → 写入属性
      → 遍历 @XmlElement → 递归子对象
        → 遇到 Adapter: 调用 marshal() 转换类型
      → XMLStreamWriter
        → Files.newOutputStream(path)
          → ZIP 文件系统中的 book.xml / sheetNNN.xml

加载时：
  ZIP中 book.xml / sheetNNN.xml
    → Files.newInputStream(path)
      → XMLStreamReader
        → JAXB Unmarshaller
          → 遍历 XML 元素/属性 → 设置 Java 字段
            → 遇到 Adapter: 调用 unmarshal() 转换类型
          → Java对象 (Book/Sheet)
            → initTransients() / afterReload() 重建瞬态字段
```
