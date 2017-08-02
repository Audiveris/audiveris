//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T e s t F a c a d e                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.jaxb.facade;

import org.audiveris.omr.util.BaseTestCase;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.Jaxb;

import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLStreamException;

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
            throws JAXBException, IOException, XMLStreamException
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
            throws JAXBException, IOException, XMLStreamException
    {
        File target = new File(dir, fileName);
        Files.deleteIfExists(target.toPath());

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
        Jaxb.marshal(mc, target.toPath(), jaxbContext);
        System.out.println("Marshalled   to   " + target);
        Jaxb.marshal(mc, System.out, jaxbContext);
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
