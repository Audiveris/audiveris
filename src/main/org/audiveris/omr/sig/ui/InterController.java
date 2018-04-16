//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  I n t e r C o n t r o l l e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ui.NestView;
import org.audiveris.omr.glyph.ui.SymbolsEditor;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.symbol.SymbolFactory;
import org.audiveris.omr.sheet.ui.BookActions;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.ChordNameInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.BasicContainment;
import org.audiveris.omr.sig.relation.ChordStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.UITask.OpKind;
import static org.audiveris.omr.sig.ui.UITask.OpKind.*;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.text.GlyphScanner;
import org.audiveris.omr.text.TextBuilder;
import org.audiveris.omr.text.TextLine;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.text.TextWord;
import org.audiveris.omr.ui.util.UIThread;
import org.audiveris.omr.util.VoidTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
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
    private SymbolsEditor editor;

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
     * Add a shape interpretation based on a provided glyph.
     *
     * @param aGlyph the glyph to interpret
     * @param shape  the shape to be assigned
     */
    @UIThread
    public void addInter (Glyph aGlyph,
                          final Shape shape)
    {
        logger.debug("addInter {} as {}", aGlyph, shape);

        if ((shape == Shape.TEXT) || (shape == Shape.LYRICS)) {
            addText(aGlyph, shape);

            return;
        }

        final Glyph glyph = sheet.getGlyphIndex().registerOriginal(aGlyph);

        new CtrlTask(DO)
        {
            @Override
            protected Void doInBackground ()
            {
                try {
                    Collection<Link> links = new ArrayList<Link>();
                    Inter ghost = SymbolFactory.createManual(shape);
                    ghost.setBounds(glyph.getBounds());
                    ghost.setGlyph(glyph);

                    // While interacting with user, make sure we have the target staff
                    Staff staff = determineStaff(glyph, ghost, links);

                    if (staff != null) {
                        SystemInfo system = staff.getSystem();
                        ghost.setStaff(staff);

                        // If glyph used by another inter, delete this other inter
                        removeCompetitors(ghost, glyph, system, seq);

                        // Make sure we have the correct links
                        if (links.isEmpty()) {
                            links.addAll(ghost.searchLinks(system, false));
                        }

                        addGhost(seq, ghost, links);
                        epilog(seq);
                    } else {
                        logger.info("No staff, abandonned.");
                    }
                } catch (Throwable ex) {
                    logger.warn("Exception in addInter {}", ex.toString(), ex);
                }

                return null;
            }
        }.execute();
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
     *
     * @param sentence the sentence to modify
     * @param newRole  the new role for the sentence
     */
    @UIThread
    public void changeSentence (final SentenceInter sentence,
                                final TextRole newRole)
    {
        logger.debug("changeSentence {} for {}", sentence, newRole);

        new CtrlTask(DO)
        {
            @Override
            protected Void doInBackground ()
            {
                try {
                    seq.add(new SentenceRoleTask(sentence, newRole));
                    seq.performDo();
                    sheet.getInterIndex().publish(sentence);
                    epilog(seq);
                } catch (Throwable ex) {
                    logger.warn("Exception in changeSentence {}", ex.toString(), ex);
                }

                return null;
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
        logger.debug("changeWord {} for {}", word, newValue);

        new CtrlTask(DO)
        {
            @Override
            protected Void doInBackground ()
            {
                try {
                    seq.add(new WordValueTask(word, newValue));
                    seq.performDo();
                    sheet.getInterIndex().publish(word);
                    epilog(seq);
                } catch (Throwable ex) {
                    logger.warn("Exception in changeWord {}", ex.toString(), ex);
                }

                return null;
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

        if (editor != null) {
            refreshUI();
        }
    }

    //-----------//
    // dropInter //
    //-----------//
    /**
     * Add a shape interpretation by dropping a symbol at given location.
     *
     * @param ghost  the populated inter (staff &amp; bounds are already set)
     * @param center the target location for inter center (could be useful)
     */
    @UIThread
    public void dropInter (final Inter ghost,
                           Point center)
    {
        logger.debug("dropInter {} at {}", ghost, center);

        new CtrlTask(DO)
        {
            @Override
            protected Void doInBackground ()
            {
                try {
                    SystemInfo system = ghost.getStaff().getSystem();

                    // Edges? this depends on ghost class...
                    Collection<Link> links = ghost.searchLinks(system, false);

                    addGhost(seq, ghost, links);
                    epilog(seq);
                } catch (Throwable ex) {
                    logger.warn("Exception in dropInter {}", ex.toString(), ex);
                }

                return null;
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
        new CtrlTask(DO)
        {
            @Override
            protected Void doInBackground ()
            {
                try {
                    if (relation instanceof HeadStemRelation) {
                        HeadInter head = (HeadInter) source;
                        StemInter stem = (StemInter) target;
                        HeadChordInter headChord = (HeadChordInter) head.getChord();

                        if (headChord != null) {
                            List<HeadChordInter> stemChords = stem.getChords();

                            if (!stemChords.isEmpty() && !stemChords.contains(headChord)) {
                                // Unlink head from headChord
                                seq.add(
                                        new UnlinkTask(
                                                sig,
                                                sig.getRelation(
                                                        headChord,
                                                        head,
                                                        BasicContainment.class)));

                                if (headChord.getNotes().size() <= 1) {
                                    // Remove headChord getting empty
                                    seq.add(new RemovalTask(headChord));
                                }

                                // Link head to stem chord
                                seq.add(
                                        new LinkTask(
                                                sig,
                                                stemChords.get(0),
                                                head,
                                                new BasicContainment()));
                            }
                        }
                    }

                    // Remove conflicting relations if any
                    Set<Relation> toRemove = new LinkedHashSet<Relation>();

                    if (relation.isSingleSource()) {
                        for (Relation rel : sig.getRelations(target, relation.getClass())) {
                            toRemove.add(rel);
                        }
                    }

                    if (relation.isSingleTarget()) {
                        for (Relation rel : sig.getRelations(source, relation.getClass())) {
                            toRemove.add(rel);
                        }
                    }

                    for (Relation rel : toRemove) {
                        seq.add(new UnlinkTask(sig, rel));
                    }

                    // Finally, add relation
                    seq.add(new LinkTask(sig, source, target, relation));
                    seq.performDo();
                    epilog(seq);
                } catch (Throwable ex) {
                    logger.warn("Exception in process {}", ex.toString(), ex);
                }

                return null;
            }
        }.execute();
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
        new CtrlTask(REDO)
        {
            @Override
            protected Void doInBackground ()
            {
                try {
                    seq = history.toRedo();
                    seq.performDo();

                    sheet.getInterIndex().publish(null);

                    epilog(seq);
                } catch (Throwable ex) {
                    logger.warn("Exception in redo {}", ex.toString(), ex);
                }

                return null;
            }
        }.execute();
    }

    //-------------//
    // removeInter //
    //-------------//
    /**
     * Remove the provided inter (with its relations)
     *
     * @param inter the inter to remove
     */
    @UIThread
    public void removeInter (final Inter inter)
    {
        removeInters(Arrays.asList(inter));
    }

    //--------------//
    // removeInters //
    //--------------//
    /**
     * Remove the provided collection of inter (with their relations)
     *
     * @param inters the inters to remove
     */
    @UIThread
    public void removeInters (final Collection<? extends Inter> inters)
    {
        new CtrlTask(DO)
        {
            @Override
            protected Void doInBackground ()
            {
                try {
                    populateRemovals(inters, seq);

                    seq.performDo();
                    sheet.getInterIndex().publish(null);
                    epilog(seq);
                } catch (Throwable ex) {
                    logger.warn("Exception in removeInters {}", ex.toString(), ex);
                }

                return null;
            }
        }.execute();
    }

    //-----------------//
    // reprocessRhythm //
    //-----------------//
    /**
     * Reprocess the rhythm on the provided measure stack.
     *
     * @param stack measure stack to reprocess
     */
    @UIThread
    public void reprocessRhythm (final MeasureStack stack)
    {
        new CtrlTask(DO)
        {
            @Override
            protected Void doInBackground ()
            {
                try {
                    sheet.getStub().setModified(true);

                    seq = null; // So that history is not modified

                    // Re-process impacted steps
                    final UITaskList tempSeq = new UITaskList(new StackTask(stack));
                    final Step latestStep = sheet.getStub().getLatestStep();
                    final Step firstStep = Step.RHYTHMS;

                    final EnumSet<Step> steps = EnumSet.range(firstStep, latestStep);

                    for (Step step : steps) {
                        logger.debug("Impact {}", step);
                        step.impact(tempSeq, OpKind.DO);
                    }
                } catch (Throwable ex) {
                    logger.warn("Exception in reprocessRhythm {}", ex.toString(), ex);
                }

                return null;
            }
        }.execute();
    }

    //------------------//
    // setSymbolsEditor //
    //------------------//
    /**
     * Late assignment of editor, to avoid circularities in elaboration, and to allow
     * handling of specific keys.
     *
     * @param symbolsEditor the user pane
     */
    public void setSymbolsEditor (SymbolsEditor symbolsEditor)
    {
        editor = symbolsEditor;

        NestView view = editor.getView();
        InputMap inputMap = view.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        // Support for delete key
        inputMap.put(KeyStroke.getKeyStroke("DELETE"), "RemoveAction");
        view.getActionMap().put("RemoveAction", new RemoveAction());
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
        new CtrlTask(UNDO)
        {
            @Override
            protected Void doInBackground ()
            {
                try {
                    UITaskList seq = history.toUndo();
                    seq.performUndo();

                    sheet.getInterIndex().publish(null);

                    epilog(seq);
                } catch (Throwable ex) {
                    logger.warn("Exception in undo {}", ex.toString(), ex);
                }

                return null;
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
        new CtrlTask(DO)
        {
            @Override
            protected Void doInBackground ()
            {
                try {
                    logger.debug("unlink on {}", relation);

                    seq.add(new UnlinkTask(sig, relation));

                    Inter source = sig.getEdgeSource(relation);

                    seq.performDo();

                    sheet.getInterIndex().publish(source);

                    epilog(seq);
                } catch (Throwable ex) {
                    logger.warn("Exception in unlink {}", ex.toString(), ex);
                }

                return null;
            }
        }.execute();
    }

    //----------//
    // addGhost //
    //----------//
    /**
     * Perform ghost addition.
     *
     * @param seq   (output) the UITaskList to populate
     * @param ghost the ghost inter to add/drop
     * @param links its links
     */
    private void addGhost (UITaskList seq,
                           Inter ghost,
                           Collection<Link> links)
    {
        final Rectangle ghostBounds = ghost.getBounds();
        final Staff staff = ghost.getStaff();
        final SIGraph sig = staff.getSystem().getSig();

        // Inter addition
        seq.add(new AdditionTask(sig, ghost, ghostBounds, links));

        // Related additions if any
        if (ghost instanceof RestInter) {
            // Wrap this rest within a rest chord
            RestChordInter restChord = new RestChordInter(-1);
            restChord.setStaff(staff);
            seq.add(
                    new AdditionTask(
                            sig,
                            restChord,
                            ghostBounds,
                            Arrays.asList(new Link(ghost, new BasicContainment(), true))));
        } else if (ghost instanceof HeadInter) {
            // If we link head to a stem, create/update the related head chord
            boolean stemFound = false;

            for (Link link : links) {
                if (link.relation instanceof HeadStemRelation) {
                    final StemInter stem = (StemInter) link.partner;
                    final HeadChordInter headChord;
                    final List<HeadChordInter> stemChords = stem.getChords();

                    if (stemChords.isEmpty()) {
                        // Create a chord based on stem
                        headChord = new HeadChordInter(-1);
                        seq.add(
                                new AdditionTask(
                                        sig,
                                        headChord,
                                        stem.getBounds(),
                                        Collections.EMPTY_SET));
                        seq.add(new LinkTask(sig, headChord, stem, new ChordStemRelation()));
                    } else {
                        if (stemChords.size() > 1) {
                            logger.warn("Stem shared by several chords, picked one");
                        }

                        headChord = stemChords.get(0);
                    }

                    // Declare head part of head-chord
                    seq.add(new LinkTask(sig, headChord, ghost, new BasicContainment()));
                    stemFound = true;

                    break;
                }
            }

            if (!stemFound) {
                // Head without stem
                HeadChordInter headChord = new HeadChordInter(-1);
                seq.add(new AdditionTask(sig, headChord, ghostBounds, Collections.EMPTY_SET));
                seq.add(new LinkTask(sig, headChord, ghost, new BasicContainment()));
            }
        }

        seq.performDo();

        sheet.getInterIndex().publish(ghost);
        sheet.getGlyphIndex().publish(null);
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
        new CtrlTask(DO)
        {
            @Override
            protected Void doInBackground ()
            {
                try {
                    final Point centroid = glyph.getCentroid();
                    final SystemInfo system = sheet.getSystemManager().getClosestSystem(
                            centroid);

                    if (system == null) {
                        return null;
                    }

                    final SIGraph sig = system.getSig();

                    // Retrieve lines relative to glyph origin
                    ByteProcessor buffer = glyph.getBuffer();
                    List<TextLine> relativeLines = new GlyphScanner(sheet).scanBuffer(
                            buffer,
                            sheet.getStub().getLanguageParam().getActual(),
                            glyph.getId());

                    // Retrieve absolute lines (and the underlying word glyphs)
                    boolean lyrics = shape == Shape.LYRICS;
                    List<TextLine> lines = new TextBuilder(system, lyrics).retrieveGlyphLines(
                            buffer,
                            relativeLines,
                            glyph.getTopLeft());

                    // Generate the sequence of word/line Inter additions
                    for (TextLine line : lines) {
                        logger.debug("line {}", line);

                        TextRole role = line.getRole();
                        SentenceInter sentence = null;
                        Staff staff = null;

                        for (TextWord textWord : line.getWords()) {
                            logger.debug("word {}", textWord);

                            WordInter word = lyrics ? new LyricItemInter(textWord)
                                    : new WordInter(textWord);

                            if (sentence != null) {
                                seq.add(
                                        new AdditionTask(
                                                sig,
                                                word,
                                                textWord.getBounds(),
                                                Arrays.asList(
                                                        new Link(sentence, new BasicContainment(), false))));
                            } else {
                                sentence = lyrics ? LyricLineInter.create(line)
                                        : ((role == TextRole.ChordName)
                                                ? ChordNameInter.create(line)
                                                : SentenceInter.create(line));
                                staff = sentence.assignStaff(system, line.getLocation());
                                seq.add(
                                        new AdditionTask(
                                                sig,
                                                word,
                                                textWord.getBounds(),
                                                Collections.EMPTY_SET));
                                seq.add(
                                        new AdditionTask(
                                                sig,
                                                sentence,
                                                line.getBounds(),
                                                Arrays.asList(
                                                        new Link(word, new BasicContainment(), true))));
                            }

                            word.setStaff(staff);
                        }
                    }

                    seq.performDo();

                    sheet.getInterIndex().publish(null);
                    sheet.getGlyphIndex().publish(null);

                    epilog(seq);
                } catch (Throwable ex) {
                    logger.warn("Exception in addText {}", ex.toString(), ex);
                }

                return null;
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
        final Point center = glyph.getCenter();
        final List<Staff> staves = sheet.getStaffManager().getStavesOf(center);

        if (staves.isEmpty()) {
            throw new IllegalStateException("No staff for " + center);
        }

        if (staves.size() == 1) {
            // Staff is uniquely defined
            staff = staves.get(0);
            system = staff.getSystem();
            links.addAll(ghost.searchLinks(system, false));

            return staff;
        }

        // Sort the 2 staves by increasing distance from glyph center
        Collections.sort(
                staves,
                new Comparator<Staff>()
        {
            @Override
            public int compare (Staff s1,
                                Staff s2)
            {
                return Double.compare(s1.distanceTo(center), s2.distanceTo(center));
            }
        });

        if ((staff == null) && constants.useStaffLink.isSet()) {
            // Try to use link
            SystemInfo prevSystem = null;
            StaffLoop:
            for (Staff stf : staves) {
                system = stf.getSystem();

                if (system != prevSystem) {
                    links.addAll(ghost.searchLinks(system, false));

                    for (Link p : links) {
                        if (p.partner.getStaff() != null) {
                            staff = p.partner.getStaff();

                            // We stop on first link found (we check closest staff first)
                            break StaffLoop;
                        }
                    }

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

    //-------------------//
    // firstImpactedStep //
    //-------------------//
    /**
     * Report the first step impacted by the provided task sequence
     *
     * @param seq the provided task sequence
     * @return the first step impacted, perhaps null
     */
    private Step firstImpactedStep (UITaskList seq)
    {
        Step firstStep = null;

        for (UITask task : seq.getTasks()) {
            Inter inter = null;

            if (task instanceof InterTask) {
                InterTask interTask = (InterTask) task;
                inter = interTask.inter;
            } else if (task instanceof RelationTask) {
                RelationTask relationTask = (RelationTask) task;
                inter = relationTask.getSource();
            }

            if (inter != null) {
                final Class interClass = inter.getClass();

                for (Step step : Step.values()) {
                    if (step.isImpactedBy(interClass)) {
                        if ((firstStep == null) || (firstStep.compareTo(step) > 0)) {
                            firstStep = step;
                        }

                        break;
                    }
                }
            }
        }

        return firstStep;
    }

    //------------------//
    // populateRemovals //
    //------------------//
    /**
     * Prepare removal of the provided inters (with their relations)
     *
     * @param inters the inters to remove
     * @param seq    the task sequence to append to
     */
    private void populateRemovals (Collection<? extends Inter> inters,
                                   UITaskList seq)
    {
        // Dry run
        final Removal removal = new Removal();

        for (Inter inter : inters) {
            if (inter.isRemoved()) {
                continue;
            }

            if (inter.isVip()) {
                logger.info("VIP removeInter {}", inter);
            }

            removal.include(inter);
        }

        // Now set the removal tasks
        removal.populateTaskList(seq);
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
        editor.refresh();

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
     * @param glyph  underlying glyph
     * @param system containing system
     */
    private void removeCompetitors (Inter ghost,
                                    Glyph glyph,
                                    SystemInfo system,
                                    UITaskList seq)
    {
        final List<Inter> intersected = system.getSig().intersectedInters(glyph.getBounds());
        final List<Inter> competitors = new ArrayList<Inter>();

        for (Inter inter : intersected) {
            if ((inter != ghost) && (inter.getGlyph() == glyph)) {
                competitors.add(inter);
            }
        }

        populateRemovals(competitors, seq);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------//
    // CtrlTask //
    //----------//
    /**
     * Task class to run user-initiated processing asynchronously.
     */
    private abstract class CtrlTask
            extends VoidTask
    {
        //~ Instance fields ------------------------------------------------------------------------

        protected UITaskList seq = new UITaskList();

        protected final OpKind opKind;

        //~ Constructors ---------------------------------------------------------------------------
        public CtrlTask (OpKind opKind)
        {
            this.opKind = opKind;
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Background epilog for any user action sequence.
         *
         * @param seq sequence of user tasks
         */
        protected void epilog (UITaskList seq)
        {
            if (opKind == OpKind.DO) {
                sheet.getStub().setModified(true);
            }

            // Re-process impacted steps
            final Step latestStep = sheet.getStub().getLatestStep();
            final Step firstStep = firstImpactedStep(seq);
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

            // Append to history?
            if ((opKind == DO) && (seq != null)) {
                history.add(seq);
            }

            // Refresh user display
            refreshUI();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

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

    //---------//
    // Removal //
    //---------//
    /**
     * Removal scenario used for dry-run before actual operations.
     */
    private static class Removal
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Non-ensemble inters to be removed. */
        LinkedHashSet<Inter> inters = new LinkedHashSet<Inter>();

        /** Ensemble inters to be removed. */
        LinkedHashSet<InterEnsemble> ensembles = new LinkedHashSet<InterEnsemble>();

        /** Ensemble inters to be watched for potential removal. */
        LinkedHashSet<InterEnsemble> watched = new LinkedHashSet<InterEnsemble>();

        //~ Methods --------------------------------------------------------------------------------
        public void include (Inter inter)
        {
            if (inter instanceof InterEnsemble) {
                // Include the ensemble and its members
                final InterEnsemble ens = (InterEnsemble) inter;
                ensembles.add(ens);
                inters.addAll(ens.getMembers());

                if (inter instanceof HeadChordInter) {
                    // Remove the chord stem as well
                    final HeadChordInter chord = (HeadChordInter) inter;
                    final StemInter stem = chord.getStem();

                    if (stem != null) {
                        inters.add(stem);
                    }
                }
            } else {
                inters.add(inter);

                // Watch the containing ensemble (if not already to be removed)
                final SIGraph sig = inter.getSig();

                for (Relation rel : sig.getRelations(inter, BasicContainment.class)) {
                    final InterEnsemble ens = (InterEnsemble) sig.getOppositeInter(inter, rel);

                    if (!ensembles.contains(ens)) {
                        watched.add(ens);
                    }
                }
            }
        }

        /**
         * Populate the operational task list
         *
         * @param seq the task list to populate
         */
        public void populateTaskList (UITaskList seq)
        {
            // Examine watched ensembles
            for (InterEnsemble ens : watched) {
                List<Inter> members = new ArrayList<Inter>(ens.getMembers());
                members.removeAll(inters);

                if (members.isEmpty()) {
                    ensembles.add(ens);
                }
            }

            // Ensembles to remove first
            for (InterEnsemble ens : ensembles) {
                seq.add(new RemovalTask(ens));
            }

            // Simple inters to remove second
            for (Inter inter : inters) {
                seq.add(new RemovalTask(inter));
            }
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("Removal{");
            sb.append("ensembles:").append(ensembles);
            sb.append(" inters:").append(inters);
            sb.append("}");

            return sb.toString();
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
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            List<Inter> inters = sheet.getInterIndex().getEntityService().getSelectedEntityList();

            if ((inters == null) || inters.isEmpty()) {
                return;
            }

            if ((inters.size() == 1)
                || OMR.gui.displayConfirmation(
                            "Do you confirm this multiple deletion?",
                            "Deletion of " + inters.size() + " inters")) {
                removeInters(inters);
            }
        }
    }
}
