---
layout: default
title: Relation removal
parent: UI tools
nav_order: 5
---
# Relation removal
{: .no_toc }

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## Removing a wrong relation

There is no direct way to select Relation instances.
They can be selected only indirectly, via the selection of one of the `Inter` instances they link.

In the following example, a sharp sign has been linked to the wrong note head:

![](../../../assets/images/wrong_relation.png)

To select this relation, we can first select the involved sharp sign.
This will result in the picture above.

Then we use a right-click to display the contextual menu {{ site.popup_inters }},
hover on the `Inters...` submenu,
then on the sharp item to see the `Relations:` list of relations this `Inter` is involved in.

By clicking on the _AlterHead_ relation, we will be prompted to confirm the removal of this
relation.

![](../../../assets/images/select_relation_for_remove.png)

Without this relation, the sharp sign is now no longer linked to any head,
it thus appears in red abnormal status.

Finally, the correct relation should be manually added
(see the [Add Relation](../ui_tools/add_relation.md) previous section) to result in the
configuration below:

![](../../../assets/images/correct_relation_after_delete.png)

## Implicit relation removal

In the precise case above (correcting reference of accidentals), explicit removal of the
relation was not necessary, provided the correct relation is inserted manually.

This is so, because an accidental can reference only one note head
(if we except the special case of a
[note head shared by two voices](./shared_head.md)).

So the wrong _AlterHead_ relation would be removed automatically when inserting a new one.

The same applies to note heads: they can reference only one stem
(still excepting the special case of a single note head _shared_ between two opposite stems).
Here again, inserting a new _HeadStem_ relation would remove the former one.
