//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   W e k a C l a s s i f i e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import omr.util.StopWatch;
import omr.util.UriUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.classifiers.Classifier;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;

/**
 * Class {@code WekaClassifier} is a shape classifier based on Weka bayesian engine.
 *
 * @author Hervé Bitteur
 */
public class WekaClassifier
        extends AbstractClassifier
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(WekaClassifier.class);

    /** The singleton. */
    private static volatile WekaClassifier INSTANCE;

    /** Bayes file name. */
    private static final String FILE_NAME = "bayesian.arff";

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying Weka classifier. */
    ///private final NaiveBayesUpdateable classifier;
    private final Classifier engine;

    private Instances structure;

    private final StopWatch watch = new StopWatch("Weka");

    private Monitor monitor;

    //~ Constructors -------------------------------------------------------------------------------
    private WekaClassifier ()
    {
        engine = (Classifier) unmarshal();

        ///dump();
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void dump ()
    {
        logger.info("{}", engine);
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance of classifier in the application.
     *
     * @return the instance
     */
    public static WekaClassifier getInstance ()
    {
        if (INSTANCE == null) {
            synchronized (WekaClassifier.class) {
                if (INSTANCE == null) {
                    INSTANCE = new WekaClassifier();
                    logger.info("Using {} as shape classifier", INSTANCE.getName());
                }
            }
        }

        return INSTANCE;
    }

    @Override
    public String getName ()
    {
        ///return engine.getClass().getSimpleName();
        return "Bayesian";
    }

    @Override
    public Evaluation[] getNaturalEvaluations (Glyph glyph,
                                               int interline)
    {
        try {
            double[] ins = ShapeDescription.features(glyph, interline);
            double[] attrs = new double[ins.length];

            for (int i = 0; i < ins.length; i++) {
                attrs[i] = ins[i];
            }

            Instance instance = new Instance(1.0, attrs);
            instance.setDataset(structure);

            double[] dist = engine.distributionForInstance(instance);

            ///logger.info("glyph#{} dist:{}", glyph.getId(), Utils.arrayToString(dist));
            Evaluation[] evals = new Evaluation[shapeCount];
            Shape[] values = Shape.values();

            for (int s = 0; s < shapeCount; s++) {
                Shape shape = values[s];
                evals[s] = new Evaluation(shape, dist[s]);
            }

            return evals;
        } catch (Exception ex) {
            logger.warn("Error in getNaturalEvaluations: " + ex, ex);

            return null;
        }
    }

    //-------//
    // train //
    //-------//
    /**
     * Produces the ARFF file.
     *
     * @param samples ignored
     * @param monitor monitoring UI
     * @param mode    ignored
     */
    @Override
    public void train (Collection<Sample> samples,
                       Monitor monitor,
                       StartingMode mode)
    {
        this.monitor = monitor;
        marshal();
    }

    @Override
    protected String getFileName ()
    {
        return FILE_NAME;
    }

    @Override
    protected boolean isCompatible (Object obj)
    {
        return true; // TODO: refine this?
    }

    //---------//
    // marshal //
    //---------//
    @Override
    protected void marshal (OutputStream os)
            throws FileNotFoundException, IOException, JAXBException
    {
        final PrintWriter out = getPrintWriter(os);

        out.println("@relation " + "samples-" + ShapeDescription.getName());
        out.println();

        for (String label : ShapeDescription.getParameterLabels()) {
            out.println("@attribute " + label + " real");
        }

        // Last attribute: shape
        out.print("@attribute shape {");

        for (Shape shape : ShapeSet.allPhysicalShapes) {
            out.print(shape);

            if (shape != Shape.LAST_PHYSICAL_SHAPE) {
                out.print(", ");
            }
        }

        out.println("}");

        out.println();
        out.println("@data");

        SampleRepository repository = SampleRepository.getInstance();
        List<Sample> samples = repository.getAllSamples();
        logger.info("Samples: {}", samples.size());

        if (monitor != null) {
            monitor.trainingStarted(0, samples.size(), 0);
        }

        int index = 0;

        for (Sample sample : samples) {
            double[] ins = ShapeDescription.features(sample, sample.getInterline());

            for (double in : ins) {
                out.print((float) in);
                out.print(",");
            }

            out.println(sample.getShape().getPhysicalShape());

            if (monitor != null) {
                monitor.epochEnded(index++, 0);
            }
        }

        out.flush();
        out.close();
        logger.info("Weka classifier data saved.");
    }

    @Override
    protected Object unmarshal (InputStream is)
            throws JAXBException, IOException
    {
        watch.start("Creation");

        ///theEngine = new MyNaiveBayesUpdateable();
        Classifier theEngine = new weka.classifiers.bayes.NaiveBayes();
        ///theEngine = new weka.classifiers.trees.J48();
        loadData(theEngine);

        if (constants.printWatch.isSet()) {
            watch.print();
        }

        return theEngine;
    }

    //----------------//
    // getInputStream //
    //----------------//
    private static InputStream getInputStream ()
    {
        // First try user file, if any (in user EVAL folder)
        try {
            Path path = WellKnowns.EVAL_FOLDER.resolve(FILE_NAME);

            if (Files.exists(path)) {
                return new FileInputStream(path.toFile());
            }
        } catch (FileNotFoundException ex) {
            logger.warn("Error getting classifier data: " + ex, ex);
        }

        // If no usable user file, use default file (in program RES folder)
        URI uri = UriUtil.toURI(WellKnowns.RES_URI, FILE_NAME);

        try {
            return uri.toURL().openStream();
        } catch (IOException ex) {
            logger.warn("Error in " + uri, ex);
        }

        return null;
    }

    //----------------//
    // getPrintWriter //
    //----------------//
    private static PrintWriter getPrintWriter (OutputStream os)
    {
        try {
            final BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(os, WellKnowns.FILE_ENCODING));

            return new PrintWriter(bw);
        } catch (Exception ex) {
            logger.warn("Error creating PrintWriter " + ex, ex);

            return null;
        }
    }

    //----------//
    // loadData //
    //----------//
    private void loadData (Classifier engine)
    {
        //        try {
        //            ArffLoader loader = new ArffLoader();
        //            loader.setFile(new File("glyphs-MIX.arff"));
        //
        //            structure = loader.getStructure();
        //            structure.setClassIndex(structure.numAttributes() - 1);
        //
        //            classifier.buildClassifier(structure);
        //
        //            Instance current;
        //
        //            while ((current = loader.getNextInstance(structure)) != null) {
        //                classifier.updateClassifier(current);
        //            }
        //        } catch (Exception ex) {
        //            logger.warn("Error loading classifier data: " + ex, ex);
        //        }
        try {
            InputStream input = getInputStream();

            if (input != null) {
                DataSource source = new DataSource(input);
                structure = source.getDataSet();
                structure.setClassIndex(structure.numAttributes() - 1);
                engine.buildClassifier(structure);
            } else {
                logger.warn("No usable classifier data");
            }
        } catch (Exception ex) {
            logger.warn("Error reading classifier data " + ex, ex);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //    //------------------------//
    //    // MyNaiveBayesUpdateable //
    //    //------------------------//
    //    private static class MyNaiveBayesUpdateable
    //        extends NaiveBayesUpdateable
    //    {
    //        //~ Methods --------------------------------------------------------------------------------
    //
    //        /**
    //         * Protected from underflow, by using logs.
    //         *
    //         * @param instance
    //         * @return
    //         * @throws Exception
    //         */
    //        @Override
    //        public double[] distributionForInstance (Instance instance)
    //            throws Exception
    //        {
    //            if (m_UseDiscretization) {
    //                m_Disc.input(instance);
    //                instance = m_Disc.output();
    //            }
    //
    //            double[] probs = new double[m_NumClasses];
    //
    //            //            for (int j = 0; j < m_NumClasses; j++) {
    //            //                probs[j] = 1000.0 + Math.log(m_ClassDistribution.getProbability(j));
    //            //            }
    //            //
    //            //            Enumeration enumAtts = instance.enumerateAttributes();
    //            //            int attIndex = 0;
    //            //
    //            //            while (enumAtts.hasMoreElements()) {
    //            //                Attribute attribute = (Attribute) enumAtts.nextElement();
    //            //
    //            //                if (!instance.isMissing(attribute)) {
    //            //                    for (int j = 0; j < m_NumClasses; j++) {
    //            //                        //                        temp = Math.max(
    //            //                        //                                1e-75,
    //            //                        //                                Math.pow(
    //            //                        //                                m_Distributions[attIndex][j].getProbability(
    //            //                        //                                        instance.value(attribute)),
    //            //                        //                                m_Instances.attribute(attIndex).weight()));
    //            //                        double delta = Math.log(
    //            //                                m_Distributions[attIndex][j].getProbability(instance.value(attribute)));
    //            //                        delta = Math.max(delta, -20);
    //            //                        probs[j] += delta;
    //            //
    //            //                        if (Double.isNaN(probs[j])) {
    //            //                            throw new Exception(
    //            //                                    "NaN returned from estimator for attribute " + attribute.name()
    //            //                                    + ":\n" + m_Distributions[attIndex][j].toString());
    //            //                        }
    //            //                    }
    //            //
    //            //                    //
    //            //                    //                    if ((max > 0) && (max < 1e-75)) { // Danger of probability underflow
    //            //                    //
    //            //                    //                        for (int j = 0; j < m_NumClasses; j++) {
    //            //                    //                            probs[j] *= 1e75;
    //            //                    //                        }
    //            //                    //                    }
    //            //                }
    //            //
    //            //                attIndex++;
    //            //            }
    //            //
    //            //            for (int j = 0; j < m_NumClasses; j++) {
    //            //                probs[j] = Math.exp(probs[j] - 500);
    //            //            }
    //            //
    //            for (int j = 0; j < m_NumClasses; j++) {
    //                probs[j] = m_ClassDistribution.getProbability(j);
    //
    //                ///logger.info("{} prior: {}", instance.dataset().classAttribute().value(j), probs[j]);
    //            }
    //
    //            Enumeration enumAtts = instance.enumerateAttributes();
    //            int         attIndex = 0;
    //
    //            while (enumAtts.hasMoreElements()) {
    //                Attribute attribute = (Attribute) enumAtts.nextElement();
    //
    //                if (!instance.isMissing(attribute)) {
    //                    double temp;
    //                    double max = 0;
    //
    //                    for (int j = 0; j < m_NumClasses; j++) {
    //                        double attValue = instance.value(attribute);
    //                        double jp = m_Distributions[attIndex][j].getProbability(attValue);
    //
    //                        if (jp == 0) {
    //                            logger.warn(
    //                                "null JP for val:{} of attr:{}-{} in class:{}",
    //                                attValue,
    //                                attIndex,
    //                                instance.dataset().attribute(attIndex).name(),
    //                                instance.dataset().classAttribute().value(j));
    //                        }
    //
    //                        temp = Math.max(
    //                            1e-75,
    //                            Math.pow(
    //                                m_Distributions[attIndex][j].getProbability(
    //                                    instance.value(attribute)),
    //                                m_Instances.attribute(attIndex).weight()));
    //                        probs[j] *= temp;
    //
    //                        if (probs[j] > max) {
    //                            max = probs[j];
    //                        }
    //
    //                        if (Double.isNaN(probs[j])) {
    //                            throw new Exception(
    //                                "NaN returned from estimator for attribute " + attribute.name() +
    //                                ":\n" + m_Distributions[attIndex][j].toString());
    //                        }
    //                    }
    //
    //                    if ((max > 0) && (max < 1e-75)) { // Danger of probability underflow
    //
    //                        for (int j = 0; j < m_NumClasses; j++) {
    //                            probs[j] *= 1e75;
    //                        }
    //                    }
    //                }
    //
    //                attIndex++;
    //            }
    //
    //            // Display probabilities
    //            Utils.normalize(probs);
    //
    //            return probs;
    //        }
    //    }
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");
    }
}
