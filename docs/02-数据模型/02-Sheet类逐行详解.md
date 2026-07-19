# Sheet 类逐行详解

## 文件信息

- **路径**: `app/src/main/java/org/audiveris/omr/sheet/Sheet.java`
- **核心定位**: Sheet 对应一个输入图像文件中的**单页图像**，包含这页图像经过 OMR 处理后的全部识别结果。
- **在持久化流程中的角色**: Sheet 通过 JAXB 序列化为 `sheetNNN.xml`，存储在 .omr ZIP 包的 `sheetNNN/` 文件夹下。

---

## 核心字段说明

### 持久化字段（参与 JAXB 序列化）

由于 `@XmlAccessorType(XmlAccessType.NONE)`，仅以下带注解的字段参与序列化：

| 字段 | 类型 | JAXB注解 | 作用 |
|------|------|----------|------|
| `id` | `int` | `@XmlAttribute` | Sheet 的标识号（1-based） |
| `version` | `Version` | — | 版本信息（通过 getter 的 @XmlAttribute 序列化） |
| `picture` | `Picture` | `@XmlElement` | 图像元数据（宽/高、缩放等），**注意图片 PNG 不在此序列化** |
| `pages` | `List<Page>` | `@XmlElement(name="page")` | Page 列表，每个 Page 对应一个"逻辑页面" |

**注意**: Sheet 的 `stub`、`sheetId`、`skew` 等关键数据字段都标记了 `@XmlTransient` 或不带注解（因 `XmlAccessorType(NONE)` 而不被序列化），它们通过 `afterReload()` 方法重建。

### 瞬态字段

| 字段 | 类型 | 作用 |
|------|------|------|
| `stub` | `SheetStub` | 所属的 SheetStub 引用（反向引用，序列化时通过 Stub 的 sheet 关联） |
| `glyphIndex` | `GlyphIndex` | 字形索引 |
| `glyphsController` | `GlyphsController` | 字形 UI 控制器 |
| `sheetId` | `int` | 同 id，用于内部引用 |
| `skew` | `Skew` | 页面倾斜信息 |

---

## 核心方法逐行详解

### `store(Path sheetFolder, Path oldSheetFolder)` — 存储 Sheet 到 .omr

**行号**: 1557-1587

```java
public void store (Path sheetFolder, Path oldSheetFolder)
```

| 行号 | 代码 | 释义 |
|------|------|------|
| 1561-1571 | `if (picture != null) { picture.store(...) }` | 保存图片数据（PNG）到 sheet 文件夹，并清理旧的 Table XML 文件 |
| 1574-1586 | **JAXB 序列化 Sheet 结构** | 核心逻辑 |
| 1575 | `sheetFolder.resolve(sheetFolder.getFileName() + ".xml")` | 构造路径 `sheetNNN/sheetNNN.xml` |
| 1576 | `Files.deleteIfExists(structurePath)` | 删除旧的 XML 文件 |
| 1577 | `Files.createDirectories(sheetFolder)` | 确保文件夹存在 |
| 1579 | `Jaxb.marshal(this, structurePath, getJaxbContext())` | **核心**：将 Sheet 对象序列化为 XML |
| 1581 | `stub.setModified(false)` | 重置 stub 修改标记 |
| 1582 | `stub.setUpgraded(false)` | 重置 stub 升级标记 |

### `unmarshal(InputStream in)` — 从 XML 流反序列化 Sheet

**行号**: 1647-1660

```java
public static Sheet unmarshal (InputStream in) throws JAXBException
```

| 行号 | 代码 | 释义 |
|------|------|------|
| 1650 | `Unmarshaller um = getJaxbContext().createUnmarshaller()` | 创建 JAXB 反序列化器 |
| 1652-1654 | 可选设置日志监听器 | 调试用 |
| 1656 | `Sheet sheet = (Sheet) um.unmarshal(in)` | **核心反序列化** |
| 1657 | `logger.debug("Sheet unmarshalled")` | 日志记录 |

### `getJaxbContext()` — JAXB 上下文（懒加载单例）

**行号**: 1611-1620

```java
public static JAXBContext getJaxbContext () throws JAXBException {
    if (jaxbContext == null) {
        jaxbContext = JAXBContext.newInstance(Sheet.class);
    }
    return jaxbContext;
}
```

### `getSheetFileName(int number)` — 获取 Sheet XML 文件名

**行号**: 1631-1634

```java
public static String getSheetFileName (int number) {
    return Sheet.INTERNALS_RADIX + number + ".xml";  // 如 "sheet001.xml"
}
```

**常量定义**:
```java
public static final String INTERNALS_RADIX = "sheet";  // Sheet.java 中定义
```

---

## 序列化机制详解

### 序列化内容

Sheet 的序列化包括：
1. **元数据**: id、version
2. **Picture**: 图像宽高、缩放比例等（**不含 PNG 像素数据**）
3. **Pages**: 每个 Page 包含：
   - PageRef 引用
   - SystemInfo 列表（每个系统包含 StaffInfo 列表）
   - 系统间连接关系

### 反序列化后的重建步骤

```java
// SheetStub.getSheet() 中调用
sheet.afterReload(this);  // 行号 987
```

`afterReload()` 负责：
1. 重新设置 stub 反向引用
2. 重建 glyphIndex
3. 重连 SIGraph 中的 Inter 和 Relation 引用
4. 重建 UI 控制器等瞬态数据

---

## 与其他核心类的关联关系

```
SheetStub ───1→1─── Sheet
                      │
                      ├── Picture         ─── 图像元数据
                      │     └── Map<ImageKey, ImageHolder> ─── 图像数据（PNG）
                      │
                      ├── List<Page>     ─── 逻辑页面
                      │     └── List<SystemInfo> ─── 系统
                      │           └── List<StaffInfo> ─── 五线谱
                      │
                      ├── GlyphIndex     ─── 字形索引
                      │     └── List<Glyph> ─── 所有字形
                      │
                      └── SIGraph        ─── 符号解释图
                            ├── Set<Inter> ─── 解释（音符、休止符、谱号等）
                            └── Set<Relation> ─── 解释间关系
```

---

## 设计要点总结

1. **Stub-Sheet 分离架构**: SheetStub 是永久驻留内存的轻量代理，Sheet 是可按需加载/卸载的重量级对象。这是 Audiveris 能够处理大量页面而内存不溢出的关键技术。
2. **JAXB 双向引用处理**: Sheet 持有到 Stub 的引用 `this.stub`（瞬态），Stub 也持有到 Sheet 的引用 `this.sheet`（瞬态），通过 `afterReload()` 重建。
3. **非严格序列化控制**: `XmlAccessorType(NONE)` 确保只有明确注解的字段才被序列化，默认不序列化所有字段。
