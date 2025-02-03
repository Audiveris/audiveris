---
layout: default
title: Relation addition
parent: UI tools
nav_order: 4
---
# Relation addition
{: .no_toc }

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

Most `Inter` instances have relations with other `Inter` instances.
For example the note head below exhibits 2 relations:
- a _SlurHead_ relation between slur and note head,
- a _HeadStem_ relation between note head and stem.

![](../../../assets/images/note_with_relations.png)

We can visually check the relations when we select an `Inter`.
The name of each relation is also displayed, provided that the current zoom is high enough (>=2).


## Mandatory vs. non-mandatory relations

Depending on the `Inter` class, an `Inter` instance may need a relation with another `Inter` instance.

If an `Inter` instance lacks a mandatory relation, it should somehow be removed before the end of
the transcription process.
This is the case when created as a candidate by the OMR engine but, if the `Inter` at hand was
manually added, it can't be automatically removed by the engine.
It is simply flagged as "_abnormal_" and shown in red to call the user's attention on it.


In the example below, a _SlurHead_ relation between slur and note head is mandatory for the slur.

| non-linked slur | linked slur |
| --- | --- |
| ![](../../../assets/images/non_linked_slur.png) | ![](../../../assets/images/linked_slur.png) |

A missing relation can happen when the geometry rules are not matched, perhaps because the gap
is a bit too wide between the slur and any note head.   
In that case, we have to either shift or resize the Inters accordingly or manually set the
needed relation.

Note, regarding relation automatic search:
* A **mandatory relation** is automatically searched for, only at the moment the dependent
inter (such as the slur in the example) is created, shifted or resized.
This is so, simply because -- using the same slur example -- the slur _needs_ a head
but the head _does not_ need a slur.
A good practice, when several Inters have to be created manually, is to create the independent
Inters first and the dependent Inters second.  
But still, when the geometry is really beyond some specified limits, the needed relation may not be
automatically created and we'll have to add it manually.

* A **non-mandatory relation** is never searched for automatically.
An example of a non-mandatory relation is the case of a direction sentence which doesn't always
have a chord precisely above or below.  
For such non-mandatory relations, we have to decide if we set them or not.

## Linking Inters

Assuming the slur is not linked to the note head, we need to insert the relation between them.

To do so, we can point and drag from the slur to the note head (a thin black vector will
appear as we move the mouse, see picture below)

![](../../../assets/images/linking_slur.png)

Then, we release the mouse when reaching the targeted head.

Audiveris will search among all the inters grabbed at the first and last locations, find the
missing relation if any between those two collections of inters and set the proper relation.  
This commits the relation insertion.

{: .note}
When linking two elements, say A and B, the direction is irrelevant:
we can either drag from A to B or from B to A, the result will be identical.  
