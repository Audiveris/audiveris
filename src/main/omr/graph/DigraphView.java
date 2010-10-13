//----------------------------------------------------------------------------//
//                                                                            //
//                           D i g r a p h V i e w                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.graph;

import java.awt.*;

/**
 * Interface <code>DigraphView</code> defines what is needed to view a graph.
 *
 * @author Hervé Bitteur
 */
public interface DigraphView
{
    //~ Methods ----------------------------------------------------------------

    //--------//
    // render //
    //--------//
    /**
     * Render the whole graph view
     *
     * @param g the graphics context
     */
    void render (Graphics2D g);
}
