//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      C l e f B u i l d e r                                     //
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
package org.audiveris.omr.sheet.clef;

import ij.process.Blitter;
import ij.process.ByteProcessor;

import org.audiveris.omr.classifier.Classifier;
import org.audiveris.omr.classifier.Evaluation;
import org.audiveris.omr.classifier.ShapeClassifier;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphCluster;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.glyph.GlyphLink;
import org.audiveris.omr.glyph.Glyphs;
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Scale.InterlineScale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.header.StaffHeader;
import org.audiveris.omr.sig.GradeUtil;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.ClefInter.ClefKind;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.relation.ClefKeyRelation;
import org.audiveris.omr.sig.relation.Exclusion;
import org.audiveris.omr.ui.symbol.SymbolIcon;
import org.audiveris.omr.ui.symbol.Symbols;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.VerticalSide;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class {@code ClefBuilder} extracts the clef symbol at the beginning of a staff.
 * <p>
 * Retrieving the clef kind (Treble, Bass, Alto or Tenor) is important for checking consistency with
 * potential key signature in the staff.
 *
 * @author Hervé Bitteur
 */
public class ClefBuilder
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ClefBuilder.class);

    /**
     * All possible clef symbols at beginning of staff: all but small clefs.
     * Octave bass clefs are reported to be extremely
     * <a href="http://en.wikipedia.org/wiki/Clef#Octave_clefs">rare</a>.
     */
    private static final EnumSet<Shape> HEADER_CLEF_SHAPES = EnumSet.of(
            F_CLEF,
            G_CLEF,
            G_CLEF_8VA,
            G_CLEF_8VB,
            C_CLEF,
            PERCUSSION_CLEF);

    /** Dedicated staff to analyze. */
    private final Staff staff;

    /** Clef range info. */
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

    /** Outer clef area. */
    private Rectangle outerRect;

    /** Inner clef area. */
    private Rectangle innerRect;

    /** Shape classifier to use. */
    private final Classifier classifier = ShapeClassifier.getInstance();

    /** All glyphs submitted to classifier. */
    private final Set<Glyph> glyphCandidates = new LinkedHashSet<>();

    /**
     * Creates a new ClefBuilder object.
     *
     * @param staff the underlying staff
     */
    public ClefBuilder (Staff staff)
    {
        this.staff = staff;

        system = staff.getSystem();
        sig = system.getSig();
        sheet = system.getSheet();
        scale = sheet.getScale();
        params = new Parameters(scale, staff.getSpecificInterline());

        final StaffHeader header = staff.getHeader();

        if (header.clefRange != null) {
            range = header.clefRange;
        } else {
            header.clefRange = (range = new StaffHeader.Range());
        }
    }

    //-----------//
    // findClefs //
    //-----------//
    /**
     * Retrieve the most probable clef(s) at beginning of staff.
     * <p>
     * At this time, we can keep several clef kinds. Final choice may be postponed until key
     * retrieval, unless maximum potential key impact could not modify the selection of best clef.
     */
    public void findClefs ()
    {
        // Define outer & inner lookup areas
        outerRect = getOuterRect();
        innerRect = getInnerRect(outerRect);

        // The user may have inserted an artificial clef
        for (Inter inter : system.getSig().inters(ClefInter.class)) {
            if (outerRect.contains(inter.getCenter())) {
                registerClefs(Arrays.asList((ClefInter) inter));
                logger.info("Using {}", inter);

                return;
            }
        }

        // First attempt, using both outer & inner areas
        Map<ClefKind, ClefInter> bestMap = getBestMap(true);

        if (bestMap.isEmpty()) {
            // Second attempt, focused on inner area only
            bestMap = getBestMap(false);
        }

        // Register the remaining clef candidates
        if (!bestMap.isEmpty()) {
            registerClefs(bestMap.values());
        }
    }

    //----------------//
    // setBrowseStart //
    //----------------//
    /**
     * Set the start abscissa for browsing.
     *
     * @param browseStart precise browse beginning abscissa (generally right after left bar line).
     */
    public void setBrowseStart (int browseStart)
    {
        range.browseStart = browseStart;
        range.browseStop = browseStart + params.maxClefEnd;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "ClefBuilder#" + staff.getId();
    }

    //------------//
    // getBestMap //
    //------------//
    /**
     * Retrieve the map of best clefs, organized per kind.
     *
     * @param isFirstPass true for first pass only
     * @return the bestMap found
     */
    private Map<ClefKind, ClefInter> getBestMap (boolean isFirstPass)
    {
        List<Glyph> parts = getParts(isFirstPass);

        // Formalize parts relationships in a global graph
        SimpleGraph<Glyph, GlyphLink> graph = Glyphs.buildLinks(parts, params.maxPartGap);
        List<Set<Glyph>> sets = new ConnectivityInspector<>(graph).connectedSets();
        logger.debug("Staff#{} sets: {}", staff.getId(), sets.size());

        // Best inter per clef kind
        Map<ClefKind, ClefInter> bestMap = new EnumMap<>(ClefKind.class);

        for (Set<Glyph> set : sets) {
            // Use only the subgraph for this set
            SimpleGraph<Glyph, GlyphLink> subGraph = GlyphCluster.getSubGraph(set, graph, false);
            ClefAdapter adapter = new ClefAdapter(subGraph, bestMap);
            new GlyphCluster(adapter, null).decompose();

            int trials = adapter.trials;
            logger.debug("Staff#{} clef parts:{} trials:{}", staff.getId(), set.size(), trials);
        }

        // Discard poor candidates as much as possible
        if (bestMap.size() > 1) {
            purgeClefs(bestMap);
        }

        return bestMap;
    }

    //--------------//
    // getInnerRect //
    //--------------//
    /**
     * Report the inner rectangle within the outer rectangle.
     *
     * @param outer provided outer rectangle
     * @return the inner rectangle
     */
    private Rectangle getInnerRect (Rectangle outer)
    {
        // Core rectangle
        Rectangle inner = new Rectangle(outer);
        inner.grow(0, -params.yCoreMargin);
        inner.x += params.xCoreMargin;
        inner.width -= params.xCoreMargin;
        staff.addAttachment("c", inner);

        return inner;
    }

    //--------------//
    // getOuterRect //
    //--------------//
    /**
     * Report the outer rectangle.
     * <p>
     * To cope with overlapping clefs across staves, the roi cannot vertically extend past the
     * middle of gutter with a neighboring staff.
     *
     * @return the outer rectangle
     */
    private Rectangle getOuterRect ()
    {
        final int xMin = range.browseStart;
        final int xMax = range.browseStop;
        final int xMid = (xMin + xMax) / 2;

        // Determine upper limit
        final int staffTop = staff.getFirstLine().yAt(xMid);
        int yMin = Math.max(0, staffTop - params.aboveStaff);

        // Staff above?
        for (Staff st : sheet.getStaffManager().vertNeighbors(staff, VerticalSide.TOP)) {
            if ((st.getAbscissa(LEFT) < xMid) && (st.getAbscissa(RIGHT) > xMid)) {
                final int yLast = st.getLastLine().yAt(xMid);
                yMin = Math.max(yMin, (int) Math.ceil(0.5 * (yLast + staffTop + 1)));
            }
        }

        // Determine lower limit
        final int staffBottom = staff.getLastLine().yAt(xMid);
        int yMax = Math.min(sheet.getHeight() - 1, staffBottom + params.belowStaff);

        // Staff below?
        for (Staff st : sheet.getStaffManager().vertNeighbors(staff, VerticalSide.BOTTOM)) {
            if ((st.getAbscissa(LEFT) < xMid) && (st.getAbscissa(RIGHT) > xMid)) {
                final int yFirst = st.getFirstLine().yAt(xMid);
                yMax = Math.min(yMax, (int) Math.floor(0.5 * ((staffBottom + yFirst) - 1)));
            }
        }

        Rectangle outer = new Rectangle(xMin, yMin, xMax - xMin + 1, yMax - yMin + 1);
        outer.grow(-params.beltMargin, 0);
        staff.addAttachment("C", outer);

        return outer;
    }

    //----------//
    // getParts //
    //----------//
    /**
     * Retrieve all glyph instances that could be part of clef.
     *
     * @param isFirstPass true for first pass
     * @return clef possible parts
     */
    private List<Glyph> getParts (boolean isFirstPass)
    {
        final Rectangle rect = isFirstPass ? outerRect : innerRect;

        // Grab pixels out of staff-free source
        ByteProcessor source = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);
        ByteProcessor buf = new ByteProcessor(rect.width, rect.height);
        buf.copyBits(source, -rect.x, -rect.y, Blitter.COPY);

        // Extract parts
        RunTable runTable = new RunTableFactory(VERTICAL).createTable(buf);
        List<Glyph> parts = GlyphFactory.buildGlyphs(runTable, rect.getLocation());

        // Keep only interesting parts
        purgeParts(parts, isFirstPass);

        system.registerGlyphs(parts, null);
        logger.debug("{} parts: {}", this, parts.size());

        return parts;
    }

    //------------//
    // purgeClefs //
    //------------//
    private void purgeClefs (Map<ClefKind, ClefInter> bestMap)
    {
        final double maxContrib = ClefKeyRelation.maxContributionForClef();
        final List<ClefInter> inters = new ArrayList<>(bestMap.values());
        Collections.sort(inters, Inters.byReverseGrade);

        interLoop:
        for (int i = 0; i < inters.size(); i++) {
            final double grade = inters.get(i).getGrade();

            for (int j = i + 1; j < inters.size(); j++) {
                final ClefInter other = inters.get(j);
                final double maxOtherCtx = GradeUtil.contextual(other.getGrade(), maxContrib);

                if (grade > maxOtherCtx) {
                    // Cut here since, whatever the key, no other clef can beat the best clef
                    for (ClefInter poor : inters.subList(j, inters.size())) {
                        logger.debug("Staff#{} discarding poor {}", staff.getId(), poor);
                        bestMap.remove(poor.getKind());
                    }

                    return;
                }
            }
        }
    }

    //------------//
    // purgeParts //
    //------------//
    /**
     * Purge the population of parts candidates as much as possible, since the cost
     * of their later combinations is exponential.
     *
     * @param parts       the collection to purge
     * @param isFirstPass true for first pass
     */
    private void purgeParts (List<Glyph> parts,
                             boolean isFirstPass)
    {
        List<Glyph> toRemove = new ArrayList<>();

        for (Glyph part : parts) {
            if (part.getWeight() < params.minPartWeight) {
                toRemove.add(part);
            } else if (isFirstPass && !part.getBounds().intersects(innerRect)) {
                toRemove.add(part);
            }
        }

        if (!toRemove.isEmpty()) {
            parts.removeAll(toRemove);
        }

        if (parts.size() > params.maxPartCount) {
            Collections.sort(parts, Glyphs.byReverseWeight);
            parts.retainAll(parts.subList(0, params.maxPartCount));
        }
    }

    //---------------//
    // registerClefs //
    //---------------//
    /**
     * Register the clefs into SIG and update staff clef abscissa stop.
     *
     * @param clefSet collection of remaining candidates
     */
    private void registerClefs (Collection<ClefInter> clefSet)
    {
        // Sort clefs by decreasing grade
        final List<ClefInter> clefList = new ArrayList<>(clefSet);
        Collections.sort(clefList, Inters.byReverseGrade);

        for (int idx = 0; idx < clefList.size(); idx++) {
            final ClefInter inter = clefList.get(idx);

            // Unerased staff line chunks may shift the symbol in abscissa,
            // so use glyph centroid for a better positioning
            // For inter bounds, use font-based symbol bounds rather than glyph bounds
            //TODO: we could also check histogram right after clef end, looking for a low point?
            Rectangle clefBox = inter.getSymbolBounds(staff.getSpecificInterline());

            if (inter.getGlyph() != null) {
                SymbolIcon symbol = Symbols.getSymbol(inter.getShape());
                Point symbolCentroid = symbol.getCentroid(clefBox);
                Point glyphCentroid = inter.getGlyph().getCentroid();
                int dx = glyphCentroid.x - symbolCentroid.x;
                int dy = glyphCentroid.y - symbolCentroid.y;
                logger.debug("Centroid translation dx:{} dy:{}", dx, dy);
                clefBox.translate(dx, 0);
                inter.setBounds(clefBox); // Force theoretical bounds as inter bounds!
                sig.addVertex(inter);
            }

            inter.setStaff(staff);

            if (idx == 0) {
                // Case of best clef
                Rectangle box = inter.getGlyph() != null
                        ? inter.getGlyph().getBounds().intersection(clefBox) : clefBox;
                int end = (box.x + box.width) - 1;
                staff.setClefStop(end);
            }
        }

        sig.insertExclusions(clefList, Exclusion.Cause.OVERLAP);
    }

    //------------//
    // selectClef //
    //------------//
    /**
     * Make the final selection of best clef for this staff header.
     */
    private void selectClef ()
    {
        List<ClefInter> clefs = staff.getCompetingClefs(range.getStop());

        if (!clefs.isEmpty()) {
            for (Inter clef : clefs) {
                sig.computeContextualGrade(clef);
            }

            Collections.sort(clefs, Inters.byReverseBestGrade);

            // Pickup the first one as header clef
            ClefInter bestClef = clefs.get(0);

            if (bestClef.getGlyph() != null) {
                bestClef.setGlyph(sheet.getGlyphIndex().registerOriginal(bestClef.getGlyph()));
            }

            staff.getHeader().clef = bestClef;

            // Delete the other clef candidates
            for (Inter other : clefs.subList(1, clefs.size())) {
                other.remove();
            }
        }
    }

    //-------------//
    // ClefAdapter //
    //-------------//
    /**
     * Handles the integration between glyph clustering class and clef environment.
     * <p>
     * For each clef kind, we keep the best result found if any.
     */
    private class ClefAdapter
            extends GlyphCluster.AbstractAdapter
    {

        /** Best inter per clef kind. */
        private final Map<ClefKind, ClefInter> bestMap;

        ClefAdapter (SimpleGraph<Glyph, GlyphLink> graph,
                     Map<ClefKind, ClefInter> bestMap)
        {
            super(graph);
            this.bestMap = bestMap;
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

            logger.debug("ClefAdapter evaluateGlyph on {}", glyph);

            Evaluation[] evals = classifier.evaluate(
                    glyph,
                    staff.getSpecificInterline(),
                    params.maxEvalRank,
                    Grades.clefMinGrade / Grades.intrinsicRatio,
                    null);

            for (Evaluation eval : evals) {
                final Shape shape = eval.shape;

                if (HEADER_CLEF_SHAPES.contains(shape)) {
                    final double grade = Grades.intrinsicRatio * eval.grade;
                    ClefKind kind = ClefInter.kindOf(glyph.getCenter(), shape, staff);
                    ClefInter bestInter = bestMap.get(kind);

                    if ((bestInter == null) || (bestInter.getGrade() < grade)) {
                        bestMap.put(kind, ClefInter.create(glyph, shape, grade, staff));
                    }
                }
            }
        }

        @Override
        public boolean isTooLarge (Rectangle bounds)
        {
            return bounds.height > params.maxGlyphHeight;
        }

        @Override
        public boolean isTooLight (int weight)
        {
            return weight < params.minGlyphWeight;
        }
    }

    //--------//
    // Column //
    //--------//
    /**
     * Manages the system consistency for a column of ClefBuilder instances.
     */
    public static class Column
    {

        private final SystemInfo system;

        /** Map of clef builders. (one per staff) */
        private final Map<Staff, ClefBuilder> builders = new TreeMap<>(Staff.byId);

        /**
         * Create a Column.
         *
         * @param system the containing system
         */
        public Column (SystemInfo system)
        {
            this.system = system;
        }

        //---------------//
        // retrieveClefs //
        //---------------//
        /**
         * Retrieve the column of staves candidate clefs.
         *
         * @return the ending abscissa offset of clefs column WRT measure start
         */
        public int retrieveClefs ()
        {
            // Retrieve staff Header clefs
            int maxClefOffset = 0;

            for (Staff staff : system.getStaves()) {
                if (staff.isTablature()) {
                    continue;
                }

                int measureStart = staff.getHeaderStart();

                // Retrieve staff clef
                ClefBuilder builder = new ClefBuilder(staff);
                builder.setBrowseStart(measureStart);
                builders.put(staff, builder);
                builder.findClefs();

                final Integer clefStop = staff.getClefStop();

                if (clefStop != null) {
                    maxClefOffset = Math.max(maxClefOffset, clefStop - measureStart);
                } else {
                    logger.warn("Staff#{} no header clef.", staff.getId());
                }
            }

            // Push StaffHeader
            return maxClefOffset;
        }

        //-------------//
        // selectClefs //
        //-------------//
        /**
         * Make final clef selection for each staff.
         */
        public void selectClefs ()
        {
            for (ClefBuilder builder : builders.values()) {
                builder.selectClef();
            }
        }
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {

        final int maxPartCount;

        final int maxEvalRank;

        // Sheet scale dependent
        //----------------------
        //
        final int maxClefEnd;

        final int beltMargin;

        final int xCoreMargin; // staff?

        final int yCoreMargin; // staff?

        // Staff scale dependent
        //----------------------
        //
        final int aboveStaff;

        final int belowStaff;

        final int minPartWeight;

        final double maxPartGap;

        final double maxGlyphHeight;

        final int minGlyphWeight;

        Parameters (Scale scale,
                    int staffSpecific)
        {
            maxPartCount = constants.maxPartCount.getValue();
            maxEvalRank = constants.maxEvalRank.getValue();

            {
                // Use sheet large interline scale
                final InterlineScale large = scale.getInterlineScale();
                maxClefEnd = large.toPixels(constants.maxClefEnd);
                beltMargin = large.toPixels(constants.beltMargin);
                xCoreMargin = large.toPixels(constants.xCoreMargin);
                yCoreMargin = large.toPixels(constants.yCoreMargin);
            }

            {
                // Use staff specific interline value
                final InterlineScale specific = scale.getInterlineScale(staffSpecific);
                aboveStaff = specific.toPixels(constants.aboveStaff);
                belowStaff = specific.toPixels(constants.belowStaff);
                minPartWeight = specific.toPixels(constants.minPartWeight);
                maxPartGap = specific.toPixelsDouble(constants.maxPartGap);
                maxGlyphHeight = specific.toPixelsDouble(constants.maxGlyphHeight);
                minGlyphWeight = specific.toPixels(constants.minGlyphWeight);
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Scale.Fraction maxClefEnd = new Scale.Fraction(
                4.5,
                "Maximum x distance from measure start to end of clef");

        private final Scale.Fraction aboveStaff = new Scale.Fraction(
                3.0,
                "Top of lookup area above stave");

        private final Scale.Fraction belowStaff = new Scale.Fraction(
                3.25,
                "Bottom of lookup area below stave");

        private final Scale.Fraction beltMargin = new Scale.Fraction(
                0.15,
                "White margin within raw rectangle");

        private final Scale.Fraction xCoreMargin = new Scale.Fraction(
                0.4,
                "Horizontal margin around core rectangle");

        private final Scale.Fraction yCoreMargin = new Scale.Fraction(
                0.5,
                "Vertical margin around core rectangle");

        private final Constant.Integer maxPartCount = new Constant.Integer(
                "Glyphs",
                8,
                "Maximum number of parts considered for a clef");

        private final Scale.AreaFraction minPartWeight = new Scale.AreaFraction(
                0.01,
                "Minimum weight for a glyph part");

        private final Scale.Fraction maxPartGap = new Scale.Fraction(
                1.0,
                "Maximum distance between two parts of a single clef symbol");

        private final Scale.Fraction maxGlyphHeight = new Scale.Fraction(
                9.0,
                "Maximum height for clef glyph");

        private final Scale.AreaFraction minGlyphWeight = new Scale.AreaFraction(
                1.0,
                "Minimum weight for clef glyph");

        private final Constant.Integer maxEvalRank = new Constant.Integer(
                "none",
                3,
                "Maximum acceptable rank in clef evaluation");
    }
}
