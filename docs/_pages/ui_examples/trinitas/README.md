---
layout: default
title: O lux beata Trinitas
grand_parent: User Edition
parent: UI Examples
nav_order: 1
---
# O lux beata Trinitas
{: .no_toc }

**NOTA**:
You will notice that description of user edition is very detailed in this example, perhaps too much.
This was done on purpose, so that the session content could be used as introductory material.
{: .nota-bg }

The case at hand describes the processing of an old office hymn available on IMSLP site.
You can download it directly via the button here below:

[O lux beata Trinitas](https://imslp.org/wiki/O_lux_beata_Trinitas_(Alberti%2C_Johann_Friedrich))
{: .btn .text-center }

The case originated from [issue #481](https://github.com/Audiveris/audiveris/issues/481)
posted on Audiveris forum, which led to several discussions and engine improvements,
and finally it's integration as a UI example into Audiveris handbook.

Main score specificities:
- Book of 3 movements over 5 sheets
- Non-standard time signature
- Whole rests that are not measure-long rests
- Voice sequences of whole note heads

Main UI actions:
- Use of "partial whole rests" option
- Common-cut (2/2) replaced by 4/2 time signature
- Missing half notes and whole notes
- Chords with no time offset
- Voice alignment

---
Table of contents
{: .text-delta }

1. TOC
{:toc}
---

## Program launch

There are many ways to install/build then launch Audiveris program, depending on your environment.

If you haven't done so, move to [Audiveris releases on github](https://github.com/Audiveris/audiveris/releases) to retrieve the latest release.

Under Windows, you can simply run the installer (for example Audiveris_Setup-5.2.2-windows-x86_64.exe
).
Under Windows, Linux and MacOS, you can decide to clone Audiveris project locally and then build it.
In any case, you will have to make sure that proper Java version (Java 11) is installed.

Here below are the first messages displayed, when pressing Windows start icon,
selecting Audiveris folder, then Audiveris program:

```
Audiveris version 5.2.2

LogUtil. Property logback.configurationFile not defined, skipped.
LogUtil. Configuration found C:\Users\herve\AppData\Roaming\AudiverisLtd\audiveris\config\logback.xml
LogUtil. Logging to C:\Users\herve\AppData\Roaming\AudiverisLtd\audiveris\log\20210710T121739.log

Loaded plugins from C:\Users\herve\AppData\Roaming\AudiverisLtd\audiveris\config\plugins.xml
Classifier loaded XML norms.
Classifier data loaded from default uri file:///D:/soft/audiveris-github/development/res/basic-classifier.zip
Versions. Poll frequency: Weekly, next poll on: 17-Jul-2021
```

Quick explanations about these early messages :

1. Program version (e.g. 5.2.2)
2. Logging:   
   - Logging can be customized, here we are using a user-defined configuration file.  
   - All session messages are also written into a dedicated user file,
     named according to session date and time (e.g. 20210710T121739.log)
3. Configuration:   
   - User plugins if any to ease access to companion programs like MuseScore, Finale, etc.
   - Glyph classifier trained data.
4. Check for new version on GitHub site

## Initial processing

1. Let's load the input file O.lux.beata.Trinitas.Alberti.Johann.Friedrich.pdf into Audiveris 5.2.    
   A quick look at the images shows it is of rather good quality.   
   So, via `Book | Transcribe Book` menu item, let's launch the whole book transcription...   
   Done in 2'30, about 30" per sheet.

2. The last message says `[O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Scores built: 3` which
   means that 3 movements have been transcribed in this book.

3. Before getting into user actions, let's save the current project status,
   via command `Book | Save Book` or command `Book | Save Book as...`.   
   We choose the latter to shrink project name from "O.lux.beata.Trinitas.Alberti.Johann.Friedrich"
   to simply "Trinitas".
   ```
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Stored /book.xml
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Stored /sheet#1/BINARY.png
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Stored /sheet#1/sheet#1.xml
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Stored /sheet#2/BINARY.png
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Stored /sheet#2/sheet#2.xml
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Stored /sheet#3/BINARY.png
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Stored /sheet#3/sheet#3.xml
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Stored /sheet#4/BINARY.png
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Stored /sheet#4/sheet#4.xml
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Stored /sheet#5/BINARY.png
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Stored /sheet#5/sheet#5.xml
   [O.lux.beata.Trinitas.Alberti.Johann.Friedrich] Book stored as D:\soft\cases\Issue-481\Trinitas.omr
   ```
4. Top of Audiveris window is now like this:   
   ![](initial_trinitas.png)   
   Notice the "memory meter" on window upper right corner which gives 324/706 MB  
   Notice also that we have the 5 sheets of Trinitas book in memory.
   This is indicated by the 5 tabs on window left side:
   there are 5 tabs because the book contains 5 sheets, and these tabs appear in standard font
   because they are currently loaded.   
   5 sheets is not a really high number, but let's do something to save on memory, an action
   specifically useful for books with dozens of sheets or more:   
   Command `Book | Swap Book Sheets` gets rid of all sheets from memory
   -- except the current sheet and after storing them to disk if so needed --   
   ```
   Disposed sheet#2
   Disposed sheet#3
   Disposed sheet#4
   Disposed sheet#5
   ```
   Audiveris window has changed:
   ![](initial_swapped.png)   
   All tabs, except tab #1, now appear in gray and memory meter now shows 99/512 MB.
   We can select again any of these gray tabs, and the corresponding sheet gets reloaded from disk.
   ```
   [Trinitas#2] Loaded /sheet#2/sheet#2.xml
   [Trinitas#3] Loaded /sheet#3/sheet#3.xml
   [Trinitas#4] Loaded /sheet#4/sheet#4.xml
   [Trinitas#5] Loaded /sheet#5/sheet#5.xml
   ```   
   If you look carefully, you can see that, even with all sheets back in memory, the total memory
   consumption is just 164/531 MB, far below the initial 324/706.
   This is so because a bunch of transient data, needed to transcribe sheets, is no longer needed
   and has been disposed of, via the store/reload process.

   Even more efficiently, we could also:
   1. Close the book using `Book | Close Book` command (or `Ctrl-W` shortcut).
      We would be prompted to save the book if so needed.
   2. Reopen the latest closed book (at its latest opened sheet), via
      `Book | Most recent book` command (or `Ctrl+Shift-T` shortcut).
   3. Going through all book sheets, total memory would stay at about 105 MB.

## Raw results

Here are sheet results, right out of the OMR engine:   
We have selected `View | Show score voice` or `F8` command so that voices in the same part
be displayed in different colors.   
-- Note: You can ask your browser to display each of these sheet images in a separate tab --

| Sheet#1 | Sheet#2 | Sheet#3 | Sheet#4 | Sheet#5 |
| :---: | :---: | :---: | :---: | :---: |
| ![](sheet1_init.png) | ![](sheet2_init.png) | ![](sheet3_init.png) | ![](sheet4_init.png) | ![](sheet5_init.png) |

First impression is that almost all measures, except for sheet #3, are displayed in pink!

A **pink** measure indicates that **rhythm** analysis has failed for that measure:
- Either because some chord starting time could not be determined,
- Or because some voice was computed to complete after the theoretical measure end,
  as derived from time signature.

So, what caused such a disastrous rhythm result on a rather good input?

We can double-check the time signatures. All have been correctly recognized:
* **Common-cut** beginning of sheet #1
* **3/4** beginning of sheet #3 (the one with no signaled errors)
* **Common-cut** beginning of sheet #4

Let's have a closer look, with colorized voices (`View | Show score Voices`) and
with chord IDs displayed (`View | Show chord IDs`).

**NOTA**: Chord IDs are dynamically assigned within each sheet by Audiveris program.
If you replay this processing even with just a slight difference in the engine code or parameters,
some chord IDs may be different that those listed in this handbook version.
Purpose of using chord IDs is to ease user mapping between snapshots and text lines.
{: .nota-bg }


## Sheet #1

### Sheet #1, Measure #1
First measure of sheet #1 looks like:

![](sheet1_m1.png)

1. Note the presence of "breve rests" (chords #3375 & #3376) on upper staff.
Such multiple rest is supposed to last two bars.

2. Note also the presence in lower staff of a dotted whole head (chord #2859),
followed by a half note (chord #2837), for a total duration of 2 for voice #6.

So, there is a conflict between the actual duration of this measure
(and of almost all other measures in the same page) with the time signature:
2 for each measure vs 2/2=1 expected by the common-cut time signature.

What is the real "value" of a common-cut time signature?
This [Wikipedia Alla breve article](https://en.wikipedia.org/wiki/Alla_breve) sheds an interesting
light, especially with the (1642-1710) indication on score top right corner.
See also this
[Ultimate music theory article](https://ultimatemusictheory.com/breve-note-and-breve-rest/).
Instead of 2/2, it would be worth 4/2.

So let's replace all the common-cuts by explicit "4/2" time signatures.

To delete:
- We select the common-cut Inter and press `DEL` key.
  Or click on `Deassign` button in Inter board.
  Or use `<popup> | Inters...` contextual menu.

Inserting 4/2 signature is more complex, since this is not one of the predefined time signatures:
1. From the shape palette, we select the "Times" family (the family figured by a 4/4 sign) and
   from this shape family, we drag n' drop the custom (0/0) time signature in correct location.
2. Then in Inter board, we replace the custom 0/0 value by 4/2 value and press `Enter`.

We do this for both common-cut sigs on first measure of sheet #1.
We'll do the same thing on sheet #4 later.

NOTA: If you delete both original common-cut sigs before entering the new 4/2 values, you will
notice that all measures of sheet #1, except measure #12, will turn white.   
The reason is that at this moment we have no time sig at all, hence no length-check can be
performed on measures.  
[In measure #12 remaining pink, we'll later see that a whole note has not been recognized].
{: .nota-bg }

With 4/2 clearly stated, among the 16 measures in sheet, we now have 11 in white.
A significant improvement! :-)

### Sheet #1, Measure #4

All the 4 measures of first system look nice, with 2 voices in upper staff and 2 other voices
in lower staff, except the last one (measure #4) which exhibits 3 voices in upper staff:

![](sheet1_m4.png)

Referring to voice legend ![](../../assets/images/voice_colors.png),
this upper staff contains:
1. Voice #1, chord #3382 made of a breve rest, horizontally located at measure center,
2. Voice #2, chord #3381 made of a whole rest, located at beginning of measure,
3. Voice #3, chord #2836 made of a whole note, located at 2/3 of measure.

The problem is with the whole rest (chord #3381) which, as a measure-long rest, is the only chord
in its voice but should be located at measure center.
In fact, and there are other similar cases in this book, the whole rest is never at measure center
but located as a plain chord in a time slot.

We can visualize the content of first slot,
using ![](../../assets/images/ModeCombined.png) combined mode and pressing left mouse
button near the first slot.
Chord #3381 is *not* part of this slot:

![](sheet1_m4_slot1.png)


This is the purpose of a new option accessible via `Book | Set Book Parameters`,
and named "Support for partial whole rests".
When this option is on for the sheet at hand, whole rests are no longer considered as
measure-long rests, they are just plain rests with a duration of 1/1:
- They belong to a time slot,
- Their voice can contain other notes or rests.

So, we open the `Book | Set Book Parameters...` dialog, and:
1. We *set* this option for the whole book (so that it applies to all sheets in book)
2. We explicitly *unset* this option for sheet #3.

| Whole Trinitas Book | Except Trinitas Sheet #3 |
| :---: | :---: |
| ![](partial_whole_rests_book.png) |  ![](partial_whole_rests_sheet.png)  |

We don't forget to press the `OK` or `Apply` button to commit the parameters.

Then, we use contextual `<popup> | Page #1 | Reprocess rhythm` to update the page.

Measure #4 now looks like this (notice chords #3381 and #2836 are now in the same green voice):

![](sheet1_m4_ok.png)

And `<popup> | Measure #4 | Dump stack voices` dumps its strip as:

```
MeasureStack#4
    |0       |1/4     |1/2     |3/4     |1       |3/2     |7/4     |2
--- P1
V 1 |Ch#3382 ======================================================|M
V 2 |Ch#3381 ===========================|Ch#2836 ==================|2 (ts:2/1)
V 5 |Ch#2849 =========|Ch#2858 ==================|Ch#2854 |Ch#2855 |2
V 6 |Ch#2850 |Ch#2851 |Ch#2852 |Ch#2853 |Ch#2861 ==================|2
```

We can see that voice #1 end time is displayed as 'M' (M for Measure), meaning full measure duration,
which is OK for a measure-long rest as the breve rest.

Also, voice #2 now contains chord #3381 (whole rest, but not measure-long rest)
followed by chord #2836 (whole note), to sum up to value 2 as the following voices
in measure.

Here is the updated view on first slot content
(which now includes the whole rest in upper staff):

![](sheet1_m4_slot1_ok.png)

### Sheet #1, Measure #1 (again)

This "Support for partial whole rest" was key to fix rhythms of measures like
measure #4 we just saw.

But, before we set this option, measure #1 already appeared OK.
In fact, its whole rest of chord #3374 had been mistaken for a half rest:

| Data view | Binary view |
| :---: | :---: |
|![](sheet1_m1_half_rest.png)|![](sheet1_m1_binary.png)|

If we click on this rest, the Inter board gives HALF_REST shape.
But the binary view clearly indicates it should be a WHOLE_REST shape.

The measure strip confirms the fact that chord #3374 duration is only 1/2,
resulting in a voice ending at 3/2 instead of 2:
```
MeasureStack#1
    |0       |1/2     |3/2     |2
--- P1
V 1 |Ch#3375 ==================|M
V 2 |Ch#3376 ==================|M
V 5 |Ch#3374 |Ch#2856 |........|3/2
V 6 |Ch#2859 =========|Ch#2837 |2

```

In fact, this is a limitation of the current engine with its glyph classifier.    
Within staff height, with staff lines "removed", the glyph for a half rest and the glyph
for a whole rest are nearly identical -- a horizontal rectangle.   
Only the position relative to the staff line nearby allows the engine to make the
difference between:
* a whole rest -- close to the staff line above --
* and a half rest -- close to the staff line below --

In the case at hand, the rest glyph is badly located with respect to the staff
(see its base aligned with the next ledger on its right side),
it was thus mistaken for a half rest.

No big deal, we can easily delete this wrong inter and, re-using its underlying glyph,
manually create a true whole rest inter.
```
MeasureStack#1
    |0       |1       |3/2     |2
--- P1
V 1 |Ch#3375 ==================|M
V 2 |Ch#3376 ==================|M
V 5 |Ch#3420 |Ch#2856 =========|2 (ts:2/1)
V 6 |Ch#2859 =========|Ch#2837 |2
```
A new chord (#3420) has replaced the former chord #3374, and the measure is now OK.

### Sheet #1, Measure #5

![](sheet1_m5.png)

This pink measure exhibits a rather obvious problem:
- A false 16th rest on the upper left corner (chord #3391).   
  Hint: the measure background goes much too high with respect to the staff line,
  indicating some abnormal chord located there.
  We can simply delete it.

This modification automatically triggers a rhythm update for the measure at hand:

![](sheet1_m5_ok.png)

### Sheet #1, Measure #7

![](sheet1_m7.png)

The lower staff of this pink measure exhibits a sequence of 2 whole notes,
together with additional quarter and half notes, all in the same voice!
This cannot fit within a 4/2 measure duration, and is confirmed by the measure strip:

```
MeasureStack#7
    |0       |1/4     |1/2     |1       |3/2     |5/2     |3
--- P1
V 1 |Ch#2874 ===========================|Ch#2869 |........|2
V 2 |Ch#2880 ==================|Ch#2876 =========|........|2 (ts:2/1)
V 5 |Ch#2892 |Ch#2894 |Ch#2905 =========|Ch#2907 |Ch#2896 |3
V 6 |Ch#2893 =========|Ch#2895 |........|........|........|1
```

The fix is to force these 2 whole notes (chords #2905 & #2907) to be in separate voices:

![](sheet1_m7_separate_voices.png)

![](sheet1_m7_ok.png)

Since chord #2905 is still selected, it displays the "Separate Voice" relation with chord #2907
as an orange-colored segment.

If we increase the zoom ratio to 2 or above, there is now enough display room,
and the relation name "SeparateVoice" explicitly appears:

![](sheet1_m7_separate_relation.png)

### Sheet #1, Measure #11

![](sheet1_m11.png)

On upper staff, 3 whote notes appear in the same voice #1 which is really too much,
while a half note stays alone in voice #2!

We can either separate the first 2 wholes (chords #2934 and #2936)   
or we declare half note (chord #2925) and whole chord #2936 as being in sequence in the same voice.

Either way is OK.

To choose the latter, we select the "Next In Voice" relation between #2925 and #2936.
![](sheet1_m11_sequence.png)

And result is now OK:
![](sheet1_m11_ok.png)

### Sheet #1, Measure #12

![](sheet1_m12.png)

A missing whole note at beginning of upper staff.

We select the glyph (which is almost intact, despite the half head on right) and assign it the
whole note shape using the shape palette.

To do so, in shape palette, we select the "HeadsAndDot" family figured by a black head and,
while the target glyph is still selected, we use a double-click on the WHOLE_NOTE icon.

**Remember**: the OMR engine uses a specific technique (*template matching*) to recognize heads.   
The glyph classifier works correctly for isolated fixed-size symbols like a treble key but would
work very poorly on heads because their underlying glyph is often difficult to retrieve
(think of glyph merge between head and staff lines and/or between head and stem).   
So, don't even think of Glyph Classifier top 5 output when dealing with heads!
{: .nota-bg }

### Sheet #1, Measure #14

![](sheet1_m14.png)

Yet another example of 2 whole notes (chord #2973 & #2971) with other notes in the same voice.
Fix is to force separate voices.

In this rather simple case, we could improve the OMR engine algorithm by checking current voice
end time when deciding to append or not the second whole note to voice #2. Room for improvement!

This concludes our manual work on Sheet #1, with no rhythm error left.

## Sheet #2

We update rhythm on whole sheet #2, via `<popup> | Page #1 | Reprocess rhythm`, to benefit from
the time signature fix (4/2) made on sheet #1.

Only 3 measures are left in pink, but let's review all measures.

NOTA: While we work an a sheet, measure numbers are local to the sheet (to the page to be precise).
Hence, even though sheet #2 image starts with a number "17", local measure numbers start at 1.   
Don't worry, measure numbers will be consolidated when scores are refined at end of book
transcription.

### Sheet #2, Measure #2

![](sheet2_m2.png)

Again a missing whole note at the upper left.
Inserting it manually makes the whole measure OK even for voices of lower staff.

![](sheet2_m2_ok.png)

### Sheet #2, Measure #6

![](sheet2_m6.png)

A missing whole note at beginning of lower staff.
We create it manually.

However, the measure remains in pink:

![](sheet2_m6_long.png)

```
MeasureStack#6
    |0       |1/2     |3/4     |1       |5/4     |3/2     |7/4     |2       |5/2
--- P1
V 1 |Ch#2756 |Ch#2758 |Ch#2760 =========|Ch#2762 |Ch#2763 =========|........|2
V 2 |Ch#2757 |Ch#2759 =========|Ch#2761 ==================|Ch#2764 |........|2
V 5 |Ch#3403 ==================|Ch#2792 =========|........|........|........|3/2
V 6 |Ch#2790 |Ch#2791 =========|Ch#2796 ===========================|Ch#3334 |5/2
```

The half rest at end of lower staff (chord #3334) should not be linked to whole chord #2796
but to half chord #2792.   
So we force either "separate voices" between #2796 and #3334 or "next in voice" between #2792
and #3334 to get correct rhythm:
```
MeasureStack#6
    |0       |1/2     |3/4     |1       |5/4     |3/2     |7/4     |2
--- P1
V 1 |Ch#2756 |Ch#2758 |Ch#2760 =========|Ch#2762 |Ch#2763 =========|2
V 2 |Ch#2757 |Ch#2759 =========|Ch#2761 ==================|Ch#2764 |2
V 5 |Ch#3403 ==================|Ch#2792 =========|Ch#3334 =========|2
V 6 |Ch#2790 |Ch#2791 =========|Ch#2796 ===========================|2
```

### Sheet #2, Measure #9

![](sheet2_m9.png)

A bunch of unwanted items on the upper left corner. To be manually deleted.

### Sheet #2, Measure #17

![](sheet2_m17.png)

This last measure in sheet exhibits a missing fermata
(mistaken for a slur and a staccato dot).

We delete both slur and staccato.

We then select the two underlying glyphs and, using the shape palette, we assign shape FERMATA_BELOW
to the compound glyph.

![](sheet2_m17_ok.png)

This completes our work on sheet #2.

## Sheet #3

This is a new movement, governed by a 3/4 time signature.

No pink measure in this page, but let's have a closer look...

A missing quarter note.

### Sheet #3, Measure #11

![](sheet3_m11.png)

On upper staff, we do have a slur but it is linked to the wrong note head on its right side,
certain because this head is closer to slur end target.

So we drag from this slur to the head of chord #4396, this move the slur-head relation off of
chord #4395 head to chord #4396 head.

For the slur on lower staff, which is linked to the wrong note head, we could also drag from the
slur to the correct note head (chord #4417):

![](sheet3_m11_ok.png)

### Sheet #3, Measure #14

![](sheet3_m14.png)

Here we have a false tiny slur on an un-detected inverted mordent.

So we first delete the false slur.

Then we select the underlying glyph for the mordent.

![](sheet3_m14_classifier.png)

This triggers the glyph classifier which proposes the MORDENT_INVERT shape with a 0.9824 grade.
We thus simply click on this top 1 button.

We then complete this sheet #3 here.

## Sheet #4

As for sheet #1, we delete the common-cut time signatures and replace them with custom 4/2 sigs.

3 measures are left in pink.

### Sheet #4, Measure #1 & #2

![](sheet4_m1_2.png)

The first two measures are OK, but we would like to align their voices in the lower staff.

So, we select last chord in measure #1 and first chord in measure #2, and use
`<popup> | Chords | Next In Voice` to get:

![](sheet4_m1_2_ok.png)

And we do the same voice alignment action between measure #2 and measure #3.

All voices are correctly aligned for this system, but we may not like the fact that the lower staff
exhibits voice #6 (displayed in orange) **above** voice #5 (displayed in cyan).
This is so because Audiveris engine assigns the voices for measure-long rests first,
and the "standard" voices only after.

To swap these voices, we can use direct voice assignment as follows:
We select (a head in) first voice chord in staff, for example chord #4290.

![](sheet4_m1_voice_assign.png)


Then in the `Chords` popup menu, we select the "Preferred voice" directly for this chord.   
We can see here that there was no voice assigned ("None").
We select the desired voice number in the menu, that is voice #5 (cyan color).

The engine then swaps voices for this measure (#1), and also in following measures #2 and #3
to fulfill the "Next In Voice" relations we had just set.

![](sheet4_m1_2_3.png)

NOTA: This direct voice number assignment is still an experimental feature:
- It is a *hard coding*, hence less flexible than the use of relations like "Next in Voice"
  or "Separate Voices",
- It is effective *only* for the starting chord of a voice in its measure.
  Setting the voice number for another chord in voice will have no effect,
  and in particular cannot be used as a means to link or unlink voices.
  {: .nota-bg }

### Sheet #4, Measure #7 and #8

| Half-measure #7a (seen as #7) | Half-measure #7b (seen as #8) |
| :---: | :---: |
| ![](sheet4_m7.png) | ![](sheet4_m8.png) |

Here we have the strange layout of a measure that ends system #2 and starts system #3.
Notice that system #2 ends with no barline, as opposed to the other systems.
Each of these "half-measures" lasts has a duration of 1.

Audiveris can see these only as separate measures, numbered #7 and #8 respectively.   
NOTA: Unfortunately, this shifts by 1 all the following measure numbers in this movement.

### Sheet #4, Measure #9

![](sheet4_m9.png)

On lower staff, we align voices between measure #8 and this measure #9.   
We also delete a false slur on middle of this staff.

### Sheet #4, Measure #11

![](sheet4_m11.png)

A measure in pink because of a too long voice.   
This is due to the final half rest, which should belong to the voice of the preceding half chord,
rather than the preceding whole chord.

### Sheet #4, Measure #12

![](sheet4_m12.png)

We can see the pink background going very low under the measure staves.   
This reveals the presence of an undesired 16th rest, to be deleted.

### Sheet #4, Measure #13

![](sheet4_m13.png)

We align voices with preceding measure,
delete two false small slurs and replace them by an inverted mordent.

### Sheet #4, Measure #14

![](sheet4_m14.png)

This measure is not in pink because no voice lasts longer than measure duration (4/2),
however the rhythm is strange:
- On upper staff, we have 3 voices, while 2 would be enough.
- On lower staff, we have 4 voices, while 2 would be enough.

```
MeasureStack#14
    |0       |1/4     |1/2     |1       |5/4     |3/2     |2
--- P1
V 1 |Ch#3759 |Ch#3760 |Ch#3761 |Ch#3764 =========|Ch#3767 |2
V 2 |Ch#4340 ==================|Ch#3762 |Ch#3763 |Ch#3768 |7/4
V 3 |........|........|........|Ch#3765 |Ch#3766 |Ch#3769 |7/4
V 5 |Ch#3796 |Ch#3797 |Ch#3798 |........|........|........|1
V 6 |Ch#3807 ==================|........|........|........|1
V 7 |........|........|........|Ch#4341 ==================|2
V 8 |........|........|........|Ch#3805 ==================|2
```
The measure strip shows that chord #4340 is considered as a whole rest.   
Also clicking on this rest shows WHOLE_REST in Inter board.

However, a look at the binary image gives a different result:   
![](sheet4_m14_binary.png)

Once again, this rest was mistaken for a whole rest because of its vertical location with respect
to the related staff.
So, we delete it and manually reassign the underlying glyph as a HALF_REST.

This immediately fixes rhythm on both upper and lower staves.

### Sheet #4, Measure #15

![](sheet4_m15.png)

The sign at beginning of lower staff is a old sign for a multi-measure rest,
a kind of entity not recognized by Audiveris.   
Since measure duration is 2, we can manually insert a breve rest at measure center,
and voice-link it (using "next in voice") with the corresponding whole note in previous measure.

### Sheet #4, Measure #16

![](sheet4_m16.png)

Here again, we can manually insert breve rests in upper and lower staves.   
And assign correct voice number (5) for starting chord #3836.

![](sheet4_m16_ok.png)

### Sheet #4, Measure #17

![](sheet4_m17.png)

There is an un-recognized whole note on upper left corner (to be inserted),
a false slur (to be deleted)
and an un-recognized inverted mordent (to be assigned from its underlying glyph).

### Sheet #4, Measure #18

![](sheet4_m18.png)

Measure rhythm is correct, but in the upper staff we can align voices with previous measure.

### Sheet #4, Measure #19

![](sheet4_m19.png)

Finally a false staccato dot (to be deleted) and yet another inverted mordent to be assigned
from its underlying glyph.

This completes our work on sheet #4.

## Sheet #5

This sheet #5 continues the movement begun with sheet #4.

We update the page rhythm.
There are two measures left in pink in this sheet plus a few others to fix.

### Sheet #5, Measure #2

![](sheet5_m2.png)

We have two slurs that should be recognized as tie slurs but are not
(and are thus displayed in black, instead of a voice color).   
The reason is that both are connected to a wrong head on their right side.

To fix this, we drag from each slur to the correct target head and release the mouse.   
Each slur is now recognized as a tie slur and its color turns to the color of its underlying voice.

![](sheet5_m2_ok.png)

### Sheet #5, Measure #4

![](sheet5_m4.png)

A missing whole head (in this half measure).

### Sheet #5, Measure #6

![](sheet5_m6.png)

In lower staff, voices to be aligned with previous measure, for example by setting "Next in Voice"
relation between chords #3921 and #3907.

### Sheet #5, Measure #8

![](sheet5_m8.png)

Lower staff: A false crescendo to delete and a breve rest to insert.

Upper staff: a slur that should be a tie.
The reason is this slur is not connected on its left side to the correct head,
it must be moved from #3902 to #3901

### Sheet #5, Measure #9

Again a breve rest to be manually inserted.

### Sheet #5, Measure #10

![](sheet5_m10.png)

Pink measure is due to incorrect voice connection in upper staff.

Aligning voices correctly in upper staff has the side effect to align them correctly
in lower staff as well.

### Sheet #5, Measure #12

In upper staff, voices to be swapped by connecting to corresponding voice in previous measure.

### Sheet #5, Measure #17

![](sheet5_m17.png)

There is a false fermata at (beginning!) of upper staff. We delete it.

```
MeasureStack#17
    |0       |1/4     |1/2     |3/2     |2
--- P1
V 1 |Ch#4020 |Ch#4021 |Ch#4022 |........|1
V 2 |Ch#4036 ==================|........|1
V 5 |Ch#4038 =========|Ch#4707 |Ch#4039 |2
```

There is also a problem with measure length.

In fact, it looks like a "half" measure which continues another "half" measure at the end of the
previous system with no ending  barline.

For Audiveris data model, these are two separate measures.
And since none of them lasts longer than 4/2, they are not flagged as abnormal pink measures.

However, the duration of voice #5 in the second half measure is 2, while we would expect 1.   
This is because of the whole rest chord #4707, which is inserted between half chords #4038 & #4039
in a single voice.

We can try to force voice separation between the whole rest chord #4707 and the surrounding
half chords.
This results in message like:
```
Measure{#17P1} No timeOffset for RestChordInter{#4707(0.796/0.796) stf:10 slot#3 dur:1}
```
In other words, the engine fails to determine the time offset of whole rest chord #4707,
alone in its voice.
This is why the measure background turns pink.

In fact, we can consider that the original score here is wrong.   
The whole rest, as it is located here in the middle of the measure, should be considered as
a measure-long rest, but this would not be consistent with the other measures in this page.

So a workaround is to actually *shift* this badly located rest to the left, at the beginning of
the measure.   
To do so, via a double-click, we put the symbol into edit mode, and drag its handle to the left.

| Shift edition | End result |
| :---: | :---: |
|![](sheet5_m17_1.png)   | ![](sheet5_m17_2.png)  |

### Sheet #5, Measure #19

Again a breve rest to be manually inserted.

### Sheet #5, Measure #20

![](sheet5_m20.png)

We have a breve rest to insert on upper staff.
Plus a small slur to delete and an inverted mordent to insert.

Apart from that, the role of a slur that connects to a rest is not obvious.
Perhaps we can just ignore these slurs, and the next measure as well.

This completes our work on sheet #5

and concludes the whole editing session.

## Completion

In this editing session, we have focused on pure musical content, that is essentially the rhythm
aspect (chords organized in voices and time slots).

### Text

There are few textual elements in this book, most of them are located on first sheet.   
We can see a mix of several languages the OCR had to deal with.

Nota, for visual check:
- original text is easier to read on binary view in physical mode ![](../../assets/images/ModePhysical.png)
- while OCR output is easier to read on logical mode ![](../../assets/images/ModeLogical.png)

Here below are all the textual elements found in this book
(we ignore text that represents a measure number)

| Original sentence | OCR output | Comments |
| :--- | :--- | :--- | :---: |
| O lux beata Trinitas |  ® fax Beam Ërinitas | Latin, gothic? font, OCR totally wrong |
| _Der du bist drei in Einigkeit._ | Der du _bist drei_ in _Einigkeit._ |German, OCR mix of italic & standard  |
| Restitution par P. Gouin. | Restitution par P. Gouin.| French  |
| Johann Friedrich Alberti  | Johann Friedrich Alberti | |
| (1642-1710) | (1642-1710) | |
| _Versus primus._ | _Versusprimus._ | Latin, OCR merged as one word |
| _Versus secundus._ | _Versus_ secunafus. | Latin, OCR mistake on "secundus" |
| _Versus tertius._ | _Versus_ tertius. | Latin |
| © Les Éditions Outremontaises - 2018 | © Les Editions Outremontaises - 2018 | French, OCR took 'É' for 'E'  |

If we except the book title "O lux beata Trinitas" which OCR totally failed to recognize,
probably because of its exotic font, raw Tesseract OCR results are pretty good.

We can manually correct the few mistakes, via the Inter board.   
This must be done word by word, by selecting the word and modifying its content
(typing new characters and validating by pressing `Enter` key).

Note that current Audiveris UI let us modify only the textual **content** of any word,
not its **font** characteristics (such as font name, italic, bold, etc).

![](trinitas_word.png)

For manual text edition, text **content** is modified at _word_ level
whereas text **role** is modified at _sentence_ level.

In this book, nearly all sentences got their role correctly assigned by the OMR engine heuristics.
Exception is "© Les Éditions Outremontaises - 2018" whose role appears to be "UnknownRole".

To fix this, we select one word of this sentence and, in the Inter board,
click on `To Ensemble` button (as you know, a sentence is modeled as an ensemble of words).

We can then choose sentence proper role ("Rights"):

![](sentence_role.png)

### MusicXML export

We can export to MusicXML via `Book | Export Book` menu item.
This gives:
```
[Trinitas] Exporting sheet(s): [#1#2]
[Trinitas] Score Trinitas.mvt1 exported to D:\soft\cases\Issue-481\Trinitas.mvt1.mxl
[Trinitas] Exporting sheet(s): [#3]
[Trinitas] Score Trinitas.mvt2 exported to D:\soft\cases\Issue-481\Trinitas.mvt2.mxl
[Trinitas] Exporting sheet(s): [#4#5]
[Trinitas] Score Trinitas.mvt3 exported to D:\soft\cases\Issue-481\Trinitas.mvt3.mxl
```
This book got exported as 3 separate .mxl files, one per detected movement.

### Plugin

If we have configured a few [plugins](../../advanced/plugins.md)
to external programs such as MuseScore or Finale,
clicking on the desired plugin would:
1. Export an updated version of the 3 movements files, if so needed,
2. Launch for example MuseScore on the 3 movement files.

![](musescore.png)

We can then for example listen to MuseScore playback on these movements.   
This is an additional convenient way to check transcription results.
