//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n t e r E d i t o r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.sheet.ui.ObjectEditor;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.ui.selection.SelectionHint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;

/**
 * Class <code>InterEditor</code> allows to edit an Inter instance set into edit mode.
 * <p>
 * Creation of an InterEditor instance, can start in various different ways:
 * <ul>
 * <li>From an existing inter, by a double-click or by opening the edit mode.
 * <li>By checking the "Edit" box in InterBoard where the inter appears.
 * <li>By clicking on a location while the repetitive mode is on.
 * This creates a new inter at current location and sets it immediately in editing mode.
 * Dragging operates on the global handle.
 * </ul>
 * <p>
 * <b>Typical scenario of events:</b>
 * (<i>for the sake of illustration, we use the case of dragging a {@link HeadInter}</i>)
 * <ol>
 * <li>Whenever the user drags the selected handle (via mouse or keyboard arrow key),
 * its specific {@link Handle#move(int, int)} method is called which generally results in some
 * modification of editor data model.
 * <br>
 * Snapping can happen in this method, <i>for example a dragged HeadInter will always snap
 * vertically to a staff line or ledger and perhaps horizontally to a stem nearby.</i>
 * <br>
 * Editor {@link #doit()} method is called to apply the (modified) model to inter geometry.
 * <p>
 * <li>Asynchronously, Java Swing repaints the sheet display whenever the user location is changed,
 * which in turn triggers the rendering of the sheet current ObjectEditor.
 * <br>
 * Depending on the inter being edited, an {@link InterTracker} is in charge of the rendering of
 * the moving inter, together with the drawing of potential relations (AKA Links) computed
 * dynamically.
 * <br>
 * <i>The tracker of a HeadInter dragged far from a staff will also draw the segments that
 * represent the needed ledgers computed on-the-fly.</i>
 * <p>
 * <li>The user can keep on editing, by pressing and dragging any handle.
 * <p>
 * <li>When the user presses the mouse outside of any handle or presses the keyboard Enter key,
 * {@link #endProcess()} method is called to finish editing.
 * <br>
 * {@link InterController#editInter(InterEditor)} is thus called to create a
 * <code>CtrlTask</code> to formally record and perform the action (and allow its future undo/redo).
 * <br>
 * The CtrlTask is a private general utility class in InterController, which is launched to run
 * asynchronously in background:
 * <ol>
 * <li><code>build()</code> to populate the sequence of tasks to perform.
 * <br>
 * For an Inter editing, this method uses {@link Inter#preEdit(InterEditor)} to at least
 * append an {@link EditingTask} with related links and unlinks for the edited inter.
 * <br>
 * Subclasses can append additional tasks to the sequence.
 * <i>For example a HeadInter will append an AdditionTask for each of the needed ledgers.</i>
 * <li><code>performDo()</code> calls editor {@link #doit()} to apply model to inter geometry
 * then simply performs each task in the sequence of tasks.
 * <br>
 * Typically, any link results in the insertion of the corresponding edge in SIG, while an
 * unlink results in an edge removal.
 * <br>
 * <i>For an edited HeadInter, the needed LedgerInter instances get inserted as vertices in
 * SIG.</i>
 * <br>
 * (<code>performUndo()</code> calls editor {@link #undo()} to apply original model to inter
 * geometry and then undoes the sequence of tasks in reverse order, typically resulting in the
 * removal (or re-insertion) of vertices and edges in SIG)
 * <li><code>publish()</code> pushes relevant event information to InterIndex,
 * GlyphIndex, shape history, etc as needed.
 * <li><code>epilog()</code> updates any sheet step(s) impacted by the editing.
 * </ol>
 * <li>When CtrlTask completes successfully, its task sequence is appended to InterController
 * history, making it available for user undo/redo.
 * <p>
 * <li>Finally, user display is refreshed.
 * </ol>
 * <img alt="Editing diagram" src="doc-files/Editor.png">
 *
 * @see InterDnd
 * @author Hervé Bitteur
 */
public abstract class InterEditor
        extends ObjectEditor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InterEditor.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Tracker to render inter and its decorations. */
    protected final InterTracker tracker;

    /** Original glyph, if any. */
    protected Glyph originalGlyph;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a new <code>InterEditor</code> object.
     *
     * @param inter the inter instance being edited
     */
    protected InterEditor (Inter inter)
    {
        super(
                inter,
                (inter.getSig() != null) ? inter.getSig().getSystem()
                        : ((inter.getStaff() != null) ? inter.getStaff().getSystem() : null));

        // Tracker
        tracker = inter.getTracker(system.getSheet());
        tracker.setSystem(system);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // concerns //
    //----------//
    /**
     * Report whether the provided Inter is concerned by this editor.
     * <p>
     * By default, check is made on Editor single inter.
     *
     * @param inter provided inter
     * @return true if provided inter is involved in editor activity
     */
    public boolean concerns (Inter inter)
    {
        return inter == getInter();
    }

    //------//
    // doit //
    //------//
    @Override
    protected void doit ()
    {
        final Inter inter = getInter();

        if (inter.getGlyph() != null) {
            originalGlyph = inter.getGlyph();
        }

        // Since inter has moved, link to its glyph is no longer valid
        inter.setGlyph(null);

        updateEnsemble();
    }

    //------------//
    // endProcess //
    //------------//
    @Override
    public void endProcess ()
    {
        logger.debug("End of inter editing");

        if (hasMoved) {
            system.getSheet().getInterController().editInter(this);
        }

        final Inter inter = getInter();

        // To update the edit checkbox on interboard
        inter.getSig().publish(inter.isRemoved() ? null : inter);

        system.getSheet().getSheetEditor().closeEditMode();
    }

    //----------//
    // getInter //
    //----------//
    /**
     * Report the inter being edited.
     *
     * @return the edited inter
     */
    public Inter getInter ()
    {
        return (Inter) getObject();
    }

    //---------//
    // publish //
    //---------//
    @Override
    public void publish ()
    {
        final Inter inter = getInter();

        if (!inter.isRemoved()) {
            inter.getSig().publish(inter, SelectionHint.ENTITY_TRANSIENT);
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the editor handles onto the provided graphics, together with current status
     * inter and its decorations (support links, needed ledgers).
     *
     * @param g provided graphics.
     *          (this can be the sheet zoomed and scrolled view, or the global glass pane)
     */
    @Override
    public void render (Graphics2D g)
    {
        final SIGraph sig = getInter().getSig();

        if (sig == null) {
            return;
        }

        // Inter with decorations, etc.
        tracker.render(g);

        // Handles
        super.render(g);
    }

    //------//
    // undo //
    //------//
    @Override
    public void undo ()
    {
        final Inter inter = getInter();

        if (originalGlyph != null) {
            inter.setGlyph(originalGlyph);
        }

        // Lookup attachments are no longer consistent, we can simply remove them
        // They will be recreated anew on next editing action
        inter.removeAttachments("");

        updateEnsemble();
    }

    //----------------//
    // updateEnsemble //
    //----------------//
    /**
     * Update bounds of containing ensemble, if any.
     */
    protected void updateEnsemble ()
    {
        // Update ensemble bounds
        Inter ens = getInter().getEnsemble();

        if (ens != null) {
            ens.setBounds(null);
        }
    }
}
