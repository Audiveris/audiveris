//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      C l e f B u i l d e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.header;

import omr.classifier.Classifier;
import omr.classifier.Evaluation;
import omr.classifier.GlyphClassifier;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphCluster;
import omr.glyph.GlyphFactory;
import omr.glyph.GlyphLink;
import omr.glyph.Glyphs;
import omr.glyph.Grades;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import static omr.run.Orientation.VERTICAL;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;

import omr.sig.SIGraph;
import omr.sig.inter.ClefInter;
import omr.sig.inter.ClefInter.ClefKind;
import omr.sig.inter.Inter;
import omr.sig.relation.Exclusion;

import omr.ui.symbol.Symbol;
import omr.ui.symbol.Symbols;

import omr.util.Navigable;

import ij.process.Blitter;
import ij.process.ByteProcessor;

import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(ClefBuilder.class);

    /**
     * All possible clef symbols at beginning of staff: all but small clefs.
     * Octave bass clefs are reported to be extremely <a
     * href="http://en.wikipedia.org/wiki/Clef#Octave_clefs">rare</a>.
     */
    private static final EnumSet<Shape> HEADER_CLEF_SHAPES = EnumSet.of(
            F_CLEF,
            G_CLEF,
            G_CLEF_8VA,
            G_CLEF_8VB,
            C_CLEF,
            PERCUSSION_CLEF);

    //~ Instance fields ----------------------------------------------------------------------------
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

    /** Shape classifier to use. */
    private final Classifier classifier = GlyphClassifier.getInstance();

    //~ Constructors -------------------------------------------------------------------------------
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
        params = new Parameters(scale);

        final StaffHeader header = staff.getHeader();

        if (header.clefRange != null) {
            range = header.clefRange;
        } else {
            header.clefRange = (range = new StaffHeader.Range());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // findClefs //
    //-----------//
    /**
     * Retrieve the most probable clef(s) at beginning of staff.
     * At this time, we can keep several clef kinds, final choice will be made later with key sig.
     */
    public void findClefs ()
    {
        List<Glyph> parts = getParts();

        // Formalize glyphs relationships in a global graph
        SimpleGraph<Glyph, GlyphLink> globalGraph = Glyphs.buildLinks(parts, params.maxPartGap);
        List<Set<Glyph>> sets = new ConnectivityInspector(globalGraph).connectedSets();
        logger.debug("Staff#{} sets: {}", staff.getId(), sets.size());

        // Best inter per clef kind
        Map<ClefKind, ClefInter> bestMap = new EnumMap<ClefKind, ClefInter>(ClefKind.class);

        for (Set<Glyph> set : sets) {
            // Use only the subgraph for this set
            SimpleGraph<Glyph, GlyphLink> subGraph = GlyphCluster.getSubGraph(set, globalGraph);
            ClefAdapter adapter = new ClefAdapter(subGraph, bestMap);
            new GlyphCluster(adapter, null).decompose();

            int trials = adapter.trials;
            logger.debug("Staff#{} clef parts:{} trials:{}", staff.getId(), set.size(), trials);
        }

        // Register the best inter, if any, for each clef kind
        if (!bestMap.isEmpty()) {
            Integer minClefStop = null;
            List<Inter> inters = new ArrayList<Inter>();

            for (Entry<ClefKind, ClefInter> entry : bestMap.entrySet()) {
                ClefInter inter = entry.getValue();
                inters.add(inter);

                // Unerased staff line chunks may shift the symbol in abscissa,
                // so use glyph centroid for a better positioning
                //TODO: we could also check histogram right after clef end, looking for a low point?
                Rectangle clefBox = inter.getSymbolBounds(scale.getInterline());
                Symbol symbol = Symbols.getSymbol(inter.getShape());
                Point symbolCentroid = symbol.getCentroid(clefBox);
                Point glyphCentroid = inter.getGlyph().getCentroid();
                int dx = glyphCentroid.x - symbolCentroid.x;
                int dy = glyphCentroid.y - symbolCentroid.y;
                logger.debug("Centroid translation dx:{} dy:{}", dx, dy);
                clefBox.translate(dx, 0);
                inter.setBounds(clefBox);
                inter.setStaff(staff);

                int gid = inter.getGlyph().getId();
                sig.addVertex(inter);
                logger.debug("Staff#{} {} g#{} {}", staff.getId(), inter, gid, clefBox);

                int end = clefBox.x + clefBox.width;
                minClefStop = (minClefStop == null) ? end : Math.min(minClefStop, end);
            }

            sig.insertExclusions(inters, Exclusion.Cause.OVERLAP);
            staff.setClefStop(minClefStop);
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

    //----------//
    // getParts //
    //----------//
    /**
     * Retrieve all glyph instances that could be part of clef.
     *
     * @return clef possible parts
     */
    private List<Glyph> getParts ()
    {
        final int interline = scale.getInterline();

        // Rectangular ROI (within sheet image)
        int areaTop = Math.max(
                0,
                staff.getFirstLine().yAt(range.browseStart) - (3 * interline));
        int areaBottom = Math.min(
                sheet.getHeight() - 1,
                staff.getLastLine().yAt(range.browseStart) + (3 * interline));
        Rectangle rect = new Rectangle(
                range.browseStart,
                areaTop,
                range.browseStop - range.browseStart,
                areaBottom - areaTop + 1);
        rect.grow(-params.beltMargin, 0);
        staff.addAttachment("C", rect);

        // Grab pixels out of staff-free source
        ByteProcessor source = sheet.getPicture().getSource(Picture.SourceKey.NO_STAFF);
        ByteProcessor buf = new ByteProcessor(rect.width, rect.height);
        buf.copyBits(source, -rect.x, -rect.y, Blitter.COPY);

        // Extract parts
        RunTable runTable = new RunTableFactory(VERTICAL).createTable(buf);
        List<Glyph> parts = GlyphFactory.buildGlyphs(runTable, rect.getLocation());

        // Keep only interesting parts
        purgeParts(parts, rect);
        logger.debug("Clef parts: {}", parts.size());

        // debug: register parts glyphs
        for (Glyph glyph : parts) {
            sheet.getGlyphIndex().register(glyph);
        }

        return parts;
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
        // The rect is used for cropping only.
        // Use a smaller core rectangle which must be intersected by any part candidate
        Rectangle core = new Rectangle(rect);
        core.grow(0, -params.yCoreMargin);
        core.x += params.xCoreMargin;
        core.width -= params.xCoreMargin;
        staff.addAttachment("c", core);

        List<Glyph> toRemove = new ArrayList<Glyph>();

        for (Glyph part : parts) {
            if ((part.getWeight() < params.minPartWeight) || !part.getBounds().intersects(core)) {
                toRemove.add(part);
            }
        }

        if (!toRemove.isEmpty()) {
            parts.removeAll(toRemove);
        }
    }

    //------------//
    // selectClef //
    //------------//
    /**
     * Make the final selection of best clef for this staff header.
     */
    private void selectClef ()
    {
        // All clef candidates for this staff (which right now means just the header)
        List<Inter> clefs = sig.inters(staff, ClefInter.class);

        if (!clefs.isEmpty()) {
            for (Inter clef : clefs) {
                sig.computeContextualGrade(clef, false);
            }

            Collections.sort(clefs, Inter.byReverseBestGrade);

            // Pickup the first one as header clef
            ClefInter bestClef = (ClefInter) clefs.get(0);
            bestClef.setGlyph(sheet.getGlyphIndex().registerOriginal(bestClef.getGlyph()));
            staff.getHeader().clef = bestClef;

            // Delete the other candidates
            for (Inter other : clefs.subList(1, clefs.size())) {
                other.delete();
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Column //
    //--------//
    /**
     * Manages the system consistency for a column of ClefBuilder instances.
     */
    public static class Column
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final SystemInfo system;

        /** Map of clef builders. (one per staff) */
        private final Map<Staff, ClefBuilder> builders = new TreeMap<Staff, ClefBuilder>(
                Staff.byId);

        //~ Constructors ---------------------------------------------------------------------------
        public Column (SystemInfo system)
        {
            this.system = system;
        }

        //~ Methods --------------------------------------------------------------------------------
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
                int measureStart = staff.getHeaderStart();

                // Retrieve staff clef
                ClefBuilder builder = new ClefBuilder(staff);
                builder.setBrowseStart(measureStart);
                builders.put(staff, builder);
                builder.findClefs();
                maxClefOffset = Math.max(maxClefOffset, staff.getClefStop() - measureStart);
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
        //~ Instance fields ------------------------------------------------------------------------

        final int maxClefEnd;

        final int beltMargin;

        final int xCoreMargin;

        final int yCoreMargin;

        final int minPartWeight;

        final double maxPartGap;

        final double maxGlyphHeight;

        final int minGlyphWeight;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxClefEnd = scale.toPixels(constants.maxClefEnd);
            beltMargin = scale.toPixels(constants.beltMargin);
            xCoreMargin = scale.toPixels(constants.xCoreMargin);
            yCoreMargin = scale.toPixels(constants.yCoreMargin);
            minPartWeight = scale.toPixels(constants.minPartWeight);
            maxPartGap = scale.toPixelsDouble(constants.maxPartGap);
            maxGlyphHeight = scale.toPixelsDouble(constants.maxGlyphHeight);
            minGlyphWeight = scale.toPixels(constants.minGlyphWeight);
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
        //~ Instance fields ------------------------------------------------------------------------

        /** Best inter per clef kind. */
        private final Map<ClefKind, ClefInter> bestMap;

        //~ Constructors ---------------------------------------------------------------------------
        public ClefAdapter (SimpleGraph<Glyph, GlyphLink> graph,
                            Map<ClefKind, ClefInter> bestMap)
        {
            super(graph);
            this.bestMap = bestMap;
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void evaluateGlyph (Glyph glyph)
        {
            trials++;

            if (glyph.getId() == 0) {
                glyph = sheet.getGlyphIndex().registerOriginal(glyph);
            }

            logger.debug("ClefAdapter evaluateGlyph on {}", glyph);

            // TODO: use some checking, such as pitch position?
            Evaluation[] evals = classifier.getNaturalEvaluations(glyph, sheet.getInterline());

            for (Shape shape : HEADER_CLEF_SHAPES) {
                Evaluation eval = evals[shape.ordinal()];
                double grade = Inter.intrinsicRatio * eval.grade;

                if (grade >= Grades.clefMinGrade) {
                    ClefKind kind = ClefInter.kindOf(glyph, shape, staff);
                    ClefInter bestInter = bestMap.get(kind);

                    if ((bestInter == null) || (bestInter.getGrade() < grade)) {
                        bestMap.put(kind, ClefInter.create(glyph, shape, grade, staff));
                    }
                }
            }
        }

        @Override
        public boolean isSizeAcceptable (Rectangle box)
        {
            return box.height <= params.maxGlyphHeight;
        }

        @Override
        public boolean isWeightAcceptable (int weight)
        {
            return weight >= params.minGlyphWeight;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction maxClefEnd = new Scale.Fraction(
                4.5,
                "Maximum x distance from measure start to end of clef");

        private final Scale.Fraction beltMargin = new Scale.Fraction(
                0.15,
                "White margin within raw rectangle");

        private final Scale.Fraction xCoreMargin = new Scale.Fraction(
                0.4,
                "Horizontal margin around core rectangle");

        private final Scale.Fraction yCoreMargin = new Scale.Fraction(
                0.5,
                "Vertical margin around core rectangle");

        private final Scale.AreaFraction minPartWeight = new Scale.AreaFraction(
                0.01,
                "Minimum weight for a glyph part");

        private final Scale.Fraction maxPartGap = new Scale.Fraction(
                1.0,
                "Maximum distance between two parts of a single clef symbol");

        private final Scale.Fraction maxGlyphHeight = new Scale.Fraction(
                8.0,
                "Maximum height for clef glyph");

        private final Scale.AreaFraction minGlyphWeight = new Scale.AreaFraction(
                1.0,
                "Minimum weight for clef glyph");
    }
}
