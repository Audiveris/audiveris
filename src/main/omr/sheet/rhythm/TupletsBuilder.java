//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T u p l e t s B u i l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.rhythm;

import omr.glyph.Shape;

import omr.math.GeoUtil;
import omr.math.Rational;

import omr.sheet.Staff;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractBeamInter;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.Inter;
import omr.sig.inter.Inters;
import omr.sig.inter.TupletInter;
import omr.sig.relation.BeamHeadRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.ChordTupletRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code TupletsBuilder} tries to connect every tuplet symbol in a measure stack
 * to its embraced chords.
 *
 * @author Hervé Bitteur
 */
public class TupletsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TupletsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated measure stack. */
    private final MeasureStack stack;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TupletsBuilder} object.
     *
     * @param stack the dedicated stack
     */
    public TupletsBuilder (MeasureStack stack)
    {
        this.stack = stack;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // linkTuplets //
    //-------------//
    /**
     * A tuplet sign embraces a specific number of notes (heads / rests).
     * <p>
     * Its neighborhood is limited in its part, vertically to staff above and staff below and
     * horizontally to its containing measure stack.
     *
     * @return a set, perhaps empty, of wrong tuplet instances to delete
     */
    public Set<TupletInter> linkTuplets ()
    {
        final SIGraph sig = stack.getSystem().getSig();
        final Set<TupletInter> toDelete = new LinkedHashSet<TupletInter>();
        final Set<TupletInter> tuplets = stack.getTuplets();

        for (TupletInter tuplet : tuplets) {
            // Clear existing tuplet-chord relations, if any
            sig.removeAllEdges(sig.getRelations(tuplet, ChordTupletRelation.class));

            // Try to link tuplet with proper chords found in measure stack
            // (just staff above and staff below)
            List<AbstractChordInter> candidates = getChordsAround(tuplet);

            // Now, get the properly embraced chords
            SortedSet<AbstractChordInter> chords = getEmbracedChords(tuplet, candidates);

            if (chords != null) {
                logger.trace("{} connectable to {}", tuplet, chords);

                for (AbstractChordInter chord : chords) {
                    sig.addEdge(chord, tuplet, new ChordTupletRelation(tuplet.getShape()));
                    chord.setTupletFactor(tuplet.getDurationFactor());
                }
            } else {
                toDelete.add(tuplet);
            }
        }

        return toDelete;
    }

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
        switch (shape) {
        case TUPLET_THREE:
            return 3;

        case TUPLET_SIX:
            return 6;

        default:
            logger.error("Incorrect tuplet shape");

            return 0;
        }
    }

    //-------------------//
    // getEmbracedChords //
    //-------------------//
    /**
     * Report the proper collection of chords that are embraced by the tuplet.
     *
     * @param tuplet     underlying tuplet sign
     * @param candidates the chords candidates, ordered by euclidian distance to sign
     * @return the set of embraced chords, ordered from left to right, or null if retrieval failed
     */
    private static SortedSet<AbstractChordInter> getEmbracedChords (TupletInter tuplet,
                                                                    List<AbstractChordInter> candidates)
    {
        logger.trace("{} getEmbracedChords", tuplet);

        // We consider each candidate in turn, with its duration
        // in order to determine the duration base of the tuplet
        TupletCollector collector = new TupletCollector(
                tuplet,
                new TreeSet<AbstractChordInter>(Inter.byFullAbscissa));

        final Staff targetStaff = getTargetStaff(candidates);

        for (AbstractChordInter chord : candidates) {
            // We assume that chords with 2 staves have their tuplet sign above...
            Staff staff = chord.getTopStaff();

            // Check that all chords are on the same staff
            if (staff != targetStaff) {
                continue;
            }

            collector.include(chord);

            // Check we have collected the exact amount of time
            // TODO: Test is questionable for non-reliable candidates
            if (collector.isNotOk()) {
                logger.debug("{} {}", tuplet, collector.getStatusMessage());

                return null;
            } else if (collector.isOk()) {
                if (logger.isDebugEnabled()) {
                    collector.dump();
                }

                // Normal exit
                return collector.getChords();
            }
        }

        // Candidates are exhausted, we lack chords
        logger.info("{} {}", tuplet, collector.getStatusMessage());

        return null;
    }

    //-----------------//
    // getChordsAround //
    //-----------------//
    /**
     * Report the list of AbstractChordInter instances (rests & heads) in the
     * neighborhood of the specified Inter.
     * Neighborhood is limited horizontally by the measure sides and vertically by the staves above
     * and below.
     *
     * @param tuplet the inter of interest
     * @return the list of neighbors, sorted by euclidian distance to tuplet sign
     */
    private List<AbstractChordInter> getChordsAround (TupletInter tuplet)
    {
        Point center = tuplet.getCenter();
        List<Staff> stavesAround = stack.getSystem().getStavesAround(center);
        logger.trace("{} around:{}", tuplet, stavesAround);

        // Collect candidate chords (heads & rests based) within stack
        List<AbstractChordInter> chords = new ArrayList<AbstractChordInter>();

        for (AbstractChordInter chord : stack.getStandardChords()) {
            final List<Staff> chordStaves = new ArrayList<Staff>(chord.getStaves());
            chordStaves.retainAll(stavesAround);

            if (!chordStaves.isEmpty()) {
                chords.add(chord);
            }
        }

        Collections.sort(chords, new ByEuclidian(center));
        logger.trace("Chords: {}", Inters.ids(chords));

        return chords;
    }

    //----------------//
    // getTargetStaff //
    //----------------//
    /**
     * Report the staff of first head-based chord among the sorted candidates
     *
     * @param candidates candidates ordered by distance from tuplet
     * @return the target staff
     */
    private static Staff getTargetStaff (List<AbstractChordInter> candidates)
    {
        for (AbstractChordInter chord : candidates) {
            if (!chord.isRest()) {
                return chord.getTopStaff();
            }
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // ByEuclidian //
    //-------------//
    private static class ByEuclidian
            implements Comparator<AbstractChordInter>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The location of the tuplet sign */
        private final Point signPoint;

        //~ Constructors ---------------------------------------------------------------------------
        public ByEuclidian (Point signPoint)
        {
            this.signPoint = signPoint;
        }

        //~ Methods --------------------------------------------------------------------------------
        /** Compare their euclidian distance from the signPoint reference */
        @Override
        public int compare (AbstractChordInter c1,
                            AbstractChordInter c2)
        {
            double dx1 = GeoUtil.ptDistanceSq(c1.getBounds(), signPoint.x, signPoint.y);
            double dx2 = GeoUtil.ptDistanceSq(c2.getBounds(), signPoint.x, signPoint.y);

            return Double.compare(dx1, dx2);
        }
    }

    //--------------//
    // DyComparator //
    //--------------//
    private static class DyComparator
            implements Comparator<AbstractChordInter>
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The location of the tuplet sign */
        private final Point signPoint;

        //~ Constructors ---------------------------------------------------------------------------
        public DyComparator (Point signPoint)
        {
            this.signPoint = signPoint;
        }

        //~ Methods --------------------------------------------------------------------------------
        /** Compare their vertical distance from the signPoint reference */
        @Override
        public int compare (AbstractChordInter c1,
                            AbstractChordInter c2)
        {
            int dy1 = Math.min(
                    Math.abs(c1.getHeadLocation().y - signPoint.y),
                    Math.abs(c1.getTailLocation().y - signPoint.y));
            int dy2 = Math.min(
                    Math.abs(c2.getHeadLocation().y - signPoint.y),
                    Math.abs(c2.getTailLocation().y - signPoint.y));

            return Integer.signum(dy1 - dy2);
        }
    }

    //-----------------//
    // TupletCollector //
    //-----------------//
    /**
     * In charge of incrementally collecting the chords for a given tuplet sign.
     * <p>
     * If some of its embraced heads are linked to a beam, then the whole beam of heads must be
     * embraced by the tuplet. If not, this is not a true tuplet.
     * <p>
     * The tuplet must be located close to the middle abscissa of the embraced notes.
     * Vertically, it should be away from the middle chord(s).
     */
    private static class TupletCollector
    {
        //~ Enumerations ---------------------------------------------------------------------------

        /** Describe the current status of the tuplet collector */
        public enum Status
        {
            //~ Enumeration constant initializers --------------------------------------------------

            TOO_SHORT,
            OK,
            TOO_LONG,
            TOO_MANY,
            OUTSIDE;
        }

        //~ Instance fields ------------------------------------------------------------------------
        /** Underlying sign. */
        private final TupletInter tuplet;

        /** The maximum number of base items expected. */
        private final int expectedCount;

        /** The chords collected so far. */
        private final SortedSet<AbstractChordInter> chords;

        /** The base duration as identified so far. */
        private Rational base = Rational.MAX_VALUE;

        /** The total duration expected (using the known base). */
        private Rational expectedTotal = Rational.MAX_VALUE;

        /** The total duration so far. */
        private Rational total = Rational.ZERO;

        /** Current status. */
        private Status status;

        //~ Constructors ---------------------------------------------------------------------------
        public TupletCollector (TupletInter tuplet,
                                SortedSet<AbstractChordInter> chords)
        {
            this.tuplet = tuplet;
            expectedCount = expectedCount(tuplet.getShape());
            this.chords = chords;
        }

        //~ Methods --------------------------------------------------------------------------------
        public void dump ()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(tuplet);

            sb.append(" ").append(status);

            sb.append(" Base:").append(base);

            sb.append(" ExpectedTotal:").append(expectedTotal);

            sb.append(" Total:").append(total);

            for (AbstractChordInter chord : chords) {
                sb.append("\n").append(chord);
            }

            logger.info(sb.toString());
        }

        public SortedSet<AbstractChordInter> getChords ()
        {
            return chords;
        }

        public String getStatusMessage ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append(status).append(" sequence in ").append(tuplet.getShape()).append(": ")
                    .append(total);

            if (expectedTotal != Rational.MAX_VALUE) {
                sb.append(" vs ").append(expectedTotal);
            }

            return sb.toString();
        }

        public Rational getTotal ()
        {
            return total;
        }

        /** Include a chord into the collection */
        public void include (AbstractChordInter chord)
        {
            if (!chords.contains(chord)) {
                // Chord together with its beam-siblings
                Set<AbstractChordInter> siblings = getBeamSiblings(chord);
                siblings.add(chord);

                for (AbstractChordInter ch : siblings) {
                    doInclude(ch);
                }

                // Check count and duration
                if (chords.size() > expectedCount) {
                    status = Status.TOO_MANY;
                } else if (total.compareTo(expectedTotal) > 0) {
                    status = Status.TOO_LONG;
                } else if (total.equals(expectedTotal)) {
                    // Check tuplet sign is within chords abscissae
                    if (isWithinChords()) {
                        status = Status.OK;
                    } else {
                        status = Status.OUTSIDE;
                    }
                }
            }
        }

        public boolean isNotOk ()
        {
            return (status != null) && (status != Status.OK);
        }

        public boolean isOk ()
        {
            return status == Status.OK;
        }

        /** Include a chord into the collection */
        private void doInclude (AbstractChordInter chord)
        {
            if (chords.add(chord)) {
                Rational sansTuplet = chord.getDurationSansTuplet();
                total = total.plus(sansTuplet);

                // If this is a shorter chord, let's update the base
                if (sansTuplet.compareTo(base) < 0) {
                    base = sansTuplet;
                    expectedTotal = base.times(expectedCount);
                }
            }
        }

        /** Retrieve all chords linked via a beam to the provided chord. */
        private Set<AbstractChordInter> getBeamSiblings (AbstractChordInter chord)
        {
            SIGraph sig = chord.getSig();
            Set<AbstractChordInter> set = new HashSet<AbstractChordInter>();
            List<AbstractBeamInter> beams = chord.getBeams();

            for (AbstractBeamInter beam : beams) {
                Set<Relation> bhRels = sig.getRelations(beam, BeamHeadRelation.class);

                for (Relation bh : bhRels) {
                    Inter head = sig.getOppositeInter(beam, bh);
                    AbstractChordInter ch = (AbstractChordInter) head.getEnsemble();
                    set.add(ch);
                }
            }

            return set;
        }

        /** Check whether the tuplet sign lies between the chords abscissae. */
        private boolean isWithinChords ()
        {
            int signX = tuplet.getCenter().x;

            return (signX >= chords.first().getTailLocation().x)
                   && (signX <= chords.last().getTailLocation().x);
        }
    }
}
