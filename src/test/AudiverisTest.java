//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   A u d i v e r i s T e s t                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test for multiple calls of Audiveris
 *
 * @author Hervé Bitteur
 */
public class AudiverisTest
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AudiverisTest.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new AudiverisTest object.
     */
    public AudiverisTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Before
    public void setUp ()
    {
    }

    @BeforeClass
    public static void setUpClass ()
            throws Exception
    {
    }

    @After
    public void tearDown ()
    {
    }

    @AfterClass
    public static void tearDownClass ()
            throws Exception
    {
    }

    /**
     * Test of main method, of class Audiveris.
     */
    @Test
    public void testMultipleCalls ()
    {
        System.out.println("testMultipleCalls");

        String[] args1 = new String[]{
            "-batch", "-step", "EXPORT", "-input", "data/examples/chula.png"
        };
        String[] args2 = new String[]{
            "-batch", "-step", "EXPORT", "-input", "data/examples/batuque.png",
            "data/examples/allegretto.png"
        };
//        System.out.println("firstCall to Audiveris.main()");
//        logger.info("firstCall to Audiveris.main()");
//        Audiveris.main(args1);
//        System.out.println("secondCall to Audiveris.main()");
//        logger.info("secondCall to Audiveris.main()");
//        Audiveris.main(args2);
//        System.out.println("finished");
    }
}
