//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              A d a p t i v e D e s c r i p t o r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.image;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.process.ByteProcessor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>AdaptiveDescriptor</code> describes the configuration parameters of a
 * {@link AdaptiveFilter}.
 * <p>
 * To decide if a pixel at coordinates (x,y) is a foreground or a background pixel, an adaptive
 * filter compares the pixel value at (x,y) with a threshold value dynamically computed in the
 * neighborhood of (x,y) point.
 * <p>
 * The threshold is computed as follows:
 *
 * <pre>
 * threshold = (MEAN_COEFF * mean) + (STD_DEV_COEFF * stdDev)
 * </pre>
 *
 * where:
 * <ul>
 * <li><code>mean</code> and <code>stdDev</code> are respectively the mean value and the
 * standard deviation value of the pixels in the neighborhood
 * <li><code>MEAN_COEFF</code> and <code>STD_DEV_COEFF</code> are constant coefficients.
 * These coefficients are configured by the filter descriptor.
 * </ul>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "adaptive-filter")
public class AdaptiveDescriptor
        extends FilterDescriptor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(AdaptiveDescriptor.class);

    /** The maximum value for meanCoeff */
    public static final double MAXMEAN = 1.5;

    /** The minimum value for meanCoeff */
    public static final double MINMEAN = 0.0;

    /** The maximum value for stdDevCoeff */
    public static final double MAXSTDDEV = 1.5;

    /** The minimum value for stdDevCoeff */
    public static final double MINSTDDEV = 0.0;

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * In the threshold formula, this parameter defines the coefficient value applied to
     * the <code>mean</code> pixel value in any point neighborhood.
     */
    @XmlAttribute(name = "mean-coeff")
    public final double meanCoeff;

    /**
     * In the threshold formula, this parameter defines the coefficient value applied to
     * the <code>standard deviation</code> of pixel values in any point neighborhood.
     */
    @XmlAttribute(name = "std-dev-coeff")
    public final double stdDevCoeff;

    //~ Constructors -------------------------------------------------------------------------------

    /** No-argument constructor meant for JAXB. */
    private AdaptiveDescriptor ()
    {
        meanCoeff = 0;
        stdDevCoeff = 0;
    }

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

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if ((obj instanceof AdaptiveDescriptor) && super.equals(obj)) {
            final AdaptiveDescriptor that = (AdaptiveDescriptor) obj;
            final double epsilon = 0.00001;

            return (Math.abs(this.meanCoeff - that.meanCoeff) < epsilon) && (Math.abs(
                    this.stdDevCoeff - that.stdDevCoeff) < epsilon);
        }

        return false;
    }

    //-----------//
    // getFilter //
    //-----------//
    @Override
    public PixelFilter getFilter (ByteProcessor source)
    {
        return new VerticalFilter(source, meanCoeff, stdDevCoeff);
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
        hash = (97 * hash) + (int) (Double.doubleToLongBits(this.meanCoeff) ^ (Double
                .doubleToLongBits(this.meanCoeff) >>> 32));
        hash = (97 * hash) + (int) (Double.doubleToLongBits(this.stdDevCoeff) ^ (Double
                .doubleToLongBits(this.stdDevCoeff) >>> 32));

        return hash;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());
        sb.append(" meanCoeff: ").append(meanCoeff);
        sb.append(" stdDevCoeff: ").append(stdDevCoeff);

        return sb.toString();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------------//
    // defaultIsSpecific //
    //-------------------//
    public static boolean defaultIsSpecific ()
    {
        return !constants.meanCoeff.isSourceValue() || !constants.stdDevCoeff.isSourceValue();
    }

    //------------//
    // getDefault //
    //------------//
    public static AdaptiveDescriptor getDefault ()
    {
        return new AdaptiveDescriptor(getDefaultMeanCoeff(), getDefaultStdDevCoeff());
    }

    //---------------------//
    // getDefaultMeanCoeff //
    //---------------------//
    public static double getDefaultMeanCoeff ()
    {
        return constants.meanCoeff.getValue();
    }

    //-----------------------//
    // getDefaultStdDevCoeff //
    //-----------------------//
    public static double getDefaultStdDevCoeff ()
    {
        return constants.stdDevCoeff.getValue();
    }

    //----------------//
    // getSourceValue //
    //----------------//
    public static AdaptiveDescriptor getSourceValue ()
    {
        return new AdaptiveDescriptor(
                constants.meanCoeff.getSourceValue(),
                constants.stdDevCoeff.getSourceValue());
    }

    //---------------//
    // resetToSource //
    //---------------//
    public static void resetToSource ()
    {
        constants.meanCoeff.resetToSource();
        constants.stdDevCoeff.resetToSource();
    }

    //---------------------//
    // setDefaultMeanCoeff //
    //---------------------//
    public static void setDefaultMeanCoeff (double meanCoeff)
    {
        constants.meanCoeff.setValue(meanCoeff);
    }

    //-----------------------//
    // setDefaultStdDevCoeff //
    //-----------------------//
    public static void setDefaultStdDevCoeff (double stdDevCoeff)
    {
        constants.stdDevCoeff.setValue(stdDevCoeff);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Ratio meanCoeff = new Constant.Ratio(
                0.7,
                "Threshold formula coefficient for mean pixel value");

        private final Constant.Ratio stdDevCoeff = new Constant.Ratio(
                0.9,
                "Threshold formula coefficient for pixel standard deviation");
    }
}
