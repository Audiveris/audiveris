//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P a r t w i s e B u i l d e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.math.Rational;
import static omr.score.MusicXML.*;
import omr.score.entity.Arpeggiate;
import omr.score.entity.Articulation;
import omr.score.entity.Fermata;
import omr.score.entity.LogicalPart;
import omr.score.entity.MeasureId;
import omr.score.entity.Ornament;
import omr.score.entity.Page;
import omr.score.entity.Segno;
import omr.score.entity.Slur;
import omr.score.entity.Text;
import omr.score.entity.Wedge;
import omr.score.midi.MidiAbstractions;

import omr.sheet.Book;
import omr.sheet.Part;
import omr.sheet.PartBarline;
import omr.sheet.Scale;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.MeasureStack;
import omr.sheet.rhythm.Slot;
import omr.sheet.rhythm.Voice;

import omr.sig.inter.AbstractBeamInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.AbstractNoteInter;
import omr.sig.inter.AlterInter;
import omr.sig.inter.ChordInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.Inter;
import omr.sig.inter.KeyInter;
import omr.sig.inter.TimeInter;
import omr.sig.inter.TupletInter;

import omr.text.FontInfo;
import static omr.util.HorizontalSide.*;
import omr.util.OmrExecutors;
import static omr.util.VerticalSide.*;

import com.audiveris.proxymusic.AboveBelow;
import com.audiveris.proxymusic.Accidental;
import com.audiveris.proxymusic.Articulations;
import com.audiveris.proxymusic.Attributes;
import com.audiveris.proxymusic.Backup;
import com.audiveris.proxymusic.BeamValue;
import com.audiveris.proxymusic.ClefSign;
import com.audiveris.proxymusic.Credit;
import com.audiveris.proxymusic.Defaults;
import com.audiveris.proxymusic.Direction;
import com.audiveris.proxymusic.DirectionType;
import com.audiveris.proxymusic.Empty;
import com.audiveris.proxymusic.EmptyPrintStyleAlign;
import com.audiveris.proxymusic.Encoding;
import com.audiveris.proxymusic.FontStyle;
import com.audiveris.proxymusic.FontWeight;
import com.audiveris.proxymusic.FormattedText;
import com.audiveris.proxymusic.Forward;
import com.audiveris.proxymusic.Identification;
import com.audiveris.proxymusic.Key;
import com.audiveris.proxymusic.LyricFont;
import com.audiveris.proxymusic.MarginType;
import com.audiveris.proxymusic.MeasureNumberingValue;
import com.audiveris.proxymusic.MidiInstrument;
import com.audiveris.proxymusic.Notations;
import com.audiveris.proxymusic.NoteType;
import com.audiveris.proxymusic.ObjectFactory;
import com.audiveris.proxymusic.Ornaments;
import com.audiveris.proxymusic.PageLayout;
import com.audiveris.proxymusic.PageMargins;
import com.audiveris.proxymusic.PartList;
import com.audiveris.proxymusic.PartName;
import com.audiveris.proxymusic.Pitch;
import com.audiveris.proxymusic.Rest;
import com.audiveris.proxymusic.RightLeftMiddle;
import com.audiveris.proxymusic.Scaling;
import com.audiveris.proxymusic.ScoreInstrument;
import com.audiveris.proxymusic.ScorePart;
import com.audiveris.proxymusic.ScorePartwise;
import com.audiveris.proxymusic.Sound;
import com.audiveris.proxymusic.StaffDetails;
import com.audiveris.proxymusic.StaffLayout;
import com.audiveris.proxymusic.Stem;
import com.audiveris.proxymusic.StemValue;
import com.audiveris.proxymusic.Supports;
import com.audiveris.proxymusic.SystemLayout;
import com.audiveris.proxymusic.SystemMargins;
import com.audiveris.proxymusic.Time;
import com.audiveris.proxymusic.TimeModification;
import com.audiveris.proxymusic.TimeSymbol;
import com.audiveris.proxymusic.TypedText;
import com.audiveris.proxymusic.UprightInverted;
import com.audiveris.proxymusic.WedgeType;
import com.audiveris.proxymusic.Work;
import com.audiveris.proxymusic.YesNo;
import com.audiveris.proxymusic.util.Marshalling;
import com.audiveris.proxymusic.util.Source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

/**
 * Class {@code PartwiseBuilder} builds a ProxyMusic MusicXML {@link ScorePartwise}
 * from an Audiveris {@link Score} instance.
 *
 * @author Hervé Bitteur
 */
