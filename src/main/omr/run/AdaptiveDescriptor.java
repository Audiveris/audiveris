//----------------------------------------------------------------------------//
//                                                                            //
//                    A d a p t i v e D e s c r i p t o r                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AdaptiveDescriptor} describes an {@link AdaptiveFilter}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "adaptive-filter")
public class AdaptiveDescriptor
        extends FilterDescriptor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            AdaptiveDescriptor.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Coefficient for mean. */
    @XmlAttribute(name = "mean-coeff")
    public final double meanCoeff;

    /** Coefficient for standard deviation. */
    @XmlAttribute(name = "std-dev-coeff")
    public final double stdDevCoeff;

    //~ Constructors -----------------------------------------------------------
    //
    //--------------------//
    // AdaptiveDescriptor //
    //--------------------//
    /**
     * Creates a new AdaptiveDescriptor object.
     *
     * @param meanCoeff   Coefficient for mean value
     * @param stdDevCoeff Coefficient for standard deviation value
     */
    public AdaptiveDescriptor (double meanCoeff,
                               double stdDevCoeff)
    {
        this.meanCoeff = meanCoeff;
        this.stdDevCoeff = stdDevCoeff;
    }

    //--------------------//
    // AdaptiveDescriptor // No-arg constructor meant for JAXB
    //--------------------//
    private AdaptiveDescriptor ()
    {
        meanCoeff = 0;
        stdDevCoeff = 0;
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if ((obj instanceof AdaptiveDescriptor) && super.equals(obj)) {
            AdaptiveDescriptor that = (AdaptiveDescriptor) obj;

            return (this.meanCoeff == that.meanCoeff)
                   && (this.stdDevCoeff == that.stdDevCoeff);
        }

        return false;
    }

    //------------//
    // getDefault //
    //------------//
    public static AdaptiveDescriptor getDefault ()
    {
        return new AdaptiveDescriptor(
                AdaptiveFilter.getDefaultMeanCoeff(),
                AdaptiveFilter.getDefaultStdDevCoeff());
    }

    //-----------//
    // getFilter //
    //-----------//
    @Override
    public PixelFilter getFilter (PixelSource source)
    {
        Class<?> classe = AdaptiveFilter.getImplementationClass();

        try {
            Constructor cons = classe.getConstructor(
                    new Class[]{PixelSource.class, double.class, double.class});

            return (PixelFilter) cons.newInstance(
                    source,
                    meanCoeff,
                    stdDevCoeff);
        } catch (Exception ex) {
            logger.error("Error on getFilter {}", ex);

            return null;
        }
    }

    //
    //---------//
    // getKind //
    //---------//
    @Override
    public FilterKind getKind ()
    {
        return FilterKind.ADAPTIVE;
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 5;
        hash = (97 * hash)
               + (int) (Double.doubleToLongBits(this.meanCoeff)
                        ^ (Double.doubleToLongBits(this.meanCoeff) >>> 32));
        hash = (97 * hash)
               + (int) (Double.doubleToLongBits(this.stdDevCoeff)
                        ^ (Double.doubleToLongBits(this.stdDevCoeff) >>> 32));

        return hash;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" meanCoeff:")
                .append(meanCoeff);
        sb.append(" stdDevCoeff:")
                .append(stdDevCoeff);

        return sb.toString();
    }
}
