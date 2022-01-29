//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I n t e r E d i t o r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
import org.audiveris.omr.sheet.ui.ObjectEditor;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.ui.selection.SelectionHint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;

/**
 * Class <code>InterEditor</code> allows to edit an Inter instance set into edit mode.
 * <p>
 * Creation of an InterEditor instance, can start in two different ways:
 * <ul>
 * <li>From an existing inter, by a double-click or by opening the edit mode.
 * <li>By clicking on a location while the repetitive mode is on.
 * This creates a new inter at current location and sets it immediately in edition mode.
 * Dragging operates on the global handle.
 * </ul>
 * <img alt="Edition diagram" src="doc-files/Editor.png">
 *
 * @see InterDnd
 *
 * @author Hervé Bitteur
 */
public abstract class InterEditor
        extends ObjectEditor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(InterEditor.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Tracker to render inter and its decorations. */
    private final InterTracker tracker;

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
        super(inter,
              (inter.getSig() != null) ? inter.getSig().getSystem()
              : ((inter.getStaff() != null) ? inter.getStaff().getSystem() : null));

        // Tracker
        tracker = inter.getTracker(system.getSheet());
        tracker.setSystem(system);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // endProcess //
    //------------//
    @Override
    public void endProcess ()
    {
        logger.debug("End of edition");

        if (hasMoved) {
            system.getSheet().getInterController().editInter(this);
        }

        final Inter inter = getInter();
        inter.getSig().publish(inter); // To update the edit checkbox on interboard
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
        inter.getSig().publish(inter, SelectionHint.ENTITY_TRANSIENT);
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
        // They will be recreated anew on next edition action
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Double handleDetectionRadius = new Constant.Double(
                "pixels",
                6.0,
                "Detection radius around inter handle");

        private final Constant.Double handleHalfSide = new Constant.Double(
                "pixels",
                4.0,
                "Half side of handle rounded rectangle");

        private final Constant.Double handleArcRadius = new Constant.Double(
                "pixels",
                3.0,
                "Arc radius at each corner of handle rounded rectangle");
    }
}
