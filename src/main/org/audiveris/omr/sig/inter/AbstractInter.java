//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    A b s t r a c t I n t e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Profiles;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.MirrorRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.Support;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.DefaultEditor;
import org.audiveris.omr.sig.ui.EditionTask;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.InterTracker;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.symbol.FontSymbol;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.util.AttachmentHolder;
import org.audiveris.omr.ui.util.BasicAttachmentHolder;
import org.audiveris.omr.util.AbstractEntity;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Version;
import org.audiveris.omr.util.WrappedBoolean;
import org.audiveris.omr.util.Wrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Class <code>AbstractInter</code> is the abstract implementation basis for {@link Inter}
 * interface.
 * <p>
 * As a general policy, subclasses of <code>AbstractInter</code> can provide convenient creation
 * static methods, which should be consistently named as follows:
 * <ul>
 * <li><code>create</code> for just inter creation.
 * <li><code>createValid</code> for inter creation and validation (if failed, inter is not
 * created).
 * <li><code>createAdded</code> for inter creation and addition to SIG.
 * <li><code>createValidAdded</code> for inter creation, validation and addition to SIG.
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

    private static final Logger logger = LoggerFactory.getLogger(AbstractInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** The shape assigned to this Inter. */
    @Navigable(false)
    @XmlAttribute(name = "shape")
    protected Shape shape;

    /** The underlying glyph, if any. */
    @XmlIDREF
    @XmlAttribute(name = "glyph")
    protected Glyph glyph;

    /** The [0..1] confidence value for this Inter <i>in isolation</i>. */
    @XmlAttribute
    @XmlJavaTypeAdapter(Jaxb.Double3Adapter.class)
    private Double grade;

    /** The [0..1] confidence value for this Inter <i>in context</i>. */
    @XmlAttribute(name = "ctx-grade")
    @XmlJavaTypeAdapter(Jaxb.Double3Adapter.class)
    protected Double ctxGrade;

    /** Indicates if this Inter was detected as abnormal by the engine. */
    @XmlAttribute(name = "abnormal")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean abnormal;

    /** Indicates if this Inter is no longer modifiable by the engine. */
    @XmlAttribute(name = "frozen")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean frozen;

    /** Indicates if this Inter was created implicitly by the engine. */
    @XmlAttribute(name = "implicit")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean implicit;

    /** Indicates if this Inter was created manually by the end-user. */
    @XmlAttribute(name = "manual")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    protected boolean manual;

    /**
     * The staff, if any, this Inter relates to.
     * Marshalled via the staff id.
     *
     * @see #getStaffId()
     */
    @Navigable(false)
    protected Staff staff;

    /** The Inter bounds, perhaps different from glyph bounds. */
    @XmlElement(name = "bounds")
    @XmlJavaTypeAdapter(Jaxb.RectangleAdapter.class)
    protected Rectangle bounds;

    // Transient data
    //---------------

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

    /** Core bounds meant for overlap check. */
    protected Rectangle coreBounds;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>AbstractInter</code> object.
     */
    protected AbstractInter ()
    {
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
                          Double grade)
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

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        return false;
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

        if (glyph != null) {
            return glyph.contains(point);
        }

        if (area != null) {
            return area.contains(point);
        }

        if (this instanceof InterEnsemble) {
            for (Inter member : ((InterEnsemble) this).getMembers()) {
                if (member.contains(point)) {
                    return true;
                }
            }
        }

        // Use central part of bounds
        Point center = getCenter();

        if (center != null) {
            return center.distanceSq(point) <= ((bounds.width * bounds.height) / 4.0);
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

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation)
    {
        // For a basic ghost, focus center is area center, and we just need to set inter bounds.
        final Dimension dim = symbol.getDimension(font);
        final Rectangle box = new Rectangle(
                dropLocation.x - (dim.width / 2),
                dropLocation.y - (dim.height / 2),
                dim.width,
                dim.height);
        setBounds(box);

        return true;
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
            if (rel instanceof Containment) {
                if (ensembles == null) {
                    ensembles = new LinkedHashSet<>();
                }

                ensembles.add(sig.getEdgeSource(rel));
            }
        }

        if (ensembles != null) {
            return ensembles;
        }

        return Collections.emptySet();
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
            return GeoUtil.center(bounds);
        }

        if (glyph != null) {
            return new Point(glyph.getCenter());
        }

        return null;
    }

    //-------------//
    // getCenter2D //
    //-------------//
    @Override
    public Point2D getCenter2D ()
    {
        if (getBounds() != null) {
            return GeoUtil.center2D(bounds);
        }

        if (glyph != null) {
            return glyph.getCenter2D();
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

        if (bounds == null) {
            return null;
        }

        return new Point(bounds.x, bounds.y + (bounds.height / 2));
    }

    //----------------//
    // getCenterRight //
    //----------------//
    @Override
    public Point getCenterRight ()
    {
        Rectangle bounds = getBounds();

        if (bounds == null) {
            return null;
        }

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

        if (!isFrozen() && (ctxGrade != null) && (ctxGrade < Grades.minContextualGrade)) {
            return Colors.INTER_WEAK;
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
    public Rectangle getCoreBounds ()
    {
        if (coreBounds == null) {
            final Rectangle box = getBounds();

            coreBounds = box;
        }

        return new Rectangle(coreBounds);
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        final StringBuilder sb = new StringBuilder();

        if (glyph != null) {
            sb.append("g#").append(glyph.getId());
        }

        if (impacts != null) {
            sb.append((sb.length() != 0) ? " " : "");
            sb.append("(").append(impacts).append(")");
        }

        if (abnormal) {
            sb.append((sb.length() != 0) ? " " : "");
            sb.append("ABNORMAL");
        }

        return sb.toString();
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new DefaultEditor(this);
    }

    //-------------//
    // getEnsemble //
    //-------------//
    @Override
    public InterEnsemble getEnsemble ()
    {
        if (isRemoved()) {
            return null;
        }

        for (Relation rel : sig.incomingEdgesOf(this)) {
            if (rel instanceof Containment) {
                return (InterEnsemble) sig.getEdgeSource(rel);
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

    //----------//
    // getGrade //
    //----------//
    @Override
    public Double getGrade ()
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

    //----------//
    // getLinks //
    //----------//
    @Override
    public Collection<Link> getLinks ()
    {
        Set<Link> links = null;

        if (sig.containsVertex(this)) {
            for (Relation rel : sig.edgesOf(this)) {
                if (links == null) {
                    links = new LinkedHashSet<>();
                }

                final Inter partner = sig.getOppositeInter(this, rel);
                links.add(new Link(partner, rel, sig.getEdgeTarget(rel) == partner));
            }
        }

        if (links == null) {
            return Collections.emptySet();
        }

        return links;
    }

    //-----------//
    // getMirror //
    //-----------//
    @Override
    public Inter getMirror ()
    {
        if (sig != null) {
            for (Relation rel : sig.getRelations(this, MirrorRelation.class)) {
                return sig.getOppositeInter(this, rel);
            }
        }

        return null;
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

    //------------//
    // getProfile //
    //------------//
    @Override
    public int getProfile ()
    {
        return isManual() ? Profiles.MANUAL : Profiles.STRICT;
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    @Override
    public Point2D getRelationCenter ()
    {
        final Point2D center = getCenter2D();

        if (center != null) {
            return center;
        }

        if (glyph != null) {
            return glyph.getCenter2D();
        }

        return null;
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    @Override
    public Point2D getRelationCenter (Relation relation)
    {
        return getRelationCenter(); // By default
    }

    //----------//
    // getShape //
    //----------//
    @Override
    public Shape getShape ()
    {
        return shape;
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return shape.toString();
    }

    //----------------//
    // getShapeSymbol //
    //----------------//
    @Override
    public ShapeSymbol getShapeSymbol (MusicFamily family)
    {
        if (shape == null) {
            return null;
        }

        return shape.getDecoratedSymbol(family);
    }

    //--------//
    // getSig //
    //--------//
    @Override
    public SIGraph getSig ()
    {
        return sig;
    }

    //-----------------//
    // getSpecificPart //
    //-----------------//
    @Override
    public Part getSpecificPart ()
    {
        return part;
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
        final Point center = getCenter(); // Use area center
        final Sheet sheet = staff.getSystem().getSheet();
        final MusicFamily family = sheet.getStub().getMusicFamily();
        final FontSymbol fs = getShape().getFontSymbolByInterline(family, interline);
        final TextLayout layout = fs.getLayout();
        final Rectangle2D box = layout.getBounds();

        return new Rectangle(
                center.x - (int) Math.rint(box.getWidth() / 2),
                center.y - (int) Math.rint(box.getHeight() / 2),
                (int) Math.rint(box.getWidth()),
                (int) Math.rint(box.getHeight()));
    }

    //------------//
    // getTracker //
    //------------//
    @Override
    public InterTracker getTracker (Sheet sheet)
    {
        return new InterTracker(this, sheet);
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

    //-------------------------//
    // imposeWithinStaffLimits //
    //-------------------------//
    @Override
    public boolean imposeWithinStaffLimits ()
    {
        return false; // By default
    }

    //----------//
    // increase //
    //----------//
    @Override
    public void increase (double ratio)
    {
        grade = increaseGrade(grade, ratio);
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

        if (isImplicit()) {
            sb.append(" IMPLICIT");
        }

        if (isFrozen()) {
            sb.append(" FROZEN");
        }

        sb.append(String.format("(%.3f", grade));

        Double cg = getContextualGrade();

        if (cg != null) {
            sb.append(String.format("/%.3f", cg));
        }

        sb.append(")");

        if (staff != null) {
            sb.append(" stf:").append(staff.getId());
        }

        if (shape != null) {
            sb.append(" ").append(shape);
        }

        return sb.toString();
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

    //------------//
    // isEditable //
    //------------//
    @Override
    public boolean isEditable ()
    {
        return !(this instanceof InterEnsemble);
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

    //------------//
    // isImplicit //
    //------------//
    @Override
    public boolean isImplicit ()
    {
        return implicit;
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
        if (this.getShape() != that.getShape()) {
            return false;
        }

        Rectangle thisBounds = this.getBounds();
        Rectangle thatBounds = that.getBounds();

        if ((thisBounds == null) || (thatBounds == null)) {
            return false;
        }

        if (!thisBounds.equals(thatBounds)) {
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
        if (this.isImplicit() || that.isImplicit()) {
            return false;
        }

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
                if (thisMember.overlaps(that) && that.overlaps(thisMember) && sig.noSupport(
                        thisMember,
                        that)) {
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

        if ((this.getGlyph() != null)) {
            return this.getGlyph().intersects(that.getBounds());
        }

        if ((that.getGlyph() != null)) {
            return that.getGlyph().intersects(this.getBounds());
        }

        return true;
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel,
                                          Wrapper<Inter> toPublish)
    {
        final SystemInfo system = staff.getSystem();

        return Arrays.asList(
                new AdditionTask(system.getSig(), this, getBounds(), searchLinks(system)));
    }

    //---------//
    // preEdit //
    //---------//
    @Override
    public List<? extends UITask> preEdit (InterEditor editor)
    {
        // Append the EditionTask with inter links and unlinks
        final SystemInfo system = getSig().getSystem();
        final Collection<Link> links = searchLinks(system);
        final Collection<Link> unlinks = searchUnlinks(system, links);
        final List<UITask> tasks = new ArrayList<>();
        tasks.add(new EditionTask(editor, links, unlinks));

        return tasks;
    }

    //-----------//
    // preRemove //
    //-----------//
    @Override
    public Set<? extends Inter> preRemove (WrappedBoolean cancel)
    {
        return Collections.singleton(this);
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
        if (isRemoved()) {
            return;
        }

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
                List<Relation> relsCopy = new ArrayList<>(sig.incomingEdgesOf(this));

                for (Relation rel : relsCopy) {
                    // A member may be contained by several ensembles (case of TimeNumberInter)
                    if (rel instanceof Containment) {
                        InterEnsemble ens = (InterEnsemble) sig.getEdgeSource(rel);

                        if (ens.getMembers().size() == 1) {
                            logger.debug("{} removing a dying ensemble {}", this, ens);
                            ens.remove(false);
                        }
                    }
                }

                if (this instanceof InterEnsemble ens) {
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

            // Make sure the underlying glyph remains accessible
            if (glyph != null) {
                final SystemInfo system = sig.getSystem();
                final OmrStep step = system.getSheet().getStub().getLatestStep();

                if (step != null && step.compareTo(OmrStep.CURVES) >= 0) {
                    system.addFreeGlyph(glyph);
                }
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

    //---------------//
    // renameShapeAs //
    //---------------//
    @Override
    public boolean renameShapeAs (Shape shape)
    {
        if (this.shape != shape) {
            this.shape = shape;
            return true;
        }

        return false;
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
    public Collection<Link> searchLinks (SystemInfo system)
    {
        return Collections.emptySet(); // By default
    }

    //---------------------//
    // searchObsoletelinks //
    //---------------------//
    /**
     * Retrieve all obsolete support links.
     * <p>
     * This results in all support relations of the specified classes, that are not contained
     * in the provided collection of non-obsolete relations.
     *
     * @param links   non-obsolete links
     * @param classes specific support relation classes to check
     * @return the collection of obsolete support links
     */
    @SafeVarargs
    protected final Collection<Link> searchObsoletelinks (Collection<Link> links,
                                                          Class<? extends Support>... classes)
    {
        if (isRemoved()) {
            return Collections.emptySet();
        }

        Collection<Link> unlinks = null;

        ExistingLoop:
        for (Relation rel : sig.getRelations(this, classes)) {
            final Inter other = sig.getOppositeInter(this, rel);

            for (Link link : links) {
                if (other == link.partner) {
                    continue ExistingLoop;
                }
            }

            if (unlinks == null) {
                unlinks = new LinkedHashSet<>();
            }

            final boolean outgoing = (other == sig.getEdgeTarget(rel));
            unlinks.add(new Link(other, rel, outgoing));
        }

        return (unlinks != null) ? unlinks : Collections.emptySet();
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
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
     * Set the underlying area.
     *
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
        this.bounds = (bounds != null) ? new Rectangle(bounds) : null;
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
    public void setGrade (Double grade)
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

    //-------------//
    // setImplicit //
    //-------------//
    @Override
    public void setImplicit (boolean implicit)
    {
        this.implicit = implicit;
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
        final boolean direct = this.getId() < mirror.getId();
        final Inter source = direct ? this : mirror;
        final Inter target = direct ? mirror : this;
        Relation rel = sig.getRelation(source, target, MirrorRelation.class);

        if (rel == null) {
            sig.addEdge(source, target, new MirrorRelation());
        }
    }

    //---------//
    // setPart //
    //---------//
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

    //------------//
    // setStaffId //
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

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    @Override
    public boolean upgradeOldStuff (List<Version> upgrades)
    {
        return false; // By default
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------------------//
    // deriveOnStaffMiddleLine //
    //-------------------------//
    /**
     * Snap ghost symbol vertically on staff middle line.
     *
     * @param inter        the dropped inter
     * @param staff        the related staff, if any
     * @param symbol       the dropped symbol
     * @param sheet        containing sheet
     * @param font         properly sized font
     * @param dropLocation (input/output) current drag/drop location.
     *                     Input: location is assumed to be the symbol focus center
     *                     Output: location may get modified when some snapping occurs
     * @return true if OK
     */
    protected static boolean deriveOnStaffMiddleLine (Inter inter,
                                                      Staff staff,
                                                      ShapeSymbol symbol,
                                                      Sheet sheet,
                                                      MusicFont font,
                                                      Point dropLocation)
    {
        // Get initial bounds
        final Dimension dim = symbol.getDimension(font);
        final Rectangle box = new Rectangle(
                dropLocation.x - (dim.width / 2),
                dropLocation.y - (dim.height / 2),
                dim.width,
                dim.height);
        final Point center = GeoUtil.center(box);

        if (staff != null) {
            if (staff.isTablature()) {
                return false;
            }

            // Snap ordinate on staff middle line
            final double y = staff.pitchToOrdinate(center.x, 0);
            dropLocation.y = (int) Math.rint(y);
            box.y = dropLocation.y - (dim.height / 2);

            inter.setBounds(box);
        }

        return true;
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

    //---------------//
    // increaseGrade //
    //---------------//
    /**
     * Increase the provided intrinsic grade by some ratio.
     *
     * @param grade initial value
     * @param ratio ratio applied
     * @return the increased grade
     */
    public static double increaseGrade (double grade,
                                        double ratio)
    {
        if (grade < Grades.intrinsicRatio) {
            grade += (ratio * (Grades.intrinsicRatio - grade));
        }

        return grade;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------------//
    // JaxbAdapter //
    //-------------//
    /**
     * Meant for JAXB handling of Inter interface.
     */
    public static class JaxbAdapter
            extends XmlAdapter<AbstractInter, Inter>
    {

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
