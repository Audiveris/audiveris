//----------------------------------------------------------------------------//
//                                                                            //
//                A b s t r a c t G l y p h E v a l u a t o r                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2012. All rights reserved.               //
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

import omr.log.Logger;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Predicate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

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

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
            AbstractEvaluationEngine.class);

    /** Number of shapes to differentiate. */
    protected static final int shapeCount = 1
                                            + Shape.LAST_PHYSICAL_SHAPE.ordinal();

    /** A special evaluation array, used to report NOISE. */
    protected static final Evaluation[] noiseEvaluations = {
        new Evaluation(Shape.NOISE, Evaluation.ALGORITHM)
    };

    /** The glyph checker for additional specific checks. */
    protected ShapeChecker glyphChecker = ShapeChecker.getInstance();

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

            // Everything is OK
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

        // First, make a backup copy of current file if it exists
        if (file.exists()) {
            try {
                Files.move(file.toPath(),
                        file.toPath().resolveSibling(getBackupName()),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception ex) {
                logger.warning("Could not backup " + file, ex);
            }
        }

        // Then save engine to file
        OutputStream os = null;

        try {
            os = new FileOutputStream(file);
            marshal(os);
            logger.info("Engine marshalled to {0}", file);
        } catch (FileNotFoundException ex) {
            logger.warning("Could not find file " + file, ex);
        } catch (IOException ex) {
            logger.warning("IO error on file " + file, ex);
        } catch (JAXBException ex) {
            logger.warning("Error marshalling engine to " + file, ex);
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
     * @throws JAXBException
     *                       throws
     *                       IOException
     */
    protected abstract Object unmarshal (InputStream is)
            throws JAXBException, IOException;

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the evaluation engine from the most suitable backup.
     * If the standard file does not exist or cannot be unmarshalled, the
     * backup file, if any, is tried.
     *
     * @return the unmarshalled engine, or null if everything failed
     */
    protected Object unmarshal ()
    {
        // Try standard file
        File file = new File(WellKnowns.EVAL_FOLDER, getFileName());
        Object obj = unmarshal(file);

        if (obj != null) {
            return obj;
        }

        // Try backup file
        File backup = new File(WellKnowns.EVAL_FOLDER, getBackupName());
        obj = unmarshal(backup);

        if (obj == null) {
            logger.warning("Could not load {0}", backup);
        } else {
            logger.info("{0} unmarshalled from {1}", getName(), backup);
        }

        return obj;
    }

    //---------------//
    // getBackupName //
    //---------------//
    private String getBackupName ()
    {
        return "backup-" + getFileName();
    }

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
            logger.warning("File not found " + file, ex);

            return null;
        } catch (Exception ex) {
            logger.warning("Error unmarshalling from " + file, ex);

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
            logger.warning("No data stream for {0} engine as {1}",
                    getName(), name);
        } else {
            try {
                Object engine = unmarshal(is);
                is.close();

                return engine;
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find or read " + name, ex);
            } catch (IOException ex) {
                logger.warning("IO error on " + name, ex);
            } catch (JAXBException ex) {
                logger.warning("Error unmarshalling evaluator from " + name, ex);
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
