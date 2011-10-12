//----------------------------------------------------------------------------//
//                                                                            //
//                                T u p l e t                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.math.Rational;

import omr.score.common.DurationFactor;
import omr.score.common.PixelPoint;
import omr.score.visitor.ScoreVisitor;

import omr.util.TreeNode;

import java.util.Collection;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class <code>Tuplet</code> represents a tuplet notation and encapsulates the
 * translation from tuplet glyph to the impacted chords.
 *
 * @author Hervé Bitteur
 */
public class Tuplet
    extends AbstractNotation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Tuplet.class);

    //~ Instance fields --------------------------------------------------------

    /** Last chord in the tuplet sequence */
    private final Chord lastChord;

    //~ Constructors -----------------------------------------------------------

    //--------//
    // Tuplet //
    //--------//
    /**
     * Creates a new instance of Tuplet event
     *
     * @param measure measure that contains this tuplet
     * @param point location of tuplet sign
     * @param firstChord the first embraced chord
     * @param lastChord the last embraced chord
     * @param glyph the underlying glyph
     */
    public Tuplet (Measure    measure,
                   PixelPoint point,
                   Chord      firstChord,
                   Chord      lastChord,
                   Glyph      glyph)
    {
        super(measure, point, firstChord, glyph);

        // Link last embraced chords to this tuplet instance
        this.lastChord = lastChord;

        if (lastChord != null) {
            lastChord.addNotation(this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // populate //
    //----------//
    /**
     * Used by SystemTranslator to allocate the tuplet instances
     *
     * @param glyph underlying glyph
     * @param measure containing measure
     * @param point location for the sign
     */
    public static void populate (Glyph      glyph,
                                 Measure    measure,
                                 PixelPoint point)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Tuplet. populate " + glyph);
        }

        // Let's gather the set of possible chords, ordered by their distance
        // (abscissa-based) to the position of the tuplet sign.
        SortedSet<Chord> candidates = new TreeSet<Chord>(
            new DxComparator(point));

        for (TreeNode node : measure.getChords()) {
            Chord chord = (Chord) node;

            // No tuplet on a whole
            if (!chord.isWholeDuration()) {
                candidates.add(chord);
            }
        }

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
                    chords.first(),
                    chords.last(),
                    glyph));

            // Apply the tuplet factor to each chord embraced
            DurationFactor factor = getFactor(glyph);

            for (Chord chord : chords) {
                chord.setTupletFactor(factor);
            }
        }
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        return super.internalsString() + " " + lastChord;
    }

    //-------------------//
    // getEmbracedChords //
    //-------------------//
    /**
     * Report the proper collection of chords that are embraced by the tuplet
     *
     * @param glyph underlying glyph
     * @param measure measure where the sign is located
     * @param point location for the sign
     * @param candidates the chords candidates, ordered wrt the sign
     * @param requiredStaff the required containing staff if known, or null
     * @return the set of embraced chords, ordered from left to right, or null
     * when the retrieval has failed
     */
    private static SortedSet<Chord> getEmbracedChords (Glyph            glyph,
                                                       Measure          measure,
                                                       PixelPoint       point,
                                                       SortedSet<Chord> candidates,
                                                       Staff            requiredStaff)
    {
        if (logger.isFineEnabled()) {
            logger.fine(
                glyph.getShape() + " glyph#" + glyph.getId() +
                ((requiredStaff != null) ? (" staff#" + requiredStaff.getId())
                 : ""));
        }

        // We consider each candidate in turn, with its duration
        // in order to determine the duration base of the tuplet
        TupletCollector collector = new TupletCollector(
            glyph,
            new TreeSet<Chord>());

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
                SortedSet<Chord> verticals = new TreeSet<Chord>(
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

            // If we have a beam, we must take the whole beam group
            if (!chord.getBeams()
                      .isEmpty()) {
                collector.includeAll(chord.getBeamGroup().getChords());
            } else {
                collector.include(chord);
            }

            // Check we have collected the exact amount of time
            if (collector.isTooLong()) {
                measure.addError(glyph, collector.getStatusMessage());

                return null;
            } else if (collector.isOk()) {
                if (logger.isFineEnabled()) {
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
     * Report the tuplet factor that corresponds to the provided tuplet sign
     * @param glyph the tuplet sign
     * @return the related factor
     */
    private static DurationFactor getFactor (Glyph glyph)
    {
        switch (glyph.getShape()) {
        case TUPLET_THREE :
            return new DurationFactor(2, 3);

        case TUPLET_SIX :
            return new DurationFactor(4, 6);

        default :
            logger.severe("Incorrect tuplet glyph shape");

            return null;
        }
    }

    //---------------//
    // expectedCount //
    //---------------//
    /**
     * Report the number of basic items governed by the tuplet
     * A given chord may represent several basic items (chords of base duration)
     * @param shape the tuplet shape
     * @return 3 or 6
     */
    private static int expectedCount (Shape shape)
    {
        switch (shape) {
        case TUPLET_THREE :
            return 3;

        case TUPLET_SIX :
            return 6;

        default :
            logger.severe("Incorrect tuplet shape");

            return 0;
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------------//
    // DxComparator //
    //--------------//
    private static class DxComparator
        implements Comparator<Chord>
    {
        //~ Instance fields ----------------------------------------------------

        /** The location of the tuplet sign */
        private final PixelPoint signPoint;

        //~ Constructors -------------------------------------------------------

        public DxComparator (PixelPoint signPoint)
        {
            this.signPoint = signPoint;
        }

        //~ Methods ------------------------------------------------------------

        /** Compare their horizontal distance from the signPoint reference */
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
        private final PixelPoint signPoint;

        //~ Constructors -------------------------------------------------------

        public DyComparator (PixelPoint signPoint)
        {
            this.signPoint = signPoint;
        }

        //~ Methods ------------------------------------------------------------

        /** Compare their vertical distance from the signPoint reference */
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
     * In charge of incrementally collecting the chords for a given tuplet sign
     */
    private static class TupletCollector
    {
        //~ Enumerations -------------------------------------------------------

        /** Describe the current status of the tuplet collector */
        public enum Status {
            //~ Enumeration constant initializers ------------------------------

            TOO_SHORT("Too short"),OK("Correct"), TOO_LONG("Too long");

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

        public TupletCollector (Glyph            glyph,
                                SortedSet<Chord> chords)
        {
            this.glyph = glyph;
            expectedCount = expectedCount(glyph.getShape());
            this.chords = chords;
        }

        //~ Methods ------------------------------------------------------------

        public SortedSet<Chord> getChords ()
        {
            return chords;
        }

        public boolean isOk ()
        {
            return status == Status.OK;
        }

        public String getStatusMessage ()
        {
            if (logger.isFineEnabled()) {
                dump();
            }

            return status.label + " sequence in " + glyph.getShape() + " " +
                   total + " vs " + expectedTotal;
        }

        public boolean isTooLong ()
        {
            return status == Status.TOO_LONG;
        }

        public Rational getTotal ()
        {
            return total;
        }

        public void dump ()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(glyph.getShape());

            sb.append(" glyph#")
              .append(glyph.getId());

            sb.append(" ")
              .append(status);

            sb.append(" Base:")
              .append(base);

            sb.append(" ExpectedTotal:")
              .append(expectedTotal);

            sb.append(" Total:")
              .append(total);

            for (Chord chord : chords) {
                sb.append("\n")
                  .append(chord);
            }

            logger.fine(sb.toString());
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
                    status = Status.OK;
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
    }
}
