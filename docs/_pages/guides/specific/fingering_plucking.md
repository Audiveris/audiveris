---
layout: default
title: Fingering and Plucking
parent: Specific features
nav_order: 9
---
# Fingering and Plucking
{: .no_toc }
{: .d-inline-block }
completed in 5.3
{: .label }

These are technical informations for guitar or similar instrument, to indicate how
a note should be played.

[Assuming a right-handed person]:
- **Fingering** describes the left-hand finger, via a digit number (0, 1, 2, 3, 4)
- **Plucking** describes the right-hand finger, via a letter (`p`, `i`, `m`, `a`)

---
Table of contents
{: .text-epsilon }
1. TOC
{:toc}
---

## Example

Here is an example with fingering indications (in red squares)
and plucking indications (in green circles):

![](../../assets/images/fingering_plucking.png)

## Detection

The OMR engine must be explicitly told to detect these indications.
This can be done via the processing switches in the {{ site.book_parameters }} pull-down menu:

![](../../assets/images/fingering_plucking_switches.png)

## Insertion 

Regardless of the switches values, we can always manually assign or drag & drop these
indications from the shape board:

![](../../assets/images/fingering_plucking_sets.png)

| Shape Set | Palette |
| :---: | :---: |
| Fingerings | ![](../../assets/images/fingering_palette.png) |
| Pluckings | ![](../../assets/images/plucking_palette.png) |

## Export

These indications get exported to MusicXML and are thus visible from MuseScore for example:

![](../../assets/images/fingering_plucking_musescore.png)