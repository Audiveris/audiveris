//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  W e a k I t e m R e n d e r e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
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
