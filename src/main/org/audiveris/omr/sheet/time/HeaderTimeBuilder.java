//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                H e a d e r T i m e B u i l d e r                               //
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
package org.audiveris.omr.sheet.time;

import ij.process.Blitter;
import ij.process.ByteProcessor;

import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.classifier.ShapeClassifier;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphCluster;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.IntegerFunction;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.header.StaffHeader;
import static org.audiveris.omr.sheet.time.TimeBuilder.TimeKind.*;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.TimeNumberInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.util.ChartPlotter;

import org.jfree.data.xy.XYSeries;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

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
 *
 * @author Hervé Bitteur
 */
public class HeaderTimeBuilder
        extends TimeBuilder
{

    private static final Logger logger = LoggerFactory.getLogger(HeaderTimeBuilder.class);

    /** Time range info. */
    private final StaffHeader.Range range;

    /** Projection of foreground pixels, indexed by abscissa. */
    private final IntegerFunction projection;

    /** Region of interest for time-sig. */
    private final Rectangle roi;

    /** 3 adapters for glyph building, one for each kind: whole, num & den. */
    final Map<TimeKind, TimeAdapter> adapters = new EnumMap<TimeKind, TimeAdapter>(TimeKind.class);

    /**
     * Creates an instance of {code HeaderTimeBuilder}.
     *
     * @param staff       the staff to work on
     * @param column      the column manager
     * @param browseStart abscissa offset to start browsing from
     *                    (overridden by header.timeRange if already known)
     */
    public HeaderTimeBuilder (Staff staff,
                              TimeColumn column,
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

    //---------//
    // cleanup //
    //---------//
    @Override
    public void cleanup ()
    {
        //        for (TimeBuilder.TimeKind kind : TimeBuilder.TimeKind.values()) {
        //            TimeAdapter adapter = adapters.get(kind);
        //
        //            if (adapter != null) {
        //                adapter.cleanup();
        //            }
        //        }
    }

    //------------//
    // lookupTime //
    //------------//
    /**
     * Look up for an existing time inter at proper location
     *
     * @return time inter or null
     */
    public AbstractTimeInter lookupTime ()
    {
        // In staff
        final List<Inter> staffTimes = sig.inters(staff, AbstractTimeInter.class);

        // In staff header
        final List<Inter> headerTimes = Inters.intersectedInters(staffTimes, GeoOrder.NONE, roi);

        if (!headerTimes.isEmpty()) {
            final StaffHeader header = staff.getHeader();
            timeInter = (AbstractTimeInter) headerTimes.get(0);
            header.time = timeInter;

            final Rectangle bounds = timeInter.getBounds();
            range.valid = true;
            range.setStart(bounds.x);
            range.setStop((bounds.x + bounds.width) - 1);

            return timeInter;
        }

        return null;
    }

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
            Rectangle timeBox = bestTimeInter.getSymbolBounds(staff.getSpecificInterline());
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
        final int stop = staff.getBrowseStop(start, (start + params.roiWidth) - 1);
        final int top = Math.min(staff.getFirstLine().yAt(start), staff.getFirstLine().yAt(stop));
        final int bottom = Math.max(staff.getLastLine().yAt(stop), staff.getLastLine().yAt(stop));
        range.browseStop = stop;

        return new Rectangle(start, top, stop - start + 1, bottom - top + 1);
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
        RunTable runTable = new RunTableFactory(Orientation.VERTICAL).createTable(buf);
        List<Glyph> parts = GlyphFactory.buildGlyphs(runTable, rect.getLocation());

        final GlyphIndex glyphIndex = sheet.getGlyphIndex();

        for (ListIterator<Glyph> li = parts.listIterator(); li.hasNext();) {
            final Glyph part = li.next();
            Glyph glyph = glyphIndex.registerOriginal(part);
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
            for (Map.Entry<Shape, Inter> entry : adapter.bestMap.entrySet()) {
                Inter inter = entry.getValue();
                Rectangle timeBox = inter.getSymbolBounds(staff.getSpecificInterline());
                inter.setBounds(timeBox);
                inter.setStaff(staff);
                sig.addVertex(inter);
                inters.add(inter);

                int gid = inter.getGlyph().getId();
                logger.debug("Staff#{} {} {} g#{} {}", staff.getId(), half, inter, gid, timeBox);
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
            for (Map.Entry<Shape, Inter> entry : wholeAdapter.bestMap.entrySet()) {
                Inter inter = entry.getValue();
                Rectangle timeBox = inter.getSymbolBounds(staff.getSpecificInterline());
                inter.setBounds(timeBox);
                inter.setStaff(staff);
                sig.addVertex(inter);
                wholes.add(inter);

                int gid = inter.getGlyph().getId();
                logger.debug("Staff#{} {} g#{} {}", staff.getId(), inter, gid, timeBox);
            }
        }
    }

    //-------//
    // Space //
    //-------//
    private static class Space
    {

        /** Left abscissa. */
        protected final int start;

        /** Right abscissa. */
        protected final int stop;

        public Space (int start,
                      int stop)
        {
            this.start = start;
            this.stop = stop;
        }

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

        /** Which half is being searched. (NUM or DEN) */
        private final TimeKind half;

        public HalfAdapter (TimeKind half,
                            List<Glyph> parts)
        {
            super(parts);
            this.half = half;
        }

        @Override
        public void evaluateGlyph (Glyph glyph,
                                   Set<Glyph> parts)
        {
            trials++;

            if (glyph.getId() == 0) {
                glyph = system.registerGlyph(glyph, null);
            }

            glyphCandidates.add(glyph);

            Evaluation[] evals = ShapeClassifier.getInstance().evaluate(
                    glyph,
                    staff.getSpecificInterline(),
                    params.maxEvalRank,
                    Grades.timeMinGrade / Grades.intrinsicRatio,
                    null);

            for (Evaluation eval : evals) {
                final Shape shape = eval.shape;

                if (halfShapes.contains(shape)) {
                    final double grade = Grades.intrinsicRatio * eval.grade;
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

        /** Best inter per time shape. */
        public Map<Shape, Inter> bestMap = new EnumMap<Shape, Inter>(Shape.class);

        public TimeAdapter (List<Glyph> parts)
        {
            super(parts, params.maxPartGap);
        }

        public void cleanup ()
        {
            for (Inter inter : bestMap.values()) {
                inter.remove();
            }
        }

        public Inter getSingleInter ()
        {
            for (Inter inter : bestMap.values()) {
                if (!inter.isRemoved()) {
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

        public WholeAdapter (List<Glyph> parts)
        {
            super(parts);
        }

        @Override
        public void evaluateGlyph (Glyph glyph,
                                   Set<Glyph> parts)
        {
            //TODO: check glyph centroid for a whole symbol is not too far from staff middle line
            trials++;

            if (glyph.getId() == 0) {
                glyph = system.registerGlyph(glyph, null);
            }

            glyphCandidates.add(glyph);

            Evaluation[] evals = ShapeClassifier.getInstance().evaluate(
                    glyph,
                    staff.getSpecificInterline(),
                    params.maxEvalRank,
                    Grades.timeMinGrade / Grades.intrinsicRatio,
                    null);

            for (Evaluation eval : evals) {
                final Shape shape = eval.shape;

                if (wholeShapes.contains(shape)) {
                    final double grade = Grades.intrinsicRatio * eval.grade;
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
