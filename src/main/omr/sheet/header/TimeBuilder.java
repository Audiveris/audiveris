//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T i m e B u i l d e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.header;

import omr.classifier.Evaluation;
import omr.classifier.GlyphClassifier;
import omr.classifier.SampleRepository;
import omr.classifier.SampleSheet;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphCluster;
import omr.glyph.GlyphFactory;
import omr.glyph.GlyphIndex;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.Symbol.Group;

import omr.math.IntegerFunction;
import static omr.run.Orientation.VERTICAL;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.score.TimeRational;
import omr.score.TimeValue;

import omr.sheet.Book;
import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Scale.InterlineScale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import static omr.sheet.header.TimeBuilder.TimeKind.*;
import omr.sheet.rhythm.MeasureStack;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractTimeInter;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.Inter;
import omr.sig.inter.TimeNumberInter;
import omr.sig.inter.TimePairInter;
import omr.sig.inter.TimeWholeInter;
import omr.sig.relation.Exclusion;
import omr.sig.relation.Relation;
import omr.sig.relation.TimeTopBottomRelation;

import omr.util.ChartPlotter;
import omr.util.Navigable;
import omr.util.VerticalSide;

import ij.process.Blitter;
import ij.process.ByteProcessor;

import org.jfree.data.xy.XYSeries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code TimeBuilder} is the abstract basis for handling a time signature (such
 * as 4/4 or C) from a staff.
 * <p>
 * Subclass {@link HeaderTimeBuilder} is used at the beginning of a staff (in staff header) while
 * subclass {@link BasicTimeBuilder} is used farther down the staff.
 * <p>
 * <img src="doc-files/TimeBuilder.png">
 *
 * @author Hervé Bitteur
 */
