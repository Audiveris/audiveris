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

import org.audiveris.omr.classifier.OmrShapeMapping;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.BasicContainment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.util.AttachmentHolder;
import org.audiveris.omr.ui.util.BasicAttachmentHolder;
import org.audiveris.omr.util.AbstractEntity;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omrdataset.api.OmrShape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
 * <p>
 * As a general policy, subclasses can provide convenient creation static methods, which should
 * be consistently named as follows: <ul>
 * <li>{@code create} for just inter creation.
 * <li>{@code createValid} for inter creation and validation (if failed, inter is not created).
 * <li>{@code createAdded} for inter creation and addition to SIG.
 * <li>{@code createValidAdded} for inter creation, validation and addition to SIG.
 * </ul>
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
    @XmlAttribute(name = "shape")
    protected Shape shape;

    /** The precise omr shape. */
    @XmlAttribute(name = "omr-shape")
    protected OmrShape omrShape;

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

    /** Is it abnormal?. */
    @XmlAttribute(name = "abnormal")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean abnormal;

    /** Is it frozen?. */
    @XmlAttribute(name = "frozen")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean frozen;

    /** Is it manual?. */
    @XmlAttribute(name = "manual")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    protected boolean manual;

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

    /** Object precise area, if any. */
    protected Area area;

    /** Details about grade. */
    protected GradeImpacts impacts;

    /** Already removed from SIG?. */
    protected boolean removed;

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
     * Creates a new AbstractInter object, with a simple grade value.
     *
     * @param bounds   the object bounds
     * @param omrShape the OMR shape
     * @param grade    the interpretation quality
     */
    public AbstractInter (Rectangle bounds,
                          OmrShape omrShape,
                          double grade)
    {
        this.bounds = bounds;
        this.omrShape = omrShape;
        this.grade = grade;

        // Just in case old shape information is needed
        shape = OmrShapeMapping.shapeOf(omrShape);
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

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        removed = false;

        if (isVip()) {
            logger.info("VIP added {}", this);
        }
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        boolean abnormal = false;

        return abnormal;
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        getBounds();

        if ((bounds != null) && !bounds.contains(point)) {
            return false;
        }

        if ((glyph != null) && glyph.contains(point)) {
            return true;
        }

        if ((area != null) && area.contains(point)) {
            return true;
        }

        if (this instanceof InterEnsemble) {
            for (Inter member : ((InterEnsemble) this).getMembers()) {
                if (member.contains(point)) {
                    return true;
                }
            }
        }

        if (isManual() || (omrShape != null)) {
            // Here we use bounds only...
            return true; // Audaces fortuna juvat!
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
                        removed ? " REMOVED" : ""));
        sb.append(String.format("   %s%n", this));

        if (!getDetails().isEmpty()) {
            sb.append(String.format("   %s%n", getDetails()));
        }

        if (sig != null) {
            for (Relation rel : sig.edgesOf(this)) {
                sb.append(String.format("   rel: %s%n", rel.seenFrom(this)));
            }
        } else {
            sb.append(" noSIG");
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

    //-----------------//
    // getAllEnsembles //
    //-----------------//
    @Override
    public Set<Inter> getAllEnsembles ()
    {
        Set<Inter> ensembles = null;

        for (Relation rel : sig.incomingEdgesOf(this)) {
            if (rel instanceof BasicContainment) {
                if (ensembles == null) {
                    ensembles = new LinkedHashSet<Inter>();
                }

                ensembles.add(sig.getOppositeInter(this, rel));
            }
        }

        if (ensembles != null) {
            return ensembles;
        }

        return Collections.EMPTY_SET;
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

    //----------//
    // getColor //
    //----------//
    @Override
    public Color getColor ()
    {
        if (abnormal) {
            return Colors.INTER_ABNORMAL;
        }

        if (shape != null) {
            return shape.getColor();
        }

        return Colors.SHAPE_UNKNOWN;
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
        for (Relation rel : sig.incomingEdgesOf(this)) {
            if (rel instanceof BasicContainment) {
                return (InterEnsemble) sig.getOppositeInter(this, rel);
            }
        }

        return null;
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
        return Grades.goodInterGrade;
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
        return Grades.minInterGrade;
    }

    //-----------//
    // getMirror //
    //-----------//
    @Override
    public Inter getMirror ()
    {
        return mirror;
    }

    //-------------//
    // getOmrShape //
    //-------------//
    @Override
    public OmrShape getOmrShape ()
    {
        return omrShape;
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
     *
     * @param interline scaling factor
     * @return the symbol bounds
     */
    @Override
    public Rectangle getSymbolBounds (int interline)
    {
        Point center = getCenter(); // Use area center

        MusicFont musicFont = MusicFont.getBaseFont(interline);
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
    // hasStaff //
    //----------//
    @Override
    public boolean hasStaff ()
    {
        return staff != null;
    }

    //----------//
    // increase //
    //----------//
    @Override
    public void increase (double ratio)
    {
        if (grade < Grades.intrinsicRatio) {
            grade += (ratio * (Grades.intrinsicRatio - grade));
        }
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        // No-op by default
    }

    //------------//
    // isAbnormal //
    //------------//
    @Override
    public boolean isAbnormal ()
    {
        return abnormal;
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
    // isManual //
    //----------//
    @Override
    public boolean isManual ()
    {
        return manual;
    }

    //-----------//
    // isRemoved //
    //-----------//
    @Override
    public boolean isRemoved ()
    {
        return removed;
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
            } else {
                // Area <--> Bounds
                return this.area.intersects(that.getBounds());
            }
        }

        return true;
    }

    //--------//
    // remove //
    //--------//
    @Override
    public void remove ()
    {
        remove(true);
    }

    //--------//
    // remove //
    //--------//
    @Override
    public void remove (boolean extensive)
    {
        if (!removed) {
            logger.debug("Removing {} extensive:{}", this, extensive);

            if (isVip()) {
                logger.info("VIP remove {}", this);
            }

            removed = true;

            if (sig != null) {
                // Extensive is true for non-manual removals only
                if (extensive) {
                    // Handle ensemble - member cases?
                    // Copy is needed to avoid concurrent modification exception
                    List<Relation> relsCopy = new ArrayList<Relation>(sig.incomingEdgesOf(this));

                    for (Relation rel : relsCopy) {
                        // A member may be contained by several ensembles (case of TimeNumberInter)
                        if (rel instanceof BasicContainment) {
                            InterEnsemble ens = (InterEnsemble) sig.getOppositeInter(this, rel);

                            if (ens.getMembers().size() == 1) {
                                logger.debug("{} removing a dying ensemble {}", this, ens);
                                ens.remove(false);
                            }
                        }
                    }

                    if (this instanceof InterEnsemble) {
                        InterEnsemble ens = (InterEnsemble) this;

                        // Delete all its members that are not part of another ensemble
                        for (Inter member : ens.getMembers()) {
                            Set<Inter> ensembles = member.getAllEnsembles();

                            if (!ensembles.isEmpty()) {
                                ensembles.remove(this);

                                if (ensembles.isEmpty()) {
                                    logger.debug("{} removing a member {}", this, member);
                                    member.remove(false);
                                }
                            }
                        }
                    }
                }

                sig.removeVertex(this);
            }
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

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system,
                                         boolean doit)
    {
        return Collections.emptySet(); // By default
    }

    //-------------//
    // setAbnormal //
    //-------------//
    @Override
    public void setAbnormal (boolean abnormal)
    {
        if (this.abnormal != abnormal) {
            this.abnormal = abnormal;

            if (sig != null) {
                sig.getSystem().getSheet().getStub().setModified(true);
            }
        }
    }

    //---------//
    // setArea //
    //---------//
    /**
     * @param area the area to set
     */
    public void setArea (Area area)
    {
        this.area = area;
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
    // setManual //
    //-----------//
    @Override
    public void setManual (boolean manual)
    {
        this.manual = manual;
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

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before marshalling of this object begins.
     *
     * @param m (not used)
     */
    @SuppressWarnings("unused")
    protected void beforeMarshal (Marshaller m)
    {
        // Make sure this Inter instance either has no glyph or has a duly registered one
        if ((glyph != null) && (glyph.getId() == 0)) {
            logger.error("Inter referencing a non-registered glyph: " + this + " glyph: " + glyph);
        }

        // Make sure bounds exist
        getBounds();

        if (sig == null) {
            logger.error("Marshalling an inter with no sig " + this);
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

        if (isRemoved()) {
            sb.append(" REMOVED");
        }

        if (isManual()) {
            sb.append(" MANUAL");
        }

        if (isFrozen()) {
            sb.append(" FROZEN");
        }

        sb.append(String.format("(%.3f", grade));

        if (ctxGrade != null) {
            sb.append(String.format("/%.3f", ctxGrade));
        }

        sb.append(")");

        if (mirror != null) {
            sb.append(" mirror#").append(mirror.getId());
        }

        if (staff != null) {
            sb.append(" s:").append(staff.getId());
        }

        if (shape != null) {
            sb.append(" ").append(shape);
        }

        if (omrShape != null) {
            sb.append(" ").append(omrShape);
        }

        return sb.toString();
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
