//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                          A b s t r a c t E v a l u a t i o n E n g i n e                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.WellKnowns;

import omr.constant.ConstantSet;

import omr.glyph.ShapeEvaluator.Condition;

import static omr.glyph.ShapeEvaluator.Condition.*;

import omr.glyph.facets.Glyph;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Predicate;
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
import java.util.EnumSet;
import java.util.List;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;

/**
 * Class {@code AbstractEvaluationEngine} is an abstract implementation for any
 * evaluation engine.
 * <p>
 * <img src="doc-files/GlyphEvaluator.png" />
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractEvaluationEngine
        implements EvaluationEngine
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            AbstractEvaluationEngine.class);

    /** Number of shapes to differentiate. */
    protected static final int shapeCount = 1 + Shape.LAST_PHYSICAL_SHAPE.ordinal();

    /** A special evaluation array, used to report NOISE. */
    protected static final Evaluation[] noiseEvaluations = {
        new Evaluation(
        Shape.NOISE,
        Evaluation.ALGORITHM)
    };

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** The glyph checker for additional specific checks. */
    protected ShapeChecker glyphChecker = ShapeChecker.getInstance();

    //~ Methods ------------------------------------------------------------------------------------
    //
    //----------//
    // evaluate //
    //----------//
    @Override
    public Evaluation[] evaluate (Glyph glyph,
                                  SystemInfo system,
                                  int count,
                                  double minGrade,
                                  EnumSet<ShapeEvaluator.Condition> conditions,
                                  Predicate<Shape> predicate)
    {
        List<Evaluation> bests = new ArrayList<Evaluation>();
        Evaluation[] evals = getSortedEvaluations(glyph);

        EvalsLoop:
        for (Evaluation eval : evals) {
            // Bounding test?
            if ((bests.size() >= count) || (eval.grade < minGrade)) {
                break;
            }

            // Predicate?
            if ((predicate != null) && !predicate.check(eval.shape)) {
                continue;
            }

            //            // Allowed?
            //            if (conditions.contains(Condition.ALLOWED) && glyph.isShapeForbidden(eval.shape)) {
            //                continue;
            //            }
            //
            // Successful checks?
            if (conditions.contains(Condition.CHECKED)) {
                ///Evaluation oldEval = new Evaluation(eval.shape, eval.grade);
                double[] ins = ShapeDescription.features(glyph);
                // This may change the eval shape...
                glyphChecker.annotate(system, eval, glyph, ins);

                if (eval.failure != null) {
                    continue;
                }

                //                // In case the specific checks have changed eval shape
                //                // we have to retest against the glyph blacklist
                //                if ((eval.shape != oldEval.shape)
                //                    && conditions.contains(Condition.ALLOWED)
                //                    && glyph.isShapeForbidden(eval.shape)) {
                //                    continue;
                //                }
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

        return bests.toArray(new Evaluation[0]);
    }

    //------------//
    // evaluateAs //
    //------------//
    @Override
    public Evaluation evaluateAs (Glyph glyph,
                                  Shape shape)
    {
        final Evaluation[] evals = getNaturalEvaluations(glyph);
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
    public boolean isBigEnough (Glyph glyph)
    {
        return isBigEnough(glyph.getNormalizedWeight());
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
    @Override
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

    //---------//
    // rawVote //
    //---------//
    @Override
    public Evaluation rawVote (Glyph glyph,
                               double minGrade,
                               Predicate<Shape> predicate)
    {
        Evaluation[] evals = evaluate(glyph, null, 1, minGrade, EnumSet.of(ALLOWED), predicate);

        if (evals.length > 0) {
            return evals[0];
        } else {
            return null;
        }
    }

    //------//
    // stop //
    //------//
    /**
     * Stop the on-going training.
     * By default, this is a no-op
     */
    @Override
    public void stop ()
    {
    }

    //------//
    // Vote //
    //------//
    @Override
    public Evaluation vote (Glyph glyph,
                            SystemInfo system,
                            double minGrade,
                            EnumSet<Condition> conditions,
                            Predicate<Shape> predicate)
    {
        Evaluation[] evals = evaluate(glyph, system, 1, minGrade, conditions, predicate);

        if (evals.length > 0) {
            return evals[0];
        } else {
            return null;
        }
    }

    //------//
    // vote //
    //------//
    @Override
    public Evaluation vote (Glyph glyph,
                            SystemInfo system,
                            double minGrade,
                            Predicate<Shape> predicate)
    {
        Evaluation[] evals = evaluate(
                glyph,
                system,
                1,
                minGrade,
                EnumSet.of(ALLOWED, CHECKED),
                predicate);

        if (evals.length > 0) {
            return evals[0];
        } else {
            return null;
        }
    }

    //------//
    // vote //
    //------//
    @Override
    public Evaluation vote (Glyph glyph,
                            SystemInfo system,
                            double minGrade)
    {
        Evaluation[] evals = evaluate(
                glyph,
                system,
                1,
                minGrade,
                EnumSet.of(ALLOWED, CHECKED),
                null);

        if (evals.length > 0) {
            return evals[0];
        } else {
            return null;
        }
    }

    //-------------//
    // getFileName //
    //-------------//
    /**
     * Report the simple file name, including extension but excluding
     * parent, which contains the marshalled data of the evaluator.
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
     * @throws JAXBException, IOException
     */
    protected abstract Object unmarshal (InputStream is)
            throws JAXBException, IOException;

    //----------------------//
    // getSortedEvaluations //
    //----------------------//
    /**
     * Run the evaluator with the specified glyph, and return a sequence of all
     * interpretations (ordered from best to worst) with no additional check.
     *
     * @param glyph the glyph to be examined
     * @return the ordered best evaluations
     */
    protected Evaluation[] getSortedEvaluations (Glyph glyph)
    {
        // If too small, it's just NOISE
        if (!isBigEnough(glyph)) {
            return noiseEvaluations;
        } else {
            Evaluation[] evals = getNaturalEvaluations(glyph);
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
                } else {
                    if (!isCompatible(obj)) {
                        final String msg = "Obsolete user data for " + getName() + " in " + path
                                           + ", trying default data";
                        logger.warn(msg);
                        JOptionPane.showMessageDialog(null, msg);
                    } else {
                        // Tell the user we are not using the default
                        logger.debug("{} unmarshalled from {}", getName(), path);

                        return obj;
                    }
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
        } else {
            if (!isCompatible(obj)) {
                final String msg = "Obsolete default data for " + getName() + " in " + uri
                                   + ", please retrain from scratch";
                logger.warn(msg);
                /////TODO: JOptionPane.showMessageDialog(null, msg);
                obj = null;
            } else {
                logger.debug("{} unmarshalled from {}", getName(), uri);
            }
        }

        return obj;
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
                logger.warn("Error unmarshalling evaluator from " + name, ex);
            }
        }

        return null;
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
