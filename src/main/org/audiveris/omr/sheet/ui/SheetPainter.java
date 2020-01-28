//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S h e e t P a i n t e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.ui.SigPainter;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.OmrFont;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.util.ConcurrentModificationException;

/**
 * Class {@code SheetPainter} provides a basis to paint sheet content.
 * <p>
 * It is specialized in:
 * <ul>
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

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SheetPainter.class);

    /** A transformation to half scale. (used for slot time annotation) */
    protected static final AffineTransform halfAT = AffineTransform.getScaleInstance(0.5, 0.5);

    /** Font for annotations. */
    protected static final Font basicFont = new Font(
            "Sans Serif",
            Font.PLAIN,
            constants.basicFontSize.getValue());

    /** Font for chord annotations. */
    protected static final Font chordFont = new Font(
            "Sans Serif",
            Font.PLAIN,
            constants.chordFontSize.getValue());

    /** Sheet. */
    protected final Sheet sheet;

    /** View parameters. */
    protected final ViewParameters viewParams = ViewParameters.getInstance();

    /** Graphic context. */
    protected final Graphics2D g;

    /** Clip rectangle. */
    protected final Rectangle clip;

    /** Painter for Inter instances. */
    protected SigPainter sigPainter;

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

    //---------//
    // process //
    //---------//
    /**
     * Paint the sheet.
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

    //-------------//
    // basicLayout //
    //-------------//
    /**
     * Build a TextLayout from a String of BasicFont characters
     * (transformed by the provided AffineTransform if any)
     *
     * @param str the string of proper codes
     * @param fat potential affine transformation
     * @return the (sized) TextLayout ready to be drawn
     */
    protected TextLayout basicLayout (String str,
                                      AffineTransform fat)
    {
        FontRenderContext frc = g.getFontRenderContext();
        Font font = (fat == null) ? basicFont : basicFont.deriveFont(fat);

        return new TextLayout(str, font, frc);
    }

    //---------------//
    // getSigPainter //
    //---------------//
    /**
     * Report the concrete sig painter to be used.
     *
     * @return the sig painter
     */
    protected abstract SigPainter getSigPainter ();

    //-------//
    // paint //
    //-------//
    /**
     * This is the general paint method for drawing a symbol layout, at a specified
     * location, using a specified alignment
     *
     * @param layout    what: the symbol, perhaps transformed
     * @param location  where: the precise location in the display
     * @param alignment how: the way the symbol is aligned wrt the location
     */
    protected void paint (TextLayout layout,
                          Point location,
                          Alignment alignment)
    {
        OmrFont.paint(g, layout, location, alignment);
    }

    //---------------//
    // processSystem //
    //---------------//
    /**
     * Process a system.
     *
     * @param system the system to process
     */
    protected void processSystem (SystemInfo system)
    {
        try {
            // Staff lines attachments
            UIUtil.setAbsoluteStroke(g, 1.0f);

            for (Staff staff : system.getStaves()) {
                staff.renderAttachments(g);
            }

            // Part limits
            if (constants.drawPartLimits.isSet()) {
                sigPainter.drawPartLimits(system);
            }

            // All interpretations for this system
            sigPainter.process(system.getSig());
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warn("Cannot paint system#{}", system.getId(), ex);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer basicFontSize = new Constant.Integer(
                "points",
                30,
                "Standard font size for annotations");

        private final Constant.Integer chordFontSize = new Constant.Integer(
                "points",
                5,
                "Font size for chord annotations");

        private final Constant.Boolean drawPartLimits = new Constant.Boolean(
                false,
                "Should we draw part upper and lower core limits");
    }
}
