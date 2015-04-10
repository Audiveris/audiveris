//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              R u n T a b l e B i n d i n g T e s t                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import static omr.run.Orientation.HORIZONTAL;

import omr.util.BaseTestCase;

import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import omr.util.Dumping;

/**
 * Class {@code RunTableBindingTest} tests the (un-)marshaling of RunTable.
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
    private final File fileSequence = new File(dir, "runsequence.xml");

    private final File fileTable = new File(dir, "runtable.xml");

    private JAXBContext jaxbContext;

    //~ Methods ------------------------------------------------------------------------------------
    @BeforeClass
    public static void createTempFolder ()
    {
        dir.mkdirs();
    }

    @Test
    public void testMarshalSequence ()
            throws PropertyException, JAXBException, FileNotFoundException
    {
        RunSequence sequence = new BasicRunSequence();
        sequence.add(new Run(1, 2));
        sequence.add(new Run(5, 3));
        System.out.println("sequence: " + sequence);

        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(sequence, new FileOutputStream(fileSequence));
        System.out.println("Marshalled to " + fileSequence);
        m.marshal(sequence, System.out);

        Unmarshaller um = jaxbContext.createUnmarshaller();
        InputStream is = new FileInputStream(fileSequence);
        RunSequence newSequence = (RunSequence) um.unmarshal(is);
        System.out.println("Unmarshalled from " + fileSequence);
        new Dumping().dump(newSequence);
        System.out.println("newSequence: " + newSequence);
    }

    @Test
    public void testMarshalTable ()
            throws PropertyException, JAXBException, FileNotFoundException
    {
        RunTable table = createHorizontalInstance();
        table.dumpSequences();
        System.out.println("table: " + table.dumpOf());

        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(table, new FileOutputStream(fileTable));
        System.out.println("Marshalled to " + fileTable);
        m.marshal(table, System.out);

        Unmarshaller um = jaxbContext.createUnmarshaller();
        InputStream is = new FileInputStream(fileTable);
        RunTable newTable = (RunTable) um.unmarshal(is);
        System.out.println("Unmarshalled from " + fileTable);
        new Dumping().dump(newTable);
        newTable.dumpSequences();
    }

    @Override
    protected void setUp ()
            throws JAXBException
    {
        jaxbContext = JAXBContext.newInstance(RunTable.class);
    }

    //--------------------------//
    // createHorizontalInstance //
    //--------------------------//
    private RunTable createHorizontalInstance ()
    {
        RunTable instance = new RunTable("hori", HORIZONTAL, dim.width, dim.height);

        RunSequence seq;

        seq = instance.getSequence(0);
        seq.add(new Run(1, 2));
        seq.add(new Run(5, 3));

        seq = instance.getSequence(1);
        seq.add(new Run(0, 1));
        seq.add(new Run(4, 2));

        seq = instance.getSequence(2);
        seq.add(new Run(3, 1));
        seq.add(new Run(5, 4));

        seq = instance.getSequence(3);
        seq.add(new Run(0, 2));
        seq.add(new Run(4, 1));
        seq.add(new Run(8, 2));

        seq = instance.getSequence(4);
        seq.add(new Run(2, 2));
        seq.add(new Run(6, 4));

        return instance;
    }
}
