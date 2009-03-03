//----------------------------------------------------------------------------//
//                                                                            //
//                         S c o r e E x p o r t e r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.Main;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.log.Logger;

import omr.score.MeasureRange;
import omr.score.Score;
import omr.score.common.PagePoint;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.entity.Arpeggiate;
import omr.score.entity.Barline;
import omr.score.entity.Beam;
import omr.score.entity.Chord;
import omr.score.entity.Clef;
import omr.score.entity.Coda;
import omr.score.entity.DirectionStatement;
import omr.score.entity.Dynamics;
import omr.score.entity.Fermata;
import omr.score.entity.KeySignature;
import omr.score.entity.LyricsItem;
import omr.score.entity.Measure;
import omr.score.entity.Notation;
import omr.score.entity.Ornament;
import omr.score.entity.Pedal;
import omr.score.entity.ScorePart;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Segno;
import omr.score.entity.Slot;
import omr.score.entity.Slur;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.entity.Text;
import omr.score.entity.Text.CreatorText;
import omr.score.entity.TimeSignature;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.entity.Tuplet;
import omr.score.entity.Voice;
import omr.score.entity.Voice.ChordInfo;
import omr.score.entity.Wedge;
import omr.score.midi.MidiAbstractions;
import static omr.score.visitor.MusicXML.*;

import omr.util.BasicTask;
import omr.util.TreeNode;

import org.w3c.dom.Node;

import proxymusic.*;

import proxymusic.util.Marshalling;

import java.awt.Font;
import java.io.*;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

