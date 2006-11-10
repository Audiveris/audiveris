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

import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.score.Barline;
import omr.score.Beam;
import omr.score.Chord;
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
import omr.score.StaffPoint;
import omr.score.System;
import omr.score.SystemPart;
import omr.score.TimeSignature;

import omr.util.Logger;

import proxymusic.*;
import proxymusic.Scaling;

import java.io.*;
import java.util.Date;
import java.lang.String;

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

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreExporter.class);

    /** Un/marshalling context for use with JAXB */
    private static JAXBContext jaxbContext;

    /** "yes" token, to avoid case typos */
    private static final String YES = "yes";

    //~ Instance fields --------------------------------------------------------

    /** The related score */
    private Score score;

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
        this.score = score;

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
            ex.printStackTrace(); // TBI
        } catch (IOException ex) {
            ex.printStackTrace(); // TBI
        } catch (Exception ex) {
            ex.printStackTrace(); // TBI
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

    //---------------//
    // visit Barline //
    //---------------//
    public boolean visit (Barline barline)
    {
        if (isFirst.staffInPart) {
            if (barline.getShape() != omr.glyph.Shape.SINGLE_BARLINE) {
                proxymusic.Barline pmBarline = new proxymusic.Barline();
                current.pmMeasure.getNoteOrBackupOrForward()
                                 .add(pmBarline);
                pmBarline.setLocation("right");

                BarStyle barStyle = new BarStyle();
                pmBarline.getContent()
                         .add(barStyle);
                barStyle.setContent(barStyleOf(barline.getShape()));
            }
        }

        return true;
    }

    //------------//
    // visit Beam //
    //------------//
    public boolean visit (Beam beam)
    {
        return true;
    }

    //------------//
    // visit Chord //
    //------------//
    public boolean visit (Chord chord)
    {
        return true;
    }


    //------------//
    // visit Clef //
    //------------//
    public boolean visit (Clef clef)
    {
        //      Clefs are represented by the sign, line, and
        //      clef-octave-change elements. Sign values include G, F, C,
        //      percussion, TAB, and none. Line numbers are counted from
        //      the bottom of the staff. Standard values are 2 for the
        //      G sign (treble clef), 4 for the F sign (bass clef), 3
        //      for the C sign (alto clef) and 5 for TAB (on a 6-line
        //      staff). The clef-octave-change element is used for
        //      transposing clefs (e.g., a treble clef for tenors would
        //      have a clef-octave-change value of -1). The optional
        //      number attribute refers to staff numbers, from top to
        //      bottom on the system. A value of 1 is assumed if not
        //      present.
        if (isNewClef(clef)) {
            proxymusic.Clef pmClef = new proxymusic.Clef();
            getAttributes()
                .getClef()
                .add(pmClef);

            // Staff number (only for multi-staff parts)
            if (current.part.getIndices()
                            .size() > 1) {
                pmClef.setNumber("" + getStaffNumber());
            }

            // Sign
            Sign sign = new Sign();
            pmClef.setSign(sign);

            // Line (General computation that could be overridden by more
            // specific shape test below)
            Line line = new Line();
            pmClef.setLine(line);
            line.setContent(
                "" + (3 - (int) Math.rint(clef.getPitchPosition() / 2.0)));

            ClefOctaveChange change = null;
            Shape            shape = clef.getShape();

            switch (shape) {
            case G_CLEF :
                sign.setContent("G");

                break;

            case G_CLEF_OTTAVA_ALTA :
                change = new ClefOctaveChange();
                pmClef.setClefOctaveChange(change);
                change.setContent("1");
                sign.setContent("G");

                break;

            case G_CLEF_OTTAVA_BASSA :
                change = new ClefOctaveChange();
                pmClef.setClefOctaveChange(change);
                change.setContent("-1");
                sign.setContent("G");

                break;

            case C_CLEF :
                sign.setContent("C");

                break;

            case F_CLEF :
                sign.setContent("F");

                break;

            case F_CLEF_OTTAVA_ALTA :
                change = new ClefOctaveChange();
                pmClef.setClefOctaveChange(change);
                change.setContent("1");
                sign.setContent("F");

                break;

            case F_CLEF_OTTAVA_BASSA :
                change = new ClefOctaveChange();
                pmClef.setClefOctaveChange(change);
                change.setContent("-1");
                sign.setContent("F");

                break;

            default :
            }
        }

        return true;
    }

    //--------------------//
    // visit KeySignature //
    //--------------------//
    public boolean visit (KeySignature keySignature)
    {
        Key key = new Key();
        getAttributes()
            .setKey(key);

        Fifths fifths = new Fifths();
        key.setFifths(fifths);
        fifths.setContent("" + keySignature.getKey());

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    public boolean visit (Measure measure)
    {
        logger.fine(measure + " : " + isFirst);
        current.measure = measure;

        if (isFirst.staffInPart) {
            // Allocate Measure
            current.pmMeasure = new proxymusic.Measure();
            current.attributes = null;
            current.pmPart.getMeasure()
                          .add(current.pmMeasure);
            current.pmMeasure.setNumber("" + measure.getId());
            current.pmMeasure.setWidth(
                "" + (unitsToTenths(measure.getWidth())));

            if (isFirst.measure) {
                // Allocate Print
                current.pmPrint = new Print();
                current.pmMeasure.getNoteOrBackupOrForward()
                                 .add(current.pmPrint);

                if (isFirst.system) {
                    // New Page ? TBD
                } else {
                    // New system
                    current.pmPrint.setNewSystem(YES);
                }
            }
        }

        if (isFirst.measure) {
            if (isFirst.part) {
                // SystemLayout
                SystemLayout systemLayout = new SystemLayout();
                current.pmPrint.setSystemLayout(systemLayout);

                // SystemMargins
                SystemMargins systemMargins = new SystemMargins();
                systemLayout.setSystemMargins(systemMargins);

                LeftMargin leftMargin = new LeftMargin();
                systemMargins.setLeftMargin(leftMargin);
                leftMargin.setContent(
                    "" + unitsToTenths(current.system.getTopLeft().x));

                RightMargin rightMargin = new RightMargin();
                systemMargins.setRightMargin(rightMargin);
                rightMargin.setContent(
                    "" +
                    unitsToTenths(
                        score.getDimension().width -
                        current.system.getTopLeft().x -
                        current.system.getDimension().width));

                if (isFirst.system) {
                    // TopSystemDistance
                    TopSystemDistance topSystemDistance = new TopSystemDistance();
                    systemLayout.setTopSystemDistance(topSystemDistance);
                    topSystemDistance.setContent(
                        "" + unitsToTenths(current.system.getTopLeft().y));
                } else {
                    // SystemDistance
                    SystemDistance systemDistance = new SystemDistance();
                    systemLayout.setSystemDistance(systemDistance);

                    System prevSystem = (System) current.system.getPreviousSibling();
                    systemDistance.setContent(
                        "" +
                        unitsToTenths(
                            current.system.getTopLeft().y -
                            prevSystem.getTopLeft().y -
                            prevSystem.getDimension().height -
                            prevSystem.getLastStaff().getSize()));
                }
            }

            // StaffLayout for all staves in this part, except 1st system staff
            if (current.staff.getStafflink() > 0) {
                StaffLayout staffLayout = new StaffLayout();
                current.pmPrint.getStaffLayout()
                               .add(staffLayout);
                staffLayout.setNumber("" + getStaffNumber());

                StaffDistance staffDistance = new StaffDistance();
                staffLayout.setStaffDistance(staffDistance);

                Staff prevStaff = (Staff) current.staff.getPreviousSibling();
                staffDistance.setContent(
                    "" +
                    unitsToTenths(
                        current.staff.getTopLeft().y -
                        prevStaff.getTopLeft().y - prevStaff.getSize()));
            }
        }

        // Forward browsing down the measure
        measure.acceptChildren(this);

        return true;
    }

    //-----------------//
    // visit MusicNode //
    //-----------------//
    public boolean visit (MusicNode musicNode)
    {
        return true;
    }

    //-------//
    // Score //
    //-------//
    /**
     * Allocate/populate everything that directly relates to the score instance.
     * The rest of processing is delegated to the score children, that is to
     * say pages (TBI), then systems, etc...
     *
     * @param score visit the score to export
     */
    public boolean visit (Score score)
    {
        // Version
        scorePartwise.setVersion("1.1");

        // Identification
        Identification identification = new Identification();
        scorePartwise.setIdentification(identification);

        // Source
        Source source = new Source();
        identification.setSource(source);
        source.setContent(score.getImagePath());

        // Encoding
        Encoding encoding = new Encoding();
        identification.setEncoding(encoding);

        // [Encoding]/Software
        Software software = new Software();
        encoding.getEncodingDateOrEncoderOrSoftware()
                .add(software);
        software.setContent(Main.getToolName() + " " + Main.getToolVersion());

        // [Encoding]/EncodingDate
        EncodingDate encodingDate = new EncodingDate();
        encoding.getEncodingDateOrEncoderOrSoftware()
                .add(encodingDate);
        encodingDate.setContent(String.format("%tF", new Date()));

        // Defaults
        Defaults defaults = new Defaults();
        scorePartwise.setDefaults(defaults);

        // [Defaults]/Scaling
        Scaling scaling = new Scaling();
        defaults.setScaling(scaling);

        Millimeters millimeters = new Millimeters();
        scaling.setMillimeters(millimeters);
        millimeters.setContent(
            String.format(
                "%.3f",
                (score.getSheet()
                      .getScale()
                      .interline() * 25.4 * 4) / 300)); // Assuming 300 DPI

        Tenths tenths = new Tenths();
        scaling.setTenths(tenths);
        tenths.setContent("40");

        // [Defaults]/PageLayout
        PageLayout pageLayout = new PageLayout();
        defaults.setPageLayout(pageLayout);

        PageWidth pageWidth = new PageWidth();
        pageLayout.setPageWidth(pageWidth);
        pageWidth.setContent("" + unitsToTenths(score.getDimension().width));

        PageHeight pageHeight = new PageHeight();
        pageLayout.setPageHeight(pageHeight);
        pageHeight.setContent("" + unitsToTenths(score.getDimension().height));

        // PartList
        PartList partList = new PartList();
        scorePartwise.setPartList(partList);
        isFirst.part = true;

        for (Part p : score.getPartList()) {
            current.part = p;

            // Scorepart in partList
            ScorePart scorePart = new ScorePart();
            partList.getPartGroupOrScorePart()
                    .add(scorePart);
            scorePart.setId(current.part.getPid());

            PartName partName = new PartName();
            scorePart.setPartName(partName);
            partName.setContent(current.part.getName());

            // Part in scorePartwise
            current.pmPart = new proxymusic.Part();
            scorePartwise.getPart()
                         .add(current.pmPart);
            current.pmPart.setId(scorePart);

            // Delegate to children the filling of measures
            logger.fine("Populating " + current.part);
            isFirst.system = true; // TBD: to be reviewed when adding pages
            score.acceptChildren(this);

            // Next part, if any
            isFirst.part = false;
        }

        return false; // That's all
    }

    //------------//
    // visit Slur //
    //------------//
    public boolean visit (Slur slur)
    {
        return true;
    }

    //-------------//
    // visit Staff //
    //-------------//
    public boolean visit (Staff staff)
    {
        return true;
    }

    //-----------------//
    // visit StaffNode //
    //-----------------//
    public boolean visit (StaffNode staffNode)
    {
        return true;
    }

    //--------//
    // System //
    //--------//
    /**
     * Allocate/populate everything that directly relates to this system in the
     * current part. The rest of processing is directly delegated to the
     * measures (TBD: add the slurs ?)
     *
     * @param system visit the system to export
     */
    public boolean visit (System system)
    {
        logger.fine("Visiting " + system);
        current.system = system;

        SystemPart systemPart = system.getParts()
                                      .get(current.part.getId() - 1);

        // Loop on measures
        isFirst.measure = true;

        for (int im = 0; im < system.getFirstStaff()
                                    .getMeasures()
                                    .size(); im++) {
            // Loop on staves (within the current part only)
            isFirst.staffInPart = true;

            for (Staff staff : systemPart.getStaves()) {
                current.staff = staff;

                Measure measure = (Measure) staff.getMeasures()
                                                 .get(im);
                // Delegate to measure
                measure.accept(this);
                isFirst.staffInPart = false;
            }

            isFirst.measure = false;
        }

        isFirst.system = false;

        return false; // No default browsing this way
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    public boolean visit (TimeSignature timeSignature)
    {
        Time time = new Time();
        getAttributes()
            .setTime(time);

        // Beats
        Beats beats = new Beats();
        time.getBeatsAndBeatType()
            .add(beats);
        beats.setContent("" + timeSignature.getNumerator());

        // BeatType
        BeatType beatType = new BeatType();
        time.getBeatsAndBeatType()
            .add(beatType);
        beatType.setContent("" + timeSignature.getDenominator());

        // Symbol ?
        switch (timeSignature.getShape()) {
        case COMMON_TIME :
            time.setSymbol("common");

            break;

        case CUT_TIME :
            time.setSymbol("cut");

            break;
        }

        return true;
    }

    //- Utility Methods --------------------------------------------------------

    //----------------//
    // getJaxbContext //
    //----------------//
    /**
     * Get access to (and elaborate if not yet done) the needed JAXB context
     *
     * @return the ready to use JAXB context
     * @exception JAXBException if anything goes wrong
     */
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

    //---------------//
    // getAttributes //
    //---------------//
    /**
     * Report (after creating it if necessary) the measure attributes element
     *
     * @return the measure attributes element
     */
    private Attributes getAttributes ()
    {
        if (current.attributes == null) {
            current.attributes = new Attributes();
            current.pmMeasure.getNoteOrBackupOrForward()
                             .add(current.attributes);
        }

        return current.attributes;
    }

    //-----------//
    // isNewClef //
    //-----------//
    /**
     * Make sure we have a NEW clef, not already assigned. We have to go back
     * in current measure, then in current staff, then in same staff in previous
     * systems, until we find a previous clef. And we compare the two shapes.
     * @param clef the potentially new clef
     * @return true if this clef is really new
     */
    private boolean isNewClef (Clef clef)
    {
        // Perhaps another clef before this one ?
        Clef previousClef = current.measure.getClefBefore(
            new StaffPoint(clef.getCenter().x - 1, 0));

        if (previousClef != null) {
            return previousClef.getShape() != clef.getShape();
        }

        return true; // Since no previous clef found
    }

    //----------------//
    // getStaffNumber //
    //----------------//
    /**
     * Report the number of the current staff
     *
     * @return the staff number
     */
    private int getStaffNumber ()
    {
        return ((1 + current.staff.getStafflink()) -
               current.part.getIndices()
                           .get(0));
    }

    //------------//
    // barStyleOf //
    //------------//
    /**
     * Report the MusicXML bar style for a recognized Barline shape
     *
     * @param shape the barline shape
     * @return the bar style
     */
    private String barStyleOf (Shape shape)
    {
        //      Bar-style contains style information. Choices are
        //      regular, dotted, dashed, heavy, light-light,
        //      light-heavy, heavy-light, heavy-heavy, and none.
        switch (shape) {
        case SINGLE_BARLINE :
            return "light";

        case DOUBLE_BARLINE :
            return "light-light";

        case FINAL_BARLINE :
            return "light-heavy";

        case REVERSE_FINAL_BARLINE :
            return "heavy-light";

        case LEFT_REPEAT_SIGN :
            return "heavy-light";

        case RIGHT_REPEAT_SIGN :
            return "light-heavy";

        case BACK_TO_BACK_REPEAT_SIGN :
            return "heavy-heavy"; // ?
        }

        return "???";
    }

    //-------------//
    // jaxbMarshal //
    //-------------//
    /**
     * Marshal the hierarchy rooted at provided Scorepartwise instance to an
     * OutputStream
     *
     * @param scorePartwise the root element
     * @param os the output stream
     * @exception JAXBException if marshalling goes wrong
     */
    private void jaxbMarshal (ScorePartwise scorePartwise,
                              OutputStream  os)
        throws JAXBException
    {
        Marshaller m = getJaxbContext()
                           .createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(scorePartwise, os);
    }

    //---------------//
    // unitsToTenths //
    //---------------//
    /**
     * Convert a value expressed in units to a value expressed in tenths
     *
     * @param units the number of units
     * @return the number of tenths
     */
    private int unitsToTenths (int units)
    {
        // Divide by 1.6 with rounding to nearest integer value
        return (int) Math.rint(units / 1.6);
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Current //
    //---------//
    /** Keep references of all current entities */
    private static class Current
    {
        proxymusic.Part       pmPart;
        Part                  part;
        System                system;
        proxymusic.Measure    pmMeasure;
        Measure               measure;
        proxymusic.Print      pmPrint;
        proxymusic.Attributes attributes;
        Staff                 staff;
    }

    //---------//
    // IsFirst //
    //---------//
    /** Composite flag to help drive processing of any entity */
    private static class IsFirst
    {
        /** We are writing the first part of the score */
        boolean part;

        /** We are writing the first system in the current page */
        boolean system;

        /** We are writing the first measure in the current system */
        boolean measure;

        /** We are writing the first staff in the current part */
        boolean staffInPart;

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();

            if (part) {
                sb.append(" firstPart");
            }

            if (system) {
                sb.append(" firstSystem");
            }

            if (measure) {
                sb.append(" firstMeasure");
            }

            if (staffInPart) {
                sb.append(" firstStaffInPart");
            }

            return sb.toString();
        }
    }
}
