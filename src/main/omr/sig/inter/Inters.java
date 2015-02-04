//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           I n t e r s                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

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
     * For comparing inter collections by decreasing mean grade.
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
     * For comparing inter collections by decreasing mean contextual grade.
     */
    public static final Comparator<Collection<Inter>> byReverseMeanContextualGrade = new Comparator<Collection<Inter>>()
    {
        @Override
        public int compare (Collection<Inter> c1,
                            Collection<Inter> c2)
        {
            return Double.compare(getMeanContextualGrade(c2), getMeanContextualGrade(c1));
        }
    };

    //~ Methods ------------------------------------------------------------------------------------
    //------------------------//
    // getMeanContextualGrade //
    //------------------------//
    public static double getMeanContextualGrade (Collection<Inter> col)
    {
        if (col.isEmpty()) {
            throw new IllegalArgumentException("Provided collection is empty");
        }

        double sum = 0;

        for (Inter inter : col) {
            Double cg = inter.getContextualGrade();

            if (cg != null) {
                sum += cg;
            } else {
                sum += inter.getGrade();
            }
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
