//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T a b l e T e s t                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.jaxb.table;

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
            throws JAXBException, FileNotFoundException, XMLStreamException, IOException
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
            throws JAXBException, FileNotFoundException, XMLStreamException, IOException
    {
        Table table = createTable();
        File target = new File(dir, fileName);
        Files.deleteIfExists(target.toPath());

        new Dumping().dump(table);

        System.out.println("Marshalling ...");
        Jaxb.marshal(table, target.toPath(), jaxbContext);
        System.out.println("Marshalled   to   " + target);
        System.out.println("=========================================================");
        Jaxb.marshal(table, System.out, jaxbContext);
    }

    private void unmarshall ()
            throws JAXBException, FileNotFoundException, IOException
    {
        System.out.println("=========================================================");
        System.out.println("Unmarshalling ...");

        File source = new File(dir, fileName);
        InputStream is = new FileInputStream(source);
        Unmarshaller um = jaxbContext.createUnmarshaller();

        Table table = (Table) um.unmarshal(is);
        is.close();

        System.out.println("Unmarshalled from " + source);

        new Dumping().dump(table);
    }
}
