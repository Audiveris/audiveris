//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P a r t w i s e B u i l d e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.OMR;
import omr.WellKnowns;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.math.Rational;
import static omr.score.MusicXML.*;

import omr.sheet.Book;
import omr.sheet.Part;
import omr.sheet.PartBarline;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SheetStub;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.MeasureStack;
import omr.sheet.rhythm.Slot;
import omr.sheet.rhythm.Voice;
import omr.sheet.ui.StubsController;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractBeamInter;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.AbstractNoteInter;
import omr.sig.inter.AlterInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.HeadChordInter;
import omr.sig.inter.Inter;
import omr.sig.inter.KeyInter;
import omr.sig.inter.LyricItemInter;
import omr.sig.inter.PedalInter;
import omr.sig.inter.RestChordInter;
import omr.sig.inter.SentenceInter;
import omr.sig.inter.SlurInter;
import omr.sig.inter.SmallChordInter;
import omr.sig.inter.StemInter;
import omr.sig.inter.TimeInter;
import omr.sig.inter.TupletInter;
import omr.sig.inter.WedgeInter;
import omr.sig.relation.ChordPedalRelation;
import omr.sig.relation.ChordSentenceRelation;
import omr.sig.relation.ChordSyllableRelation;
import omr.sig.relation.ChordWedgeRelation;
import omr.sig.relation.FlagStemRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.SlurHeadRelation;

import omr.text.FontInfo;
import omr.text.TextRole;
import static omr.text.TextRole.*;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.LEFT;
import static omr.util.HorizontalSide.RIGHT;
import omr.util.OmrExecutors;
import static omr.util.VerticalSide.*;

