//----------------------------------------------------------------------------//
//                                                                            //
//                              L i n e I n f o                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.glyph.GlyphSection;

import omr.score.common.PixelPoint;

import omr.util.HorizontalSide;

import java.awt.Graphics2D;
import java.util.Collection;

/**
 * Interface <code>LineInfo</code> describes the handling of one staff line.
 *
 * @author Hervé Bitteur
 */
public interface LineInfo
{
    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getEndPoint //
    //-------------//
    /**
     * Selector for the left or right ending point of the line
     * @param side proper horizontal side
     * @return left point
     */
    public PixelPoint getEndPoint (HorizontalSide side);

    //-------//
    // getId //
    //-------//
    /**
     * Report the id of this line
     * @return the line id (debugging info)
     */
    public int getId ();

    //--------------//
    // getLeftPoint //
    //--------------//
    /**
     * Selector for the left point of the line
     * @return left point
     */
    public PixelPoint getLeftPoint ();

    //---------------//
    // getRightPoint //
    //---------------//
    /**
     * Selector for the right point of the line
     * @return right point
     */
    public PixelPoint getRightPoint ();

    //-------------//
    // getSections //
    //-------------//
    /**
     * Report the lag sections that compose the staff line
     * @return a collection of the line sections
     */
    public Collection<GlyphSection> getSections ();

    //---------//
    // cleanup //
    //---------//
    /**
     * Cleanup the line, by removing the sections that compose the line and by
     * extending the external crossing sections
     */
    public void cleanup ();

    //--------//
    // render //
    //--------//
    /**
     * Paint the computed line on the provided environment.
     * @param g     the graphics context
     */
    public void render (Graphics2D g);

    //-----//
    // yAt //
    //-----//
    /**
     * Retrieve the staff line ordinate at given abscissa x, using int values
     *
     * @param x the given abscissa
     * @return the corresponding y value
     */
    public int yAt (int x);

    //-----//
    // yAt //
    //-----//
    /**
     * Retrieve the staff line ordinate at given abscissa x, using double values
     *
     * @param x the given abscissa
     * @return the corresponding y value
     */
    public double yAt (double x);
}