public abstract class TimeBuilder
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
    protected final Column column;

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
    protected final Set<Glyph> glyphCandidates = new HashSet<Glyph>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimeBuilder} object.
     *
     * @param staff  the underlying staff
     * @param column the column manager
     */
    public TimeBuilder (Staff staff,
                        Column column)
    {
        this.staff = staff;
        this.column = column;

        system = staff.getSystem();
        sig = system.getSig();
        scale = system.getSheet().getScale();

        params = new Parameters(scale, staff.getSpecificInterline());
    }

    //~ Methods ------------------------------------------------------------------------------------
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

            // If best time is a whole signature (common, cut, combo) it is already in SIG.
            // If it is a pair, only the halves (num & den) are already in SIG, so save the pair.
            if (bestTimeInter instanceof TimePairInter) {
                sig.addVertex(bestTimeInter);

                TimePairInter pair = (TimePairInter) bestTimeInter;

                for (Inter inter : pair.getMembers()) {
                    inter.setGlyph(index.registerOriginal(inter.getGlyph()));
                }
            } else {
                bestTimeInter.setGlyph(index.registerOriginal(bestTimeInter.getGlyph()));
            }
        }
    }

    /**
     * Retrieve all acceptable candidates (whole or half) for this staff.
     * <p>
     * All candidates are stored as Inter instances in system sig, and in dedicated builder lists
     * (wholes, nums or dens).
     */
    protected abstract void findCandidates ();

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
                    inter.delete();
                }
            }

            // Discard numbers
            if (ShapeSet.SingleWholeTimes.contains(timeInter.getShape())) {
                // Chosen is COMMON or CUT, so delete any pair member
                for (VerticalSide side : VerticalSide.values()) {
                    final List<Inter> members = (side == VerticalSide.TOP) ? nums : dens;

                    for (Inter inter : members) {
                        inter.delete();
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

                        inter.delete();
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

                inter.delete();
            }

            // Check numbers
            for (VerticalSide side : VerticalSide.values()) {
                final List<Inter> members = (side == VerticalSide.TOP) ? nums : dens;
                final TimeNumberInter chosenNumber = chosenPair.getMember(side);
                glyphCandidates.remove(chosenNumber.getGlyph()); // Positive

                for (Inter inter : members) {
                    if (inter != chosenNumber) {
                        inter.delete();
                    }
                }
            }
        }

        glyphCandidates.removeAll(compatibles.values());
        recordSamples(compatibles);
    }

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
                    inter.delete();
                    it.remove();
                }
            }
        }

        return !wholes.isEmpty() || !nums.isEmpty();
    }

    //---------------//
    // recordSamples //
    //---------------//
    /**
     * If desired, record positive and/or negative samples.
     */
    protected void recordSamples (Map<Shape, Glyph> compatibles)
    {
        if (!constants.recordPositiveSamples.isSet() && !constants.recordNegativeSamples.isSet()) {
            return;
        }

        final Sheet sheet = staff.getSystem().getSheet();
        final Book book = sheet.getStub().getBook();
        final SampleRepository repository = book.getSampleRepository();

        if (repository == null) {
            return;
        }

        final SampleSheet sampleSheet = repository.findSampleSheet(sheet);
        final int interline = staff.getSpecificInterline();

        // Positive samples
        if (constants.recordPositiveSamples.isSet()) {
            if (timeInter instanceof TimeWholeInter) {
                final Glyph glyph = timeInter.getGlyph();
                final double pitch = staff.pitchPositionOf(glyph.getCentroid());
                repository.addSample(timeInter.getShape(), glyph, interline, sampleSheet, pitch);
            } else if (timeInter instanceof TimePairInter) {
                TimePairInter pair = (TimePairInter) timeInter;

                for (Inter member : pair.getMembers()) {
                    final Glyph glyph = member.getGlyph();
                    final double pitch = staff.pitchPositionOf(glyph.getCentroid());
                    repository.addSample(member.getShape(), glyph, interline, sampleSheet, pitch);
                }
            }

            // Include compatibles as positives
            for (Entry<Shape, Glyph> entry : compatibles.entrySet()) {
                final Glyph glyph = entry.getValue();
                final double pitch = staff.pitchPositionOf(glyph.getCentroid());
                repository.addSample(entry.getKey(), glyph, interline, sampleSheet, pitch);
            }
        }

        // Negative samples (assigned to CLUTTER)
        if (constants.recordNegativeSamples.isSet()) {
            for (Glyph glyph : glyphCandidates) {
                final double pitch = staff.pitchPositionOf(glyph.getCentroid());
                repository.addSample(Shape.CLUTTER, glyph, interline, sampleSheet, pitch);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------------//
    // BasicColumn //
    //-------------//
    /**
     * Class meant to handle a column of time signatures outside system header.
     */
    public static class BasicColumn
            extends Column
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** The measure stack for which a column of times is checked. */
        private final MeasureStack stack;

        /** Relevant time symbols found in stack. */
        private final Set<Inter> timeSet;

        /** Maximum abscissa shift between de-skewed time items in stack. */
        private final int maxDxOffset;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new {@code BasicColumn} object.
         *
         * @param stack   stack to be worked upon
         * @param timeSet set of time symbols found in stack
         */
        public BasicColumn (MeasureStack stack,
                            Set<Inter> timeSet)
        {
            super(stack.getSystem());

            this.stack = stack;
            this.timeSet = new HashSet<Inter>(timeSet);

            maxDxOffset = stack.getSystem().getSheet().getScale().toPixels(constants.maxDxOffset);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected TimeBuilder allocateBuilder (Staff staff)
        {
            return new BasicTimeBuilder(staff, this);
        }

        @Override
        protected void cleanup ()
        {
            for (Inter inter : timeSet) {
                inter.delete();
            }
        }

        //----------------//
        // purgeUnaligned //
        //----------------//
        @Override
        protected void purgeUnaligned ()
        {
            class Item
            {

                final Inter time;

                final double xOffset;

                public Item (Inter time,
                             double xOffset)
                {
                    this.time = time;
                    this.xOffset = xOffset;
                }
            }

            class Line
            {

                List<Item> items = new ArrayList<Item>();

                Double meanOffset = null;

                void addItem (Item item)
                {
                    items.add(item);
                    meanOffset = null; // Reset cached value
                }

                double getOffset ()
                {
                    if (meanOffset == null) {
                        double sum = 0;

                        for (Item item : items) {
                            sum += item.xOffset;
                        }

                        sum /= items.size();
                        meanOffset = sum;
                    }

                    return meanOffset;
                }
            }

            List<Line> lines = new ArrayList<Line>();

            for (Staff staff : system.getStaves()) {
                TimeBuilder builder = builders.get(staff);

                for (List<Inter> list : Arrays.asList(builder.wholes, builder.nums, builder.dens)) {
                    for (Inter inter : list) {
                        double xOffset = stack.getXOffset(inter.getCenter(), inter.getStaff());
                        Item item = new Item(inter, xOffset);

                        // is there a compatible line?
                        boolean found = false;

                        for (Line line : lines) {
                            if (Math.abs(line.getOffset() - xOffset) <= maxDxOffset) {
                                line.addItem(item);
                                found = true;

                                break;
                            }
                        }

                        if (!found) {
                            // No compatible line found, create a brand new one
                            Line line = new Line();
                            line.addItem(item);
                            lines.add(line);
                        }
                    }
                }
            }

            // Select a single vertical line (based on item count? or the left-most line?)
            Collections.sort(
                    lines,
                    new Comparator<Line>()
            {
                @Override
                public int compare (Line o1,
                                    Line o2)
                {
                    return Double.compare(o1.getOffset(), o2.getOffset());
                }
            });

            Line chosenLine = lines.get(0);
            List<Inter> kept = new ArrayList<Inter>();

            for (Item item : chosenLine.items) {
                kept.add(item.time);
            }

            // Purge all entities non kept
            for (Staff staff : system.getStaves()) {
                TimeBuilder builder = builders.get(staff);

                for (List<Inter> list : Arrays.asList(builder.wholes, builder.nums, builder.dens)) {
                    for (Iterator<Inter> it = list.iterator(); it.hasNext();) {
                        Inter inter = it.next();

                        if (!kept.contains(inter)) {
                            inter.delete();
                            it.remove();
                        }
                    }
                }
            }
        }
    }

    //------------------//
    // BasicTimeBuilder //
    //------------------//
    /**
     * A subclass of TimeBuilder specifically meant for extraction outside system header,
     * further down in the system measures.
     * <p>
     * Symbol extraction has already been performed, so time-signature shaped symbols are now
     * checked for consistency across all staves of the containing system.
     */
    public static class BasicTimeBuilder
            extends TimeBuilder
    {
        //~ Constructors ---------------------------------------------------------------------------

        public BasicTimeBuilder (Staff staff,
                                 BasicColumn column)
        {
            super(staff, column);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected void findCandidates ()
        {
            // For time symbols found (whole or half), pitch is correct, but abscissa is random
            // For now, at staff level, we can only check that nums & dens are x-compatible
            BasicColumn basicColumn = (BasicColumn) column;

            for (Inter inter : basicColumn.timeSet) {
                if (inter.getStaff() == staff) {
                    if (inter instanceof TimeWholeInter) {
                        wholes.add(inter);
                    } else if (inter instanceof TimeNumberInter) {
                        TimeNumberInter number = (TimeNumberInter) inter;
                        VerticalSide side = number.getSide();

                        if (side == VerticalSide.TOP) {
                            nums.add(inter);
                        } else {
                            dens.add(inter);
                        }
                    }
                }
            }
        }
    }

    //--------------//
    // HeaderColumn //
    //--------------//
    /**
     * A subclass of {@link Column}, specifically meant for managing times in a system
     * header.
     */
    public static class HeaderColumn
            extends Column
    {
        //~ Constructors ---------------------------------------------------------------------------

        public HeaderColumn (SystemInfo system)
        {
            super(system);
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------//
        // addPlot //
        //---------//
        /**
         * Contribute to the staff plotter with data related to time signature.
         * <p>
         * Since the user can ask for time plot at any time, we allocate an instance of
         * HeaderTimeBuilder on demand just to provide the plot information that pertains to the
         * desired staff. This saves us the need to keep rather useless instances in memory.
         *
         * @param plotter the staff plotter to populate
         * @param staff   the desired staff
         * @return the time chosen to be added to plot title
         */
        public String addPlot (ChartPlotter plotter,
                               Staff staff)
        {
            final int browseStart;

            if (staff.getKeyStop() != null) {
                browseStart = staff.getKeyStop();
            } else if (staff.getClefStop() != null) {
                browseStart = staff.getClefStop();
            } else {
                browseStart = staff.getHeaderStart();
            }

            HeaderTimeBuilder builder = new HeaderTimeBuilder(staff, this, browseStart);
            builder.addPlot(plotter);

            AbstractTimeInter timeInter = staff.getHeader().time;

            if (timeInter != null) {
                return "time:" + timeInter.getValue();
            } else {
                return null;
            }
        }

        /**
         * @return ending abscissa offset of time-sig column WRT measure start, or -1 if invalid
         */
        @Override
        public int retrieveTime ()
        {
            if (-1 != super.retrieveTime()) {
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
            } else {
                return -1;
            }
        }

        @Override
        protected TimeBuilder allocateBuilder (Staff staff)
        {
            int browseStart = staff.getHeaderStop();

            return new HeaderTimeBuilder(staff, this, browseStart);
        }

        @Override
        protected void cleanup ()
        {
            for (TimeBuilder builder : builders.values()) {
                for (TimeKind kind : TimeKind.values()) {
                    TimeAdapter adapter = ((HeaderTimeBuilder) builder).adapters.get(kind);

                    if (adapter != null) {
                        adapter.cleanup();
                    }
                }
            }
        }
    }

    //-------------------//
    // HeaderTimeBuilder //
    //-------------------//
    /**
     * A subclass of TimeBuilder specifically meant for extraction from system header.
     * <p>
     * The staff region that follows the staff clef (and staff key-sig if any) is searched for
     * presence of chunks.
     * <p>
     * A global match can be tried for TimeWholeInter: COMMON_TIME and for CUT_TIME shapes as well
     * as predefined combo shapes like 4/4, 3/4, 6/8, etc.
     * <p>
     * We also try to combine a numerator and a denominator into a TimePairInter (such as [3,4]),
     * by splitting time area using middle staff line to ease recognition of individual numbers.
     */
    public static class HeaderTimeBuilder
            extends TimeBuilder
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Time range info. */
        private final StaffHeader.Range range;

        /** Projection of foreground pixels, indexed by abscissa. */
        private final IntegerFunction projection;

        /** Region of interest for time-sig. */
        private final Rectangle roi;

        /** 3 adapters for glyph building, one for each kind: whole, num & den. */
        private final Map<TimeKind, TimeAdapter> adapters = new EnumMap<TimeKind, TimeAdapter>(
                TimeKind.class);

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates an instance of {code HeaderTimeBuilder}.
         *
         * @param staff       the staff to work on
         * @param column      the column manager
         * @param browseStart abscissa offset to start browsing from
         *                    (overridden by header.timeRange if already known)
         */
        public HeaderTimeBuilder (Staff staff,
                                  Column column,
                                  int browseStart)
        {
            super(staff, column);

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

        //~ Methods --------------------------------------------------------------------------------
        //---------//
        // addPlot //
        //---------//
        /**
         * Augment the provided plotter with projection data pertaining to time signature
         *
         * @param plotter the plotter to augment
         */
        protected void addPlot (ChartPlotter plotter)
        {
            final int xMin = projection.getXMin();
            final int xMax = projection.getXMax();

            {
                // Values
                XYSeries series = projection.getValueSeries();
                series.setKey("Time");
                plotter.add(series, Color.BLUE);
            }

            if (range.hasStart() || (staff.getTimeStop() != null)) {
                // Area limits
                XYSeries series = new XYSeries("TimeArea", false); // No autosort
                int start = range.hasStart() ? range.getStart() : staff.getTimeStart();
                int stop = range.hasStart() ? range.getStop() : staff.getTimeStop();

                series.add(start, 0);
                series.add(start, staff.getHeight());

                series.add(stop, staff.getHeight());
                series.add(stop, 0);

                plotter.add(series, Color.BLUE);
            }
        }

        //---------------//
        // createTimeSig //
        //---------------//
        @Override
        protected void createTimeSig (AbstractTimeInter bestTimeInter)
        {
            super.createTimeSig(bestTimeInter);

            // Expend header info
            if (bestTimeInter != null) {
                Rectangle timeBox = bestTimeInter.getSymbolBounds(scale.getInterline());
                int end = timeBox.x + timeBox.width;
                staff.setTimeStop(end);

                staff.getHeader().time = bestTimeInter;
            }
        }

        //----------------//
        // findCandidates //
        //----------------//
        @Override
        protected void findCandidates ()
        {
            // Projection can help refine the abscissa range
            browseProjection();

            if (range.hasStart() && (range.getWidth() >= params.minTimeWidth)) {
                processWhole(); //   Look for whole time sigs (common, cut or combo like 6/8)
                processHalf(NUM); // Look for top halves      (like 6/)
                processHalf(DEN); // Look for bottom halves   (like /8)
            }
        }

        //------------------//
        // browseProjection //
        //------------------//
        /**
         * Analyze projection data to refine time sig abscissa range.
         * We expect a small space before, suitable range for time sig, space after.
         * Output: range.start & range.stop
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

            range.setStart(first.stop);
            range.setStop(spaces.get(spaces.size() - 1).start);
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
            final Sheet sheet = system.getSheet();

            // Grab pixels out of staff-free source
            ByteProcessor source = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);
            ByteProcessor buf = new ByteProcessor(rect.width, rect.height);
            buf.copyBits(source, -rect.x, -rect.y, Blitter.COPY);

            // Extract parts
            RunTable runTable = new RunTableFactory(VERTICAL).createTable(buf);
            List<Glyph> parts = GlyphFactory.buildGlyphs(runTable, rect.getLocation());

            // Keep only interesting parts
            purgeParts(parts, rect);

            final GlyphIndex glyphIndex = sheet.getGlyphIndex();

            for (ListIterator<Glyph> li = parts.listIterator(); li.hasNext();) {
                final Glyph part = li.next();
                Glyph glyph = glyphIndex.registerOriginal(part);
                glyph.addGroup(Group.TIME_PART); // For debug?
                system.addFreeGlyph(glyph);
                li.set(glyph);
            }

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
        private IntegerFunction getProjection ()
        {
            // Staff-free pixel source
            final ByteProcessor source = system.getSheet().getPicture().getSource(
                    Picture.SourceKey.NO_STAFF);
            final int xMin = roi.x;
            final int xMax = (roi.x + roi.width) - 1;
            final IntegerFunction function = new IntegerFunction(xMin, xMax);

            for (int x = xMin; x <= xMax; x++) {
                short cumul = 0;

                for (int y = roi.y, yBreak = roi.y + roi.height; y < yBreak; y++) {
                    if (source.get(x, y) == 0) {
                        cumul++;
                    }
                }

                function.setValue(x, cumul);
            }

            return function;
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

            final int top = Math.min(
                    staff.getFirstLine().yAt(start),
                    staff.getFirstLine().yAt(stop));
            final int bottom = Math.max(
                    staff.getLastLine().yAt(stop),
                    staff.getLastLine().yAt(stop));
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
            final List<Space> spaces = new ArrayList<Space>();

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
                } else if (spaceStart != -1) {
                    // End of space
                    spaces.add(new Space(spaceStart, spaceStop));
                    spaceStart = -1;
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
         * Populates 'nums' or 'dens' and sig.
         *
         * @param half which half (top or bottom) is being searched for
         */
        private void processHalf (TimeKind half)
        {
            final List<Inter> inters = (half == NUM) ? nums : dens;

            // Define proper rectangular search area for this side
            int top = roi.y + ((half == NUM) ? 0 : (roi.height - (roi.height / 2)));
            Rectangle rect = new Rectangle(range.getStart(), top, range.getWidth(), roi.height / 2);
            rect.grow(0, -params.yMargin);
            staff.addAttachment("T" + ((half == NUM) ? "N" : "D"), rect);

            //TODO: Should not try each and every combination of parts
            // Perhaps take all of them (or the largest ones?) and evaluate the compound
            List<Glyph> parts = getParts(rect);
            HalfAdapter adapter = new HalfAdapter(half, parts);
            adapters.put(half, adapter);

            new GlyphCluster(adapter, null).decompose();
            logger.debug(
                    "Staff#{} {} {} trials:{}",
                    staff.getId(),
                    half,
                    Glyphs.ids("parts", parts),
                    adapter.trials);

            if (!adapter.bestMap.isEmpty()) {
                for (Entry<Shape, Inter> entry : adapter.bestMap.entrySet()) {
                    Inter inter = entry.getValue();

                    Rectangle timeBox = inter.getSymbolBounds(scale.getInterline());
                    inter.setBounds(timeBox);
                    inter.setStaff(staff);
                    sig.addVertex(inter);
                    inters.add(inter);

                    int gid = inter.getGlyph().getId();
                    logger.debug(
                            "Staff#{} {} {} g#{} {}",
                            staff.getId(),
                            half,
                            inter,
                            gid,
                            timeBox);
                }
            }
        }

        //--------------//
        // processWhole //
        //--------------//
        /**
         * Lookup staff header for whole time-sig candidates.
         * Populates 'wholes' and sig.
         */
        private void processWhole ()
        {
            // Define proper rectangular search area for a whole time-sig
            Rectangle rect = new Rectangle(range.getStart(), roi.y, range.getWidth(), roi.height);
            rect.grow(0, -params.yMargin);
            staff.addAttachment("TF", rect);

            List<Glyph> parts = getParts(rect);
            TimeAdapter wholeAdapter = new WholeAdapter(parts);
            adapters.put(WHOLE, wholeAdapter);
            new GlyphCluster(wholeAdapter, null).decompose();
            logger.debug(
                    "Staff#{} WHOLE {} trials:{}",
                    staff.getId(),
                    Glyphs.ids("parts", parts),
                    wholeAdapter.trials);

            if (!wholeAdapter.bestMap.isEmpty()) {
                for (Entry<Shape, Inter> entry : wholeAdapter.bestMap.entrySet()) {
                    Inter inter = entry.getValue();
                    Rectangle timeBox = inter.getSymbolBounds(scale.getInterline());
                    inter.setBounds(timeBox);
                    inter.setStaff(staff);
                    sig.addVertex(inter);
                    wholes.add(inter);

                    int gid = inter.getGlyph().getId();
                    logger.debug("Staff#{} {} g#{} {}", staff.getId(), inter, gid, timeBox);
                }
            }
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
    }

    //--------//
    // Column //
    //--------//
    /**
     * This abstract class provides the basis for management of a system-level column
     * of staff-level TimeBuilder instances since, within a system, a colum of time
     * signatures must be complete and contain only identical signatures.
     * <p>
     * Subclass {@link HeaderColumn} works for system header, while subclass {@link BasicColumn}
     * works for time signatures found outside of system header.
     */
    protected abstract static class Column
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Containing system. */
        protected final SystemInfo system;

        /** Best time value found, if any. */
        protected TimeValue timeValue;

        /** Map of time builders. (one per staff) */
        protected final Map<Staff, TimeBuilder> builders = new TreeMap<Staff, TimeBuilder>(
                Staff.byId);

        //~ Constructors ---------------------------------------------------------------------------
        public Column (SystemInfo system)
        {
            this.system = system;
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------------//
        // getTimeInters //
        //---------------//
        /**
         * Report the time inter instance for each staff in the column.
         *
         * @return the map: staff -> time inter
         */
        public Map<Staff, AbstractTimeInter> getTimeInters ()
        {
            Map<Staff, AbstractTimeInter> times = new TreeMap<Staff, AbstractTimeInter>(Staff.byId);

            for (Entry<Staff, TimeBuilder> entry : builders.entrySet()) {
                times.put(entry.getKey(), entry.getValue().getTimeInter());
            }

            return times;
        }

        //--------------//
        // retrieveTime //
        //--------------//
        /**
         * This is the main entry point for time signature, it retrieves the column of
         * staves candidates time signatures, and selects the best one at system level.
         *
         * @return 0 if valid, -1 if invalid
         */
        public int retrieveTime ()
        {
            // Allocate one time-sig builder for each staff within system
            for (Staff staff : system.getStaves()) {
                builders.put(staff, allocateBuilder(staff));
            }

            // Process each staff on turn, to find candidates
            for (TimeBuilder builder : builders.values()) {
                // Retrieve candidates for time items
                builder.findCandidates();

                // This fails if no candidate at all is kept in staff after filtering
                if (!builder.filterCandidates()) {
                    cleanup(); // Clean up what has been constructed

                    return -1; // We failed to find a time sig in stack
                }
            }

            // Check vertical alignment
            purgeUnaligned();

            // Check time sig consistency at system level
            return checkConsistency() ? 0 : (-1);
        }

        /**
         * Allocate instance of proper subclass of TimeBuilder
         *
         * @param staff the dedicated staff for this builder
         * @return the created TimeBuilder instance
         */
        protected abstract TimeBuilder allocateBuilder (Staff staff);

        //------------------//
        // checkConsistency //
        //------------------//
        /**
         * Use vertical redundancy within a system column of time signatures to come up
         * with the best selection.
         * <p>
         * The selection is driven from the whole system column point of view, as follows:
         * <ol>
         * <li>For each staff, identify all the possible & supported AbstractTimeInter instances,
         * each with its own grade.</li>
         * <li>Then for each possible AbstractTimeInter value (called TimeValue), make sure it
         * appears in each staff as a AbstractTimeInter instance and assign a global grade (as
         * average of staff-based AbstractTimeInter instances for the same TimeValue).</li>
         * <li>The best system-based TimeValue is then chosen as THE time signature for this
         * system column. </li>
         * <li>All staff non compatible AbstractTimeInter instances are destroyed and the member
         * numbers that don't belong to the chosen AbstractTimeInter are destroyed.</li>
         * </ol>
         *
         * @return true if OK, false otherwise
         */
        protected boolean checkConsistency ()
        {
            // Retrieve all time values found, organized by value and staff
            Map<TimeValue, AbstractTimeInter[]> vectors = getValueVectors();
            Map<TimeValue, Double> grades = new HashMap<TimeValue, Double>();

            TimeLoop:
            for (Entry<TimeValue, AbstractTimeInter[]> entry : vectors.entrySet()) {
                TimeValue time = entry.getKey();
                AbstractTimeInter[] vector = entry.getValue();

                // Check that this time is present in all staves and compute the time mean grade
                double mean = 0;

                for (Inter inter : vector) {
                    if (inter == null) {
                        logger.debug(
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

            logger.debug("System#{} time sig grades {}", system.getId(), grades);

            // Select the best time value at system level
            double bestGrade = 0;

            for (Entry<TimeValue, Double> entry : grades.entrySet()) {
                double grade = entry.getValue();

                if (grade > bestGrade) {
                    bestGrade = grade;
                    timeValue = entry.getKey();
                }
            }

            if (timeValue == null) {
                return false; // Invalid column
            }

            // Forward the chosen time to each staff
            AbstractTimeInter[] bestVector = vectors.get(timeValue);
            List<Staff> staves = system.getStaves();

            for (int is = 0; is < staves.size(); is++) {
                Staff staff = staves.get(is);
                TimeBuilder builder = builders.get(staff);
                builder.createTimeSig(bestVector[is]);
                builder.discardOthers();
            }

            logger.debug("System#{} TimeSignature: {}", system.getId(), timeValue);

            return true;
        }

        /**
         * This is called when we discover that a column of candidate(s) is wrong,
         * so that all related data inserted in sig is removed.
         */
        protected abstract void cleanup ();

        /**
         * Report the system vector of values for each time value found.
         * A vector is an array, one element per staff, the element being the staff candidate
         * AbstractTimeInter for the desired time value, or null if the time value has no acceptable
         * candidate in this staff.
         *
         * @return the system vectors of candidates found, organized per TimeValue
         */
        protected Map<TimeValue, AbstractTimeInter[]> getValueVectors ()
        {
            // Retrieve all occurrences of time values across staves.
            final Map<TimeValue, AbstractTimeInter[]> values = new HashMap<TimeValue, AbstractTimeInter[]>();

            // Loop on system staves
            final List<Staff> staves = system.getStaves();

            for (int index = 0; index < staves.size(); index++) {
                final Staff staff = staves.get(index);
                final TimeBuilder builder = builders.get(staff);
                final SIGraph sig = builder.sig;

                // Whole candidate signatures, if any, in this staff
                for (Inter inter : builder.wholes) {
                    AbstractTimeInter whole = (AbstractTimeInter) inter;
                    TimeValue time = whole.getValue();
                    AbstractTimeInter[] vector = values.get(time);

                    if (vector == null) {
                        values.put(time, vector = new AbstractTimeInter[staves.size()]);
                    }

                    if ((vector[index] == null) || (inter.getGrade() > vector[index].getGrade())) {
                        vector[index] = whole;
                    }
                }

                // Num/Den pair candidate signatures, if any
                for (Inter nInter : builder.nums) {
                    TimeNumberInter num = (TimeNumberInter) nInter;

                    for (Relation rel : sig.getRelations(num, TimeTopBottomRelation.class)) {
                        TimeNumberInter den = (TimeNumberInter) sig.getOppositeInter(
                                nInter,
                                rel);
                        TimePairInter pair = TimePairInter.create(num, den);
                        TimeValue time = pair.getValue();
                        AbstractTimeInter[] vector = values.get(time);

                        if (vector == null) {
                            values.put(time, vector = new AbstractTimeInter[staves.size()]);
                        }

                        if ((vector[index] == null)
                            || (pair.getGrade() > vector[index].getGrade())) {
                            vector[index] = pair;
                        }
                    }
                }
            }

            return values;
        }

        /**
         * Check that all candidates are vertically aligned.
         * When processing system header, candidates are aligned by construction.
         * But, outside headers, candidates within the same stack have to be checked for such
         * alignment.
         */
        protected void purgeUnaligned ()
        {
            // Void by default
        }
    }

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
                           int specific)
        {
            // Use sheet global interline value
            roiWidth = scale.toPixels(constants.roiWidth);
            maxFirstSpaceWidth = scale.toPixels(constants.maxFirstSpaceWidth);
            maxInnerSpace = scale.toPixels(constants.maxInnerSpace);

            // Use staff specific interline value
            yMargin = InterlineScale.toPixels(specific, constants.yMargin);
            minTimeWidth = InterlineScale.toPixels(specific, constants.minTimeWidth);
            maxTimeWidth = InterlineScale.toPixels(specific, constants.maxTimeWidth);
            maxHalvesDx = InterlineScale.toPixels(specific, constants.maxHalvesDx);
            maxPartGap = InterlineScale.toPixels(specific, constants.maxPartGap);
            minWholeTimeWeight = InterlineScale.toPixels(specific, constants.minWholeTimeWeight);
            minHalfTimeWeight = InterlineScale.toPixels(specific, constants.minHalfTimeWeight);
            maxSpaceCumul = InterlineScale.toPixels(specific, constants.maxSpaceCumul);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean recordPositiveSamples = new Constant.Boolean(
                false,
                "Should we record positive samples from TimeBuilder?");

        private final Constant.Boolean recordNegativeSamples = new Constant.Boolean(
                false,
                "Should we record negative samples from TimeBuilder?");

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

        private final Scale.Fraction maxDxOffset = new Scale.Fraction(
                2,
                "Maximum abscissa shift between deskewed time items in a stack");

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
        public void evaluateGlyph (Glyph glyph,
                                   Set<Glyph> parts)
        {
            trials++;

            final Sheet sheet = system.getSheet();

            if (glyph.getId() == 0) {
                glyph = sheet.getGlyphIndex().registerOriginal(glyph);
                system.addFreeGlyph(glyph);
            }

            glyphCandidates.add(glyph);

            Evaluation[] evals = GlyphClassifier.getInstance()
                    .getNaturalEvaluations(
                            glyph,
                            staff.getSpecificInterline());

            for (Shape shape : halfShapes) {
                Evaluation eval = evals[shape.ordinal()];
                double grade = Inter.intrinsicRatio * eval.grade;

                if (grade >= Grades.timeMinGrade) {
                    if (glyph.getId() == 0) {
                        glyph = sheet.getGlyphIndex().registerOriginal(glyph);
                    }

                    logger.debug("   {} eval {} for glyph#{}", half, eval, glyph.getId());

                    Inter bestInter = bestMap.get(shape);

                    if ((bestInter == null) || (bestInter.getGrade() < grade)) {
                        TimeNumberInter inter = TimeNumberInter.create(glyph, shape, grade, staff);

                        if (inter != null) {
                            bestMap.put(shape, inter);
                        }
                    }
                }
            }
        }

        @Override
        public boolean isTooLight (int weight)
        {
            return weight < params.minHalfTimeWeight;
        }
    }

    //-------------//
    // TimeAdapter //
    //-------------//
    private abstract class TimeAdapter
            extends GlyphCluster.AbstractAdapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Best inter per time shape. */
        public Map<Shape, Inter> bestMap = new EnumMap<Shape, Inter>(Shape.class);

        //~ Constructors ---------------------------------------------------------------------------
        public TimeAdapter (List<Glyph> parts)
        {
            super(parts, params.maxPartGap);
        }

        //~ Methods --------------------------------------------------------------------------------
        public void cleanup ()
        {
            for (Inter inter : bestMap.values()) {
                inter.delete();
            }
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
        public boolean isTooLarge (Rectangle bounds)
        {
            return bounds.width > params.maxTimeWidth;
        }
    }

    //--------------//
    // WholeAdapter //
    //--------------//
    /**
     * Handles the integration between glyph clustering class and time-sig environment.
     * <p>
     * For each time value, we keep the best result found if any.
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
        public void evaluateGlyph (Glyph glyph,
                                   Set<Glyph> parts)
        {
            //TODO: check glyph centroid for a whole symbol is not too far from staff middle line
            trials++;

            final Sheet sheet = system.getSheet();

            if (glyph.getId() == 0) {
                glyph = sheet.getGlyphIndex().registerOriginal(glyph);
                system.addFreeGlyph(glyph);
            }

            glyphCandidates.add(glyph);

            Evaluation[] evals = GlyphClassifier.getInstance()
                    .getNaturalEvaluations(
                            glyph,
                            staff.getSpecificInterline());

            for (Shape shape : wholeShapes) {
                Evaluation eval = evals[shape.ordinal()];
                double grade = Inter.intrinsicRatio * eval.grade;

                if (grade >= Grades.timeMinGrade) {
                    if (glyph.getId() == 0) {
                        glyph = sheet.getGlyphIndex().registerOriginal(glyph);
                    }

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
        public boolean isTooLight (int weight)
        {
            return weight < params.minWholeTimeWeight;
        }
    }
}
