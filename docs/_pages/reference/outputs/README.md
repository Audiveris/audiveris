---
layout: default
title: Outputs
parent: Reference
nav_order: 7
---
# Outputs

Output files are generally located in the dedicated subfolder of the related book.
See the [Folders](../folders/README.md) chapter.

This chapter presents the various output formats designated by their file extension.

A special emphasis is put on the `.omr` format, since this format governs the internal
structuring of any Audiveris project file.
Such file is also named a Book file, because there is one `.omr` project file per Book, and the
file merely represents the marshalling of Book internal data from memory to disk.

# Output Formats

There are 3 possibilities for the output of a transcribed score:

* Output as **OMR** file for saving and later reloading of OMR data for additional processing,
manual correction or production of other outputs.  
See the [.omr format](../outputs/omr.md) section.

* Output as **PDF** file for direct use by a musician.
It writes the resulting image into a PDF file, which is basically the content of the Picture/Binary
tab in logical mode.  
See the [.pdf format](../outputs/pdf.md) section.

* Output as **MusicXML** file for use in a score notation program.
It writes a MusicXML file with the exported score entities.  
See the [.mxl format](../outputs/mxl.md) section.
