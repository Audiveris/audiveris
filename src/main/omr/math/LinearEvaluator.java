//----------------------------------------------------------------------------//
//                                                                            //
//                       L i n e a r E v a l u a t o r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code LinearEvaluator} is an evaluator using linear regression.
 *
 * <p>It provides a distance between 2 "patterns". A pattern is a vector of
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
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "linear-evaluator")
public class LinearEvaluator
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            LinearEvaluator.class);

    /** Un/marshalling context for use with JAXB */
    private static volatile JAXBContext jaxbContext;

    /** To avoid infinity */
    public static final double INFINITE_DISTANCE = 50e50;

    /** To detect a near-zero value in a double */
    private static final double EPSILON = 1E-10;

    //~ Instance fields --------------------------------------------------------
    /** A descriptor for each input parameter. */
    @XmlElementWrapper(name = "defaults")
    @XmlElement(name = "parameter")
    private final Parameter[] parameters;

    /** A descriptor for each output category. */
    @XmlJavaTypeAdapter(CategoryMapAdapter.class)
    @XmlElement(name = "categories")
    private final SortedMap<String, Category> categories;

    /**
     * Flag to indicate that some data has changed since unmarshalling
     * and that engine internals must be marshalled to disk before
     * exiting.
     */
    private boolean dataModified = false;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // LinearEvaluator //
    //-----------------//
    /**
     * Creates a new LinearEvaluator object.
     *
     * @param inputNames the parameter names
     */
    public LinearEvaluator (String[] inputNames)
    {
        categories = new TreeMap<>();
        parameters = new Parameter[inputNames.length];

        for (int i = 0; i < inputNames.length; i++) {
            parameters[i] = new Parameter(inputNames[i]);
        }
    }

    //-----------------//
    // LinearEvaluator //
    //-----------------//
    /** Private no-arg constructor meant for the JAXB compiler only */
    private LinearEvaluator ()
    {
        categories = null;
        parameters = null;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-------------------//
    // getParameterNames //
    //-------------------//
    /**
     * Report the sequence of parameter names.
     *
     * @return the sequence of parameter names
     */
    public String[] getParameterNames ()
    {
        if (parameters == null) {
            return new String[0];
        } else {
            String[] names = new String[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                names[i] = parameters[i].name;
            }

            return names;
        }
    }

    //------------------//
    // getCategoryNames //
    //------------------//
    /**
     * Report the collection of category names (order is irrelevant).
     *
     * @return the collection of category names
     */
    public String[] getCategoryNames ()
    {
        if (categories == null) {
            return new String[0];
        } else {
            Collection<Category> values = categories.values();
            String[] names = new String[values.size()];

            int index = 0;
            for (Category cat : values) {
                names[index++] = cat.getId();
            }

            return names;
        }
    }

    //--------------//
    // getInputSize //
    //--------------//
    /**
     * Report the number of parameters in the input patterns.
     *
     * @return the count of pattern parameters
     */
    public final int getInputSize ()
    {
        return parameters.length;
    }

    //------------------//
    // categoryDistance //
    //------------------//
    /**
     * Measure the "distance" information between a given pattern and
     * (the mean pattern of) a category.
     *
     * @param pattern    the value for each parameter of the pattern to evaluate
     * @param categoryId the category id to measure distance from
     * @return the measured distance
     */
    public double categoryDistance (double[] pattern,
                                    String categoryId)
    {
        return checkArguments(pattern, categoryId).distance(pattern, parameters);
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

        // Input size
        System.out.println("Inputs  : " + getInputSize() + " parameters");

        // Output size
        System.out.println(
                "Outputs : " + categories.keySet().size() + " categories");

        // Description of each category
        for (Category category : categories.values()) {
            category.dump();
        }
    }

    //--------------//
    // dumpDistance //
    //--------------//
    /**
     * Print out the "distance" information between a given pattern and
     * a category.
     * It's a sort of debug information.
     *
     * @param pattern  the pattern at hand
     * @param category the category to measure distance from
     */
    public void dumpDistance (double[] pattern,
                              String category)
    {
        categories.get(category).dumpDistance(pattern, parameters);
    }

    //------------//
    // getMaximum //
    //------------//
    /**
     * Get the constraint test on maximum for a parameter of the
     * provided category.
     *
     * @param paramIndex the impacted parameter
     * @param categoryId the targeted category
     * @return the current maximum value (null if test is disabled)
     */
    public Double getMaximum (int paramIndex,
                              String categoryId)
    {
        return getCategoryParam(paramIndex, categoryId).max;
    }

    //------------//
    // getMinimum //
    //------------//
    /**
     * Get the constraint test on minimum for a parameter of the
     * provided category.
     *
     * @param paramIndex the impacted parameter
     * @param categoryId the targeted category
     * @return the current minimum value (null if test is disabled)
     */
    public Double getMinimum (int paramIndex,
                              String categoryId)
    {
        return getCategoryParam(paramIndex, categoryId).min;
    }

    //---------------//
    // includeSample //
    //---------------//
    /**
     * Include a new sample (on top of unmarshalled data).
     * We use this to widen the min/max constraints, and also to increase
     * the population and thus the categories training status.
     *
     * @param params     the parameters
     * @param categoryId the targeted category
     * @return true if some min/max bound has changed
     */
    public boolean includeSample (double[] params,
                                  String categoryId)
    {
        // Check category label
        Category category = categories.get(categoryId);

        if (category == null) {
            throw new IllegalArgumentException(
                    "Unknown category: " + categoryId);
        }

        boolean extended = category.include(params);

        // Update categories parameters accordingly
        computeCategoriesParams();

        dataModified = true;

        return extended;
    }

    //----------------//
    // isDataModified //
    //----------------//
    /**
     * @return true if some data has been modified since unmarshalling
     */
    public boolean isDataModified ()
    {
        return dataModified;
    }

    //---------//
    // marshal //
    //---------//
    /**
     * Marshal the LinearEvaluator to its XML file.
     *
     * @param os the XML output stream, which is not closed by this method
     * @exception JAXBException raised when marshalling goes wrong
     */
    public void marshal (OutputStream os)
            throws JAXBException
    {
        Marshaller m = getJaxbContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(this, os);
        logger.debug("LinearEvaluator marshalled");
    }

    //-----------------//
    // patternDistance //
    //-----------------//
    /**
     * Measure the "distance" information between two patterns.
     *
     * @param one the first pattern
     * @param two the second pattern
     * @return the measured distance between them
     */
    public double patternDistance (double[] one,
                                   double[] two)
    {
        final int inputSize = getInputSize();

        // Check sizes
        if ((one == null)
            || (one.length != inputSize)
            || (two == null)
            || (two.length != inputSize)) {
            throw new IllegalArgumentException(
                    "Patterns are null or inconsistent with the LinearEvaluator");
        }

        double dist = 0;

        for (int p = 0; p < inputSize; p++) {
            double dif = one[p] - two[p];
            dist += (dif * dif * parameters[p].defaultWeight);
        }

        return dist / inputSize;
    }

    //-------//
    // train //
    //-------//
    /**
     * Perform the training of the evaluator.
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

        // Reset counters for each category, if needed
        for (Category category : categories.values()) {
            for (CategoryParam param : category.params) {
                param.reset();
            }
        }

        // Accumulate data from samples into categories descriptors
        for (Sample sample : samples) {
            Category category = categories.get(sample.category);

            if (category == null) {
                category = new Category(sample.category, parameters);
                categories.put(sample.category, category);
            }

            category.include(sample.pattern);
            logger.debug("Accu {} count:{}",
                    category.getId(), category.getCardinality());
        }

        computeCategoriesParams();
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal the provided XML stream to allocate the corresponding
     * LinearEvaluator.
     *
     * @param in the input stream that contains the evaluator definition in XML
     *           format. The stream is not closed by this method
     * @return the allocated network.
     * @exception JAXBException raised when unmarshalling goes wrong
     */
    public static LinearEvaluator unmarshal (InputStream in)
            throws JAXBException
    {
        Unmarshaller um = getJaxbContext().createUnmarshaller();
        LinearEvaluator evaluator = (LinearEvaluator) um.unmarshal(in);
        logger.debug("LinearEvaluator unmarshalled");

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

    //----------------//
    // checkArguments //
    //----------------//
    private Category checkArguments (double[] pattern,
                                     String categoryId)
    {
        // Check sizes
        if ((pattern == null) || (pattern.length != getInputSize())) {
            throw new IllegalArgumentException(
                    "Pattern is null or inconsistent with the LinearEvaluator");
        }

        // Check category label
        Category category = categories.get(categoryId);

        if (category == null) {
            throw new IllegalArgumentException(
                    "Unknown category: " + categoryId);
        }

        return category;
    }

    //-------------------------//
    // computeCategoriesParams //
    //-------------------------//
    private void computeCategoriesParams ()
    {
        // Compute parameters means & weights for each category
        for (Category category : categories.values()) {
            logger.debug("Computing {} count:{}",
                    category.getId(), category.getCardinality());
            category.compute();
        }

        // Compute default weight for each parameter
        // (using the sample populations of all categories)
        for (int p = 0; p < parameters.length; p++) {
            Population paramPop = new Population();

            for (Category category : categories.values()) {
                CategoryParam param = category.params[p];

                if (param.training != CategoryParam.TrainingStatus.NONE) {
                    paramPop.includePopulation(param.population);
                }
            }

            if (paramPop.getCardinality() > 1) {
                double var = paramPop.getVariance();

                if (var >= EPSILON) {
                    parameters[p].defaultWeight = 1 / var;
                }
            }
        }
    }

    //------------------//
    // getCategoryParam //
    //------------------//
    private CategoryParam getCategoryParam (int paramIndex,
                                            String categoryId)
    {
        // Check category label
        Category category = categories.get(categoryId);

        if (category == null) {
            throw new IllegalArgumentException(
                    "Unknown category: " + categoryId);
        }

        return category.params[paramIndex];
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------//
    // Sample //
    //--------//
    /**
     * Meant to host one sample for training, representing pattern
     * values for a given category.
     */
    public static class Sample
    {
        //~ Instance fields ----------------------------------------------------

        /** The known category */
        public final String category;

        /** The observed pattern */
        public final double[] pattern;

        //~ Constructors -------------------------------------------------------
        public Sample (String category,
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
            sb.append(" ").append(category);
            sb.append(" ").append(Arrays.toString(pattern));
            sb.append("}");

            return sb.toString();
        }
    }

    //---------//
    // Printer //
    //---------//
    /**
     * Printouts meant for analysis of behavior of LinearEvaluator.
     */
    public class Printer
    {
        //~ Instance fields ----------------------------------------------------

        // Format strings
        private final String sf; // For String

        private final String df; // For double

        //~ Constructors -------------------------------------------------------
        public Printer (int width)
        {
            sf = "%" + width + "s";
            df = "%" + width + "f";
        }

        //~ Methods ------------------------------------------------------------
        public String getDashes ()
        {
            StringBuilder sb = new StringBuilder();

            for (int p = 0; p < parameters.length; p++) {
                sb.append(String.format(sf, "----------"));
            }

            return sb.toString();
        }

        public String getDefaults ()
        {
            StringBuilder sb = new StringBuilder();

            for (Parameter param : parameters) {
                sb.append(String.format(df, param.defaultWeight));
            }

            return sb.toString();
        }

        public String getDeltas (double[] one,
                                 double[] two)
        {
            StringBuilder sb = new StringBuilder();

            for (int p = 0; p < parameters.length; p++) {
                double dif = one[p] - two[p];
                sb.append(String.format(df, dif * dif));
            }

            return sb.toString();
        }

        public String getNames ()
        {
            StringBuilder sb = new StringBuilder();

            for (Parameter param : parameters) {
                sb.append(String.format(sf, param.name));
            }

            return sb.toString();
        }

        public String getWeightedDeltas (double[] one,
                                         double[] two)
        {
            StringBuilder sb = new StringBuilder();

            for (int p = 0; p < parameters.length; p++) {
                double dif = one[p] - two[p];
                sb.append(
                        String.format(df,
                        dif * dif * parameters[p].defaultWeight));
            }

            return sb.toString();
        }
    }

    //----------//
    // Category //
    //----------//
    /**
     * Meant to encapsulate the regression data for one category.
     */
    private static class Category
    {
        //~ Instance fields ----------------------------------------------------

        /** Category id */
        @XmlAttribute(name = "id")
        private final String id;

        /** A specific descriptor for each parameter */
        @XmlElement(name = "parameter")
        final CategoryParam[] params;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Category object.
         *
         * @param id         the category id
         * @param parameters the sequence of parameter descriptors
         */
        public Category (String id,
                         Parameter[] parameters)
        {
            this.id = id;
            params = new CategoryParam[parameters.length];

            for (int p = 0; p < params.length; p++) {
                params[p] = new CategoryParam(parameters[p]);
            }
        }

        /**
         * Meant to please JAXB
         */
        private Category ()
        {
            id = null;
            params = null;
        }

        //~ Methods ------------------------------------------------------------
        public void compute ()
        {
            if (getCardinality() > 0) {
                for (CategoryParam param : params) {
                    try {
                        param.compute();
                    } catch (Exception ex) {
                        logger.warn(
                                "Category {} cannot compute parameters ex:{}",
                                id, ex);
                    }
                }
            } else {
                logger.warn("Category {} has no sample", id);
            }
        }

        public synchronized double distance (double[] pattern,
                                             Parameter[] parameters)
        {
            double dist = 0;

            for (int p = 0; p < params.length; p++) {
                dist += params[p].weightedDelta(
                        pattern[p],
                        parameters[p].defaultWeight);
            }

            dist /= params.length;

            return dist;
        }

        public synchronized void dump ()
        {
            System.out.println(
                    "\ncategory:" + id + " cardinality:" + getCardinality());

            for (CategoryParam param : params) {
                param.dump();
            }
        }

        public synchronized double dumpDistance (double[] pattern,
                                                 Parameter[] parameters)
        {
            if ((pattern == null) || (pattern.length != params.length)) {
                throw new IllegalArgumentException(
                        "dumpDistance."
                        + " Pattern array is null or non compatible in length ");
            }

            if (getCardinality() >= 2) {
                double dist = 0;

                for (int p = 0; p < params.length; p++) {
                    CategoryParam param = params[p];
                    double wDelta = param.weightedDelta(
                            pattern[p],
                            parameters[p].defaultWeight);
                    dist += wDelta;
                    System.out.printf(
                            "%2d-> weight:%e wDelta:%e\n",
                            p,
                            param.weight,
                            wDelta);
                }

                dist /= params.length;
                System.out.println("Dist to cat " + id + " = " + dist);

                return dist;
            } else {
                return INFINITE_DISTANCE;
            }
        }

        public int getCardinality ()
        {
            return params[0].population.getCardinality();
        }

        /**
         * @return the id
         */
        public String getId ()
        {
            return id;
        }

        /** Include data from the provided pattern into category descriptor */
        public synchronized boolean include (double[] pattern)
        {
            boolean extended = false;

            if ((pattern == null) || (pattern.length != params.length)) {
                throw new IllegalArgumentException(
                        "include."
                        + " Pattern array is null or non compatible in length ");
            }

            for (int p = 0; p < params.length; p++) {
                if (params[p].includeValue(pattern[p])) {
                    extended = true;
                }
            }

            return extended;
        }
    }

    //--------------------//
    // CategoryMapAdapter //
    //--------------------//
    /**
     * Meant for JAXB support of a map.
     */
    private static class CategoryMapAdapter
            extends XmlAdapter<Category[], Map<String, Category>>
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
        public Category[] marshal (Map<String, Category> map)
                throws Exception
        {
            return map.values().toArray(new Category[map.size()]);
        }

        //-----------//
        // unmarshal //
        //-----------//
        @Override
        public Map<String, Category> unmarshal (Category[] categories)
        {
            SortedMap<String, Category> map = new TreeMap<>();

            for (Category category : categories) {
                map.put(category.getId(), category);
            }

            return map;
        }
    }

    //---------------//
    // CategoryParam //
    //---------------//
    /**
     * Meant to encapsulate the regression data for one parameter in
     * the context of a category.
     */
    private static class CategoryParam
    {
        //~ Static fields/initializers -----------------------------------------

        /** Used instead of infinitive weight, when variance is zero */
        private static final double HIGH_WEIGHT_FACTOR = 10;

        //~ Enumerations -------------------------------------------------------
        /** Description of the training done so far on a parameter */
        public static enum TrainingStatus
        {
            //~ Enumeration constant initializers ------------------------------

            /**
             * Not trained
             * => no mean value, no weight
             */
            NONE,
            /**
             * Just one data element
             * => a mean value, but artificial (average) weight
             */
            SINGLE_DATA,
            /**
             * Several data elements, but with identical values
             * => a mean value, but infinite weight
             */
            IDENTICAL_VALUES,
            /**
             * Several data elements, with some variation in the values
             * => a mean value and weight computed as 1/variance
             */
            NOMINAL;

        }

        //~ Instance fields ----------------------------------------------------
        /** Population to compute mean value & std deviation */
        @XmlElement(name = "population")
        private Population population;

        /** Maximum value for this parameter */
        @XmlAttribute(name = "max")
        private Double max = null;

        /** Mean value for this parameter */
        @XmlAttribute(name = "mean")
        private double mean;

        /** Minimum value for this parameter */
        @XmlAttribute(name = "min")
        private Double min = null;

        /** Weight for this parameter */
        @XmlAttribute(name = "weight")
        private double weight;

        /** Training status */
        @XmlAttribute(name = "training")
        private TrainingStatus training = TrainingStatus.NONE;

        /** Related parameter descriptor */
        @XmlIDREF
        @XmlAttribute(name = "name")
        private Parameter parameter;

        //~ Constructors -------------------------------------------------------
        public CategoryParam (Parameter parameter)
        {
            this.parameter = parameter;
            population = new Population();
        }

        /**
         * Meant to please JAXB
         */
        public CategoryParam ()
        {
        }

        //~ Methods ------------------------------------------------------------
        /** Compute the param characteristics out of its data sample */
        public void compute ()
        {
            int count = population.getCardinality();

            if (count > 0) {
                mean = population.getMeanValue();
            }

            if (count == 1) {
                training = TrainingStatus.SINGLE_DATA;
            } else {
                double var = population.getVariance();

                if (var < EPSILON) {
                    training = TrainingStatus.IDENTICAL_VALUES;
                } else {
                    training = TrainingStatus.NOMINAL;
                    weight = 1 / var;
                }
            }
        }

        public void dump ()
        {
            StringBuilder sb = new StringBuilder();

            sb.append(" ").append(parameter);

            sb.append(" training=").append(training);

            sb.append(" min=").append(min);

            sb.append(" mean=").append(mean);

            sb.append(" max=").append(max);

            sb.append(" weight=").append(weight);

            if (population.getCardinality() > 1) {
                sb.append(" var=").append(population.getVariance());
            }

            System.out.println(sb);
        }

        /**
         * Include a new value for this category parameter.
         *
         * @param val the new value
         * @return true if any of the min/max bounds has changed
         */
        public boolean includeValue (double val)
        {
            boolean extended = false;

            // Cumulate into Population
            population.includeValue(val);

            // Handle min value
            if (min != null) {
                if (val < min) {
                    min = val;
                    extended = true;
                }
            } else {
                min = val;
                extended = true;
            }

            // Handle max value
            if (max != null) {
                if (val > max) {
                    max = val;
                    extended = true;
                }
            } else {
                max = val;
                extended = true;
            }

            return extended;
        }

        public void reset ()
        {
            population.reset();
            min = null;
            max = null;
            mean = 0;
            weight = 0;
        }

        /**
         * Report the weighted square delta of a value vs param mean value
         *
         * @param val       the observed value
         * @param stdWeight the standard average weight
         * @return the weighted square delta
         */
        public double weightedDelta (double val,
                                     double stdWeight)
        {
            if (training == TrainingStatus.NONE) {
                return INFINITE_DISTANCE;
            } else {
                double dif = mean - val;

                return dif * dif * getWeight(stdWeight);
            }
        }

        /**
         * Report the proper value to be used for parameter weight.
         *
         * @param stdWeight the standard average weight
         * @return the proper weight value
         */
        private double getWeight (double stdWeight)
        {
            switch (training) {
            case NONE:
                return 0;

            case SINGLE_DATA:
                return stdWeight;

            case IDENTICAL_VALUES:
                return stdWeight * HIGH_WEIGHT_FACTOR;

            default:
                return weight;
            }
        }
    }

    //-----------//
    // Parameter //
    //-----------//
    /**
     * Description of an input parameter for the LinearEvaluator.
     */
    private static class Parameter
    {
        //~ Instance fields ----------------------------------------------------

        /** Default weight */
        @XmlAttribute(name = "weight")
        public double defaultWeight;

        /** Name used for this parameter */
        @XmlID
        @XmlAttribute(name = "name")
        public final String name;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Parameter object.
         *
         * @param name the unique name for this parameter
         */
        public Parameter (String name)
        {
            this.name = name;
        }

        /**
         * Needed by JAXB
         */
        public Parameter ()
        {
            name = null;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public String toString ()
        {
            return "{Param " + name + " defaultWeight:" + defaultWeight + "}";
        }
    }
}
