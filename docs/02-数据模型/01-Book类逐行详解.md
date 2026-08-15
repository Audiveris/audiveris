# Book 类逐行详解

## 文件信息

- **路径**: `app/src/main/java/org/audiveris/omr/sheet/Book.java`
- **行数**: 3075 行（包含内部类）
- **核心定位**: Book 是 Audiveris 中**顶级的数据模型根类**，对应一个物理输入文件（PDF/图像）经过 OMR 处理后产生的所有数据。一个 Book 包含多个 Sheet（每页一个），Sheet 再包含多个 SystemInfo。

## 在持久化流程中的角色

Book 是**整个 .omr 文件的根节点**。它通过 JAXB 序列化为 `book.xml` 存储在 .omr ZIP 包中。Book 的反序列化是加载 .omr 文件的第一个步骤。

---

## 核心字段说明

### 持久化字段（参与 JAXB 序列化）

| 字段 | 类型 | JAXB注解 | 作用 | 行号 |
|------|------|----------|------|------|
| `version` | `String` | `@XmlAttribute(name="software-version")` | 最后操作此书的 Audiveris 版本号 | 154 |
| `build` | `String` | `@XmlAttribute(name="software-build")` | 最后操作此书的构建号 | 158 |
| `alias` | `String` | `@XmlAttribute(name="alias")` | 书籍别名 | 162 |
| `path` | `Path` | `@XmlAttribute @XmlJavaTypeAdapter(Jaxb.PathAdapter.class)` | 输入图像路径，创建时记录 | 173 |
| `dirty` | `boolean` | `@XmlAttribute @XmlJavaTypeAdapter(BooleanPositiveAdapter.class)` | 标记是否需要更新乐谱，仅 true 时输出 | 178 |
| `parameters` | `BookParams` | `@XmlElement(name="parameters")` | 书级参数（二值化、谱间距等） | 185 |
| `sheetsSelection` | `String` | `@XmlElement(name="sheets-selection")` | 页面选择规则，如"1-3,5,10-12,30-" | 203 |
| `stubs` | `List<SheetStub>` | `@XmlElement(name="sheet")` | **核心**：所有 Sheet 存根列表 | 213 |
| `scores` | `List<Score>` | `@XmlElement(name="score")` | 逻辑乐谱（乐章）列表 | 222 |

### 废弃的持久化字段（向后兼容）

| 字段 | 类型 | 说明 | 行号 |
|------|------|------|------|
| `old_musicFamily` | `MusicFamily.MyParam` | 废弃，已迁移到 `parameters.musicFamily` | 278 |
| `old_textFamily` | `TextFamily.MyParam` | 废弃，已迁移到 `parameters.textFamily` | 283 |
| `old_inputQuality` | `InputQualityParam` | 废弃，已迁移到 `parameters.inputQuality` | 288 |
| `old_binarizationFilter` | `FilterParam` | 废弃，已迁移到 `parameters.binarizationFilter` | 293 |
| `old_beamSpecification` | `IntegerParam` | 废弃，已迁移到 `parameters.beamSpecification` | 298 |
| `old_ocrLanguages` | `StringParam` | 废弃，已迁移到 `parameters.ocrLanguages` | 303 |
| `old_switches` | `ProcessingSwitches` | 废弃，已迁移到 `parameters.switches` | 308 |

### 瞬态字段（不参与序列化）

| 字段 | 类型 | 作用 | 行号 |
|------|------|------|------|
| `lock` | `ReentrantLock` | 保护 .omr 文件读写的并发锁 | 228 |
| `radix` | `String` | 文件名（不含扩展名） | 231 |
| `bookPath` | `Path` | .omr 文件的磁盘路径 | 234 |
| `modified` | `boolean` | 数据是否已修改 | 252 |
| `repository` | `SampleRepository` | 书级样本仓库 | 255 |

---

## 核心方法逐行详解

### `store(Path bookPath, boolean withBackup)` — 保存 Book 到磁盘

**行号**: 2443-2538

