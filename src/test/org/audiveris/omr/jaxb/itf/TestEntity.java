//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T e s t E n t i t y                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.jaxb.itf;

import org.audiveris.omr.util.BaseTestCase;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.Jaxb;

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
 * Class {@code TestEntity}
 *
 * @author Hervé Bitteur
 */
public class TestEntity
        extends BaseTestCase
{

    private JAXBContext jaxbContext;

    private final File dir = new File("data/temp/test-itf");

    private final String indexFileName = "itf-index.xml";

    public void testInSequence ()
            throws JAXBException,
                   FileNotFoundException,
                   IOException,
                   XMLStreamException
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
            throws JAXBException,
                   FileNotFoundException,
                   IOException,
                   XMLStreamException
    {
        File target = new File(dir, indexFileName);
        Files.deleteIfExists(target.toPath());

        MyBasicIndex<MyEntity> index = new MyBasicIndex<MyEntity>("my-index-name");

        index.register(new MyGlyph("First"));
        index.register(new MySymbol(100));
        index.register(new MyGlyph("Second"));
        index.register(new MyGlyph("Third"));
        index.register(new MySymbol(200));

        new Dumping().dump(index);
        System.out.println("index: " + index);

        System.out.println("Marshalling ...");
        Jaxb.marshal(index, target.toPath(), jaxbContext);
        System.out.println("Marshalled   to   " + target);
        System.out.println("=========================================================");
        Jaxb.marshal(index, System.out, jaxbContext);
    }

    private void unmarshall ()
            throws JAXBException,
                   FileNotFoundException,
                   IOException
    {
        System.out.println("=========================================================");
        System.out.println("Unmarshalling ...");

        File source = new File(dir, indexFileName);
        Unmarshaller um = jaxbContext.createUnmarshaller();
        InputStream is = new FileInputStream(source);

        MyBasicIndex index = (MyBasicIndex) um.unmarshal(is);
        is.close();
        System.out.println("Unmarshalled from " + source);
        new Dumping().dump(index);
        System.out.println("index: " + index);
    }
}
