//-----------------------------------------------------------------------//
//                                                                       //
//                           L a g R e a d e r                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.lag;

import omr.sheet.Picture;
import omr.util.Logger;

import java.awt.*;
import java.util.List;

/**
 * Class <code>LagReader</code> retrieves the runs structure out of a given
 * picture. Input is thus the picture, the runs are the output, and the
 * provided lag reference is used to access the picture pixels using the
 * proper orientation horizontal / vertical.
 *
 * <p>These runs can then be used to build a lag, using {@link LagBuilder}.
 */
public class LagReader
        implements Run.Reader
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(LagReader.class);

    //~ Instance variables ------------------------------------------------

    /** The lag to populate */
    protected final Lag lag;

    /**
     * Runs found in each horizontal row or vertical column
     */
    protected final List<List<Run>> runs;

    /**
     * The picture to read runs of pixels from
     */
    protected final Picture picture;

    /**
     * The minimum value for a run length to be considered
     */
    protected final int minLength;

    /**
     * The maximum pixel grey level to be foreground
     */
    protected final int maxLevel;

    // To avoid too many allocations
    private Point cp = new Point();
    private Point xy = new Point();

    //~ Constructors ------------------------------------------------------

    //-----------//
    // LagReader //
    //-----------//

    /**
     * Create an adapter, with its key parameters.
     *
     * @param lag       the lag for which runs have to be filled
     * @param runs      the collections of runs to be filled
     * @param picture the picture to read runs from. Lag orientation is
     *                  used to properly access the picture pixels.
     * @param minLength the minimum length for each run
     */
    public LagReader (Lag lag,
                      List<List<Run>> runs,
                      Picture picture,
                      int minLength)
    {
        this.lag = lag;
        this.runs = runs;
        this.picture = picture;
        this.minLength = minLength;
        this.maxLevel = Picture.FOREGROUND;
    }

    //~ Methods -----------------------------------------------------------

    //--------//
    // isFore //
    //--------//
    public boolean isFore (int level)
    {
        return level <= maxLevel;
    }

    //----------//
    // getLevel //
    //----------//
    public int getLevel (int coord,
                         int pos)
    {
        cp.x = coord;
        cp.y = pos;
        lag.switchRef(cp, xy);
        return picture.getPixel(xy.x, xy.y);
    }

    //---------//
    // backRun //
    //---------//
    public void backRun (int coord,
                         int pos,
                         int length)
    {
    }

    //---------//
    // foreRun //
    //---------//
    public void foreRun (int coord,
                         int pos,
                         int length,
                         int cumul)
    {
        // We consider only runs that are longer than minLength
        if (length >= minLength) {
            final int level = ((2 * cumul) + length) / (2 * length);
            runs.get(pos).add(new Run(coord - length, length, level));
        }
    }

    //-----------//
    // terminate //
    //-----------//
    public void terminate ()
    {
        if (logger.isDebugEnabled()) {
            StringBuffer buf = new StringBuffer(2048);
            buf.append("Retrieved runs");

            int i = 0;
            for (List<Run> runList : runs) {
                buf.append("\nCol " + i++ + " = " + runList.size());
            }

            logger.debug(buf.toString());
        }
    }
}
