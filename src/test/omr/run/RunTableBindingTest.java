//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              R u n T a b l e B i n d i n g T e s t                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import static omr.run.Orientation.HORIZONTAL;

import omr.util.BaseTestCase;

import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;

/**
 * Class {@code RunTableBindingTest} tests the (un-)marshalling of RunTable.
 *
 * @author Hervé Bitteur
 */
public class RunTableBindingTest
        extends BaseTestCase
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final File dir = new File("data/temp");

    private static final Dimension dim = new Dimension(10, 5);

    //~ Instance fields ----------------------------------------------------------------------------
    private final File fileTable = new File(dir, "runtable.xml");

    private JAXBContext jaxbContext;

    //~ Methods ------------------------------------------------------------------------------------
    @BeforeClass
    public static void createTempFolder ()
    {
        dir.mkdirs();
    }

    @Test
    public void testMarshalTable ()
            throws PropertyException, JAXBException, FileNotFoundException
    {
        RunTable table = createHorizontalInstance();
        table.dumpSequences();
        System.out.println("table: " + table.dumpOf());

        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(table, new FileOutputStream(fileTable));
        System.out.println("Marshalled to " + fileTable);
        System.out.println("===========================================");
        m.marshal(table, System.out);

        Unmarshaller um = jaxbContext.createUnmarshaller();
        InputStream is = new FileInputStream(fileTable);
        RunTable newTable = (RunTable) um.unmarshal(is);
        System.out.println("===========================================");
        System.out.println("Unmarshalled from " + fileTable);

        table.dumpSequences();
        System.out.println("table: " + table.dumpOf());

        newTable.dumpSequences();
        System.out.println("newTable: " + newTable.dumpOf());

        assertEquals(table.dumpOf(), newTable.dumpOf());
        assertEquals(table, newTable);
    }

    @Override
    protected void setUp ()
            throws JAXBException
    {
        jaxbContext = JAXBContext.newInstance(RunTable.class);
    }

    //--------------------------//
    // createHorizontalInstance //
    //--------------------------//
    private RunTable createHorizontalInstance ()
    {
        RunTable instance = new RunTable(HORIZONTAL, dim.width, dim.height);

        instance.addRun(0, new Run(1, 2));
        instance.addRun(0, new Run(5, 3));

        instance.addRun(1, new Run(0, 1));
        instance.addRun(1, new Run(4, 2));

        // Leave sequence empty at index 2
        //
        instance.addRun(3, new Run(0, 2));
        instance.addRun(3, new Run(4, 1));
        instance.addRun(3, new Run(8, 2));

        instance.addRun(4, new Run(2, 2));
        instance.addRun(4, new Run(6, 4));

        System.out.println("createHorizontalInstance:\n" + instance.dumpOf());

        return instance;
    }
}
