---
layout: default
title: Export
nav_order: 4
parent: Quick Tour
---
## Export

From the input image, Audiveris OMR engine has gradually built specific information that we
collectively refer to as "_OMR data_".

In the advanced chapter, we describe more thoroughly how this OMR data is organized, can be
persisted on disk (in a `.omr` file) and directly reused by Audiveris or other external programs.

Right now, we are focused only on how to feed a music sequencer with music data it can easily import.  
And as of this writing, this is achieved by going through MusicXML-formatted data.

We thus have to export OMR data as MusicXML data.  
This can be done via pulldown menu `Book | Export Book...`:

![](../assets/images/book_export.png)

From our "chula" example, this command produces a file named `chula.mxl`
(the `.mxl` extension indicates a compressed MusicXML format).

The default policy is to put all output files related to a given input file into one specific sub-folder
(the "book folder"), named according to the input file name.
In our concrete example, the book folder ("chula") will contain the export file ("chula.mxl").

The default base folder location of all "book folders" depends on the operating system.
For Windows OS, the default base folder is the "Audiveris" sub-folder of user's Documents folder.

More details about available output policies and the default base folder are available in
[Standard folders](../folders/standard.md) chapter.

Note that this export, from OMR to MusicXML,  is _lossy_, since a large amount of OMR
information can't go into MusicXML.
A `.omr` file can always be used to regenerate the `.mxl` export, but the reverse is not true.

{: .note}
A good advice is to keep these `.omr` files
-- unless you are running out of disk space! :-) --
because they represent a valuable source of OMR information,
suitable for training newer versions of Audiveris (more on this later).
