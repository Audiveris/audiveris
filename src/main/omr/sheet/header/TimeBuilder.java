//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T i m e B u i l d e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.header;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphClassifier;
import omr.glyph.GlyphCluster;
import omr.glyph.GlyphLayer;
import omr.glyph.GlyphLink;
import omr.glyph.GlyphNest;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.ShapeEvaluator;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.SectionFactory;

import omr.math.Projection;

import omr.score.TimeRational;
import omr.score.TimeValue;

import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.header.HeaderBuilder.Plotter;
import static omr.sheet.header.TimeBuilder.TimeKind.*;
import static omr.sheet.symbol.SymbolsFilter.SYMBOL_ORIENTATION;

import omr.sig.SIGraph;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.Inter;
import omr.sig.inter.TimeInter;
import omr.sig.inter.TimeNumberInter;
import omr.sig.inter.TimePairInter;
import omr.sig.inter.TimeWholeInter;
import omr.sig.relation.Exclusion;
import omr.sig.relation.TimeNumberRelation;

import omr.util.Navigable;

import ij.process.Blitter;
import ij.process.ByteProcessor;

import org.jfree.data.xy.XYSeries;

import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Class {@code TimeBuilder} extracts a time signature (such as 4/4 or C) from a staff.
 * It is meant to be used at the beginning of a staff (in StaffHeader) or later along the staff.
 * <p>
 * Within a system, a colum of time signatures must contain only identical signatures.
 * <p>
 * The staff region that follows the staff clef (and staff key-sig if any) is searched for presence
 * of chunks.
 * A global match can be tried for COMMON_TIME and for CUT_TIME shapes.
 * All other shapes combine a numerator and a denominator, hence the area is split using middle
 * staff line to ease the recognition of individual numbers.
 *
 * @author Hervé Bitteur
 */
