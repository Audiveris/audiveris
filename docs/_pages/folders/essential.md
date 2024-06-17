---
layout: default
title: Essential folders
grand_parent: References
parent: Folders
nav_order: 2
---
# Essential folders
{: .no_toc :}

---
Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}
---

## Purpose

This is where Audiveris stores user-specific essential parameters:

Under Windows notably, these are _hidden locations_ by default.  
Please do not create or modify these files, unless you are an advanced user and
know what you are doing.

## Config folder

Audiveris defines a `CONFIG_FOLDER` for configuration files:

| File name | Description |
| :--- | :--- |
| **run.properties** | User-modified application options |
| **logback.xml** | Logging configuration |
| **plugins.xml** | Definition of plugins to external programs |
| **user-actions.xml** | Additional GUI actions |

Precise location of `CONFIG_FOLDER` depends on OS environment:

|  OS | `CONFIG_FOLDER` |
| :--- | :--- |
| **Windows** | %APPDATA%\\AudiverisLtd\\audiveris\\config |
| **Linux** (choice #1)| $XDG_CONFIG_HOME/AudiverisLtd/audiveris |
| **Linux** (choice #2)| $HOME/.config/AudiverisLtd/audiveris |
| **MacOS** | $HOME/Library/Application Support/AudiverisLtd/audiveris |

## Train folder

There is a `TRAIN_FOLDER` that can be populated with user-specific training
material and trained model to override default Audiveris model:

| File name | Description |
| :--- | :--- |
| **basic-classifier.zip** | Trained model and norms for glyph classifier |
| **samples.zip** | Global repository of training samples |
| **images.zip** | Background sheet images of training samples |

``TRAIN_FOLDER`` is defined as the direct `train` sub-folder of `CONFIG_FOLDER`.
