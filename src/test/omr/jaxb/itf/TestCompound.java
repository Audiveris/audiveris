//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     T e s t C o m p o u n d                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.itf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import omr.util.BaseTestCase;
import omr.util.Dumping;

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
        jaxbContext = JAXBContext.newInstance(MyCompound.class);
    }

    private void marshall ()
            throws JAXBException, FileNotFoundException
    {
        File target = new File(dir, indexFileName);

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

        Marshaller m = jaxbContext.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

        m.marshal(compound, new FileOutputStream(target));
        System.out.println("Marshalled   to   " + target);
        System.out.println("=========================================================");
        m.marshal(compound, System.out);
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
