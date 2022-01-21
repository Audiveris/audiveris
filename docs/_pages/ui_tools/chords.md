---
layout: default
title: Chords
grand_parent: User Edition
parent: UI Tools
nav_order: 12
---
## Chords
{: .no_toc }

You should know by now that clicking on a note selects that note rather than its containing
chord (this is the rule known as "_member over ensemble_").
However, by selecting one or several notes, you can indirectly select and act on these chords.

This is made possible via the usual popup menu which can provide a specific `Chords...`
sub-menu, whose content tightly depends on the chords configuration.

---
Table of contents
{: .no_toc .text-delta }

1. TOC
{:toc}
---

### Chords menu

Here below, we have selected two notes as indicated by the arrows: one note head and one rest,
before opening the popup/Chords menu:

![](../assets/images/chords_selection.png)

Notice that the global bounding box (red rectangle) encompasses the bounds of both chords.

Also, the menu begins with information lines about the selected chords.
And if you hover over these lines, the bounding box is dynamically updated to show just the
selected item.
This is meant to allow a visual check of the selected chords:

| all chords | first chord | second chord |
| --- | --- | --- |
| ![](../assets/images/chords_both.png) | ![](../assets/images/chords_one.png) | ![](../assets/images/chords_two.png) |

To ease manual dealing with chords, you can make each chord ID visible as in the picture below
(use pull-down menu `View | Show chord IDs`):

![](../assets/images/chord_ids.png)

NOTA: The example of Chords menu above shows only a partial list of possible chords actions,
because the list depends on the current status and configuration of the selected chords.

Here after, we list all the possible items of `Chords...` menu.

### Chord

The gathering of note heads into chord(s) may need some user correction.

#### Split

| One chord? | Two chords? |
| --- | --- |
| ![](../assets/images/chord_split_1.png) | ![](../assets/images/chord_split_2.png) |

The OMR engine may have considered this is just one chord with a long stem, whereas we find
these are in fact two separate chords, one above the other.
In that case, select this chord and use the Split command.

![](../assets/images/chord_split.png)

#### Merge

Or, just the opposite, we want to merge these two chords into a single one.
In that case, we select both chords and use the Merge command.

In the specific case of whole notes, the merge command is often needed.
Because there is no stem involved, the engine has no clear heuristic [^whole_chord]
to gather whole heads into one chord.

![](../assets/images/chord_merge.png)

### Voice

A voice is defined as a sequence of chords (head chords and rest chords) in the same music part. [^voice_sharing]

Audiveris algorithm for voice building is already very tricky.
It tries to reconcile different heuristics, but in some cases the result may not be the one the
user would expect.

Purpose of these voice actions is to guide the engine in voice building.

#### [cancel] Next in Voice

![](../assets/images/next_in_voice.png)

"Next in Voice" command operates on a horizontal sequence of 2 chords,
say chord A and chord B, by establishing a relation from A to B,
stating two things:
1. That B voice should be dynamically **copied** from A.   
   If later, for some reason, voice for A gets modified, the same modification will propagate to B.
2. And that B should start at (A start time + A duration).   
   In other words, B time slot should **immediately** follow A time slot.

So we have a double dynamic propagation rule: on voice and on time.
This is the reason why "Next in Voice" should be chosen whenever possible over "Same Voice".

Here is the result:

![](../assets/images/next_in_voice_after.png)

You can always **undo** such task, as any other UI task described here,
via `Sheet | Undo` or `Ctrl-Z` standard commands.

Also if you want, much later in the process, to cancel this task, you can always get back
to selecting the same chords and you'll be offered to **cancel** the task.   
Cancelling this action removes the relation (and thus the related guidance).

#### [cancel] Same Voice

"Same Voice" is weaker than "Next in Voice".

It states only that the two selected chords should belong to the same voice.

It provides no information about related time values.   
This is the reason why "Next in Voice" relation should be preferred whenever possible.

Note that "Same Voice" (or "Next in Voice") can be used in the context of rest chords interleaved
between beam head chords, to "push" these rests into the beam area.
And conversely, "Separate Voice" can be used to "pull" these rests out of the beam area.

#### [cancel] Separate Voices

This command imposes the voice algorithm to assign the selected chords to **separate** voices.

Note this is not exactly the reverse of "_next in voice_" command (or weaker "_same voice_" command):
* Without any command, you let the algorithm decide with no guidance.
* With a command (whether it's _next_, _same_ or _separate_), you explicitly guide the algorithm.

#### [cancel] Preferred Voice

Whereas "Next in Voice/Same Voice" and "Separate Voices" commands operate on 2 chords,
by establishing a **dynamic** computing rule from chord A to chord B,
"Preferred Voice" operates on the single chord at hand, by assigning this chord a **fixed** voice
numeric value.

This feature is still experimental, and is effective only on the _first chord_ in each measure voice.

It can be useful only for the very first chord of a voice in a system:
- Because the A-B relations can take place only within the same system (SIG), there is no way to
establish a voice computing rule across systems.
- Specifically, we may need a way to set the voice number of a chord at start of
the very first measure of a movement.

These are the two cases where this feature can be useful.   
But except for these very specific cases, you are advised to not use this feature.
In particular, do not think you could use it to somehow build voice sequences of chords!

### Time

Assigning a chord to proper time slot is as tricky as voice assignment.
In fact, time and voice algorithms are tightly coupled.

When two chords are rather close abscissa-wise, when should we consider them as part of the same
time slot?

#### [cancel] Same Time Slot

![](../assets/images/same_slot.png)

Here, we can see that time slots on second staff of the part are not correctly assigned.
This is because the whole note on upper staff and the 8th note on lower staff are too far apart
abscissa-wise.
So, we force these two notes to share the same time slot.

Experience shows that the most efficient action is generally to grab the set of _all_ the chords
that should share the same slot (a rather vertical selection **within the same part**)
and apply the _Same Time Slot_ command on the whole set.

#### [cancel] Separate Time Slots

As opposed to "Same Time Slot", this command is used to force time separation between two
chords that the engine has considered as adjacent.

---
[^voice_sharing]: There is an on-going debate about the possibility for Audiveris to share [rest-] chords between voices. But for current 5.2 release, a chord can be assigned to exactly one voice.

[^whole_chord]: Current heuristic for whole chords is to gather whole heads if they are aligned vertically and not more than one interline apart.
