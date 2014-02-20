//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         M a s k T e s t                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Mask instance = createInstance();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new MaskTest object.
     */
    public MaskTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
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