import com.audiveris.proxymusic.AboveBelow;
import com.audiveris.proxymusic.Accidental;
import com.audiveris.proxymusic.Articulations;
import com.audiveris.proxymusic.Attributes;
import com.audiveris.proxymusic.Backup;
import com.audiveris.proxymusic.BackwardForward;
import com.audiveris.proxymusic.BarStyleColor;
import com.audiveris.proxymusic.Barline;
import com.audiveris.proxymusic.Beam;
import com.audiveris.proxymusic.BeamValue;
import com.audiveris.proxymusic.Clef;
import com.audiveris.proxymusic.ClefSign;
import com.audiveris.proxymusic.Credit;
import com.audiveris.proxymusic.Defaults;
import com.audiveris.proxymusic.Direction;
import com.audiveris.proxymusic.DirectionType;
import com.audiveris.proxymusic.Empty;
import com.audiveris.proxymusic.Encoding;
import com.audiveris.proxymusic.FontStyle;
import com.audiveris.proxymusic.FontWeight;
import com.audiveris.proxymusic.FormattedText;
import com.audiveris.proxymusic.Forward;
import com.audiveris.proxymusic.Grace;
import com.audiveris.proxymusic.Identification;
import com.audiveris.proxymusic.Key;
import com.audiveris.proxymusic.LeftCenterRight;
import com.audiveris.proxymusic.Lyric;
import com.audiveris.proxymusic.LyricFont;
import com.audiveris.proxymusic.MarginType;
import com.audiveris.proxymusic.MeasureNumbering;
import com.audiveris.proxymusic.MeasureNumberingValue;
import com.audiveris.proxymusic.MidiInstrument;
import com.audiveris.proxymusic.Notations;
import com.audiveris.proxymusic.Note;
import com.audiveris.proxymusic.NoteType;
import com.audiveris.proxymusic.ObjectFactory;
import com.audiveris.proxymusic.Ornaments;
import com.audiveris.proxymusic.OverUnder;
import com.audiveris.proxymusic.PageLayout;
import com.audiveris.proxymusic.PageMargins;
import com.audiveris.proxymusic.PartList;
import com.audiveris.proxymusic.PartName;
import com.audiveris.proxymusic.Pedal;
import com.audiveris.proxymusic.Pitch;
import com.audiveris.proxymusic.Print;
import com.audiveris.proxymusic.Repeat;
import com.audiveris.proxymusic.Rest;
import com.audiveris.proxymusic.RightLeftMiddle;
import com.audiveris.proxymusic.Scaling;
import com.audiveris.proxymusic.ScoreInstrument;
import com.audiveris.proxymusic.ScorePart;
import com.audiveris.proxymusic.ScorePartwise;
import com.audiveris.proxymusic.Slur;
import com.audiveris.proxymusic.Sound;
import com.audiveris.proxymusic.StaffDetails;
import com.audiveris.proxymusic.StaffLayout;
import com.audiveris.proxymusic.StartStop;
import com.audiveris.proxymusic.StartStopChangeContinue;
import com.audiveris.proxymusic.StartStopContinue;
import com.audiveris.proxymusic.Stem;
import com.audiveris.proxymusic.StemValue;
import com.audiveris.proxymusic.Supports;
import com.audiveris.proxymusic.SystemLayout;
import com.audiveris.proxymusic.SystemMargins;
import com.audiveris.proxymusic.TextElementData;
import com.audiveris.proxymusic.Tie;
import com.audiveris.proxymusic.Tied;
import com.audiveris.proxymusic.Time;
import com.audiveris.proxymusic.TimeModification;
import com.audiveris.proxymusic.TimeSymbol;
import com.audiveris.proxymusic.Tuplet;
import com.audiveris.proxymusic.TypedText;
import com.audiveris.proxymusic.Wedge;
import com.audiveris.proxymusic.WedgeType;
import com.audiveris.proxymusic.Work;
import com.audiveris.proxymusic.YesNo;
import com.audiveris.proxymusic.util.Marshalling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Point2D;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
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

    /** Score source. */
    private Source source;

    /** Current context. */
    private final Current current = new Current();

    /** Current flags. */
    private final IsFirst isFirst = new IsFirst();

    /** Map of Slur numbers, reset for every LogicalPart. */
    private final Map<SlurInter, Integer> slurNumbers = new HashMap<SlurInter, Integer>();

    /** Map of Tuplet numbers, reset for every Measure. */
    private final Map<TupletInter, Integer> tupletNumbers = new HashMap<TupletInter, Integer>();

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
     * Visit the whole score tree and build the corresponding ScorePartwise.
     *
     * @param score the score to export (cannot be null)
     * @return the populated ScorePartwise
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public static ScorePartwise build (Score score)
            throws InterruptedException, ExecutionException
    {
        Objects.requireNonNull(score, "Trying to export a null score");

        final PartwiseBuilder builder = new PartwiseBuilder(score);

        builder.processScore();

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

    //----------//
    // areEqual //
    //----------//
    /**
     * Check whether the two Clef instances are equal.
     *
     * @param left  one clef
     * @param right another clef
     * @return true if equal
     */
    private static boolean areEqual (Clef left,
                                     Clef right)
    {
        return Objects.equals(left.getNumber(), right.getNumber())
               && Objects.equals(left.getSign(), right.getSign())
               && Objects.equals(left.getLine(), right.getLine())
               && Objects.equals(left.getClefOctaveChange(), right.getClefOctaveChange());
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
    // getCurrentKey //
    //---------------//
    /**
     * Report the key that applies to the current part.
     * TODO: we could as well add a Key member in current structure?
     *
     * @return the current key
     */
    private Key getCurrentKey ()
    {
        // Browse the current list of measures backwards within current part
        List<ScorePartwise.Part.Measure> measures = current.pmPart.getMeasure();

        for (ListIterator<ScorePartwise.Part.Measure> it = measures.listIterator(measures.size());
                it.hasPrevious();) {
            ScorePartwise.Part.Measure pmMeasure = it.previous();

            for (Object obj : pmMeasure.getNoteOrBackupOrForward()) {
                if (obj instanceof Attributes) {
                    Attributes attributes = (Attributes) obj;
                    List<Key> keys = attributes.getKey();

                    if (!keys.isEmpty()) {
                        return keys.get(keys.size() - 1);
                    }
                }
            }
        }

        return null; // No key found
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

    //---------//
    // addSlur //
    //---------//
    private void addSlur (SlurInter slur,
                          Integer num,
                          HorizontalSide side,
                          boolean continuation)
    {
        final Staff staff = current.note.getStaff();
        final CubicCurve2D curve = slur.getCurve();

        // Slur element
        final Slur pmSlur = factory.createSlur();

        if (num != null) {
            pmSlur.setNumber(num);
        }

        // Type
        pmSlur.setType(
                continuation ? StartStopContinue.CONTINUE
                        : ((side == LEFT) ? StartStopContinue.START : StartStopContinue.STOP));

        // Placement
        if (side == LEFT) {
            pmSlur.setPlacement(slur.isAbove() ? AboveBelow.ABOVE : AboveBelow.BELOW);
        }

        // End point
        final Point2D end = (side == LEFT) ? curve.getP1() : curve.getP2();
        pmSlur.setDefaultX(toTenths(end.getX() - current.note.getCenterLeft().x));
        pmSlur.setDefaultY(yOf(end, staff));

        // Control point
        final Point2D ctrl = (side == LEFT) ? curve.getCtrlP1() : curve.getCtrlP2();

        if ((side == LEFT) && continuation) {
            pmSlur.setBezierX2(toTenths(ctrl.getX() - end.getX()));
            pmSlur.setBezierY2(toTenths(end.getY() - ctrl.getY()));
        } else {
            pmSlur.setBezierX(toTenths(ctrl.getX() - end.getX()));
            pmSlur.setBezierY(toTenths(end.getY() - ctrl.getY()));
        }

        getNotations().getTiedOrSlurOrTuplet().add(pmSlur);
    }

    //-----------//
    // buildClef //
    //-----------//
    private Clef buildClef (ClefInter clef)
    {
        Clef pmClef = factory.createClef();

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

    //-----------------//
    // createScorePart //
    //-----------------//
    /**
     * Allocate a Proxymusic {@link ScorePart} instance that corresponds to the
     * provided Audiveris {@link LogicalPart} instance.
     *
     * @param logicalPart provided score LogicalPart
     * @return the properly initialized ScorePart instance
     */
    private ScorePartwise.Part createScorePart (LogicalPart logicalPart)
    {
        logger.debug("Creating ScorePartwise.Part for {}", logicalPart);

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
        ScorePartwise.Part pmPart = factory.createScorePartwisePart();
        scorePartwise.getPart().add(pmPart);
        pmPart.setId(pmScorePart);

        return pmPart;
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

    //----------//
    // getGrace //
    //----------//
    /**
     * Report the grace chord, if any, which precedes the provided head-chord.
     *
     * @param chord the standard chord to check
     * @return the linked grace chord if any
     */
    private SmallChordInter getGrace (HeadChordInter chord)
    {
        final SIGraph sig = chord.getSig();

        for (Inter interNote : chord.getNotes()) {
            for (Relation rel : sig.getRelations(interNote, SlurHeadRelation.class)) {
                SlurInter slur = (SlurInter) sig.getOppositeInter(interNote, rel);
                AbstractHeadInter head = slur.getHead(HorizontalSide.LEFT);

                if ((head != null) && head.getShape().isSmall()) {
                    return (SmallChordInter) head.getChord();
                }
            }
        }

        return null;
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

    //    //---------------//
    // getSlurNumber //
    //---------------//
    private Integer getSlurNumber (SlurInter slur)
    {
        Integer num = slurNumbers.get(slur);

        if (num != null) {
            slurNumbers.remove(slur);
            logger.debug("{} last use {} -> {}", slur, num, slurNumbers);

            return num;
        } else {
            // Determine first available number
            for (int i = 1; i <= 6; i++) {
                if (!slurNumbers.containsValue(i)) {
                    if (slur.getExtension(RIGHT) != null) {
                        slurNumbers.put(slur.getExtension(RIGHT), i);
                    } else {
                        slurNumbers.put(slur, i);
                    }

                    logger.debug("{} first use {} -> {}", slur, i, slurNumbers);

                    return i;
                }
            }
        }

        logger.warn("No number for {}", slur);

        return null;
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

    //---------------//
    // insertForward //
    //---------------//
    private void insertForward (Rational delta,
                                AbstractChordInter chord)
    {
        try {
            Forward forward = factory.createForward();
            forward.setDuration(new BigDecimal(current.page.simpleDurationOf(delta)));
            forward.setVoice("" + current.voice.getId());
            current.pmMeasure.getNoteOrBackupOrForward().add(forward);

            // Staff ? (only if more than one staff in logicalPart)
            insertStaffId(forward, chord.getTopStaff());
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
    // isNewClef //
    //-----------//
    /**
     * Make sure we have a NEW clef, not already assigned in proper staff.
     * We simply look in previous measures within current part data.
     *
     * @param newClef the potentially new clef
     * @return true if this clef is really new
     */
    private boolean isNewClef (Clef newClef)
    {
        // Browse the  current list of measures backwards
        List<ScorePartwise.Part.Measure> measures = current.pmPart.getMeasure();

        for (ListIterator<ScorePartwise.Part.Measure> mit = measures.listIterator(measures.size());
                mit.hasPrevious();) {
            ScorePartwise.Part.Measure pmMeasure = mit.previous();

            // Look backwards in measure items, checking staff
            List<Object> items = pmMeasure.getNoteOrBackupOrForward();

            for (ListIterator<Object> it = items.listIterator(items.size()); it.hasPrevious();) {
                Object obj = it.previous();

                if (obj instanceof Attributes) {
                    Attributes attributes = (Attributes) obj;

                    // Check for clef on proper staff
                    for (Clef clef : attributes.getClef()) {
                        // Check proper staff (in case of multi-staff part)
                        if (Objects.equals(clef.getNumber(), newClef.getNumber())) {
                            // Same staff, so check whether the clef is the same
                            return !areEqual(clef, newClef);
                        }
                    }
                }
            }
        }

        return true; // Since no previous clef was found for the same staff
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
                    Barline pmBarline = factory.createBarline();
                    BarStyleColor barStyleColor = factory.createBarStyleColor();
                    barStyleColor.setValue(barStyleOf(style));

                    if (barline == current.measure.getRightBarline()) {
                        // The bar is on right side
                        pmBarline.setLocation(RightLeftMiddle.RIGHT);

                        // Repeat?
                        ///if (barline.isRightRepeat()) {
                        if (current.measure.getStack().isRepeat(RIGHT)) {
                            Repeat repeat = factory.createRepeat();
                            repeat.setDirection(BackwardForward.BACKWARD);
                            pmBarline.setRepeat(repeat);
                        }
                    } else {
                        // Inside barline (on left)
                        // Or bar is on left side
                        pmBarline.setLocation(RightLeftMiddle.LEFT);

                        // Repeat?
                        ///if (barline.isLeftRepeat()) {
                        if (current.measure.getStack().isRepeat(LEFT)) {
                            Repeat repeat = factory.createRepeat();
                            repeat.setDirection(BackwardForward.FORWARD);
                            pmBarline.setRepeat(repeat);
                        }
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
    private void processChord (AbstractChordInter chord)
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

            Clef pmClef = buildClef(clef);

            if (isNewClef(pmClef)) {
                getAttributes().getClef().add(pmClef);
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + clef + " " + ex, ex);
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
    //            EmptyPrintStyleAlign pmCoda = factory.createEmptyPrintStyleAlign();
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
    //------------------//
    // processDirection //
    //------------------//
    private void processDirection (SentenceInter sentence)
    {
        try {
            logger.debug("Visiting {}", sentence);

            String content = sentence.getValue();

            if (content != null) {
                Direction direction = factory.createDirection();
                DirectionType directionType = factory.createDirectionType();
                FormattedText pmWords = factory.createFormattedText();
                Point location = sentence.getLocation();

                pmWords.setValue(content);

                // Staff
                Staff staff = current.note.getStaff();
                insertStaffId(direction, staff);

                // Placement
                direction.setPlacement(
                        (location.y < current.note.getCenter().y) ? AboveBelow.ABOVE : AboveBelow.BELOW);

                // default-y
                pmWords.setDefaultY(yOf(location, staff));

                // Font information
                setFontInfo(pmWords, sentence);

                // relative-x
                pmWords.setRelativeX(toTenths(location.x - current.note.getCenterLeft().x));

                // Everything is now OK
                directionType.getWords().add(pmWords);
                direction.getDirectionType().add(directionType);
                current.pmMeasure.getNoteOrBackupOrForward().add(direction);
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + sentence, ex);
        }
    }

    //
    //    //------------------//
    //    // processChordName //
    //    //------------------//
    //    private void processChordName (ChordNameInter chordName)
    //    {
    //        try {
    //            logger.debug("Visiting {}", chordName);
    //
    //            omr.score.entity.ChordInfo info = chordName.getInfo();
    //            OldStaff staff = current.note.getStaff();
    //            Harmony harmony = factory.createHarmony();
    //
    //            // default-y
    //            harmony.setDefaultY(yOf(chordName.getReferencePoint(), staff));
    //
    //            // font-size
    //            harmony.setFontSize("" + chordName.getText().getExportedFontSize());
    //
    //            // relative-x
    //            harmony.setRelativeX(
    //                    toTenths(chordName.getReferencePoint().x - current.note.getCenterLeft().x));
    //
    //            // Placement
    //            harmony.setPlacement(
    //                    (chordName.getReferencePoint().y < current.note.getCenter().y) ? AboveBelow.ABOVE
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
    //            logger.warn("Error visiting " + chordName, ex);
    //        }
    //    }
    //
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
    //            Dynamics pmDynamics = factory.createDynamics();
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
    //    //----------------//
    //    // processFermata //
    //    //----------------//
    //    private void processFermata (OldFermata fermata)
    //    {
    //        try {
    //            logger.debug("Visiting {}", fermata);
    //
    //            Fermata pmFermata = factory.createFermata();
    //
    //            // default-y (of the fermata dot)
    //            // For upright we use bottom of the box, for inverted the top of the box
    //            Rectangle box = fermata.getBox();
    //            Point dot;
    //
    //            if (fermata.getShape() == Shape.FERMATA_BELOW) {
    //                dot = new Point(box.x + (box.width / 2), box.y);
    //            } else {
    //                dot = new Point(box.x + (box.width / 2), box.y + box.height);
    //            }
    //
    //            pmFermata.setDefaultY(yOf(dot, current.note.getStaff()));
    //
    //            // Type
    //            pmFermata.setType(
    //                    (fermata.getShape() == Shape.FERMATA) ? UprightInverted.UPRIGHT
    //                            : UprightInverted.INVERTED);
    //            // Everything is now OK
    //            getNotations().getTiedOrSlurOrTuplet().add(pmFermata);
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + fermata, ex);
    //        }
    //    }
    //
    //------------//
    // processKey //
    //------------//
    /**
     * Process a key signature, either for just one staff or globally for the whole part.
     *
     * @param keySignature the key event
     * @param global       true for whole part, false for staff indication
     */
    private void processKey (KeyInter keySignature,
                             boolean global)
    {
        try {
            logger.debug("Visiting {}", keySignature);

            final Key key = factory.createKey();
            key.setFifths(new BigInteger("" + keySignature.getFifths()));

            if (global) {
                // Is this new?
                final int staffCount = current.measure.getPart().getStaves().size();
                boolean isNew = false;

                for (int index = 0; index < staffCount; index++) {
                    Key currentKey = current.keys.get(index);

                    if ((currentKey == null) || !areEqual(currentKey, key)) {
                        isNew = true;

                        break;
                    }
                }

                if (isNew) {
                    getAttributes().getKey().add(key);

                    for (int index = 0; index < staffCount; index++) {
                        current.keys.put(index, key);
                    }
                }
            } else {
                final int staffIndex = keySignature.getStaff().getIndexInPart();
                final Key currentKey = current.keys.get(staffIndex);

                if ((currentKey == null) || !areEqual(currentKey, key)) {
                    key.setNumber(new BigInteger("" + (1 + staffIndex)));
                    getAttributes().getKey().add(key);
                    current.keys.put(staffIndex, key);
                }
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + keySignature, ex);
        }
    }

    //-------------//
    // processKeys //
    //-------------//
    /**
     * Process the potential key signatures of the current measure.
     * We may have no key at all, or different keys from one staff to the other.
     * If all keys are the same, only one info is written.
     */
    private void processKeys ()
    {
        // Something to process?
        if (!current.measure.hasKeys()) {
            return;
        }

        // Check if all keys are the same across all staves in measure
        if (current.measure.hasSameKeys()) {
            processKey(current.measure.getKey(0), true);
        } else {
            // Work staff by staff
            final int staffCount = current.measure.getPart().getStaves().size();

            for (int index = 0; index < staffCount; index++) {
                KeyInter key = current.measure.getKey(index);
                processKey(key, false);
            }
        }
    }

    //--------------------//
    // processLogicalPart //
    //--------------------//
    private void processLogicalPart (LogicalPart logicalPart,
                                     ScorePartwise.Part pmPart)
    {
        logger.debug("Processing {} for {}", logicalPart, current.page.getSheet());

        current.logicalPart = logicalPart;
        current.pmPart = pmPart;
        current.keys.clear();

        // Delegate to children the filling of measures
        logger.debug("Populating {}", logicalPart);
        isFirst.system = true;

        // Reset slur numbers
        slurNumbers.clear();

        // Process all systems in page
        for (SystemInfo system : current.page.getSystems()) {
            processSystem(system);
        }
    }

    //----------------//
    // processMeasure //
    //----------------//
    private void processMeasure (Measure measure)
    {
        try {
            logger.debug("Processing {}", measure);
            current.pmMeasure = null;

            // Very first measure in score?
            final boolean isPageFirstMeasure = isFirst.system && isFirst.measure;
            final boolean isScoreFirstMeasure = isFirst.page && isPageFirstMeasure;
            final MeasureStack stack = measure.getStack();

            logger.debug("{} : {}", measure, isFirst);

            current.measure = measure;
            tupletNumbers.clear();

            // Allocate proxymusic Measure
            current.pmMeasure = factory.createScorePartwisePartMeasure();
            current.pmPart.getMeasure().add(current.pmMeasure);
            current.pmMeasure.setNumber(stack.getScoreId(score));

            if (!measure.isDummy()) {
                current.pmMeasure.setWidth(toTenths(measure.getWidth()));
            }

            if (stack.isImplicit()) {
                current.pmMeasure.setImplicit(YesNo.YES);
            }

            // Print?
            new MeasurePrint(measure).process();

            //
            //            // Inside barline?
            //            process(measure.getInsideBarline());
            //
            // Left barline ?
            final Measure prevMeasure = measure.getPreviousSibling();

            if ((prevMeasure != null) && !prevMeasure.isDummy()) {
                processBarline(prevMeasure.getRightBarline());
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

            // Insert KeySignature(s), if any (they may vary between staves)
            processKeys();

            // Insert TimeSignature, if any
            if (measure.getTimeSignature() != null) {
                processTime(measure.getTimeSignature());
            } else if (isScoreFirstMeasure) {
                // We need to insert a time sig!
                ///processTime(2,4,null);
            }

            // Clefs may be inserted further down the measure
            final ClefIterators clefIters = new ClefIterators(measure);

            // Insert clefs that occur before the first time slot
            final List<Slot> slots = stack.getSlots();

            if (slots.isEmpty()) {
                clefIters.push(null, null);
            } else {
                clefIters.push(slots.get(0).getXOffset(), null);
            }

            // Now voice per voice
            Rational timeCounter = Rational.ZERO;

            for (Voice voice : measure.getVoices()) {
                current.voice = voice;

                // Need a backup ?
                if (!timeCounter.equals(Rational.ZERO)) {
                    insertBackup(timeCounter);
                    timeCounter = Rational.ZERO;
                }

                if (voice.isWhole()) {
                    // Delegate to the chord children directly
                    AbstractChordInter chord = voice.getWholeChord();
                    clefIters.push(measure.getWidth(), chord.getTopStaff());
                    processChord(chord);
                    timeCounter = stack.getActualDuration();
                } else {
                    for (Slot slot : stack.getSlots()) {
                        Voice.SlotVoice info = voice.getSlotInfo(slot);

                        if ((info != null)
                            && // Skip free slots
                                (info.status == Voice.Status.BEGIN)) {
                            AbstractChordInter chord = info.chord;
                            clefIters.push(slot.getXOffset(), chord.getTopStaff());

                            // Need a forward before this chord ?
                            Rational startTime = chord.getStartTime();

                            if (timeCounter.compareTo(startTime) < 0) {
                                insertForward(startTime.minus(timeCounter), chord);
                                timeCounter = startTime;
                            }

                            // Grace note before this chord?
                            if (chord instanceof HeadChordInter) {
                                SmallChordInter small = getGrace((HeadChordInter) chord);

                                if (small != null) {
                                    processChord(small);
                                }
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

                current.endVoice();
            }

            // Clefs that occur after time slots, if any
            clefIters.push(null, null);

            // Right Barline
            if (!measure.isDummy()) {
                processBarline(measure.getRightBarline());
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + measure + " in " + current.page, ex);

            if (current.pmMeasure != null) {
                current.pmPart.getMeasure().remove(current.pmMeasure);
            }
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

            final SIGraph sig = note.getSig();
            current.note = note;

            final AbstractChordInter chord = note.getChord();
            final boolean isFirstInChord = chord.getNotes().indexOf(note) == 0;

            // For first note in chord
            if (!current.measure.isDummy()) {
                if (isFirstInChord) {
                    // Chord direction events (statement, pedal, TODO: others?)
                    for (Relation rel : sig.edgesOf(chord)) {
                        if (rel instanceof ChordSentenceRelation) {
                            processDirection((SentenceInter) sig.getOppositeInter(chord, rel));
                        } else if (rel instanceof ChordPedalRelation) {
                            processPedal((PedalInter) sig.getOppositeInter(chord, rel));
                        } else if (rel instanceof ChordWedgeRelation) {
                            HorizontalSide side = ((ChordWedgeRelation) rel).getSide();
                            processWedge((WedgeInter) sig.getOppositeInter(chord, rel), side);
                        }
                    }

                    //
                    //                // Chord symbol, if any
                    //                if (chord.getChordSymbol() != null) {
                    //                    ///chord.getChordSymbol().accept(this);
                    //                    process(chord.getChordSymbol());
                    //                }
                }
            }

            current.pmNote = factory.createNote();

            Staff staff = note.getStaff();

            // Chord notation events for first note in chord
            if (isFirstInChord) {
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
                RestChordInter restChord = (RestChordInter) chord;

                // Rest for the whole measure?
                if (current.measure.isDummy() || current.measure.isMeasureRest(restChord)) {
                    rest.setMeasure(YesNo.YES);
                    isMeasureRest = true;
                }

                if (!current.measure.isDummy() && !isMeasureRest) {
                    // Set displayStep & displayOctave for rest
                    rest.setDisplayStep(stepOf(note.getStep()));
                    rest.setDisplayOctave(note.getOctave());
                }

                current.pmNote.setRest(rest);
            } else {
                // Grace?
                if (isFirstInChord && note.getShape().isSmall()) {
                    Grace grace = factory.createGrace();
                    current.pmNote.setGrace(grace);

                    // Slash? (check the flag)
                    StemInter stem = chord.getStem();

                    if (stem != null) {
                        for (Relation rel : sig.getRelations(stem, FlagStemRelation.class)) {
                            if (Shape.SMALL_FLAG_SLASH == sig.getOppositeInter(stem, rel).getShape()) {
                                grace.setSlash(YesNo.YES);

                                break;
                            }
                        }
                    }
                }

                // Pitch
                Pitch pitch = factory.createPitch();
                pitch.setStep(stepOf(note.getStep()));
                pitch.setOctave(note.getOctave());

                // Alter?
                AbstractHeadInter head = (AbstractHeadInter) note;
                Key key = current.keys.get(staff.getIndexInPart());
                Integer fifths = (key != null) ? key.getFifths().intValue() : null;
                int alter = head.getAlter(fifths);

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

                TupletInter tuplet = chord.getTuplet();
                Rational chordDur = chord.getDurationSansDotOrTuplet();

                if (!chordDur.equals(tuplet.getBaseDuration())) {
                    timeModification.setNormalType(getNoteTypeName(tuplet.getBaseDuration()));
                }

                current.pmNote.setTimeModification(timeModification);

                // Tuplet start/stop?
                if (isFirstInChord) {
                    List<AbstractChordInter> embraced = tuplet.getChords();

                    if ((embraced.get(0) == chord) || (embraced.get(embraced.size() - 1) == chord)) {
                        processTuplet(tuplet);
                    }
                }
            }

            // Duration (not for grace note)
            if (current.pmNote.getGrace() == null) {
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
            }

            // Voice
            current.pmNote.setVoice("" + chord.getVoice().getId());

            // Type
            if (!current.measure.isDummy()) {
                if (!isMeasureRest) {
                    NoteType noteType = factory.createNoteType();
                    noteType.setValue(getNoteTypeName(note));
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
                    Beam pmBeam = factory.createBeam();
                    pmBeam.setNumber(1 + chord.getBeams().indexOf(beam));

                    if (beam.isHook()) {
                        if (beam.getCenter().x > chord.getStem().getCenter().x) {
                            pmBeam.setValue(BeamValue.FORWARD_HOOK);
                        } else {
                            pmBeam.setValue(BeamValue.BACKWARD_HOOK);
                        }
                    } else {
                        List<AbstractChordInter> chords = beam.getChords();

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

            if (!current.measure.isDummy()) {
                // Ties / Slurs
                for (Relation rel : sig.getRelations(note, SlurHeadRelation.class)) {
                    processSlur((SlurInter) sig.getOppositeInter(note, rel));
                }

                // Lyrics ?
                for (Relation rel : sig.getRelations(chord, ChordSyllableRelation.class)) {
                    processSyllable((LyricItemInter) sig.getOppositeInter(chord, rel));
                }
            }

            // Everything is OK
            current.pmMeasure.getNoteOrBackupOrForward().add(current.pmNote);
        } catch (Exception ex) {
            logger.warn("Error visiting " + note, ex);
        }

        // Safer...
        current.endNote();
    }

    //-------------//
    // processPart //
    //-------------//
    private void processPart (Part part)
    {
        try {
            logger.debug("Processing {}", part);

            // Delegate to measures
            for (Measure measure : part.getMeasures()) {
                if (!measure.getStack().isCautionary()) {
                    processMeasure(measure);
                }
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + part, ex);
        }
    }

    //-----------------//
    // processPartList //
    //-----------------//
    private void processPartList ()
    {
        logger.debug("Processing PartList for {}", score);

        PartList partList = factory.createPartList();
        scorePartwise.setPartList(partList);

        // Allocate & initialize a ScorePart instance for each logical part
        Map<LogicalPart, ScorePartwise.Part> partMap = new LinkedHashMap<LogicalPart, ScorePartwise.Part>();

        for (LogicalPart p : score.getLogicalParts()) {
            ScorePartwise.Part pmPart = createScorePart(p);
            partMap.put(p, pmPart);
            partList.getPartGroupOrScorePart().add(pmPart.getId());
        }

        // Then, stub by stub, populate all ScorePartwise.Part instances in parallel
        for (SheetStub stub : score.getStubs()) {
            processStub(stub, partMap);

            // Lean management of sheet instances ...
            if ((OMR.getGui() == null) || (StubsController.getCurrentStub() != stub)) {
                stub.swapSheet();
            }
        }
    }

    //--------------//
    // processPedal //
    //--------------//
    private void processPedal (PedalInter pedal)
    {
        try {
            logger.debug("Visiting {}", pedal);

            Direction direction = new Direction();
            DirectionType directionType = new DirectionType();
            Pedal pmPedal = new Pedal();

            // No line (for the time being)
            pmPedal.setLine(YesNo.NO);

            // Sound
            Sound sound = factory.createSound();
            direction.setSound(sound);

            // Start / Stop type
            if (pedal.getShape() == Shape.PEDAL_MARK) {
                pmPedal.setType(StartStopChangeContinue.START);
                sound.setDamperPedal("yes");
            } else {
                pmPedal.setType(StartStopChangeContinue.STOP);
                sound.setDamperPedal("no");
            }

            // Staff ?
            Staff staff = current.note.getStaff();
            insertStaffId(direction, staff);

            // Reference point (bottom left)
            Rectangle bounds = pedal.getBounds();
            Point refPoint = new Point(bounds.x, bounds.y + bounds.height);
            pmPedal.setHalign(LeftCenterRight.LEFT);

            // default-x
            pmPedal.setDefaultX(toTenths(refPoint.x - current.measure.getAbscissa(LEFT, staff)));

            // default-y
            pmPedal.setDefaultY(yOf(refPoint, staff));

            // Placement
            direction.setPlacement(
                    (refPoint.y < current.note.getCenter().y) ? AboveBelow.ABOVE : AboveBelow.BELOW);

            // Everything is OK
            directionType.setPedal(pmPedal);
            direction.getDirectionType().add(directionType);
            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
        } catch (Exception ex) {
            logger.warn("Error visiting " + pedal, ex);
        }
    }

    //--------------//
    // processScore //
    //--------------//
    /**
     * Allocate/populate everything that relates to the score instance and its children.
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
                identification.setSource(book.getInputPath().toString());

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
                    current.scale = firstPage.getSheet().getScale();
                }

                if (current.scale != null) {
                    Scaling scaling = factory.createScaling();
                    defaults.setScaling(scaling);
                    // Assuming 300 DPI
                    scaling.setMillimeters(
                            new BigDecimal(
                                    String.format("%.4f", (current.scale.getInterline() * 25.4 * 4) / 300)));
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
                Font lyricFont = omr.ui.symbol.TextFont.baseTextFont;
                LyricFont pmLyricFont = factory.createLyricFont();
                pmLyricFont.setFontFamily(lyricFont.getName());
                pmLyricFont.setFontSize("" + omr.ui.symbol.TextFont.baseTextFont.getSize());

                if (lyricFont.isItalic()) {
                    pmLyricFont.setFontStyle(FontStyle.ITALIC);
                }

                defaults.getLyricFont().add(pmLyricFont);
                scorePartwise.setDefaults(defaults);
            }

            {
                // Score source
                source = new Source();

                final Book book = score.getBook();
                source.setFile(book.getInputPath().toString());
                source.setOffset((book.getOffset() != null) ? book.getOffset() : 0);
                source.encodeScore(scorePartwise);
            }

            // PartList & sequence of parts (if not done yet)
            //TODO: this has nothing to do here!
            if (score.getLogicalParts() == null) {
                // Merge the pages (connecting the parts across pages)
                new ScoreReduction(score).reduce();
            }

            if (score.getLogicalParts() != null) {
                processPartList();
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + score + " " + ex, ex);
        }
    }

    //- All Visiting Methods -----------------------------------------------------------------------
    //    //--------------------//
    //    // process Arpeggiate //
    //    //--------------------//
    //    private void process (OldArpeggiate arpeggiate)
    //    {
    //        try {
    //            logger.debug("Visiting {}", arpeggiate);
    //
    //            Arpeggiate pmArpeggiate = factory.createArpeggiate();
    //
    //            // relative-x
    //            pmArpeggiate.setRelativeX(
    //                    toTenths(arpeggiate.getReferencePoint().x - current.note.getCenterLeft().x));
    //
    //            // number ???
    //            // TODO
    //            //
    //            getNotations().getTiedOrSlurOrTuplet().add(pmArpeggiate);
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + arpeggiate, ex);
    //        }
    //    }
    //
    //    //----------------------//
    //    // process Articulation //
    //    //----------------------//
    //    private void process (OldArticulation articulation)
    //    {
    //        try {
    //            logger.debug("Visiting {}", articulation);
    //
    //            JAXBElement<?> element = getArticulationObject(articulation.getShape());
    //
    //            // Staff ?
    //            Staff staff = current.note.getStaff();
    //
    //            // Placement
    //            Class<?> classe = element.getDeclaredType();
    //
    //            Method method = classe.getMethod("setPlacement", AboveBelow.class);
    //            method.invoke(
    //                    element.getValue(),
    //                    (articulation.getReferencePoint().y < current.note.getCenter().y)
    //                            ? AboveBelow.ABOVE : AboveBelow.BELOW);
    //
    //            // Default-Y
    //            method = classe.getMethod("setDefaultY", BigDecimal.class);
    //            method.invoke(element.getValue(), yOf(articulation.getReferencePoint(), staff));
    //
    //            // Include in Articulations
    //            getArticulations().getAccentOrStrongAccentOrStaccato().add(element);
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + articulation, ex);
    //        }
    //    }
    //
    //    //------------------//
    //    // process Ornament //
    //    //------------------//
    //    @SuppressWarnings("unchecked")
    //    private void process (OldOrnament ornament)
    //    {
    //        try {
    //            logger.debug("Visiting {}", ornament);
    //
    //            JAXBElement<?> element = getOrnamentObject(ornament.getShape());
    //
    //            // Placement?
    //            Class<?> classe = element.getDeclaredType();
    //            Method method = classe.getMethod("setPlacement", AboveBelow.class);
    //            method.invoke(
    //                    element.getValue(),
    //                    (ornament.getReferencePoint().y < current.note.getCenter().y) ? AboveBelow.ABOVE
    //                            : AboveBelow.BELOW);
    //            // Everything is OK
    //            // Include in ornaments
    //            getOrnaments().getTrillMarkOrTurnOrDelayedTurn().add(element);
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + ornament, ex);
    //        }
    //    }
    //
    //    //---------------//
    //    // process Segno //
    //    //---------------//
    //    private void process (OldSegno segno)
    //    {
    //        try {
    //            logger.debug("Visiting {}", segno);
    //
    //            Direction direction = new Direction();
    //            DirectionType directionType = factory.createDirectionType();
    //
    //            EmptyPrintStyleAlign empty = factory.createEmptyPrintStyleAlign();
    //
    //            // Staff ?
    //            Staff staff = current.note.getStaff();
    //            insertStaffId(direction, staff);
    //
    //            // default-x
    //            empty.setDefaultX(
    //                    toTenths(segno.getReferencePoint().x - current.measure.getAbscissa(LEFT, staff)));
    //
    //            // default-y
    //            empty.setDefaultY(yOf(segno.getReferencePoint(), staff));
    //
    //            // Need also a Sound element (TODO: We don't do anything with sound!)
    //            Sound sound = factory.createSound();
    //            sound.setSegno("" + current.measure.getStack().getScoreId(score));
    //            sound.setDivisions(
    //                    new BigDecimal(
    //                            current.page.simpleDurationOf(omr.score.entity.OldNote.QUARTER_DURATION)));
    //
    //            // Everything is OK
    //            directionType.getSegno().add(empty);
    //            direction.getDirectionType().add(directionType);
    //            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
    //        } catch (Exception ex) {
    //            logger.warn("Error visiting " + segno, ex);
    //        }
    //    }
    //-----------------//
    // processSentence //
    //-----------------//
    private void processSentence (SentenceInter sentence)
    {
        try {
            logger.debug("Visiting {}", sentence);

            final TextRole role = sentence.getRole();
            TypedText typedText = null;

            switch (role) {
            case Title:
                getWork().setWorkTitle(sentence.getValue());

                break;

            case Number:
                getWork().setWorkNumber(sentence.getValue());

                break;

            case Rights: {
                typedText = factory.createTypedText();
                typedText.setValue(sentence.getValue());
                scorePartwise.getIdentification().getRights().add(typedText);
            }

            break;

            case CreatorArranger:
            case CreatorComposer:
            case CreatorLyricist:
            case Creator: {
                typedText = factory.createTypedText();
                typedText.setValue(sentence.getValue());

                // Additional type information?
                if (role != null) {
                    switch (role) {
                    case CreatorArranger:
                        typedText.setType("arranger");

                        break;

                    case CreatorComposer:
                        typedText.setType("composer");

                        break;

                    case CreatorLyricist:
                        typedText.setType("lyricist");

                        break;

                    default:
                        break;
                    }
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
            pmCredit.setPage(new BigInteger("" + (1 + score.getPageIndex(current.page))));

            if (typedText != null) {
                pmCredit.getCreditTypeOrLinkOrBookmark().add(typedText.getType());
            }

            FormattedText creditWords = factory.createFormattedText();
            creditWords.setValue(sentence.getValue());

            // Font information
            setFontInfo(creditWords, sentence);

            // Position is wrt page
            Point pt = sentence.getLocation();
            creditWords.setDefaultX(toTenths(pt.x));
            creditWords.setDefaultY(toTenths(current.page.getDimension().height - pt.y));

            pmCredit.getCreditTypeOrLinkOrBookmark().add(creditWords);
            scorePartwise.getCredit().add(pmCredit);
        } catch (Exception ex) {
            logger.warn("Error visiting " + sentence, ex);
        }
    }

    //-------------//
    // processSlur //
    //-------------//
    /**
     * Export slur data.
     * <p>
     * This method is called from each linked note head (typically two for left and right sides, but
     * only one for an orphan slur).
     * So, if the slur is orphan, this method must also export the continuing end point.
     * This does not apply for ties, since Bézier info is not needed for ties.
     *
     * @param slur slur to export (tie or standard slur).
     */
    private void processSlur (SlurInter slur)
    {
        try {
            logger.debug("Visiting {}", slur);

            // Note contextual data
            final boolean isStart = slur.getHead(LEFT) == current.note;

            if (slur.isTie()) {
                // Tie element
                Tie tie = factory.createTie();
                tie.setType(isStart ? StartStop.START : StartStop.STOP);
                current.pmNote.getTie().add(tie);

                // Tied element (no number needed, no bezier info, no continuation point)
                Tied tied = factory.createTied();

                // Tied type
                tied.setType(isStart ? StartStopContinue.START : StartStopContinue.STOP);

                // Tied orientation
                if (isStart) {
                    tied.setOrientation(slur.isAbove() ? OverUnder.OVER : OverUnder.UNDER);
                }

                getNotations().getTiedOrSlurOrTuplet().add(tied);
            } else {
                // Number attribute
                Integer num = getSlurNumber(slur);

                if (isStart) {
                    addSlur(slur, num, LEFT, false);

                    if (slur.getHead(RIGHT) == null) {
                        addSlur(slur, num, RIGHT, true);
                    }
                } else {
                    if (slur.getHead(LEFT) == null) {
                        addSlur(slur, num, LEFT, true);
                    }

                    addSlur(slur, num, RIGHT, false);
                }
            }
        } catch (Exception ex) {
            logger.warn("Error visiting " + slur, ex);
        }
    }

    //-------------//
    // processStub //
    //-------------//
    /**
     * Process the sheet stub at hand, by appending part material for each part
     *
     * @param stub    the stub to process
     * @param partMap the map of parts to populate
     */
    private void processStub (SheetStub stub,
                              Map<LogicalPart, ScorePartwise.Part> partMap)
    {
        logger.debug("Processing {}", stub);

        final Integer localPageId = score.getSheetPageId(stub.getNumber());

        // This should never occur if processStub() is called only on score relevant stubs
        if (localPageId == null) {
            return;
        }

        final Sheet sheet = stub.getSheet();
        final Page page = sheet.getPages().get(localPageId - 1);

        source.encodePage(page, scorePartwise);

        current.page = page;
        current.scale = page.getSheet().getScale();
        page.resetDurationDivisor();

        isFirst.page = score.isFirst(page);
        isFirst.system = true;
        isFirst.measure = true;
        isFirst.part = true;

        for (Entry<LogicalPart, ScorePartwise.Part> entry : partMap.entrySet()) {
            processLogicalPart(entry.getKey(), entry.getValue());
            isFirst.part = false;
        }
    }

    //-----------------//
    // processSyllable //
    //-----------------//
    private void processSyllable (LyricItemInter syllable)
    {
        if (syllable.getValue() != null) {
            Lyric pmLyric = factory.createLyric();
            pmLyric.setDefaultY(yOf(syllable.getLocation(), syllable.getStaff()));
            pmLyric.setNumber("" + syllable.getLyricLine().getNumber());

            TextElementData pmText = factory.createTextElementData();
            pmText.setValue(syllable.getValue());
            pmLyric.getElisionAndSyllabicAndText().add(getSyllabic(syllable.getSyllabicType()));
            pmLyric.getElisionAndSyllabicAndText().add(pmText);

            current.pmNote.getLyric().add(pmLyric);
        }
    }

    //---------------//
    // processSystem //
    //---------------//
    /**
     * Allocate/populate everything that directly relates to this system in the current
     * logicalPart.
     * The rest of processing is directly delegated to the parts
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

            // Sentences in system
            if (isFirst.part) {
                for (Inter inter : system.getSig().inters(SentenceInter.class)) {
                    processSentence((SentenceInter) inter);
                }
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

    //---------------//
    // processTuplet //
    //---------------//
    private void processTuplet (TupletInter tuplet)
    {
        try {
            logger.debug("Visiting {}", tuplet);

            Tuplet pmTuplet = factory.createTuplet();

            // Brackets
            if (constants.avoidTupletBrackets.isSet()) {
                pmTuplet.setBracket(YesNo.NO);
            }

            // Placement (for start only)
            if (tuplet.getChords().get(0) == current.note.getChord()) {
                pmTuplet.setPlacement(
                        (tuplet.getCenter().y <= current.note.getCenter().y) ? AboveBelow.ABOVE
                                : AboveBelow.BELOW);
            }

            // Type
            pmTuplet.setType(
                    (tuplet.getChords().get(0) == current.note.getChord()) ? StartStop.START
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

            getNotations().getTiedOrSlurOrTuplet().add(pmTuplet);
        } catch (Exception ex) {
            logger.warn("Error visiting " + tuplet, ex);
        }
    }

    //--------------//
    // processWedge //
    //--------------//
    private void processWedge (WedgeInter wedge,
                               HorizontalSide side)
    {
        try {
            logger.debug("Visiting {}", wedge);

            Direction direction = factory.createDirection();
            DirectionType directionType = factory.createDirectionType();
            Wedge pmWedge = factory.createWedge();

            // Spread
            pmWedge.setSpread(toTenths(wedge.getSpread(side)));

            // Staff ?
            Staff staff = current.note.getStaff();
            insertStaffId(direction, staff);

            // Start or stop ?
            final Point2D refPoint;

            if (side == LEFT) {
                Point2D top = wedge.getLine1().getP1();
                Point2D bot = wedge.getLine2().getP1();
                refPoint = new Point2D.Double(
                        (top.getX() + bot.getX()) / 2.0,
                        (top.getY() + bot.getY()) / 2.0);

                // Type
                Shape shape = wedge.getShape();
                pmWedge.setType(
                        (shape == Shape.CRESCENDO) ? WedgeType.CRESCENDO : WedgeType.DIMINUENDO);

                // Placement
                direction.setPlacement(
                        (refPoint.getY() < current.note.getCenter().y) ? AboveBelow.ABOVE
                                : AboveBelow.BELOW);

                // default-y
                pmWedge.setDefaultY(yOf(refPoint, staff));
            } else { // It's a stop
                pmWedge.setType(WedgeType.STOP);

                Point2D top = wedge.getLine1().getP2();
                Point2D bot = wedge.getLine2().getP2();
                refPoint = new Point2D.Double(
                        (top.getX() + bot.getX()) / 2.0,
                        (top.getY() + bot.getY()) / 2.0);
            }

            // default-x using note left side (No offset for the time being)
            pmWedge.setDefaultX(toTenths(refPoint.getX() - current.note.getCenterLeft().x));

            // Everything is OK
            directionType.setWedge(pmWedge);
            direction.getDirectionType().add(directionType);
            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
        } catch (Exception ex) {
            logger.warn("Error visiting " + wedge, ex);
        }
    }

    //- Utilities ----------------------------------------------------------------------------------
    //
    //-------------//
    // setFontInfo //
    //-------------//
    private void setFontInfo (FormattedText formattedText,
                              SentenceInter sentence)
    {
        FontInfo fontInfo = sentence.getMeanFont();
        formattedText.setFontSize("" + sentence.getExportedFontSize());

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
     * Report the musicXML staff-based Y value of a Point.
     * This method is safer than the other one which simply accepts a (de-typed) double ordinate.
     *
     * @param point the pixel point
     * @param staff the related staff
     * @return the upward-oriented ordinate WRT staff top line (in tenths)
     */
    private BigDecimal yOf (Point2D point,
                            Staff staff)
    {
        double staffTopY = staff.getFirstLine().yAt(point.getX());

        return toTenths(staffTopY - point.getY());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer pageHorizontalMargin = new Constant.Integer(
                "tenths",
                80,
                "Page horizontal margin");

        private final Constant.Integer pageVerticalMargin = new Constant.Integer(
                "tenths",
                80,
                "Page vertical margin");

        private final Constant.Boolean avoidTupletBrackets = new Constant.Boolean(
                false,
                "Should we avoid brackets for all tuplets");
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
                Collections.sort(list, Inter.byCenterAbscissa); // not needed? (already sorted)
                iters.put(entry.getKey(), list.listIterator());
            }
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Push as far as possible the relevant clefs iterators, according to the
         * current abscissa offset.
         *
         * @param xOffset       the abscissa offset of chord to be exported, if any
         * @param specificStaff a specific staff, or null for all staves within current part
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

    //---------//
    // Current //
    //---------//
    /** Keep references of all current entities. */
    private static class Current
    {
        //~ Instance fields ------------------------------------------------------------------------

        // Score dependent
        Work pmWork;

        // Part dependent
        LogicalPart logicalPart;

        ScorePartwise.Part pmPart;

        // Page dependent
        Page page;

        Scale scale;

        // System dependent
        SystemInfo system;

        // Measure dependent
        Measure measure;

        ScorePartwise.Part.Measure pmMeasure;

        final TreeMap<Integer, Key> keys = new TreeMap<Integer, Key>();

        Voice voice;

        Attributes pmAttributes;

        // Note dependent
        AbstractNoteInter note;

        Note pmNote;

        Notations pmNotations;

        //~ Methods --------------------------------------------------------------------------------
        // Cleanup at end of measure
        void endMeasure ()
        {
            measure = null;
            pmMeasure = null;
            voice = null;
            pmAttributes = null;

            endVoice();
        }

        // Cleanup at end of note
        void endNote ()
        {
            note = null;
            pmNote = null;
            pmNotations = null;
        }

        // Cleanup at end of voice
        void endVoice ()
        {
            voice = null;
            pmAttributes = null;

            endNote();
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

        private final Print pmPrint;

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

        private Print getPrint ()
        {
            used = true;

            return pmPrint;
        }

        private void populatePrint ()
        {
            // New system?
            if (isFirst.measure && !isFirst.system) {
                getPrint().setNewSystem(YesNo.YES);
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
                MeasureNumbering pmNumbering = factory.createMeasureNumbering();

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
