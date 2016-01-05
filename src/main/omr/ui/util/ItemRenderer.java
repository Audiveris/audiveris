//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      I t e m R e n d e r e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import java.awt.Graphics2D;

/**
 * Describes a class as able to render specific items of its own upon a provided
 * graphics environment.
 *
 * @author herve
 */
public interface ItemRenderer
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Render items on the provided graphics
     *
     * @param graphics the graphics environment
     */
    void renderItems (Graphics2D graphics);
}
