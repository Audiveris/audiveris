//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S e c t i o n V i e w                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Class {@code SectionView} defines one view meant for display of a given section.
 *
 * @author Hervé Bitteur
 */
public interface SectionView
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Return the display rectangle used by the rendering of the section
     *
     * @return the bounding rectangle in the display space
     */
    Rectangle getBounds ();

    /**
     * Render the section
     *
     * @param g             the graphics context
     * @param drawBorders   should section borders be drawn
     * @param specificColor specific color
     * @return true if actually rendered, i.e. is displayed
     */
    boolean render (Graphics g,
                    boolean drawBorders,
                    Color specificColor);

    /**
     * Render the section using the provided graphics object, while
     * showing that the section has been selected.
     *
     * @param g the graphics environment (which may be applying transformation such as scale)
     * @return true if the section is concerned by the clipping rectangle, which means if (part of)
     *         the section has been drawn
     */
    boolean renderSelected (Graphics g);
}
