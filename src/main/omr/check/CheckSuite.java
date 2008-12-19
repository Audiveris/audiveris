//----------------------------------------------------------------------------//
//                                                                            //
//                            C h e c k S u i t e                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.check;

import omr.log.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class <code>CheckSuite</code> represents a suite of homogeneous checks, that
 * is checks working on the same type. Every check in the suite is assigned a
 * weight, to represent its relative importance in the suite.
 *
 * @param <C> the subtype of Checkable-compatible objects used in the
 * homogeneous collection of checks in this suite
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class CheckSuite<C extends Checkable>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(CheckSuite.class);

    //~ Instance fields --------------------------------------------------------

    /** Name of this suite */
    protected String name;

    /** Minimum threshold for final grade */
    protected double threshold;

    /** List of checks in the suite */
    private final List<Check<C>> checks = new ArrayList<Check<C>>();

    /** List of related weights in the suite */
    private final List<Double> weights = new ArrayList<Double>();

    /** Total checks weight */
    private double totalWeight = 0.0d;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // CheckSuite //
    //------------//
    /**
     * Create a suite of checks
     *
     * @param name the name for the suite (for debug)
     * @param threshold the threshold to test results
     */
    public CheckSuite (String name,
                       double threshold)
    {
        this.name = name;
        this.threshold = threshold;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getChecks //
    //-----------//
    /**
     * Report the collection of checks that compose this suite
     *
     * @return the collection of checks
     */
    public List<Check<C>> getChecks ()
    {
        return checks;
    }

    //---------//
    // setName //
    //---------//
    /**
     * Assings a new name to the check suite
     *
     * @param name the new name
     */
    public void setName (String name)
    {
        this.name = name;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the name of this suite
     *
     * @return suite name
     */
    public String getName ()
    {
        return name;
    }

    //--------------//
    // setThreshold //
    //--------------//
    /**
     * Allows to assign a new threshold for the suite
     *
     * @param threshold the new minimum result
     */
    public void setThreshold (double threshold)
    {
        this.threshold = threshold;
    }

    //--------------//
    // getThreshold //
    //--------------//
    /**
     * Report the assigned threshold
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
     * Report the sum of all individual checks
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
     * Report the weights of the checks (collection parallel to the suite
     * checks)
     *
     * @return the collection of checks weights
     */
    public List<Double> getWeights ()
    {
        return weights;
    }

    //-----//
    // add //
    //-----//
    /**
     * Add a check to the suite, with its assigned weight
     *
     * @param weight the weight of this check in the suite
     * @param check  the check to add to the suite
     */
    public void add (double   weight,
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
    public CheckSuite addAll (CheckSuite<C> suite)
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
     * Dump a readable description of all checks that compose this suite
     */
    public void dump ()
    {
        System.out.println();

        if (name != null) {
            System.out.print(name);
        }

        System.out.println(" Check Suite: threshold=" + threshold);

        dumpSpecific();

        System.out.println(
            "Weight    Name             Covariant    Low       High");
        System.out.println(
            "------    ----                ------    ---       ----");

        int index = 0;

        for (Check check : checks) {
            System.out.printf(
                "%4.1f      %-19s  %5b  % 6.2f    % 6.2f \n",
                weights.get(index++),
                check.getName(),
                check.isCovariant(),
                check.getLow(),
                check.getHigh());
        }
    }

    //------//
    // pass //
    //------//
    /**
     * Pass sequentially the checks in the suite, stopping at the first test
     * with red result.
     *
     * @param object the object to be checked
     * @return the computed grade.
     */
    public double pass (C object)
    {
        double       grade = 0.0d;
        CheckResult  result = new CheckResult();
        StringBuffer sb = null;

        if (logger.isFineEnabled()) {
            sb = new StringBuffer(512);
            sb.append(name)
              .append(" ");
        }

        int index = 0;

        for (Check<C> check : checks) {
            check.pass(object, result, true);

            if (logger.isFineEnabled()) {
                sb.append(
                    String.format("%15s :%5.2f", check.getName(), result.value));
            }

            if (result.flag == Check.RED) {
                // The check totally failed, we give up immediately!
                if (logger.isFineEnabled()) {
                    logger.fine(sb.toString());
                }

                return result.flag;
            } else {
                // Aggregate results
                double weight = weights.get(index);
                grade += (result.flag * weight);
            }

            index++;
        }

        // Final grade
        grade /= totalWeight;

        if (logger.isFineEnabled()) {
            sb.append(String.format("=> %5.2f ", grade));
            logger.fine(sb.toString());
        }

        return grade;
    }

    //----------------//
    // passCollection //
    //----------------//
    /**
     * Pass the whole collection of suites in a row and return the global result
     *
     * @param <T> The specific type of checked object
     * @param object the object to be checked
     * @param suites the collection of check suites to pass
     *
     * @return the global result
     */
    public static <T extends Checkable> double passCollection (T                         object,
                                                               Collection<CheckSuite<T>> suites)
    {
        double totalWeight = 0.0d;
        double grade = 0.0d;

        for (CheckSuite<T> suite : suites) {
            double res = suite.pass(object);

            // If one totally failed, give up immediately
            if (res == Check.RED) {
                return res;
            } else {
                // Aggregate results
                double weight = suite.getTotalWeight();
                totalWeight += weight;
                grade += (res * weight);
            }
        }

        // Final grade
        return grade / totalWeight;
    }

    //--------------//
    // dumpSpecific //
    //--------------//
    /**
     * Just an empty placeholder, meant to be overridden
     */
    protected void dumpSpecific ()
    {
    }
}
