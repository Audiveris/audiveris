# .omr 加载链路逐行详解

## 一、入口链路总览

```
用户操作（GUI文件选择 / CLI参数 / 最近文件）
  │
  ├─ BookManager.loadBook(bookPath)           ← 引擎层入口
  │     └─ Book.loadBook(bookPath)            ← 核心加载逻辑
  │
  └─ 或者：Book.loadBook(bookPath)             ← 直接静态调用
```

## 二、核心方法 `BookManager.loadBook()` 完整代码 + 逐行释义

**文件**: `BookManager.java`，行号 250-263

```java
public Book loadBook (Path bookPath)
{
    Book book = Book.loadBook(bookPath);    // ① 委托给静态方法

    if (book != null) {
        book.setBookPath(bookPath);          // ② 设置路径
        addBook(book);                       // ③ 注册到引擎

        getBookHistory().remove(new SheetPath(bookPath));  // ④ 从历史移除（关闭时再添加）
    }

    return book;
}
```

## 三、核心方法 `Book.loadBook()` 完整代码 + 逐行释义

**文件**: `Book.java`，行号 2955-3010

```java
public static Book loadBook (Path bookPath)
{
    final StopWatch watch = new StopWatch("loadBook " + bookPath);
    Book book = null;

    try {
        // === 第一步：文件存在性检查 ===
        // 行 2961-2966
        if (!Files.exists(bookPath)) {
            logger.warn("The file {} does not exist", bookPath);
            return null;
        } else {
            logger.info("Loading book {}", bookPath);
        }

        // === 第二步：打开 ZIP 文件系统 ===
        // 行 2971
        watch.start("book");
        Path rootPath = ZipFileSystem.open(bookPath);

        // === 第三步：定位 book.xml ===
        // 行 2974
        Path internalsPath = rootPath.resolve(BOOK_INTERNALS);

        // === 第四步：JAXB 反序列化 ===
        // 行 2976-2982
        try (InputStream is = Files.newInputStream(internalsPath, StandardOpenOption.READ)) {
            JAXBContext ctx = getJaxbContext();
            Unmarshaller um = ctx.createUnmarshaller();
            book = (Book) um.unmarshal(is);
            LogUtil.start(book);
            book.getLock().lock();
            rootPath.getFileSystem().close();

            // === 第五步：瞬态字段初始化 ===
            // 行 2984
            boolean ok = book.initTransients(null, bookPath);

            // === 第六步：兼容性检查 ===
            // 行 2986-2990
            if (!ok) {
                logger.info("Discarded {}", bookPath);
                return null;
            }

            return book;
        }
    } catch (IOException | JAXBException ex) {
        logger.warn("Error loading book " + bookPath + " " + ex, ex);
        return null;
    } finally {
        if (constants.printWatch.isSet()) {
            watch.print();
        }
        if (book != null) {
            book.getLock().unlock();
        }
        LogUtil.stopBook();
    }
}
```

### 逐行释义

| 行号 | 代码 | 详细说明 |
|------|------|----------|
| 2961 | `Files.exists(bookPath)` | 检查 .omr 文件是否存在。不存在则直接返回 null |
| 2971 | `ZipFileSystem.open(bookPath)` | **打开 ZIP 文件系统**。调用 `FileSystems.newFileSystem(URI.create("jar:" + uri), {}, null)`，这会创建一个指向 .omr ZIP 包的虚拟文件系统。返回根路径 |
| 2974 | `rootPath.resolve(BOOK_INTERNALS)` | 构造 `book.xml` 的完整路径。`BOOK_INTERNALS = "book.xml"` |
| 2976 | `Files.newInputStream(internalsPath, READ)` | 打开 book.xml 的输入流 |
| 2977 | `getJaxbContext()` | 获取/创建 JAXB 上下文。内部是 `JAXBContext.newInstance(Book.class)`，懒加载单例 |
| 2978 | `ctx.createUnmarshaller()` | 创建 JAXB 反序列化器 |
| 2979 | `(Book) um.unmarshal(is)` | **核心反序列化**：从 XML 输入流重建 Book 对象。JAXB 会递归处理所有 `@XmlElement`/`@XmlAttribute` 注解的字段，包括 stubs 列表、scores 列表等 |
| 2980 | `LogUtil.start(book)` | 开始日志记录跟踪 |
| 2981 | `book.getLock().lock()` | 获取 Book 锁，保护后续初始化操作 |
| 2982 | `rootPath.getFileSystem().close()` | **关闭 ZIP 文件系统**。注意：book.xml 已经读入内存（使用 try-with-resources 的 InputStream），关闭 ZIP 不影响已反序列化的 Book 对象 |
| 2984 | `book.initTransients(null, bookPath)` | **初始化瞬态字段**（最关键的步骤之一） |

