//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  P a r t w i s e B u i l d e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.score;

import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.CODA;
import static org.audiveris.omr.glyph.Shape.SEGNO;
import org.audiveris.omr.math.Rational;
import static org.audiveris.omr.score.MusicXML.*;
import org.audiveris.omr.sheet.Book;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.PartBarline;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.beam.BeamGroup;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Slot;
import org.audiveris.omr.sheet.rhythm.SlotVoice;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import static org.audiveris.omr.sig.inter.AbstractNoteInter.QUARTER_DURATION;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.ArticulationInter;
import org.audiveris.omr.sig.inter.ChordNameInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.DynamicsInter;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.FermataInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.MarkerInter;
import org.audiveris.omr.sig.inter.OrnamentInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.RestChordInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallChordInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.relation.ChordArpeggiatoRelation;
import org.audiveris.omr.sig.relation.ChordArticulationRelation;
import org.audiveris.omr.sig.relation.ChordDynamicsRelation;
import org.audiveris.omr.sig.relation.ChordNameRelation;
import org.audiveris.omr.sig.relation.ChordOrnamentRelation;
import org.audiveris.omr.sig.relation.ChordPedalRelation;
import org.audiveris.omr.sig.relation.ChordSentenceRelation;
import org.audiveris.omr.sig.relation.ChordSyllableRelation;
import org.audiveris.omr.sig.relation.ChordWedgeRelation;
import org.audiveris.omr.sig.relation.FermataChordRelation;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.MarkerBarRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.text.TextRole;
import static org.audiveris.omr.text.TextRole.*;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.OmrExecutors;
import static org.audiveris.omr.util.VerticalSide.*;
import org.audiveris.proxymusic.AboveBelow;
import org.audiveris.proxymusic.Accidental;
import org.audiveris.proxymusic.Arpeggiate;
import org.audiveris.proxymusic.Articulations;
import org.audiveris.proxymusic.Attributes;
import org.audiveris.proxymusic.Backup;
import org.audiveris.proxymusic.BackwardForward;
import org.audiveris.proxymusic.BarStyleColor;
import org.audiveris.proxymusic.Barline;
import org.audiveris.proxymusic.Beam;
import org.audiveris.proxymusic.BeamValue;
import org.audiveris.proxymusic.Clef;
import org.audiveris.proxymusic.ClefSign;
import org.audiveris.proxymusic.Credit;
import org.audiveris.proxymusic.Defaults;
import org.audiveris.proxymusic.Direction;
import org.audiveris.proxymusic.DirectionType;
import org.audiveris.proxymusic.Dynamics;
import org.audiveris.proxymusic.Empty;
import org.audiveris.proxymusic.EmptyPrintStyleAlign;
import org.audiveris.proxymusic.Encoding;
import org.audiveris.proxymusic.Ending;
import org.audiveris.proxymusic.Fermata;
import org.audiveris.proxymusic.FontStyle;
import org.audiveris.proxymusic.FontWeight;
import org.audiveris.proxymusic.FormattedText;
import org.audiveris.proxymusic.Forward;
import org.audiveris.proxymusic.Grace;
import org.audiveris.proxymusic.Identification;
import org.audiveris.proxymusic.Key;
import org.audiveris.proxymusic.LeftCenterRight;
import org.audiveris.proxymusic.Lyric;
import org.audiveris.proxymusic.LyricFont;
import org.audiveris.proxymusic.MarginType;
import org.audiveris.proxymusic.MeasureNumbering;
import org.audiveris.proxymusic.MeasureNumberingValue;
import org.audiveris.proxymusic.MidiInstrument;
import org.audiveris.proxymusic.Notations;
import org.audiveris.proxymusic.Note;
import org.audiveris.proxymusic.NoteType;
import org.audiveris.proxymusic.Notehead;
import org.audiveris.proxymusic.NoteheadValue;
import org.audiveris.proxymusic.ObjectFactory;
import org.audiveris.proxymusic.Ornaments;
import org.audiveris.proxymusic.OverUnder;
import org.audiveris.proxymusic.PageLayout;
import org.audiveris.proxymusic.PageMargins;
import org.audiveris.proxymusic.PartList;
import org.audiveris.proxymusic.PartName;
import org.audiveris.proxymusic.Pedal;
import org.audiveris.proxymusic.Pitch;
import org.audiveris.proxymusic.Print;
import org.audiveris.proxymusic.Repeat;
import org.audiveris.proxymusic.Rest;
import org.audiveris.proxymusic.RightLeftMiddle;
import org.audiveris.proxymusic.Scaling;
import org.audiveris.proxymusic.ScoreInstrument;
import org.audiveris.proxymusic.ScorePart;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.Slur;
import org.audiveris.proxymusic.Sound;
import org.audiveris.proxymusic.StaffDetails;
import org.audiveris.proxymusic.StaffLayout;
import org.audiveris.proxymusic.StartStop;
import org.audiveris.proxymusic.StartStopChangeContinue;
import org.audiveris.proxymusic.StartStopContinue;
import org.audiveris.proxymusic.StartStopDiscontinue;
import org.audiveris.proxymusic.Stem;
import org.audiveris.proxymusic.StemValue;
import org.audiveris.proxymusic.Supports;
import org.audiveris.proxymusic.SystemLayout;
import org.audiveris.proxymusic.SystemMargins;
import org.audiveris.proxymusic.TextElementData;
import org.audiveris.proxymusic.Tie;
import org.audiveris.proxymusic.Tied;
import org.audiveris.proxymusic.Time;
import org.audiveris.proxymusic.TimeModification;
import org.audiveris.proxymusic.TimeSymbol;
import org.audiveris.proxymusic.Tuplet;
import org.audiveris.proxymusic.TypedText;
import org.audiveris.proxymusic.UprightInverted;
import org.audiveris.proxymusic.Wedge;
import org.audiveris.proxymusic.WedgeType;
import org.audiveris.proxymusic.Work;
import org.audiveris.proxymusic.YesNo;
import org.audiveris.proxymusic.util.Marshalling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.proxymusic.Bass;
import org.audiveris.proxymusic.BassAlter;
import org.audiveris.proxymusic.BassStep;
import org.audiveris.proxymusic.Degree;
import org.audiveris.proxymusic.DegreeAlter;
import org.audiveris.proxymusic.DegreeType;
import org.audiveris.proxymusic.DegreeValue;
import org.audiveris.proxymusic.Harmony;
import org.audiveris.proxymusic.Kind;
import org.audiveris.proxymusic.Root;
import org.audiveris.proxymusic.RootAlter;
import org.audiveris.proxymusic.RootStep;
import org.audiveris.proxymusic.Step;
import org.audiveris.proxymusic.Unpitched;

