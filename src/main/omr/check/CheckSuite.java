//----------------------------------------------------------------------------//
//                                                                            //
//                            C h e c k S u i t e                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright � Herv� Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.check;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code CheckSuite} represents a suite of homogeneous checks,
 * meaning that all checks in the suite work on the same object type.
 * <p>
 * A check suite is typically applied on a candidate to evaluate the
 * <b>intrinsic</b> quality (grade) of this candidate that depends on the
 * candidate alone.
 * This intrinsic grade can be complemented by <b>contextual</b> grade that
 * takes into account potential supporting entities nearby.
 * To leave room for contextual increment, the intrinsic grade is applied a
 * standard reduction ratio.
 * <p>
 * Every check in the suite is assigned a <b>weight</b>, to represent its
 * relative importance in the computation of the final grade value.
 * A weight value of <b>zero</b> implements a pure <b>constraint</b> check that
 * has no role in the precise computation of the final grade value, except that
 * it can detect that a constraint is not matched and thus make the whole check
 * suite fail.
 *
 * @param <C> the subtype of Checkable objects used in the
 *            homogeneous collection of checks in this suite
 *
 * @author Hervé Bitteur
 */
public class CheckSuite<C extends Checkable>
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            CheckSuite.class);

    //~ Instance fields --------------------------------------------------------
    /** Name of this suite. */
    protected final String name;

    /** Minimum threshold for final grade. */
    protected final double threshold;

    /** List of checks in the suite. */
    private final List<Check<C>> checks = new ArrayList<Check<C>>();

    /** Parallel list of related weights. */
    private final List<Double> weights = new ArrayList<Double>();

    /** Total checks weight. */
    private double totalWeight = 0.0d;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // CheckSuite //
    //------------//
    /**
     * Create a suite of checks
     *
     * @param name      the name for the suite (for debug)
     * @param threshold the threshold to test results
     */
    public CheckSuite (String name,
                       double threshold)
    {
        this.name = name;
        this.threshold = threshold;
    }

    //~ Methods ----------------------------------------------------------------
    //-----//
    // add //
    //-----//
    /**
     * Add a check to the suite, with its assigned weight
     *
     * @param weight the weight of this check in the suite
     * @param check  the check to add to the suite
     */
    public void add (double weight,
                     Check<C> check)
    {
        checks.add(check);
        weights.add(weight);
        totalWeight += weight;
    }

    //--------//
    // addAll //
    //--------//
    /**
     * Add all checks of another suite
     *
     * @param suite the suite of checks to be appended
     * @return the suite with checks appended, for easy chaining
     */
    public CheckSuite<C> addAll (CheckSuite<C> suite)
    {
        int index = 0;

        for (Check<C> check : suite.checks) {
            double weight = suite.weights.get(index++);
            add(weight, check);
        }

        // Allow chaining
        return this;
    }

    //------//
    // dump //
    //------//
    /**
     * Dump a readable description of all checks of this suite.
     */
    public void dump ()
    {
        StringBuilder sb = new StringBuilder(String.format("%n"));

        if (name != null) {
            sb.append(name);
        }

        sb.append(String.format(" Check Suite: threshold=%f%n", threshold));

        dumpSpecific(sb);

        sb.append(
                String.format(
                        "Weight    Name             Covariant    Low       High%n"));
        sb.append(
                String.format(
                        "------    ----                ------    ---       ----%n"));

        int index = 0;

        for (Check<C> check : checks) {
            sb.append(
                    String.format(
                            "%4.1f      %-19s  %5b  % 6.2f    % 6.2f %n",
                            weights.get(index++),
                            check.getName(),
                            check.isCovariant(),
                            check.getLow(),
                            check.getHigh()));
        }

        logger.info(sb.toString());
    }

    //-----------//
    // getChecks //
    //-----------//
    /**
     * Report the collection of checks that compose this suite.
     *
     * @return the collection of checks
     */
    public List<Check<C>> getChecks ()
    {
        return checks;
    }

    //--------------------//
    // getDefaultMinGrade //
    //--------------------//
    public static double getDefaultMinGrade ()
    {
        return constants.defaultMinGrade.getValue();
    }

    //------------//
    // getImpacts //
    //------------//
    /**
     * Pass the suite of checks and store details in the returned
     * Impacts structure.
     *
     * @param checkable the object to be checked
     * @return the detailed impacts
     */
    public SuiteImpacts getImpacts (C checkable)
    {
        final SuiteImpacts<C> impacts = new SuiteImpacts<C>(this, checkable);
        pass(checkable, impacts);

        return impacts;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name of this suite.
     *
     * @return suite name
     */
    public String getName ()
    {
        return name;
    }

    //--------------//
    // getThreshold //
    //--------------//
    /**
     * Report the assigned threshold.
     *
     * @return the assigned minimum result
     */
    public double getThreshold ()
    {
        return threshold;
    }

    //----------------//
    // getTotalWeight //
    //----------------//
    /**
     * Report the sum of all individual checks.
     *
     * @return the total weight of the checks in the suite
     */
    public double getTotalWeight ()
    {
        return totalWeight;
    }

    //------------//
    // getWeights //
    //------------//
    /**
     * Report the weights of the checks.
     * (collection parallel to the suite checks)
     *
     * @return the collection of checks weights
     */
    public List<Double> getWeights ()
    {
        return weights;
    }

    //------//
    // pass //
    //------//
    /**
     * Pass sequentially the checks in the suite, stopping at the first
     * test with red result.
     *
     * @param checkable the object to be checked
     * @param impacts   the suite impacts if any, to record detailed results
     * @return the computed grade.
     */
    public double pass (C checkable,
                        SuiteImpacts impacts)
    {
        final CheckResult result = new CheckResult();
        double grade = 1d;
        int index = 0;

        for (Check<C> check : checks) {
            check.pass(checkable, result, true);

            if (impacts != null) {
                impacts.setValue(index, result.value);
            }

            // Aggregate results
            double weight = weights.get(index);

            if (weight != 0) {
                if (impacts != null) {
                    impacts.setImpact(index, result.grade);
                }

                grade *= Math.pow(result.grade, weight);
            }

            index++;
        }

        // Final grade
        grade = Math.pow(grade, 1 / totalWeight) * constants.intrinsicRatio.getValue();

        if (impacts != null) {
            impacts.setGrade(grade);
        }

        return grade;
    }

    //--------------//
    // dumpSpecific //
    //--------------//
    /**
     * Just an empty placeholder, meant to be overridden.
     *
     * @param sb StringBuilder to populate
     */
    protected void dumpSpecific (StringBuilder sb)
    {
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    /**
     * General constants for CheckSuite.
     */
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio intrinsicRatio = new Constant.Ratio(
                0.8,
                "Reduction ratio applied on any intrinsic grade");

        Constant.Ratio defaultMinGrade = new Constant.Ratio(
                0.5,
                "Default minimum acceptable suite result");

    }
}
