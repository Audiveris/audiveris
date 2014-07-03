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
package omr.sheet;

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

import omr.grid.StaffInfo;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.math.Projection;

import omr.run.Orientation;

import omr.sheet.DmzBuilder.Plotter;
import static omr.sheet.TimeBuilder.TimeKind.*;

import omr.sig.BarlineInter;
import omr.sig.Exclusion;
import omr.sig.IdemRelation;
import omr.sig.Inter;
import omr.sig.Relation;
import omr.sig.SIGraph;
import omr.sig.TimeFullInter;
import omr.sig.TimeInter;
import omr.sig.TimeNumberInter;
import omr.sig.TimeNumberRelation;
import omr.sig.TimePairInter;

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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

/**
 * Class {@code TimeBuilder} extracts a time signature (such as 4/4 or C) from a staff.
 * It is meant to be used at the beginning of a staff (in DMZ) or later along the staff.
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

    /** Possible shapes for full time signatures. */
    private static final EnumSet<Shape> fullShapes = ShapeSet.FullTimes;

    /** Possible shapes for top or bottom half of time signatures. */
    private static final EnumSet<Shape> halfShapes = EnumSet.copyOf(ShapeSet.PartialTimes);

    //~ Enumerations -------------------------------------------------------------------------------
    /**
     * The different parts of a time signature.
     */
    public static enum TimeKind
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Full signature (common or cut). */
        FULL,
        /** Upper half (numerator number). */
        NUM,
        /** Lower half (denominator number). */
        DEN;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Dedicated staff to analyze. */
    private final StaffInfo staff;

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
    private final ShapeEvaluator evaluator = GlyphClassifier.getInstance();

    /** Staff-free pixel source. */
    private final ByteProcessor staffFreeSource;

    /** Projection of foreground pixels, indexed by abscissa. */
    private final Projection projection;

    /** Precise beginning abscissa of measure. */
    private final int measureStart;

    /** Region of interest for time-sig. */
    private final Rectangle roi;

    /** Detected start abscissa, if any, of first pixel in time-sig. */
    private Integer areaStart;

    /** Detected stop abscissa, if any, of last pixel in time-sig. */
    private Integer areaStop;

    /** Lag for glyph sections. */
    private final Lag lag = new BasicLag("time", Orientation.VERTICAL);

    /** Adapters. */
    private final Map<TimeKind, TimeAdapter> adapters = new EnumMap<TimeKind, TimeAdapter>(
            TimeKind.class);

    /** Resulting time signature, if any. */
    private TimeInter timeSignature;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimeBuilder} object.
     *
     * @param staff        the underlying staff
     * @param globalWidth  global plotting width
     * @param measureStart precise beginning abscissa of measure (generally right after bar line)
     * @param browseStart  estimated beginning abscissa for browsing time-sig
     */
    public TimeBuilder (StaffInfo staff,
                        int globalWidth,
                        int measureStart,
                        int browseStart)
    {
        this.staff = staff;
        this.measureStart = measureStart;

        system = staff.getSystem();
        sig = system.getSig();
        sheet = system.getSheet();
        staffFreeSource = sheet.getPicture().getSource(Picture.SourceKey.STAFF_LINE_FREE);

        scale = sheet.getScale();
        params = new Parameters(scale);
        roi = getRoi(browseStart);
        projection = getProjection();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // addPlot //
    //---------//
    public String addPlot (Plotter plotter)
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

        if (areaStart != null) {
            // Area limits
            XYSeries series = new XYSeries("Area");
            int start = areaStart;
            int stop = areaStop;
            series.add(start, -Plotter.MARK);
            series.add(start, staff.getHeight());
            series.add(stop, staff.getHeight());
            series.add(stop, -Plotter.MARK);
            plotter.add(series, Color.MAGENTA, false);

            return "time:" + "N/D";
        } else {
            return null;
        }

        //            String timeString = "";
        //            TimeBuilder timeBuilder = timeBuilders.get(staff);
        //
        //            if (timeBuilder != null) {
        //                timeString = timeBuilder.addPlot(this);
        //            }
    }

    //----------//
    // findTime //
    //----------//
    /**
     * Retrieve all acceptable inters (full or partial)
     *
     * @return true if success, false otherwise
     */
    public boolean findTime ()
    {
        // Projection can help refine the abscissa range
        // Small space before, suitable range for time sig, large space after
        browseProjection();

        if (areaStart == null) {
            return false;
        }

        // Look for a full time sig (only common or cut)
        List<Inter> fulls = processFull();

        // Look for top and bottom halves
        List<Inter> nums = processHalf(NUM);
        List<Inter> dens = processHalf(DEN);

        if (dens.isEmpty()) {
            // Num half needs a den half
            if (!nums.isEmpty()) {
                logger.debug("Staff#{} num without den", staff.getId());

                Map<Shape, Inter> map = adapters.get(NUM).bestMap;

                for (Inter num : nums) {
                    map.remove(num.getShape());
                    sig.removeVertex(num);
                }

                nums.clear();
            }
        } else if (nums.isEmpty()) {
            // Den half needs a num half
            if (!dens.isEmpty()) {
                logger.debug("Staff#{} den without num", staff.getId());

                Map<Shape, Inter> map = adapters.get(DEN).bestMap;

                for (Inter den : dens) {
                    map.remove(den.getShape());
                    sig.removeVertex(den);
                }

                dens.clear();
            }
        } else {
            // Halves support each other
            for (Inter top : nums) {
                for (Inter bottom : dens) {
                    sig.addEdge(top, bottom, new TimeNumberRelation());
                }
            }
        }

        return !fulls.isEmpty() || !nums.isEmpty();
    }

    //--------------//
    // getSignature //
    //--------------//
    public TimeInter getSignature ()
    {
        return timeSignature;
    }

    //------------------//
    // browseProjection //
    //------------------//
    /**
     * Analyze projection data to refine time sig abscissa range.
     * We expect a small space before, suitable range for time sig, space after.
     * Output: areaStart & areaStop.
     */
    private void browseProjection ()
    {
        List<Space> spaces = getSpaces();

        // We need at lest space before and space after
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

        areaStart = first.stop;
        areaStop = spaces.get(spaces.size() - 1).start;
    }

    //-----------//
    // createSig //
    //-----------//
    private TimeInter createSig ()
    {
        TimeAdapter fullAdapter = adapters.get(FULL);
        TimeInter fullInter = (TimeFullInter) fullAdapter.getSingleInter();

        if (fullInter != null) {
            timeSignature = fullInter;
        } else {
            timeSignature = TimePairInter.create(
                    (TimeNumberInter) adapters.get(NUM).getSingleInter(),
                    (TimeNumberInter) adapters.get(DEN).getSingleInter());
        }

        // Store time end for this staff
        if (timeSignature != null) {
            sig.addVertex(timeSignature);

            Rectangle timeBox = timeSignature.getSymbolBounds(scale.getInterline());
            int end = timeBox.x + timeBox.width;
            staff.setTimeStop(end);
        }

        return timeSignature;
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
        ByteProcessor source = sheet.getPicture().getSource(Picture.SourceKey.STAFF_LINE_FREE);
        ByteProcessor buf = new ByteProcessor(rect.width, rect.height);
        buf.copyBits(source, -rect.x, -rect.y, Blitter.COPY);

        // Extract parts
        SectionsBuilder builder = new SectionsBuilder(lag, new JunctionRatioPolicy());
        List<Section> sections = builder.createSections(buf, rect.getLocation());
        List<Glyph> parts = sheet.getNest().retrieveGlyphs(
                sections,
                GlyphLayer.SYMBOL,
                true, // false, // True for debugging only
                Glyph.Linking.NO_LINK);

        // Keep only interesting parts
        purgeParts(parts, rect);

        return parts;
    }

    //---------------//
    // getProjection //
    //---------------//
    /**
     * We use the STAFF_FREE source of pixels.
     *
     * @return the projection on x-axis
     */
    private Projection getProjection ()
    {
        final int xMin = roi.x;
        final int xMax = (roi.x + roi.width) - 1;
        final Projection table = new Projection.Short(xMin, xMax);

        for (int x = xMin; x <= xMax; x++) {
            short cumul = 0;

            for (int y = roi.y, yBreak = roi.y + roi.height; y < yBreak; y++) {
                if (staffFreeSource.get(x, y) == 0) {
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
    private Rectangle getRoi (int start)
    {
        // Check bar line for end of ROI
        int stop = (start + params.roiWidth) - 1;

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

        int top = Math.min(staff.getFirstLine().yAt(start), staff.getFirstLine().yAt(stop));
        int bottom = Math.max(staff.getLastLine().yAt(stop), staff.getLastLine().yAt(stop));

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
    // processFull //
    //-------------//
    private List<Inter> processFull ()
    {
        List<Inter> inters = new ArrayList<Inter>();

        // Define proper rectangular search area for a whole time-sig
        Rectangle rect = new Rectangle(areaStart, roi.y, areaStop - areaStart + 1, roi.height);
        rect.grow(0, -params.yMargin);
        staff.addAttachment("TF", rect);

        List<Glyph> parts = getParts(rect);
        TimeAdapter fullAdapter = new FullAdapter(parts);
        adapters.put(FULL, fullAdapter);
        new GlyphCluster(fullAdapter).decompose();
        logger.debug(
                "Staff#{} FULL {} trials:{}",
                staff.getId(),
                Glyphs.toString("parts", parts),
                fullAdapter.trials);

        if (!fullAdapter.bestMap.isEmpty()) {
            for (Entry<Shape, Inter> entry : fullAdapter.bestMap.entrySet()) {
                ///sheet.getNest().registerGlyph(adapter.bestGlyph);
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

    //-------------//
    // processHalf //
    //-------------//
    private List<Inter> processHalf (TimeKind half)
    {
        List<Inter> inters = new ArrayList<Inter>();

        // Define proper rectangular search area for this side
        final int top = roi.y + ((half == NUM) ? 0 : (roi.height / 2));
        final int width = areaStop - areaStart + 1;
        final Rectangle rect = new Rectangle(areaStart, top, width, roi.height / 2);
        rect.grow(0, -params.yMargin);
        staff.addAttachment("T" + ((half == NUM) ? "N" : "D"), rect);

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
                ///sheet.getNest().registerGlyph(adapter.bestGlyph);
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

        private final SIGraph sig;

        private final Sheet sheet;

        /** Scale-dependent parameters. */
        private final Parameters params;

        /** Map of time builders. (one per staff) */
        private final Map<StaffInfo, TimeBuilder> builders = new TreeMap<StaffInfo, TimeBuilder>(
                StaffInfo.byId);

        //~ Constructors ---------------------------------------------------------------------------
        public Column (SystemInfo system)
        {
            this.system = system;
            sig = system.getSig();
            sheet = system.getSheet();
            params = new Parameters(sheet.getScale());
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------//
        // addPlot //
        //---------//
        public String addPlot (Plotter plotter,
                               StaffInfo staff)
        {
            TimeBuilder builder = builders.get(staff);

            if (builder != null) {
                builder.addPlot(plotter);

                String signature = getSigString();

                return (signature != null) ? ("time:" + signature) : null;
            } else {
                logger.info("No time-sig info yet for staff#{}", staff.getId());

                return null;
            }
        }

        //--------------//
        // getSigString //
        //--------------//
        public String getSigString ()
        {
            TimeBuilder first = builders.get(system.getFirstStaff());
            TimeInter signature = first.getSignature();

            return (signature != null) ? signature.sigString() : null;
        }

        //--------------//
        // retrieveTime //
        //--------------//
        /**
         * Retrieve the column of staves time signatures.
         *
         * @param projectionWidth desired width for projection
         * @return the ending abscissa offset of time-sig column WRT measure start
         */
        public int retrieveTime (int projectionWidth)
        {
            // Retrieve DMZ time-sig
            for (StaffInfo staff : system.getStaves()) {
                int measureStart = staff.getDmzStart();
                int browseStart = staff.getDmzStop();
                builders.put(
                        staff,
                        new TimeBuilder(staff, projectionWidth, measureStart, browseStart));
            }

            // Loop on staves
            for (TimeBuilder builder : builders.values()) {
                // This may fail
                if (!builder.findTime()) {
                    // Clean up what has been constructed
                    cleanup();

                    return 0;
                }
            }

            // Check time sig consistency at system level, if applicable
            checkConsistency();

            // Create final time sig, if so needed
            for (TimeBuilder builder : builders.values()) {
                builder.createSig();
            }

            String sigString = getSigString();

            if (sigString != null) {
                logger.debug("System#{} TimeSignature: {}", system.getId(), sigString);
            }

            // Push DMZ
            int maxTimeOffset = 0;

            for (StaffInfo staff : system.getStaves()) {
                int measureStart = staff.getDmzStart();
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
         * Verify vertical consistency of a system column of time signatures.
         */
        private void checkConsistency ()
        {
            List<Relation> exclusions = new ArrayList<Relation>();
            Map<Shape, List<Inter>> fullParts = getPartitions(FULL, exclusions);
            Map<Shape, List<Inter>> numParts = getPartitions(NUM, exclusions);
            Map<Shape, List<Inter>> denParts = getPartitions(DEN, exclusions);

            List<Inter> fulls = new ArrayList<Inter>();

            for (List<Inter> list : fullParts.values()) {
                fulls.addAll(list);
            }

            List<Inter> halves = new ArrayList<Inter>();

            for (List<Inter> list : numParts.values()) {
                halves.addAll(list);
            }

            for (List<Inter> list : denParts.values()) {
                halves.addAll(list);
            }

            // Full is incompatible with either top or bottom
            for (Inter full : fulls) {
                for (Inter half : halves) {
                    exclusions.add(sig.insertExclusion(full, half, Exclusion.Cause.INCOMPATIBLE));
                }
            }

            // Let the sig reduce this sub-population
            for (Inter inter : fulls) {
                sig.computeContextualGrade(inter, false);
            }

            for (Inter inter : halves) {
                sig.computeContextualGrade(inter, false);
            }

            sig.reduceExclusions(SIGraph.ReductionMode.STRICT, exclusions);
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

        //---------------//
        // getPartitions //
        //---------------//
        /**
         * For a given time kind, partition the occurrences of shapes found across the
         * column of staves.
         * Insert exclusions between members of different partitions.
         * Insert supports between members of the same partition.
         *
         * @param kind       the kind to be processed (full, num or den)
         * @param exclusions (output) collection to be populated with added exclusions
         * @return the occurrences found, partitioned per shape
         */
        private Map<Shape, List<Inter>> getPartitions (TimeKind kind,
                                                       List<Relation> exclusions)
        {
            // Retrieve all occurrences of time shapes across staves, per kind.
            Map<Shape, List<Inter>> partitions = new EnumMap<Shape, List<Inter>>(Shape.class);

            for (TimeBuilder builder : builders.values()) {
                TimeAdapter adapter = builder.adapters.get(kind);
                Map<Shape, Inter> map = adapter.bestMap;

                for (Entry<Shape, Inter> entry : map.entrySet()) {
                    Shape shape = entry.getKey();
                    Inter inter = entry.getValue();
                    List<Inter> list = partitions.get(shape);

                    if (list == null) {
                        partitions.put(shape, list = new ArrayList<Inter>());
                    }

                    list.add(inter);
                }
            }

            // Formalize mutual support within a partition (shape is identical)
            for (Entry<Shape, List<Inter>> entry : partitions.entrySet()) {
                List<Inter> idems = entry.getValue();

                for (int i = 0; i < idems.size(); i++) {
                    Inter inter = idems.get(i);

                    for (Inter idem : idems.subList(i + 1, idems.size())) {
                        sig.addEdge(inter, idem, new IdemRelation());
                    }
                }
            }

            // Formalize exclusion across partitions (shapes are different)
            for (Shape shape : partitions.keySet()) {
                List<Inter> goods = partitions.get(shape);

                for (Entry<Shape, List<Inter>> entry : partitions.entrySet()) {
                    // No need for double exclusion ...
                    if (entry.getKey().ordinal() <= shape.ordinal()) {
                        continue;
                    }

                    List<Inter> bads = entry.getValue();

                    for (Inter good : goods) {
                        for (Inter bad : bads) {
                            exclusions.add(
                                    sig.insertExclusion(good, bad, Exclusion.Cause.INCOMPATIBLE));
                        }
                    }
                }
            }

            return partitions;
        }
    }

    //-------//
    // Space //
    //-------//
    public static class Space
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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Scale.Fraction roiWidth = new Scale.Fraction(
                4.0,
                "Width of region of interest for time signature");

        final Scale.Fraction yMargin = new Scale.Fraction(
                0.10,
                "Vertical white margin on raw rectangle");

        final Scale.Fraction maxTimeWidth = new Scale.Fraction(
                2.0,
                "Maximum width for a time signature");

        final Scale.Fraction minTimeWidth = new Scale.Fraction(
                1.2,
                "Minimum width for a time signature");

        final Scale.Fraction maxPartGap = new Scale.Fraction(
                1.0,
                "Maximum distance between two parts of a single time symbol");

        final Scale.AreaFraction minFullTimeWeight = new Scale.AreaFraction(
                1.0,
                "Minimum weight for a full time signature");

        final Scale.AreaFraction minHalfTimeWeight = new Scale.AreaFraction(
                0.75,
                "Minimum weight for a half time signature");

        final Scale.Fraction maxSpaceCumul = new Scale.Fraction(
                0.4,
                "Maximum cumul value in space");

        final Scale.Fraction minFirstSpaceWidth = new Scale.Fraction(
                0.2,
                "Minimum initial space before time signature");

        // Beware: A too small value might miss the whole time-sig
        final Scale.Fraction maxFirstSpaceWidth = new Scale.Fraction(
                2.5,
                "Maximum initial space before time signature");

        // Beware: A too small value might miss some time-sig items
        final Scale.Fraction maxInnerSpace = new Scale.Fraction(
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

        final int minFullTimeWeight;

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
            minFullTimeWeight = scale.toPixels(constants.minFullTimeWeight);
            minHalfTimeWeight = scale.toPixels(constants.minHalfTimeWeight);
            maxSpaceCumul = scale.toPixels(constants.maxSpaceCumul);
            minFirstSpaceWidth = scale.toPixels(constants.minFirstSpaceWidth);
            maxFirstSpaceWidth = scale.toPixels(constants.maxFirstSpaceWidth);
            maxInnerSpace = scale.toPixels(constants.maxInnerSpace);
        }
    }

    //-------------//
    // FullAdapter //
    //-------------//
    /**
     * Handles the integration between glyph clustering class and key-sig environment.
     * <p>
     * For each clef kind, we keep the best result found if any.
     */
    private class FullAdapter
            extends TimeAdapter
    {
        //~ Constructors ---------------------------------------------------------------------------

        public FullAdapter (List<Glyph> parts)
        {
            super(parts);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void evaluateGlyph (Glyph glyph)
        {
            //TODO: check glyph centroid for a full symbol is not too far from staff middle line
            trials++;

            Evaluation[] evals = evaluator.getNaturalEvaluations(glyph);

            for (Shape shape : fullShapes) {
                Evaluation eval = evals[shape.ordinal()];
                double grade = Inter.intrinsicRatio * eval.grade;

                if (grade >= Grades.timeMinGrade) {
                    logger.debug("   FULL eval {} for glyph#{}", eval, glyph.getId());

                    Inter bestInter = bestMap.get(shape);

                    if ((bestInter == null) || (bestInter.getGrade() < grade)) {
                        bestMap.put(shape, new TimeFullInter(glyph, shape, grade));
                    }
                }
            }
        }

        @Override
        public boolean isWeightAcceptable (int weight)
        {
            return weight >= params.minFullTimeWeight;
        }
    }

    //-------------//
    // HalfAdapter //
    //-------------//
    /**
     * Handles the integration between glyph clustering class and key-sig environment.
     * <p>
     * For each clef kind, we keep the best result found if any.
     */
    private class HalfAdapter
            extends TimeAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

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

            Evaluation[] evals = evaluator.getNaturalEvaluations(glyph);

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
                sig.removeVertex(inter);
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
            return sheet.getNest();
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
}
