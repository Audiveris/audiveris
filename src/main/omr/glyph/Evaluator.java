//-----------------------------------------------------------------------//
//                                                                       //
//                           E v a l u a t o r                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

import omr.Main;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.math.Moments;
import omr.math.NeuralNetwork;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.util.Logger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Class <code>Evaluator</code> is an abstract class that gathers data and
 * processing common to any evaluator working on glyph characteristics to
 * infer glyph shape.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public abstract class Evaluator
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(Evaluator.class);

    // Extension used by evaluator backup files
    private static final String BACKUP_EXTENSION = ".def";

    /** Describes the various modes for starting the training of an
        evaluator */
    public static enum StartingMode
    {
            /** Start from scratch, with new initial values */
            SCRATCH,

            /** Start with the current values */
            INCREMENTAL;
    }

    /**
     * Number of useful moments
     */
    public static final int inMoments = 10;

    /**
     * Number of useful input parameters : 10 moments +
     * isWithinSystem, stemNumber, hasLedger, stepLine
     */
    public static final int inSize = inMoments + 4;

    /**
     * Number of shapes to differentiate
     */
    public static final int outSize = Shape.LastPhysicalShape.ordinal() +1;

    /**
     * A special evaluation array, used to report NOISE
     */
    protected static final Evaluation[] noiseEvaluations =
    {
        new Evaluation(Shape.NOISE, 0d)
    };

    /**
     * An Evaluation comparator in increasing order, where smaller grave
     * value means better interpretation
     */
    protected static Comparator<Evaluation> comparator
            = new Comparator<Evaluation>()
            {
                public int compare (Evaluation e1,
                                    Evaluation e2)
                {
                    if (e1.grade < e2.grade) {
                        return -1;
                    }
                    if (e1.grade > e2.grade) {
                        return +1;
                    }
                    return 0;
                }
            };

    /** Descriptive labels for glyph characteristics */
    protected static String[] labels;

    //~ Methods -----------------------------------------------------------

    //-----------------//
    // getCustomBackup //
    //-----------------//
    /**
     * Report the custom file used to store or load the internal evaluator
     * data
     *
     * @return the evaluator custom backup file
     */
    protected File getCustomBackup()
    {
        // The custom file, if any, is located at the root of the train
        // directory
        return new File(Main.getTrainPath(), getName() + BACKUP_EXTENSION);
    }

    //-----------------//
    // getSystemBackup //
    //-----------------//
    /**
     * Report the default system resource used to load the internal
     * evaluator data
     *
     * @return the evaluator system backup file
     */
    protected String getSystemBackup()
    {
        // The system file, is located in the config directory of the
        // application jar file
        return "/config/" + getName() + BACKUP_EXTENSION;
    }

    //------//
    // dump //
    //------//
    /**
     * Dump the internals of the evaluator
     */
    public abstract void dump ();

    //-----------//
    // feedInput //
    //-----------//
    /**
     * Prepare the evaluator input, by picking up some characteristics of
     * the glyph (some of its moments, and some info on surroundings)
     *
     * @param glyph the glyph to be evaluated
     * @param ins   the evaluator input array to be filled (if null, it is
     *              allocated by the routine)
     *
     * @return the filled input array
     */
    public static double[] feedInput (Glyph glyph,
                                      double[] ins)
    {
        if (ins == null) {
            ins = new double[inSize];
        }

        Moments m = glyph.getMoments();

        // We take all the first moments
        System.arraycopy(m.k, 0, ins, 0, inMoments);

        // We append flags and step position
        int i = inMoments;
        /* 10 */ ins[i++] = boolAsDouble(glyph.isWithinSystem());
        /* 11 */ ins[i++] = boolAsDouble(glyph.hasLedger());
        /* 12 */ ins[i++] = glyph.getStemNumber();
        /* 13 */ ins[i++] = glyph.getStepLine();

        // We skip moments 17 & 18 (xMean and yMean) ???

        return ins;
    }

    public static String[] getLabels ()
    {
        if (labels == null) {
            labels = new String[inSize];

            // We take all the first moments
            System.arraycopy(Moments.labels, 0, labels, 0, inMoments);

            // We append flags and step position
            int i = inMoments;
            /* 10 */ labels[i++] = "w/iSyst";
            /* 11 */ labels[i++] = "ledger";
            /* 12 */ labels[i++] = "stemNb";
            /* 13 */ labels[i++] = "sLine";
        }

        return labels;
    }

    //-------------------//
    // getBestEvaluation //
    //-------------------//
    /**
     * Run the network with the specified glyph, and infer a shape.
     *
     * @param glyph the glyph to be examined
     *
     * @return the best evaluation, if its grade is acceptable, null
     * otherwise
     */
    public Evaluation getBestEvaluation (Glyph glyph)
    {
        return getEvaluations(glyph)[0];
    }

    //----------------//
    // getEvaluations //
    //----------------//
    /**
     * Run the evaluator with the specified glyph, and return a prioritized
     * collection of interpretations (ordered from best to wors).
     *
     * @param glyph the glyph to be examined
     *
     * @return the ordered best evaluations
     */
    public abstract Evaluation[] getEvaluations (Glyph glyph);

    //---------//
    // getName //
    //---------//
    /**
     * Report the name of this evaluator
     *
     * @return the evaluator declared name
     */
    public abstract String getName();

    //------------//
    // guessSheet //
    //------------//
    /**
     * Compute and register a guess for each of the unknown glyphs of the
     * sheet. Too small glyphs are flagged as noisy.
     *
     * @param sheet the sheet at hand
     * @param maxGrade the maximum acceptable grade value
     */
    public void guessSheet (Sheet sheet,
                            double maxGrade)
    {
        for (SystemInfo system : sheet.getSystems()) {
            for (Glyph glyph : system.getGlyphs()) {
                if (!glyph.isKnown()) {
                    glyph.setGuess(vote(glyph, maxGrade));
                }
            }
        }
    }

    //-------------//
    // isBigEnough //
    //-------------//
    /**
     * Use a threshold on glyph weight, to tell if the provided glyph is
     * just {@link Shape#NOISE}, or a real glyph
     *
     * @param glyph the glyph to be checked
     * @return true if not noise, false otherwise
     */
    public static boolean isBigEnough (Glyph glyph)
    {
        return glyph.getMoments().k[0] >= constants.minWeight.getValue();
    }

    //-----------//
    // isTrained //
    //-----------//
    /**
     * Check whether the evaluator has been trained
     *
     * @return true if trained
     */
    public abstract boolean isTrained ();

    //------//
    // stop //
    //------//
    /**
     * Stop the on-going training. By default, this is a no-op
     */
    public void stop()
    {
    }

    //-------//
    // train //
    //-------//
    /**
     * Here we train the evaluator "ab initio", based on the set of known
     * glyphs accumulated in the previous runs.
     *
     * @param base the list of glyphs to retrain the evaluator
     * @param monitor a monitoring interface
     * @param mode specify the starting mode of the training session
     */
    public abstract void train (List<Glyph> base,
                                Monitor           monitor,
                                StartingMode      mode);

    //------//
    // vote //
    //------//
    /**
     * Run the evaluator with the specified glyph, and infer a shape.
     *
     * @param glyph the glyph to be examined
     * @param maxGrade the maximum grade to be accepted
     *
     * @return the best suitable interpretation, or null
     */
    public Shape vote (Glyph glyph,
                       double maxGrade)
    {
        if (isBigEnough(glyph)) {
            Evaluation eval = getBestEvaluation(glyph);
            if (eval.grade <= maxGrade) {
                return eval.shape;
            } else {
                return null;
            }
        } else {
            return Shape.NOISE;
        }
    }

    //--------------//
    // boolAsDouble //
    //--------------//
    private static double boolAsDouble (boolean b)
    {
        if (b)
            return 1d;
        else
            return 0d;
    }

    //----------------//
    // getMaxDistance //
    //----------------//
    /**
     * Report the maximum grade value for an evaluation to be accepted
     *
     * @return the maximum grade value
     */
    public double getMaxDistance ()
    {
        return constants.maxDistance.getValue();
    }

    //---------------------//
    // getMaxDistanceRatio //
    //---------------------//
    /**
     * Report the maximum ratio (with respect to the best evaluation) for
     * secondary evaluations to be also considered
     *
     * @return the maximum "degrading" ratio
     */
    public double getMaxDistanceRatio ()
    {
        return constants.maxDistanceRatio.getValue();
    }

    //~ Classes -----------------------------------------------------------

    //---------//
    // Monitor //
    //---------//
    /**
     * Interface <code>Monitor</code> specifies a general monitoring
     * interface to pass information about the behavior of evaluators.
     */
    public static interface Monitor
        extends NeuralNetwork.Monitor
    {
        /**
         * Entry called when a glyph is processed
         */
        void glyphProcessed (Glyph glyph);
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        //~ Instance variables --------------------------------------------

        Constant.Double minWeight = new Constant.Double
                (0.19,
                 "Minimum normalized weight to be considered not a noise");

        Constant.Double okDistance = new Constant.Double
                (1.5,
                 "Threshold on OK distance");

        Constant.Double maxDistance = new Constant.Double
                (5.0,
                 "Threshold on displayable distance");

        Constant.Double maxDistanceRatio = new Constant.Double
                (3.0,
                 "Max ratio, WRT the best evaluation");

        Constants ()
        {
            initialize();
        }
    }
}
