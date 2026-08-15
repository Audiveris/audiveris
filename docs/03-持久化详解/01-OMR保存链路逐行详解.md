# .omr 保存链路逐行详解

## 一、入口链路总览

```
BookActions.save()  (UI层)
  │
  ├─ Batch模式下: SheetStub.swapSheet() 或 Book.store()
  │
  └─ 交互模式下: BookActions.saveAction()
        │
        └─ 用户选择保存路径/触发自动保存
              │
              ▼
           Book.store(bookPath, withBackup)    ← 核心入口
```

## 二、核心方法 `Book.store()` 完整代码 + 逐行释义

### 方法签名

**文件**: `Book.java`，行号 2437-2538

```java
public void store (Path bookPath, boolean withBackup)
```

### 代码与逐行释义

#### 第一步：预处理

```java
// 行 2446
Memory.gc();
```
- **功能**: 强制执行 Java 垃圾回收
- **设计意图**: 在序列化之前回收弱引用的 Glyph 对象，减少需要写入磁盘的数据量
- **输入**: 无
- **输出**: 堆内存尽可能被清理

```java
// 行 2448
boolean diskWritten = false;
```
- **功能**: 标记是否实际写入了磁盘
- **设计意图**: 避免"无变更保存"时产生误导性日志

#### 第二步：备份

```java
// 行 2451-2457
if (withBackup && Files.exists(bookPath)) {
    Path backup = FileUtil.backup(bookPath);
    if (backup != null) {
        logger.info("Previous book file renamed as {}", backup);
    }
}
```
- **功能**: 如果 withBackup 为 true 且文件已存在，先将原文件重命名为 `.bak`
- **输出**: `.omr.bak` 备份文件
- **调用依赖**: `FileUtil.backup()`

#### 第三步：获取锁

```java
// 行 2461-2463
getLock().lock();
checkRadixChange(bookPath);
```
- **功能**: 获取可重入锁，然后检查文件名的 radix 是否需要更新
- **设计意图**: 锁防止并发多线程同时写入同一个 .omr 文件

#### 第四步：打开 ZIP 文件系统

```java
// 行 2466-2473
if ((this.bookPath == null) || this.bookPath.toAbsolutePath().equals(
        bookPath.toAbsolutePath())) {
    // 同一路径：直接打开现有ZIP
    if (this.bookPath == null) {
        root = ZipFileSystem.create(bookPath);   // 新建（行2469）
        diskWritten = true;
    } else {
        root = ZipFileSystem.open(bookPath);      // 打开已有（行2472）
    }
}
```
- **关键**: 区分"新建ZIP"和"打开已有ZIP"两种场景
- `ZipFileSystem.create()` → `FileSystems.newFileSystem("jar:...", {"create":"true"})` → 自动创建空 ZIP
- `ZipFileSystem.open()` → `FileSystems.newFileSystem("jar:...", {})` → 打开已有 ZIP 供读写

#### 第五步：存储 Book 元数据

```java
// 行 2475-2478
if (isModified() || isUpgraded()) {
    storeBookInfo(root);   // 写入 book.xml
    diskWritten = true;
}
```
- **条件**: 仅当 Book 本身被修改或升级时才写入
- **调用**: `Book.storeBookInfo(root)` → 详见下文

#### 第六步：存储 Sheet

```java
// 行 2481-2487
for (SheetStub stub : stubs) {
    if (stub.isModified() || stub.isUpgraded()) {
        final Path sheetFolder = root.resolve(INTERNALS_RADIX + stub.getNumber());
        stub.getSheet().store(sheetFolder, null);
        diskWritten = true;
    }
}
```
- **功能**: 遍历所有 stub，仅保存被修改/升级的 sheet
- **路径**: `sheetNNN/` → `sheetNNN.xml` + `sheetNNN.png` + `sheetNNN-BINARY.png`
- **关键**: 只有 stub.modified==true 的 sheet 才保存，这是增量保存逻辑

#### 第七步：保存独立样本仓库

```java
// 行 2490-2492
if ((repository != null) && repository.isModified()) {
    repository.storeRepository();
}
```
- **功能**: 如果 Book 有独立的样本仓库且被修改，单独保存

#### 第八步：处理目标路径变更

```java
// 行 2494-2519
} else {
    // 路径变更：新建 ZIP
    root = ZipFileSystem.create(bookPath);
    storeBookInfo(root);

    // 从旧ZIP复制已有 sheet 文件夹
    final Path oldRoot = openBookFile(this.bookPath);
    for (SheetStub stub : stubs) {
        // 复制旧文件
        if (Files.exists(oldSheetFolder)) {
            FileUtil.copyTree(oldSheetFolder, sheetFolder);
        }
        // 覆盖被修改的 sheet
        if (stub.isModified() || stub.isUpgraded()) {
            stub.getSheet().store(sheetFolder, oldSheetFolder);
        }
    }
    oldRoot.getFileSystem().close();
}
```
- **功能**: 当用户执行"另存为"（保存到不同路径）时
- **核心**: 先从旧 .omr 复制所有现有数据，再覆盖新修改的部分

#### 第九步：收尾

