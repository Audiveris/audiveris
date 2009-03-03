//----------------------------------------------------------------------------//
//                                                                            //
//                         A u d i v e r i s T e s t                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Etiolles
 */
public class AudiverisTest
{
    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AudiverisTest object.
     */
    public AudiverisTest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    @BeforeClass
    public static void setUpClass ()
        throws Exception
    {
    }

    @AfterClass
    public static void tearDownClass ()
        throws Exception
    {
    }

    @Before
    public void setUp ()
    {
    }

    @After
    public void tearDown ()
    {
    }

    /**
     * Test of main method, of class Audiveris.
     */
    @Test
    public void testMultipleCalls ()
    {
        System.out.println("multipleCalls");

        String[] args = new String[] {
                            "-batch", "-step", "SYSTEMS", "examples/chula.png"
                        };
        System.out.println("firstCall");
        Audiveris.main(args);
        System.out.println("secondCall");
        Audiveris.main(args);
        System.out.println("Finished");

        // TODO review the generated test code and remove the default call to fail.
        //fail("The test case is a prototype.");
    }
}
