//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        G r a d e U t i l                                       //
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

import java.util.Objects;

/**
 * Class {@code GradeUtil} gathers utility methods dealing with grade computation.
 *
 * @author Hervé Bitteur
 */
public class GradeUtil
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GradeUtil.class);

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
     * Compute contextual grade, knowing inter grade and, for each partner,
     * its intrinsic grade and the ratio brought by supporting relation.
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
        Objects.requireNonNull(ratios, "Null ratios array");
        Objects.requireNonNull(partners, "Null sources array");

        if (ratios.length != partners.length) {
            throw new IllegalArgumentException("Arrays of different lengths");
        }

        double contribution = 0;

        for (int i = 0; i < partners.length; i++) {
            contribution += contributionOf(partners[i], ratios[i]);
        }

        return contextual(inter, contribution);
    }

    //------------//
    // contextual //
    //------------//
    /**
     * Compute contextual grade, knowing inter grade and contribution of each partner.
     *
     * @param inter             intrinsic grade of inter
     * @param contributionArray contribution of each partner
     * @return contextual grade for inter
     */
    public static double contextual (double inter,
                                     double[] contributionArray)
    {
        if (contributionArray == null) {
            return inter;
        }

        // Sum contribution on all partners
        double contribution = 0;

        for (int i = 0; i < contributionArray.length; i++) {
            contribution += contributionArray[i];
        }

        return contextual(inter, contribution);
    }

    //------------//
    // contextual //
    //------------//
    /**
     * Compute contextual grade, knowing inter grade and total contribution of partners.
     *
     * @param inter        intrinsic grade of inter
     * @param contribution total contribution of partners
     * @return contextual grade for inter
     */
    public static double contextual (double inter,
                                     double contribution)
    {
        return ((1 + contribution) * inter) / (1 + (contribution * inter));
    }

    //----------------//
    // contributionOf //
    //----------------//
    /**
     * Compute a contribution, knowing partner intrinsic grade and supporting ratio.
     *
     * @param partner partner intrinsic grade
     * @param ratio   supporting ratio from partner
     * @return the partner contribution
     */
    public static double contributionOf (double partner,
                                         double ratio)
    {
        return partner * (ratio - 1);
    }
}
