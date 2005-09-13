//-----------------------------------------------------------------------//
//                                                                       //
//                          C h e c k S u i t e                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.check;

import omr.util.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class <code>CheckSuite</code> represents a suite of homogeneous checks,
 * that is checks working on the same type. Every check in the suite is
 * assigned a weight, to represent its relative importance in the suite.
 *
 * @param <T> the subtype of Checkable-compatible objects used in the
 * homogeneous collection of checks in this suite
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class CheckSuite <T extends Checkable>
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(CheckSuite.class);
    private static final String RED_COLOR = "#FF0000";
    private static final String ORANGE_COLOR = "#FFAA00";
    private static final String GREEN_COLOR = "#00FF00";

    //~ Instance variables ------------------------------------------------

    /** Name of this suite */
    protected String name;

    /** Minimum threshold for final grade */
    protected double threshold;

    // List of checks in the suite
    private final List<Check<T>> checks = new ArrayList<Check<T>>();

    // List of related weights in the suite
    private final List<Double> weights = new ArrayList<Double>();

    // Total checks weight
    private double totalWeight = 0.0d;

    //~ Constructors ------------------------------------------------------

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

    //~ Methods -----------------------------------------------------------

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
                     Check<T> check)
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
     */
    public CheckSuite addAll (CheckSuite<T> suite)
    {
        int index = 0;
        for (Check<T> check : suite.checks) {
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

        System.out.println("Weight    Name             Covariant    Low       High");
        System.out.println("------    ----                ------    ---       ----");

        int index = 0;
        for (Check check : checks) {
            //   %[argument_index$][flags][width][.precision]conversion
            System.out.printf("%4.1f      %-19s  %5b  % 6.2f    % 6.2f \n",
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
     * @return the computed grade.
     */
    public double pass (T object)
    {
        double grade = 0.0d;
        CheckResult result = new CheckResult();
        StringBuffer sb = null;

        if (logger.isDebugEnabled()) {
            sb = new StringBuffer(512);
            sb.append(name).append(" ");
        }

        int index = 0;
        for (Check<T> check : checks) {
            check.pass(object, result, true);

            if (logger.isDebugEnabled()) {
                sb.append(String.format("%15s :%5.2f", check.getName(), result.value));
            }

            if (result.flag == Check.RED) {
                // The check totally failed, we give up immediately!
                if (logger.isDebugEnabled()) {
                    logger.debug(sb.toString());
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

        if (logger.isDebugEnabled()) {
            sb.append(String.format("=> %5.2f ", grade));
            logger.debug(sb.toString());
        }

        return grade;
    }

    //----------------//
    // passCollection //
    //----------------//
    /**
     * Pass the whole collection of suites in a row and return the global
     * result
     *
     * @param object the object to be checked
     * @param suites the collection of check suites to pass
     *
     * @return the global result
     */
    public static <T extends Checkable>
            double passCollection (T object,
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

    //----------//
    // passHtml //
    //----------//
    /**
     * Pass all the test in the suite, even over totally failed ones, and
     * return detailed result in html
     *
     * @param prolog a potential html prolog (such as head), null otherwise
     * @param object the object to check
     *
     * @return the resulting html stream
     */
    public String passHtml (String prolog,
                            T      object)
    {
        CheckResult result = new CheckResult();
        double grade = 0.0d;
        boolean failed = false;

        StringBuffer sb = new StringBuffer(4096);
        if (prolog != null) {
            sb.append(prolog);
        } else {
            // Head Style
            sb.append("<head>");
            sb.append("<style type=\"text/css\">");
            sb.append("BODY {margin: 0; padding: 0;font-family: sans-serif}");
            sb.append("TH {background-color: #DDDDDD; font-size: 11pt}");
            sb.append("TD {font-size: 11pt}");
            sb.append("</style>");
            sb.append("</head>");
        }

        //sb.append("<body>");
        sb.append("<table border=0 cellspacing=1 cellpadding=0 width=280>");

        // First line: Titles
        sb.append("<tr>");
        sb.append("<th>W</th><th>Name</th><th>X</th><th>L</th><th>L</th><th>X</th><th>Result</th>");
        sb.append("</tr>");

        // One line per check
        int index = 0;
        for (Check<T> check : checks) {
            Double weight = weights.get(index++);
            sb.append("<tr>");
            // Weight
            sb.append("<td>").append(weight.intValue()).append("</td>");
            // Name
            sb.append("<td>").append(check.getName()).append("</td>");
            // Lower range ?
            sb.append("<td>");
            if (!check.isCovariant()) {
                sb.append("X");
            }
            sb.append("</td>");
            // Low limit
            sb.append("<td>");
            sb.append(String.format("%.2f", check.getLow()));
            sb.append("</td>");
            // High Limit
            sb.append("<td>");
            sb.append(String.format("%.2f", check.getHigh()));
            sb.append("</td>");
            // Higher range ?
            sb.append("<td>");
            if (check.isCovariant()) {
                sb.append("X");
            }
            sb.append("</td>");
            // Result
            check.pass(object, result, false);
            sb.append("<td align=right>").append("<font color=\"");
            if (result.flag == Check.RED) {
                failed = true;
                sb.append(RED_COLOR);
            } else {
                // Aggregate results
                grade += result.flag * weight;
                if (result.flag == Check.ORANGE) {
                    sb.append(ORANGE_COLOR);
                } else {
                    sb.append(GREEN_COLOR);
                }
            }
            sb.append("\">");
            sb.append(String.format("%5.2f", result.value));
            sb.append("</font></td></tr>");
        }

        // Global result
        sb.append("<tr>");
        sb.append("<td></td><td></td><td></td><td></td><td></td><td></td><td ");
        sb.append(" align=right");
        sb.append(" bgcolor=\"");
        if (failed) {
            sb.append(RED_COLOR).append("\">");
            sb.append(Check.RED);
        } else {
            grade /= totalWeight;
            if (grade >= threshold) {
                sb.append(GREEN_COLOR);
            } else {
                sb.append(ORANGE_COLOR);
            }
            sb.append("\">");
            sb.append(String.format("%3.1f", grade));
        }
        sb.append("</td>");
        sb.append("</tr>");
        sb.append("</table>");

        if (logger.isDebugEnabled()) {
            logger.debug(sb.toString());
        }

        return sb.toString();
    }
}
