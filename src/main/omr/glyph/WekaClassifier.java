//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   W e k a C l a s s i f i e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.util.StopWatch;
import omr.util.UriUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.classifiers.Classifier;

import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;

import javax.xml.bind.JAXBException;

/**
 * Class {@code WekaClassifier} is a shape classifier based on Weka software.
 *
 * @author Hervé Bitteur
 */
public class WekaClassifier
        extends AbstractEvaluationEngine
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(WekaClassifier.class);

    /** The singleton. */
    private static volatile WekaClassifier INSTANCE;

    /** Samples file name. */
    private static final String FILE_NAME = "samples-MIX.arff";

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying Weka classifier. */
    ///private final NaiveBayesUpdateable classifier;
    private final Classifier classifier;

    private Instances structure;

    private final StopWatch watch = new StopWatch("Weka");

    //~ Constructors -------------------------------------------------------------------------------
    private WekaClassifier ()
    {
        watch.start(("Creation"));
        ///classifier = new MyNaiveBayesUpdateable();
        classifier = new weka.classifiers.bayes.NaiveBayes();
        ///classifier = new weka.classifiers.trees.J48();
        loadData();

        if (constants.printWatch.isSet()) {
            watch.print();
        }

        ///dump();
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void dump ()
    {
        logger.info("{}", classifier);
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
        return classifier.getClass().getSimpleName();
    }

    @Override
    public Evaluation[] getNaturalEvaluations (Glyph glyph)
    {
        try {
            double[] ins = ShapeDescription.features(glyph);
            double[] attrs = new double[ins.length];

            for (int i = 0; i < ins.length; i++) {
                attrs[i] = ins[i];
            }

            Instance instance = new Instance(1.0, attrs);
            instance.setDataset(structure);

            double[] dist = classifier.distributionForInstance(instance);

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

    @Override
    public void train (Collection<Glyph> base,
                       Monitor monitor,
                       StartingMode mode)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected String getFileName ()
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected boolean isCompatible (Object obj)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void marshal (OutputStream os)
            throws FileNotFoundException, IOException, JAXBException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected Object unmarshal (InputStream is)
            throws JAXBException, IOException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //----------------//
    // getInputStream //
    //----------------//
    private InputStream getInputStream ()
    {
        // First try user file, if any (in user EVAL folder)
        try {
            File file = new File(WellKnowns.EVAL_FOLDER, FILE_NAME);

            if (file.exists()) {
                return new FileInputStream(file);
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

    //----------//
    // loadData //
    //----------//
    private void loadData ()
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
                classifier.buildClassifier(structure);
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

        Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");
    }
}
