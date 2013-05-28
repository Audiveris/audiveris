//----------------------------------------------------------------------------//
//                                                                            //
//                             T o l e r a n c e                              //
//                                                                            //
//----------------------------------------------------------------------------//
package com.audiveris.musicxmldiff;

import javax.xml.bind.annotation.XmlValue;

/**
 * Abstract class to provide some tolerance when testing for equality.
 *
 * @author herve
 */
public abstract class Tolerance
{
    //~ Instance fields --------------------------------------------------------

    /** Acceptable gap. */
    @XmlValue
    private final double maxGap;

    //~ Constructors -----------------------------------------------------------
    //
    //-----------//
    // Tolerance //
    //-----------//
    /**
     * Creates a new Tolerance object.
     *
     * @param maxGap Maximum acceptable difference value
     */
    protected Tolerance (double maxGap)
    {
        this.maxGap = maxGap;
    }

    /**
     * No-arg meant for JAXB
     */
    private Tolerance ()
    {
        maxGap = 0;
    }

    //~ Methods ----------------------------------------------------------------
    //
    //-------//
    // check //
    //-------//
    public boolean check (String control,
                          String test)
    {
        try {
            double controlVal = Double.parseDouble(control);
            double testVal = Double.parseDouble(test);

            return getGap(controlVal, testVal) <= maxGap;
        } catch (NumberFormatException nfe) {
            return false;
        }
    }

    //--------//
    // getGap //
    //--------//
    protected abstract double getGap (double controlVal,
                                      double testVal);

    //-----------//
    // getMaxGap //
    //-----------//
    /**
     * @return the maxGap
     */
    public double getMaxGap ()
    {
        return maxGap;
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //-------//
    // Delta //
    //-------//
    /**
     * Tolerance based on simple delta difference.
     */
    public static class Delta
            extends Tolerance
    {
        //~ Constructors -------------------------------------------------------

        public Delta (double maxGap)
        {
            super(maxGap);
        }

        private Delta ()
        {
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected double getGap (double controlVal,
                                 double testVal)
        {
            double delta = Math.abs(controlVal - testVal);
            ///System.out.print(" delta:" + (float) delta);

            return delta;
        }
    }

    //-------//
    // Ratio //
    //-------//
    /**
     * Tolerance based on ratio with respect to largest value.
     */
    public static class Ratio
            extends Tolerance
    {
        //~ Constructors -------------------------------------------------------

        public Ratio (double maxGap)
        {
            super(maxGap);
        }

        private Ratio ()
        {
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected double getGap (double controlVal,
                                 double testVal)
        {
            double largest = Math.max(Math.abs(controlVal), Math.abs(testVal));
            double ratio = Math.abs(controlVal - testVal) / largest;
            ///System.out.print(" ratio:" + (float) ratio);

            return ratio;
        }
    }
}
