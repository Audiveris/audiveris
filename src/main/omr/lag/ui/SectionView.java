//----------------------------------------------------------------------------//
//                                                                            //
//                           S e c t i o n V i e w                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag.ui;

import omr.graph.VertexView;

import java.awt.Color;
import java.awt.Graphics;

/**
 * Class {@code SectionView} defines one view meant for display of a
 * given section.
 *
 * @author Hervé Bitteur
 */
public interface SectionView
        extends VertexView
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the default color. This is the permanent default, which is used
     * when the color is reset by {@link #resetColor}
     *
     * @return the section default color
     */
    public Color getDefaultColor ();

    /**
     * Report whether a default color has been assigned
     *
     * @return true if defaultColor is no longer null
     */
    public boolean isColorized ();

    /**
     * Render the section using the provided graphics object, while
     * showing that the section has been selected.
     *
     * @param g the graphics environment (which may be applying transformation
     *          such as scale)
     * @return true if the section is concerned by the clipping rectangle, which
     *         means if (part of) the section has been drawn
     */
    public boolean renderSelected (Graphics g);

    /**
     * Allow to reset to default the display color of a given section
     */
    public void resetColor ();

    /**
     * Allow to modify the display color of a given section.
     *
     * @param color the new color
     */
    public void setColor (Color color);

    /**
     * Set the default color.
     *
     * @param color the default color for this section
     */
    public void setDefaultColor (Color color);
}
