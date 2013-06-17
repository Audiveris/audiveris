//----------------------------------------------------------------------------//
//                                                                            //
//                                 C l o c k                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Class {@code Clock} provides various features related to the current
 * date and time, as well as the elapsed time since the beginning of
 * the application.
 *
 * @author Hervé Bitteur
 */
public class Clock
{
    //~ Static fields/initializers ---------------------------------------------

    /** To have a reference time */
    private static long startTime = System.currentTimeMillis();

    /** General date formatting */
    private static final DateFormat dateFormatter = DateFormat.getDateTimeInstance(
            DateFormat.FULL,
            DateFormat.FULL,
            Locale.US);

    /** General time formatting. Locale to be used, could be: //Locale.US;
     * //Locale.FRANCE; */
    private static Locale locale = Locale.getDefault();

    /** Corresponding number format */
    private static NumberFormat nf = NumberFormat.getNumberInstance(locale);

    /** Decimal format */
    private static DecimalFormat timeFormatter = (DecimalFormat) nf;

    static {
        timeFormatter.applyPattern("000,000.00");
    }

    //~ Constructors -----------------------------------------------------------
    //-------//
    // Clock // To prevent instantiation
    //-------//
    private Clock ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // getDate //
    //---------//
    /**
     * Retrieves the values of current date and time of day, and formats a
     * standard string,
     *
     * @return A standardized date + time string
     */
    public static String getDate ()
    {
        return dateFormatter.format(new Date());
    }

    //------------//
    // getElapsed //
    //------------//
    /**
     * Retrieves the number of milliseconds since the reference start time, and
     * formats a standardized string using seconds and milliseconds. NB: The
     * start time is usually the time when this class was elaborated. It can
     * also be later reset, via the 'reset' method.
     *
     * @return A standardized duration string
     */
    public static String getElapsed ()
    {
        long delta = System.currentTimeMillis() - startTime;

        return timeFormatter.format((double) delta / 1000);
    }

    //-----------//
    // resetTime //
    //-----------//
    /**
     * Resets the reference start value at the time this method is called.
     */
    public static void resetTime ()
    {
        startTime = System.currentTimeMillis();
    }
}
