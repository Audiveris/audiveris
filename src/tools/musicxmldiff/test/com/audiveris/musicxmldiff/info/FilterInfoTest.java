//----------------------------------------------------------------------------//
//                                                                            //
//                         F i l t e r I n f o T e s t                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.musicxmldiff.info;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;
import com.audiveris.musicxmldiff.Tolerance;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Hervé Bitteur
 */
public class FilterInfoTest
{

    private static FilterInfo reference;

    public FilterInfoTest ()
    {
    }

    @BeforeClass
    public static void setUpClass ()
    {
    }

    @AfterClass
    public static void tearDownClass ()
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
     * Test of marshall method, of class FilterInfo.
     */
    @Test
    public void testEmptyMarshall () throws Exception
    {
        System.out.println("empty-marshall");
        File output = new File("target/filter-test.empty.xml");

        FilterInfo instance = new FilterInfo(null, null);
        try (OutputStream os = new FileOutputStream(output)) {
            instance.marshall(os);
        }
    }

    /**
     * Test of marshall method, of class FilterInfo.
     */
    @Test
    public void testNoElemMarshall () throws Exception
    {
        System.out.println("noelem-marshall");
        File output = new File("target/filter-test.noelem.xml");

        Map<String, DiffInfo> diffs = new TreeMap<>();
        DiffInfo diff1 = new DiffInfo("number of child nodes", true);
        diffs.put(diff1.getName(), diff1);

        FilterInfo instance = new FilterInfo(diffs, null);
        try (OutputStream os = new FileOutputStream(output)) {
            instance.marshall(os);
        }
    }

    /**
     * Test of marshall method, of class FilterInfo.
     */
    @Test
    public void testMarshall () throws Exception
    {
        System.out.println("marshall");
        File output = new File("target/filter-test.out.xml");

        Map<String, DiffInfo> diffs = new TreeMap<>();
        DiffInfo diff1 = new DiffInfo("number of child nodes", true);
        diffs.put(diff1.getName(), diff1);

        Map<String, AttrInfo> attrs = new TreeMap<>();
        AttrInfo attr1 = new AttrInfo(
                "default-x", null, new Tolerance.Ratio(0.02), null);
        attrs.put(attr1.getName(), attr1);
        AttrInfo attr2 = new AttrInfo(
                "default-y", null, new Tolerance.Ratio(0.02), new Tolerance.Delta(5));
        attrs.put(attr2.getName(), attr2);

        Map<String, ElemInfo> elems = new TreeMap<>();
        ElemInfo elem1 = new ElemInfo("*", true, null, null, attrs);
        elems.put(elem1.getName(), elem1);

        FilterInfo instance = new FilterInfo(diffs, elems);
        try (OutputStream os = new FileOutputStream(output)) {
            instance.marshall(os);
        }
    }

    /**
     * Test of unmarshall method, of class FilterInfo.
     */
    private void testUnmarshall () throws Exception
    {
        System.out.println("unmarshall");

//        String path = "/filter-test.in.xml";
//        InputStream inputStream = getClass().getResourceAsStream(path);

        String path = "config/filter-test.in.xml";
        InputStream inputStream = new FileInputStream(path);

//        FilterInfo expResult = null;
        FilterInfo filterInfo = FilterInfo.unmarshall(inputStream);
        System.out.println("Unmarshalled: " + filterInfo);

        reference = filterInfo;
    }

    /**
     * Test of marshall method, of class FilterInfo.
     */
    private void testReMarshall () throws Exception
    {
        System.out.println("re-marshall");
        File output = new File("target/filter-test.copy.xml");

        FilterInfo instance = reference;
        System.out.println("Using instance: " + reference);
        try (OutputStream os = new FileOutputStream(output)) {
            instance.marshall(os);
        }
    }

    /**
     * Test of unmarshall then (re) marchall, of class FilterInfo.
     */
    @Test
    public void testBoth () throws Exception
    {
        System.out.println("testBoth");
        testUnmarshall();
        testReMarshall();
    }
}
