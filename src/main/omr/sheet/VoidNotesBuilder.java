//----------------------------------------------------------------------------//
//                                                                            //
//                       V o i d N o t e s B u i l d e r                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.FilamentLine;
import omr.grid.LineInfo;
import omr.grid.StaffInfo;

import omr.image.Anchored.Anchor;
import static omr.image.Anchored.Anchor.*;
import omr.image.PixelDistance;
import omr.image.Table;
import omr.image.Template;
import omr.image.TemplateFactory;

import omr.math.GeoOrder;
import omr.math.GeoPath;
import omr.math.GeoUtil;
import omr.math.LineUtil;
import omr.math.NaturalSpline;
import omr.math.ReversePathIterator;

import omr.run.Orientation;

import omr.sig.AbstractInter;
import omr.sig.BlackHeadInter;
import omr.sig.Exclusion;
import omr.sig.Grades;
import omr.sig.Inter;
import omr.sig.SIGraph;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import omr.sig.AbstractNoteInter;

import static omr.sig.AbstractNoteInter.shrink;

/**
 * Class {@code VoidNotesBuilder} retrieves the void note heads and
 * the whole notes for a system.
 * <p>
 * It uses a distance matching approach which works well for such symbols that
 * exhibit a fixed shape, with a combination of foreground and background
 * information.
 * Besides void heads, this approach is used to complement the retrieval of
 * black note heads, since it can address black heads even if those lack many
 * foreground pixels.
 * <p>
 * We don't need to check each and every location in the system, but only the
 * locations where such note kind is possible:<ul>
 * <li>We can stick to staff lines and ledgers locations.</li>
 * <li>We cannot fully use stems, since at this time we just have vertical seeds
 * and not all stems will contain seeds. However, if a vertical seed exists
 * nearby we can use it to evaluate a note candidate at proper location.</li>
 * <li>We can reasonably skip the locations where a (good) black note head or a
 * (good) beam has been detected.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class VoidNotesBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            VoidNotesBuilder.class);

    /**
     * Shapes that occur right on staff lines or ledgers.
     * Pitch value is even, they are crossed by a horizontal line.
     */
    private static final List<Shape> evens = Arrays.asList(
            Shape.VOID_EVEN,
            Shape.WHOLE_EVEN,
            Shape.NOTEHEAD_BLACK);

    /**
     * Shapes that occur between staff lines or ledgers.
     * Pitch value is odd, they are not crossed by any horizontal line.
     */
    private static final List<Shape> odds = Arrays.asList(
            Shape.VOID_ODD,
            Shape.WHOLE_ODD,
            Shape.NOTEHEAD_BLACK);

    /** Shapes that cannot occur near stem. */
    private static final List<Shape> noStems = Arrays.asList(
            Shape.WHOLE_ODD,
            Shape.WHOLE_EVEN);

    /** Shapes of competitors. */
    private static final List<Shape> competingShapes = Arrays.asList(
            Shape.NOTEHEAD_BLACK,
            Shape.BEAM);

    /** The template factory singleton. */
    private static TemplateFactory factory = TemplateFactory.getInstance();

    //~ Instance fields --------------------------------------------------------
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
    private final int minTemplateWidth;

    /** The <b>properly scaled</b> templates to use. */
    private final Map<Shape, Template> templates = new HashMap<Shape, Template>();

    /** The distance table to use. */
    private Table distances;

    /** The competing interpretations for the system. */
    private List<Inter> systemCompetitors;

    /** The vertical (stem) seeds for the system. */
    private List<Glyph> systemSeeds;

    //~ Constructors -----------------------------------------------------------
    //------------------//
    // VoidNotesBuilder //
    //------------------//
    /**
     * Creates a new VoidNotesBuilder object.
     *
     * @param system the system to process
     */
    public VoidNotesBuilder (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
        sheet = system.getSheet();
        scale = sheet.getScale();

        params = new Parameters(scale);

        if ((system.getId() == 1) && constants.printParameters.isSet()) {
            Main.dumping.dump(params);
        }

        minTemplateWidth = buildTemplates();
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // buildVoidHeads //
    //----------------//
    /**
     * Retrieve all void heads and whole notes in the system, as well
     * as additional black heads.
     */
    public void buildVoidHeads ()
    {
        StopWatch watch = new StopWatch("buildVoidHeads S#" + system.getId());
        systemCompetitors = getSystemCompetitors(); // Competitors
        systemSeeds = getSystemSeeds(); // Vertical seeds
        distances = sheet.getDistanceImage();

        for (StaffInfo staff : system.getStaves()) {
            logger.debug("Staff #{}", staff.getId());
            watch.start("Staff #" + staff.getId());

            // First, process all seed-based heads for the staff
            List<Inter> ch = processStaff(staff, true);

            // Consider seed-based heads as competitors for x-based heads
            systemCompetitors.addAll(ch);
            Collections.sort(systemCompetitors, Inter.byOrdinate);

            // Second, process x-based heads for the staff
            ch.addAll(processStaff(staff, false));

            // Finally, detect heads overlaps for current staff
            flagOverlaps(ch);
        }

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //----------------//
    // buildTemplates //
    //----------------//
    /**
     * Populate the map of desired templates, scaled at proper value.
     *
     * @return the map to use for template matching
     */
    private int buildTemplates ()
    {
        final int interline = scale.getInterline();
        int minWidth = Integer.MAX_VALUE;

        final Set<Shape> allShapes = new HashSet<Shape>();
        allShapes.addAll(evens);
        allShapes.addAll(odds);

        for (Shape shape : allShapes) {
            Template tpl = factory.getTemplate(shape, interline);
            minWidth = Math.min(minWidth, tpl.getWidth());
            templates.put(shape, tpl);
        }

        return minWidth;
    }

    //-------------//
    // createInter //
    //-------------//
    /**
     * Create the interpretation that corresponds to the match found.
     *
     * @param loc    location of the match
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
        final Template tpl = templates.get(shape);
        final Rectangle box = tpl.getBoundsAt(loc.x, loc.y, anchor);
        final double grade = Grades.clamp(
                1 - (loc.d / params.maxMatchingDistance));

        // Is grade acceptable?
        if (grade < AbstractInter.getMinGrade()) {
            logger.debug(
                    "Too weak {} dist: {} grade: {} at {}",
                    shape,
                    String.format("%.3f", loc.d),
                    String.format("%.2f", grade),
                    box);

            return null;
        } else {
            final Inter inter = interOf(shape, box, grade, pitch);
            sig.addVertex(inter);

            if (logger.isDebugEnabled()) {
                logger.info(
                        "Created {} at {} dist:{}",
                        inter,
                        GeoUtil.centerOf(box),
                        String.format("%.3f", loc.d));
            }

            return inter;
        }
    }

    //---------------//
    // filterMatches //
    //---------------//
    private List<PixelDistance> filterMatches (List<PixelDistance> rawLocs)
    {
        // Sort by increasing template distance
        Collections.sort(rawLocs);

        // Gather matches per close locations
        // Avoid duplicate locations
        List<Aggregate> aggregates = new ArrayList<Aggregate>();

        for (PixelDistance loc : rawLocs) {
            // Check among already filtered locations for similar location
            Aggregate aggregate = null;

            for (Aggregate ag : aggregates) {
                Point p = ag.point;
                int dx = loc.x - p.x;
                int dy = loc.y - p.y;

                if (Math.abs(dx) <= params.maxTemplateDelta) {
                    aggregate = ag;

                    break;
                }
            }

            if (aggregate == null) {
                aggregate = new Aggregate();
                aggregates.add(aggregate);
            }

            aggregate.add(loc);
        }

        List<PixelDistance> filtered = new ArrayList<PixelDistance>();

        for (Aggregate ag : aggregates) {
            filtered.add(ag.getMeanLocation());
        }

        return filtered;
    }

    //--------------//
    // flagOverlaps //
    //--------------//
    /**
     * In the provided list of interpretations, detect and flag as
     * such the overlapping ones.
     *
     * @param inters the provided interpretations (for a staff)
     */
    private void flagOverlaps (List<Inter> inters)
    {
        purgeDuplicates(inters);

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
        List<Inter> rawComps = sig.intersectedInters(
                systemCompetitors,
                GeoOrder.BY_ORDINATE,
                area);

        // Keep only the "good" interpretations
        List<Inter> kept = new ArrayList<Inter>();

        for (Inter inter : rawComps) {
            if (inter.isGood()) {
                kept.add(inter);
            }
        }

        // Sort by abscissa for easier lookup
        Collections.sort(kept, Inter.byAbscissa);

        return kept;
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
        List<Glyph> seeds = new ArrayList<Glyph>(
                Glyphs.intersectedGlyphs(systemSeeds, area));
        Collections.sort(seeds, Glyph.byAbscissa);

        return seeds;
    }

    //----------------------//
    // getSystemCompetitors //
    //----------------------//
    /**
     * Retrieve the collection of (good) other interpretations that
     * might compete with void heads and whole note candidates.
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
                        return competingShapes.contains(inter.getShape());
                    }
                });

        Collections.sort(comps, Inter.byOrdinate);

        return comps;
    }

    //----------------//
    // getSystemSeeds //
    //----------------//
    /**
     * Retrieves the vertical stem seeds for the system
     *
     * @return the collection of system stem seeds, sorted by ordinate
     */
    private List<Glyph> getSystemSeeds ()
    {
        List<Glyph> seeds = new ArrayList<Glyph>();

        for (Glyph glyph : system.getGlyphs()) {
            if (glyph.getShape() == Shape.VERTICAL_SEED) {
                seeds.add(glyph);
            }
        }

        Collections.sort(seeds, Glyph.byOrdinate);

        return seeds;
    }

    //---------//
    // interOf //
    //---------//
    /**
     * Create proper inter for shape
     *
     * @param shape the shape used for the template
     * @param box   the template bounds
     * @param grade assignment quality
     * @param pitch note pitch
     * @return the created inter
     */
    private Inter interOf (Shape shape,
                           Rectangle box,
                           double grade,
                           int pitch)
    {
        switch (shape) {
        case NOTEHEAD_BLACK:
            return new BlackHeadInter(box, grade, pitch);

        case VOID_ODD:
        case VOID_EVEN:
            return new VoidHeadInter(box, grade, pitch);

        case WHOLE_ODD:
        case WHOLE_EVEN:
            return new WholeInter(box, grade, pitch);
        }
        assert false : "No root shape for " + shape;

        return null;
    }

    //--------//
    // lookup //
    //--------//
    /**
     * Lookup a line in the provided direction for note candidates,
     * skipping the location already used by (good) competitors.
     * <p>
     * Abscissae tied to stem seeds are tried on left and right sides for void
     * and black heads only (no whole notes).
     * All abscissae within line range are tried for all symbols (void and black
     * heads, whole notes), but seed-tied locations are given priority over
     * these ones.
     *
     * @param prefix   a prefix used to avoid name collision between ledgers
     * @param staff    the containing staff
     * @param line     adapter to the line (staff line or ledger)
     * @param dir      the desired direction (-1: above, 0: on line, 1: below)
     * @param pitch    step pitch step position
     * @param useSeeds true for seed-based heads, false for x-based heads
     */
    private List<Inter> lookup (LineAdapter line,
                                int dir,
                                int pitch,
                                boolean useSeeds)
    {
        StaffInfo staff = line.getStaff();

        // Competitors for this horizontal slice
        final double ratio = AbstractNoteInter.getShrinkVertRatio();
        final double above = (scale.getInterline() * (dir - ratio)) / 2;
        final double below = (scale.getInterline() * (dir + ratio)) / 2;
        final Area area = line.getArea(above, below);

        if (constants.allowAttachments.isSet()) {
            staff.addAttachment(line.getPrefix() + "#" + pitch, area);
        }

        List<Inter> competitors = getCompetitorsSlice(area);
        logger.debug("lookup step: {} comps: {}", pitch, competitors.size());

        // Inters created for this line
        final List<Inter> createdInters = new ArrayList<Inter>();

        if (useSeeds) {
            // Inters tied to stem seeds
            createdInters.addAll(
                    lookupSeeds(area, line, dir, pitch, competitors));
        } else {
            // Inters not tied to stem seeds
            createdInters.addAll(lookupRange(line, dir, pitch, competitors));
        }

        return createdInters;
    }

    //-------------//
    // lookupRange //
    //-------------//
    /**
     * Lookup an area, using all abscissae in line range.
     *
     * @param area        the slice to browse
     * @param line        adapter to the underlying line (staff line or ledger)
     * @param dir         direction from the line
     * @param pitch       pitch position
     * @param competitors collection of existing competing symbols
     * @return the list of created interpretations
     * @see #lookupSeeds
     */
    private List<Inter> lookupRange (LineAdapter line,
                                     int dir,
                                     int pitch,
                                     List<Inter> competitors)
    {
        final List<Inter> createdInters = new ArrayList<Inter>();

        /** Shapes to look for */
        final List<Shape> shapes = (dir == 0) ? evens : odds;

        /** Template anchor to use */
        final Anchor anchor = (dir == 0) ? MIDDLE_LEFT
                : ((dir < 0) ? BOTTOM_LEFT : TOP_LEFT);

        /** Abscissa range for scan */
        final int scanLeft = Math.max(
                line.getLeftAbscissa(),
                (int) line.getStaff().getDmzEnd());
        final int scanRight = line.getRightAbscissa()
                              - minTemplateWidth;

        /** The shape-based locations found */
        final Map<Shape, List<PixelDistance>> locations = new HashMap<Shape, List<PixelDistance>>();

        for (Shape shape : shapes) {
            locations.put(shape, new ArrayList<PixelDistance>());
        }

        // Scan
        for (int x = scanLeft; x <= scanRight; x++) {
            final int y = line.yAt(x);

            for (Shape shape : shapes) {
                final Template tpl = templates.get(shape);
                final Rectangle tplBox = tpl.getBoundsAt(x, y, anchor);
                final Rectangle2D smallBox = shrink(tplBox);

                // Skip if location already used by good object (beam, black head)
                if (!overlap(smallBox, competitors)) {
                    final List<PixelDistance> locs = locations.get(shape);

                    // Get match value for template located at (x,y)
                    final double dist = tpl.evaluate(x, y, anchor, distances);

                    if (dist <= params.maxMatchingDistance) {
                        locs.add(new PixelDistance(x, y, dist));
                    }
                }
            }
        }

        // Filter
        for (Shape shape : shapes) {
            List<PixelDistance> locs = locations.get(shape);
            locs = filterMatches(locs);

            if (!locs.isEmpty()) {
                for (PixelDistance loc : locs) {
                    Inter inter = createInter(loc, anchor, shape, pitch);

                    if (inter != null) {
                        createdInters.add(inter);
                    }
                }
            }
        }

        return createdInters;
    }

    //-------------//
    // lookupSeeds //
    //-------------//
    /**
     * Lookup an area, only at abscissae tied to existing stem seeds.
     * Whole notes cannot be searched for at these locations.
     *
     * @param area        the slice to browse
     * @param line        adapter to the underlying line (staff line or ledger)
     * @param dir         direction from the line
     * @param pitch       pitch position
     * @param competitors collection of existing competing symbols
     * @return the list of created interpretations
     * @see #lookupRange
     */
    private List<Inter> lookupSeeds (Area area,
                                     LineAdapter line,
                                     int dir,
                                     int pitch,
                                     List<Inter> competitors)
    {
        final List<Inter> createdInters = new ArrayList<Inter>();

        // Shapes to look for
        final List<Shape> shapes = new ArrayList<Shape>(
                (dir == 0) ? evens : odds);
        shapes.removeAll(noStems);

        // Intersected seeds in the area
        final List<Glyph> seeds = getSeedsSlice(area);

        // Use one anchor for each horizontal side of the stem seed
        final Anchor[] anchors = new Anchor[]{
            // On left of stem
            (dir == 0) ? LEFT_STEM : ((dir < 0) ? BOTTOM_LEFT_STEM : TOP_LEFT_STEM),
            // On right of stem
            (dir == 0) ? RIGHT_STEM : ((dir < 0) ? BOTTOM_RIGHT_STEM : TOP_RIGHT_STEM)
        };

        for (Glyph seed : seeds) {
            // Compute precise stem link point
            // x value is imposed by seed alignment, y value by line
            int x = GeoUtil.centerOf(seed.getBounds()).x; // Rough x value
            final int y = line.yAt(x); // Precise y value
            final Point2D top = seed.getStartPoint(Orientation.VERTICAL);
            final Point2D bot = seed.getStopPoint(Orientation.VERTICAL);
            final Point2D pt = LineUtil.intersectionAtY(top, bot, y);
            x = (int) Math.rint(pt.getX()); // Precise x value

            for (Anchor anchor : anchors) {
                for (Shape shape : shapes) {
                    final Template tpl = templates.get(shape);
                    final Rectangle tplBox = tpl.getBoundsAt(x, y, anchor);
                    final Rectangle2D smallBox = shrink(tplBox);

                    // Skip if location already used by good object (beam, black head)
                    if (!overlap(smallBox, competitors)) {
                        final double dist = tpl.evaluate(
                                x,
                                y,
                                anchor,
                                distances);

                        if (dist <= params.maxMatchingDistance) {
                            PixelDistance loc = new PixelDistance(x, y, dist);
                            Inter inter = createInter(
                                    loc,
                                    anchor,
                                    shape,
                                    pitch);

                            if (inter != null) {
                                createdInters.add(inter);
                            }
                        }
                    }
                }
            }
        }

        return createdInters;
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
            Rectangle cBox = comp.getBounds();

            if (cBox.intersects(box)) {
                return true;
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
     *
     * @param staff the staff to process
     * @param seeds should we stick to stem seeds or not?
     * @return the list of created heads
     */
    private List<Inter> processStaff (StaffInfo staff,
                                      boolean seeds)
    {
        List<Inter> ch = new ArrayList<Inter>(); // Created heads

        // Use all staff lines
        boolean isFirstLine = true;
        int pitch = -5; // Current pitch

        for (LineInfo line : staff.getLines()) {
            LineAdapter adapter = new StaffLineAdapter(staff, line);

            // For first line only, look right above
            if (isFirstLine) {
                isFirstLine = false;
                ch.addAll(lookup(adapter, -1, pitch++, seeds));
            }

            // Look right on line, then just below
            ch.addAll(lookup(adapter, 0, pitch++, seeds));
            ch.addAll(lookup(adapter, +1, pitch++, seeds));
        }

        // Use all ledgers, above staff, then below staff
        for (int dir : new int[]{-1, 1}) {
            pitch = dir * 4;

            for (int i = dir;; i += dir) {
                SortedSet<Glyph> set = staff.getLedgers(i);

                if ((set == null) || set.isEmpty()) {
                    break;
                }

                char c = 'a';
                pitch += (2 * dir);

                for (Glyph ledger : set) {
                    String p = "" + c++;
                    LineAdapter adapter = new LedgerAdapter(staff, p, ledger);
                    // Look right on ledger, then just further from staff
                    ch.addAll(lookup(adapter, 0, pitch, seeds));
                    ch.addAll(lookup(adapter, dir, pitch + dir, seeds));
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
        Collections.sort(inters, Inter.byAbscissa);

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
                    logger.info("Purging {} at {}", inter, inter.getBounds());
                }

                sig.removeVertex(inter);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Aggregate //
    //-----------//
    /**
     * Describes an aggregate of matches around similar location.
     */
    private static class Aggregate
    {
        //~ Instance fields ----------------------------------------------------

        Point point;

        List<PixelDistance> matches = new ArrayList<PixelDistance>();

        //~ Methods ------------------------------------------------------------
        public void add (PixelDistance match)
        {
            if (point == null) {
                point = new Point(match.x, match.y);
            }

            matches.add(match);
        }

        /**
         * Use barycenter (with weights decreasing with distance? no!)
         *
         * @return a mean location
         */
        public PixelDistance getMeanLocation ()
        {
            return matches.get(0);
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("{");
            sb.append(getClass().getSimpleName());

            if (point != null) {
                sb.append(" point:(")
                        .append(point.x)
                        .append(",")
                        .append(point.y)
                        .append(")");
            }

            sb.append(" ")
                    .append(matches.size())
                    .append(" matches: ");

            for (PixelDistance match : matches) {
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
        //~ Instance fields ----------------------------------------------------

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
                "distance**2",
                1.5,
                "Maximum (square) matching distance");

        final Scale.Fraction maxTemplateDelta = new Scale.Fraction(
                0.75,
                "Maximum dx or dy between similar template instances");

        final Constant.Ratio shrinkHoriRatio = new Constant.Ratio(
                0.7,
                "Shrink horizontal ratio to apply when checking for overlap");

        final Constant.Ratio shrinkVertRatio = new Constant.Ratio(
                0.5,
                "Shrink vertical ratio to apply when checking for overlap");

    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants.
     */
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final double maxMatchingDistance;

        final int maxTemplateDelta;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            maxMatchingDistance = constants.maxMatchingDistance.getValue();
            maxTemplateDelta = scale.toPixels(constants.maxTemplateDelta);
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
        //~ Instance fields ----------------------------------------------------

        private final Glyph ledger;

        private final Point2D left;

        private final Point2D right;

        //~ Constructors -------------------------------------------------------
        public LedgerAdapter (StaffInfo staff,
                              String prefix,
                              Glyph ledger)
        {
            super(staff, prefix);
            this.ledger = ledger;
            left = ledger.getStartPoint(Orientation.HORIZONTAL);
            right = ledger.getStopPoint(Orientation.HORIZONTAL);
        }

        //~ Methods ------------------------------------------------------------
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
            return (int) Math.rint(
                    LineUtil.intersectionAtX(left, right, x).getY());
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
        //~ Instance fields ----------------------------------------------------

        private final StaffInfo staff;

        private final String prefix;

        //~ Constructors -------------------------------------------------------
        public LineAdapter (StaffInfo staff,
                            String prefix)
        {
            this.staff = staff;
            this.prefix = prefix;
        }

        //~ Methods ------------------------------------------------------------
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
        //~ Instance fields ----------------------------------------------------

        private final LineInfo line;

        //~ Constructors -------------------------------------------------------
        public StaffLineAdapter (StaffInfo staff,
                                 LineInfo line)
        {
            super(staff, "");
            this.line = line;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public Area getArea (double above,
                             double below)
        {
            FilamentLine fLine = (FilamentLine) line;
            NaturalSpline spline = fLine.getFilament()
                    .getAlignment()
                    .getLine();

            GeoPath path = new GeoPath();
            AffineTransform at;

            // Top line
            at = AffineTransform.getTranslateInstance(0, above);
            path.append(spline.getPathIterator(at), false);

            // Bottom line (reversed)
            at = AffineTransform.getTranslateInstance(0, below + 1);
            path.append(
                    ReversePathIterator.getReversePathIterator(spline, at),
                    true);

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
    }
}
