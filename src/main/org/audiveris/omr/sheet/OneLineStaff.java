//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     O n e L i n e S t a f f                                    //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.note.NotePosition;

import java.awt.geom.Point2D;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code OneLineStaff} is a one-line staff dedicated to percussion instrument.
 * <p>
 * Note that percussion can also be found using standard 5-line staves.
 * <p>
 * Although this kind of staff has just one physical staff line, its area used for barlines, header,
 * symbols, etc is defined as if it had the standard 5 lines.
 * Hence, its methods {@link #getFirstLine()} and {@link #getLastLine()} are redefined to return
 * "virtual" lines located 2 interlines above and below the real physical line.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "one-line-staff")
public class OneLineStaff
        extends Staff
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a {@code OneLineStaff} object
     *
     * @param id                the id of the staff
     * @param left              abscissa of the left side
     * @param right             abscissa of the right side
     * @param specificInterline the interline of other standard staves
     * @param lines             just one line in this list
     */
    public OneLineStaff (int id,
                         double left,
                         double right,
                         int specificInterline,
                         List<LineInfo> lines)
    {
        super(id, left, right, specificInterline, lines);
    }

    /**
     * No-arg constructor needed for JAXB.
     */
    public OneLineStaff ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public boolean isOneLineStaff ()
    {
        return true;
    }

    //--------------//
    // getFirstLine //
    //--------------//
    /**
     * Report the virtual line at top of OneLineStaff.
     *
     * @return the (virtual) first line
     */
    @Override
    public LineInfo getFirstLine ()
    {
        return lines.get(0).yTranslated(-2 * getSpecificInterline());
    }

    //-------------//
    // getLastLine //
    //-------------//
    /**
     * Report the virtual line at bottom of OneLineStaff.
     *
     * @return the (virtual) last line
     */
    @Override
    public LineInfo getLastLine ()
    {
        return lines.get(0).yTranslated(2 * getSpecificInterline());
    }

    //-----------------//
    // getNotePosition //
    //-----------------//
    @Override
    public NotePosition getNotePosition (Point2D point)
    {
        return null; // No notion of pitch for a one-line staff
    }

    //-----------------//
    // pitchPositionOf //
    //-----------------//
    @Override
    public double pitchPositionOf (Point2D pt)
    {
        // We compute with respect to the one line
        // (Used to disambiguate whole vs half rests)
        double middle = getMidLine().yAt(pt.getX());

        return (2 * (pt.getY() - middle)) / getSpecificInterline();
    }
}
