//-----------------------------------------------------------------------//
//                                                                       //
//                   V e r t i c a l L a g R e a d e r                   //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.lag;

import omr.sheet.Picture;

import java.util.List;

/**
 * Class <code>VerticalLagReader</code> is a {@link LagReader} optimized
 * for a lag made of vertical sections.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class VerticalLagReader
    extends LagReader
{
    //~ Constructors ------------------------------------------------------

    //-----------//
    // LagReader //
    //-----------//

    /**
     * Create a vertical lag adapter, with its key parameters.
     *
     * @param lag the lag for which runs have to be filled
     * @param runs the collections of runs to be filled
     * @param picture the picture to read runs from. Lag orientation is
     *                  used to properly access the picture pixels.
     * @param minLength the minimum length for each run
     */
    public VerticalLagReader (Lag lag,
                              List<List<Run>> runs,
                              Picture picture,
                              int minLength)
    {
        super(lag, runs, picture, minLength);
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // getLevel //
    //----------//
    @Override
    public final int getLevel (int coord,
                               int pos)
    {
        // Beware the parameters order
        return picture.getPixel(pos, coord);
    }
}
