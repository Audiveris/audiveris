---
layout: default
title: Installation
nav_order: 1
has_children: true
---
# Installation

For **Windows** only, there is an **installer** available in 64-bit version,
which takes care of installing all Audiveris binaries.  
The installer makes installation simple but has the main drawback to be limited to
the last official release, which can be several months old.

For all three major operating systems, that is **Windows**, **Linux** and **MacOS**,
you have the ability to **download and build** from source material.  
The main advantage is to benefit from all the bug fixes and improvements that are
continuously published on Audiveris project site in its "development" branch.

{: .warning }
In both cases, whether you decide to use the installer or to build Audiveris from source,
there are two points to take care of:

{: .highlight }
Point #1: Proper **Java** version must be installed.  
Audiveris version {{ site.audiveris_version }} requires Java version {{ site.java_version }} or up. 

{: .highlight }
Point #2: Suitable Tesseract **OCR language** files must be locally available.  
Audiveris uses Tesseract OCR in legacy mode, which requires the standard (best) tessdata files.

