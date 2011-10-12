//----------------------------------------------------------------------------//
//                                                                            //
//                   I n j e c t i o n S o l v e r T e s t                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import junit.framework.*;

/**
 *
 * @author Etiolles
 */
public class InjectionSolverTest
    extends TestCase
{
    //~ Constructors -----------------------------------------------------------

    public InjectionSolverTest (String testName)
    {
        super(testName);
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * Test of solve method, of class omr.math.InjectionSolver.
     */
    public void testSolve ()
    {
        System.out.println("solve");

        InjectionSolver instance = new InjectionSolver(3, 3, new MyDistance());

        int[]     expResult = new int[] { 0, 1, 2 };
        int[]     result = instance.solve();

        //assertEquals(expResult, result);
    }

    protected void setUp ()
        throws Exception
    {
    }

    protected void tearDown ()
        throws Exception
    {
    }

    //~ Inner Classes ----------------------------------------------------------

    public static class MyDistance
        implements InjectionSolver.Distance
    {
        public MyDistance ()
        {
        }

        public int getDistance (int in,
                                int ip)
        {
            return Math.abs((1 + in) - ip);
        }
    }
}
