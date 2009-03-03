//----------------------------------------------------------------------------//
//                                                                            //
//                           D i g r a p h V i e w                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.graph;

import java.awt.*;

/**
 * Interface <code>DigraphView</code> defines what is needed to view a graph.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
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
    void render (Graphics g);
}
