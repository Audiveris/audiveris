//----------------------------------------------------------------------------//
//                                                                            //
//                             B a s i c T e s t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
//----------------------------------------------------------------------------//
//
package omr.jaxb.basic;

import omr.util.BaseTestCase;
import omr.util.Dumper;
import static junit.framework.Assert.*;

import java.awt.Point;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

import javax.xml.bind.*;

/**
 * DOCUMENT ME!
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class BasicTest
    extends BaseTestCase
{
    //~ Methods ----------------------------------------------------------------

    public static void main (String... args)
    {
        new BasicTest().play(args[0]);
    }

    public void play (String fileName)
    {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Waiter.class);

            Waiter      waiter = new Waiter();
            waiter.id = 456;
            waiter.firstName = "Robert";
            waiter.location = new Point(1234, 5678);

            ArrayList<Day> days = new ArrayList<Day>();

            Day            d1 = new Day();
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

            waiter.purse = new Purse();

            waiter.titles = new String[] { "One", "Two", "Three" };

            waiter.results = new double[] { 1d, 2d, 4d, 67d };

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
            Dumper.dump(waiter);

            System.out.println("\ntips:");

            for (double e : waiter.purse.tips) {
                System.out.println(e);
            }

            System.out.println("Marshalling ...");

            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(waiter, new FileOutputStream(fileName + ".out.xml"));
            System.out.println("Marshalled to " + fileName + ".out.xml");
        } catch (JAXBException ex) {
            ex.printStackTrace();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void testMarshall ()
    {
        play("u:/soft/audiveris/src/test/omr/jaxb/basic/basic-data.xml");
    }
}
