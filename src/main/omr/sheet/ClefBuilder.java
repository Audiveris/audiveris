//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      C l e f B u i l d e r                                     //
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
import static omr.glyph.Shape.*;
import omr.glyph.ShapeEvaluator;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.math.GeoUtil;

import omr.run.Orientation;

import omr.sig.ClefInter;
import omr.sig.ClefInter.ClefKind;
import omr.sig.Exclusion;
import omr.sig.Inter;
import omr.sig.SIGraph;

import omr.ui.symbol.Symbol;
import omr.ui.symbol.Symbols;

import omr.util.Navigable;

import ij.process.Blitter;
import ij.process.ByteProcessor;

import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
     * Octave bass clefs are extremely <a
     * href="http://en.wikipedia.org/wiki/Clef#Octave_clefs">rare</a>.
     */
    private static final EnumSet<Shape> clefShapes = EnumSet.of(
            F_CLEF,
            G_CLEF,
            G_CLEF_8VA,
            G_CLEF_8VB,
            C_CLEF,
            PERCUSSION_CLEF);

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

    /** Precise beginning abscissa of measure. */
    private final int measureStart;

    /** Estimated ending abscissa of cumuls. */
    private final int cumulEnd;

    /** Lag for glyph sections. */
    private final Lag lag = new BasicLag("clef", Orientation.VERTICAL);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new ClefBuilder object.
     *
     * @param staff        the underlying staff
     * @param measureStart precise beginning abscissa of measure (generally right after bar line).
     */
    public ClefBuilder (StaffInfo staff,
                        int measureStart)
    {
        this.staff = staff;
        this.measureStart = measureStart;

        system = staff.getSystem();
        sig = system.getSig();
        sheet = system.getSheet();
        scale = sheet.getScale();
        params = new Parameters(scale);
        cumulEnd = measureStart + params.maxClefEnd;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // findClef //
    //----------//
    /**
     * Retrieve the most probable clef(s) at beginning of staff.
     */
    public void findClef ()
    {
        List<Glyph> parts = getParts();

        MyAdapter adapter = new MyAdapter(parts);
        new GlyphCluster(adapter).decompose();
        logger.debug(
                "Staff#{} clef parts:{} trials:{}",
                staff.getId(),
                parts.size(),
                adapter.trials);

        if (!adapter.bestMap.isEmpty()) {
            Integer minClefStop = null;
            List<Inter> inters = new ArrayList<Inter>();

            for (Entry<ClefKind, ClefInter> entry : adapter.bestMap.entrySet()) {
                ///sheet.getNest().registerGlyph(adapter.bestGlyph);
                ClefInter inter = entry.getValue();
                inter.increase(0.2); //TODO boost these DMZ clefs
                inters.add(inter);

                // Unerased staff line chunks may shift the symbol in abscissa,
                // so use glyph centroid for a better positioning
                //TODO: we could also check histogram right after clef end, looking for a low point
                Rectangle clefBox = inter.getSymbolBounds(scale.getInterline());
                Symbol symbol = Symbols.getSymbol(inter.getShape());
                Point symbolCentroid = symbol.getCentroid(clefBox);
                Point glyphCentroid = inter.getGlyph().getCentroid();
                int dx = glyphCentroid.x - symbolCentroid.x;
                int dy = glyphCentroid.y - symbolCentroid.y;
                logger.debug("Centroid translation dx:{} dy:{}", dx, dy);
                clefBox.translate(dx, 0);
                inter.setBounds(clefBox);

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
        // Rectangular ROI (within sheet image)
        int areaTop = Math.max(
                0,
                staff.getFirstLine().yAt(measureStart) - (3 * scale.getInterline()));
        int areaBottom = Math.min(
                sheet.getHeight() - 1,
                staff.getLastLine().yAt(measureStart) + (3 * scale.getInterline()));
        Rectangle rect = new Rectangle(
                measureStart,
                areaTop,
                cumulEnd - measureStart,
                areaBottom - areaTop + 1);
        rect.grow(-params.beltMargin, 0);
        staff.addAttachment("C", rect);

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
                true);

        // Keep only interesting parts
        purgeParts(parts, rect);

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

        private final Sheet sheet;

        /** Scale-dependent parameters. */
        private final Parameters params;

        /** Map of clef builders. (one per staff) */
        private final Map<StaffInfo, ClefBuilder> builders = new TreeMap<StaffInfo, ClefBuilder>(
                StaffInfo.byId);

        //~ Constructors ---------------------------------------------------------------------------
        public Column (SystemInfo system)
        {
            this.system = system;
            sheet = system.getSheet();
            params = new Parameters(sheet.getScale());
        }

        //~ Methods --------------------------------------------------------------------------------
        //---------------//
        // retrieveClefs //
        //---------------//
        /**
         * Retrieve the column of staves clefs.
         *
         * @return the ending abscissa offset of clefs column WRT measure start
         */
        public int retrieveClefs ()
        {
            // Retrieve DMZ clefs
            int maxClefOffset = 0;

            for (StaffInfo staff : system.getStaves()) {
                int measureStart = staff.getDmzStart();

                // Retrieve staff clef
                ClefBuilder builder = new ClefBuilder(staff, measureStart);
                builders.put(staff, builder);
                builder.findClef();
                maxClefOffset = Math.max(maxClefOffset, staff.getClefStop() - measureStart);
            }

            // Push DMZ
            return maxClefOffset;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Scale.Fraction maxClefEnd = new Scale.Fraction(
                4.5,
                "Maximum x distance from measure start to end of clef");

        final Scale.Fraction beltMargin = new Scale.Fraction(
                0.15,
                "White margin within raw rectangle");

        final Scale.Fraction xCoreMargin = new Scale.Fraction(
                0.4,
                "Horizontal margin around core rectangle");

        final Scale.Fraction yCoreMargin = new Scale.Fraction(
                0.5,
                "Vertical margin around core rectangle");

        final Scale.AreaFraction minPartWeight = new Scale.AreaFraction(
                0.01,
                "Minimum weight for a glyph part");

        final Scale.Fraction maxPartGap = new Scale.Fraction(
                1.0,
                "Maximum distance between two parts of a single clef symbol");

        final Scale.Fraction maxGlyphHeight = new Scale.Fraction(
                8.0,
                "Maximum height for clef glyph");

        final Scale.AreaFraction minGlyphWeight = new Scale.AreaFraction(
                0.3,
                "Minimum weight for clef glyph");
    }

    //-----------//
    // MyAdapter //
    //-----------//
    /**
     * Handles the integration between glyph clustering class and key-sig environment.
     * <p>
     * For each clef kind, we keep the best result found if any.
     */
    private class MyAdapter
            implements GlyphCluster.Adapter
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Graph of the connected glyphs, with their distance edges if any. */
        private final SimpleGraph<Glyph, GlyphLink> graph;

        /** Best inter per clef kind. */
        private Map<ClefKind, ClefInter> bestMap = new EnumMap<ClefKind, ClefInter>(ClefKind.class);

        // For debug only
        private int trials = 0;

        //~ Constructors ---------------------------------------------------------------------------
        public MyAdapter (List<Glyph> parts)
        {
            graph = Glyphs.buildLinks(parts, params.maxPartGap);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public void evaluateGlyph (Glyph glyph)
        {
            trials++;

            // TODO: use some checking, such as pitch position?
            Evaluation[] evals = evaluator.getNaturalEvaluations(glyph);

            for (Shape shape : clefShapes) {
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
}
