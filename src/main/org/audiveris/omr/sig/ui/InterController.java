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
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.ChordStemRelation;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.UITask.OpKind;
import static org.audiveris.omr.sig.ui.UITask.OpKind.*;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.VoidTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

    /** User focus on a given Inter, if any. */
    private Inter interFocus;

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
                Point center = glyph.getCenter();
                List<Staff> staves = staffManager.getStavesOf(center);

                if (staves.isEmpty()) {
                    throw new IllegalStateException("No staff for " + center);
                }

                Inter ghost = SymbolFactory.createGhost(shape, 1);
                ghost.setBounds(glyph.getBounds());
                ghost.setGlyph(glyph);

                Collection<Link> links = null;

                if (staves.size() == 1) {
                    // Staff is uniquely defined
                    staff = staves.get(0);
                    system = staff.getSystem();
                    links = ghost.searchLinks(system, false);
                }

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

                                    // We stop on first link found. TODO: is this erroneous?
                                    break StaffLoop;
                                }
                            }
                        }

                        prevSystem = system;
                    }
                }

                if ((staff == null) && constants.useStaffProximity.isSet()) {
                    // Use proximity to staff (vertical margin defined as ratio of gutter)
                    double bestDist = Double.MAX_VALUE;
                    Staff bestStf = null;
                    double gutter = 0;

                    for (Staff stf : staves) {
                        double dist = stf.distanceTo(center);
                        gutter += dist;

                        if (bestDist > dist) {
                            bestDist = dist;
                            bestStf = stf;
                        }
                    }

                    if (bestDist <= (gutter * constants.gutterRatio.getValue())) {
                        staff = bestStf;
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

                addGhost(ghost, links);

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

    //---------------//
    // getInterFocus //
    //---------------//
    /**
     * Report the inter, if any, which has UI focus.
     *
     * @return the interFocus, perhaps null
     */
    public Inter getInterFocus ()
    {
        return interFocus;
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

                sheet.getStub().setModified(true);
                sheet.getInterIndex().publish(source);

                epilog(seq, DO);

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
                seq.performRedo();

                sheet.getStub().setModified(true);

                UITask task = seq.getLastTask();

                if (task instanceof InterTask) {
                    sheet.getGlyphIndex().publish(null);

                    Inter inter = ((InterTask) task).getInter();
                    sheet.getInterIndex().publish(inter.isRemoved() ? null : inter);
                } else {
                    RelationTask relTask = (RelationTask) task;
                    Inter source = relTask.getSource();
                    sheet.getInterIndex().publish(source);
                }

                epilog(seq, REDO);

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
        if (inter.isRemoved()) {
            return;
        }

        final Set<Inter> toRemove = new LinkedHashSet<Inter>();

        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                if (inter.isVip()) {
                    logger.info("VIP removeInter {}", inter);
                }

                syncRemoveInter(inter, toRemove);

                return null;
            }
        }.execute();
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
        final Set<Inter> toRemove = new LinkedHashSet<Inter>();

        new CtrlTask()
        {
            @Override
            protected Void doInBackground ()
                    throws Exception
            {
                for (Inter inter : inters) {
                    if (!toRemove.contains(inter)) {
                        syncRemoveInter(inter, toRemove);
                    }
                }

                return null;
            }
        }.execute();
    }

    //---------------//
    // setInterFocus //
    //---------------//
    /**
     * Set UI focus on provided inter.
     *
     * @param interFocus the interFocus to set
     */
    public void setInterFocus (Inter interFocus)
    {
        if (interFocus != null) {
            logger.info("Focus set on {}", interFocus);
        } else {
            logger.info("Focus unset");
        }

        this.interFocus = interFocus;
        editor.refresh();
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

        // Support for escape key
        inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "UnfocusAction");
        view.getActionMap().put("UnfocusAction", new UnfocusAction());
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

                sheet.getStub().setModified(true);

                UITask task = seq.getFirstTask();

                if (task instanceof InterTask) {
                    sheet.getGlyphIndex().publish(null);

                    Inter inter = ((InterTask) task).getInter();
                    sheet.getInterIndex().publish(inter.isRemoved() ? null : inter);
                } else {
                    RelationTask relTask = (RelationTask) task;
                    Inter source = relTask.getSource();
                    sheet.getInterIndex().publish(source);
                }

                epilog(seq, UNDO);

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

                sheet.getStub().setModified(true);
                sheet.getInterIndex().publish(source);

                epilog(seq, DO);

                return null;
            }
        }.execute();
    }

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

        sheet.getStub().setModified(true);
        sheet.getGlyphIndex().publish(null);
        sheet.getInterIndex().publish(ghost);

        epilog(seq, DO);
    }

    /**
     * Epilog for any user action sequence.
     * <p>
     * This depends on the impacted items (inters, relations) and on the current processing step.
     *
     * @param seq    the sequence of user tasks
     * @param opKind how is seq used
     */
    private void epilog (UITaskList seq,
                         OpKind opKind)
    {
        // Re-process impacted steps
        final Step latestStep = sheet.getStub().getLatestStep();
        final Step firstStep = firstImpactedStep(seq);

        if ((firstStep != null) && (firstStep.compareTo(latestStep) <= 0)) {
            final EnumSet<Step> steps = EnumSet.range(firstStep, latestStep);

            for (Step step : steps) {
                logger.debug("Impact {}", step);
                step.impact(seq, opKind);
            }
        }
    }

    /**
     * Report the first step impacted by the provided task sequence
     *
     * @param seq the provided task sequence
     * @return the first step impacted, perhaps null
     */
    private Step firstImpactedStep (UITaskList seq)
    {
        InterTask interTask = seq.getFirstInterTask();

        if (interTask == null) {
            // seq contains only relation(s) and no inter
            logger.info("TODO: No inter in UI task sequence {}", seq);

            return null;
        }

        Inter inter = interTask.inter;
        Class interClass = inter.getClass();

        for (Step step : Step.values()) {
            if (step.impactingInterClasses().contains(interClass)) {
                return step;
            }
        }

        return null;
    }

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

        for (Inter inter : intersected) {
            if ((inter != ghost) && (inter.getGlyph() == glyph)) {
                syncRemoveInter(inter, null);
            }
        }
    }

    //-----------------//
    // syncRemoveInter //
    //-----------------//
    /**
     * Remove the provided inter (with its relations)
     *
     * @param inter    the inter to remove
     * @param toRemove current set of inters to remove, or null
     */
    private void syncRemoveInter (Inter inter,
                                  Set<Inter> toRemove)
    {
        if (inter.isRemoved()) {
            return;
        }

        if (inter.isVip()) {
            logger.info("VIP removeInter {}", inter);
        }

        if (toRemove == null) {
            toRemove = new LinkedHashSet<Inter>();
        }

        toRemove.add(inter);

        final UITaskList seq = new UITaskList();
        seq.add(new RemovalTask(inter));

        if (inter instanceof InterEnsemble) {
            // Remove all the ensemble members
            final InterEnsemble ens = (InterEnsemble) inter;
            final List<? extends Inter> members = ens.getMembers();

            for (Inter member : members) {
                if (!toRemove.contains(member)) {
                    seq.add(new RemovalTask(member));
                }
            }

            if (inter instanceof HeadChordInter) {
                // Remove the chord stem as well
                final HeadChordInter chord = (HeadChordInter) inter;
                final StemInter stem = chord.getStem();

                if ((stem != null) && !toRemove.contains(stem)) {
                    seq.add(new RemovalTask(stem));
                }
            }
        } else {
            // Delete containing ensemble if this is the last member in ensemble
            final SIGraph sig = inter.getSig();

            for (Relation rel : sig.getRelations(inter, Containment.class)) {
                final InterEnsemble ens = (InterEnsemble) sig.getOppositeInter(inter, rel);

                if (!toRemove.contains(ens) && (ens.getMembers().size() <= 1)) {
                    toRemove.add(ens);
                    seq.add(new RemovalTask(ens));
                }
            }
        }

        // Slur extensions if any
        if (inter instanceof SlurInter) {
            final SlurInter slur = (SlurInter) inter;

            for (HorizontalSide side : HorizontalSide.values()) {
                final SlurInter ext = slur.getExtension(side);

                if ((ext != null) && !ext.isRemoved()) {
                    seq.add(new RemovalTask(ext));
                }
            }
        }

        history.add(seq);
        seq.performDo();

        sheet.getStub().setModified(true);
        ///sheet.getGlyphIndex().publish(null); // Let glyph displayed, to ease new inter assignment
        sheet.getInterIndex().publish(null);

        epilog(seq, DO);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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
            final Inter inter = sheet.getInterIndex().getEntityService().getSelectedEntity();

            if (inter != null) {
                removeInter(inter);
            }
        }
    }

    //---------------//
    // UnfocusAction //
    //---------------//
    /**
     * Action to remove inter focus. (Bound to ESCAPE key)
     */
    private class UnfocusAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            if (getInterFocus() != null) {
                setInterFocus(null);
            }
        }
    }
}
