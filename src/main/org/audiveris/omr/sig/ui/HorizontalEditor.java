//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 H o r i z o n t a l E d i t o r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sig.inter.Inter;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class <code>HorizontalEditor</code> provides just one handle to globally move the inter
 * horizontally only.
 *
 * @author Hervé Bitteur
 */
public class HorizontalEditor
        extends InterEditor
{
    //~ Instance fields ----------------------------------------------------------------------------

    // Original data
    private final Rectangle originalBounds;

    // Latest data
    private final Rectangle latestBounds;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>HorizontalEditor</code> object.
     *
     * @param inter DOCUMENT ME!
     */
    public HorizontalEditor (final Inter inter)
    {
        super(inter);

        originalBounds = inter.getBounds();
        latestBounds = inter.getBounds();

        // Middle handle: move horizontally only
        handles.add(
                selectedHandle = new Handle(inter.getCenter())
        {
            @Override
            public boolean move (Point vector)
            {
                final double dx = vector.getX();

                if (dx == 0) {
                    return false;
                }

                // Data
                latestBounds.x += dx;

                // Handle
                for (Handle handle : handles) {
                    PointUtil.add(handle.getHandleCenter(), dx, 0);
                }

                return true;
            }
        });
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    protected void doit ()
    {
        inter.setBounds(latestBounds);

        super.doit(); // No more glyph
    }

    @Override
    public void undo ()
    {
        inter.setBounds(originalBounds);

        super.undo();
    }
}