/**
 * Class {@code PartwiseBuilder} builds a ProxyMusic MusicXML {@link ScorePartwise}
 * from an Audiveris {@link Score} instance.
 *
 * @author Hervé Bitteur
 */
public class PartwiseBuilder
{

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

    /** Should be 6, but is 12 to cope with slurs not closed for lack of time slot. */
    private static final int MAX_SLUR_NUMBER = 12;

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
    private final Map<SlurInter, Integer> slurNumbers = new HashMap<>();

    /** Map of Tuplet numbers, reset for every Measure. */
    private final Map<TupletInter, Integer> tupletNumbers = new HashMap<>();

    /** Factory for ProxyMusic entities. */
    private final ObjectFactory factory = new ObjectFactory();

    /**
     * Create a new PartwiseBuilder object, on a related score instance.
     *
     * @param score the underlying score
     * @throws InterruptedException if the thread has been interrupted
     * @throws ExecutionException   if a checked exception was thrown
     */
    private PartwiseBuilder (Score score)
            throws InterruptedException,
                   ExecutionException
    {
        // Make sure the JAXB context is ready
        loading.get();

        this.score = score;
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

        Shape shape = clef.getShape();

        if (shape != Shape.PERCUSSION_CLEF) {
            /**
             * MusicXML specifications for clef 'line' element.
             * Line numbers are counted from the bottom of the staff.
             * Standard values are:
             * 2 for the G sign (treble clef),
             * 4 for the F sign (bass clef),
             * 3 for the C sign (alto clef) and
             * 5 for TAB (on a 6-line staff).
             *
             * We could add:
             * 4 for the C sign (tenor clef)
             */
            double p = clef.getPitch();
            pmClef.setLine(new BigInteger("" + (3 - (int) Math.rint(clef.getPitch() / 2.0))));
        }

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
                (logicalPart.getName() != null) ? logicalPart.getName()
                : logicalPart.getDefaultName());

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

