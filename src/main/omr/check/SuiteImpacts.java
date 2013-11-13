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

import omr.sig.AbstractImpacts;

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
        extends AbstractImpacts
{
    //~ Instance fields --------------------------------------------------------

    /** The underlying suite of check instances. */
    private final CheckSuite<C> suite;

    /** The checked object. */
    private final Checkable checkable;

    /** Individual check values. */
    private final double[] values;

    /** Individual check impacts. */
    private final double[] impacts;

    /** Resulting suite grade. */
    private double grade;

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // SuiteImpacts //
    //--------------//
    /**
     * Creates a new SuiteImpacts object.
     *
     * @param suite     the underlying check suite
     * @param checkable the checkable entity
     */
    public SuiteImpacts (CheckSuite<C> suite,
                         C checkable)
    {
        this.suite = suite;
        this.checkable = checkable;

        final int size = suite.getChecks()
                .size();
        values = new double[size];
        impacts = new double[size];
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

    //-----------//
    // getImpact //
    //-----------//
    @Override
    public double getImpact (int index)
    {
        return impacts[index];
    }

    //----------------//
    // getImpactCount //
    //----------------//
    @Override
    public int getImpactCount ()
    {
        return impacts.length;
    }

    //---------//
    // getName //
    //---------//
    @Override
    public String getName (int index)
    {
        return suite.getChecks()
                .get(index)
                .getName();
    }

    //----------//
    // getValue //
    //----------//
    public double getValue (int index)
    {
        return values[index];
    }

    //-----------//
    // getWeight //
    //-----------//
    @Override
    public double getWeight (int index)
    {
        return suite.getWeights()
                .get(index);
    }

    //----------//
    // setGrade //
    //----------//
    public void setGrade (double grade)
    {
        this.grade = grade;
    }

    //-----------//
    // setImpact //
    //-----------//
    public void setImpact (int index,
                           double detail)
    {
        impacts[index] = detail;
    }

    //----------//
    // setValue //
    //----------//
    public void setValue (int index,
                          double value)
    {
        values[index] = value;
    }
}
