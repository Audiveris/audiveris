//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  I n t e r C o n t r o l l e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig.ui;

import ij.process.ByteProcessor;

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ui.NestView;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.symbol.InterFactory;
import org.audiveris.omr.sheet.ui.BookActions;
import org.audiveris.omr.sheet.ui.SheetEditor;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.BarConnectorInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.ChordNameInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeCustomInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.BarConnectionRelation;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.RelationPair;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.sig.relation.Support;
import org.audiveris.omr.sig.ui.UITask.OpKind;
import static org.audiveris.omr.sig.ui.UITask.OpKind.*;
import org.audiveris.omr.sig.ui.UITaskList.Option;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.text.BlockScanner;
import org.audiveris.omr.text.OCR;
import org.audiveris.omr.text.OcrUtil;
import org.audiveris.omr.text.TextBuilder;
import org.audiveris.omr.text.TextLine;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.text.TextWord;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.util.UIThread;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.VoidTask;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Class {@code InterController} is the UI in charge of dealing with Inter and
 * Relation instances (addition, removal, modifications) to correct OMR output,
 * with the ability to undo and redo at will.
 * <p>
 * It works at sheet level.
 * <p>
 * When adding or dropping an inter, the instance is allocated in proper system (and staff if
 * relevant) together with its relations with existing partners nearby.
 * It is not always obvious to select the proper staff, various techniques are used, and if
 * all have failed, the user is prompted for staff indication.
 * <p>
 * Finally, a proper {@link UITaskList} is allocated, inserted in controller's history, and run.
 * Undo and Redo actions operate on this history.
 * <p>
 * User actions are processed asynchronously in background.
 *
 * @author Hervé Bitteur
 */
