//-----------------------------------------------------------------------//
//                                                                       //
//                           L o g S l i d e r                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.ui;

import javax.swing.*;
import java.util.Hashtable;

/**
 * Class <code>LogSlider</code> builds a slider with a logarithmic scale
 */
public class LogSlider
        extends JSlider
{
    //~ Static variables/initializers -------------------------------------

    // Internal precision
    private static final int unit = 100;

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
     * @param orientation to specify the slider orientation, either
     *                    VERTICAL or HORIZONTAL.
     * @param min to set lower bound of the slider, specified in power of
     *                    base, for example -3 to mean 1/8 (2**-3 if base =
     *                    2).
     * @param max to set upper bound, for example 5 to mean 32 (2**5, if
     *                    base = 2).
     * @param initial to set the slider initial value, specified in power
     *                    of base, for example 0 to mean 1 (2**0, if base =
     *                    2)
     */
    public LogSlider (int base,
                      int orientation,
                      int min,
                      int max,
                      int initial)
    {
        // JSlider
        super(orientation, min * unit, max * unit, initial * unit);

        // Cache data
        this.base = (double) base;

        // Ticks
//         setMajorTickSpacing (unit);
//         if (unit >= 10)
//             setMinorTickSpacing (unit /10);
//         setPaintTicks  (true);

        // Labels
        setPaintLabels(true);

        // More room
        //      switch (orientation) {
        //      case HORIZONTAL : setBorder (BorderFactory.createEmptyBorder(0,0,5,0));
        //          break;
        //      case VERTICAL   : setBorder (BorderFactory.createEmptyBorder(0,0,0,5));
        //      }
        // Create the label table
        Hashtable<Integer, JLabel> labelTable
            = new Hashtable<Integer, JLabel>();

        for (int i = min; i <= max; i++) {
            labelTable.put(new Integer(i * unit),
                           new JLabel((i < 0)
                                      ? ("1/" + (int) expOf(-i * unit))
                                      : ("" + (int) expOf(i * unit))));
        }

        setLabelTable(labelTable);
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // readValue //
    //-----------//

    /**
     * Retrieves the slider current position, and returns the corresponding
     * value
     *
     * @return The current value, such as 32 or 0.125.
     */
    public double readValue ()
    {
        return expOf((int) getValue());
    }

    //------------//
    // writeValue //
    //------------//

    /**
     * Uses the provided value, to set the slider internal position.
     *
     * @param d a <code>double</code> value, such as 32 or 0.125.
     */
    public void writeValue (double d)
    {
        setValue(logOf(d));
    }

    //-------//
    // expOf //
    //-------//
    private double expOf (int i)
    {
        return Math.pow(base, (double) i / (double) unit);
    }

    //-------//
    // logOf //
    //-------//
    private int logOf (double d)
    {
        return (int) Math.rint
            (((double) unit * Math.log(d)) / Math.log(base));
    }
}
