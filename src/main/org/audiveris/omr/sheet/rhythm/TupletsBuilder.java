//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T u p l e t s B u i l d e r                                  //
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.relation.BeamHeadRelation;
import org.audiveris.omr.sig.relation.ChordTupletRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
    //------------------//
    // linkStackTuplets //
    //------------------//
    /**
     * Try to link all tuplet signs in stack.
     * <p>
     * This method is used by RHYTHMS step, when these tuplets signs are not certain at all.
     * <p>
     * A tuplet sign embraces a specific number of notes (heads / rests).
     * <p>
     * Its neighborhood is limited in its part, vertically to staff above and staff below and
     * horizontally to its containing measure stack.
     *
     * @return a set, perhaps empty, of wrong tuplet instances to delete
     */
    public Set<TupletInter> linkStackTuplets ()
    {
        final Set<TupletInter> toDelete = new LinkedHashSet<TupletInter>();
        final Set<TupletInter> tuplets = stack.getTuplets();

        for (TupletInter tuplet : tuplets) {
            // Clear existing tuplet-chord relations, if any
            final SIGraph sig = stack.getSystem().getSig();
            sig.removeAllEdges(sig.getRelations(tuplet, ChordTupletRelation.class));

            Collection<Link> links = lookupLinks(tuplet);

            if (!links.isEmpty()) {
                for (Link link : links) {
                    link.applyTo(tuplet);
                }
            } else {
                toDelete.add(tuplet);
            }
        }

        return toDelete;
    }

    //--------------------//
    // lookupLinks //
    //--------------------//
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

        List<Link> links = new ArrayList<Link>();

        for (AbstractChordInter chord : chords) {
            links.add(new Link(chord, new ChordTupletRelation(tuplet.getShape()), false));
        }

        return links;
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
                new TreeSet<AbstractChordInter>(Inters.byFullAbscissa));

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

                return collector.getChords(); // Normal exit
            }
        }

        // Candidates are exhausted, we lack chords
        logger.info("{} {}", tuplet, collector.getStatusMessage());

        return null;
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

        /** The maximum number of base items expected. (TODO: this is not always true) */
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
                // TODO: this is not always true, and thus should be refined!
                if (chords.size() > expectedCount) {
                    status = Status.TOO_MANY;
                } else if (total.compareTo(expectedTotal) > 0) {
                    status = Status.TOO_LONG;
                } else if (total.equals(expectedTotal)) {
                    // Check tuplet sign is within chords abscissae
                    if (isWithinChordsAbscissaRange()) {
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
                // TODO: this is not always true.
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
            Set<AbstractChordInter> set = new LinkedHashSet<AbstractChordInter>();

            for (AbstractBeamInter beam : chord.getBeams()) {
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
        private boolean isWithinChordsAbscissaRange ()
        {
            int signX = tuplet.getCenter().x;

            return (signX >= chords.first().getTailLocation().x)
                   && (signX <= chords.last().getTailLocation().x);
        }
    }
}