---

## 四、`initTransients()` 完整代码 + 逐行释义

**文件**: `Book.java`，行号 1517-1608

```java
private boolean initTransients (String nameSansExt, Path bookPath)
{
    // === 参数初始化 ===
    // 行 1520-1521
    initParameters();          // 迁移旧参数、分配新参数结构
    setParamParents();         // 设置参数层级引用

    // === Stub 瞬态初始化 ===
    // 行 1523-1525
    for (SheetStub stub : stubs) {
        stub.initTransients(this);   // 每个 stub 设置 book 引用等
    }

    // === 别名处理 ===
    // 行 1527-1538
    if (alias == null) {
        final Path inputPath = getInputPath();
        if (inputPath != null) {
            alias = checkAlias(inputPath);
            if (alias != null) {
                nameSansExt = alias;
            }
        }
    }

    // === Radix 设置 ===
    // 行 1540-1549
    if (nameSansExt != null) {
        radix = nameSansExt.trim();
    }
    if (bookPath != null) {
        this.bookPath = bookPath;
        if (nameSansExt == null) {
            radix = FileUtil.getNameSansExtension(bookPath).trim();
        }
    }

    // === 版本处理 ===
    // 行 1552-1603
    if (build == null) {
        build = WellKnowns.TOOL_BUILD;        // 旧文件无 build → 使用当前 build
    }
    if (version == null) {
        version = WellKnowns.TOOL_REF;         // 旧文件无 version → 使用当前版本
    } else {
        // 版本兼容性检查
        final CheckResult status = Versions.check(new Version(version));
        switch (status) {
            case BOOK_TOO_OLD:
                // 书太旧 → 询问是否重置为二值化
                if (确认重置) {
                    resetTo(OmrStep.BINARY);
                    version = WellKnowns.TOOL_REF;
                    return true;
                }
                return false;
            case PROGRAM_TOO_OLD:
                // 程序太旧 → 提示用户更新
                return false;
            case COMPATIBLE:
                // 兼容 → 继续
        }
    }

    // === 升级检查 ===
    stubsToUpgrade = getStubsToUpgrade();

    return true;
}
```

### `stub.initTransients(book)` 内部（SheetStub.java:1154-1173）

```java
final void initTransients (Book book) {
    this.book = book;                       // 设置所属book引用
    setParamParents(book);                   // 设置参数层级
    if (!isValid()) {
        doneSteps.removeIf(...);             // 清理无效sheet的步骤记录
    }
    if (OMR.gui != null) {
        assembly = new SheetAssembly(this);  // 创建UI组件
    }
}
```

---

## 五、Sheet 懒加载流程 `SheetStub.getSheet()`

**文件**: `SheetStub.java`，行号 934-1009

```java
public Sheet getSheet ()
{
    if (sheet != null) return sheet;      // 已有 → 直接返回

    synchronized (this) {
        if (sheet != null) return sheet;  // 双重检查锁定

        if (!isDone(OmrStep.LOAD)) {
            // 从未加载过 → 从原始图像重新创建
            return sheet = new Sheet(this, null, false);
        }

        // === 从 .omr ZIP 加载 ===
        book.getLock().lock();
        try {
            sheetFile = book.openSheetFolder(number)
                        .resolve(Sheet.getSheetFileName(number));
            try (InputStream is = Files.newInputStream(sheetFile)) {
                sheet = Sheet.unmarshal(is);        // JAXB 反序列化
            }
            sheetFile.getFileSystem().close();

            sheet.afterReload(this);                // 重建瞬态引用
            setVersionValue(WellKnowns.TOOL_REF);   // 标记版本已更新
        } catch (IOException | JAXBException ex) {
            logger.warn("Error loading sheet structure");
            resetToBinary();                         // 降级：从头开始
        } finally {
            book.getLock().unlock();
        }

        return sheet;
    }
}
```

