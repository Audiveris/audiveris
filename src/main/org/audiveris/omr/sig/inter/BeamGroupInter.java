//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B e a m G r o u p I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.BeamBeamRelation;
import org.audiveris.omr.sig.relation.BeamRestRelation;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.NextInVoiceRelation;
import org.audiveris.omr.sig.relation.NoExclusion;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SameVoiceRelation;
import org.audiveris.omr.sig.relation.SeparateVoiceRelation;
import org.audiveris.omr.sig.relation.StemAlignmentRelation;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sig.relation.Containment;

/**
 * Class <code>BeamGroupInter</code> represents a group of related beams.
 * <p>
 * It is an <code>InterEnsemble</code> linked via <code>Containment</code> relations to its Beam
 * members.
 * <p>
 * Beams in a BeamGroupInter are in no particular order.
 * They support each other via BeamBeamRelation.
 * <p>
 * This class replaces the (old)BeamGroup class that was not an inter and had to directly manage its
 * contained beams.
 * <p>
 * @since 5.2
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "beam-group")
public class BeamGroupInter
        extends AbstractInter
        implements InterEnsemble
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BeamGroupInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /**
     * Indicates if this beam group relates to more than one staff.
     */
    @XmlAttribute(name = "multi-staff")
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean multiStaff;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new instance of BeamGroup.
     */
    public BeamGroupInter ()
    {
        setGrade(1.0);
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

    //-----------//
    // addMember //
    //-----------//
    @Override
    public void addMember (Inter member)
    {
        if (!(member instanceof AbstractBeamInter)) {
            throw new IllegalArgumentException(
                    "Only AbstractBeamInter can be added to BeamGroupInter");
        }

        EnsembleHelper.addMember(this, member);

        // Mutual beam support within group
        for (Inter beam : getMembers()) {
            if (beam != member) {
                sig.insertSupport(member, beam, BeamBeamRelation.class);
            }
        }
    }

    //----------------//
    // canBeNeighbors //
    //----------------//
    /**
     * Check whether the two provided beams can belong to the same group, one directly
     * above or below the other.
     *
     * @param one   a beam candidate
     * @param two   another beam candidate
     * @param scale the sheet or staff scale
     * @return true if they can be direct neighbors in a group
     */
    public static boolean canBeNeighbors (AbstractBeamInter one,
                                          AbstractBeamInter two,
                                          Scale scale)
    {
        return canBeNeighbors(one,
                              two,
                              scale.toPixelsDouble(constants.minXOverlap),
                              scale.toPixelsDouble(constants.maxYDistance),
                              constants.maxSlopeDiff.getValue());
    }

    //----------------//
    // canBeNeighbors //
    //----------------//
    /**
     * Check whether the two provided beams can belong to the same group, one directly
     * above or below the other.
     *
     * @param one          a beam candidate
     * @param two          another beam candidate
     * @param minXOverlap  minimum horizontal overlap between candidates median lines
     * @param maxYDistance maximum vertical distance between candidates median lines
     * @param maxSlopeDiff maximum slope difference between candidates median lines
     * @return true if they can be direct neighbors in a group
     */
    public static boolean canBeNeighbors (AbstractBeamInter one,
                                          AbstractBeamInter two,
                                          double minXOverlap,
                                          double maxYDistance,
                                          double maxSlopeDiff)
    {
        if (one.isVip() && two.isVip()) {
            logger.info("VIP canBeNeighbors? {} {}", one, two);
        }

        final Line2D m1 = one.getMedian();
        final Line2D m2 = two.getMedian();

        // Check min x overlap
        final double maxLeft = Math.max(m1.getX1(), m2.getX1());
        final double minRight = Math.min(m1.getX2(), m2.getX2());
        final double xOverlap = minRight - maxLeft;

        if (xOverlap < minXOverlap) {
            return false;
        }

        // Measure vertical distance at middle of x overlap
        final double x = (maxLeft + minRight) / 2;
        final double y1 = LineUtil.yAtX(m1, x);
        final double y2 = LineUtil.yAtX(m2, x);
        final double dy = Math.abs(y2 - y1);

        if (dy > maxYDistance) {
            return false;
        }

        // Check slopes
        final double slope1 = LineUtil.getSlope(m1);
        final double slope2 = LineUtil.getSlope(m2);
        final double slopeDiff = Math.abs(slope2 - slope1);

        return slopeDiff <= maxSlopeDiff;
    }

    //----------------------------//
    // checkSystemForOldBeamGroup //
    //----------------------------//
    /**
     * Check whether in the provided system beams have their related beam group,
     * and, if not, populate the system with needed BeamGroupInter instances.
     *
     * @param system the system to check for beam groups
     */
    @Deprecated
    public static void checkSystemForOldBeamGroup (SystemInfo system)
    {
        // Checking one beam is enough
        if (system.getSig().vertexSet().stream().anyMatch(inter
                -> !inter.isRemoved()
                           && (inter instanceof AbstractBeamInter)
                           && ((AbstractBeamInter) inter).getGroup() == null)) {
            logger.info("Upgrading BeamGroups for {}", system);
            populateSystem(system);
        }
    }

    //------------------------//
    // detectInterleavedRests //
    //------------------------//
    /**
     * Detect all the interleaved rests for this beam group.
     *
     * Rests lookup depends on the potential existence of an explicit voice relation
     * between the rest and one of the stemmed chords of the beam group:
     * <ul>
     * <li>If there is a direct or indirect {@link SameVoiceRelation}/{@link NextInVoiceRelation}
     * between rest chord and some beamed chord (white list) and if the rest center is located
     * within the beam group abscissa range, rest chord is considered as interleaved.
     * <li>If there is a direct {@link SeparateVoiceRelation} between rest chord and some beam
     * chord (black list) then the rest chord cannot be considered as interleaved.
     * <li>With no such relations, the rest chord is considered as a plain candidate which must
     * be located within the lookup area defined by the beam chords that surround rest chord
     * abscissa.
     * </ul>
     */
    public void detectInterleavedRests ()
    {
        final List<AbstractChordInter> headChords = getChords();
        final Set<RestChordInter> blackRests = getLinkedRests(headChords,
                                                              false,
                                                              SeparateVoiceRelation.class);
        final Set<RestChordInter> whiteRests = getLinkedRests(headChords,
                                                              true,
                                                              SameVoiceRelation.class,
                                                              NextInVoiceRelation.class);
        whiteRests.removeAll(blackRests); // Safer

        // Plain candidate rests
        final Set<RestChordInter> plainRests = new LinkedHashSet<>();
        for (Measure measure : getMeasures()) {
            plainRests.addAll(measure.getRestChords());
        }
        plainRests.removeAll(blackRests);
        plainRests.removeAll(whiteRests);

        AbstractChordInter prevChord = null;

        for (AbstractChordInter chord : headChords) {
            if (prevChord != null) {
                // Look for interleaved rest(s) between prevChord and chord
                // Priority on white listed rests if any
                final int prevChordX = prevChord.getTailLocation().x;
                final int chordX = chord.getTailLocation().x;

                for (RestChordInter white : whiteRests) {
                    final int x = white.getTailLocation().x;

                    if (x > prevChordX && x < chordX) {
                        final RestInter rest = (RestInter) white.getMembers().get(0);
                        final Point restCenter = rest.getCenter();
                        final NearestBeam nearest = getNearestBeam(restCenter);

                        if (nearest != null) {
                            sig.addEdge(nearest.beam, rest, new BeamRestRelation());
                        }
                    }
                }

                // Add compatible plain candidates
                lookupRests(prevChord, chord, plainRests, whiteRests);
            }

            prevChord = chord;
        }
    }

    //--------------//
    // getAllChords //
    //--------------//
    /**
     * Report the x-ordered collection of chords that are grouped by this beam group,
     * including the interleaved rests if any.
     *
     * @return the (perhaps empty) collection of 'beamed' head chords and interleaved rest chords.
     * @see #getChords()
     */
    public List<AbstractChordInter> getAllChords ()
    {
        // Start with beamed head chords
        final List<AbstractChordInter> allChords = getChords();

        // Add interleaved rests
        for (Inter member : getMembers()) {
            final AbstractBeamInter beam = (AbstractBeamInter) member;

            for (Relation rel : sig.getRelations(member, BeamRestRelation.class)) {
                final RestInter rest = (RestInter) sig.getOppositeInter(beam, rel);
                final RestChordInter restChord = rest.getChord();

                if (!allChords.contains(restChord)) {
                    allChords.add(restChord);
                }
            }
        }

        Collections.sort(allChords, Inters.byAbscissa);

        return allChords;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            bounds = Entities.getBounds(getMembers());
        }

        return super.getBounds();
    }

    //-----------//
    // getChords //
    //-----------//
    /**
     * Report the x-ordered collection of chords that are grouped by this beam group.
     * <p>
     * NOTA: This does NOT include any interleaved rest chord, only the really 'beamed' head chords.
     *
     * @return the (perhaps empty) collection of 'beamed' chords.
     * @see #getAllChords()
     */
    public List<AbstractChordInter> getChords ()
    {
        final List<AbstractChordInter> chords = new ArrayList<>();

        for (Inter bInter : getMembers()) {
            final AbstractBeamInter beam = (AbstractBeamInter) bInter;

            for (AbstractChordInter chord : beam.getChords()) {
                if (!chords.contains(chord)) {
                    chords.add(chord);
                }
            }
        }

        Collections.sort(chords, Inters.byAbscissa);

        return chords;
    }

    //--------------------//
    // getContextualGrade //
    //--------------------//
    @Override
    public Double getContextualGrade ()
    {
        // Compute global grade, based on contained beams
        return EnsembleHelper.computeMeanContextualGrade(this);
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (multiStaff) {
            if (sb.length() > 0) {
                sb.append(' ');
            }

            sb.append("multi-staff");
        }

        return sb.toString();
    }

    //-------------//
    // getDuration //
    //-------------//
    /**
     * Report the total duration of the sequence of chords within this group.
     * <p>
     * Beware, there may be rests inserted within beam-grouped notes.
     *
     * @return the total group duration, perhaps null
     */
    public Rational getDuration ()
    {
        final AbstractChordInter first = getFirstChord();
        final Rational firstOffset = first.getTimeOffset();

        final AbstractChordInter last = getLastChord();
        final Rational lastOffset = last.getTimeOffset();

        if (firstOffset == null || lastOffset == null) {
            return null;
        }

        return lastOffset.minus(firstOffset).plus(last.getDuration());
    }

    //---------------//
    // getFirstChord //
    //---------------//
    /**
     * Report the first chord on the left.
     *
     * @return the first chord
     */
    public AbstractChordInter getFirstChord ()
    {
        List<AbstractChordInter> chords = getChords();

        if (!chords.isEmpty()) {
            return chords.get(0);
        } else {
            return null;
        }
    }

    //--------------//
    // getLastChord //
    //--------------//
    /**
     * Report the last chord on the right.
     *
     * @return the last chord
     */
    public AbstractChordInter getLastChord ()
    {
        List<AbstractChordInter> chords = getChords();

        if (!chords.isEmpty()) {
            return chords.get(chords.size() - 1);
        } else {
            return null;
        }
    }

    //---------------//
    // getMainMedian //
    //---------------//
    /**
     * Report the median line of the longest beam in the group.
     *
     * @return group main median line
     */
    public Line2D getMainMedian ()
    {
        Line2D mainMedian = null;
        double mainWidth = Double.MIN_VALUE;

        for (Inter bInter : getMembers()) {
            final AbstractBeamInter beam = (AbstractBeamInter) bInter;
            final Line2D median = beam.getMedian();
            final double width = median.getX2() - median.getX1();

            if (mainMedian == null || mainWidth < width) {
                mainMedian = median;
                mainWidth = width;
            }
        }

        return mainMedian;
    }

    //-----------------//
    // getMaxSlopeDiff //
    //-----------------//
    /**
     * Report the maximum acceptable difference in slope between beams of a group.
     *
     * @return max slope diff
     */
    public static double getMaxSlopeDiff ()
    {
        return constants.maxSlopeDiff.getValue();
    }

    //-----------------//
    // getMaxYDistance //
    //-----------------//
    /**
     * Report the maximum acceptable vertical distance between median lines of subsequent
     * beams within a group.
     *
     * @return max vertical distance (median to median)
     */
    public static Scale.Fraction getMaxYDistance ()
    {
        return constants.maxYDistance;
    }

    //-------------//
    // getMeasures //
    //-------------//
    /**
     * Report the sequence of measures this beam group is involved in.
     *
     * @return one or several measures
     */
    public List<Measure> getMeasures ()
    {
        final Set<Measure> measureSet = new LinkedHashSet<>();

        for (Inter member : getMembers()) {
            final AbstractBeamInter beam = (AbstractBeamInter) member;
            for (AbstractChordInter chord : beam.getChords()) {
                measureSet.add(chord.getMeasure());
            }
        }

        // Sort measures by abscissa
        List<Measure> measureList = new ArrayList<>(measureSet);
        Collections.sort(measureList, (m1, m2) -> Double.compare(m1.getStack().getLeft(),
                                                                 m2.getStack().getLeft()));

        return measureList;
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public List<Inter> getMembers ()
    {
        return EnsembleHelper.getMembers(this, null);
    }

    //----------------//
    // getNearestBeam //
    //----------------//
    /**
     * Report the vertically nearest (full) beam if any that embraces abscissa-wise the
     * provided point.
     *
     * @param pt provided point
     * @return nearest compatible beam or null
     */
    private NearestBeam getNearestBeam (Point pt)
    {
        // Use sheet slope rather than plain vertical
        final Line2D vertical = sig.getSystem().getSkew().skewedVertical(pt);

        BeamInter bestBeam = null;
        double bestDist = Double.MAX_VALUE;

        for (Inter member : getMembers()) {
            if (member instanceof BeamInter) {
                final BeamInter beam = (BeamInter) member;
                final Line2D median = beam.getMedian();
                final Point2D cross = LineUtil.intersection(median, vertical);

                if (cross.getX() >= median.getX1() && cross.getX() <= median.getX2()) {
                    final double dist = PointUtil.length(PointUtil.subtraction(pt, cross));

                    if ((bestBeam == null) || (dist < bestDist)) {
                        bestBeam = beam;
                        bestDist = dist;
                    }
                }
            }
        }

        if (bestBeam == null) {
            return null;
        }

        return new NearestBeam(bestBeam, bestDist);
    }

    //---------//
    // getPart //
    //---------//
    @Override
    public Part getPart ()
    {
        for (Relation em : sig.getRelations(this, Containment.class)) {
            final Inter beam = sig.getOppositeInter(this, em);

            for (Relation bs : sig.getRelations(beam, BeamStemRelation.class)) {
                final Inter stem = sig.getOppositeInter(beam, bs);

                for (Relation hs : sig.getRelations(stem, HeadStemRelation.class)) {
                    final Inter head = sig.getOppositeInter(stem, hs);

                    return head.getPart();
                }
            }
        }

        return super.getPart();
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "BeamGroup";
    }

    //----------//
    // getVoice //
    //----------//
    /**
     * Report the assigned voice.
     *
     * @return beam group voice
     */
    @Override
    public Voice getVoice ()
    {
        AbstractChordInter ch = getFirstChord();

        return (ch != null) ? ch.getVoice() : null;
    }

    //-------------//
    // hasLongBeam //
    //-------------//
    /**
     * Report whether the beam group contains one beam with a width equal or larger than
     * the provided minimum width.
     *
     * @param minWidth minimum width (in pixels)
     * @return true if so
     */
    public boolean hasLongBeam (int minWidth)
    {
        for (Inter member : getMembers()) {
            if (member.getBounds().width >= minWidth) {
                return true;
            }
        }

        return false;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    public String internals ()
    {
        final StringBuilder sb = new StringBuilder(super.internals());
        sb.append(Entities.ids(" beams", getMembers()));

        if (multiStaff) {
            sb.append(" multiStaff");
        }

        return sb.toString();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    /**
     * Invalidate cached information.
     */
    @Override
    public void invalidateCache ()
    {
        bounds = null;
    }

    //--------------//
    // isMultiStaff //
    //--------------//
    /**
     * Tell whether this beam group is linked to more than one staff.
     *
     * @return the multiStaff
     */
    public boolean isMultiStaff ()
    {
        return multiStaff;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //---------------//
    // findBeamGroup //
    //---------------//
    /**
     * Try to find a compatible group for the provided beam.
     *
     * @param beam          provided beam
     * @param system        containing system
     * @param excludedGroup excluded group or null
     * @return existing compatible group if any, null otherwise
     */
    public static BeamGroupInter findBeamGroup (AbstractBeamInter beam,
                                                SystemInfo system,
                                                BeamGroupInter excludedGroup)
    {
        final Scale scale = system.getSheet().getScale();
        final double minXOverlap = scale.toPixels(constants.minXOverlap);
        final double maxYDistance = scale.toPixels(constants.maxYDistance);
        final double maxSlopeDiff = constants.maxSlopeDiff.getValue();
        final SIGraph sig = system.getSig();
        final List<Inter> groups = sig.inters(BeamGroupInter.class);

        if (excludedGroup != null) {
            groups.remove(excludedGroup);
        }

        for (Inter inter : groups) {
            final BeamGroupInter group = (BeamGroupInter) inter;

            for (Inter member : group.getMembers()) {
                final AbstractBeamInter b = (AbstractBeamInter) member;

                if (canBeNeighbors(beam, b, minXOverlap, maxYDistance, maxSlopeDiff)) {
                    return group;
                }
            }
        }

        return null;
    }

    //-----------------//
    // populateMeasure //
    //-----------------//
    /**
     * Populate all the BeamGroupInter instances for a given measure.
     *
     * @param measure         the containing measure
     * @param checkGroupSplit true for check on group split
     */
    public static void populateMeasure (Measure measure,
                                        boolean checkGroupSplit)
    {
        // Retrieve beams in this measure
        final Set<AbstractBeamInter> beams = new LinkedHashSet<>();
        for (AbstractChordInter chord : measure.getHeadChords()) {
            beams.addAll(chord.getBeams());
        }

        // Retrieve beam groups for this measure
        final Set<BeamGroupInter> groups = new LinkedHashSet<>();
        for (AbstractBeamInter beam : beams) {
            if (beam.isRemoved()) {
                continue;
            }

            final BeamGroupInter group = beam.getGroup();
            groups.add(group);
        }

        // Detect groups that are linked to more than one staff
        for (BeamGroupInter group : measure.getBeamGroups()) {
            group.countStaves();
        }
    }

    //----------------------//
    // populateCueAggregate //
    //----------------------//
    /**
     * Group the (cue) beams of a CueAggregate.
     *
     * @param beams (cue) beams found in CueAggregate
     */
    public static void populateCueAggregate (List<Inter> beams)
    {
        if (beams.isEmpty()) {
            return;
        }

        final Scale scale = beams.get(0).getSig().getSystem().getSheet().getScale();
        groupBeams(beams,
                   scale.toPixels(constants.cueMinXOverlap),
                   scale.toPixels(constants.cueMaxYDistance),
                   constants.cueMaxSlopeDiff.getValue());
    }

    //----------------//
    // populateSystem //
    //----------------//
    /**
     * Populate a system with all the needed BeamGroupInter instances to gather the
     * system beams.
     *
     * @param system the system to process
     */
    public static void populateSystem (SystemInfo system)
    {
        final Scale scale = system.getSheet().getScale();
        groupBeams(system.getSig().inters(AbstractBeamInter.class),
                   scale.toPixels(constants.minXOverlap),
                   scale.toPixels(constants.maxYDistance),
                   constants.maxSlopeDiff.getValue());
    }

    //------------//
    // groupBeams //
    //------------//
    /**
     * Organize the provided beams into groups.
     *
     * @param beams        the beams to be grouped
     * @param minXOverlap  minimum abscissa overlap to be neighbors
     * @param maxYDistance maximum vertical distance to be neighbors
     * @param maxSlopeDiff maximum difference in slope to be neighbors
     */
    private static void groupBeams (List<Inter> beams,
                                    double minXOverlap,
                                    double maxYDistance,
                                    double maxSlopeDiff)
    {
        if (beams.isEmpty()) {
            return;
        }

        final SIGraph sig = beams.get(0).getSig();
        Collections.sort(beams, Inters.byOrdinate);

        for (int i = 0; i < beams.size(); i++) {
            final AbstractBeamInter beam = (AbstractBeamInter) beams.get(i);

            if (beam.isVip()) {
                logger.info("VIP groupBeam for {}", beam);
            }

            BeamGroupInter group = beam.getGroup();

            if (group == null) {
                // This beam is not compatible with any previous beam, so let's start a new group
                group = new BeamGroupInter();
                sig.addVertex(group);
                group.addMember(beam);
            }

            final Rectangle luBox = beam.getBounds();
            luBox.grow(0, (int) Math.ceil(maxYDistance - (beam.getHeight() / 2)));
            final int yBreak = luBox.y + luBox.height;
            beam.addAttachment("g", luBox);

            // Inspect all following beams until out of reach
            for (int j = i + 1; j < beams.size(); j++) {
                final AbstractBeamInter b = (AbstractBeamInter) beams.get(j);
                final Rectangle bBox = b.getBounds();

                if (luBox.intersects(bBox)) {
                    if (canBeNeighbors(beam, b, minXOverlap, maxYDistance, maxSlopeDiff)) {
                        group.addMember(b);
                    }
                } else if (bBox.y >= yBreak) {
                    break; // Since list is ordered by ordinate
                }
            }
        }

        // If a beam belongs to several groups, merge these groups
        for (int i = 0; i < beams.size(); i++) {
            final AbstractBeamInter beam = (AbstractBeamInter) beams.get(i);
            final List<Inter> groups = new ArrayList<>(beam.getAllEnsembles());

            if (groups.size() > 1) {
                final BeamGroupInter firstGroup = (BeamGroupInter) groups.get(0);

                for (Inter otherInter : groups.subList(1, groups.size())) {
                    final BeamGroupInter otherGroup = (BeamGroupInter) otherInter;

                    for (Inter m : otherGroup.getMembers()) {
                        firstGroup.addMember(m);
                        otherGroup.removeMember(m);
                    }

                    otherGroup.remove();
                }
            }
        }
    }

    //--------------//
    // removeMember //
    //--------------//
    @Override
    public void removeMember (Inter member)
    {
        if (!(member instanceof AbstractBeamInter)) {
            throw new IllegalArgumentException(
                    "Only AbstractBeamInter can be removed from BeamGroupInter");
        }

        EnsembleHelper.removeMember(this, member);
    }

    //---------------//
    // setMultiStaff //
    //---------------//
    public void setMultiStaff (boolean multiStaff)
    {
        this.multiStaff = multiStaff;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip (boolean vip)
    {
        this.vip = vip;
    }

    //----------//
    // setVoice //
    //----------//
    /**
     * Set a voice to this beam group, and to the related entities.
     *
     * @see #justAssignVoice(Voice)
     *
     * @param voice the voice to set
     */
    public void setVoice (Voice voice)
    {
        // Propagate voice to the beamed chords, including the interleaved rests if any
        for (AbstractChordInter chord : getAllChords()) {
            if (chord.getMeasure() == voice.getMeasure()) {
                chord.setVoice(voice);
            }
        }
    }

    //-------------//
    // assignGroup //
    //-------------//
    /**
     * Recursively determine BeamGroupInter for the provided beam, as well as all other
     * beams connected within the same group.
     *
     * @param beam    the beam seed
     * @param measure the containing measure
     */
    private static void assignGroup (BeamGroupInter group,
                                     AbstractBeamInter beam)
    {
        group.addMember(beam);

        for (AbstractChordInter chord : beam.getChords()) {
            for (AbstractBeamInter b : chord.getBeams()) {
                if (b.getEnsemble() == null) {
                    assignGroup(group, b);
                }
            }
        }
    }

    //-----------------//
    // checkBeamGroups //
    //-----------------//
    /**
     * Check all the BeamGroupInter instances of the given measure, to find the first
     * split if any to perform.
     *
     * @param measure the given measure
     * @return the first split parameters, or null if everything is OK
     */
    private static boolean checkBeamGroups (Measure measure)
    {
        for (BeamGroupInter group : measure.getBeamGroups()) {
            AbstractChordInter alienChord = group.checkForSplit();

            if (alienChord != null) {
                group.split(alienChord);

                return true;
            }
        }

        return false;
    }

    //---------------//
    // checkForSplit //
    //---------------//
    /**
     * Run a consistency check on the group, and detect when a group has to be split.
     *
     * @return the detected alien chord, or null if no split is needed
     */
    private AbstractChordInter checkForSplit ()
    {
        final Scale scale = sig.getSystem().getSheet().getScale();
        final double maxChordDy = constants.maxChordDy.getValue();

        // Make sure all chords are part of the same group
        // We check the vertical distance between any chord and the beams above or below the chord.
        for (AbstractChordInter chord : getChords()) {
            if (chord.isVip()) {
                logger.info("VIP checkForSplit on {}", chord);
            }

            final Rectangle chordBox = chord.getBounds();
            final Point tail = chord.getTailLocation();

            // Get the collection of questionable beams WRT chord
            List<AbstractBeamInter> questionableBeams = new ArrayList<>();

            for (Inter bInter : getMembers()) {
                final AbstractBeamInter beam = (AbstractBeamInter) bInter;
                // Skip beam hooks
                // Skip beams attached to this chord
                // Skip beams with no abscissa overlap WRT this chord
                if (!beam.isHook() && !beam.getChords().contains(chord)
                            && (GeoUtil.xOverlap(beam.getBounds(), chordBox) > 0)) {
                    // Check vertical gap
                    int lineY = (int) Math.rint(LineUtil.yAtX(beam.getMedian(), tail.x));
                    int yOverlap = Math.min(lineY, chordBox.y + chordBox.height) - Math.max(
                            lineY,
                            chordBox.y);

                    if (yOverlap < 0) {
                        questionableBeams.add(beam);
                    }
                }
            }

            if (questionableBeams.isEmpty()) {
                continue; // No problem found around the chord at hand
            }

            // Sort these questionable beams vertically, at chord stem abscissa,
            // according to distance from chord tail.
            Collections.sort(questionableBeams, (b1, b2) -> {
                         final double y1 = LineUtil.yAtX(b1.getMedian(), tail.x);
                         final double tailDy1 = Math.abs(y1 - tail.y);
                         final double y2 = LineUtil.yAtX(b2.getMedian(), tail.x);
                         final double tailDy2 = Math.abs(y2 - tail.y);
                         return Double.compare(tailDy1, tailDy2);
                     });

            AbstractBeamInter nearestBeam = questionableBeams.get(0);
            int lineY = (int) Math.rint(LineUtil.yAtX(nearestBeam.getMedian(), tail.x));
            int tailDy = Math.abs(lineY - tail.y);
            double normedDy = scale.pixelsToFrac(tailDy);

            if (normedDy > maxChordDy) {
                logger.debug(
                        "Vertical gap between {} and {}, {} vs {}",
                        chord,
                        nearestBeam,
                        normedDy,
                        maxChordDy);

                // Split the beam group here
                return chord;
            }
        }

        return null; // everything is OK
    }

    //-------------//
    // countStaves //
    //-------------//
    /**
     * Check whether this group is linked to more than one staff.
     * If so, it is flagged as such.
     */
    private void countStaves ()
    {
        Set<Staff> staves = new LinkedHashSet<>();

        for (Inter beam : getMembers()) {
            for (Relation rel : sig.getRelations(beam, BeamStemRelation.class)) {
                Inter stem = sig.getOppositeInter(beam, rel);
                Staff staff = stem.getStaff();

                if (staff != null) {
                    staves.add(staff);
                }
            }
        }

        if (staves.size() > 1) {
            multiStaff = true;
        }
    }

    //----------------//
    // getLinkedRests //
    //----------------//
    /**
     * Report the rest chords with a specific relation to this group.
     *
     * @param headChords head chords of this group
     * @param transitive true for re-applying search to rest chords themselves, etc
     * @param classes    desired relation classes
     * @return white listed rest chords
     */
    private Set<RestChordInter> getLinkedRests (List<AbstractChordInter> headChords,
                                                boolean transitive,
                                                Class<?>... classes)
    {
        final Set<RestChordInter> allRests = new LinkedHashSet<>();

        // First, restChords directly linked to headChords
        for (AbstractChordInter ch : headChords) {
            for (Relation sameRel : sig.getRelations(ch, classes)) {
                final Inter other = sig.getOppositeInter(ch, sameRel);

                if (other instanceof RestChordInter) {
                    allRests.add((RestChordInter) other);
                }
            }
        }

        if (transitive) {
            // Then, other restChords directly linked to already collected restChords, etc...
            final Set<RestChordInter> newRests = new LinkedHashSet<>();
            do {
                allRests.addAll(newRests);
                newRests.clear();

                for (AbstractChordInter ch : allRests) {
                    for (Relation sameRel : sig.getRelations(ch, classes)) {
                        final Inter other = sig.getOppositeInter(ch, sameRel);

                        if ((other instanceof RestChordInter) && !allRests.contains(other)) {
                            newRests.add((RestChordInter) other);
                        }
                    }
                }
            } while (!newRests.isEmpty());
        }

        return allRests;
    }

    //-------------//
    // lookupRests //
    //-------------//
    /**
     * Look for plain rests interleaved between the provided left and right headChords.
     * <p>
     * A lookup area is defined between every sequence of two stemmed chords of the beam group,
     * and all measure rests are checked for intersection with this lookup area:
     * <ul>
     * <li>If the 2 chords are in the same vertical direction, the area is the parallelogram
     * defined by their stems, vertically extended if needed to the middle line of related staff.
     * <li>If in opposite directions, we focus only above the beams.
     * The area is then defined by the up stem and the main beam median line,
     * vertically extended if needed to the middle line of the upper staff
     * </ul>
     *
     * Additional checks:
     * <ul>
     * <li>An interleaved rest is expected to lie horizontally BETWEEN the beam chords, hence
     * we impose that rest bounds stay horizontally away from left and right chords.
     * <li>Two interleaved rests cannot overlap in abscissa, so only the one closer to the
     * related beam is kept.
     * <li>An interleaved rest can relate to just one beam group.
     * So, if the rest at hand is already related to (another) beam group, the more distant relation
     * is discarded.
     * <li>A rest chord can belong to exactly one voice.
     * NOTA: this is not yet implemented, and might be questionable (notion of "shared rests").
     * </ul>
     *
     * @param left       provided head chord on left side
     * @param right      provided head chord on right side
     * @param candidates plain candidate rests, already purged of white-listed or blacklisted rests
     * @param whiteRests white-listed rests
     */
    private void lookupRests (AbstractChordInter left,
                              AbstractChordInter right,
                              Set<RestChordInter> candidates,
                              Set<RestChordInter> whiteRests)
    {
        final Rectangle leftBox = left.getBounds();
        final Point leftHead = left.getHeadLocation();
        final Point leftTail = left.getTailLocation();
        final Rectangle rightBox = right.getBounds();
        final Point rightHead = right.getHeadLocation();
        final Point rightTail = right.getTailLocation();
        final Line2D median = getMainMedian();

        // Define proper lookup area, according to beamed chords directions
        final Polygon polygon = new Polygon();
        final int leftDir = Integer.signum(leftHead.y - leftTail.y);
        final int rightDir = Integer.signum(rightHead.y - rightTail.y);

        if (leftDir == rightDir) {
            if ((left.getStaff() != null) && left.getStaff() == right.getStaff()) {
                // Extend heads vertically to staff middle line
                final LineInfo midLine = left.getStaff().getMidLine();
                final int midLeft = midLine.yAt(leftHead.x);
                final int midRight = midLine.yAt(rightHead.x);

                if (leftDir * (midLeft - leftHead.y) > 0) {
                    leftHead.y = midLeft;
                }

                if (leftDir * (midRight - rightHead.y) > 0) {
                    rightHead.y = midRight;
                }
            }

            polygon.addPoint(rightHead.x, rightHead.y);
            polygon.addPoint(rightTail.x, rightTail.y);
            polygon.addPoint(leftTail.x, leftTail.y);
            polygon.addPoint(leftHead.x, leftHead.y);
        } else {
            // Opposite directions, we select the area ABOVE the beam (this is questionable...)
            final Point upHead = leftDir < 0 ? leftHead : rightHead;
            final Point upTail = leftDir < 0 ? leftTail : rightTail;
            final Point downHead = leftDir > 0 ? leftHead : rightHead;
            final Point downTail = leftDir > 0 ? leftTail : rightTail;

            // Extend upHead vertically to upper staff middle line
            final Staff upStaff = leftDir < 0 ? left.getStaff() : right.getStaff();
            final LineInfo midLine = upStaff.getMidLine();
            final int mid = midLine.yAt((leftHead.x + rightHead.x) / 2);

            if (upHead.y > mid) {
                upHead.y = mid;
            }

            polygon.addPoint(upHead.x, upHead.y);

            final Point upMedian = PointUtil.rounded(LineUtil.intersection(
                    median,
                    new Line2D.Double(upHead, upTail)));
            polygon.addPoint(upMedian.x, upMedian.y);

            final Point downMedian = PointUtil.rounded(LineUtil.intersection(
                    median,
                    new Line2D.Double(downHead, downTail)));
            polygon.addPoint(downMedian.x, downMedian.y);

            polygon.addPoint(downMedian.x,
                             downMedian.y - (upMedian.y - upHead.y));
        }

        CandidateLoop:
        for (RestChordInter restChord : candidates) {
            final Rectangle box = restChord.getBounds();

            if (restChord.isVip()) {
                logger.info("VIP lookupRests restChord: {}", restChord);
            }

            if (polygon.intersects(box.x, box.y, box.width, box.height)) {
                final RestInter rest = (RestInter) restChord.getMembers().get(0);
                final Point center = rest.getCenter();
                final Line2D vertical = sig.getSystem().getSkew().skewedVertical(center);

                // Check candidate lies horizontally away from left & right chords
                if ((GeoUtil.xOverlap(box, leftBox) > 0)
                            || (GeoUtil.xOverlap(box, rightBox) > 0)) {
                    logger.debug("{} overlaps {} or {}", restChord, left, right);
                    continue;
                }

                final NearestBeam nearest = getNearestBeam(center);

                if (nearest == null) {
                    continue;
                }

                // Check no abscissa overlap with sibling interleaved rests
                for (Relation rel : sig.getRelations(nearest.beam, BeamRestRelation.class)) {
                    final BeamRestRelation br = (BeamRestRelation) rel;
                    final Inter oRest = sig.getOppositeInter(nearest.beam, rel);
                    final Point2D pt = LineUtil.intersectionAtY(vertical, oRest.getCenter().y);

                    if (oRest.getBounds().contains(pt)) {
                        // Give up in front of a whitelisted rest
                        final RestChordInter oRestChord = (RestChordInter) oRest.getEnsemble();

                        if (whiteRests.contains(oRestChord)) {
                            continue CandidateLoop;
                        }

                        // Compare distance to beam
                        if (nearest.dist > br.getDistance()) {
                            logger.debug("{} farther to {} than {}", restChord, nearest.beam, oRest);

                            continue CandidateLoop;
                        } else {
                            logger.debug("{} closer to {} than {}", restChord, nearest.beam, oRest);
                            sig.removeEdge(rel);
                        }
                    }
                }

                // Case of rest already interleaved (in another beam group)
                for (Relation rel : sig.getRelations(rest, BeamRestRelation.class)) {
                    final BeamRestRelation br = (BeamRestRelation) rel;
                    final Inter oBeam = sig.getOppositeInter(rest, rel);

                    if (nearest.dist > br.getDistance()) {
                        logger.debug("{} farther to {} than to {}", restChord, nearest.beam, oBeam);

                        continue CandidateLoop;
                    } else {
                        logger.debug("{} closer to {} than to {}", restChord, nearest.beam, oBeam);
                        sig.removeEdge(rel);
                    }
                }

                // All tests are OK
                sig.addEdge(nearest.beam, rest, new BeamRestRelation());
            }
        }
    }

    //-------//
    // split //
    //-------//
    private void split (AbstractChordInter alienChord)
    {
        new Splitter(alienChord).process();
    }

    //~ Inner classes ------------------------------------------------------------------------------
    //-------------//
    // NearestBeam //
    //-------------//
    private class NearestBeam
    {

        public final BeamInter beam;

        public final double dist;

        public NearestBeam (BeamInter beam,
                            double dist)
        {
            this.beam = beam;
            this.dist = dist;
        }
    }

    //----------//
    // Splitter //
    //----------//
    /**
     * Utility class meant to perform a split on this group.
     * This group is shrunk, because some of its beams are moved to a new (alien) group.
     */
    private class Splitter
    {

        /** Chord detected as belonging to a (new) alien group. */
        private final AbstractChordInter alienChord;

        /**
         * Beams that belong to new alien group.
         * (Initially populated with all beams (except beam hooks) attached to alienChord)
         */
        private List<Inter> alienBeams;

        /** The new alien group. */
        private BeamGroupInter alienGroup;

        /** The chord that embraces both (old) group and (new) alien group. */
        private HeadChordInter pivotChord;

        /**
         * Create a splitter for this BeamGroupInter, triggered by alienChord
         *
         * @param alienChord a detected chord that should belong to a separate group
         */
        Splitter (AbstractChordInter alienChord)
        {
            this.alienChord = alienChord;
        }

        //---------//
        // process //
        //---------//
        /**
         * Actually split the group in two, around the detected pivot chord.
         * <p>
         * Some beams of this group instance are moved to a new separate BeamGroupInter instance.
         * The two instances are articulated around a pivot chord, common to both groups.
         * <p>
         */
        public void process ()
        {
            logger.debug("{} splitter on {}", BeamGroupInter.this, alienChord);

            // The new group on alienChord side
            alienGroup = createAlienGroup();

            // Detect the pivot chord shared by the two groups, and "split" it for both groups
            pivotChord = detectPivotChord();

            // Dispatch beams attached to pivotChord to their proper group
            dispatchPivotBeams();

            // Make sure all beams have been dispatched
            dispatchAllBeams();

            // Duplicate the chord between the two group
            splitChord();
        }

        //------------------//
        // createAlienGroup //
        //------------------//
        private BeamGroupInter createAlienGroup ()
        {
            alienGroup = new BeamGroupInter();

            // Check all former beams: any beam linked to the detected alienChord should be
            // moved to the alienGroup.
            // (This cannot apply to beam hooks, they will be processed later)
            alienBeams = new ArrayList<>(alienChord.getBeams());

            // Now apply the move
            for (Inter bInter : alienBeams) {
                AbstractBeamInter beam = (AbstractBeamInter) bInter;
                beam.switchToGroup(alienGroup);
            }

            return alienGroup;
        }

        //------------------//
        // detectPivotChord //
        //------------------//
        /**
         * Look through the chords on the alienGroup to detect the one which is shared
         * by both this group and the alienGroup.
         *
         * @return the pivot chord found
         */
        private HeadChordInter detectPivotChord ()
        {
            List<AbstractChordInter> commons = getChords();
            commons.retainAll(alienGroup.getChords());

            // TODO: what if we have more than one common chord???
            return (HeadChordInter) commons.get(0);
        }

        //------------------//
        // dispatchAllBeams //
        //------------------//
        /**
         * Inspect all remaining beams in (old) group, and move to the (new) alien group
         * the ones which are connected to alien beams (except through the pivotChord).
         */
        private void dispatchAllBeams ()
        {
            List<AbstractBeamInter> pivotBeams = pivotChord.getBeams();
            AllLoop:
            for (Inter bInter : new ArrayList<>(getMembers())) {
                final AbstractBeamInter beam = (AbstractBeamInter) bInter;

                // If beam is attached to pivotChord, skip it
                if (pivotBeams.contains(beam)) {
                    continue;
                }

                // Check every beam chord, for touching an alienBeam
                for (AbstractChordInter chord : beam.getChords()) {
                    for (AbstractBeamInter b : chord.getBeams()) {
                        if (b.getGroup() == alienGroup) {
                            beam.switchToGroup(alienGroup);

                            continue AllLoop;
                        }
                    }
                }
            }
        }

        //--------------------//
        // dispatchPivotBeams //
        //--------------------//
        /**
         * Inspect the beams connected to pivotChord, and move to the (new) alien group
         * those which fall on the alienSide of the pivotChord.
         * This does not apply to beam hooks.
         */
        private void dispatchPivotBeams ()
        {
            // Select the tail beam of alienChord
            final AbstractBeamInter alienTailBeam = alienChord.getBeams().get(0);

            final List<AbstractBeamInter> pivotBeams = pivotChord.getBeams();
            Boolean onAlienSide = null;

            // Inspect the pivot beams, from tail to head
            for (int ib = 0; ib < pivotBeams.size(); ib++) {
                AbstractBeamInter b = pivotChord.getBeams().get(ib);

                if (b.isHook()) {
                    continue;
                }

                if (onAlienSide == null) {
                    onAlienSide = alienBeams.contains(b);
                }

                if (b == alienTailBeam) {
                    if (onAlienSide) {
                        // End of alien side
                        logger.debug("Alien end");

                        for (AbstractBeamInter ab : pivotBeams.subList(0, ib + 1)) {
                            if (!alienBeams.contains(ab)) {
                                ab.switchToGroup(alienGroup);
                            }
                        }
                    } else {
                        // Start of alien side
                        logger.debug("Alien start");

                        for (AbstractBeamInter ab : pivotBeams.subList(
                                ib,
                                pivotChord.getBeams().size())) {
                            if (!alienBeams.contains(ab)) {
                                ab.switchToGroup(alienGroup);
                            }
                        }
                    }

                    return;
                }
            }
        }

        //------------------//
        // extractShortStem //
        //------------------//
        private StemInter extractShortStem (AbstractChordInter chord,
                                            int yStop)
        {
            final int stemDir = chord.getStemDir();
            final StemInter rootStem = chord.getStem();

            // Ordinate of head side of stem
            final int yStart = (int) Math.rint(
                    ((stemDir > 0) ? rootStem.getTop() : rootStem.getBottom()).getY() - 1);

            return rootStem.extractSubStem(yStart, yStop);
        }

        //------------//
        // splitChord //
        //------------//
        /**
         * Split the chord which embraces the two beam groups.
         * <p>
         * At this point, each beam has been moved to its proper group, either this (old) group or
         * the (new) alienGroup. What remains to be done is to split the pivot chord between the
         * two groups.
         * <p>
         * The beam group (old or alien) located at tail of pivot chord reuses pivot chord & stem.
         * The other group (the one closer to heads) must use a shorter stem (and chord).
         * <p>
         * Also, we have to avoid exclusion between any beam and the opposite (mirror) chord/stem.
         */
        private void splitChord ()
        {
            logger.debug("splitChord: {}", pivotChord);

            final SIGraph sig = pivotChord.getSig();
            final List<AbstractBeamInter> pivotBeams = pivotChord.getBeams();
            final StemInter pivotStem = pivotChord.getStem();

            // Create a clone of pivotChord (heads are duplicated, but no stem or beams initially)
            HeadChordInter shortChord = pivotChord.duplicate(true);

            // The beams closer to tail will stay with pivotChord and its long stem
            // The beams closer to head (headBeams) will migrate to a new short chord & stem
            // For this, let's look at tail end of pivotChord
            final boolean aliensAtTail = alienBeams.contains(pivotBeams.get(0));
            final List<Inter> headBeams = aliensAtTail ? getMembers() : alienBeams;

            // Determine tail end for short stem, by walking on pivot from tail to head
            AbstractBeamInter firstHeadBeam = null;

            for (int i = 0; i < pivotBeams.size(); i++) {
                AbstractBeamInter beam = pivotBeams.get(i);

                if (headBeams.contains(beam)) {
                    firstHeadBeam = beam;

                    // Beam hooks to move?
                    for (AbstractBeamInter b : pivotBeams.subList(i + 1, pivotBeams.size())) {
                        if (b.isHook() && !headBeams.contains(b)) {
                            headBeams.add(b);
                        }
                    }

                    break;
                }
            }

            // Build shortStem
            Relation r = sig.getRelation(firstHeadBeam, pivotStem, BeamStemRelation.class);
            BeamStemRelation bsRel = (BeamStemRelation) r;
            int y = (int) Math.rint(bsRel.getExtensionPoint().getY());
            final StemInter shortStem = extractShortStem(pivotChord, y);
            shortChord.setStem(shortStem);
            sig.addEdge(shortStem, pivotStem, new StemAlignmentRelation());

            // Link mirrored heads to short stem
            for (Inter note : shortChord.getNotes()) {
                for (Relation hs : sig.getRelations(note.getMirror(), HeadStemRelation.class)) {
                    sig.addEdge(note, shortStem, hs.duplicate());
                }
            }

            // Update information related to headBeams
            for (Inter bInter : headBeams) {
                final AbstractBeamInter beam = (AbstractBeamInter) bInter;

                // Avoid exclusion between head beam and pivotStem
                sig.addEdge(beam, pivotStem, new NoExclusion());

                // Move BeamStem relation from pivot to short
                Relation bs = sig.getRelation(beam, pivotStem, BeamStemRelation.class);

                if (bs != null) {
                    sig.removeEdge(bs);
                    sig.addEdge(beam, shortStem, bs);
                }
            }

            // Notify updates to both chords
            shortChord.invalidateCache();
            pivotChord.invalidateCache();

            pivotChord.getMeasure().getStack().addInter(shortChord);
        }
    }

//-----------//
// Constants //
//-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer maxSplitLoops = new Constant.Integer(
                "loops",
                10,
                "Maximum number of loops allowed for splitting beam groups");

        private final Scale.Fraction maxChordDy = new Scale.Fraction(
                0.5,
                "Maximum vertical gap between a chord and a beam");

        private final Scale.Fraction minXOverlap = new Scale.Fraction(
                0.7,
                "Minimum horizontal overlap between subsequent beams of a group");

        private final Scale.Fraction cueMinXOverlap = new Scale.Fraction(
                0.7,
                "(Cue) Minimum horizontal overlap between subsequent beams of a group");

        private final Scale.Fraction maxYDistance = new Scale.Fraction(
                1.5,
                "Maximum vertical distance between subsequent beams of a group");

        private final Scale.Fraction cueMaxYDistance = new Scale.Fraction(
                1.5,
                "(Cue) Maximum vertical distance between subsequent beams of a group");

        private final Constant.Double maxSlopeDiff = new Constant.Double(
                "tangent",
                0.065,
                "Maximum slope difference between beams of a group");

        private final Constant.Double cueMaxSlopeDiff = new Constant.Double(
                "tangent",
                0.2,
                "(Cue) Maximum slope difference between beams of a group");

    }
}
