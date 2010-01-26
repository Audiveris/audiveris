//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h E v a l u a t o r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.Main;
import omr.WellKnowns;

import omr.constant.ConstantSet;

import omr.log.Logger;

import omr.math.Moments;
import omr.math.NeuralNetwork;

import omr.sheet.Scale;

import omr.util.ClassUtil;

import java.io.*;
import java.util.*;

import javax.xml.bind.JAXBException;

/**
 * Class <code>GlyphEvaluator</code> is an abstract class that gathers data and
 * processing common to any evaluator working on glyph characteristics to infer
 * glyph shape.
 *
 * @author Herv&eacute; Bitteur
 */
public abstract class GlyphEvaluator
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphEvaluator.class);

    /** Number of useful moments : {@value} */
    public static final int inMoments = 10;

    /**
     * Number of useful input parameters : nb of moments +
     * stemNumber, isWithLedger, pitchPosition = {@value}
     */
    public static final int paramCount = inMoments + 2;

    /** Number of shapes to differentiate */
    public static final int shapeCount = Shape.LAST_PHYSICAL_SHAPE.ordinal() +
                                         1;

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
        return LabelsHolder.labels[index];
    }

    //--------------------//
    // getParameterLabels //
    //--------------------//
    /**
     * Report the parameters labels
     *
     * @return the array of parameters labels
     */
    public static String[] getParameterLabels ()
    {
        return LabelsHolder.labels;
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
            ins = new double[paramCount];
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
    public Evaluation[] getEvaluations (Glyph glyph)
    {
        List<Evaluation> kept = new ArrayList<Evaluation>();

        for (Evaluation eval : getAllEvaluations(glyph)) {
            if (!glyph.isShapeForbidden(eval.shape)) {
                kept.add(eval);
            }
        }

        return kept.toArray(new Evaluation[kept.size()]);
    }

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

    //---------//
    // marshal //
    //---------//
    /**
     * Store the engine in XML format, always as a custom file
     */
    public void marshal ()
    {
        final File   file = getCustomFile();
        OutputStream os = null;

        try {
            os = new FileOutputStream(file);
            marshal(os);
            logger.info("Engine marshalled to " + file);
        } catch (FileNotFoundException ex) {
            logger.warning("Could not find file " + file);
        } catch (IOException ex) {
            logger.warning("IO error on file " + file);
        } catch (JAXBException ex) {
            logger.warning("Error marshalling engine to " + file);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

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

    //---------//
    // marshal //
    //---------//
    protected abstract void marshal (OutputStream os)
        throws FileNotFoundException, IOException, JAXBException;

    //---------------//
    // getCustomFile //
    //---------------//
    /**
     * Report the custom file used to store or load the internal evaluator data
     *
     * @return the evaluator custom backup file
     */
    protected File getCustomFile ()
    {
        // The custom file, if any, is located in the configuration folder
        return new File(WellKnowns.CONFIG_FOLDER, getFileName());
    }

    //---------------//
    // getDefaultUrl //
    //---------------//
    /**
     * Report the name of the resource used to retrieve the evaluator marshalled
     * data from the distribution resource
     * @return the data resource name
     */
    protected String getDefaultUrl ()
    {
        return "/config/" + getFileName();
    }

    //-------------//
    // getFileName //
    //-------------//
    /**
     * Report the simple file name, including extension but excluding parent,
     * which contains the marshalled data of the evaluator
     * @return the file name
     */
    protected abstract String getFileName ();

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the evaluation engine from the most suitable backup, which
     * is first a custom file, and second the distribution resource.
     * @return the engine, or null if failed
     */
    protected Object unmarshal ()
    {
        InputStream input = ClassUtil.getProperStream(
            WellKnowns.CONFIG_FOLDER,
            getFileName());

        return unmarshal(input, getFileName());
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * The specific unmarshalling method which builds a suitable engine
     * @param is the input stream to read
     * @return the newly built evaluation engine
     * @throws JAXBException
     * @throws IOException
     */
    protected abstract Object unmarshal (InputStream is)
        throws JAXBException, IOException;

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

    //-----------//
    // unmarshal //
    //-----------//
    private Object unmarshal (InputStream is,
                              String      name)
    {
        if (is == null) {
            logger.warning(
                "No data stream for " + getName() + " engine as " + name);
        } else {
            try {
                Object engine = unmarshal(is);
                is.close();

                return engine;
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find or read " + name);
            } catch (IOException ex) {
                logger.warning("IO error on " + name);
            } catch (JAXBException ex) {
                logger.warning("Error unmarshalling evaluator from " + name);
            }
        }

        return null;
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
            0.08,
            "Minimum normalized weight to be considered not a noise");
    }

    //--------------//
    // LabelsHolder //
    //--------------//
    /** Descriptive strings for glyph characteristics */
    private static class LabelsHolder
    {
        //~ Static fields/initializers -----------------------------------------

        public static final String[] labels = new String[paramCount];

        static {
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
