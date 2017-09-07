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
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterMutableEnsemble;
import org.audiveris.omr.sig.relation.AbstractContainment;
import org.audiveris.omr.sig.relation.Partnership;
import org.audiveris.omr.sig.relation.Relation;

import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Class {@code InterController} is the UI in charge of dealing with Inters (addition,
 * removal, modifications) to correct OMR output, with the ability to undo & redo at
 * will.
 * <p>
 * It works at sheet level.
 * <p>
 * When adding or dropping an inter, the instance is allocated in proper system (and staff if
 * relevant) together with its relations with existing partners nearby.
 * It is not always obvious to select the proper staff, various techniques are used, and if
 * all have failed, TODO: the user is prompted for staff indication.
 * <p>
 * Finally, a proper InterTask is allocated, inserted in controller's history, and run.
 * Undo & redo actions operate on this history.
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
    /**
     * Add a shape interpretation based on a provided glyph.
     *
     * @param glyph the glyph to interpret
     * @param shape the shape to be assigned
     * @return the task that carries out the processing
     */
    public Task<Void, Void> addInter (Glyph glyph,
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

            return null;
        }

        ghost.setStaff(staff);
        system = staff.getSystem();

        // If glyph used by another inter, delete this other inter
        removeCompetitors(glyph, system);

        InterTask task = new AdditionTask(system.getSig(), ghost, partnerships);

        history.add(task);

        Task<Void, Void> uTask = task.performDo();

        sheet.getStub().setModified(true);
        sheet.getGlyphIndex().publish(null);
        sheet.getInterIndex().publish(ghost);
        editor.refresh();

        BookActions.getInstance().setUndoable(history.canUndo());
        BookActions.getInstance().setRedoable(history.canRedo());

        logger.info("Added {} in staff#{}", ghost, staff.getId());

        return uTask;
    }

    /**
     * Is a redo possible?
     *
     * @return true if so
     */
    public boolean canRedo ()
    {
        return history.canRedo();
    }

    /**
     * Is an undo possible?
     *
     * @return true if so
     */
    public boolean canUndo ()
    {
        return history.canUndo();
    }

    /**
     * Add a shape interpretation by dropping a symbol at given location.
     *
     * @param ghost  the populated inter (staff & bounds are already set)
     * @param center the target location for inter center
     * @return the task that carries out additional processing
     */
    public Task<Void, Void> dropInter (Inter ghost,
                                       Point center)
    {
        logger.info("drop {} at {}", ghost, center);

        SystemInfo system = ghost.getStaff().getSystem();

        // Edges? this depends on ghost class...
        Collection<Partnership> partnerships = ghost.searchPartnerships(system, false);

        InterTask task = new AdditionTask(system.getSig(), ghost, partnerships);
        history.add(task);

        Task<Void, Void> uTask = task.performDo();

        sheet.getStub().setModified(true);
        sheet.getGlyphIndex().publish(null);
        sheet.getInterIndex().publish(ghost);
        editor.refresh();

        BookActions.getInstance().setUndoable(history.canUndo());
        BookActions.getInstance().setRedoable(history.canRedo());

        return uTask;
    }

    /**
     * Redo last user (undone) action.
     *
     * @return the task that carries out additional processing
     */
    public Task<Void, Void> redo ()
    {
        InterTask task = history.redo();
        logger.info("Redo {}", task);

        Task<Void, Void> uTask = task.performRedo();

        sheet.getStub().setModified(true);
        sheet.getGlyphIndex().publish(null);

        Inter inter = task.getInter();
        sheet.getInterIndex().publish(inter.isDeleted() ? null : inter);
        editor.refresh();

        BookActions.getInstance().setUndoable(history.canUndo());
        BookActions.getInstance().setRedoable(history.canRedo());

        return uTask;
    }

    /**
     * Remove the provided inter (with its relations)
     *
     * @param inter    the inter to remove
     * @param toRemove current set of inters to remove, or null
     * @return the task that carries out additional processing
     */
    public Task<Void, Void> removeInter (Inter inter,
                                         Set<Inter> toRemove)
    {
        if (inter.isDeleted()) {
            return null;
        }

        if (inter.isVip()) {
            logger.info("VIP removeInter for {}", inter);
        }

        if (toRemove == null) {
            toRemove = new LinkedHashSet<Inter>();
        }

        toRemove.add(inter);

        List<? extends Inter> members = null;

        // Remember members if any
        if (inter instanceof InterMutableEnsemble) {
            InterMutableEnsemble ime = (InterMutableEnsemble) inter;
            members = ime.getMembers();
        } else {
            // Delete containing ensemble if this is the last member
            SIGraph sig = inter.getSig();

            for (Relation rel : sig.getRelations(inter, AbstractContainment.class)) {
                InterMutableEnsemble ime = (InterMutableEnsemble) sig.getOppositeInter(inter, rel);

                if (!toRemove.contains(ime) && (ime.getMembers().size() <= 1)) {
                    removeInter(ime, toRemove);
                }
            }
        }

        InterTask task = new RemovalTask(inter);
        history.add(task);

        Task<Void, Void> uTask = task.performDo();

        sheet.getStub().setModified(true);
        ///sheet.getGlyphIndex().publish(null); // Let glyph displayed, to ease new inter assignment
        sheet.getInterIndex().publish(null);
        editor.refresh();

        BookActions.getInstance().setUndoable(history.canUndo());
        BookActions.getInstance().setRedoable(history.canRedo());

        logger.info("Removed {}", inter);

        // Finally, members is any
        if (members != null) {
            removeInters(members, toRemove);
        }

        return uTask;
    }

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

    /**
     * Late assignment of editor, to avoid circularities in elaboration, and to
     * allow handling of delete key.
     *
     * @param symbolsEditor the user pane
     */
    public void setSymbolsEditor (SymbolsEditor symbolsEditor)
    {
        editor = symbolsEditor;

        // Support for delete key
        NestView view = editor.getView();
        view.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("DELETE"),
                "RemoveAction");
        view.getActionMap().put("RemoveAction", new RemoveAction());
    }

    /**
     * Undo last user action.
     *
     * @return the task that carries out additional processing
     */
    public Task<Void, Void> undo ()
    {
        InterTask task = history.undo();
        logger.info("Undo {}", task);

        Task<Void, Void> uTask = task.performUndo();

        sheet.getStub().setModified(true);
        sheet.getGlyphIndex().publish(null);

        Inter inter = task.getInter();
        sheet.getInterIndex().publish(inter.isDeleted() ? null : inter);
        editor.refresh();

        BookActions.getInstance().setUndoable(history.canUndo());
        BookActions.getInstance().setRedoable(history.canRedo());

        return uTask;
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
    private void removeCompetitors (Glyph glyph,
                                    SystemInfo system)
    {
        final Rectangle glyphBounds = glyph.getBounds();

        for (Inter inter : system.getSig().containedInters(glyphBounds)) {
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
}
