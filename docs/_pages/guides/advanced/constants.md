---
layout: default
title: Constants
parent: Advanced features
nav_order: 4
---
# Constants
{: .no_toc }

There is (or should be) no hard-coded constant in Audiveris code.
Instead, algorithms are backed by "_application constants_" (more than 800 of them today).
This mechanism is a low level yet powerful way to handle nearly all application tuning data.

This data is presented as constants to the end user, and is modifiable at run time:
* Through the CLI `-constant KEY=VALUE` argument
* Through the pull-down menu {{ site.tools_constants }}

---
Table of contents
{: .no_toc .text-epsilon }
1. TOC
{:toc}
---
## Dialog

The display below combines a tree of classes on the left side, and a table on the right side,
where details of the constants from the containing classes are available for display and modification.

![](../../assets/images/constants.png)

The picture represents the top of the scrollable Constants view:
We are located at the top of Audiveris software, with the root packages: `org`, `audiveris`, `omr`.

To ease the browsing of all constants, there is a `Search` area, where the user can enter some text
to be searched for among the class names, constant names and constant descriptions.
The search is not case-sensitive.

Let's suppose we would like to somewhat relax the constraint on the horizontal distance between
an accidental alteration sign (like a sharp) and the note head on its right side.

We can enter `accid` in the search area and press the search button (perhaps several times)
until we get to several interesting constants.
Here is what we can read:

![](../../assets/images/constants_accid.png)

1. We are in the package named `relation`, (its full name is org.audiveris.omr.sig.relation)
and the class named `AlterHeadRelation`, which governs a potential relation between an alteration
and a head.
2. Among the various class constants, we are interested by `xOutGapMax` and `xOutGapMaxManual` which
define the maximum horizontal gap between an accidental alteration and a note head.
The former is meant for the OMR engine, the latter for manual assignment.
Threshold for manual assignment is larger,  because we can offer to be less strict when the user
is in the driver's seat.
3. Notice that these value fields are coded using "interline" fractions,
(2.0 and 3.0 on this picture) to be scale independent.
If we have a selected sheet at hand, the previous column (`Pixels`) displays the corresponding
number of pixels using the sheet interline scale (42 and 63 on this picture).
4. We can directly select the value field by a double-click and then type a new
value there.
The new value applies immediately.

## Constants Lifecycle

The overriding mechanism is defined as follows, from lower to higher priority:

1.  FACTORY: The default value as defined in the source code,
2.  USER: The modified value, if any, persisted in a specific `run.properties` file
located in the user [config folder](../../reference/folders/essential.md#config-folder),
3.  CLI: The value, if any, specified in the command line by a `-constant key=value`.
The CLI value is persisted in the USER file when running in interactive mode,
and not persisted when running in batch.
4.  UI: The value, if any, specified through the {{ site.tools_constants }} user interface.
These UI values are persisted in the USER file.

If ever we want to discard the modification we have made and get back to the FACTORY value of an
constant, we simply uncheck the box in the `Modif` column of this constant in the Constants window.

To restore the FACTORY value for **all** constants, we use the `Reset` button located at the top of the
Constants window.
Needless to say, we will be prompted for confirmation beforehand...
