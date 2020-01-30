## Standard folders

This is where, by default, Audiveris stores all score outputs, such as the `.omr` project
files, the `.pdf` printouts, the `.mxl` MusicXML files.

Audiveris needs just one _base_ folder, since all the outputs of a given input file are organized
into a specific sub-folder, named according to the input radix.

For example, the processing of some "Bach_Fugue.tif" input file would result in a single specific
"Bach_Fugue" folder containing "Bach_Fugue.omr", "Bach_Fugue.mxl" and perhaps
"Bach_Fugue-samples.zip", etc.

### OS default location

|  OS | `DATA_FOLDER` base |
| --- | --- |
| **Windows** | &lt;User Documents&gt;\\Audiveris |
| **Linux** (choice #1)| $XDG_DATA_HOME/AudiverisLtd/audiveris |
| **Linux** (choice #2)| $HOME/AudiverisLtd/audiveris |
| **MacOS** | $HOME/Library/AudiverisLtd/audiveris/data |

### User modification

You can override this _default_ base folder via either:
* The `-output <output-folder>` [CLI](../advanced/cli.md) argument.  
  (the modification is valid for this run only).
* The pulldown menu `Tools | Options` dialog, by modifying the value of
  `org.audiveris.omr.sheet.BookManager.baseFolder` constant.  
  (the modification persists between runs, until it is reset).
