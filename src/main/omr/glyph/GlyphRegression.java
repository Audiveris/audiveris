//-----------------------------------------------------------------------//
//                                                                       //
//                     G l y p h R e g r e s s i o n                     //
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
import omr.math.Cumul;
import omr.util.Logger;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * Class <code>GlyphRegression</code> is a glyph evaluator based on a
 * {@link omr.math.NeuralNetwork}.
 *
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class GlyphRegression
    extends Evaluator
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(GlyphRegression.class);
    private static final Constants constants = new Constants();

    // The singleton
    private static GlyphRegression INSTANCE;

    //~ Instance variables ------------------------------------------------

    // All shape descriptions
    private ShapeDesc[] shapeDescs;

    //~ Constructors ------------------------------------------------------

    //-----------------//
    // GlyphRegression //
    //-----------------//
    /**
     * Private constructor
     */
    private GlyphRegression ()
    {
        // Retrieve last custom version if any
        try {
            // First, look for a custom version
            File custom = getCustomBackup();
            if (custom.exists()){
                logger.info ("Deserializing GlyphRegression from custom"
                             + " file " + custom);
                shapeDescs = deserialize(new FileInputStream(custom));
            }
        } catch (FileNotFoundException ex) {
            logger.warning ("Cannot find custom backup " +
                            getCustomBackup());
        }

        // Second, use the system default
        if (shapeDescs == null){
            String resource = getSystemBackup();
            InputStream is = GlyphRegression.class.getResourceAsStream
                (resource);
            if (is != null) {
                logger.info ("Deserializing GlyphRegression from " +
                             "system resource " + resource);
                shapeDescs = deserialize(is);
            } else {
                logger.warning ("Cannot find system resource " + resource);
            }
        }

        // Basic check
        if (shapeDescs != null) {
            if (shapeDescs.length != outSize) {
                logger.warning("Obsolete Regression data,"
                               + " reconstructing from scratch");
                shapeDescs = null;
            }
        }

        if (shapeDescs == null) {
            // Allocate shape descriptors, as a brand new one
            logger.info ("Creating a brand new GlyphRegression");
            shapeDescs = new ShapeDesc[outSize];
            for (int s = outSize - 1; s >= 0; s--) {
                shapeDescs[s] = new ShapeDesc(Shape.values()[s]);
            }
        }
    }

    //~ Methods -----------------------------------------------------------

    //------//
    // dump //
    //------//
    @Override
        public void dump ()
    {
        for (ShapeDesc desc : shapeDescs) {
            desc.dump();
        }
    }

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
    @Override
        public Evaluation[] getEvaluations (Glyph glyph)
    {
        // If too small, it's just NOISE
        if (!isBigEnough(glyph)) {
            return noiseEvaluations;
        }

        double[] ins = feedInput(glyph, null);
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

    //---------//
    // getName //
    //---------//
    @Override
        public String getName()
    {
        return "Regression";
    }

    //-----------//
    // isTrained //
    //-----------//
    @Override
        public boolean isTrained ()
    {
        for (ShapeDesc desc : shapeDescs) {
            if (desc.cumuls[0].getNumber() >= 2) {
                return true;
            }
        }

        return false;
    }

    //-------//
    // train //
    //-------//
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
                for (Cumul cumul : desc.cumuls) {
                    cumul.reset();
                }
            }
        }

        // Accumulate
        double[] ins = new double[inSize];
        for (Glyph glyph : base) {
            if (monitor != null) {
                monitor.glyphProcessed(glyph);
            }

            ShapeDesc desc = shapeDescs[glyph.getShape().ordinal()];
            desc.include(feedInput(glyph, ins));
        }

        // Determine means & weights
        for (ShapeDesc desc : shapeDescs) {
            desc.compute();
        }

        serialize(getCustomBackup());
    }

    //--------------------//
    // getAcceptanceGrade //
    //--------------------//
    @Override
        public double getAcceptanceGrade ()
    {
        return constants.acceptanceGrade.getValue();
    }

    //-----------------//
    // measureDistance //
    //-----------------//
    /**
     * Measure the "distance" information between a given glyph and a
     * shape.
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

    //-------------//
    // deserialize //
    //-------------//
    /**
     * Deserialize the provided binary file to allocate the corresponding
     * regression evaluator
     *
     * @param in the input stream that contains the regression evaluator
     * definition in binary format
     *
     * @return the allocated shape descriptors.
     */
    public static ShapeDesc[] deserialize (InputStream in)
    {
        try {
            ObjectInputStream s = new ObjectInputStream(in);

            ShapeDesc[] sd = (ShapeDesc[]) s.readObject();
            s.close();
            if (logger.isDebugEnabled()) {
                logger.debug("Regression evaluator deserialized");
            }

            return sd;
        } catch (Exception ex) {
            logger.error("Could not deserialize Regression evaluator");
            logger.error(ex.toString());
            return null;
        }
    }

    //-----------//
    // serialize //
    //-----------//
    /**
     * Serialize the regression to its binary file
     */
    public void serialize (File file)
    {
        if (logger.isDebugEnabled()) {
            logger.debug("serialize to " + file.getPath());
        }

        try {
            FileOutputStream f = new FileOutputStream(file);
            ObjectOutput s = new ObjectOutputStream(f);
            s.writeObject(shapeDescs);
            s.close();
            logger.info("Regression evaluator serialized to " + file.getPath());
        } catch (Exception ex) {
            logger.error("Could not serialize Regression evaluator to " + file.getPath());
            logger.error(ex.toString());
        }
    }

    //~ Classes -----------------------------------------------------------

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
        //~ Static variables/initializers ---------------------------------

        public static final double[] factors = new double[inSize];

        static
        {
            Arrays.fill(factors, 1d);
//             factors[0]  = 2.0d;            // Weight
//             factors[11] = 0.2d;
//             factors[12] = 0.2d;
//             factors[13] = 0.2d;
//             factors[14] = 0.1d;
        }


        //~ Instance variables --------------------------------------------

        // The related shape
        final Shape shape;

        // Counters to compute mean value & std deviation
        final Cumul[] cumuls = new Cumul[inSize];

        // Mean for each criteria
        final double[] means = new double[inSize];

        // Weight for each criteria
        final double[] weights = new double[inSize];

        //-----------//
        // ShapeDesc //
        //-----------//
        ShapeDesc (Shape shape)
        {
            this.shape = shape;

            // Allocate cumul counters
            for (int c = inSize - 1; c >= 0; c--) {
                cumuls[c] = new Cumul();
            }

            Arrays.fill(means, 0);
            Arrays.fill(weights, 0);
        }

        //---------//
        // include //
        //---------//
        public void include (double[] ins)
        {
            for (int c = inSize - 1; c >= 0; c--) {
                cumuls[c].include(ins[c]);
            }
        }

        //---------//
        // compute //
        //---------//
        public void compute ()
        {
            double weightMax = constants.weightMax.getValue();
            if (cumuls[0].getNumber() >= 2) {
                for (int c = 0; c < inSize; c++) {
                    Cumul cumul = cumuls[c];
                    means[c] = cumul.getMean();
                    weights[c] = Math.min
                        (weightMax,
                         1d / (inSize * cumul.getStdDeviation()));
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

        //--------------//
        // dumpDistance //
        //--------------//
        public double dumpDistance (Glyph glyph)
        {
            return dumpDistance(feedInput(glyph, null));
        }

        //----------//
        // distance //
        //----------//
        public double distance (double[] ins)
        {
            if (cumuls[0].getNumber() >= 2) {
                double dist = 0;
                for (int c = 0; c < inSize; c++) {
                    //dist += Math.abs(means[c] - ins[c]) * weights[c] * factors[c];
                    double d = (means[c] - ins[c]) * weights[c] * factors[c];
                    dist += d * d;
                }
                return dist;
            } else {
                return 50e50;
            }
        }

        //--------------//
        // dumpDistance //
        //--------------//
        public double dumpDistance (double[] ins)
        {
            if (cumuls[0].getNumber() >= 2) {
                double dist = 0;
                for (int c = 0; c < inSize; c++) {
                    double dm = Math.abs(means[c] - ins[c]);
                    double wdm = weights[c] * dm;
                    double fwdm = factors[c] * wdm;
                    //dist += fwdm;
                    dist += fwdm * fwdm;
                    System.out.printf
                        ("%2d-> dm:%e wgt:%e wdm:%e fwdm:%e\n",
                         c, dm, weights[c], wdm, fwdm);
                }
                System.out.printf("Dist to %s=%e\n", shape, dist);
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
            System.out.printf("\n%30s %3d\n",
                              shape.toString(),
                              cumuls[0].getNumber());

            if (cumuls[0].getNumber() >= 2) {
                for (int c = 0; c < inSize; c++) {
                    System.out.printf
                            ("%2d %7s -> mean=% e std=% e wgt=% e\n",
                             c,
                             getLabels()[c],
                             means[c],
                             cumuls[c].getStdDeviation(),
                             weights[c]);
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        Constant.Double acceptanceGrade = new Constant.Double
                (3.0,
                 "Maximum grade for being accepted");

        Constant.Double weightMax = new Constant.Double
                (5e3,
                 "Maximum weight");

        Constants ()
        {
            initialize();
        }
    }
}
