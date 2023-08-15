//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    D e f a u l t E d i t o r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import java.awt.Rectangle;

/**
 * Class <code>DefaultEditor</code> provides just one handle to globally move the inter in
 * any direction.
 */
public class DefaultEditor
        extends InterEditor
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final Rectangle originalBounds;

    private final Rectangle latestBounds;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>DefaultEditor</code> object.
     *
     * @param inter the inter being edited
     */
    public DefaultEditor (final Inter inter)
    {
        super(inter);

        originalBounds = inter.getBounds();
        latestBounds = inter.getBounds();

        handles.add(selectedHandle = new Handle(inter.getCenter())
        {
            @Override
            public boolean move (int dx,
                                 int dy)
            {
                // Data
                latestBounds.x += dx;
                latestBounds.y += dy;

                // Handle
                for (Handle handle : handles) {
                    PointUtil.add(handle.getPoint(), dx, dy);
                }

                return true;
            }
        });
    }

    //~ Methods ------------------------------------------------------------------------------------

    @Override
    protected void doit ()
    {
        getInter().setBounds(latestBounds);
        super.doit(); // No more glyph
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("DefaultEditor{");
        sb.append(getInter());

        Rectangle b = latestBounds;
        sb.append(String.format(" latestBounds(x:%d,y:%d,w:%d,h:%d)", b.x, b.y, b.width, b.height));

        sb.append('}');

        return sb.toString();
    }

    @Override
    public void undo ()
    {
        getInter().setBounds(originalBounds);
        super.undo();
    }
}
