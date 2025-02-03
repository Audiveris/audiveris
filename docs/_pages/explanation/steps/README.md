---
layout: default
title: Steps internals
parent: Explanation
nav_order: 1
---
# Steps internals

All the 20 steps of the OMR engine are to be presented here in chronological order.

This is a work in progress, to date only the first 9 have been documented in this handbook.

A step presentation is generally organized in:
1. goal
2. inputs
3. outputs
4. main processing operations

Some steps work on the sheet as a whole.  
Other steps work system per system, and can present:
- sheet-level prolog
- system-level processing
- sheet-level epilog

{: .note :}
This type of documentation is halfway between a user manual (functional) and a developer manual (implementation).
Reading it is not mandatory, but should help understand step by step how the engine behaves and how to best control it.

