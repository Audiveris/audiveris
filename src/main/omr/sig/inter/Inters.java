//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           I n t e r s                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import java.awt.Point;
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
