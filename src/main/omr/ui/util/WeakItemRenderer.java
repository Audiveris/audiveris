//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  W e a k I t e m R e n d e r e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import java.awt.Graphics2D;
import java.lang.ref.WeakReference;

/**
 * Provides a way to weakly register a renderer.
 *
 * @author Hervé Bitteur
 */
public class WeakItemRenderer
        implements ItemRenderer
{
    //~ Instance fields ----------------------------------------------------------------------------

    protected final WeakReference<ItemRenderer> weakRenderer;

    //~ Constructors -------------------------------------------------------------------------------
    //------------------//
    // WeakItemRenderer //
    //------------------//
    /**
     * Creates a new WeakItemRenderer object.
     *
     * @param renderer the actual renderer
     */
    public WeakItemRenderer (ItemRenderer renderer)
    {
        weakRenderer = new WeakReference<ItemRenderer>(renderer);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // renderItems //
    //-------------//
    @Override
    public void renderItems (Graphics2D g)
    {
        ItemRenderer renderer = weakRenderer.get();

        if (renderer != null) {
            renderer.renderItems(g);
        }
    }
}
