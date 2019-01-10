//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         M a s k T e s t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.image;

import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.geom.Area;
import java.awt.geom.Path2D;

/**
 *
 * @author herve
 */
public class MaskTest
{

    private static final Mask instance = createInstance();

    /**
     * Creates a new MaskTest object.
     */
    public MaskTest ()
    {
    }

    /**
     * Test of apply method, of class Mask.
     */
    @Test
    public void testApply ()
    {
        System.out.println("apply");

        Mask.Adapter adapter = new Mask.Adapter()
        {
            @Override
            public void process (int x,
                                 int y)
            {
                System.out.printf("x:%d y:%d%n", x, y);
            }
        };

        instance.apply(adapter);
    }

    /**
     * Test of getPointCount method, of class Mask.
     */
    @Test
    public void testGetPointCount ()
    {
        System.out.println("getPointCount");

        int expResult = 6;
        int result = instance.getPointCount();
        assertEquals(expResult, result);
    }

    private static Mask createInstance ()
    {
        final Path2D path = new Path2D.Double();
        path.moveTo(10, 20);
        path.lineTo(13, 20);
        path.lineTo(13, 22);
        path.lineTo(10, 22);
        path.closePath();

        return new Mask(new Area(path));
    }
}
