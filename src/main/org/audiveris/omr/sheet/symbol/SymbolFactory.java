//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l F a c t o r y                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;

import static org.audiveris.omr.glyph.Shape.*;
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
import org.audiveris.omr.sig.inter.InterMutableEnsemble;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.MarkerInter;
import org.audiveris.omr.sig.inter.OrnamentInter;
import org.audiveris.omr.sig.inter.PedalInter;
import org.audiveris.omr.sig.inter.PluckingInter;
import org.audiveris.omr.sig.inter.RepeatDotInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SmallFlagInter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.TupletInter;
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

    private static final Constants constants = new Constants();

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
        Collections.sort(systemStems, Inter.byAbscissa);

        systemHeads = sig.inters(HeadInter.class);
        Collections.sort(systemHeads, Inter.byAbscissa);

        systemHeadChords = sig.inters(AbstractChordInter.class);
        Collections.sort(systemHeadChords, Inter.byAbscissa);

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
        final double grade = Inter.intrinsicRatio * eval.grade;

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
        } else if (constants.supportFingerings.isSet() && Digits.contains(shape)) {
            addSymbol(new FingeringInter(glyph, shape, grade));
        } else if (constants.supportFrets.isSet() && Romans.contains(shape)) {
            addSymbol(new FretInter(glyph, shape, grade));
        } else if (constants.supportPluckings.isSet() && Pluckings.contains(shape)) {
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
            return new KeyInter(null, grade, shape, null);

        // Brace, bracket ???
        //
        // Barlines ???
        //
        // Beams ???
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
            return new ClefInter(null, shape, grade, null, null, null);

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

        // Stem ???
        //
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
            return new FermataInter(null, null, shape, grade, null); // No visit

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

            if (constants.supportFingerings.isSet()) {
                return new FingeringInter(null, shape, grade); // No visit
            } else {
                return null;
            }

        case PLUCK_P:
        case PLUCK_I:
        case PLUCK_M:
        case PLUCK_A:

            if (constants.supportPluckings.isSet()) {
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

            if (constants.supportFrets.isSet()) {
                return new FretInter(null, shape, grade); // No visit
            } else {
                return null;
            }

        default:

            String msg = "No moving Inter class for " + shape;
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
            Collections.sort(systemBars, Inter.byAbscissa);
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
            Collections.sort(systemRests, Inter.byAbscissa);
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
                               && !(inter instanceof InterMutableEnsemble);
                    }
                });

                neighbors.removeAll(times);

                for (AbstractTimeInter time : times) {
                    for (Iterator<Inter> it = neighbors.iterator(); it.hasNext();) {
                        Inter neighbor = it.next();

                        try {
                            if (neighbor.overlaps(time)) {
                                logger.debug("Deleting time overlapping {}", neighbor);
                                neighbor.delete();
                                it.remove();
                            }
                        } catch (DeletedInterException ignored) {
                        }
                    }
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean supportPluckings = new Constant.Boolean(
                false,
                "Should we support Pluckings (guitar right-hand)?");

        private final Constant.Boolean supportFingerings = new Constant.Boolean(
                false,
                "Should we support Fingerings (guitar left-hand)?");

        private final Constant.Boolean supportFrets = new Constant.Boolean(
                false,
                "Should we support Fret indications (guitar left-hand)?");
    }
}
//
//    //------------------//
//    // createGhostInter //
//    //------------------//
//    /**
//     * Create a ghost instance (its location is meant to evolve) for a given shape.
//     *
//     * @param shape the provided shape
//     * @return proper ghost instance
//     */
//    public static Inter createGhostInter (Shape shape)
//    {
//        Class<? extends Inter> classe = forShape.get(shape);
//
//        if (classe == null) {
//            String msg = "No ghost Inter class for " + shape;
//            logger.error(msg);
//            throw new IllegalArgumentException(msg);
//        }
//
//        try {
//            Inter instance = null;
//
//            // Try using 'createGhost()' method if any
//            try {
//                Method create = classe.getDeclaredMethod(
//                        "createGhost",
//                        new Class[]{Shape.class});
//
//                if (Modifier.isStatic(create.getModifiers())) {
//                    instance = (Inter) create.invoke(shape);
//                }
//            } catch (NoSuchMethodException ignored) {
//            }
//
//            if (instance == null) {
//                // Fall back to allocate a new class instance
//                // What about shape???
//                instance = classe.newInstance();
//            }
//
//            return instance;
//        } catch (Throwable ex) {
//            logger.error("Cannot create ghost inter for shape {}", shape, ex);
//
//            return null;
//        }
//    }
//

