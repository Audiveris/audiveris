---
layout: default
title: LOAD step
parent: Steps internals
nav_order: 1
---
# LOAD step
{: .no_toc }

It is the very first step applied when processing any sheet.
Its goal is to provide the sheet gray image, whatever the input color model.

## Inputs

- The path to the book input file
- The sheet number within the book -- 1 by default

## Output

- The gray image of the sheet

## Processing

1. A proper image loader is chosen according to the extension of the input file name
(``.pdf``, ``.tif``, ``.png``, ``jpg``, etc),
2. The loader then extracts the input image, based on the sheet number,
3. The image is discarded if its size is too large,
4. According to its color model, this initial image is converted to a gray scale of pixels
-- ranging from 0 for a black pixel to 255 for a white pixel.
    - The alpha channel if any is discarded,
    - If RGB channels are provided, the gray value for each pixel is set as
    the highest value among the 3 RGB values.