    //------------------//
    // getBarlineOnLeft //
    //------------------//
    /**
     * Report the barline located on left side of the provided measure.
     *
     * @param measure the provided measure
     * @return the partBarline or null
     */
    private PartBarline getBarlineOnLeft (Measure measure)
    {
        if (measure.getLeftPartBarline() != null) {
            return measure.getLeftPartBarline();
        } else if (isFirst.measure) {
            return measure.getPart().getLeftPartBarline();
        } else {
            final Measure prevMeasure = measure.getPrecedingInSystem();

            if (prevMeasure != null) {
                if (!prevMeasure.isDummy()) {
                    return prevMeasure.getRightPartBarline();
                }
            }
        }

        return null;
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

        for (ListIterator<ScorePartwise.Part.Measure> it = measures.listIterator(
                measures.size()); it.hasPrevious();) {
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

    //---------------//
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
            for (int i = 1; i <= MAX_SLUR_NUMBER; i++) {
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
                logger.warn("Not able to insert backup {} at {} in {}",
                            delta, current.measure, current.page, ex);
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

            // Staff? (only if more than one staff in logicalPart)
            insertStaffId(forward, chord.getTopStaff());
        } catch (Exception ex) {
            if (current.page.getDurationDivisor() != null) {
                logger.warn("Not able to insert forward {} for {} at {} in {}",
                            delta, chord, current.measure, current.page, ex);
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
            } catch (IllegalAccessException |
                     IllegalArgumentException |
                     NoSuchMethodException |
                     SecurityException |
                     InvocationTargetException ex) {
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

        for (ListIterator<ScorePartwise.Part.Measure> mit = measures.listIterator(
                measures.size()); mit.hasPrevious();) {
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

    //- All processing Methods ---------------------------------------------------------------------
    //-------------------//
    // processArpeggiato //
    //-------------------//
    /**
     * Try to process an arpeggiato item.
     *
     * @param arpeggiate, perhaps null
     */
    private void processArpeggiato (ArpeggiatoInter arpeggiate)
    {
        if (arpeggiate == null) {
            return;
        }

        try {
            logger.debug("Visiting {}", arpeggiate);

            Arpeggiate pmArpeggiate = factory.createArpeggiate();

            // relative-x
            pmArpeggiate.setRelativeX(
                    toTenths(arpeggiate.getCenter().x - current.note.getCenterLeft().x));

            // number ???
            // TODO
            //
            getNotations().getTiedOrSlurOrTuplet().add(pmArpeggiate);
        } catch (Exception ex) {
            logger.warn("Error visiting {} in {}", arpeggiate, current.page, ex);
        }
    }

    //---------------------//
    // processArticulation //
    //---------------------//
    private void processArticulation (ArticulationInter articulation)
    {
        try {
            logger.debug("Visiting {}", articulation);

            JAXBElement<?> element = getArticulationObject(articulation.getShape());

            // Staff?
            Staff staff = current.note.getStaff();

            // Placement
            Class<?> classe = element.getDeclaredType();

            Method method = classe.getMethod("setPlacement", AboveBelow.class);
            method.invoke(
                    element.getValue(),
                    (articulation.getCenter().y < current.note.getCenter().y) ? AboveBelow.ABOVE
                    : AboveBelow.BELOW);

            // Default-Y
            method = classe.getMethod("setDefaultY", BigDecimal.class);
            method.invoke(element.getValue(), yOf(articulation.getCenter(), staff));

            // Include in Articulations
            getArticulations().getAccentOrStrongAccentOrStaccato().add(element);
        } catch (IllegalAccessException |
                 IllegalArgumentException |
                 NoSuchMethodException |
                 SecurityException |
                 InvocationTargetException ex) {
            logger.warn("Error visiting " + articulation, ex);
        }
    }

    //----------------//
    // processBarline //
    //----------------//
    /**
     * Process a part barline for the current measure.
     * <p>
     * This can be a left, mid or right barline WRT the current measure.
     * <p>
     * Related entities: repeat, ending, fermata, segno, coda.
     *
     * @param partBarline the PartBarline to process
     * @param location    barline location WRT current measure
     */
    private void processBarline (PartBarline partBarline,
                                 RightLeftMiddle location)
    {
        try {
            if (partBarline == null) {
                return;
            }

            final MeasureStack stack = current.measure.getStack();
            final PartBarline.Style style = partBarline.getStyle();
            final List<FermataInter> fermatas = partBarline.getFermatas(); // Top down list
            final EndingInter ending = partBarline.getEnding(
                    (location == RightLeftMiddle.RIGHT) ? RIGHT : LEFT);
            final String endingValue = (ending != null) ? ending.getValue() : null;
            String endingNumber = (ending != null) ? ending.getExportedNumber() : null;

            if (endingNumber == null) {
                endingNumber = "99"; // Dummy integer value to mean: unknown
            }

            // Is export of barline element really needed? MusicXML says that if we just have a
            // regular barline on right side of measure, and nothing else, answer is no.
            boolean needed = false;

            // Specific barline on left side:
            needed |= (partBarline == current.measure.getLeftPartBarline());
            // On left side, with stuff (left repeat, left ending):
            needed |= ((location == RightLeftMiddle.LEFT)
                               && (stack.isRepeat(LEFT) || (ending != null)));
            // Specific barline on middle location:
            needed |= (location == RightLeftMiddle.MIDDLE);
            // On right side, but with stuff (right repeat, right ending, fermata) or non regular:
            needed |= ((location == RightLeftMiddle.RIGHT)
                               && (stack.isRepeat(RIGHT)
                                           || (ending != null)
                                           || !fermatas.isEmpty()
                                           || (style != PartBarline.Style.REGULAR)));

            if (needed) {
                try {
                    logger.debug("Visiting {} on {}", partBarline, location);

                    final Barline pmBarline = factory.createBarline();
                    pmBarline.setLocation(
                            (location == RightLeftMiddle.RIGHT) ? RightLeftMiddle.RIGHT
                                    : RightLeftMiddle.LEFT);

                    BarStyleColor barStyleColor = factory.createBarStyleColor();
                    barStyleColor.setValue(barStyleOf(style, location));
                    pmBarline.setBarStyle(barStyleColor);

                    switch (location) {
                    case LEFT:
                    case MIDDLE:

                        // (Left) repeat?
                        if (stack.isRepeat(LEFT)) {
                            Repeat repeat = factory.createRepeat();
                            repeat.setDirection(BackwardForward.FORWARD);
                            pmBarline.setRepeat(repeat);
                        }

                        // (Left side of) Ending?
                        if (ending != null) {
                            Ending pmEnding = factory.createEnding();
                            Point2D pt = ending.getLine().getP1();

                            Staff staff = current.measure.getPart().getFirstStaff();
                            pmEnding.setDefaultY(yOf(pt, staff));

                            Line2D leg = ending.getLeftLeg();
                            pmEnding.setEndLength(toTenths(leg.getY2() - pt.getY()));

                            pmEnding.setType(StartStopDiscontinue.START);

                            // Number (mandatory)
                            pmEnding.setNumber(endingNumber);

                            // Value (optional)
                            if (endingValue != null) {
                                pmEnding.setValue(endingValue);
                            }

                            pmBarline.setEnding(pmEnding);
                        }

                        break;

                    case RIGHT:

                        // (Right) repeat?
                        if (stack.isRepeat(RIGHT)) {
                            Repeat repeat = factory.createRepeat();
                            repeat.setDirection(BackwardForward.BACKWARD);
                            pmBarline.setRepeat(repeat);
                        }

                        // Fermata(s)?
                        if (!fermatas.isEmpty()) {
                            // Pick up first upright fermata if any
                            for (FermataInter f : fermatas) {
                                if (f.getShape() == Shape.FERMATA) {
                                    processFermata(f, pmBarline);
                                }

                                break;
                            }

                            // Pick up last inverted fermata if any.
                            for (ListIterator<FermataInter> it = fermatas.listIterator(
                                    fermatas.size()); it.hasPrevious();) {
                                FermataInter f = it.previous();

                                if (f.getShape() == Shape.FERMATA_BELOW) {
                                    processFermata(f, pmBarline);
                                }

                                break;
                            }
                        }

                        // (Right side of) Ending?
                        if (ending != null) {
                            Ending pmEnding = factory.createEnding();
                            Point2D pt = ending.getLine().getP2();

                            Staff staff = current.measure.getPart().getFirstStaff();
                            pmEnding.setDefaultY(yOf(pt, staff));

                            Line2D leg = ending.getRightLeg();

                            if (leg != null) {
                                pmEnding.setEndLength(toTenths(leg.getY2() - pt.getY()));
                                pmEnding.setType(StartStopDiscontinue.STOP);
                            } else {
                                pmEnding.setType(StartStopDiscontinue.DISCONTINUE);
                            }

                            // Number (mandatory)
                            pmEnding.setNumber(endingNumber);

                            pmBarline.setEnding(pmEnding);
                        }
                    }

                    // Everything is now OK
                    current.pmMeasure.getNoteOrBackupOrForward().add(pmBarline);
                } catch (Exception ex) {
                    logger.warn("Cannot process barline {} in {}", partBarline, current.page, ex);
                }
            }

            // Markers(Coda,Segno)? (TODO: add daCapo & dalSegno?)
            if (location != RightLeftMiddle.RIGHT) {
                // Check staffBarline of top staff of top part in current stack
                Part part = current.measure.getPart();
                Measure topMeasure = current.measure.getStack().getFirstMeasure();
                PartBarline topPartBarline = getBarlineOnLeft(topMeasure);
                StaffBarlineInter topBarline = topPartBarline.getStaffBarline(
                        part,
                        part.getFirstStaff());

                for (Inter marker : topBarline.getRelatedInters(MarkerBarRelation.class)) {
                    processMarker((MarkerInter) marker);
                }
            }
        } catch (Exception ex) {
            logger.warn("Error visiting {} in {}", partBarline, current.page, ex);
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

    //------------------//
    // processChordName //
    //------------------//
    private void processChordName (ChordNameInter chordName)
    {
        if (chordName == null) {
            return;
        }

        try {
            logger.debug("Visiting {}", chordName);

            Point2D location = chordName.getLocation();
            Staff staff = current.note.getStaff();
            Harmony harmony = factory.createHarmony();

            // default-y
            harmony.setDefaultY(yOf(location, staff));

            // font-size
            harmony.setFontSize("" + chordName.getFontInfo().pointsize * TextFont.TO_POINT);

            // relative-x
            harmony.setRelativeX(
                    toTenths(location.getX() - current.note.getCenterLeft().x));

            // Placement
            harmony.setPlacement(
                    (location.getY() < current.note.getCenter().y)
                    ? AboveBelow.ABOVE
                    : AboveBelow.BELOW);

            // Staff
            insertStaffId(harmony, staff);

            // Root
            Root root = factory.createRoot();
            RootStep rootStep = factory.createRootStep();
            rootStep.setValue(stepOf(chordName.getRoot().step));
            root.setRootStep(rootStep);

            if (chordName.getRoot().alter != 0) {
                RootAlter alter = factory.createRootAlter();
                alter.setValue(new BigDecimal(chordName.getRoot().alter));
                root.setRootAlter(alter);
            }

            harmony.getHarmonyChord().add(root);

            // Kind
            Kind kind = factory.createKind();
            kind.setValue(kindOf(chordName.getKind().type));
            kind.setText(chordName.getKind().text);

            if (chordName.getKind().parentheses) {
                kind.setParenthesesDegrees(YesNo.YES);
            }

            if (chordName.getKind().symbol) {
                kind.setUseSymbols(YesNo.YES);
            }

            harmony.getHarmonyChord().add(kind);

            // Bass
            if (chordName.getBass() != null) {
                Bass bass = factory.createBass();
                BassStep bassStep = factory.createBassStep();
                bassStep.setValue(stepOf(chordName.getBass().step));
                bass.setBassStep(bassStep);

                if (chordName.getBass().alter != 0) {
                    BassAlter bassAlter = factory.createBassAlter();
                    bassAlter.setValue(new BigDecimal(chordName.getBass().alter));
                    bass.setBassAlter(bassAlter);
                }

                harmony.getHarmonyChord().add(bass);
            }

            // Degrees?
            if (chordName.getDegrees() != null) {
                for (ChordNameInter.Degree deg : chordName.getDegrees()) {
                    Degree degree = factory.createDegree();

                    DegreeValue value = factory.createDegreeValue();
                    value.setValue(new BigInteger("" + deg.value));
                    degree.setDegreeValue(value);

                    DegreeAlter alter = factory.createDegreeAlter();
                    alter.setValue(new BigDecimal(deg.alter));
                    degree.setDegreeAlter(alter);

                    DegreeType type = factory.createDegreeType();
                    type.setValue(typeOf(deg.type));
                    degree.setDegreeType(type);

                    harmony.getHarmonyChord().add(degree);
                }
            }

            // Everything is now OK
            current.pmMeasure.getNoteOrBackupOrForward().add(harmony);
        } catch (Exception ex) {
            logger.warn("Error visiting {} in {}", chordName, current.page, ex);
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
            logger.warn("Error visiting {} in {}", clef, current.page, ex);
        }
    }

    //------------------//
    // processDirection //
    //------------------//
    private void processDirection (SentenceInter sentence)
    {
        try {
            logger.debug("Visiting {}", sentence);

            String content = sentence.getValue();

            Direction direction = factory.createDirection();
            DirectionType directionType = factory.createDirectionType();
            FormattedText pmWords = factory.createFormattedText();
            Point2D location = sentence.getLocation();

            pmWords.setValue(content);

            // Staff
            Staff staff = current.note.getStaff();
            insertStaffId(direction, staff);

            // Placement
            direction.setPlacement(
                    (location.getY() < current.note.getCenter().y) ? AboveBelow.ABOVE
                    : AboveBelow.BELOW);

            // default-y
            pmWords.setDefaultY(yOf(location, staff));

            // Font information
            setFontInfo(pmWords, sentence);

            // relative-x
            pmWords.setRelativeX(toTenths(location.getX() - current.note.getCenterLeft().x));

            // Everything is now OK
            directionType.getWords().add(pmWords);
            direction.getDirectionType().add(directionType);
            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
        } catch (Exception ex) {
            logger.warn("Error visiting {} in {}", sentence, current.page, ex);
        }
    }

    //-----------------//
    // processDynamics //
    //-----------------//
    private void processDynamics (DynamicsInter dynamics)
    {
        try {
            logger.debug("Visiting {}", dynamics);

            // No point to export incorrect dynamics
            if (dynamics.getShape() == null) {
                return;
            }

            Direction direction = factory.createDirection();
            DirectionType directionType = factory.createDirectionType();
            Dynamics pmDynamics = factory.createDynamics();

            // Precise dynamic signature
            pmDynamics.getPOrPpOrPpp().add(getDynamicsObject(dynamics.getShape()));

            // Staff?
            Staff staff = current.note.getStaff();
            insertStaffId(direction, staff);

            // Placement
            final Point location = dynamics.getCenterLeft();

            if (location.y < current.note.getCenter().y) {
                direction.setPlacement(AboveBelow.ABOVE);
            } else {
                direction.setPlacement(AboveBelow.BELOW);
            }

            // default-y
            pmDynamics.setDefaultY(yOf(location, staff));

            // Relative-x (No offset for the time being) using note left side
            pmDynamics.setRelativeX(toTenths(location.x - current.note.getCenterLeft().x));

            // Related sound level, if available
            Integer soundLevel = dynamics.getSoundLevel();

            if (soundLevel != null) {
                Sound sound = factory.createSound();
                sound.setDynamics(new BigDecimal(soundLevel));
                direction.setSound(sound);
            }

            // Everything is now OK
            directionType.getDynamics().add(pmDynamics);
            direction.getDirectionType().add(directionType);
            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
        } catch (Exception ex) {
            logger.warn("Error visiting {} in {}", dynamics, current.page, ex);
        }
    }

    //----------------//
    // processFermata //
    //----------------//
    /**
     * Process a fermata via a note or via a barline.
     *
     * @param fermata   the fermata inter
     * @param pmBarline ProxyMusic barline when via barline, null when via note
     */
    private void processFermata (FermataInter fermata,
                                 Barline pmBarline)
    {
        try {
            logger.debug("Visiting {}", fermata);

            Fermata pmFermata = factory.createFermata();

            // default-y (of the fermata dot)
            // For upright we use bottom of the box, for inverted the top of the box
            Rectangle box = fermata.getBounds();
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
            if (pmBarline != null) {
                pmBarline.getFermata().add(pmFermata); // Add to barline
            } else {
                getNotations().getTiedOrSlurOrTuplet().add(pmFermata); // Add to note
            }
        } catch (Exception ex) {
            logger.warn("Error visiting {} in {}", fermata, current.page, ex);
        }
    }

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

            if (keySignature == null) {
                return;
            }

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
            logger.warn("Error visiting {} in {}", keySignature, current.page, ex);
        }
    }

    //----------------//
    // processKeyVoid //
    //----------------//
    /**
     * Process a lack of key signature at system start.
     */
    private void processKeyVoid ()
    {
        try {
            logger.debug("processKeyVoid");

            final Key key = factory.createKey();
            key.setFifths(new BigInteger("0"));

            // Is this new?
            final int staffCount = current.measure.getPart().getStaves().size();
            boolean isNew = false;

            for (int index = 0; index < staffCount; index++) {
                Key currentKey = current.keys.get(index);

                if ((currentKey != null) && !areEqual(currentKey, key)) {
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
        } catch (Exception ex) {
            logger.warn("Error in processKeyVoid in {}", current.page, ex);
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
        if (current.measure.hasKeys()) {
            // Check if all keys are the same across all staves in measure
            if (current.measure.hasSameKeys()) {
                processKey(current.measure.getKey(0), true); // global: true
            } else {
                // Work staff by staff
                final int staffCount = current.measure.getPart().getStaves().size();

                for (int index = 0; index < staffCount; index++) {
                    KeyInter key = current.measure.getKey(index);
                    processKey(key, false); // global: false
                }
            }
        } else {
            // No key signature in measure: this is meaningful only at beginning of staff
            if (isFirst.measure) {
                processKeyVoid();
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

    //---------------//
    // processMarker //
    //---------------//
    private void processMarker (MarkerInter marker)
    {
        if (marker == null) {
            return;
        }

        try {
            logger.debug("Visiting {}", marker);

            String measureId = current.measure.getStack().getScoreId(current.pageMeasureIdOffset);
            Direction direction = factory.createDirection();
            EmptyPrintStyleAlign empty = factory.createEmptyPrintStyleAlign();
            DirectionType directionType = factory.createDirectionType();
            direction.getDirectionType().add(directionType);

            // Staff? We use top staff of current measure, perhaps not the marker staff.
            insertStaffId(direction, current.measure.getPart().getFirstStaff());

            //            // default-x
            //            empty.setDefaultX(
            //                    toTenths(marker.getCenterLeft().x - current.measure.getAbscissa(LEFT, staff)));
            //
            //            // default-y
            //            empty.setDefaultY(yOf(marker.getCenterLeft(), staff));
            //
            // Need also a Sound element
            Sound sound = factory.createSound();
            direction.setSound(sound);
            sound.setDivisions(new BigDecimal(current.page.simpleDurationOf(QUARTER_DURATION)));

            switch (marker.getShape()) {
            case CODA:
                sound.setCoda(measureId);
                directionType.getCoda().add(empty);

                break;

            case SEGNO:
                sound.setSegno(measureId);
                directionType.getSegno().add(empty);

                break;

            case DA_CAPO: {
                FormattedText text = new FormattedText();
                text.setValue("D.C.");
                directionType.getWords().add(text);
                sound.setDacapo(YesNo.YES);
            }

            break;

            case DAL_SEGNO: {
                // Example:
                //  <direction placement="above">
                //	<direction-type>
                //	    <words font-style="italic">D.S. al Fine</words>
                //	</direction-type>
                //	<sound dalsegno="9"/>
                //  </direction>
                FormattedText text = new FormattedText();
                text.setValue("D.S.");
                directionType.getWords().add(text);

                //TODO: we need to point back to id of measure where segno is located
                ///sound.setDalsegno(measureId); // NO, not this measure, but the target measure!
            }

            break;

            default:
                logger.warn("Unknown marker shape: {}", marker.getShape());

                return;
            }

            // Everything is now OK
            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
        } catch (Exception ex) {
            logger.warn("Error visiting {} in {}", marker, current.page, ex);
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
            current.pmMeasure.setNumber(stack.getScoreId(current.pageMeasureIdOffset));

            if (!measure.isDummy()) {
                current.pmMeasure.setWidth(toTenths(measure.getWidth()));
            }

            if (stack.isImplicit()) {
                current.pmMeasure.setImplicit(YesNo.YES);
            }

            // Print?
            new MeasurePrint(measure).process();

            // Left/mid barline?
            PartBarline mid = measure.getMidPartBarline();

            if (mid != null) {
                processBarline(mid, RightLeftMiddle.MIDDLE);
            } else {
                processBarline(getBarlineOnLeft(measure), RightLeftMiddle.LEFT);
            }

            // Divisions?
            if (isPageFirstMeasure) {
                try {
                    getAttributes().setDivisions(
                            new BigDecimal(current.page.simpleDurationOf(QUARTER_DURATION)));
                } catch (Exception ex) {
                    if (current.page.getDurationDivisor() == null) {
                        logger.warn(
                                "Not able to infer division value for part {} in {}",
                                current.logicalPart.getPid(), current.page);
                    } else {
                        logger.warn("Error on divisions in {}", current.page, ex);
                    }
                }
            }

            // Number of staves, if > 1
            if (isScoreFirstMeasure && current.logicalPart.isMultiStaff()) {
                getAttributes().setStaves(new BigInteger("" + current.logicalPart.getStaffCount()));
            }

            // Tempo?
            if (isScoreFirstMeasure && isFirst.part && !measure.isDummy()) {
                Direction direction = factory.createDirection();
                current.pmMeasure.getNoteOrBackupOrForward().add(direction);
                direction.setPlacement(AboveBelow.ABOVE);

                DirectionType directionType = factory.createDirectionType();
                direction.getDirectionType().add(directionType);

                // Use a dummy words element
                FormattedText pmWords = factory.createFormattedText();
                directionType.getWords().add(pmWords);
                pmWords.setValue("");

                Sound sound = factory.createSound();
                sound.setTempo(new BigDecimal(score.getTempoParam().getValue()));
                direction.setSound(sound);
            }

            // Insert KeySignature(s), if any (they may vary between staves)
            processKeys();

            // Insert TimeSignature, if any
            if (measure.getTimeSignature() != null) {
                processTime(measure.getTimeSignature());
            } else if (isScoreFirstMeasure) {
                // We need to insert a time sig!
                // TODO
                ///processTime(4,4,null);
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

                // Need a backup?
                if (!timeCounter.equals(Rational.ZERO)) {
                    insertBackup(timeCounter);
                    timeCounter = Rational.ZERO;
                }

                if (voice.isWhole()) {
                    // Delegate to the chord children directly
                    AbstractChordInter chord = voice.getWholeChord();
                    clefIters.push(measure.getWidth(), chord.getTopStaff());
                    processChord(chord);

                    if (stack.getActualDuration() != null) {
                        timeCounter = stack.getActualDuration();
                    }
                } else {
                    for (Slot slot : stack.getSlots()) {
                        SlotVoice info = voice.getSlotInfo(slot);

                        if ((info != null) && // Skip free slots
                                (info.status == SlotVoice.Status.BEGIN)) {
                            AbstractChordInter chord = info.chord;
                            clefIters.push(slot.getXOffset(), chord.getTopStaff());

                            // Need a forward before this chord?
                            Rational timeOffset = chord.getTimeOffset();

                            if (timeCounter.compareTo(timeOffset) < 0) {
                                insertForward(timeOffset.minus(timeCounter), chord);
                                timeCounter = timeOffset;
                            }

                            // Grace chord(s) before this chord?
                            if (chord instanceof HeadChordInter) {
                                HeadChordInter headChord = (HeadChordInter) chord;
                                SmallChordInter small = headChord.getGraceChord();

                                if (small != null) {
                                    BeamGroup group = small.getBeamGroup();

                                    if (group != null) {
                                        for (AbstractChordInter ch : group.getChords()) {
                                            processChord(ch);
                                        }
                                    } else {
                                        processChord(small);
                                    }
                                }
                            }

                            // Delegate to the chord children directly
                            processChord(chord);
                            timeCounter = timeCounter.plus(chord.getDuration());
                        }
                    }

                    //                    // Need an ending forward?
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
                processBarline(measure.getRightPartBarline(), RightLeftMiddle.RIGHT);
            }
        } catch (Exception ex) {
            logger.warn("Error visiting {} in {}", measure, current.page, ex);

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
            final Staff staff = note.getStaff();
            final AbstractChordInter chord = note.getChord();
            final boolean isFirstInChord = chord.getNotes().indexOf(note) == 0;

            current.note = note;
            current.pmNote = factory.createNote();

            // For first note in chord
            if (isFirstInChord) {
                if (!current.measure.isDummy()) {
                    // Chord events (direction, pedal, dynamics, articulation, ornament)
                    for (Relation rel : sig.edgesOf(chord)) {
                        final Inter other = sig.getOppositeInter(chord, rel);

                        if (rel instanceof ChordSentenceRelation) {
                            processDirection((SentenceInter) other);
                        } else if (rel instanceof ChordPedalRelation) {
                            processPedal((PedalInter) other);
                        } else if (rel instanceof ChordWedgeRelation) {
                            HorizontalSide side = ((ChordWedgeRelation) rel).getSide();
                            processWedge((WedgeInter) other, side);
                        } else if (rel instanceof ChordDynamicsRelation) {
                            processDynamics((DynamicsInter) other);
                        } else if (rel instanceof ChordArticulationRelation) {
                            processArticulation((ArticulationInter) other);
                        } else if (rel instanceof ChordOrnamentRelation) {
                            processOrnament((OrnamentInter) other);
                        } else if (rel instanceof ChordArpeggiatoRelation) {
                            processArpeggiato((ArpeggiatoInter) other);
                        } else if (rel instanceof FermataChordRelation) {
                            processFermata((FermataInter) other, null);
                        } else if (rel instanceof ChordNameRelation) {
                            processChordName((ChordNameInter) other);
                        }
                    }
                }
            } else {
                // Chord indication for every other note
                current.pmNote.setChord(new Empty());
            }

            // Rest?
            boolean isMeasureRest = false;

            if (note.getShape().isRest()) {
                Rest rest = factory.createRest();
                RestChordInter restChord = (RestChordInter) chord;

                // Rest for the whole measure?
                if (current.measure.isDummy() || current.measure.isMeasureRest(restChord)) {
                    rest.setMeasure(YesNo.YES);
                    isMeasureRest = true;
                }

                if (!current.measure.isDummy() && !isMeasureRest && !staff.isOneLineStaff()) {
                    // Set displayStep & displayOctave for rest
                    rest.setDisplayStep(stepOf(note.getStep()));
                    rest.setDisplayOctave(note.getOctave());
                }

                current.pmNote.setRest(rest);
            } else {
                HeadChordInter headChord = (HeadChordInter) chord;

                if (!current.measure.isDummy()) {
                    // Grace?
                    if (isFirstInChord && note.getShape().isSmall()) {
                        Grace grace = factory.createGrace();
                        current.pmNote.setGrace(grace);

                        // Slash? (check the flag)
                        StemInter stem = headChord.getStem();

                        if (stem != null) {
                            for (Relation rel : sig.getRelations(stem, FlagStemRelation.class)) {
                                if (Shape.SMALL_FLAG_SLASH == sig.getOppositeInter(stem, rel)
                                        .getShape()) {
                                    grace.setSlash(YesNo.YES);

                                    break;
                                }
                            }
                        }
                    }
                }

                if (staff.isOneLineStaff()) {
                    // Unpitched
                    Unpitched unpitched = factory.createUnpitched();
                    // For MuseScore: F5
                    // For Finale:    G3
                    unpitched.setDisplayStep(Step.F);
                    unpitched.setDisplayOctave(5);
                    current.pmNote.setUnpitched(unpitched);
                } else {
                    // Pitch
                    Pitch pitch = factory.createPitch();
                    pitch.setStep(stepOf(note.getStep()));
                    pitch.setOctave(note.getOctave());

                    // Alter?
                    HeadInter head = (HeadInter) note;
                    Key key = current.keys.get(staff.getIndexInPart());
                    Integer fifths = (key != null) ? key.getFifths().intValue() : null;
                    int alter = head.getAlteration(fifths);

                    if (alter != 0) {
                        pitch.setAlter(new BigDecimal(alter));
                    }

                    current.pmNote.setPitch(pitch);
                }

                // Cross(x)?
                if (note.getShape() == Shape.NOTEHEAD_CROSS) {
                    Notehead notehead = factory.createNotehead();
                    notehead.setValue(NoteheadValue.X);
                    current.pmNote.setNotehead(notehead);
                }
            }

            // Default-x (use left side of the note wrt measure)
            if (!current.measure.isDummy()) {
                int noteLeft = note.getCenterLeft().x;
                current.pmNote.setDefaultX(
                        toTenths(noteLeft - current.measure.getAbscissa(LEFT, staff)));
            }

            // Tuplet factor?
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

                    if ((embraced.get(0) == chord) || (embraced.get(
                            embraced.size() - 1) == chord)) {
                        processTuplet(tuplet);
                    }
                }
            }

            // Duration (not for grace note)
            if (current.pmNote.getGrace() == null) {
                try {
                    final Rational dur;

                    if (chord.isWholeRest()) {
                        Rational measureDur = current.measure.getStack().getActualDuration();
                        dur = (measureDur != null) ? measureDur : Rational.ONE; // Not too bad...
                    } else {
                        dur = chord.getDuration();
                    }

                    current.pmNote.setDuration(new BigDecimal(current.page.simpleDurationOf(dur)));
                } catch (Exception ex) {
                    if (current.page.getDurationDivisor() != null) {
                        logger.warn("Not able to get duration of {} in {}", note, current.page, ex);
                    }
                }
            }

            // Voice
            Voice voice = chord.getVoice();

            if (voice != null) {
                current.pmNote.setVoice("" + voice.getId());
            } else {
                logger.warn("No voice for {}", chord);
            }

            // Type
            if (!current.measure.isDummy()) {
                if (!isMeasureRest) {
                    NoteType noteType = factory.createNoteType();
                    noteType.setValue(getNoteTypeName(note));
                    current.pmNote.setType(noteType);
                }
            }

            // For specific mirrored note
            if (note.getMirror() != null) {
                int fbn = note.getChord().getBeamsOrFlagsNumber();

                if ((fbn > 0) && (note.getShape() == Shape.NOTEHEAD_VOID)) {
                    // Indicate that the head should not be filled
                    //   <notehead filled="no">normal</notehead>
                    Notehead notehead = factory.createNotehead();
                    notehead.setFilled(YesNo.NO);
                    notehead.setValue(NoteheadValue.NORMAL);
                    current.pmNote.setNotehead(notehead);
                }
            }

            // Stem?
            if (chord.getStem() != null) {
                Stem pmStem = factory.createStem();
                Point tail = chord.getTailLocation();

                if (!staff.isOneLineStaff()) {
                    pmStem.setDefaultY(yOf(tail, staff));
                }

                if (tail.y < note.getCenter().y) {
                    pmStem.setValue(StemValue.UP);
                } else {
                    pmStem.setValue(StemValue.DOWN);
                }

                current.pmNote.setStem(pmStem);
            }

            // Staff?
            if (current.logicalPart.isMultiStaff()) {
                current.pmNote.setStaff(new BigInteger("" + (1 + staff.getIndexInPart())));
            }

            // Dots
            for (int i = 0; i < chord.getDotsNumber(); i++) {
                current.pmNote.getDot().add(factory.createEmptyPlacement());
            }

            if (!note.getShape().isRest()) {
                // Accidental?
                HeadInter head = (HeadInter) note;
                AlterInter alter = head.getAccidental();

                if (alter != null) {
                    Accidental accidental = factory.createAccidental();
                    accidental.setValue(accidentalValueOf(alter.getShape()));
                    current.pmNote.setAccidental(accidental);
                }

                // Beams?
                int beamCounter = 0;

                for (AbstractBeamInter beam : chord.getBeams()) {
                    Beam pmBeam = factory.createBeam();
                    pmBeam.setNumber(1 + beamCounter++);

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

                // Lyrics?
                if (isFirstInChord) {
                    for (Relation rel : sig.getRelations(chord, ChordSyllableRelation.class)) {
                        processSyllable((LyricItemInter) sig.getOppositeInter(chord, rel));
                    }
                }
            }

            // Everything is OK
            current.pmMeasure.getNoteOrBackupOrForward().add(current.pmNote);
        } catch (Exception ex) {
            logger.warn("Error visiting {} in {}", note, current.page, ex);
        }

        // Safer...
        current.endNote();
    }

    //-----------------//
    // processOrnament //
    //-----------------//
    @SuppressWarnings("unchecked")
    private void processOrnament (OrnamentInter ornament)
    {
        try {
            logger.debug("Visiting {}", ornament);

            JAXBElement<?> element = getOrnamentObject(ornament.getShape());

            // Placement?
            Class<?> classe = element.getDeclaredType();
            Method method = classe.getMethod("setPlacement", AboveBelow.class);
            method.invoke(
                    element.getValue(),
                    (ornament.getCenter().y < current.note.getCenter().y) ? AboveBelow.ABOVE
                    : AboveBelow.BELOW);
            // Everything is OK
            // Include in ornaments
            getOrnaments().getTrillMarkOrTurnOrDelayedTurn().add(element);
        } catch (IllegalAccessException |
                 IllegalArgumentException |
                 NoSuchMethodException |
                 SecurityException |
                 InvocationTargetException ex) {
            logger.warn("Error visiting " + ornament, ex);
        }
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
            logger.warn("Error visiting {}", part, ex);
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
        Map<LogicalPart, ScorePartwise.Part> partMap = new LinkedHashMap<>();

        for (LogicalPart p : score.getLogicalParts()) {
            ScorePartwise.Part pmPart = createScorePart(p);
            partMap.put(p, pmPart);
            partList.getPartGroupOrScorePart().add(pmPart.getId());
        }

        // Then, stub by stub, populate all ScorePartwise.Part instances in parallel
        for (SheetStub stub : score.getStubs()) {
            processStub(stub, partMap);
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

            // Staff?
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
                    (refPoint.y < current.note.getCenter().y) ? AboveBelow.ABOVE
                    : AboveBelow.BELOW);

            // Everything is OK
            directionType.setPedal(pmPedal);
            direction.getDirectionType().add(directionType);
            current.pmMeasure.getNoteOrBackupOrForward().add(direction);
        } catch (Exception ex) {
            logger.warn("Error visiting {} in {}", pedal, current.page, ex);
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
                Book book = score.getFirstPage().getSheet().getStub().getBook();
                identification.setSource(book.getInputPath().toString());

                // Encoding
                Encoding encoding = factory.createEncoding();
                scorePartwise.setIdentification(identification);

                // [Encoding]/Software
                encoding.getEncodingDateOrEncoderOrSoftware().add(
                        factory.createEncodingSoftware(
                                WellKnowns.TOOL_NAME + " " + WellKnowns.TOOL_REF));

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
                                    String.format(
                                            Locale.US,
                                            "%.4f",
                                            (current.scale.getInterline() * 25.4 * 4) / 300)));
                    scaling.setTenths(new BigDecimal(40));

                    // [Defaults]/PageLayout (using first page)
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

                // [Defaults]/LyricFont
                Font lyricFont = org.audiveris.omr.ui.symbol.TextFont.baseTextFont;
                LyricFont pmLyricFont = factory.createLyricFont();
                pmLyricFont.setFontFamily(lyricFont.getName());
                pmLyricFont.setFontSize(
                        "" + org.audiveris.omr.ui.symbol.TextFont.baseTextFont.getSize());

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

            // PartList & sequence of parts
            if (score.getLogicalParts() != null) {
                processPartList();
            }
        } catch (Exception ex) {
            logger.warn("Error visiting {} {}", score, ex.toString(), ex);
        }
    }

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

                scorePartwise.getIdentification().getCreator().add(typedText);
            }

            break;

            case UnknownRole:
                break;

            default: // LyricsItem, Direction, ChordName

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
            Point2D pt = sentence.getLocation();
            creditWords.setDefaultX(toTenths(pt.getX()));
            creditWords.setDefaultY(toTenths(current.page.getDimension().height - pt.getY()));

            pmCredit.getCreditTypeOrLinkOrBookmark().add(creditWords);
            scorePartwise.getCredit().add(pmCredit);
        } catch (Exception ex) {
            logger.warn("Error visiting {} in {}", sentence, current.page, ex);
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
            logger.warn("Error visiting {} in {}", slur, current.page, ex);
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

        final Integer sheetPageId = score.getSheetPageId(stub.getNumber());

        // This should never occur if processStub() is called only on score relevant stubs
        if (sheetPageId == null) {
            return;
        }

        final Sheet sheet = stub.getSheet();
        final Page page = sheet.getPages().get(sheetPageId - 1);

        source.encodePage(page, scorePartwise);

        current.page = page;
        current.pageMeasureIdOffset = score.getMeasureIdOffset(page);
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
            BigDecimal defaultY = yOf(syllable.getLocation(), syllable.getStaff());
            pmLyric.setDefaultY(defaultY);
            pmLyric.setPlacement(defaultY.intValue() >= 0 ? AboveBelow.ABOVE : AboveBelow.BELOW);
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
            logger.warn("Error visiting {} in {}", system, current.page, ex);
        }
    }

    //-------------//
    // processTime //
    //-------------//
    private void processTime (AbstractTimeInter timeSig)
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

        // Symbol?
        if (shape != null) {
            switch (shape) {
            case COMMON_TIME:
                time.setSymbol(TimeSymbol.COMMON);

                break;

            case CUT_TIME:
                time.setSymbol(TimeSymbol.CUT);

                break;

            default:
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
            logger.warn("Error visiting {} in {}", tuplet, current.page, ex);
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

            // Staff?
            Staff staff = current.note.getStaff();
            insertStaffId(direction, staff);

            // Start or stop?
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
            logger.warn("Error visiting {} in {}", wedge, current.page, ex);
        }
    }

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

    //-------//
    // build //
    //-------//
    /**
     * Visit the whole score tree and build the corresponding ScorePartwise.
     *
     * @param score the score to export (cannot be null)
     * @return the populated ScorePartwise
     * @throws InterruptedException if the thread has been interrupted
     * @throws ExecutionException   if a checked exception was thrown
     */
    public static ScorePartwise build (Score score)
            throws InterruptedException,
                   ExecutionException
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
        return Objects.equals(left.getNumber(), right.getNumber()) && Objects.equals(
                left.getSign(),
                right.getSign()) && Objects.equals(left.getLine(), right.getLine()) && Objects
                .equals(left.getClefOctaveChange(), right.getClefOctaveChange());
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

        /** Containing measure. */
        private final Measure measure;

        /** Staves of the containing part. */
        private final List<Staff> staves;

        /** Per staff, iterator on Clefs sorted by abscissa. */
        private final Map<Staff, ListIterator<ClefInter>> iters;

        ClefIterators (Measure measure)
        {
            this.measure = measure;

            staves = measure.getPart().getStaves();

            // Temporary map: staff -> staff's clefs
            Map<Staff, List<ClefInter>> map = new HashMap<>();

            for (ClefInter clef : measure.getClefs()) {
                Staff staff = clef.getStaff();
                List<ClefInter> list = map.get(staff);

                if (list == null) {
                    map.put(staff, list = new ArrayList<>());
                }

                list.add(clef);
            }

            // Populate iterators
            iters = new LinkedHashMap<>();

            for (Map.Entry<Staff, List<ClefInter>> entry : map.entrySet()) {
                List<ClefInter> list = entry.getValue(); // Already sorted by full center abscissa
                iters.put(entry.getKey(), list.listIterator());
            }
        }

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

    //--------------//
    // MeasurePrint //
    //--------------//
    /**
     * Handles the print element for a measure.
     */
    private class MeasurePrint
    {

        private final Measure measure;

        private final Print pmPrint;

        /** Needed to remove the element if not actually used. */
        private boolean used = false;

        MeasurePrint (Measure measure)
        {
            this.measure = measure;

            // Allocate and insert Print immediately (before any attribute or note element)
            // It will later be removed if not actually used in the measure
            pmPrint = factory.createPrint();
            current.pmMeasure.getNoteOrBackupOrForward().add(pmPrint);
        }

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
                                        - current.system.getWidth()).subtract(
                                pageHorizontalMargin));

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
                                        toTenths(
                                                staff.getLeftY(TOP) - staffAbove.getLeftY(BOTTOM)));
                                getPrint().getStaffLayout().add(staffLayout);
                            }
                        } catch (Exception ex) {
                            logger.warn(
                                    "Error exporting staff layout system#" + current.system.getId()
                                            + " part#" + current.logicalPart.getId() + " staff#"
                                            + staff.getId() + " in " + current.page,
                                    ex);
                        }
                    }
                }
            }

            if (isFirst.measure) {
                StaffDetails staffDetails = factory.createStaffDetails();

                // Do not print artificial parts
                staffDetails.setPrintObject(measure.isDummy() ? YesNo.NO : YesNo.YES);

                // OneLineStaff?
                if (measure.getPart().getStaves().size() == 1) {
                    Staff staff = measure.getPart().getStaves().get(0);

                    if (staff.isOneLineStaff()) {
                        staffDetails.setStaffLines(BigInteger.ONE);
                    }
                }

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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

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

    //---------//
    // Current //
    //---------//
    /** Keep references of all current entities. */
    private static class Current
    {

        // Score dependent
        Work pmWork;

        // Part dependent
        LogicalPart logicalPart;

        ScorePartwise.Part pmPart;

        // Page dependent
        Page page;

        int pageMeasureIdOffset;

        Scale scale;

        // System dependent
        SystemInfo system;

        // Measure dependent
        Measure measure;

        ScorePartwise.Part.Measure pmMeasure;

        final TreeMap<Integer, Key> keys = new TreeMap<>();

        Voice voice;

        Attributes pmAttributes;

        // Note dependent
        AbstractNoteInter note;

        Note pmNote;

        Notations pmNotations;

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

        /** We are writing the first part of the score */
        boolean part;

        /** We are writing the first page of the score */
        boolean page;

        /** We are writing the first system in the current page */
        boolean system;

        /** We are writing the first measure in current system (in current logicalPart) */
        boolean measure;

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

}
