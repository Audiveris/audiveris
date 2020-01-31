---
---
## Scanning of paper scores

Audiveris doesn't support direct scanning because there is still no open-source solution for
cross-platform access to scanning devices from Java projects.
You will need to use an external scanning utility.
Fortunately, all major scanner manufacturers offer such utility.
There is also a variety of freely available scanning software for all common operating systems.

When scanning for music recognition, please pay attention to the following advices:

* Prefer grayscale images to black-and-white or color images.
This will help avoid unwanted symbols distortion in the scanning software.
Audiveris will binarize the image by itself using a specific adaptive algorithm.

* Choose an optimal image resolution according to the following criteria:
  * Too low resolution (below 200 DPI) may hide key details while too high one
  (more than 500 DPI) turns quickly into a significant waste of CPU and memory resources
  without any advantage for the recognition.
  * As a rule of thumb, **300 DPI** is generally a good resolution for a paper of standard A4 size.
  For scores that exhibit small symbols, you'll need to increase the image resolution to 400 DPI.
  * An even better rule is to scan the image so that the resulting vertical distance between two
  staff lines is about **20 pixels**.

* Paper placement should be made carefully to avoid image rotation, warping, shadows and dark
vertical stripes.
Audiveris usually detects any rotation and/or warping but, to keep image quality intact,
it never attempts to graphically correct them.
Instead, the distorted staff lines are used as local references for any symbol processing.

* Any post-processing of scanned images in the scanning software should be better turned off.
Special settings like "Line Art", "Text", "Dithering" or "Halftone" should be disabled because
they often lead to unwanted image distortions.   
Prefer a dedicated image processing software in order to carefully enhance low-quality images
when needed.
You can refer to the [Improve Input](../advanced/improve_input.md) section.
