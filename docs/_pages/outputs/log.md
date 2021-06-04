---
layout: default
title: .log
grand_parent: References
parent: Outputs
nav_order: 1
---
## .log files
{: .no_toc :}

---
Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}
---

### Session global log

All messages displayed in Audiveris events window during the same session are also written into a
separate log file located in Audiveris log folder (see [Cached Folders](../folders/cached.md)).

The log file is named like `20180725T185854.log`, according to the date and time of session start,
formatted as "yyyymmdd" + T + "hhmmss".

Such log is a simple text file meant for later analysis.
In particular, it is very useful when filing a bug report on
[Audiveris Issues](https://github.com/Audiveris/audiveris/issues) site.

The advanced user can precisely customize the logged information by manually editing the logging
configuration file `logback.xml` located in Audiveris config folder
(see [Essential Folders](../folders/essential.md)).

### Batch books logs

When running in batch, Audiveris can process hundreds of books in a row.
As mentioned above, this results in one global log file, perhaps a huge one.

So, in addition to the global log file, the batch processing of each book stores all the
book-related log events in a separate book log file.

This book log file has a name formatted as `bookname-dateTtime.log`.
It is located in the book subfolder itself.

This eases later analysis of batch processing for any specific book.
