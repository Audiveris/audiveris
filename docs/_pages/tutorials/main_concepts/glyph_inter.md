---
layout: default
title: Glyph vs. Inter
parent: Main concepts
nav_order: 3
---
# `Glyph` vs. `Inter`

## `Glyph`

A `Glyph` is nothing more than a set of foreground (black) pixels
in a sheet binary image.

It carries no shape.

It is not related to a staff.
It does not even belong to a system.
The reason is there is no reliable way to assign a glyph located in the "gutter" between two systems
or two staves: does it belong to the upper or the lower system/staff?

These restrictions on `Glyph` don't apply to glyph interpretations (a.k.a. `Inter`).

## `Inter`

An interpretation, or **`Inter`** for short, is meant to formalize one reasonable
interpretation of a `Glyph`.

There may be several reasonable interpretations for a given glyph and, in many cases,
the OMR engine cannot immediately decide on the right interpretation among these mutually
exclusive interpretations.
This decision will then be postponed until later down in the OMR process,
when additional information (such as the discovery of other `Inter` instances located nearby)
becomes available and helps clarify the configuration.

As opposed to a `Glyph`, an `Inter` belongs to a system and is often related to a staff.

It carries a _shape_ and a _grade_ in [0..1] range, which can be considered as
the probability for the interpretation to be a true positive.  
This grade is an interpretation _intrinsic_ grade, only based on the glyph at hand in isolation
(this grade is often provided by the glyph classifier).

Later, the `Inter` will generally be assigned a _contextual_ grade, based on the `Inter` intrinsic grade
and the supporting relations with other `Inter` instances nearby.

## Typical display example


|   View  |  `Inter` over `Glyph`   |
| --- | --- |
|![](../../assets/images/glyph_inter.png) | Here we have a Treble Clef `Inter` displayed in dark blue. <br> Its related `Glyph`, using pale blue and pink colors, is mostly hidden behind. |
