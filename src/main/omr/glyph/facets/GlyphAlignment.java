//----------------------------------------------------------------------------//
//                                                                            //
//                        G l y p h A l i g n m e n t                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.math.Line;

import omr.score.common.PixelPoint;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Interface {@code GlyphAlignment} describes glyph alignment, either
 * horizontal or vertical. The key feature is the approximating Line which is
 * the least-square fitted line on all points contained in the stick.
 *
 * <ul> <li> Staff lines, ledgers, alternate ends are examples of horizontal
 * sticks </li>
 *
 * <li> Bar lines, stems are examples of vertical sticks </li> </ul>
 *
 * @author Hervé Bitteur
 */
interface GlyphAlignment
    extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    //------------------//
    // getAlienPixelsIn //
    //------------------//
    /**
     * Report the number of pixels found in the specified rectangle that do not
     * belong to the stick, and are not artificial patch sections.
     *
     * @param area the rectangular area to investigate, in (coord, pos) form
     *
     * @return the number of alien pixels found
     */
    int getAlienPixelsIn (Rectangle area);

    //------------------//
    // getAliensAtStart //
    //------------------//
    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     * +-------+
     * |       |
     * +=======+==================================+
     * |       |
     * +-------+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    int getAliensAtStart (int dCoord,
                          int dPos);

    //-----------------------//
    // getAliensAtStartFirst //
    //-----------------------//
    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     * +-------+
     * |       |
     * +=======+==================================+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    int getAliensAtStartFirst (int dCoord,
                               int dPos);

    //----------------------//
    // getAliensAtStartLast //
    //----------------------//
    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     * +=======+==================================+
     * |       |
     * +-------+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    int getAliensAtStartLast (int dCoord,
                              int dPos);

    //-----------------//
    // getAliensAtStop //
    //-----------------//

    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     *                                    +-------+
     *                                    |       |
     * +==================================+=======+
     *                                    |       |
     *                                    +-------+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    int getAliensAtStop (int dCoord,
                         int dPos);

    //----------------------//
    // getAliensAtStopFirst //
    //----------------------//
    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     *                                    +-------+
     *                                    |       |
     * +==================================+=======+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    int getAliensAtStopFirst (int dCoord,
                              int dPos);

    //---------------------//
    // getAliensAtStopLast //
    //---------------------//
    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     * +==================================+=======+
     *                                    |       |
     *                                    +-------+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    int getAliensAtStopLast (int dCoord,
                             int dPos);

    //-----------//
    // getAspect //
    //-----------//
    /**
     * Report the ratio of length over thickness
     *
     * @return the "slimness" of the stick
     */
    double getAspect ();

    //---------------//
    // isExtensionOf //
    //---------------//
    /**
     * Checks whether a provided stick can be considered as an extension of this
     * one.  Due to some missing points, a long stick can be broken into several
     * smaller ones, that we must check for this.  This is checked before
     * actually merging them.
     *
     * @param other           the other stick
     * @param maxDeltaCoord Max gap in coordinate (x for horizontal)
     * @param maxDeltaPos   Max gap in position (y for horizontal)
     * @param maxDeltaSlope Max difference in slope
     *
     * @return The result of the test
     */
    boolean isExtensionOf (Stick  other,
                           int    maxDeltaCoord,
                           int    maxDeltaPos,
                           double maxDeltaSlope);

    //-------------//
    // getFirstPos //
    //-------------//
    /**
     * Return the first position (ordinate for stick of horizontal sections,
     * abscissa for stick of vertical sections and runs)
     *
     * @return the position at the beginning
     */
    int getFirstPos ();

    //---------------//
    // getFirstStuck //
    //---------------//
    /**
     * Compute the number of pixels stuck on first side of the stick
     *
     * @return the number of pixels
     */
    int getFirstStuck ();

    //------------//
    // getLastPos //
    //------------//
    /**
     * Return the last position (maximum ordinate for a horizontal stick,
     * maximum abscissa for a vertical stick)
     *
     * @return the position at the end
     */
    int getLastPos ();

    //--------------//
    // getLastStuck //
    //--------------//
    /**
     * Compute the nb of pixels stuck on last side of the stick
     *
     * @return the number of pixels
     */
    int getLastStuck ();

    //-----------//
    // getLength //
    //-----------//
    /**
     * Report the length of the stick
     *
     * @return the stick length in pixels
     */
    int getLength ();

    //---------//
    // getLine //
    //---------//
    /**
     * Return the approximating line computed on the stick.
     *
     * @return The line
     */
    Line getLine ();

    //-----------//
    // getMidPos //
    //-----------//
    /**
     * Return the position (ordinate for horizontal stick, abscissa for vertical
     * stick) at the middle of the stick
     *
     * @return the position of the middle of the stick
     */
    int getMidPos ();

    //----------//
    // getStart //
    //----------//
    /**
     * Return the beginning of the stick (xmin for horizontal, ymin for
     * vertical)
     *
     * @return The starting coordinate
     */
    int getStart ();

    //---------------//
    // getStartPoint //
    //---------------//
    /**
     * Report the point at the beginning of the approximating line
     * @return the starting point of the stick line
     */
    PixelPoint getStartPoint ();

    //----------------//
    // getStartingPos //
    //----------------//
    /**
     * Return the best pos value at starting of the stick
     *
     * @return mean pos value at stick start
     */
    int getStartingPos ();

    //---------//
    // getStop //
    //---------//
    /**
     * Return the end of the stick (xmax for horizontal, ymax for vertical)
     *
     * @return The ending coordinate
     */
    int getStop ();

    //--------------//
    // getStopPoint //
    //--------------//
    /**
     * Report the point at the end of the approximating line
     * @return the ending point of the line
     */
    PixelPoint getStopPoint ();

    //----------------//
    // getStoppingPos //
    //----------------//
    /**
     * Return the best pos value at the stopping end of the stick
     *
     * @return mean pos value at stick stop
     */
    int getStoppingPos ();

    //--------------//
    // getThickness //
    //--------------//
    /**
     * Report the stick thickness
     *
     * @return the thickness in pixels
     */
    int getThickness ();

    //-------------//
    // computeLine //
    //-------------//
    /**
     * Computes the least-square fitted line among all the section points of the
     * stick.
     */
    void computeLine ();

    //--------------//
    // overlapsWith //
    //--------------//
    /**
     * Check whether this stick overlaps with the other stick along their
     * orientation (that is abscissae for horizontal ones, and ordinates for
     * vertical ones)
     * @param other the other stick to check with
     * @return true if overlap, false otherwise
     */
    boolean overlapsWith (Stick other);

    //------------//
    // renderLine //
    //------------//
    /**
     * Render the main guiding line of the stick, using the current foreground
     * color.
     *
     * @param g the graphic context
     */
    void renderLine (Graphics g);
}
