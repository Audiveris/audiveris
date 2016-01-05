/*
 * Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
 * This software is released under the GNU General Public License.
 * Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
 */
package omr.sheet;

import omr.util.BaseTestCase;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Hervé Bitteur
 */
public class ExportPatternTest
        extends BaseTestCase
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code ExportPatternTest} object.
     */
    public ExportPatternTest ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Test
    public void testGetPathRadix ()
    {
        Path path = Paths.get("toto");
        System.out.println("input  = " + path);

        Path expResult = Paths.get("toto");
        Path result = ExportPattern.getPathSansExt(path);
        System.out.println("result = " + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetPathRadix_mvt_mxl ()
    {
        Path path = Paths.get("toto.mvt3.mxl");
        System.out.println("input  = " + path);

        Path expResult = Paths.get("toto");
        Path result = ExportPattern.getPathSansExt(path);
        System.out.println("result = " + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetPathRadix_mvt_xml ()
    {
        Path path = Paths.get("toto.mvt12.xml");
        System.out.println("input  = " + path);

        Path expResult = Paths.get("toto");
        Path result = ExportPattern.getPathSansExt(path);
        System.out.println("result = " + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetPathRadix_mxl ()
    {
        Path path = Paths.get("toto.mxl");
        System.out.println("input  = " + path);

        Path expResult = Paths.get("toto");
        Path result = ExportPattern.getPathSansExt(path);
        System.out.println("result = " + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetPathRadix_opus ()
    {
        Path path = Paths.get("toto.opus.mxl"); // Legal opus name
        System.out.println("input  = " + path);

        Path expResult = Paths.get("toto");
        Path result = ExportPattern.getPathSansExt(path);
        System.out.println("result = " + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetPathRadix_opus_xml ()
    {
        Path path = Paths.get("toto.opus.xml"); // NOT a legal opus name!
        System.out.println("input  = " + path);

        Path expResult = Paths.get("toto.opus");
        Path result = ExportPattern.getPathSansExt(path);
        System.out.println("result = " + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetPathRadix_other ()
    {
        Path path = Paths.get("toto.other");
        System.out.println("input  = " + path);

        Path expResult = Paths.get("toto.other");
        Path result = ExportPattern.getPathSansExt(path);
        System.out.println("result = " + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetPathRadix_xml ()
    {
        Path path = Paths.get("toto.xml");
        System.out.println("input  = " + path);

        Path expResult = Paths.get("toto");
        Path result = ExportPattern.getPathSansExt(path);
        System.out.println("result = " + result);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetPathRadix_xml_other ()
    {
        Path path = Paths.get("toto.xml.other");
        System.out.println("input  = " + path);

        Path expResult = Paths.get("toto.xml.other");
        Path result = ExportPattern.getPathSansExt(path);
        System.out.println("result = " + result);
        assertEquals(expResult, result);
    }
}
