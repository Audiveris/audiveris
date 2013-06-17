//----------------------------------------------------------------------------//
//                                                                            //
//                            V e r t e x V i e w                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.graph;

import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * Interface {@code VertexView} defines the interface needed to handle
 * the rendering of a vertex.
 *
 * @author Hervé Bitteur
 */
public interface VertexView
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Return the display rectangle used by the rendering of the vertex
     *
     * @return the bounding rectangle in the display space
     */
    Rectangle getBounds ();

    /**
     * Render the vertex
     *
     * @param g           the graphics context
     * @param drawBorders should vertex borders be drawn
     * @return true if actually rendered, i.e. is displayed
     */
    boolean render (Graphics g,
                    boolean drawBorders);
}
