//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Glyphs;
import omr.glyph.Shape;

import omr.math.AreaUtil;
import omr.math.GeoUtil;

import omr.sheet.Part;
import omr.sheet.Staff;
import omr.sheet.rhythm.Voice;

import omr.sig.GradeImpacts;
import omr.sig.SIGraph;
import omr.sig.SigValue.InterSet;

import omr.ui.symbol.MusicFont;
import omr.ui.util.AttachmentHolder;
import omr.ui.util.BasicAttachmentHolder;

import omr.util.AbstractEntity;
import omr.util.Jaxb;
import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Collections;
import java.util.Map;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code AbstractInter} is the abstract implementation basis for {@link Inter}
 * interface.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractInter
        extends AbstractEntity
        implements Inter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            AbstractInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** The underlying glyph, if any. */
    @XmlIDREF
    @XmlAttribute(name = "glyph")
    protected Glyph glyph;

    /** The assigned shape. */
    @XmlAttribute
    protected Shape shape;

    /** The quality of this interpretation. */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double3Adapter.class)
    protected double grade;

    /** Mirror instance, if any. */
    @XmlIDREF
    @XmlAttribute(name = "mirror")
    protected AbstractInter mirror;

    /** Frozen flag, if any. */
    @XmlElement(name = "frozen")
    private Jaxb.True frozen;

    // Transient data
    //---------------
    //
    /** The contextual grade of this interpretation, if any. */
    protected Double ctxGrade;

    /** Related staff, if any. */
    @Navigable(false)
    protected Staff staff;

    /** Related part, if any. */
    @Navigable(false)
    protected Part part;

    /** The hosting SIG. */
    @Navigable(false)
    protected SIGraph sig;

    /** Deleted flag, if any. */
    private boolean deleted;

    /** Containing ensemble, if any. */
    protected InterEnsemble ensemble;

    /** Object bounds, perhaps different from glyph bounds. */
    protected Rectangle bounds;

    /** Object precise area, if any. */
    protected Area area;

    /** Details about grade. */
    protected GradeImpacts impacts;

    /** Potential attachments, lazily allocated. */
    private AttachmentHolder attachments;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractInter object, with detailed impacts information.
     *
     * @param glyph   the glyph to interpret
     * @param bounds  the precise object bounds (if different from glyph bounds)
     * @param shape   the possible shape
     * @param impacts assignment details
     */
    public AbstractInter (Glyph glyph,
                          Rectangle bounds,
                          Shape shape,
                          GradeImpacts impacts)
    {
        this(glyph, bounds, shape, (impacts != null) ? impacts.getGrade() : 0);
        this.impacts = impacts;
    }

    /**
     * Creates a new AbstractInter object, with a simple grade value.
     *
     * @param glyph  the glyph to interpret
     * @param bounds the precise object bounds (if different from glyph bounds)
     * @param shape  the possible shape
     * @param grade  the interpretation quality
     */
    public AbstractInter (Glyph glyph,
                          Rectangle bounds,
                          Shape shape,
                          double grade)
    {
        this.glyph = glyph;
        this.bounds = bounds;
        this.shape = shape;
        this.grade = grade;

        // Cross-linking
        if (glyph != null) {
            if (glyph.isVip()) {
                setVip(true);
            }
        }
    }

    /**
     * Creates a new {@code AbstractInter} object.
     */
    protected AbstractInter ()
    {
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

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        Rectangle bounds = getBounds();

        //
        //        if (bounds == null) {
        //            logger.info("No bounds for {}", this);
        //        }
        //
        if ((bounds != null) && !bounds.contains(point)) {
            return false;
        }

        if ((glyph != null) && glyph.contains(point)) {
            return true;
        }

        if ((area != null) && area.contains(point)) {
            return true;
        }

        return false;
    }

    //----------//
    // decrease //
    //----------//
    @Override
    public void decrease (double ratio)
    {
        grade *= (1 - ratio);
    }

    //--------//
    // delete //
    //--------//
    @Override
    public void delete ()
    {
        if (!deleted) {
            ///logger.info("delete {}", this);
            if (isVip()) {
                logger.info("VIP delete {}", this);
            }

            deleted = true;

            if (ensemble instanceof InterMutableEnsemble) {
                InterMutableEnsemble ime = (InterMutableEnsemble) ensemble;

                if (ime.getMembers().size() == 1) {
                    ime.delete();
                } else {
                    ime.removeMember(this);
                }
            }

            if (sig != null) {
                sig.removeVertex(this);
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
                        "%s@%s%s%s%n",
                        getClass().getSimpleName(),
                        Integer.toHexString(this.hashCode()),
                        (sig != null) ? (" System#" + sig.getSystem().getId()) : "",
                        deleted ? " deleted" : ""));
        sb.append(String.format("   %s%n", this));

        if (!getDetails().isEmpty()) {
            sb.append(String.format("   %s%n", getDetails()));
        }

        if (this instanceof InterEnsemble) {
            InterEnsemble ens = (InterEnsemble) this;
            sb.append("   members:").append(ens.getMembers());
        }

        return sb.toString();
    }

    //--------//
    // freeze //
    //--------//
    @Override
    public void freeze ()
    {
        frozen = Jaxb.TRUE;

        // Freeze members if any
        if (this instanceof InterEnsemble) {
            InterEnsemble ens = (InterEnsemble) this;

            for (Inter member : ens.getMembers()) {
                member.freeze();
            }
        }
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

    //--------------//
    // getBestGrade //
    //--------------//
    @Override
    public double getBestGrade ()
    {
        Double cg = getContextualGrade();

        if (cg != null) {
            return cg;
        }

        return getGrade();
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds != null) {
            return new Rectangle(bounds);
        }

        if (glyph != null) {
            return new Rectangle(bounds = glyph.getBounds());
        }

        return null;
    }

    //-----------//
    // getCenter //
    //-----------//
    @Override
    public Point getCenter ()
    {
        if (getBounds() != null) {
            return GeoUtil.centerOf(bounds);
        }

        if (glyph != null) {
            return new Point(glyph.getCenter());
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

        return new Point((bounds.x + bounds.width) - 1, bounds.y + (bounds.height / 2));
    }

    //--------------------//
    // getContextualGrade //
    //--------------------//
    @Override
    public Double getContextualGrade ()
    {
        return ctxGrade;
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

    //-------------//
    // getEnsemble //
    //-------------//
    @Override
    public InterEnsemble getEnsemble ()
    {
        return ensemble;
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

    //-----------//
    // getMirror //
    //-----------//
    @Override
    public Inter getMirror ()
    {
        return mirror;
    }

    //---------//
    // getPart //
    //---------//
    @Override
    public Part getPart ()
    {
        if (part == null) {
            // Default implementation, for the case of Inter instances with staff defined.
            if (staff != null) {
                part = staff.getPart();
            }
        }

        return part;
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

    //----------//
    // getStaff //
    //----------//
    /**
     * @return the assigned staff, if any, otherwise null
     */
    @Override
    public Staff getStaff ()
    {
        return staff;
    }

    //-----------------//
    // getSymbolBounds //
    //-----------------//
    /**
     * {@inheritDoc}
     * <p>
     * This implementation uses the area center of inter and of symbol.
     * TODO: A better implementation could use centroids instead, but would require the handling of
     * symbol centroid.
     *
     * @param interline scaling factor
     * @return the symbol bounds
     */
    @Override
    public Rectangle getSymbolBounds (int interline)
    {
        Point center = getCenter(); // Use area center

        MusicFont musicFont = MusicFont.getFont(interline);
        TextLayout layout = musicFont.layout(getShape());
        Rectangle2D bounds = layout.getBounds();

        return new Rectangle(
                center.x - (int) Math.rint(bounds.getWidth() / 2),
                center.y - (int) Math.rint(bounds.getHeight() / 2),
                (int) Math.rint(bounds.getWidth()),
                (int) Math.rint(bounds.getHeight()));
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        return null;
    }

    //----------//
    // increase //
    //----------//
    @Override
    public void increase (double ratio)
    {
        if (grade < intrinsicRatio) {
            grade += (ratio * (intrinsicRatio - grade));
        }
    }

    //--------------------//
    // isContextuallyGood //
    //--------------------//
    @Override
    public boolean isContextuallyGood ()
    {
        if (ctxGrade != null) {
            return ctxGrade >= getGoodGrade();
        }

        return grade >= getGoodGrade();
    }

    //-----------//
    // isDeleted //
    //-----------//
    @Override
    public boolean isDeleted ()
    {
        return deleted;
    }

    //----------//
    // isFrozen //
    //----------//
    @Override
    public boolean isFrozen ()
    {
        return frozen != null;
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
        if ((this.getShape() != that.getShape()) || !this.getBounds().equals(that.getBounds())) {
            return false;
        }

        if ((this.getGlyph() != null) && (that.getGlyph() != null)) {
            return this.getGlyph().isIdentical(that.getGlyph());
        }

        return true;
    }

    //----------//
    // overlaps //
    //----------//
    @Override
    public boolean overlaps (Inter that)
    {
        // Trivial case? TODO: use getBounds() rather than getCoreBounds()???????????????????????
        Rectangle2D thisBounds = this.getCoreBounds();
        Rectangle2D thatBounds = that.getCoreBounds();

        if (!thisBounds.intersects(thatBounds)) {
            return false;
        }

        // Ensemble <--> that?
        if (this instanceof InterEnsemble) {
            InterEnsemble thisEnsemble = (InterEnsemble) this;

            if (thisEnsemble.getMembers().contains(that)) {
                return false;
            }

            for (Inter thisMember : thisEnsemble.getMembers()) {
                if (thisMember.overlaps(that)
                    && that.overlaps(thisMember)
                    && sig.noSupport(thisMember, that)) {
                    return true;
                }
            }

            return false;
        }

        if ((this.getGlyph() != null) && (that.getGlyph() != null)) {
            // Slur involved
            if (this instanceof SlurInter || that instanceof SlurInter) {
                // TODO: to catch glyphs left over between two slur arcs, we use "touching" instead of
                // true intersection, because the glyphs do not intersect per se. Could be improved.
                return Glyphs.intersect(this.getGlyph(), that.getGlyph(), true);
            }

            // Fermata involved
            if (this instanceof FermataInter) {
                return that.getGlyph().intersects(this.getBounds());
            } else if (that instanceof FermataInter) {
                return this.getGlyph().intersects(that.getBounds());
            }

            // Glyph <--> Glyph? (not to be used when a slur or a fermata is involved)
            return Glyphs.intersect(this.getGlyph(), that.getGlyph(), false);
        }

        // Area <--> that?
        if (this.area != null) {
            if (that.getArea() != null) {
                // Area <--> Area?
                return AreaUtil.intersection(this.area, that.getArea());
            } else if (that.getGlyph() != null) {
                // Area <--> Glyph?
                return that.getGlyph().intersects(this.area);
            }
        }

        return true;
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
    public void setBounds (Rectangle bounds)
    {
        this.bounds = bounds;
    }

    //--------------------//
    // setContextualGrade //
    //--------------------//
    @Override
    public void setContextualGrade (double value)
    {
        ctxGrade = value;
    }

    //-------------//
    // setEnsemble //
    //-------------//
    @Override
    public void setEnsemble (InterEnsemble ensemble)
    {
        this.ensemble = ensemble;
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        this.glyph = glyph;
    }

    //----------//
    // setGrade //
    //----------//
    @Override
    public void setGrade (double grade)
    {
        this.grade = grade;
    }

    //-------//
    // setId //
    //-------//
    @Override
    public void setId (String id)
    {
        assert this.id == null : "Reassigning inter id";
        assert id != null : "Assigning null inter id";

        this.id = id;
    }

    //-----------//
    // setMirror //
    //-----------//
    @Override
    public void setMirror (Inter mirror)
    {
        this.mirror = (AbstractInter) mirror;
    }

    //---------//
    // setPart //
    //---------//
    /**
     * @param part the part to set
     */
    @Override
    public void setPart (Part part)
    {
        this.part = part;
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
    // setStaff //
    //----------//
    /**
     * @param staff the staff to set
     */
    @Override
    public void setStaff (Staff staff)
    {
        this.staff = staff;
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return shape.toString();
    }

    //----------//
    // undelete //
    //----------//
    @Override
    public void undelete ()
    {
        if (isVip()) {
            logger.info("VIP undelete {}", this);
        }

        deleted = false;
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before marshalling of this object begins.
     */
    @SuppressWarnings("unused")
    protected void beforeMarshal (Marshaller m)
    {
        // Make sure this Inter instance either has no glyph or has a duly registered one
        if ((glyph != null) && (glyph.getId() == null)) {
            logger.error("Inter referencing a non-registered glyph: " + this + " glyph: " + glyph);
        }

        if (sig == null) {
            logger.error("Marshalling an inter with no sig " + this);
        } else {
            InterSet interSet = sig.getSystem().getInterSet();

            if (interSet != null) {
                interSet.addInter(this);
            }
        }
    }

    //-----------//
    // internals //
    //-----------//
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(String.format("(%.3f", grade));

        if (ctxGrade != null) {
            sb.append(String.format("/%.3f", ctxGrade));
        }

        sb.append(")");

        if (mirror != null) {
            sb.append(" mirror#").append(mirror.getId());
        }

        if (ensemble != null) {
            sb.append(" e#").append(ensemble.getId());
        }

        if (staff != null) {
            sb.append(" s:").append(staff.getId());
        }

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

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Meant for JAXB handling of Inter interface.
     */
    public static class Adapter
            extends XmlAdapter<AbstractInter, Inter>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public AbstractInter marshal (Inter inter)
                throws Exception
        {
            return (AbstractInter) inter;
        }

        @Override
        public Inter unmarshal (AbstractInter abstractInter)
                throws Exception
        {
            return abstractInter;
        }
    }
}
