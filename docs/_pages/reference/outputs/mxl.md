---
layout: default
title: .mxl
parent: Outputs
nav_order: 2
---
# .mxl files
{: .no_toc :}

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## Purpose
These are zip-compressed files of XML music files formatted according to **MusicXML** specification.

Most, if not all, music editors are able to import / export this kind of file,
making MusicXML format a _de facto_ standard for music information exchange.
Please refer to the detailed
[MusicXML specification](http://usermanuals.musicxml.com/MusicXML/MusicXML.htm).

{: .highlight }
Instead of `.mxl` files, which are compressed XML files, Audiveris can export plain
(non-compressed) `.xml` files.  
To do this, we just set the option `org.audiveris.omr.sheet.BookManager.useCompression`
to false.

## Book level
A **book** export provides:
* Either one separate `.mxl` file for each movement found in the book (this is the default).
* Or a single `.mxl` file for a global `opus` containing the separate movements.
  The notion of opus is fully defined by MusicXML, but not supported by many editors, if any.
  To make Audiveris use opus, we simply set the option `org.audiveris.omr.sheet.BookManager.useOpus`
  to true.

## Sheet level
A **sheet** export provides:
* One separate `.mxl` file for each page found in the sheet.
