//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T u p l e t s B u i l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.BeamGroupInter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.relation.ChordTupletRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class <code>TupletsBuilder</code> tries to connect every tuplet symbol in a measure stack
 * to its embraced chords.
 *
 * @author Hervé Bitteur
 */
public class TupletsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TupletsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The dedicated measure stack. */
    private final MeasureStack stack;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>TupletsBuilder</code> object.
     *
     * @param stack the dedicated stack
     */
    public TupletsBuilder (MeasureStack stack)
    {
        this.stack = stack;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------------//
    // getChordsAround //
    //-----------------//
    /**
     * Report the list of AbstractChordInter instances (rests & heads) in the
     * neighborhood of the specified Inter.
     * <p>
     * Neighborhood is limited horizontally by the measure sides and vertically by the staves above
     * and below.
     *
     * @param tuplet the inter of interest
     * @return the list of neighbors, sorted by euclidean distance to tuplet sign
     */
    private List<AbstractChordInter> getChordsAround (TupletInter tuplet)
    {
        final List<AbstractChordInter> chords = new ArrayList<>();
        final Point tupletCenter = tuplet.getCenter();

        if (stack == null) {
            return chords;
        }

        final List<Staff> stavesAround = stack.getSystem().getStavesAround(tupletCenter);
        logger.trace("{} around:{}", tuplet, stavesAround);

        for (AbstractChordInter chord : stack.getStandardChords()) {
            final List<Staff> chordStaves = new ArrayList<>(chord.getStaves());
            chordStaves.retainAll(stavesAround);

            if (!chordStaves.isEmpty()) {
                chords.add(chord);
            }
        }

        Collections.sort(chords, new ByEuclidean(tupletCenter));
        logger.trace("Chords: {}", Inters.ids(chords));

        return chords;
    }

    //------------------//
    // linkStackTuplets //
    //------------------//
    /**
     * Try to link all tuplet signs in stack.
     * <p>
     * A tuplet sign embraces a specific number of notes (heads / rests).
     * <p>
     * Its neighborhood is limited in its part, vertically to staff above and staff below and
     * horizontally to its containing measure stack.
     */
    public void linkStackTuplets ()
    {
        final Set<TupletInter> tuplets = stack.getTuplets();

        for (TupletInter tuplet : tuplets) {
            if (tuplet.isVip()) {
                logger.info("VIP linkStackTuplets for {}", tuplet);
            }

            // Purge existing tuplet-chord relations, except manual ones if any
            final SIGraph sig = stack.getSystem().getSig();
            final Set<Relation> rels = sig.getRelations(tuplet, ChordTupletRelation.class);

            for (Relation rel : rels) {
                if (!rel.isManual()) {
                    sig.removeEdge(rel);
                }
            }

            final Collection<Link> links = lookupLinks(tuplet);

            for (Link link : links) {
                link.applyTo(tuplet);
            }

            if (!tuplet.isManual() && !sig.hasRelation(tuplet, ChordTupletRelation.class)) {
                tuplet.remove();
            }
        }
    }

    //-------------//
    // lookupLinks //
    //-------------//
    /**
     * Look up for tuplet relevant chords.
     *
     * @param tuplet the tuplet sign
     * @return the collection of links found, perhaps empty
     */
    public Collection<Link> lookupLinks (TupletInter tuplet)
    {
        // Try to link tuplet with proper chords found in measure stack
        // (just staff above and staff below)
        List<AbstractChordInter> chordcandidates = getChordsAround(tuplet);

        // Now, get the properly embraced chords
        SortedSet<AbstractChordInter> chords = getEmbracedChords(tuplet, chordcandidates);

        if (chords == null) {
            return Collections.emptySet();
        }

        logger.trace("{} connectable to {}", tuplet, chords);

        List<Link> links = new ArrayList<>();

        for (AbstractChordInter chord : chords) {
            links.add(new Link(chord, new ChordTupletRelation(tuplet.getShape()), false));
        }

        return links;
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //---------------//
    // expectedCount //
    //---------------//
    /**
     * Report the number of basic items governed by the tuplet.
     * A given chord may represent several basic items (chords of base duration)
     *
     * @param shape the tuplet shape
     * @return 3 or 6
     */
    private static int expectedCount (Shape shape)
    {
        return switch (shape) {
            case TUPLET_THREE -> 3;
            case TUPLET_SIX -> 6;
            default -> {
                logger.error("Incorrect tuplet shape {}", shape);
                yield 0;
            }
        };
    }

    //-----------//
    // beamGroup //
    //-----------//
    /**
     * Report the beam group the chord belongs to, if any.
     *
     * @param chord the chord at hand
     * @return its beam group, or null if the chord is not beamed
     */
    private static BeamGroupInter beamGroup (AbstractChordInter chord)
    {
        final StemInter stem = chord.getStem();

        if (stem == null) {
            return null;
        }

        for (AbstractBeamInter beam : stem.getBeams()) {
            return beam.getGroup();
        }

        return null;
    }

    //---------//
    // bestRun //
    //---------//
    /**
     * Among the runs of consecutive chords that make up a tuplet, report the one centered
     * closest to the sign.
     *
     * @param row    the candidate chords, ordered by abscissa
     * @param tuplet the tuplet sign
     * @return the best run, or null if no run makes up the tuplet
     */
    private static List<AbstractChordInter> bestRun (List<AbstractChordInter> row,
                                                     TupletInter tuplet)
    {
        final Point sign = tuplet.getCenter();
        final int count = expectedCount(tuplet.getShape());
        final Rational factor = tuplet.getDurationFactor();
        final int maxChords = count * constants.maxChordsPerItem.getValue();
        final double maxOffsetRatio = constants.maxSignOffset.getValue();
        List<AbstractChordInter> best = null;
        int bestOffset = Integer.MAX_VALUE;

        for (int first = 0; first < row.size(); first++) {
            final AbstractChordInter firstChord = row.get(first);

            if (Math.min(firstChord.getCenter().x, firstChord.getTailLocation().x) > sign.x) {
                break;
            }

            Rational total = firstChord.getDurationSansTuplet();
            final int lastMax = Math.min(row.size(), first + maxChords);

            for (int last = first + 1; (total != null) && (last < lastMax); last++) {
                final AbstractChordInter lastChord = row.get(last);
                final Rational duration = lastChord.getDurationSansTuplet();

                if (duration == null) {
                    break;
                }

                total = total.plus(duration);

                final List<AbstractChordInter> run = row.subList(first, last + 1);
                final int offset = signOffset(row, first, last, sign.x);
                final int width = lastChord.getCenter().x - firstChord.getCenter().x;

                if ((offset <= (maxOffsetRatio * width)) && (offset < bestOffset)
                        && isTupletTotal(total, count)
                        && respectsBeamGroup(row, first, last, factor)
                        && standsBeyond(sign, run)) {
                    bestOffset = offset;
                    best = run;
                }
            }
        }

        return best;
    }

    //---------------//
    // makesUpTuplet //
    //---------------//
    /**
     * Check whether the chords linked to a tuplet still make it up.
     * <p>
     * Where the chords sit was checked when they were linked, which needs their neighbors;
     * what can change since is their durations.
     *
     * @param tuplet the tuplet sign
     * @param chords the chords linked to it
     * @return true if there are at least two chords and their durations add up to the tuplet
     */
    public static boolean makesUpTuplet (TupletInter tuplet,
                                         List<AbstractChordInter> chords)
    {
        if (chords.size() < 2) {
            return false;
        }

        Rational total = Rational.ZERO;

        for (AbstractChordInter chord : chords) {
            final Rational duration = chord.getDurationSansTuplet();

            if (duration == null) {
                return false;
            }

            total = total.plus(duration);
        }

        return isTupletTotal(total, expectedCount(tuplet.getShape()));
    }

    //------------------------//
    // filterChordsOnAbscissa //
    //------------------------//
    /**
     * If two chords overlap horizontally, discard the one with higher vertical distance
     * from the tuplet sign.
     *
     * @param tuplet     underlying tuplet sign
     * @param candidates the chords candidates, ordered by euclidean distance to sign
     */
    private static void filterChordsOnAbscissa (TupletInter tuplet,
                                                List<AbstractChordInter> candidates)
    {
        final Point pt = tuplet.getCenter();

        for (int idx1 = 0; idx1 < candidates.size(); idx1++) {
            final AbstractChordInter ch1 = candidates.get(idx1);
            final Rectangle b1 = ch1.getBounds();
            final Point p1 = ch1.getCenter();

            for (int idx2 = idx1 + 1; idx2 < candidates.size(); idx2++) {
                final AbstractChordInter ch2 = candidates.get(idx2);
                final Rectangle b2 = ch2.getBounds();

                // Chords of one beam group are never alternatives, however close
                final BeamGroupInter g1 = beamGroup(ch1);

                if ((g1 != null) && (g1 == beamGroup(ch2))) {
                    continue;
                }

                if (GeoUtil.xOverlap(b1, b2) > 0) {
                    // Discard ch1 or ch2, based on y-distance
                    final Point p2 = ch2.getCenter();
                    final int d1 = Math.abs(p1.y - pt.y);
                    final int d2 = Math.abs(p2.y - pt.y);

                    if (d1 < d2) {
                        candidates.remove(ch2);
                        idx2--;
                    } else {
                        candidates.remove(ch1);
                        idx1--;
                        break;
                    }
                }
            }
        }
    }

    //-------------------//
    // getEmbracedChords //
    //-------------------//
    /**
     * Report the proper collection of chords that are embraced by the tuplet.
     * <p>
     * They are a run of consecutive chords, rests included, whose durations add up to the tuplet
     * count (3 or 6) of one plain note value, and whose middle abscissa is close to the sign.
     * A run may hold more chords than the count, where a value is split (two 16ths for an eighth),
     * or fewer, where values are merged (a quarter for two eighths).
     * When several runs qualify, the one centered closest to the sign is chosen.
     * <p>
     * A run fits the beam group it holds, and holds no rest lying on the staff half away from
     * the sign, where the rests of another voice are. On a drum staff, it holds only the chords
     * stemmed like the chord closest to the sign.
     * <p>
     * The sign stands beyond the ends of the run chords.
     *
     * @param tuplet     underlying tuplet sign
     * @param candidates the chords candidates, ordered by euclidean distance to sign
     * @return the set of embraced chords, ordered from left to right, or null if retrieval failed
     */
    private static SortedSet<AbstractChordInter> getEmbracedChords (TupletInter tuplet,
                                                                    List<AbstractChordInter> candidates)
    {
        logger.trace("{} getEmbracedChords", tuplet);

        filterChordsOnAbscissa(tuplet, candidates);

        final AbstractChordInter target = getTargetChord(candidates);

        if (target == null) {
            return null;
        }

        // We assume that chords with 2 staves have their tuplet sign above...
        final Staff targetStaff = target.getTopStaff();

        // On a drum staff, voices are told apart by their stem direction
        final int voiceDir = targetStaff.isDrum() ? target.getStemDir() : 0;
        final Point sign = tuplet.getCenter();
        final List<AbstractChordInter> row = new ArrayList<>();

        for (AbstractChordInter chord : candidates) {
            if (chord.getTopStaff() != targetStaff) {
                continue;
            }

            if (chord.isRest()) {
                if (isRestAway(chord, sign, targetStaff)) {
                    continue;
                }
            } else if ((voiceDir != 0) && (chord.getStemDir() == -voiceDir)) {
                continue;
            }

            row.add(chord);
        }

        Collections.sort(row, Inters.byCenterAbscissa);

        final List<AbstractChordInter> best = bestRun(row, tuplet);

        if (best == null) {
            logger.debug("{} no run of chords adds up to its count", tuplet);

            return null;
        }

        final SortedSet<AbstractChordInter> embraced = new TreeSet<>(Inters.byFullAbscissa);
        embraced.addAll(best);

        return embraced;
    }

    //----------------//
    // getTargetChord //
    //----------------//
    /**
     * Report the first head-based chord among the sorted candidates.
     *
     * @param candidates candidates ordered by distance from tuplet
     * @return the target chord, or null if there is none
     */
    private static AbstractChordInter getTargetChord (List<AbstractChordInter> candidates)
    {
        for (AbstractChordInter chord : candidates) {
            if (!chord.isRest()) {
                return chord;
            }
        }

        return null;
    }

    //------------//
    // isRestAway //
    //------------//
    /**
     * Check whether the rest lies on the half of the staff away from the sign, as the rest of
     * another voice does.
     *
     * @param rest  the rest chord candidate
     * @param sign  the tuplet sign center
     * @param staff the staff of the embraced chords
     * @return true if the rest pitch is beyond the staff middle, on the side opposite the sign
     */
    private static boolean isRestAway (AbstractChordInter rest,
                                       Point sign,
                                       Staff staff)
    {
        final double restPitch = staff.pitchPositionOf(rest.getCenter());
        final double signPitch = staff.pitchPositionOf(sign);

        return (Math.signum(restPitch) != Math.signum(signPitch))
                && (Math.abs(restPitch) > constants.maxRestPitchAway.getValue());
    }

    //---------------//
    // isTupletTotal //
    //---------------//
    /**
     * Check whether a total duration is the tuplet count of one plain note value.
     *
     * @param total the total duration of a run of chords
     * @param count the tuplet count, 3 or 6
     * @return true if total / count is 1/2^n
     */
    private static boolean isTupletTotal (Rational total,
                                          int count)
    {
        return isPlainValue(total.divides(count));
    }

    //--------------//
    // isPlainValue //
    //--------------//
    /**
     * Check whether a duration is a plain note value, 1/2^n.
     *
     * @param duration the duration to check
     * @return true if so
     */
    private static boolean isPlainValue (Rational duration)
    {
        return (duration.num == 1) && (Integer.bitCount(duration.den) == 1);
    }

    //------------//
    // signOffset //
    //------------//
    /**
     * Report how far the sign lies from the middle of a run of chords.
     * <p>
     * A number is centered on its beam, thus on the stems, or on its bracket, which spans the
     * heads, and sometimes the time up to the next chord: the closest middle is used.
     *
     * @param row   the candidate chords, ordered by abscissa
     * @param first index of first run chord in row
     * @param last  index of last run chord in row
     * @param signX the sign center abscissa
     * @return the abscissa offset of the sign from the run middle
     */
    private static int signOffset (List<AbstractChordInter> row,
                                   int first,
                                   int last,
                                   int signX)
    {
        final AbstractChordInter firstChord = row.get(first);
        final AbstractChordInter lastChord = row.get(last);
        final int headsMiddle = (firstChord.getCenter().x + lastChord.getCenter().x) / 2;
        final int stemsMiddle = (firstChord.getTailLocation().x + lastChord.getTailLocation().x) / 2;
        int offset = Math.min(Math.abs(signX - headsMiddle), Math.abs(signX - stemsMiddle));

        if (last + 1 < row.size()) {
            final int timeMiddle = (firstChord.getCenter().x + row.get(last + 1).getCenter().x) / 2;
            offset = Math.min(offset, Math.abs(signX - timeMiddle));
        }

        return offset;
    }

    //-------------------//
    // respectsBeamGroup //
    //-------------------//
    /**
     * Check that a run of chords fits the beam group it holds, if any.
     * <p>
     * A run holds at most one beam group. When the group goes beyond the run, the run must start
     * and end on a boundary of its own length: an eighth then a triplet of 16ths, or four
     * triplets of 16ths under one beam, but not three of four beamed eighths.
     *
     * @param row    the abscissa-ordered chords
     * @param first  index of first run chord in row
     * @param last   index of last run chord in row
     * @param factor the tuplet duration factor
     * @return true if the run respects the beam group
     */
    private static boolean respectsBeamGroup (List<AbstractChordInter> row,
                                              int first,
                                              int last,
                                              Rational factor)
    {
        final List<AbstractChordInter> run = row.subList(first, last + 1);
        BeamGroupInter group = null;

        for (AbstractChordInter chord : run) {
            final BeamGroupInter g = beamGroup(chord);

            if (g != null) {
                if ((group != null) && (g != group)) {
                    return false;
                }

                group = g;
            }
        }

        if (group == null) {
            return true;
        }

        final List<AbstractChordInter> groupChords = new ArrayList<>();
        int left = Integer.MAX_VALUE;
        int right = Integer.MIN_VALUE;

        for (AbstractChordInter chord : group.getChords()) {
            if (!chord.isRest()) {
                groupChords.add(chord);
                left = Math.min(left, chord.getCenter().x);
                right = Math.max(right, chord.getCenter().x);
            }
        }

        if (run.containsAll(groupChords)) {
            return true;
        }

        // Durations of the group chords before and after the run
        Rational before = Rational.ZERO;
        Rational after = Rational.ZERO;
        Rational total = Rational.ZERO;

        for (int i = 0; i < row.size(); i++) {
            final AbstractChordInter chord = row.get(i);
            final int x = chord.getCenter().x;
            final boolean inRun = (i >= first) && (i <= last);

            if (!inRun && ((x < left) || (x > right))) {
                continue;
            }

            final Rational duration = chord.getDurationSansTuplet();

            if (duration == null) {
                return false;
            }

            if (inRun) {
                total = total.plus(duration);
            } else if (i < first) {
                before = before.plus(duration);
            } else {
                after = after.plus(duration);
            }
        }

        final Rational played = total.times(factor);

        return isBoundary(before, total, played) && isBoundary(after, total, played);
    }

    //------------//
    // isBoundary //
    //------------//
    /**
     * Check whether a duration of neighbor chords ends on a boundary of the run length,
     * counting the neighbors as tuplets like the run or as plain values.
     *
     * @param neighbors the duration of the neighbor chords
     * @param total     the run duration, as written
     * @param played    the run duration, as played
     * @return true if neighbors is a multiple of total or of played
     */
    private static boolean isBoundary (Rational neighbors,
                                       Rational total,
                                       Rational played)
    {
        return (neighbors.divides(total).den == 1) || (neighbors.divides(played).den == 1);
    }

    //--------------//
    // standsBeyond //
    //--------------//
    /**
     * Check whether the sign stands beyond the ends of all chords of a run.
     * <p>
     * A tuplet number is printed past the stems or past the heads of the chords it embraces,
     * never among them.
     *
     * @param sign the sign center
     * @param run  the run of chords
     * @return true if the sign is above all chords or below all chords
     */
    private static boolean standsBeyond (Point sign,
                                         List<AbstractChordInter> run)
    {
        boolean above = true;
        boolean below = true;

        for (AbstractChordInter chord : run) {
            final Rectangle box = chord.getBounds();
            above &= sign.y < box.y;
            below &= sign.y >= (box.y + box.height);
        }

        return above || below;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-------------//
    // ByEuclidean //
    //-------------//
    private static class ByEuclidean
            implements Comparator<AbstractChordInter>
    {
        /** The location of the tuplet sign */
        private final Point signPoint;

        ByEuclidean (Point signPoint)
        {
            this.signPoint = signPoint;
        }

        /** Compare their euclidean distance from the signPoint reference */
        @Override
        public int compare (AbstractChordInter c1,
                            AbstractChordInter c2)
        {
            double dx1 = GeoUtil.ptDistanceSq(c1.getBounds(), signPoint.x, signPoint.y);
            double dx2 = GeoUtil.ptDistanceSq(c2.getBounds(), signPoint.x, signPoint.y);

            return Double.compare(dx1, dx2);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer maxChordsPerItem = new Constant.Integer(
                "chords",
                2,
                "Maximum number of chords embraced per tuplet item");

        private final Constant.Ratio maxSignOffset = new Constant.Ratio(
                0.25,
                "Maximum abscissa offset of the sign from the middle of its chords, per run width");

        private final Constant.Double maxRestPitchAway = new Constant.Double(
                "PitchPosition",
                2.0,
                "Maximum pitch position of an embraced rest, on the staff half away from the sign");
    }
}
