---
layout: default
title: Essential folders
parent: Folders
nav_order: 2
---
# Essential folders
{: .no_toc :}

This is where Audiveris stores user-specific essential parameters:

You can create or modify these files, provided you are an advanced user and
know what you are doing.

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## Config folder

Audiveris defines a `CONFIG_FOLDER` for configuration files:

| File name | Description |
| :--- | :--- |
| **run.properties** | User-modified application constants |
| **logback.xml** | Logging configuration |
| **plugins.xml** | Definition of plugins to external programs <br> See the [Plugins](../../guides/advanced/plugins.md) section. |
| **user-actions.xml** | Additional GUI actions |

The precise location of `CONFIG_FOLDER` depends on OS environment:

|  OS | `CONFIG_FOLDER` |
| :--- | :--- |
| **Windows** | %APPDATA%\\AudiverisLtd\\audiveris\\config |
| **Linux** (choice #1)| $XDG_CONFIG_HOME/AudiverisLtd/audiveris |
| **Linux** (choice #2)| $HOME/.config/AudiverisLtd/audiveris |
| **Flatpak** | $HOME/.var/app/org.audiveris.audiveris/config |
| **macOS** | $HOME/Library/Application Support/AudiverisLtd/audiveris |

## Tessdata folder

Starting with 5.4 release, the language files needed by Tesseract OCR software,
are hosted in the `tessdata` sub-folder of `CONFIG_FOLDER`.

To populate this `tessdata` folder, the interactive user can simply use the 
{{ site.tools_languages}} pull-down menu.

## Train folder

There is a `TRAIN_FOLDER` that can be populated with user-specific training
material and trained model to override default Audiveris model:

| File name | Description |
| :--- | :--- |
| **basic-classifier.zip** | Trained model and norms for the glyph classifier |
| **samples.zip** | Global repository of training samples |
| **images.zip** | Background sheet images of training samples |

``TRAIN_FOLDER`` is defined as the direct `train` sub-folder of `CONFIG_FOLDER`.
