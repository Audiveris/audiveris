//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S h e e t P a i n t e r                                     //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.ui.SigPainter;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ConcurrentModificationException;

/**
 * Class {@code SheetPainter} provides a basis to paint sheet content.
 * <p>
 * It is specialized in:<ul>
 * <li>{@link SheetGradedPainter} which displays all SIG inters with opacity derived from each inter
 * grade value.</li>
 * <li>{@link SheetResultPainter} which displays the resulting score (SIG remaining inters,
 * measures, time slots, etc).</li>
 * </ul>
 * The bulk of painting is delegated to an internal {@link SigPainter} instance.
 *
 * @author Hervé Bitteur
 */
public abstract class SheetPainter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetPainter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sheet. */
    protected final Sheet sheet;

    /** Graphic context. */
    protected final Graphics2D g;

    /** Clip rectangle. */
    protected final Rectangle clip;

    /** Painter for Inter instances. */
    protected SigPainter sigPainter;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SheetPainter object.
     *
     * @param sheet the sheet to paint
     * @param g     Graphic context
     */
    public SheetPainter (Sheet sheet,
                         Graphics g)
    {
        this.sheet = sheet;
        this.g = (Graphics2D) g;

        clip = g.getClipBounds();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Paint the sheet.
     *
     */
    public void process ()
    {
        sigPainter = getSigPainter();

        if (!sheet.getSystems().isEmpty()) {
            for (SystemInfo system : sheet.getSystems()) {
                // Check whether this system is visible
                Rectangle bounds = system.getBounds();

                if ((bounds != null) && ((clip == null) || bounds.intersects(clip))) {
                    processSystem(system);
                }
            }
        }
    }

    //---------------//
    // getSigPainter //
    //---------------//
    protected abstract SigPainter getSigPainter ();

    //---------------//
    // processSystem //
    //---------------//
    protected void processSystem (SystemInfo system)
    {
        try {
            // Staff lines attachments
            UIUtil.setAbsoluteStroke(g, 1.0f);

            for (Staff staff : system.getStaves()) {
                staff.renderAttachments(g);
            }

            // All interpretations for this system
            sigPainter.process(system.getSig());
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn("Cannot paint system#{}", system.getId(), ex);
        }
    }
}
