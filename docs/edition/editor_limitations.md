## Editor Limitations

The current editor is still young, it exhibits some limitations that should be removed in a future
release.
Purpose of this section is to warn the user about them in the current release.

To quickly summarize Audiveris current editor, we can say that it is a user interface that provides
only the ability to add and remove Inter and Relation instances.

### Containers

We can assume you are now familiar with the  hierarchy of containers
(if not, see [Main Entities](../main/entities.md)):

> Book / Sheet / Page / System / Part / Staff.

This hierarchy is set by the OMR engine, and _cannot_ be modified by the user.

Beside this hierarchy, all OMR information is kept in the Inter/Relation items, which are organized
in a graph (SIG) within each system:

* An Inter belongs to a system (SIG), to a part and often to a staff.
* A Relation can be created only between 2 Inter instances, and these Inter instances must belong to
the same system (SIG).

A part is horizontally composed of measures, delimited by barlines (or by system edge).
The user has no direct action on measures, but can add/remove barlines.
After the `MEASURES` step, any barline modification impacts the whole system height and thus
indirectly impacts the measures definition.

The only barlines that _cannot_ be modified by the user are the group of barlines on the left side
of the sheet, since they define the systems and parts.

### Location

The location of a given Inter instance is fixed:

* It's the glyph location when the Inter is created by "assigning a shape" to a glyph,
* It's the drop point when the Inter results from a drag n' drop action.

In either case, the Inter location _cannot_ be modified.

Typically, once dropped, the Inter cannot be moved, the only possibility is to delete it and
to drop a new one.

### Shape

The precise shape of an Inter is fully determined, either by the underlying glyph or by the dropped
shape.

The user _cannot_ "reshape" an Inter, for example no resizing, no rotation is possible.

The consequences are as follows:

* All fixed shapes can be dropped.
* For non-fixed shapes, drag n' drop is limited to just a few cases: standard stems and beam hooks.  
But long stems, regular beams, slurs, brackets, braces, etc, can by nature vary in size and thus
cannot be dragged.
Any attempt to drag such items from the Shapes palette will display a red cross.

### The future

The ability to move and precisely resize an Inter sits on the top list of future Audiveris Editor
improvements.