### `Sheet.unmarshal()` 内部

**文件**: `Sheet.java`，行号 1647-1660

```java
public static Sheet unmarshal (InputStream in) throws JAXBException
{
    Unmarshaller um = getJaxbContext().createUnmarshaller();
    Sheet sheet = (Sheet) um.unmarshal(in);     // JAXB → Java 对象
    return sheet;
}
```

---

## 六、向后兼容逻辑

### 版本检查 (`Versions.check()`)

**文件**: `Versions.java`，行号 179-191

```java
public static CheckResult check (Version version) {
    if (version.major > CURRENT_SOFTWARE.major)
        return PROGRAM_TOO_OLD;            // 书由更新的程序创建
    if (version.major == CURRENT_SOFTWARE.major && version.minor > CURRENT_SOFTWARE.minor)
        return PROGRAM_TOO_OLD;
    return COMPATIBLE;                     // 版本兼容
}
```

### 旧参数迁移 (`migrateOldParams()`)

**文件**: `Book.java`，行号 1786-1822

加载时自动将旧版独立的参数实例迁移到新的 `BookParams` 结构中：

```java
if (old_musicFamily != null) {
    upgradeParameters().musicFamily = old_musicFamily;
    old_musicFamily = null;        // 迁移后清空旧字段
}
// ... 同样处理 textFamily、inputQuality、beamSpecification 等
```

### 升级检测 (`getStubsToUpgrade()`)

**文件**: `Book.java`，行号 1284-1301

```java
public Set<SheetStub> getStubsToUpgrade () {
    stubsToUpgrade.addAll(getStubsWithOldVersion());  // 版本过旧
    stubsToUpgrade.addAll(getStubsWithTableFiles());  // 还有旧Table文件
}
```

---

## 七、异常处理

| 异常场景 | 处理方式 | 结果 |
|----------|----------|------|
| .omr 文件不存在 | `logger.warn()` → 返回 null | 加载失败 |
| JAXB 反序列化失败 | `catch (JAXBException ex)` | 返回 null |
| ZIP 文件格式错误 | `catch (IOException ex)` | 返回 null |
| 版本不兼容（书太旧且拒绝重置） | `initTransients` 返回 false | 丢弃此 .omr |
| 程序版本太旧 | 弹窗提示（GUI）/ 日志警告（批处理） | 不加载 |
| Sheet 反序列化失败 | 降级为 `resetToBinary()` | 从图像重新处理 |

---

## 八、加载流程总结图

```
BookManager.loadBook(bookPath)
  │
  └─ Book.loadBook(bookPath)
        │
        ├── 1. 文件存在性检查
        │
        ├── 2. ZipFileSystem.open(bookPath)
        │     └─ FileSystems.newFileSystem("jar:...", {}, null)
        │
        ├── 3. 读取 book.xml
        │     └─ Files.newInputStream(internalsPath)
        │
        ├── 4. JAXB 反序列化
        │     └─ um.unmarshal(is) → Book 对象
        │           ├─ stubs[] ← SheetStub列表（轻量，无Sheet数据）
        │           └─ scores[] ← Score列表
        │
        ├── 5. ZIP.close() → 读取完 book.xml 即可关闭ZIP
        │
        ├── 6. initTransients(null, bookPath)
        │     ├─ 参数初始化与迁移
        │     ├─ 版本兼容性检查
        │     ├─ 别名与radix设置
        │     └─ 升级需要检测
        │
        └── 7. 返回 Book（Sheet数据尚未加载）
              │
              └── [按需] stub.getSheet()
                    ├─ 未LOAD → new Sheet(this, null, false)
                    └─ 已LOAD → Zip → JAXB反序列化 → afterReload
```
