## Editor Principles

For a good understanding of the rationale and also the current limitations of this editor,
we recommend to read the [perpective](https://github.com/Audiveris/audiveris/wiki/Perspective)
section in Audiveris WiKi.

This section on editor principles is organized as follows:

1.	Which [entities](#Entities) are handled,
2.	Which [actions](#Actions) are possible on these entities,
3.	Which user [gestures](#gestures) can trigger these actions.

### Entities

We can think of Audiveris OMR as a process that takes an image as input and produces
as output a structured collection of music symbols.

* The OMR _engine_ is one actor, which moves forward from step to step,
impacting a bunch of entities.
* The OMR _user_ is another actor, which can interactively modify some entities,
and decide how to move the engine.

The resulting symbols are divided into two categories:

1.  Symbols that are built programmatically, especially at the beginning of the sheet processing
pipeline.
They define the global structure of the sheet musical content.
They are not represented by Inter instances, but rather by a hierarchical structure of objects.
As of this writing, the user cannot modify this structure composed of "static" objects:

    - *staff lines*, gathered in ...
    - *staves*, gathered in ...
    - *parts*, gathered in ...
    - *systems*, gathered in ...
    - *pages*, gathered in ...
    - *scores*

2.  Symbols that are considered as candidate interpretations. These are Inter instances, handled
within each system by a graph (`SIG`) of Inter vertices linked by Relation edges.
Within each system SIG, it's the struggle for life, since eventually only the strongest inters
survive.   
There is no structure within the Inter instances of a SIG, just Relation instance possibly set
between a pair of Inter instances.
All these Inter and Relation instances should be interactively modifiable.
As of this writing, only the most relevant are (the Inters with fixed-shape and size, with their
Relations), but it's only a matter of development priorities.
Inter objects are everything except the static objects mentioned above:   
    - Barline, ledger, clef, key signature, time signature, chord, head, alteration,
    augmentation dot, stem, beam, rest, flag, slur, sentence, word, etc... are Inters.
    - An _InterEnsemble_ is an Inter composed of other Inter instances.
    This composition is formalized by a Containment relation between the ensemble inter and each of
    the contained Inter instances.  
    These ensembles are:
      - A Chord which contains one or several note heads or one rest.
      - A Sentence which contains words. A LyricLine is a special kind of Sentence.
      - A Key which contains key alter signs.
      - A TimePair which contains a numerator number and a denominator number.

Measures are hybrid, since they are not Inters but depend on barlines which are Inters.

Beside Inter and Relation entities, glyphs, which are just sets of foreground pixels, can be used
to create new Inters.
Hence, Glyph instances are also entities that the user can select and transcribe to Inter entities.

The user interface has been designed to work on the SIG data model, that is essentially to play with
the addition or removal of Inter instances and the linking or unlinking of Relation instances
between them.

### Actions

#### When

The OMR engine works in batch, or more precisely in a sequence of some 20 mini-batches since each
of the 20 sheet steps is a mini-batch.
You, as an interacting user, can get in only at the end of a mini-batch.


Most of the user corrections can be done at the end of a sheet transcription
(at the final `PAGE` step of the engine pipeline).
Some corrections are more effective at earlier moments, experience will tell you.  
Here below are the most interesting steps/stops:

*   **REDUCTION** step is where all candidate note heads are combined with candidate stems and
beams and reduced to come up with reliable notes (this does not include any flag or rest,
which are addressed in a later step).
It is a key moment in the engine pipeline, because these "reliable" notes will never be called into
question by the following steps.   
So much that their underlying pixels will not be presented to the symbols classifier during the
`SYMBOLS` step.
This includes a "dilation" margin ring around each note head, something that the user cannot select
as a glyph in any subsequent selection.
So, if some of these notes are false positives, then it's much more efficient to correct them
immediately (at end of `HEADS` step).

*   **TEXTS** step is another key moment.
It aims at retrieving all the textual items in the image and assigning them a role (such as title,
part name, lyrics).
The engine output for the `TEXT` step depends heavily on an external program (Tesseract OCR) on
which Audiveris has little control.
It is hard for the OMR engine to decide if an item is really a piece of text, or if it should also
be considered as a possible music symbol.

*   **CURVES** step, which deals with slurs, endings and wedges, is the last stop before the
crucial `SYMBOLS` step.

*   **PAGE** step is where the engine ends on a sheet and where you can observe and correct the
final result.

#### Action sequence

Whenever you modify an entity, this action is recorded as such, perhaps with some
joint actions, into an atomic "action sequence" which is performed on the targeted entities.
This sequence may have consequences on past steps, in which case its impact is immediately performed.

An action sequence can be pretty large. For example:

*   You can select a dozen of inters and ask for the removal of the whole set.
*   You can remove an inter ensemble (sentence, chord, ...), this triggers the removal of all the
members of the ensemble: words of the sentence, heads and stem of the head-chord, etc.
*   You can remove the last member of an ensemble, this also removes the now-empty ensemble.

The key point here is that any "action sequence" is **atomic**.
You can **undo** and **redo** it at will, with no limits.
Only moving the engine forward (or pseudo backward) from one step to another clears the user
action history.

#### Inter add / remove

The set of black pixels of one or several selected glyphs can be transcribed as one inter,
by specifying the target inter shape.
The precise location and bounds of the new inter is defined by the underlying set of pixels.

If no precise pixels can be selected, you can often directly drag a "ghost" Inter from the shape
palette, and drop the "ghost" precisely at the desired location.   
Pay attention to the precision of location, because the relation(s) with the nearby inter(s) if any,
will be set automatically, but only if the geometrical relationships can apply.
If not, you will have to set the relation manually afterwards.  
As of this writing, only fixed-shape inters can be added via drag n' drop, since there is yet
no support for inter resizing.

Adding an inter may trigger joint actions, when the inter starts a brand new ensemble:

*   Rest always create a Rest-Chord
*   Head creates a Head-Chord, if added to a stem not yet part of a head-chord
*   Key alter creates a Key signature if isolated (not yet implemented)

Removing a selected inter (or a set of selected inters) automatically removes the relations these
inters were involved in.

Removing the last member of an ensemble also removes that ensemble.

#### Relation link / unlink

Some Inter classes need a relation with another inter instance.

For example, a slur needs a note head on both horizontal sides, if we except the case of slurs
connected across systems breaks.
If there is no such note head nearby, the slur should not survive (this is the case when
processed by the OMR engine), but since here the slur was manually added, it is simply flagged as
"abnormal" and shown in red to call user attention on it.
This can happen when the geometry rules are not matched, perhaps because the gap is a bit too wide
between the slur end and the target note head.   
In that case, the user has to manually set the relation.


Nota: The mandatory relation is automatically searched for only at the moment the dependent inter is
created.
This is so, simply because, using the same slur example, the slur needs a head but the head does
not need a slur.
A good practice, when several Inters have to be created manually, is to create the independent
Inters first and the dependent Inters second.

Non-mandatory relations are never searched for automatically.
An example of non-mandatory relation is the case of a direction sentence which don't always have
a chord precisely above or below.
For such non-mandatory relations, the user has to decide to set them manually.

A manual unlink simply removes the relation, which may or not have an impact on the
previously-linked Inter instances.

#### Undo / Redo

This works as one would expect, with no limits:

*   Undo cancels the last action sequence (whether it was a do or a redo) with all its consequences.
*   Redo re-performs the last un-done action sequence.

### Gestures

#### Selection

Glyphs and Inters can be selected in the same manners:

*   **Pointing to the entity**: A left-button mouse click grabs the entities that contain the
    location point.   
    If several entities are grabbed, only one is chosen to start the new selection.
    For glyphs, it's the smallest one.
    For inters, it's the member before the ensemble.

*   **Using a lasso**: Pressing then dragging with the left-button, defines a rectangular area.
    All entities fully contained in this area start a new selection.   
    Nota: make sure that the rectangle *fully* contains the desired entities, the ones that are only
    intersected won't be picked up.

*   **Adding an entity**: Pointing to an entity while pressing the CTRL key (Option key for MacOS)
    adds this entity to the current selection, whether the selection was done via point or lasso.

*   **Naming an entity**: Entering the integer ID of a glyph in the Glyph-Board spinner or the Id
    of an Inter in the Inter-Board spinner selects this entity.

*   **Pointing outside of any entity**: This clears both glyphs and inters selections.

*   **Choosing entity in selection list**: The contextual popup menu, when entities have
    been selected, offers in `Glyphs` and in `Inters` sub-menus the list of selected entities.
    Simply moving the mouse onto the entity of choice will select this entity.

*   **Choosing relation in relations list**: Via the contextual popup menu, moving to a
    selected inter displays a sub-menu with all the relations the inter is part of.
    Selecting a relation will remove this relation (after user confirmation).

#### Inter add

*   When one or several glyphs are selected, a resulting compound glyph is automatically built and
    made available for inter creation with proper shape using the location and bounds of the
    underlying glyph.   
    Note that if the related staff cannot easily be determined by the context, you will be prompted
    to select the staff above or the staff below.  

    The target shape can be selected as follows:

    *   By selecting among the top results shown in the classifiers boards,

    *   By choosing in the popup contextual menu, the `Glyphs` sub-menu from which you can navigate
        to the proper shape,

    *   By a double-click on proper button of one of the shape palette categories.    
        Note that the "Physicals" category contains a "lyrics" button and a "txt" button.
        Both will launch the OCR on the selected glyph(s) to generate one or several sentences of
        words, the "lyrics" button guiding specifically towards lyric lines and items.

*   You can work the other way round: You can first pick up a shape in one of the shape palette
    categories, then drag the "ghost" inter to the desired location and drop it there.   
    The best way to make sure you select the right staff is to hover the staff before dropping the
    ghost inter. If you don't do that, the drop will be discarded.   

    Mind the fact that not all shapes are "draggable". Typically, only the fixed-shape ghosts are
    draggable, the others will display a red cross if you attempt to drag them.

#### Inter remove

*   The Inter-board has a `Deassign` button which removes the selected inter displayed in the board.

*   When one or several inters have been selected, pressing the `DELETE` key will remove all these
    inters.
    If more than one inter is selected, you will be prompted for confirmation beforehand.

#### Relation link

*   To set a relation between inter A and inter B, simply point with left-button to inter A and
    drag to inter B, where you can release the button.   

    Audiveris will search among all the inters grabbed at first and at last location, find the
    missing relation if any between those two collections of inters and set the proper relation.

#### Relation unlink

*   From the contextual popup, select `Inters` sub-menu, hover on one inter involved,
    and click on the desired relation to delete it.

#### Undo

*   Click on the Undo button (left-pointing arrow, located on tool bar)

*   Press `CTRL`-`Z` (`Command`-`Z` for MacOS)

#### Redo

*   Click on the Redo button (right-pointing arrow, located on tool bar)

*   Press `SHIFT`-`CTRL`-`Z` (`SHIFT`-`Command`-`Z` for MacOS).
