//----------------------------------------------------------------------------//
//                                                                            //
//                      E x p o r t i n g V i s i t o r                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.Main;

import omr.score.Barline;
import omr.score.Clef;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.MusicNode;
import omr.score.Part;
import omr.score.Score;
import omr.score.ScoreFormat;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.StaffNode;
import omr.score.System;
import omr.score.SystemPart;
import omr.score.TimeSignature;

import omr.util.Logger;

import proxymusic.generated.Encoding;
import proxymusic.generated.EncodingDate;
import proxymusic.generated.Identification;
import proxymusic.generated.PartList;
import proxymusic.generated.PartName;
import proxymusic.generated.ScorePart;
import proxymusic.generated.ScorePartwise;
import proxymusic.generated.Software;
import proxymusic.generated.Source;

import java.io.*;
import java.util.Date;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

/**
 * Class <code>ScoreExporter</code> can visit the score hierarchy to export
 * the score to a MusicXML file
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScoreExporter
    implements Visitor
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(ScoreExporter.class);

    /** Un/marshalling context for use with JAXB */
    private static JAXBContext jaxbContext;

    //~ Instance fields --------------------------------------------------------

    /** The score proxy built precisely for export via JAXB */
    private ScorePartwise scorePartwise;

    /** Current context */
    private Current current = new Current();

    /** Current flags */
    private IsFirst isFirst = new IsFirst();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // ScoreExporter //
    //---------------//
    /**
     * Create a new ScoreExporter object, which triggers the export.
     *
     * @param score the score to export
     * @param xmlFile the xml file to write, or null
     */
    public ScoreExporter (Score score,
                          File  xmlFile)
    {
        // Where do we write the score xml file?
        if (xmlFile == null) {
            xmlFile = new File(
                Main.getOutputFolder(),
                score.getRadix() + ScoreFormat.XML.extension);
        }

        // Make sure the folder exists
        File folder = new File(xmlFile.getParent());

        if (!folder.exists()) {
            logger.info("Creating folder " + folder);
            folder.mkdirs();
        }

        // Allocate a score proxy, and let visited nodes fill this proxy
        scorePartwise = new ScorePartwise();
        score.accept(this);

        //  Marshal the proxy
        try {
            OutputStream os = new FileOutputStream(xmlFile);
            jaxbMarshal(scorePartwise, os);
            logger.info("Score exported to " + xmlFile);
            os.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // preloadJaxbContext //
    //--------------------//
    /**
     * This method allows to preload the JaxbContext in a background task, so 
     * that it is immediately available when the interactive user needs it.
     */
    public static void preloadJaxbContext ()
    {
        Thread worker = new Thread() {
            public void run ()
            {
                try {
                    getJaxbContext();
                } catch (JAXBException ex) {
                    ex.printStackTrace();
                    logger.warning("Error preloading JaxbContext");
                }
            }
        };

        worker.setName("JaxbContextLoader");
        worker.setPriority(Thread.MIN_PRIORITY);
        worker.start();
    }

    public boolean visit (Barline barline)
    {
        return true;
    }

    public boolean visit (Clef clef)
    {
        return true;
    }

    public boolean visit (KeySignature keySignature)
    {
        return true;
    }

    //---------//
    // Measure //
    //---------//
    public boolean visit (Measure measure)
    {
        logger.fine(measure + " : " + isFirst);

        if (isFirst.staff) {
            ///current.partMeasure = new PartMeasure();
        }

        //          <print>
        //              <system-layout>
        //                  <system-margins>
        //                      <left-margin>64</left-margin>
        //                      <right-margin>0</right-margin>
        //                  </system-margins>
        //                  <top-system-distance>208</top-system-distance>
        //              </system-layout>
        //              <staff-layout number="2">
        //                  <staff-distance>64</staff-distance>
        //              </staff-layout>
        //          </print>

        //        if (measure.getLeftX() != null) {
        //            logger.fine("Adding " + measure);
        //
        //            proxy.getMeasures()
        //                  .add(measure);
        //        } else {
        //            logger.fine("Skipping " + measure);
        //        }
        //
        //        isFirst.measure = false;
        return true;
    }

    public boolean visit (MusicNode musicNode)
    {
        return true;
    }

    //-------//
    // Score //
    //-------//
    public boolean visit (Score score)
    {
        // Version
        scorePartwise.setVersion("1.1");

        // Source
        Source source = new Source();
        source.setContent(score.getImagePath());

        // Encoding
        Encoding encoding = new Encoding();
        Software software = new Software();
        software.setContent(Main.getToolName() + " " + Main.getToolVersion());
        encoding.getEncodingDateOrEncoderOrSoftware()
                .add(software);

        EncodingDate encodingDate = new EncodingDate();
        encodingDate.setContent(String.format("%tF", new Date()));
        encoding.getEncodingDateOrEncoderOrSoftware()
                .add(encodingDate);

        // Identification
        Identification identification = new Identification();
        scorePartwise.setIdentification(identification);
        identification.setSource(source);
        identification.setEncoding(encoding);

        // PartList
        PartList partList = new PartList();
        scorePartwise.setPartList(partList);

        for (Part part : score.getPartList()) {
            ScorePart scorePart = new ScorePart();
            partList.getPartGroupOrScorePart()
                    .add(scorePart);
            scorePart.setId(part.getPid());

            PartName partName = new PartName();
            scorePart.setPartName(partName);
            partName.setContent(part.getName());
        }

        // Populate each part in turn
        //        for (Part part : score.getPartList()) {
        //            current.part = part;
        //            logger.fine("Populating " + current.part);
        //            isFirst.system = true;
        //            score.acceptChildren(this);
        //        }

        ///logger.fine("measures built nb=" + scorePartWise.getMeasures().size());
        return false;
    }

    public boolean visit (Slur slur)
    {
        return true;
    }

    public boolean visit (Staff staff)
    {
        return true;
    }

    public boolean visit (StaffNode staffNode)
    {
        return true;
    }

    //--------//
    // System //
    //--------//
    public boolean visit (System system)
    {
        logger.fine("Visiting " + system);

        SystemPart systemPart = system.getParts()
                                      .get(current.part.getId() - 1);
        isFirst.measure = true;
        isFirst.staff = true;

        for (int im = 0; im < system.getFirstStaff()
                                    .getMeasures()
                                    .size(); im++) {
            for (Staff staff : systemPart.getStaves()) {
                Measure measure = (Measure) staff.getMeasures()
                                                 .get(im);
                measure.accept(this);
                isFirst.staff = false;
            }

            isFirst.measure = false;
        }

        isFirst.system = false;

        return false; // No default browsing
    }

    public boolean visit (TimeSignature timeSignature)
    {
        return true;
    }

    //----------------//
    // getJaxbContext //
    //----------------//
    private static synchronized JAXBContext getJaxbContext ()
        throws JAXBException
    {
        // Lazy creation
        if (jaxbContext == null) {
            logger.fine("Creating JAXBContext ...");
            jaxbContext = JAXBContext.newInstance(ScorePartwise.class);
            logger.fine("JAXBContext created");
        }
        
        return jaxbContext;
    }

    //-------------//
    // jaxbMarshal //
    //-------------//
    private void jaxbMarshal (ScorePartwise scorePartwise,
                              OutputStream  os)
        throws JAXBException
    {
        Marshaller m = getJaxbContext()
                           .createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(scorePartwise, os);
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Current //
    //---------//
    private static class Current
    {
        Part    part;
        System  system;
        Measure measure;
        Staff   staff;
    }

    //---------//
    // IsFirst //
    //---------//
    private static class IsFirst
    {
        /** We are writing the first system in the current page */
        boolean system;

        /** We are writing the first measure in the current system */
        boolean measure;

        /** We are writing the first staff in the current measure */
        boolean staff;

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();

            if (system) {
                sb.append(" firstSystem");
            }

            if (measure) {
                sb.append(" firstMeasure");
            }

            if (staff) {
                sb.append(" firstStaff");
            }

            return sb.toString();
        }
    }
}
