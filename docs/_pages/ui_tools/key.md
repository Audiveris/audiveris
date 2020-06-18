---
layout: default
title: Key
parent: UI Tools
nav_order: 13
---
## Key
{: .no_toc }

A key signature cannot be built or modified incrementally by adding or removing one alteration
sign at a time.
It must be built or modified **globally**:
* Either by selecting a compound glyph and assigning the desired key shape,
* Or by dragging and dropping the desired key shape from the shape palette.

---

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

### Flats and Sharps

When dragging a "ghost" key from the shape palette, the ghost turns from dark-gray to green
when you enter a staff and as usual a thin red segment goes from ghost center to staff mid line.

Moreover, the dragged key snaps immediately to proper vertical position, according to the
effective clef at the point of insertion.

For example, let's insert a 2-sharp key:

![](../assets/images/key_drop.png)

Notice the two sharp signs are located on F and C steps respectively, and they can move only
horizontally until you release the mouse.  
Once dropped, you can still set the key into edition mode and shift again the inserted key.

### Naturals

As of this writing, Audiveris OMR engine can recognize all-sharp keys and all-flat keys but
no key with natural signs inserted.

However, you have the ability to insert an all-natural key manually.
To do so, drag the 1-natural key from the shape palette and move it to the desired insertion
point.

![](../assets/images/key_natural.png)

When you enter the target staff, the 1-natural ghost key with turn as usual from dark-gray to
green, but its configuration and position will be dynamically updated, to fit:
* The effective **clef**,
* The effective **key** to be cancelled.

If for example the effective key is a 3-sharp key, the "cancel" key will be a 3-natural key,
with each natural sign located according to the corresponding sharp sign to cancel:

| Staff-relative location| Cancel Key appearance|
| --- | --- |
| Outside: | ![](../assets/images/key_natural_outside.png) |
| Inside: | ![](../assets/images/key_natural_inside.png) |
