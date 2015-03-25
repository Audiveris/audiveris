//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S u i t e I m p a c t s                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.check;

import omr.sig.BasicImpacts;

import java.util.List;

/**
 * Class {@code SuiteImpacts} is a GradeImpacts implementation based on an underlying
 * CheckSuite.
 *
 * @author Hervé Bitteur
 */
public class SuiteImpacts
        extends BasicImpacts
{
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
    public static <C extends Checkable> SuiteImpacts newInstance (CheckSuite<C> suite,
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

        // Populate impacts with check results on checkable entity
        suite.pass(checkable, impacts);

        return impacts;
    }

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
    public void setGrade (double grade)
    {
        this.grade = grade;
    }
}
