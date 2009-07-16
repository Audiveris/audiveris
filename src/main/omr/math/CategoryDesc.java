
package omr.math;

import omr.log.Logger;

import javax.xml.bind.annotation.*;

/**
 * Class <code>CategoryDesc</code> gathers all characteristics needed to
 * recognize a given category
 */
@XmlRootElement(name = "category-desc")
public class CategoryDesc
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(CategoryDesc.class);

    //~ Instance fields --------------------------------------------------------

    /** Number of values in a pattern */
    private final int inputSize;

    /** Category id */
    @XmlElement(name = "id")
    private final String id;

    /** The specific descriptor for each parameter */
    @XmlElementWrapper(name = "parameters")
    @XmlElement(name = "parameter")
    final ParamDesc[] params;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CategoryDesc object.
     *
     * @param id DOCUMENT ME!
     * @param inputSize DOCUMENT ME!
     */
    public CategoryDesc (String id,
                         int    inputSize)
    {
        super();
        this.id = id;
        this.inputSize = inputSize;
        params = new ParamDesc[inputSize];

        for (int p = 0; p < inputSize; p++) {
            params[p] = new ParamDesc();
        }
    }

    private CategoryDesc ()
    {
        inputSize = -1;
        id = null;
        params = null;
    }

    //~ Methods ----------------------------------------------------------------

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
        for (ParamDesc param : params) {
            try {
                param.compute();
            } catch (Exception ex) {
                logger.warning(
                    "Category#" + id + " cannot compute parameters ex:" + ex);
            }
        }
    }

    public double distance (double[] pattern)
    {
        if (getCardinality() >= 2) {
            double dist = 0;

            for (int p = 0; p < inputSize; p++) {
                dist += params[p].weightedDelta(pattern[p]);
            }

            dist /= inputSize;

            return dist;
        } else {
            return LinearEvaluator.INFINITE_DISTANCE;
        }
    }

    public void dump ()
    {
        System.out.println(
            "\ncategory:" + id + " cardinality:" + getCardinality());

        for (ParamDesc param : params) {
            param.dump();
        }
    }

    public double dumpDistance (double[] pattern)
    {
        if ((pattern == null) || (pattern.length != inputSize)) {
            throw new IllegalArgumentException(
                "dumpDistance. Pattern array is null or non compatible in length ");
        }

        if (getCardinality() >= 2) {
            double dist = 0;

            for (int p = 0; p < inputSize; p++) {
                ParamDesc param = params[p];
                double    wDelta = param.weightedDelta(pattern[p]);
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
            return LinearEvaluator.INFINITE_DISTANCE;
        }
    }

    public void include (double[] pattern)
    {
        for (int p = 0; p < inputSize; p++) {
            params[p].includeValue(pattern[p]);
        }
    }
}
