---
layout: default
title: Standard folders
parent: Folders
nav_order: 1
---

# Standard folders
{: .no_toc }
{: .d-inline-block }
changed in 5.4
{: .label .label-green}

This is where Audiveris stores all score outputs, such as the `.omr` project
files, the `.pdf` printouts, the `.mxl` MusicXML files, etc.

Of course, we always have the ability to interactively store output
by using the commands that prompt for a target location, like:
- {{ site.book_print_as}}
- {{ site.book_export_as}}
- {{ site.book_save_as}}

Also, if we load an existing `.omr` file, perhaps to further modify it,
it will be saved by default to the same location it was loaded from.

Now, if we are processing some input file, say `foo.pdf`, the question is:
where will its output files (`foo.omr`, `foo-print.pdf`, `foo.mxl`, ...) be stored *by default*?

The target locations are presented in the following sections, by decreasing priority.

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---

## Command line option

The CLI option `-output <output-folder>`, if present, defines the target folder for every output.

For instance, the transcription of our `foo.pdf` input will be stored by default as `<output-folder>/foo.mxl`

This specific CLI option overrides the general `Preferences` described in the sections below.

## Preferences dialog

The [Preferences](../../guides/main/preferences.md) dialog is accessible via {{ site.tools_preferences }}.

### Input sibling

The  `Preferences` dialog offers an option named `Input sibling`.

When this option is set, any output is stored next to its input.

For instance, the transcription of `/some/path/to/foo.pdf` will be stored as `/some/path/to/foo.mxl`
(assuming the folder `/some/path/to/` is writable).

### Browse

If the `Input sibling` option is OFF, the `Browse` button allows to select an output folder.

For instance, if we have selected the output folder `/my/folder/`, 
the transcription of `/some/path/to/foo.pdf` will be stored as `/my/folder/foo.mxl`.

### Separate folders

If the `Input sibling` option is OFF, the `Separate folders` option allows to gather all the 
outputs of an input file into a separate folder named according to the input radix.

For instance, still using the `/some/path/to/foo.pdf` input, the various outputs will be gathered
into the specific folder `/my/folder/foo/`, thus resulting in something like:

```
    my/folder/
    ├── foo/
    │   ├── foo.omr
    │   ├── foo.mxl
    │   └── foo-samples.zip
    ├── ...
    ...
```