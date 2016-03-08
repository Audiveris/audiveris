//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t C l a s s i f i e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

import omr.WellKnowns;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeChecker;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.UriUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

import javax.xml.bind.JAXBException;

/**
 * Class {@code AbstractClassifier} is an abstract implementation for any managed
 * classifier.
 * <p>
 * <img src="doc-files/GlyphEvaluator.png">
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractClassifier
        implements Classifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AbstractClassifier.class);

    /** Number of shapes to differentiate. */
    protected static final int shapeCount = 1 + Shape.LAST_PHYSICAL_SHAPE.ordinal();

    /** A special evaluation array, used to report NOISE. */
    protected static final Evaluation[] noiseEvaluations = {
        new Evaluation(
        Shape.NOISE,
        Evaluation.ALGORITHM)
    };

    //~ Enumerations -------------------------------------------------------------------------------
    /** The various modes for starting the training of a classifier. */
    public static enum StartingMode
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Start with the current values. */
        INCREMENTAL,
        /** Start from
         * scratch, with new initial values. */
        SCRATCH;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The glyph checker for additional specific checks. */
    protected ShapeChecker glyphChecker = ShapeChecker.getInstance();

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // dump //
    //------//
    /**
     * Dump the internals of the engine. */
    public abstract void dump ();

    //----------//
    // evaluate //
    //----------//
    @Override
    public Evaluation[] evaluate (Glyph glyph,
                                  int interline,
                                  int count,
                                  double minGrade,
                                  EnumSet<Condition> conditions)
    {
        return evaluate(glyph, null, count, minGrade, conditions, interline);
    }

    //-------//
    // train //
    //-------//
    /**
     * Train the classifier on the provided base of shape samples.
     *
     * @param base    the collection of Sample instances to train the classifier
     * @param monitor a monitoring interface
     * @param mode    specify the starting mode of the training session
     */
    public abstract void train (Collection<Sample> base,
                                Monitor monitor,
                                StartingMode mode);

    //----------//
    // evaluate //
    //----------//
    @Override
    public Evaluation[] evaluate (Glyph glyph,
                                  SystemInfo system,
                                  int count,
                                  double minGrade,
                                  EnumSet<Classifier.Condition> conditions)
    {
        final int interline = system.getSheet().getInterline();

        return evaluate(glyph, system, count, minGrade, conditions, interline);
    }

    //------------//
    // evaluateAs //
    //------------//
    @Override
    public Evaluation evaluateAs (Glyph glyph,
                                  int interline,
                                  Shape shape)
    {
        final Evaluation[] evals = getNaturalEvaluations(glyph, interline);
        final int ordinal = shape.ordinal();

        if (ordinal < evals.length) {
            return evals[ordinal];
        } else {
            logger.error("Shape {} cannot be evaluated directly", shape);

            return null;
        }
    }

    //-------------//
    // isBigEnough //
    //-------------//
    @Override
    public boolean isBigEnough (Glyph glyph,
                                int interline)
    {
        return isBigEnough(glyph.getNormalizedWeight(interline));
    }

    //-------------//
    // isBigEnough //
    //-------------//
    @Override
    public boolean isBigEnough (double weight)
    {
        return weight >= constants.minWeight.getValue();
    }

    //---------//
    // marshal //
    //---------//
    /**
     * Store the engine in XML format, always as a user file.
     */
    public void marshal ()
    {
        final Path path = WellKnowns.EVAL_FOLDER.resolve(getFileName());
        OutputStream os = null;

        try {
            if (!Files.exists(WellKnowns.EVAL_FOLDER)) {
                Files.createDirectories(WellKnowns.EVAL_FOLDER);
                logger.info("Created directory {}", WellKnowns.EVAL_FOLDER);
            }

            os = new FileOutputStream(path.toFile());
            marshal(os);
            logger.info("Engine marshalled to {}", path);
        } catch (FileNotFoundException ex) {
            logger.warn("Could not find file " + path, ex);
        } catch (IOException ex) {
            logger.warn("IO error on file " + path, ex);
        } catch (JAXBException ex) {
            logger.warn("Error marshalling engine to " + path, ex);
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
    // stop //
    //------//
    /**
     * Stop the on-going training.
     * By default, this is a no-op
     */
    public void stop ()
    {
    }

    //-------------//
    // getFileName //
    //-------------//
    /**
     * Report the simple file name, including extension but excluding
     * parent, which contains the marshaled data of the classifier.
     *
     * @return the file name
     */
    protected abstract String getFileName ();

    //--------------//
    // isCompatible //
    //--------------//
    /**
     * Make sure the provided engine object is compatible with the
     * current application.
     *
     * @param obj the engine object
     * @return true if engine is usable and found compatible
     */
    protected abstract boolean isCompatible (Object obj);

    //---------//
    // marshal //
    //---------//
    protected abstract void marshal (OutputStream os)
            throws FileNotFoundException, IOException, JAXBException;

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * The specific unmarshalling method which builds a suitable engine.
     *
     * @param is the input stream to read
     * @return the newly built evaluation engine
     * @throws JAXBException
     * @throws IOException
     */
    protected abstract Object unmarshal (InputStream is)
            throws JAXBException, IOException;

    //----------------------//
    // getSortedEvaluations //
    //----------------------//
    /**
     * Run the classifier with the specified glyph, and return a sequence of all
     * interpretations (ordered from best to worst) with no additional check.
     *
     * @param glyph     the glyph to be examined
     * @param interline the global sheet interline
     * @return the ordered best evaluations
     */
    protected Evaluation[] getSortedEvaluations (Glyph glyph,
                                                 int interline)
    {
        // If too small, it's just NOISE
        if (!isBigEnough(glyph, interline)) {
            return noiseEvaluations;
        } else {
            Evaluation[] evals = getNaturalEvaluations(glyph, interline);
            // Order the evals from best to worst
            Arrays.sort(evals);

            return evals;
        }
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the evaluation engine from the most suitable file.
     * If a user file does not exist or cannot be unmarshalled, the
     * system default file is used
     *
     * @return the unmarshalled engine, or null if everything failed
     */
    protected Object unmarshal ()
    {
        // First try user file, if any (in user EVAL folder)
        {
            Path path = WellKnowns.EVAL_FOLDER.resolve(getFileName());

            if (Files.exists(path)) {
                Object obj = unmarshal(path);

                if (obj == null) {
                    logger.warn("Could not load {}", path);
                } else if (!isCompatible(obj)) {
                    final String msg = "Obsolete user data for " + getName() + " in " + path
                                       + ", trying default data";
                    logger.warn(msg);

                    ///JOptionPane.showMessageDialog(null, msg);
                } else {
                    // Tell the user we are not using the default
                    logger.debug("{} unmarshalled from {}", getName(), path);

                    return obj;
                }
            }
        }

        // Use default file (in program RES folder)
        //file = new File(WellKnowns.RES_URI, getFileName());
        URI uri = UriUtil.toURI(WellKnowns.RES_URI, getFileName());
        InputStream input;

        try {
            input = uri.toURL().openStream();
        } catch (Exception ex) {
            logger.warn("Error in " + uri, ex);

            return null;
        }

        Object obj = unmarshal(input, getFileName());

        if (obj == null) {
            logger.warn("Could not load {}", uri);
        } else if (!isCompatible(obj)) {
            final String msg = "Obsolete default data for " + getName() + " in " + uri
                               + ", please retrain from scratch";
            logger.warn(msg);
            ///JOptionPane.showMessageDialog(null, msg);
            obj = null;
        } else {
            logger.debug("{} unmarshalled from {}", getName(), uri);
        }

        return obj;
    }

    //----------//
    // evaluate //
    //----------//
    private Evaluation[] evaluate (Glyph glyph,
                                   SystemInfo system,
                                   int count,
                                   double minGrade,
                                   EnumSet<Classifier.Condition> conditions,
                                   int interline)
    {
        List<Evaluation> bests = new ArrayList<Evaluation>();
        Evaluation[] evals = getSortedEvaluations(glyph, interline);

        EvalsLoop:
        for (Evaluation eval : evals) {
            // Bounding test?
            if ((bests.size() >= count) || (eval.grade < minGrade)) {
                break;
            }

            // Successful checks?
            if (conditions.contains(Condition.CHECKED)) {
                double[] ins = ShapeDescription.features(glyph, interline);
                // This may change the eval shape in only one case:
                // HW_REST_set may be changed for HALF_REST or WHOLE_REST based on pitch
                glyphChecker.annotate(system, eval, glyph, ins);

                if (eval.failure != null) {
                    continue;
                }
            }

            // Everything is OK, add the shape if not already in the list
            // (this may happen when checks have modified the eval original shape)
            for (Evaluation e : bests) {
                if (e.shape == eval.shape) {
                    continue EvalsLoop;
                }
            }

            bests.add(eval);
        }

        return bests.toArray(new Evaluation[bests.size()]);
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the evaluation engine using provided file.
     *
     * @return the unmarshalled engine, or null if failed
     */
    private Object unmarshal (Path path)
    {
        try {
            InputStream input = new FileInputStream(path.toFile());

            return unmarshal(input, getFileName());
        } catch (FileNotFoundException ex) {
            logger.warn("File not found " + path, ex);

            return null;
        } catch (Exception ex) {
            logger.warn("Error unmarshalling from " + path, ex);

            return null;
        }
    }

    //-----------//
    // unmarshal //
    //-----------//
    private Object unmarshal (InputStream is,
                              String name)
    {
        if (is == null) {
            logger.warn("No data stream for {} engine as {}", getName(), name);
        } else {
            try {
                Object engine = unmarshal(is);
                is.close();

                return engine;
            } catch (FileNotFoundException ex) {
                logger.warn("Cannot find or read " + name, ex);
            } catch (IOException ex) {
                logger.warn("IO error on " + name, ex);
            } catch (JAXBException ex) {
                logger.warn("Error unmarshalling classifier from " + name, ex);
            }
        }

        return null;
    }

    //~ Inner Interfaces ---------------------------------------------------------------------------
    //---------//
    // Monitor //
    //---------//
    /**
     * General monitoring interface to pass information about the
     * training of an classifier when processing a sample glyph.
     */
    public static interface Monitor
    {
        //~ Methods --------------------------------------------------------------------------------

        /**
         * Entry called at end of each epoch during the training phase.
         *
         * @param epochIndex the sequential index of completed epoch
         * @param mse        the remaining mean square error
         */
        void epochEnded (int epochIndex,
                         double mse);

        /**
         * Entry called when a sample is being processed.
         *
         * @param sample the sample being processed
         */
        void sampleProcessed (Sample sample);

        /**
         * Entry called at the beginning of the training phase, to allow
         * initial snap shots for example.
         *
         * @param epochIndex the sequential index (0)
         * @param epochMax   expected maximum index
         * @param mse        the starting mean square error
         * */
        void trainingStarted (final int epochIndex,
                              final int epochMax,
                              final double mse);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.AreaFraction minWeight = new Scale.AreaFraction(
                0.08,
                "Minimum normalized weight to be considered not a noise");
    }
}
