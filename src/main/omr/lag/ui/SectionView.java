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
     * Render the section using the provided graphics object, while
     * showing that the section has been selected.
     *
     * @param g the graphics environment (which may be applying transformation
     *          such as scale)
     * @return true if the section is concerned by the clipping rectangle, which
     *         means if (part of) the section has been drawn
     */
    public boolean renderSelected (Graphics g);
}
