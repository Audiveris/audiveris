//----------------------------------------------------------------------------//
//                                                                            //
//                                M e m o r y                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

/**
 * Class {@code Memory} is a collection of static methods to ease the
 * interaction with the memory characteristics.
 *
 * @author Hervé Bitteur
 */
public class Memory
{
    //~ Static fields/initializers ---------------------------------------------

    private static Runtime rt = Runtime.getRuntime();

    //~ Constructors -----------------------------------------------------------
    // Not meant to be instantiated
    private Memory ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // free //
    //------//
    /**
     * Get the size of the free memory
     *
     * @return this size
     */
    public static long free ()
    {
        return rt.freeMemory();
    }

    //----//
    // gc //
    //----//
    /**
     * 'Suggest' to run the garbage collector. Note this does not call the
     * garbage collector synchronously.
     */
    public static void gc ()
    {
        rt.runFinalization();
        rt.gc();
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Return a well formatted string about the occupied memory
     *
     * @return the string ready to be printed
     */
    public static String getValue ()
    {
        gc();

        // Format by slices of 3 digits
        return String.format("%,d", occupied());
    }

    //----------//
    // occupied //
    //----------//
    /**
     * Get the size of the currently occupied memory
     *
     * @return this size
     */
    public static long occupied ()
    {
        return total() - free();
    }

    //-----------//
    // printFree //
    //-----------//
    /**
     * Print on the standard output the free memory
     */
    public static void printFree ()
    {
        System.out.println("Free Memory=" + free());
    }

    //---------------//
    // printOccupied //
    //---------------//
    /**
     * Print on the standard output the occupied memory
     */
    public static void printOccupied ()
    {
        System.out.println("Occupied Memory=" + occupied());
    }

    //------------//
    // printTotal //
    //------------//
    /**
     * Print on the standard output the total memory
     */
    public static void printTotal ()
    {
        System.out.println("Total Memory=" + total());
    }

    //-------//
    // total //
    //-------//
    /**
     * Get the total memory in use.
     *
     * @return this total
     */
    public static long total ()
    {
        return rt.totalMemory();
    }
}
