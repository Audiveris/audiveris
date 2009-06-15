//----------------------------------------------------------------------------//
//                                                                            //
//                            V e r t e x V i e w                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.graph;

import java.awt.*;

/**
 * Interface <code>VertexView</code> defines the interface needed to handle the
 * rendering of a vertex.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface VertexView
{
    //~ Methods ----------------------------------------------------------------

    //--------------//
    // getRectangle //
    //--------------//
    /**
     * Return the display rectangle used by the rendering of the vertex
     *
     * @return the bounding rectangle in the display space
     */
    Rectangle getRectangle ();

    //--------//
    // render //
    //--------//
    /**
     * Render the vertex
     *
     * @param g the graphics context
     * @param drawBorders should vertex borders be drawn
     * 
     * @return true if actually rendered, i.e. is displayed
     */
    boolean render (Graphics g,
                    boolean  drawBorders);
}
