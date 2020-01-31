---
---
## Issues and Workarounds

In some cases, the normal ways to correct the transcription don't give a satisfying result.
In the following sections, you may find some hint on how you could solve encountered problems.

### Recognition of Text Elements

It is very difficult to automatically derive the meaning from the textual items in a musical score.

On basic line, Audiveris only separates between **lyrics** and **normal text**.

For **lyrics**, the single syllables are connected to the chords above or below.
But it is not always evident if the text concerns the staff above or below nor it's always clear
which voice is concerned (if there is more than one voice per staff).

In future version, it's planned to give the user the possibility to select the staff and voice.
Up to now, this must be done during further editing in some external notation software.

For **normal text**, Audiveris tries to detect text role, such as directions or typical header
elements like: title, composer and lyricist.
If it fails, the role can be easily corrected manually.

### Chord Symbols

There is no support for chord names/symbols (like F#m) in the current version.
Chords are mostly detected as normal text and will be treated as lyrics by default.

Although there is an indirect possibility to manually mark textual items as chord symbols,
exporting chords to MusicXML isn't currently implemented.

### Incorrect Key Signatures

If the transcription failed to recognize a key signature correctly, there is currently no way to fix this.
The same applies to key signature changes in the middle of a staff or system.

Audiveris doesn't currently offer the possibility to correct or to add key signature changes.
Such a correction must be done in a notation editor later on.

### Unknown Time Signatures

Audiveris does support a set of common time signatures, enough to cover a wide range
of musical pieces of the 18th and 19th centuries.
However, older music and contemporary scores may contain less frequent time signatures Audiveris
doesn't currently support.

### Common Note Head for two Voices (manually added)

It's not possible to manually define a common note head for two voices.

If a note head hasn't been automatically recognized as part of two voices,
it's not possible to connect it to more than one stem.
Adding a connection to one stem will remove the existing one.

### Rhythm Check Failures

Sometimes the rhythm check will fail for a particular measure causing that measure to be marked
as erroneous.
This happens if the program cannot verify the correctness of the note durations in that measure.
The common reason for that are errors in the recognition of bars, flags, tuplets or augmentation dots.

In the case of staves containing multiple voices (keyboard music, for example),
rhythm check may also fail due to incorrect voice assignment (see next section).

It may also fail if some note heads are not aligned properly
(Audiveris expects that notes on the same tick are rather aligned vertically to the same position).

### Voice Assignment

The automatic assignment of notes to voices is very tricky.
Audiveris tries to find a logical assignment with a minimum of voices,
but sometimes it does not give the correct result.
This often appears as a consequence of rhythm problems as described above.

Audiveris doesn't currently offer a GUI for fixing that.
You will have to re-assign the notes in your notation program.

### Support of Tuplets other than 3-let and 6-let

Currently the program only supports these two types of tuplets.
