//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S h e e t G r a d e d P a i n t e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.ui;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.sig.inter.Inter;
import omr.sig.ui.SigPainter;

import java.awt.Color;
import java.awt.Graphics;

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
            final Color base = inter.getShape().getColor();

            // Prefer contextual grade over intrinsic grade when available
            final double grade = inter.getBestGrade();

            // Alpha value [0 .. 255] is derived from grade [0.0 .. 1.0]
            final int alpha = Math.min(255, Math.max(0, (int) Math.rint(255 * grade)));

            final Color color = new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
            g.setColor(color);
        }
    }
}
