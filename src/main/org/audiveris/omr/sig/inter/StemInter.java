//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S t e m I n t e r                                      //
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.math.LineUtil;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.Versions;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.AbstractStemConnection;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.ChordStemRelation;
import org.audiveris.omr.sig.relation.Exclusion;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.HeadHeadRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.StemPortion;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_BOTTOM;
import static org.audiveris.omr.sig.relation.StemPortion.STEM_TOP;
import org.audiveris.omr.sig.relation.TremoloStemRelation;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Version;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.BOTTOM;
import static org.audiveris.omr.util.VerticalSide.TOP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.process.ByteProcessor;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>StemInter</code> represents Stem interpretations.
 * <p>
 * Main relations around a stem:
 * <ul>
 * <li>{@link BeamStemRelation}
 * <li>{@link FlagStemRelation}
 * <li>{@link TremoloStemRelation}
 * <li>{@link HeadStemRelation}
 * <li>{@link ChordStemRelation} for the containing chord (generally one)
 * </ul>
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "stem")
public class StemInter
        extends AbstractVerticalInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StemInter.class);

    /** Default thickness of a stem. */
    public static final double DEFAULT_THICKNESS = constants.defaultThickness.getValue();

    /** Anchor vertical margin, relative to head height. */
    private static final double ANCHOR_MARGIN_RATIO = constants.anchorMarginRatio.getValue();

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /**
     * <strike>Upper point of stem</strike>
     * <p>
     * <b>Deprecated</b>, replaced by median line inherited from super class.
     */
    @Deprecated
    @XmlElement(name = "top")
    @XmlJavaTypeAdapter(Jaxb.Point2DAdapter.class)
    private Point2D oldTop;

    /**
     * <strike>Lower point of stem</strike>
     * <p>
     * <b>Deprecated</b>, replaced by median line inherited from super class.
     */
    @Deprecated
    @XmlElement(name = "bottom")
    @XmlJavaTypeAdapter(Jaxb.Point2DAdapter.class)
    private Point2D oldBottom;

    //~ Constructors -------------------------------------------------------------------------------

    protected StemInter ()
    {
        super(null, Shape.STEM, 0.0);
    }

    /**
     * Creates a new StemInter object.
     *
     * @param glyph the underlying glyph
     * @param grade the assigned grade
     */
    public StemInter (Glyph glyph,
                      Double grade)
    {
        super(glyph, Shape.STEM, grade);
    }

    /**
     * Creates a new StemInter object.
     *
     * @param glyph   the underlying glyph
     * @param impacts the grade details
     */
    public StemInter (Glyph glyph,
                      GradeImpacts impacts)
    {
        super(glyph, Shape.STEM, impacts);
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

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        setAbnormal(true); // No head linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if a head is connected
        setAbnormal(!sig.hasRelation(this, HeadStemRelation.class));

        return isAbnormal();
    }

    //---------------------//
    // computeAnchoredLine //
    //---------------------//
    /**
     * Compute the line between extreme anchors, assuming that wrong-side ending heads
     * have been disconnected.
     *
     * @return the anchor line
     */
    public Line2D computeAnchoredLine ()
    {
        if (isRemoved()) {
            return null;
        }

        final Set<Relation> links = sig.getRelations(this, HeadStemRelation.class);

        if (!links.isEmpty()) {
            int dir = computeDirection();

            if (dir > 0) {
                // Stem down, heads are at top of stem
                double yAnchor = Double.MAX_VALUE;

                for (Relation rel : links) {
                    HeadInter head = (HeadInter) sig.getEdgeSource(rel);
                    Rectangle headBox = head.bounds;
                    double y = headBox.y - (ANCHOR_MARGIN_RATIO * headBox.height);

                    if (y < yAnchor) {
                        yAnchor = y;
                    }
                }

                if (yAnchor > median.getY1()) {
                    return new Line2D.Double(
                            new Point2D.Double(median.getX1(), yAnchor),
                            median.getP2());
                }
            } else if (dir < 0) {
                // Stem up, heads are at bottom of stem
                double yAnchor = Double.MIN_VALUE;

                for (Relation rel : links) {
                    HeadInter head = (HeadInter) sig.getEdgeSource(rel);
                    Rectangle headBox = head.bounds;
                    double y = headBox.y + ((1 - ANCHOR_MARGIN_RATIO) * headBox.height);

                    if (y > yAnchor) {
                        yAnchor = y;
                    }
                }

                if (yAnchor < median.getY2()) {
                    return new Line2D.Double(
                            median.getP1(),
                            new Point2D.Double(median.getX2(), yAnchor));
                }
            }
        }

        // No change
        return new Line2D.Double(median.getX1(), median.getY1(), median.getX2(), median.getY2());
    }

    //------------------//
    // computeDirection //
    //------------------//
    /**
     * Report the direction (from head to tail) of this stem, compliant with standard
     * display y orientation (-1 for stem up, +1 for stem down, 0 for unknown).
     * <p>
     * For this, we check what is found on each stem end (is it a tail: beam/flag or is it a head)
     * and use contextual grade to pick up the best reference.
     * <p>
     * Potential tremolos don't give orientation information, hence they are not checked.
     *
     * @return the stem direction
     */
    public int computeDirection ()
    {
        Scale scale = sig.getSystem().getSheet().getScale();
        final Line2D stemLine = computeExtendedLine();
        final List<Relation> links = new ArrayList<>(
                sig.getRelations(this, AbstractStemConnection.class));
        sig.sortBySource(links);

        for (Relation rel : links) {
            // Source is a head, a beam, a tremolo or a flag
            final Inter source = sig.getEdgeSource(rel);

            // Retrieve the stem portion for this link
            if (rel instanceof HeadStemRelation link) {
                // Head -> Stem
                final Shape headShape = source.getShape();
                final StemPortion portion = link.getStemPortion(source, stemLine, scale);

                if (portion == STEM_BOTTOM) {
                    if ((link.getHeadSide() == RIGHT) || headShape.isPercussion()) {
                        return -1;
                    }
                } else if (portion == STEM_TOP) {
                    if ((link.getHeadSide() == LEFT) || headShape.isPercussion()) {
                        return 1;
                    }
                }
            } else if (rel instanceof BeamStemRelation link) {
                // Beam -> Stem
                final StemPortion portion = link.getStemPortion(source, stemLine, scale);
                return (portion == STEM_TOP) ? (-1) : 1;
            } else if (rel instanceof FlagStemRelation link) {
                // Flag -> Stem
                final StemPortion portion = link.getStemPortion(source, stemLine, scale);

                if (portion == STEM_TOP) {
                    return -1;
                } else if (portion == STEM_BOTTOM) {
                    return 1;
                }
            } else {
                // Perhaps a Tremolo -> Stem, to be ignored
            }
        }

        return 0; // Cannot decide with current config!
    }

    //---------------------//
    // computeExtendedLine //
    //---------------------//
    /**
     * Compute the extended line, taking all stem connections into account.
     *
     * @return the connection range
     */
    public Line2D computeExtendedLine ()
    {
        if (isRemoved()) {
            return null;
        }

        Point2D extTop = new Point2D.Double(median.getX1(), median.getY1());
        Point2D extBottom = new Point2D.Double(median.getX2(), median.getY2());

        for (Relation rel : sig.getRelations(this, AbstractStemConnection.class)) {
            AbstractStemConnection link = (AbstractStemConnection) rel;
            Point2D ext = link.getExtensionPoint();

            if (ext != null) {
                if (ext.getY() < extTop.getY()) {
                    extTop = ext;
                }

                if (ext.getY() > extBottom.getY()) {
                    extBottom = ext;
                }
            }
        }

        return new Line2D.Double(extTop, extBottom);
    }

    //---------------------//
    // computeExtendedLine //
    //---------------------//
    /**
     * Compute the extended line, taking only into account the stem connections from
     * heads compatible with the provided head.
     *
     * @param head the provided head
     * @return the connection range
     */
    public Line2D computeExtendedLine (HeadInter head)
    {
        if (isRemoved()) {
            return null;
        }

        // Heads in exclusion
        final Set<Inter> excluded = new LinkedHashSet<>();
        for (Relation rel : sig.getRelations(head, Exclusion.class)) {
            excluded.add(sig.getOppositeInter(head, rel));
        }

        Point2D extTop = new Point2D.Double(median.getX1(), median.getY1());
        Point2D extBottom = new Point2D.Double(median.getX2(), median.getY2());

        for (Relation rel : sig.getRelations(this, AbstractStemConnection.class)) {
            final AbstractStemConnection link = (AbstractStemConnection) rel;
            final Inter otherHead = sig.getOppositeInter(this, rel);

            if (excluded.contains(otherHead)) {
                continue;
            }

            final Point2D ext = link.getExtensionPoint();

            if (ext != null) {
                if (ext.getY() < extTop.getY()) {
                    extTop = ext;
                }

                if (ext.getY() > extBottom.getY()) {
                    extBottom = ext;
                }
            }
        }

        return new Line2D.Double(extTop, extBottom);
    }

    //----------------//
    // extractSubStem //
    //----------------//
    /**
     * Build a new stem from a portion of this one (extrema ordinates can be provided
     * in any order).
     *
     * @param y1 ordinate of one side of sub-stem
     * @param y2 ordinate of the other side of sub-stem
     * @return the extracted sub-stem inter
     */
    public StemInter extractSubStem (int y1,
                                     int y2)
    {
        final int yTop = Math.min(y1, y2);
        final int yBottom = Math.max(y1, y2);

        final Sheet sheet = sig.getSystem().getSheet();
        final ByteProcessor buffer = glyph.getRunTable().getBuffer();

        // ROI definition (WRT stem buffer coordinates)
        final Rectangle roi = new Rectangle(
                0,
                yTop - glyph.getTop(),
                glyph.getWidth(),
                yBottom - yTop + 1);

        // Create sub-glyph
        final Point stemOffset = new Point();
        final RunTableFactory factory = new RunTableFactory(VERTICAL);
        final RunTable table = factory.createTable(buffer, roi).trim(stemOffset);
        final int x = glyph.getLeft() + stemOffset.x;
        final int y = glyph.getTop() + roi.y + stemOffset.y;
        final Glyph g = sheet.getGlyphIndex().registerOriginal(new Glyph(x, y, table));

        // Create sub-stem
        final StemInter subStem = new StemInter(g, getGrade());
        sig.addVertex(subStem);

        return subStem;
    }

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the beams linked to this stem.
     *
     * @return set of linked beams
     */
    public Set<AbstractBeamInter> getBeams ()
    {
        final Set<AbstractBeamInter> set = new LinkedHashSet<>();

        if (!isRemoved()) {
            for (Relation relation : sig.getRelations(this, BeamStemRelation.class)) {
                set.add((AbstractBeamInter) sig.getEdgeSource(relation));
            }
        }

        return set;
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the chord(s) currently attached to the provided stem.
     * <p>
     * We can have:
     * <ul>
     * <li>No chord found, simply because this stem has not yet been processed.</li>
     * <li>One chord found, this is the normal case.</li>
     * <li>Two chords found, when the same stem is "shared" by two chords (as in complex structures
     * like in Dichterliebe example, part 2, page 2, measure 14).</li>
     * </ul>
     *
     * @return the perhaps empty collection of chords found for this stem
     */
    public List<HeadChordInter> getChords ()
    {
        List<HeadChordInter> chords = null;

        if (!isRemoved()) {
            for (Relation rel : sig.getRelations(this, ChordStemRelation.class)) {
                if (chords == null) {
                    chords = new ArrayList<>();
                }

                chords.add((HeadChordInter) sig.getOppositeInter(this, rel));
            }
        }

        if (chords == null) {
            return Collections.emptyList();
        }

        return chords;
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //---------------//
    // getFreeLength //
    //---------------//
    /**
     * Report the length of stem tail portion when not linked to a beam.
     *
     * @return length of tail portion without heads, null if a beam is involved
     */
    public Integer getFreeLength ()
    {
        if (sig.hasRelation(this, BeamStemRelation.class)) {
            return null;
        }

        final List<HeadInter> heads = new ArrayList<>(getHeads());

        if (heads.isEmpty()) {
            return null;
        }

        Collections.sort(heads, Inters.byCenterOrdinate); // Top down
        final int vDir = computeDirection(); // Head to tail
        final VerticalSide vSide = vDir < 0 ? TOP : BOTTOM;
        final HeadInter lastHead = (vDir < 0) ? heads.get(0) : heads.get(heads.size() - 1);
        final Relation rel = sig.getRelation(lastHead, this, HeadStemRelation.class);
        final HeadStemRelation hsRel = (HeadStemRelation) rel;
        final HorizontalSide headSide = hsRel.getHeadSide();
        final Point2D refPt = lastHead.getStemReferencePoint(headSide, vSide.opposite());
        final Point2D stemEnd = (vDir < 0) ? median.getP1() : median.getP2();

        return (int) Math.rint(Math.abs(stemEnd.getY() - refPt.getY()));
    }

    //----------//
    // getHeads //
    //----------//
    /**
     * Report the heads linked to this stem, whatever the side.
     *
     * @return set of linked heads
     */
    public Set<HeadInter> getHeads ()
    {
        final Set<HeadInter> set = new LinkedHashSet<>();

        if (!isRemoved()) {
            for (Relation relation : sig.getRelations(this, HeadStemRelation.class)) {
                set.add((HeadInter) sig.getEdgeSource(relation));
            }
        }

        return set;
    }

    //-----------------//
    // getReliableLine //
    //-----------------//
    private Line2D getReliableLine ()
    {
        // Extrapolate stem line or use a "vertical" through its center abscissa?
        final Sheet sheet = getSig().getSystem().getSheet();
        final Scale scale = sheet.getScale();

        if (getBounds().height >= scale.toPixels(constants.minLengthForSlope)) {
            return getMedian();
        } else {
            return sheet.getSkew().skewedVertical(getCenter());
        }
    }

    //----------//
    // getStaff //
    //----------//
    @Override
    public Staff getStaff ()
    {
        if (staff != null) {
            return staff;
        }

        // Check related chord(s)
        Staff stemStaff = null;

        for (HeadChordInter chord : getChords()) {
            Staff chordStaff = chord.getStaff();

            if (chordStaff == null) {
                return null;
            }

            if (stemStaff != null && stemStaff != chordStaff) {
                return null;
            }

            stemStaff = chordStaff;
        }

        return staff = stemStaff;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        if (!isRemoved()) {
            for (Relation rel : sig.getRelations(this, HeadStemRelation.class)) {
                return sig.getOppositeInter(this, rel).getVoice();
            }
        }

        return null;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report item width.
     *
     * @return the width
     */
    @Override
    public Double getWidth ()
    {
        if (width != null) {
            return width;
        }

        return DEFAULT_THICKNESS;
    }

    //-------------//
    // isGraceStem //
    //-------------//
    /**
     * Report whether this stem is linked to grace note heads (rather than standard heads)
     *
     * @return true if connected head is small
     */
    public boolean isGraceStem ()
    {
        if (!isRemoved()) {
            for (Relation rel : sig.getRelations(this, HeadStemRelation.class)) {
                Shape headShape = sig.getOppositeInter(this, rel).getShape();

                // First head tested is enough.
                return (headShape == Shape.NOTEHEAD_BLACK_SMALL)
                        || (headShape == Shape.NOTEHEAD_VOID_SMALL);
            }
        }

        return false;
    }

    //-----------------//
    // lookupBeamLinks //
    //-----------------//
    /**
     * Look for links to beams nearby.
     *
     * @param system  the containing system
     * @param profile the profile to use
     * @return collection of links, perhaps empty
     */
    public Collection<Link> lookupBeamLinks (SystemInfo system,
                                             int profile)
    {
        Collection<Link> links = new LinkedHashSet<>();

        // Look for beams around the stem
        final Scale scale = system.getSheet().getScale();
        final int maxBeamOutDx = scale.toPixels(BeamStemRelation.getXOutGapMaximum(profile));
        final int maxYGap = scale.toPixels(BeamStemRelation.getYGapMaximum(profile));
        final Rectangle luBox = getBounds();
        luBox.grow(maxBeamOutDx, maxYGap);

        final Set<Inter> beams = new LinkedHashSet<>(
                system.getSig().inters(
                        (Inter inter) -> !inter.isRemoved() && inter instanceof AbstractBeamInter
                                && inter.getBounds().intersects(luBox)));

        // Include also the beams already connected to the stem
        if (sig != null) {
            beams.addAll(getBeams());
        }

        // Now, keep only beams that would still link to this stem
        final Line2D stemMedian = getMedian();
        final double yStem = getCenter().y;

        for (Inter inter : beams) {
            final AbstractBeamInter beam = (AbstractBeamInter) inter;
            final Point2D crossPt = LineUtil.intersection(stemMedian, beam.getMedian());
            final VerticalSide vSide = (crossPt.getY() < yStem) ? TOP : BOTTOM;
            final Link link = BeamStemRelation.checkLink(beam, this, vSide, scale, profile);

            // BeamStemRelation link is implemented from beam to stem, hence we have to reverse it
            if (link != null) {
                links.add(link.reverse(beam));
            }
        }

        return links;
    }

    //-----------------//
    // lookupHeadLinks //
    //-----------------//
    /**
     * Look for links to heads nearby that need a stem relation.
     *
     * @return collection of links, perhaps empty
     */
    private Collection<Link> lookupHeadLinks (SystemInfo system,
                                              int profile)
    {
        Collection<Link> links = null;

        // Search for non-linked heads in a lookup area around the stem
        final Scale scale = system.getSheet().getScale();
        final int maxHeadOutDx = scale.toPixels(HeadStemRelation.getXOutGapMaximum(profile));
        final int maxYGap = scale.toPixels(HeadStemRelation.getYGapMaximum(profile));
        final Rectangle luBox = getBounds();
        luBox.grow(maxHeadOutDx, maxYGap);

        // @formatter:off
        final Set<Inter> heads = new LinkedHashSet<>(
                system.getSig().inters(
                        (inter) -> !inter.isRemoved()
                                   && ShapeSet.StemHeads.contains(inter.getShape())
                                   && inter.getBounds().intersects(luBox)
                                   && ((HeadInter) inter).getStems().isEmpty()));
        // @formatter:on

        // Include also the heads already connected to the stem
        if (sig != null) {
            heads.addAll(getHeads());
        }

        // Now, keep only heads that would still link to this stem
        final List<Inter> thisStem = new ArrayList<>();
        thisStem.add(this);

        for (Inter inter : heads) {
            HeadInter head = (HeadInter) inter;
            Link link = head.lookupLink(thisStem, system, profile);

            if ((link != null) && (link.partner == this)) {
                if (links == null) {
                    links = new ArrayList<>();
                }

                // Use link reverse
                Link rev = new Link(head, link.relation, false);
                links.add(rev);
            }
        }

        // TODO: solve conflicts if any (perhaps using preferred corners & relation.grade)
        return (links == null) ? Collections.emptyList() : links;
    }

    //---------------//
    // refineHeadEnd //
    //---------------//
    /**
     * Using the last head on stem, refine the precise location of stem head end.
     */
    public void refineHeadEnd ()
    {
        final List<HeadInter> heads = new ArrayList<>(getHeads());

        if (heads.isEmpty()) {
            return;
        }

        Collections.sort(heads, Inters.byCenterOrdinate);
        final int dir = computeDirection(); // From head to tail
        final VerticalSide vSide = (dir > 0) ? BOTTOM : TOP;
        final HeadInter head = (dir > 0) ? heads.get(0) : heads.get(heads.size() - 1);
        final HeadStemRelation rel = (HeadStemRelation) sig.getRelation(
                head,
                this,
                HeadStemRelation.class);
        final HorizontalSide hSide = rel.getHeadSide();
        final double yRef = head.getStemReferencePoint(hSide, vSide).getY();

        final Line2D longLine = getReliableLine();
        final Point2D crossPt = LineUtil.intersectionAtY(longLine, yRef);

        if (dir > 0) {
            setMedian(crossPt, getMedian().getP2());
        } else {
            setMedian(getMedian().getP1(), crossPt);
        }
    }

    //---------------//
    // refineTailEnd //
    //---------------//
    /**
     * Using the last beam if any on stem, refine the precise location of stem tail end.
     */
    public void refineTailEnd ()
    {
        final List<AbstractBeamInter> beams = new ArrayList<>(getBeams());

        if (beams.isEmpty()) {
            return;
        }

        // Sort beams top down
        final int x = getCenter().x;
        Collections.sort(
                beams,
                (AbstractBeamInter b1,
                 AbstractBeamInter b2) -> Double.compare(
                         LineUtil.yAtX(b1.getMedian(), x),
                         LineUtil.yAtX(b2.getMedian(), x)));

        final int dir = computeDirection(); // From head to tail
        final AbstractBeamInter beam = (dir < 0) ? beams.get(0) : beams.get(beams.size() - 1);
        final Line2D border = beam.getBorder(VerticalSide.of(dir));

        final Line2D longLine = getReliableLine();
        final Point2D crossPt = LineUtil.intersection(border, longLine);

        if (dir < 0) {
            setMedian(crossPt, getMedian().getP2());
        } else {
            setMedian(getMedian().getP1(), crossPt);
        }
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove head-head relations that were based on this stem.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        if (isRemoved()) {
            return;
        }

        if (isGood()) {
            // Discard head-head relations that are based only on this stem instance
            Set<HeadInter> stemHeads = getHeads(); // Heads linked to this stem

            for (HeadInter head : stemHeads) {
                // Other stems this head is linked to
                Set<StemInter> otherStems = head.getStems();
                otherStems.remove(this);

                for (Relation rel : sig.getRelations(head, HeadHeadRelation.class)) {
                    HeadInter similarHead = (HeadInter) sig.getOppositeInter(head, rel);

                    if (stemHeads.contains(similarHead)) {
                        // Head - otherHead are both on this stem
                        // Keep HH support only if they are on same good stem (different of this)
                        Set<StemInter> similarStems = similarHead.getStems();
                        similarStems.retainAll(otherStems);

                        if (!Inters.hasGoodMember(similarStems)) {
                            logger.debug("Removing head-head within {} & {}", head, similarHead);
                            sig.removeEdge(rel);
                        }
                    }
                }
            }
        }

        super.remove(extensive);
    }

    //-------------//
    // searchLinks //
    //-------------//
    /**
     * {@inheritDoc}
     * <p>
     * Specifically, look for heads and beams nearby that need a stem relation.
     *
     * @return collection of links, perhaps empty
     */
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final int profile = Math.max(getProfile(), system.getProfile());
        List<Link> allLinks = new ArrayList<>();
        allLinks.addAll(lookupHeadLinks(system, profile));
        allLinks.addAll(lookupBeamLinks(system, profile));

        return allLinks;
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, HeadStemRelation.class, BeamStemRelation.class);
    }

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    @Override
    public boolean upgradeOldStuff (List<Version> upgrades)
    {
        boolean upgraded = false;

        if (upgrades.contains(Versions.INTER_GEOMETRY)) {
            if (width == null) {
                if (glyph != null) {
                    width = glyph.getMeanThickness(VERTICAL);
                } else {
                    width = DEFAULT_THICKNESS;
                }

                upgraded = true;
            }

            if ((oldTop != null) && (oldBottom != null)) {
                // These old integer points were min/max points INSIDE the stem
                setMedian(
                        new Point2D.Double(oldTop.getX() + 0.5, oldTop.getY()),
                        new Point2D.Double(oldBottom.getX() + 0.5, oldBottom.getY() + 1));
                oldTop = oldBottom = null;
                upgraded = true;
            }

            if (upgraded) {
                computeArea();
            }
        }

        return upgraded;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //----------------//
    // bestTailLength //
    //----------------//
    public static Scale.Fraction bestTailLength ()
    {
        return constants.bestTailLength;
    }

    //-------------//
    // getMinGrade //
    //-------------//
    /**
     * Report the minimum acceptable grade
     *
     * @return minimum grade
     */
    public static double getMinGrade ()
    {
        return AbstractInter.getMinGrade();
    }

    //---------------//
    // minTailLength //
    //---------------//
    public static Scale.Fraction minTailLength ()
    {
        return constants.minTailLength;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Double defaultThickness = new Constant.Double(
                "pixels",
                3.0,
                "Default stem width");

        private final Constant.Ratio anchorMarginRatio = new Constant.Ratio(
                0.67,
                "Anchor vertical margin, relative to head height");

        private final Scale.Fraction minLengthForSlope = new Scale.Fraction(
                1.0,
                "Minimum stem length for a reliable slope");

        private final Scale.Fraction minTailLength = new Scale.Fraction(
                1.75,
                "Minimum stem tail length (after last head)");

        private final Scale.Fraction bestTailLength = new Scale.Fraction(
                2.5,
                "Optimal stem tail length (after last head)");
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a stem.
     * <p>
     * See {@link AbstractVerticalInter.Editor}
     */
    private static class Editor
            extends AbstractVerticalInter.Editor
    {

        public Editor (StemInter stem)
        {
            super(stem, true /* full */);
        }

        /**
         * Invalidate internal data of all chords that use this stem.
         */
        @Override
        protected void updateChords ()
        {
            final StemInter stem = (StemInter) object;

            for (AbstractChordInter chord : stem.getChords()) {
                chord.invalidateCache();
            }
        }
    }
}
