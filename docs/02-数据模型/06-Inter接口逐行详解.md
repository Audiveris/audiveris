# Inter 接口逐行详解

## 文件信息

- **路径**: `app/src/main/java/org/audiveris/omr/sig/inter/Inter.java`
- **核心定位**: Inter（Interpretation 的缩写）是 SIGraph 中的**基本节点**，代表一个音乐符号的解释。例如一个音符、休止符、谱号、调号等都是一个 Inter 实例。

## 在持久化流程中的角色

Inter 是 `sheetNNN.xml` 中最细粒度的持久化实体。SIGraph 中的 Inter 和 Relation 共同构成完整的 OMR 识别结果。

---

## 核心字段说明

| 字段 | 类型 | 作用 |
|------|------|------|
| `id` | `int` | Inter 全局唯一标识 |
| `shape` | `Shape` | 形状枚举（NOTE_HALF、REST_QUARTER、CLEF_TREBLE 等） |
| `staff` | `StaffInfo` | 所属五线谱 |
| `bounds` | `Rectangle` | 边界框 |
| `grade` | `double` | 置信度评分 |
| `sig` | `SIGraph` | 所属的 SIGraph |

## 序列化机制

Inter 是抽象基类，其子类包括：

| 子类 | 代表符号 | 示例 |
|------|----------|------|
| `AbstractNoteInter` | 音符 | 二分音符、四分音符 |
| `AbstractPitchedInter` | 有音高的符号 | 音符头 |
| `AbstractHeadInter` | 符头 | 椭圆形符头 |
| `RestInter` | 休止符 | 全休止、半休止 |
| `ClefInter` | 谱号 | 高音谱号、低音谱号 |
| `KeyInter` | 调号 | 升降号 |
| `TimeInter` | 拍号 | 4/4、3/4 |
| `BarlineInter` | 小节线 | 单线、双线、终止线 |

JAXB 通过 `@XmlSeeAlso` 或运行时类型发现来支持这些子类的序列化。JAXB 的 `@XmlElementRef` 机制允许不带注释的子类型多态序列化。

---

## 在 SIGraph 中的角色

```
SIGraph (符号解释图)
  ├── Set<Inter>           ─── 节点：音符、休止符、谱号等
  └── Set<Relation>        ─── 边：连接关系
        ├── HeadStemRelation   ─── 符头→符干
        ├── StemBeamRelation   ─── 符干→符杠
        ├── SlurHeadRelation   ─── 连音线→音符
        └── ...
```

---

## 序列化精度说明

Inter 的形状和位置数据使用 `@XmlJavaTypeAdapter` 控制 XML 输出精度：

- 整数坐标：直接输出
- 浮点坐标：通过 `Double1Adapter`（1位小数）或 `Double3Adapter`（3位小数）控制精度
- 这有助于控制 `sheetNNN.xml` 的文件大小
