//----------------------------------------------------------------------------//
//                                                                            //
//                         A l i g n m e n t T e s t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.ui.symbol.Alignment.Horizontal;
import static omr.ui.symbol.Alignment.Horizontal.*;
import omr.ui.symbol.Alignment.Vertical;
import static omr.ui.symbol.Alignment.Vertical.*;
import static org.junit.Assert.*;
import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class {@code AlignmentTest}
 *
 * @author Hervé Bitteur
 */
public class AlignmentTest
{
    //~ Instance fields --------------------------------------------------------

    /** Map Alignment -> Point */
    Map<Alignment, Point> points = new HashMap<>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlignmentTest object.
     */
    public AlignmentTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Test of toPoint method, of class Alignment.
     */
    @Test
    public void testToPoint ()
    {
        System.out.println("toPoint");

        Rectangle rect = new Rectangle(-6, -26, 50, 38);
        assignPoints();

        for (Vertical vert : Vertical.values()) {
            for (Horizontal hori : Horizontal.values()) {
                Alignment instance = new Alignment(vert, hori);
                Point     start = points.get(instance);

                for (Vertical v : Vertical.values()) {
                    for (Horizontal h : Horizontal.values()) {
                        Alignment expAlign = new Alignment(v, h);
                        Point     to = instance.toPoint(expAlign, rect);
                        Point     target = new Point(start);
                        target.translate(to.x, to.y);

                        System.out.print(
                            instance + " + " + to + " = " + target);

                        Alignment align = getAlign(target);
                        Point     expTarget = points.get(expAlign);

                        System.out.println("  " + expAlign + " =? " + align);
                        assertEquals("Different points", expTarget, target);
                        assertEquals("Different aligns", expAlign, align);
                    }
                }

                System.out.println();
            }
        }
    }

    //    /**
    //     * Test of toPoint method, of class Alignment.
    //     */
    //    @Test
    //    public void testToPoint2D ()
    //    {
    //        System.out.println("toPoint2D");
    //
    //        Rectangle2D rect = new Rectangle2D.Float(-5.8f, -26.0f, 50.0f, 37.4f);
    //        Point2D     expTo = null;
    //
    //        for (Vertical vert : Vertical.values()) {
    //            for (Horizontal hori : Horizontal.values()) {
    //                Alignment instance = new Alignment(vert, hori);
    //
    //                for (Vertical v : Vertical.values()) {
    //                    for (Horizontal h : Horizontal.values()) {
    //                        Alignment that = new Alignment(v, h);
    //                        Point2D   to = instance.toPoint(that, rect);
    //
    //                        System.out.println(
    //                            instance + " + " + to + " = " + that);
    //                    }
    //                }
    //
    //                System.out.println();
    //            }
    //        }
    //    }
    private Alignment getAlign (Point target)
    {
        for (Entry<Alignment, Point> entry : points.entrySet()) {
            if (entry.getValue()
                     .equals(target)) {
                return entry.getKey();
            }
        }

        return null;
    }

    private void assignPoints ()
    {
        points.put(new Alignment(TOP, LEFT), new Point(-6, -26));
        points.put(new Alignment(TOP, CENTER), new Point(19, -26));
        points.put(new Alignment(TOP, RIGHT), new Point(44, -26));
        points.put(new Alignment(TOP, XORIGIN), new Point(0, -26));

        points.put(new Alignment(MIDDLE, LEFT), new Point(-6, -7));
        points.put(new Alignment(MIDDLE, CENTER), new Point(19, -7));
        points.put(new Alignment(MIDDLE, RIGHT), new Point(44, -7));
        points.put(new Alignment(MIDDLE, XORIGIN), new Point(0, -7));

        points.put(new Alignment(BOTTOM, LEFT), new Point(-6, 12));
        points.put(new Alignment(BOTTOM, CENTER), new Point(19, 12));
        points.put(new Alignment(BOTTOM, RIGHT), new Point(44, 12));
        points.put(new Alignment(BOTTOM, XORIGIN), new Point(0, 12));

        points.put(new Alignment(BASELINE, LEFT), new Point(-6, 0));
        points.put(new Alignment(BASELINE, CENTER), new Point(19, 0));
        points.put(new Alignment(BASELINE, RIGHT), new Point(44, 0));
        points.put(new Alignment(BASELINE, XORIGIN), new Point(0, 0));
    }
}
