//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T a b l e T e s t                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.table;

import omr.util.BaseTestCase;
import omr.util.Dumping;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Class {@code TableTest}
 *
 * @author Hervé Bitteur
 */
public class TableTest
        extends BaseTestCase
{
    //~ Instance fields ----------------------------------------------------------------------------

    private JAXBContext jaxbContext;

    private final File dir = new File("data/temp/table");

    private final String fileName = "table.xml";

    //~ Methods ------------------------------------------------------------------------------------
    public void testInSequence ()
            throws JAXBException, FileNotFoundException
    {
        marshall();
        unmarshall();
    }

    @Override
    protected void setUp ()
            throws Exception
    {
        dir.mkdirs();
        jaxbContext = JAXBContext.newInstance(Table.class);
    }

    private Table createTable ()
    {
        short[][] sequences = new short[][]{
            {0, 2, 5, 4},
            {10, 3, 5},
            {},
            {20, 3, 30}
        };

        return new Table(3, 5, sequences);
    }

    private void marshall ()
            throws JAXBException, FileNotFoundException
    {
        Table table = createTable();
        File target = new File(dir, fileName);

        new Dumping().dump(table);

        System.out.println("Marshalling ...");

        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        m.marshal(table, new FileOutputStream(target));
        System.out.println("Marshalled   to   " + target);
        System.out.println("=========================================================");
        m.marshal(table, System.out);
    }

    private void unmarshall ()
            throws JAXBException, FileNotFoundException
    {
        System.out.println("=========================================================");
        System.out.println("Unmarshalling ...");

        File source = new File(dir, fileName);
        InputStream is = new FileInputStream(source);
        Unmarshaller um = jaxbContext.createUnmarshaller();

        Table table = (Table) um.unmarshal(is);
        System.out.println("Unmarshalled from " + source);

        new Dumping().dump(table);
    }
}
