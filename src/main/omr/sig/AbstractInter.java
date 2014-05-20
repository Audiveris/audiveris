//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.AttachmentHolder;
import omr.glyph.ui.BasicAttachmentHolder;

import omr.math.GeoUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Map;

/**
 * Class {@code AbstractInter} is the abstract implementation basis for Interpretation
 * interface.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractInter
        implements Inter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying glyph, if any. */
    protected Glyph glyph;

    /** The assigned shape. */
    protected final Shape shape;

    /** The quality of this interpretation. */
    protected double grade;

    /** The contextual grade of this interpretation, if any. */
    protected Double contextualGrade;

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

    /** Object precise area, if any. */
    protected Area area;

    /** Details about grade (for debugging). */
    protected GradeImpacts impacts;

    /** Potential attachments, lazily allocated. */
    private AttachmentHolder attachments;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractInter object.
     *
     * @param glyph   the glyph to interpret
     * @param shape   the possible shape
     * @param impacts assignment details
     */
    public AbstractInter (Glyph glyph,
                          Shape shape,
                          GradeImpacts impacts)
    {
        this(glyph, null, shape, impacts);
    }

    /**
     * Creates a new AbstractInter object.
     *
     * @param box     the object bounds
     * @param shape   the possible shape
     * @param impacts assignment details
     */
    public AbstractInter (Rectangle box,
                          Shape shape,
                          GradeImpacts impacts)
    {
        this(null, box, shape, impacts);
    }

    /**
     * Creates a new AbstractInter object.
     *
     * @param glyph   the glyph to interpret
     * @param box     the precise object bounds (if different from glyph bounds)
     * @param shape   the possible shape
     * @param impacts assignment details
     */
    public AbstractInter (Glyph glyph,
                          Rectangle box,
                          Shape shape,
                          GradeImpacts impacts)
    {
        this(glyph, box, shape, impacts.getGrade());
        this.impacts = impacts;
    }

    /**
     * Creates a new AbstractInter object.
     *
     * @param glyph the glyph to interpret
     * @param box   the precise object bounds (if different from glyph bounds)
     * @param shape the possible shape
     * @param grade the interpretation quality
     */
    public AbstractInter (Glyph glyph,
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

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //---------------//
    // addAttachment //
    //---------------//
    @Override
    public void addAttachment (String id,
                               java.awt.Shape attachment)
    {
        assert attachment != null : "Adding a null attachment";

        if (attachments == null) {
            attachments = new BasicAttachmentHolder();
        }

        attachments.addAttachment(id, attachment);
    }

    //-------//
    // boost //
    //-------//
    @Override
    public void boost (double ratio)
    {
        if (grade < intrinsicRatio) {
            grade += (ratio * (intrinsicRatio - grade));
        }
    }

    //--------//
    // delete //
    //--------//
    @Override
    public void delete ()
    {
        if (!deleted) {
            deleted = true;

            if (isVip()) {
                logger.info("VIP delete {}", this);
            }

            if (sig != null) {
                sig.removeVertex(this);
            }

            if (glyph != null) {
                glyph.getInterpretations().remove(this);
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
                        "%s@%s%s%n",
                        getClass().getSimpleName(),
                        Integer.toHexString(this.hashCode()),
                        (sig != null) ? (" S#" + sig.getSystem().getId()) : ""));
        sb.append(String.format("   %s%n", this));

        if (!getDetails().isEmpty()) {
            sb.append(String.format("   %s", getDetails()));
        }

        return sb.toString();
    }

    //---------//
    // getArea //
    //---------//
    /**
     * @return the area, if any
     */
    @Override
    public Area getArea ()
    {
        return area;
    }

    //----------------//
    // getAttachments //
    //----------------//
    @Override
    public Map<String, java.awt.Shape> getAttachments ()
    {
        if (attachments != null) {
            return attachments.getAttachments();
        } else {
            return Collections.emptyMap();
        }
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (box != null) {
            return new Rectangle(box);
        }

        if (glyph != null) {
            return glyph.getBounds();
        }

        return null;
    }

    //-----------//
    // getCenter //
    //-----------//
    @Override
    public Point getCenter ()
    {
        if (box != null) {
            return GeoUtil.centerOf(box);
        }

        if (glyph != null) {
            return glyph.getAreaCenter();
        }

        return null;
    }

    //---------------//
    // getCenterLeft //
    //---------------//
    @Override
    public Point getCenterLeft ()
    {
        Rectangle bounds = getBounds();

        return new Point(bounds.x, bounds.y + (bounds.height / 2));
    }

    //----------------//
    // getCenterRight //
    //----------------//
    @Override
    public Point getCenterRight ()
    {
        Rectangle bounds = getBounds();

        return new Point(bounds.x + bounds.width, bounds.y + (bounds.height / 2));
    }

    //--------------------//
    // getContextualGrade //
    //--------------------//
    @Override
    public Double getContextualGrade ()
    {
        return contextualGrade;
    }

    //---------------//
    // getCoreBounds //
    //---------------//
    @Override
    public Rectangle2D getCoreBounds ()
    {
        return getBounds();
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder();

        if (glyph != null) {
            sb.append("g#").append(glyph.getId());
        }

        if (impacts != null) {
            if (sb.length() != 0) {
                sb.append(" ");
            }

            sb.append("(").append(impacts).append(")");
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

    //--------------//
    // getGoodGrade //
    //--------------//
    /**
     * Report the minimum grade to consider an interpretation as good.
     *
     * @return the minimum grade value for a good interpretation
     */
    public static double getGoodGrade ()
    {
        return goodGrade;
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

    //-------------//
    // getMinGrade //
    //-------------//
    /**
     * Report the minimum grade for an acceptable interpretation
     *
     * @return the minimum grade for keeping an Inter instance
     */
    public static double getMinGrade ()
    {
        return minGrade;
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

    //----------//
    // overlaps //
    //----------//
    @Override
    public boolean overlaps (Inter that)
    {
        if (this.area != null) {
            return this.area.intersects(that.getCoreBounds());
        } else if (that.getArea() != null) {
            return that.getArea().intersects(this.getCoreBounds());
        } else if ((this.getGlyph() != null) && (that.getGlyph() != null)) {
            return this.getGlyph().intersects(that.getGlyph());
        } else {
            return this.getCoreBounds().intersects(that.getCoreBounds());
        }
    }

    //-------------------//
    // removeAttachments //
    //-------------------//
    @Override
    public int removeAttachments (String prefix)
    {
        if (attachments != null) {
            return attachments.removeAttachments(prefix);
        } else {
            return 0;
        }
    }

    //-------------------//
    // renderAttachments //
    //-------------------//
    @Override
    public void renderAttachments (Graphics2D g)
    {
        if (attachments != null) {
            attachments.renderAttachments(g);
        }
    }

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public void setBounds (Rectangle box)
    {
        this.box = box;
    }

    //--------------------//
    // setContextualGrade //
    //--------------------//
    @Override
    public void setContextualGrade (double value)
    {
        contextualGrade = value;
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

        sb.append(shape);

        if (getId() != 0) {
            sb.append("#").append(getId());
        }

        sb.append(String.format("(%.2f", grade));

        if (contextualGrade != null) {
            sb.append(String.format("/%.2f", contextualGrade));
        }

        sb.append(")");

        return sb.toString();
    }

    //---------//
    // setArea //
    //---------//
    /**
     * @param area the area to set
     */
    protected void setArea (Area area)
    {
        this.area = area;
    }
}
