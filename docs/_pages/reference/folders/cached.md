---
layout: default
title: Cached folders
parent: Folders
nav_order: 3
---
# Cached folders
{: .no_toc :}

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## Purpose

Audiveris uses these locations for persistency of internal information.

These files are **not** meant to be edited, period!

## GUI folder

These are opaque files used for GUI lifecycle (notably last position and size of each window)
- AudiverisMainFrame.session.xml
- LanguagesFrame.session.xml
- LogicalPartsEditor.session.xml
- etc...

|  OS | `GUI folder` |
| --- | --- |
| **Windows** | %APPDATA%\\AudiverisLtd\\audiveris |
| **Linux** | $HOME/.audiveris |
| **Flatpak** | $HOME/.audiveris |
| **macOS** | TODO: !!! **I DON'T KNOW** !!!|

## Log folder

Each session of Audiveris application creates in this folder a single global `dateTtime.log` file
that covers all log events of the session.

|  OS | ``LOG_FOLDER`` |
| --- | --- |
| **Windows** | %APPDATA%\\AudiverisLtd\\audiveris\\log |
| **Linux** (choice #1)| $XDG_CACHE_HOME/AudiverisLtd/audiveris/log |
| **Linux** (choice #2)| $HOME/.cache/AudiverisLtd/audiveris/log |
| **Flatpak** | $HOME/.var/app/org.audiveris.audiveris/cache/log |
| **macOS** | $HOME/Library/AudiverisLtd/audiveris/log |

## Temp folder

It is a temporary storage for various needs, such as saving a snapshot of a score image portion.

`TEMP_FOLDER` is defined as the direct `temp` sub-folder of `LOG_FOLDER`.
