---
layout: default
title: Sheet selection
parent: Book portions
nav_order: 2
---

# Sheet selection

An action launched at the sheet level processes only that sheet.    
An action launched at the book level, processes by default all the (valid) sheets of the book.
And this may be too much when what we want is to work only on a portion of the book
(for example to print or listen to just a movement or a few selected sheets).

We can specify a sheet selection, via the pull-down menu {{ site.book_select }} which opens a
selection dialog based on sheet IDs as shown below:
(The highest sheet ID in the current book is recalled)

|  Selection  |   Meaning    |
| ----------- | ------------ |
|![](../../../assets/images/specified_sheets_empty.png)| Blank specification: **All** sheets are selected|
|![](../../../assets/images/specified_sheets.png)| Specification says: 1, then 4 through 10|

Remarks:

- This is only a _specification_.
  To be really processed, each selected sheet must also be _valid_.
- A sheet specification must be written in a strict increasing order.
- Specifications "1,4-10" and "1,4,5,6,7,8,9,10" are equivalent.
- Since there is a gap in this example (sheets 2 and 3 are not selected), exporting the book
  to disk or plugin will result in at least two separate movements, one for sheet 1 and one
  for sheets 4-10.
- If the specification string is blank or null, all sheets are selected by default. [^empty_spec]
- A sheet specification can also be provided in batch via the `-sheets` argument on the command line interface.
- The latest sheet specification is persisted in the book `.omr` project file
  when specified via the GUI dialog but not when specified via the command line interface in batch.

---

[^empty_spec]:
    In Audiveris 5.2, a null specification used to mean _all sheets_
    while an empty specification used to mean _no sheets_.  
    Starting with Audiveris 5.3, for the sake of consistency with the new "Split and merge" feature,
    they now both mean the same thing: _**all sheets**_.
