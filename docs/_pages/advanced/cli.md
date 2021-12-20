---
layout: default
title: Command Line Interface
parent: Advanced Features
nav_order: 1
---
## Command Line Interface
{: .no_toc }

---
Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}
---

### Syntax

Note that any argument beginning with the `@` character is considered as the name of a text file
to be immediately expended _in situ_ (the text file is assumed to contain one argument per line).

CLI syntax is displayed as follows when the `-help` argument is present:

```
Audiveris Version:
   5.3-alpha

Syntax:
    audiveris [OPTIONS] [--] [INPUT_FILES]

@file:
    Content of file to be extended in line

Options:
 -help                                            : Display general help then stop
 -batch                                           : Run with no graphic user interface
 -sheets int[]                                    : Select sheet numbers and ranges (1 4-5)
 -transcribe                                      : Transcribe whole book
 -step [LOAD | BINARY | SCALE | GRID | HEADERS |  : Define a specific target step
 STEM_SEEDS | BEAMS | LEDGERS | HEADS | STEMS |      
 REDUCTION | CUE_BEAMS | TEXTS | MEASURES |          
 CHORDS | CURVES | SYMBOLS | LINKS | RHYTHMS |       
 PAGE]                                               
 -force                                           : Force step/transcribe re-processing
 -output <output-folder>                          : Define base output folder
 -playlist <file.xml>                             : Build a compound book from playlist
 -export                                          : Export MusicXML
 -print                                           : Print out book
 -option key=value                                : Define an application constant
 -upgrade                                         : Upgrade whole book file
 -save                                            : In batch, save book on every successful step
 -swap                                            : Swap out every sheet after its processing
 -run <qualified-class-name>                      : (advanced) Run provided class on valid sheets
 -sample                                          : (advanced) Sample all book symbols
 -annotate                                        : (advanced) Annotate book symbols

Input file extensions:
    .omr        : book file  (input/output)
    [any other] : image file (input)

Sheet steps are in order:
    LOAD       : Get the sheet gray picture
    BINARY     : Binarize the sheet gray picture
    SCALE      : Compute sheet line thickness, interline, beam thickness
    GRID       : Retrieve staff lines, barlines, systems & parts
    HEADERS    : Retrieve Clef-Key-Time systems headers
    STEM_SEEDS : Retrieve stem thickness & seeds for stems
    BEAMS      : Retrieve beams
    LEDGERS    : Retrieve ledgers
    HEADS      : Retrieve note heads
    STEMS      : Retrieve stems connected to heads & beams
    REDUCTION  : Reduce conflicts in heads, stems & beams
    CUE_BEAMS  : Retrieve cue beams
    TEXTS      : Call OCR on textual items
    MEASURES   : Retrieve raw measures from groups of barlines
    CHORDS     : Gather notes heads into chords
    CURVES     : Retrieve slurs, wedges & endings
    SYMBOLS    : Retrieve fixed-shape symbols
    LINKS      : Link and reduce symbols
    RHYTHMS    : Handle rhythms within measures
    PAGE       : Connect systems within page
```

### Arguments

These are the standard arguments that are listed when the help option is used.
They are presented here in alphabetical order.

#### -batch

Launches Audiveris without any Graphic User Interface.

#### -export

Exports each book music as a MusicXML file.

#### -force

Forces reprocessing even if target step has already been reached.  
This option is effective only when a target step is specified (see the `-step` option) or the `-transcribe` option is present.

#### -help

Displays the arguments summary as printed above, then exits.

#### -option KEY=VALUE

Specifies the value of one application option,
KEY being the qualified name of the option,
VALUE being the value to assign.

This is the CLI equivalent of the GUI pulldown menu `Tools | Options`.

#### -output DIRNAME

Defines the path to the default base output folder.

This output folder will be the parent of any subfolder created according to an input file name.

#### -playlist FILE.XML

Loads the provided `.xml` file as a playlist.

If in batch mode, the loaded playlist is used to build a compound book according to the playlist
content.

If in interactive mode, the loaded playlist is used only to populated and display a
`Split and Merge` dialog.
The user can then review and/or edit the playlist and potentially launch the building of
the compound book at a desired location.

#### -print

Exports each book music as a PDF file.

#### -save

Saves each book OMR data to its `.omr` project file as soon as a sheet step is processed
successfully.

This option is effective only in `-batch` mode.

#### -sheets N M X-Y

Specifies the IDs of sheets to process.

IDs are specified as a space-separated sequence of numbers (a sheet ID starts at 1).  
Also, the X-Y notation is accepted, to refer to all IDs between X and Y included.
Mind the fact that X-Y must be one argument, with no space around the `-` character.

This option is meant to initially open the book on a specific sheet, or to restrict processing
to some sheets.
If no sheet IDs are specified, all (valid) sheets are concerned.

Sheet IDs apply to all books referenced on the command line.

#### -step STEPNAME

Specifies a sheet target step.

This target step will be reached on every sheet referenced from the command line.
This means all valid sheets if no explicit sheet numbers are specified.

For any given sheet, if the target step has already been reached, no further processing is done.  
However, if the `-force` option is present, this sheet will be reset to BINARY and then processed
again to the target step.

#### -transcribe

Transcribes each book.

#### `--`

This argument (a double dash: "`--`") is not a real argument _per se_, but merely a delimiter
so that each following argument in the command line is taken as an input file path
(even if this argument begins with a `-` character).

#### FILENAME

Path to one input file.

If the file name extension is `.omr`, the file is an Audiveris project file which will be used
as input / output.

For any other extension, the file is considered as an image input file.

### Advanced Arguments

These arguments are made available for the advanced user.

#### -annotate

For each book, populates a Zip archive with images and symbol annotations derived from book Inter
instances.

These annotations are meant to populate a dataset for training future Audiveris 6.x new classifiers
(Page and/or Patch).


#### -sample

Populates each book sample repository with samples derived from the book Inter instances.

A book-level repository can be later merged into the global Audiveris sample repository in order
to prepare a training of Audiveris 5.x Glyph classifier.

#### -run CLASS_NAME
Runs the specified Java class on each valid sheet.

CLASS_NAME must be the fully qualified name of a Java class, which must extend the abstract class
`org.audiveris.omr.step.RunClass` and override its process() method:

```Java
public abstract class RunClass
{
    protected Book book;
    protected SortedSet<Integer> sheetIds;
    /**
     * Creates a new {@code RunClass} object.
     *
     * @param book     the book to process
     * @param sheetIds specific sheet IDs if any
     */
    public RunClass (Book book,
                     SortedSet<Integer> sheetIds)
    {
        this.book = book;
        this.sheetIds = sheetIds;
    }
    /**
     * The processing to be done.
     */
    public abstract void process ();
}
```
