//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T u p l e t s B u i l d e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.Shape;

import omr.math.GeoUtil;
import omr.math.Rational;

import omr.sig.SIGraph;
import omr.sig.inter.ChordInter;
import omr.sig.inter.Inter;
import omr.sig.inter.Inters;
import omr.sig.inter.TupletInter;
import omr.sig.relation.TupletChordRelation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
     * Its neighborhood is limited in its part, vertically to staff above and staff below and
     * horizontally to its containing measure stack.
     *
     * @return a list, perhaps empty, of wrong tuplet instances to delete
     */
    public List<TupletInter> linkTuplets ()
    {
        final List<TupletInter> tuplets = stack.getTuplets();
        final SIGraph sig = stack.getSystem().getSig();
        final List<TupletInter> toDelete = new ArrayList<TupletInter>();

        for (TupletInter tuplet : tuplets) {
            // Clear existing tuplet-chord relations, if any
            sig.removeAllEdges(sig.getRelations(tuplet, TupletChordRelation.class));

            // Try to link tuplet with proper chords found in measure stack
            // (just staff above and staff below)
            List<ChordInter> candidates = getChordsAround(tuplet);

            // Now, get the properly embraced chords
            SortedSet<ChordInter> chords = getEmbracedChords(tuplet, candidates);

            if (chords != null) {
                logger.debug("{} connectable to {}", tuplet, chords);

                for (ChordInter chord : chords) {
                    sig.addEdge(tuplet, chord, new TupletChordRelation(tuplet.getShape()));
                    chord.setTupletFactor(tuplet.getDurationFactor()); // Too early? TODO
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
     * @return the set of embraced chords, ordered from left to right, or null
     *         when the retrieval has failed
     */
    private static SortedSet<ChordInter> getEmbracedChords (TupletInter tuplet,
                                                            List<ChordInter> candidates)
    {
        logger.debug("{} getEmbracedChords", tuplet);

        // We consider each candidate in turn, with its duration
        // in order to determine the duration base of the tuplet
        TupletCollector collector = new TupletCollector(
                tuplet,
                new TreeSet<ChordInter>(Inter.byFullAbscissa));

        final Staff targetStaff = getTargetStaff(candidates);

        for (ChordInter chord : candidates) {
            Staff staff = chord.getStaff();

            // Check that all chords are on the same staff (for head-based chords)
            // For rest-based chords we must relax the rule
            if (chord.isRest()) {
                // Rest staff is not very precise
                if (Math.abs(staff.getId() - targetStaff.getId()) > 1) {
                    continue;
                }
            } else if (staff != targetStaff) {
                continue;
            }

            collector.include(chord);

            // Check we have collected the exact amount of time
            // TODO: Test is questionable for non-reliable candidates
            if (collector.isTooLong()) {
                logger.info("{} {}", tuplet, collector.getStatusMessage());

                return null;
            } else if (collector.isOutside()) {
                logger.info("{} {}", tuplet, collector.getStatusMessage());

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
     * Report the list of ChordInter instances in the neighborhood of the specified Inter.
     * Neighborhood is limited horizontally by the measure sides and vertically by the staves above
     * and below inter.
     *
     * @param tuplet the inter of interest
     * @return the lift of neighbors, sorted by euclidian distance to tuplet sign
     */
    private List<ChordInter> getChordsAround (TupletInter tuplet)
    {
        Point center = tuplet.getCenter();
        List<Staff> stavesAround = stack.getSystem().getStavesAround(center);
        logger.debug("{} around:{}", tuplet, stavesAround);

        // Collect candidate chords (heads & rests based) in proper measure & staves
        List<ChordInter> chords = new ArrayList<ChordInter>();

        for (ChordInter chord : stack.getChords()) {
            if (stavesAround.contains(chord.getStaff())) {
                chords.add(chord);
            }
        }

        Collections.sort(chords, new ByEuclidian(center));
        logger.debug("Chords: {}", Inters.ids(chords));

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
    private static Staff getTargetStaff (List<ChordInter> candidates)
    {
        for (ChordInter chord : candidates) {
            if (!chord.isRest()) {
                return chord.getStaff();
            }
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // ByEuclidian //
    //-------------//
    private static class ByEuclidian
            implements Comparator<ChordInter>
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
        public int compare (ChordInter c1,
                            ChordInter c2)
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
            implements Comparator<ChordInter>
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
        public int compare (ChordInter c1,
                            ChordInter c2)
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
     */
    private static class TupletCollector
    {
        //~ Enumerations ---------------------------------------------------------------------------

        /** Describe the current status of the tuplet collector */
        public enum Status
        {
            //~ Enumeration constant initializers --------------------------------------------------

            TOO_SHORT("Too short"),
            OK("Correct"),
            TOO_LONG("Too long"),
            OUTSIDE("Outside chords");

            //~ Instance fields --------------------------------------------------------------------
            final String label;

            //~ Constructors -----------------------------------------------------------------------
            private Status (String label)
            {
                this.label = label;
            }
        }

        //~ Instance fields ------------------------------------------------------------------------
        /** Underlying sign. */
        private final TupletInter tuplet;

        /** Number of base items expected. */
        private final int expectedCount;

        /** The chords collected so far. */
        private final SortedSet<ChordInter> chords;

        /** The base duration as identified so far. */
        private Rational base = Rational.MAX_VALUE;

        /** The total duration expected (using the known base). */
        private Rational expectedTotal = Rational.MAX_VALUE;

        /** The total duration so far. */
        private Rational total = Rational.ZERO;

        /** Current status. */
        private Status status = Status.TOO_SHORT;

        //~ Constructors ---------------------------------------------------------------------------
        public TupletCollector (TupletInter tuplet,
                                SortedSet<ChordInter> chords)
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

            for (ChordInter chord : chords) {
                sb.append("\n").append(chord);
            }

            logger.debug(sb.toString());
        }

        public SortedSet<ChordInter> getChords ()
        {
            return chords;
        }

        public String getStatusMessage ()
        {
            if (logger.isDebugEnabled()) {
                dump();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(status.label).append(" sequence in ").append(tuplet.getShape()).append(": ").append(
                    total);

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
        public void include (ChordInter chord)
        {
            if (chords.add(chord)) {
                Rational duration = chord.getRawDuration();
                total = total.plus(duration);

                // If this is a shorter chord, let's update the base
                if (duration.compareTo(base) < 0) {
                    base = duration;
                    expectedTotal = base.times(expectedCount);
                }

                // Update status
                if (total.equals(expectedTotal)) {
                    // Check tuplet sign is within chords abscissae
                    if (isWithinChords()) {
                        status = Status.OK;
                    } else {
                        status = Status.OUTSIDE;
                    }
                } else if (total.compareTo(expectedTotal) > 0) {
                    status = Status.TOO_LONG;
                }
            }
        }

        /** Include a bunch of chords, all in a row */
        public void includeAll (Collection<ChordInter> newChords)
        {
            for (ChordInter chord : newChords) {
                include(chord);
            }
        }

        public boolean isOk ()
        {
            return status == Status.OK;
        }

        public boolean isOutside ()
        {
            return status == Status.OUTSIDE;
        }

        public boolean isTooLong ()
        {
            return status == Status.TOO_LONG;
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
