//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T e s t F a c a d e                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.facade;

import omr.util.BaseTestCase;
import omr.util.Dumping;

import java.awt.Point;
import java.awt.Rectangle;
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
 * Class {@code TestFacade}
 *
 * @author Hervé Bitteur
 */
public class TestFacade
        extends BaseTestCase
{
    //~ Instance fields ----------------------------------------------------------------------------

    private JAXBContext jaxbContext;

    private final File dir = new File("data/temp/test-facade");

    private final String fileName = "facade-data.xml";

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
        jaxbContext = JAXBContext.newInstance(MyClass.class);
    }

    private void marshall ()
            throws JAXBException, FileNotFoundException
    {
        File target = new File(dir, fileName);
        MyClass mc = new MyClass("TheName", new Point(10, 20), new Rectangle(2, 3, 40, 50));

        mc.lastId.incrementAndGet();
        mc.lastId.incrementAndGet();
        mc.lastId.incrementAndGet();
        mc.lastId.incrementAndGet();
        mc.lastId.incrementAndGet();

        mc.allEntities.put(560, "String for 560");
        mc.allEntities.put(780, "String for 780");

        new Dumping().dump(mc);

        System.out.println("Marshalling ...");

        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(mc, new FileOutputStream(target));
        System.out.println("Marshalled   to   " + target);
        m.marshal(mc, System.out);
    }

    private void unmarshall ()
            throws JAXBException, FileNotFoundException
    {
        System.out.println("================================================================");
        System.out.println("Unmarshalling ...");

        File source = new File(dir, fileName);
        Unmarshaller um = jaxbContext.createUnmarshaller();
        InputStream is = new FileInputStream(source);
        MyClass mc = (MyClass) um.unmarshal(is);
        System.out.println("Unmarshalled from " + source);
        new Dumping().dump(mc);
        System.out.println("Map class: " + mc.allEntities.getClass());
    }
}
