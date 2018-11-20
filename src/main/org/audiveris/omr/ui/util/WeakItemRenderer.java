//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  W e a k I t e m R e n d e r e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.ui.util;

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

    /**
     * Weak reference to the underlying renderer.
     */
    protected final WeakReference<ItemRenderer> weakRenderer;

    /**
     * Creates a new WeakItemRenderer object.
     *
     * @param renderer the actual renderer
     */
    public WeakItemRenderer (ItemRenderer renderer)
    {
        weakRenderer = new WeakReference<>(renderer);
    }

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
