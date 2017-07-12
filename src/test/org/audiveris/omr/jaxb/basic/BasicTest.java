//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B a s i c T e s t                                        //
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
package org.audiveris.omr.jaxb.basic;

import org.audiveris.omr.util.BaseTestCase;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.Jaxb;

import java.awt.Point;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.bind.*;
import javax.xml.stream.XMLStreamException;

/**
 * DOCUMENT ME!
 *
 * @author Hervé Bitteur
 */
public class BasicTest
        extends BaseTestCase
{
    //~ Methods ------------------------------------------------------------------------------------

    public static void main (String... args)
            throws JAXBException, FileNotFoundException, IOException, XMLStreamException
    {
        new BasicTest().play(args[0]);
    }

    public void play (String fileName)
            throws JAXBException, FileNotFoundException, IOException, XMLStreamException
    {
        JAXBContext jaxbContext = JAXBContext.newInstance(Waiter.class);

        Waiter waiter = new Waiter();
        waiter.id = 456;
        waiter.firstName = "Robert";
        waiter.location = new Point(1234, 5678);

        ArrayList<Day> days = new ArrayList<Day>();

        Day d1 = new Day();
        d1.label = Weekday.FRIDAY;

        Meeting m1 = new Meeting(8, 9);
        Meeting m2 = new Meeting(10, 12);
        d1.meetings = Arrays.asList(m1, m2);

        Day d2 = new Day();
        d2.label = Weekday.THURSDAY;

        Meeting m3 = new Meeting(8, 9);
        Meeting m4 = new Meeting(15, 17);
        d2.meetings = Arrays.asList(m3, m4);

        waiter.setDays(Arrays.asList(d1, d2));

        waiter.path = new File("PathToMyFile.txt");

        waiter.purse = new Purse();

        waiter.titles = new String[]{"One", "Two", "Three"};

        waiter.results = new double[]{1d, 2d, 4d, 67d};

        double[][] matrix = new double[2][];

        for (int row = 0; row < matrix.length; row++) {
            double[] vector = new double[3];
            matrix[row] = vector;

            for (int col = 0; col < vector.length; col++) {
                vector[col] = (row * 10) + col;
            }
        }

        waiter.mat = matrix;

        //            Unmarshaller um;
        //            um = jaxbContext.createUnmarshaller();
        //
        //            Waiter waiter = (Waiter) um.unmarshal(new File(fileName));
        new Dumping().dump(waiter);

        System.out.println("\ntips:");

        for (double e : waiter.purse.tips) {
            System.out.println(e);
        }

        System.out.println("Marshalling ...");
        Jaxb.marshal(waiter, Paths.get(fileName), jaxbContext);
        System.out.println("Marshalled to " + fileName);
    }

    public void testMarshall ()
            throws JAXBException, FileNotFoundException, IOException, XMLStreamException
    {
        File dir = new File("data/temp");
        dir.mkdirs();
        play(new File(dir, "basic-data.xml").toString());
    }
}
