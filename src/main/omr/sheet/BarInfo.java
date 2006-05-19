//-----------------------------------------------------------------------//
//                                                                       //
//                             B a r I n f o                             //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.lag.Lag;
import omr.stick.Stick;
import omr.ui.view.Zoom;

import java.awt.*;

/**
 * Class <code>BarInfo</code> handles information about a bar line.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class BarInfo
    implements java.io.Serializable
{
    //~ Instance variables ------------------------------------------------

    private Stick stick;
    private int topIdx;
    private int botIdx;

    //~ Constructors ------------------------------------------------------

    //---------//
    // BarInfo //
    //---------//
    /**
     * Create a bar info, with provided parameters
     *
     * @param stick  the stick that corresponds to the bar line
     * @param topIdx the index of the staff at top of bar line
     * @param botIdx the index of the staff at bottom of bar line
     */
    public BarInfo (Stick stick,
                    int   topIdx,
                    int   botIdx)
    {
        this.stick  = stick;
        this.topIdx = topIdx;
        this.botIdx = botIdx;
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // getBotIdx //
    //-----------//
    /**
     * Report the index of the staff at bottom of bar line
     *
     * @return the staff index, starting from 0
     */
    public int getBotIdx ()
    {
        return botIdx;
    }

    //----------//
    // getStick //
    //----------//
    /**
     * Report the vertical stick that represents in the sheet picture, this
     * bar line
     *
     * @return the original stick
     */
    public Stick getStick ()
    {
        return stick;
    }

    //-----------//
    // getTopIdx //
    //-----------//
    /**
     * Report the index of the staff at top of bar line
     *
     * @return the staff index, starting from 0
     */
    public int getTopIdx ()
    {
        return topIdx;
    }

    //----------//
    // colorize //
    //----------//
    /**
     * Define the display color for the related stick
     *
     * @param lag       the lag to be colorized
     * @param viewIndex index of the display
     * @param color     color to be used for display
     */
    public void colorize (Lag lag,
                          int viewIndex,
                          Color color)
    {
        stick.colorize(lag, viewIndex, color);
    }

    //--------//
    // render //
    //--------//
    /**
     * Paint the bar contour
     *
     * @param g the graphics context
     * @param z the display zoom
     */
    public void render (Graphics g,
                        Zoom z)
    {
        stick.renderLine(g, z);
    }

    /**
     * Report a readable description
     *
     * @return a string based on main parameters
     */
    @Override
    public String toString ()
    {
        return "{BarInfo topIdx=" + topIdx + " botIdx=" + botIdx + "}";
    }
}
