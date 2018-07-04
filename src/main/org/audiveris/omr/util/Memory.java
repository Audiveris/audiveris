//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          M e m o r y                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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

/**
 * Class {@code Memory} is a collection of static methods to ease the interaction with
 * the memory characteristics.
 *
 * @author Hervé Bitteur
 */
public abstract class Memory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Runtime rt = Runtime.getRuntime();

    //~ Constructors -------------------------------------------------------------------------------
    /** Not meant to be instantiated. */
    private Memory ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
     * 'Suggest' to run the garbage collector.
     * Note this does not call the garbage collector synchronously.
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
