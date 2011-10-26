/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.lag;

import omr.Main;

import omr.glyph.Shape;
import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphValue;

import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunsTable;

import omr.util.BaseTestCase;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

/**
 * Class {@code SectionBindingTest} tests the marshalling / unmarshalling of
 * a {@link Section}.
 *
 * @author Hervé Bitteur
 */
public class SectionBindingTest
    extends BaseTestCase
{
    //~ Instance fields --------------------------------------------------------

    private final String fileNameVertical = "section.vertical.xml";
    private final String fileNameHorizontal = "section.horizontal.xml";
    private final String fileNameValue = "glyph.value.xml";
    private JAXBContext  jaxbContext;

    // Lags and RunsTable instances
    Lag       vLag;
    RunsTable vTable;
    Lag       hLag;
    RunsTable hTable;

    //~ Methods ----------------------------------------------------------------

        //------------------//
        // testMarshalGlyph //
        //------------------//
        public void testMarshalGlyph ()
            throws JAXBException, FileNotFoundException
        {
            Section sv = vLag.createSection(180, new Run(100, 10, 127));
            sv.append(new Run(101, 20, 127));
    
            int     p = 180;
            Section sh = hLag.createSection(180, createRun(hTable, p++, 100, 10));
            sh.append(createRun(hTable, p++, 102, 20));
            sh.append(createRun(hTable, p++, 102, 20));
            sh.append(createRun(hTable, p++, 102, 20));
    
            SortedSet<Section> sections = new TreeSet<Section>();
            sections.add(sv);
            sections.add(sh);
    
            GlyphValue value = new GlyphValue(
                Shape.BREVE,
                20,
                1,
                0,
                false,
                0.5,
                sections);
    
            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(value, new FileOutputStream(fileNameValue));
            System.out.println("Marshalled to " + fileNameValue);
        }
    
        //-----------------------//
        // testMarshalHorizontal //
        //-----------------------//
        public void testMarshalHorizontal ()
            throws JAXBException, FileNotFoundException
        {
            int     p = 180;
            Section section = hLag.createSection(
                180,
                createRun(hTable, p++, 100, 10));
            section.append(createRun(hTable, p++, 102, 20));
            section.append(createRun(hTable, p++, 102, 20));
            section.append(createRun(hTable, p++, 102, 20));
    
            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(section, new FileOutputStream(fileNameHorizontal));
            System.out.println("Marshalled to " + fileNameHorizontal);
        }
    
        //---------------------//
        // testMarshalVertical //
        //---------------------//
        public void testMarshalVertical ()
            throws JAXBException, FileNotFoundException
        {
            Section section = vLag.createSection(180, new Run(100, 10, 127));
            section.append(new Run(101, 20, 127));
    
            Marshaller m = jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(section, new FileOutputStream(fileNameVertical));
            System.out.println("Marshalled to " + fileNameVertical);
        }

    //--------------------//
    // testUnmarshalGlyph //
    //--------------------//
    public void testUnmarshalGlyph ()
        throws JAXBException, FileNotFoundException
    {
        Unmarshaller um = jaxbContext.createUnmarshaller();
        InputStream  is = new FileInputStream(fileNameValue);
        GlyphValue   value = (GlyphValue) um.unmarshal(is);
        System.out.println("Unmarshalled from " + fileNameValue);
        Main.dumping.dump(value);

        Glyph glyph = new BasicGlyph(value);
        System.out.println("Glyph: " + glyph);
        glyph.dump();

        for (Section s : glyph.getMembers()) {
            System.out.println("member: " + s);
        }
    }

        //-------------------------//
        // testUnmarshalHorizontal //
        //-------------------------//
        public void testUnmarshalHorizontal ()
            throws JAXBException, FileNotFoundException
        {
            Unmarshaller um = jaxbContext.createUnmarshaller();
            InputStream  is = new FileInputStream(fileNameHorizontal);
            Section      section = (Section) um.unmarshal(is);
            System.out.println("Unmarshalled from " + fileNameHorizontal);
            System.out.println("Section: " + section);
        }
    
        //-----------------------//
        // testUnmarshalVertical //
        //-----------------------//
        public void testUnmarshalVertical ()
            throws JAXBException, FileNotFoundException
        {
            Unmarshaller um = jaxbContext.createUnmarshaller();
            InputStream  is = new FileInputStream(fileNameVertical);
            Section      section = (Section) um.unmarshal(is);
            System.out.println("Unmarshalled from " + fileNameVertical);
            System.out.println("Section: " + section);
        }

    //-------//
    // setUp //
    //-------//
    @Override
    protected void setUp ()
        throws JAXBException
    {
        jaxbContext = JAXBContext.newInstance(GlyphValue.class);
        
        vLag = new BasicLag("My Vertical Lag", Orientation.VERTICAL);
        vTable = new RunsTable(
            "Vert Runs",
            Orientation.VERTICAL,
            new Dimension(100, 200)); // Absolute
        vLag.setRuns(vTable);

        hLag = new BasicLag("My Horizontal Lag", Orientation.HORIZONTAL);
        hTable = new RunsTable(
            "Hori Runs",
            Orientation.HORIZONTAL,
            new Dimension(100, 200)); // Absolute
        hLag.setRuns(hTable);
    }

    //-----------//
    // createRun //
    //-----------//
    private Run createRun (RunsTable table,
                           int       alignment,
                           int       start,
                           int       length)
    {
        Run run = new Run(start, length, 127);

        table.getSequence(alignment)
             .add(run);

        return run;
    }
}
