//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.SigValue.InterSet;
import org.audiveris.omr.sig.relation.Partnership;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.util.AttachmentHolder;
import org.audiveris.omr.ui.util.BasicAttachmentHolder;
import org.audiveris.omr.util.AbstractEntity;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    /** The assigned shape. */
    @XmlAttribute
    protected Shape shape;

    /** The underlying glyph, if any. */
    @XmlIDREF
    @XmlAttribute(name = "glyph")
    protected Glyph glyph;

    /** Object bounds, perhaps different from glyph bounds. */
    @XmlElement(name = "bounds")
    protected Rectangle bounds;

    /** The quality of this interpretation. */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double3Adapter.class)
    protected double grade;

    /** Mirror instance, if any. */
    @XmlIDREF
    @XmlAttribute(name = "mirror")
    protected AbstractInter mirror;

    /** Frozen flag, if any. */
    @XmlAttribute(name = "frozen")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean frozen;

    /** The contextual grade of this interpretation, if any. */
    @XmlAttribute(name = "ctx-grade")
    @XmlJavaTypeAdapter(Jaxb.Double3Adapter.class)
    protected Double ctxGrade;

    /** Related staff, if any. Marshalled via its ID */
    @Navigable(false)
    protected Staff staff;

    // Transient data
    //---------------
    //
    /** Related part, if any. */
    @Navigable(false)
    protected Part part;

    /** The hosting SIG. */
    @Navigable(false)
    protected SIGraph sig;

    /** Deleted flag, if any. */
    protected boolean deleted;

    /** Containing ensemble, if any. */
    protected InterEnsemble ensemble;

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
        Objects.requireNonNull(attachment, "Adding a null attachment");

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
        delete(true);
    }

    //--------//
    // delete //
    //--------//
    @Override
    public void delete (boolean extensive)
    {
        if (!deleted) {
            if (isVip()) {
                logger.info("VIP delete {}", this);
            }

            deleted = true;

            if (extensive && ensemble instanceof InterEnsemble) {
                InterEnsemble ens = (InterEnsemble) ensemble;

                if (ens.getMembers().size() == 1) {
                    ens.delete(extensive);
                } else {
                    ens.removeMember(this);
                }
            }

            if (extensive && this instanceof InterEnsemble) {
                InterEnsemble ens = (InterEnsemble) this;

                // Delete the members
                for (Inter member : ens.getMembers()) {
                    member.delete(extensive);
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

        for (Relation rel : sig.edgesOf(this)) {
            sb.append(String.format("   rel: %s%n", rel.seenFrom(this)));
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

    //--------------------//
    // getReallyGoodGrade //
    //--------------------//
    /**
     * Report the minimum grade to consider an interpretation as really good.
     *
     * @return the minimum grade value for a really good interpretation
     */
    public static double getReallyGoodGrade ()
    {
        return goodGrade;
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    @Override
    public Point getRelationCenter ()
    {
        return getCenter(); // By default
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

    //--------------//
    // isReallyGood //
    //--------------//
    @Override
    public boolean isReallyGood ()
    {
        return grade >= getReallyGoodGrade();
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
            throws DeletedInterException
    {
        // Trivial case?
        if (!this.getCoreBounds().intersects(that.getCoreBounds())) {
            return false;
        }

        // Ensemble <--> that?
        if (this instanceof InterEnsemble) {
            final InterEnsemble thisEnsemble = (InterEnsemble) this;
            final List<? extends Inter> members = thisEnsemble.getMembers();

            if (members.contains(that)) {
                return false;
            }

            for (Inter thisMember : members) {
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

    //--------------------//
    // searchPartnerships //
    //--------------------//
    @Override
    public Collection<Partnership> searchPartnerships (SystemInfo system,
                                                       boolean doit)
    {
        return Collections.emptySet(); // By default
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
    public void setId (int id)
    {
        if (this.id != 0) {
            throw new IllegalStateException("Reassigning inter id");
        }

        if (id == 0) {
            throw new IllegalArgumentException("Assigning zero inter id");
        }

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
        if ((glyph != null) && (glyph.getId() == 0)) {
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
    /**
     * Protected method to report internal values of inter instance.
     *
     * @return a string on internal values
     */
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (isDeleted()) {
            sb.append(" DELETED");
        }

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

    //------------//
    // getStaffId //
    //------------//
    /**
     * Meant for JAXB.
     *
     * @return the ID of related staff.
     */
    @SuppressWarnings("unused")
    @XmlAttribute(name = "staff")
    private Integer getStaffId ()
    {
        if (staff == null) {
            return null;
        }

        return staff.getId();
    }

    //------------//
    // getStaffId //
    //------------//
    /**
     * Meant for JAXB.
     *
     * @param id the ID of related staff, if any.
     */
    @SuppressWarnings("unused")
    private void setStaffId (Integer id)
    {
        if (id != null) {
            setStaff(Staff.StaffHolder.getStaffHolder(id));
        }
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
