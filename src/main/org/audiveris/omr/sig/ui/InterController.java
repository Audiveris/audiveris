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
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Partnership;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.step.Step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.PageRhythm;

/**
 * Class {@code InterController} is the UI in charge of dealing with Inter and
 * Relation instances (addition, removal, modifications) to correct OMR output,
 * with the ability to undo & redo at will.
 * <p>
 * It works at sheet level.
 * <p>
 * When adding or dropping an inter, the instance is allocated in proper system (and staff if
 * relevant) together with its relations with existing partners nearby.
 * It is not always obvious to select the proper staff, various techniques are used, and if
 * all have failed, the user is prompted for staff indication.
 * <p>
 * Finally, a proper {@link UITask} is allocated, inserted in controller's history, and run.
 * Undo & Redo actions operate on this history.
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
     * @param glyph the glyph to interpret
     * @param shape the shape to be assigned
     */
    public void addInter (Glyph glyph,
                          Shape shape)
    {
        logger.debug("addInter {} as {}", glyph, shape);

        StaffManager staffManager = sheet.getStaffManager();
        glyph = sheet.getGlyphIndex().registerOriginal(glyph);

        // While interacting with user, make sure we have the related staff & system
        SystemInfo system;
        Staff staff = null;
        Point center = glyph.getCenter();
        List<Staff> staves = staffManager.getStavesOf(center);

        if (staves.isEmpty()) {
            throw new IllegalStateException("No staff for " + center);
        }

        Inter ghost = SymbolFactory.createGhost(shape, 1);
        ghost.setGlyph(glyph);
        ghost.setBounds(glyph.getBounds());

        Collection<Partnership> partnerships = null;

        if (staves.size() == 1) {
            // Staff is uniquely defined
            staff = staves.get(0);
            system = staff.getSystem();
            partnerships = ghost.searchPartnerships(system, false);
        }

        if ((staff == null) && constants.useStaffPartnership.isSet()) {
            // Try to use partnership
            SystemInfo prevSystem = null;
            StaffLoop:
            for (Staff stf : staves) {
                system = stf.getSystem();

                if (system != prevSystem) {
                    partnerships = ghost.searchPartnerships(system, false);

                    for (Partnership p : partnerships) {
                        if (p.partner.getStaff() != null) {
                            staff = p.partner.getStaff();

                            // We stop on first partnership found. TODO: is this erroneous?
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

            return;
        }

        ghost.setStaff(staff);
        system = staff.getSystem();

        // If glyph used by another inter, delete this other inter
        removeCompetitors(glyph, system);

        addGhost(ghost, partnerships);
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
     * @param ghost  the populated inter (staff & bounds are already set)
     * @param center the target location for inter center
     */
    public void dropInter (Inter ghost,
                           Point center)
    {
        logger.debug("dropInter {} at {}", ghost, center);

        SystemInfo system = ghost.getStaff().getSystem();

        // Edges? this depends on ghost class...
        Collection<Partnership> partnerships = ghost.searchPartnerships(system, false);

        addGhost(ghost, partnerships);
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
    public void link (SIGraph sig,
                      Inter source,
                      Inter target,
                      Relation relation)
    {
        logger.debug("link on {} between {} and {}", relation, source, target);

        UITaskList seq = new UITaskList();
        seq.add(new LinkTask(sig, source, target, relation));
        history.add(seq);
        seq.performDo();

        sheet.getStub().setModified(true);
        sheet.getInterIndex().publish(source);

        epilog(seq);
    }

    //------//
    // redo //
    //------//
    /**
     * Redo last user (undone) action sequence.
     */
    public void redo ()
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

        epilog(seq);
    }

    //-------------//
    // removeInter //
    //-------------//
    /**
     * Remove the provided inter (with its relations)
     *
     * @param inter    the inter to remove
     * @param toRemove current set of inters to remove, or null
     */
    public void removeInter (Inter inter,
                             Set<Inter> toRemove)
    {
        if (inter.isRemoved()) {
            return;
        }

        logger.debug("removeInter on {}", inter);

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
        } else {
            // Delete containing ensemble if this is the last member in ensemble
            SIGraph sig = inter.getSig();

            for (Relation rel : sig.getRelations(inter, Containment.class)) {
                InterEnsemble ens = (InterEnsemble) sig.getOppositeInter(inter, rel);

                if (!toRemove.contains(ens) && (ens.getMembers().size() <= 1)) {
                    toRemove.add(ens);
                    seq.add(new RemovalTask(ens));
                }
            }
        }

        history.add(seq);
        seq.performDo();

        sheet.getStub().setModified(true);
        ///sheet.getGlyphIndex().publish(null); // Let glyph displayed, to ease new inter assignment
        sheet.getInterIndex().publish(null);

        epilog(seq);
    }

    //--------------//
    // removeInters //
    //--------------//
    /**
     * Remove the provided collection of inter (with their relations)
     *
     * @param inters   the inters to remove
     * @param toRemove current set of inters to remove, or null
     */
    public void removeInters (Collection<? extends Inter> inters,
                              Set<Inter> toRemove)
    {
        if (toRemove == null) {
            toRemove = new LinkedHashSet<Inter>();
        }

        for (Inter inter : inters) {
            if (!toRemove.contains(inter)) {
                removeInter(inter, toRemove);
            }
        }
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

        epilog(seq);
    }

    /**
     * Remove a relation between inters.
     *
     * @param sig      the containing SIG
     * @param relation the relation to remove
     */
    public void unlink (SIGraph sig,
                        Relation relation)
    {
        logger.debug("unlink on {}", relation);

        UITaskList seq = new UITaskList();
        seq.add(new UnlinkTask(sig, relation));

        Inter source = sig.getEdgeSource(relation);
        history.add(seq);

        seq.performDo();

        sheet.getStub().setModified(true);
        sheet.getInterIndex().publish(source);

        epilog(seq);
    }

    /**
     * Perform ghost addition.
     *
     * @param ghost        the ghost inter to add/drop
     * @param partnerships its partnerships
     */
    private void addGhost (Inter ghost,
                           Collection<Partnership> partnerships)
    {
        final Staff staff = ghost.getStaff();
        final SIGraph sig = staff.getSystem().getSig();
        final UITaskList seq = new UITaskList(new AdditionTask(sig, ghost, partnerships));

        if (ghost instanceof RestInter) {
            // Wrap this rest within a rest chord
            RestChordInter restChord = new RestChordInter(-1);
            restChord.setBounds(ghost.getBounds());
            restChord.setStaff(staff);
            seq.add(
                    new AdditionTask(
                            sig,
                            restChord,
                            Arrays.asList(new Partnership(ghost, new Containment(), true))));
        } else if (ghost instanceof StemInter) {
            // Wrap this stem within a head chord
            final StemInter stem = (StemInter) ghost;
            seq.add(new AdditionTask(sig, new HeadChordInter(-1, stem), Collections.EMPTY_SET));
        } else if (ghost instanceof HeadInter) {
            // If we link head to a stem, create/update the related head chord
            for (Partnership partnership : partnerships) {
                if (partnership.relation instanceof HeadStemRelation) {
                    StemInter stem = (StemInter) partnership.partner;
                    HeadChordInter headChord = stem.getChord();

                    if (headChord == null) {
                        // Create a chord based on stem
                        headChord = new HeadChordInter(-1, stem);
                        seq.add(new AdditionTask(sig, headChord, Collections.EMPTY_SET));
                    }

                    seq.add(new LinkTask(sig, headChord, ghost, new Containment()));

                    break;
                }
            }
        }

        history.add(seq);

        seq.performDo();

        sheet.getStub().setModified(true);
        sheet.getGlyphIndex().publish(null);
        sheet.getInterIndex().publish(ghost);

        epilog(seq);
    }

    /**
     * Epilog for any user action sequence.
     * <p>
     * This depends on the impacted items (inters, relations) and on the current processing step.
     */
    private void epilog (UITaskList seq)
    {
        // Determine which music items should be updated

        // Update stack rhythm?
        final Step step = sheet.getStub().getLatestStep();

        if (step.compareTo(Step.RHYTHMS) >= 0) {
            // Interest in stem, head, beam, chord, flag, rest, augDot, tuplet, curve (tie), ...
            for (UITask task : seq.getTasks()) {
                if (task instanceof InterTask) {
                    InterTask interTask = (InterTask) task;
                    Inter inter = interTask.getInter();
                    Class classe = inter.getClass();

                    if (Step.RHYTHMS.impactingInterClasses().contains(classe)) {
                        logger.info("Update for RHYTHMS step");
                        SystemInfo system = inter.getSig().getSystem();
                        MeasureStack stack = system.getMeasureStackAt(inter.getCenter());
                        Page page = system.getPage();
                        new PageRhythm(page).reprocessStack(stack);

                        break;
                    }
                }
            }
        }

        // Finally, refresh user interface
        refreshUI();
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
    private void removeCompetitors (Glyph glyph,
                                    SystemInfo system)
    {
        final List<Inter> intersected = system.getSig().intersectedInters(glyph.getBounds());

        for (Inter inter : intersected) {
            if (inter.getGlyph() == glyph) {
                removeInter(inter, null);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean useStaffPartnership = new Constant.Boolean(
                true,
                "Should we use partnership for staff selection");

        private final Constant.Boolean useStaffProximity = new Constant.Boolean(
                true,
                "Should we use proximity for staff selection");

        private final Constant.Ratio gutterRatio = new Constant.Ratio(
                0.33,
                "Vertical margin as ratio of inter-staff gutter");
    }

    //--------------//
    // RemoveAction //
    //--------------//
    /**
     * Action to remove the selected inter.
     */
    private class RemoveAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void actionPerformed (ActionEvent e)
        {
            Inter inter = sheet.getInterIndex().getEntityService().getSelectedEntity();

            if (inter != null) {
                removeInter(inter, null);
            }
        }
    }

    //---------------//
    // UnfocusAction //
    //---------------//
    /**
     * Action to remove inter focus.
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