//    private static final Map<Shape, Class<? extends Inter>> forShape = buildShapeMap();
//
//    //---------------//
//    // buildShapeMap //
//    //---------------//
//    /**
//     * Report the Inter subclass that handles the provided shape.
//     *
//     * @param shape provided shape
//     * @return corresponding class or null
//     */
//    private static Map<Shape, Class<? extends Inter>> buildShapeMap ()
//    {
//        Map<Shape, Class<? extends Inter>> map = new EnumMap<Shape, Class<? extends Inter>>(
//                Shape.class);
//
//        // Key signatures
//        map.put(KEY_FLAT_1, KeyInter.class);
//        map.put(KEY_FLAT_2, KeyInter.class);
//        map.put(KEY_FLAT_3, KeyInter.class);
//        map.put(KEY_FLAT_4, KeyInter.class);
//        map.put(KEY_FLAT_5, KeyInter.class);
//        map.put(KEY_FLAT_6, KeyInter.class);
//        map.put(KEY_FLAT_7, KeyInter.class);
//        map.put(KEY_SHARP_1, KeyInter.class);
//        map.put(KEY_SHARP_2, KeyInter.class);
//        map.put(KEY_SHARP_3, KeyInter.class);
//        map.put(KEY_SHARP_4, KeyInter.class);
//        map.put(KEY_SHARP_5, KeyInter.class);
//        map.put(KEY_SHARP_6, KeyInter.class);
//        map.put(KEY_SHARP_7, KeyInter.class);
//        // Brace, bracket ???
//        //
//        // Barlines ???
//        //
//        // Repeats
//        map.put(REPEAT_DOT, RepeatDotInter.class);
//
//        // Markers
//        map.put(CODA, MarkerInter.class);
//        map.put(SEGNO, MarkerInter.class);
//        map.put(DAL_SEGNO, MarkerInter.class);
//        map.put(DA_CAPO, MarkerInter.class);
//
//        // Clefs
//        map.put(G_CLEF, ClefInter.class);
//        map.put(G_CLEF_SMALL, ClefInter.class);
//        map.put(G_CLEF_8VA, ClefInter.class);
//        map.put(G_CLEF_8VB, ClefInter.class);
//        map.put(F_CLEF, ClefInter.class);
//        map.put(F_CLEF_SMALL, ClefInter.class);
//        map.put(F_CLEF_8VA, ClefInter.class);
//        map.put(F_CLEF_8VB, ClefInter.class);
//        map.put(C_CLEF, ClefInter.class);
//        map.put(PERCUSSION_CLEF, ClefInter.class);
//
//        // Time sig
//        map.put(TIME_ZERO, TimeNumberInter.class);
//        map.put(TIME_ONE, TimeNumberInter.class);
//        map.put(TIME_TWO, TimeNumberInter.class);
//        map.put(TIME_THREE, TimeNumberInter.class);
//        map.put(TIME_FOUR, TimeNumberInter.class);
//        map.put(TIME_FIVE, TimeNumberInter.class);
//        map.put(TIME_SIX, TimeNumberInter.class);
//        map.put(TIME_SEVEN, TimeNumberInter.class);
//        map.put(TIME_EIGHT, TimeNumberInter.class);
//        map.put(TIME_NINE, TimeNumberInter.class);
//        map.put(TIME_TWELVE, TimeNumberInter.class);
//        map.put(TIME_SIXTEEN, TimeNumberInter.class);
//
//        map.put(COMMON_TIME, TimeWholeInter.class);
//        map.put(CUT_TIME, TimeWholeInter.class);
//        map.put(TIME_FOUR_FOUR, TimeWholeInter.class);
//        map.put(TIME_TWO_TWO, TimeWholeInter.class);
//        map.put(TIME_TWO_FOUR, TimeWholeInter.class);
//        map.put(TIME_THREE_FOUR, TimeWholeInter.class);
//        map.put(TIME_FIVE_FOUR, TimeWholeInter.class);
//        map.put(TIME_THREE_EIGHT, TimeWholeInter.class);
//        map.put(TIME_SIX_EIGHT, TimeWholeInter.class);
//
//        // Noteheads
//        map.put(NOTEHEAD_BLACK, HeadInter.class);
//        map.put(NOTEHEAD_BLACK_SMALL, HeadInter.class);
//        map.put(NOTEHEAD_VOID, HeadInter.class);
//        map.put(NOTEHEAD_VOID_SMALL, HeadInter.class);
//        map.put(BREVE, HeadInter.class);
//        map.put(WHOLE_NOTE, HeadInter.class);
//        map.put(WHOLE_NOTE_SMALL, HeadInter.class);
//
//        map.put(AUGMENTATION_DOT, AugmentationDotInter.class);
//
//        // Ledger
//        map.put(LEDGER, LedgerInter.class);
//
//        // Stem ???
//        //
//        // Flags
//        map.put(FLAG_1, FlagInter.class);
//        map.put(FLAG_2, FlagInter.class);
//        map.put(FLAG_3, FlagInter.class);
//        map.put(FLAG_4, FlagInter.class);
//        map.put(FLAG_5, FlagInter.class);
//        map.put(FLAG_1_UP, FlagInter.class);
//        map.put(FLAG_2_UP, FlagInter.class);
//        map.put(FLAG_3_UP, FlagInter.class);
//        map.put(FLAG_4_UP, FlagInter.class);
//        map.put(FLAG_5_UP, FlagInter.class);
//        map.put(SMALL_FLAG, FlagInter.class);
//        map.put(SMALL_FLAG_SLASH, FlagInter.class);
//
//        // Accidentals
//        map.put(FLAT, AlterInter.class);
//        map.put(NATURAL, AlterInter.class);
//        map.put(SHARP, AlterInter.class);
//        map.put(DOUBLE_SHARP, AlterInter.class);
//        map.put(DOUBLE_FLAT, AlterInter.class);
//
//        // Articulations
//        map.put(ACCENT, ArticulationInter.class);
//        map.put(TENUTO, ArticulationInter.class);
//        map.put(STACCATO, ArticulationInter.class);
//        map.put(STACCATISSIMO, ArticulationInter.class);
//        map.put(STRONG_ACCENT, ArticulationInter.class);
//
//        // Holds
//        map.put(FERMATA, FermataInter.class);
//        map.put(FERMATA_BELOW, FermataInter.class);
//        map.put(FERMATA_DOT, FermataDotInter.class);
//        map.put(CAESURA, CaesuraInter.class);
//        map.put(BREATH_MARK, BreathMarkInter.class);
//
//        // Rests
//        map.put(LONG_REST, RestInter.class);
//        map.put(BREVE_REST, RestInter.class);
//        map.put(WHOLE_REST, RestInter.class);
//        map.put(HALF_REST, RestInter.class);
//        map.put(QUARTER_REST, RestInter.class);
//        map.put(EIGHTH_REST, RestInter.class);
//        map.put(ONE_16TH_REST, RestInter.class);
//        map.put(ONE_32ND_REST, RestInter.class);
//        map.put(ONE_64TH_REST, RestInter.class);
//        map.put(ONE_128TH_REST, RestInter.class);
//
//        // Ottava ???
//        //
//        // Dynamics
//        map.put(DYNAMICS_P, DynamicsInter.class);
//        map.put(DYNAMICS_PP, DynamicsInter.class);
//        map.put(DYNAMICS_MP, DynamicsInter.class);
//        map.put(DYNAMICS_F, DynamicsInter.class);
//        map.put(DYNAMICS_FF, DynamicsInter.class);
//        map.put(DYNAMICS_MF, DynamicsInter.class);
//        map.put(DYNAMICS_FP, DynamicsInter.class);
//        map.put(DYNAMICS_SF, DynamicsInter.class);
//        map.put(DYNAMICS_SFZ, DynamicsInter.class);
//        map.put(CRESCENDO, DynamicsInter.class);
//        map.put(DIMINUENDO, DynamicsInter.class);
//
//        // Ornaments
//        map.put(GRACE_NOTE_SLASH, OrnamentInter.class);
//        map.put(GRACE_NOTE, OrnamentInter.class);
//        map.put(TR, OrnamentInter.class);
//        map.put(TURN, OrnamentInter.class);
//        map.put(TURN_INVERTED, OrnamentInter.class);
//        map.put(TURN_UP, OrnamentInter.class);
//        map.put(TURN_SLASH, OrnamentInter.class);
//        map.put(MORDENT, OrnamentInter.class);
//        map.put(MORDENT_INVERTED, OrnamentInter.class);
//
//        // Plucked techniques
//        map.put(ARPEGGIATO, ArpeggiatoInter.class);
//
//        // Keyboards
//        map.put(PEDAL_MARK, PedalInter.class);
//        map.put(PEDAL_UP_MARK, PedalInter.class);
//
//        // Tuplets
//        map.put(TUPLET_THREE, TupletInter.class);
//        map.put(TUPLET_SIX, TupletInter.class);
//
//        // Fingering
//        map.put(DIGIT_0, FingeringInter.class);
//        map.put(DIGIT_1, FingeringInter.class);
//        map.put(DIGIT_2, FingeringInter.class);
//        map.put(DIGIT_3, FingeringInter.class);
//        map.put(DIGIT_4, FingeringInter.class);
//
//        // Plucking
//        map.put(PLUCK_P, PluckingInter.class);
//        map.put(PLUCK_I, PluckingInter.class);
//        map.put(PLUCK_M, PluckingInter.class);
//        map.put(PLUCK_A, PluckingInter.class);
//
//        // Romans
//        map.put(ROMAN_I, FretInter.class);
//        map.put(ROMAN_II, FretInter.class);
//        map.put(ROMAN_III, FretInter.class);
//        map.put(ROMAN_IV, FretInter.class);
//        map.put(ROMAN_V, FretInter.class);
//        map.put(ROMAN_VI, FretInter.class);
//        map.put(ROMAN_VII, FretInter.class);
//        map.put(ROMAN_VIII, FretInter.class);
//        map.put(ROMAN_IX, FretInter.class);
//        map.put(ROMAN_X, FretInter.class);
//        map.put(ROMAN_XI, FretInter.class);
//        map.put(ROMAN_XII, FretInter.class);
//
//        return map;
//    }
