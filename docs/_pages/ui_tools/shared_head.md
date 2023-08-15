---
layout: default
title: Shared Head
grand_parent: User Editing
parent: UI Tools
nav_order: 14
---
## Shared Head
{: .no_toc }

---
Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}
---

### Note head shared by two voices

Generally a note head (filled or void -- except whole and breve notes) is connected to exactly one stem, as in the following example.

If head and stem are connected (via a `HeadStemRelation`), both appear with their own standard color.
If not, one or both appear in red and to fix this, we can simply drag a link from one to the other.

![](../assets/images/shared_head_1.png)

Then, if needed, we can insert a stem on the other side of the head, in the opposite direction.

![](../assets/images/shared_head_2.png)

It appears in red because it can't get automatically connected to the head
(because this head is already connected on the other side).

To actually set the  connection, we have to **manually** drag a link between the (new) stem and the
head.
The program then checks if the resulting configuration is the canonical one
(which means _a stem down on the left side of the head and a stem up on the right side,
the shared head being located at the end of each stem_):

* If the check fails, the new connection is set but the old one is removed.
This is the standard behavior.
* But if the check succeeds, both connections are kept and the _'shared'_ head gets logically
duplicated into two heads, one _'half'_ for the left and one _'half'_ for the right:

![](../assets/images/shared_head_3.png)

By selecting all components, we can see the various links
(_HeadStemRelation_ between each head half and "its" stem, and _MirrorRelation_ between the two
half heads):

![](../assets/images/shared_head_4.png)

And if voices are colorized, the separation between head 'halves' becomes even more visible:

![](../assets/images/shared_head_5.png)

### Impact on relations

Playing with relations around note heads, such as the relation with an accidental or with an
augmentation dot, is still possible with shared note heads.
Simply, we have to pay attention to point precisely to the desired _'half'_ head.

| Example | Explanation |
| :---:   | :---        |
| ![](../assets/images/shared_alter.png) | Here, the alteration sign is _'shared'_ and thus also split, each sign _'half'_ colorized with the same color as its related head half.| 
| ![](../assets/images/non_shared_dot.png) | Here, the augmentation dot is related only to one head _'half'_ (otherwise the dot would exhibit both colors as the alteration sign in the previous example) |
