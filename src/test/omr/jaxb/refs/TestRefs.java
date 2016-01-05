//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         T e s t R e f s                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.refs;

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
 * Class {@code TestRefs}
 *
 * @author Hervé Bitteur
 */
public class TestRefs
        extends BaseTestCase
{
    //~ Instance fields ----------------------------------------------------------------------------

    private JAXBContext jaxbContext;

    private final File dir = new File("data/temp/test-refs");

    private final String fileName = "universe.xml";

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
        jaxbContext = JAXBContext.newInstance(Universe.class);
    }

    private void marshall ()
            throws JAXBException, FileNotFoundException
    {
        Universe universe = new Universe();
        File target = new File(dir, fileName);

        Store store = universe.store;
        Basket basket = universe.basket;

        Orange orange;
        Apple apple;

        orange = new Orange("O10", "orange 10");
        store.add(orange);
        basket.add(orange);

        apple = new Apple("A1", "pomme 1");
        store.add(apple);
        basket.add(apple);

        apple = new Apple("A2", "pomme 2");
        store.add(apple);
        basket.add(apple);

        apple = new Apple("A30", "pomme 30");
        store.add(apple);
        basket.add(apple);

        orange = new Orange("O1", "orange 1");
        store.add(orange);
        basket.add(orange);

        orange = new Orange("O2", "orange 2");
        store.add(orange);
        basket.add(orange);

        orange = new Orange("O3", "orange 3");

        store.add(orange);
        basket.add(orange);

        new Dumping().dump(universe);
        new Dumping().dump(store);
        new Dumping().dump(basket);

        System.out.println("Marshalling ...");

        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        m.marshal(universe, new FileOutputStream(target));
        System.out.println("Marshalled   to   " + target);
        System.out.println("=========================================================");
        m.marshal(universe, System.out);
    }

    private void unmarshall ()
            throws JAXBException, FileNotFoundException
    {
        System.out.println("=========================================================");
        System.out.println("Unmarshalling ...");

        File source = new File(dir, fileName);
        InputStream is = new FileInputStream(source);
        Unmarshaller um = jaxbContext.createUnmarshaller();

        //        MyIDResolver resolver = new MyIDResolver();
        //        um.setProperty(IDResolver.class.getName(), resolver);
        //        um.setListener(resolver.createListener());
        Universe universe = (Universe) um.unmarshal(is);
        System.out.println("Unmarshalled from " + source);

        new Dumping().dump(universe);
        new Dumping().dump(universe.store);
        new Dumping().dump(universe.basket);

        for (Fruit fRef : universe.store.fruits) {
            System.out.println("==================================");
            System.out.print("   " + fRef);

            if (fRef instanceof Apple) {
                Apple apple = (Apple) fRef;
                System.out.println(" apple:" + apple.name);
            }

            if (fRef instanceof Orange) {
                Orange orange = (Orange) fRef;
                System.out.println(" orange: " + orange.name);
            }
        }
    }
}
