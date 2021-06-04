---
layout: default
title: Sheet scale
parent: Main Features
nav_order: 6
---
## Sheet scale

The behavior of steps like GRID (staves, etc), BEAMS and STEMS depends highly on the accuracy
of scaling data estimated during:

* SCALE step: Staff line thickness, interline, small interline if any, beam thickness.
* STEM_SEEDS step: Stem thickness.

Scaling data is inferred from histograms of vertical run lengths.   
For sheets with very few beams, the risk is that histograms may have too limited contribution from
beams runs, resulting in wrong estimates of main beam thickness.

And since this data is taken as input parameters for subsequent processing, it is important to check
if its value is correct, before being used by the depending steps.

Data can be reviewed and modified for the sheet at hand.    
Selecting `Sheet | Set scaling data` menu item opens the dialog as follows:

![](../assets/images/sheet_scale.png)

Initially, all rows appear in gray, and the data values, if any, are displayed on the right.

* To modify a data, first select the row by checking the box on left, the row turns black.
You can then modify the data value.

* Deselecting a row resets the data to its initial value.

* Finally, pressing the `OK` button commits the modified values and closes the dialog.
