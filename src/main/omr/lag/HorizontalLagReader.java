//----------------------------------------------------------------------------//
//                                                                            //
//                   H o r i z o n t a l L a g R e a d e r                    //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.lag;

import omr.sheet.Picture;

import java.util.List;

/**
 * Class <code>HorizontalLagReader</code> is a {@link LagReader} optimized for a
 * lag made of horizontal sections.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class HorizontalLagReader
    extends LagReader
{
    //~ Constructors -----------------------------------------------------------

    //-----------//
    // LagReader //
    //-----------//
    /**
     * Create a horizontal lag adapter, with its key parameters.
     *
     * @param lag the lag for which runs have to be filled
     * @param runs the collections of runs to be filled
     * @param picture the picture to read runs from. Lag orientation is used to
     *                  properly access the picture pixels.
     * @param minLength the minimum length for each run
     */
    public HorizontalLagReader (Lag             lag,
                                List<List<Run>> runs,
                                Picture         picture,
                                int             minLength)
    {
        super(lag, runs, picture, minLength);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getLevel //
    //----------//
    /**
     * Report the grey level of a point in the underlying picture
     *
     * @param coord coordinate (parallel to lag orientation)
     * @param pos position (orthogonal to lag orientation)
     *
     * @return the grey value
     */
    @Override
    public final int getLevel (int coord,
                               int pos)
    {
        // Beware the parameters order
        return picture.getPixel(coord, pos);
    }
}
