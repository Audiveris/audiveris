//-----------------------------------------------------------------------//
//                                                                       //
//                           L o g S l i d e r                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import javax.swing.*;
import java.util.Hashtable;

/**
 * Class <code>LogSlider</code> is a specific {@link JSlider} which handles
 * double values with a logarithmic scale (while normal JSlider handles only
 * integer values).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class LogSlider
    extends JSlider
{
    //~ Static variables/initializers -------------------------------------

    // Internal resolution (number of values between two major ticks)
    // There is no real need to export this value to the user.
    private static final int unit = 480;
    private static final double doubleUnit = unit; // To speed computing up

    //~ Instance variables ------------------------------------------------

    // Base of log (generally 2 or 10)
    private final double base;

    //~ Constructors ------------------------------------------------------

    //-----------//
    // LogSlider //
    //-----------//

    /**
     * Creates a new <code>LogSlider</code> instance.
     *
     * @param base to specify the base of logarithm, generally either 2 or
     *                    10.
     * @param minors number of minor intervals within one major interval (if
     *                    specified as 1 minor per major, this means that
     *                    there is no minor ticks)
     * @param orientation to specify the slider orientation, either VERTICAL
     *                    or HORIZONTAL.
     * @param min to set lower bound of the slider, specified in power of
     *                    base, for example -3 to mean 1/8 (2**-3 if base =
     *                    2).
     * @param max to set upper bound, for example 5 to mean 32 (2**5, if base
     *                    = 2).
     * @param initial to set the slider initial value, specified in power of
     *                    base, for example 0 to mean 1 (2**0, if base = 2)
     */
    public LogSlider (int base,
                      int minors,
                      int orientation,
                      int min,
                      int max,
                      int initial)
    {
        // JSlider construction
        super(orientation, min * unit, max * unit, initial * unit);

        // Cache data
        this.base = (double) base;

        // Ticks
        super.setMajorTickSpacing (unit);
        if (minors > 1) {
            super.setMinorTickSpacing (unit / minors);
        }
        setPaintTicks(true);

        // More room given to labels
//         switch (orientation) {
//         case HORIZONTAL : setBorder (BorderFactory.createEmptyBorder(0,0,5,0));
//             break;
//         case VERTICAL   : setBorder (BorderFactory.createEmptyBorder(0,0,0,5));
//         }

        // Create and populate the label table
        Hashtable<Integer, JLabel> labelTable
            = new Hashtable<Integer, JLabel>();
        for (int i = min; i <= max; i++) {
            labelTable.put(new Integer(i * unit),
                           new JLabel((i < 0)
                                      ? ("1/" + (int) expOf(-i * unit))
                                      : ("" + (int) expOf(i * unit))));
        }
        setLabelTable(labelTable);
        setPaintLabels(true);

        // Force the knob to align on predefined ticks
        setSnapToTicks(true);
    }

    //~ Methods -----------------------------------------------------------

    //----------------//
    // getDoubleValue //
    //----------------//
    /**
     * Retrieve the slider current position, and return the corresponding
     * value
     *
     * @return The current value, such as 32 or 0.125.
     */
    public double getDoubleValue ()
    {
        return expOf(super.getValue());
    }

    //----------------//
    // setDoubleValue //
    //----------------//
    /**
     * Use the provided value, to set the slider internal position.
     *
     * @param d a <code>double</code> value, such as 32 or 0.125.
     */
    public void setDoubleValue (double d)
    {
        super.setValue(logOf(d));
    }

    //---------------------//
    // setMajorTickSpacing //
    //---------------------//
    /**
     * This is a non supported operation, though part of the JSlider
     * interface, since there is exactly one major tick per each increment
     * of base power.
     *
     * @param n not used
     */
    @Override
        public void setMajorTickSpacing (int n)
    {
        throw new UnsupportedOperationException
            ("Method setMajorTickSpacing not supported by LogSlider");
    }

    //---------------------//
    // setMinorTickSpacing //
    //---------------------//
    /**
     * This is a non supported operation, though part of the JSlider
     * interface, since the correct way to define minor ticks is in the
     * constructor to specify the number of minors (minor intervals) within
     * any major interval.
     *
     * @param n not used
     */
    @Override
        public void setMinorTickSpacing (int n)
    {
        throw new UnsupportedOperationException
            ("Method setMinorTickSpacing not supported by LogSlider");
    }

    //-------//
    // expOf //
    //-------//
    private double expOf (int i)
    {
        return Math.pow(base, (double) i / doubleUnit);
    }

    //-------//
    // logOf //
    //-------//
    private int logOf (double d)
    {
        return (int) Math.rint
            ((doubleUnit * Math.log(d)) / Math.log(base));
    }
}
