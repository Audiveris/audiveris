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

import omr.math.GeoUtil;

import java.awt.Rectangle;

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
    /** The underlying glyph, if any. */
    protected final Glyph glyph;

    /** The assigned shape. */
    protected final Shape shape;

    /** The hosting SIG. */
    protected SIGraph sig;

    /** Object bounds, perhaps different from glyph bounds. */
    protected Rectangle box;

    /** The quality of this interpretation. */
    protected double grade;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // BasicInter //
    //------------//
    /**
     * Creates a new BasicInter object.
     *
     * @param glyph the glyph to interpret
     * @param shape the possible shape
     */
    public BasicInter (Glyph glyph,
                       Shape shape)
    {
        this(glyph, null, shape, 0);
    }

    //------------//
    // BasicInter //
    //------------//
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
        this(glyph, null, shape, grade);
    }

    //------------//
    // BasicInter //
    //------------//
    /**
     * Creates a new BasicInter object.
     *
     * @param box   the object bounds
     * @param shape the possible shape
     * @param grade the interpretation quality
     */
    public BasicInter (Rectangle box,
                       Shape shape,
                       double grade)
    {
        this(null, box, shape, grade);
    }

    //------------//
    // BasicInter //
    //------------//
    /**
     * Creates a new BasicInter object.
     *
     * @param glyph the glyph to interpret
     * @param box   the precise object bounds (if different from glyph bounds)
     * @param shape the possible shape
     * @param grade the interpretation quality
     */
    public BasicInter (Glyph glyph,
                       Rectangle box,
                       Shape shape,
                       double grade)
    {
        this.glyph = glyph;
        this.box = box;
        this.shape = shape;
        this.grade = grade;

        // Cross-linking
        if (glyph != null) {
            glyph.addInterpretation(this);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // getGoodGrade //
    //--------------//
    public static double getGoodGrade ()
    {
        return constants.goodGrade.getValue();
    }

    //-------------//
    // getMinGrade //
    //-------------//
    public static double getMinGrade ()
    {
        return constants.minGrade.getValue();
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (box != null) {
            return box;
        }

        if (glyph != null) {
            return glyph.getBounds();
        }

        return null;
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        if (getGlyph() != null) {
            return glyph.idString();
        }

        return "";
    }

    //----------//
    // getGlyph //
    //----------//
    @Override
    public Glyph getGlyph ()
    {
        return glyph;
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
    // getShape //
    //----------//
    @Override
    public Shape getShape ()
    {
        return shape;
    }

    //--------//
    // getSig //
    //--------//
    @Override
    public SIGraph getSig ()
    {
        return sig;
    }

    //--------//
    // isGood //
    //--------//
    @Override
    public boolean isGood ()
    {
        return grade >= getGoodGrade();
    }

    //----------//
    // isSameAs //
    //----------//
    @Override
    public boolean isSameAs (Inter that)
    {
        return ((this.getShape() == that.getShape())
                && GeoUtil.areIdentical(this.getBounds(), that.getBounds()));
    }

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public void setBounds (Rectangle box)
    {
        this.box = box;
    }

    //----------//
    // setGrade //
    //----------//
    @Override
    public void setGrade (double grade)
    {
        this.grade = grade;
    }

    //--------//
    // setSig //
    //--------//
    @Override
    public void setSig (SIGraph sig)
    {
        this.sig = sig;
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

        return sb.toString();
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
                0.08,
                "Minimum interpretation grade");

        final Constant.Ratio goodGrade = new Constant.Ratio(
                0.2,
                "Good interpretation grade");

    }
}
