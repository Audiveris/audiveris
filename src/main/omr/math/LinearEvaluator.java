//----------------------------------------------------------------------------//
//                                                                            //
//                       L i n e a r E v a l u a t o r                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.math;

import omr.log.Logger;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>LinearEvaluator</code> is an evaluator using linear regression.
 *
 * <p>It provides a distance between 2 "patterns". A pattern is a  vector of
 * parameter values in the input domain.
 *
 * <p>It provides a distance between a "pattern" from the input domain to a
 * "category" in the output range, thus allowing to map patterns to categories.
 * This feature can be used for example to map a given Glyph (through the
 * pattern of its measured moments values) to the best fitting Shape category.
 *
 * <p>This evaluator can be trained, by feeding it with sample patterns for each
 * defined category.
 *
 * <p>The evaluator data can be marshalled to and unmarshalled from an XML
 * formatted stream.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "linear-evaluator")
public class LinearEvaluator
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        LinearEvaluator.class);

    /** Un/marshalling context for use with JAXB */
    private static JAXBContext jaxbContext;

    /** To avoid infinity */
    public static double INFINITE_DISTANCE = 50e50;

    /** To indicate no weight value */
    public static final double NO_WEIGHT_VALUE = -1;

    //~ Instance fields --------------------------------------------------------

    /** Size of output range (the number of defined categories) */
    @XmlAttribute(name = "output-size")
    private final int outputSize;

    /** Size of input domain (the number of variables in each pattern) */
    @XmlAttribute(name = "input-size")
    private final int inputSize;

    /** The name of each input parameter (array of inputSize strings) */
    private final String[] inputNames;

    /** A descriptor for each output category */
    @XmlJavaTypeAdapter(CategoryMapAdapter.class)
    @XmlElement(name = "categories")
    private final SortedMap<String, CategoryDesc> categoryDescs;

    /** Category-independant param weights (array of inputSize weights) */
    @XmlElementWrapper(name = "global-weights")
    @XmlElement(name = "weight")
    private final double[] paramWeights;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // LinearEvaluator //
    //-----------------//
    /**
     * Creates a new LinearEvaluator object.
     * @param inputSize the count of parameters in the input domain
     * @param inputNames the parameter names
     * @param outputSize the count of categories in the output range
     */
    public LinearEvaluator (int      inputSize,
                            String[] inputNames,
                            int      outputSize)
    {
        this.inputSize = inputSize;
        this.inputNames = inputNames;
        this.outputSize = outputSize;
        categoryDescs = new TreeMap<String, CategoryDesc>();
        paramWeights = new double[inputSize];
    }

    //-----------------//
    // LinearEvaluator //
    //-----------------//
    /** Private no-arg constructor meant for the JAXB compiler only */
    private LinearEvaluator ()
    {
        inputSize = -1;
        inputNames = null;
        outputSize = -1;
        categoryDescs = null;
        paramWeights = null;
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getInputSize //
    //--------------//
    /**
     * Report the number of parameters in the input patterns
     *
     * @return the count of pattern parameters
     */
    public int getInputSize ()
    {
        return inputSize;
    }

    //---------------//
    // getOutputSize //
    //---------------//
    /**
     * Report the count of categories
     * @return the count of categories
     */
    public int getOutputSize ()
    {
        return outputSize;
    }

    //------------------//
    // categoryDistance //
    //------------------//
    /**
     * Measure the "distance" information between a given pattern and (the mean
     * pattern of) a category
     *
     * @param pattern the value for each parameter of the pattern to evaluate
     * @param category the category id to measure distance from
     * @return the measured distance
     */
    public double categoryDistance (double[] pattern,
                                    String   category)
    {
        // Check sizes
        if ((pattern == null) || (pattern.length != inputSize)) {
            throw new IllegalArgumentException(
                "Pattern is null or inconsistent with the LinearEvaluator");
        }

        // Check category label
        CategoryDesc desc = categoryDescs.get(category);

        if (desc == null) {
            throw new IllegalArgumentException("Unknown category: " + category);
        }

        return desc.distance(pattern, paramWeights);
    }

    //------//
    // dump //
    //------//
    public void dump ()
    {
        System.out.println();
        System.out.println("LinearEvaluator");
        System.out.println("===============");
        System.out.println();

        // Input
        System.out.print("Inputs  : " + inputSize);
        System.out.println(" parameters\n");

        // Output
        System.out.print("Outputs : " + outputSize);
        System.out.println(" categories\n");

        // Known output
        System.out.println("Known   : " + categoryDescs.size());

        for (CategoryDesc desc : categoryDescs.values()) {
            desc.dump();
        }
    }

    //--------------//
    // dumpDistance //
    //--------------//
    /**
     * Print out the "distance" information between a given pattern and a
     * category. It's a sort of debug information.
     *
     * @param pattern the pattern at hand
     * @param category the category to measure distance from
     */
    public void dumpDistance (double[] pattern,
                              String   category)
    {
        categoryDescs.get(category)
                     .dumpDistance(pattern, paramWeights);
    }

    //---------//
    // marshal //
    //---------//
    /**
     * Marshal the LinearEvaluator to its XML file
     * @param os the XML output stream, which is not closed by this method
     * @exception JAXBException raised when marshalling goes wrong
     */
    public void marshal (OutputStream os)
        throws JAXBException
    {
        Marshaller m = getJaxbContext()
                           .createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(this, os);
        logger.fine("LinearEvaluator marshalled");
    }

    //-----------------//
    // patternDistance //
    //-----------------//
    /**
     * Measure the "distance" information between two patterns
     *
     * @param one the first pattern
     * @param two the second pattern
     * @return the measured distance between them
     */
    public double patternDistance (double[] one,
                                   double[] two)
    {
        // Check sizes
        if ((one == null) ||
            (one.length != inputSize) ||
            (two == null) ||
            (two.length != inputSize)) {
            throw new IllegalArgumentException(
                "Patterns are null or inconsistent with the LinearEvaluator");
        }

        double dist = 0;

        for (int p = 0; p < inputSize; p++) {
            double dif = one[p] - two[p];
            dist += (dif * dif * paramWeights[p]);
        }

        return dist / inputSize;
    }

    //-------//
    // train //
    //-------//
    /**
     * Launch the training of the evaluator
     *
     * @param samples a collection of samples (category + pattern)
     */
    public void train (Collection<Sample> samples)
    {
        // Check size consistencies.
        if ((samples == null) || samples.isEmpty()) {
            throw new IllegalArgumentException(
                "samples collection is null or empty");
        }

        // Reset counters
        for (CategoryDesc desc : categoryDescs.values()) {
            for (ParamDesc param : desc.params) {
                param.reset();
            }
        }

        // Accumulate
        for (Sample sample : samples) {
            CategoryDesc desc = categoryDescs.get(sample.category);

            if (desc == null) {
                desc = new CategoryDesc(sample.category, inputSize, inputNames);
                categoryDescs.put(sample.category, desc);
            }

            desc.include(sample.pattern);
            logger.info("Accu " + desc.getId() + " count:" + desc.getCardinality());
        }

        // Determine means & weights
        for (CategoryDesc desc : categoryDescs.values()) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Computing " + desc.getId() + " count:" +
                    desc.getCardinality());
            }

            desc.compute();
        }

        // Compute global weights (average of shape-dependant weights)
        for (int p = 0; p < inputSize; p++) {
            int    weightCount = 0;
            double globalWeight = 0;

            for (CategoryDesc desc : categoryDescs.values()) {
                double weight = desc.params[p].weight;

                if (weight != NO_WEIGHT_VALUE) {
                    weightCount++;
                    globalWeight += weight;
                }
            }

            paramWeights[p] = globalWeight / weightCount;
        }
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the provided XML stream to allocate the corresponding
     * LinearEvaluator
     *
     * @param in the input stream that contains the evaluator definition in XML
     * format. The stream is not closed by this method
     *
     * @return the allocated network.
     * @exception JAXBException raised when unmarshalling goes wrong
     */
    public static LinearEvaluator unmarshal (InputStream in)
        throws JAXBException
    {
        Unmarshaller    um = getJaxbContext()
                                 .createUnmarshaller();
        LinearEvaluator evaluator = (LinearEvaluator) um.unmarshal(in);

        if (logger.isFineEnabled()) {
            logger.fine("LinearEvaluator unmarshalled");
        }

        return evaluator;
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static JAXBContext getJaxbContext ()
        throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            jaxbContext = JAXBContext.newInstance(LinearEvaluator.class);
        }

        return jaxbContext;
    }

    //~ Inner Classes ----------------------------------------------------------

    //--------//
    // Sample //
    //--------//
    /**
     * Meant to host one sample for training, representing pattern values for a
     * given category
     */
    public static class Sample
    {
        //~ Instance fields ----------------------------------------------------

        public final String   category;
        public final double[] pattern;

        //~ Constructors -------------------------------------------------------

        public Sample (String   category,
                       double[] pattern)
        {
            this.category = category;
            this.pattern = pattern;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{");
            sb.append(getClass().getSimpleName());
            sb.append(" ")
              .append(category);
            sb.append(" ")
              .append(Arrays.toString(pattern));
            sb.append("}");

            return sb.toString();
        }
    }

    //--------------//
    // CategoryDesc //
    //--------------//
    /**
     * Meant to encapsulate the regression data for one category
     */
    private static class CategoryDesc
    {
        //~ Instance fields ----------------------------------------------------

        /** Number of values in a pattern */
        @XmlElement(name = "input-size")
        private  int inputSize;

        /** Category id */
        @XmlElement(name = "id")
        private final String id;

        /** The specific descriptor for each parameter */
        @XmlElementWrapper(name = "parameters")
        @XmlElement(name = "parameter")
        final ParamDesc[] params;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new CategoryDesc object.
         *
         * @param id the category id
         * @param inputSize the number of parameters
         */
        public CategoryDesc (String   id,
                             int      inputSize,
                             String[] inputNames)
        {
            logger.info("Creating CategoryDesc id:" + id);
            this.id = id;
            this.inputSize = inputSize;
            params = new ParamDesc[this.inputSize];

            for (int p = 0; p < this.inputSize; p++) {
                params[p] = new ParamDesc(p, inputNames[p]);
            }
        }

        /**
         * Meant to please JAXB
         */
        private CategoryDesc ()
        {
            ///inputSize = -1;
            id = null;
            params = null;
        }

        //~ Methods ------------------------------------------------------------

        public int getCardinality ()
        {
            return params[0].sample.getCardinality();
        }

        /**
         * @return the id
         */
        public String getId ()
        {
            return id;
        }

        public void compute ()
        {
            if (getCardinality() > 0) {
                for (ParamDesc param : params) {
                    try {
                        param.compute();
                    } catch (Exception ex) {
                        logger.fine(
                            "Category#" + id +
                            " cannot compute parameters ex:" + ex);
                    }
                }
            } else {
                logger.warning("Category#" + id + " has no sample");
            }
        }

        public double distance (double[] pattern,
                                double[] paramWeights)
        {
            double dist = 0;

            for (int p = 0; p < inputSize; p++) {
                dist += params[p].weightedDelta(pattern[p], paramWeights[p]);
            }

            dist /= inputSize;

            return dist;
        }

        public void dump ()
        {
            System.out.println(
                "\ncategory:" + id + " cardinality:" + getCardinality());

            for (ParamDesc param : params) {
                param.dump();
            }
        }

        public double dumpDistance (double[] pattern,
                                    double[] paramWeights)
        {
            if ((pattern == null) || (pattern.length != inputSize)) {
                throw new IllegalArgumentException(
                    "dumpDistance. Pattern array is null or non compatible in length ");
            }

            if (getCardinality() >= 2) {
                double dist = 0;

                for (int p = 0; p < inputSize; p++) {
                    ParamDesc param = params[p];
                    double    wDelta = param.weightedDelta(
                        pattern[p],
                        paramWeights[p]);
                    dist += wDelta;
                    System.out.printf(
                        "%2d-> weight:%e wDelta:%e\n",
                        p,
                        param.weight,
                        wDelta);
                }

                dist /= inputSize;
                System.out.println("Dist to cat " + id + " = " + dist);

                return dist;
            } else {
                return INFINITE_DISTANCE;
            }
        }

        public void include (double[] pattern)
        {
            for (int p = 0; p < inputSize; p++) {
                params[p].includeValue(pattern[p]);
            }
        }
    }

    //--------------------//
    // CategoryMapAdapter //
    //--------------------//
    /**
     * Meant for JAXB support of a map
     */
    private static class CategoryMapAdapter
        extends XmlAdapter<CategoryDesc[], Map<String, CategoryDesc>>
    {
        //~ Constructors -------------------------------------------------------

        /**
         * Meant to please JAXB
         */
        public CategoryMapAdapter ()
        {
        }

        //~ Methods ------------------------------------------------------------

        //-----------//
        // unmarshal //
        //-----------//
        @Override
        public CategoryDesc[] marshal (Map<String, CategoryDesc> map)
            throws Exception
        {
            return map.values()
                      .toArray(new CategoryDesc[map.size()]);
        }

        //-----------//
        // unmarshal //
        //-----------//
        @Override
        public Map<String, CategoryDesc> unmarshal (CategoryDesc[] categories)
        {
            SortedMap<String, CategoryDesc> map = new TreeMap<String, CategoryDesc>();

            for (CategoryDesc category : categories) {
                map.put(category.getId().toString(), category);
            }

            return map;
        }
    }

    //-----------//
    // ParamDesc //
    //-----------//
    /**
     * Meant to encapsulate the regression data for one parameter
     */
    private static class ParamDesc
    {
        //~ Instance fields ----------------------------------------------------

        /** Sample to compute mean value & std deviation */
        Population sample = new Population();

        /** Mean value for this parameter */
        @XmlAttribute(name = "mean")
        double mean;

        /** Weight for this parameter */
        @XmlAttribute(name = "weight")
        double weight = NO_WEIGHT_VALUE;

        /** Name used for this parameter */
        @XmlAttribute(name = "name")
        String name;

        /** Parameter index in input pattern */
        @XmlAttribute(name = "id")
        int id;

        //~ Constructors -------------------------------------------------------

        public ParamDesc (int    id,
                          String name)
        {
            this.id = id;
            this.name = name;
        }

        /**
         * Meant to please JAXB
         */
        public ParamDesc ()
        {
        }

        //~ Methods ------------------------------------------------------------

        /** Compute the param characteristics out of its data sample */
        public void compute ()
        {
            mean = sample.getMeanValue();

            if (sample.getCardinality() > 1) {
                double var = sample.getVariance();
                weight = 1 / var;
            }
        }

        public void dump ()
        {
            StringBuilder sb = new StringBuilder();
            sb.append("id=")
              .append(id);
            sb.append(" name=")
              .append(name);
            sb.append(" mean=")
              .append(mean);
            sb.append(" weight=");

            if (weight == NO_WEIGHT_VALUE) {
                sb.append("NO_WEIGHT_VALUE");
            } else {
                sb.append(weight);
            }

            if ((sample != null) & (sample.getCardinality() >= 2)) {
                sb.append(" var=")
                  .append(sample.getVariance());
            }

            System.out.println(sb);
        }

        public void includeValue (double val)
        {
            sample.includeValue(val);
        }

        public void reset ()
        {
            sample.reset();
            mean = 0;
            weight = NO_WEIGHT_VALUE;
        }

        /** Report the weighted square delta of a value vs param mean value */
        public double weightedDelta (double val,
                                     double stdWeight)
        {
            double dif = mean - val;

            if (weight != NO_WEIGHT_VALUE) {
                return dif * dif * weight;
            } else {
                return dif * dif * stdWeight;
            }
        }
    }
}
