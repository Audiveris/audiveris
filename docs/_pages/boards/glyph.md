---
layout: default
title: Glyph board
grand_parent: References
parent: Boards
nav_order: 4
---
## Glyph board
{: .no_toc :}

![](../assets/images/glyph_board.png)

---
Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}
---

### Vip
(input/output)  
Flag this entity as VIP, resulting in verbose processing information.

### Dump
(input)  
Dump main entity data into the log window.

### Id
(input/output)  
Integer ID of entity.

### (groups)
(output)  
If any, the group(s) this glyph is part of.
A group is a tag assigned to a glyph, related to the intended usage of the glyph.
For example, `VERTICAL_SEED` is assigned to a glyph considered for the detection of
stem candidates.

### Weight
(output)  
The _normalized_ glyph weight.

The glyph raw weight (number of black pixels it is composed of) is divided by the square of
interline value to provide the interline-normalized weight.

### Width & Height
(output)  
The _normalized_ dimension of the glyph bounding box (raw dimension divided by interline value).
