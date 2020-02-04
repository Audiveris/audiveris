---
layout: default
title: Cached folders
grand_parent: References
parent: Folders
nav_order: 3
---
## Cached folders
{: .no_toc :}

## Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}

---

### Purpose

Audiveris uses these locations for persistency of internal information.

These files are **not** meant to be edited, period!

### GUI folder

These are opaque files used for GUI lifecycle (notably last position and size of each window)
- mainFrame.session.xml
- optionsFrame.session.xml
- aboutDialog.session.xml
- etc...

|  OS | GUI folder |
| --- | --- |
| **Windows** | %APPDATA%\\AudiverisLtd\\audiveris |
| **Linux** | ~/audiveris |
| **MacOS** | TODO: !!! **I DON'T KNOW** !!!|

### Log folder

Each session of Audiveris application creates in this folder a single global `dateTtime.log` file
that covers all log events of the session.

|  OS | ``LOG_FOLDER`` |
| --- | --- |
| **Windows** | %APPDATA%\\AudiverisLtd\\audiveris\\log |
| **Linux** (choice #1)| $XDG_CACHE_HOME/AudiverisLtd/audiveris/log |
| **Linux** (choice #2)| $HOME/.cache/AudiverisLtd/audiveris/log |
| **MacOS** | $HOME/Library/AudiverisLtd/audiveris/log |

### Temp folder

It is a temporary storage for various needs, such as saving a snapshot of a score image portion.

`TEMP_FOLDER` is defined as the direct `temp` sub-folder of `LOG_FOLDER`.
