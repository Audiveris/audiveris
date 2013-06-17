//----------------------------------------------------------------------------//
//                                                                            //
//                                T u p l e t                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import java.util.ArrayList;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.Rational;

import omr.score.visitor.ScoreVisitor;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code Tuplet} represents a tuplet notation and encapsulates
 * the translation from tuplet glyph to the impacted chords.
 *
 * @author Hervé Bitteur
 */
public class Tuplet
        extends AbstractNotation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Tuplet.class);

    //~ Instance fields --------------------------------------------------------
    //
    /**
     * Set of chords involved in the tuplet sequence.
     */
    private final SortedSet<Chord> chords;

    //~ Constructors -----------------------------------------------------------
    //
    //--------//
    // Tuplet //
    //--------//
    /**
     * Creates a new instance of Tuplet event
     *
     * @param measure measure that contains this tuplet
     * @param point   location of tuplet sign
     * @param chords  the embraced chords
     * @param glyph   the underlying glyph
     */
    private Tuplet (Measure measure,
                    Point point,
                    SortedSet<Chord> chords,
                    Glyph glyph)
    {
        super(measure, point, chords.first(), glyph);

        this.chords = chords;

        // Apply the tuplet factor to each chord embraced
        DurationFactor factor = getFactor(glyph);

        for (Chord chord : chords) {
            chord.setTupletFactor(factor);
        }

        // Link last embraced chord to this tuplet instance
        chords.last().addNotation(this);
    }

    //~ Methods ----------------------------------------------------------------
    //
    //----------//
    // populate //
    //----------//
    /**
     * Used by SystemTranslator to allocate the tuplet instances.
     *
     * @param glyph   underlying glyph
     * @param measure containing measure
     * @param point   location for the sign
     */
    public static void populate (Glyph glyph,
                                 Measure measure,
                                 Point point)
    {
        if (glyph.isVip()) {
            logger.info("Tuplet. populate {}", glyph);
        }

        // Let's gather the set of possible chords, ordered by their distance
        // (abscissa-based) to the position of the tuplet sign.
        List<Chord> candidates = new ArrayList<>();
        for (TreeNode node : measure.getChords()) {
            Chord chord = (Chord) node;
            if (chord.getReferencePoint() != null) {
                // No tuplet on a whole
                if (!chord.isWholeDuration()) {
                    candidates.add(chord);
                }
            }
        }
        Collections.sort(candidates, new DxComparator(point));

        // Now, get the properly embraced chords
        SortedSet<Chord> chords = getEmbracedChords(
                glyph,
                measure,
                point,
                candidates,
                null);

        if (chords != null) {
            glyph.setTranslation(
                    new Tuplet(
                    measure,
                    point,
                    chords,
                    glyph));
        } else {
            // Nullify shape unless manual
            if (!glyph.isManualShape()) {
                glyph.setShape(null);
            }
        }
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //---------------------//
    // getTranslationLinks //
    //---------------------//
    @Override
    public List<Line2D> getTranslationLinks (Glyph glyph)
    {
        List<Line2D> links = new ArrayList<>();

        for (Chord chord : chords) {
            links.addAll(chord.getTranslationLinks(glyph));
        }

        return links;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return super.internalsString() + " " + chords.last();
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
     * Report the proper collection of chords that are embraced by the
     * tuplet
     *
     * @param glyph         underlying glyph
     * @param measure       measure where the sign is located
     * @param point         location for the sign
     * @param candidates    the chords candidates, ordered wrt distance to sign
     * @param requiredStaff the required containing staff if known, or null
     * @return the set of embraced chords, ordered from left to right, or null
     *         when the retrieval has failed
     */
    private static SortedSet<Chord> getEmbracedChords (Glyph glyph,
                                                       Measure measure,
                                                       Point point,
                                                       List<Chord> candidates,
                                                       Staff requiredStaff)
    {
        logger.debug("{} {}{}",
                glyph.getShape(),
                glyph.idString(),
                (requiredStaff != null)
                ? (" staff#" + requiredStaff.getId())
                : "");

        // We consider each candidate in turn, with its duration
        // in order to determine the duration base of the tuplet
        TupletCollector collector = new TupletCollector(
                glyph,
                new TreeSet<Chord>(Chord.byAbscissa));

        // Check that all chords are on the same staff
        Staff commonStaff = null;

        for (Chord chord : candidates) {
            Staff staff = chord.getStaff();

            // If we have a constraint on the staff, let's use it
            if ((requiredStaff != null) && (requiredStaff != staff)) {
                continue;
            }

            if (commonStaff == null) {
                commonStaff = staff;
            } else if (staff != commonStaff) {
                // We have chords in different staves, we must fix that.
                // We choose the closest in ordinate of the chords so far
                SortedSet<Chord> verticals = new TreeSet<>(
                        new DyComparator(point));

                for (Chord ch : candidates) {
                    verticals.add(ch);

                    if (ch == chord) {
                        break;
                    }
                }

                // Now, we can impose the staff!
                return getEmbracedChords(
                        glyph,
                        measure,
                        point,
                        candidates,
                        verticals.first().getStaff());
            }

            collector.include(chord);

            // Check we have collected the exact amount of time
            if (collector.isTooLong()) {
                measure.addError(glyph, collector.getStatusMessage());

                return null;
            } else if (collector.isOutside()) {
                measure.addError(glyph, collector.getStatusMessage());

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
        measure.addError(glyph, collector.getStatusMessage());

        return null;
    }

    //-----------//
    // getFactor //
    //-----------//
    /**
     * Report the tuplet factor that corresponds to the provided tuplet
     * sign
     *
     * @param glyph the tuplet sign
     * @return the related factor
     */
    private static DurationFactor getFactor (Glyph glyph)
    {
        switch (glyph.getShape()) {
        case TUPLET_THREE:
            return new DurationFactor(2, 3);

        case TUPLET_SIX:
            return new DurationFactor(4, 6);

        default:
            logger.error("Incorrect tuplet glyph shape");

            return null;
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //--------------//
    // DxComparator //
    //--------------//
    private static class DxComparator
            implements Comparator<Chord>
    {
        //~ Instance fields ----------------------------------------------------

        /** The location of the tuplet sign */
        private final Point signPoint;

        //~ Constructors -------------------------------------------------------
        public DxComparator (Point signPoint)
        {
            this.signPoint = signPoint;
        }

        //~ Methods ------------------------------------------------------------
        /** Compare their horizontal distance from the signPoint reference */
        @Override
        public int compare (Chord c1,
                            Chord c2)
        {
            int dx1 = Math.abs(c1.getReferencePoint().x - signPoint.x);
            int dx2 = Math.abs(c2.getReferencePoint().x - signPoint.x);

            return Integer.signum(dx1 - dx2);
        }
    }

    //--------------//
    // DyComparator //
    //--------------//
    private static class DyComparator
            implements Comparator<Chord>
    {
        //~ Instance fields ----------------------------------------------------

        /** The location of the tuplet sign */
        private final Point signPoint;

        //~ Constructors -------------------------------------------------------
        public DyComparator (Point signPoint)
        {
            this.signPoint = signPoint;
        }

        //~ Methods ------------------------------------------------------------
        /** Compare their vertical distance from the signPoint reference */
        @Override
        public int compare (Chord c1,
                            Chord c2)
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
     * In charge of incrementally collecting the chords for a given
     * tuplet sign.
     */
    private static class TupletCollector
    {
        //~ Enumerations -------------------------------------------------------

        /** Describe the current status of the tuplet collector */
        public enum Status
        {
            //~ Enumeration constant initializers ------------------------------

            TOO_SHORT("Too short"),
            OK("Correct"),
            TOO_LONG("Too long"),
            OUTSIDE("Outside chords");

            //~ Instance fields ------------------------------------------------
            final String label;

            //~ Constructors ---------------------------------------------------
            private Status (String label)
            {
                this.label = label;
            }
        }

        //~ Instance fields ----------------------------------------------------
        /** Underlying glyph */
        private final Glyph glyph;

        /** Number of base items expected */
        private final int expectedCount;

        /** The chords collected so far */
        private final SortedSet<Chord> chords;

        /** The base duration as identified so far */
        private Rational base = Rational.MAX_VALUE;

        /** The total duration expected (using the known base) */
        private Rational expectedTotal = Rational.MAX_VALUE;

        /** The total duration so far */
        private Rational total = Rational.ZERO;

        /** Current status */
        private Status status = Status.TOO_SHORT;

        //~ Constructors -------------------------------------------------------
        public TupletCollector (Glyph glyph,
                                SortedSet<Chord> chords)
        {
            this.glyph = glyph;
            expectedCount = expectedCount(glyph.getShape());
            this.chords = chords;
        }

        //~ Methods ------------------------------------------------------------
        public void dump ()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(glyph.getShape());

            sb.append(" ").append(glyph.idString());

            sb.append(" ").append(status);

            sb.append(" Base:").append(base);

            sb.append(" ExpectedTotal:").append(expectedTotal);

            sb.append(" Total:").append(total);

            for (Chord chord : chords) {
                sb.append("\n").append(chord);
            }

            logger.debug(sb.toString());
        }

        public SortedSet<Chord> getChords ()
        {
            return chords;
        }

        public String getStatusMessage ()
        {
            if (logger.isDebugEnabled()) {
                dump();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(status.label).append(" sequence in ")
                    .append(glyph.getShape()).append(": ").append(total);

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
        public void include (Chord chord)
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
        public void includeAll (Collection<Chord> newChords)
        {
            for (Chord chord : newChords) {
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

        /** Check whether the tuplet sign lies between the chords abscissae */
        private boolean isWithinChords ()
        {
            int signX = glyph.getAreaCenter().x;

            return (signX >= chords.first().getTailLocation().x)
                   && (signX <= chords.last().getTailLocation().x);
        }
    }
}
