//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t S u p p o r t                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sig.GradeImpacts;

/**
 * Class {@code AbstractSupport} is the base implementation of {@link Support} interface.
 *
 * @author Hervé Bitteur
 */
public class AbstractSupport
        extends AbstractRelation
        implements Support
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Quality of the geometric junction. */
    protected double grade;

    /** Details about grade (for debugging). */
    protected GradeImpacts impacts;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicSupport object, with a grade set to 1.
     */
    public AbstractSupport ()
    {
        this(1.0);
    }

    /**
     * Creates a new BasicSupport object.
     *
     * @param grade the grade evaluated for this relation
     */
    public AbstractSupport (double grade)
    {
        this.grade = grade;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getGrade //
    //----------//
    /**
     * @return the grade
     */
    public double getGrade ()
    {
        return grade;
    }

    //------------//
    // getImpacts //
    //------------//
    @Override
    public GradeImpacts getImpacts ()
    {
        return impacts;
    }

    //-------------//
    // getMinGrade //
    //-------------//
    public double getMinGrade ()
    {
        return constants.minGrade.getValue();
    }

    //----------------//
    // getSourceRatio //
    //----------------//
    @Override
    public final double getSourceRatio ()
    {
        return 1.0 + (getSourceCoeff() * grade);
    }

    //----------------//
    // getTargetRatio //
    //----------------//
    @Override
    public final double getTargetRatio ()
    {
        return 1.0 + (getTargetCoeff() * grade);
    }

    //----------//
    // setGrade //
    //----------//
    /**
     * @param grade the grade to set
     */
    public void setGrade (double grade)
    {
        this.grade = grade;
    }

    //------------//
    // setImpacts //
    //------------//
    @Override
    public void setImpacts (GradeImpacts impacts)
    {
        this.impacts = impacts;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return String.format("%.2f~%s", grade, super.toString());
    }

    //----------------//
    // getSourceCoeff //
    //----------------//
    /**
     * @return the coefficient used to compute source support ratio (default: 0)
     */
    protected double getSourceCoeff ()
    {
        return 0;
    }

    //----------------//
    // getTargetCoeff //
    //----------------//
    /**
     * @return the coefficient used to compute target support ratio (default: 0)
     */
    protected double getTargetCoeff ()
    {
        return 0;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Ratio minGrade = new Constant.Ratio(0.1, "Minimum support relation grade");
    }
}
