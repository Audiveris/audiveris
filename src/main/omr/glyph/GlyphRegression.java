//----------------------------------------------------------------------------//
//                                                                            //
//                       G l y p h R e g r e s s i o n                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.Population;

import omr.util.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * Class <code>GlyphRegression</code> is a glyph evaluator based on a {@link
 * omr.math.NeuralNetwork}.
 *
 * <p>Note that this evaluator has been deprecated. It is used internally and
 * temporarily in the selection of core sheets among the training material. It
 * is no longer visible by the end user.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphRegression
    extends Evaluator
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger    logger = Logger.getLogger(
        GlyphRegression.class);

    /** The singleton */
    private static GlyphRegression INSTANCE;

    //~ Instance fields --------------------------------------------------------

    /** All shape descriptions */
    private ShapeDesc[] shapeDescs;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // GlyphRegression //
    //-----------------//
    /**
     * Private constructor
     */
    private GlyphRegression ()
    {
        // Allocate shape descriptors, as a brand new one
        logger.fine("Creating a brand new GlyphRegression");
        shapeDescs = new ShapeDesc[outSize];

        for (int s = outSize - 1; s >= 0; s--) {
            shapeDescs[s] = new ShapeDesc(Shape.values()[s]);
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

    //----------------//
    // getEvaluations //
    //----------------//
    /**
     * Report the results of evaluating a glyph
     *
     * @param glyph the glyph to evaluate
     *
     * @return ordered array of evaluations
     */
    @Override
    public Evaluation[] getEvaluations (Glyph glyph)
    {
        // If too small, it's just NOISE
        if (!isBigEnough(glyph)) {
            return noiseEvaluations;
        } else {
            double[]     ins = feedInput(glyph, null);
            Evaluation[] evals = new Evaluation[outSize];

            for (int s = 0; s < outSize; s++) {
                ShapeDesc desc = shapeDescs[s];
                evals[s] = new Evaluation();
                evals[s].shape = desc.shape;
                evals[s].grade = desc.distance(ins);
            }

            // Order the evals from best to worst
            Arrays.sort(evals, comparator);

            return evals;
        }
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report a name for this evaluator
     *
     * @return a simple name
     */
    @Override
    public String getName ()
    {
        return "Regression";
    }

    //-----------//
    // isTrained //
    //-----------//
    /**
     * Check whether the evaluator has been trained
     *
     * @return true if trained
     */
    @Override
    public boolean isTrained ()
    {
        for (ShapeDesc desc : shapeDescs) {
            if (desc.populations[0].getCardinality() >= 2) {
                return true;
            }
        }

        return false;
    }

    //------//
    // dump //
    //------//
    /**
     * Dump all descriptions to the standard output
     */
    @Override
    public void dump ()
    {
        for (ShapeDesc desc : shapeDescs) {
            desc.dump();
        }
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
        shapeDescs[shape.ordinal()].dumpDistance(glyph);
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
        return shapeDescs[shape.ordinal()].distance(glyph);
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
        if (base.size() == 0) {
            logger.warning("No glyph to retrain Regression Evaluator");

            return;
        }

        // Reset counters ?
        if (mode == StartingMode.SCRATCH) {
            for (ShapeDesc desc : shapeDescs) {
                for (Population population : desc.populations) {
                    population.reset();
                }
            }
        }

        // Accumulate
        double[] ins = new double[inSize];

        for (Glyph glyph : base) {
            if (monitor != null) {
                monitor.glyphProcessed(glyph);
            }

            try {
                ShapeDesc desc = shapeDescs[glyph.getShape()
                                                 .ordinal()];
                desc.include(feedInput(glyph, ins));
            } catch (Exception ex) {
                logger.warning("Weird shape : " + glyph.getShape());
            }
        }

        // Determine means & weights
        for (ShapeDesc desc : shapeDescs) {
            desc.compute();
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
        extends ConstantSet
    {
        Constant.Double weightMax = new Constant.Double(5e3, "Maximum weight");

        Constants ()
        {
            initialize();
        }
    }

    //-----------//
    // ShapeDesc //
    //-----------//
    /**
     * Class <code>ShapeDesc</code> gathers all characteristics needed to
     * recognize the shape of a glyph
     */
    private static class ShapeDesc
        implements java.io.Serializable
    {
        // The related shape
        final Shape        shape;

        // Mean for each criteria
        final double[]     means = new double[inSize];

        // Counters to compute mean value & std deviation
        final Population[] populations = new Population[inSize];

        // Weight for each criteria
        final double[] weights = new double[inSize];

        //-----------//
        // ShapeDesc //
        //-----------//
        ShapeDesc (Shape shape)
        {
            this.shape = shape;

            // Allocate population counters
            for (int c = inSize - 1; c >= 0; c--) {
                populations[c] = new Population();
            }

            Arrays.fill(means, 0);
            Arrays.fill(weights, 0);
        }

        //---------//
        // compute //
        //---------//
        public void compute ()
        {
            double weightMax = constants.weightMax.getValue();

            if (populations[0].getCardinality() >= 2) {
                for (int c = 0; c < inSize; c++) {
                    Population population = populations[c];
                    means[c] = population.getMeanValue();
                    weights[c] = Math.min(
                        weightMax,
                        1d / (inSize * population.getStandardDeviation()));
                }
            }
        }

        //----------//
        // distance //
        //----------//
        public double distance (Glyph glyph)
        {
            return distance(feedInput(glyph, null));
        }

        //----------//
        // distance //
        //----------//
        public double distance (double[] ins)
        {
            if (populations[0].getCardinality() >= 2) {
                double dist = 0;

                for (int c = 0; c < inSize; c++) {
                    double d = (means[c] - ins[c]) * weights[c];
                    dist += (d * d);
                }

                return dist;
            } else {
                return 50e50;
            }
        }

        //------//
        // dump //
        //------//
        public void dump ()
        {
            System.out.printf(
                "\n%30s %3d\n",
                shape.toString(),
                populations[0].getCardinality());

            if (populations[0].getCardinality() >= 2) {
                for (int c = 0; c < inSize; c++) {
                    System.out.printf(
                        "%2d %7s -> mean=% e std=% e wgt=% e\n",
                        c,
                        getLabel(c),
                        means[c],
                        populations[c].getStandardDeviation(),
                        weights[c]);
                }
            }
        }

        //--------------//
        // dumpDistance //
        //--------------//
        public double dumpDistance (Glyph glyph)
        {
            return dumpDistance(feedInput(glyph, null));
        }

        //--------------//
        // dumpDistance //
        //--------------//
        public double dumpDistance (double[] ins)
        {
            if (populations[0].getCardinality() >= 2) {
                double dist = 0;

                for (int c = 0; c < inSize; c++) {
                    double dm = Math.abs(means[c] - ins[c]);
                    double wdm = weights[c] * dm;
                    dist += (wdm * wdm);
                    System.out.printf(
                        "%2d-> dm:%e wgt:%e wdm:%e\n",
                        c,
                        dm,
                        weights[c],
                        wdm);
                }

                System.out.printf("Dist to %s=%e\n", shape, dist);

                return dist;
            } else {
                return 50e50;
            }
        }

        //---------//
        // include //
        //---------//
        public void include (double[] ins)
        {
            for (int c = inSize - 1; c >= 0; c--) {
                populations[c].includeValue(ins[c]);
            }
        }
    }
}
