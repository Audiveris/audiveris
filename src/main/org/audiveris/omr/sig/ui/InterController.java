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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ui.NestView;
import org.audiveris.omr.glyph.ui.SymbolsEditor;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.inter.Inter;

import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.event.ActionEvent;

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
        // Disabled for 5.0
        return null;

        //        logger.info("add {} for {}", glyph, shape);
        //
        //        StaffManager staffManager = sheet.getStaffManager();
        //        glyph = sheet.getGlyphIndex().registerOriginal(glyph);
        //
        //        // TODO: while interacting with user, make sure we have the related staff & system
        //        SystemInfo system = null;
        //        Staff staff = null;
        //        Point center = glyph.getCenter();
        //        List<Staff> staves = staffManager.getStavesOf(center);
        //
        //        if (staves.isEmpty()) {
        //            throw new IllegalStateException("No staff for " + center);
        //        }
        //
        //        Inter ghost = SymbolFactory.createGhost(shape, 1);
        //        ghost.setGlyph(glyph);
        //        ghost.setBounds(glyph.getBounds());
        //
        //        Collection<Partnership> partnerships = null;
        //
        //        if (staves.size() == 1) {
        //            // We are within one staff height
        //            staff = staves.get(0);
        //            system = staff.getSystem();
        //            partnerships = ghost.searchPartnerships(system, false);
        //        } else {
        //            // We are between two staves
        //            SystemInfo prevSystem = null;
        //            StaffLoop:
        //            for (int i = 0; i < 2; i++) {
        //                system = staves.get(i).getSystem();
        //
        //                if (system != prevSystem) {
        //                    partnerships = ghost.searchPartnerships(system, false);
        //
        //                    for (Partnership p : partnerships) {
        //                        if (p.partner.getStaff() != null) {
        //                            staff = p.partner.getStaff();
        //
        //                            break StaffLoop;
        //                        }
        //                    }
        //                }
        //
        //                prevSystem = system;
        //            }
        //
        //            if (staff == null) {
        //                // Use proximity to staff (1/3 of gutter is considered as staff-related)
        //                double bestDist = Double.MAX_VALUE;
        //                double gutter = 0;
        //                int bestIdx = -1;
        //
        //                for (int i = 0; i < 2; i++) {
        //                    double dist = staves.get(i).distanceTo(center);
        //                    gutter += dist;
        //
        //                    if (bestDist > dist) {
        //                        bestDist = dist;
        //                        bestIdx = i;
        //                    }
        //                }
        //
        //                if (bestDist <= (gutter / 3)) {
        //                    staff = staves.get(bestIdx);
        //                } else {
        //                    // TODO: Ask user!
        //                }
        //            }
        //        }
        //
        //        if (staff == null) {
        //            logger.warn("No staff known at {}", center);
        //
        //            return null;
        //        }
        //
        //        ghost.setStaff(staff);
        //
        //        InterTask task = new AdditionTask(system.getSig(), ghost, partnerships);
        //        history.add(task);
        //
        //        Task<Void, Void> uTask = task.performDo();
        //
        //        sheet.getStub().setModified(true);
        //        sheet.getGlyphIndex().publish(null);
        //        sheet.getInterIndex().publish(ghost);
        //        editor.refresh();
        //
        //        BookActions.getInstance().setUndoable(history.canUndo());
        //        BookActions.getInstance().setRedoable(history.canRedo());
        //
        //        return uTask;
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
        // Disabled for 5.0
        return null;

        //
        //        logger.info("drop {} at {}", ghost, center);
        //
        //        SystemInfo system = ghost.getStaff().getSystem();
        //
        //        // Edges? this depends on ghost class...
        //        Collection<Partnership> partnerships = ghost.searchPartnerships(system, false);
        //
        //        InterTask task = new AdditionTask(system.getSig(), ghost, partnerships);
        //        history.add(task);
        //
        //        Task<Void, Void> uTask = task.performDo();
        //
        //        sheet.getStub().setModified(true);
        //        sheet.getGlyphIndex().publish(null);
        //        sheet.getInterIndex().publish(ghost);
        //        editor.refresh();
        //
        //        BookActions.getInstance().setUndoable(history.canUndo());
        //        BookActions.getInstance().setRedoable(history.canRedo());
        //
        //        return uTask;
    }

    /**
     * Redo last user (undone) action.
     *
     * @return the task that carries out additional processing
     */
    public Task<Void, Void> redo ()
    {
        // Disabled for 5.0
        return null;

        //        InterTask task = history.redo();
        //        Task<Void, Void> uTask = task.performRedo();
        //
        //        sheet.getStub().setModified(true);
        //        sheet.getGlyphIndex().publish(null);
        //
        //        Inter inter = task.getInter();
        //        sheet.getInterIndex().publish(inter.isDeleted() ? null : inter);
        //        editor.refresh();
        //
        //        BookActions.getInstance().setUndoable(history.canUndo());
        //        BookActions.getInstance().setRedoable(history.canRedo());
        //
        //        return uTask;
    }

    /**
     * Remove the provided inter (with its relations)
     *
     * @param inter the inter to remove
     * @return the task that carries out additional processing
     */
    public Task<Void, Void> removeInter (Inter inter)
    {
        // Disabled for 5.0
        return null;

        //        logger.info("remove {}", inter);
        //
        //        InterTask task = new RemovalTask(inter);
        //        history.add(task);
        //
        //        Task<Void, Void> uTask = task.performDo();
        //
        //        sheet.getStub().setModified(true);
        //        sheet.getGlyphIndex().publish(null);
        //        sheet.getInterIndex().publish(null);
        //        editor.refresh();
        //
        //        BookActions.getInstance().setUndoable(history.canUndo());
        //        BookActions.getInstance().setRedoable(history.canRedo());
        //
        //        return uTask;
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
        // Disabled for 5.0
        return null;

        //        InterTask task = history.undo();
        //        Task<Void, Void> uTask = task.performUndo();
        //
        //        sheet.getStub().setModified(true);
        //        sheet.getGlyphIndex().publish(null);
        //
        //        Inter inter = task.getInter();
        //        sheet.getInterIndex().publish(inter.isDeleted() ? null : inter);
        //        editor.refresh();
        //
        //        BookActions.getInstance().setUndoable(history.canUndo());
        //        BookActions.getInstance().setRedoable(history.canRedo());
        //
        //        return uTask;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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
                removeInter(inter);
            }
        }
    }
}
