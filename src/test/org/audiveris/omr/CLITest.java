//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          C l i T e s t                                         //
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
package org.audiveris.omr;

import org.audiveris.omr.step.Step;
import org.audiveris.omr.util.Dumping;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

import org.kohsuke.args4j.CmdLineException;

import java.util.Arrays;

/**
 *
 * @author Hervé Bitteur
 */
public class CLITest
{

    private final CLI instance = new CLI("AudiverisTest");

    /**
     * Creates a new {@code CLITest} object.
     */
    public CLITest ()
    {
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
        CLI.Parameters params = instance.parseParameters(args);
        new Dumping().dump(params);
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
    public void testRun ()
            throws Exception
    {
        System.out.println("\n+++ testRun");

        String[] args = new String[]{"-run", "org.audiveris.omr.step.RunClass"};
        CLI.Parameters params = instance.parseParameters(args);
        new Dumping().dump(params);
        assertNotNull("baratin", params.runClass);
    }

    @Test
    public void testRunError ()
            throws Exception
    {
        System.out.println("\n+++ testRunError");

        String[] args = new String[]{"-run", "fooBar"};

        try {
            instance.parseParameters(args);

            fail();
        } catch (CmdLineException ex) {
            System.out.println(ex.getMessage());
            System.out.println(ex.getLocalizedMessage());
            assertTrue(ex.getMessage().contains("java.lang.ClassNotFoundException"));
        }
    }

    @Test
    public void testSheets ()
            throws Exception
    {
        System.out.println("\n+++ testSheets");

        String[] args = new String[]{"-sheets", "3", "4", "6", "11 14"};
        CLI.Parameters params = instance.parseParameters(args);
        new Dumping().dump(params);
        assertEquals(Arrays.asList(3, 4, 6, 11, 14).toString(), params.getSheetIds().toString());
    }

    @Test
    public void testSheetsRange ()
            throws Exception
    {
        System.out.println("\n+++ testSheetsRange");

        String[] args = new String[]{"-sheets", "1", "3-6", "10"};
        CLI.Parameters params = instance.parseParameters(args);
        new Dumping().dump(params);
        assertEquals(Arrays.asList(1, 3, 4, 5, 6, 10).toString(), params.getSheetIds().toString());
    }

    @Test
    public void testSheetsRange2 ()
            throws Exception
    {
        System.out.println("\n+++ testSheetsRange2");

        String[] args = new String[]{"-sheets", "1", "4 - 6", "20"};
        CLI.Parameters params = instance.parseParameters(args);
        new Dumping().dump(params);
        assertEquals(Arrays.asList(1, 4, 5, 6, 20).toString(), params.getSheetIds().toString());
    }

    @Test
    public void testSome ()
            throws Exception
    {
        System.out.println("\n+++ testSome");

        String[] args = new String[]{
            "-help", "-batch", "-sheets", "5 2", " 3", "-step", "PAGE",
            "myScript.xml", "my Input.pdf"
        };
        CLI.Parameters params = instance.parseParameters(args);
        new Dumping().dump(params);
        assertEquals(true, params.batchMode);
        assertEquals(true, params.helpMode);
        assertEquals(Arrays.asList(2, 3, 5).toString(), params.getSheetIds().toString());
        assertEquals(Step.PAGE, params.step);
        assertEquals(2, params.arguments.size());
        assertEquals("myScript.xml", params.arguments.get(0).toString());
        assertEquals("my Input.pdf", params.arguments.get(1).toString());
    }

    @Test
    public void testStep ()
            throws Exception
    {
        System.out.println("\n+++ testStep");

        String[] args = new String[]{"-step", "PAGE"};
        CLI.Parameters params = instance.parseParameters(args);
        new Dumping().dump(params);
        assertEquals(Step.PAGE, params.step);
    }

    @Test
    public void testStepEmpty ()
            throws Exception
    {
        System.out.println("\n+++ testStepEmpty");

        String[] args = new String[]{"-step"};

        try {
            instance.parseParameters(args);

            fail();
        } catch (CmdLineException ex) {
            System.out.println(ex.getMessage());
            System.out.println(ex.getLocalizedMessage());
            assertTrue(ex.getMessage().contains("-step"));
        }
    }

    @Test
    public void testVoid ()
            throws Exception
    {
        System.out.println("\n+++ testVoid");

        String[] args = new String[0];
        CLI.Parameters params = instance.parseParameters(args);
        new Dumping().dump(params);
    }
}
