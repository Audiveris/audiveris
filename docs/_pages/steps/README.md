---
layout: default
title: Steps internals
parent: References
nav_order: 1
has_children: true
---
# Steps internals

All the 20 steps of the OMR engine are presented here in chronological order.

A step presentation is generally organized in:
- goal
- inputs
- outputs
- main processing operations

Some steps work on the sheet as a whole.  
Other steps work system per system, and can present:
- sheet-level prolog
- system-level processing
- sheet-level epilog

{: .note :}
This type of documentation is halfway between a user manual (functional) and a developer manual (implementation).
Reading it is not obligatory, but should help understand step by step how the engine behaves and how to best control it.