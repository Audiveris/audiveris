//----------------------------------------------------------------------------//
//                                                                            //
//                         A u d i v e r i s T e s t                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for multiple calls of Audiveris
 * @author Hervé Bitteur
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
        System.out.println("testMultipleCalls");

        String[] args1 = new String[] {
                             "-batch", "-step", "EXPORT", "-input", "examples/chula.png"
                         };
        String[] args2 = new String[] {
                             "-batch", "-step", "EXPORT", "-input", "examples/batuque.png",
                             "examples/allegretto.png"
                         };
        System.out.println("firstCall to Audiveris.main()");
        Logger.getAnonymousLogger().log(Level.INFO, "firstCall to Audiveris.main()");
        Audiveris.main(args1);
        System.out.println("secondCall to Audiveris.main()");
        Logger.getAnonymousLogger().log(Level.INFO, "secondCall to Audiveris.main()");
        Audiveris.main(args2);
        System.out.println("finished");
    }
}
