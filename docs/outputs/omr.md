---
---
## .omr files

A `.omr` file is the OMR project file for a given image input file (book).
It is located in the book subfolder.

It contains all OMR internal data, except some transient variables.
This data allows gradual processing of books of any size, since it can be continuously saved to
(and restored from) disk.

All Audiveris outputs derive from this data.

As opposed to commercial softwares, this data is not opaque.
It is a zip-compressed collection of XML files.
Its content is made publicly available and its structure is fully documented in this
[wiki article](https://github.com/Audiveris/audiveris/wiki/Project-Structure).
