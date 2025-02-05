---
layout: default
title: Tools menu
parent: Menus
nav_order: 6
---
# Tools menu
{: .no_toc }

![](../../assets/images/tools_menu.png)

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## Install languages
{: .d-inline-block }
new in 5.4
{: .label .label-yellow }

Open a dialog to select and download additional OCR languages. 
See [OCR languages](../../guides/main/languages.md#getting-additional-languages).

## Browse global repository

Open a dialog to verify and edit the content of the Global sample repository
(this is _the_ only repository which is used for classifier training).

(needs the `SAMPLES` topic)

## Save global repository

Saves the Global sample repository to disk.

(needs the `SAMPLES` topic)

## Browse a local repository

Open a dialog to verify and edit a local (book) sample repository
(generally before merging it into the Global repository).

(needs the `SAMPLES` topic)

## Train classifier

Open a dialog to launch, monitor and evaluate the training of the glyph classifier.

See the [Training](../../guides/advanced/training.md) section.

(needs the `SAMPLES` topic)

## Memory

Displays the used memory in the output window

## Constants

Opens the constants management window.

All available constants are listed.
Most of them concern development constants only.

A search field allows to search for a string portion in the constant name or its description.

![](../../assets/images/constants.png)

See the [Constants](../../guides/advanced/constants.md) section.

## Preferences

Opens a dialog where the users can make their own choices regarding:
the default processing of input images, 
the default plugin,
the policy for outputs location, 
the font size in application items,
the chosen locale,
and additional items in pull-down menus.

See the [Preferences](../../guides/main/preferences.md) section.
