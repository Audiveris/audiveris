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
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.SystemInfo;
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
import org.audiveris.omr.sig.relation.ChordStemRelation;
import org.audiveris.omr.sig.relation.Containment;
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
 * Finally, a proper {@link UITask} is allocated, inserted in controller's history, and run.
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

    /** User relation suggestion, if any. TODO: use a set for multiple items? */
    private RelationClassAction relationClassAction;

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
    public void addInter (Glyph aGlyph,
                          final Shape shape)
    {
        logger.debug("addInter {} as {}", aGlyph, shape);

        if ((shape == Shape.TEXT) || (shape == Shape.LYRICS)) {
            addText(aGlyph, shape);

            return;
        }

        final Glyph glyph = sheet.getGlyphIndex().registerOriginal(aGlyph);

        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                StaffManager staffManager = sheet.getStaffManager();

                // While interacting with user, make sure we have the related staff & system
                SystemInfo system;
                Staff staff = null;
                final Point center = glyph.getCenter();
                final List<Staff> staves = staffManager.getStavesOf(center);

                if (staves.isEmpty()) {
                    throw new IllegalStateException("No staff for " + center);
                }

                Inter ghost = SymbolFactory.createGhost(shape, 1);
                ghost.setManual(true);
                ghost.setBounds(glyph.getBounds());
                ghost.setGlyph(glyph);

                Collection<Link> links = null;

                if (staves.size() == 1) {
                    // Staff is uniquely defined
                    staff = staves.get(0);
                    system = staff.getSystem();
                    links = ghost.searchLinks(system, false);
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
                        return Double.compare(
                                s1.distanceTo(center),
                                s2.distanceTo(center));
                    }
                });

                if ((staff == null) && constants.useStaffLink.isSet()) {
                    // Try to use link
                    SystemInfo prevSystem = null;
                    StaffLoop:
                    for (Staff stf : staves) {
                        system = stf.getSystem();

                        if (system != prevSystem) {
                            links = ghost.searchLinks(system, false);

                            for (Link p : links) {
                                if (p.partner.getStaff() != null) {
                                    staff = p.partner.getStaff();

                                    // We stop on first link found (we check closest staff first)
                                    break StaffLoop;
                                }
                            }

                            links = null;
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

                if (staff == null) {
                    logger.info("No staff, abandonned.");

                    return null;
                }

                ghost.setStaff(staff);
                system = staff.getSystem();

                // If glyph used by another inter, delete this other inter
                removeCompetitors(ghost, glyph, system);

                // Make sure we have the correct links
                if (links == null) {
                    links = ghost.searchLinks(system, false);
                }

                addGhost(ghost, links);

                return null;
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
    public void addText (final Glyph glyph,
                         final Shape shape)
    {
        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                final Point centroid = glyph.getCentroid();
                final SystemInfo system = sheet.getSystemManager().getClosestSystem(centroid);

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
                TextRole expected = (shape == Shape.LYRICS) ? TextRole.Lyrics : null;
                List<TextLine> lines = new TextBuilder(system, expected, true).retrieveGlyphLines(
                        buffer,
                        relativeLines,
                        glyph.getTopLeft());

                // Generate the sequence of word/line Inter additions
                final UITaskList seq = new UITaskList();

                for (TextLine line : lines) {
                    logger.debug("line {}", line);

                    TextRole role = line.getRole();
                    SentenceInter sentence = null;
                    Staff staff = null;

                    for (TextWord textWord : line.getWords()) {
                        logger.debug("word {}", textWord);

                        WordInter word = (role == TextRole.Lyrics)
                                ? new LyricItemInter(textWord) : new WordInter(
                                        textWord);

                        if (sentence != null) {
                            seq.add(
                                    new AdditionTask(
                                            sig,
                                            word,
                                            textWord.getBounds(),
                                            Arrays.asList(new Link(sentence, new Containment(), false))));
                        } else {
                            sentence = (role == TextRole.Lyrics) ? LyricLineInter.create(line)
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
                                            Arrays.asList(new Link(word, new Containment(), true))));
                        }

                        word.setStaff(staff);
                    }
                }

                history.add(seq);
                seq.performDo();

                sheet.getInterIndex().publish(null);

                epilog(DO, seq);

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
    public void changeSentence (final SentenceInter sentence,
                                final TextRole newRole)
    {
        logger.debug("changeSentence {} for {}", sentence, newRole);

        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                final UITaskList seq = new UITaskList(new SentenceRoleTask(sentence, newRole));
                history.add(seq);

                seq.performDo();

                sheet.getInterIndex().publish(sentence);

                epilog(DO, seq);

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
    public void changeWord (final WordInter word,
                            final String newValue)
    {
        logger.debug("changeWord {} for {}", word, newValue);

        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                final UITaskList seq = new UITaskList(new WordValueTask(word, newValue));
                history.add(seq);

                seq.performDo();

                sheet.getInterIndex().publish(word);

                epilog(DO, seq);

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
    public void dropInter (final Inter ghost,
                           Point center)
    {
        logger.debug("dropInter {} at {}", ghost, center);

        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                SystemInfo system = ghost.getStaff().getSystem();

                // Edges? this depends on ghost class...
                Collection<Link> links = ghost.searchLinks(system, false);

                addGhost(ghost, links);

                return null;
            }
        }.execute();
    }

    //------------------------//
    // getRelationClassAction //
    //------------------------//
    /**
     * @return the relationClassAction
     */
    public RelationClassAction getRelationClassAction ()
    {
        return relationClassAction;
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
    public void link (final SIGraph sig,
                      final Inter source,
                      final Inter target,
                      final Relation relation)
    {
        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                logger.debug("link on {} between {} and {}", relation, source, target);

                UITaskList seq = new UITaskList();
                seq.add(new LinkTask(sig, source, target, relation));
                history.add(seq);
                seq.performDo();

                sheet.getInterIndex().publish(source);

                epilog(DO, seq);

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
    public void redo ()
    {
        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                UITaskList seq = history.toRedo();
                seq.performDo();

                sheet.getInterIndex().publish(null);

                epilog(DO, seq);

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
    public void removeInters (final Collection<? extends Inter> inters)
    {
        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                syncRemoveInters(inters);

                return null;
            }
        }.execute();
    }

    //------------------------//
    // setRelationClassAction //
    //------------------------//
    /**
     * @param relationClassAction the relationClassAction to set
     */
    public void setRelationClassAction (RelationClassAction relationClassAction)
    {
        this.relationClassAction = relationClassAction;
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
    public void undo ()
    {
        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                UITaskList seq = history.toUndo();
                seq.performUndo();

                sheet.getInterIndex().publish(null);

                epilog(UNDO, seq);

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
    public void unlink (final SIGraph sig,
                        final Relation relation)
    {
        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                logger.debug("unlink on {}", relation);

                UITaskList seq = new UITaskList();
                seq.add(new UnlinkTask(sig, relation));

                Inter source = sig.getEdgeSource(relation);
                history.add(seq);

                seq.performDo();

                sheet.getInterIndex().publish(source);

                epilog(DO, seq);

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
     * @param ghost the ghost inter to add/drop
     * @param links its links
     */
    private void addGhost (Inter ghost,
                           Collection<Link> links)
    {
        final Rectangle ghostBounds = ghost.getBounds();
        final Staff staff = ghost.getStaff();
        final SIGraph sig = staff.getSystem().getSig();

        // Inter addition
        final UITaskList seq = new UITaskList(new AdditionTask(sig, ghost, ghostBounds, links));

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
                            Arrays.asList(new Link(ghost, new Containment(), true))));
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
                    seq.add(new LinkTask(sig, headChord, ghost, new Containment()));
                    stemFound = true;

                    break;
                }
            }

            if (!stemFound) {
                // Head without stem
                HeadChordInter headChord = new HeadChordInter(-1);
                seq.add(new AdditionTask(sig, headChord, ghostBounds, Collections.EMPTY_SET));
            }
        }

        history.add(seq);

        seq.performDo();

        sheet.getInterIndex().publish(ghost);

        epilog(DO, seq);
    }

    //--------//
    // epilog //
    //--------//
    /**
     * Epilog for any user action sequence.
     * <p>
     * This depends on the impacted items (inters, relations) and on the current processing step.
     *
     * @param opKind how is seq used
     * @param seq    sequence of user tasks
     */
    private void epilog (OpKind opKind,
                         UITaskList seq)
    {
        sheet.getStub().setModified(true);

        sheet.getGlyphIndex().publish(null);

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

    //-----------//
    // refreshUI //
    //-----------//
    /**
     * Refresh UI after any user action sequence.
     */
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
                                    SystemInfo system)
    {
        final List<Inter> intersected = system.getSig().intersectedInters(glyph.getBounds());
        final List<Inter> competitors = new ArrayList<Inter>();

        for (Inter inter : intersected) {
            if ((inter != ghost) && (inter.getGlyph() == glyph)) {
                competitors.add(inter);
            }
        }

        syncRemoveInters(competitors);
    }

    //------------------//
    // syncRemoveInters //
    //------------------//
    /**
     * Remove the provided inters (with their relations)
     *
     * @param inters the inters to remove
     */
    private void syncRemoveInters (Collection<? extends Inter> inters)
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

        // Now apply the removals
        final UITaskList seq = removal.getTaskList();
        history.add(seq);
        seq.performDo();

        sheet.getInterIndex().publish(null);

        epilog(DO, seq);
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
        //~ Methods --------------------------------------------------------------------------------

        @Override
        protected void finished ()
        {
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
        /**
         * Build the operational task list
         *
         * @return the task list
         */
        public UITaskList getTaskList ()
        {
            UITaskList seq = new UITaskList();

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

            return seq;
        }

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

                for (Relation rel : sig.getRelations(inter, Containment.class)) {
                    final InterEnsemble ens = (InterEnsemble) sig.getOppositeInter(inter, rel);

                    if (!ensembles.contains(ens)) {
                        watched.add(ens);
                    }
                }
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
