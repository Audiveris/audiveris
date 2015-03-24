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
package omr.sig.inter;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.AttachmentHolder;
import omr.glyph.ui.BasicAttachmentHolder;

import omr.math.AreaUtil;
import omr.math.GeoUtil;

import omr.sheet.Staff;
import omr.sheet.rhythm.Voice;

import omr.sig.GradeImpacts;
import omr.sig.SIGraph;

import omr.ui.symbol.MusicFont;

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

/**
 * Class {@code AbstractInter} is the abstract implementation basis for {@link Inter}
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
    protected Shape shape;

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

    /** Frozen flag, if any. */
    private boolean frozen;

    /** VIP flag. */
    private boolean vip;

    /** Mirror instance, if any. */
    protected Inter mirror;

    /** Containing ensemble, if any. */
    protected InterEnsemble ensemble;

    /** Object bounds, perhaps different from glyph bounds. */
    protected Rectangle box;

    /** Object precise area, if any. */
    protected Area area;

    /** Details about grade (for debugging). */
    protected GradeImpacts impacts;

    /** Potential attachments, lazily allocated. */
    private AttachmentHolder attachments;

    /** Related staff, if any. */
    protected Staff staff;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AbstractInter object, with detailed impacts information.
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
     * Creates a new AbstractInter object, with a simple grade value.
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
            deleted = true;

            if (isVip()) {
                logger.info("VIP delete {}", this);
            }

            if ((ensemble != null) && ensemble instanceof InterMutableEnsemble) {
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
                        "%s@%s%s%s%n",
                        getClass().getSimpleName(),
                        Integer.toHexString(this.hashCode()),
                        (sig != null) ? (" S#" + sig.getSystem().getId()) : "",
                        deleted ? " deleted" : ""));
        sb.append(String.format("   %s grade:%.6f%n", this, this.getGrade()));

        if (!getDetails().isEmpty()) {
            sb.append(String.format("   %s", getDetails()));
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
        frozen = true;

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
            return new Point(glyph.getAreaCenter());
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

    //-----------//
    // getMirror //
    //-----------//
    @Override
    public Inter getMirror ()
    {
        return mirror;
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
     * @return the staff
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
     * {@inheritDoc}.
     * <p>
     * This implementation uses the area center of inter and of symbol.
     * TODO: A better implementation would use centroids instead, but would require the handling of
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
        if (contextualGrade != null) {
            return contextualGrade >= getGoodGrade();
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
        return frozen;
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
            return this.getGlyph().getSignature().equals(that.getGlyph().getSignature());
        }

        return true;
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

            ///if (!(this instanceof InterMutableEnsemble)) {
            for (Inter thisMember : thisEnsemble.getMembers()) {
                if (thisMember.overlaps(that)
                    && that.overlaps(thisMember)
                    && sig.noSupport(thisMember, that)) {
                    return true;
                }
            }

            ///}
            return false;
        }

        if ((this.getGlyph() != null) && (that.getGlyph() != null)) {
            // Slur involved
            if (this instanceof SlurInter || that instanceof SlurInter) {
                // TODO: to catch glyphs left over between two slur arcs, we use "touching" instead of
                // true intersection, because the glyphs do not intersect per se. Could be improved.
                return this.getGlyph().touches(that.getGlyph());
            }

            // Fermata involved
            if (this instanceof FermataInter) {
                return that.getGlyph().intersects(this.getBounds());
            } else if (that instanceof FermataInter) {
                return this.getGlyph().intersects(that.getBounds());
            }

            // Glyph <--> Glyph? (not to be used when a slur or a fermata is involved)
            return this.getGlyph().intersects(that.getGlyph());
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

    //-------------//
    // setEnsemble //
    //-------------//
    @Override
    public void setEnsemble (InterEnsemble ensemble)
    {
        this.ensemble = ensemble;
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
    public void setId (int id)
    {
        assert this.id == 0 : "Reassigning inter id";
        assert id != 0 : "Assigning zero inter id";

        this.id = id;
    }

    //-----------//
    // setMirror //
    //-----------//
    @Override
    public void setMirror (Inter mirror)
    {
        this.mirror = mirror;
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
    public void setStaff (Staff staff)
    {
        this.staff = staff;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip ()
    {
        vip = true;
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
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(shapeString());

        if (getId() != 0) {
            sb.append("#").append(getId());
        }

        sb.append(String.format("(%.3f", grade));

        if (contextualGrade != null) {
            sb.append(String.format("/%.3f", contextualGrade));
        }

        sb.append(internals());
        sb.append(")");

        return sb.toString();
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

        if (glyph != null) {
            glyph.getInterpretations().add(this);
        }
    }

    //-----------//
    // internals //
    //-----------//
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder();

        if (mirror != null) {
            sb.append(" mirror#").append(mirror.getId());
        }

        if (ensemble != null) {
            sb.append(" m");
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
}