public class InterController
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(InterController.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying sheet. */
    private final Sheet sheet;

    /** History of tasks. */
    private final TaskHistory history = new TaskHistory();

    /** User pane. Lazily assigned */
    private SheetEditor sheetEditor;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code IntersController} object.
     *
     * @param sheet the underlying sheet
     */
    public InterController (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addInter //
    //----------//
    /**
     * Add one inter.
     *
     * @param inter the inter to add (staff and bounds are already set)
     */
    @UIThread
    public void addInter (final Inter inter)
    {
        new CtrlTask(DO, "addInter")
        {
            @Override
            protected void build ()
            {
                // If glyph is used by another inter, delete this other inter
                removeCompetitors(inter, inter.getGlyph(), seq);

                // Addition task and other related tasks (additions, links) if any
                final WrappedBoolean cancel = new WrappedBoolean(false);
                seq.addAll(inter.preAdd(cancel));

                if (cancel.isSet()) {
                    seq.setCancelled(true);
                }
            }

            @Override
            protected void publish ()
            {
                if (!seq.isCancelled()) {
                    sheet.getSheetEditor().getShapeBoard().addToHistory(inter.getShape());
                }

                sheet.getInterIndex().publish(inter);
                sheet.getGlyphIndex().publish(null);
            }
        }.execute();
    }

    //-------------//
    // assignGlyph //
    //-------------//
    /**
     * Add a shape interpretation based on a provided glyph.
     *
     * @param aGlyph the glyph to interpret
     * @param shape  the shape to be assigned
     */
    @UIThread
    public void assignGlyph (Glyph aGlyph,
                             final Shape shape)
    {
        if ((shape == Shape.TEXT) || (shape == Shape.LYRICS)) {
            addText(aGlyph, shape);

            return;
        }

        final Inter ghost = InterFactory.createManual(shape, sheet);

        if (ghost == null) {
            return;
        }

        final Glyph glyph = sheet.getGlyphIndex().registerOriginal(aGlyph);
        ghost.setBounds(glyph.getBounds());
        ghost.setGlyph(glyph);

        // While we are still interacting with the user, make sure we have the target staff
        final Collection<Link> links = new ArrayList<>();
        final Staff staff = determineStaff(glyph, ghost, links);

        if (staff == null) {
            logger.info("No staff, abandonned.");

            return;
        }

        // For barlines, make sure length is only one-staff high
        if (ghost instanceof BarlineInter || ghost instanceof StaffBarlineInter) {
            Rectangle box = ghost.getBounds();
            int y1 = staff.getFirstLine().yAt(box.x);
            int y2 = staff.getLastLine().yAt(box.x);
            ghost.setBounds(new Rectangle(box.x, y1, box.width, y2 - y1 + 1));
            ghost.setGlyph(null);
        }

        ghost.setStaff(staff);
        addInter(ghost);
    }

    //---------//
    // canRedo //
    //---------//
    /**
     * Is a redo possible?
     *
     * @return true if so
     */
    @UIThread
    public boolean canRedo ()
    {
        return history.canRedo();
    }

    //---------//
    // canUndo //
    //---------//
    /**
     * Is an undo possible?
     *
     * @return true if so
     */
    @UIThread
    public boolean canUndo ()
    {
        return history.canUndo();
    }

    //----------------//
    // changeSentence //
    //----------------//
    /**
     * Change the role of a sentence.
     * <p>
     * When a sentence changes its role between "plain", chordName and lyrics, each of its word
     * may have to be converted to a WordInter, ChordNameInter of LyricItemInter.
     * <p>
     * Plus some conversion for the sentence as well.
     *
     * @param sentence the sentence to modify
     * @param newRole  the new role for the sentence, not null
     */
    @UIThread
    public void changeSentence (final SentenceInter sentence,
                                final TextRole newRole)
    {
        new CtrlTask(DO, "changeSentence")
        {
            @Override
            protected void build ()
            {
                final Staff staff = sentence.getStaff();
                final SystemInfo system = staff.getSystem();
                final SIGraph sig = system.getSig();

                switch (newRole) {
                case Lyrics: {
                    // Convert to LyricItem words, all within a single LyricLine sentence
                    final WrappedBoolean cancel = new WrappedBoolean(false);
                    final LyricLineInter line = new LyricLineInter(
                            sentence.getBounds(), sentence.getGrade(), sentence.getMeanFont());
                    line.setManual(true);
                    line.setStaff(staff);
                    seq.add(new AdditionTask(sig, line, line.getBounds(), line.searchLinks(system)));

                    for (Inter inter : sentence.getMembers()) {
                        if (!(inter instanceof LyricItemInter)) {
                            // Add new LyricItemInter for any plain WordInter
                            final WordInter orgWord = (WordInter) inter;
                            final LyricItemInter item = new LyricItemInter(orgWord);
                            item.setManual(true);
                            item.setStaff(staff);
                            seq.add(new AdditionTask(
                                    sig, item, item.getBounds(),
                                    Arrays.asList(new Link(line, new Containment(), false))));

                            for (Link link : item.searchLinks(system)) {
                                // Link from chord to syllable
                                seq.add(new LinkTask(sig, link.partner, item, link.relation));
                            }

                            if (cancel.isSet()) {
                                seq.setCancelled(true);
                                return;
                            }

                            // Remove the plain word
                            seq.add(new RemovalTask(orgWord));
                        }
                    }

                    if (!seq.getTasks().isEmpty()) {
                        // Remove the now useless original sentence
                        seq.add(new RemovalTask(sentence));
                    }

                    break;
                }

                case ChordName: {
                    // Convert to ChordName words, each within its own sentence
                    final WrappedBoolean cancel = new WrappedBoolean(false);

                    for (Inter inter : sentence.getMembers()) {
                        if (!(inter instanceof ChordNameInter)) {
                            // Add a new ChordNameInter for any original word
                            WordInter orgWord = (WordInter) inter;
                            ChordNameInter cn = new ChordNameInter(orgWord);
                            cn.setManual(true);
                            cn.setStaff(staff);
                            seq.addAll(cn.preAdd(cancel));

                            if (cancel.isSet()) {
                                seq.setCancelled(true);
                                return;
                            }

                            // Remove the original word
                            seq.add(new RemovalTask(orgWord));
                        }
                    }

                    if (!seq.getTasks().isEmpty()) {
                        // Remove the now useless original sentence
                        seq.add(new RemovalTask(sentence));
                    }

                    break;
                }

                default: {
                    final SentenceInter finalSentence;
                    if (sentence.getClass() != SentenceInter.class) {
                        // Create a basic SentenceInter
                        finalSentence = new SentenceInter(
                                sentence.getBounds(), 1.0, sentence.getMeanFont(), newRole);
                        finalSentence.setManual(true);
                        finalSentence.setStaff(staff);
                        seq.add(new AdditionTask(sig, finalSentence, finalSentence.getBounds(),
                                                 finalSentence.searchLinks(system)));
                    } else {
                        finalSentence = sentence;
                    }

                    for (Inter inter : sentence.getMembers()) {
                        if (inter.getClass() != WordInter.class) {
                            // LyricItem/ChordName -> WordInter
                            WordInter orgWord = (WordInter) inter;
                            WordInter word = new WordInter(orgWord, Shape.TEXT);
                            word.setManual(true);
                            word.setStaff(staff);
                            seq.add(new AdditionTask(
                                    sig, word, word.getBounds(),
                                    Arrays.asList(new Link(finalSentence, new Containment(), false))));

                            // Remove the original word
                            seq.add(new RemovalTask(orgWord));
                        }
                    }

                    if (finalSentence == sentence) {
                        seq.add(new SentenceRoleTask(sentence, newRole));
                    } else {
                        // Remove original sentence
                        seq.add(new RemovalTask(sentence));
                    }

                    break;
                }
                }
            }

            @Override
            protected void publish ()
            {
                sheet.getInterIndex().publish(!sentence.isRemoved() ? sentence : null);
            }
        }.execute();
    }

    //------------//
    // changeTime //
    //------------//
    /**
     * Change the value of a custom time signature.
     *
     * @param custom  the custom signature to modify
     * @param newTime the new time value
     */
    @UIThread
    public void changeTime (final TimeCustomInter custom,
                            final TimeRational newTime)
    {
        new CtrlTask(DO, "changeTime")
        {
            @Override
            protected void build ()
            {
                seq.add(new TimeValueTask(custom, newTime));
            }

            @Override
            protected void publish ()
            {
                sheet.getInterIndex().publish(custom);
            }
        }.execute();
    }

    //---------------//
    // changeVoiceId //
    //---------------//
    /**
     * Change the preferred voice ID of a chord.
     *
     * @param chord the chord to modify
     * @param newId the new voice ID
     */
    @UIThread
    public void changeVoiceId (final AbstractChordInter chord,
                               final Integer newId)
    {
        new CtrlTask(DO, "changeVoiceId")
        {
            @Override
            protected void build ()
            {
                seq.add(new ChordVoiceIdTask(chord, newId));
            }
        }.execute();
    }

    //------------//
    // changeWord //
    //------------//
    /**
     * Change the text content of a word.
     *
     * @param word     the word to modify
     * @param newValue the new word content
     */
    @UIThread
    public void changeWord (final WordInter word,
                            final String newValue)
    {
        new CtrlTask(DO, "changeWord")
        {
            @Override
            protected void build ()
            {
                seq.add(new WordValueTask(word, newValue));
            }

            @Override
            protected void publish ()
            {
                sheet.getInterIndex().publish(word);
            }
        }.execute();
    }

    //--------------//
    // clearHistory //
    //--------------//
    /**
     * Clear history of user actions.
     */
    @UIThread
    public void clearHistory ()
    {
        history.clear();

        if (sheetEditor != null) {
            refreshUI();
        }
    }

    //-----------//
    // editInter //
    //-----------//
    /**
     * Modify position or geometry of an inter.
     *
     * @param editor the editor used on inter
     */
    @UIThread
    public void editInter (final InterEditor editor)
    {

        new CtrlTask(DO, "editInter")
        {
            @Override
            protected void build ()
            {
                final Inter inter = editor.getInter();
                seq.addAll(inter.preEdit(editor));
            }
        }.execute();
    }

    //------//
    // link //
    //------//
    /**
     * Add a relation between inters.
     *
     * @param sig      the containing SIG
     * @param source   the source inter
     * @param target   the target inter
     * @param relation the relation to add
     */
    @UIThread
    public void link (final SIGraph sig,
                      final Inter source,
                      final Inter target,
                      final Relation relation)
    {
        new CtrlTask(DO, "link")
        {
            @Override
            protected void build ()
            {
                // Additional tasks?
                final RelationPair pair = new RelationPair(source, target);
                seq.addAll(relation.preLink(pair));

                // Remove conflicting relations if any
                final boolean sourceIsNew = pair.source != source;
                removeConflictingRelations(seq, sig, sourceIsNew, pair.source, pair.target, relation);

                // Finally, add relation
                seq.add(new LinkTask(sig, pair.source, pair.target, relation));
            }
        }.execute();
    }

    //--------------//
    // linkMultiple //
    //--------------//
    /**
     * Add a relation between inters.
     *
     * @param sig  the containing SIG
     * @param strs the list of SourceTargetRelation to add
     */
    @UIThread
    public void linkMultiple (final SIGraph sig,
                              final List<SourceTargetRelation> strs)
    {
        new CtrlTask(DO, "link")
        {
            @Override
            protected void build ()
            {
                for (SourceTargetRelation str : strs) {
                    // Additional tasks?
                    final RelationPair pair = new RelationPair(str.source, str.target);
                    seq.addAll(str.relation.preLink(pair));

                    // Remove conflicting relations if any
                    final boolean sourceIsNew = pair.source != str.source;
                    removeConflictingRelations(seq, sig, sourceIsNew, pair.source, pair.target,
                                               str.relation);
                }

                // Finally, add relation
                for (SourceTargetRelation str : strs) {
                    seq.add(new LinkTask(sig, str.source, str.target, str.relation));
                }
            }
        }.execute();
    }

    //-------------//
    // mergeChords //
    //-------------//
    /**
     * Make a single chord out of the provided two (or more) head chords.
     *
     * @param chords   the head chords to merge
     * @param withStem true for a merge with stem-based head chords, false for whole head chords
     */
    @UIThread
    public void mergeChords (final List<HeadChordInter> chords,
                             final boolean withStem)
    {
        new CtrlTask(DO, "mergeChords")
        {
            private final HeadChordInter newChord = new HeadChordInter(1.0);

            @Override
            protected void build ()
            {
                final SIGraph sig = chords.get(0).getSig();
                final Rectangle newChordBounds = Entities.getBounds(chords);

                // All heads involved
                final List<HeadInter> heads = new ArrayList<>();

                for (HeadChordInter ch : chords) {
                    for (Inter iHead : ch.getNotes()) {
                        heads.add((HeadInter) iHead);
                    }
                }

                Collections.sort(heads, Inters.byReverseCenterOrdinate);

                // Create a new chord ensemble will all heads
                final List<Link> newChordLinks = new ArrayList<>();
                for (HeadInter head : heads) {
                    newChordLinks.add(new Link(head, new Containment(), true));
                }

                // Transfer original chords support relations to the compound chord
                for (HeadChordInter ch : chords) {
                    for (Relation rel : sig.getRelations(ch, Support.class)) {
                        Inter target = sig.getEdgeTarget(rel);
                        Inter other = sig.getOppositeInter(ch, rel);
                        newChordLinks.add(new Link(other, rel.duplicate(), other == target));
                        seq.add(new UnlinkTask(sig, rel));
                    }
                }

                newChord.setManual(true);
                seq.add(new AdditionTask(sig, newChord, newChordBounds, newChordLinks));

                // Unlink each head from its original chord
                for (HeadChordInter ch : chords) {
                    for (Relation rel : sig.getRelations(ch, Containment.class)) {
                        seq.add(new UnlinkTask(sig, rel));
                    }
                }

                if (withStem) {
                    // Build the new stem linked to all heads
                    final List<StemInter> stems = new ArrayList<>();
                    final StemInter newStem = buildStem(chords, stems);
                    final Rectangle newStemBounds = Entities.getBounds(stems);

                    final List<Link> newStemLinks = new ArrayList<>();
                    for (HeadInter head : heads) {
                        newStemLinks.add(new Link(head, new HeadStemRelation(), false));
                    }

                    // Transfer original stem relations (beam, flag) to the compound stem
                    for (StemInter st : stems) {
                        for (Relation rel : sig.getRelations(st, BeamStemRelation.class,
                                                             FlagStemRelation.class)) {
                            Inter target = sig.getEdgeTarget(rel);
                            Inter other = sig.getOppositeInter(st, rel);
                            newStemLinks.add(new Link(other, rel.duplicate(), other == target));
                        }
                    }

                    seq.add(new AdditionTask(sig, newStem, newStemBounds, newStemLinks));

                    // Remove the original stems (and their relations)
                    for (StemInter stem : stems) {
                        seq.add(new RemovalTask(stem));
                    }
                }

                // Remove the original chords (and their relations)
                for (HeadChordInter ch : chords) {
                    seq.add(new RemovalTask(ch));
                }

                logger.debug("Merge {}", seq);
            }

            @Override
            protected void publish ()
            {
                newChord.countDots();
                sheet.getInterIndex().publish(newChord);
            }
        }.execute();
    }

    //-------------//
    // mergeSystem //
    //-------------//
    /**
     * Merge the provided system with its sibling below.
     *
     * @param system the system above
     */
    @UIThread
    public void mergeSystem (final SystemInfo system)
    {
        new CtrlTask(DO, "mergeSystem")
        {
            @Override
            protected void build ()
            {
                final Staff upStaff = system.getLastStaff();
                final BarlineInter upBar = upStaff.getSideBarline(LEFT);

                final List<SystemInfo> systems = sheet.getSystems();
                final SystemInfo systemBelow = systems.get(1 + systems.indexOf(system));
                final Staff downStaff = systemBelow.getFirstStaff();
                final BarlineInter downBar = downStaff.getSideBarline(LEFT);

                // Merge the systems into one
                seq.add(new SystemMergeTask(system));

                if (upBar != null && downBar != null) {
                    // Add connector between up & down bars
                    Shape shape = (upBar.getShape() == Shape.THICK_BARLINE) ? Shape.THICK_CONNECTOR
                            : Shape.THIN_CONNECTOR;
                    Point2D p1 = upBar.getMedian().getP2();
                    Point2D p2 = downBar.getMedian().getP1();
                    Line2D median = new Line2D.Double(p1, p2);
                    double width = (upBar.getWidth() + downBar.getWidth()) * 0.5;
                    BarConnectorInter connector = new BarConnectorInter(shape, 1.0, median, width);
                    SIGraph sig = system.getSig();
                    seq.add(new AdditionTask(
                            sig, connector, connector.getBounds(), Collections.emptySet()));

                    // Link up & down bars
                    seq.add(new LinkTask(sig, upBar, downBar, new BarConnectionRelation()));
                }
            }
        }.execute();
    }

    //-----------//
    // buildStem //
    //-----------//
    /**
     * Build a compound stem out of the provided stem-based head chords.
     *
     * @param chords the provided head chords
     * @param stems  (output) the original chords stems
     * @return the created compound stem
     */
    private StemInter buildStem (List<HeadChordInter> chords,
                                 List<StemInter> stems)
    {
        List<Glyph> glyphs = new ArrayList<>();

        for (HeadChordInter ch : chords) {
            StemInter stem = ch.getStem();
            stems.add(stem);

            if (stem.getGlyph() != null) {
                glyphs.add(stem.getGlyph());
            }
        }

        Collections.sort(stems, Inters.byCenterOrdinate);

        Glyph stemGlyph = glyphs.isEmpty() ? null : sheet.getGlyphIndex().registerOriginal(
                GlyphFactory.buildGlyph(glyphs));
        StemInter stemInter = new StemInter(stemGlyph, 1.0);
        stemInter.setManual(true);

        return stemInter;
    }

    //------//
    // redo //
    //------//
    /**
     * Redo last user (undone) action sequence.
     */
    @UIThread
    public void redo ()
    {
        new CtrlTask(REDO, "redo")
        {
            @Override
            protected void build ()
            {
                seq = history.toRedo();
            }
        }.execute();
    }

    //--------------//
    // removeInters //
    //--------------//
    /**
     * Remove the provided collection of inter (with their relations)
     *
     * @param inters  the inters to remove
     * @param options added options if any
     */
    @UIThread
    public void removeInters (final List<? extends Inter> inters,
                              final Option... options)
    {
        new CtrlTask(DO, "removeInters", options)
        {
            @Override
            protected void build ()
            {
                new RemovalScenario().populate(inters, seq);
            }

            @Override
            protected void publish ()
            {
                sheet.getInterIndex().publish(null);

                // Make sure an on-going edition is not impacted
                Inter edited = sheet.getSheetEditor().getEditedInter();

                if (edited != null && inters.contains(edited)) {
                    sheet.getSheetEditor().closeEditMode();
                }
            }
        }.execute();
    }

    //---------------------//
    // reprocessPageRhythm //
    //---------------------//
    /**
     * Reprocess the rhythm on the whole provided page.
     *
     * @param page page to reprocess
     */
    @UIThread
    public void reprocessPageRhythm (final Page page)
    {
        new CtrlTask(DO, "reprocessPageRhythm", Option.NO_HISTORY)
        {
            @Override
            protected void build ()
            {
                seq.add(new PageTask(page));
            }

            @Override
            protected Step firstImpactedStep ()
            {
                return Step.RHYTHMS;
            }
        }.execute();
    }

    //----------------------//
    // reprocessStackRhythm //
    //----------------------//
    /**
     * Reprocess the rhythm on the provided measure stack.
     *
     * @param stack measure stack to reprocess
     */
    @UIThread
    public void reprocessStackRhythm (final MeasureStack stack)
    {
        new CtrlTask(DO, "reprocessStackRhythm", Option.NO_HISTORY)
        {
            @Override
            protected void build ()
            {
                seq.add(new StackTask(stack));
            }

            @Override
            protected Step firstImpactedStep ()
            {
                return Step.RHYTHMS;
            }

        }.execute();
    }

    //----------------//
    // setSheetEditor //
    //----------------//
    /**
     * Late assignment of editor, to avoid circularities in elaboration, and to allow
     * handling of specific keys.
     *
     * @param sheetEditor the user pane
     */
    public void setSheetEditor (SheetEditor sheetEditor)
    {
        this.sheetEditor = sheetEditor;

        NestView sheetView = sheetEditor.getSheetView();
        InputMap inputMap = sheetView.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // Support for delete key
        inputMap.put(KeyStroke.getKeyStroke("DELETE"), "RemoveAction");
        sheetView.getActionMap().put("RemoveAction", new RemoveAction());
    }

    //------------//
    // splitChord //
    //------------//
    /**
     * Split the provided head chord into 2 separate chords.
     * <p>
     * The strategy is to split the heads where inter-head vertical distance is maximum.
     *
     * @param chord the chord to be split
     */
    @UIThread
    public void splitChord (final HeadChordInter chord)
    {
        new CtrlTask(DO, "splitChord")
        {
            private final List<HeadChordInter> newChords = new ArrayList<>();

            @Override
            protected void build ()
            {
                final SIGraph sig = chord.getSig();

                // Notes are assumed to be ordered bottom up (byReverseCenterOrdinate)
                final List<List<HeadInter>> partitions = partitionHeads(chord);

                for (List<HeadInter> partition : partitions) {
                    final List<Link> newChordLinks = new ArrayList<>();
                    for (HeadInter head : partition) {
                        newChordLinks.add(new Link(head, new Containment(), true));
                    }

                    // Transfer original chords relations to proper sub-chords
                    //TODO
                    //
                    final Rectangle bounds = Entities.getBounds(partition);
                    final HeadChordInter ch = new HeadChordInter(1.0);
                    ch.setManual(true);
                    ch.setStaff(partition.get(0).getStaff());
                    newChords.add(ch);
                    seq.add(new AdditionTask(sig, ch, bounds, newChordLinks));
                }

                // Unlink each head from the original chord
                for (Relation rel : sig.getRelations(chord, Containment.class)) {
                    seq.add(new UnlinkTask(sig, rel));
                }

                final StemInter stem = chord.getStem();
                final Point tail = chord.getTailLocation();
                final int yDir = Integer.compare(tail.y, chord.getCenter().y);

                // Remove the original chord (before dealing with beams)
                seq.add(new RemovalTask(chord));

                // Case of stem-based chord
                if (stem != null) {
                    final Rectangle[] boxes = getSubStemsBounds(stem, tail, yDir, partitions);

                    for (int i = 0; i < 2; i++) {
                        final List<HeadInter> partition = partitions.get(i);

                        // Create stem
                        StemInter s = new StemInter(null, 1.0);
                        s.setManual(true);

                        final List<Link> newStemLinks = new ArrayList<>();
                        for (HeadInter head : partition) {
                            newStemLinks.add(new Link(head, new HeadStemRelation(), false));
                        }

                        // Transfer original stem relations (beams, flags) to proper sub-stem
                        if ((yDir == -1 && i == 1) || (yDir == 1 && i == 0)) {
                            for (Relation rel : sig.getRelations(stem, BeamStemRelation.class,
                                                                 FlagStemRelation.class)) {
                                Inter target = sig.getEdgeTarget(rel);
                                Inter other = sig.getOppositeInter(stem, rel);
                                Relation dup = rel.duplicate();
                                newStemLinks.add(new Link(other, dup, other == target));
                            }
                        }

                        seq.add(new AdditionTask(sig, s, boxes[i], newStemLinks));
                    }

                    // Remove the original stem
                    seq.add(new RemovalTask(stem));
                }

                logger.debug("Split {}", seq);
            }

            @Override
            protected void publish ()
            {
                for (HeadChordInter ch : newChords) {
                    ch.countDots();
                }

                sheet.getInterIndex().publish(null); // TODO: publish both parts?
            }
        }.execute();
    }

    //-------------------//
    // getSubStemsBounds //
    //-------------------//
    /**
     * Compute the box for each of the 2 sub-stems that result from chord split.
     *
     * @param stem       the original chord stem
     * @param tail       the chord tail point
     * @param yDir       stem direction
     * @param partitions the 2 detected head partitions (bottom up)
     * @return the bounds for each sub-stem (bottom up)
     */
    private Rectangle[] getSubStemsBounds (StemInter stem,
                                           Point tail,
                                           int yDir,
                                           List<List<HeadInter>> partitions)
    {
        final Rectangle[] boundsArray = new Rectangle[2];
        final Line2D median = stem.getMedian();
        final int width = sheet.getScale().getStemThickness();

        for (int i = 0; i < 2; i++) {
            final List<HeadInter> p = partitions.get(i);
            final int stemTop;
            final int stemBottom;

            if (i == 0) {
                // Process bottom partition
                if (yDir < 0) {
                    // Stem going up
                    final List<HeadInter> p1 = partitions.get(1); // Other (top) partition
                    stemTop = p1.get(0).getCenter().y;
                    stemBottom = p.get(0).getCenter().y;
                } else {
                    // Stem going down
                    stemTop = p.get(p.size() - 1).getCenter().y;
                    stemBottom = tail.y;
                }
            } else {
                // Process top partition
                if (yDir < 0) {
                    // Stem going up
                    stemTop = tail.y;
                    stemBottom = p.get(0).getCenter().y;
                } else {
                    // Stem going down
                    final List<HeadInter> p0 = partitions.get(0); // Other (bottom) partition
                    stemTop = p.get(p.size() - 1).getCenter().y;
                    stemBottom = p0.get(p0.size() - 1).getCenter().y;
                }
            }

            final Point top = PointUtil.rounded(LineUtil.intersectionAtY(median, stemTop));
            final Point bottom = PointUtil.rounded(LineUtil.intersectionAtY(median, stemBottom));
            final Area area = AreaUtil.verticalParallelogram(top, bottom, width);
            boundsArray[i] = area.getBounds();
        }

        return boundsArray;
    }

    //----------------//
    // partitionHeads //
    //----------------//
    /**
     * Partition the heads of provided chord into 2 partitions.
     *
     * @param chord the provided chord
     * @return the sequence of 2 head partitions
     */
    private List<List<HeadInter>> partitionHeads (HeadChordInter chord)
    {
        final List<? extends Inter> notes = chord.getNotes();

        Point prevCenter = null;
        Integer maxDy = null;
        int bestIndex = 0;

        for (int i = 0; i < notes.size(); i++) {
            HeadInter head = (HeadInter) notes.get(i);
            Point center = head.getCenter();

            if (prevCenter != null) {
                int dy = prevCenter.y - center.y;

                if (maxDy == null || maxDy < dy) {
                    maxDy = dy;
                    bestIndex = i;
                }
            }

            prevCenter = center;
        }

        // We decide to split at bestIndex
        final List<List<HeadInter>> lists = new ArrayList<>();

        List<HeadInter> one = new ArrayList<>();
        for (Inter inter : notes.subList(0, bestIndex)) {
            one.add((HeadInter) inter);
        }

        List<HeadInter> two = new ArrayList<>();
        for (Inter inter : notes.subList(bestIndex, notes.size())) {
            two.add((HeadInter) inter);
        }

        lists.add(one);
        lists.add(two);

        return lists;
    }

    //------//
    // undo //
    //------//
    /**
     * Undo last user action.
     */
    @UIThread
    public void undo ()
    {
        // If an inter is being edited, "undo" simply cancels the ongoing edition
        InterEditor interEditor = sheet.getSheetEditor().getInterEditor();

        if (interEditor != null) {
            interEditor.undo();
            sheet.getSheetEditor().closeEditMode();
            Inter inter = interEditor.getInter();
            inter.getSig().publish(inter, SelectionHint.ENTITY_TRANSIENT);
            BookActions.getInstance().setUndoable(canUndo());

            return;
        }

        new CtrlTask(UNDO, "undo")
        {
            @Override
            protected void build ()
            {
                seq = history.toUndo();
            }
        }.execute();
    }

    //--------//
    // unlink //
    //--------//
    /**
     * Remove a relation between inters.
     *
     * @param sig      the containing SIG
     * @param relation the relation to remove
     */
    @UIThread
    public void unlink (final SIGraph sig,
                        final Relation relation)
    {
        new CtrlTask(DO, "unlink")
        {
            private Inter source = null;

            @Override
            protected void build ()
            {
                seq.add(new UnlinkTask(sig, relation));
                source = sig.getEdgeSource(relation);
            }

            @Override
            protected void publish ()
            {
                sheet.getInterIndex().publish(source);
            }
        }.execute();
    }

    //---------//
    // addText //
    //---------//
    /**
     * Special addition of glyph text
     *
     * @param glyph to be OCR'ed to text lines and words
     * @param shape either TEXT or LYRICS
     */
    @UIThread
    private void addText (final Glyph glyph,
                          final Shape shape)
    {
        new CtrlTask(DO, "addText")
        {
            @Override
            protected void build ()
            {
                if (!OcrUtil.getOcr().isAvailable()) {
                    logger.info(OCR.NO_OCR);

                    return;
                }

                final Point centroid = glyph.getCentroid();
                final SystemInfo system = sheet.getSystemManager().getClosestSystem(centroid);

                if (system == null) {
                    return;
                }

                final SIGraph sig = system.getSig();

                // Retrieve lines relative to glyph origin
                final ByteProcessor buffer = glyph.getBuffer();
                final List<TextLine> relativeLines = new BlockScanner(sheet).scanBuffer(
                        buffer,
                        sheet.getStub().getOcrLanguages().getValue(),
                        glyph.getId());

                // Retrieve absolute lines (and the underlying word glyphs)
                final boolean lyrics = shape == Shape.LYRICS;
                final TextBuilder textBuilder = new TextBuilder(system, lyrics);
                final List<TextLine> lines = textBuilder.processGlyph(
                        buffer,
                        relativeLines,
                        glyph.getTopLeft());

                // Generate the sequence of word/line Inter additions
                for (TextLine line : lines) {
                    logger.debug("line {}", line);

                    TextRole role = line.getRole();
                    Staff staff = null;

                    SentenceInter sentence = null;

                    if (lyrics) {
                        // In lyrics role, check if we should join an existing lyric line
                        sentence = textBuilder.lookupLyricLine(line.getLocation());
                    }

                    for (TextWord textWord : line.getWords()) {
                        logger.debug("word {}", textWord);

                        final WordInter word = lyrics
                                ? new LyricItemInter(textWord)
                                : ((role == TextRole.ChordName)
                                        ? ChordNameInter.createValid(textWord)
                                        : new WordInter(textWord));

                        if (sentence != null) {
                            staff = sentence.getStaff();
                            seq.add(
                                    new AdditionTask(
                                            sig,
                                            word,
                                            textWord.getBounds(),
                                            Arrays.asList(
                                                    new Link(sentence, new Containment(), false))));
                        } else {
                            sentence = lyrics ? LyricLineInter.create(line)
                                    : ((role == TextRole.ChordName) ? ChordNameInter.create(line)
                                            : SentenceInter.create(line));
                            staff = sentence.assignStaff(system, line.getLocation());
                            seq.add(
                                    new AdditionTask(
                                            sig,
                                            word,
                                            textWord.getBounds(),
                                            Collections.emptySet()));
                            seq.add(
                                    new AdditionTask(
                                            sig,
                                            sentence,
                                            line.getBounds(),
                                            Arrays.asList(
                                                    new Link(word, new Containment(), true))));
                        }

                        word.setStaff(staff);
                    }
                }
            }

            @Override
            protected void publish ()
            {
                sheet.getInterIndex().publish(null);
                sheet.getGlyphIndex().publish(null);

                if (!seq.isCancelled()) {
                    sheet.getSheetEditor().getShapeBoard().addToHistory(shape);
                }
            }
        }.execute();
    }

    //----------------//
    // determineStaff //
    //----------------//
    /**
     * Determine the target staff for the provided glyph.
     *
     * @param glyph provided glyph
     * @param ghost glyph-based ghost
     * @param links (output) to be populated by links
     * @return the staff found or null
     */
    private Staff determineStaff (Glyph glyph,
                                  Inter ghost,
                                  Collection<Link> links)
    {
        Staff staff = null;
        SystemInfo system;
        final Point2D center = glyph.getCenter2D();
        final List<Staff> staves = sheet.getStaffManager().getStavesOf(center);

        if (staves.isEmpty()) {
            throw new IllegalStateException("No staff for " + center);
        }

        if ((staves.size() == 1)
                    || ghost instanceof BarlineInter
                    || ghost instanceof StaffBarlineInter) {
            // Staff is uniquely defined
            staff = staves.get(0);
            system = staff.getSystem();
            ghost.setStaff(staff);
            links.addAll(ghost.searchLinks(system));

            return staff;
        }

        // Sort the 2 staves by increasing distance from glyph center
        Collections.sort(staves,
                         (s1, s2) -> Double.compare(s1.distanceTo(center), s2.distanceTo(center)));

        if (constants.useStaffLink.isSet()) {
            // Try to use link
            SystemInfo prevSystem = null;
            StaffLoop:
            for (Staff stf : staves) {
                system = stf.getSystem();

                if (system != prevSystem) {
                    ghost.setStaff(stf); // Start of hack ...
                    links.addAll(ghost.searchLinks(system));

                    for (Link p : links) {
                        if (p.partner.getStaff() != null) {
                            staff = p.partner.getStaff();

                            // We stop on first link found (we check closest staff first)
                            break StaffLoop;
                        }
                    }

                    ghost.setStaff(null); // End of hack
                    links.clear();
                }

                prevSystem = system;
            }
        }

        if ((staff == null) && constants.useStaffProximity.isSet()) {
            // Use proximity to staff (vertical margin defined as ratio of gutter)
            final double bestDist = staves.get(0).distanceTo(center);
            final double otherDist = staves.get(1).distanceTo(center);
            final double gutter = bestDist + otherDist;

            if (bestDist <= (gutter * constants.gutterRatio.getValue())) {
                staff = staves.get(0);
            }
        }

        if (staff == null) {
            // Finally, prompt user...
            int option = StaffSelection.getInstance().prompt();

            if (option >= 0) {
                staff = staves.get(option);
            }
        }

        return staff;
    }

    //-----------//
    // refreshUI //
    //-----------//
    /**
     * Refresh UI after any user action sequence.
     */
    @UIThread
    private void refreshUI ()
    {
        // Update editor display
        sheetEditor.refresh();

        // Update status of undo/redo actions
        final BookActions bookActions = BookActions.getInstance();
        bookActions.setUndoable(canUndo());
        bookActions.setRedoable(canRedo());
    }

    //-------------------//
    // removeCompetitors //
    //-------------------//
    /**
     * Discard any existing Inter with the same underlying glyph.
     *
     * @param ghost the ghost to be added
     * @param glyph the underlying glyph, perhaps null
     * @param seq   (output) task sequence to be populated
     */
    private void removeCompetitors (Inter ghost,
                                    Glyph glyph,
                                    UITaskList seq)
    {
        if (glyph == null) {
            return;
        }

        final SystemInfo system = ghost.getStaff().getSystem();
        final List<Inter> intersected = system.getSig().intersectedInters(glyph.getBounds());
        final List<Inter> competitors = new ArrayList<>();

        for (Inter inter : intersected) {
            if ((inter != ghost) && (inter.getGlyph() == glyph)) {
                competitors.add(inter);
            }
        }

        new RemovalScenario().populate(competitors, seq);
    }

    //----------------------------//
    // removeConflictingRelations //
    //----------------------------//
    /**
     * Remove relations that would conflict with the provided to-be-inserted relation.
     *
     * @param seq         the action sequence being worked upon
     * @param sig         the containing SIG
     * @param sourceIsNew true if source has been changed
     * @param source      the actual source (perhaps different from src)
     * @param target      the target provided by user
     * @param relation    the relation to be inserted between source and target
     */
    private void removeConflictingRelations (UITaskList seq,
                                             SIGraph sig,
                                             boolean sourceIsNew,
                                             Inter source,
                                             Inter target,
                                             Relation relation)
    {
        Set<Relation> toRemove = new LinkedHashSet<>();

        if (relation instanceof SlurHeadRelation) {
            // This relation is declared multi-source & multi-target
            // But is single target (head) for each given side
            SlurInter slur = (SlurInter) source;
            HeadInter head = (HeadInter) target;
            HorizontalSide side = (head.getCenter().x < slur.getCenter().x) ? LEFT : RIGHT;
            SlurHeadRelation existingRel = slur.getHeadRelation(side);

            if (existingRel != null) {
                toRemove.add(existingRel);
            }
        }

        // Conflict on sources
        if (relation.isSingleSource()) {
            for (Relation rel : sig.getRelations(target, relation.getClass())) {
                toRemove.add(rel);
            }
        }

        // Conflict on targets
        if (relation.isSingleTarget()) {
            if (!sourceIsNew) {
                for (Relation rel : sig.getRelations(source, relation.getClass())) {
                    toRemove.add(rel);
                }

                // Specific case of (single target) augmentation dot to shared head:
                // We allow a dot source to augment both mirrored head targets
                if (relation instanceof AugmentationRelation && target instanceof HeadInter) {
                    HeadInter mirror = (HeadInter) target.getMirror();

                    if (mirror != null) {
                        Relation mirrorRel = sig.getRelation(source, mirror, relation.getClass());

                        if (mirrorRel != null) {
                            toRemove.remove(mirrorRel);
                        }
                    }
                }
            }
        }

        for (Relation rel : toRemove) {
            seq.add(new UnlinkTask(sig, rel));
        }
    }

    //-----------------//
    // staffBarlinesOf //
    //-----------------//
    private List<Inter> staffBarlinesOf (Collection<? extends Inter> inters)
    {
        return Inters.inters(inters, new Inters.ClassPredicate(StaffBarlineInter.class));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean useStaffLink = new Constant.Boolean(
                true,
                "Should we use link for staff selection");

        private final Constant.Boolean useStaffProximity = new Constant.Boolean(
                true,
                "Should we use proximity for staff selection");

        private final Constant.Ratio gutterRatio = new Constant.Ratio(
                0.33,
                "Vertical margin as ratio of inter-staff gutter");
    }

    //----------//
    // CtrlTask //
    //----------//
    /**
     * Task class to run user-initiated processing asynchronously.
     */
    private abstract class CtrlTask
            extends VoidTask
    {

        protected final OpKind opKind; // Kind of operation to be performed (DO/UNDO/REDO)

        protected final String opName; // Descriptive name of user action

        protected UITaskList seq = new UITaskList(); // Atomic sequence of tasks

        public CtrlTask (OpKind opKind,
                         String opName,
                         Option... options)
        {
            this.opKind = opKind;
            this.opName = opName;

            seq.setOptions(options);
        }

        @Override
        protected final Void doInBackground ()
        {
            try {
                // 1) Build task(s) sequence
                build();

                if ((seq == null) || seq.isCancelled()) {
                    return null; // Stop!
                }

                // 2) Perform the task(s) sequence
                if (opKind == OpKind.UNDO) {
                    seq.performUndo();
                } else {
                    seq.performDo();
                }

                // 3) Publications at end of sequence
                publish();

                // 4) Impacted steps
                epilog();
            } catch (Throwable ex) {
                logger.warn("Exception in {} {}", opName, ex.toString(), ex);
            }

            return null;
        }

        /** User background building of task(s) sequence. */
        protected void build ()
        {
            // Void by default
        }

        /** User background epilog. */
        protected void epilog ()
        {
            if (opKind == OpKind.DO) {
                sheet.getStub().setModified(true);
            }

            // Re-processKeyboard impacted steps
            final Step latestStep = sheet.getStub().getLatestStep();
            final Step firstStep = firstImpactedStep();
            logger.debug("firstStep: {}", firstStep);

            if ((firstStep != null) && (firstStep.compareTo(latestStep) <= 0)) {
                final EnumSet<Step> steps = EnumSet.range(firstStep, latestStep);

                for (Step step : steps) {
                    logger.debug("Impact {}", step);
                    step.impact(seq, opKind);
                }
            }
        }

        @Override
        @UIThread
        protected void finished ()
        {
            // This method runs on EDT

            // Append seq to history?
            if ((opKind == DO) && (!seq.isCancelled()) && !seq.isOptionSet(Option.NO_HISTORY)) {
                history.add(seq);
            }

            // Refresh user display
            refreshUI();
        }

        /**
         * Report the first step impacted by the task sequence
         *
         * @return the first impacted step
         */
        protected Step firstImpactedStep ()
        {
            // Classes of inter and relation instances involved
            final Set<Class> classes = new HashSet<>();

            for (UITask task : seq.getTasks()) {
                if (task instanceof InterTask) {
                    InterTask interTask = (InterTask) task;
                    classes.add(interTask.getInter().getClass());
                } else if (task instanceof SystemMergeTask) {
                    classes.add(task.getClass());
                } else if (task instanceof RelationTask) {
                    RelationTask relationTask = (RelationTask) task;
                    classes.add(relationTask.getRelation().getClass());
                }
            }

            for (Step step : Step.values()) {
                for (Class classe : classes) {
                    if (step.isImpactedBy(classe)) {
                        return step; // First step impacted
                    }
                }
            }

            return null; // No impact detected
        }

        /** User background publications at end of task(s) sequence. */
        protected void publish ()
        {
            // Void by default
        }
    }

    //--------------//
    // RemoveAction //
    //--------------//
    /**
     * Action to remove the selected inter. (Bound to DELETE key)
     */
    private class RemoveAction
            extends AbstractAction
    {

        @Override
        public void actionPerformed (ActionEvent e)
        {
            List<Inter> inters = sheet.getInterIndex().getEntityService().getSelectedEntityList();

            if ((inters == null) || inters.isEmpty()) {
                return;
            }

            if ((inters.size() == 1) || OMR.gui.displayConfirmation(
                    "Do you confirm this multiple deletion?",
                    "Deletion of " + inters.size() + " inters")) {
                removeInters(inters);
            }
        }
    }

    //----------------------//
    // SourceTargetRelation //
    //----------------------//
    /**
     * A tuple combining source, target and relation, to be handled as a whole.
     */
    public static class SourceTargetRelation
    {

        public final Inter source;

        public final Inter target;

        public final Relation relation;

        public SourceTargetRelation (Inter source,
                                     Inter target,
                                     Relation relation)
        {
            this.source = source;
            this.target = target;
            this.relation = relation;
        }
    }
}
