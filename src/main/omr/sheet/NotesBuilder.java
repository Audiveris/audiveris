//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     N o t e s B u i l d e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.grid.FilamentLine;
import omr.grid.LineInfo;
import omr.grid.StaffInfo;

import omr.image.Anchored.Anchor;
import static omr.image.Anchored.Anchor.*;
import omr.image.DistanceTable;
import omr.image.PixelDistance;
import omr.image.ShapeDescriptor;
import omr.image.TemplateFactory;
import omr.image.TemplateFactory.Catalog;

import omr.math.GeoOrder;
import omr.math.GeoPath;
import omr.math.GeoUtil;
import omr.math.LineUtil;
import omr.math.NaturalSpline;
import omr.math.ReversePathIterator;

import omr.run.Orientation;

import omr.sig.AbstractInter;
import omr.sig.AbstractNoteInter;
import omr.sig.BlackHeadInter;
import omr.sig.Exclusion;
import omr.sig.GradeImpacts;
import omr.sig.Inter;
import omr.sig.LedgerInter;
import omr.sig.SIGraph;
import omr.sig.SmallBlackHeadInter;
import omr.sig.SmallVoidHeadInter;
import omr.sig.SmallWholeInter;
import omr.sig.VoidHeadInter;
import omr.sig.WholeInter;

import omr.util.Navigable;
import omr.util.Predicate;
import omr.util.StopWatch;

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
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

