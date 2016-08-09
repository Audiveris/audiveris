//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S e g m e n t I n f o                                     //
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
package omr.sheet.curve;

import java.awt.Point;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code SegmentInfo} gathers physical description of a segment.
 *
 * @author Hervé Bitteur
 */
public class SegmentInfo
        extends Curve
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SegmentInfo object.
     *
     * @param id            curve id
     * @param firstJunction first junction point, if any
     * @param lastJunction  second junction point, if any
     * @param points        sequence of defining points
     * @param model         underlying model, if any
     * @param parts         set of arcs used for this curve
     */
    public SegmentInfo (int id,
                        Point firstJunction,
                        Point lastJunction,
                        List<Point> points,
                        Model model,
                        Collection<Arc> parts)
    {
        super(id, firstJunction, lastJunction, points, model, parts);
    }
}