```java
// 行 2522-2536
this.bookPath = bookPath;
if (diskWritten) {
    logger.info("Book stored as {}", bookPath);
}
// finally:
root.getFileSystem().close();    // 关闭 ZIP（实际写入磁盘）
getLock().unlock();
```
- **流关闭**: `root.getFileSystem().close()` 触发实际的磁盘写入

---

## 三、`storeBookInfo()` 完整代码 + 逐行释义

**文件**: `Book.java`，行号 2549-2566

```java
public void storeBookInfo (Path root) throws Exception
{
    // 行 2553-2557：处理版本号
    Version oldest = getOldestSheetVersion();
    if (oldest != null) {
        setVersionValue(oldest.value);
    }

    // 行 2559-2561：写入 XML
    Path bookInternals = root.resolve(BOOK_INTERNALS);  // → book.xml
    Files.deleteIfExists(bookInternals);                 // 先删旧文件
    Jaxb.marshal(this, bookInternals, getJaxbContext()); // JAXB 序列化

    // 行 2563-2565：重置标记
    setModified(false);
    bookUpgraded = false;
}
```

### `Jaxb.marshal()` 内部流程

**文件**: `Jaxb.java`，行号 128-141

```java
public static void marshal (Object object, Path path, JAXBContext jaxbContext)
    throws IOException, JAXBException, XMLStreamException
{
    try (OutputStream os = Files.newOutputStream(path, CREATE)) {
        Marshaller m = jaxbContext.createMarshaller();                         // 创建Marshaller
        XMLStreamWriter writer = new CustomXMLStreamWriter(
                XMLOutputFactory.newInstance().createXMLStreamWriter(os, "UTF-8")); // 创建XML写器
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);                // 设置格式化输出
        m.marshal(object, writer);                                            // 核心：对象→XML
        os.flush();
    }
}
```

**JAXB 序列化触发链**:
1. `Marshaller.marshal(book, writer)` → 从 Book 根元素开始
2. JAXB 发现 `@XmlElement(name="sheet") List<SheetStub> stubs` → 对每个 SheetStub 调用其 getter
3. `SheetStub` 上的 `@XmlAttribute`/`@XmlElement` 被逐一处理
4. 序列化 `@XmlList EnumSet<OmrStep>` 等特殊类型
5. XMLStreamWriter 输出到 .omr ZIP 内的 `book.xml`

---

## 四、`Sheet.store()` 完整代码 + 逐行释义

**文件**: `Sheet.java`，行号 1557-1587

```java
public void store (Path sheetFolder, Path oldSheetFolder)
{
    // --- 图片数据存储 ---
    if (picture != null) {
        try {
            Files.createDirectories(sheetFolder);
            picture.store(sheetFolder, oldSheetFolder);     // → 保存 PNG
        } catch (IOException ex) {
            logger.warn("IOException on storing " + this, ex);
        }
    }

    // --- 结构数据 JAXB 序列化 ---
    Path structurePath = sheetFolder.resolve(sheetFolder.getFileName() + ".xml");
    Files.deleteIfExists(structurePath);
    Files.createDirectories(sheetFolder);

    Jaxb.marshal(this, structurePath, getJaxbContext());     // JAXB 序列化 Sheet

    stub.setModified(false);
    stub.setUpgraded(false);
}
```

### `Picture.store()` 内部

**文件**: `Picture.java`，行号 1030-1063

```
遍历所有 ImageHolder:
  ├─ 如果被丢弃 → 删除文件
  └─ 保存 PNG → 如果成功，删除对应的旧 Table XML 文件
```

---

## 五、异常处理与资源释放

| 阶段 | 异常类型 | 处理方式 |
|------|----------|----------|
| ZIP 文件系统创建 | `IOException` | 外层 catch：日志警告，不阻止继续执行 |
| JAXB 序列化 | `JAXBException, XMLStreamException` | 外层 catch：日志并继续下一个操作 |
| PNG 写入 | `IOException` | 图片层 catch：不影响结构数据写入 |
| 流关闭 | `IOException` | finally 块中 `close()`，异常被忽略 |

**资源释放机制**:
```
Lock获取 → try块 → finally { 关闭ZIP; 释放锁; }
```
确保在任何异常情况下都能正确释放文件和锁资源。

---

## 六、保存流程总结图

```
Book.store(bookPath, withBackup)
  │
  ├── 内存清理：Memory.gc()
  │
  ├── 锁保护：getLock().lock()
  │
  ├── ZIP FS 准备：
  │   ├── [新建] ZipFileSystem.create(bookPath)  → 空ZIP
  │   └── [已有] ZipFileSystem.open(bookPath)    → 现有ZIP
  │
  ├── 元数据：storeBookInfo(root)
  │     └── JAXB.marshal(Book) → book.xml
  │
  ├── 页面数据：for每个stub（仅modified/upgraded）
  │     └── Sheet.store(sheetFolder, null)
  │           ├── Picture.store() → PNG
  │           └── JAXB.marshal(Sheet) → sheetNNN.xml
  │
  ├── 样本仓库：(可选) repository.storeRepository()
  │
  ├── [另存为] 从旧ZIP复制未修改的sheet数据
  │
  └── 收尾：ZIP.close() → 锁释放
```
