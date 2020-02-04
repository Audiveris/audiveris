---
layout: default
title: Section board
grand_parent: References
parent: Boards
nav_order: 3
---
## Section board
{: .no_toc :}

![](../assets/section_board.png)

There are two different section boards, one for sections with horizontal runs,
one for sections with vertical runs.

## Table of contents
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

### Weight
(output)  
Number of (black) pixels that compose the section.

### X & Y
(output)  
Coordinates of top left corner of the section:
* For a horizontal section, this is the left side of the top run.
* For a vertical section, this is the top side of the left run.

### Width & Height
(output)  
Width and height of the section bounding box.
