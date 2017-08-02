//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     T e s t C o m p o u n d                                    //
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
 * Class {@code TestCompound}
 *
 * @author Hervé Bitteur
 */
public class TestCompound
        extends BaseTestCase
{
    //~ Instance fields ----------------------------------------------------------------------------

    private JAXBContext jaxbContext;

    private final File dir = new File("data/temp/test-itf");

    private final String indexFileName = "itf-compound.xml";

    //~ Methods ------------------------------------------------------------------------------------
    public void testInSequence ()
            throws JAXBException, FileNotFoundException, IOException, XMLStreamException
    {
        marshall();
        unmarshall();
    }

    @Override
    protected void setUp ()
            throws Exception
    {
        dir.mkdirs();
        jaxbContext = JAXBContext.newInstance(MyCompound.class);
    }

    private void marshall ()
            throws JAXBException, IOException, XMLStreamException
    {
        File target = new File(dir, indexFileName);
        Files.deleteIfExists(target.toPath());

        MyCompound compound = new MyCompound();
        compound.index = new MyBasicIndex<MyEntity>("my-index-name");

        compound.index.register(compound.topGlyph = new MyGlyph("First"));
        compound.index.register(compound.leftSymbol = new MySymbol(100));
        compound.index.register(compound.bottomGlyph = new MyGlyph("Second"));
        compound.index.register(new MyGlyph("Third"));
        compound.index.register(compound.rightSymbol = new MySymbol(200));

        new Dumping().dump(compound);
        new Dumping().dump(compound.index);
        System.out.println("compound.index: " + compound.index);

        System.out.println("Marshalling ...");
        Jaxb.marshal(compound, target.toPath(), jaxbContext);
        System.out.println("Marshalled   to   " + target);
        System.out.println("=========================================================");
        Jaxb.marshal(compound, System.out, jaxbContext);
    }

    private void unmarshall ()
            throws JAXBException, FileNotFoundException
    {
        System.out.println("=========================================================");
        System.out.println("Unmarshalling ...");

        File source = new File(dir, indexFileName);
        Unmarshaller um = jaxbContext.createUnmarshaller();
        InputStream is = new FileInputStream(source);

        MyCompound compound = (MyCompound) um.unmarshal(is);
        System.out.println("Unmarshalled from " + source);
        new Dumping().dump(compound);
        System.out.println("compound.index: " + compound.index);
    }
}
