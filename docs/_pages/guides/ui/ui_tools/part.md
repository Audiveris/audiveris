---
layout: default
title: Part merge
parent: UI tools
nav_order: 8
---
# Part merge
{: .no_toc }

During the ``GRID`` step, the OMR engine detects staves and assigns exactly one part per staff,
unless:
* There a multi-staff brace on the left margin of the staff, and
* The staves joined by the brace are also joined by connectors further along the staves
(a connector is a vertical concrete segment joining two barlines).

If these two conditions are met, the two staves (more rarely three staves) are considered to
refer to the same instrument (e.g. piano or organ) and thus to a single common part.

Some score images exhibit damaged braces, impeding the detection of a common part.

This is the case in the following example, where a poorly done scan has cropped an important
portion on the image left side
(the faint red cross is just the current location as given by the user):

![](../../../assets/images/brace_missing.png)

So, this leads the OMR engine to detect a system with 3 parts.

To fix this, we manually drag & drop a brace `Inter` from the shape palette to where there should be
a brace.

Let's pay attention to hover over a staff of the target system.
The brace ghost will turn from dark-gray to green, with a red segment going from brace center
to staff middle line:

![](../../../assets/images/brace_dropped.png)

We can now drop the brace.
The drop will commit the merge of the two embraced staves into a single part.

We can undo this operation (or remove the manual brace, which is equivalent).

{: .note }
This is an _ad hoc_ feature, meant to fix a brace cropped out.
It works only by inserting a **manual** brace.  
We can still slightly shift or resize the brace if so desired.
But let's not try to extend the brace to a 3rd staff, this wouldn't work.
