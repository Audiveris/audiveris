//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                   A b s t r a c t V e r t i c a l C o n n e c t o r I n t e r                  //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.grid.BarConnection;
import org.audiveris.omr.sig.GradeImpacts;

import java.awt.geom.Line2D;
import java.util.List;
import org.audiveris.omr.sheet.Versions;
import org.audiveris.omr.util.Version;

/**
 * Class {@code AbstractVerticalConnectorInter} represents a vertical connector between
 * staves.
 * <p>
 * It is the basis for {@link BarConnectorInter} and {@link BracketConnectorInter}.
 *
 * @author Hervé Bitteur
 */
public class AbstractVerticalConnectorInter
        extends AbstractVerticalInter
{

    /**
     * Creates a new {@code AbstractVerticalConnectorInter} object.
     *
     * @param connection the underlying connection
     * @param shape      the assigned shape
     * @param impacts    the assignment details
     */
    public AbstractVerticalConnectorInter (BarConnection connection,
                                           Shape shape,
                                           GradeImpacts impacts)
    {
        super(null, shape, impacts, connection.getMedian(), connection.getWidth());
    }

    /**
     * Creates a new {@code AbstractVerticalConnectorInter} object.
     *
     * @param shape  the assigned shape
     * @param grade  quality
     * @param median vertical segment
     * @param width  segment width
     */
    public AbstractVerticalConnectorInter (Shape shape,
                                           Double grade,
                                           Line2D median,
                                           double width)
    {
        super(null, shape, grade, median, width);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected AbstractVerticalConnectorInter ()
    {
        super(null, null, (Double) null, null, null);
    }

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    @Override
    public boolean upgradeOldStuff (List<Version> upgrades)
    {
        boolean upgraded = false;

        if (upgrades.contains(Versions.INTER_GEOMETRY)) {
            if (median != null) {
                // Height is decreased of halfLine on top and bottom sides
                final double halfLine = sig.getSystem().getSheet().getScale().getFore() / 2.0;
                median.setLine(median.getX1() + 0.5, median.getY1() + halfLine,
                               median.getX2() + 0.5, median.getY2() + 1 - halfLine);
                computeArea();
                upgraded = true;
            }
        }

        return upgraded;
    }
}
