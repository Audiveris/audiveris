---
layout: default
title: Inter Removal
grand_parent: User Editing
parent: UI Tools
nav_order: 3
---
## Inter Removal
{: .no_toc }

When one or several Inter instances have been selected, we can remove them as follows:

*   The Inter-board has a `Deassign` button which removes the selected inter displayed in the
  board.
  This applies only for the displayed Inter.  
  ![](../assets/images/deassign_button.png)

*   The popup menu, trigerred by a right click, in its `Inters...` sub-menu, provides an item to
    remove the selected Inter entities according to their containing system.
  ![](../assets/images/remove_inters.png)

*   Pressing `DELETE` key or `BACKSPACE` key on the keyboard removes all the selected inters.

If more than one Inter is selected, we will be prompted for confirmation beforehand.

Removing a selected Inter (or a set of selected Inters) automatically removes the relations these
Inters were involved in.

Removing the last member of an ensemble also removes that ensemble.
