
package omr.math;

import javax.xml.bind.annotation.*;

/**
 * Class <code>ParamDesc</code> gathers the information about a given
 * parameter (in the context of a given category). It provides the mean
 * value measured in the training samples,
 */
@XmlRootElement(name = "parameter-desc")
class ParamDesc
{
    //~ Instance fields --------------------------------------------------------

    /** Sample to compute mean value & std deviation */
    Population sample = new Population();

    /** Mean value for this parameter */
    @XmlAttribute(name = "mean")
    double mean;

    /** Weight for this parameter */
    @XmlAttribute(name = "weight")
    double weight;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ParamDesc object.
     */
    public ParamDesc ()
    {
        super();
    }

    //~ Methods ----------------------------------------------------------------

    /** Compute the param characteristics out of its data sample */
    public void compute ()
    {
        double weightMax = 2500;
        mean = sample.getMeanValue();
        weight = Math.min(weightMax, 1 / sample.getVariance());
    }

    public void dump ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("mean=")
          .append(mean);
        sb.append(" weight=")
          .append(weight);

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
        mean = weight = 0;
    }

    /** Report the weighted square delta of a value vs param mean value */
    public double weightedDelta (double val)
    {
        double dif = mean - val;

        return dif * dif * weight;
    }
}
