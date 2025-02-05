//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S l u r I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.math.CubicUtil;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.Versions;
import org.audiveris.omr.sheet.curve.GlyphSlurInfo;
import org.audiveris.omr.sheet.curve.SlurHeadLink;
import org.audiveris.omr.sheet.curve.SlurInfo;
import org.audiveris.omr.sheet.curve.SlurLinker;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sheet.ui.ObjectUIModel;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.SlurSymbol;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Version;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>SlurInter</code> represents a slur interpretation.
 * <p>
 * It can be linked via a <code>SlurHeadRelation</code> to a head on its LEFT and/or RIGHT side,
 * provided that such head belongs to the same system as this slur.
 * <p>
 * It can instead be connected to a counterpart slur in previous or following system via
 * a LEFT and/or RIGHT extension.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "slur")
public class SlurInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SlurInter.class);

    /** To sort slurs vertically within a measure. */
    public static final Comparator<SlurInter> verticalComparator = (s1,
                                                                    s2) -> Double.compare(
                                                                            s1.getCurve().getY1(),
                                                                            s2.getCurve().getY1());

    /**
     * Predicate for a slur not connected on both ends.
     */
    public static final Predicate<SlurInter> isOrphan = (slur) -> {
        for (HorizontalSide side : HorizontalSide.values()) {
            if (slur.getHead(side) == null) {
                return true;
            }
        }

        return false;
    };

    /** Predicate for an orphan slur at the end of its system/part. */
    public static final Predicate<SlurInter> isEndingOrphan = (slur) -> {
        if ((slur.getHead(RIGHT) == null) && (slur.getExtension(RIGHT) == null)) {
            // Check we end in last measure or beyond
            Point2D end = slur.getCurve().getP2();
            SystemInfo system = slur.getSig().getSystem();
            Staff staff1 = system.getClosestStaff(end);
            MeasureStack stack = system.getStackAt(end);

            // Case where the end point is beyond staff right limit
            if (stack == null && end.getX() >= staff1.getAbscissa(RIGHT)) {
                return true;
            }

            if (stack == system.getLastStack()) {
                // Check slur ends in last measure half
                Measure measure = stack.getMeasureAt(staff1);
                int middle = measure.getAbscissa(LEFT, staff1) + (measure.getWidth() / 2);
                if (end.getX() > middle) {
                    return true;
                }
            }
        }
        return false;
    };

    /** Predicate for an orphan slur at the beginning of its system/part. */
    public static final Predicate<SlurInter> isBeginningOrphan = (slur) -> {
        if ((slur.getHead(LEFT) == null) && (slur.getExtension(LEFT) == null)) {
            // Check we are in first measure
            Point2D end = slur.getCurve().getP1();
            SystemInfo system = slur.getSig().getSystem();
            MeasureStack stack = system.getStackAt(end);
            if (stack == system.getFirstStack()) {
                // Check slur ends in first measure half (excluding header area)
                Staff staff1 = system.getClosestStaff(end);
                Measure measure = stack.getMeasureAt(staff1);
                int middle = (staff1.getHeaderStop() + measure.getAbscissa(LEFT, staff1) + measure
                        .getWidth()) / 2;
                if (end.getX() < middle) {
                    return true;
                }
            }
        }
        return false;
    };

    /** Predicate for an extended slur at the end of its system/part. */
    public static final Predicate<SlurInter> isEndingExtended = (slur) -> slur.getExtension(
            RIGHT) != null;

    /** Predicate for an extended slur at the beginning of its system/part. */
    public static final Predicate<SlurInter> isBeginningExtended = (slur) -> slur.getExtension(
            LEFT) != null;

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Is the slur above heads or below heads. */
    @XmlAttribute
    private boolean above;

    /** Is this a tie?. (rather than a plain slur) */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean tie;

    /** The precise Bezier curve. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.CubicAdapter.class)
    private CubicCurve2D curve;

    /** Extension slur on left, if any (within the same sheet). */
    @XmlIDREF
    @XmlAttribute(name = "left-extension")
    private SlurInter leftExtension;

    /** Extension slur on right, if any (within the same sheet). */
    @XmlIDREF
    @XmlAttribute(name = "right-extension")
    private SlurInter rightExtension;

    // Transient data
    //---------------

    /** Physical characteristics. */
    private SlurInfo info;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private SlurInter ()
    {
    }

    /**
     * Creates a new <code>SlurInter</code> object (meant for manual assignment).
     *
     * @param above true for a slur above notes, false for a slur below notes
     * @param grade inter grade
     */
    public SlurInter (boolean above,
                      Double grade)
    {
        super(null, null, above ? Shape.SLUR_ABOVE : Shape.SLUR_BELOW, grade);
    }

    /**
     * Creates a new <code>SlurInter</code> object.
     *
     * @param info    the underlying slur information
     * @param impacts the assignment details
     */
    public SlurInter (SlurInfo info,
                      GradeImpacts impacts)
    {
        super(
                info.getGlyph(),
                null,
                (info.above() == 1) ? Shape.SLUR_ABOVE : Shape.SLUR_BELOW,
                impacts);
        this.info = info;

        above = info.above() == 1;
        curve = info.getCurve();

        // To debug attachments
        for (Entry<String, java.awt.Shape> entry : info.getAttachments().entrySet()) {
            addAttachment(entry.getKey(), entry.getValue());
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

    //-------//
    // added //
    //-------//
    /**
     * Since a slur instance is held by its containing part, make sure part
     * slurs collection is updated.
     *
     * @see #remove(boolean)
     */
    @Override
    public void added ()
    {
        super.added();

        if (getPart() != null) {
            getPart().addSlur(this);
        }

        setAbnormal(true); // No head linked yet
    }

    //-----------//
    // canExtend //
    //-----------//
    /**
     * Check whether two slurs to-be-connected between two systems in sequence are
     * roughly compatible with each other. (same staff index, and similar pitch positions).
     *
     * @param prevSlur the previous slur
     * @return true if found compatible
     */
    public boolean canExtend (SlurInter prevSlur)
    {
        // Retrieve prev staff, using the left note head of the prev slur
        HeadInter prevHead = prevSlur.getHead(LEFT);

        if (prevHead == null) {
            return false;
        }

        Staff prevStaff = prevHead.getStaff();

        // Retrieve this staff, using the right note head of this slur
        HeadInter head = getHead(RIGHT);

        if (head == null) {
            return false;
        }

        Staff thisStaff = head.getStaff();

        // Check that part-based staff indices are the same
        if (prevStaff.getIndexInPart() != thisStaff.getIndexInPart()) {
            logger.debug(
                    "{} prevStaff:{} {} staff:{} different part-based staff indices",
                    prevSlur,
                    prevStaff.getId(),
                    this,
                    thisStaff.getId());

            return false;
        }

        // Retrieve prev position, using the right point of the prev slur
        double prevPp = prevStaff.pitchPositionOf(PointUtil.rounded(prevSlur.getCurve().getP2()));

        // Retrieve position, using the left point of the slur
        double pp = thisStaff.pitchPositionOf(PointUtil.rounded(getCurve().getP1()));

        // Compare pitch positions (very roughly)
        double deltaPitch = pp - prevPp;

        // Beware: maxDeltaY is specified in interlines, and 1 interline = 2 pitches
        boolean res = Math.abs(deltaPitch) <= (constants.maxDeltaY.getValue() * 2);
        logger.debug("{} --- {} deltaPitch:{} res:{}", prevSlur, this, deltaPitch, res);

        return res;
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Being a tie status means it's OK, even perhaps across sheets
        if (isTie()) {
            setAbnormal(false);

            return false;
        }

        // Check if slur is connected (or extended) on both ends
        boolean abnormal = false;

        for (HorizontalSide side : HorizontalSide.values()) {
            if ((this.getHead(side) == null) && (this.getExtension(side) == null)) {
                abnormal = true;

                break;
            }
        }

        setAbnormal(abnormal);

        return isAbnormal();
    }

    //---------------//
    // checkCrossTie //
    //---------------//
    /**
     * Check whether the cross-system slur connection is a tie.
     * <p>
     * This method assumes that 'this' and 'prevSlur' belong to the same logical part but in two
     * systems in sequence:
     * <ul>
     * <li>Either within the same page (during PAGE step),
     * <li>Or from one page/sheet to the next (during score refinement at end of transcription).
     * </ul>
     *
     * @param prevSlur slur at the end of previous system (perhaps in previous sheet)
     */
    public void checkCrossTie (SlurInter prevSlur)
    {
        final HeadInter h1 = prevSlur.getHead(LEFT);
        final HeadInter h2 = this.getHead(RIGHT);
        final boolean result;

        if (!areTieCompatible(h1, h2)) {
            result = false;
        } else {
            // Check staff continuity (we are within the same logical part)
            Staff s1 = h1.getStaff();
            Staff s2 = h2.getStaff();
            result = s1.getIndexInPart() == s2.getIndexInPart();
        }

        prevSlur.setTie(result);
        this.setTie(result);

        if (isVip() || prevSlur.isVip()) {
            logger.info("VIP {} connection {} -> {}", result ? "Tie" : "Slur", prevSlur, this);
        }
    }

    //---------------//
    // checkStaffTie //
    //---------------//
    /**
     * Check whether this slur is a tie within the same staff,
     * with no check for mirror heads potential mirrors.
     *
     * @param systemHeadChords system head chords, not null
     */
    public void checkStaffTie (List<Inter> systemHeadChords)
    {
        if (isVip()) {
            logger.info("VIP checkStaffTie? for {}", this);
        }

        HeadInter h1 = getHead(LEFT);
        HeadInter h2 = getHead(RIGHT);
        boolean result = (h1 != null) && (h2 != null) && (h1.getStaff() == h2.getStaff())
                && areTieCompatible(h1, h2) && isSpaceClear(h1, h2, systemHeadChords);
        setTie(result);

        if (isVip()) {
            logger.info("VIP {} {}", result ? "Tie" : "Slur", this);
        }
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

        // Check ordinate difference between point and (approximate) curve point at same abscissa
        // Nothing to be proud of :-)
        final double y = CubicUtil.yAtX(curve, point.x);
        final double dy = point.y - y;

        return Math.abs(dy) <= constants.maxPointingDy.getValue();
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
        final SlurSymbol slurSymbol = (SlurSymbol) symbol;
        curve = new CubicCurve2D.Double();
        curve.setCurve(slurSymbol.getModel(font, dropLocation).points, 0);
        above = CubicUtil.above(curve) > 0;
        setBounds(null);

        return true;
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

        return new Rectangle(bounds = curve.getBounds());
    }

    //----------//
    // getCurve //
    //----------//
    /**
     * Report the left-to-right Bézier curve which best approximates the slur.
     *
     * @return the Bézier curve
     */
    public CubicCurve2D getCurve ()
    {
        return curve;
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        final StringBuilder sb = new StringBuilder(super.getDetails());

        if (isTie()) {
            sb.append((sb.length() != 0) ? " " : "");
            sb.append("tie");
        }

        if (info != null) {
            sb.append((sb.length() != 0) ? " " : "");
            sb.append(info);
        }

        for (HorizontalSide side : HorizontalSide.values()) {
            SlurInter ext = getExtension(side);

            if (ext != null) {
                sb.append((sb.length() != 0) ? " " : "");
                sb.append(side).append("-extension:").append(ext);
            }
        }

        return sb.toString();
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //--------------//
    // getExtension //
    //--------------//
    /**
     * Report the connected slur, if any, in the other system on the provided side
     * (only within the same sheet).
     *
     * @param side the desired side
     * @return the connected slur, if any, in the other system within the same sheet.
     */
    public SlurInter getExtension (HorizontalSide side)
    {
        Objects.requireNonNull(side, "No side provided for slur getExtension");

        return (side == HorizontalSide.LEFT) ? leftExtension : rightExtension;
    }

    //---------//
    // getHead //
    //---------//
    /**
     * Report the note head, if any, embraced on the specified side.
     *
     * @param side the desired side
     * @return the connected head on this side, if any
     */
    public HeadInter getHead (HorizontalSide side)
    {
        SlurHeadRelation shRel = getHeadRelation(side);

        if (shRel != null) {
            return (HeadInter) sig.getOppositeInter(this, shRel);
        }

        return null;
    }

    //----------------//
    //getHeadRelation //
    //----------------//
    /**
     * Report the relation to note head, if any, on the specified side.
     *
     * @param side the desired side
     * @return the relation found or null
     */
    public SlurHeadRelation getHeadRelation (HorizontalSide side)
    {
        for (Relation rel : sig.getRelations(this, SlurHeadRelation.class)) {
            SlurHeadRelation shRel = (SlurHeadRelation) rel;

            if (shRel.getSide() == side) {
                return shRel;
            }
        }

        return null;
    }

    //---------//
    // getInfo //
    //---------//
    /**
     * Report the related physical information.
     *
     * @return build info
     */
    public SlurInfo getInfo ()
    {
        return info;
    }

    //---------//
    // getPart //
    //---------//
    @Override
    public Part getPart ()
    {
        Part p = super.getPart();

        if (p != null) {
            return p;
        }

        if (sig != null) {
            for (HorizontalSide side : HorizontalSide.values()) {
                HeadInter head = getHead(side);

                if ((head != null) && (head.getPart() != null)) {
                    return part = head.getPart();
                }
            }
        }

        return null;
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    /**
     * We use curve middle point rather than bounds center.
     *
     * @return curve middle point
     */
    @Override
    public Point2D getRelationCenter ()
    {
        return CubicUtil.getMidPoint(curve);
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    @Override
    public Point2D getRelationCenter (Relation relation)
    {
        if (relation instanceof SlurHeadRelation shRel) {
            return switch (shRel.getSide()) {
                case LEFT -> curve.getP1();
                case RIGHT -> curve.getP2();
            };
        } else {
            return getRelationCenter();
        }
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        if (isTie()) {
            for (Relation rel : sig.getRelations(this, SlurHeadRelation.class)) {
                return sig.getOppositeInter(this, rel).getVoice();
            }
        }

        return null;
    }

    //---------//
    // isAbove //
    //---------//
    /**
     * @return the above
     */
    public boolean isAbove ()
    {
        return above;
    }

    //--------------//
    // isSpaceClear //
    //--------------//
    /**
     * Check if the space between leftHead and rightHead is clear of other heads or too
     * many measures.
     * <p>
     * This is meant to allow a tie.
     *
     * @param leftHead         left side head
     * @param rightHead        right side head
     * @param systemHeadChords the set of head chords in system
     * @return true if clear
     */
    public boolean isSpaceClear (HeadInter leftHead,
                                 HeadInter rightHead,
                                 List<Inter> systemHeadChords)
    {
        if ((leftHead == null) || (rightHead == null)) {
            return false;
        }

        if (leftHead.isVip() && rightHead.isVip()) {
            logger.info("VIP isSpaceClear? {} {}", leftHead, rightHead);
        }

        // Check number of measures limits crossed?
        final Part prt = leftHead.getPart();
        final MeasureStack leftStack = prt.getMeasureAt(leftHead.getCenter()).getStack();
        final MeasureStack rightStack = prt.getMeasureAt(rightHead.getCenter()).getStack();
        final int maxDeltaId = constants.maxTieDeltaMeasureID.getValue();

        if ((rightStack.getIdValue() - leftStack.getIdValue()) > maxDeltaId) {
            return false;
        }

        final AbstractChordInter leftChord = leftHead.getChord();
        final AbstractChordInter rightChord = rightHead.getChord();

        final BeamGroupInter leftGroup = leftChord.getBeamGroup();
        final BeamGroupInter rightGroup = rightChord.getBeamGroup();

        // Define a lookup box limited to heads and stems tail ends
        Rectangle box = leftHead.getCoreBounds();
        box.add(rightHead.getCoreBounds());
        box.add(leftChord.getTailLocation());
        box.add(rightChord.getTailLocation());

        List<Inter> found = Inters.intersectedInters(systemHeadChords, null, box);

        // Exclude left & right chords
        found.remove(leftChord);
        found.remove(rightChord);

        // Exclude mirrors if any
        if (leftHead.getMirror() != null) {
            found.remove(leftHead.getMirror().getEnsemble());
        }

        if (rightHead.getMirror() != null) {
            found.remove(rightHead.getMirror().getEnsemble());
        }

        ChordLoop:
        for (Iterator it = found.iterator(); it.hasNext();) {
            AbstractChordInter chord = (AbstractChordInter) it.next();

            // This intersected chord cannot be in the same beam group as left or right chords
            final BeamGroupInter group = chord.getBeamGroup();

            if ((group != null) && ((group == leftGroup) || (group == rightGroup))) {
                logger.debug("Tie forbidden across beamed {}", chord);

                return false;
            }

            // Check invading chord into tie box
            final Rectangle chordBox = chord.getBounds();
            final Rectangle inter = chordBox.intersection(box);
            double invasion = (double) inter.height / box.height;

            if (invasion > constants.maxTieIntersection.getValue()) {
                logger.debug("Tie forbidden across invading {}", chord);

                return false;
            }
        }

        return true;
    }

    //-------//
    // isTie //
    //-------//
    /**
     * Report whether this slur is actually a tie (a slur between same pitched notes).
     *
     * @return true if is a tie, false otherwise
     */
    public boolean isTie ()
    {
        return tie;
    }

    //-------------//
    // lookupLinks //
    //-------------//
    /**
     * Try to detect link between this Slur instance and head on left side
     * plus head on right side.
     *
     * @param systemHeads ordered collection of heads in system
     * @param system      the containing system
     * @param profile     desired profile level (currently ineffective)
     * @return the collection of links found, perhaps null
     */
    private Collection<Link> lookupLinks (List<Inter> systemHeads,
                                          SystemInfo system,
                                          int profile)
    {
        if (systemHeads.isEmpty()) {
            return Collections.emptySet();
        }

        if (isVip()) {
            logger.info("VIP lookupLinks for {}", this);
        }

        SlurLinker slurLinker = new SlurLinker(system.getSheet());

        // Define slur side areas
        Map<HorizontalSide, Area> sideAreas = slurLinker.defineAreaPair(this);

        // Retrieve candidate chords
        Map<HorizontalSide, List<Inter>> chords = new EnumMap<>(HorizontalSide.class);
        List<Inter> systemChords = system.getSig().inters(HeadChordInter.class);

        for (HorizontalSide side : HorizontalSide.values()) {
            Rectangle box = sideAreas.get(side).getBounds();
            chords.put(side, Inters.intersectedInters(systemChords, GeoOrder.NONE, box));
        }

        // Select the best link pair, if any
        Map<HorizontalSide, SlurHeadLink> linkPair = slurLinker.lookupLinkPair(
                this,
                sideAreas,
                system,
                chords);

        if (linkPair == null) {
            return Collections.emptySet();
        }

        final List<Link> links = new ArrayList<>();

        for (Link link : linkPair.values()) {
            if (link != null) {
                links.add(link);
            }
        }

        return links;
    }

    //-----------//
    // preRemove //
    //-----------//
    @Override
    public Set<? extends Inter> preRemove (WrappedBoolean cancel)
    {
        final Set<Inter> inters = new LinkedHashSet<>();

        inters.add(this);

        // If this slur has an extension to another slur, remove that other slur (now an orphan).
        // NOTA: We do this EVEN IF this other slur is a manual slur, because today we have no way
        // to handle the del/undel lifecycle of this direct cross-system inter reference
        // similar to the lifecycle we have for inter relations within the same SIG.
        for (HorizontalSide hSide : HorizontalSide.values()) {
            final SlurInter otherSlur = getExtension(hSide);

            if (otherSlur != null) {
                inters.add(otherSlur);
            }
        }

        return inters;
    }

    //--------//
    // remove //
    //--------//
    /**
     * Since a slur instance is held by its containing part, make sure part
     * slurs collection is updated.
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

        if (part != null) {
            part.removeSlur(this);
        } else {
            logger.info("{} no part to remove from.", this);
        }

        super.remove(extensive);
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final List<Inter> systemHeads = system.getSig().inters(HeadInter.class);
        Collections.sort(systemHeads, Inters.byAbscissa);

        final int profile = Math.max(getProfile(), system.getProfile());

        return lookupLinks(systemHeads, system, profile);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, SlurHeadRelation.class);
    }

    //--------------//
    // setExtension //
    //--------------//
    /**
     * Link the slur to another one, on the provided side.
     *
     * @param side  the provided side
     * @param other the other slur
     */
    public void setExtension (HorizontalSide side,
                              SlurInter other)
    {
        Objects.requireNonNull(side, "No side provided for slur setExtension");
        logger.debug("Slur#{} set {} extension to: {}", getId(), side, other);

        if (side == HorizontalSide.LEFT) {
            leftExtension = other;
        } else {
            rightExtension = other;
        }

        checkAbnormal();
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        if ((glyph != null) && (info == null)) {
            // Slur manually created out of a glyph
            info = GlyphSlurInfo.create(glyph);
            above = info.above() > 0;
            curve = info.getCurve();
        }
    }

    //--------//
    // setTie //
    //--------//
    /**
     * Set this slur as being a tie.
     *
     * @param tie new tie value
     */
    public void setTie (boolean tie)
    {
        if (isVip()) {
            logger.info("VIP {} setTie {}", this, tie);
        }

        if (this.tie != tie) {
            this.tie = tie;

            if (tie) {
                logger.debug("Tie for {}", this);
            }

            checkAbnormal();

            if (sig != null) {
                sig.getSystem().getSheet().getStub().setModified(true);
            }
        }
    }

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    @Override
    public boolean upgradeOldStuff (List<Version> upgrades)
    {
        boolean upgraded = false;

        // Shape SLUR is obsolete, replaced by SLUR_ABOVE or SLUR_BELOW
        if (shape == Shape.SLUR) {
            shape = above ? Shape.SLUR_ABOVE : Shape.SLUR_BELOW;
            upgraded = true;
        }

        if (upgrades.contains(Versions.INTER_GEOMETRY)) {
            // Force bounds recomputation from curve (including control points)
            setBounds(null);
            upgraded = true;
        }

        return upgraded;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // areTieCompatible //
    //------------------//
    /**
     * Check whether two heads represent the same height
     * (same octave, same step, same key fifths)
     * but accidental alteration is <b>NOT</b> considered.
     * <p>
     * Potential accidental alterations must be taken care of by the caller, if so needed.
     *
     * @param h1 first head
     * @param h2 second head, down the score
     * @return true if the heads are equivalent.
     */
    private static boolean areTieCompatible (HeadInter h1,
                                             HeadInter h2)
    {
        if ((h1 == null) || (h2 == null)) {
            return false;
        }

        // NoteStep
        if (h1.getStep() != h2.getStep()) {
            return false;
        }

        // Octave
        if (h1.getOctave() != h2.getOctave()) {
            return false;
        }

        // Accidental
        AlterInter a1 = h1.getMeasureAccidental();
        AlterInter a2 = h2.getMeasureAccidental();

        if (a1 == null) {
            if (a2 != null) {
                return false;
            }
        } else if ((a2 != null) && (a2.getShape() != a1.getShape())) {
            return false;
        }

        // Let's caller handle staff / part / system compatibility
        return true;
    }

    //----------------//
    // discardOrphans //
    //----------------//
    /**
     * Discard every orphan left over, unless it's a manual one.
     *
     * @param orphans the orphan slurs left over
     * @param side    side of missing connection
     */
    public static void discardOrphans (List<SlurInter> orphans,
                                       HorizontalSide side)
    {
        for (SlurInter slur : orphans) {
            if (slur.isVip()) {
                logger.info("VIP could not {}-connect {}", side, slur);
            }

            if (!slur.isManual()) {
                slur.remove();
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction maxDeltaY = new Scale.Fraction(
                4,
                "Maximum vertical difference in interlines between connecting slurs");

        private final Constant.Integer maxTieDeltaMeasureID = new Constant.Integer(
                "none",
                1,
                "Maximum delta in measure ID when setting a tie");

        private final Constant.Ratio maxTieIntersection = new Constant.Ratio(
                0.25,
                "Maximum intersection for a chord invading a tie");

        private final Constant.Double maxPointingDy = new Constant.Double(
                "pixels",
                2,
                "Maximum ordinate distance for pointing a slur");
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a slur.
     * <p>
     * For a slur editor, there are 6 handles:
     * <ul>
     * <li>Left point, middle of left and right points, right point.
     * <li>Left control point, middle of left and right control points, right control point,
     * </ul>
     */
    private static class Editor
            extends InterEditor
    {
        private final Model originalModel;

        private final Model model;

        public Editor (SlurInter slur)
        {
            super(slur);

            final CubicCurve2D curve = slur.getCurve();

            originalModel = new Model(curve);
            model = new Model(curve);

            final Point2D middle = PointUtil.middle(model.p1, model.p2);
            final Point2D midC = PointUtil.middle(model.c1, model.c2);

            handles.add(new Handle(model.p1)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    PointUtil.add(model.p1, dx, dy);
                    PointUtil.add(middle, dx / 2.0, dy / 2.0);

                    PointUtil.add(model.c1, dx, dy); // Move ctrl point like end point
                    midC.setLocation(PointUtil.middle(model.c1, model.c2));

                    return true;
                }
            });

            handles.add(selectedHandle = new Handle(middle)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    for (Handle handle : handles) {
                        PointUtil.add(handle.getPoint(), dx, dy);
                    }

                    return true;
                }
            });

            handles.add(new Handle(model.p2)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    PointUtil.add(model.p2, dx, dy);
                    PointUtil.add(middle, dx / 2.0, dy / 2.0);

                    PointUtil.add(model.c2, dx, dy); // Move ctrl point like end point
                    midC.setLocation(PointUtil.middle(model.c1, model.c2));

                    return true;
                }
            });

            handles.add(new Handle(model.c2)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    PointUtil.add(model.c2, dx, dy);
                    PointUtil.add(midC, dx / 2.0, dy / 2.0);

                    return true;
                }
            });

            handles.add(new Handle(midC)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    PointUtil.add(midC, dx, dy);
                    PointUtil.add(model.c1, dx, dy);
                    PointUtil.add(model.c2, dx, dy);

                    return true;
                }
            });

            handles.add(new Handle(model.c1)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    PointUtil.add(model.c1, dx, dy);
                    PointUtil.add(midC, dx / 2.0, dy / 2.0);

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            final SlurInter slur = (SlurInter) getInter();
            slur.getCurve().setCurve(model.points, 0);

            slur.setBounds(null);
            super.doit(); // No more glyph
        }

        @Override
        public void render (Graphics2D g)
        {
            // First, draw lines between handles
            if (!handles.isEmpty()) {
                g.setColor(Colors.EDITING_LINE);
                UIUtil.setAbsoluteStroke(g, 1f);

                Point2D last = null;

                for (Handle handle : handles) {
                    Point2D p = handle.getPoint();

                    if (last != null) {
                        g.draw(new Line2D.Double(last, p));
                    }

                    last = p;
                }

                // Close path?
                if (handles.size() > 2) {
                    g.draw(new Line2D.Double(last, handles.get(0).getPoint()));
                }
            }

            // Second, draw inter and handles themselves
            super.render(g);
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append('{');
            sb.append(getInter());
            sb.append(" org:").append(PointUtil.toString(originalModel.p1)).append('-').append(
                    PointUtil.toString(originalModel.p2));
            sb.append(" new:").append(PointUtil.toString(model.p1)).append('-').append(
                    PointUtil.toString(model.p2));
            sb.append('}');

            return sb.toString();
        }

        @Override
        public void undo ()
        {
            final SlurInter slur = (SlurInter) getInter();
            slur.getCurve().setCurve(originalModel.points, 0);

            slur.setBounds(null);
            super.undo();
        }
    }

    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends GradeImpacts
    {
        private static final String[] NAMES = new String[]
        { "dist", "angle", "width", "height", "vert" };

        private static final double[] WEIGHTS = new double[]
        { 3, 1, 1, 1, 1 };

        public Impacts (double dist,
                        double angle,
                        double width,
                        double height,
                        double vert)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, dist);
            setImpact(1, angle);
            setImpact(2, width);
            setImpact(3, height);
            setImpact(4, vert);
        }
    }

    //-------//
    // Model //
    //-------//
    public static class Model
            implements ObjectUIModel
    {
        public final Point2D p1;

        public final Point2D c1;

        public final Point2D c2;

        public final Point2D p2;

        // Array of 4 points used to update cubic curve via setCurve() method
        public final Point2D[] points;

        public Model (CubicCurve2D curve)
        {
            p1 = curve.getP1();
            c1 = curve.getCtrlP1();
            c2 = curve.getCtrlP2();
            p2 = curve.getP2();
            points = new Point2D[]
            { p1, c1, c2, p2 };
        }

        @Override
        public void translate (double dx,
                               double dy)
        {
            for (Point2D pt : points) {
                PointUtil.add(pt, dx, dy);
            }
        }
    }
}
