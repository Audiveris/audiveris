//-----------------------------------------------------------------------//
//                                                                       //
//                                 R u n                                 //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.lag;

import java.awt.*;

/**
 * Class <code>Run</code> implements a contiguous run of pixels of the same
 * color. Note that the direction (vertical or horizontal) is not relevant.
 */
public class Run
        implements java.io.Serializable
{
    //~ Instance variables ---------------------------------------------------

    // Run characteristics
    private final int start;
    private final int length;
    private final int level;

    //~ Constructors ---------------------------------------------------------

    //-----//
    // Run //
    //-----//

    /**
     * Creates a new <code>Run</code> instance.
     *
     * @param start  the coordinate of start for a run (y for vertical run)
     * @param length the length of the run in pixels
     * @param level  the average level of grey in the run (0 for totally
     *               black, 255 for totally white)
     */
    public Run (int start,
                int length,
                int level)
    {
        this.start = start;
        this.length = length;
        this.level = level;
    }

    //~ Methods --------------------------------------------------------------

    //----------//
    // readRuns //
    //----------//

    /**
     * The <code>readRuns</code> method can be used to build the runs on the
     * fly, by providing a given rectangle. Note that the w and h parameters
     * can be swapped, which allows both vertical and horizontal uses, if the
     * Reader.getPixel() method is defined accordingly.
     *
     * @param reader a <code>Reader</code> instance, used to link to specific
     *               call-back actions on behalf of the caller, when a run
     *               (either foreground or background) has just been read.
     * @param rect   the rectangular area coord x pos to explore
     */
    public static void readRuns (Reader reader,
                                 Rectangle rect)
    {
        final int cMin = rect.x;
        final int cMax = rect.x + rect.width - 1;
        final int pMin = rect.y;
        final int pMax = rect.y + rect.height - 1;

        boolean isFore; // Current run is FOREGROUND or BACKGROUND
        int length; // Current length of the run in progress
        int cumul; // Current cumulated grey level for the run in progress

        // Browse one dimension
        for (int p = pMin; p <= pMax; p++) {
            isFore = false;
            length = 0;
            cumul = 0;

            // Browse other dimension
            for (int c = cMin; c <= cMax; c++) {
                final int level = reader.getLevel(c, p);

                if (reader.isFore(level)) {
                    // We are on a foreground pixel
                    if (isFore) {
                        // Append to the foreground run in progress
                        length++;
                        cumul += level;
                    } else {
                        // End the previous background run if any
                        if (length > 0) {
                            reader.backRun(c, p, length);
                        }

                        // Initialize values for the starting foreground run
                        isFore = true;
                        length = 1;
                        cumul = level;
                    }
                } else {
                    // We are on a background pixel
                    if (isFore) {
                        // End the previous foreground run
                        reader.foreRun(c, p, length, cumul);

                        // Initialize values for the starting background run
                        isFore = false;
                        length = 1;
                    } else {
                        // Append to the background run in progress
                        length++;
                    }
                }
            }

            // Process end of last run
            if (isFore) {
                reader.foreRun(cMax + 1, p, length, cumul);
            } else {
                reader.backRun(cMax + 1, p, length);
            }
        }

        // Last wishes
        reader.terminate();
    }

    //-----------//
    // getLength //
    //-----------//

    /**
     * Report the length of the run in pixels
     *
     * @return this length
     */
    public int getLength ()
    {
        return length;
    }

    //----------//
    // getLevel //
    //----------//

    /**
     * Return the mean grey level of the run
     *
     * @return the average value of grey level along this run
     */
    public int getLevel ()
    {
        return level;
    }

    //----------//
    // getStart //
    //----------//

    /**
     * Report the starting coordinate of the run (x for horizontal, y for
     * vertical)
     *
     * @return the start coordinate
     */
    public int getStart ()
    {
        return start;
    }

    //---------//
    // getStop //
    //---------//

    /**
     * Return the coordinate of the stop for a run.  This is the bottom
     * ordinate for a vertical run, or the right abscissa for a horizontal
     * run.
     *
     * @return the stop coordinate
     */
    public int getStop ()
    {
        return (start + length) - 1;
    }

    //----------//
    // toString //
    //----------//

    /**
     * The <code>toString</code> method is used to get a readable image of the
     * run.
     *
     * @return a <code>String</code> value
     */
    public String toString ()
    {
        StringBuffer sb = new StringBuffer(80);
        sb.append("{Run ");
        sb.append(start).append("/").append(length);
        sb.append("@").append(level);
        sb.append("}");

        return sb.toString();
    }

    //~ Interfaces -----------------------------------------------------------

    /**
     * Interface <code<Run.Reader</code> is used to plug call-backs to a
     * run retrieval process.
     */
    public static interface Reader
    {
        //~ Methods ----------------------------------------------------------

        //--------//
        // isFore //
        //--------//

        /**
         * This method is used to check if the grey level corresponds to a
         * foreground pixel.
         *
         * @param level pixel level of grey
         *
         * @return true if pixel is foreground, false otherwise
         */
        boolean isFore (int level);

        //----------//
        // getLevel //
        //----------//

        /**
         * This method is used to report the grey level of the pixel read at
         * location (coord, pos).
         *
         * @param coord x for horizontal runs, y for vertical runs
         * @param pos   y for horizontal runs, x for vertical runs
         *
         * @return the pixel grey value (from 0 for black up to 255 for
         *         white)
         */
        int getLevel (int coord,
                      int pos);

        //---------//
        // backRun //
        //---------//

        /**
         * Called at end of a background run, with the related coordinates
         *
         * @param coord  location of the point past the end of the run
         * @param pos    constant position of the run
         * @param length length of the run just found
         */
        void backRun (int coord,
                      int pos,
                      int length);

        //---------//
        // foreRun //
        //---------//

        /**
         * Same as background, but for a foreground run. We also provide the
         * measure of accumulated grey level in that case.
         *
         * @param coord  location of the point past the end of the run
         * @param pos    constant position of the run
         * @param length length of the run just found
         * @param cumul  cumulated grey levels along the run
         */
        void foreRun (int coord,
                      int pos,
                      int length,
                      int cumul);

        //-----------//
        // terminate //
        //-----------//

        /**
         * Called at the very end of run retrieval
         */
        void terminate ();
    }
}
