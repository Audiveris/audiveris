//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           G r a d e s                                          //
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
package omr.sig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Grades} gathers utility methods dealing with grade values.
 *
 * @author Hervé Bitteur
 */
public class Grades
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Grades.class);

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // clamp //
    //-------//
    /**
     * Constraint the provided value to lie within [0..1] range.
     *
     * @param value the value to check
     * @return the adjusted value
     */
    public static double clamp (double value)
    {
        if (value < 0) {
            value = 0;
        }

        if (value > 1) {
            value = 1;
        }

        return value;
    }

    //------------//
    // contextual //
    //------------//
    /**
     * General computation of contextual probability for an inter when supported by an
     * array of partners.
     *
     * @param inter    the (intrinsic grade of the) inter
     * @param partners the array of (intrinsic grades of the) supporting partners
     * @param ratios   the array of ratios of supporting partners, parallel to partners array
     * @return the resulting contextual probability for inter
     */
    public static double contextual (double inter,
                                     double[] partners,
                                     double[] ratios)
    {
        assert ratios != null : "Null ratios array";
        assert partners != null : "Null sources array";
        assert ratios.length == partners.length : "Arrays of different lengths";

        double contrib = 0;

        for (int i = 0; i < partners.length; i++) {
            contrib += (partners[i] * (ratios[i] - 1));
        }

        double total = support(inter, 1 + contrib);

        return total;
    }

    //---------//
    // support //
    //---------//
    /**
     * Compute the actual support brought on a interpretation with 'inter' grade
     *
     * @param inter the intrinsic grade of inter
     * @param ratio the supporting ratio of a partner
     * @return the resulting support
     */
    private static double support (double inter,
                                   double ratio)
    {
        double support = (ratio * inter) / (1 + ((ratio - 1) * inter));

        //        System.out.printf(
        //                "target:%.2f ratio:%.2f -> support:%.2f%n",
        //                target,
        //                ratio,
        //                support);
        return support;
    }

    //    private static void dump (boolean[] vector)
    //    {
    //        for (boolean b : vector) {
    //            System.out.print(b ? "T" : "F");
    //        }
    //
    //        System.out.println();
    //    }
}
