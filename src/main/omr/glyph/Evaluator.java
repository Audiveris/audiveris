//----------------------------------------------------------------------------//
//                                                                            //
//                             E v a l u a t o r                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.constant.ConstantSet;

import omr.math.Moments;
import omr.math.NeuralNetwork;

import omr.sheet.PixelRectangle;
import omr.sheet.Scale;

import omr.stick.Stick;

import omr.util.Logger;

import java.util.Comparator;
import java.util.List;

/**
 * Class <code>Evaluator</code> is an abstract class that gathers data and
 * processing common to any evaluator working on glyph characteristics to infer
 * glyph shape.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public abstract class Evaluator
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Evaluator.class);

    /** Number of useful moments : {@value} */
    public static final int inMoments = 10;

    /**
     * Number of useful input parameters : nb of moments +
     * stemNumber, isWithLedger, pitchPosition = {@value}
     */
    public static final int inSize = inMoments + 2;

    /** Number of shapes to differentiate */
    public static final int outSize = Shape.LastPhysicalShape.ordinal() + 1;

    /** A special evaluation array, used to report NOISE */
    static final Evaluation[] noiseEvaluations = {
                                                     new Evaluation(
        Shape.NOISE,
        0d)
                                                 };

    /**
     * An Evaluation comparator in increasing order, where smaller doubt value
     * means better interpretation
     */
    protected static final Comparator<Evaluation> comparator = new Comparator<Evaluation>() {
        public int compare (Evaluation e1,
                            Evaluation e2)
        {
            if (e1.doubt < e2.doubt) {
                return -1;
            }

            if (e1.doubt > e2.doubt) {
                return +1;
            }

            return 0;
        }
    };

    /** Descriptive labels for glyph characteristics */
    private static volatile String[] labels;

    //~ Enumerations -----------------------------------------------------------

    /** Describes the various modes for starting the training of an evaluator */
    public static enum StartingMode {
        //~ Enumeration constant initializers ----------------------------------


        /** Start with the current values */
        INCREMENTAL,
        /** Start from scratch, with new initial values */
        SCRATCH;
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getName //
    //---------//
    /**
     * Report the name of this evaluator
     *
     * @return the evaluator declared name
     */
    public abstract String getName ();

    //------//
    // dump //
    //------//
    /**
     * Dump the internals of the evaluator
     */
    public abstract void dump ();

    //-------------//
    // isBigEnough //
    //-------------//
    /**
     * Use a threshold on glyph weight, to tell if the provided glyph is just
     * {@link Shape#NOISE}, or a real glyph
     *
     * @param glyph the glyph to be checked
     * @return true if not noise, false otherwise
     */
    public static boolean isBigEnough (Glyph glyph)
    {
        return glyph.getNormalizedWeight() >= constants.minWeight.getValue();
    }

    //-------------------//
    // getParameterLabel //
    //-------------------//
    /**
     * Report the label assigned to a given parameter
     *
     * @param index the paarameter index
     * @return the assigned label
     */
    public static String getParameterLabel (int index)
    {
        if (labels == null) {
            synchronized (Evaluator.class) {
                if (labels == null) {
                    labels = new String[inSize];

                    // We take all the first moments
                    for (int i = 0; i < inMoments; i++) {
                        labels[i] = Moments.getLabel(i);
                    }

                    // We append flags and step position
                    int i = inMoments;
                    /* 10 */ labels[i++] = "ledger";
                    /* 11 */ labels[i++] = "stemNb";

                    ////* 12 */ labels[i++] = "pitch";
                }
            }
        }

        return labels[index];
    }

    //-----------//
    // feedInput //
    //-----------//
    /**
     * Prepare the evaluator input, by picking up some characteristics of the
     * glyph (some of its moments, and some info on surroundings)
     *
     * @param glyph the glyph to be evaluated
     * @param ins   the evaluator input array to be filled (if null, it is
     *              allocated by the routine)
     *
     * @return the filled input array
     */
    public static double[] feedInput (Glyph    glyph,
                                      double[] ins)
    {
        if (ins == null) {
            ins = new double[inSize];
        }

        // We take all the first moments
        Double[] k = glyph.getMoments()
                          .getValues();

        for (int i = 0; i < inMoments; i++) {
            ins[i] = k[i];
        }

        // We append flags and step position
        int i = inMoments;
        /* 10 */ ins[i++] = boolAsDouble(glyph.isWithLedger());
        /* 11 */ ins[i++] = glyph.getStemNumber();

        ////////* 12 */ ins[i++] = glyph.getPitchPosition();

        // We skip moments 17 & 18 (xMean and yMean) ???
        return ins;
    }

    //-------------------//
    // getAllEvaluations //
    //-------------------//
    /**
     * Run the evaluator with the specified glyph, and return a prioritized
     * collection of interpretations (ordered from best to worst).
     *
     * @param glyph the glyph to be examined
     *
     * @return the ordered best evaluations
     */
    public abstract Evaluation[] getAllEvaluations (Glyph glyph);

    //----------------//
    // getEvaluations //
    //----------------//
    /**
     * Run the evaluator with the specified glyph, and return a prioritized
     * collection of interpretations (ordered from best to worst), without the
     * shapes that are flagged as forbidden for this glyph.
     *
     * @param glyph the glyph to be examined
     *
     * @return the ordered best evaluations
     */
    public abstract Evaluation[] getEvaluations (Glyph glyph);

    //------//
    // stop //
    //------//
    /**
     * Stop the on-going training. By default, this is a no-op
     */
    public void stop ()
    {
    }

    //-------//
    // train //
    //-------//
    /**
     * Here we train the evaluator "ab initio", based on the set of known glyphs
     * accumulated in the previous runs.
     *
     * @param base the list of glyphs to retrain the evaluator
     * @param monitor a monitoring interface
     * @param mode specify the starting mode of the training session
     */
    public abstract void train (List<Glyph>  base,
                                Monitor      monitor,
                                StartingMode mode);

    //------//
    // vote //
    //------//
    /**
     * Run the evaluator with the specified glyph, and infer a shape.
     *
     * @param glyph the glyph to be examined
     * @param maxDoubt the maximum doubt to be accepted
     * @return the best acceptable evaluation, or null
     */
    public Evaluation vote (Glyph  glyph,
                            double maxDoubt)
    {
        Evaluation[] evaluations = getEvaluations(glyph);

        if ((evaluations.length > 0) && (evaluations[0].doubt <= maxDoubt)) {
            Evaluation best = evaluations[0];

            // Temporary logic, to be validated:
            // If the best shape found is a CLUTTER while a second best is also 
            // acceptable wrt maxDoubt, we choose the second
            if ((best.shape == Shape.CLUTTER) &&
                (evaluations.length > 1) &&
                (evaluations[1].doubt <= maxDoubt)) {
                best = evaluations[1];

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Shape CLUTTER discarded for " + best.shape +
                        " at glyph #" + glyph.getId());
                }
            }

            return best;
        } else {
            return null;
        }
    }

    //--------------------//
    // specificShapeCheck //
    //--------------------//
    protected Shape specificShapeCheck (Shape shape,
                                        Glyph glyph)
    {
        // Special case for WHOLE/HALF rests : use pitch position
        if (shape == Shape.WHOLE_OR_HALF_REST) {
            int pp = (int) Math.rint(2 * glyph.getPitchPosition());

            if (pp == -1) {
                return Shape.HALF_REST;
            } else if (pp == -3) {
                return Shape.WHOLE_REST;
            } else {
                return null;
            }
        } else if (Shape.Clefs.contains(shape)) {
            // Check reasonable height
            if (glyph.getNormalizedHeight() > constants.maxClefHeight.getValue()) {
                return null;
            }

            // Check distance from closest staff
            if (Math.abs(glyph.getPitchPosition()) >= 15) {
                return null;
            }
        } else if (Shape.HeadAndFlags.contains(shape)) {
            // Check that we do have a stem on left side
            if (glyph.getLeftStem() == null) {
                return null;
            }
        } else if (Shape.BEAM_HOOK == shape) {
            // Check we have exactly 1 stem
            if (glyph.getStemNumber() != 1) {
                return null;
            }

            if (!validBeamSlope(glyph)) {
                return null;
            }
        } else if (Shape.Times.contains(shape)) {
            // A time signature is on staff !
            if (Math.abs(glyph.getPitchPosition()) >= 3) {
                return null;
            }
        } else if (Shape.Dynamics.contains(shape)) {
            // Check distance from closest staff
            if (Math.abs(glyph.getPitchPosition()) >= 15) {
                return null;
            }
        } else if (Shape.Notes.contains(shape) ||
                   Shape.NoteHeads.contains(shape) ||
                   Shape.Rests.contains(shape) ||
                   Shape.HeadAndFlags.contains(shape)) {
            // A note / rest cannot be too far from a staff
            if (Math.abs(glyph.getPitchPosition()) >= 15) {
                return null;
            }

            if (shape == Shape.NOTEHEAD_BLACK) {
                if (glyph.getNormalizedWeight() > constants.maxHeadBlackWeight.getValue()) {
                    return null;
                }
            }
        }

        // Pass-through by default
        return shape;
    }

    //--------------//
    // boolAsDouble //
    //--------------//
    private static double boolAsDouble (boolean b)
    {
        if (b) {
            return 1d;
        } else {
            return 0d;
        }
    }

    //----------------//
    // validBeamSlope //
    //----------------//
    private boolean validBeamSlope (Glyph glyph)
    {
        try {
            Stick          stick = (Stick) glyph;
            double         slope = stick.getLine()
                                        .getInvertedSlope(); // vertical lag!
            PixelRectangle box = glyph.getContourBox();
            double         maxSlope = (double) box.height / (double) box.width;

            return Math.abs(slope) <= maxSlope;
        } catch (Exception ex) {
            return false;
        }
    }

    //~ Inner Interfaces -------------------------------------------------------

    //---------//
    // Monitor //
    //---------//
    /**
     * Interface <code>Monitor</code> specifies a general monitoring interface
     * to pass information about the behavior of evaluators.
     */
    public static interface Monitor
        extends NeuralNetwork.Monitor
    {
        //~ Methods ------------------------------------------------------------

        /**
         * Entry called when a glyph is processed
         * @param glyph
         */
        void glyphProcessed (Glyph glyph);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.AreaFraction minWeight = new Scale.AreaFraction(
            0.19,
            "Minimum normalized weight to be considered not a noise");
        Scale.AreaFraction maxHeadBlackWeight = new Scale.AreaFraction(
            1.2,
            "Maximum normalized weight for a NOTEHEAD_BLACK");
        Scale.Fraction     maxClefHeight = new Scale.Fraction(
            9d,
            "Maximum normalized height for a clef");
    }
}
