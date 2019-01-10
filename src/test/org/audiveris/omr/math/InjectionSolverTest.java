//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             I n j e c t i o n S o l v e r T e s t                              //
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
package org.audiveris.omr.math;

import junit.framework.*;

/**
 *
 * @author Hervé Bitteur
 */
public class InjectionSolverTest
        extends TestCase
{

    /**
     * Creates a new InjectionSolverTest object.
     *
     * @param testName DOCUMENT ME!
     */
    public InjectionSolverTest (String testName)
    {
        super(testName);
    }

    /**
     * Test of solve method, of class omr.math.InjectionSolver.
     */
    public void testSolve ()
    {
        System.out.println("solve");

        InjectionSolver instance = new InjectionSolver(3, 3, new MyDistance());

        int[] expResult = new int[]{0, 1, 2};
        int[] result = instance.solve();

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
