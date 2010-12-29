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

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import static omr.glyph.Shape.*;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.GlyphRepository;

import omr.log.Logger;

import omr.math.LinearEvaluator;
import omr.math.LinearEvaluator.Sample;

import org.jdesktop.application.Application.ExitListener;

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

    /** The constraints (minimum, maximum) per shape & per parameter */
    private EnumMap<Shape, Range[]> constraintMap = new EnumMap<Shape, Range[]>(
        Shape.class);

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
            defineConstraints();

            // debug
            //            for (Shape shape : ShapeRange.allSymbols) {
            //                dumpOneShapeConstraints(shape);
            //            }
        }

        // Listen to application exit
        if (Main.getGui() != null) {
            Main.getGui()
                .addExitListener(
                new ExitListener() {
                        public boolean canExit (EventObject eo)
                        {
                            return true;
                        }

                        public void willExit (EventObject eo)
                        {
                            if (engine.isDataModified()) {
                                marshal();
                            }
                        }
                    });
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

    //------------//
    // setMaximum //
    //------------//
    /**
     * Set the constraint test on maximum for a parameter of the provided
     * shape
     * @param paramIndex the impacted parameter
     * @param shape the targeted shape
     * @param val the new maximum value (null for disabling the test)
     */
    public void setMaximum (int    paramIndex,
                            Shape  shape,
                            Double val)
    {
        doGetRange(paramIndex, shape).max = val;
    }

    //------------//
    // getMaximum //
    //------------//
    /**
     * Get the constraint test on maximum for a parameter of the provided
     * shape
     * @param paramIndex the impacted parameter
     * @param shape the targeted shape
     * @return the current maximum value (null if test is disabled)
     */
    public Double getMaximum (int   paramIndex,
                              Shape shape)
    {
        Range[] ranges = constraintMap.get(shape);

        if (ranges == null) {
            return null;
        }

        Range range = ranges[paramIndex];

        return (range != null) ? range.max : null;
    }

    //------------//
    // setMinimum //
    //------------//
    /**
     * Set the constraint test on minimum for a parameter of the provided
     * shape
     * @param paramIndex the impacted parameter
     * @param shape the targeted shape
     * @param val the new minimum value (null for disabling the test)
     */
    public void setMinimum (int    paramIndex,
                            Shape  shape,
                            Double val)
    {
        doGetRange(paramIndex, shape).min = val;
    }

    //------------//
    // getMinimum //
    //------------//
    /**
     * Get the constraint test on minimum for a parameter of the provided
     * shape
     * @param paramIndex the impacted parameter
     * @param shape the targeted shape
     * @return the current minimum value (null if test is disabled)
     */
    public Double getMinimum (int   paramIndex,
                              Shape shape)
    {
        Range[] ranges = constraintMap.get(shape);

        if (ranges == null) {
            return null;
        }

        Range range = ranges[paramIndex];

        return (range != null) ? range.min : null;
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
            double[]     ins = feedInput(glyph);
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

    //    //--------------------//
    //    // constraintsMatched //
    //    //--------------------//
    //    /**
    //     * Check that all the (non-disabled) constraints matched between a given
    //     * glyph and a shape
    //     * @param glyph the glyph at hand
    //     * @param eval the evaluation context
    //     * @return true if matched, false otherwise
    //     */
    //    public boolean constraintsMatched (Glyph      glyph,
    //                                       Evaluation eval)
    //    {
    //        return constraintsMatched(feedInput(glyph), eval);
    //    }

    //--------------------//
    // constraintsMatched //
    //--------------------//
    /**
     * Check that all the (non-disabled) constraints matched between a given
     * glyph and a shape
     * @param params the glyph features
     * @param eval the evaluation context to update
     * @return true if matched, false otherwise
     */
    public boolean constraintsMatched (double[]   params,
                                       Evaluation eval)
    {
        String failed = firstMisMatched(params, eval.shape);

        if (failed != null) {
            eval.failure = new Evaluation.Failure(failed);

            return false;
        } else {
            return true;
        }
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

    //-------------------------//
    // dumpOneShapeConstraints //
    //-------------------------//
    public void dumpOneShapeConstraints (Shape shape)
    {
        System.out.print("Constraints for " + shape + ": ");

        String[] labels = getParameterLabels();
        Range[]  ranges = constraintMap.get(shape);

        if (ranges == null) {
            System.out.println("none");
        } else {
            System.out.println();

            for (int p = 0; p < paramCount; p++) {
                StringBuilder sb = new StringBuilder();
                Range         range = ranges[p];

                if (range != null) {
                    Double min = range.min;

                    if (min != null) {
                        sb.append(" min=")
                          .append(min);
                    }

                    Double max = range.max;

                    if (max != null) {
                        sb.append(" max=")
                          .append(max);
                    }
                }

                if (sb.length() > 0) {
                    System.out.println("   " + labels[p] + ":" + sb.toString());
                }
            }
        }
    }

    //---------------//
    // includeSample //
    //---------------//
    /**
     * Take into account the observed parameters for the provided shape, and
     * relax the related constraints if needed
     * @param params the observed input parameters
     * @param shape the provided shape
     * @return true if constraints have been widened
     */
    public boolean includeSample (double[] params,
                                  Shape    shape)
    {
        // Include this observation
        if (engine.includeSample(params, shape.toString())) {
            // Update constraints for the shape
            defineOneShapeConstraints(shape);

            return true;
        }

        return false;
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
        return engine.categoryDistance(feedInput(glyph), shape.toString());
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
        return measureDistance(one, feedInput(two));
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
        return engine.patternDistance(feedInput(glyph), ins);
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
    public void train (Collection<Glyph> base,
                       Monitor           monitor,
                       StartingMode      mode)
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
                Sample sample = new Sample(shape.toString(), feedInput(glyph));
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

    //-------------------//
    // defineConstraints //
    //-------------------//
    /**
     * Here we customize the linear evaluator to our specific needs, by removing
     * some constraint checks and relaxing others.
     */
    private void defineConstraints ()
    {
        for (Shape shape : ShapeRange.allSymbols) {
            defineOneShapeConstraints(shape);
        }
    }

    //---------------------------//
    // defineOneShapeConstraints //
    //---------------------------//
    /**
     * Here we customize the constraints to our specific needs, by removing
     * some constraint checks and relaxing others.
     * @param shape the shape at hand
     */
    private void defineOneShapeConstraints (Shape shape)
    {
        // First, use LinearEvaluator observed constraints
        for (int p = 0; p < paramCount; p++) {
            setMinimum(p, shape, engine.getMinimum(p, shape.name()));
            setMaximum(p, shape, engine.getMaximum(p, shape.name()));
        }

        // Second, relax some constraints
        // Add some margin around constraints
        double minFactor = constants.factorForMinima.getValue();
        double maxFactor = constants.factorForMaxima.getValue();

        for (String label : Arrays.asList("weight", "width", "height")) {
            int    p = GlyphEvaluator.getParameterIndex(label);
            double val = getMinimum(p, shape);

            if (val > 0) {
                setMinimum(p, shape, val * minFactor);
            } else {
                setMinimum(p, shape, val * maxFactor);
            }

            val = getMaximum(p, shape);

            if (val > 0) {
                setMaximum(p, shape, val * maxFactor);
            } else {
                setMaximum(p, shape, val * minFactor);
            }
        }

        // Disable some selected features
        for (String label : Arrays.asList(
            "ledger",
            "n11",
            "n20",
            "n02",
            "n30",
            "n21",
            "n12",
            "n03")) {
            int p = GlyphEvaluator.getParameterIndex(label);
            setMinimum(p, shape, null);
            setMaximum(p, shape, null);
        }

        // Keep "stemNb" exactly as it is, with no margin

        // Third, remove some constraints
        switch (shape) {
        case TEXT :
            disableMaximum(TEXT, "weight");
            disableMaximum(TEXT, "width");

            break;

        case STRUCTURE :
            disableMaximum(STRUCTURE, "weight");
            disableMaximum(STRUCTURE, "width");
            disableMaximum(STRUCTURE, "height");

            break;

        case BRACE :
            disableMaximum(BRACE, "weight");
            disableMaximum(BRACE, "height");

            break;

        case BRACKET :
            disableMaximum(BRACKET, "weight");
            disableMaximum(BRACKET, "height");

            break;

        case BEAM :
            disableMaximum(BEAM, "weight");
            disableMaximum(BEAM, "width");
            disableMaximum(BEAM, "height");

            break;

        case BEAM_2 :
            disableMaximum(BEAM_2, "weight");
            disableMaximum(BEAM_2, "width");
            disableMaximum(BEAM_2, "height");

            break;

        case BEAM_3 :
            disableMaximum(BEAM_3, "weight");
            disableMaximum(BEAM_3, "width");
            disableMaximum(BEAM_3, "height");

            break;

        case SLUR :
            disableMaximum(SLUR, "weight");
            disableMaximum(SLUR, "width");
            disableMaximum(SLUR, "height");

            break;

        case ARPEGGIATO :
            disableMaximum(ARPEGGIATO, "weight");
            disableMaximum(ARPEGGIATO, "height");

            break;

        case CRESCENDO :
            disableMaximum(CRESCENDO, "weight");
            disableMaximum(CRESCENDO, "width");
            disableMaximum(CRESCENDO, "height");

            break;

        case DECRESCENDO :
            disableMaximum(DECRESCENDO, "weight");
            disableMaximum(DECRESCENDO, "width");
            disableMaximum(DECRESCENDO, "height");

        default :
        }
    }

    //----------------//
    // disableMaximum //
    //----------------//
    private void disableMaximum (Shape  shape,
                                 String paramLabel)
    {
        setMaximum(GlyphEvaluator.getParameterIndex(paramLabel), shape, null);
    }

    //----------------//
    // disableMinimum //
    //----------------//
    private void disableMinimum (Shape  shape,
                                 String paramLabel)
    {
        setMinimum(GlyphEvaluator.getParameterIndex(paramLabel), shape, null);
    }

    //------------//
    // doGetRange //
    //------------//
    /**
     * Retrieve (and create if necessary) the range entity that corresponds to
     * parameter of paramIndex for the provided shape
     * @param paramIndex parameter reference
     * @param shape provided shape
     * @return the desired range entity
     */
    private Range doGetRange (int   paramIndex,
                              Shape shape)
    {
        Range[] ranges = constraintMap.get(shape);

        if (ranges == null) {
            ranges = new Range[paramCount];
            constraintMap.put(shape, ranges);
        }

        Range range = ranges[paramIndex];

        if (range == null) {
            ranges[paramIndex] = range = new Range();
        }

        return range;
    }

    //-----------------//
    // firstMisMatched //
    //-----------------//
    /**
     * Perform a basic check on max / min bounds, if any, for each parameter
     * value of the provided pattern
     * @param pattern the collection of parameters to check with respect to
     * targeted shape
     * @param shape the targeted shape
     * @return the name of the first failing check, null otherwise
     */
    private String firstMisMatched (double[] pattern,
                                    Shape    shape)
    {
        String[] labels = getParameterLabels();
        Range[]  ranges = constraintMap.get(shape);

        if (ranges == null) {
            return null; // No test => OK
        }

        for (int p = 0; p < paramCount; p++) {
            String label = labels[p];
            double val = pattern[p];
            Range  range = ranges[p];

            if (range == null) {
                continue;
            }

            Double min = range.min;

            if ((min != null) && (val < min)) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        shape + " failed on minimum for " + label + " " + val +
                        " < " + min);
                }

                return label + ".min";
            }

            Double max = range.max;

            if ((max != null) && (val > max)) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        shape + " failed on maximum for " + label + " " + val +
                        " > " + max);
                }

                return label + ".max";
            }
        }

        // Everything is OK
        return null;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-------//
    // Range //
    //-------//
    public static class Range
    {
        //~ Instance fields ----------------------------------------------------

        /** Constraint on minimum, if any */
        Double min;

        /** Constraint on maximum, if any */
        Double max;
    }

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
