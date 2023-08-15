//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S u i t e I m p a c t s                                    //
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
package org.audiveris.omr.check;

import org.audiveris.omr.sig.GradeImpacts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Class <code>SuiteImpacts</code> is a GradeImpacts implementation based on an underlying
 * CheckSuite.
 *
 * @author Hervé Bitteur
 */
public class SuiteImpacts
        extends GradeImpacts
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SuiteImpacts.class);

    //~ Instance fields ----------------------------------------------------------------------------

    private final String suiteName;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SuiteImpacts object.
     *
     * @param names     array of names
     * @param weights   array of weights
     * @param suiteName name for the suite
     */
    protected SuiteImpacts (String[] names,
                            double[] weights,
                            String suiteName)
    {
        super(names, weights);
        this.suiteName = suiteName;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------//
    // getDump //
    //---------//
    @Override
    public String getDump ()
    {
        //        final List<Check<C>> checks = suite.getChecks();
        //        final StringBuilder sb = new StringBuilder();
        //        sb.append(suite.getName()).append(" ").append(checkable);
        //
        //        for (int i = 0; i < checks.size(); i++) {
        //            Check<C> check = checks.get(i);
        //            sb.append(" ").append(check.getName()).append(":").append(
        //                    String.format("%.2f", values[i]));
        //        }
        //
        //        sb.append(
        //                String.format(
        //                        " => %.2f (min %.2f, good %.2f)",
        //                        grade,
        //                        suite.getMinThreshold(),
        //                        suite.getGoodThreshold()));
        //
        //        return sb.toString();
        return suiteName;
    }

    //----------//
    // setGrade //
    //----------//
    /**
     * Set the suite grade value
     *
     * @param grade suite grade value
     */
    public void setGrade (double grade)
    {
        this.grade = grade;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // newInstance //
    //-------------//
    /**
     * Create a SuiteImpacts instance from a check suite and a checkable entity.
     *
     * @param <C>       Precise Checkable subtype
     * @param suite     the check suite to run on entity
     * @param checkable the checkable entity
     * @return the populated SuiteImpacts instance
     */
    public static <C> SuiteImpacts newInstance (CheckSuite<C> suite,
                                                C checkable)
    {
        final List<Check<C>> checks = suite.getChecks();
        final int nb = checks.size();

        // Build names & weights
        String[] names = new String[nb];
        double[] weights = new double[nb];

        for (int i = 0; i < nb; i++) {
            Check check = checks.get(i);
            names[i] = check.getName();
            weights[i] = suite.getWeights().get(i);
        }

        SuiteImpacts impacts = new SuiteImpacts(names, weights, suite.getName());

        try {
            // Populate impacts with check results on checkable entity
            suite.pass(checkable, impacts);

            return impacts;
        } catch (Exception ex) {
            logger.warn("Error computing SuiteImpacts on " + checkable, ex);

            return null;
        }
    }
}
