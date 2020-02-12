---
layout: default
title: Sheet Parameters
parent: Main Features
nav_order: 6
---
## Sheet Parameters

The behavior of steps like GRID (staves, etc), BEAMS and STEMS depends highly on the accuracy
of scaling data estimated during:

* SCALE step: Staff line thickness, interline, small interline if any, beam thickness.
* STEM_SEEDS: Stem thickness.

Scaling data is inferred from histograms of vertical run lengths.
Typically, sheets with very few beams sometimes result in wrong estimates of main beam thickness.

Since this data is taken as input parameters for subsequent processing, it can be reviewed and
modified for the sheet at hand, before being used by the depending steps.

Selecting `Sheet | SetSheet Parameters` menu item opens the dialog as follows:

![](../assets/images/sheet_parameters.png)

Initially, all rows appear in gray, and the data values, if any, are displayed on the right.

* To modify a data, first select the row by checking the box on left, the row turns black.
You can then modify the data value.

* Deselecting a row resets the data to its initial value.

* Finally, pressing the `OK` button commits the modified values and closes the dialog.
