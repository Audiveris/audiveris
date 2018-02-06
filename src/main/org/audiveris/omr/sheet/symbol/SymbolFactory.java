//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l F a c t o r y                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sheet.symbol;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;
import org.audiveris.omr.glyph.ShapeSet;
import static org.audiveris.omr.glyph.ShapeSet.*;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.header.TimeBuilder;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractFlagInter;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.ArticulationInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeamHookInter;
import org.audiveris.omr.sig.inter.BeamInter;
import org.audiveris.omr.sig.inter.BreathMarkInter;
import org.audiveris.omr.sig.inter.CaesuraInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.DeletedInterException;
import org.audiveris.omr.sig.inter.DynamicsInter;
import org.audiveris.omr.sig.inter.FermataArcInter;
import org.audiveris.omr.sig.inter.FermataDotInter;
import org.audiveris.omr.sig.inter.FermataInter;
import org.audiveris.omr.sig.inter.FingeringInter;
import org.audiveris.omr.sig.inter.FlagInter;
import org.audiveris.omr.sig.inter.FretInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.InterEnsemble;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.MarkerInter;
import org.audiveris.omr.sig.inter.OrnamentInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.PluckingInter;
import org.audiveris.omr.sig.inter.RepeatDotInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.SmallFlagInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code SymbolFactory} generates the inter instances corresponding to
 * to an acceptable symbol evaluation in a given system.
 * <p>
 * (Generally there is one inter instance per evaluation, an exception is the case of full time
 * signature which leads to upper plus lower number instances).
 *
 * @author Hervé Bitteur
 */
