//----------------------------------------------------------------------------//
//                                                                            //
//                         S c o r e E x p o r t e r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.Main;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.score.Barline;
import omr.score.Beam;
import omr.score.Chord;
import omr.score.Clef;
import omr.score.Dynamics;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.PartNode;
import omr.score.Pedal;
import omr.score.Score;
import omr.score.ScoreFormat;
import omr.score.ScorePart;
import omr.score.Slot;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.System;
import omr.score.SystemPart;
import omr.score.SystemPoint;
import omr.score.TimeSignature;
import omr.score.Wedge;
import static omr.score.visitor.MusicXML.*;

import omr.sheet.PixelPoint;
import omr.sheet.PixelRectangle;

import omr.util.Logger;
import omr.util.TreeNode;

import proxymusic.*;

import proxymusic.util.Marshalling;

import java.io.*;
import java.lang.String;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreExporter.class);

    //~ Instance fields --------------------------------------------------------

    /** The related score */
    private Score score;

    /** The score proxy built precisely for export via JAXB */
    private final ScorePartwise scorePartwise = new ScorePartwise();

    /** Current context */
    private Current current = new Current();

    /** Current flags */
    private IsFirst isFirst = new IsFirst();

    /** Slur numbers */
    private Map<Slur, Integer> slurNumbers = new HashMap<Slur, Integer>();

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

        // Let visited nodes fill the scorePartWise proxy
        score.accept(this);

        //  Finally, marshal the proxy
        try {
            final OutputStream os = new FileOutputStream(xmlFile);
            Marshalling.marshal(scorePartwise, os);
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
                    Marshalling.getContext();
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
    @Override
    public boolean visit (Barline barline)
    {
        ///logger.info("Visiting " + barline);
        if (barline.getShape() != omr.glyph.Shape.SINGLE_BARLINE) {
            proxymusic.Barline pmBarline = new proxymusic.Barline();
            current.pmMeasure.getNoteOrBackupOrForward()
                             .add(pmBarline);
            pmBarline.setLocation("right");

            BarStyle barStyle = new BarStyle();
            pmBarline.setBarStyle(barStyle);
            barStyle.setContent(barStyleOf(barline.getShape()));
        }

        return true;
    }

    //-------------//
    // visit Chord //
    //-------------//
    @Override
    public boolean visit (Chord chord)
    {
        return false;
    }

    //------------//
    // visit Clef //
    //------------//
    @Override
    public boolean visit (Clef clef)
    {
        ///logger.info("Visiting " + clef);
        if (isNewClef(clef)) {
            proxymusic.Clef pmClef = new proxymusic.Clef();
            getMeasureAttributes()
                .getClef()
                .add(pmClef);

            // Staff number (only for multi-staff parts)
            if (current.part.getStaffIds()
                            .size() > 1) {
                pmClef.setNumber("" + (clef.getStaff().getId()));
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

    //----------------//
    // visit Dynamics //
    //----------------//
    @Override
    public boolean visit (Dynamics dynamics)
    {
        Direction direction = new Direction();
        current.pmMeasure.getNoteOrBackupOrForward()
                         .add(direction);

        DirectionType directionType = new DirectionType();
        direction.getDirectionType()
                 .add(directionType);

        proxymusic.Dynamics pmDynamics = new proxymusic.Dynamics();
        directionType.getDynamics()
                     .add(pmDynamics);

        // Precise dynamic signature
        pmDynamics.getPOrPpOrPpp()
                  .add(getDynamicsObject(dynamics.getShape()));

        // Staff ?
        Staff staff = current.note.getStaff();

        if (current.note.getPart()
                        .getStaves()
                        .size() > 1) {
            proxymusic.Staff pmStaff = new proxymusic.Staff();
            direction.setStaff(pmStaff);
            pmStaff.setContent("" + (staff.getId()));
        }

        // Placement
        if (dynamics.getPoint().y < current.note.getCenter().y) {
            direction.setPlacement(ABOVE);
        } else {
            direction.setPlacement(BELOW);
        }

        // default-y
        PixelPoint pix = current.system.toPixelPoint(dynamics.getPoint());
        PixelPoint staffPix = new PixelPoint(
            pix.x,
            staff.getInfo().getFirstLine().getLine().yAt(pix.x));
        pmDynamics.setDefaultY(
            toTenths(dynamics.getScale().pixelsToUnits(staffPix.y - pix.y)));

        // Relative-x (No offset for the time being) using note left side
        pmDynamics.setRelativeX(
            toTenths(dynamics.getPoint().x - current.note.getCenterLeft().x));

        return true;
    }

    //--------------------//
    // visit KeySignature //
    //--------------------//
    @Override
    public boolean visit (KeySignature keySignature)
    {
        ///logger.info("Visiting " + keySignature);
        if (isNewKeySignature(keySignature)) {
            //            logger.info(
            //                keySignature.getContextString() + " New key " + keySignature);
            Key key = new Key();
            getMeasureAttributes()
                .setKey(key);

            Fifths fifths = new Fifths();
            key.setFifths(fifths);
            fifths.setContent("" + keySignature.getKey());

            //        } else {
            //            logger.info(
            //                keySignature.getContextString() + " No new key " +
            //                keySignature);
        }

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        ///logger.info("Visiting " + measure);
        logger.fine(measure + " : " + isFirst);
        current.measure = measure;

        // Allocate Measure
        current.pmMeasure = new proxymusic.Measure();
        current.attributes = null;
        current.pmPart.getMeasure()
                      .add(current.pmMeasure);
        current.pmMeasure.setNumber("" + measure.getId());
        current.pmMeasure.setWidth(toTenths(measure.getWidth()));

        if (measure.isImplicit()) {
            current.pmMeasure.setImplicit(YES);
        }

        if (isFirst.measure) {
            // Allocate Print
            current.pmPrint = new Print();
            current.pmMeasure.getNoteOrBackupOrForward()
                             .add(current.pmPrint);

            if (isFirst.system) {
                // New Page ? TBD

                // Divisions
                Divisions divisions = new Divisions();
                getMeasureAttributes()
                    .setDivisions(divisions);
                divisions.setContent(
                    "" +
                    measure.getPart().getScorePart().simpleDurationOf(
                        omr.score.Note.QUARTER_DURATION));

                // Number of staves, if > 1
                int stavesNb = measure.getPart()
                                      .getStaves()
                                      .size();

                if (stavesNb > 1) {
                    Staves staves = new Staves();
                    getMeasureAttributes()
                        .setStaves(staves);
                    staves.setContent("" + stavesNb);
                }
            } else {
                // New system
                current.pmPrint.setNewSystem(YES);
            }

            if (isFirst.part) {
                // SystemLayout
                SystemLayout systemLayout = new SystemLayout();
                current.pmPrint.setSystemLayout(systemLayout);

                // SystemMargins
                SystemMargins systemMargins = new SystemMargins();
                systemLayout.setSystemMargins(systemMargins);

                LeftMargin leftMargin = new LeftMargin();
                systemMargins.setLeftMargin(leftMargin);
                leftMargin.setContent(toTenths(current.system.getTopLeft().x));

                RightMargin rightMargin = new RightMargin();
                systemMargins.setRightMargin(rightMargin);
                rightMargin.setContent(
                    toTenths(
                        score.getDimension().width -
                        current.system.getTopLeft().x -
                        current.system.getDimension().width));

                if (isFirst.system) {
                    // TopSystemDistance
                    TopSystemDistance topSystemDistance = new TopSystemDistance();
                    systemLayout.setTopSystemDistance(topSystemDistance);
                    topSystemDistance.setContent(
                        toTenths(current.system.getTopLeft().y));
                } else {
                    // SystemDistance
                    SystemDistance systemDistance = new SystemDistance();
                    systemLayout.setSystemDistance(systemDistance);

                    System prevSystem = (System) current.system.getPreviousSibling();
                    systemDistance.setContent(
                        toTenths(
                            current.system.getTopLeft().y -
                            prevSystem.getTopLeft().y -
                            prevSystem.getDimension().height -
                            prevSystem.getLastPart().getLastStaff().getHeight()));
                }
            }

            // StaffLayout for all staves in this part, except 1st system staff
            for (TreeNode sNode : measure.getPart()
                                         .getStaves()) {
                Staff staff = (Staff) sNode;

                if (!isFirst.part || (staff.getId() > 1)) {
                    StaffLayout staffLayout = new StaffLayout();
                    current.pmPrint.getStaffLayout()
                                   .add(staffLayout);
                    staffLayout.setNumber("" + staff.getId());

                    StaffDistance staffDistance = new StaffDistance();
                    staffLayout.setStaffDistance(staffDistance);

                    Staff prevStaff = (Staff) staff.getPreviousSibling();

                    if (prevStaff == null) {
                        SystemPart prevPart = (SystemPart) measure.getPart()
                                                                  .getPreviousSibling();
                        prevStaff = prevPart.getLastStaff();
                    }

                    staffDistance.setContent(
                        toTenths(
                            staff.getTopLeft().y - prevStaff.getTopLeft().y -
                            prevStaff.getHeight()));
                }
            }
        }

        // Specific browsing down the measure
        // Clefs, KeySignatures, TimeSignatures
        measure.getClefList()
               .acceptChildren(this);
        measure.getKeySigList()
               .acceptChildren(this);
        measure.getTimeSigList()
               .acceptChildren(this);

        // Now voice per voice
        int timeCounter = 0;

        for (current.voice = 1; current.voice <= measure.getVoicesNumber();
             current.voice++) {
            // Need a backup ?
            if (timeCounter != 0) {
                insertBackup(timeCounter);
                timeCounter = 0;
            }

            Chord lastChord = null;

            for (Slot slot : measure.getSlots()) {
                for (Chord chord : slot.getChords()) {
                    if (chord.getVoice() == current.voice) {
                        // Need a forward before this chord ?
                        if (timeCounter < slot.getOffset()) {
                            insertForward(
                                slot.getOffset() - timeCounter,
                                chord);
                            timeCounter = slot.getOffset();
                        }

                        // Delegate to the chord children directly
                        chord.acceptChildren(this);
                        lastChord = chord;
                        timeCounter += chord.getDuration();
                    }
                }
            }

            // Need an ending forward ?
            if ((lastChord != null) &&
                !measure.isImplicit() &&
                (timeCounter < measure.getExpectedDuration())) {
                insertForward(
                    measure.getExpectedDuration() - timeCounter,
                    lastChord);
            }
        }

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    @Override
    public boolean visit (omr.score.Note note)
    {
        ///logger.info("Visiting " + note);
        Staff     staff = note.getStaff();
        Chord     chord = note.getChord();
        Notations notations = null;

        Note      pmNote = new Note();
        current.pmMeasure.getNoteOrBackupOrForward()
                         .add(pmNote);

        // Chord events for first note in chord
        if (chord.getNotes()
                 .indexOf(note) == 0) {
            current.note = note;

            for (PartNode node : chord.getEvents()) {
                node.accept(this);
            }
        } else {
            // Chord indication for every other note
            pmNote.setChord(new proxymusic.Chord());
        }

        // Rest ?
        if (note.isRest()) {
            Rest rest = new Rest();
            pmNote.setRest(rest);
        } else {
            // Pitch
            Pitch pitch = new Pitch();
            pmNote.setPitch(pitch);

            Step step = new Step();
            pitch.setStep(step);
            step.setContent("" + note.getStep());

            Octave octave = new Octave();
            pitch.setOctave(octave);
            octave.setContent("" + note.getOctave());

            if (note.getAlter() != 0) {
                Alter alter = new Alter();
                pitch.setAlter(alter);
                alter.setContent("" + note.getAlter());
            }
        }

        // Default-x (use left side of the note wrt measure)
        int noteLeft = note.getCenterLeft().x;
        pmNote.setDefaultX(toTenths(noteLeft - note.getMeasure().getLeftX()));

        // Duration
        Duration duration = new Duration();
        pmNote.setDuration(duration);
        duration.setContent(
            "" + current.part.simpleDurationOf(chord.getDuration()));

        // Voice
        Voice voice = new Voice();
        pmNote.setVoice(voice);
        voice.setContent("" + chord.getVoice());

        // Type
        Type type = new Type();
        pmNote.setType(type);
        type.setContent("" + note.getTypeName());

        // Stem ?
        if (chord.getStem() != null) {
            Glyph stem = chord.getStem();
            Stem  pmStem = new Stem();
            pmNote.setStem(pmStem);

            SystemPoint tail = chord.getTailLocation();
            pmStem.setDefaultY(yOf(tail.y, staff));

            if (tail.y < note.getCenter().y) {
                pmStem.setContent(UP);
            } else {
                pmStem.setContent(DOWN);
            }
        }

        // Staff ? (only if more than one staff in part)
        if (note.getPart()
                .getStaves()
                .size() > 1) {
            proxymusic.Staff pmStaff = new proxymusic.Staff();
            pmNote.setStaff(pmStaff);
            pmStaff.setContent("" + staff.getId());
        }

        // Dots
        for (int i = 0; i < chord.getDotsNumber(); i++) {
            pmNote.getDot()
                  .add(new Dot());
        }

        // Accidental ?
        if (note.getAccidental() != null) {
            Accidental accidental = new Accidental();
            pmNote.setAccidental(accidental);
            accidental.setContent(accidentalNameOf(note.getAccidental()));
        }

        // Beams ?
        for (Beam beam : chord.getBeams()) {
            proxymusic.Beam pmBeam = new proxymusic.Beam();
            pmNote.getBeam()
                  .add(pmBeam);
            pmBeam.setNumber("" + beam.getLevel());

            Glyph glyph = beam.getGlyphs()
                              .first();

            if (glyph.getShape() == BEAM_HOOK) {
                if (glyph.getCenter().x > chord.getStem()
                                               .getCenter().x) {
                    pmBeam.setContent(FORWARD_HOOK);
                } else {
                    pmBeam.setContent(BACKWARD_HOOK);
                }
            } else {
                if (beam.getChords()
                        .first() == chord) {
                    pmBeam.setContent(BEGIN);
                } else if (beam.getChords()
                               .last() == chord) {
                    pmBeam.setContent(END);
                } else {
                    pmBeam.setContent(CONTINUE);
                }
            }
        }

        // Tie / Slur
        for (Slur slur : note.getSlurs()) {
            boolean isStart = slur.getLeftNote() == note;

            // Notations element
            if (notations == null) {
                notations = new Notations();
                pmNote.getNotations()
                      .add(notations);
            }

            if (slur.isTie()) {
                // Tie element
                Tie tie = new Tie();
                pmNote.getTie()
                      .add(tie);
                tie.setType(isStart ? START : STOP);

                // Tied element
                Tied tied = new Tied();
                notations.getTiedOrSlurOrTuplet()
                         .add(tied);

                // Type
                tied.setType(isStart ? START : STOP);

                // Orientation
                if (isStart) {
                    tied.setOrientation(slur.isBelow() ? UNDER : OVER);
                }

                // Bezier
                if (isStart) {
                    tied.setDefaultX(
                        toTenths(slur.getCurve().getX1() - noteLeft));
                    tied.setDefaultY(yOf(slur.getCurve().getY1(), staff));
                    tied.setBezierX(
                        toTenths(slur.getCurve().getCtrlX1() - noteLeft));
                    tied.setBezierY(yOf(slur.getCurve().getCtrlY1(), staff));
                } else {
                    tied.setDefaultX(
                        toTenths(slur.getCurve().getX2() - noteLeft));
                    tied.setDefaultY(yOf(slur.getCurve().getY2(), staff));
                    tied.setBezierX(
                        toTenths(slur.getCurve().getCtrlX2() - noteLeft));
                    tied.setBezierY(yOf(slur.getCurve().getCtrlY2(), staff));
                }
            } else {
                // Slur element
                proxymusic.Slur pmSlur = new proxymusic.Slur();
                notations.getTiedOrSlurOrTuplet()
                         .add(pmSlur);

                // Number attribute
                Integer num = slurNumbers.get(slur);

                if (num != null) {
                    pmSlur.setNumber(num.toString());
                    slurNumbers.remove(slur);

                    if (logger.isFineEnabled()) {
                        logger.fine(
                            note.getContextString() + " last use " + num +
                            " -> " + slurNumbers.toString());
                    }
                } else {
                    // Determine first available number
                    for (num = 1; num <= 6; num++) {
                        if (!slurNumbers.containsValue(num)) {
                            if (slur.getRightExtension() != null) {
                                slurNumbers.put(slur.getRightExtension(), num);
                            } else {
                                slurNumbers.put(slur, num);
                            }

                            pmSlur.setNumber(num.toString());

                            if (logger.isFineEnabled()) {
                                logger.fine(
                                    note.getContextString() + " first use " +
                                    num + " -> " + slurNumbers.toString());
                            }

                            break;
                        }
                    }
                }

                // Type
                pmSlur.setType(isStart ? START : STOP);

                // Placement
                if (isStart) {
                    pmSlur.setPlacement(slur.isBelow() ? BELOW : ABOVE);
                }

                // Bezier
                if (isStart) {
                    pmSlur.setDefaultX(
                        toTenths(slur.getCurve().getX1() - noteLeft));
                    pmSlur.setDefaultY(yOf(slur.getCurve().getY1(), staff));
                    pmSlur.setBezierX(
                        toTenths(slur.getCurve().getCtrlX1() - noteLeft));
                    pmSlur.setBezierY(yOf(slur.getCurve().getCtrlY1(), staff));
                } else {
                    pmSlur.setDefaultX(
                        toTenths(slur.getCurve().getX2() - noteLeft));
                    pmSlur.setDefaultY(yOf(slur.getCurve().getY2(), staff));
                    pmSlur.setBezierX(
                        toTenths(slur.getCurve().getCtrlX2() - noteLeft));
                    pmSlur.setBezierY(yOf(slur.getCurve().getCtrlY2(), staff));
                }
            }
        }

        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    /**
     * Allocate/populate everything that directly relates to the score instance.
     * The rest of processing is delegated to the score children, that is to
     * say pages (TBI), then systems, etc...
     *
     * @param score visit the score to export
     */
    @Override
    public boolean visit (Score score)
    {
        ///logger.info("Visiting " + score);

        // No version inserted
        // Let the marshalling class handle it

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
                Locale.US,
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
        pageWidth.setContent(toTenths(score.getDimension().width));

        PageHeight pageHeight = new PageHeight();
        pageLayout.setPageHeight(pageHeight);
        pageHeight.setContent(toTenths(score.getDimension().height));

        // PartList
        PartList partList = new PartList();
        scorePartwise.setPartList(partList);
        isFirst.part = true;

        for (ScorePart p : score.getPartList()) {
            current.part = p;

            // Scorepart in partList
            proxymusic.ScorePart scorePart = new proxymusic.ScorePart();
            partList.getPartGroupOrScorePart()
                    .add(scorePart);
            scorePart.setId(current.part.getPid());

            PartName partName = new PartName();

            scorePart.setPartName(partName);
            partName.setContent(current.part.getName());

            // ScorePart in scorePartwise
            current.pmPart = new proxymusic.Part();
            scorePartwise.getPart()
                         .add(current.pmPart);
            current.pmPart.setId(scorePart);

            // Delegate to children the filling of measures
            logger.fine("Populating " + current.part);
            isFirst.system = true; // TBD: to be reviewed when adding pages
            slurNumbers.clear(); // Reset slur numbers
            score.acceptChildren(this);

            // Next part, if any
            isFirst.part = false;
        }

        return false; // That's all
    }

    //------------//
    // visit Slur //
    //------------//
    @Override
    public boolean visit (Slur slur)
    {
        ///logger.info("Visiting " + slur);
        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    /**
     * Allocate/populate everything that directly relates to this system in the
     * current part. The rest of processing is directly delegated to the
     * measures (TBD: add the slurs ?)
     *
     * @param system visit the system to export
     */
    @Override
    public boolean visit (System system)
    {
        ///logger.info("Visiting " + system);
        current.system = system;

        SystemPart systemPart = (SystemPart) system.getParts()
                                                   .get(
            current.part.getId() - 1);

        // Loop on measures
        isFirst.measure = true;

        for (TreeNode node : systemPart.getMeasures()) {
            Measure measure = (Measure) node;
            // Delegate to measure
            measure.accept(this);
            isFirst.measure = false;
        }

        isFirst.system = false;

        return false; // No default browsing this way
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    @Override
    public boolean visit (TimeSignature timeSignature)
    {
        ///logger.info("Visiting " + timeSignature);
        Time time = new Time();
        getMeasureAttributes()
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
        if (timeSignature.getShape() != null) {
            switch (timeSignature.getShape()) {
            case COMMON_TIME :
                time.setSymbol(COMMON);

                break;

            case CUT_TIME :
                time.setSymbol(CUT);

                break;
            }
        }

        return true;
    }

    //-------------//
    // visit Pedal //
    //-------------//
    @Override
    public boolean visit (Pedal pedal)
    {
        Direction direction = new Direction();
        current.pmMeasure.getNoteOrBackupOrForward()
                         .add(direction);

        DirectionType directionType = new DirectionType();
        direction.getDirectionType()
                 .add(directionType);

        proxymusic.Pedal pmPedal = new proxymusic.Pedal();
        directionType.setPedal(pmPedal);

        // No line (for the time being)
        pmPedal.setLine(NO);

        // Staff ?
        Staff staff = current.note.getStaff();

        if (current.note.getPart()
                        .getStaves()
                        .size() > 1) {
            proxymusic.Staff pmStaff = new proxymusic.Staff();
            direction.setStaff(pmStaff);
            pmStaff.setContent("" + staff.getId());
        }

        // Placement
        if (pedal.getPoint().y < current.note.getCenter().y) {
            direction.setPlacement(ABOVE);
        } else {
            direction.setPlacement(BELOW);
        }

        // default-y
        PixelPoint pix = current.system.toPixelPoint(pedal.getPoint());
        PixelPoint staffPix = new PixelPoint(
            pix.x,
            staff.getInfo().getFirstLine().getLine().yAt(pix.x));
        pmPedal.setDefaultY(
            toTenths(pedal.getScale().pixelsToUnits(staffPix.y - pix.y)));

        // Start / Stop
        if (pedal.isStart()) {
            // type
            pmPedal.setType(START);
        } else {
            pmPedal.setType(STOP);
        }

        // Relative-x (No offset for the time being) using note left side
        pmPedal.setRelativeX(
            toTenths(pedal.getPoint().x - current.note.getCenterLeft().x));

        return true;
    }

    //-------------//
    // visit Wedge //
    //-------------//
    @Override
    public boolean visit (Wedge wedge)
    {
        Direction direction = new Direction();
        current.pmMeasure.getNoteOrBackupOrForward()
                         .add(direction);

        DirectionType directionType = new DirectionType();
        direction.getDirectionType()
                 .add(directionType);

        proxymusic.Wedge pmWedge = new proxymusic.Wedge();
        directionType.setWedge(pmWedge);

        // Spread
        pmWedge.setSpread(toTenths(wedge.getSpread()));

        // Start or stop ?
        if (wedge.isStart()) {
            // Type
            if (wedge.getShape() == Shape.CRESCENDO) {
                pmWedge.setType("crescendo");
            } else {
                pmWedge.setType("diminuendo");
            }

            // Staff ?
            Staff staff = current.note.getStaff();

            if (current.note.getPart()
                            .getStaves()
                            .size() > 1) {
                proxymusic.Staff pmStaff = new proxymusic.Staff();
                direction.setStaff(pmStaff);
                pmStaff.setContent("" + staff.getId());
            }

            // Placement
            if (wedge.getPoint().y < current.note.getCenter().y) {
                direction.setPlacement(ABOVE);
            } else {
                direction.setPlacement(BELOW);
            }

            // default-y
            PixelPoint pix = current.system.toPixelPoint(wedge.getPoint());
            PixelPoint staffPix = new PixelPoint(
                pix.x,
                staff.getInfo().getFirstLine().getLine().yAt(pix.x));
            pmWedge.setDefaultY(
                toTenths(wedge.getScale().pixelsToUnits(staffPix.y - pix.y)));
        } else { // It's a stop
            pmWedge.setType(STOP);
        }

        // Relative-x (No offset for the time being) using note left side
        pmWedge.setRelativeX(
            toTenths(wedge.getPoint().x - current.note.getCenterLeft().x));

        return true;
    }

    //- Utility Methods --------------------------------------------------------

    //----------------------//
    // getMeasureAttributes //
    //----------------------//
    /**
     * Report (after creating it if necessary) the measure attributes element
     *
     * @return the measure attributes element
     */
    private Attributes getMeasureAttributes ()
    {
        if (current.attributes == null) {
            current.attributes = new Attributes();
            current.pmMeasure.getNoteOrBackupOrForward()
                             .add(current.attributes);
        }

        return current.attributes;
    }

    //--------------//
    // insertBackup //
    //--------------//
    private void insertBackup (int delta)
    {
        Backup backup = new Backup();
        current.pmMeasure.getNoteOrBackupOrForward()
                         .add(backup);

        Duration duration = new Duration();
        backup.setDuration(duration);
        duration.setContent("" + current.part.simpleDurationOf(delta));
    }

    //---------------//
    // insertForward //
    //---------------//
    private void insertForward (int   delta,
                                Chord chord)
    {
        Forward forward = new Forward();
        current.pmMeasure.getNoteOrBackupOrForward()
                         .add(forward);

        Duration duration = new Duration();
        forward.setDuration(duration);
        duration.setContent("" + current.part.simpleDurationOf(delta));

        Voice voice = new Voice();
        forward.setVoice(voice);
        voice.setContent("" + current.voice);

        // Staff ? (only if more than one staff in part)
        if (chord.getPart()
                 .getStaves()
                 .size() > 1) {
            proxymusic.Staff pmStaff = new proxymusic.Staff();
            forward.setStaff(pmStaff);
            pmStaff.setContent("" + (chord.getStaff().getId()));
        }
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
            new SystemPoint(clef.getCenter().x - 1, 0));

        if (previousClef != null) {
            return previousClef.getShape() != clef.getShape();
        }

        return true; // Since no previous clef found
    }

    //-------------------//
    // isNewKeySignature //
    //-------------------//
    /**
     * Make sure we have a NEW key, not already assigned. We have to go back
     * in current measure, then in current staff, then in same staff in previous
     * systems, until we find a previous key. And we compare the two shapes.
     * @param key the potentially new key
     * @return true if this key is really new
     */
    private boolean isNewKeySignature (KeySignature key)
    {
        // Perhaps another key before this one ?
        KeySignature previousKey = current.measure.getKeyBefore(
            key.getCenter());

        if (previousKey != null) {
            return previousKey.getKey() != key.getKey();
        }

        return true; // Since no previous key found
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Current //
    //---------//
    /** Keep references of all current entities */
    private static class Current
    {
        proxymusic.Part       pmPart;
        ScorePart             part;
        System                system;
        proxymusic.Measure    pmMeasure;
        Measure               measure;
        omr.score.Note        note;
        proxymusic.Print      pmPrint;
        proxymusic.Attributes attributes;
        int                   voice;
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

        /** We are writing the first measure in current system (in current part) */
        boolean measure;

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

            return sb.toString();
        }
    }
}
