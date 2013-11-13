//----------------------------------------------------------------------------//
//                                                                            //
//                            B a s i c S u p p o r t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.Constant;
import omr.constant.ConstantSet;

/**
 * Class {@code BasicSupport} is the base implementation of {@link
 * Support} interface.
 *
 * @author Hervé Bitteur
 */
public class BasicSupport
        extends BasicRelation
        implements Support
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields --------------------------------------------------------
    /** Quality of the geometric junction. */
    protected double grade;

    /** Details about grade (for debugging). */
    protected GradeImpacts impacts;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BasicSupport object.
     */
    public BasicSupport ()
    {
        this(1.0);
    }

    /**
     * Creates a new BasicSupport object.
     *
     * @param grade the grade evaluated for this relation
     */
    public BasicSupport (double grade)
    {
        this.grade = grade;
    }

    //~ Methods ----------------------------------------------------------------
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

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "Support";
    }

    //-----------------//
    // getSupportRatio //
    //-----------------//
    @Override
    public double getSupportRatio ()
    {
        return 1.0 + (getSupportCoeff() * grade);
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
        return String.format("%.2f~%s", grade, getName());
    }

    //-----------------//
    // getSupportCoeff //
    //-----------------//
    /**
     * @return the coefficient used to compute support ratio
     */
    protected double getSupportCoeff ()
    {
        return constants.defaultSupportCoeff.getValue();
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Ratio minGrade = new Constant.Ratio(
                0.1,
                "Minimum support relation grade");

        final Constant.Ratio defaultSupportCoeff = new Constant.Ratio(
                10,
                "Default value for coeff in support formula: 1 + coeff*grade");

    }
}