/**
 * Class {@code NotesBuilder} retrieves the void note heads, the black note heads,
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
 * <li>We can reasonably skip the locations where a (good) beam or a (good) bar line has been
 * detected.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class NotesBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(NotesBuilder.class);

    /** Shapes of note competitors. */
    private static final Set<Shape> competingShapes = EnumSet.copyOf(
            Arrays.asList(
                    Shape.THICK_BARLINE,
                    Shape.THIN_BARLINE,
                    Shape.THICK_CONNECTION,
                    Shape.THIN_CONNECTION,
                    Shape.BEAM,
                    Shape.BEAM_HOOK,
                    Shape.BEAM_SMALL,
                    Shape.BEAM_HOOK_SMALL));

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

    /** Scale-dependent constants. */
    private final Parameters params;

    /** Minimum width of templates. */
    private int minTemplateWidth = 0; // TODO

    /** The <b>properly scaled</b> templates to use. */
    private Catalog catalog;

    /** The distance table to use. */
    private DistanceTable distances;

    /** The competing interpretations for the system. */
    private List<Inter> systemCompetitors;

    /** The vertical (stem) seeds for the system. */
    private List<Glyph> systemSeeds;

    //~ Constructors -------------------------------------------------------------------------------
    //--------------//
    // NotesBuilder //
    //--------------//
    /**
     * Creates a new NotesBuilder object.
     *
     * @param system the system to process
     */
    public NotesBuilder (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
        sheet = system.getSheet();
        scale = sheet.getScale();

        params = new Parameters(scale);

        if ((system.getId() == 1) && constants.printParameters.isSet()) {
            Main.dumping.dump(params);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // buildNotes //
    //------------//
    /**
     * Retrieve all void heads, black heads and whole notes in the
     * system both for standard and small (cue/grace) sizes.
     */
    public void buildNotes ()
    {
        StopWatch watch = new StopWatch("buildNotes S#" + system.getId());
        systemCompetitors = getSystemCompetitors(); // Competitors
        systemSeeds = system.lookupShapedGlyphs(Shape.VERTICAL_SEED); // Vertical seeds
        Collections.sort(systemSeeds, Glyph.byOrdinate);

        distances = sheet.getDistanceImage();

        for (StaffInfo staff : system.getStaves()) {
            logger.debug("Staff #{}", staff.getId());
            watch.start("Staff #" + staff.getId());

            final int interline = staff.getSpecificScale().getInterline();
            catalog = TemplateFactory.getInstance().getCatalog(interline);

            List<Inter> ch = new ArrayList<Inter>();

            // First, process all seed-based heads for the staff
            ch.addAll(processStaff(staff, true));

            // Consider seed-based heads as special competitors for x-based notes
            systemCompetitors.addAll(ch);
            Collections.sort(systemCompetitors, Inter.byOrdinate);

            // Second, process x-based notes for the staff
            ch.addAll(processStaff(staff, false));

            // Finally, detect notes overlaps for current staff
            Collections.sort(ch, Inter.byAbscissa);
            purgeDuplicates(ch);
            flagOverlaps(ch);
        }

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //------------------//
    // aggregateMatches //
    //------------------//
    private List<Inter> aggregateMatches (List<Inter> inters)
    {
        // Sort by decreasing grade
        Collections.sort(inters, Inter.byReverseGrade);

        // Gather matches per close locations
        // Avoid duplicate locations
        List<Aggregate> aggregates = new ArrayList<Aggregate>();

        for (Inter inter : inters) {
            // Check among already filtered locations for similar location
            Aggregate aggregate = null;

            for (Aggregate ag : aggregates) {
                Point loc = GeoUtil.centerOf(inter.getBounds());
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

        List<Inter> filtered = new ArrayList<Inter>();

        for (Aggregate ag : aggregates) {
            filtered.add(ag.getMainInter());
        }

        return filtered;
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
     * @param pitch  the note pitch
     * @return the inter created, if any
     */
    private Inter createInter (PixelDistance loc,
                               Anchor anchor,
                               Shape shape,
                               int pitch)
    {
        final ShapeDescriptor desc = catalog.getDescriptor(shape);
        final Rectangle box = desc.getSymbolBoundsAt(loc.x, loc.y, anchor);
        final double distImpact = 1 - (loc.d / params.maxMatchingDistance);
        final GradeImpacts impacts = new AbstractNoteInter.Impacts(distImpact);
        final double grade = impacts.getGrade();

        // Is grade acceptable?
        if (grade < AbstractInter.getMinGrade()) {
            return null;
        }

        switch (desc.getShape()) {
        case NOTEHEAD_BLACK:
            return new BlackHeadInter(desc, box, impacts, pitch);

        case NOTEHEAD_BLACK_SMALL:
            return new SmallBlackHeadInter(desc, box, impacts, pitch);

        case NOTEHEAD_VOID:
            return new VoidHeadInter(desc, box, impacts, pitch);

        case NOTEHEAD_VOID_SMALL:
            return new SmallVoidHeadInter(desc, box, impacts, pitch);

        case WHOLE_NOTE:
            return new WholeInter(desc, box, impacts, pitch);

        case WHOLE_NOTE_SMALL:
            return new SmallWholeInter(desc, box, impacts, pitch);
        }

        logger.error("No root shape for " + desc.getShape());

        return null;
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
    private List<Inter> filterSeedConflicts (List<Inter> inters,
                                             List<Inter> competitors)
    {
        List<Inter> filtered = new ArrayList<Inter>();

        for (Inter inter : inters) {
            if (!overlapSeed(inter, competitors)) {
                filtered.add(inter);
            }
        }

        return filtered;
    }

    //--------------//
    // flagOverlaps //
    //--------------//
    /**
     * In the provided list of note interpretations, detect and flag as
     * such the overlapping ones.
     *
     * @param inters the provided interpretations (for a staff)
     */
    private void flagOverlaps (List<Inter> inters)
    {
        for (int i = 0, iBreak = inters.size() - 1; i < iBreak; i++) {
            Inter left = inters.get(i);
            Rectangle box = left.getBounds();
            Rectangle2D smallBox = AbstractNoteInter.shrink(box);
            double xMax = smallBox.getMaxX();

            for (Inter right : inters.subList(i + 1, inters.size())) {
                Rectangle rightBox = right.getBounds();

                if (smallBox.intersects(rightBox)) {
                    sig.insertExclusion(left, right, Exclusion.Cause.OVERLAP);
                } else if (rightBox.x > xMax) {
                    break;
                }
            }
        }
    }

    //---------------------//
    // getCompetitorsSlice //
    //---------------------//
    /**
     * Retrieve the competitors intersected by the provided horizontal
     * slice.
     *
     * @param area the horizontal slice
     * @return the list of competitors, sorted by abscissa.
     */
    private List<Inter> getCompetitorsSlice (Area area)
    {
        List<Inter> rawComps = sig.intersectedInters(systemCompetitors, GeoOrder.BY_ORDINATE, area);

        // Keep only the "good" interpretations
        List<Inter> kept = new ArrayList<Inter>();

        for (Inter inter : rawComps) {
            //TODO: perhaps useless since all competitors are "good"?
            if (inter.isGood()) {
                kept.add(inter);
            }
        }

        // Sort by abscissa for more efficient lookup
        Collections.sort(kept, Inter.byAbscissa);

        return kept;
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
    private List<LedgerAdapter> getLedgerAdapters (StaffInfo staff,
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
            SortedSet<LedgerInter> set = staff.getLedgers(i);

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

    //---------------//
    // getSeedsSlice //
    //---------------//
    /**
     * Retrieve the vertical seeds intersected by the provided
     * horizontal slice.
     *
     * @param area the horizontal slice
     * @return the list of seeds, sorted by abscissa
     */
    private List<Glyph> getSeedsSlice (Area area)
    {
        List<Glyph> seeds = new ArrayList<Glyph>(Glyphs.intersectedGlyphs(systemSeeds, area));
        Collections.sort(seeds, Glyph.byAbscissa);

        return seeds;
    }

    //----------------------//
    // getSystemCompetitors //
    //----------------------//
    /**
     * Retrieve the collection of (good) other interpretations that might compete with
     * heads and note candidates.
     *
     * @return the good competitors
     */
    private List<Inter> getSystemCompetitors ()
    {
        List<Inter> comps = sig.inters(
                new Predicate<Inter>()
                {
                    @Override
                    public boolean check (Inter inter)
                    {
                        return inter.isGood() && competingShapes.contains(inter.getShape());
                    }
                });

        Collections.sort(comps, Inter.byOrdinate);

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
            if (comp instanceof AbstractNoteInter) {
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
     * the overlapping seed-based inter has a (really) better grade.
     *
     * @param inter       the x-based inter to check
     * @param competitors abscissa-sorted slice of competitors, including
     *                    seed-based inter instances
     * @return true if real conflict found
     */
    private boolean overlapSeed (Inter inter,
                                 List<Inter> competitors)
    {
        final Rectangle box = inter.getBounds();
        final double loweredGrade = inter.getGrade() * (1 - constants.gradeMargin.getValue());
        final double xMax = box.getMaxX();

        for (Inter comp : competitors) {
            if (!(comp instanceof AbstractNoteInter)) {
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
    private List<Inter> processStaff (StaffInfo staff,
                                      boolean seeds)
    {
        List<Inter> ch = new ArrayList<Inter>(); // Created heads

        // Use all staff lines
        int pitch = -5; // Current pitch
        LineAdapter prevAdapter = null;
        Scanner scanner;

        for (FilamentLine line : staff.getLines()) {
            LineAdapter adapter = new StaffLineAdapter(staff, line);

            // Look above line
            scanner = new Scanner(adapter, prevAdapter, -1, pitch++, seeds);
            ch.addAll(scanner.lookup());

            // Look exactly on line
            scanner = new Scanner(adapter, null, 0, pitch++, seeds);
            ch.addAll(scanner.lookup());

            // For the last line only, look just below line
            if (pitch == 5) {
                scanner = new Scanner(adapter, null, 1, pitch++, seeds);
                ch.addAll(scanner.lookup());
            }

            prevAdapter = adapter;
        }

        // Use all ledgers, above staff, then below staff
        for (int dir : new int[]{-1, 1}) {
            pitch = dir * 4;

            for (int i = dir;; i += dir) {
                SortedSet<LedgerInter> set = staff.getLedgers(i);

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
                    scanner = new Scanner(adapter, null, 0, pitch, seeds);
                    ch.addAll(scanner.lookup());

                    // Look just further from staff
                    int pitch2 = pitch + dir;
                    scanner = new Scanner(adapter, null, dir, pitch2, seeds);
                    ch.addAll(scanner.lookup());
                }
            }
        }

        return ch;
    }

    //-----------------//
    // purgeDuplicates //
    //-----------------//
    private void purgeDuplicates (List<Inter> inters)
    {
        List<Inter> toRemove = new ArrayList<Inter>();

        for (int i = 0, iBreak = inters.size() - 1; i < iBreak; i++) {
            Inter left = inters.get(i);
            Rectangle leftBox = left.getBounds();
            int xMax = (leftBox.x + leftBox.width) - 1;

            for (Inter right : inters.subList(i + 1, inters.size())) {
                Rectangle rightBox = right.getBounds();

                if (leftBox.intersects(rightBox)) {
                    if (left.isSameAs(right)) {
                        toRemove.add(right);
                    }
                } else if (rightBox.x > xMax) {
                    break;
                }
            }
        }

        if (!toRemove.isEmpty()) {
            inters.removeAll(toRemove);

            for (Inter inter : toRemove) {
                if (inter.isVip()) {
                    logger.info("VIP purging {} at {}", inter, inter.getBounds());
                }

                sig.removeVertex(inter);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
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

        List<Inter> matches = new ArrayList<Inter>();

        //~ Methods --------------------------------------------------------------------------------
        public void add (Inter inter)
        {
            if (point == null) {
                point = GeoUtil.centerOf(inter.getBounds());
            }

            matches.add(inter);
        }

        public Inter getMainInter ()
        {
            return matches.get(0);
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{");
            sb.append(getClass().getSimpleName());

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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        final Constant.Boolean printParameters = new Constant.Boolean(
                false,
                "Should we print out the class parameters?");

        final Constant.Boolean allowAttachments = new Constant.Boolean(
                false,
                "Should we allow staff attachments for created areas?");

        final Constant.Double maxMatchingDistance = new Constant.Double(
                "distance",
                0.08,
                "Maximum matching distance");

        final Scale.Fraction maxTemplateDx = new Scale.Fraction(
                0.375,
                "Maximum dx between similar template instances");

        final Scale.Fraction maxClosedDy = new Scale.Fraction(
                0.2,
                "Extension allowed in y for closed lines");

        final Scale.Fraction maxOpenDy = new Scale.Fraction(
                0.25,
                "Extension allowed in y for open lines");

        final Constant.Ratio shrinkHoriRatio = new Constant.Ratio(
                0.7,
                "Shrink horizontal ratio to apply when checking for overlap");

        final Constant.Ratio shrinkVertRatio = new Constant.Ratio(
                0.5,
                "Shrink vertical ratio to apply when checking for overlap");

        final Constant.Ratio gradeMargin = new Constant.Ratio(
                0.1,
                "Grade margin to boost seed-based competitors");
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
        public LedgerAdapter (StaffInfo staff,
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
            return LineUtil.intersectionAtX(left, right, x).getY();
        }
    }

    //-------------//
    // LineAdapter //
    //-------------//
    /**
     * Such adapter is needed to interact with staff LineInfo or ledger
     * glyph line in a consistent way.
     */
    private abstract static class LineAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final StaffInfo staff;

        private final String prefix;

        //~ Constructors ---------------------------------------------------------------------------
        public LineAdapter (StaffInfo staff,
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

        public StaffInfo getStaff ()
        {
            return staff;
        }

        /** Report the ordinate at provided abscissa. */
        public abstract int yAt (int x);

        /** Report the precise ordinate at provided precise abscissa. */
        public abstract double yAt (double x);
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

        final double maxMatchingDistance;

        final int maxTemplateDx;

        final int maxClosedDy;

        final int maxOpenDy;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            maxMatchingDistance = constants.maxMatchingDistance.getValue();
            maxTemplateDx = scale.toPixels(constants.maxTemplateDx);
            maxClosedDy = Math.max(1, scale.toPixels(constants.maxClosedDy));
            maxOpenDy = Math.max(1, scale.toPixels(constants.maxOpenDy));
        }
    }

    //---------//
    // Scanner //
    //---------//
    private class Scanner
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final LineAdapter line;

        private final LineAdapter line2;

        private final int dir;

        private final int pitch;

        private final boolean useSeeds;

        private final boolean hasLine;

        private final boolean isOpen;

        private final Area area;

        private final List<Inter> competitors;

        private final List<LedgerAdapter> ledgers;

        private final int[] yClosed = new int[params.maxClosedDy];

        private final int[] yOpen = new int[params.maxOpenDy];

        private List<Inter> inters = new ArrayList<Inter>();

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Create a Scanner, dedicated to a staff line or ledger.
         *
         * @param line     adapter to the main line
         * @param line2    adapter to secondary line, if any
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

            // Middle line?
            hasLine = (pitch % 2) == 0;

            // Open line?
            isOpen = ((pitch % 2) != 0) && ((line2 == null) || (Math.abs(pitch) == 5));

            final StaffInfo staff = line.getStaff();
            ledgers = getLedgerAdapters(staff, pitch);

            // Retrieve competitors for this horizontal slice
            final double ratio = AbstractNoteInter.getShrinkVertRatio();
            final double above = (scale.getInterline() * (dir - ratio)) / 2;
            final double below = (scale.getInterline() * (dir + ratio)) / 2;
            area = line.getArea(above, below);

            if (constants.allowAttachments.isSet()) {
                staff.addAttachment(line.getPrefix() + "#" + pitch, area);
            }

            competitors = getCompetitorsSlice(area);
        }

        //~ Methods --------------------------------------------------------------------------------
        public List<Inter> lookup ()
        {
            return useSeeds ? lookupSeeds() : lookupRange();
        }

        //------//
        // eval //
        //------//
        private Inter eval (Shape shape,
                            int x,
                            int y,
                            Anchor anchor)
        {
            final ShapeDescriptor desc = catalog.getDescriptor(shape);
            final Rectangle tplBox = desc.getTemplateBoundsAt(x, y, anchor);

            // Skip if location already used by good object (beam, etc)
            //TODO: perhaps use a slightly fattened box?
            if (overlap(tplBox, competitors)) {
                return null;
            }

            // Then try (all variants for) the shape and keep the best dist
            double dist = desc.evaluate(x, y, anchor, distances, hasLine);

            if (dist > params.maxMatchingDistance) {
                return null;
            }

            PixelDistance loc = new PixelDistance(x, y, dist);

            return createInter(loc, anchor, shape, pitch);
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
                        return (int) Math.rint(line.yAt((double) x) + (scale.getInterline() / 2d));
                    } else {
                        // Bottom is always present
                        return (int) Math.rint(line.yAt((double) x) - (scale.getInterline() / 2d));
                    }
                }
            } else {
                // Within staff lines, compute ordinate precisely
                if (line2 != null) {
                    return (int) Math.rint((line.yAt((double) x) + line2.yAt((double) x)) / 2d);
                } else {
                    return line.yAt(x);
                }
            }
        }

        //-------------//
        // lookupRange //
        //-------------//
        private List<Inter> lookupRange ()
        {
            // Template anchor to use
            final Anchor anchor = MIDDLE_LEFT;

            // Abscissa range for scan
            final int scanLeft = Math.max(
                    line.getLeftAbscissa(),
                    (int) line.getStaff().getDmzEnd());
            final int scanRight = line.getRightAbscissa() - minTemplateWidth;

            // Scan from left to right
            for (int x = scanLeft; x <= scanRight; x++) {
                final int ord = getTheoreticalOrdinate(x);

                for (int y : ordinates(ord)) {
                    for (Shape shape : ShapeSet.TemplateNotes) {
                        Inter inter = eval(shape, x, y, anchor);

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

            for (Inter inter : inters) {
                sig.addVertex(inter);
            }

            return inters;
        }

        //-------------//
        // lookupSeeds //
        //-------------//
        private List<Inter> lookupSeeds ()
        {
            // Intersected seeds in the area
            final List<Glyph> seeds = getSeedsSlice(area);

            // Use one anchor for each horizontal side of the stem seed
            final Anchor[] anchors = new Anchor[]{LEFT_STEM, RIGHT_STEM};

            for (Glyph seed : seeds) {
                // Compute precise stem link point
                // x value is imposed by seed alignment, y value by line
                int x = GeoUtil.centerOf(seed.getBounds()).x; // Rough x value
                int y0 = line.yAt(x); // Rather good y value
                final Point2D top = seed.getStartPoint(Orientation.VERTICAL);
                final Point2D bot = seed.getStopPoint(Orientation.VERTICAL);
                final Point2D pt = LineUtil.intersectionAtY(top, bot, y0);
                x = (int) Math.rint(pt.getX()); // Precise x value

                final int ord = getTheoreticalOrdinate(x);

                for (Anchor anchor : anchors) {
                    // For each stem side, keep only the best shape
                    Inter bestInter = null;

                    for (int y : ordinates(ord)) {
                        for (Shape shape : ShapeSet.StemTemplateNotes) {
                            Inter inter = eval(shape, x, y, anchor);

                            if (inter != null) {
                                if ((bestInter == null)
                                    || (bestInter.getGrade() < inter.getGrade())) {
                                    bestInter = inter;
                                }
                            }
                        }
                    }

                    if (bestInter != null) {
                        sig.addVertex(bestInter);
                        inters.add(bestInter);
                    }
                }
            }

            return inters;
        }

        //-----------//
        // ordinates //
        //-----------//
        /**
         * Report the y values to scan with template.
         * TODO: harmonize definition of yOpen & yClosed?
         *
         * @return the range of y values
         */
        private int[] ordinates (int ord)
        {
            if (isOpen) {
                for (int i = 0; i < yOpen.length; i++) {
                    yOpen[i] = ord + (dir * (i - 1));
                }

                return yOpen;
            } else {
                for (int i = 0; i < yClosed.length; i++) {
                    if ((i % 2) == 0) {
                        yClosed[i] = ord - (i / 2);
                    } else {
                        yClosed[i] = ord + ((i + 1) / 2);
                    }
                }

                return yClosed;
            }
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
        public StaffLineAdapter (StaffInfo staff,
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
            FilamentLine fLine = (FilamentLine) line;
            NaturalSpline spline = fLine.getFilament().getAlignment().getLine();

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
            return (int) Math.floor(line.getLeftPoint().getX());
        }

        @Override
        public int getRightAbscissa ()
        {
            return (int) Math.floor(line.getRightPoint().getX());
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
