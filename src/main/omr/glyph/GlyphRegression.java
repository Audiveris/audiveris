//----------------------------------------------------------------------------//
//                                                                            //
//                       G l y p h R e g r e s s i o n                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import static omr.glyph.Shape.*;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.GlyphRepository;

import omr.log.Logger;

import omr.math.LinearEvaluator;
import omr.math.LinearEvaluator.Sample;

import java.io.*;
import java.util.*;

import javax.swing.JOptionPane;
import javax.xml.bind.JAXBException;

/**
 * Class <code>GlyphRegression</code> is a glyph evaluator that encapsulates a
 * {@link LinearEvaluator} working on glyph parameters.
 *
 * @author Hervé Bitteur
 */
public class GlyphRegression
    extends GlyphEvaluator
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        GlyphRegression.class);

    /** LinearEvaluator backup file name */
    private static final String BACKUP_FILE_NAME = "linear-evaluator.xml";

    /** The singleton */
    private static volatile GlyphRegression INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** The encapsulated linear evaluator */
    private LinearEvaluator engine;

    /** The glyph checker for additional specific checks */
    private GlyphChecker glyphChecker = GlyphChecker.getInstance();

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // GlyphRegression //
    //-----------------//
    /**
     * Private constructor
     */
    private GlyphRegression ()
    {
        // Unmarshal from backup data
        engine = (LinearEvaluator) unmarshal();

        // Basic check
        if (engine != null) {
            if (engine.getInputSize() != paramCount) {
                final String msg = "Linear Evaluator data is obsolete," +
                                   " it must be retrained from scratch";
                logger.warning(msg);
                JOptionPane.showMessageDialog(null, msg);

                engine = null;
            }
        }

        if (engine == null) {
            // Get a brand new one (not trained)
            logger.info("Creating a brand new LinearEvaluator");
            engine = new LinearEvaluator(getParameterLabels());
        } else {
            customizeEngine();
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Provide access to the single instance of GlyphRegression for the
     * application
     *
     * @return the GlyphRegression instance
     */
    public static GlyphRegression getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new GlyphRegression();
        }

        return INSTANCE;
    }

    //-----------------------//
    // getCheckedEvaluations //
    //-----------------------//
    @Override
    public Evaluation[] getCheckedEvaluations (Glyph glyph)
    {
        // If too small, it's just NOISE
        if (!isBigEnough(glyph)) {
            return noiseEvaluations;
        } else {
            double[]     ins = feedInput(glyph, null);
            Evaluation[] evals = new Evaluation[shapeCount];
            Shape[]      values = Shape.values();

            for (int s = 0; s < shapeCount; s++) {
                Shape shape = values[s];
                shape = glyphChecker.specificCheck(shape, glyph, ins);

                if (shape != null) {
                    evals[s] = new Evaluation(
                        shape,
                        measureDistance(ins, shape));
                } else {
                    evals[s] = new Evaluation(values[s], Double.MAX_VALUE);
                }
            }

            // Order the evals from best to worst
            Arrays.sort(evals, comparator);

            return evals;
        }
    }

    //-----------//
    // getEngine //
    //-----------//
    /**
     * @return the engine
     */
    public LinearEvaluator getEngine ()
    {
        return engine;
    }

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "Regression";
    }

    //-------------------//
    // getRawEvaluations //
    //-------------------//
    @Override
    public Evaluation[] getRawEvaluations (Glyph glyph)
    {
        // If too small, it's just NOISE
        if (!isBigEnough(glyph)) {
            return noiseEvaluations;
        } else {
            double[]     ins = feedInput(glyph, null);
            Evaluation[] evals = new Evaluation[shapeCount];
            Shape[]      values = Shape.values();

            for (int s = 0; s < shapeCount; s++) {
                Shape shape = values[s];
                evals[s] = new Evaluation(shape, measureDistance(ins, shape));
            }

            // Order the evals from best to worst
            Arrays.sort(evals, comparator);

            return evals;
        }
    }

    //--------------------//
    // constraintsMatched //
    //--------------------//
    /**
     * Check that all the (non-disabled) constraints matched between a given
     * glyph and a shape
     * @param glyph the glyph at hand
     * @param shape the shape to check constraints for
     * @return true if matched, false otherwise
     */
    public boolean constraintsMatched (Glyph glyph,
                                       Shape shape)
    {
        return constraintsMatched(feedInput(glyph, null), shape);
    }

    //--------------------//
    // constraintsMatched //
    //--------------------//
    /**
     * Check that all the (non-disabled) constraints matched between a given
     * glyph and a shape
     * @param params the glyph features
     * @param shape the shape to check constraints for
     * @return true if matched, false otherwise
     */
    public boolean constraintsMatched (double[] params,
                                       Shape    shape)
    {
        return engine.categoryMatched(params, shape.toString());
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        engine.dump();
    }

    //--------------//
    // dumpDistance //
    //--------------//
    /**
     * Print out the "distance" information between a given glyph and a
     * shape. It's a sort of debug information.
     *
     * @param glyph the glyph at hand
     * @param shape the shape to measure distance from
     */
    public void dumpDistance (Glyph glyph,
                              Shape shape)
    {
        //        shapeDescs[shape.ordinal()].dumpDistance(glyph);
    }

    //-----------------//
    // measureDistance //
    //-----------------//
    /**
     * Measure the "distance" information between a given glyph and a shape.
     *
     * @param glyph the glyph at hand
     * @param shape the shape to measure distance from
     * @return the measured distance
     */
    public double measureDistance (Glyph glyph,
                                   Shape shape)
    {
        return engine.categoryDistance(
            feedInput(glyph, null),
            shape.toString());
    }

    //-----------------//
    // measureDistance //
    //-----------------//
    /**
     * Measure the "distance" information between a given glyph and a shape.
     *
     * @param ins the input parameters
     * @param shape the shape to measure distance from
     * @return the measured distance
     */
    public double measureDistance (double[] ins,
                                   Shape    shape)
    {
        return engine.categoryDistance(ins, shape.toString());
    }

    //-----------------//
    // measureDistance //
    //-----------------//
    /**
     * Measure the "distance" information between two glyphs
     *
     * @param one the first glyph
     * @param two the second glyph
     * @return the measured distance
     */
    public double measureDistance (Glyph one,
                                   Glyph two)
    {
        return measureDistance(one, feedInput(two, null));
    }

    //-----------------//
    // measureDistance //
    //-----------------//
    /**
     * Measure the "distance" information between a glyph and an array of
     * parameters (generally fed from another glyph)
     *
     * @param glyph the given glyph
     * @param ins the array (size = paramCount) of parameters
     * @return the measured distance
     */
    public double measureDistance (Glyph    glyph,
                                   double[] ins)
    {
        return engine.patternDistance(feedInput(glyph, null), ins);
    }

    //-------//
    // train //
    //-------//
    /**
     * Launch the training of the evaluator
     *
     * @param base the collection of glyphs used for training
     * @param monitor a monitoring entity
     * @param mode incremental or scratch mode
     */
    @Override
    public void train (List<Glyph>  base,
                       Monitor      monitor,
                       StartingMode mode)
    {
        if (base.isEmpty()) {
            logger.warning("No glyph to retrain Regression Evaluator");

            return;
        }

        // Prepare the collection of samples
        Collection<Sample> samples = new ArrayList<Sample>();

        for (Glyph glyph : base) {
            try {
                Shape  shape = glyph.getShape();
                Sample sample = new Sample(
                    shape.toString(),
                    feedInput(glyph, null));
                samples.add(sample);
            } catch (Exception ex) {
                logger.warning(
                    "Weird glyph shape: " + glyph.getShape() + " file=" +
                    GlyphRepository.getInstance().getGlyphName(glyph),
                    ex);
            }
        }

        // Do the training
        engine.train(samples);

        // Save to disk
        marshal();
    }

    //-------------//
    // getFileName //
    //-------------//
    @Override
    protected String getFileName ()
    {
        return BACKUP_FILE_NAME;
    }

    //---------//
    // marshal //
    //---------//
    @Override
    protected void marshal (OutputStream os)
        throws FileNotFoundException, IOException, JAXBException
    {
        engine.marshal(os);
    }

    //-----------//
    // unmarshal //
    //-----------//
    @Override
    protected LinearEvaluator unmarshal (InputStream is)
        throws JAXBException
    {
        return LinearEvaluator.unmarshal(is);
    }

    //-----------------//
    // customizeEngine //
    //-----------------//
    private void customizeEngine ()
    {
        for (Shape shape : ShapeRange.allSymbols) {
            final String id = shape.name();

            // Add some margin around constraints
            double minFactor = constants.factorForMinima.getValue();
            double maxFactor = constants.factorForMaxima.getValue();

            for (String label : Arrays.asList("weight", "width", "height")) {
                int p = GlyphEvaluator.getParameterIndex(label);
                engine.setMinimum(p, id, engine.getMinimum(p, id) * minFactor);
                engine.setMaximum(p, id, engine.getMaximum(p, id) * maxFactor);
            }

            // Use only selected features
            for (String label : Arrays.asList(
                "n20",
                "n11",
                "n02",
                "n30",
                "n21",
                "n12",
                "n03",
                "ledger")) {
                int p = GlyphEvaluator.getParameterIndex(label);
                engine.setMinimum(p, id, null);
                engine.setMaximum(p, id, null);
            }

            // Keep "stemNb" exactly as it is, with no margin
        }

        // Some specific tuning
        disableMaximum(TEXT, "weight");
        disableMaximum(TEXT, "width");

        disableMaximum(STRUCTURE, "weight");
        disableMaximum(STRUCTURE, "width");
        disableMaximum(STRUCTURE, "height");

        disableMaximum(BRACE, "weight");
        disableMaximum(BRACE, "height");

        disableMaximum(BRACKET, "weight");
        disableMaximum(BRACKET, "height");

        disableMaximum(BEAM, "weight");
        disableMaximum(BEAM, "width");
        disableMaximum(BEAM, "height");

        disableMaximum(BEAM_2, "weight");
        disableMaximum(BEAM_2, "width");
        disableMaximum(BEAM_2, "height");

        disableMaximum(BEAM_3, "weight");
        disableMaximum(BEAM_3, "width");
        disableMaximum(BEAM_3, "height");

        disableMaximum(SLUR, "weight");
        disableMaximum(SLUR, "width");
        disableMaximum(SLUR, "height");

        disableMaximum(ARPEGGIATO, "weight");
        disableMaximum(ARPEGGIATO, "height");

        disableMaximum(CRESCENDO, "weight");
        disableMaximum(CRESCENDO, "width");
        disableMaximum(CRESCENDO, "height");

        disableMaximum(DECRESCENDO, "weight");
        disableMaximum(DECRESCENDO, "width");
        disableMaximum(DECRESCENDO, "height");
    }

    private void disableMaximum (Shape  shape,
                                 String paramLabel)
    {
        engine.setMaximum(
            GlyphEvaluator.getParameterIndex(paramLabel),
            shape.name(),
            null);
    }

    private void disableMinimum (Shape  shape,
                                 String paramLabel)
    {
        engine.setMinimum(
            GlyphEvaluator.getParameterIndex(paramLabel),
            shape.name(),
            null);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Double factorForMinima = new Constant.Double(
            "factor",
            0.8,
            "Factor applied to all minimum constraints");
        Constant.Double factorForMaxima = new Constant.Double(
            "factor",
            1.2,
            "Factor applied to all maximum constraints");
    }
}