public class SymbolFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    private final SIGraph sig;

    /** All system stems, ordered by abscissa. */
    private final List<Inter> systemStems;

    /** All system heads, ordered by abscissa. */
    private final List<Inter> systemHeads;

    /** All system notes (heads and rests), ordered by abscissa. */
    private List<Inter> systemNotes;

    /** All system head-based chords, ordered by abscissa. */
    private final List<Inter> systemHeadChords;

    /** All system rests, ordered by abscissa. */
    private List<Inter> systemRests;

    /** All system bar lines, ordered by abscissa. */
    private List<Inter> systemBars;

    /** Dot factory companion. */
    private final DotFactory dotFactory;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SymbolsFactory object.
     *
     * @param system the dedicated system
     */
    public SymbolFactory (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();

        systemStems = sig.inters(Shape.STEM);
        Collections.sort(systemStems, Inters.byAbscissa);

        systemHeads = sig.inters(HeadInter.class);
        Collections.sort(systemHeads, Inters.byAbscissa);

        systemHeadChords = sig.inters(AbstractChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        dotFactory = new DotFactory(this);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Create the proper inter instance(s) for the provided evaluated glyph.
     * <p>
     * TODO: method to be completed so that all possible inter classes are really handled!!!!
     *
     * @param eval         evaluation result
     * @param glyph        evaluated glyph
     * @param closestStaff only the closest staff, ordinate-wise
     */
    public void create (Evaluation eval,
                        Glyph glyph,
                        Staff closestStaff)
    {
        final Shape shape = eval.shape;
        final double grade = Grades.intrinsicRatio * eval.grade;

        if (glyph.isVip()) {
            logger.info("VIP glyph#{} symbol created as {}", glyph.getId(), eval.shape);
        }

        if (shape == Shape.CLUTTER) {
            return;
        }

        if (Clefs.contains(shape)) {
            addSymbol(ClefInter.create(glyph, shape, grade, closestStaff)); // Staff is OK
        } else if (Rests.contains(shape)) {
            addSymbol(RestInter.create(glyph, shape, grade, system, systemHeadChords));
        } else if (Accidentals.contains(shape)) {
            AlterInter alterInter = AlterInter.create(glyph, shape, grade, closestStaff); // Staff is very questionable!
            addSymbol(alterInter);
            alterInter.detectHeadRelation(systemHeads);
        } else if (Flags.contains(shape)) {
            AbstractFlagInter.create(glyph, shape, grade, system, systemStems); // Glyph is checked
        } else if (PartialTimes.contains(shape)) {
            addSymbol(TimeNumberInter.create(glyph, shape, grade, closestStaff)); // Staff is OK
        } else if (WholeTimes.contains(shape)) {
            TimeWholeInter time = new TimeWholeInter(glyph, shape, grade);
            time.setStaff(closestStaff); // Staff is OK
            addSymbol(time);
        } else if (Dynamics.contains(shape)) {
            addSymbol(new DynamicsInter(glyph, shape, grade));
        } else if (Tuplets.contains(shape)) {
            addSymbol(TupletInter.create(glyph, shape, grade, system, systemHeadChords));
        } else if (FermataArcs.contains(shape)) {
            addSymbol(FermataArcInter.create(glyph, shape, grade, system));
        } else if (shape == Shape.DOT_set) {
            dotFactory.instantDotChecks(eval, glyph);
        } else if (Articulations.contains(shape)) {
            addSymbol(ArticulationInter.create(glyph, shape, grade, system, systemHeadChords));
        } else if (Pedals.contains(shape)) {
            addSymbol(new PedalInter(glyph, shape, grade));
        } else if (Markers.contains(shape)) {
            MarkerInter marker = new MarkerInter(glyph, shape, grade);
            marker.setStaff(closestStaff); // Staff is OK
            addSymbol(marker);
            marker.linkWithBarline();
        } else if (shape == Shape.CAESURA) {
            addSymbol(CaesuraInter.create(glyph, grade, system));
        } else if (shape == Shape.BREATH_MARK) {
            addSymbol(BreathMarkInter.create(glyph, grade, system));
        } else if (shape == Shape.ARPEGGIATO) {
            addSymbol(ArpeggiatoInter.create(glyph, grade, system, systemHeadChords));
        } else if (ShapeSet.supportFingerings() && Digits.contains(shape)) {
            addSymbol(new FingeringInter(glyph, shape, grade));
        } else if (ShapeSet.supportFrets() && Romans.contains(shape)) {
            addSymbol(new FretInter(glyph, shape, grade));
        } else if (ShapeSet.supportPluckings() && Pluckings.contains(shape)) {
            addSymbol(new PluckingInter(glyph, shape, grade));
        } else {
            logger.debug("SymbolFactory no support yet for {} {}", shape, glyph);
        }
    }

    /**
     * Create a not-yet-settled inter instance to handle the provided shape.
     *
     * @param shape provided shape
     * @param grade quality grade
     * @return the created ghost inter or null
     */
    public static Inter createGhost (Shape shape,
                                     double grade)
    {
        switch (shape) {
        //
        // Key signatures
        case KEY_FLAT_1:
        case KEY_FLAT_2:
        case KEY_FLAT_3:
        case KEY_FLAT_4:
        case KEY_FLAT_5:
        case KEY_FLAT_6:
        case KEY_FLAT_7:
        case KEY_SHARP_1:
        case KEY_SHARP_2:
        case KEY_SHARP_3:
        case KEY_SHARP_4:
        case KEY_SHARP_5:
        case KEY_SHARP_6:
        case KEY_SHARP_7:
            return new KeyInter(grade, shape);

        // Brace, bracket ???
        //
        // Barlines ???
        //
        // Beams
        case BEAM:
            return new BeamInter(grade);

        case BEAM_HOOK:
            return new BeamHookInter(grade);

        //
        // Repeats
        case REPEAT_DOT:
            return new RepeatDotInter(null, grade, null, null); // No visit

        // Markers
        case CODA:
        case SEGNO:
        case DAL_SEGNO:
        case DA_CAPO:
            return new MarkerInter(null, shape, grade); // No visit

        // Clefs
        case G_CLEF:
        case G_CLEF_SMALL:
        case G_CLEF_8VA:
        case G_CLEF_8VB:
        case F_CLEF:
        case F_CLEF_SMALL:
        case F_CLEF_8VA:
        case F_CLEF_8VB:
        case C_CLEF:
        case PERCUSSION_CLEF:
            return new ClefInter(shape, grade);

        // Time sig
        case TIME_ZERO:
        case TIME_ONE:
        case TIME_TWO:
        case TIME_THREE:
        case TIME_FOUR:
        case TIME_FIVE:
        case TIME_SIX:
        case TIME_SEVEN:
        case TIME_EIGHT:
        case TIME_NINE:
        case TIME_TWELVE:
        case TIME_SIXTEEN:
            return new TimeNumberInter(null, shape, grade, null); // No visit

        case COMMON_TIME:
        case CUT_TIME:
        case TIME_FOUR_FOUR:
        case TIME_TWO_TWO:
        case TIME_TWO_FOUR:
        case TIME_THREE_FOUR:
        case TIME_FIVE_FOUR:
        case TIME_THREE_EIGHT:
        case TIME_SIX_EIGHT:
            return new TimeWholeInter(null, shape, grade);

        // Noteheads
        case NOTEHEAD_BLACK:
        case NOTEHEAD_BLACK_SMALL:
        case NOTEHEAD_VOID:
        case NOTEHEAD_VOID_SMALL:
        case BREVE:
        case WHOLE_NOTE:
        case WHOLE_NOTE_SMALL:
            return new HeadInter(null, null, null, shape, grade, null, null);

        case AUGMENTATION_DOT:
            return new AugmentationDotInter(null, grade); // No visit

        // Ledger
        case LEDGER:
            return new LedgerInter(null, grade);

        // Stem
        case STEM:
            return new StemInter(null, grade);

        // Flags
        case FLAG_1:
        case FLAG_2:
        case FLAG_3:
        case FLAG_4:
        case FLAG_5:
        case FLAG_1_UP:
        case FLAG_2_UP:
        case FLAG_3_UP:
        case FLAG_4_UP:
        case FLAG_5_UP:
            return new FlagInter(null, shape, grade);

        case SMALL_FLAG:
        case SMALL_FLAG_SLASH:
            return new SmallFlagInter(null, shape, grade);

        // Accidentals
        case FLAT:
        case NATURAL:
        case SHARP:
        case DOUBLE_SHARP:
        case DOUBLE_FLAT:
            return new AlterInter(null, shape, grade, null, null, null); // No visit

        // Articulations
        case ACCENT:
        case TENUTO:
        case STACCATO:
        case STACCATISSIMO:
        case STRONG_ACCENT:
            return new ArticulationInter(null, shape, grade); // No visit

        // Holds
        case FERMATA:
        case FERMATA_BELOW:
            return new FermataInter(shape, grade); // No visit

        case FERMATA_DOT:
            return new FermataDotInter(null, grade); // No visit

        case CAESURA:
            return new CaesuraInter(null, grade); // No visit

        case BREATH_MARK:
            return new BreathMarkInter(null, grade); // No visit

        // Rests
        case LONG_REST:
        case BREVE_REST:
        case WHOLE_REST:
        case HALF_REST:
        case QUARTER_REST:
        case EIGHTH_REST:
        case ONE_16TH_REST:
        case ONE_32ND_REST:
        case ONE_64TH_REST:
        case ONE_128TH_REST:
            return new RestInter(null, shape, grade, null, null); // No visit

        // Ottava ???
        //
        // Dynamics
        case DYNAMICS_P:
        case DYNAMICS_PP:
        case DYNAMICS_MP:
        case DYNAMICS_F:
        case DYNAMICS_FF:
        case DYNAMICS_MF:
        case DYNAMICS_FP:
        case DYNAMICS_SF:
        case DYNAMICS_SFZ:
        case CRESCENDO:
        case DIMINUENDO:
            return new DynamicsInter(null, shape, grade); // No visit

        // Ornaments
        case GRACE_NOTE_SLASH:
        case GRACE_NOTE:
        case TR:
        case TURN:
        case TURN_INVERTED:
        case TURN_UP:
        case TURN_SLASH:
        case MORDENT:
        case MORDENT_INVERTED:
            return new OrnamentInter(null, shape, grade); // No visit

        // Plucked techniques
        case ARPEGGIATO:
            return new ArpeggiatoInter(null, grade);

        // Keyboards
        case PEDAL_MARK:
        case PEDAL_UP_MARK:
            return new PedalInter(null, shape, grade); // No visit

        // Tuplets
        case TUPLET_THREE:
        case TUPLET_SIX:
            return new TupletInter(null, shape, grade); // No visit

        // Fingering
        case DIGIT_0:
        case DIGIT_1:
        case DIGIT_2:
        case DIGIT_3:
        case DIGIT_4:

            if (ShapeSet.supportFingerings()) {
                return new FingeringInter(null, shape, grade); // No visit
            } else {
                return null;
            }

        case PLUCK_P:
        case PLUCK_I:
        case PLUCK_M:
        case PLUCK_A:

            if (ShapeSet.supportPluckings()) {
                return new PluckingInter(null, shape, grade); // No visit
            } else {
                return null;
            }

        // Romans
        case ROMAN_I:
        case ROMAN_II:
        case ROMAN_III:
        case ROMAN_IV:
        case ROMAN_V:
        case ROMAN_VI:
        case ROMAN_VII:
        case ROMAN_VIII:
        case ROMAN_IX:
        case ROMAN_X:
        case ROMAN_XI:
        case ROMAN_XII:

            if (ShapeSet.supportFrets()) {
                return new FretInter(null, shape, grade); // No visit
            } else {
                return null;
            }

        // Curves
        case SLUR:
            return new SlurInter(grade);

        // Text
        case LYRICS:
            return new SentenceInter(grade);

        case TEXT:
            return new WordInter(grade);

        // Others
        default:

            String msg = "No ghost instance for " + shape;
            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }
    }

    //-----------//
    // getSystem //
    //-----------//
    public SystemInfo getSystem ()
    {
        return system;
    }

    //---------------//
    // getSystemBars //
    //---------------//
    public List<Inter> getSystemBars ()
    {
        if (systemBars == null) {
            systemBars = sig.inters(BarlineInter.class);
            Collections.sort(systemBars, Inters.byAbscissa);
        }

        return systemBars;
    }

    //----------------//
    // getSystemRests //
    //----------------//
    public List<Inter> getSystemRests ()
    {
        if (systemRests == null) {
            systemRests = sig.inters(RestInter.class);
            Collections.sort(systemRests, Inters.byAbscissa);
        }

        return systemRests;
    }

    //------------//
    // lateChecks //
    //------------//
    public void lateChecks ()
    {
        // Conflicting dot interpretations
        dotFactory.lateDotChecks();

        // Column consistency of Time Signatures in a system
        handleTimes();
    }

    //---------------------//
    // getSystemHeadChords //
    //---------------------//
    List<Inter> getSystemHeadChords ()
    {
        return systemHeadChords;
    }

    //----------------//
    // getSystemHeads //
    //----------------//
    List<Inter> getSystemHeads ()
    {
        return systemHeads;
    }

    //----------------//
    // getSystemNotes //
    //----------------//
    List<Inter> getSystemNotes ()
    {
        if (systemNotes == null) {
            systemNotes = new ArrayList<Inter>(getSystemHeads().size() + getSystemRests().size());
            systemNotes.addAll(getSystemHeads());
            systemNotes.addAll(getSystemRests());
            Collections.sort(systemNotes, Inters.byAbscissa);
        }

        return systemNotes;
    }

    //-----------//
    // addSymbol //
    //-----------//
    /**
     * Add the provided inter to the SIG, and make sure its glyph if any is registered.
     *
     * @param inter the created inter to add to SIG (perhaps null)
     */
    private void addSymbol (Inter inter)
    {
        if (inter == null) {
            return;
        }

        sig.addVertex(inter);
    }

    //-------------//
    // handleTimes //
    //-------------//
    /**
     * Handle time symbols outside of system header.
     * <p>
     * Isolated time symbols found outside of system header lead to the retrieval of a column of
     * time signatures.
     */
    private void handleTimes ()
    {
        // Retrieve all time symbols (outside staff headers)
        List<Inter> systemTimes = sig.inters(
                new Class[]{TimeWholeInter.class, // Whole symbol like C or 6/8
                            TimeNumberInter.class}); // Partial symbol like 6 or 8
        List<Inter> headerTimes = new ArrayList<Inter>();

        for (Inter inter : systemTimes) {
            Staff staff = inter.getStaff();

            if (inter.getCenter().x < staff.getHeaderStop()) {
                headerTimes.add(inter);
            }
        }

        systemTimes.removeAll(headerTimes);

        if (systemTimes.isEmpty()) {
            return;
        }

        // Dispatch these time symbols into their containing stack
        Map<MeasureStack, Set<Inter>> timeMap = new TreeMap<MeasureStack, Set<Inter>>(
                new Comparator<MeasureStack>()
        {
            @Override
            public int compare (MeasureStack s1,
                                MeasureStack s2)
            {
                return Integer.compare(s1.getIdValue(), s2.getIdValue());
            }
        });

        for (Inter inter : systemTimes) {
            MeasureStack stack = system.getMeasureStackAt(inter.getCenter());
            Set<Inter> stackSet = timeMap.get(stack);

            if (stackSet == null) {
                timeMap.put(stack, stackSet = new LinkedHashSet<Inter>());
            }

            stackSet.add(inter);
        }

        // Finally, scan each stack populated with some time sig(s)
        for (Entry<MeasureStack, Set<Inter>> entry : timeMap.entrySet()) {
            MeasureStack stack = entry.getKey();
            TimeBuilder.BasicColumn column = new TimeBuilder.BasicColumn(stack, entry.getValue());
            int res = column.retrieveTime();

            // If the stack does have a validated time sig, discard overlapping stuff right now!
            if (res != -1) {
                final Collection<AbstractTimeInter> times = column.getTimeInters().values();
                final Rectangle columnBox = Inters.getBounds(times);
                List<Inter> neighbors = sig.inters(
                        new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        return inter.getBounds().intersects(columnBox)
                               && !(inter instanceof InterEnsemble);
                    }
                });

                neighbors.removeAll(times);

                for (AbstractTimeInter time : times) {
                    for (Iterator<Inter> it = neighbors.iterator(); it.hasNext();) {
                        Inter neighbor = it.next();

                        try {
                            if (neighbor.overlaps(time)) {
                                logger.debug("Deleting time overlapping {}", neighbor);
                                neighbor.remove();
                                it.remove();
                            }
                        } catch (DeletedInterException ignored) {
                        }
                    }
                }
            }
        }
    }
}
