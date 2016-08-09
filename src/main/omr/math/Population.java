//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P o p u l a t i o n                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.math;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Population} is used to cumulate measurements, and compute mean value,
 * standard deviation and variance on them.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "population")
public class Population
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Sum of measured values */
    @XmlAttribute(name = "sum")
    private double s = 0d;

    /** Sum of squared measured values */
    @XmlAttribute(name = "squares-sum")
    private double s2 = 0d;

    /** Number of measurements */
    @XmlAttribute(name = "count")
    private int n = 0;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Construct a structure to cumulate the measured values.
     */
    public Population ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // excludeValue //
    //--------------//
    /**
     * Remove a measurement from the cumulated values
     *
     * @param val the measure value to remove
     */
    public void excludeValue (double val)
    {
        if (n < 1) {
            throw new RuntimeException("Population is empty");
        }

        n -= 1;
        s -= val;
        s2 -= (val * val);
    }

    //----------------//
    // getCardinality //
    //----------------//
    /**
     * Get the number of cumulated measurements
     *
     * @return this number
     */
    public int getCardinality ()
    {
        return n;
    }

    //--------------//
    // getMeanValue //
    //--------------//
    /**
     * Retrieve the mean value from the measurements cumulated so far
     *
     * @return the mean value
     */
    public double getMeanValue ()
    {
        if (n == 0) {
            throw new RuntimeException("Population is empty");
        }

        return s / (double) n;
    }

    //----------------------//
    // getStandardDeviation //
    //----------------------//
    /**
     * Get the standard deviation around the mean value
     *
     * @return the standard deviation
     */
    public double getStandardDeviation ()
    {
        return Math.sqrt(getVariance());
    }

    //-------------//
    // getVariance //
    //-------------//
    /**
     * Get the variance around the mean value
     *
     * @return the variance (square of standard deviation)
     */
    public double getVariance ()
    {
        if (n == 0) {
            throw new RuntimeException("Population is empty");
        }

        if (n == 1) {
            return 0;
        }

        return Math.max(0d, (s2 - ((s * s) / n)) / (n - 1)); // Unbiased
        ///return Math.max(0d, (s2 - ((s * s) / n)) / n); // Biased
    }

    //-------------------//
    // includePopulation //
    //-------------------//
    /**
     * Add a whole population to this one
     *
     * @param other the other population to include
     */
    public void includePopulation (Population other)
    {
        n += other.n;
        s += other.s;
        s2 += other.s2;
    }

    //--------------//
    // includeValue //
    //--------------//
    /**
     * Add a measurement to the cumulated values
     *
     * @param val the measure value
     */
    public void includeValue (double val)
    {
        n += 1;
        s += val;
        s2 += (val * val);
    }

    //-------//
    // reset //
    //-------//
    /**
     * Forget all measurements made so far.
     */
    public void reset ()
    {
        n = 0;
        s = 0d;
        s2 = 0d;
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reset to the single measurement provided
     *
     * @param val the new first measured value
     */
    public void reset (double val)
    {
        reset();
        includeValue(val);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("count:").append(n);

        if (n > 0) {
            sb.append(String.format(" mean:%.2f", getMeanValue()));

            if (n > 1) {
                sb.append(String.format(" stdDev:%.2f", getStandardDeviation()));
            }
        }

        return sb.toString();
    }
}
