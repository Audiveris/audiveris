//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S h e e t P a i n t e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.sig.ui.SigPainter;

import omr.ui.util.UIUtil;

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
            logger.warn("Cannot paint system#" + system.getId(), ex);
        }
    }
}
