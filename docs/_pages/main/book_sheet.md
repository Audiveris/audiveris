---
layout: default
title: Book of Sheets
grand_parent: Main Features
parent: Main Entities
nav_order: 1
---
### Book of Sheets

An image file fed into OMR software contains one or several images.
Typically PDF and TIFF formats support the notion of multi-image files while, for example,
JPEG or PNG formats can deal only with single-image files.

For Audiveris, using the metaphor of a physical book made of several sheets of paper,
this physical containment is modeled as one **Book** instance (corresponding to the input file)
and a sequence of one or several **Sheet** instances (one sheet corresponding to one image).

Note that a sheet image may contain no music.
This happens for example for a title or illustration or simply a blank sheet.
In that case, the sheet will later be recognized as "_invalid_" (from the OMR point of view)
and flagged as such.

With Audiveris 5.3, we can now split a book into smaller ones or, conversely,
merge small books into a larger one.  
This feature is documented in the [Split and Merge](split_merge.md) section.
