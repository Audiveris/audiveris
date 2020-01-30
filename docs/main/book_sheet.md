### Book vs Sheet

An image file fed into OMR software contains one or several images.
Typically PDF and TIFF formats support the notion of multi-image files while, for example,
JPEG or PNG formats can deal only with single-image files.

For Audiveris, using the metaphore of a physical book made of several sheets of paper,
this physical containment is modeled as one **Book** instance (corresponding to the input file)
and a sequence of one or several **Sheet** instances (one sheet corresponding to one image).

Note that a sheet image may contain no music.
This happens for example for a title or illustration or simply a blank sheet.
In that case, the sheet will later be recognized as "_invalid_" (from the OMR point of view)
and flagged as such.

A (super-) Book instance could recursively contain (sub-) Book instances,
although this feature is not fully implemented yet.