/**
 * Class <code>ScoreExporter</code> can visit the score hierarchy to export
 * the score to a MusicXML file, stream or DOM.
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

    /** A future which reflects whether JAXB has been initialized **/
    private static final LoadTask loading = new LoadTask();

    static {
        loading.execute();
    }

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

    /** Potential range of selected measures */
    private MeasureRange measureRange;

    /** Factory for proxymusic entities */
    private final proxymusic.ObjectFactory factory = new proxymusic.ObjectFactory();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // ScoreExporter //
    //---------------//
    /**
     * Create a new ScoreExporter object, on a related score instance
     *
     * @param score the score to export (cannot be null)
     */
    public ScoreExporter (Score score)
    {
        if (score == null) {
            throw new IllegalArgumentException("Trying to export a null score");
        }

        this.score = score;
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // preload //
    //---------//
    /**
     * Empty static method, just to trigger class elaboration
     */
    public static void preload ()
    {
    }

    //-----------------//
    // setMeasureRange //
    //-----------------//
    /**
     * Set a specific range of measures to export
     *
     * @param measureRange the range of desired measures
     */
    public void setMeasureRange (MeasureRange measureRange)
    {
        this.measureRange = measureRange;
    }

    //--------//
    // export //
    //--------//
    /**
     * Export the score to a file
     *
     * @param xmlFile the xml file to write (cannot be null)
     * @throws java.lang.Exception
     */
    public void export (File xmlFile)
        throws Exception
    {
        export(new FileOutputStream(xmlFile));
    }

    //--------//
    // export //
    //--------//
    /**
     * Export the score to an output stream
     *
     * @param os the output stream where XML data is written (cannot be null)
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    public void export (OutputStream os)
        throws IOException, Exception
    {
        if (os == null) {
            throw new IllegalArgumentException(
                "Trying to export a score to a null output stream");
        }

        // Let visited nodes fill the scorePartWise proxy
        score.accept(this);

        //  Finally, marshal the proxy
        Marshalling.marshal(scorePartwise, os);
    }

    //--------//
    // export //
    //--------//
    /**
     * Export the score to DOM node
     *
     * @param node the DOM node to export to (cannot be null)
     * @throws java.io.IOException
     * @throws java.lang.Exception
     */
    public void export (Node node)
        throws IOException, Exception
    {
        if (node == null) {
            throw new IllegalArgumentException(
                "Trying to export a score to a null DOM Node");
        }

        // Let visited nodes fill the scorePartWise proxy
        score.accept(this);

        //  Finally, marshal the proxy
        Marshalling.marshal(scorePartwise, node, /* Signature => */
                            true);
    }

    //- All Visiting Methods ---------------------------------------------------

    //------------------//
    // visit Arpeggiate //
    //------------------//
    @Override
    public boolean visit (Arpeggiate arpeggiate)
    {
        proxymusic.Arpeggiate pmArpeggiate = factory.createArpeggiate();
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
        if (barline == null) {
            return false;
        }

        Shape shape = barline.getShape();

        if (shape != omr.glyph.Shape.THIN_BARLINE) {
            try {
                proxymusic.Barline       pmBarline = factory.createBarline();
                proxymusic.BarStyleColor barStyleColor = factory.createBarStyleColor();

                pmBarline.setBarStyle(barStyleColor);

                if (barline == current.measure.getBarline()) {
                    // The bar is on right side
                    pmBarline.setLocation(RightLeftMiddle.RIGHT);

                    if ((shape == RIGHT_REPEAT_SIGN) ||
                        (shape == BACK_TO_BACK_REPEAT_SIGN)) {
                        barStyleColor.setValue(BarStyle.LIGHT_HEAVY);

                        Repeat repeat = factory.createRepeat();
                        pmBarline.setRepeat(repeat);
                        repeat.setDirection(BackwardForward.BACKWARD);
                    }
                } else {
                    // The bar is on left side
                    pmBarline.setLocation(RightLeftMiddle.LEFT);

                    if ((shape == LEFT_REPEAT_SIGN) ||
                        (shape == BACK_TO_BACK_REPEAT_SIGN)) {
                        barStyleColor.setValue(BarStyle.HEAVY_LIGHT);

                        Repeat repeat = factory.createRepeat();
                        pmBarline.setRepeat(repeat);
                        repeat.setDirection(BackwardForward.FORWARD);
                    }
                }

                // Default: use style inferred from shape
                if (barStyleColor.getValue() == null) {
                    barStyleColor.setValue(barStyleOf(barline.getShape()));
                }

                // Everything is now OK
                current.pmMeasure.getNoteOrBackupOrForward()
                                 .add(pmBarline);
            } catch (Exception ex) {
                logger.warning("Cannot visit barline", ex);
            }
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
            proxymusic.Clef pmClef = factory.createClef();
            getMeasureAttributes()
                .getClef()
                .add(pmClef);

            // Staff number (only for multi-staff parts)
            if (current.part.getStaffIds()
                            .size() > 1) {
                pmClef.setNumber(
                    new BigInteger("" + (clef.getStaff().getId())));
            }

            // Line (General computation that could be overridden by more
            // specific shape test below)
            pmClef.setLine(
                new BigInteger(
                    "" + (3 - (int) Math.rint(clef.getPitchPosition() / 2.0))));

            Shape shape = clef.getShape();

            switch (shape) {
            case G_CLEF :
                pmClef.setSign(ClefSign.G);

                break;

            case G_CLEF_OTTAVA_ALTA :
                pmClef.setSign(ClefSign.G);
                pmClef.setClefOctaveChange(new BigInteger("1"));

                break;

            case G_CLEF_OTTAVA_BASSA :
                pmClef.setSign(ClefSign.G);
                pmClef.setClefOctaveChange(new BigInteger("-1"));

                break;

            case C_CLEF :
                pmClef.setSign(ClefSign.C);

                break;

            case F_CLEF :
                pmClef.setSign(ClefSign.F);

                break;

            case F_CLEF_OTTAVA_ALTA :
                pmClef.setSign(ClefSign.F);
                pmClef.setClefOctaveChange(new BigInteger("1"));

                break;

            case F_CLEF_OTTAVA_BASSA :
                pmClef.setSign(ClefSign.F);
                pmClef.setClefOctaveChange(new BigInteger("-1"));

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
        Direction direction = factory.createDirection();
        current.pmMeasure.getNoteOrBackupOrForward()
                         .add(direction);

        DirectionType directionType = new DirectionType();
        direction.getDirectionType()
                 .add(directionType);

        proxymusic.EmptyPrintStyle pmCoda = factory.createEmptyPrintStyle();
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
        Sound sound = factory.createSound();
        direction.setSound(sound);
        sound.setCoda("" + current.measure.getId());
        sound.setDivisions(
            createDecimal(
                score.simpleDurationOf(omr.score.entity.Note.QUARTER_DURATION)));

        return true;
    }

    //----------------//
    // visit Dynamics //
    //----------------//
    @Override
    public boolean visit (Dynamics dynamics)
    {
        ///logger.info("Visiting " + dynamics);
        try {
            Direction direction = factory.createDirection();
            current.pmMeasure.getNoteOrBackupOrForward()
                             .add(direction);

            DirectionType directionType = factory.createDirectionType();
            direction.getDirectionType()
                     .add(directionType);

            proxymusic.Dynamics pmDynamics = factory.createDynamics();
            directionType.setDynamics(pmDynamics);

            // Precise dynamic signature
            pmDynamics.getPOrPpOrPpp()
                      .add(getDynamicsObject(dynamics.getShape()));

            // Staff ?
            Staff staff = current.note.getStaff();
            insertStaffId(direction, staff);

            // Placement
            if (dynamics.getPoint().y < current.note.getCenter().y) {
                direction.setPlacement(AboveBelow.ABOVE);
            } else {
                direction.setPlacement(AboveBelow.BELOW);
            }

            // default-y
            pmDynamics.setDefaultY(yOf(dynamics.getPoint(), staff));

            // Relative-x (No offset for the time being) using note left side
            pmDynamics.setRelativeX(
                toTenths(
                    dynamics.getPoint().x - current.note.getCenterLeft().x));
        } catch (Exception ex) {
            logger.warning("Error exporting " + dynamics, ex);
        }

        return false;
    }

    //---------------//
    // visit Fermata //
    //---------------//
    @Override
    public boolean visit (Fermata fermata)
    {
        proxymusic.Fermata pmFermata = factory.createFermata();
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
            (fermata.getShape() == Shape.FERMATA) ? UprightInverted.UPRIGHT
                        : UprightInverted.INVERTED);

        return false;
    }

    //--------------------//
    // visit KeySignature //
    //--------------------//
    @Override
    public boolean visit (KeySignature keySignature)
    {
        ///logger.info("Visiting " + keySignature);
        try {
            if (isNewKeySignature(keySignature)) {
                Key key = factory.createKey();
                key.setFifths(new BigInteger("" + keySignature.getKey()));

                // Trick: add this key signature only if it does not already exist
                List<Key> keys = getMeasureAttributes()
                                     .getKey();

                for (Key k : keys) {
                    if (areEqual(k, key)) {
                        return true; // Already inserted, so give up
                    }
                }

                keys.add(key);
            }
        } catch (Exception ex) {
            logger.warning("Error exporting " + keySignature, ex);
        }

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        // Make sure this measure is to be exported
        if (!isDesired(measure)) {
            if (logger.isFineEnabled()) {
                logger.fine(measure + " skipped.");
            }

            return false;
        }

        ///logger.info("Visiting " + measure);

        // Do we need to create & export a dummy initial measure?
        if (((measureRange != null) && !measure.isTemporary() &&
            (measure.getId() > 1)) &&
            (measure.getId() == measureRange.getFirstId())) {
            visit(measure.createTemporaryBefore());
        }

        if (logger.isFineEnabled()) {
            logger.fine(measure + " : " + isFirst);
        }

        current.measure = measure;
        tupletNumbers.clear();

        // Allocate Measure
        current.pmMeasure = factory.createScorePartwisePartMeasure();
        current.pmPart.getMeasure()
                      .add(current.pmMeasure);
        current.pmMeasure.setNumber("" + measure.getId());

        if (measure.getWidth() != null) {
            current.pmMeasure.setWidth(toTenths(measure.getWidth()));
        }

        if (measure.isImplicit()) {
            current.pmMeasure.setImplicit(YesNo.YES);
        }

        // Right Barline
        if (!measure.isDummy()) {
            visit(measure.getBarline());
        }

        // Left barline ?
        Measure prevMeasure = (Measure) measure.getPreviousSibling();

        if ((prevMeasure != null) && !prevMeasure.isDummy()) {
            visit(prevMeasure.getBarline());
        }

        if (isFirst.measure) {
            // Allocate Print
            current.pmPrint = factory.createPrint();
            current.pmMeasure.getNoteOrBackupOrForward()
                             .add(current.pmPrint);

            if (isFirst.system) {
                // New Page ? TBD

                // Divisions
                try {
                    getMeasureAttributes()
                        .setDivisions(
                        createDecimal(
                            score.simpleDurationOf(
                                omr.score.entity.Note.QUARTER_DURATION)));
                } catch (Exception ex) {
                    if (score.getDurationDivisor() == null) {
                        logger.warning(
                            "Not able to infer division value for part " +
                            current.part.getPid());
                    } else {
                        logger.warning("Error on divisions", ex);
                    }
                }

                // Number of staves, if > 1
                if (current.part.isMultiStaff()) {
                    getMeasureAttributes()
                        .setStaves(
                        new BigInteger("" + current.part.getStaffIds().size()));
                }
            } else {
                // New system
                current.pmPrint.setNewSystem(YesNo.YES);
            }

            if (isFirst.part) {
                // SystemLayout
                SystemLayout systemLayout = factory.createSystemLayout();
                current.pmPrint.setSystemLayout(systemLayout);

                // SystemMargins
                SystemMargins systemMargins = factory.createSystemMargins();
                systemLayout.setSystemMargins(systemMargins);
                systemMargins.setLeftMargin(
                    toTenths(current.system.getTopLeft().x));
                systemMargins.setRightMargin(
                    toTenths(
                        score.getDimension().width -
                        current.system.getTopLeft().x -
                        current.system.getDimension().width));

                if (isFirst.system) {
                    // TopSystemDistance
                    systemLayout.setTopSystemDistance(
                        toTenths(current.system.getTopLeft().y));

                    // Default tempo?
                    if (current.part.getTempo() != null) {
                        Sound sound = factory.createSound();
                        current.pmMeasure.getNoteOrBackupOrForward()
                                         .add(sound);
                        sound.setTempo(createDecimal(current.part.getTempo()));
                    }

                    // Default velocity?
                    if (score.getVelocity() != null) {
                        Sound sound = factory.createSound();
                        current.pmMeasure.getNoteOrBackupOrForward()
                                         .add(sound);
                        sound.setDynamics(createDecimal(score.getVelocity()));
                    }
                } else {
                    // SystemDistance
                    ScoreSystem prevSystem = (ScoreSystem) current.system.getPreviousSibling();
                    systemLayout.setSystemDistance(
                        toTenths(
                            current.system.getTopLeft().y -
                            prevSystem.getTopLeft().y -
                            prevSystem.getDimension().height -
                            prevSystem.getLastPart().getLastStaff().getHeight()));
                }
            }

            // StaffLayout for all staves in this part, except 1st system staff
            if (!measure.isDummy()) {
                for (TreeNode sNode : measure.getPart()
                                             .getStaves()) {
                    Staff staff = (Staff) sNode;

                    if (!isFirst.part || (staff.getId() > 1)) {
                        try {
                            StaffLayout staffLayout = factory.createStaffLayout();
                            staffLayout.setNumber(
                                new BigInteger("" + staff.getId()));

                            Staff prevStaff = (Staff) staff.getPreviousSibling();

                            if (prevStaff == null) {
                                SystemPart prevPart = (SystemPart) measure.getPart()
                                                                          .getPreviousSibling();
                                prevStaff = prevPart.getLastStaff();
                            }

                            staffLayout.setStaffDistance(
                                toTenths(
                                    staff.getPageTopLeft().y -
                                    prevStaff.getPageTopLeft().y -
                                    prevStaff.getHeight()));
                            current.pmPrint.getStaffLayout()
                                           .add(staffLayout);
                        } catch (Exception ex) {
                            logger.warning(
                                "Error exporting staff layout system#" +
                                current.system.getId() + " part#" +
                                current.part.getId() + " staff#" +
                                staff.getId(),
                                ex);
                        }
                    }
                }
            }

            // Do not print artificial parts
            StaffDetails staffDetails = factory.createStaffDetails();
            staffDetails.setPrintObject(
                measure.isDummy() ? YesNo.NO : YesNo.YES);
            getMeasureAttributes()
                .getStaffDetails()
                .add(staffDetails);
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

            for (Voice voice : measure.getVoices()) {
                current.voice = voice;

                // Need a backup ?
                if (timeCounter != 0) {
                    insertBackup(timeCounter);
                    timeCounter = 0;
                }

                if (voice.isWhole()) {
                    // Delegate to the chord children directly
                    voice.getWholeChord()
                         .acceptChildren(this);
                    timeCounter = measure.getExpectedDuration();
                } else {
                    for (Slot slot : measure.getSlots()) {
                        ChordInfo info = voice.getSlotInfo(slot);

                        if (info != null) { // Skip free slots

                            if (info.getStatus() == Voice.Status.BEGIN) {
                                Chord chord = info.getChord();

                                // Need a forward before this chord ?
                                int startTime = chord.getStartTime();

                                if (timeCounter < startTime) {
                                    insertForward(
                                        startTime - timeCounter,
                                        chord);
                                    timeCounter = startTime;
                                }

                                // Delegate to the chord children directly
                                chord.acceptChildren(this);
                                timeCounter += chord.getDuration();
                            }
                        }
                    }

                    // Need an ending forward ?
                    if (!measure.isImplicit() &&
                        !measure.isPartial() &&
                        (timeCounter < measure.getExpectedDuration())) {
                        insertForward(
                            measure.getExpectedDuration() - timeCounter,
                            voice.getLastChord());
                        timeCounter = measure.getExpectedDuration();
                    }
                }
            }
        } catch (InvalidTimeSignature ex) {
        }

        // Safer...
        current.endMeasure();
        tupletNumbers.clear();
        isFirst.measure = false;

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    @Override
    public boolean visit (omr.score.entity.Note note)
    {
        ///logger.info("Visiting " + note);
        try {
            current.note = note;

            Chord chord = note.getChord();

            // Chord direction events for first note in chord
            if (chord.getNotes()
                     .indexOf(note) == 0) {
                for (omr.score.entity.Direction node : chord.getDirections()) {
                    node.accept(this);
                }
            }

            current.pmNote = factory.createNote();
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
                current.pmNote.getContent()
                              .add(factory.createNoteChord(new Empty()));

                // Arpeggiate also?
                for (Notation node : chord.getNotations()) {
                    if (node instanceof Arpeggiate) {
                        node.accept(this);
                    }
                }
            }

            // Rest ?
            if (note.isRest()) {
                DisplayStepOctave displayStepOctave = factory.createDisplayStepOctave();
                /// ??? Set Step or Octave ???
                current.pmNote.getContent()
                              .add(factory.createNoteRest(displayStepOctave));
            } else {
                // Pitch
                Pitch pitch = factory.createPitch();
                pitch.setStep(stepOf(note.getStep()));
                pitch.setOctave(note.getOctave());

                if (note.getAlter() != 0) {
                    pitch.setAlter(createDecimal(note.getAlter()));
                }

                current.pmNote.getContent()
                              .add(factory.createNotePitch(pitch));
            }

            // Default-x (use left side of the note wrt measure)
            if (!note.getMeasure()
                     .isDummy()) {
                int noteLeft = note.getCenterLeft().x;
                current.pmNote.setDefaultX(
                    toTenths(noteLeft - note.getMeasure().getLeftX()));
            }

            // Tuplet factor ?
            if (chord.getTupletFactor() != null) {
                TimeModification timeModification = factory.createTimeModification();
                timeModification.setActualNotes(
                    new BigInteger(
                        "" + chord.getTupletFactor().getDenominator()));
                timeModification.setNormalNotes(
                    new BigInteger("" + chord.getTupletFactor().getNumerator()));
                current.pmNote.getContent()
                              .add(
                    factory.createNoteTimeModification(timeModification));
            }

            // Duration
            try {
                Integer dur = null;

                if (chord.isWholeDuration()) {
                    dur = chord.getMeasure()
                               .getActualDuration();
                } else {
                    dur = chord.getDuration();
                }

                current.pmNote.getContent()
                              .add(
                    factory.createNoteDuration(
                        createDecimal(score.simpleDurationOf(dur))));
            } catch (Exception ex) {
                if (score.getDurationDivisor() != null) {
                    logger.warning("Not able to get duration of note", ex);
                }
            }

            // Voice
            current.pmNote.getContent()
                          .add(
                factory.createNoteVoice("" + chord.getVoice().getId()));

            // Type
            NoteType noteType = factory.createNoteType();
            noteType.setValue("" + getNoteTypeName(note));
            current.pmNote.getContent()
                          .add(factory.createNoteType(noteType));

            // Stem ?
            if (chord.getStem() != null) {
                Stem        pmStem = factory.createStem();
                SystemPoint tail = chord.getTailLocation();
                pmStem.setDefaultY(yOf(tail, staff));

                if (tail.y < note.getCenter().y) {
                    pmStem.setValue(StemValue.UP);
                } else {
                    pmStem.setValue(StemValue.DOWN);
                }

                current.pmNote.getContent()
                              .add(factory.createNoteStem(pmStem));
            }

            // Staff ? 
            if (current.part.isMultiStaff()) {
                current.pmNote.getContent()
                              .add(
                    factory.createNoteStaff(new BigInteger("" + staff.getId())));
            }

            // Dots
            for (int i = 0; i < chord.getDotsNumber(); i++) {
                current.pmNote.getContent()
                              .add(
                    factory.createNoteDot(factory.createEmptyPlacement()));
            }

            // Accidental ?
            if (note.getAccidental() != null) {
                Accidental accidental = factory.createAccidental();
                accidental.setValue(accidentalTextOf(note.getAccidental()));
                current.pmNote.getContent()
                              .add(factory.createNoteAccidental(accidental));
            }

            // Beams ?
            for (Beam beam : chord.getBeams()) {
                proxymusic.Beam pmBeam = factory.createBeam();
                pmBeam.setNumber(beam.getLevel());

                if (beam.isHook()) {
                    if (beam.getCenter().x > current.system.toSystemPoint(
                        chord.getStem().getLocation()).x) {
                        pmBeam.setValue(BeamValue.FORWARD_HOOK);
                    } else {
                        pmBeam.setValue(BeamValue.BACKWARD_HOOK);
                    }
                } else {
                    if (beam.getChords()
                            .first() == chord) {
                        pmBeam.setValue(BeamValue.BEGIN);
                    } else if (beam.getChords()
                                   .last() == chord) {
                        pmBeam.setValue(BeamValue.END);
                    } else {
                        pmBeam.setValue(BeamValue.CONTINUE);
                    }
                }

                current.pmNote.getContent()
                              .add(factory.createNoteBeam(pmBeam));
            }

            // Ties / Slurs
            for (Slur slur : note.getSlurs()) {
                slur.accept(this);
            }

            // Lyrics ?
            if (note.getSyllables() != null) {
                for (LyricsItem syllable : note.getSyllables()) {
                    if (syllable.getContent() != null) {
                        Lyric pmLyric = factory.createLyric();
                        pmLyric.setDefaultY(yOf(syllable.getLocation(), staff));
                        pmLyric.setNumber(
                            "" + syllable.getLyricsLine().getId());

                        TextElementData pmText = factory.createTextElementData();
                        pmText.setValue(syllable.getContent());
                        pmLyric.getContent()
                               .add(factory.createLyricText(pmText));

                        pmLyric.getContent()
                               .add(
                            factory.createLyricSyllabic(
                                getSyllabic(syllable.getSyllabicType())));

                        current.pmNote.getContent()
                                      .add(factory.createNoteLyric(pmLyric));
                    }
                }
            }
        } catch (Exception ex) {
            logger.warning("Error exporting " + note, ex);
        }

        // Safer...
        current.endNote();

        return true;
    }

    //----------------//
    // visit Ornament //
    //----------------//
    @Override
    @SuppressWarnings("unchecked")
    public boolean visit (Ornament ornament)
    {
        JAXBElement<?> element = getOrnamentObject(ornament.getShape());

        // Include in ornaments
        getOrnaments()
            .getTrillMarkOrTurnOrDelayedTurn()
            .add(element);

        // Placement?
        Class<?> classe = element.getDeclaredType();

        try {
            Method method = classe.getMethod(
                "setPlacement",
                java.lang.String.class);
            method.invoke(
                element.getValue(),
                (ornament.getPoint().y < current.note.getCenter().y)
                                ? AboveBelow.ABOVE : AboveBelow.BELOW);
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
        pmPedal.setLine(YesNo.NO);

        // Start / Stop type
        pmPedal.setType(
            pedal.isStart() ? StartStopChange.START : StartStopChange.STOP);

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
            (pedal.getPoint().y < current.note.getCenter().y)
                        ? AboveBelow.ABOVE : AboveBelow.BELOW);

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

        // Reset durations for the score
        score.setDurationDivisor(null);

        // No version inserted
        // Let the marshalling class handle it

        // Identification
        Identification identification = factory.createIdentification();
        scorePartwise.setIdentification(identification);

        // Source
        identification.setSource(score.getImagePath());

        // Encoding
        Encoding encoding = factory.createEncoding();
        identification.setEncoding(encoding);

        // [Encoding]/Software
        encoding.getEncodingDateOrEncoderOrSoftware()
                .add(
            factory.createEncodingSoftware(
                Main.getToolName() + " " + Main.getToolVersion()));

        // [Encoding]/EncodingDate
        // Let the Marshalling class handle it

        // Defaults
        Defaults defaults = new Defaults();
        scorePartwise.setDefaults(defaults);

        // [Defaults]/Scaling
        Scaling scaling = factory.createScaling();
        defaults.setScaling(scaling);
        scaling.setMillimeters(
            createDecimal(
                (score.getSheet()
                      .getScale()
                      .interline() * 25.4 * 4) / 300)); // Assuming 300 DPI
        scaling.setTenths(new BigDecimal("40"));

        // [Defaults]/PageLayout
        PageLayout pageLayout = factory.createPageLayout();
        defaults.setPageLayout(pageLayout);
        pageLayout.setPageHeight(toTenths(score.getDimension().height));
        pageLayout.setPageWidth(toTenths(score.getDimension().width));

        // [Defaults]/LyricFont
        Font      lyricFont = omr.score.entity.Text.getLyricsFont();
        LyricFont pmLyricFont = factory.createLyricFont();
        defaults.getLyricFont()
                .add(pmLyricFont);
        pmLyricFont.setFontFamily(lyricFont.getName());
        pmLyricFont.setFontSize("" + omr.score.entity.Text.getLyricsFontSize());
        pmLyricFont.setFontStyle(
            (lyricFont.getStyle() == Font.ITALIC) ? FontStyle.ITALIC
                        : FontStyle.NORMAL);

        // PartList
        PartList partList = factory.createPartList();
        scorePartwise.setPartList(partList);
        isFirst.part = true;

        for (ScorePart p : score.getPartList()) {
            current.part = p;

            ///logger.info("Processing " + p);

            // Scorepart in partList
            proxymusic.ScorePart scorePart = factory.createScorePart();
            partList.getPartGroupOrScorePart()
                    .add(scorePart);
            scorePart.setId(current.part.getPid());

            PartName partName = factory.createPartName();
            scorePart.setPartName(partName);
            partName.setValue(current.part.getName());

            if (p.getMidiProgram() != null) {
                // Score instrument
                ScoreInstrument scoreInstrument = new ScoreInstrument();
                scorePart.getScoreInstrument()
                         .add(scoreInstrument);
                scoreInstrument.setId(scorePart.getId() + "-I1");
                scoreInstrument.setInstrumentName(
                    MidiAbstractions.getProgramName(p.getMidiProgram()));

                // Midi instrument
                MidiInstrument midiInstrument = factory.createMidiInstrument();
                scorePart.getMidiInstrument()
                         .add(midiInstrument);
                midiInstrument.setId(scoreInstrument);
                midiInstrument.setMidiChannel(p.getId());
                midiInstrument.setMidiProgram(p.getMidiProgram());
            }

            // ScorePart in scorePartwise
            current.pmPart = factory.createScorePartwisePart();
            scorePartwise.getPart()
                         .add(current.pmPart);
            current.pmPart.setId(scorePart);

            // Delegate to children the filling of measures
            if (logger.isFineEnabled()) {
                logger.fine("Populating " + current.part);
            }

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

        DirectionType directionType = factory.createDirectionType();
        direction.getDirectionType()
                 .add(directionType);

        EmptyPrintStyle empty = factory.createEmptyPrintStyle();
        directionType.getSegno()
                     .add(empty);

        // Staff ?
        Staff staff = current.note.getStaff();
        insertStaffId(direction, staff);

        // default-x
        empty.setDefaultX(
            toTenths(segno.getPoint().x - current.measure.getLeftX()));

        // default-y
        empty.setDefaultY(yOf(segno.getPoint(), staff));

        // Need also a Sound element
        Sound sound = factory.createSound();
        sound.setSegno("" + current.measure.getId());
        sound.setDivisions(
            createDecimal(
                score.simpleDurationOf(omr.score.entity.Note.QUARTER_DURATION)));

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
            Tie tie = factory.createTie();
            tie.setType(isStart ? StartStop.START : StartStop.STOP);
            current.pmNote.getContent()
                          .add(factory.createNoteTie(tie));

            // Tied element
            Tied tied = factory.createTied();

            // Type
            tied.setType(isStart ? StartStop.START : StartStop.STOP);

            // Orientation
            if (isStart) {
                tied.setOrientation(
                    slur.isBelow() ? OverUnder.UNDER : OverUnder.OVER);
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

            getNotations()
                .getTiedOrSlurOrTuplet()
                .add(tied);
        } else {
            // Slur element
            proxymusic.Slur pmSlur = factory.createSlur();

            // Number attribute
            Integer num = slurNumbers.get(slur);

            if (num != null) {
                pmSlur.setNumber(num);
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

                        pmSlur.setNumber(num);

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
            pmSlur.setType(
                isStart ? StartStopContinue.START : StartStopContinue.STOP);

            // Placement
            if (isStart) {
                pmSlur.setPlacement(
                    slur.isBelow() ? AboveBelow.BELOW : AboveBelow.ABOVE);
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

            getNotations()
                .getTiedOrSlurOrTuplet()
                .add(pmSlur);
        }

        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    /**
     * Allocate/populate everything that directly relates to this system in the
     * current part. The rest of processing is directly delegated to the
     * measures
     *
     * @param system visit the system to export
     * @return false
     */
    @Override
    public boolean visit (ScoreSystem system)
    {
        ///logger.info("Visiting " + system);
        current.system = system;
        isFirst.measure = true;

        SystemPart systemPart = (SystemPart) system.getPart(
            current.part.getId());

        if (systemPart != null) {
            systemPart.accept(this);
        } else {
            // Need to build an artificial system part
            // Or simply delegating to the series of artificial measures
            SystemPart dummyPart = system.getFirstRealPart()
                                         .createDummyPart(current.part.getId());
            visit(dummyPart);
        }

        // If we have exported a measure, we are no longer in the first system
        if (!isFirst.measure) {
            isFirst.system = false;
        }

        return false; // No default browsing this way
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart systemPart)
    {
        ///logger.info("Visiting " + systemPart);

        // Delegate to texts
        for (TreeNode node : systemPart.getTexts()) {
            ((Text) node).accept(this);
        }

        // Delegate to measures
        for (TreeNode node : systemPart.getMeasures()) {
            ((Measure) node).accept(this);
        }

        return false; // No default browsing this way
    }

    //------------//
    // visit Text //
    //------------//
    @Override
    public boolean visit (Text text)
    {
        ///logger.info("Visiting " + text);

        // Safer
        if (text.getContent() == null) {
            return false;
        }

        switch (text.getSentence()
                    .getTextType()) {
        case Title :
            getWork()
                .setWorkTitle(text.getContent());

            break;

        case Number :
            getWork()
                .setWorkNumber(text.getContent());

            break;

        case Rights : { // Rights

            TypedText typedText = factory.createTypedText();
            typedText.setValue(text.getContent());
            scorePartwise.getIdentification()
                         .getRights()
                         .add(typedText);
        }

        break;

        case Creator : { // Creator

            TypedText typedText = factory.createTypedText();
            typedText.setValue(text.getContent());

            CreatorText creatorText = (CreatorText) text;

            if (creatorText.getCreatorType() != null) {
                typedText.setType(creatorText.getCreatorType().toString());
            }

            scorePartwise.getIdentification()
                         .getCreator()
                         .add(typedText);
        }

        break;

        default : // LyricsItem, Direction

            // Handle them through related Note
            return false;
        }

        // Credits
        Credit        pmCredit = factory.createCredit();
        FormattedText creditWords = factory.createFormattedText();
        creditWords.setValue(text.getContent());
        creditWords.setFontSize("" + text.getFontSize());

        // Position is wrt to page
        PagePoint pt = text.getSystem()
                           .toPagePoint(text.getLocation());
        creditWords.setDefaultX(toTenths(pt.x));
        creditWords.setDefaultY(toTenths(score.getDimension().height - pt.y));

        pmCredit.getLinkOrBookmarkOrCreditImage()
                .add(creditWords);
        scorePartwise.getCredit()
                     .add(pmCredit);

        return true;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    @Override
    public boolean visit (TimeSignature timeSignature)
    {
        ///logger.info("Visiting " + timeSignature);
        try {
            Time time = factory.createTime();

            // Beats
            time.getBeatsAndBeatType()
                .add(
                factory.createTimeBeats("" + timeSignature.getNumerator()));

            // BeatType
            time.getBeatsAndBeatType()
                .add(
                factory.createTimeBeatType("" + timeSignature.getDenominator()));

            // Symbol ?
            if (timeSignature.getShape() != null) {
                switch (timeSignature.getShape()) {
                case COMMON_TIME :
                    time.setSymbol(TimeSymbol.COMMON);

                    break;

                case CUT_TIME :
                    time.setSymbol(TimeSymbol.CUT);

                    break;
                }
            }

            // Trick: add this time signature only if it does not already exist
            List<Time> times = getMeasureAttributes()
                                   .getTime();

            for (Time t : times) {
                if (areEqual(t, time)) {
                    return true; // Already inserted, so give up
                }
            }

            times.add(time);
        } catch (InvalidTimeSignature ex) {
        }

        return true;
    }

    //--------------//
    // visit Tuplet //
    //--------------//
    @Override
    public boolean visit (Tuplet tuplet)
    {
        proxymusic.Tuplet pmTuplet = factory.createTuplet();
        getNotations()
            .getTiedOrSlurOrTuplet()
            .add(pmTuplet);

        // Bracket
        // TBD

        // Placement
        if (tuplet.getChord() == current.note.getChord()) { // i.e. start
            pmTuplet.setPlacement(
                (tuplet.getCenter().y <= current.note.getCenter().y)
                                ? AboveBelow.ABOVE : AboveBelow.BELOW);
        }

        // Type
        pmTuplet.setType(
            (tuplet.getChord() == current.note.getChord()) ? StartStop.START
                        : StartStop.STOP);

        // Number
        Integer num = tupletNumbers.get(tuplet);

        if (num != null) {
            pmTuplet.setNumber(num);
            tupletNumbers.remove(tuplet); // Release the number
        } else {
            // Determine first available number
            for (num = 1; num <= 6; num++) {
                if (!tupletNumbers.containsValue(num)) {
                    tupletNumbers.put(tuplet, num);
                    pmTuplet.setNumber(num);

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
        Direction direction = factory.createDirection();
        current.pmMeasure.getNoteOrBackupOrForward()
                         .add(direction);

        DirectionType directionType = factory.createDirectionType();
        direction.getDirectionType()
                 .add(directionType);

        proxymusic.Wedge pmWedge = factory.createWedge();
        directionType.setWedge(pmWedge);

        // Spread
        pmWedge.setSpread(toTenths(wedge.getSpread()));

        // Start or stop ?
        if (wedge.isStart()) {
            // Type
            pmWedge.setType(
                (wedge.getShape() == Shape.CRESCENDO) ? WedgeType.CRESCENDO
                                : WedgeType.DIMINUENDO);

            // Staff ?
            Staff staff = current.note.getStaff();
            insertStaffId(direction, staff);

            // Placement
            direction.setPlacement(
                (wedge.getPoint().y < current.note.getCenter().y)
                                ? AboveBelow.ABOVE : AboveBelow.BELOW);

            // default-y
            pmWedge.setDefaultY(yOf(wedge.getPoint(), staff));
        } else { // It's a stop
            pmWedge.setType(WedgeType.STOP);
        }

        //        // Relative-x (No offset for the time being) using note left side
        //        pmWedge.setRelativeX(
        //            toTenths(wedge.getPoint().x - current.note.getCenterLeft().x));

        // default-x
        pmWedge.setDefaultX(
            toTenths(wedge.getPoint().x - current.measure.getLeftX()));

        return true;
    }

    //--------------------------//
    // visit DirectionStatement //
    //--------------------------//
    @Override
    public boolean visit (DirectionStatement words)
    {
        if (words.getText()
                 .getContent() != null) {
            Direction direction = factory.createDirection();
            current.pmMeasure.getNoteOrBackupOrForward()
                             .add(direction);

            DirectionType directionType = factory.createDirectionType();
            direction.getDirectionType()
                     .add(directionType);

            FormattedText pmWords = factory.createFormattedText();
            directionType.getWords()
                         .add(pmWords);

            pmWords.setValue(words.getText().getContent());

            // Placement
            direction.setPlacement(
                (words.getPoint().y < current.note.getCenter().y)
                                ? AboveBelow.ABOVE : AboveBelow.BELOW);

            // default-y
            Staff staff = current.note.getStaff();
            pmWords.setDefaultY(yOf(words.getPoint(), staff));

            // font-size
            pmWords.setFontSize("" + words.getText().getFontSize());

            // relative-x
            pmWords.setRelativeX(
                toTenths(words.getPoint().x - current.note.getCenterLeft().x));
        }

        return true;
    }

    //--------//
    // getDen // A VERIFIER A VERIFIER A VERIFIER A VERIFIER A VERIFIER
    //--------//
    private static java.lang.String getDen (Time time)
    {
        for (JAXBElement<java.lang.String> elem : time.getBeatsAndBeatType()) {
            if (elem.getName()
                    .getLocalPart()
                    .equals("beat-type")) {
                return elem.getValue();
            }
        }

        logger.severe("No denominator found in " + time);

        return "";
    }

    //--------//
    // getNum // A VERIFIER A VERIFIER A VERIFIER A VERIFIER A VERIFIER
    //--------//
    private static java.lang.String getNum (Time time)
    {
        for (JAXBElement<java.lang.String> elem : time.getBeatsAndBeatType()) {
            if (elem.getName()
                    .getLocalPart()
                    .equals("beats")) {
                return elem.getValue();
            }
        }

        logger.severe("No numerator found in " + time);

        return "";
    }

    //- Utility Methods --------------------------------------------------------

    //-----------//
    // isDesired //
    //-----------//
    /**
     * Check whether the provided measure is to be exported
     *
     * @param measure the measure to check
     * @return true is desired
     */
    private boolean isDesired (Measure measure)
    {
        return (measureRange == null) || // No range : take all of them
               (measure.isTemporary()) || // A temporary measure for export
               measureRange.contains(measure.getId()); // Part of the range
    }

    //----------//
    // areEqual //
    //----------//
    private static boolean areEqual (Time left,
                                     Time right)
    {
        return (getNum(left).equals(getNum(right))) &&
               (getDen(left).equals(getDen(right)));
    }

    //----------//
    // areEqual //
    //----------//
    private static boolean areEqual (Key left,
                                     Key right)
    {
        return left.getFifths()
                   .equals(right.getFifths());
    }

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

    //-----------//
    // isNewClef //
    //-----------//
    /**
     * Make sure we have a NEW clef, not already assigned. We have to go back
     * (on the same staff) in current measure, then in previous measures,
     * then in same staff in previous systems, until we find a previous clef.
     * And we compare the two shapes.
     * @param clef the potentially new clef
     * @return true if this clef is really new
     */
    private boolean isNewClef (Clef clef)
    {
        if (current.measure.isDummy()) {
            return true;
        }

        // Perhaps another clef before this one ?
        Clef previousClef = current.measure.getClefBefore(
            new SystemPoint(clef.getCenter().x - 1, clef.getCenter().y));

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
        if (current.measure.isDummy()) {
            return true;
        }

        // Perhaps another key before this one ?
        KeySignature previousKey = current.measure.getKeyBefore(
            key.getCenter());

        if (previousKey != null) {
            return !previousKey.getKey()
                               .equals(key.getKey());
        }

        return true; // Since no previous key found
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
            current.notations = factory.createNotations();
            current.pmNote.getContent()
                          .add(factory.createNoteNotations(current.notations));
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
        Ornaments ornaments = factory.createOrnaments();
        getNotations()
            .getTiedOrSlurOrTuplet()
            .add(ornaments);

        return ornaments;
    }

    //---------//
    // getWork //
    //---------//
    private Work getWork ()
    {

        if (current.pmWork == null) {
            current.pmWork = factory.createWork();
            scorePartwise.setWork(current.pmWork);
        }

        return current.pmWork;
    }

    //--------------//
    // insertBackup //
    //--------------//
    private void insertBackup (int delta)
    {
        try {
            Backup backup = factory.createBackup();
            backup.setDuration(createDecimal(score.simpleDurationOf(delta)));
            current.pmMeasure.getNoteOrBackupOrForward()
                             .add(backup);
        } catch (Exception ex) {
            if (score.getDurationDivisor() != null) {
                logger.warning("Not able to insert backup", ex);
            }
        }
    }

    //---------------//
    // insertForward //
    //---------------//
    private void insertForward (int   delta,
                                Chord chord)
    {
        try {
            Forward forward = factory.createForward();
            forward.setDuration(createDecimal(score.simpleDurationOf(delta)));
            forward.setVoice("" + current.voice.getId());
            current.pmMeasure.getNoteOrBackupOrForward()
                             .add(forward);

            // Staff ? (only if more than one staff in part)
            insertStaffId(forward, chord.getStaff());
        } catch (Exception ex) {
            if (score.getDurationDivisor() != null) {
                logger.warning("Not able to insert forward", ex);
            }
        }
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
    @SuppressWarnings("unchecked")
    private void insertStaffId (Object obj,
                                Staff  staff)
    {
        if (current.part.isMultiStaff()) {
            Class classe = obj.getClass();

            try {
                Method method = classe.getMethod("setStaff", BigInteger.class);
                method.invoke(obj, new BigInteger("" + staff.getId()));
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.severe("Could not setStaff for element " + classe);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------//
    // LoadTask //
    //----------//
    public static class LoadTask
        extends BasicTask
    {
        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            if (logger.isFineEnabled()) {
                logger.fine("Pre-loading JAXB ...");
            }

            try {
                long startTime = java.lang.System.currentTimeMillis();
                Marshalling.getContext();

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "JAXB Loaded in " +
                        (java.lang.System.currentTimeMillis() - startTime) +
                        "ms");
                }
            } catch (JAXBException ex) {
                logger.warning("Error preloading JaxbContext", ex);
            }

            return null;
        }
    }

    //---------//
    // Current //
    //---------//
    /** Keep references of all current entities */
    private static class Current
    {
        //~ Instance fields ----------------------------------------------------

        // Score dependent
        proxymusic.Work                       pmWork;

        // Part dependent
        ScorePart                             part;
        proxymusic.ScorePartwise.Part         pmPart;

        // System dependent
        ScoreSystem                           system;

        // Measure dependent
        Measure                               measure;
        proxymusic.ScorePartwise.Part.Measure pmMeasure;
        Voice                                 voice;

        // Note dependent
        omr.score.entity.Note note;
        proxymusic.Note       pmNote;
        proxymusic.Notations  notations;
        proxymusic.Print      pmPrint;
        proxymusic.Attributes attributes;

        //~ Methods ------------------------------------------------------------

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
        //~ Instance fields ----------------------------------------------------

        /** We are writing the first part of the score */
        boolean part;

        /** We are writing the first system in the current page */
        boolean system;

        /** We are writing the first measure in current system (in current part) */
        boolean measure;

        //~ Methods ------------------------------------------------------------

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
