//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                       A b s t r a c t S t a f f V e r t i c a l I n t e r                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Versions;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.util.Version;

import java.awt.geom.Line2D;
import java.util.List;

/**
 * Class {@code AbstractStaffVerticalInter} is a {@link AbstractVerticalInter} whose
 * height is expected to be the staff height.
 * <p>
 * It is the basis for {@link BarlineInter} and {@link BracketInter}.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractStaffVerticalInter
        extends AbstractVerticalInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code AbstractStaffVerticalInter} object, with its grade.
     *
     * @param glyph   the underlying glyph
     * @param shape   the assigned shape
     * @param impacts the assignment details
     * @param median  the median line
     * @param width   the line width
     */
    public AbstractStaffVerticalInter (Glyph glyph,
                                       Shape shape,
                                       GradeImpacts impacts,
                                       Line2D median,
                                       Double width)
    {
        super(glyph, shape, impacts, median, width);
    }

    /**
     * Creates a new {@code AbstractStaffVerticalInter} object, with detailed impacts.
     *
     * @param glyph  the underlying glyph
     * @param shape  the assigned shape
     * @param grade  the assignment quality
     * @param median the median line
     * @param width  the line width
     */
    public AbstractStaffVerticalInter (Glyph glyph,
                                       Shape shape,
                                       Double grade,
                                       Line2D median,
                                       Double width)
    {
        super(glyph, shape, grade, median, width);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new AbstractVerticalInter.Editor(this, false);
    }

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    @Override
    public boolean upgradeOldStuff (List<Version> upgrades)
    {
        // Applies to sub-classes BarlineInter and BracketInter
        boolean upgraded = false;

        if (upgrades.contains(Versions.INTER_GEOMETRY)) {
            if (median != null) {
                // Height is increased of halfLine on top and bottom sides
                final Scale scale = sig.getSystem().getSheet().getScale();
                final double halfLine = scale.getFore() / 2.0;
                median.setLine(median.getX1() + 0.5, median.getY1() - halfLine,
                               median.getX2() + 0.5, median.getY2() + 1 + halfLine);
                computeArea();
                upgraded = true;
            }
        }

        return upgraded;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    /**
     * Grade impacts.
     */
    public static class Impacts
            extends GradeImpacts
    {

        private static final String[] NAMES = new String[]{
            "core", "gap", "start", "stop", "left", "right"};

        private static final double[] WEIGHTS = new double[]{1, 1, 1, 1, 0.5, 0.5};

        /**
         * Create an Impacts object.
         *
         * @param core  value of black core impact
         * @param gap   value of vertical gap impact
         * @param start derivative impact at start abscissa
         * @param stop  derivative impact at stop abscissa
         * @param left  chunk impact on left of peak
         * @param right chunk impact on right of peak
         */
        public Impacts (double core,
                        double gap,
                        double start,
                        double stop,
                        double left,
                        double right)
        {
            super(NAMES, WEIGHTS);

            setImpact(0, core);
            setImpact(1, gap);
            setImpact(2, start);
            setImpact(3, stop);
            setImpact(4, left);
            setImpact(5, right);
        }
    }
}
