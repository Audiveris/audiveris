---
layout: default
title: Book menu
grand_parent: References
parent: Pulldown Menus
nav_order: 2
---
## Book menu
{: .no_toc }

![](../assets/images/book_menu.png)

A book is a collection of one or more sheets, each with its current state of transcription.
It can be saved and reloaded at any time during a processing session.

As opposed to [Loading an image ](../quick/load.md) which restarts from the initial image,
loading a saved book allows to resume processing from the last step saved.
This is especially useful for processing books with many sheets.

---
Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}
---

### Recent Books

Open a list of recently saved books.
After selection, the book is opened in the state it was saved.

### Most recent Book

Open the book most recently closed.

### Open Books

Open a dialog box allowing to select a book that has been saved before.

### Split and Merge

Open a dialog to select and combine inputs from different sources (books, image files).

See [Split and Merge](../main/split_merge.md) section.

### Set Book Parameters

Modify parameters for transcription, at global level, book level and sheets level.

See [Book parameters](../main/book_parameters.md).

### Reset Book to Gray

Reset all sheets of a book to the initial state, as if it were just loaded as a plain image.

### Reset Book to Binary

Same as `Reset Book to Gray` above, except that the initial processing of the images
from gray-scale to B&W image is kept.

### Transcribe Book

Start the transcription of all sheets of the book.

Transcription is by default performed sheet by sheet.
During processing, the user may start editing sheets that have already been transcribed.

### Rebuild Scores

Force the rebuilding of scores, out of the processed sheets.

### Swap Book Sheets

Swap to disk all the sheets of current book (except the current sheet).

This is useful to save memory when working on a book with numerous sheets.
When needed, a swapped sheet is transparently reloaded.

### Select sheets

Select some sheets out of the current book, and limit processing to these sheets.

See [Sheets Selection](../main/sheets_selection.md) section.

### Print Book

Save the transcribed book in PDF format, so that it can be printed or saved for further purposes.
The name of the output file is derived from the book name.

By default, the file is saved into book folder, a (newly created) subdirectory of the base output directory.  
But we can modify this policy.

### Print Book as

Same as above, except that we can define the file name and the directory where the file is saved.

### Export Book

Export the transcribed score as a MusicXML `.mxl` file for exchange with a notation program.
See [Outputs Formats](../main/output_formats.md).

By default, the file is saved into book folder, a (newly created) subdirectory of the base output directory.  
But we can modify this policy.

### Export Book as

Same as above except that we can define the file name and the directory where the file is saved.

### Sample Book Symbols

Populate the book sample repository with samples derived from relevant inters of all book sheets.

(needs `SAMPLES` topic, see [Samples section](../advanced/samples.md))  

### Browse Book Repository

Open a window to browse, check and filter the samples of book repository.

(needs `SAMPLES` topic, see [Samples section](../advanced/samples.md))

### Save Book Repository

Save book sample repository to disk into book folder.

(needs `SAMPLES` topic, see [Samples section](../advanced/samples.md))

### Annotate Book Symbols

Populate a Zip archive with images and symbol annotations derived from book Inter instances.

These annotations are meant to populate a dataset for training the coming Audiveris 6.x new
classifiers (Page and/or Patch).

(needs `ANNOTATIONS` topic)

### Save Book

Save the collection of all book sheets with the current state of transcription.

By default, the file is saved into book folder, a (newly created) subdirectory of the base output directory.  
But we can modify this policy.

### Save Book as

Same as above except that we can define the file name and the directory where the file is saved.

### Close Book

Close the current book and releases all its resources.

If the book has some unsaved data, the user is prompted to save it before closing.
