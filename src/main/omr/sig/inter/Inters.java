//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           I n t e r s                                          //
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
package omr.sig.inter;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Comparator;

/**
 * Class {@code Inters} gathers utilities on inter instances.
 *
 * @author Hervé Bitteur
 */
public abstract class Inters
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /**
     * For comparing inter instances by decreasing mean grade.
     */
    public static final Comparator<Collection<Inter>> byReverseMeanGrade = new Comparator<Collection<Inter>>()
    {
        @Override
        public int compare (Collection<Inter> c1,
                            Collection<Inter> c2)
        {
            return Double.compare(getMeanGrade(c2), getMeanGrade(c1));
        }
    };

    /**
     * For comparing inter instances by decreasing mean contextual grade.
     */
    public static final Comparator<Collection<Inter>> byReverseMeanContextualGrade = new Comparator<Collection<Inter>>()
    {
        @Override
        public int compare (Collection<Inter> c1,
                            Collection<Inter> c2)
        {
            return Double.compare(getMeanBestGrade(c2), getMeanBestGrade(c1));
        }
    };

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // getBounds //
    //-----------//
    /**
     * Return the bounding box of a collection of Inter instances.
     *
     * @param inters the provided collection of inter instances
     * @return the bounding contour
     */
    public static Rectangle getBounds (Collection<? extends Inter> inters)
    {
        Rectangle box = null;

        for (Inter inter : inters) {
            if (box == null) {
                box = new Rectangle(inter.getBounds());
            } else {
                box.add(inter.getBounds());
            }
        }

        return box;
    }

    //-----------------//
    // getClosestChord //
    //-----------------//
    /**
     * From a provided Inter collection, report the one with the lowest euclidian
     * distance to the provided point.
     *
     * @param inters the collection of inters to browse
     * @param point  the provided point
     * @return the closest inter
     */
    public static Inter getClosestInter (Collection<? extends Inter> inters,
                                         Point2D point)
    {
        Inter bestInter = null;
        double bestDistSq = Double.MAX_VALUE;

        for (Inter inter : inters) {
            final Point center = inter.getCenter();
            final double dx = center.x - point.getX();
            final double dy = center.y - point.getY();
            final double distSq = (dx * dx) + (dy * dy);

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                bestInter = inter;
            }
        }

        return bestInter;
    }

    //------------------//
    // getMeanBestGrade //
    //------------------//
    public static double getMeanBestGrade (Collection<Inter> col)
    {
        if (col.isEmpty()) {
            throw new IllegalArgumentException("Provided collection is empty");
        }

        double sum = 0;

        for (Inter inter : col) {
            sum += inter.getBestGrade();
        }

        return sum / col.size();
    }

    //--------------//
    // getMeanGrade //
    //--------------//
    public static double getMeanGrade (Collection<Inter> col)
    {
        if (col.isEmpty()) {
            throw new IllegalArgumentException("Provided collection is empty");
        }

        double sum = 0;

        for (Inter inter : col) {
            sum += inter.getGrade();
        }

        return sum / col.size();
    }

    //-----//
    // ids //
    //-----//
    public static String ids (Collection<? extends Inter> inters)
    {
        if (inters == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");

        for (Inter inter : inters) {
            sb.append("#").append(inter.getId());
        }

        sb.append("]");

        return sb.toString();
    }
}
