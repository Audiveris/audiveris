---
layout: default
title: Java
nav_order: 1
parent: Installation
---
# Java
{: .no_toc }

## Java runtime environment

To check the installed Java environment, you can use the `java -version` command from a terminal window, to provide Java information like:
```
$ java -version
java version "17.0.6" 2023-01-17 LTS
Java(TM) SE Runtime Environment (build 17.0.6+9-LTS-190)
Java HotSpot(TM) 64-Bit Server VM (build 17.0.6+9-LTS-190, mixed mode, sharing)
```
If you don't have a suitable Java Runtime Environment (JRE) yet, you must install one.
If a JRE is not available for download, you can pick up a JDK (Java Development Kit)
which is a superset of a JRE.

Make sure to install **Java version {{ site.java_version }}**
(which is today the latest LTS - Long Term Support - version) for instance from
[https://www.oracle.com/java/technologies/downloads/](https://www.oracle.com/java/technologies/downloads/)

Audiveris {{ site.audiveris_version}} will not work with older Java versions.  
It may work with newer versions. This section will be updated as new results become available.

### 64-bit architecture

The latest Audiveris version ({{ site.audiveris_version}})
uses Java {{ site.java_version }}.  
Starting with Java 10, Oracle now provides Java environments only for 64-bit architecture.

So a 64-bit architecture is now required.
