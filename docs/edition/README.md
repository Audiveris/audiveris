# Manual Edition

_As an introduction, you can watch Audiveris manual edition in action thanks to Baruch's
[tutorial video](https://www.youtube.com/watch?v=718iy10sKV4&feature=youtu.be)._

In the OMR transcription process, some elements may not be recognized correctly by the engine.

Starting with 5.1 release, Audiveris gives the user a few means to manually fix
such wrongly detected elements.
This is the purpose of this chapter.

It is important to realize that Audiveris editing features cannot compare
with music editors like MuseScore, Finale or Sibelius, to name a few.

Those are high-level editors meant for composers, arrangers or publishers
who need a tool to create, arrange, transpose music, etc.

Instead, Audiveris is focused on OMR, which attempts to transcribe an existing score image
to its symbolic representation:
* Fidelity to the original score image is of paramount importance.
* To this end, Audiveris tries to remain as close as possible to the original layout.
* This even includes to stick to any original image distortions (such as skew or warping).

For Audiveris, music is something very graphical, a 2-D view of music symbols
(staff lines, bar lines, note heads, stems, flags, beams, alterations, ...)
with some "geometrical" relations between them (a note head and its stem, an alteration sign
and the altered head, a lyric item and its related chord, a slur and its embraced heads, ...).

Do not expect Audiveris GUI to let you say transpose a few notes.
Instead, user actions are meant to be _corrections_ so that the OMR graphic representation
(i.e. both inters and relations) gets closer to what it should be.

Following any user manual action, the sheet view is updated to reflect impact on OMR elements and
to indicate "abnormal" situations detected on-the-fly by the OMR engine.
For example, a black note head with no compatible stem nearby will appear in red.
Similarly, a whole measure may have its background colored in pink to indicate a rhythm problem.

In most cases, the relations between inters (symbols) is automatically detected when new inters
are manually inserted.
This assumes that the respective locations of these inters are compatible for such relation.
For example, a sharp sign cannot be too far away from its target note head.

This also assumes that the inters are inserted in proper order: insert head first, then insert
the sharp sign.
If you insert the sharp sign first, and head second, the OMR engine will not automatically try
to link the head and the sharp, the sharp will remain "abnormal" and displayed in red.
This is so, because the sharp _needs_ a head, but the head _does not need_ a sharp.

Anyway, you always have the ability to manually set a needed relation.
In this example, simply point to an inter (head or sharp), drag to the other inter
(sharp or head) and release the mouse button.
The relation will be set (and visible as a green segment for the selected inter).

With some practice, you should easily get used to this way of playing with the inters/relations
graph.
