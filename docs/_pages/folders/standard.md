---
layout: default
title: Standard folders
grand_parent: References
parent: Folders
nav_order: 1
---
## Standard folders
{: .no_toc :}
{: .d-inline-block }
new in 5.3
{: .label .label-yellow }
---
Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}
---

### Purpose

This is where, by default, Audiveris stores all score outputs, such as the `.omr` project
files, the `.pdf` printouts, the `.mxl` MusicXML files, etc.

### Historical target policy

Before 5.3, Audiveris policy was to gather all outputs of a given input file
in a specific sub-folder, named according to the input radix, created under the base folder.

For example, with this historical policy, the processing of some "``Bach_Fugue.tif``" input file
results in a single specific "``Bach_Fugue``" sub-folder
containing "``Bach_Fugue.omr``", "``Bach_Fugue.mxl``" and perhaps "``Bach_Fugue-samples.zip``", etc.

This gives something like:

```
    <base_folder>
    ├── Bach_Fugue
    │   ├── Bach_Fugue.omr
    │   ├── Bach_Fugue.mxl
    │   └── Bach_Fugue-samples.zip
    ├── ...
    ...
```

All that Audiveris needs to know is the **base folder**.

The *default* path to the base folder depends on Operating System:

|  OS | `DATA_FOLDER` base |
| --- | --- |
| **Windows** | &lt;User Documents&gt;\\Audiveris |
| **Linux** (choice #1)| $XDG_DATA_HOME/AudiverisLtd/audiveris |
| **Linux** (choice #2)| $HOME/AudiverisLtd/audiveris |
| **MacOS** | $HOME/Library/AudiverisLtd/audiveris/data |

We can change this _default_ **base folder** via the pulldown menu `Tools | Options` dialog,
by modifying the value of  `org.audiveris.omr.sheet.BookManager.baseFolder` constant.  
(the modification persists between runs, until it is reset).

Rather than a base folder and a specific book sub-folder, we can choose to directly define an **output folder**
via the `-output <output-folder>` [CLI](../advanced/cli.md) argument.  
(the modification is valid for this run only).  
Still using the same processing example of ``Bach_Fugue.tif``,
this time with CLI option ``-output /path/to/fooBar``, we would get outputs like:

```
    /path/to/fooBar
    ├── Bach_Fugue.omr
    ├── Bach_Fugue.mxl
    ├── Bach_Fugue-samples.zip
    ...
```

### Updated target policy

The historical target policy is still the default policy, but additional possibilities
are now provided by Audiveris 5.3.

Beside the CLI option ``-output <output-folder>``, we now have two options selectable in
``Tools | Advanced topics`` pulldown menu.
See its [Target output folders](../advanced/topics.md#target-output-folders) section.
- **Input sibling** option, off by default, to locate outputs next to the input file
- **Separate folders** option, on by default, to gather outputs in a specific sub-folder

With these three options in hand, here is the algorithm to determine the default output folder of a book:
- If the book has already been saved (read: there already exists a book ``.omr`` file,
for example because we have already saved it), then we use the same folder.
- Else if CLI option ``-output <output-folder>`` exists, we use the specified output-folder.
- Else if ``Input sibling`` option is on, we use the same folder as the input file.
- Else if ``Separate folders`` option is on, we create a book specific sub-folder in the base folder.
- Else we use the base folder.
