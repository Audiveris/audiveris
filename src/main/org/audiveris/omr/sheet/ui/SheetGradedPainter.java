//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S h e e t G r a d e d P a i n t e r                              //
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

import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.ui.SigPainter;
import org.audiveris.omr.ui.ViewParameters;

import java.awt.Color;
import java.awt.Graphics;
import org.audiveris.omr.sheet.rhythm.Voice;

/**
 * Class {@code SheetGradedPainter} paints a sheet using shape-based colors and
 * grade-based opacities.
 *
 * @author Hervé Bitteur
 */
public class SheetGradedPainter
        extends SheetPainter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code SheetGradedPainter} object.
     *
     * @param sheet the sheet to paint
     * @param g     Graphic context
     */
    public SheetGradedPainter (Sheet sheet,
                               Graphics g)
    {
        super(sheet, g);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // getSigPainter //
    //---------------//
    @Override
    protected SigPainter getSigPainter ()
    {
        return new GradedSigPainter(g, sheet.getScale());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------------//
    // GradedSigPainter //
    //------------------//
    private class GradedSigPainter
            extends SigPainter
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** View parameters. */
        private final ViewParameters viewParams = ViewParameters.getInstance();

        //~ Constructors ---------------------------------------------------------------------------
        public GradedSigPainter (Graphics g,
                                 Scale scale)
        {
            super(g, scale);
        }

        //~ Methods --------------------------------------------------------------------------------
        //----------//
        // setColor //
        //----------//
        /**
         * Use color that depends on shape with an alpha value that depends on
         * interpretation grade.
         *
         * @param inter the interpretation to colorize
         */
        @Override
        protected void setColor (Inter inter)
        {
            // Shape base color
            Color base = inter.getColor();

            if (viewParams.isVoicePainting()) {
                final Voice voice = inter.getVoice();

                if (voice != null) {
                    base = colorOf(voice);
                }
            }

            final Color color;

            // Should we use translucency?
            if (viewParams.isTranslucentPainting()) {
                // Prefer contextual grade over intrinsic grade when available
                final double grade = inter.getBestGrade();

                // Alpha value [0 .. 255] is derived from grade [0.0 .. 1.0]
                final int alpha = Math.min(255, Math.max(0, (int) Math.rint(255 * grade)));

                color = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
            } else {
                color = base;
            }

            g.setColor(color);
        }
    }
}
