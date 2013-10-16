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

    /** The quality of this interpretation. */
    protected final double grade;

    /** The hosting SIG. */
    protected SIGraph sig;

    /** The interpretation id, if identified. */
    private int id;

    /** Deleted flag, if any. */
    private boolean deleted;

    /** VIP flag. */
    private boolean vip;

    /** Object bounds, perhaps different from glyph bounds. */
    protected Rectangle box;

    /** Details about grade (for debugging). */
    protected GradeImpacts impacts;

    //~ Constructors -----------------------------------------------------------
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

            if (glyph.isVip()) {
                setVip();
            }
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

    //--------//
    // delete //
    //--------//
    @Override
    public void delete ()
    {
        if (!deleted) {
            deleted = true;

            if (sig != null) {
                sig.removeVertex(this);
            }

            if (glyph != null) {
                glyph.getInterpretations()
                        .remove(this);
            }
        }
    }

    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(
                String.format(
                "Inter: %s@%s%n",
                getClass().getSimpleName(),
                Integer.toHexString(this.hashCode())));
        sb.append(String.format("   %s%n", this));
        sb.append(String.format("   %s%n", getDetails()));

        return sb.toString();
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
        StringBuilder sb = new StringBuilder();

        if (getGlyph() != null) {
            sb.append(glyph.idString());
        }

        if (impacts != null) {
            if (sb.length() != 0) {
                sb.append(" ");
            }

            sb.append(impacts);
        }

        return sb.toString();
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

    //-------//
    // getId //
    //-------//
    @Override
    public int getId ()
    {
        return id;
    }

    //------------//
    // getImpacts //
    //------------//
    @Override
    public GradeImpacts getImpacts ()
    {
        return impacts;
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

    //-----------//
    // isDeleted //
    //-----------//
    @Override
    public boolean isDeleted ()
    {
        return deleted;
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

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public void setBounds (Rectangle box)
    {
        this.box = box;
    }

    //-------//
    // setId //
    //-------//
    @Override
    public void setId (int id)
    {
        assert this.id == 0 : "Reassigning inter id";
        assert id != 0 : "Assigning zero inter id";

        this.id = id;
    }

    //------------//
    // setImpacts //
    //------------//
    @Override
    public void setImpacts (GradeImpacts impacts)
    {
        this.impacts = impacts;
    }

    //--------//
    // setSig //
    //--------//
    @Override
    public void setSig (SIGraph sig)
    {
        this.sig = sig;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip ()
    {
        vip = true;
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

        if (getId() != 0) {
            sb.append("#")
                    .append(getId());
        }

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
