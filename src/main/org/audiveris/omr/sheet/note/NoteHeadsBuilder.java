//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 N o t e H e a d s B u i l d e r                                //
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
package org.audiveris.omr.sheet.note;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.glyph.Symbol;
import org.audiveris.omr.image.Anchored.Anchor;

import static org.audiveris.omr.image.Anchored.Anchor.*;

import org.audiveris.omr.image.DistanceTable;
import org.audiveris.omr.image.PixelDistance;
import org.audiveris.omr.image.ShapeDescriptor;
import org.audiveris.omr.image.Template;
import org.audiveris.omr.image.TemplateFactory;
import org.audiveris.omr.image.TemplateFactory.Catalog;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoPath;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.NaturalSpline;
import org.audiveris.omr.math.ReversePathIterator;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractInter;
import org.audiveris.omr.sig.inter.AbstractVerticalInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.util.Dumping;

import static org.audiveris.omr.util.HorizontalSide.*;

import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Predicate;
import org.audiveris.omr.util.StopWatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class {@code NoteHeadsBuilder} retrieves the void note heads, the black note heads,
 * the grace notes and the whole notes for a system.
 * <p>
 * It uses a distance matching approach which works well for such symbols that exhibit a fixed
 * shape, with a combination of foreground and background information.
 * <p>
 * We don't need to check each and every location in the system, but only the locations where such
 * note kind is possible:<ul>
 * <li>We can stick to staff lines and ledgers locations.</li>
 * <li>We cannot fully use stems, since at this time we just have vertical seeds and not all stems
 * will contain seeds. However, if a vertical seed exists nearby we can use it to evaluate a note
 * candidate at proper location.</li>
 * <li>We can reasonably skip the locations where a really good beam or a really good bar line has
 * been detected.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class NoteHeadsBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            NoteHeadsBuilder.class);

    /** Shapes of note head competitors. */
    private static final Set<Shape> COMPETING_SHAPES = EnumSet.copyOf(
            Arrays.asList(
                    Shape.THICK_BARLINE,
                    Shape.THIN_BARLINE,
                    Shape.THICK_CONNECTOR,
                    Shape.THIN_CONNECTOR,
                    Shape.BEAM,
                    Shape.BEAM_HOOK,
                    Shape.BEAM_SMALL,
                    Shape.BEAM_HOOK_SMALL));

    /** Specific value for no offsets. */
    private static final int[] NO_OFFSETS = new int[]{0};

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    @Navigable(false)
    private final SIGraph sig;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Sheet scale. */
    @Navigable(false)
    private final Scale scale;

    /** The distance table to use. */
    private final DistanceTable distances;

    /** The note-oriented spots for this system. */
    private final List<Glyph> systemSpots;

    /** Scale-dependent constants. */
    private final Parameters params;

    /** Minimum width of templates. */
    private int minTemplateWidth = 0; // TODO

    /** The <b>properly scaled</b> templates to use, based on <b>current</b> staff. */
    private Catalog catalog;

    /** The competing interpretations for the system. */
    private List<Inter> systemCompetitors;

    /** The vertical (stem) seeds for the system. */
    private List<Glyph> systemSeeds;

    /** The image used to retrieve underlying glyphs. */
    private ByteProcessor image;

    /** Offsets tried around a given (stem-based) abscissa. */
    private final int[] xOffsets;

    // Debug
    private final Perf seedsPerf = new Perf();

    private final Perf rangePerf = new Perf();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code NoteHeadsBuilder} object.
     *
     * @param system      the system to process
     * @param distances   the distance table
     * @param systemSpots spots detected for this system
     */
    public NoteHeadsBuilder (SystemInfo system,
                             DistanceTable distances,
                             List<Glyph> systemSpots)
    {
        this.system = system;
        this.distances = distances;
        this.systemSpots = systemSpots;

        sig = system.getSig();
        sheet = system.getSheet();
        scale = sheet.getScale();

        params = new Parameters(scale);

        if ((system.getId() == 1) && constants.printParameters.isSet()) {
            new Dumping().dump(params);
        }

        // Compute window in x
        xOffsets = computeXOffsets();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // buildHeads //
    //------------//
    /**
     * Retrieve all void heads, black heads and whole notes in the system
     * both for standard and small (cue/grace) sizes.
     */
    public void buildHeads ()
    {
        StopWatch watch = new StopWatch("buildHeads S#" + system.getId());
        systemCompetitors = getSystemCompetitors(); // Competitors
        systemSeeds = system.getGroupedGlyphs(Symbol.Group.VERTICAL_SEED); // Vertical seeds
        Collections.sort(systemSeeds, Glyphs.byOrdinate);
        Collections.sort(systemSpots, Glyphs.byOrdinate);
        image = sheet.getPicture().getSource(Picture.SourceKey.BINARY);

        for (Staff staff : system.getStaves()) {
            logger.debug("Staff #{}", staff.getId());

            // Determine the proper catalog, based on staff size
            final int pointSize = staff.getHeadPointSize();
            catalog = TemplateFactory.getInstance().getCatalog(pointSize);

            List<Inter> ch = new ArrayList<Inter>(); // Created Heads for this staff

            // First, process all seed-based heads for the staff
            watch.start("Staff #" + staff.getId() + " seed");
            ch.addAll(processStaff(staff, true));

            // Consider seed-based heads as special competitors for x-based notes
            systemCompetitors.addAll(ch);
            Collections.sort(systemCompetitors, Inters.byOrdinate);

            // Second, process x-based notes for the staff
            watch.start("Staff #" + staff.getId() + " range");
            ch.addAll(processStaff(staff, false));

            // Finally, detect notes overlaps for current staff
            Collections.sort(ch, Inters.byFullAbscissa);
            watch.start("Staff #" + staff.getId() + " duplicates");

            int duplicates = purgeDuplicates(ch);

            if (duplicates > 0) {
                logger.debug("Staff#{} {} duplicates", staff.getId(), duplicates);
            }

            for (Inter inter : ch) {
                // Boost head shapes that don't expect stem
                if (ShapeSet.StemLessHeads.contains(inter.getShape())) {
                    inter.increase(constants.wholeBoost.getValue());
                }

                // Keep created heads in staff
                staff.addNote((HeadInter) inter);
            }
        }

        if (constants.printWatch.isSet()) {
            watch.print();
        }

        logger.debug("S#{} seeds {}", system.getId(), seedsPerf);
        logger.debug("    range {}", rangePerf);
    }

    //------------------//
    // aggregateMatches //
    //------------------//
    private List<HeadInter> aggregateMatches (List<HeadInter> inters)
    {
        // Sort by decreasing grade
        Collections.sort(inters, Inters.byReverseGrade);

        // Gather matches per close locations
        // Avoid duplicate locations
        List<Aggregate> aggregates = new ArrayList<Aggregate>();

        for (HeadInter inter : inters) {
            Point loc = GeoUtil.centerOf(inter.getBounds());

            // Check among already filtered locations for similar location
            Aggregate aggregate = null;

            for (Aggregate ag : aggregates) {
                int dx = loc.x - ag.point.x;

                if (Math.abs(dx) <= params.maxTemplateDx) {
                    aggregate = ag;

                    break;
                }
            }

            if (aggregate == null) {
                aggregate = new Aggregate();
                aggregates.add(aggregate);
            }

            aggregate.add(inter);
        }

        List<HeadInter> filtered = new ArrayList<HeadInter>();

        for (Aggregate ag : aggregates) {
            filtered.add(ag.getMainInter());
        }

        return filtered;
    }

    //-----------------//
    // computeXOffsets //
    //-----------------//
    /**
     * Compute offsets to be tried around the abscissa of a stem.
     *
     * @return the sequence of offsets in x
     */
    private int[] computeXOffsets ()
    {
        //        // Use a window as wide as maxStem value, ensure odd value
        //        int length = sheet.getScale().getMaxStem();
        //
        //        if ((length % 2) == 0) {
        //            length++;
        //        }
        //
        //        int[] offsets = new int[length];
        //
        //        for (int i = 0; i < length; i++) {
        //            if ((i % 2) == 0) {
        //                offsets[i] = -(i / 2);
        //            } else {
        //                offsets[i] = ((i + 1) / 2);
        //            }
        //        }
        //
        //        return offsets;
        return NO_OFFSETS;
    }

    //-------------//
    // createInter //
    //-------------//
    /**
     * Create the interpretation that corresponds to the match found.
     *
     * @param loc    (valued) location of the match
     * @param anchor position of location WRT shape
     * @param shape  the shape tested
     * @param staff  the related staff
     * @param pitch  the note pitch
     * @return the inter created, if any
     */
    private HeadInter createInter (PixelDistance loc,
                                   Anchor anchor,
                                   Shape shape,
                                   Staff staff,
                                   double pitch)
    {
        final double distImpact = Template.impactOf(loc.d);
        final GradeImpacts impacts = new HeadInter.Impacts(distImpact);
        final double grade = impacts.getGrade();

        // Is grade acceptable?
        if (grade < AbstractInter.getMinGrade()) {
            return null;
        }

        final ShapeDescriptor desc = catalog.getDescriptor(shape);
        final Rectangle box = desc.getSymbolBoundsAt(loc.x, loc.y, anchor);
        final Point pivot = new Point(loc.x, loc.y);

        return new HeadInter(pivot, anchor, box, shape, impacts, staff, pitch);
    }

    //---------------------//
    // filterSeedConflicts //
    //---------------------//
    /**
     * Check the provided collection of x-based inter instances with
     * conflicting seed-based inter instances.
     *
     * @param inters      x-based instances
     * @param competitors all competitors, including seed-based inter instances
     * @return the filtered x-based instances
     */
    private List<HeadInter> filterSeedConflicts (List<HeadInter> inters,
                                                 List<Inter> competitors)
    {
        List<HeadInter> filtered = new ArrayList<HeadInter>();

        for (HeadInter inter : inters) {
            if (!overlapSeed(inter, competitors)) {
                filtered.add(inter);
            }
        }

        return filtered;
    }

    //---------------------//
    // getCompetitorsSlice //
    //---------------------//
    /**
     * Retrieve the competitors intersected by the provided horizontal slice.
     *
     * @param area the horizontal slice
     * @return the list of competitors, sorted by abscissa.
     */
    private List<Inter> getCompetitorsSlice (Area area)
    {
        List<Inter> rawComps = SIGraph.intersectedInters(
                systemCompetitors,
                GeoOrder.BY_ORDINATE,
                area);

        // Keep only the "really good" competitors
        List<Inter> kept = new ArrayList<Inter>();

        for (Inter inter : rawComps) {
            if (inter.isGood()) {
                kept.add(inter);
            }
        }

        // Sort by abscissa for more efficient lookup
        Collections.sort(kept, Inters.byAbscissa);

        return kept;
    }

    //----------------//
    // getGlyphsSlice //
    //----------------//
    /**
     * Retrieve among the collection, the glyphs intersected by the provided horizontal
     * slice.
     *
     * @param glyphs the whole collection of glyph instances
     * @param area   the horizontal slice
     * @return the list of selected glyph instances, sorted by abscissa
     */
    private List<Glyph> getGlyphsSlice (List<Glyph> glyphs,
                                        Area area)
    {
        List<Glyph> slice = new ArrayList<Glyph>(Glyphs.intersectedGlyphs(glyphs, area));
        Collections.sort(slice, Glyphs.byAbscissa);

        return slice;
    }

    //-------------------//
    // getLedgerAdapters //
    //-------------------//
    /**
     * Report the sequence of adapters for all ledgers found
     * immediately further from staff from the provided pitch.
     *
     * @param staff staff at hand
     * @param pitch pitch of current location (assumed to be odd)
     * @return the proper list of ledger adapters
     */
    private List<LedgerAdapter> getLedgerAdapters (Staff staff,
                                                   final int pitch)
    {
        if (((pitch % 2) == 0) || (Math.abs(pitch) <= 4)) {
            return Collections.emptyList();
        }

        List<LedgerAdapter> list = new ArrayList<LedgerAdapter>();

        // Check for ledgers
        final int dir = Integer.signum(pitch);
        final int targetPitch = pitch + dir;
        int p = dir * 4;

        for (int i = dir;; i += dir) {
            List<LedgerInter> set = staff.getLedgers(i);

            if ((set == null) || set.isEmpty()) {
                break;
            }

            p += (2 * dir);

            if (p == targetPitch) {
                for (LedgerInter ledger : set) {
                    list.add(new LedgerAdapter(staff, null, ledger.getGlyph()));
                }

                break;
            }
        }

        return list;
    }

    //----------------------//
    // getSystemCompetitors //
    //----------------------//
    /**
     * Retrieve the collection of (really good) other interpretations that might compete
     * with heads and note candidates.
     *
     * @return the really good competitors
     */
    private List<Inter> getSystemCompetitors ()
    {
        List<Inter> comps = sig.inters(
                new Predicate<Inter>()
        {
            @Override
            public boolean check (Inter inter)
            {
                final Shape shape = inter.getShape();

                if (!inter.isGood() || !COMPETING_SHAPES.contains(shape)) {
                    return false;
                }

                if (inter instanceof AbstractVerticalInter) {
                    // We may have a stem mistaken for a thin barline or a thin connector
                    // So, check bar/connector width vs max stem width
                    AbstractVerticalInter vertical = (AbstractVerticalInter) inter;
                    int width = (int) Math.floor(vertical.getWidth());

                    if (width <= scale.getMaxStem()) {
                        return false;
                    }
                } else if ((shape == Shape.BEAM) || (shape == Shape.BEAM_HOOK)) {
                    // Check beam width
                    if (inter.getBounds().width < params.minBeamWidth) {
                        return false;
                    }
                }

                return true;
            }
        });

        Collections.sort(comps, Inters.byOrdinate);

        return comps;
    }

    //---------//
    // overlap //
    //---------//
    /**
     * Check whether the provided box overlaps one of the competitors.
     *
     * @param box         template box
     * @param competitors sequence of competitors, sorted by abscissa
     * @return true if overlap was detected
     */
    private boolean overlap (Rectangle2D box,
                             List<Inter> competitors)
    {
        final double xMax = box.getMaxX();

        for (Inter comp : competitors) {
            if (comp instanceof HeadInter) {
                continue;
            }

            if (comp.getArea() != null) {
                if (comp.getArea().intersects(box)) {
                    return true;
                }
            } else {
                Rectangle cBox = comp.getBounds();

                if (cBox.intersects(box)) {
                    return true;
                } else if (cBox.x > xMax) {
                    break;
                }
            }
        }

        return false;
    }

    //-------------//
    // overlapSeed //
    //-------------//
    /**
     * We check overlap with seed-based note and return true only when
     * the overlapping seed-based inter has a rather similar or better grade.
     *
     * @param inter       the x-based inter to check
     * @param competitors abscissa-sorted slice of competitors, including seed-based inter instances
     * @return true if real conflict found
     */
    private boolean overlapSeed (Inter inter,
                                 List<Inter> competitors)
    {
        final Rectangle box = inter.getBounds();
        final double loweredGrade = inter.getGrade() * (1 - constants.gradeMargin.getValue());
        final double xMax = box.getMaxX();

        for (Inter comp : competitors) {
            if (!(comp instanceof HeadInter)) {
                continue;
            }

            Rectangle cBox = comp.getBounds();

            if (cBox.intersects(box)) {
                if (comp.getGrade() >= loweredGrade) {
                    return true;
                }
            } else if (cBox.x > xMax) {
                break;
            }
        }

        return false;
    }

    //--------------//
    // processStaff //
    //--------------//
    /**
     * Retrieve notes along the provided staff.
     * <p>
     * Pay attention to adjust ordinate as precisely as possible in the middle
     * of staff or ledger lines.
     *
     * @param staff the staff to process
     * @param seeds should we stick to stem seeds or not?
     * @return the list of created notes
     */
    private List<Inter> processStaff (Staff staff,
                                      boolean seeds)
    {
        List<Inter> ch = new ArrayList<Inter>(); // Created heads

        // Use all staff lines
        int pitch = -5; // Current pitch
        LineAdapter prevAdapter = null;

        for (LineInfo line : staff.getLines()) {
            LineAdapter adapter = new StaffLineAdapter(staff, line);

            // Look above line
            ch.addAll(new Scanner(adapter, prevAdapter, -1, pitch++, seeds).lookup());

            // Look exactly on line
            ch.addAll(new Scanner(adapter, null, 0, pitch++, seeds).lookup());

            // For the last line only, look just below line
            if (pitch == 5) {
                ch.addAll(new Scanner(adapter, null, 1, pitch++, seeds).lookup());
            }

            prevAdapter = adapter;
        }

        // Use all ledgers, above staff, then below staff
        for (int dir : new int[]{-1, 1}) {
            pitch = dir * 4;

            for (int i = dir;; i += dir) {
                List<LedgerInter> set = staff.getLedgers(i);

                if ((set == null) || set.isEmpty()) {
                    break;
                }

                char c = 'a';
                pitch += (2 * dir);

                for (LedgerInter ledger : set) {
                    String p = "" + c++;
                    Glyph glyph = ledger.getGlyph();
                    LineAdapter adapter = new LedgerAdapter(staff, p, glyph);
                    // Look right on ledger
                    ch.addAll(new Scanner(adapter, null, 0, pitch, seeds).lookup());

                    // Look just further from staff
                    int pitch2 = pitch + dir;
                    ch.addAll(new Scanner(adapter, null, dir, pitch2, seeds).lookup());
                }
            }
        }

        return ch;
    }

    //-----------------//
    // purgeDuplicates //
    //-----------------//
    private int purgeDuplicates (List<Inter> inters)
    {
        List<Inter> removed = new ArrayList<Inter>();

        LeftLoop:
        for (int i = 0, iBreak = inters.size() - 1; i < iBreak; i++) {
            Inter left = inters.get(i);

            if (left.isRemoved()) {
                continue;
            }

            Rectangle leftBox = left.getBounds();
            int xMax = (leftBox.x + leftBox.width) - 1;

            for (Inter right : inters.subList(i + 1, inters.size())) {
                if (right.isRemoved()) {
                    continue;
                }

                Rectangle rightBox = right.getBounds();

                if (leftBox.intersects(rightBox)) {
                    if (left.isSameAs(right)) {
                        if (left.getGrade() < right.getGrade()) {
                            if (left.isVip()) {
                                logger.info("VIP purging {} at {}", left, left.getBounds());
                            }

                            left.remove();
                            removed.add(left);

                            continue LeftLoop;
                        } else {
                            if (right.isVip()) {
                                logger.info("VIP purging {} at {}", right, right.getBounds());
                            }

                            right.remove();
                            removed.add(right);
                        }
                    }
                } else if (rightBox.x > xMax) {
                    break;
                }
            }
        }

        inters.removeAll(removed);

        return removed.size();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // LineAdapter //
    //-------------//
    /**
     * Such adapter is needed to interact with staff LineInfo or ledger glyph line in a
     * consistent way.
     */
    private abstract static class LineAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Staff staff;

        private final String prefix;

        //~ Constructors ---------------------------------------------------------------------------
        public LineAdapter (Staff staff,
                            String prefix)
        {
            this.staff = staff;
            this.prefix = prefix;
        }

        //~ Methods --------------------------------------------------------------------------------
        /**
         * Report the competitors lookup area, according to limits above
         * and below, defined as ordinate shifts relative to the
         * reference line.
         *
         * @param above offset (positive or negative) from line to top limit.
         * @param below offset (positive or negative) from line to bottom limit.
         */
        public abstract Area getArea (double above,
                                      double below);

        /** Report the abscissa at beginning of line. */
        public abstract int getLeftAbscissa ();

        /** Needed to allow various attachments on the same staff. */
        public String getPrefix ()
        {
            return prefix;
        }

        /** Report the abscissa at end of line. */
        public abstract int getRightAbscissa ();

        public Staff getStaff ()
        {
            return staff;
        }

        /** Report the ordinate at provided abscissa. */
        public abstract int yAt (int x);

        /** Report the precise ordinate at provided precise abscissa. */
        public abstract double yAt (double x);
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Boolean printParameters = new Constant.Boolean(
                false,
                "Should we print out the class parameters?");

        private final Constant.Boolean allowAttachments = new Constant.Boolean(
                false,
                "Should we allow staff attachments for created areas?");

        private final Scale.Fraction maxTemplateDx = new Scale.Fraction(
                0.375,
                "Maximum dx between similar template instances");

        private final Scale.Fraction maxOpenDy = new Scale.Fraction(
                0.25,
                "Extension allowed in y for open lines");

        private final Constant.Ratio gradeMargin = new Constant.Ratio(
                0.1,
                "Grade margin to boost seed-based competitors");

        private final Constant.Ratio pitchMargin = new Constant.Ratio(
                0.75,
                "Vertical margin for intercepting stem seed around a target pitch");

        private final Constant.Ratio wholeBoost = new Constant.Ratio(
                0.4,
                "How much do we boost whole notes (always isolated)");

        private final Scale.Fraction minBeamWidth = new Scale.Fraction(
                2.5,
                "Minimum good beam width to exclude heads");

        private final Constant.Ratio minHoleWhiteRatio = new Constant.Ratio(
                0.2,
                "Minimum ratio of hole white pixel to reassign Black to Void");
    }

    //-----------//
    // Aggregate //
    //-----------//
    /**
     * Describes an aggregate of matches around similar location.
     */
    private static class Aggregate
    {
        //~ Instance fields ------------------------------------------------------------------------

        Point point;

        List<HeadInter> matches = new ArrayList<HeadInter>();

        //~ Methods --------------------------------------------------------------------------------
        public void add (HeadInter inter)
        {
            if (point == null) {
                point = GeoUtil.centerOf(inter.getBounds());
            }

            matches.add(inter);
        }

        public HeadInter getMainInter ()
        {
            return matches.get(0);
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder(getClass().getSimpleName());
            sb.append("{");

            if (point != null) {
                sb.append(" point:(").append(point.x).append(",").append(point.y).append(")");
            }

            sb.append(" ").append(matches.size()).append(" matches: ");

            for (Inter match : matches) {
                sb.append(match);
            }

            sb.append("}");

            return sb.toString();
        }
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants.
     */
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final double maxDistanceLow;

        final double maxDistanceHigh;

        final double reallyBadDistance;

        final int maxTemplateDx;

        final int maxOpenDy;

        final int minBeamWidth;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            maxDistanceLow = Template.maxDistanceLow();
            maxDistanceHigh = Template.maxDistanceHigh();
            reallyBadDistance = Template.reallyBadDistance();

            maxTemplateDx = scale.toPixels(constants.maxTemplateDx);
            maxOpenDy = Math.max(1, scale.toPixels(constants.maxOpenDy));
            minBeamWidth = scale.toPixels(constants.minBeamWidth);
        }
    }

    //---------------//
    // LedgerAdapter //
    //---------------//
    /**
     * Adapter for Ledger.
     */
    private class LedgerAdapter
            extends LineAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Glyph ledger;

        private final Point2D left;

        private final Point2D right;

        //~ Constructors ---------------------------------------------------------------------------
        public LedgerAdapter (Staff staff,
                              String prefix,
                              Glyph ledger)
        {
            super(staff, prefix);
            this.ledger = ledger;
            left = ledger.getStartPoint(Orientation.HORIZONTAL);
            right = ledger.getStopPoint(Orientation.HORIZONTAL);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public Area getArea (double above,
                             double below)
        {
            Path2D path = new Path2D.Double();
            path.moveTo(left.getX(), left.getY() + above);
            path.lineTo(right.getX(), right.getY() + above);
            path.lineTo(right.getX(), right.getY() + below + 1);
            path.lineTo(left.getX(), left.getY() + below + 1);
            path.closePath();

            return new Area(path);
        }

        @Override
        public int getLeftAbscissa ()
        {
            return (int) Math.ceil(left.getX());
        }

        @Override
        public int getRightAbscissa ()
        {
            return (int) Math.floor(right.getX());
        }

        @Override
        public int yAt (int x)
        {
            return (int) Math.rint(yAt((double) x));
        }

        @Override
        public double yAt (double x)
        {
            return LineUtil.yAtX(left, right, x);
        }
    }

    /**
     * DEBUG: meant to precisely measure behavior of notes retrieval.
     */
    private static class Perf
    {
        //~ Instance fields ------------------------------------------------------------------------

        int overlaps;

        int evals;

        int abandons;

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            return String.format(
                    "%7d overlaps, %7d evals, %7d abandons",
                    overlaps,
                    evals,
                    abandons);
        }
    }

    //---------//
    // Scanner //
    //---------//
    private class Scanner
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final int interline;

        private final LineAdapter line;

        private final LineAdapter line2;

        private final int dir;

        private final int pitch;

        private final boolean useSeeds;

        private final Area competitorsArea;

        private final Area seedsArea;

        private final List<Inter> competitors;

        private final List<LedgerAdapter> ledgers;

        private List<HeadInter> inters = new ArrayList<HeadInter>();

        /** Offsets tried around a given ordinate. */
        private final int[] yOffsets;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create a Scanner, dedicated to a staff line or ledger.
         *
         * @param line     adapter to the main line
         * @param line2    adapter to secondary line, if any, otherwise null
         * @param dir      direction WRT main line
         * @param pitch    pitch position value
         * @param useSeeds true for seed-based notes, false for x-based notes
         */
        public Scanner (LineAdapter line,
                        LineAdapter line2,
                        int dir,
                        int pitch,
                        boolean useSeeds)
        {
            this.line = line;
            this.line2 = line2;
            this.dir = dir;
            this.pitch = pitch;
            this.useSeeds = useSeeds;

            // Open line?
            boolean isOpen = ((pitch % 2) != 0) && ((line2 == null) || (Math.abs(pitch) == 5));
            yOffsets = computeYOffsets(isOpen);

            final Staff staff = line.getStaff();
            interline = staff.getSpecificInterline();
            ledgers = getLedgerAdapters(staff, pitch);

            {
                // Horizontal slice to detect stem seeds
                final double maxGap = scale.toPixelsDouble(HeadStemRelation.getYGapMaximum(false));
                final double ratio = constants.pitchMargin.getValue();
                final double above = ((interline * (dir - ratio)) / 2) - maxGap;
                final double below = ((interline * (dir + ratio)) / 2) + maxGap;
                seedsArea = line.getArea(above, below);
            }

            {
                // Horizontal slice to detect competitors
                final double ratio = HeadInter.getShrinkVertRatio();
                final double above = ((interline * (dir - ratio)) / 2);
                final double below = ((interline * (dir + ratio)) / 2);
                competitorsArea = line.getArea(above, below);
            }

            if (constants.allowAttachments.isSet()) {
                staff.addAttachment(line.getPrefix() + "#s" + pitch, seedsArea);
                staff.addAttachment(line.getPrefix() + "#c" + pitch, competitorsArea);
            }

            competitors = getCompetitorsSlice(competitorsArea);
        }

        //~ Methods --------------------------------------------------------------------------------
        public List<HeadInter> lookup ()
        {
            return useSeeds ? lookupSeeds() : lookupRange();
        }

        //-----------------//
        // computeYOffsets //
        //-----------------//
        private int[] computeYOffsets (boolean isOpen)
        {
            if (isOpen) {
                int[] offsets = new int[params.maxOpenDy];

                for (int i = 0; i < offsets.length; i++) {
                    // 0, +1, -1, +2,+ 3, +4, ... (according to dir sign)
                    switch (i) {
                    case 0:
                        offsets[0] = 0;

                        break;

                    case 1:
                        offsets[1] = dir;

                        break;

                    case 2:
                        offsets[2] = -dir;

                        break;

                    default:
                        offsets[i] = dir * (i - 1);
                    }
                }

                return offsets;
            } else {
                //                int[] offsets = new int[params.maxClosedDy];
                //
                //                for (int i = 0; i < offsets.length; i++) {
                //                    if ((i % 2) == 0) {
                //                        offsets[i] = -(i / 2);
                //                    } else {
                //                        offsets[i] = ((i + 1) / 2);
                //                    }
                //                }
                //
                //                return offsets;
                return NO_OFFSETS;
            }
        }

        //------//
        // eval //
        //------//
        private PixelDistance eval (Shape shape,
                                    int x,
                                    int y,
                                    Anchor anchor)
        {
            final ShapeDescriptor desc = catalog.getDescriptor(shape);
            final Rectangle symBox = desc.getSymbolBoundsAt(x, y, anchor);

            // Skip if location already used by really good object (beam, etc)
            //TODO: perhaps use a slightly fattened box?
            if (overlap(symBox, competitors)) {
                if (useSeeds) {
                    seedsPerf.overlaps++;
                } else {
                    rangePerf.overlaps++;
                }

                return null;
            }

            // Then try (all variants for) the shape and keep the best dist
            double dist = desc.evaluate(x, y, anchor, distances);

            if (useSeeds) {
                seedsPerf.evals++;
            } else {
                rangePerf.evals++;
            }

            return new PixelDistance(x, y, dist);
        }

        //-----------------//
        // evalBlackAsVoid //
        //-----------------//
        /**
         * Evaluate the provided location (of a black candidate) for white pixels
         * expected in the hole part of a void candidate.
         *
         * @param x      pivot abscissa
         * @param y      pivot ordinate
         * @param anchor precise anchor
         * @return either NOTEHEAD_VOID (positive test) or null (negative test)
         */
        private Shape evalBlackAsVoid (int x,
                                       int y,
                                       Anchor anchor)
        {
            final ShapeDescriptor desc = catalog.getDescriptor(Shape.NOTEHEAD_VOID);
            final double holeWhiteRatio = desc.evaluateHole(x, y, anchor, distances);

            if (holeWhiteRatio >= constants.minHoleWhiteRatio.getValue()) {
                return Shape.NOTEHEAD_VOID;
            } else {
                return null;
            }
        }

        //----------------------//
        // getRelevantAbscissae //
        //----------------------//
        /**
         * Select the x values that are intersected by note spots and thus could
         * correspond to notes.
         *
         * @param scanLeft  range starting abscissa
         * @param scanRight range stopping abscissa
         * @return an array of booleans, telling which x values are relevant
         */
        private boolean[] getRelevantAbscissae (int scanLeft,
                                                int scanRight)
        {
            List<Glyph> spots = getGlyphsSlice(systemSpots, competitorsArea);
            boolean[] relevants = new boolean[scanRight - scanLeft + 1];
            int maxNoteWidth = interline; // Rough value is sufficient

            for (Glyph spot : spots) {
                Rectangle spotBox = spot.getBounds();
                spotBox.grow(maxNoteWidth, 0);

                for (int x = spotBox.x; x < (spotBox.x + spotBox.width); x++) {
                    int ix = x - scanLeft;

                    if ((ix >= 0) && (ix < relevants.length)) {
                        relevants[ix] = true;
                    }
                }
            }

            return relevants;
        }

        //------------------------//
        // getTheoreticalOrdinate //
        //------------------------//
        /**
         * Determine the theoretical ordinate.
         *
         * @param x current abscissa value
         * @return the most probable ordinate value
         */
        private int getTheoreticalOrdinate (int x)
        {
            // Determine lines config according to ledgers if outside staff
            final boolean isOutside = Math.abs(pitch) > 4;

            if (isOutside) {
                if ((pitch % 2) == 0) {
                    return line.yAt(x);
                } else {
                    // Both are present only if a further ledger exists in abscissa
                    //TODO: refine using width of template?
                    if (Math.abs(pitch) > 5) {
                        for (LedgerAdapter ledger : ledgers) {
                            if ((x >= ledger.getLeftAbscissa())
                                && (x <= ledger.getRightAbscissa())) {
                                return (int) Math.rint(
                                        (line.yAt((double) x) + ledger.yAt((double) x)) / 2);
                            }
                        }
                    }

                    if (pitch > 0) {
                        return (int) Math.rint(line.yAt((double) x) + (interline / 2d));
                    } else {
                        // Bottom is always present
                        return (int) Math.rint(line.yAt((double) x) - (interline / 2d));
                    }
                }
            } else if (line2 != null) {
                return (int) Math.rint((line.yAt((double) x) + line2.yAt((double) x)) / 2d);
            } else {
                return line.yAt(x);
            }
        }

        //-------------//
        // lookupRange //
        //-------------//
        /**
         * Try every relevant abscissa in the provided range of the line.
         * <p>
         * For a staff line, if every abscissa in range were checked, the cost of lookupRange()
         * would be about 3 times the cost of lookupSeed().
         * Hence, to limit the number of abscissa values to browse for black heads, we first check
         * with note spots. Void notes however can exist without detected note spots.
         * <p>
         * Regarding the template shapes, it would be tempting to restrain range browsing to
         * stem-less shapes (wholes & cue wholes).
         * However we cannot skip the check for stem-based shapes because some stems are so poor
         * that we don't have stem seeds of proper length for them, and range browsing is then the
         * only way to reach note heads with such poor stems.
         *
         * @return the inters created
         */
        private List<HeadInter> lookupRange ()
        {
            // Abscissa range for scan
            final int scanLeft = Math.max(
                    line.getLeftAbscissa(),
                    (int) line.getStaff().getHeaderStop());
            final int scanRight = line.getRightAbscissa() - minTemplateWidth;

            if (scanRight < scanLeft) {
                return inters;
            }

            // Use the note spots to limit the abscissae to be checked
            // OK for blacks, not for voids
            boolean[] relevants = getRelevantAbscissae(scanLeft, scanRight);

            // Scan from left to right
            for (int x0 = scanLeft; x0 <= scanRight; x0++) {
                final int y0 = getTheoreticalOrdinate(x0);

                // Shapes to try depend on whether location belongs to a black spot
                EnumSet<Shape> shapeSet = relevants[x0 - scanLeft] ? ShapeSet.TemplateNotes
                        : ShapeSet.VoidTemplateNotes;
                ShapeLoop:
                for (Shape shape : shapeSet) {
                    PixelDistance bestLoc = null;

                    for (int yOffset : yOffsets) {
                        final int y = y0 + yOffset;
                        PixelDistance loc = eval(shape, x0, y, MIDDLE_LEFT);

                        if ((loc != null) && (loc.d <= params.maxDistanceLow)) {
                            if ((bestLoc == null) || (bestLoc.d > loc.d)) {
                                bestLoc = loc;
                            }
                        } else if (y == y0) {
                            // This is the very first (best guess) location tried.
                            // If eval is really bad, stop immediately
                            if ((loc == null) || (loc.d >= params.reallyBadDistance)) {
                                rangePerf.abandons++;

                                continue ShapeLoop;
                            }
                        }
                    }

                    if (bestLoc != null) {
                        // Special case: NOTEHEAD_VOID mistaken for NOTEHEAD_BLACK
                        if (shape == Shape.NOTEHEAD_BLACK) {
                            Shape newShape = evalBlackAsVoid(bestLoc.x, bestLoc.y, MIDDLE_LEFT);

                            if (newShape != null) {
                                shape = newShape;
                            }
                        }

                        HeadInter inter = createInter(
                                bestLoc,
                                MIDDLE_LEFT,
                                shape,
                                line.getStaff(),
                                pitch);

                        if (inter != null) {
                            inters.add(inter);
                        }
                    }
                }
            }

            // Aggregate matching inters
            inters = aggregateMatches(inters);

            // Check conflict with seed-based instances
            inters = filterSeedConflicts(inters, competitors);

            for (Iterator<HeadInter> it = inters.iterator(); it.hasNext();) {
                HeadInter inter = it.next();
                Glyph glyph = inter.retrieveGlyph(image);

                if (glyph != null) {
                    sig.addVertex(inter);
                } else {
                    it.remove();
                }
            }

            return inters;
        }

        //-------------//
        // lookupSeeds //
        //-------------//
        private List<HeadInter> lookupSeeds ()
        {
            // Intersected seeds in the area
            final List<Glyph> seeds = getGlyphsSlice(systemSeeds, seedsArea);

            // Use one anchor for each horizontal side of the stem seed
            final Anchor[] anchors = new Anchor[]{LEFT_STEM, RIGHT_STEM};

            for (Glyph seed : seeds) {
                if (seed.isVip()) {
                    logger.info("lookupSeeds for seed#{}", seed.getId());
                }

                // Compute precise stem link point.
                // x value is imposed by seed alignment, y value by line(s)
                int x0 = GeoUtil.centerOf(seed.getBounds()).x; // Rough x value
                int yLine = line.yAt(x0); // Rather good line y value
                final Point2D top = seed.getStartPoint(Orientation.VERTICAL);
                final Point2D bot = seed.getStopPoint(Orientation.VERTICAL);
                x0 = (int) Math.rint(LineUtil.xAtY(top, bot, yLine)); // Precise x value

                final int y0 = getTheoreticalOrdinate(x0); // Precise y value

                for (Anchor anchor : anchors) {
                    // For each stem side and for each possible shape,
                    // keep the best match (if acceptable) among all locations tried.
                    ShapeLoop:
                    for (Shape shape : ShapeSet.StemTemplateNotes) {
                        PixelDistance bestLoc = null;

                        // Brute force: explore the whole rectangle around (x0, y0)
                        for (int yOffset : yOffsets) {
                            final int y = y0 + yOffset;

                            for (int xOffset : xOffsets) {
                                final int x = x0 + xOffset;
                                PixelDistance loc = eval(shape, x, y, anchor);

                                if ((loc != null) && (loc.d <= params.maxDistanceLow)) {
                                    if ((bestLoc == null) || (bestLoc.d > loc.d)) {
                                        bestLoc = loc;
                                    }
                                } else if ((x == x0) && (y == y0)) {
                                    // This is the very first (best guess) location tried.
                                    // If eval is really bad, stop immediately
                                    if ((loc == null) || (loc.d >= params.reallyBadDistance)) {
                                        seedsPerf.abandons++;

                                        continue ShapeLoop;
                                    }
                                }
                            }
                        }

                        if (bestLoc != null) {
                            // Special case: NOTEHEAD_VOID mistaken for NOTEHEAD_BLACK
                            if (shape == Shape.NOTEHEAD_BLACK) {
                                Shape newShape = evalBlackAsVoid(bestLoc.x, bestLoc.y, anchor);

                                if (newShape != null) {
                                    shape = newShape;
                                }
                            }

                            HeadInter inter = createInter(
                                    bestLoc,
                                    anchor,
                                    shape,
                                    line.getStaff(),
                                    pitch);

                            if (inter != null) {
                                Glyph glyph = inter.retrieveGlyph(image);

                                if (glyph != null) {
                                    sig.addVertex(inter);
                                    inters.add(inter);
                                }
                            }
                        }
                    }
                }
            }

            return inters;
        }
    }

    //------------------//
    // StaffLineAdapter //
    //------------------//
    /**
     * Adapter for staff line.
     */
    private class StaffLineAdapter
            extends LineAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final LineInfo line;

        //~ Constructors ---------------------------------------------------------------------------
        public StaffLineAdapter (Staff staff,
                                 LineInfo line)
        {
            super(staff, "");
            this.line = line;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public Area getArea (double above,
                             double below)
        {
            NaturalSpline spline = line.getSpline();
            GeoPath path = new GeoPath();
            AffineTransform at;

            // Top line
            at = AffineTransform.getTranslateInstance(0, above);
            path.append(spline.getPathIterator(at), false);

            // Bottom line (reversed)
            at = AffineTransform.getTranslateInstance(0, below + 1);
            path.append(ReversePathIterator.getReversePathIterator(spline, at), true);

            path.closePath();

            return new Area(path);
        }

        @Override
        public int getLeftAbscissa ()
        {
            return (int) Math.floor(line.getEndPoint(LEFT).getX());
        }

        @Override
        public int getRightAbscissa ()
        {
            return (int) Math.floor(line.getEndPoint(RIGHT).getX());
        }

        @Override
        public int yAt (int x)
        {
            return line.yAt(x);
        }

        @Override
        public double yAt (double x)
        {
            return line.yAt(x);
        }
    }
}
//
//    //--------------//
//    // flagOverlaps //
//    //--------------//
//    /**
//     * In the provided list of note interpretations, detect and flag as
//     * such the overlapping ones.
//     *
//     * @param inters the provided interpretations (for a staff)
//     */
//    private void flagOverlaps (List<Inter> inters)
//    {
//        for (int i = 0, iBreak = inters.size() - 1; i < iBreak; i++) {
//            Inter left = inters.get(i);
//            Rectangle box = left.getBounds();
//            Rectangle2D smallBox = HeadInter.shrink(box);
//            double xMax = smallBox.getMaxX();
//
//            for (Inter right : inters.subList(i + 1, inters.size())) {
//                Rectangle rightBox = right.getBounds();
//
//                if (smallBox.intersects(rightBox)) {
//                    sig.insertExclusion(left, right, Exclusion.Cause.OVERLAP);
//                } else if (rightBox.x > xMax) {
//                    break;
//                }
//            }
//        }
//    }
//
