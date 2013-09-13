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
 * Class {@code BasicSupport}
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

    /** Support ratio. */
    protected Double ratio;

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
    /**
     * @return the grade
     */
    public double getGrade ()
    {
        return grade;
    }

    //-------------//
    // getMinGrade //
    //-------------//
    public double getMinGrade ()
    {
        return constants.minGrade.getValue();
    }

    @Override
    public String getName ()
    {
        return "Support";
    }

    /**
     * @return the support ratio
     */
    @Override
    public Double getRatio ()
    {
        return ratio;
    }

    /**
     * @param grade the grade to set
     */
    public void setGrade (double grade)
    {
        this.grade = grade;
    }

    /**
     * @param support the support value to set
     */
    @Override
    public void setRatio (double support)
    {
        this.ratio = support;
    }

    @Override
    public String toString ()
    {
        return String.format("%.2f~%s %s", grade, getName(), internals());
    }

    @Override
    protected String internals ()
    {
        return "";
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

    }
}
