---
layout: default
title: Inter editors
parent: References
nav_order: 0
---
## Inter editors
{: .no_toc }

---
Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}
---

### Default editor

The default editor applies to every Inter class, for example the augmentation dot class here below,
unless a more specific editor is defined for the Inter class at hand.

![](../assets/images/aug_dot_edited.png)

The default editor provides only one handle located at Inter center.
This very basic editor allows to move the Inter in any direction, but provides no way to resize it.

The involved relations if any -- like the "_Augmentation_" relation between the note head and the
augmentation dot in this example -- are dynamically updated while the Inter is being moved.

![](../assets/images/aug_dot_edited_2.png)
Here, we have moved the augmentation dot one line below and the relation is
kept with the augmented note.

![](../assets/images/aug_dot_edited_3.png)
Here, we are currently located too far from the note and the relation disappears.

The next sections describe all specific editors, listed in alphabetical order.

### Barline/Bracket editor

![](../assets/images/barline_edited.png)

A Barline can move only horizontally, and can't be resized, because it is **snapped** to
staff height.

(In Audiveris data model, a Barline goes from staff top line to staff bottom line.
Aligned barlines from different staves are often connected by so-called "Connectors").

The same editor applies to Bracket as well.

### Brace editor

![](../assets/images/brace_edited.png)

* Center handle shifts the brace in any direction,
* Top and bottom handles resize the brace vertically.

IMPORTANT NOTICE: Shifting or resizing a brace is meant for small adjustments only.  
If you want to extend or reduce the number of staves embraced by a Brace instance
(which is a rather heavy operation that impacts the definition of parts within a system),
this must be done **explicitly** by adding or removing a manual Brace.

### Beam editor

![](../assets/images/beam_edited.png)

* Center handle moves the whole beam in any direction.
* A side handle moves the side in any direction, **snapping** the beam side on any stem nearby.

### Ending editor

![](../assets/images/ending_edited.png)

* Center handle moves the whole ending in any direction
* A side handle moves the ending side horizontally
(together with its side leg if any)

### Flag editor

![](../assets/images/flag_edited.png)

The flag editor is another variation of the default editor with its single center handle,
which here can move **only vertically** along the related stem.

### Head editor

![](../assets/images/head_edited.png)

The note head editor is similar to the default editor, with its single center handle.
The only difference is that the head being moved is snapped:
* **Vertically** to the underlying staff lines, or ledger lines
  (perhaps dynamically created while head is shifted away from staff),
* **Horizontally** to the stem nearby, if any, on left or right.
Of course, this does not apply to WHOLE or BREVE shapes since these heads use no stem.

### KeyAlter/Key editor

![](../assets/images/key_alter_edited.png)

The key alter editor allows to move **horizontally** one KeyAlter member of the key signature.

![](../assets/images/key_edited.png)

If the key signature is a whole manual signature (e.g. it has been dropped from the ShapePalette),
then the editor can shift the **whole key** horizontally.

### TimeHalf/Time editor

![](../assets/images/time_half_edited.png)

If the time signature was recognized with two separate halves, each of these halves is an inter
by itself, and can be shifted horizontally only.

![](../assets/images/time_whole_edited.png)

If the time signature is a whole signature then is can be shifted horizontally as a whole.

Note this applies as well to signatures handled globally, such as a manual custom signature
or even a two-part signature (such as 2/4) if it was recognized globally.

### Ledger editor

![](../assets/images/ledger_edited.png)

### Slur editor
![](../assets/images/slur_edited.png)

This is the most complex editor:
* Center handle shifts the whole slur in any direction,
* A side handle extends the slur side in any direction
  (together with the related control point),
* Control handles move their underlying control point,
* Middle of control segment moves both control points in any direction.

### Stem/Arpeggiato/Connector editor

![](../assets/images/stem_edited.png)

The editor applies to Stem as well as Arpeggiato and Connector (of barlines or brackets)

### Wedge editor

![](../assets/images/wedge_edited.png)

The handle on the lower segment allows to increase or decrease the wedge vertical spread.

### Word editor

![](../assets/images/word_edited.png)

* Center handle shifts the word in any direction,
* Right handle modifies the word dimensions, by increasing or decreasing the font size.