```java
public void store (Path bookPath, boolean withBackup)
```

| 行号 | 代码 | 释义 |
|------|------|------|
| 2446 | `Memory.gc()` | 触发 GC 释放弱引用的 Glyph 对象，减少序列化数据量 |
| 2451-2457 | `if (withBackup && Files.exists(bookPath))` | 备份已存在的 .omr 文件（重命名为 .bak） |
| 2461 | `getLock().lock()` | 获取可重入锁，防止并发写入 |
| 2463 | `checkRadixChange(bookPath)` | 检查文件名是否变更，如有则更新 radix |
| 2466-2473 | 判断目标路径是否改变 | 同一文件 → `open`，不同文件 → `create`（复制旧数据） |
| 2469 | `ZipFileSystem.create(bookPath)` | 创建新 ZIP 文件系统（新建/另存为） |
| 2472 | `ZipFileSystem.open(bookPath)` | 打开已有 ZIP 文件系统（保存到原位置） |
| 2475-2478 | `storeBookInfo(root)` | 将 Book 元数据写入 `book.xml`，仅 modified/upgraded 时执行 |
| 2481-2487 | `for stub→sheet.store(sheetFolder, null)` | 遍历每个 stub，将修改过的 Sheet 写入 ZIP |
| 2490-2492 | `repository.storeRepository()` | 保存独立的样本仓库（如有修改） |
| 2494-2519 | **切换目标路径分支** | 从旧 .omr 切换到新 .omr：复制旧 sheet 文件夹，更新修改过的 sheet |
| 2522 | `this.bookPath = bookPath` | 更新 bookPath |
| 2530-2534 | `root.getFileSystem().close()` | 关闭 ZIP 文件系统，**关键**：所有数据在此刻写入磁盘 |
| 2536 | `getLock().unlock()` | 释放锁 |

### `storeBookInfo(Path root)` — 存储 Book 元数据

**行号**: 2549-2566

```java
public void storeBookInfo (Path root) throws Exception
```

| 行号 | 代码 | 释义 |
|------|------|------|
| 2553 | `getOldestSheetVersion()` | 找出所有 sheet 中的最低版本 |
| 2555-2557 | `setVersionValue(oldest.value)` | Book 版本设置为所有 sheet 的最低版本 |
| 2559 | `root.resolve(BOOK_INTERNALS)` | 定位到 `book.xml` |
| 2560 | `Files.deleteIfExists(bookInternals)` | 删除旧的 `book.xml` |
| 2561 | `Jaxb.marshal(this, bookInternals, getJaxbContext())` | **核心**：JAXB 序列化 Book 对象到 XML |
| 2563 | `setModified(false)` | 重置修改标记 |
| 2564 | `bookUpgraded = false` | 重置升级标记 |

**`getJaxbContext()`** 行号 2886-2895:
```java
if (jaxbContext == null) {
    jaxbContext = JAXBContext.newInstance(Book.class);
}
return jaxbContext;
```
→ 以 Book 类为根创建 JAXB 上下文，JAXB 会自动发现通过 `@XmlElement`/`@XmlAttribute` 引用的所有类。

### `loadBook(Path bookPath)` — 从 .omr 加载 Book

**行号**: 2955-3010

```java
public static Book loadBook (Path bookPath)
```

| 行号 | 代码 | 释义 |
|------|------|------|
| 2961-2966 | `Files.exists(bookPath)` 检查 | 文件不存在则返回 null |
| 2971 | `ZipFileSystem.open(bookPath)` | 打开 .omr 的 ZIP 文件系统 |
| 2974 | `rootPath.resolve(BOOK_INTERNALS)` | 定位到 `book.xml` |
| 2976-2982 | **JAXB 反序列化** | 创建 InputStream → Unmarshaller → unmarshal → Book 对象 |
| 2977 | `JAXBContext ctx = getJaxbContext()` | 获取 JAXB 上下文 |
| 2978 | `Unmarshaller um = ctx.createUnmarshaller()` | 创建反序列化器 |
| 2979 | `book = (Book) um.unmarshal(is)` | **核心反序列化**，SheetStub 列表在此重建 |
| 2981 | `LogUtil.start(book)` | 开始日志跟踪 |
| 2982 | `rootPath.getFileSystem().close()` | 关闭 ZIP（反序列化后即可关闭） |
| 2984 | `book.initTransients(null, bookPath)` | 初始化瞬态字段（关键步骤） |
| 2986-2990 | 不兼容则返回 null | 版本检查失败时丢弃此文件 |

