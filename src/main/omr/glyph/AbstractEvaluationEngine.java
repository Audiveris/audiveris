//----------------------------------------------------------------------------//
//                                                                            //
//                A b s t r a c t G l y p h E v a l u a t o r                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;

/**
 * Class {@code AbstractEvaluationEngine} is an abstract implementation
 * for any evaluation engine.
 *
 * <p> <img src="doc-files/GlyphEvaluator.jpg" />
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractEvaluationEngine
        implements EvaluationEngine
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            AbstractEvaluationEngine.class);

    /** Number of shapes to differentiate. */
    protected static final int shapeCount = 1
                                            + Shape.LAST_PHYSICAL_SHAPE.ordinal();

    /** A special evaluation array, used to report NOISE. */
    protected static final Evaluation[] noiseEvaluations = {
        new Evaluation(Shape.NOISE, Evaluation.ALGORITHM)
    };

    //~ Instance fields --------------------------------------------------------
    //
    /** The glyph checker for additional specific checks. */
    protected ShapeChecker glyphChecker = ShapeChecker.getInstance();

    //~ Methods ----------------------------------------------------------------
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
        List<Evaluation> best = new ArrayList<>();
        Evaluation[] evals = getRawEvaluations(glyph);

        EvalsLoop:
        for (Evaluation eval : evals) {
            // Bounding test?
            if ((best.size() >= count) || (eval.grade < minGrade)) {
                break;
            }

            // Predicate?
            if ((predicate != null) && !predicate.check(eval.shape)) {
                continue;
            }

            // Allowed?
            if (conditions.contains(Condition.ALLOWED)
                && glyph.isShapeForbidden(eval.shape)) {
                continue;
            }

            // Successful checks?
            if (conditions.contains(Condition.CHECKED)) {
                Evaluation oldEval = new Evaluation(eval.shape, eval.grade);
                double[] ins = ShapeDescription.features(glyph);
                // This may change the eval shape...
                glyphChecker.annotate(system, eval, glyph, ins);

                if (eval.failure != null) {
                    continue;
                }

                // In case the specific checks have changed eval shape
                // we have to retest against the glyph blacklist
                if ((eval.shape != oldEval.shape)
                    && conditions.contains(Condition.ALLOWED)
                    && glyph.isShapeForbidden(eval.shape)) {
                    continue;
                }
            }

            // Everything is OK, add the shape if not already in the list
            for (Evaluation e : best) {
                if (e.shape == eval.shape) {
                    continue EvalsLoop;
                }
            }
            best.add(eval);
        }

        return best.toArray(new Evaluation[0]);
    }

    //-------------//
    // isBigEnough //
    //-------------//
    @Override
    public boolean isBigEnough (Glyph glyph)
    {
        return glyph.getNormalizedWeight() >= constants.minWeight.getValue();
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
        final File file = new File(WellKnowns.EVAL_FOLDER, getFileName());
        OutputStream os = null;

        try {
            os = new FileOutputStream(file);
            marshal(os);
            logger.info("Engine marshalled to {}", file);
        } catch (FileNotFoundException ex) {
            logger.warn("Could not find file " + file, ex);
        } catch (IOException ex) {
            logger.warn("IO error on file " + file, ex);
        } catch (JAXBException ex) {
            logger.warn("Error marshalling engine to " + file, ex);
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
        Evaluation[] evals = evaluate(glyph, null, 1, minGrade,
                EnumSet.of(ALLOWED), predicate);

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
        Evaluation[] evals = evaluate(glyph, system, 1, minGrade, conditions,
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
                            double minGrade,
                            Predicate<Shape> predicate)
    {
        Evaluation[] evals = evaluate(glyph, system, 1, minGrade,
                EnumSet.of(ALLOWED, CHECKED), predicate);

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
        Evaluation[] evals = evaluate(glyph, system, 1, minGrade,
                EnumSet.of(ALLOWED, CHECKED), null);

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

    //-------------------//
    // getRawEvaluations //
    //-------------------//
    /**
     * Run the evaluator with the specified glyph, and return a
     * sequence of interpretations (ordered from best to worst) with
     * no additional check.
     *
     * @param glyph the glyph to be examined
     * @return the ordered best evaluations
     */
    protected abstract Evaluation[] getRawEvaluations (Glyph glyph);

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
            File file = new File(WellKnowns.EVAL_FOLDER, getFileName());
            if (file.exists()) {
                Object obj = unmarshal(file);

                if (obj == null) {
                    logger.warn("Could not load {}", file);
                } else {
                    if (!isCompatible(obj)) {
                        final String msg = "Obsolete user data for " + getName()
                                           + " in " + file
                                           + ", trying default data";
                        logger.warn(msg);
                        JOptionPane.showMessageDialog(null, msg);
                    } else {
                        // Tell the user we are not using the default
                        logger.info("{} unmarshalled from {}", getName(), file);
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
                final String msg = "Obsolete default data for " + getName()
                                   + " in " + uri
                                   + ", please retrain from scratch";
                logger.warn(msg);
                JOptionPane.showMessageDialog(null, msg);

                obj = null;
            } else {
                logger.debug("{} unmarshalled from {}", getName(), uri);
            }
        }

        return obj;
    }

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

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the evaluation engine using provided file.
     *
     * @return the unmarshalled engine, or null if failed
     */
    private Object unmarshal (File file)
    {
        try {
            InputStream input = new FileInputStream(file);

            return unmarshal(input, getFileName());
        } catch (FileNotFoundException ex) {
            logger.warn("File not found " + file, ex);

            return null;
        } catch (Exception ex) {
            logger.warn("Error unmarshalling from " + file, ex);

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
            logger.warn("No data stream for {} engine as {}",
                    getName(), name);
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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {

        Scale.AreaFraction minWeight = new Scale.AreaFraction(0.08,
                "Minimum normalized weight to be considered not a noise");

    }
}
