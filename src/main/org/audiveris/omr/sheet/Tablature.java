//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T a b l a t u r e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.sheet.grid.LineInfo;

import java.awt.geom.Point2D;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>Tablature</code> is a staff dedicated to a fretted string instrument like
 * the guitar, where the lines represent the instrument strings, and numbers on a line
 * represent fret numbers.
 * <p>
 * While standard staff notation uses 5 lines, tablature notation uses 6 lines for a guitar
 * and 4 lines for a bass.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "tablature")
public class Tablature
        extends Staff
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor needed for JAXB.
     */
    public Tablature ()
    {
    }

    /**
     * Creates a new <code>Tablature</code> object.
     *
     * @param id                the id of the tablature
     * @param left              abscissa of the left side
     * @param right             abscissa of the right side
     * @param specificInterline specific interline detected for this tablature
     * @param lines             the sequence of contained tablature lines
     */
    public Tablature (int id,
                      double left,
                      double right,
                      int specificInterline,
                      List<LineInfo> lines)
    {
        super(id, left, right, specificInterline, lines);
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    public boolean isTablature ()
    {
        return true;
    }

    //-----------------//
    // pitchPositionOf //
    //-----------------//
    @Override
    public double pitchPositionOf (Point2D pt)
    {
        throw new IllegalStateException("A tablature has no notion of pitch");
    }

    //-----------------//
    // pitchToOrdinate //
    //-----------------//
    @Override
    public double pitchToOrdinate (double x,
                                   double pitch)
    {
        throw new IllegalStateException("A tablature has no notion of pitch");
    }
}
