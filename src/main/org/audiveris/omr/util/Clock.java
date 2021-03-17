//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           C l o c k                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                                              //
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Class {@code Clock} provides various features related to the current date and time,
 * as well as the elapsed time since the beginning of the application.
 *
 * @author Hervé Bitteur
 */
public abstract class Clock
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** To have a reference time */
    private static long startTime = System.currentTimeMillis();

    /** General date formatting */
    private static final DateFormat dateFormatter = DateFormat.getDateTimeInstance(
            DateFormat.FULL,
            DateFormat.FULL,
            Locale.US);

    /**
     * General time formatting.
     * <p>
     * Locale to be used, could be: Locale.US; Locale.FRANCE;
     */
    private static final Locale locale = Locale.getDefault();

    /** Corresponding number format */
    private static final NumberFormat nf = NumberFormat.getNumberInstance(locale);

    /** Decimal format */
    private static final DecimalFormat timeFormatter = (DecimalFormat) nf;

    static {
        timeFormatter.applyPattern("000,000.00");
    }

    //~ Constructors -------------------------------------------------------------------------------
    /** To prevent instantiation. */
    private Clock ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getDate //
    //---------//
    /**
     * Retrieve the values of current date and time of day, and formats a
     * standard string,
     *
     * @return A standardized date + time string
     */
    public static synchronized String getDate ()
    {
        return dateFormatter.format(new Date());
    }

    //------------//
    // getElapsed //
    //------------//
    /**
     * Retrieve the number of milliseconds since the reference start time, and
     * formats a standardized string using seconds and milliseconds.
     * <p>
     * NB: The start time is usually the time when this class was elaborated.
     * It can also be later reset, via the 'reset' method.
     *
     * @return A standardized duration string
     */
    public static String getElapsed ()
    {
        long delta = System.currentTimeMillis() - startTime;

        return timeFormatter.format((double) delta / 1_000);
    }

    //-----------//
    // resetTime //
    //-----------//
    /**
     * Reset the reference start value at the time this method is called.
     */
    public static void resetTime ()
    {
        startTime = System.currentTimeMillis();
    }
}
