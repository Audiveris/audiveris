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

import omr.sig.GradeImpacts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code CheckSuite} represents a suite of homogeneous checks,
 * meaning that all checks in the suite work on the same object type.
 *
 * Every check in the suite is assigned a weight, to represent its relative
 * importance in the suite.
 *
 * @param <C> the subtype of Checkable objects used in the
 *            homogeneous collection of checks in this suite
 *
 * @author Hervé Bitteur
 */
public class CheckSuite<C extends Checkable>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            CheckSuite.class);

    //~ Instance fields --------------------------------------------------------
    /** Name of this suite. */
    protected String name;

    /** Minimum threshold for final grade. */
    protected double threshold;

    /** List of checks in the suite. */
    private final List<Check<C>> checks = new ArrayList<Check<C>>();

    /** List of related weights in the suite. */
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
    //----------------//
    // passCollection //
    //----------------//
    /**
     * Pass the whole collection of suites in a row and return
     * the global result.
     *
     * @param <C>       The specific type of checked object
     * @param checkable the object to be checked
     * @param suites    the collection of check suites to pass
     *
     * @return the global result
     */
    public static <C extends Checkable> double passCollection (C checkable,
                                                               Collection<CheckSuite<C>> suites)
    {
        double totalWeight = 0.0d;
        double grade = 0.0d;

        for (CheckSuite<C> suite : suites) {
            double res = suite.pass(checkable, null);

            // If one totally failed, give up immediately
            if (res == -1) {
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
    public Impacts getImpacts (C checkable)
    {
        final Impacts<C> impacts = new Impacts<C>(this, checkable);
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
                        Impacts impacts)
    {
        final boolean debug = logger.isDebugEnabled() || checkable.isVip();
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
                    impacts.setDetail(index, result.grade);
                }

                grade *= Math.pow(result.grade, weight);
            }

            index++;
        }

        // Final grade
        grade = 0.8 * Math.pow(grade, 1 / totalWeight); // BINGO 0.8 factor

        if (impacts != null) {
            impacts.setGrade(grade);
        }

        return grade;
    }

    //---------//
    // setName //
    //---------//
    /**
     * Assign a new name to the check suite.
     *
     * @param name the new name
     */
    public void setName (String name)
    {
        this.name = name;
    }

    //--------------//
    // setThreshold //
    //--------------//
    /**
     * Allows to assign a new threshold for the suite.
     *
     * @param threshold the new minimum result
     */
    public void setThreshold (double threshold)
    {
        this.threshold = threshold;
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
    //---------//
    // Impacts //
    //---------//
    /**
     * A GradeImpacts implementation based on a CheckSuite.
     *
     * @param <C> precise Checkable type
     */
    public static class Impacts<C extends Checkable>
            implements GradeImpacts
    {
        //~ Instance fields ----------------------------------------------------

        /** The underlying suite of check instances. */
        private final CheckSuite<C> suite;

        /** The checked object. */
        private final Checkable checkable;

        /** Individual check values. */
        private final double[] values;

        /** Individual check details. */
        private final double[] details;

        /** Resulting suite grade. */
        private double grade;

        //~ Constructors -------------------------------------------------------
        public Impacts (CheckSuite<C> suite,
                        C checkable)
        {
            this.suite = suite;
            this.checkable = checkable;

            final int size = suite.getChecks()
                    .size();
            values = new double[size];
            details = new double[size];
        }

        //~ Methods ------------------------------------------------------------
        public String getDump ()
        {
            final List<Check<C>> checks = suite.getChecks();
            final List<Double> weights = suite.getWeights();

            final StringBuilder sb = new StringBuilder();
            sb.append(suite.getName())
                    .append(" ")
                    .append(checkable);

            for (int i = 0; i < checks.size(); i++) {
                Check<C> check = checks.get(i);
                sb.append(" ")
                        .append(check.getName())
                        .append(":")
                        .append(values[i]);
            }

            sb.append(String.format(" => %.2f", grade));

            return sb.toString();
        }

        @Override
        public double getGrade ()
        {
            return grade;
        }

        public double getValue (int index)
        {
            return values[index];
        }

        public void setDetail (int index,
                               double detail)
        {
            details[index] = detail;
        }

        public void setGrade (double grade)
        {
            this.grade = grade;
        }

        public void setValue (int index,
                              double value)
        {
            values[index] = value;
        }

        @Override
        public String toString ()
        {
            final StringBuilder sb = new StringBuilder();
            final List<Check<C>> checks = suite.getChecks();
            final List<Double> weights = suite.getWeights();

            for (int i = 0; i < checks.size(); i++) {
                double weight = weights.get(i);

                if (weight != 0) {
                    Check<C> check = checks.get(i);

                    if (sb.length() > 0) {
                        sb.append(" ");
                    }

                    sb.append(
                            String.format("%s:%.2f", check.getName(), details[i]));
                }
            }

            return sb.toString();
        }
    }
}