public class TimeBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(TimeBuilder.class);

    /** Possible shapes for whole time signatures. */
    private static final EnumSet<Shape> wholeShapes = ShapeSet.WholeTimes;

    /** Possible shapes for top or bottom half of time signatures. */
    private static final EnumSet<Shape> halfShapes = EnumSet.copyOf(ShapeSet.PartialTimes);

    //~ Enumerations -------------------------------------------------------------------------------
    /**
     * The different parts of a time signature.
     */
    public static enum TimeKind
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Whole signature (common or cut). */
        WHOLE,
        /** Upper half (numerator number). */
        NUM,
        /** Lower half (denominator number). */
        DEN;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Dedicated staff to analyze. */
    private final Staff staff;

    /** Time range info. */
    private final StaffHeader.Range range;

    /** The containing system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related SIG. */
    private final SIGraph sig;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Related scale. */
    private final Scale scale;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Shape classifier to use. */
    private final ShapeEvaluator classifier = GlyphClassifier.getInstance();

    /** Projection of foreground pixels, indexed by abscissa. */
    private final Projection projection;

    /** Region of interest for time-sig. */
    private final Rectangle roi;

    /** Adapters for glyph building. */
    private final Map<TimeKind, TimeAdapter> adapters = new EnumMap<TimeKind, TimeAdapter>(
            TimeKind.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimeBuilder} object.
     *
     * @param staff       the underlying staff
     * @param browseStart estimated beginning abscissa for browsing time-sig
     */
    public TimeBuilder (Staff staff,
                        int browseStart)
    {
        this.staff = staff;

        system = staff.getSystem();
        sig = system.getSig();
        sheet = system.getSheet();
        scale = sheet.getScale();
        params = new Parameters(scale);

        final StaffHeader header = staff.getHeader();

        if (header.timeRange != null) {
            range = header.timeRange;
        } else {
            header.timeRange = (range = new StaffHeader.Range());
            range.browseStart = browseStart;
        }

        roi = getRoi();
        projection = getProjection();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "TimeBuilder#" + staff.getId();
    }

    //---------//
    // addPlot //
    //---------//
    /**
     * Augment the provided plotter with projection data pertaining to time signature
     *
     * @param plotter the plotter to augment
     */
    protected void addPlot (Plotter plotter)
    {
        final int xMin = projection.getStart();
        final int xMax = projection.getStop();

        {
            // Values
            XYSeries series = new XYSeries("Time");
            series.add(xMin, -Plotter.MARK);

            for (int x = xMin; x <= xMax; x++) {
                series.add(x, projection.getValue(x));
            }

            series.add(xMax, -Plotter.MARK);

            plotter.add(series, Color.BLUE, false);
        }

        if (range.start != 0) {
            // Area limits
            XYSeries series = new XYSeries("TimeArea");
            int start = range.start;
            int stop = range.stop;
            series.add(start, -Plotter.MARK);
            series.add(start, staff.getHeight());
            series.add(stop, staff.getHeight());
            series.add(stop, -Plotter.MARK);
            plotter.add(series, Color.MAGENTA, false);
        }
    }

    //---------------------//
    // findStaffCandidates //
    //---------------------//
    /**
     * Retrieve all acceptable candidates (whole or half) for this staff.
     * <p>
     * All acceptable candidates are stored as Inter instances in system sig.
     *
     * @return true if success, false otherwise (when no candidate at all has been found)
     */
    protected boolean findStaffCandidates ()
    {
        // Projection can help refine the abscissa range
        // Small space before, suitable range for time sig, large space after
        browseProjection();

        if (range.start == 0) {
            return false;
        }

        // Look for a whole time sig (only common or cut)
        List<Inter> wholes = processWhole();

        // Look for top and bottom halves
        List<Inter> nums = processHalf(NUM);
        List<Inter> dens = processHalf(DEN);

        if (dens.isEmpty()) {
            // Any num half needs a den half, so delete all nums
            if (!nums.isEmpty()) {
                logger.debug("Staff#{} num without den", staff.getId());

                Map<Shape, Inter> map = adapters.get(NUM).bestMap;

                for (Inter num : nums) {
                    map.remove(num.getShape());
                    num.delete();
                }

                nums.clear();
            }
        } else if (nums.isEmpty()) {
            // Any den half needs a num half, so delete all dens
            if (!dens.isEmpty()) {
                logger.debug("Staff#{} den without num", staff.getId());

                Map<Shape, Inter> map = adapters.get(DEN).bestMap;

                for (Inter den : dens) {
                    map.remove(den.getShape());
                    den.delete();
                }

                dens.clear();
            }
        } else {
            for (Inter topInter : nums) {
                TimeNumberInter top = (TimeNumberInter) topInter;

                for (Inter bottomInter : dens) {
                    TimeNumberInter bottom = (TimeNumberInter) bottomInter;

                    // Restrict num/den pairs to supported combinations
                    TimeRational nd = new TimeRational(top.getValue(), bottom.getValue());

                    if (TimeInter.isSupported(nd)) {
                        // Halves support each other
                        sig.addEdge(top, bottom, new TimeNumberRelation());
                    } else {
                        sig.insertExclusion(top, bottom, Exclusion.Cause.INCOMPATIBLE);
                    }
                }
            }
        }

        return !wholes.isEmpty() || !nums.isEmpty();
    }

    //------------------//
    // browseProjection //
    //------------------//
    /**
     * Analyze projection data to refine time sig abscissa range.
     * We expect a small space before, suitable range for time sig, space after.
     * Output: range.start & range.stop.
     */
    private void browseProjection ()
    {
        List<Space> spaces = getSpaces();

        // We need at least space before and space after
        if (spaces.size() < 2) {
            logger.debug("Staff#{} lacking spaces around time sig", staff.getId());

            return;
        }

        Space first = spaces.get(0);

        // Check distance from roi start
        int delta = first.stop - roi.x;

        if (delta > params.maxFirstSpaceWidth) {
            logger.debug(
                    "Staff#{} too large space before time sig: {} vs {}",
                    staff.getId(),
                    delta,
                    params.maxFirstSpaceWidth);

            return;
        }

        range.start = first.stop;
        range.stop = spaces.get(spaces.size() - 1).start;
    }

    //-----------//
    // createSig //
    //-----------//
    /**
     * Actually assign the time signature to the staff.
     *
     * @param bestTimeInter the time inter instance for this staff
     */
    private void createSig (TimeInter bestTimeInter)
    {
        // Store time ending abscissa for this staff
        if (bestTimeInter != null) {
            bestTimeInter.setStaff(staff);

            // If best time is a whole signature (common/cut) it is already in SIG.
            // If it is a pair, only the halves (num & den) are already in SIG, so save the pair.
            if (bestTimeInter instanceof TimePairInter) {
                sig.addVertex(bestTimeInter);
            }

            Rectangle timeBox = bestTimeInter.getSymbolBounds(scale.getInterline());
            int end = timeBox.x + timeBox.width;
            staff.setTimeStop(end);

            staff.getHeader().time = bestTimeInter;
        }
    }

    //----------------------//
    // discardOtherMaterial //
    //----------------------//
    /**
     * Discard all inters that do not pertain to chosen time signature.
     * <p>
     * This accounts for other WholeTimeInter instances and to all TimeNumberInter instances that
     * do not correspond to chosen num/den.
     *
     * @param bestTime the time inter chosen for this staff
     */
    private void discardOtherMaterial (TimeValue bestTime)
    {
        if (bestTime.shape != null) {
            // Discard other wholes
            for (Inter inter : adapters.get(WHOLE).bestMap.values()) {
                if (inter.getShape() != bestTime.shape) {
                    inter.delete();
                }
            }

            // Discard all num & den numbers
            sig.deleteInters(adapters.get(NUM).bestMap.values());
            sig.deleteInters(adapters.get(DEN).bestMap.values());
        } else {
            // Discard all wholes
            sig.deleteInters(adapters.get(WHOLE).bestMap.values());

            // Discard num's different from chosen num
            for (Inter inter : adapters.get(NUM).bestMap.values()) {
                TimeNumberInter number = (TimeNumberInter) inter;

                if (number.getValue() != bestTime.timeRational.num) {
                    inter.delete();
                }
            }

            // Discard den's different from chosen den
            for (Inter inter : adapters.get(DEN).bestMap.values()) {
                TimeNumberInter number = (TimeNumberInter) inter;

                if (number.getValue() != bestTime.timeRational.den) {
                    inter.delete();
                }
            }
        }
    }

    //----------//
    // getParts //
    //----------//
    /**
     * Retrieve all glyph instances that could be part of time sig.
     *
     * @return time sig possible parts
     */
    private List<Glyph> getParts (Rectangle rect)
    {
        // Grab pixels out of staff-free source
        ByteProcessor source = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);
        ByteProcessor buf = new ByteProcessor(rect.width, rect.height);
        buf.copyBits(source, -rect.x, -rect.y, Blitter.COPY);

        // Extract parts
        //        SectionFactory sectionFactory = new SectionFactory(VERTICAL, new JunctionRatioPolicy());
        //        List<Section> sections = sectionFactory.createSections(buf, rect.getLocation());
        // Needs a lag
        final String name = "LagForTime";
        Lag lag = sheet.getLagManager().getLag(name);

        if (lag == null) {
            lag = new BasicLag(name, SYMBOL_ORIENTATION);
            sheet.getLagManager().setLag(name, lag);

            ////new LagController(sheet, lag, name).refresh();
        }

        SectionFactory sectionFactory = new SectionFactory(lag, new JunctionRatioPolicy());
        List<Section> sections = sectionFactory.createSections(buf, rect.getLocation());
        List<Glyph> parts = sheet.getGlyphNest().retrieveGlyphs(
                sections,
                GlyphLayer.SYMBOL,
                true);

        // Keep only interesting parts
        purgeParts(parts, rect);

        return parts;
    }

    //---------------//
    // getProjection //
    //---------------//
    /**
     * We use the NO_STAFF source of pixels.
     *
     * @return the projection on x-axis
     */
    private Projection getProjection ()
    {
        // Staff-free pixel source
        final ByteProcessor source = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);
        final int xMin = roi.x;
        final int xMax = (roi.x + roi.width) - 1;
        final Projection table = new Projection.Short(xMin, xMax);

        for (int x = xMin; x <= xMax; x++) {
            short cumul = 0;

            for (int y = roi.y, yBreak = roi.y + roi.height; y < yBreak; y++) {
                if (source.get(x, y) == 0) {
                    cumul++;
                }
            }

            table.increment(x, cumul);
        }

        return table;
    }

    //--------//
    // getRoi //
    //--------//
    /**
     * Define the region of interest to browse for time signature.
     *
     * @param start left abscissa
     * @return the roi rectangle
     */
    private Rectangle getRoi ()
    {
        final int start = range.browseStart;
        int stop = (start + params.roiWidth) - 1;

        // Check bar line for end of ROI
        for (BarlineInter bar : staff.getBars()) {
            if (!bar.isGood()) {
                continue;
            }

            int barStart = bar.getBounds().x;

            if ((barStart > start) && (barStart <= stop)) {
                logger.debug("Staff#{} stopping time search before {}", staff.getId(), bar);
                stop = barStart - 1;

                break;
            }
        }

        final int top = Math.min(staff.getFirstLine().yAt(start), staff.getFirstLine().yAt(stop));
        final int bottom = Math.max(staff.getLastLine().yAt(stop), staff.getLastLine().yAt(stop));
        range.browseStop = stop;

        return new Rectangle(start, top, stop - start + 1, bottom - top + 1);
    }

    //-----------//
    // getSpaces //
    //-----------//
    private List<Space> getSpaces ()
    {
        final int xMin = roi.x;
        final int xMax = (roi.x + roi.width) - 1;
        List<Space> spaces = new ArrayList<Space>();

        // Space parameters
        int spaceStart = -1; // Space start abscissa
        int spaceStop = -1; // Space stop abscissa

        for (int x = xMin; x <= xMax; x++) {
            int cumul = projection.getValue(x);

            if (cumul <= params.maxSpaceCumul) {
                // We are in a space
                if (spaceStart == -1) {
                    // Start of space
                    spaceStart = x;
                }

                spaceStop = x;
            } else {
                // We are NOT in a space
                if (spaceStart != -1) {
                    // End of space
                    spaces.add(new Space(spaceStart, spaceStop));
                    spaceStart = -1;
                }
            }
        }

        // Finish ongoing space if any
        if (spaceStart != -1) {
            spaces.add(new Space(spaceStart, spaceStop));
        }

        return spaces;
    }

    //-------------//
    // processHalf //
    //-------------//
    /**
     * Lookup staff header for half time-sig candidates.
     *
     * @param half which half (top or bottom) is being searched for
     * @return the candidates found (TimeNumberInter instances created in SIG)
     */
    private List<Inter> processHalf (TimeKind half)
    {
        List<Inter> inters = new ArrayList<Inter>();

        // Define proper rectangular search area for this side
        final int top = roi.y + ((half == NUM) ? 0 : (roi.height / 2));
        final int width = range.stop - range.start + 1;
        final Rectangle rect = new Rectangle(range.start, top, width, roi.height / 2);
        rect.grow(0, -params.yMargin);
        staff.addAttachment("T" + ((half == NUM) ? "N" : "D"), rect);

        //TODO: Should not try each and every combination of parts
        // Perhaps take all of them (or the largest ones?) and evaluate the compound
        List<Glyph> parts = getParts(rect);
        HalfAdapter adapter = new HalfAdapter(half, parts);
        adapters.put(half, adapter);

        new GlyphCluster(adapter).decompose();
        logger.debug(
                "Staff#{} {} {} trials:{}",
                staff.getId(),
                half,
                Glyphs.toString("parts", parts),
                adapter.trials);

        if (!adapter.bestMap.isEmpty()) {
            for (Entry<Shape, Inter> entry : adapter.bestMap.entrySet()) {
                ///sheet.getGlyphNest().registerGlyph(adapter.bestGlyph);
                Inter inter = entry.getValue();

                Rectangle timeBox = inter.getSymbolBounds(scale.getInterline());
                inter.setBounds(timeBox);

                int gid = inter.getGlyph().getId();
                sig.addVertex(inter);
                inters.add(inter);
                logger.debug("Staff#{} {} {} g#{} {}", staff.getId(), half, inter, gid, timeBox);
            }
        }

        return inters;
    }

    //--------------//
    // processWhole //
    //--------------//
    /**
     * Lookup staff header for whole time-sig candidates.
     *
     * @return the candidates found (TimeWholeInter instances created in SIG)
     */
    private List<Inter> processWhole ()
    {
        List<Inter> inters = new ArrayList<Inter>();

        // Define proper rectangular search area for a whole time-sig
        Rectangle rect = new Rectangle(
                range.start,
                roi.y,
                range.stop - range.start + 1,
                roi.height);
        rect.grow(0, -params.yMargin);
        staff.addAttachment("TF", rect);

        List<Glyph> parts = getParts(rect);
        TimeAdapter wholeAdapter = new WholeAdapter(parts);
        adapters.put(WHOLE, wholeAdapter);
        new GlyphCluster(wholeAdapter).decompose();
        logger.debug(
                "Staff#{} WHOLE {} trials:{}",
                staff.getId(),
                Glyphs.toString("parts", parts),
                wholeAdapter.trials);

        if (!wholeAdapter.bestMap.isEmpty()) {
            for (Entry<Shape, Inter> entry : wholeAdapter.bestMap.entrySet()) {
                ///sheet.getGlyphNest().registerGlyph(adapter.bestGlyph);
                Inter inter = entry.getValue();

                Rectangle timeBox = inter.getSymbolBounds(scale.getInterline());
                inter.setBounds(timeBox);

                int gid = inter.getGlyph().getId();
                sig.addVertex(inter);
                inters.add(inter);
                logger.debug("Staff#{} {} g#{} {}", staff.getId(), inter, gid, timeBox);
            }
        }

        return inters;
    }

    //------------//
    // purgeParts //
    //------------//
    /**
     * Purge the population of parts candidates as much as possible, since the cost
     * of their later combinations is worse than exponential.
     *
     * @param parts the collection to purge
     * @param rect  the slice rectangle
     */
    private void purgeParts (List<Glyph> parts,
                             Rectangle rect)
    {
        //        // The rect is used for cropping only.
        //        // Use a smaller core rectangle which must be intersected by any part candidate
        //        Rectangle core = new Rectangle(rect);
        //        core.grow(-params.xCoreMargin, -params.yCoreMargin);
        //        staff.addAttachment("c", core);
        //
        //        List<Glyph> toRemove = new ArrayList<Glyph>();
        //
        //        for (Glyph part : parts) {
        //            if ((part.getWeight() < params.minPartWeight) || !part.getBounds().intersects(core)) {
        //                toRemove.add(part);
        //            }
        //        }
        //
        //        if (!toRemove.isEmpty()) {
        //            parts.removeAll(toRemove);
        //        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Column //
    //--------//
    /**
     * Manages the system consistency for a column of staves TimeBuilder instances.
     */
    public static class Column
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final SystemInfo system;

        /** Best time value found, if any. */
        private TimeValue bestTime;

        /** Map of time builders. (one per staff) */
        private final Map<Staff, TimeBuilder> builders = new TreeMap<Staff, TimeBuilder>(
                Staff.byId);

        //~ Constructors ---------------------------------------------------------------------------
        public Column (SystemInfo system)
        {
            this.system = system;
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------//
        // addPlot //
        //---------//
        /**
         * Contribute to the staff plotter with data related to time signature.
         *
         * @param plotter the staff plotter to populate
         * @param staff   the desired staff
         * @return the time chosen to be added to plot title
         */
        public String addPlot (Plotter plotter,
                               Staff staff)
        {
            int browseStart = staff.getHeaderStop();
            TimeBuilder builder = new TimeBuilder(staff, browseStart);

            builder.addPlot(plotter);

            TimeInter timeInter = staff.getHeader().time;

            if (timeInter != null) {
                return "time:" + timeInter.getValue();
            } else {
                return null;
            }
        }

        //--------------//
        // retrieveTime //
        //--------------//
        /**
         * This is the main entry point for header time signature, it retrieves the
         * column of staves candidates time signatures, and select the best one at
         * system level.
         *
         * @param projectionWidth desired width for projection
         * @return the ending abscissa offset of time-sig column WRT measure start
         */
        public int retrieveTime (int projectionWidth)
        {
            // Allocate one time-sig builder for each staff within system
            for (Staff staff : system.getStaves()) {
                int browseStart = staff.getHeaderStop();
                builders.put(staff, new TimeBuilder(staff, browseStart));
            }

            // Process each staff on turn
            for (TimeBuilder builder : builders.values()) {
                // This fails if no candidate at all is found in staff
                if (!builder.findStaffCandidates()) {
                    // Clean up what has been constructed
                    cleanup();

                    return 0; // We failed to find a time sig in system header
                }
            }

            // Check time sig consistency at system level
            checkConsistency();

            // Push abscissa end for each StaffHeader
            int maxTimeOffset = 0;

            for (Staff staff : system.getStaves()) {
                int measureStart = staff.getHeaderStart();
                Integer timeStop = staff.getTimeStop();

                if (timeStop != null) {
                    maxTimeOffset = Math.max(maxTimeOffset, timeStop - measureStart);
                }
            }

            return maxTimeOffset;
        }

        //------------------//
        // checkConsistency //
        //------------------//
        /**
         * Use vertical redundancy within a system column of time signatures to come up
         * with the best selection.
         * <p>
         * The selection is driven from the whole system header point of view, as follows:
         * <ol>
         * <li>For each staff, identify all the possible & supported TimeInter instances, each with
         * its own grade.</li>
         * <li>Then for each possible TimeInter value (called TimeValue), make sure it appears in
         * each staff as a TimeInter instance and assign a global grade (as average of staff-based
         * TimeInter instances for the same TimeValue).</li>
         * <li>The best system-based TimeValue is then chosen as THE time signature for this
         * system header. </li>
         * <li>All staff non compatible TimeInter instances are destroyed and the member numbers
         * that don't belong to the chosen TimeInter are destroyed.
         * (TODO: perhaps removed from SIG but saved apart and restored if ever a new TimeValue is
         * chosen based on measure intrinsic rhythm data?)</li>
         * </ol>
         */
        private void checkConsistency ()
        {
            // All time values found, organized by value and staff
            Map<TimeValue, Inter[]> vectors = getValueVectors();

            Map<TimeValue, Double> grades = new HashMap<TimeValue, Double>();

            TimeLoop:
            for (Entry<TimeValue, Inter[]> entry : vectors.entrySet()) {
                TimeValue time = entry.getKey();
                Inter[] vector = entry.getValue();

                // Check that this time is present in all staves and compute the time mean grade
                double mean = 0;

                for (Inter inter : vector) {
                    if (inter == null) {
                        logger.info(
                                "System#{} TimeValue {} not found in all staves",
                                system.getId(),
                                time);

                        continue TimeLoop;
                    }

                    mean += inter.getGrade(); // TODO: use contextual?????
                }

                mean /= vector.length;
                grades.put(time, mean);
            }

            logger.debug("System#{} Header time sig grades {}", system.getId(), grades);

            // Keep the best time value
            bestTime = findBestTime(grades);

            Inter[] bestVector = vectors.get(bestTime);
            List<Staff> staves = system.getStaves();

            for (int is = 0; is < staves.size(); is++) {
                Staff staff = staves.get(is);
                TimeBuilder builder = builders.get(staff);
                builder.createSig((TimeInter) bestVector[is]);
                builder.discardOtherMaterial(bestTime);
            }

            logger.debug("System#{} TimeSignature: {}", system.getId(), bestTime);
        }

        //---------//
        // cleanup //
        //---------//
        private void cleanup ()
        {
            for (TimeBuilder builder : builders.values()) {
                for (TimeKind kind : TimeKind.values()) {
                    TimeAdapter adapter = builder.adapters.get(kind);

                    if (adapter != null) {
                        adapter.cleanup();
                    }
                }
            }
        }

        //--------------//
        // findBestTime //
        //--------------//
        private TimeValue findBestTime (Map<TimeValue, Double> grades)
        {
            TimeValue bestTime = null;
            double bestGrade = 0;

            for (Entry<TimeValue, Double> entry : grades.entrySet()) {
                double grade = entry.getValue();

                if (grade > bestGrade) {
                    bestGrade = grade;
                    bestTime = entry.getKey();
                }
            }

            return bestTime;
        }

        //-----------------//
        // getValueVectors //
        //-----------------//
        /**
         * Report the system vector of values for each time value found.
         * One vector is an array, one element per staff, the element being the staff candidate
         * TimeInter for the desired time value, or null if the time value has no acceptable
         * candidate in this staff.
         *
         * @return the system vectors of candidates found, organized per TimeValue
         */
        private Map<TimeValue, Inter[]> getValueVectors ()
        {
            // Retrieve all occurrences of time values across staves.
            Map<TimeValue, Inter[]> values = new HashMap<TimeValue, Inter[]>();

            // Loop on system staves
            List<Staff> staves = system.getStaves();

            for (int index = 0; index < staves.size(); index++) {
                Staff staff = staves.get(index);
                TimeBuilder builder = builders.get(staff);

                // Whole candidate signatures, if any
                Map<Shape, Inter> wholeMap = builder.adapters.get(TimeKind.WHOLE).bestMap;

                for (Entry<Shape, Inter> entry : wholeMap.entrySet()) {
                    Shape shape = entry.getKey();
                    TimeValue time = new TimeValue(shape);
                    Inter inter = entry.getValue();
                    Inter[] vector = values.get(time);

                    if (vector == null) {
                        values.put(time, vector = new Inter[staves.size()]);
                    }

                    vector[index] = inter;
                }

                // Num/Den pair candidate signatures, if any
                Map<Shape, Inter> numMap = builder.adapters.get(TimeKind.NUM).bestMap;
                Map<Shape, Inter> denMap = builder.adapters.get(TimeKind.DEN).bestMap;

                for (Inter numInter : numMap.values()) {
                    TimeNumberInter num = (TimeNumberInter) numInter;

                    for (Inter denInter : denMap.values()) {
                        TimeNumberInter den = (TimeNumberInter) denInter;

                        // Check this time value is supported
                        TimeRational nd = new TimeRational(num.getValue(), den.getValue());

                        if (TimeInter.isSupported(nd)) {
                            TimePairInter pair = TimePairInter.create(num, den);
                            TimeValue time = new TimeValue(nd);
                            Inter[] vector = values.get(time);

                            if (vector == null) {
                                values.put(time, vector = new Inter[staves.size()]);
                            }

                            vector[index] = pair;
                        }
                    }
                }
            }

            return values;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction roiWidth = new Scale.Fraction(
                4.0,
                "Width of region of interest for time signature");

        private final Scale.Fraction yMargin = new Scale.Fraction(
                0.10,
                "Vertical white margin on raw rectangle");

        private final Scale.Fraction maxTimeWidth = new Scale.Fraction(
                2.0,
                "Maximum width for a time signature");

        private final Scale.Fraction minTimeWidth = new Scale.Fraction(
                1.2,
                "Minimum width for a time signature");

        private final Scale.Fraction maxPartGap = new Scale.Fraction(
                1.0,
                "Maximum distance between two parts of a single time symbol");

        private final Scale.AreaFraction minWholeTimeWeight = new Scale.AreaFraction(
                1.0,
                "Minimum weight for a whole time signature");

        private final Scale.AreaFraction minHalfTimeWeight = new Scale.AreaFraction(
                0.75,
                "Minimum weight for a half time signature");

        private final Scale.Fraction maxSpaceCumul = new Scale.Fraction(
                0.4,
                "Maximum cumul value in space");

        private final Scale.Fraction minFirstSpaceWidth = new Scale.Fraction(
                0.2,
                "Minimum initial space before time signature");

        // Beware: A too small value might miss the whole time-sig
        private final Scale.Fraction maxFirstSpaceWidth = new Scale.Fraction(
                2.5,
                "Maximum initial space before time signature");

        // Beware: A too small value might miss some time-sig items
        private final Scale.Fraction maxInnerSpace = new Scale.Fraction(
                0.5,
                "Maximum inner space within time signature");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int roiWidth;

        final int yMargin;

        final int maxTimeWidth;

        final int minTimeWidth;

        final double maxPartGap;

        final int minWholeTimeWeight;

        final int minHalfTimeWeight;

        final int maxSpaceCumul;

        final int minFirstSpaceWidth;

        final int maxFirstSpaceWidth;

        final int maxInnerSpace;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            roiWidth = scale.toPixels(constants.roiWidth);
            yMargin = scale.toPixels(constants.yMargin);
            maxTimeWidth = scale.toPixels(constants.maxTimeWidth);
            minTimeWidth = scale.toPixels(constants.minTimeWidth);
            maxPartGap = scale.toPixelsDouble(constants.maxPartGap);
            minWholeTimeWeight = scale.toPixels(constants.minWholeTimeWeight);
            minHalfTimeWeight = scale.toPixels(constants.minHalfTimeWeight);
            maxSpaceCumul = scale.toPixels(constants.maxSpaceCumul);
            minFirstSpaceWidth = scale.toPixels(constants.minFirstSpaceWidth);
            maxFirstSpaceWidth = scale.toPixels(constants.maxFirstSpaceWidth);
            maxInnerSpace = scale.toPixels(constants.maxInnerSpace);
        }
    }

    //-------//
    // Space //
    //-------//
    private static class Space
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Left abscissa. */
        protected final int start;

        /** Right abscissa. */
        protected final int stop;

        //~ Constructors ---------------------------------------------------------------------------
        public Space (int start,
                      int stop)
        {
            this.start = start;
            this.stop = stop;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder();

            sb.append("Space(").append(start).append("-").append(stop).append(")");

            return sb.toString();
        }
    }

    //-------------//
    // HalfAdapter //
    //-------------//
    /**
     * Handles the integration between glyph clustering class and time-sig environment.
     * <p>
     * For each time kind, we keep the best result found if any.
     */
    private class HalfAdapter
            extends TimeAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Which half is being searched. (NUM or DEN) */
        private final TimeKind half;

        //~ Constructors ---------------------------------------------------------------------------
        public HalfAdapter (TimeKind half,
                            List<Glyph> parts)
        {
            super(parts);
            this.half = half;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void evaluateGlyph (Glyph glyph)
        {
            trials++;

            Evaluation[] evals = classifier.getNaturalEvaluations(glyph);

            for (Shape shape : halfShapes) {
                Evaluation eval = evals[shape.ordinal()];
                double grade = Inter.intrinsicRatio * eval.grade;

                if (grade >= Grades.timeMinGrade) {
                    logger.debug("   {} eval {} for glyph#{}", half, eval, glyph.getId());

                    Inter bestInter = bestMap.get(shape);

                    if ((bestInter == null) || (bestInter.getGrade() < grade)) {
                        bestMap.put(shape, TimeNumberInter.create(glyph, shape, grade, staff));
                    }
                }
            }
        }

        @Override
        public boolean isWeightAcceptable (int weight)
        {
            return weight >= params.minHalfTimeWeight;
        }
    }

    //-------------//
    // TimeAdapter //
    //-------------//
    private abstract class TimeAdapter
            implements GlyphCluster.Adapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Graph of the connected glyphs, with their distance edges if any. */
        protected final SimpleGraph<Glyph, GlyphLink> graph;

        /** Best inter per time shape. */
        public Map<Shape, Inter> bestMap = new EnumMap<Shape, Inter>(Shape.class);

        // For debug only
        public int trials = 0;

        //~ Constructors ---------------------------------------------------------------------------
        public TimeAdapter (List<Glyph> parts)
        {
            graph = Glyphs.buildLinks(parts, params.maxPartGap);
        }

        //~ Methods --------------------------------------------------------------------------------
        public void cleanup ()
        {
            for (Inter inter : bestMap.values()) {
                inter.delete();
            }
        }

        @Override
        public List<Glyph> getNeighbors (Glyph part)
        {
            return Graphs.neighborListOf(graph, part);
        }

        @Override
        public GlyphNest getNest ()
        {
            return sheet.getGlyphNest();
        }

        @Override
        public List<Glyph> getParts ()
        {
            return new ArrayList<Glyph>(graph.vertexSet());
        }

        public Inter getSingleInter ()
        {
            for (Inter inter : bestMap.values()) {
                if (!inter.isDeleted()) {
                    return inter;
                }
            }

            return null;
        }

        @Override
        public boolean isSizeAcceptable (Rectangle box)
        {
            return box.width <= params.maxTimeWidth;
        }
    }

    //--------------//
    // WholeAdapter //
    //--------------//
    /**
     * Handles the integration between glyph clustering class and time-sig environment.
     * <p>
     * For each time kind, we keep the best result found if any.
     */
    private class WholeAdapter
            extends TimeAdapter
    {
        //~ Constructors ---------------------------------------------------------------------------

        public WholeAdapter (List<Glyph> parts)
        {
            super(parts);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void evaluateGlyph (Glyph glyph)
        {
            //TODO: check glyph centroid for a whole symbol is not too far from staff middle line
            trials++;

            Evaluation[] evals = classifier.getNaturalEvaluations(glyph);

            for (Shape shape : wholeShapes) {
                Evaluation eval = evals[shape.ordinal()];
                double grade = Inter.intrinsicRatio * eval.grade;

                if (grade >= Grades.timeMinGrade) {
                    logger.debug("   WHOLE eval {} for glyph#{}", eval, glyph.getId());

                    Inter bestInter = bestMap.get(shape);

                    if ((bestInter == null) || (bestInter.getGrade() < grade)) {
                        TimeWholeInter inter = new TimeWholeInter(glyph, shape, grade);
                        inter.setStaff(staff);
                        bestMap.put(shape, inter);
                    }
                }
            }
        }

        @Override
        public boolean isWeightAcceptable (int weight)
        {
            return weight >= params.minWholeTimeWeight;
        }
    }
}
