//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          C l i T e s t                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr;

import omr.step.Steps;

import omr.util.Dumping;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import junit.framework.TestCase;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.kohsuke.args4j.CmdLineException;

import java.util.Arrays;
import java.util.Locale;

/**
 * Unitary tests for CLI.
 *
 * @author Hervé Bitteur
 */
public class CLITest
        extends TestCase
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final CLI instance = new CLI("AudiverisTest");

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code CLITest} object.
     */
    public CLITest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Test
    public void testInput ()
            throws Exception
    {
        System.out.println("\n+++ testInput");

        String[] args = new String[]{"-input", "myInput#1.pdf", "-input", "myInput#2.pdf"};
        CLI.Parameters params = instance.getParameters(args);
        new Dumping().dump(params);
        assertEquals(Arrays.asList("myInput#1.pdf", "myInput#2.pdf"), params.inputFiles);
    }

    @Test
    public void testInputMissing ()
            throws Exception
    {
        System.out.println("\n+++ testInputMissing");
        Locale.setDefault(Locale.GERMAN);

        String[] args = new String[]{"-input"};

        try {
            CLI.Parameters params = instance.getParameters(args);

            fail();
        } catch (CmdLineException ex) {
            System.out.println(ex.getMessage());
            System.out.println(ex.getLocalizedMessage());
            assertTrue(ex.getMessage().contains("-input"));
        }
    }

    @Test
    public void testOption ()
            throws Exception
    {
        System.out.println("\n+++ testOption");

        String[] args = new String[]{
            "-option", "omr.toto  :  totoValue", "-option",
            "omr.ui.tata=tataValue", "-option", "keyWithNoValue", "-option",
            "myKey : my value"
        };
        CLI.Parameters params = instance.getParameters(args);
        new Dumping().dump(params);
    }

    @Test
    public void testPages ()
            throws Exception
    {
        System.out.println("\n+++ testPages");

        String[] args = new String[]{"-pages", "3", "4", "6"};
        CLI.Parameters params = instance.getParameters(args);
        new Dumping().dump(params);
        assertEquals(Arrays.asList(3, 4, 6).toString(), params.getPageIds().toString());
    }

    @Test
    public void testPrintCommandLine ()
    {
        System.out.println("\n+++ printCommandLine");
        instance.printCommandLine();
    }

    @Test
    public void testPrintUsage ()
    {
        System.out.println("\n+++ printUsage");
        instance.printUsage();
    }

    @Test
    public void testSome ()
            throws Exception
    {
        System.out.println("\n+++ testSome");

        String[] args = new String[]{
            "-help", "-batch", "-script", "myScript.xml", "-input",
            "my Input.pdf", "-pages", "5 2", " 3", "-steps"
        };
        CLI.Parameters params = instance.getParameters(args);
        new Dumping().dump(params);
        assertEquals(true, params.batchMode);
        assertEquals(true, params.helpMode);
        assertEquals("myScript.xml", params.scriptFiles.get(0));
        assertEquals("my Input.pdf", params.inputFiles.get(0));
        assertEquals(Arrays.asList(2, 3, 5).toString(), params.getPageIds().toString());
        assertEquals(0, params.getSteps().size());
    }

    @Test
    public void testSteps ()
            throws Exception
    {
        System.out.println("\n+++ testSteps");

        String[] args = new String[]{"-steps", "RHYTHMS", "PAGE"};
        CLI.Parameters params = instance.getParameters(args);
        new Dumping().dump(params);
        assertEquals(
                Arrays.asList(Steps.valueOf("RHYTHMS"), Steps.valueOf("PAGE")).toString(),
                params.getSteps().toString());
    }

    @Test
    public void testVoid ()
            throws Exception
    {
        System.out.println("\n+++ testVoid");

        String[] args = new String[0];
        CLI.Parameters params = instance.getParameters(args);
        new Dumping().dump(params);
    }
}