### `store()` — 无参保存（简化入口）

**行号**: 2425-2432

```java
public void store ()
```
→ 检查 `bookPath` 是否定义，然后调用 `store(bookPath, false)`。

### `createStubs()` — 创建 SheetStub

**行号**: 549-566

```java
public void createStubs ()
```
→ 根据输入图像文件的页面数量创建对应数量的 `SheetStub` 实例，每个 stub 代表一页。

### `close(Integer sheetNumber)` — 关闭 Book

**行号**: 450-502

```java
public void close (Integer sheetNumber)
```
→ 设置 closing 标记 → 关闭所有 stub 的 UI → 关闭参数对话框 → 关闭浏览器 → 关闭 Score → 从引擎移除。

---

## JAXB 序列化注解说明

| 注解 | 作用 | 在 Book 中的应用 |
|------|------|-----------------|
| `@XmlRootElement(name="book")` | 声明 XML 根元素 | Book 类级别，`<book>` 为根标签 |
| `@XmlAccessorType(XmlAccessType.NONE)` | 严格控制序列化字段 | 只有带注解的字段才参与序列化 |
| `@XmlAttribute(name="software-version")` | 属性序列化 | version、build、alias、path、dirty |
| `@XmlElement(name="sheet")` | 子元素序列化 | stubs、scores、parameters |
| `@XmlJavaTypeAdapter(Jaxb.PathAdapter.class)` | 自定义类型转换 | Path ↔ String 互转 |
| `@XmlJavaTypeAdapter(Jaxb.BooleanPositiveAdapter.class)` | 仅输出 true | 节省 XML 空间 |

### `beforeMarshal` / `afterMarshal` 钩子

```java
// 行号 398-404
private void beforeMarshal (Marshaller m) {
    if ((parameters != null) && parameters.prune()) {
        parameters = null;  // 序列化前"剪枝"：如果参数为默认值则设为 null
    }
}

// 行号 349-352
private void afterMarshal (Marshaller m) {
    parameters = parametersMirror.duplicate();  // 序列化后恢复镜像副本
}
```

这套机制的目的是：如果所有参数都是默认值，就不在 `book.xml` 中输出 `<parameters>` 元素，从而**精简 XML 体积**。但为了不丢失内存中的参数对象，通过 `parametersMirror` 镜像来保存和恢复。

---

## 与其他核心类的关联关系

```
Book
├── List<SheetStub> stubs          ─── 轻量级 Sheet 代理，每个图像一页对应一个
│     └── Sheet sheet              ─── 完整 Sheet 数据（懒加载）
│           ├── List<SystemInfo>   ─── 系统（System = 乐谱的一行）
│           │     └── List<StaffInfo> ─── 五线谱
│           ├── SIGraph sig        ─── 符号解释图（Inter + Relation）
│           └── Picture picture    ─── 图像数据
├── List<Score> scores             ─── 逻辑乐谱（乐章）
└── BookParams parameters          ─── 书级参数（可层层覆盖到 Sheet、System 级别）
```

---

## 关键设计模式

1. **BookManager (单例)**: `BookManager` 实现 `OmrEngine` 接口，管理所有 Book 实例的集合
2. **懒加载**: Sheet 数据不在 Book 加载时读取，而是在 `stub.getSheet()` 时才从 .omr ZIP 中读取
3. **引用完整性**: Stub 持有对其所属 Book 的引用，Sheet 通过 Stub 获取 Book 引用
4. **参数层级**: BookParams → SheetParams，Sheet 参数可覆盖 Book 参数
