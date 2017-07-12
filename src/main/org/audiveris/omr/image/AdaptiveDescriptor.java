//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              A d a p t i v e D e s c r i p t o r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import ij.process.ByteProcessor;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            AdaptiveDescriptor.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** Coefficient for mean. */
    @XmlAttribute(name = "mean-coeff")
    public final double meanCoeff;

    /** Coefficient for standard deviation. */
    @XmlAttribute(name = "std-dev-coeff")
    public final double stdDevCoeff;

    //~ Constructors -------------------------------------------------------------------------------
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

    /** No-arg constructor meant for JAXB. */
    private AdaptiveDescriptor ()
    {
        meanCoeff = 0;
        stdDevCoeff = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if ((obj instanceof AdaptiveDescriptor) && super.equals(obj)) {
            AdaptiveDescriptor that = (AdaptiveDescriptor) obj;
            final double epsilon = 0.00001;

            return (Math.abs(this.meanCoeff - that.meanCoeff) < epsilon)
                   && (Math.abs(this.stdDevCoeff - that.stdDevCoeff) < epsilon);
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
    public PixelFilter getFilter (ByteProcessor source)
    {
        Class<?> classe = AdaptiveFilter.getImplementationClass();

        try {
            Constructor cons = classe.getConstructor(
                    new Class[]{ByteProcessor.class, double.class, double.class});

            return (PixelFilter) cons.newInstance(source, meanCoeff, stdDevCoeff);
        } catch (Throwable ex) {
            logger.error("Error on getFilter {}", ex.toString(), ex);

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
        sb.append(" meanCoeff: ").append(meanCoeff);
        sb.append(" stdDevCoeff: ").append(stdDevCoeff);

        return sb.toString();
    }
}
