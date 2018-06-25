//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T i m e B u i l d e r                                     //
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
package org.audiveris.omr.sheet.time;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.score.TimeValue;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.InterlineScale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.relation.Exclusion;
import org.audiveris.omr.sig.relation.TimeTopBottomRelation;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code TimeBuilder} is the abstract basis for handling a time signature (such
 * as 4/4 or C) from a staff.
 * <p>
 * Subclass {@link HeaderTimeBuilder} is used at the beginning of a staff (in staff header) while
 * subclass {@link BasicTimeBuilder} is used farther down the staff.
 * <p>
 * <img src="doc-files/TimeBuilder.png" alt="TimeBuilder UML">
 *
 * @author Hervé Bitteur
 */
public abstract class TimeBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TimeBuilder.class);

    /** Possible shapes for whole time signatures. */
    protected static final EnumSet<Shape> wholeShapes = ShapeSet.WholeTimes;

    /** Possible shapes for top or bottom half of time signatures. */
    protected static final EnumSet<Shape> halfShapes = EnumSet.copyOf(ShapeSet.PartialTimes);

    //~ Enumerations -------------------------------------------------------------------------------
    /**
     * The different parts of a time signature.
     */
    public static enum TimeKind
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Whole signature (common or cut or combo). */
        WHOLE,
        /** Upper half (numerator number). */
        NUM,
        /** Lower half (denominator number). */
        DEN;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Dedicated staff to analyze. */
    protected final Staff staff;

    /** The column manager. */
    protected final TimeColumn column;

    /** The containing system. */
    @Navigable(false)
    protected final SystemInfo system;

    /** The related SIG. */
    protected final SIGraph sig;

    /** Related scale. */
    protected final Scale scale;

    /** Scale-dependent parameters. */
    protected final Parameters params;

    /** whole candidates. (common or cut or combo like 6/8 ...) */
    protected final List<Inter> wholes = new ArrayList<Inter>();

    /** Top half candidates. (6/ ...) */
    protected final List<Inter> nums = new ArrayList<Inter>();

    /** Bottom half candidates. (/8 ...) */
    protected final List<Inter> dens = new ArrayList<Inter>();

    /** The time inter instance chosen for the staff. */
    protected AbstractTimeInter timeInter;

    /** All glyphs submitted to classifier. */
    protected final Set<Glyph> glyphCandidates = new LinkedHashSet<Glyph>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimeBuilder} object.
     *
     * @param staff  the underlying staff
     * @param column the column manager
     */
    public TimeBuilder (Staff staff,
                        TimeColumn column)
    {
        this.staff = staff;
        this.column = column;

        system = staff.getSystem();
        sig = system.getSig();
        scale = system.getSheet().getScale();

        params = new Parameters(scale, staff.getSpecificInterline());
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * This is called when we discover that a candidate is wrong,
     * so that all related data inserted in sig is removed.
     */
    public abstract void cleanup ();

    //--------------//
    // getTimeInter //
    //--------------//
    /**
     * Report the time sig instance, if any, for the staff.
     *
     * @return the timeInter or null
     */
    public AbstractTimeInter getTimeInter ()
    {
        return timeInter;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return getClass().getSimpleName() + "#" + staff.getId();
    }

    //---------------//
    // createTimeSig //
    //---------------//
    /**
     * Actually assign the time signature to the staff.
     *
     * @param bestTimeInter the time inter instance for this staff
     */
    protected void createTimeSig (AbstractTimeInter bestTimeInter)
    {
        timeInter = bestTimeInter;

        if (bestTimeInter != null) {
            bestTimeInter.setStaff(staff);

            final GlyphIndex index = system.getSheet().getGlyphIndex();

            if (bestTimeInter instanceof TimePairInter) {
                TimePairInter pair = (TimePairInter) bestTimeInter;

                for (Inter inter : pair.getMembers()) {
                    if (inter.getGlyph() != null) {
                        inter.setGlyph(index.registerOriginal(inter.getGlyph()));
                    }
                }
            } else {
                if (bestTimeInter.getGlyph() != null) {
                    bestTimeInter.setGlyph(index.registerOriginal(bestTimeInter.getGlyph()));
                }
            }
        }
    }

    /**
     * Discard all inters that do not pertain to chosen time signature.
     * <p>
     * This accounts for other WholeTimeInter instances and to all TimeNumberInter instances
     * that do not correspond to chosen num/den.
     */
    protected void discardOthers ()
    {
        final Map<Shape, Glyph> compatibles = new EnumMap<Shape, Glyph>(Shape.class);

        if (timeInter instanceof TimeWholeInter) {
            // It's a whole sig: COMMON_TIME or CUT_TIME or combo
            glyphCandidates.remove(timeInter.getGlyph()); // Positive

            // Check wholes
            for (Inter inter : wholes) {
                if (inter.getShape() != timeInter.getShape()) {
                    inter.remove();
                }
            }

            // Discard numbers
            if (ShapeSet.SingleWholeTimes.contains(timeInter.getShape())) {
                // Chosen is COMMON or CUT, so delete any pair member
                for (VerticalSide side : VerticalSide.values()) {
                    final List<Inter> members = (side == VerticalSide.TOP) ? nums : dens;

                    for (Inter inter : members) {
                        inter.remove();
                    }
                }
            } else {
                // Chosen is a combo 3/4, 6/8, ...
                for (VerticalSide side : VerticalSide.values()) {
                    final List<Inter> members = (side == VerticalSide.TOP) ? nums : dens;
                    final int value = (side == VerticalSide.TOP) ? timeInter.getNumerator()
                            : timeInter.getDenominator();

                    for (Inter inter : members) {
                        TimeNumberInter number = (TimeNumberInter) inter;

                        if (number.getValue() == value) {
                            compatibles.put(number.getShape(), inter.getGlyph()); // Compatible member
                        }

                        inter.remove();
                    }
                }
            }
        } else {
            // It's a pair of numbers
            final TimePairInter chosenPair = (TimePairInter) timeInter;
            final TimeValue value = timeInter.getValue();

            // Discard wholes
            for (Inter inter : wholes) {
                TimeWholeInter time = (TimeWholeInter) inter;

                if (time.getValue().equals(value)) {
                    compatibles.put(inter.getShape(), inter.getGlyph()); // Compatible whole
                }

                inter.remove();
            }

            // Check numbers
            for (VerticalSide side : VerticalSide.values()) {
                final List<Inter> members = (side == VerticalSide.TOP) ? nums : dens;
                final TimeNumberInter chosenNumber = chosenPair.getMember(side);
                glyphCandidates.remove(chosenNumber.getGlyph()); // Positive

                for (Inter inter : members) {
                    if (inter != chosenNumber) {
                        // Remove the pair this number is part of, if any
                        for (Inter ensemble : inter.getAllEnsembles()) {
                            ensemble.remove();
                        }

                        // Remove this member as well, if not yet done
                        if (!inter.isRemoved()) {
                            inter.remove();
                        }
                    }
                }
            }
        }

        glyphCandidates.removeAll(compatibles.values());
    }

    /**
     * Retrieve all acceptable candidates (whole or half) for this staff.
     * <p>
     * All candidates are stored as Inter instances in system sig, and in dedicated builder lists
     * (wholes, nums or dens).
     */
    protected abstract void findCandidates ();

    //------------------//
    // filterCandidates //
    //------------------//
    /**
     * Filter the candidates found for this staff.
     * <p>
     * Some basic tests are run at staff level to purge candidates
     * (every 'num' item must have a compatible 'den' item and vice versa).
     *
     * @return true if success, false otherwise (when no suitable candidate at all has been left)
     */
    protected boolean filterCandidates ()
    {
        // Establish all possible num/den pairs
        for (Inter topInter : nums) {
            TimeNumberInter top = (TimeNumberInter) topInter;
            final int topX = top.getCenter().x;

            for (Inter bottomInter : dens) {
                TimeNumberInter bottom = (TimeNumberInter) bottomInter;

                // Make sure top & bottom are abscissa-wise compatible
                int dx = bottom.getCenter().x - topX;

                if (Math.abs(dx) <= params.maxHalvesDx) {
                    // Restrict num/den pairs to supported combinations
                    TimeRational nd = new TimeRational(top.getValue(), bottom.getValue());

                    if (AbstractTimeInter.isSupported(nd)) {
                        // Halves support each other
                        sig.addEdge(top, bottom, new TimeTopBottomRelation());
                    } else {
                        sig.insertExclusion(top, bottom, Exclusion.Cause.INCOMPATIBLE);
                    }
                }
            }
        }

        // Make sure each half has a compatible partnering half
        for (List<Inter> list : Arrays.asList(nums, dens)) {
            for (Iterator<Inter> it = list.iterator(); it.hasNext();) {
                Inter inter = it.next();

                if (!sig.hasRelation(inter, TimeTopBottomRelation.class)) {
                    inter.remove();
                    it.remove();
                }
            }
        }

        // Compute contextual grades of nums & dens
        for (List<Inter> list : Arrays.asList(nums, dens)) {
            for (Inter inter : list) {
                sig.computeContextualGrade(inter);
            }
        }

        return !wholes.isEmpty() || !nums.isEmpty();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // Parameters //
    //------------//
    /**
     * Some parameters depend on global sheet scale but some depend on staff specific
     * scale when the sheet contains staves on different sizes (small and large).
     */
    protected static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int maxEvalRank;

        // Sheet scale dependent
        //----------------------
        //
        final int roiWidth;

        final int maxFirstSpaceWidth;

        final int maxInnerSpace;

        // Staff scale dependent
        //----------------------
        //
        final int yMargin;

        final int minTimeWidth;

        final int maxTimeWidth;

        final int maxHalvesDx;

        final double maxPartGap;

        final int minWholeTimeWeight;

        final int minHalfTimeWeight;

        final int maxSpaceCumul;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale,
                           int staffSpecific)
        {
            maxEvalRank = constants.maxEvalRank.getValue();

            {
                // Use sheet large interline scale
                final InterlineScale large = scale.getInterlineScale();
                roiWidth = large.toPixels(constants.roiWidth);
                maxFirstSpaceWidth = large.toPixels(constants.maxFirstSpaceWidth);
                maxInnerSpace = large.toPixels(constants.maxInnerSpace);
            }

            {
                // Use staff specific interline value
                final InterlineScale specific = scale.getInterlineScale(staffSpecific);
                yMargin = specific.toPixels(constants.yMargin);
                minTimeWidth = specific.toPixels(constants.minTimeWidth);
                maxTimeWidth = specific.toPixels(constants.maxTimeWidth);
                maxHalvesDx = specific.toPixels(constants.maxHalvesDx);
                maxPartGap = specific.toPixels(constants.maxPartGap);
                minWholeTimeWeight = specific.toPixels(constants.minWholeTimeWeight);
                minHalfTimeWeight = specific.toPixels(constants.minHalfTimeWeight);
                maxSpaceCumul = specific.toPixels(constants.maxSpaceCumul);
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

        private final Constant.Integer maxEvalRank = new Constant.Integer(
                "none",
                3,
                "Maximum acceptable rank in time evaluation");

        private final Scale.Fraction roiWidth = new Scale.Fraction(
                4.0,
                "Width of region of interest for time signature");

        private final Scale.Fraction yMargin = new Scale.Fraction(
                0.10,
                "Vertical white margin on raw rectangle");

        private final Scale.Fraction minTimeWidth = new Scale.Fraction(
                1.0,
                "Minimum width for a time signature");

        private final Scale.Fraction maxTimeWidth = new Scale.Fraction(
                2.0,
                "Maximum width for a time signature");

        private final Scale.Fraction maxHalvesDx = new Scale.Fraction(
                1,
                "Maximum abscissa shift between top & bottom halves of a time signature");

        private final Scale.Fraction maxPartGap = new Scale.Fraction(
                1.0,
                "Maximum distance between two glyph parts of a single time symbol");

        private final Scale.AreaFraction minWholeTimeWeight = new Scale.AreaFraction(
                1.0,
                "Minimum weight for a whole time signature");

        private final Scale.AreaFraction minHalfTimeWeight = new Scale.AreaFraction(
                0.75,
                "Minimum weight for a half time signature");

        private final Scale.Fraction maxSpaceCumul = new Scale.Fraction(
                0.4,
                "Maximum cumul value in space");

        // Beware: A too small value might miss the whole time-sig
        private final Scale.Fraction maxFirstSpaceWidth = new Scale.Fraction(
                2.5,
                "Maximum initial space before time signature");

        // Beware: A too small value might miss some time-sig items
        private final Scale.Fraction maxInnerSpace = new Scale.Fraction(
                0.5,
                "Maximum inner space within time signature");
    }
}
