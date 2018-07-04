//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              R u n T a b l e B i n d i n g T e s t                             //
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
package org.audiveris.omr.run;

import static org.audiveris.omr.run.Orientation.HORIZONTAL;
import org.audiveris.omr.util.BaseTestCase;
import org.audiveris.omr.util.Jaxb;

import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.stream.XMLStreamException;

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
    @Test
    public void testMarshalTable ()
            throws PropertyException, JAXBException, FileNotFoundException, IOException,
                   XMLStreamException
    {
        // Make sure target folder exists but target file does not exist
        dir.mkdirs();
        Files.deleteIfExists(fileTable.toPath());

        // Generate JAXB context
        jaxbContext = JAXBContext.newInstance(RunTable.class);

        RunTable table = createHorizontalInstance();
        table.dumpSequences();
        System.out.println("table: " + table.dumpOf());
        Jaxb.marshal(table, fileTable.toPath(), jaxbContext);
        System.out.println("Marshalled to " + fileTable);
        System.out.println("===========================================");

        Jaxb.marshal(table, System.out, jaxbContext);
        System.out.println();

        RunTable newTable = (RunTable) Jaxb.unmarshal(fileTable.toPath(), jaxbContext);
        System.out.println("===========================================");
        System.out.println("Unmarshalled from " + fileTable);

        table.dumpSequences();
        System.out.println("table: " + table.dumpOf());

        newTable.dumpSequences();
        System.out.println("newTable: " + newTable.dumpOf());

        assertEquals(table.dumpOf(), newTable.dumpOf());
        assertEquals(table, newTable);
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