public class PartwiseBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(PartwiseBuilder.class);

    /** A future which reflects whether JAXB has been initialized. */
    private static final Future<Void> loading = OmrExecutors.getCachedLowExecutor().submit(
            new Callable<Void>()
            {
                @Override
                public Void call ()
                throws Exception
                {
                    try {
                        Marshalling.getContext(ScorePartwise.class);
                    } catch (JAXBException ex) {
                        logger.warn("Error preloading JaxbContext", ex);
                        throw ex;
                    }

                    return null;
                }
            });

    /** Default page horizontal margin. */
    private static final BigDecimal pageHorizontalMargin = new BigDecimal(
            constants.pageHorizontalMargin.getValue());

    /** Default page vertical margin. */
    private static final BigDecimal pageVerticalMargin = new BigDecimal(
            constants.pageVerticalMargin.getValue());

    //~ Instance fields ----------------------------------------------------------------------------
    /** The ScorePartwise instance to be populated. */
    private final ScorePartwise scorePartwise = new ScorePartwise();

    /** The related score. */
    private final Score score;

    /** First system id in page, if any. */
    private Integer firstSystemId;

    /** Last system id in page, if any. */
    private Integer lastSystemId;

    /** Current context. */
    private final Current current = new Current();

    /** Current flags. */
    private final IsFirst isFirst = new IsFirst();

    /** Map of Slur numbers, reset for every LogicalPart. */
    private final Map<Slur, Integer> slurNumbers = new HashMap<Slur, Integer>();

    /** Map of Tuplet numbers, reset for every Measure. */
    private final Map<TupletInter, Integer> tupletNumbers = new HashMap<TupletInter, Integer>();

    /** Potential range of selected measures. */
    private MeasureId.MeasureRange measureRange;

    /** Factory for ProxyMusic entities. */
    private final ObjectFactory factory = new ObjectFactory();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new PartwiseBuilder object, on a related score instance.
     *
     * @param score the underlying score
     * @throws InterruptedException
     * @throws ExecutionException
     */
    private PartwiseBuilder (Score score)
            throws InterruptedException, ExecutionException
    {
        // Make sure the JAXB context is ready
        loading.get();

        this.score = score;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // build //
    //-------//
    /**
     * Visit the whole score tree and populate the corresponding ScorePartwise.
     *
     * @param score the score to export (cannot be null)
     * @return the populated ScorePartwise
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static ScorePartwise build (Score score)
            throws InterruptedException, ExecutionException
    {
        if (score == null) {
            throw new IllegalArgumentException("Trying to export a null score");
        }

        final PartwiseBuilder builder = new PartwiseBuilder(score);

        // Let visited nodes fill the ScorePartWise proxy
        builder.processScore();

        // Populate the source structure (encoded as part of the miscellaneous element in MusicXML)
        Source source = score.buildSource();
        source.encode(builder.scorePartwise);

        return builder.scorePartwise;
    }

    //---------//
    // preload //
    //---------//
    /**
     * Empty static method, just to trigger class elaboration (and thus JAXB context).
     */
    public static void preload ()
    {
    }

    //
    //    //-----------------//
    //    // setMeasureRange //
    //    //-----------------//
    //    /**
    //     * Set a specific range of measures to export.
    //     *
    //     * @param measureRange the range of desired measures
    //     */
    //    public void setMeasureRange (MeasureId.MeasureRange measureRange)
    //    {
    //        this.measureRange = measureRange;
    //    }
    //
    //----------//
    // areEqual //
    //----------//
    private static boolean areEqual (Time left,
                                     Time right)
    {
        return (getNum(left).equals(getNum(right))) && (getDen(left).equals(getDen(right)));
    }

    //----------//
    // areEqual //
    //----------//
    private static boolean areEqual (Key left,
                                     Key right)
    {
        return left.getFifths().equals(right.getFifths());
    }

    //--------//
    // getDen // A VERIFIER A VERIFIER A VERIFIER A VERIFIER A VERIFIER
    //--------//
    private static java.lang.String getDen (Time time)
    {
        for (JAXBElement<java.lang.String> elem : time.getTimeSignature()) {
            if (elem.getName().getLocalPart().equals("beat-type")) {
                return elem.getValue();
            }
        }

        logger.error("No denominator found in {}", time);

        return "";
    }

    //--------//
    // getNum // A VERIFIER A VERIFIER A VERIFIER A VERIFIER A VERIFIER
    //--------//
    private static java.lang.String getNum (Time time)
    {
        for (JAXBElement<java.lang.String> elem : time.getTimeSignature()) {
            if (elem.getName().getLocalPart().equals("beats")) {
                return elem.getValue();
            }
        }

        logger.error("No numerator found in {}", time);

        return "";
    }

    //-----------//
    // buildClef //
    //-----------//
    private com.audiveris.proxymusic.Clef buildClef (ClefInter clef)
    {
        com.audiveris.proxymusic.Clef pmClef = factory.createClef();

        // Staff number (only for multi-staff parts)
        if (current.logicalPart.isMultiStaff()) {
            pmClef.setNumber(new BigInteger("" + (1 + clef.getStaff().getIndexInPart())));
        }

        // Line (General computation that could be overridden by more specific shape test below)
        pmClef.setLine(new BigInteger("" + (3 - (int) Math.rint(clef.getPitch() / 2.0))));

        Shape shape = clef.getShape();

        switch (shape) {
        case G_CLEF:
        case G_CLEF_SMALL:
            pmClef.setSign(ClefSign.G);

            break;

        case G_CLEF_8VA:
            pmClef.setSign(ClefSign.G);
            pmClef.setClefOctaveChange(new BigInteger("1"));

            break;

        case G_CLEF_8VB:
            pmClef.setSign(ClefSign.G);
            pmClef.setClefOctaveChange(new BigInteger("-1"));

            break;

        case C_CLEF:
            pmClef.setSign(ClefSign.C);

            break;

        case F_CLEF:
        case F_CLEF_SMALL:
            pmClef.setSign(ClefSign.F);

            break;

        case F_CLEF_8VA:
            pmClef.setSign(ClefSign.F);
            pmClef.setClefOctaveChange(new BigInteger("1"));

            break;

        case F_CLEF_8VB:
            pmClef.setSign(ClefSign.F);
            pmClef.setClefOctaveChange(new BigInteger("-1"));

            break;

        case PERCUSSION_CLEF:
            pmClef.setSign(ClefSign.PERCUSSION);

            break;

        default:
            logger.error("Clef shape not exported {}", shape);
        }

        return pmClef;
    }

    //------------------//
    // getArticulations //
    //------------------//
    /**
     * Report (after creating it if necessary) the articulations elements in the
     * notations element of the current note.
     *
     * @return the note notations articulations element
     */
    private Articulations getArticulations ()
    {
        for (Object obj : getNotations().getTiedOrSlurOrTuplet()) {
            if (obj instanceof Articulations) {
                return (Articulations) obj;
            }
        }

        // Need to allocate articulations
        Articulations articulations = factory.createArticulations();
        getNotations().getTiedOrSlurOrTuplet().add(articulations);

        return articulations;
    }

    //---------------//
    // getAttributes //
    //---------------//
    /**
     * Report (after creating it if necessary) the measure attributes element.
     *
     * @return the measure attributes element
     */
    private Attributes getAttributes ()
    {
        if (current.pmAttributes == null) {
            current.pmAttributes = new Attributes();
            current.pmMeasure.getNoteOrBackupOrForward().add(current.pmAttributes);
        }

        return current.pmAttributes;
    }

    //--------------//
    // getNotations //
    //--------------//
    /**
     * Report (after creating it if necessary) the notations element of the current note.
     *
     * @return the note notations element
     */
    private Notations getNotations ()
    {
        // Notations allocated?
        if (current.pmNotations == null) {
            current.pmNotations = factory.createNotations();
            current.pmNote.getNotations().add(current.pmNotations);
        }

        return current.pmNotations;
    }

    //--------------//
    // getOrnaments //
    //--------------//
    /**
     * Report (after creating it if necessary) the ornaments elements in the notations
     * element of the current note.
     *
     * @return the note notations ornaments element
     */
    private Ornaments getOrnaments ()
    {
        for (Object obj : getNotations().getTiedOrSlurOrTuplet()) {
            if (obj instanceof Ornaments) {
                return (Ornaments) obj;
            }
        }

        // Need to allocate ornaments
        Ornaments ornaments = factory.createOrnaments();
        getNotations().getTiedOrSlurOrTuplet().add(ornaments);

        return ornaments;
    }

    //--------------//
    // getScorePart //
    //--------------//
    /**
     * Generate the Proxymusic {@link ScorePart} instance that corresponds to the
     * provided Audiveris {@link LogicalPart} instance.
     *
     * @param logicalPart provided score LogicalPart
     * @return the newly built proxymusic ScorePart instance
     */
    private ScorePart getScorePart (LogicalPart logicalPart)
    {
        current.logicalPart = logicalPart;

        logger.info("Processing " + logicalPart);

        // Scorepart in partList
        ScorePart pmScorePart = factory.createScorePart();
        pmScorePart.setId(logicalPart.getPid());

        PartName partName = factory.createPartName();
        pmScorePart.setPartName(partName);
        partName.setValue(
                (logicalPart.getName() != null) ? logicalPart.getName() : logicalPart.getDefaultName());

        // Score instrument
        Integer midiProgram = logicalPart.getMidiProgram();

        if (midiProgram == null) {
            midiProgram = logicalPart.getDefaultProgram();
        }

        ScoreInstrument scoreInstrument = new ScoreInstrument();
        pmScorePart.getScoreInstrument().add(scoreInstrument);
        scoreInstrument.setId(pmScorePart.getId() + "-I1");
        scoreInstrument.setInstrumentName(MidiAbstractions.getProgramName(midiProgram));

        // Midi instrument
        MidiInstrument midiInstrument = factory.createMidiInstrument();
        pmScorePart.getMidiDeviceAndMidiInstrument().add(midiInstrument);
        midiInstrument.setId(scoreInstrument);
        midiInstrument.setMidiChannel(logicalPart.getId());
        midiInstrument.setMidiProgram(midiProgram);
        midiInstrument.setVolume(new BigDecimal(score.getVolume()));

        // LogicalPart in scorePartwise
        current.pmPart = factory.createScorePartwisePart();
        scorePartwise.getPart().add(current.pmPart);
        current.pmPart.setId(pmScorePart);

        // Delegate to children the filling of measures
        logger.debug("Populating {}", logicalPart);
        isFirst.system = true;
        slurNumbers.clear(); // Reset slur numbers

        // Browse the whole score hierarchy for this score logicalPart
        for (Page page : score.getPages()) {
            processPage(page);

            for (SystemInfo system : page.getSystems()) {
                processSystem(system);
            }
        }

        return pmScorePart;
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
    private void insertBackup (Rational delta)
    {
        try {
            Backup backup = factory.createBackup();
            backup.setDuration(new BigDecimal(current.page.simpleDurationOf(delta)));
            current.pmMeasure.getNoteOrBackupOrForward().add(backup);
        } catch (Exception ex) {
            if (current.page.getDurationDivisor() != null) {
                logger.warn("Not able to insert backup", ex);
            }
        }
    }

    //
    //    //----------------------//
    //    // insertCurrentContext //
    //    //----------------------//
    //    private void insertCurrentContext (Measure measure)
    //    {
    //        // Browse measure, staff per staff
    //        Part part = measure.getPart();
    //
    //        for (Staff staff : part.getStaves()) {
    //            int right = measure.getAbscissa(LEFT, staff); // Right of dummy = Left of current
    //            int midY = (staff.getTopLeft().y + (staff.getHeight() / 2))
    //                       - measure.getSystem().getTopLeft().y;
    //            Point staffPoint = new Point(right, midY);
    //
    //            // Clef?
    //            ClefInter clef = measure.getClefBefore(staffPoint, staff);
    //
    //            if (clef != null) {
    //                ///clef.accept(this);
    //                process(clef);
    //            }
    //
    //            // Key?
    //            KeySignature key = measure.getKeyBefore(staffPoint, staff);
    //
    //            if (key != null) {
    //                ///key.accept(this);
    //                process(key);
    //            }
    //
    //            // Time?
    //            TimeInter time = measure.getCurrentTimeSignature();
    //
    //            if (time != null) {
    //                ///time.accept(this);
    //                process(time);
    //            }
    //        }
    //    }
    //
    //---------------//
    // insertForward //
    //---------------//
    private void insertForward (Rational delta,
                                ChordInter chord)
    {
        try {
            Forward forward = factory.createForward();
            forward.setDuration(new BigDecimal(current.page.simpleDurationOf(delta)));
            forward.setVoice("" + current.voice.getId());
            current.pmMeasure.getNoteOrBackupOrForward().add(forward);

            // Staff ? (only if more than one staff in logicalPart)
            insertStaffId(forward, chord.getStaff());
        } catch (Exception ex) {
            if (current.page.getDurationDivisor() != null) {
                logger.warn("Not able to insert forward", ex);
            }
        }
    }

    //---------------//
    // insertStaffId //
    //---------------//
    /**
     * If needed (if current logicalPart contains more than one staff),
     * we insert the id of the staff related to the element at hand.
     *
     * @param obj the element at hand
     * @staff the related score staff
     */
    @SuppressWarnings("unchecked")
    private void insertStaffId (Object obj,
                                Staff staff)
    {
        if (current.logicalPart.isMultiStaff()) {
            Class<?> classe = obj.getClass();

            try {
                Method method = classe.getMethod("setStaff", BigInteger.class);
                method.invoke(obj, new BigInteger("" + (1 + staff.getIndexInPart())));
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.error("Could not setStaff for element {}", classe);
            }
        }
    }

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
        //        return (measureRange == null) || // No range : take all of them
        //                (measure.isTemporary()) || // A temporary measure for export
        //                measureRange.contains(measure.getPageId()); // Part of the range
        return true;
    }

    //-----------//
    // isNewClef //
    //-----------//
    /**
     * Make sure we have a NEW clef, not already assigned.
     * We have to go back (on the same staff) in current measure, then in
     * previous measures, then in same staff in previous systems, until we find
     * a previous clef.
     * And we compare the two shapes.
     *
     * @param clef the potentially new clef
     * @return true if this clef is really new
     */
    private boolean isNewClef (ClefInter clef)
    {
        if (current.measure.isDummy()) {
            return true;
        }

        // Perhaps another clef before this one ?
        ClefInter previousClef = current.measure.getClefBefore(
                new Point(clef.getCenter().x - 1, clef.getCenter().y),
                clef.getStaff());

        if (previousClef != null) {
            return previousClef.getShape() != clef.getShape();
        }

        return true; // Since no previous clef found
    }

    //-------------------//
    // isNewKeySignature //
    //-------------------//
    /**
     * Make sure we have a NEW key, not already assigned.
     * We have to go back in current measure, then in current staff, then in same staff in previous
     * systems, until we find a previous key.
     * And we compare the two shapes.
     *
     * @param key the potentially new key
     * @return true if this key is really new
     */
    private boolean isNewKeySignature (KeyInter key)
    {
        if (current.measure.isDummy()) {
            return true;
        }

        // Perhaps another key before this one ?
        KeyInter previousKey = current.measure.getKeyBefore(key.getCenter(), key.getStaff());

        if (previousKey != null) {
            return previousKey.getSignature() != key.getSignature();
        }

        return true; // Since no previous key was found
    }

    //- All Visiting Methods -----------------------------------------------------------------------
    //--------------------//
    // process Arpeggiate //
    //--------------------//
    private void process (Arpeggiate arpeggiate)
    {
        try {
            logger.debug("Visiting {}", arpeggiate);

            com.audiveris.proxymusic.Arpeggiate pmArpeggiate = factory.createArpeggiate();

            // relative-x
            pmArpeggiate.setRelativeX(
                    toTenths(arpeggiate.getReferencePoint().x - current.note.getCenterLeft().x));

            // number ???
            // TODO
            //
            getNotations().getTiedOrSlurOrTuplet().add(pmArpeggiate);
        } catch (Exception ex) {
            logger.warn("Error visiting " + arpeggiate, ex);
        }
    }

    //----------------------//
    // process Articulation //
    //----------------------//
    private void process (Articulation articulation)
    {
        try {
            logger.debug("Visiting {}", articulation);

            JAXBElement<?> element = getArticulationObject(articulation.getShape());

            // Staff ?
            Staff staff = current.note.getStaff();

            // Placement
            Class<?> classe = element.getDeclaredType();

            Method method = classe.getMethod("setPlacement", AboveBelow.class);
            method.invoke(
                    element.getValue(),
                    (articulation.getReferencePoint().y < current.note.getCenter().y)
                            ? AboveBelow.ABOVE : AboveBelow.BELOW);

            // Default-Y
            method = classe.getMethod("setDefaultY", BigDecimal.class);
            method.invoke(element.getValue(), yOf(articulation.getReferencePoint(), staff));

            // Include in Articulations
            getArticulations().getAccentOrStrongAccentOrStaccato().add(element);
        } catch (Exception ex) {
            logger.warn("Error visiting " + articulation, ex);
        }
    }

    //------------------//
    // process Ornament //
    //------------------//
    @SuppressWarnings("unchecked")
    private void process (Ornament ornament)
    {
        try {
            logger.debug("Visiting {}", ornament);

            JAXBElement<?> element = getOrnamentObject(ornament.getShape());

            // Placement?
            Class<?> classe = element.getDeclaredType();
            Method method = classe.getMethod("setPlacement", AboveBelow.class);
            method.invoke(
                    element.getValue(),
                    (ornament.getReferencePoint().y < current.note.getCenter().y) ? AboveBelow.ABOVE
                            : AboveBelow.BELOW);
            // Everything is OK
            // Include in ornaments
            getOrnaments().getTrillMarkOrTurnOrDelayedTurn().add(element);
        } catch (Exception ex) {
            logger.warn("Error visiting " + ornament, ex);
        }
    }

    //---------------//
    // process Segno //
    //---------------//
    private void process (Segno segno)
    {
        try {
            logger.debug("Visiting {}", segno);

            Direction direction = new Direction();
            DirectionType directionType = factory.createDirectionType();

            EmptyPrintStyleAlign empty = factory.createEmptyPrintStyleAlign();

            // Staff ?
            Staff staff = current.note.getStaff();
            insertStaffId(direction, staff);

            // default-x
            empty.setDefaultX(
                    toTenths(segno.getReferencePoint().x - current.measure.getAbscissa(LEFT, staff)));

            // default-y
            empty.setDefaultY(yOf(segno.getReferencePoint(), staff));

            // Need also a Sound element (TODO: We don't do anything with sound!)
            Sound sound = factory.createSound();
            sound.setSegno("" + current.measure.getStack().getScoreId(score));
            sound.setDivisions(
                    new BigDecimal(
                            current.page.simpleDurationOf(omr.score.entity.Note.QUARTER_DURATION)));

            // Everything is OK
            directionType.getSegno().add(empty);
            direction.getDirectionType().add(directionType);
            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
        } catch (Exception ex) {
            logger.warn("Error visiting " + segno, ex);
        }
    }

    //--------------//
    // process Text //
    //--------------//
    private void process (Text text)
    {
        try {
            logger.debug("Visiting {}", text);

            switch (text.getSentence().getRole().role) {
            case Title:
                getWork().setWorkTitle(text.getContent());

                break;

            case Number:
                getWork().setWorkNumber(text.getContent());

                break;

            case Rights: {
                TypedText typedText = factory.createTypedText();
                typedText.setValue(text.getContent());
                scorePartwise.getIdentification().getRights().add(typedText);
            }

            break;

            case Creator: {
                TypedText typedText = factory.createTypedText();
                typedText.setValue(text.getContent());

                Text.CreatorText.CreatorType type = text.getSentence().getRole().creatorType;

                if (type != null) {
                    typedText.setType(type.toString());
                }

                scorePartwise.getIdentification().getCreator().add(typedText);
            }

            break;

            case UnknownRole:
                break;

            default: // LyricsItem, Direction, Chord

                // Handle them through related Note
                return;
            }

            // Credits
            Credit pmCredit = factory.createCredit();
            // For MusicXML, page # is counted from 1, whatever the pageIndex
            pmCredit.setPage(new BigInteger("" + (1 + current.page.getChildIndex())));

            FormattedText creditWords = factory.createFormattedText();
            creditWords.setValue(text.getContent());

            // Font information
            setFontInfo(creditWords, text);

            // Position is wrt page
            Point pt = text.getReferencePoint();
            creditWords.setDefaultX(toTenths(pt.x));
            creditWords.setDefaultY(toTenths(current.page.getDimension().height - pt.y));

            pmCredit.getCreditTypeOrLinkOrBookmark().add(creditWords);
            scorePartwise.getCredit().add(pmCredit);
        } catch (Exception ex) {
            logger.warn("Error visiting " + text, ex);
        }
    }

    //
    //    //----------------//
    //    // process Tuplet //
    //    //----------------//
    //    private void process (TupletInter tuplet)
    //    {
    //        try {
    //            logger.debug("Visiting {}", tuplet);
    //
    //            com.audiveris.proxymusic.Tuplet pmTuplet = factory.createTuplet();
    //
    //            // Brackets
    //            if (constants.avoidTupletBrackets.isSet()) {
    //                pmTuplet.setBracket(YesNo.NO);
    //            }
    //
    //            // Placement
    //            if (tuplet.getChord() == current.note.getChord()) { // i.e. start
    //                pmTuplet.setPlacement(
    //                        (tuplet.getCenter().y <= current.note.getCenter().y) ? AboveBelow.ABOVE
    //                                : AboveBelow.BELOW);
    //            }
    //
    //            // Type
    //            pmTuplet.setType(
    //                    (tuplet.getChord() == current.note.getChord()) ? StartStop.START : StartStop.STOP);
    //
    //            // Number
    //            Integer num = tupletNumbers.get(tuplet);
    //
    //            if (num != null) {
    //                pmTuplet.setNumber(num);
    //                tupletNumbers.remove(tuplet); // Release the number
    //            } else {
    //                // Determine first available number
    //                for (num = 1; num <= 6; num++) {
    //                    if (!tupletNumbers.containsValue(num)) {
    //                        tupletNumbers.put(tuplet, num);
    //                        pmTuplet.setNumber(num);
    //
    //                        break;
    //                    }
    //                }
    //            }
    //
    //            getNotations().getTiedOrSlurOrTuplet().add(pmTuplet);
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + tuplet, ex);
    //        }
    //    }
    //
    //---------------//
    // process Wedge //
    //---------------//
    private void process (Wedge wedge)
    {
        try {
            logger.debug("Visiting {}", wedge);

            Direction direction = factory.createDirection();
            DirectionType directionType = factory.createDirectionType();
            com.audiveris.proxymusic.Wedge pmWedge = factory.createWedge();

            // Spread
            pmWedge.setSpread(toTenths(wedge.getSpread()));

            // Staff ?
            Staff staff = current.note.getStaff();
            insertStaffId(direction, staff);

            // Start or stop ?
            if (wedge.isStart()) {
                // Type
                pmWedge.setType(
                        (wedge.getShape() == Shape.CRESCENDO) ? WedgeType.CRESCENDO : WedgeType.DIMINUENDO);

                // Placement
                direction.setPlacement(
                        (wedge.getReferencePoint().y < current.note.getCenter().y) ? AboveBelow.ABOVE
                                : AboveBelow.BELOW);

                // default-y
                pmWedge.setDefaultY(yOf(wedge.getReferencePoint(), staff));
            } else { // It's a stop
                pmWedge.setType(WedgeType.STOP);
            }

            //        // Relative-x (No offset for the time being) using note left side
            //        pmWedge.setRelativeX(
            //            toTenths(wedge.getReferencePoint().x - current.note.getCenterLeft().x));
            //        // default-x
            //        pmWedge.setDefaultX(
            //            toTenths(wedge.getReferencePoint().x - current.measure.getLeftX()));
            // Everything is OK
            directionType.setWedge(pmWedge);
            direction.getDirectionType().add(directionType);
            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
        } catch (Exception ex) {
            logger.warn("Error visiting " + wedge, ex);
        }
    }

    //----------------//
    // processBarline //
    //----------------//
    private void processBarline (PartBarline barline)
    {
        try {
            if (barline == null) {
                return;
            }

            logger.debug("Visiting {}", barline);

            //TODO: handle endings if any
            PartBarline.Style style = barline.getStyle();

            if (style != PartBarline.Style.REGULAR) {
                try {
                    com.audiveris.proxymusic.Barline pmBarline = factory.createBarline();
                    com.audiveris.proxymusic.BarStyleColor barStyleColor = factory.createBarStyleColor();
                    barStyleColor.setValue(barStyleOf(style));

                    if (barline == current.measure.getBarline()) {
                        // The bar is on right side
                        pmBarline.setLocation(RightLeftMiddle.RIGHT);

                        // Repeat?
                        //                            Repeat repeat = factory.createRepeat();
                        //                            repeat.setDirection(BackwardForward.BACKWARD);
                        //                            pmBarline.setRepeat(repeat);
                    } else {
                        // Inside barline (on left)
                        // Or bar is on left side
                        pmBarline.setLocation(RightLeftMiddle.LEFT);

                        // Repeat?
                        //                            Repeat repeat = factory.createRepeat();
                        //                            repeat.setDirection(BackwardForward.FORWARD);
                        //                            pmBarline.setRepeat(repeat);
                    }

                    // Everything is now OK
                    pmBarline.setBarStyle(barStyleColor);
                    current.pmMeasure.getNoteOrBackupOrForward().add(pmBarline);
                } catch (Exception ex) {
                    logger.warn("Cannot process barline", ex);
                }
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + barline, ex);
        }
    }

    //--------------//
    // processChord //
    //--------------//
    private void processChord (ChordInter chord)
    {
        for (Inter inter : chord.getNotes()) {
            processNote((AbstractNoteInter) inter);
        }
    }

    //-------------//
    // processClef //
    //-------------//
    private void processClef (ClefInter clef)
    {
        try {
            logger.debug("Visiting {}", clef);

            if (isNewClef(clef)) {
                getAttributes().getClef().add(buildClef(clef));
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + clef, ex);
        }
    }

    //
    //    //--------------//
    //    // process Coda //
    //    //--------------//
    //    private void process (Coda coda)
    //    {
    //        try {
    //            logger.debug("Visiting {}", coda);
    //
    //            Direction direction = factory.createDirection();
    //
    //            // Staff ?
    //            OldStaff staff = current.note.getStaff();
    //            insertStaffId(direction, staff);
    //
    //            com.audiveris.proxymusic.EmptyPrintStyleAlign pmCoda = factory.createEmptyPrintStyleAlign();
    //            // default-x
    //            pmCoda.setDefaultX(toTenths(coda.getReferencePoint().x - current.measure.getLeftX()));
    //
    //            // default-y
    //            pmCoda.setDefaultY(yOf(coda.getReferencePoint(), staff));
    //
    //            DirectionType directionType = new DirectionType();
    //            directionType.getCoda().add(pmCoda);
    //            direction.getDirectionType().add(directionType);
    //
    //            // Need also a Sound element
    //            Sound sound = factory.createSound();
    //            direction.setSound(sound);
    //            sound.setCoda("" + current.measure.getScoreId());
    //            sound.setDivisions(
    //                    new BigDecimal(score.simpleDurationOf(omr.score.entity.Note.QUARTER_DURATION)));
    //
    //            // Everything is now OK
    //            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + coda, ex);
    //        }
    //    }
    //
    //    //----------------------------//
    //    // process DirectionStatement //
    //    //----------------------------//
    //    private void process (DirectionStatement words)
    //    {
    //        try {
    //            logger.debug("Visiting {}", words);
    //
    //            String content = words.getText().getContent();
    //
    //            if (content != null) {
    //                Direction direction = factory.createDirection();
    //                DirectionType directionType = factory.createDirectionType();
    //                FormattedText pmWords = factory.createFormattedText();
    //
    //                pmWords.setValue(content);
    //
    //                // Staff
    //                OldStaff staff = current.note.getStaff();
    //                insertStaffId(direction, staff);
    //
    //                // Placement
    //                direction.setPlacement(
    //                        (words.getReferencePoint().y < current.note.getCenter().y) ? AboveBelow.ABOVE
    //                                : AboveBelow.BELOW);
    //
    //                // default-y
    //                pmWords.setDefaultY(yOf(words.getReferencePoint(), staff));
    //
    //                // Font information
    //                setFontInfo(pmWords, words.getText());
    //
    //                // relative-x
    //                pmWords.setRelativeX(
    //                        toTenths(words.getReferencePoint().x - current.note.getCenterLeft().x));
    //
    //                // Everything is now OK
    //                directionType.getWords().add(pmWords);
    //                direction.getDirectionType().add(directionType);
    //                current.pmMeasure.getNoteOrBackupOrForward().add(direction);
    //            }
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + words, ex);
    //        }
    //    }
    //
    //    //---------------------//
    //    // process ChordSymbol //
    //    //---------------------//
    //    private void process (ChordSymbol symbol)
    //    {
    //        try {
    //            logger.debug("Visiting {}", symbol);
    //
    //            omr.score.entity.ChordInfo info = symbol.getInfo();
    //            OldStaff staff = current.note.getStaff();
    //            Harmony harmony = factory.createHarmony();
    //
    //            // default-y
    //            harmony.setDefaultY(yOf(symbol.getReferencePoint(), staff));
    //
    //            // font-size
    //            harmony.setFontSize("" + symbol.getText().getExportedFontSize());
    //
    //            // relative-x
    //            harmony.setRelativeX(
    //                    toTenths(symbol.getReferencePoint().x - current.note.getCenterLeft().x));
    //
    //            // Placement
    //            harmony.setPlacement(
    //                    (symbol.getReferencePoint().y < current.note.getCenter().y) ? AboveBelow.ABOVE
    //                            : AboveBelow.BELOW);
    //
    //            // Staff
    //            insertStaffId(harmony, staff);
    //
    //            // Root
    //            Root root = factory.createRoot();
    //            RootStep rootStep = factory.createRootStep();
    //            rootStep.setValue(stepOf(info.getRoot().step));
    //            root.setRootStep(rootStep);
    //
    //            if (info.getRoot().alter != 0) {
    //                RootAlter alter = factory.createRootAlter();
    //                alter.setValue(new BigDecimal(info.getRoot().alter));
    //                root.setRootAlter(alter);
    //            }
    //
    //            harmony.getHarmonyChord().add(root);
    //
    //            // Kind
    //            Kind kind = factory.createKind();
    //            kind.setValue(kindOf(info.getKind().type));
    //            kind.setText(info.getKind().text);
    //
    //            if (info.getKind().paren) {
    //                kind.setParenthesesDegrees(YesNo.YES);
    //            }
    //
    //            if (info.getKind().symbol) {
    //                kind.setUseSymbols(YesNo.YES);
    //            }
    //
    //            harmony.getHarmonyChord().add(kind);
    //
    //            // Bass
    //            if (info.getBass() != null) {
    //                Bass bass = factory.createBass();
    //                BassStep bassStep = factory.createBassStep();
    //                bassStep.setValue(stepOf(info.getBass().step));
    //                bass.setBassStep(bassStep);
    //
    //                if (info.getBass().alter != 0) {
    //                    BassAlter bassAlter = factory.createBassAlter();
    //                    bassAlter.setValue(new BigDecimal(info.getBass().alter));
    //                    bass.setBassAlter(bassAlter);
    //                }
    //
    //                harmony.getHarmonyChord().add(bass);
    //            }
    //
    //            // Degrees?
    //            for (omr.score.entity.ChordInfo.Degree deg : info.getDegrees()) {
    //                Degree degree = factory.createDegree();
    //
    //                DegreeValue value = factory.createDegreeValue();
    //                value.setValue(new BigInteger("" + deg.value));
    //                degree.setDegreeValue(value);
    //
    //                DegreeAlter alter = factory.createDegreeAlter();
    //                alter.setValue(new BigDecimal(deg.alter));
    //                degree.setDegreeAlter(alter);
    //
    //                DegreeType type = factory.createDegreeType();
    //                type.setValue(typeOf(deg.type));
    //                degree.setDegreeType(type);
    //
    //                harmony.getHarmonyChord().add(degree);
    //            }
    //
    //            // Everything is now OK
    //            current.pmMeasure.getNoteOrBackupOrForward().add(harmony);
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + symbol, ex);
    //        }
    //    }
    //
    //    //------------------//
    //    // process Dynamics //
    //    //------------------//
    //    private void process (Dynamics dynamics)
    //    {
    //        try {
    //            logger.debug("Visiting {}", dynamics);
    //
    //            // No point to export incorrect dynamics
    //            if (dynamics.getShape() == null) {
    //                return;
    //            }
    //
    //            Direction direction = factory.createDirection();
    //            DirectionType directionType = factory.createDirectionType();
    //            com.audiveris.proxymusic.Dynamics pmDynamics = factory.createDynamics();
    //
    //            // Precise dynamic signature
    //            pmDynamics.getPOrPpOrPpp().add(getDynamicsObject(dynamics.getShape()));
    //
    //            // Staff ?
    //            OldStaff staff = current.note.getStaff();
    //            insertStaffId(direction, staff);
    //
    //            // Placement
    //            if (dynamics.getReferencePoint().y < current.note.getCenter().y) {
    //                direction.setPlacement(AboveBelow.ABOVE);
    //            } else {
    //                direction.setPlacement(AboveBelow.BELOW);
    //            }
    //
    //            // default-y
    //            pmDynamics.setDefaultY(yOf(dynamics.getReferencePoint(), staff));
    //
    //            // Relative-x (No offset for the time being) using note left side
    //            pmDynamics.setRelativeX(
    //                    toTenths(dynamics.getReferencePoint().x - current.note.getCenterLeft().x));
    //
    //            // Related sound level, if available
    //            Integer soundLevel = dynamics.getSoundLevel();
    //
    //            if (soundLevel != null) {
    //                Sound sound = factory.createSound();
    //                sound.setDynamics(new BigDecimal(soundLevel));
    //                direction.setSound(sound);
    //            }
    //
    //            // Everything is now OK
    //            directionType.getDynamics().add(pmDynamics);
    //            direction.getDirectionType().add(directionType);
    //            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + dynamics, ex);
    //        }
    //    }
    //
    //----------------//
    // processFermata //
    //----------------//
    private void processFermata (Fermata fermata)
    {
        try {
            logger.debug("Visiting {}", fermata);

            com.audiveris.proxymusic.Fermata pmFermata = factory.createFermata();

            // default-y (of the fermata dot)
            // For upright we use bottom of the box, for inverted the top of the box
            Rectangle box = fermata.getBox();
            Point dot;

            if (fermata.getShape() == Shape.FERMATA_BELOW) {
                dot = new Point(box.x + (box.width / 2), box.y);
            } else {
                dot = new Point(box.x + (box.width / 2), box.y + box.height);
            }

            pmFermata.setDefaultY(yOf(dot, current.note.getStaff()));

            // Type
            pmFermata.setType(
                    (fermata.getShape() == Shape.FERMATA) ? UprightInverted.UPRIGHT
                            : UprightInverted.INVERTED);
            // Everything is now OK
            getNotations().getTiedOrSlurOrTuplet().add(pmFermata);
        } catch (Exception ex) {
            logger.warn("Error visiting " + fermata, ex);
        }
    }

    //------------//
    // processKey //
    //------------//
    private void processKey (KeyInter keySignature)
    {
        try {
            logger.debug("Visiting {}", keySignature);

            if (isNewKeySignature(keySignature)) {
                Key key = factory.createKey();
                key.setFifths(new BigInteger("" + keySignature.getSignature()));

                List<Key> keys = getAttributes().getKey();
                keys.add(key);
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + keySignature, ex);
        }
    }

    //----------------//
    // processMeasure //
    //----------------//
    private void processMeasure (Measure measure)
    {
        try {
            logger.debug("Processing {}", measure);

            // Very first measure in score?
            final boolean isPageFirstMeasure = isFirst.system && isFirst.measure;
            final boolean isScoreFirstMeasure = isFirst.page && isPageFirstMeasure;
            final MeasureStack stack = measure.getStack();

            // Make sure this measure is within the range to be exported
            if (!isDesired(measure)) {
                logger.debug("{} skipped.", measure);

                return;
            }

            logger.debug("{} : {}", measure, isFirst);

            current.measure = measure;
            tupletNumbers.clear();

            // Allocate Measure
            current.pmMeasure = factory.createScorePartwisePartMeasure();
            current.pmMeasure.setNumber(stack.getScoreId(score));

            if (!measure.isDummy()) {
                current.pmMeasure.setWidth(toTenths(measure.getWidth()));
            }

            if (stack.isImplicit()) {
                current.pmMeasure.setImplicit(YesNo.YES);
            }

            //
            //            // Do we need to create & export a dummy initial measure?
            //            if (((measureRange != null) && !measure.isTemporary() && (measure.getIdValue() > 1)) && // TODO: Following line is illegal
            //                    (measure.getScoreId().equals(measureRange.getFirstId()))) {
            //                insertCurrentContext(measure);
            //            }
            //
            // Print?
            new MeasurePrint(measure).process();

            //
            //            // Inside barline?
            //            process(measure.getInsideBarline());
            //
            // Left barline ?
            final Measure prevMeasure = measure.getPreviousSibling();

            if ((prevMeasure != null) && !prevMeasure.isDummy()) {
                processBarline(prevMeasure.getBarline());
            }

            // Divisions?
            if (isPageFirstMeasure) {
                try {
                    getAttributes().setDivisions(
                            new BigDecimal(
                                    current.page.simpleDurationOf(AbstractNoteInter.QUARTER_DURATION)));
                } catch (Exception ex) {
                    if (current.page.getDurationDivisor() == null) {
                        logger.warn(
                                "Not able to infer division value for part {}",
                                current.logicalPart.getPid());
                    } else {
                        logger.warn("Error on divisions", ex);
                    }
                }
            }

            // Number of staves, if > 1
            if (isScoreFirstMeasure && current.logicalPart.isMultiStaff()) {
                getAttributes().setStaves(new BigInteger("" + current.logicalPart.getStaffCount()));
            }

            // Tempo?
            if (isScoreFirstMeasure && !measure.isDummy()) {
                Direction direction = factory.createDirection();
                current.pmMeasure.getNoteOrBackupOrForward().add(direction);

                DirectionType directionType = factory.createDirectionType();
                direction.getDirectionType().add(directionType);

                // Use a dummy words element
                FormattedText pmWords = factory.createFormattedText();
                directionType.getWords().add(pmWords);
                pmWords.setValue("");

                Sound sound = factory.createSound();
                sound.setTempo(new BigDecimal(score.getTempoParam().getTarget()));
                direction.setSound(sound);
            }

            // Insert KeySignature, if any
            if (measure.getKeySignature() != null) {
                processKey(measure.getKeySignature());
            }

            // Insert TimeSignature, if any
            if (measure.getTimeSignature() != null) {
                processTime(measure.getTimeSignature());
            } else if (isScoreFirstMeasure) {
                // We need to insert a time sig!
                ///processTime(2,4,null);
            }

            // Clefs may be inserted further down the measure
            final ClefIterators clefIters = new ClefIterators(measure);

            // Insert clefs that occur before first time slot
            final List<Slot> slots = stack.getSlots();

            if (slots.isEmpty()) {
                clefIters.push(null, null);
            } else {
                clefIters.push(slots.get(0).getXOffset(), null);
            }

            // Now voice per voice (TODO: this won't work for dummy measures!)
            Rational timeCounter = Rational.ZERO;

            for (Voice voice : stack.getVoices()) {
                if (voice.getMeasure() != measure) {
                    continue;
                }

                current.voice = voice;

                // Need a backup ?
                if (!timeCounter.equals(Rational.ZERO)) {
                    insertBackup(timeCounter);
                    timeCounter = Rational.ZERO;
                }

                if (voice.isWhole()) {
                    // Delegate to the chord children directly
                    ChordInter chord = voice.getWholeChord();
                    clefIters.push(measure.getWidth(), chord.getStaff());
                    processChord(chord);
                    timeCounter = stack.getExpectedDuration();
                } else {
                    for (Slot slot : stack.getSlots()) {
                        Voice.SlotVoice info = voice.getSlotInfo(slot);

                        if ((info != null) && // Skip free slots
                                (info.status == Voice.Status.BEGIN)) {
                            ChordInter chord = info.chord;
                            clefIters.push(slot.getXOffset(), chord.getStaff());

                            // Need a forward before this chord ?
                            Rational startTime = chord.getStartTime();

                            if (timeCounter.compareTo(startTime) < 0) {
                                insertForward(startTime.minus(timeCounter), chord);
                                timeCounter = startTime;
                            }

                            // Delegate to the chord children directly
                            processChord(chord);
                            timeCounter = timeCounter.plus(chord.getDuration());
                        }
                    }

                    //                    // Need an ending forward ?
                    //                    if (!stack.isImplicit() && !stack.isFirstHalf()) {
                    //                        Rational termination = voice.getTermination();
                    //
                    //                        if ((termination != null) && (termination.compareTo(Rational.ZERO) < 0)) {
                    //                            Rational delta = termination.opposite();
                    //                            insertForward(delta, voice.getLastChord());
                    //                            timeCounter = timeCounter.plus(delta);
                    //                        }
                    //                    }
                }
            }

            // Clefs that occur after time slots, if any
            clefIters.push(null, null);

            // Right Barline
            if (!measure.isDummy()) {
                processBarline(measure.getBarline());
            }

            // Everything is now OK
            current.pmPart.getMeasure().add(current.pmMeasure);
        } catch (Exception ex) {
            logger.warn("Error visiting " + measure + " in " + current.page, ex);
        }

        // Safer...
        current.endMeasure();
        tupletNumbers.clear();
        isFirst.measure = false;
    }

    //-------------//
    // processNote //
    //-------------//
    private void processNote (AbstractNoteInter note)
    {
        try {
            logger.debug("Visiting {}", note);
            current.note = note;

            ChordInter chord = note.getChord();

            // For first note in chord
            if (chord.getNotes().indexOf(note) == 0) {
                //                // Chord direction events
                //                for (omr.score.entity.Direction node : chord.getDirections()) {
                //                    ///node.accept(this);
                //                    process(node);
                //                }
                //
                //                // Chord symbol, if any
                //                if (chord.getChordSymbol() != null) {
                //                    ///chord.getChordSymbol().accept(this);
                //                    process(chord.getChordSymbol());
                //                }
            }

            current.pmNote = factory.createNote();

            Staff staff = note.getStaff();

            // Chord notation events for first note in chord
            if (chord.getNotes().indexOf(note) == 0) {
                //                for (Notation node : chord.getNotations()) {
                //                    ///node.accept(this);
                //                    process(node);
                //                }
            } else {
                // Chord indication for every other note
                current.pmNote.setChord(new Empty());

                //
                //                // Arpeggiate also?
                //                for (Notation node : chord.getNotations()) {
                //                    if (node instanceof Arpeggiate) {
                //                        ///node.accept(this);
                //                        process((Arpeggiate) node);
                //                    }
                //                }
            }

            // Rest ?
            boolean isMeasureRest = false;

            if (note.getShape().isRest()) {
                Rest rest = factory.createRest();

                // Rest for the whole measure?
                if (current.measure.isMeasureRest(chord)) {
                    rest.setMeasure(YesNo.YES);
                    isMeasureRest = true;
                }

                // Set displayStep & displayOctave for rest
                rest.setDisplayStep(stepOf(note.getStep()));
                rest.setDisplayOctave(note.getOctave());
                current.pmNote.setRest(rest);
            } else {
                // Pitch
                Pitch pitch = factory.createPitch();
                pitch.setStep(stepOf(note.getStep()));
                pitch.setOctave(note.getOctave());

                // Alter?
                AbstractHeadInter head = (AbstractHeadInter) note;
                int alter = head.getAlter();

                if (alter != 0) {
                    pitch.setAlter(new BigDecimal(alter));
                }

                current.pmNote.setPitch(pitch);
            }

            // Default-x (use left side of the note wrt measure)
            if (!current.measure.isDummy()) {
                int noteLeft = note.getCenterLeft().x;
                current.pmNote.setDefaultX(
                        toTenths(noteLeft - current.measure.getAbscissa(LEFT, staff)));
            }

            // Tuplet factor ?
            if (chord.getTupletFactor() != null) {
                TimeModification timeModification = factory.createTimeModification();
                timeModification.setActualNotes(
                        new BigInteger("" + chord.getTupletFactor().actualDen));
                timeModification.setNormalNotes(
                        new BigInteger("" + chord.getTupletFactor().actualNum));
                current.pmNote.setTimeModification(timeModification);
            }

            // Duration
            try {
                final Rational dur;

                if (chord.isWholeRest()) {
                    dur = current.measure.getStack().getActualDuration();
                } else {
                    dur = chord.getDuration();
                }

                current.pmNote.setDuration(new BigDecimal(current.page.simpleDurationOf(dur)));
            } catch (Exception ex) {
                if (current.page.getDurationDivisor() != null) {
                    logger.warn("Not able to get duration of note", ex);
                }
            }

            // Voice
            current.pmNote.setVoice("" + chord.getVoice().getId());

            // Type
            if (!current.measure.isDummy()) {
                if (!isMeasureRest) {
                    NoteType noteType = factory.createNoteType();
                    noteType.setValue("" + getNoteTypeName(note));
                    current.pmNote.setType(noteType);
                }
            }

            //
            //            // For specific mirrored note
            //            if (note.getMirroredNote() != null) {
            //                int fbn = note.getChord().getFlagsNumber() + note.getChord().getBeams().size();
            //
            //                if ((fbn > 0) && (note.getShape() == NOTEHEAD_VOID)) {
            //                    // Indicate that the head should not be filled
            //                    //   <notehead filled="no">normal</notehead>
            //                    Notehead notehead = factory.createNotehead();
            //                    notehead.setFilled(YesNo.NO);
            //                    notehead.setValue(NoteheadValue.NORMAL);
            //                    current.pmNote.setNotehead(notehead);
            //                }
            //            }
            //
            // Stem ?
            if (chord.getStem() != null) {
                Stem pmStem = factory.createStem();
                Point tail = chord.getTailLocation();
                pmStem.setDefaultY(yOf(tail, staff));

                if (tail.y < note.getCenter().y) {
                    pmStem.setValue(StemValue.UP);
                } else {
                    pmStem.setValue(StemValue.DOWN);
                }

                current.pmNote.setStem(pmStem);
            }

            // Staff ?
            if (current.logicalPart.isMultiStaff()) {
                current.pmNote.setStaff(new BigInteger("" + (1 + staff.getIndexInPart())));
            }

            // Dots
            for (int i = 0; i < chord.getDotsNumber(); i++) {
                current.pmNote.getDot().add(factory.createEmptyPlacement());
            }

            if (!note.getShape().isRest()) {
                // Accidental ?
                AbstractHeadInter head = (AbstractHeadInter) note;
                AlterInter alter = head.getAccidental();

                if (alter != null) {
                    Accidental accidental = factory.createAccidental();
                    accidental.setValue(accidentalValueOf(alter.getShape()));
                    current.pmNote.setAccidental(accidental);
                }

                // Beams ?
                for (AbstractBeamInter beam : chord.getBeams()) {
                    com.audiveris.proxymusic.Beam pmBeam = factory.createBeam();
                    pmBeam.setNumber(1 + chord.getBeams().indexOf(beam));

                    if (beam.isHook()) {
                        if (beam.getCenter().x > chord.getStem().getCenter().x) {
                            pmBeam.setValue(BeamValue.FORWARD_HOOK);
                        } else {
                            pmBeam.setValue(BeamValue.BACKWARD_HOOK);
                        }
                    } else {
                        List<ChordInter> chords = beam.getChords();

                        if (chords.get(0) == chord) {
                            pmBeam.setValue(BeamValue.BEGIN);
                        } else if (chords.get(chords.size() - 1) == chord) {
                            pmBeam.setValue(BeamValue.END);
                        } else {
                            pmBeam.setValue(BeamValue.CONTINUE);
                        }
                    }

                    current.pmNote.getBeam().add(pmBeam);
                }
            }

            //            // Ties / Slurs
            //            for (Slur slur : note.getSlurs()) {
            //                ///slur.accept(this);
            //                process(slur);
            //            }
            //
            //            // Lyrics ?
            //            if (note.getSyllables() != null) {
            //                for (LyricsItem syllable : note.getSyllables()) {
            //                    if (syllable.getContent() != null) {
            //                        Lyric pmLyric = factory.createLyric();
            //                        pmLyric.setDefaultY(yOf(syllable.getReferencePoint(), staff));
            //                        pmLyric.setNumber("" + syllable.getLyricsLine().getId());
            //
            //                        TextElementData pmText = factory.createTextElementData();
            //                        pmText.setValue(syllable.getContent());
            //                        pmLyric.getElisionAndSyllabicAndText().add(
            //                                getSyllabic(syllable.getSyllabicType()));
            //                        pmLyric.getElisionAndSyllabicAndText().add(pmText);
            //
            //                        current.pmNote.getLyric().add(pmLyric);
            //                    }
            //                }
            //            }
            //
            // Everything is OK
            current.pmMeasure.getNoteOrBackupOrForward().add(current.pmNote);
        } catch (Exception ex) {
            logger.warn("Error visiting " + note, ex);
        }

        // Safer...
        current.endNote();
    }

    //-------------//
    // processPage //
    //-------------//
    private void processPage (Page page)
    {
        try {
            logger.debug("Processing {}", page);

            isFirst.page = (page == score.getFirstPage());
            isFirst.system = true;
            isFirst.measure = true;
            current.page = page;

            Page prevPage = (Page) page.getPreviousSibling();
            current.pageMeasureIdOffset = (prevPage == null) ? 0
                    : (current.pageMeasureIdOffset
                       + prevPage.getDeltaMeasureId());
            current.scale = page.getScale();
            page.resetDurationDivisor();
        } catch (Exception ex) {
            logger.warn("Error visiting " + page, ex);
        }
    }

    //-------------//
    // processPart //
    //-------------//
    private void processPart (Part part)
    {
        try {
            logger.debug("Processing {}", part);

            //
            //            // Delegate to texts
            //            for (TreeNode node : part.getTexts()) {
            //                ((Text) node).accept(this);
            //            }
            //
            // Delegate to measures
            for (Measure measure : part.getMeasures()) {
                processMeasure(measure);
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + part, ex);
        }
    }

    //
    //    //---------------//
    //    // process Pedal //
    //    //---------------//
    //    private void process (Pedal pedal)
    //    {
    //        try {
    //            logger.debug("Visiting {}", pedal);
    //
    //            Direction direction = new Direction();
    //            DirectionType directionType = new DirectionType();
    //            com.audiveris.proxymusic.Pedal pmPedal = new com.audiveris.proxymusic.Pedal();
    //
    //            // No line (for the time being)
    //            pmPedal.setLine(YesNo.NO);
    //
    //            // Start / Stop type
    //            pmPedal.setType(
    //                    pedal.isStart() ? StartStopChangeContinue.START : StartStopChangeContinue.STOP);
    //
    //            // Staff ?
    //            OldStaff staff = current.note.getStaff();
    //            insertStaffId(direction, staff);
    //
    //            // default-x
    //            pmPedal.setDefaultX(toTenths(pedal.getReferencePoint().x - current.measure.getLeftX()));
    //
    //            // default-y
    //            pmPedal.setDefaultY(yOf(pedal.getReferencePoint(), staff));
    //
    //            // Placement
    //            direction.setPlacement(
    //                    (pedal.getReferencePoint().y < current.note.getCenter().y) ? AboveBelow.ABOVE
    //                            : AboveBelow.BELOW);
    //            // Everything is OK
    //            directionType.setPedal(pmPedal);
    //            direction.getDirectionType().add(directionType);
    //            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + pedal, ex);
    //        }
    //    }
    //
    //--------------//
    // processScore //
    //--------------//
    /**
     * Allocate/populate everything that directly relates to the score instance.
     * The rest of processing is delegated to the score children, that is to say pages (TBI), then
     * systems, etc...
     */
    private void processScore ()
    {
        try {
            logger.debug("Processing {}", score);

            Page firstPage = score.getFirstPage();
            // No version inserted: Let the marshalling class handle it
            //
            {
                // Work? (reference to larger work)
                //
                // Movement? (number/title of this score as a movement)
                // Using score ID within containing book
                if (firstPage.isMovementStart()) {
                    scorePartwise.setMovementNumber("" + score.getId());
                    scorePartwise.setMovementTitle("[Audiveris detected movement]");
                }

                // Identification
                Identification identification = factory.createIdentification();

                // Source
                Book book = score.getFirstPage().getSheet().getBook();
                identification.setSource(book.getImagePath().toString());

                // Encoding
                Encoding encoding = factory.createEncoding();
                scorePartwise.setIdentification(identification);

                // [Encoding]/Software
                encoding.getEncodingDateOrEncoderOrSoftware().add(
                        factory.createEncodingSoftware("Audiveris" + " " + WellKnowns.TOOL_REF));

                // [Encoding]/EncodingDate
                // Let the Marshalling class handle it
                //
                // [Encoding]/Supports
                for (String feature : new String[]{"new-system", "new-page"}) {
                    Supports supports = factory.createSupports();
                    supports.setAttribute(feature);
                    supports.setElement("print");
                    supports.setType(YesNo.YES);
                    supports.setValue("yes");
                    encoding.getEncodingDateOrEncoderOrSoftware().add(
                            factory.createEncodingSupports(supports));
                }

                identification.setEncoding(encoding);
            }

            {
                // Defaults
                Defaults defaults = new Defaults();

                // [Defaults]/Scaling (using first page)
                if (current.scale == null) {
                    current.scale = firstPage.getScale();
                }

                if (current.scale != null) {
                    Scaling scaling = factory.createScaling();
                    defaults.setScaling(scaling);
                    scaling.setMillimeters(
                            new BigDecimal(
                                    String.format("%.4f", (current.scale.getInterline() * 25.4 * 4) / 300))); // Assuming 300 DPI
                    scaling.setTenths(new BigDecimal(40));

                    // [Defaults]/PageLayout (using first page)
                    if (firstPage.getDimension() != null) {
                        PageLayout pageLayout = factory.createPageLayout();
                        defaults.setPageLayout(pageLayout);
                        pageLayout.setPageHeight(toTenths(firstPage.getDimension().height));
                        pageLayout.setPageWidth(toTenths(firstPage.getDimension().width));

                        PageMargins pageMargins = factory.createPageMargins();
                        pageMargins.setType(MarginType.BOTH);
                        pageMargins.setLeftMargin(pageHorizontalMargin);
                        pageMargins.setRightMargin(pageHorizontalMargin);
                        pageMargins.setTopMargin(pageVerticalMargin);
                        pageMargins.setBottomMargin(pageVerticalMargin);
                        pageLayout.getPageMargins().add(pageMargins);
                    }
                }

                // [Defaults]/LyricFont
                Font lyricFont = omr.score.entity.Text.getLyricsFont();
                LyricFont pmLyricFont = factory.createLyricFont();
                pmLyricFont.setFontFamily(lyricFont.getName());
                pmLyricFont.setFontSize("" + omr.score.entity.Text.getLyricsFontSize());

                if (lyricFont.isItalic()) {
                    pmLyricFont.setFontStyle(FontStyle.ITALIC);
                }

                defaults.getLyricFont().add(pmLyricFont);
                scorePartwise.setDefaults(defaults);
            }

            // PartList & sequence of parts (if not done yet)
            if (score.getLogicalParts() == null) {
                // Merge the pages (connecting the parts across pages)
                new ScoreReduction(score).reduce();
            }

            if (score.getLogicalParts() != null) {
                PartList partList = factory.createPartList();
                scorePartwise.setPartList(partList);

                // Here we browse the score hierarchy once for each score logicalPart
                isFirst.part = true;

                for (LogicalPart p : score.getLogicalParts()) {
                    partList.getPartGroupOrScorePart().add(getScorePart(p));
                    isFirst.part = false;
                }
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + score, ex);
        }
    }

    //
    //    //--------------//
    //    // process Slur //
    //    //--------------//
    //    private void process (Slur slur)
    //    {
    //        try {
    //            logger.debug("Visiting {}", slur);
    //
    //            // Make sure we have notes (or extension) on both sides
    //            // TODO: Make an exception for slurs at beginning of page!
    //            if ((slur.getLeftNote() == null) && (slur.getLeftExtension() == null)) {
    //                slur.addError("Non left-connected slur is not exported");
    //
    //                return;
    //            }
    //
    //            // TODO: Make an exception for slurs at end of page!
    //            if ((slur.getRightNote() == null) && (slur.getRightExtension() == null)) {
    //                slur.addError("Non right-connected slur is not exported");
    //
    //                return;
    //            }
    //
    //            // Note contextual data
    //            boolean isStart = slur.getLeftNote() == current.note;
    //            int noteLeft = current.note.getCenterLeft().x;
    //            OldStaff staff = current.note.getStaff();
    //
    //            if (slur.isTie()) {
    //                // Tie element
    //                Tie tie = factory.createTie();
    //                tie.setType(isStart ? StartStop.START : StartStop.STOP);
    //                current.pmNote.getTie().add(tie);
    //
    //                // Tied element
    //                Tied tied = factory.createTied();
    //
    //                // Type
    //                tied.setType(isStart ? StartStopContinue.START : StartStopContinue.STOP);
    //
    //                // Orientation
    //                if (isStart) {
    //                    tied.setOrientation(slur.isBelow() ? OverUnder.UNDER : OverUnder.OVER);
    //                }
    //
    //                // Bezier
    //                if (isStart) {
    //                    tied.setDefaultX(toTenths(slur.getCurve().getX1() - noteLeft));
    //                    tied.setDefaultY(yOf(slur.getCurve().getY1(), staff));
    //                    tied.setBezierX(toTenths(slur.getCurve().getCtrlX1() - noteLeft));
    //                    tied.setBezierY(yOf(slur.getCurve().getCtrlY1(), staff));
    //                } else {
    //                    tied.setDefaultX(toTenths(slur.getCurve().getX2() - noteLeft));
    //                    tied.setDefaultY(yOf(slur.getCurve().getY2(), staff));
    //                    tied.setBezierX(toTenths(slur.getCurve().getCtrlX2() - noteLeft));
    //                    tied.setBezierY(yOf(slur.getCurve().getCtrlY2(), staff));
    //                }
    //
    //                getNotations().getTiedOrSlurOrTuplet().add(tied);
    //            } else {
    //                // Slur element
    //                com.audiveris.proxymusic.Slur pmSlur = factory.createSlur();
    //
    //                // Number attribute
    //                Integer num = slurNumbers.get(slur);
    //
    //                if (num != null) {
    //                    pmSlur.setNumber(num);
    //                    slurNumbers.remove(slur);
    //
    //                    logger.debug(
    //                            "{} last use {} -> {}",
    //                            current.note.getContextString(),
    //                            num,
    //                            slurNumbers.toString());
    //                } else {
    //                    // Determine first available number
    //                    for (num = 1; num <= 6; num++) {
    //                        if (!slurNumbers.containsValue(num)) {
    //                            if (slur.getRightExtension() != null) {
    //                                slurNumbers.put(slur.getRightExtension(), num);
    //                            } else {
    //                                slurNumbers.put(slur, num);
    //                            }
    //
    //                            pmSlur.setNumber(num);
    //
    //                            logger.debug(
    //                                    "{} first use {} -> {}",
    //                                    current.note.getContextString(),
    //                                    num,
    //                                    slurNumbers.toString());
    //
    //                            break;
    //                        }
    //                    }
    //                }
    //
    //                // Type
    //                pmSlur.setType(isStart ? StartStopContinue.START : StartStopContinue.STOP);
    //
    //                // Placement
    //                if (isStart) {
    //                    pmSlur.setPlacement(slur.isBelow() ? AboveBelow.BELOW : AboveBelow.ABOVE);
    //                }
    //
    //                // Bezier
    //                if (isStart) {
    //                    pmSlur.setDefaultX(toTenths(slur.getCurve().getX1() - noteLeft));
    //                    pmSlur.setDefaultY(yOf(slur.getCurve().getY1(), staff));
    //                    pmSlur.setBezierX(toTenths(slur.getCurve().getCtrlX1() - noteLeft));
    //                    pmSlur.setBezierY(yOf(slur.getCurve().getCtrlY1(), staff));
    //                } else {
    //                    pmSlur.setDefaultX(toTenths(slur.getCurve().getX2() - noteLeft));
    //                    pmSlur.setDefaultY(yOf(slur.getCurve().getY2(), staff));
    //                    pmSlur.setBezierX(toTenths(slur.getCurve().getCtrlX2() - noteLeft));
    //                    pmSlur.setBezierY(yOf(slur.getCurve().getCtrlY2(), staff));
    //                }
    //
    //                getNotations().getTiedOrSlurOrTuplet().add(pmSlur);
    //            }
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + slur, ex);
    //        }
    //    }
    //
    //---------------//
    // processSystem //
    //---------------//
    /**
     * Allocate/populate everything that directly relates to this system in the current
     * logicalPart.
     * The rest of processing is directly delegated to the measures
     *
     * @param system the system to export
     */
    private void processSystem (SystemInfo system)
    {
        try {
            logger.debug("Processing {}", system);

            current.system = system;
            isFirst.measure = true;

            Part systemPart = system.getPartById(current.logicalPart.getId());

            if (systemPart != null) {
                processPart(systemPart);
            } else {
                // Need to build a dummy system Part on-the-fly
                Part dummyPart = system.getFirstPart().createDummyPart(current.logicalPart.getId());
                processPart(dummyPart);
            }

            // If we have exported a measure, we are no longer in the first system
            if (!isFirst.measure) {
                isFirst.system = false;
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + system, ex);
        }
    }

    //-------------//
    // processTime //
    //-------------//
    private void processTime (TimeInter timeSig)
    {
        logger.debug("Visiting {}", timeSig);
        processTime(timeSig.getNumerator(), timeSig.getDenominator(), timeSig.getShape());
    }

    //-------------//
    // processTime //
    //-------------//
    private void processTime (int num,
                              int den,
                              Shape shape)
    {
        Time time = factory.createTime();

        // Beats
        time.getTimeSignature().add(factory.createTimeBeats("" + num));

        // BeatType
        time.getTimeSignature().add(factory.createTimeBeatType("" + den));

        // Symbol ?
        if (shape != null) {
            switch (shape) {
            case COMMON_TIME:
                time.setSymbol(TimeSymbol.COMMON);

                break;

            case CUT_TIME:
                time.setSymbol(TimeSymbol.CUT);

                break;
            }
        }

        List<Time> times = getAttributes().getTime();
        times.add(time);
    }

    //-------------//
    // setFontInfo //
    //-------------//
    private void setFontInfo (FormattedText formattedText,
                              Text text)
    {
        FontInfo fontInfo = text.getSentence().getFirstWord().getFontInfo();
        formattedText.setFontSize("" + text.getExportedFontSize());

        // Family
        if (fontInfo.isSerif) {
            formattedText.setFontFamily("serif");
        } else if (fontInfo.isMonospace) {
            formattedText.setFontFamily("monospace");
        } else {
            formattedText.setFontFamily("sans-serif");
        }

        // Italic?
        if (fontInfo.isItalic) {
            formattedText.setFontStyle(FontStyle.ITALIC);
        }

        // Bold?
        if (fontInfo.isBold) {
            formattedText.setFontWeight(FontWeight.BOLD);
        }
    }

    //- Utilities --------------------------------------------------------------
    //
    //----------//
    // toTenths //
    //----------//
    /**
     * Convert a distance expressed in pixels to a string value
     * expressed in tenths of interline.
     *
     * @param dist the distance in pixels
     * @return the number of tenths as a string
     */
    private BigDecimal toTenths (double dist)
    {
        return new BigDecimal("" + (int) Math.rint((10f * dist) / current.scale.getInterline()));
    }

    //-----//
    // yOf //
    //-----//
    /**
     * Report the musicXML staff-based Y value of a Point ordinate.
     *
     * @param ordinate the ordinate (page-based, in pixels)
     * @param staff    the related staff
     * @return the upward-oriented ordinate wrt staff top line (in tenths)
     */
    private BigDecimal yOf (double ordinate,
                            Staff staff)
    {
        return toTenths(staff.getLeftY(TOP) - ordinate);
    }

    //-----//
    // yOf //
    //-----//
    /**
     * Report the musicXML staff-based Y value of a Point.
     * This method is safer than the other one which simply accepts a (detyped)
     * double ordinate.
     *
     * @param point the pixel point
     * @param staff the related staff
     * @return the upward-oriented ordinate wrt staff top line (in tenths)
     */
    private BigDecimal yOf (Point point,
                            Staff staff)
    {
        return yOf(point.y, staff);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //
    //---------//
    // Current //
    //---------//
    /** Keep references of all current entities. */
    private static class Current
    {
        //~ Instance fields ------------------------------------------------------------------------

        // Score dependent
        com.audiveris.proxymusic.Work pmWork;

        // Part dependent
        LogicalPart logicalPart;

        com.audiveris.proxymusic.ScorePartwise.Part pmPart;

        // Page dependent
        Page page;

        int pageMeasureIdOffset = 0;

        Scale scale;

        // System dependent
        SystemInfo system;

        // Measure dependent
        Measure measure;

        com.audiveris.proxymusic.ScorePartwise.Part.Measure pmMeasure;

        Voice voice;

        com.audiveris.proxymusic.Attributes pmAttributes;

        // Note dependent
        AbstractNoteInter note;

        com.audiveris.proxymusic.Note pmNote;

        com.audiveris.proxymusic.Notations pmNotations;

        //~ Methods --------------------------------------------------------------------------------
        // Cleanup at end of measure
        void endMeasure ()
        {
            measure = null;
            pmMeasure = null;
            voice = null;
            pmAttributes = null;

            endNote();
        }

        // Cleanup at end of note
        void endNote ()
        {
            note = null;
            pmNote = null;
            pmNotations = null;
        }
    }

    //---------//
    // IsFirst //
    //---------//
    /** Composite flag to help drive processing of any entity. */
    private static class IsFirst
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** We are writing the first part of the score */
        boolean part;

        /** We are writing the first page of the score */
        boolean page;

        /** We are writing the first system in the current page */
        boolean system;

        /** We are writing the first measure in current system (in current logicalPart) */
        boolean measure;

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public java.lang.String toString ()
        {
            StringBuilder sb = new StringBuilder();

            if (part) {
                sb.append(" firstPart");
            }

            if (page) {
                sb.append(" firstPage");
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

    //---------------//
    // ClefIterators //
    //---------------//
    /**
     * Class to handle the insertion of clefs in a measure.
     * If needed, this class could be reused for some attribute other than clef,
     * such as key signature or time signature (if these attributes can indeed
     * occur in the middle of a measure. To be checked).
     */
    private class ClefIterators
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Containing measure. */
        private final Measure measure;

        /** Staves of the containing part. */
        private final List<Staff> staves;

        /** Per staff, iterator on Clefs sorted by abscissa. */
        private final Map<Staff, ListIterator<ClefInter>> iters;

        //~ Constructors ---------------------------------------------------------------------------
        public ClefIterators (Measure measure)
        {
            this.measure = measure;

            staves = measure.getPart().getStaves();

            // Temporary map: staff -> staff's clefs
            Map<Staff, List<ClefInter>> map = new HashMap<Staff, List<ClefInter>>();

            for (ClefInter clef : measure.getClefs()) {
                Staff staff = clef.getStaff();
                List<ClefInter> list = map.get(staff);

                if (list == null) {
                    map.put(staff, list = new ArrayList<ClefInter>());
                }

                list.add(clef);
            }

            // Populate iterators
            iters = new HashMap<Staff, ListIterator<ClefInter>>();

            for (Map.Entry<Staff, List<ClefInter>> entry : map.entrySet()) {
                List<ClefInter> list = entry.getValue();
                Collections.sort(list, Inter.byCenterAbscissa);
                iters.put(entry.getKey(), list.listIterator());
            }
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Push as far as possible the relevant clefs iterators, according to the
         * current abscissa offset.
         *
         * @param xOffset       the abscissa offset of chord to be exported, if any
         * @param specificStaff a specific staff, or null for all staves
         */
        public void push (Integer xOffset,
                          Staff specificStaff)
        {
            if (xOffset != null) {
                MeasureStack stack = measure.getStack();

                for (Staff staff : staves) {
                    List<Staff> theStaff = Collections.singletonList(staff);

                    if ((specificStaff == null) || (staff == specificStaff)) {
                        final ListIterator<ClefInter> it = iters.get(staff);

                        // Check pending clef WRT current abscissa offset
                        if ((it != null) && it.hasNext()) {
                            final ClefInter clef = it.next();

                            if (measure.isDummy() /// || measure.isTemporary()
                                || (stack.getXOffset(clef.getCenter(), theStaff) <= xOffset)) {
                                // Consume this clef
                                processClef(clef);
                            } else {
                                // Reset iterator
                                it.previous();
                            }
                        }
                    }
                }
            } else {
                // Flush all iterators
                for (ListIterator<ClefInter> it : iters.values()) {
                    while (it.hasNext()) {
                        processClef(it.next());
                    }
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Constant.Integer pageHorizontalMargin = new Constant.Integer(
                "tenths",
                80,
                "Page horizontal margin");

        Constant.Integer pageVerticalMargin = new Constant.Integer(
                "tenths",
                80,
                "Page vertical margin");

        Constant.Boolean avoidTupletBrackets = new Constant.Boolean(
                false,
                "Should we avoid brackets for all tuplets");
    }

    //--------------//
    // MeasurePrint //
    //--------------//
    /**
     * Handles the print element for a measure.
     */
    private class MeasurePrint
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Measure measure;

        private final com.audiveris.proxymusic.Print pmPrint;

        /** Needed to remove the element if not actually used. */
        private boolean used = false;

        //~ Constructors ---------------------------------------------------------------------------
        public MeasurePrint (Measure measure)
        {
            this.measure = measure;

            // Allocate and insert Print immediately (before any attribute or note element)
            // It will later be removed if not actually used in the measure
            pmPrint = factory.createPrint();
            current.pmMeasure.getNoteOrBackupOrForward().add(pmPrint);
        }

        //~ Methods --------------------------------------------------------------------------------
        public void process ()
        {
            populatePrint();

            // Something to print actually?
            if (!used) {
                current.pmMeasure.getNoteOrBackupOrForward().remove(pmPrint);
            }
        }

        private com.audiveris.proxymusic.Print getPrint ()
        {
            used = true;

            return pmPrint;
        }

        private void populatePrint ()
        {
            // New system?
            if (isFirst.measure) {
                if (!isFirst.system) {
                    getPrint().setNewSystem(YesNo.YES);
                }
            } else {
                getPrint().setNewSystem(YesNo.NO);
            }

            // New page?
            if (!isFirst.page && isFirst.system && isFirst.measure) {
                getPrint().setNewPage(YesNo.YES);
            }

            // SystemLayout?
            if (isFirst.measure && !measure.isDummy()) {
                SystemLayout systemLayout = factory.createSystemLayout();

                // SystemMargins
                SystemMargins systemMargins = factory.createSystemMargins();
                systemLayout.setSystemMargins(systemMargins);
                systemMargins.setLeftMargin(
                        toTenths(current.system.getLeft()).subtract(pageHorizontalMargin));
                systemMargins.setRightMargin(
                        toTenths(
                                current.page.getDimension().width - current.system.getLeft()
                                - current.system.getWidth()).subtract(pageHorizontalMargin));

                if (isFirst.system) {
                    // TopSystemDistance
                    systemLayout.setTopSystemDistance(
                            toTenths(current.system.getTop()).subtract(pageVerticalMargin));
                } else {
                    // SystemDistance
                    SystemInfo prevSystem = current.system.getPrecedingInPage();
                    systemLayout.setSystemDistance(
                            toTenths(current.system.getTop() - prevSystem.getBottom()));
                }

                getPrint().setSystemLayout(systemLayout);
            }

            // StaffLayout for all staves in this logicalPart, except 1st system staff
            if (isFirst.measure && !measure.isDummy()) {
                SystemInfo system = measure.getStack().getSystem();

                for (Staff staff : measure.getPart().getStaves()) {
                    if (!isFirst.part || (staff.getIndexInPart() > 0)) {
                        try {
                            StaffLayout staffLayout = factory.createStaffLayout();
                            staffLayout.setNumber(
                                    new BigInteger("" + (1 + staff.getIndexInPart())));

                            int staffIndexInSystem = system.getStaves().indexOf(staff);

                            if (staffIndexInSystem > 0) {
                                Staff staffAbove = system.getStaves().get(staffIndexInSystem - 1);
                                staffLayout.setStaffDistance(
                                        toTenths(staff.getLeftY(TOP) - staffAbove.getLeftY(BOTTOM)));
                                getPrint().getStaffLayout().add(staffLayout);
                            }
                        } catch (Exception ex) {
                            logger.warn(
                                    "Error exporting staff layout system#" + current.system.getId()
                                    + " part#" + current.logicalPart.getId() + " staff#" + staff.getId(),
                                    ex);
                        }
                    }
                }
            }

            // Do not print artificial parts
            if (isFirst.measure) {
                StaffDetails staffDetails = factory.createStaffDetails();
                staffDetails.setPrintObject(measure.isDummy() ? YesNo.NO : YesNo.YES);
                getAttributes().getStaffDetails().add(staffDetails);
            }

            // Measure numbering?
            if (isFirst.system && isFirst.measure) {
                com.audiveris.proxymusic.MeasureNumbering pmNumbering = factory.createMeasureNumbering();

                if (isFirst.part) {
                    pmNumbering.setValue(MeasureNumberingValue.SYSTEM);
                } else {
                    pmNumbering.setValue(MeasureNumberingValue.NONE);
                }

                getPrint().setMeasureNumbering(pmNumbering);
            }
        }
    }
}
