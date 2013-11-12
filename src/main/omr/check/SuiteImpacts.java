//----------------------------------------------------------------------------//
//                                                                            //
//                           S u i t e I m p a c t s                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.check;

import omr.sig.GradeImpacts;

import java.util.List;

/**
 * Class {@code SuiteImpacts} is a GradeImpacts implementation based
 * on an underlying CheckSuite.
 *
 * @param <C> precise Checkable type
 *
 * @author Hervé Bitteur
 */
public class SuiteImpacts<C extends Checkable>
        implements GradeImpacts
{
    //~ Instance fields --------------------------------------------------------

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

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // SuiteImpacts //
    //--------------//
    /**
     * Creates a new SuiteImpacts object.
     *
     * @param suite     DOCUMENT ME!
     * @param checkable DOCUMENT ME!
     */
    public SuiteImpacts (CheckSuite<C> suite,
                         C checkable)
    {
        this.suite = suite;
        this.checkable = checkable;

        final int size = suite.getChecks()
                .size();
        values = new double[size];
        details = new double[size];
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // getDump //
    //---------//
    public String getDump ()
    {
        final List<Check<C>> checks = suite.getChecks();
        final StringBuilder sb = new StringBuilder();
        sb.append(suite.getName())
                .append(" ")
                .append(checkable);

        for (int i = 0; i < checks.size(); i++) {
            Check<C> check = checks.get(i);
            sb.append(" ")
                    .append(check.getName())
                    .append(":")
                    .append(String.format("%.2f", values[i]));
        }

        sb.append(
                String.format(" => %.2f (min %.2f)", grade, suite.getThreshold()));

        return sb.toString();
    }

    //----------//
    // getGrade //
    //----------//
    @Override
    public double getGrade ()
    {
        return grade;
    }

    //----------//
    // getValue //
    //----------//
    public double getValue (int index)
    {
        return values[index];
    }

    //-----------//
    // setDetail //
    //-----------//
    public void setDetail (int index,
                           double detail)
    {
        details[index] = detail;
    }

    //----------//
    // setGrade //
    //----------//
    public void setGrade (double grade)
    {
        this.grade = grade;
    }

    //----------//
    // setValue //
    //----------//
    public void setValue (int index,
                          double value)
    {
        values[index] = value;
    }

    //----------//
    // toString //
    //----------//
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
