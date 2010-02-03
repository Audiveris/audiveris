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

import omr.glyph.facets.Glyph;
import omr.glyph.ui.GlyphRepository;

import omr.log.Logger;

import omr.math.LinearEvaluator;
import omr.math.LinearEvaluator.Sample;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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

    //-------------------//
    // getAllEvaluations //
    //-------------------//
    @Override
    public Evaluation[] getAllEvaluations (Glyph glyph)
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
                shape = GlyphChecks.specificCheck(shape, glyph);

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
}
