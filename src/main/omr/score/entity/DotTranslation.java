//----------------------------------------------------------------------------//
//                                                                            //
//                        D o t T r a n s l a t i o n                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.facets.Glyph;

import omr.sheet.Scale;

import omr.util.TreeNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Class {@code DotTranslation} is a set of functions forassigning a dot
 * glyph, since a dot can be an augmentation dot, part of a repeat sign,
 * a staccato sign.
 *
 * @author Hervé Bitteur
 */
public class DotTranslation
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(DotTranslation.class);

    /** Sequence of dot trials */
    private static final List<? extends Trial> trials = Arrays.asList(
            new StaccatoTrial(),
            new RepeatTrial(),
            new AugmentationTrial());

    //~ Constructors -----------------------------------------------------------
    //
    //----------------//
    // DotTranslation //
    //----------------//
    private DotTranslation ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-------------//
    // populateDot //
    //-------------//
    /**
     * Try to find the best assignment for a dot (variant) glyph.
     *
     * @param glyph     the glyph of dot
     * @param measure   the containing measure
     * @param dotCenter the location of the dot
     */
    public static void populateDot (Glyph glyph,
                                    Measure measure,
                                    Point dotCenter)
    {
        logger.debug("{} populateDot {}",
                measure.getContextString(), glyph);

        // Keep specific shape only if manually assigned
        if (!glyph.isManualShape()) {
            glyph.setShape(DOT_set);
        }

        Shape shape = glyph.getShape();

        /** To remember results of trials */
        SortedSet<Trial.Result> results = new TreeSet<>();

        // Try the various possibilities
        for (Trial trial : trials) {
            if ((shape == DOT_set) || (shape == trial.targetShape)) {
                Trial.Result result = trial.process(glyph, measure, dotCenter);

                if (result != null) {
                    results.add(result);
                }
            }
        }

        // Debug
        if (logger.isDebugEnabled()) {
            for (Trial.Result info : results) {
                logger.debug(info.toString());
            }
        }

        // Choose best result, if any
        if (!results.isEmpty()) {
            Trial.Result result = results.first();
            Shape targetShape = result.getTargetShape();

            // Assign proper glyph shape (and thus color)
            if (glyph.getShape() != targetShape) {
                glyph.setShape(targetShape);
            }

            // Assign proper translation
            result.commit(glyph, measure, dotCenter);
        } else {
            measure.addError(glyph, "Dot unassigned");
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-------------------//
    // AugmentationTrial //
    //-------------------//
    /**
     * Try to assign a dot as a chord augmentation dot
     */
    private static class AugmentationTrial
            extends Trial
    {
        //~ Constructors -------------------------------------------------------

        public AugmentationTrial ()
        {
            super(AUGMENTATION_DOT);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        Result process (Glyph glyph,
                        Measure measure,
                        Point dotCenter)
        {
            Scale scale = measure.getScale();
            final int maxDx = scale.toPixels(
                    constants.maxAugmentationDotDx);
            final int maxDy = scale.toPixels(
                    constants.maxAugmentationDotDy);
            SortedMap<Double, Note> distances = new TreeMap<>();

            // Check for a note/rest nearby:
            // - on the left w/ same even pitch (note w/ even pitch)
            // - slighly above or below (note with odd pitch = on a staff line)
            ChordLoop:
            for (TreeNode node : measure.getChords()) {
                Chord chord = (Chord) node;

                for (TreeNode n : chord.getNotes()) {
                    Note note = (Note) n;

                    if (!note.getShape().isMeasureRest()) {
                        Point noteRef = note.getCenterRight();
                        Point toDot = new Point(
                                dotCenter.x - noteRef.x,
                                dotCenter.y - noteRef.y);

                        logger.debug("Augmentation {} {}", toDot, note);

                        if (((glyph.getShape() == getTargetShape())
                             && glyph.isManualShape())
                            || ((toDot.x > 0) && (toDot.x <= maxDx)
                                && (Math.abs(toDot.y) <= maxDy))) {
                            distances.put(toDot.distanceSq(0, 0), note);
                        } else if (toDot.x < (-2 * maxDx)) {
                            break ChordLoop; // Speed up
                        }
                    }
                }
            }

            if (!distances.isEmpty()) {
                Double firstKey = distances.firstKey();
                Note note = distances.get(firstKey);

                // Beware of mirrored notes
                // Choose the one with longest duration
                Note mirror = note.getMirroredNote();

                if ((mirror == null)
                    || (note.getChord().getDuration().compareTo(mirror.
                        getChord().getDuration()) > 0)) {
                    return new AugmentationResult(note, firstKey);
                } else {
                    return new AugmentationResult(mirror, firstKey);
                }
            } else {
                return null;
            }
        }

        //~ Inner Classes ------------------------------------------------------
        public class AugmentationResult
                extends Result
        {
            //~ Instance fields ------------------------------------------------

            final Note note;

            //~ Constructors ---------------------------------------------------
            public AugmentationResult (Note note,
                                       double dist)
            {
                super(dist);
                this.note = note;
            }

            //~ Methods --------------------------------------------------------
            @Override
            public void commit (Glyph glyph,
                                Measure measure,
                                Point dotCenter)
            {
                // Is there a second dot on the right?
                Glyph second = secondDot(glyph, measure, dotCenter);
                note.setDots(glyph, second);
                glyph.setTranslation(note);
                note.getChord().setDotsNumber((second != null) ? 2 : 1);

                logger.debug("{} dot#{} Augmented {}",
                        note.getContextString(), glyph.getId(), note);
            }

            @Override
            protected String internals ()
            {
                return "note:" + note;
            }

            private Glyph secondDot (Glyph glyph,
                                     Measure measure,
                                     Point dotCenter)
            {
                Scale scale = measure.getScale();
                final int maxDx = scale.toPixels(
                        constants.maxAugmentationDoubleDotsDx);
                final int maxDy = scale.toPixels(
                        constants.maxAugmentationDoubleDotsDy);

                boolean started = false;

                // Check for a suitable second dot nearby
                for (Glyph g : measure.getSystem().getInfo().getGlyphs()) {
                    if (g == glyph) {
                        started = true;

                        continue;
                    }

                    if (!started) {
                        continue;
                    }

                    if (!g.isTranslated()
                        && ((g.getShape() == DOT_set)
                            || (g.getShape() == AUGMENTATION_DOT))) {
                        // Check relative position
                        Point gCenter = g.getLocation();
                        int dx = gCenter.x - dotCenter.x;
                        int dy = gCenter.y - dotCenter.y;

                        if (dx > maxDx) {
                            return null;
                        }

                        if ((dx > 0) && (Math.abs(dy) <= maxDy)) {
                            logger.debug("Double dot with {}", g);

                            g.setTranslation(note);

                            // Assign proper glyph shape (and thus color)
                            if (g.getShape() != targetShape) {
                                g.setShape(targetShape);
                            }

                            return g;
                        }
                    }
                }

                return null;
            }
        }
    }

    //-------------//
    // RepeatTrial //
    //-------------//
    /**
     * Try to assign a dot to the relevant repeat barline if any
     */
    private static class RepeatTrial
            extends Trial
    {
        //~ Constructors -------------------------------------------------------

        public RepeatTrial ()
        {
            super(REPEAT_DOT);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        RepeatResult process (Glyph glyph,
                              Measure measure,
                              Point dotCenter)
        {
            if (glyph.isVip()) {
                logger.info("RepeatTrial. process {}", glyph.idString());
            }

            SortedMap<Double, Barline> distances = new TreeMap<>();

            // Check vertical pitch position within the staff: close to +1 or -1
            double pitchDif = Math.abs(Math.abs(glyph.getPitchPosition()) - 1);

            if (pitchDif > (2 * constants.maxRepeatDotDy.getValue())) {
                return null;
            }

            final Scale scale = measure.getScale();
            final int maxDx = scale.toPixels(constants.maxRepeatDotDx);

            // Check wrt inside/starting barline on left & ending barline on right
            Barline leftBar;
            if (measure.getInsideBarline() != null) {
                leftBar = measure.getInsideBarline();
            } else {
                Measure prevMeasure = (Measure) measure.getPreviousSibling();
                leftBar = (prevMeasure != null) ? prevMeasure.getBarline()
                        : measure.getPart().getStartingBarline();
            }
            Barline rightBar = measure.getBarline();

            for (Barline bar : Arrays.asList(leftBar, rightBar)) {
                if (bar != null) {
                    final int dx = (bar == leftBar)
                            ? (dotCenter.x - bar.getRightX())
                            : (bar.getLeftX() - dotCenter.x);

                    logger.debug("Repeat dx:{} {}", dx, bar);

                    if (((glyph.getShape() == getTargetShape())
                         && glyph.isManualShape())
                        || ((dx > 0) && (dx <= maxDx))) {
                        distances.put(new Double(dx * dx), bar);
                    }
                }
            }

            // Take the best, if any
            if (!distances.isEmpty()) {
                Double firstKey = distances.firstKey();

                return new RepeatResult(distances.get(firstKey), firstKey);
            } else {
                return null;
            }
        }

        //~ Inner Classes ------------------------------------------------------
        public class RepeatResult
                extends Trial.Result
        {
            //~ Instance fields ------------------------------------------------

            final Barline barline;

            //~ Constructors ---------------------------------------------------
            public RepeatResult (Barline barline,
                                 double dist)
            {
                super(dist);
                this.barline = barline;
            }

            //~ Methods --------------------------------------------------------
            @Override
            public void commit (Glyph glyph,
                                Measure measure,
                                Point dotCenter)
            {
                barline.addGlyph(glyph);

                logger.debug("{} dot#{} Repeat dot for {}",
                        barline.getContextString(), glyph.getId(), barline);
            }

            @Override
            protected String internals ()
            {
                return "barline:" + barline;
            }
        }
    }

    //---------------//
    // StaccatoTrial //
    //---------------//
    /**
     * Try to assign a dot as a staccato
     */
    private static class StaccatoTrial
            extends Trial
    {
        //~ Constructors -------------------------------------------------------

        public StaccatoTrial ()
        {
            super(STACCATO);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        StaccatoResult process (Glyph glyph,
                                Measure measure,
                                Point dotCenter)
        {
            Scale scale = measure.getScale();
            final int maxDx = scale.toPixels(
                    constants.maxStaccatoDotDx);
            final int maxDy = scale.toPixels(
                    constants.maxStaccatoDotDy);
            SortedMap<Double, Chord> distances = new TreeMap<>();

            ChordLoop:
            for (TreeNode node : measure.getChords()) {
                Chord chord = (Chord) node;

                for (TreeNode n : chord.getNotes()) {
                    Note note = (Note) n;

                    if (!note.isRest()) {
                        // Check distance wrt both top & bottom of note
                        for (Point noteRef : Arrays.asList(
                                note.getCenterTop(),
                                note.getCenterBottom())) {
                            Point toDot = new Point(
                                    dotCenter.x - noteRef.x,
                                    dotCenter.y - noteRef.y);

                            logger.debug("Staccato {} {}", toDot, note);

                            if (((glyph.getShape() == getTargetShape())
                                 && glyph.isManualShape())
                                || ((Math.abs(toDot.x) <= maxDx)
                                    && (Math.abs(toDot.y) <= maxDy))) {
                                distances.put(toDot.distanceSq(0, 0), chord);
                            } else if (toDot.x < (-2 * maxDx)) {
                                break ChordLoop; // Speed up
                            }
                        }
                    }
                }
            }

            if (!distances.isEmpty()) {
                Double firstKey = distances.firstKey();

                return new StaccatoResult(distances.get(firstKey), firstKey);
            } else {
                return null;
            }
        }

        //~ Inner Classes ------------------------------------------------------
        private class StaccatoResult
                extends Result
        {
            //~ Instance fields ------------------------------------------------

            final Chord chord;

            //~ Constructors ---------------------------------------------------
            public StaccatoResult (Chord chord,
                                   double dist)
            {
                super(dist);
                this.chord = chord;
            }

            //~ Methods --------------------------------------------------------
            @Override
            public void commit (Glyph glyph,
                                Measure measure,
                                Point dotCenter)
            {
                glyph.setTranslation(
                        new Articulation(measure, dotCenter, chord, glyph));

                logger.debug("{} dot#{} Staccato {}",
                        chord.getContextString(), glyph.getId(), chord);
            }

            @Override
            protected String internals ()
            {
                return "chord:" + chord;
            }
        }
    }

    //-------//
    // Trial //
    //-------//
    private abstract static class Trial
    {
        //~ Instance fields ----------------------------------------------------

        public final Shape targetShape;

        //~ Constructors -------------------------------------------------------
        public Trial (Shape targetShape)
        {
            this.targetShape = targetShape;
        }

        //~ Methods ------------------------------------------------------------
        public Shape getTargetShape ()
        {
            return targetShape;
        }

        abstract Result process (Glyph glyph,
                                 Measure measure,
                                 Point dotCenter);

        //~ Inner Classes ------------------------------------------------------
        /**
         * Remember information about possible assignment of a dot
         */
        public abstract class Result
                implements Comparable<Result>
        {
            //~ Instance fields ------------------------------------------------

            /* The measured distance to the related entity */
            final double dist;

            //~ Constructors ---------------------------------------------------
            public Result (double dist)
            {
                this.dist = dist;
            }

            //~ Methods --------------------------------------------------------
            public abstract void commit (Glyph glyph,
                                         Measure measure,
                                         Point dotCenter);

            @Override
            public int compareTo (Result other)
            {
                return Double.compare(this.dist, other.dist);
            }

            public Shape getTargetShape ()
            {
                return targetShape;
            }

            @Override
            public String toString ()
            {
                return "{" + getClass().getSimpleName() + " dist:" + (float) dist
                       + " " + internals() + "}";
            }

            protected String internals ()
            {
                return "";
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxAugmentationDotDx = new Scale.Fraction(
                1.7d,
                "Maximum dx between note and augmentation dot");

        Scale.Fraction maxAugmentationDotDy = new Scale.Fraction(
                1d,
                "Maximum absolute dy between note and augmentation dot");

        Scale.Fraction maxAugmentationDoubleDotsDx = new Scale.Fraction(
                1.5d,
                "Maximum dx between two augmentation dots");

        Scale.Fraction maxAugmentationDoubleDotsDy = new Scale.Fraction(
                0.2d,
                "Maximum absolute dy between two augmentation dots");

        Scale.Fraction maxRepeatDotDy = new Scale.Fraction(
                0.5d,
                "Margin for vertical position of a dot againt a repeat barline");

        Scale.Fraction maxRepeatDotDx = new Scale.Fraction(
                1.5d,
                "Maximum dx between dot and edge of repeat barline");

        Scale.Fraction maxStaccatoDotDy = new Scale.Fraction(
                6d,
                "Maximum absolute dy between note and staccato dot");

        Scale.Fraction maxStaccatoDotDx = new Scale.Fraction(
                0.75d,
                "Maximum dx between note and staccato dot");

    }
}
