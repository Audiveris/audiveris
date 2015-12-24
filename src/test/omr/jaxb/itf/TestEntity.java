//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T e s t E n t i t y                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.itf;

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
 * Class {@code TestEntity}
 *
 * @author Hervé Bitteur
 */
public class TestEntity
        extends BaseTestCase
{
    //~ Instance fields ----------------------------------------------------------------------------

    private JAXBContext jaxbContext;

    private final File dir = new File("data/temp/test-itf");

    private final String indexFileName = "itf-index.xml";

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
        jaxbContext = JAXBContext.newInstance(MyBasicIndex.class);
    }

    private void marshall ()
            throws JAXBException, FileNotFoundException
    {
        File target = new File(dir, indexFileName);

        MyBasicIndex<MyEntity> index = new MyBasicIndex<MyEntity>("my-index-name");

        index.register(new MyGlyph("First"));
        index.register(new MySymbol(100));
        index.register(new MyGlyph("Second"));
        index.register(new MyGlyph("Third"));
        index.register(new MySymbol(200));

        new Dumping().dump(index);
        System.out.println("index: " + index);

        System.out.println("Marshalling ...");

        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        m.marshal(index, new FileOutputStream(target));
        System.out.println("Marshalled   to   " + target);
        System.out.println("=========================================================");
        m.marshal(index, System.out);
    }

    private void unmarshall ()
            throws JAXBException, FileNotFoundException
    {
        System.out.println("=========================================================");
        System.out.println("Unmarshalling ...");

        File source = new File(dir, indexFileName);
        Unmarshaller um = jaxbContext.createUnmarshaller();
        InputStream is = new FileInputStream(source);

        MyBasicIndex index = (MyBasicIndex) um.unmarshal(is);
        System.out.println("Unmarshalled from " + source);
        new Dumping().dump(index);
        System.out.println("index: " + index);
    }
}
