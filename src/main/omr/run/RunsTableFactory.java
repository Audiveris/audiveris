//----------------------------------------------------------------------------//
//                                                                            //
//                      R u n s T a b l e F a c t o r y                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import omr.lag.PixelSource;

import omr.log.Logger;

import omr.score.common.PixelRectangle;

import omr.util.Implement;

import java.awt.Dimension;

/**
 * Class {@code RunsTableFactory} retrieves the runs structure out of a given
 * pixel source and builds the related {@link RunsTable} structure.
 *
 * @author Hervé Bitteur
 */
public class RunsTableFactory
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        RunsTableFactory.class);

    //~ Instance fields --------------------------------------------------------

    /** The source to read runs of pixels from*/
    private final PixelSource source;

    /** The desired orientation */
    private final Orientation orientation;

    /** The maximum pixel gray level to be foreground */
    private final int maxLevel;

    /** The minimum value for a run length to be considered */
    private final int minLength;

    /** Remember if we have to swap x and y coordinates */
    private final boolean swapNeeded;

    /** The created RunsTable */
    private RunsTable table;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // RunsTableFactory //
    //------------------//
    /**
     * Create an RunsTableFactory, with its key parameters.
     *
     * @param orientation the desired orientation of runs
     * @param source the source to read runs from. Orientation parameter is
     * used to properly access the source pixels.
     * @param maxLevel maximum gray level to be a foreground pixel
     * @param minLength the minimum length for each run
     */
    public RunsTableFactory (Orientation orientation,
                             PixelSource source,
                             int         maxLevel,
                             int         minLength)
    {
        this.orientation = orientation;
        this.source = source;
        this.minLength = minLength;
        this.maxLevel = maxLevel;

        swapNeeded = orientation.isVertical();
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // createTable //
    //------------//
    /**
     * Report the RunsTable created with the runs retrieved from the provided
     * source.
     * @param name the name to be assigned to the table
     * @return a populated RunsTable
     */
    public RunsTable createTable (String name)
    {
        table = new RunsTable(
            name,
            orientation,
            new Dimension(source.getWidth(), source.getHeight()));

        RunsRetriever retriever = new RunsRetriever(
            orientation,
            new MyAdapter());

        retriever.retrieveRuns(
            new PixelRectangle(0, 0, source.getWidth(), source.getHeight()));

        return table;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // MyAdapter //
    //-----------//
    private class MyAdapter
        implements RunsRetriever.Adapter
    {
        //~ Methods ------------------------------------------------------------

        //--------//
        // isFore //
        //--------//
        /**
         * Check whether the provide pixel value is foreground or background
         *
         * @param level pixel gray level
         *
         * @return true if foreground, false if background
         */
        @Implement(RunsRetriever.Adapter.class)
        public final boolean isFore (int level)
        {
            return level <= maxLevel;
        }

        //----------//
        // getLevel //
        //----------//
        /**
         * Retrieve the pixel gray level of a point in the underlying source
         *
         * @param coord coordinate value, relative to lag orientation
         * @param pos position value, relative to lag orientation
         *
         * @return pixel gray level
         */
        @Implement(RunsRetriever.Adapter.class)
        public final int getLevel (int coord,
                                   int pos)
        {
            if (swapNeeded) {
                return source.getPixel(pos, coord);
            } else {
                return source.getPixel(coord, pos);
            }
        }

        //---------//
        // backRun //
        //---------//
        /**
         * Call-back called when a background run has been built
         *
         * @param coord coordinate of run start
         * @param pos position of run start
         * @param length run length
         */
        @Implement(RunsRetriever.Adapter.class)
        public final void backRun (int coord,
                                   int pos,
                                   int length)
        {
            // No interest in background runs
        }

        //---------//
        // foreRun //
        //---------//
        /**
         * Call-back called when a foreground run has been built
         *
         * @param coord coordinate of run start
         * @param pos position of run start
         * @param length run length
         * @param cumul cumulated pixel gray levels on all run points
         */
        @Implement(RunsRetriever.Adapter.class)
        public final void foreRun (int coord,
                                   int pos,
                                   int length,
                                   int cumul)
        {
            // We consider only runs that are longer than minLength
            if (length >= minLength) {
                final int level = ((2 * cumul) + length) / (2 * length);
                table.getSequence(pos)
                     .add(new Run(coord - length, length, level));
            }
        }

        //-----------//
        // terminate //
        //-----------//
        /**
         * Method called-back when all runs have been read
         */
        @Implement(RunsRetriever.Adapter.class)
        public final void terminate ()
        {
            if (logger.isFineEnabled()) {
                StringBuilder buf = new StringBuilder(2048);
                buf.append("Retrieved runs");

                for (int i = 0; i < table.getSize(); i++) {
                    buf.append(
                        "\nCol " + i + " = " + table.getSequence(i).size());
                }

                logger.fine(buf.toString());
            }
        }
    }
}
