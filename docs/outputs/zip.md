---
---
## .zip files

These files are containers mainly used for training material.

### Book

A book folder (let's name it "BOOK") may contain:

* **BOOK-samples.zip**:
This is the book samples repository.
It gathers all samples (glyph + shape) collected in the book.
The advanced user should carefully review (and perhaps edit) all the samples of this _local_
repository, before pushing its content to the _global_ repository.

* **BOOK-images.zip**:
This is an optional companion of the local samples repository.
It contains the related image of each sheet for which at least a sample is present.
This allows to display the background context of the sample.

* **BOOK-annotations.zip**:
This is just an output of Audiveris 5.1, meant for the future page and patch classifiers scheduled
for 6.x versions.
It provides sheet image and symbol annotations (shape + bounding box) for each recognized inter.

### Global

These files are located in the `TRAIN_FOLDER`.
See section on [Essential folders](../folders/essential.md).

**samples.zip**:
This is the global samples repository.
Its content is based on all local repositories that have been pushed to it.
It is the only source for training the glyph classifier.

**images.zip**:
This archive contains the images pushed with samples from local repositories.
It can get fairly big, and is used only for visual check, not for training.
