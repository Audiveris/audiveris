//-----------------------------------------------------------------------//
//                                                                       //
//                               C u m u l                               //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.math;

import omr.util.Logger;

/**
 * Class <code>Cumul</code> is used to cumulate measurements, and compute
 * standard deviation and variance on them.
 */
public class Cumul
    implements java.io.Serializable
{
    //~ Static variables/initializers ----------------------------------------

    private static final Logger logger = Logger.getLogger(Cumul.class);

    //~ Instance variables ---------------------------------------------------

    // Instance stuff
    private int n = 0; // Number of measurements
    private double s = 0d; // Sum of measured values
    private double s2 = 0d; // Sum of squared measured values

    //~ Constructors ---------------------------------------------------------

    //-------//
    // Cumul //
    //-------//

    /**
     * Construct a structure to cumulate the measured values
     */
    public Cumul ()
    {
    }

    //~ Methods --------------------------------------------------------------

    //---------//
    // getMean //
    //---------//

    /**
     * Retrieve the mean value from the measurements so far
     *
     * @return the mean value
     */
    public double getMean ()
    {
        if (logger.isDebugEnabled()) {
            logger.logAssert(n > 0, "mean : Cumul is empty");
        }

        return s / (double) n;
    }

    //-----------//
    // getNumber //
    //-----------//

    /**
     * Get the number of cumulated measurements
     *
     * @return this number
     */
    public int getNumber ()
    {
        return n;
    }

    //-----------------//
    // getStdDeviation //
    //-----------------//

    /**
     * Get the standard deviation around the mean value
     *
     * @return the standard deviation
     */
    public double getStdDeviation ()
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
        if (logger.isDebugEnabled()) {
            logger.logAssert(n > 1, "variance : not enough elements");
        }

        return Math.max(0d, (s2 - ((s * s) / (double) n)) / (double) (n - 1));
    }

    //---------//
    // exclude //
    //---------//

    /**
     * Remove a measurement from the cumulated values
     *
     * @param val the measure value to remove
     */
    public void exclude (double val)
    {
        if (logger.isDebugEnabled()) {
            logger.logAssert(n > 0, "exclude : Cumul is empty");
        }

        n -= 1;
        s -= val;
        s2 -= (val * val);
    }

    //---------//
    // include //
    //---------//

    /**
     * Add a measurement to the cumulated values
     *
     * @param val the measure value
     */
    public void include (double val)
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
        include(val);
    }
}
