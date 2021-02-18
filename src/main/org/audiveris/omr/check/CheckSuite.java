//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      C h e c k S u i t e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import org.audiveris.omr.glyph.Grades;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code CheckSuite} represents a suite of homogeneous checks, meaning that all
 * checks in the suite work on the same object type.
 * <p>
 * Every check in the suite is assigned a <b>weight</b>, to represent its relative importance in
 * the computation of the final grade value.
 * <p>
 * There are 2 specific weight values:
 * <ul>
 * <li>A weight value of <b>0</b> implements a pure <b>constraint</b> check that has no role in
 * the precise computation of the final grade value, except that it can detect that a constraint is
 * not matched and thus make the whole check suite fail.
 * <li>A weight value of <b>-1</b> tells that the check will be run, but with no impact at all on
 * the final grade value.
 * This is useful when running the check has side effects needed by some following checks.
 * </ul>
 * <p>
 * A check suite is typically applied on a candidate to evaluate the <b>intrinsic</b> quality
 * (grade) of this candidate that depends on the candidate alone.
 * This intrinsic grade can be complemented by <b>contextual</b> grade that takes into account
 * potential supporting entities nearby.
 * To leave room for contextual increment, the intrinsic grade is applied a standard reduction ratio
 * (0.8 as of this writing).
 *
 * @param <C> the subtype of Checkable used in the homogeneous collection of checks in the suite.
 *
 * @author Hervé Bitteur
 */
public class CheckSuite<C>
{

    private static final Logger logger = LoggerFactory.getLogger(CheckSuite.class);

    /** Name of this suite. */
    protected final String name;

    /** Minimum threshold for final grade. */
    protected final double minThreshold;

    /** Good threshold for final grade. */
    protected final double goodThreshold;

    /** List of checks in the suite. */
    private final List<Check<C>> checks = new ArrayList<>();

    /** Parallel list of related weights. */
    private final List<Double> weights = new ArrayList<>();

    /** Total checks weight. */
    private double totalWeight = 0.0;

    /**
     * Create a suite of checks with standard threshold values.
     *
     * @param name the name for the suite (for debug)
     */
    public CheckSuite (String name)
    {
        this(name, Grades.minInterGrade, Grades.goodInterGrade);
    }

    //------------//
    // CheckSuite //
    //------------//
    /**
     * Create a suite of checks with specific thresholds.
     *
     * @param name          the name for the suite (for debug)
     * @param minThreshold  the threshold for acceptable results
     * @param goodThreshold the threshold for good results
     */
    public CheckSuite (String name,
                       double minThreshold,
                       double goodThreshold)
    {
        this.name = name;
        this.minThreshold = minThreshold;
        this.goodThreshold = goodThreshold;
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

        if (weight >= 0) {
            totalWeight += weight;
        }
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

        sb.append(String.format(" Check Suite: min=%f good=%f%n", minThreshold, goodThreshold));

        dumpSpecific(sb);

        sb.append(String.format("Weight    Name             Covariant    Low       High%n"));
        sb.append(String.format("------    ----                ------    ---       ----%n"));

        int index = 0;

        for (Check<C> check : checks) {
            sb.append(
                    String.format(
                            "%4.1f      %-19s  %5b  % 6.2f    % 6.2f%n",
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

    //------------------//
    // getGoodThreshold //
    //------------------//
    /**
     * Report the assigned good threshold.
     *
     * @return the assigned good result
     */
    public double getGoodThreshold ()
    {
        return goodThreshold;
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
        return SuiteImpacts.newInstance(this, checkable);
    }

    //-----------------//
    // getMinThreshold //
    //-----------------//
    /**
     * Report the assigned minimum threshold.
     *
     * @return the assigned minimum result
     */
    public double getMinThreshold ()
    {
        return minThreshold;
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
            check.pass(checkable, result);

            if (impacts != null) {
                impacts.setImpact(index, result.grade);
            }

            // Aggregate results
            final double weight = weights.get(index);

            if (weight >= 0) {
                if (result.grade == 0) {
                    grade = 0;
                } else if (weight != 0) {
                    grade *= Math.pow(result.grade, weight);
                }
            }

            index++;
        }

        // Final grade
        grade = Math.pow(grade, 1 / totalWeight) * Grades.intrinsicRatio;

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
}
