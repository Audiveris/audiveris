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
import omr.OmrExecutors;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.score.Arpeggiate;
import omr.score.Barline;
import omr.score.Beam;
import omr.score.Chord;
import omr.score.Clef;
import omr.score.Coda;
import omr.score.Dynamics;
import omr.score.Fermata;
import omr.score.KeySignature;
import omr.score.Measure;
import omr.score.Notation;
import omr.score.Ornament;
import omr.score.Pedal;
import omr.score.Score;
import omr.score.ScorePart;
import omr.score.Segno;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.System;
import omr.score.SystemPart;
import omr.score.SystemPoint;
import omr.score.SystemRectangle;
import omr.score.TimeSignature;
import omr.score.TimeSignature.InvalidTimeSignature;
import omr.score.Tuplet;
import omr.score.Wedge;
import static omr.score.visitor.MusicXML.*;

import omr.util.Logger;
import omr.util.TreeNode;

import proxymusic.*;

import proxymusic.util.Marshalling;

import java.io.*;
import java.lang.String;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.Executor;

import javax.xml.bind.JAXBException;

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

    /** Map of Slur numbers, reset for every part */
    private Map<Slur, Integer> slurNumbers = new HashMap<Slur, Integer>();

    /** Map of Tuplet numbers, reset for every measure */
    private Map<Tuplet, Integer> tupletNumbers = new HashMap<Tuplet, Integer>();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // ScoreExporter //
    //---------------//
    /**
     * Create a new ScoreExporter object, which triggers the export.
     *
     * @param score the score to export (cannot be null)
     * @param xmlFile the xml file to write (cannot be null)
     */
    public ScoreExporter (Score score,
                          File  xmlFile)
    {
        if (score == null) {
            throw new IllegalArgumentException("Trying to export a null score");
        }

        if (xmlFile == null) {
            throw new IllegalArgumentException(
                "Trying to export a score to a null file");
        }

        this.score = score;

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
        Executor executor = OmrExecutors.getLowExecutor();

        executor.execute(
            new Runnable() {
                    public void run ()
                    {
                        try {
                            Marshalling.getContext();
                        } catch (JAXBException ex) {
                            ex.printStackTrace();
                            logger.warning("Error preloading JaxbContext");
                        }
                    }
                });
    }

    //------------------//
    // visit Arpeggiate //
    //------------------//
    @Override
    public boolean visit (Arpeggiate arpeggiate)
    {
        proxymusic.Arpeggiate pmArpeggiate = new proxymusic.Arpeggiate();
        getNotations()
            .getTiedOrSlurOrTuplet()
            .add(pmArpeggiate);

        // relative-x
        pmArpeggiate.setRelativeX(
            toTenths(arpeggiate.getPoint().x - current.note.getCenterLeft().x));

        // number ???
        // TBD
        return false;
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
        logger.severe("Chord objects should not be visited by ScoreExporter");

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

    //------------//
    // visit Coda //
    //------------//
    @Override
    public boolean visit (Coda coda)
    {
        Direction direction = new Direction();
        current.pmMeasure.getNoteOrBackupOrForward()
                         .add(direction);

        DirectionType directionType = new DirectionType();
        direction.getDirectionType()
                 .add(directionType);

        proxymusic.Coda pmCoda = new proxymusic.Coda();
        directionType.getCoda()
                     .add(pmCoda);

        // Staff ?
        Staff staff = current.note.getStaff();
        insertStaffId(direction, staff);

        // default-x
        pmCoda.setDefaultX(
            toTenths(coda.getPoint().x - current.measure.getLeftX()));

        // default-y
        pmCoda.setDefaultY(yOf(coda.getPoint(), staff));

        // Need also a Sound element
        Sound sound = new Sound();
        direction.setSound(sound);
        sound.setCoda("" + current.measure.getId());
        sound.setDivisions(
            "" +
            current.part.simpleDurationOf(omr.score.Note.QUARTER_DURATION));

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
        insertStaffId(direction, staff);

        // Placement
        if (dynamics.getPoint().y < current.note.getCenter().y) {
            direction.setPlacement(ABOVE);
        } else {
            direction.setPlacement(BELOW);
        }

        // default-y
        pmDynamics.setDefaultY(yOf(dynamics.getPoint(), staff));

        // Relative-x (No offset for the time being) using note left side
        pmDynamics.setRelativeX(
            toTenths(dynamics.getPoint().x - current.note.getCenterLeft().x));

        return false;
    }

    //---------------//
    // visit Fermata //
    //---------------//
    @Override
    public boolean visit (Fermata fermata)
    {
        proxymusic.Fermata pmFermata = new proxymusic.Fermata();
        getNotations()
            .getTiedOrSlurOrTuplet()
            .add(pmFermata);

        // default-y (of the fermata dot)
        // For upright we use bottom of the box, for inverted the top of the box
        SystemRectangle box = fermata.getBox();
        SystemPoint     dot;

        if (fermata.getShape() == Shape.FERMATA_BELOW) {
            dot = new SystemPoint(box.x + (box.width / 2), box.y);
        } else {
            dot = new SystemPoint(box.x + (box.width / 2), box.y + box.height);
        }

        pmFermata.setDefaultY(yOf(dot, current.note.getStaff()));

        // Type
        pmFermata.setType(
            (fermata.getShape() == Shape.FERMATA) ? UPRIGHT : INVERTED);

        return false;
    }

    //--------------------//
    // visit KeySignature //
    //--------------------//
    @Override
    public boolean visit (KeySignature keySignature)
    {
        ///logger.info("Visiting " + keySignature);
        if (isNewKeySignature(keySignature)) {
            Key key = new Key();
            getMeasureAttributes()
                .setKey(key);

            Fifths fifths = new Fifths();
            key.setFifths(fifths);
            fifths.setContent("" + keySignature.getKey());

            // For 2.0 and on, add default-y
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
                    current.part.simpleDurationOf(
                        omr.score.Note.QUARTER_DURATION));

                // Number of staves, if > 1
                if (current.part.isMultiStaff()) {
                    Staves staves = new Staves();
                    getMeasureAttributes()
                        .setStaves(staves);
                    staves.setContent(
                        "" + measure.getPart().getStaves().size());
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
        try {
            int timeCounter = 0;

            for (int voice = 1; voice <= measure.getVoicesNumber(); voice++) {
                current.voice = voice;

                // Need a backup ?
                if (timeCounter != 0) {
                    insertBackup(timeCounter);
                    timeCounter = 0;
                }

                Chord lastChord = null;

                for (TreeNode nc : measure.getChords()) {
                    Chord chord = (Chord) nc;

                    if (chord.getVoice() == voice) {
                        // Need a forward before this chord ?
                        int startTime = chord.getStartTime();

                        if (timeCounter < startTime) {
                            insertForward(startTime - timeCounter, chord);
                            timeCounter = startTime;
                        }

                        // Delegate to the chord children directly
                        chord.acceptChildren(this);
                        lastChord = chord;
                        timeCounter += ((chord.getDuration() != null)
                                        ? chord.getDuration()
                                        : measure.getExpectedDuration());
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
        } catch (InvalidTimeSignature ex) {
        }

        // Safer...
        current.endMeasure();
        tupletNumbers.clear();

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    @Override
    public boolean visit (omr.score.Note note)
    {
        ///logger.info(note.getContextString() + " Visiting " + note);
        current.note = note;

        Chord chord = note.getChord();

        // Chord direction  events for first note in chord
        if (chord.getNotes()
                 .indexOf(note) == 0) {
            for (omr.score.Direction node : chord.getDirections()) {
                node.accept(this);
            }
        }

        current.pmNote = new Note();
        current.pmMeasure.getNoteOrBackupOrForward()
                         .add(current.pmNote);

        Staff staff = note.getStaff();

        // Chord notation events for first note in chord
        if (chord.getNotes()
                 .indexOf(note) == 0) {
            for (Notation node : chord.getNotations()) {
                node.accept(this);
            }
        } else {
            // Chord indication for every other note
            current.pmNote.setChord(new proxymusic.Chord());

            // Arpeggiate also?
            for (Notation node : chord.getNotations()) {
                if (node instanceof Arpeggiate) {
                    node.accept(this);
                }
            }
        }

        // Rest ?
        if (note.isRest()) {
            Rest rest = new Rest();
            current.pmNote.setRest(rest);
        } else {
            // Pitch
            Pitch pitch = new Pitch();
            current.pmNote.setPitch(pitch);

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
        current.pmNote.setDefaultX(
            toTenths(noteLeft - note.getMeasure().getLeftX()));

        // Tuplet factor ?
        if (chord.getTupletFactor() != null) {
            TimeModification timeModification = new TimeModification();
            current.pmNote.setTimeModification(timeModification);

            ActualNotes actualNotes = new ActualNotes();
            timeModification.setActualNotes(actualNotes);
            actualNotes.setContent(
                "" + chord.getTupletFactor().getDenominator());

            NormalNotes normalNotes = new NormalNotes();
            timeModification.setNormalNotes(normalNotes);
            normalNotes.setContent("" + chord.getTupletFactor().getNumerator());
        }

        // Duration
        try {
            Duration duration = new Duration();
            current.pmNote.setDuration(duration);

            Integer dur = chord.getDuration();

            if (dur == null) { // Case of whole rests
                dur = chord.getMeasure()
                           .getExpectedDuration();
            }

            duration.setContent("" + current.part.simpleDurationOf(dur));
        } catch (InvalidTimeSignature ex) {
        }

        // Voice
        Voice voice = new Voice();
        current.pmNote.setVoice(voice);
        voice.setContent("" + chord.getVoice());

        // Type
        Type type = new Type();
        current.pmNote.setType(type);
        type.setContent("" + note.getTypeName());

        // Stem ?
        if (chord.getStem() != null) {
            Glyph stem = chord.getStem();
            Stem  pmStem = new Stem();
            current.pmNote.setStem(pmStem);

            SystemPoint tail = chord.getTailLocation();
            pmStem.setDefaultY(yOf(tail, staff));

            if (tail.y < note.getCenter().y) {
                pmStem.setContent(UP);
            } else {
                pmStem.setContent(DOWN);
            }
        }

        // Staff ?
        insertStaffId(current.pmNote, staff);

        // Dots
        for (int i = 0; i < chord.getDotsNumber(); i++) {
            current.pmNote.getDot()
                          .add(new Dot());
        }

        // Accidental ?
        if (note.getAccidental() != null) {
            Accidental accidental = new Accidental();
            current.pmNote.setAccidental(accidental);
            accidental.setContent(accidentalNameOf(note.getAccidental()));
        }

        // Beams ?
        for (Beam beam : chord.getBeams()) {
            proxymusic.Beam pmBeam = new proxymusic.Beam();
            current.pmNote.getBeam()
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

        // Ties / Slurs
        for (Slur slur : note.getSlurs()) {
            slur.accept(this);
        }

        // Safer...
        current.endNote();

        return true;
    }

    //----------------//
    // visit Ornament //
    //----------------//
    @Override
    public boolean visit (Ornament ornament)
    {
        Object element = getOrnamentObject(ornament.getShape());

        // Include in ornaments
        getOrnaments()
            .getTrillMarkOrTurnOrDelayedTurn()
            .add(element);

        // Placement?
        Class classe = element.getClass();

        try {
            Method method = classe.getMethod(
                "setPlacement",
                java.lang.String.class);
            method.invoke(
                element,
                (ornament.getPoint().y < current.note.getCenter().y) ? ABOVE
                                : BELOW);
        } catch (Exception ex) {
            ///ex.printStackTrace();
            logger.severe("Could not setPlacement for element " + classe);
        }

        return false;
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

        // Start / Stop type
        pmPedal.setType(pedal.isStart() ? START : STOP);

        // Staff ?
        Staff staff = current.note.getStaff();
        insertStaffId(direction, staff);

        // default-x
        pmPedal.setDefaultX(
            toTenths(pedal.getPoint().x - current.measure.getLeftX()));

        // default-y
        pmPedal.setDefaultY(yOf(pedal.getPoint(), staff));

        // Placement
        direction.setPlacement(
            (pedal.getPoint().y < current.note.getCenter().y) ? ABOVE : BELOW);

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
     * @return false, since no further processing is required after this node
     */
    @Override
    public boolean visit (Score score)
    {
        ///logger.info("Visiting " + score);

        // Reset durations for each part
        for (ScorePart scorePart : score.getPartList()) {
            scorePart.resetDurations();
        }

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
        encodingDate.setContent(java.lang.String.format("%tF", new Date()));

        // Defaults
        Defaults defaults = new Defaults();
        scorePartwise.setDefaults(defaults);

        // [Defaults]/Scaling
        Scaling scaling = new Scaling();
        defaults.setScaling(scaling);

        Millimeters millimeters = new Millimeters();
        scaling.setMillimeters(millimeters);
        millimeters.setContent(
            java.lang.String.format(
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

    //-------------//
    // visit Segno //
    //-------------//
    @Override
    public boolean visit (Segno segno)
    {
        Direction direction = new Direction();
        current.pmMeasure.getNoteOrBackupOrForward()
                         .add(direction);

        DirectionType directionType = new DirectionType();
        direction.getDirectionType()
                 .add(directionType);

        proxymusic.Segno pmSegno = new proxymusic.Segno();
        directionType.getSegno()
                     .add(pmSegno);

        // Staff ?
        Staff staff = current.note.getStaff();
        insertStaffId(direction, staff);

        // default-x
        pmSegno.setDefaultX(
            toTenths(segno.getPoint().x - current.measure.getLeftX()));

        // default-y
        pmSegno.setDefaultY(yOf(segno.getPoint(), staff));

        // Need also a Sound element
        Sound sound = new Sound();
        direction.setSound(sound);
        sound.setSegno("" + current.measure.getId());
        sound.setDivisions(
            "" +
            current.part.simpleDurationOf(omr.score.Note.QUARTER_DURATION));

        return true;
    }

    //------------//
    // visit Slur //
    //------------//
    @Override
    public boolean visit (Slur slur)
    {
        ///logger.info("Visiting " + slur);

        // Note contextual data
        boolean isStart = slur.getLeftNote() == current.note;
        int     noteLeft = current.note.getCenterLeft().x;
        Staff   staff = current.note.getStaff();

        if (slur.isTie()) {
            // Tie element
            Tie tie = new Tie();
            current.pmNote.getTie()
                          .add(tie);
            tie.setType(isStart ? START : STOP);

            // Tied element
            Tied tied = new Tied();
            getNotations()
                .getTiedOrSlurOrTuplet()
                .add(tied);

            // Type
            tied.setType(isStart ? START : STOP);

            // Orientation
            if (isStart) {
                tied.setOrientation(slur.isBelow() ? UNDER : OVER);
            }

            // Bezier
            if (isStart) {
                tied.setDefaultX(toTenths(slur.getCurve().getX1() - noteLeft));
                tied.setDefaultY(yOf(slur.getCurve().getY1(), staff));
                tied.setBezierX(
                    toTenths(slur.getCurve().getCtrlX1() - noteLeft));
                tied.setBezierY(yOf(slur.getCurve().getCtrlY1(), staff));
            } else {
                tied.setDefaultX(toTenths(slur.getCurve().getX2() - noteLeft));
                tied.setDefaultY(yOf(slur.getCurve().getY2(), staff));
                tied.setBezierX(
                    toTenths(slur.getCurve().getCtrlX2() - noteLeft));
                tied.setBezierY(yOf(slur.getCurve().getCtrlY2(), staff));
            }
        } else {
            // Slur element
            proxymusic.Slur pmSlur = new proxymusic.Slur();
            getNotations()
                .getTiedOrSlurOrTuplet()
                .add(pmSlur);

            // Number attribute
            Integer num = slurNumbers.get(slur);

            if (num != null) {
                pmSlur.setNumber(num.toString());
                slurNumbers.remove(slur);

                if (logger.isFineEnabled()) {
                    logger.fine(
                        current.note.getContextString() + " last use " + num +
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
                                current.note.getContextString() +
                                " first use " + num + " -> " +
                                slurNumbers.toString());
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
            // Delegate to measure
            ((Measure) node).accept(this);
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
        try {
            Time  time = new Time();

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

            getMeasureAttributes()
                .setTime(time);
        } catch (InvalidTimeSignature ex) {
        }

        return true;
    }

    //--------------//
    // visit Tuplet //
    //--------------//
    public boolean visit (Tuplet tuplet)
    {
        //          <tuplet bracket="no" number="1" placement="above" type="start"/>
        proxymusic.Tuplet pmTuplet = new proxymusic.Tuplet();
        getNotations()
            .getTiedOrSlurOrTuplet()
            .add(pmTuplet);

        // bracket
        // TBD

        // placement
        if (tuplet.getChord() == current.note.getChord()) {
            pmTuplet.setPlacement(
                (tuplet.getCenter().y <= current.note.getCenter().y) ? ABOVE
                                : BELOW);
        }

        // type
        pmTuplet.setType(
            (tuplet.getChord() == current.note.getChord()) ? START : STOP);

        // Number attribute
        Integer num = tupletNumbers.get(tuplet);

        if (num != null) {
            pmTuplet.setNumber(num.toString());
            tupletNumbers.remove(tuplet); // Release the number
        } else {
            // Determine first available number
            for (num = 1; num <= 6; num++) {
                if (!tupletNumbers.containsValue(num)) {
                    tupletNumbers.put(tuplet, num);
                    pmTuplet.setNumber(num.toString());

                    break;
                }
            }
        }

        return false;
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
            pmWedge.setType(
                (wedge.getShape() == Shape.CRESCENDO) ? MusicXML.CRESCENDO
                                : MusicXML.DIMINUENDO);

            // Staff ?
            Staff staff = current.note.getStaff();
            insertStaffId(direction, staff);

            // Placement
            direction.setPlacement(
                (wedge.getPoint().y < current.note.getCenter().y) ? ABOVE : BELOW);

            // default-y
            pmWedge.setDefaultY(yOf(wedge.getPoint(), staff));
        } else { // It's a stop
            pmWedge.setType(STOP);
        }

        //        // Relative-x (No offset for the time being) using note left side
        //        pmWedge.setRelativeX(
        //            toTenths(wedge.getPoint().x - current.note.getCenterLeft().x));

        // default-x
        pmWedge.setDefaultX(
            toTenths(wedge.getPoint().x - current.measure.getLeftX()));

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
    // getNotations //
    //--------------//
    /**
     * Report (after creating it if necessary) the notations element of the
     * current note
     *
     * @return the note notations element
     */
    private Notations getNotations ()
    {
        // Notations allocated?
        if (current.notations == null) {
            current.notations = new Notations();
            current.pmNote.getNotations()
                          .add(current.notations);
        }

        return current.notations;
    }

    //--------------//
    // getOrnaments //
    //--------------//
    /**
     * Report (after creating it if necessary) the ornaments elements in the
     * notations element of the current note
     *
     * @return the note notations ornaments element
     */
    private Ornaments getOrnaments ()
    {
        for (Object obj : getNotations()
                              .getTiedOrSlurOrTuplet()) {
            if (obj instanceof Ornaments) {
                return (Ornaments) obj;
            }
        }

        // Need to allocate ornaments
        Ornaments ornaments = new Ornaments();
        getNotations()
            .getTiedOrSlurOrTuplet()
            .add(ornaments);

        return ornaments;
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
        insertStaffId(forward, chord.getStaff());
    }

    //---------------//
    // insertStaffId //
    //---------------//
    /**
     * If needed (if current part contains more than one staff), we insert the
     * id of the staff related to the element at hand
     *
     * @param obj the element at hand
     * @staff the related score staff
     */
    private void insertStaffId (Object obj,
                                Staff  staff)
    {
        if (current.part.isMultiStaff()) {
            Class            classe = obj.getClass();
            proxymusic.Staff pmStaff = new proxymusic.Staff();
            pmStaff.setContent("" + staff.getId());

            try {
                Method method = classe.getMethod(
                    "setStaff",
                    proxymusic.Staff.class);
                method.invoke(obj, pmStaff);
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.severe("Could not setStaff for element " + classe);
            }
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
        // Part dependent
        ScorePart             part;
        proxymusic.Part       pmPart;

        // System dependent
        System                system;

        // Measure dependent
        Measure               measure;
        proxymusic.Measure    pmMeasure;
        Integer               voice;

        // Note dependent
        omr.score.Note        note;
        proxymusic.Note       pmNote;
        proxymusic.Notations  notations;
        proxymusic.Print      pmPrint;
        proxymusic.Attributes attributes;

        // Cleanup at end of measure
        void endMeasure ()
        {
            measure = null;
            pmMeasure = null;
            voice = null;

            endNote();
        }

        // Cleanup at end of note
        void endNote ()
        {
            note = null;
            pmNote = null;
            notations = null;
            pmPrint = null;
            attributes = null;
        }
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
        public java.lang.String toString ()
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
