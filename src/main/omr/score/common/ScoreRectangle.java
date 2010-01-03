//----------------------------------------------------------------------------//
//                                                                            //
//                        S c o r e R e c t a n g l e                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;

import java.awt.Rectangle;

/**
 * Class <code>ScoreRectangle</code> is a simple Rectangle that is meant to
 * represent a rectangle in a score display, with its components specified in
 * units, and its origin being the score display origin.
 *
 * <p> This specialization is used to take benefit of compiler checks, to
 * prevent the use of rectangles with incorrect meaning or units. </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreRectangle
    extends SimpleRectangle
{
    //~ Constructors -----------------------------------------------------------

    //----------------//
    // ScoreRectangle //
    //----------------//
    /**
     * Creates an instance of <code>ScoreRectangle</code> with all items set to
     * zero.
     */
    public ScoreRectangle ()
    {
    }

    //----------------//
    // ScoreRectangle //
    //----------------//
    /**
     * Constructs a <code>ScoreRectangle</code> and initializes it with the
     * specified data.
     *
     * @param x the specified x
     * @param y the specified y
     * @param width the specified width
     * @param height the specified height
     */
    public ScoreRectangle (int x,
                           int y,
                           int width,
                           int height)
    {
        super(x, y, width, height);
    }

    //----------------//
    // ScoreRectangle //
    //----------------//
    /**
     * Construct a <code>ScoreRectangle</code> and initialize it with the
     * specified generic rectangle
     *
     * @param rect the specified generic rectangle
     */
    public ScoreRectangle (Rectangle rect)
    {
        super(rect.x, rect.y, rect.width, rect.height);
    }

    //----------------//
    // ScoreRectangle //
    //----------------//
    /**
     * Construct a <code>ScoreRectangle</code> with just a ScorePoint
     *
     * @param pt the specified point
     */
    public ScoreRectangle (ScorePoint pt)
    {
        super(pt.x, pt.y, 0, 0);
    }
}
