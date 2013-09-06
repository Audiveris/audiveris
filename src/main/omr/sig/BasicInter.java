//----------------------------------------------------------------------------//
//                                                                            //
//                    B a s i c I n t e r p r e t a t i o n                    //
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

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

/**
 * Class {@code BasicInter} is the basis implementation for
 * Interpretation interface.
 *
 * @author Hervé Bitteur
 */
public class BasicInter
        implements Inter
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields --------------------------------------------------------
    /** The related glyph. */
    protected final Glyph glyph;

    /** The assigned shape. */
    protected Shape shape; // Make it final ASAP!!!!!!

    /** The quality (grade) of this possible interpretation. */
    protected double grade;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BasicInter object.
     *
     * @param glyph the glyph to interpret
     * @param shape the possible shape
     */
    public BasicInter (Glyph glyph,
                       Shape shape)
    {
        this(glyph, shape, 0);
    }

    /**
     * Creates a new BasicInter object.
     *
     * @param glyph the glyph to interpret
     * @param shape the possible shape
     * @param grade the interpretation quality
     */
    public BasicInter (Glyph glyph,
                       Shape shape,
                       double grade)
    {
        this.glyph = glyph;
        this.shape = shape;
        this.grade = grade;

        // Cross-linking
        if (glyph != null) {
            glyph.addInterpretation(this);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * @return the glyph
     */
    @Override
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //--------------//
    // getGoodGrade //
    //--------------//
    public static double getGoodGrade ()
    {
        return constants.goodGrade.getValue();
    }

    /**
     * @return the grade
     */
    @Override
    public double getGrade ()
    {
        return grade;
    }

    //-------------//
    // getMinGrade //
    //-------------//
    public static double getMinGrade ()
    {
        return constants.minGrade.getValue();
    }

    /**
     * @return the shape
     */
    @Override
    public Shape getShape ()
    {
        return shape;
    }

    /**
     * @param grade the grade to set
     */
    @Override
    public void setGrade (double grade)
    {
        this.grade = grade;
    }

    /**
     * Delete this ASAP !!!!!!!!!!!!!!!!!!!!!!!
     *
     * @param shape
     */
    @Override
    public void setShape (Shape shape)
    {
        this.shape = shape;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%.2f", grade));

        sb.append("~")
                .append(shape);

        if (getGlyph() != null) {
            sb.append('#')
                    .append(getGlyph().getId());
        }

        return sb.toString();
    }

    //--------//
    // isGood //
    //--------//
    @Override
    public boolean isGood ()
    {
        return grade >= getGoodGrade();
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
                "Minimum interpretation grade");

        final Constant.Ratio goodGrade = new Constant.Ratio(
                0.2,
                "Good interpretation grade");

    }
}
