//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S e g m e n t I n f o                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.curve;

import java.awt.Point;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code SegmentInfo}
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
