//----------------------------------------------------------------------------//
//                                                                            //
//                        L i n e s R e t r i e v e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.NestView;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.run.Orientation;
import static omr.run.Orientation.*;
import omr.run.Run;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import omr.sheet.SystemInfo;

import omr.ui.Colors;
import omr.ui.util.UIUtil;
import static omr.util.HorizontalSide.*;
import omr.util.Predicate;
import omr.util.StopWatch;
import omr.util.VipUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code LinesRetriever} retrieves the staff lines of a sheet.
 *
 * @author Hervé Bitteur
 */
public class LinesRetriever
        implements NestView.ItemRenderer
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(LinesRetriever.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** related sheet */
    private final Sheet sheet;

    /** Related scale */
    private final Scale scale;

    /** Scale-dependent constants for horizontal stuff */
    private final Parameters params;

    /** Lag of horizontal runs */
    private Lag hLag;

    /** Filaments factory */
    private FilamentsFactory factory;

    /** Long horizontal filaments found, non sorted */
    private final List<LineFilament> filaments = new ArrayList<>();

    /** Second collection of filaments */
    private List<LineFilament> secondFilaments;

    /** Discarded filaments */
    private List<LineFilament> discardedFilaments;

    /** Global slope of the sheet */
    private double globalSlope;

    /** Companion in charge of clusters of main interline */
    private ClustersRetriever clustersRetriever;

    /** Companion in charge of clusters of second interline, if any */
    private ClustersRetriever secondClustersRetriever;

    /** Companion in charge of bar lines */
    private final BarsRetriever barsRetriever;

    /** Too-short horizontal runs */
    private RunsTable shortHoriTable;

    /** For runs display, if any */
    private final RunsViewer runsViewer;

    //~ Constructors -----------------------------------------------------------
    //
    //----------------//
    // LinesRetriever //
    //----------------//
    /**
     * Retrieve the frames of all staff lines.
     *
     * @param sheet         the sheet to process
     * @param barsRetriever the companion in charge of bars
     */
    public LinesRetriever (Sheet sheet,
                           BarsRetriever barsRetriever)
    {
        this.sheet = sheet;
        this.barsRetriever = barsRetriever;

        runsViewer = (Main.getGui() != null)
                     ? new RunsViewer(sheet, this, barsRetriever) : null;

        scale = sheet.getScale();
        params = new Parameters(scale);
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // buildLag //
    //----------//
    /**
     * Build the underlying lag, out of the provided runs table.
     *
     * @param wholeVertTable the provided table of all (vertical) runs
     * @param showRuns       (debug) true to create intermediate views on runs
     * @return the vertical runs too long to be part of any staff line
     */
    public RunsTable buildLag (RunsTable wholeVertTable,
                               boolean showRuns)
    {
        hLag = new BasicLag("hLag", Orientation.HORIZONTAL);

        // Create filament factory
        try {
            factory = new FilamentsFactory(
                    scale,
                    sheet.getNest(),
                    Orientation.HORIZONTAL,
                    LineFilament.class);
        } catch (Exception ex) {
            logger.warn("Cannot create lines filament factory", ex);
        }

        // To record the purged vertical runs
        RunsTable longVertTable = new RunsTable(
                "long-vert",
                VERTICAL,
                new Dimension(sheet.getWidth(), sheet.getHeight()));

        // Remove runs whose height is larger than line thickness
        RunsTable shortVertTable = wholeVertTable.copy("short-vert").purge(
                new Predicate<Run>()
                {
                    @Override
                    public final boolean check (Run run)
                    {
                        return run.getLength() > params.maxVerticalRunLength;
                    }
                },
                longVertTable);

        if (showRuns) {
            runsViewer.display(longVertTable);
            runsViewer.display(shortVertTable);
        }

        // Build table of long horizontal runs
        RunsTable wholeHoriTable = new RunsTableFactory(
                HORIZONTAL,
                shortVertTable.getBuffer(),
                0).createTable("whole-hori");

        // To record the purged horizontal runs
        shortHoriTable = new RunsTable(
                "short-hori",
                HORIZONTAL,
                new Dimension(sheet.getWidth(), sheet.getHeight()));

        RunsTable longHoriTable = wholeHoriTable.copy("long-hori").purge(
                new Predicate<Run>()
                {
                    @Override
                    public final boolean check (Run run)
                    {
                        return run.getLength() < params.minRunLength;
                    }
                },
                shortHoriTable);

        if (showRuns) {
            runsViewer.display(shortHoriTable);
            runsViewer.display(longHoriTable);
        }

        // Build the horizontal hLag with the long horizontal runs
        // (short horizontal runs will be added later)
        SectionsBuilder sectionsBuilder = new SectionsBuilder(
                hLag,
                new JunctionRatioPolicy(params.maxLengthRatio));
        sectionsBuilder.createSections(longHoriTable);

        sheet.setHorizontalLag(hLag);

        setVipSections();

        return longVertTable;
    }

    //---------------//
    // completeLines //
    //---------------//
    /**
     * Complete the retrieved staff lines whenever possible with
     * filaments and short sections left over.
     *
     * <p><b>Synopsis:</b>
     * <pre>
     *      + includeDiscardedFilaments
     *          + canIncludeFilament(fil1, fil2)
     *      + createShortSections()
     *      + includeSections()
     *          + canIncludeSection(fil, sct)
     * </pre>
     */
    public void completeLines ()
    {
        StopWatch watch = new StopWatch("completeLines");

        try {
            // Browse discarded filaments for possible inclusion
            watch.start("include discarded filaments");
            includeDiscardedFilaments();

            // Build sections out of shortHoriTable (too short horizontal runs)
            watch.start("create shortSections");

            List<Section> shortSections = createShortSections();

            // Dispatch sections into thick & thin ones
            watch.start(
                    "dispatching " + shortSections.size() + " thick / thin");

            List<Section> thickSections = new ArrayList<>();
            List<Section> thinSections = new ArrayList<>();

            for (Section section : shortSections) {
                if (section.getWeight() > params.maxThinStickerWeight) {
                    thickSections.add(section);
                } else {
                    thinSections.add(section);
                }
            }

            // First, consider thick sections and update geometry
            watch.start("include " + thickSections.size() + " thick stickers");
            includeSections(thickSections, true);

            // Second, consider thin sections w/o updating the geometry
            watch.start("include " + thinSections.size() + " thin stickers");
            includeSections(thinSections, false);

            // Update system coordinates
            for (SystemInfo system : sheet.getSystems()) {
                system.updateCoordinates();
            }
        } finally {
            if (constants.printWatch.getValue()) {
                watch.print();
            }
        }
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render the filaments, their ending tangents, their combs
     *
     * @param g graphics context
     */
    @Override
    public void renderItems (Graphics2D g)
    {
        final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
        final Color oldColor = g.getColor();
        g.setColor(Colors.ENTITY_MINOR);

        // Combs stuff?
        if (constants.showCombs.isSet()) {
            if (clustersRetriever != null) {
                clustersRetriever.renderItems(g);
            }

            if (secondClustersRetriever != null) {
                secondClustersRetriever.renderItems(g);
            }
        }

        // Filament lines?
        if (constants.showHorizontalLines.isSet()) {
            List<LineFilament> allFils = new ArrayList<>(filaments);

            if (secondFilaments != null) {
                allFils.addAll(secondFilaments);
            }

            for (Filament filament : allFils) {
                filament.renderLine(g);
            }

            // Draw tangent at each ending point?
            if (constants.showTangents.isSet()) {
                g.setColor(Colors.TANGENT);

                double dx = sheet.getScale().toPixels(constants.tangentLg);

                for (Filament filament : allFils) {
                    Point2D p = filament.getStartPoint(HORIZONTAL);
                    double der = filament.slopeAt(p.getX(), HORIZONTAL);
                    g.draw(
                            new Line2D.Double(
                            p.getX(),
                            p.getY(),
                            p.getX() - dx,
                            p.getY() - (der * dx)));
                    p = filament.getStopPoint(HORIZONTAL);
                    der = filament.slopeAt(p.getX(), HORIZONTAL);
                    g.draw(
                            new Line2D.Double(
                            p.getX(),
                            p.getY(),
                            p.getX() + dx,
                            p.getY() + (der * dx)));
                }
            }
        }

        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    //---------------//
    // retrieveLines //
    //---------------//
    /**
     * Organize the long and thin horizontal sections into filaments
     * (glyphs) that will be good candidates for staff lines.
     * <ol>
     * <li>First, retrieve long horizontal sections and merge them into
     * filaments.</li>
     * <li>Second, detect series of filaments regularly spaced and aggregate
     * them into clusters of lines (as staff candidates). </li>
     * </ol>
     *
     * <p><b>Synopsis:</b>
     * <pre>
     *      + filamentFactory.retrieveFilaments()
     *      + retrieveGlobalSlope()
     *      + clustersRetriever.buildInfo()
     *      + secondClustersRetriever.buildInfo()
     *      + buildStaves()
     * </pre>
     */
    public void retrieveLines ()
    {
        StopWatch watch = new StopWatch("retrieveLines");

        try {
            // Retrieve filaments out of merged long sections
            watch.start("retrieveFilaments");

            for (Glyph fil : factory.retrieveFilaments(
                    hLag.getSections(),
                    true)) {
                filaments.add((LineFilament) fil);
            }

            // Compute global slope out of longest filaments
            watch.start("retrieveGlobalSlope");
            globalSlope = retrieveGlobalSlope();
            sheet.setSkew(new Skew(globalSlope, sheet));
            logger.info("{}Global slope: {}",
                        sheet.getLogPrefix(), (float) globalSlope);

            // Retrieve regular patterns of filaments and pack them into clusters
            clustersRetriever = new ClustersRetriever(
                    sheet,
                    filaments,
                    scale.getInterline(),
                    Colors.COMB);
            watch.start("clustersRetriever");

            discardedFilaments = clustersRetriever.buildInfo();

            // Check for a second interline
            Integer secondInterline = scale.getSecondInterline();

            if (secondInterline != null && !discardedFilaments.isEmpty()) {
                secondFilaments = discardedFilaments;
                Collections.sort(secondFilaments, Glyph.byId);
                logger.info("{}Searching clusters with secondInterline: {}",
                            sheet.getLogPrefix(), secondInterline);
                secondClustersRetriever = new ClustersRetriever(
                        sheet,
                        secondFilaments,
                        secondInterline,
                        Colors.COMB_MINOR);
                watch.start("secondClustersRetriever");
                discardedFilaments = secondClustersRetriever.buildInfo();
            }

            logger.debug("Discarded filaments: {}", Glyphs.toString(
                    discardedFilaments));

            // Convert clusters into staves
            watch.start("BuildStaves");
            buildStaves();
        } finally {
            if (constants.printWatch.getValue()) {
                watch.print();
            }
        }
    }

    //-------------//
    // buildStaves //
    //-------------//
    /**
     * Register line clusters as staves
     */
    private void buildStaves ()
    {
        // Accumulate all clusters, and sort them by ordinate
        List<LineCluster> allClusters = new ArrayList<>();
        allClusters.addAll(clustersRetriever.getClusters());

        if (secondClustersRetriever != null) {
            allClusters.addAll(secondClustersRetriever.getClusters());
        }

        Collections.sort(allClusters, clustersRetriever.ordinateComparator);

        // Populate the staff manager
        StaffManager staffManager = sheet.getStaffManager();
        int staffId = 0;
        staffManager.reset();

        for (LineCluster cluster : allClusters) {
            logger.debug(cluster.toString());
            List<LineInfo> lines = new ArrayList<LineInfo>(cluster.getLines());
            double left = Integer.MAX_VALUE;
            double right = Integer.MIN_VALUE;

            for (LineInfo line : lines) {
                left = Math.min(left, line.getEndPoint(LEFT).getX());
                right = Math.max(right, line.getEndPoint(RIGHT).getX());
            }

            StaffInfo staff = new StaffInfo(
                    ++staffId,
                    left,
                    right,
                    new Scale(cluster.getInterline(), scale.getMainFore()),
                    lines);
            staffManager.addStaff(staff);
        }

        staffManager.computeStaffLimits();

        // Polish staff lines
        for (StaffInfo staff : staffManager.getStaves()) {
            staff.getArea();

            for (LineInfo l : staff.getLines()) {
                FilamentLine line = (FilamentLine) l;
                line.fil.polishCurvature();
            }
        }
    }

    //------------//
    // canInclude //
    //------------//
    /**
     * Check whether the staff line filament could include the provided
     * entity (section or filament)
     *
     * @param filament  the staff line filament
     * @param idStr     (debug) entity id
     * @param isVip     true if entity is vip
     * @param box       the entity contour box
     * @param center    the entity center
     * @param candidate the section or glyph candidate
     * @return true if OK, false otherwise
     */
    private boolean canInclude (LineFilament filament,
                                boolean isVip,
                                String idStr,
                                Rectangle box,
                                Point center,
                                Object candidate)
    {
        // For VIP debugging
        String vips = null;

        if (isVip) {
            vips = idStr + ": "; // BP here!
        }

        // Check entity thickness
        int height = box.height;

        if (height > params.maxStickerThickness) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info("{}SSS height:{} vs {}",
                            vips, height, params.maxStickerThickness);
            }

            return false;
        }

        // Check entity center gap with theoretical line
        double yFil = filament.getPositionAt(center.x, HORIZONTAL);
        double dy = Math.abs(yFil - center.y);
        double gap = dy - (scale.getMainFore() / 2.0);

        if (gap > params.maxStickerGap) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info("{}GGG gap:{} vs {}",
                            vips, (float) gap, (float) params.maxStickerGap);
            }

            return false;
        }

        // Check max extension from theoretical line
        double extension = Math.max(
                Math.abs(yFil - box.y),
                Math.abs((box.y + height) - yFil));

        if (extension > params.maxStickerExtension) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info("{}XXX ext:{} vs {}",
                            vips, (float) extension, params.maxStickerExtension);
            }

            return false;
        }

        // Check resulting thickness
        double thickness = 0;

        if (candidate instanceof Section) {
            thickness = Glyphs.getThicknessAt(
                    center.x,
                    HORIZONTAL,
                    (Section) candidate,
                    filament);
        } else if (candidate instanceof Glyph) {
            thickness = Glyphs.getThicknessAt(
                    center.x,
                    HORIZONTAL,
                    (Glyph) candidate,
                    filament);
        }

        if (thickness > params.maxStickerThickness) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info("{}RRR thickness:{} vs {}",
                            vips, (float) thickness, params.maxStickerExtension);
            }

            return false;
        }

        if (logger.isDebugEnabled() || isVip) {
            logger.info("{}---", vips);
        }

        return true;
    }

    //--------------------//
    // canIncludeFilament //
    //--------------------//
    /**
     * Check whether the staff line filament could include the candidate
     * filament
     *
     * @param filament the staff line filament
     * @param fil      the candidate filament
     * @return true if OK
     */
    private boolean canIncludeFilament (LineFilament filament,
                                        Filament fil)
    {
        return canInclude(
                filament,
                fil.isVip(),
                "Fil#" + fil.getId(),
                fil.getBounds(),
                fil.getCentroid(),
                fil);
    }

    //-------------------//
    // canIncludeSection //
    //-------------------//
    /**
     * Check whether the staff line filament could include the candidate
     * section
     *
     * @param filament the staff line filament
     * @param section  the candidate sticker
     * @return true if OK, false otherwise
     */
    private boolean canIncludeSection (LineFilament filament,
                                       Section section)
    {
        return canInclude(
                filament,
                section.isVip(),
                "Sct#" + section.getId(),
                section.getBounds(),
                section.getCentroid(),
                section);
    }

    //---------------------//
    // createShortSections //
    //---------------------//
    /**
     * Build horizontal sections out of shortHoriTable runs
     *
     * @return the list of created sections
     */
    private List<Section> createShortSections ()
    {
        // Note the current section id
        sheet.setLongSectionMaxId(hLag.getLastVertexId());

        // Augment the horizontal hLag with the short sections
        SectionsBuilder sectionsBuilder = new SectionsBuilder(
                hLag,
                new JunctionRatioPolicy(params.maxLengthRatioShort));
        List<Section> shortSections = sectionsBuilder.createSections(
                shortHoriTable);

        setVipSections();

        return shortSections;
    }

    //---------------------------//
    // includeDiscardedFilaments //
    //---------------------------//
    /**
     * Last attempt to include discarded filaments to retrieved staff lines
     */
    private void includeDiscardedFilaments ()
    {
        // Sort these discarded filaments by top ordinate
        Collections.sort(discardedFilaments, Filament.topComparator);

        int iMin = 0;
        int iMax = discardedFilaments.size() - 1;

        for (SystemInfo system : sheet.getSystems()) {
            for (StaffInfo staff : system.getStaves()) {
                for (LineInfo l : staff.getLines()) {
                    FilamentLine line = (FilamentLine) l;
                    LineFilament filament = line.fil;
                    Rectangle lineBox = filament.getBounds();
                    lineBox.grow(0, scale.getMainFore());

                    double minX = filament.getStartPoint(HORIZONTAL).getX();
                    double maxX = filament.getStopPoint(HORIZONTAL).getX();
                    int minY = lineBox.y;
                    int maxY = lineBox.y + lineBox.height;

                    for (int i = iMin; i <= iMax; i++) {
                        Filament fil = discardedFilaments.get(i);

                        if (fil.getPartOf() != null) {
                            continue;
                        }

                        int firstPos = fil.getBounds().y;

                        if (firstPos < minY) {
                            iMin = i;

                            continue;
                        }

                        if (firstPos > maxY) {
                            break;
                        }

                        Point center = fil.getCentroid();

                        if ((center.x >= minX) && (center.x <= maxX)) {
                            if (canIncludeFilament(filament, fil)) {
                                filament.stealSections(fil);
                            }
                        }
                    }
                }
            }

            barsRetriever.adjustStaffLines(system);
        }
    }

    //-----------------//
    // includeSections //
    //-----------------//
    /**
     * Include "sticker" sections into their related lines, when
     * applicable
     *
     * @param sections List of sections that are stickers candidates
     * @param update   should we update the line geometry with stickers (this
     *                 should be limited to large sections).
     */
    private void includeSections (List<Section> sections,
                                  boolean update)
    {
        // Sections are sorted according to their top run (Y first and X second)
        int iMin = 0;
        int iMax = sections.size() - 1;

        // Inclusion on the fly would imply recomputation of filament at each
        // section inclusion. So we need to retrieve all "stickers" for a given
        // staff line, and perform a global inclusion at the end only.
        for (SystemInfo system : sheet.getSystems()) {
            for (StaffInfo staff : system.getStaves()) {
                for (LineInfo l : staff.getLines()) {
                    FilamentLine line = (FilamentLine) l;
                    LineFilament fil = line.fil;
                    Rectangle lineBox = fil.getBounds();
                    lineBox.grow(0, scale.getMainFore());

                    double minX = fil.getStartPoint(HORIZONTAL).getX();
                    double maxX = fil.getStopPoint(HORIZONTAL).getX();
                    int minY = lineBox.y;
                    int maxY = lineBox.y + lineBox.height;
                    List<Section> stickers = new ArrayList<>();

                    for (int i = iMin; i <= iMax; i++) {
                        Section section = sections.get(i);

                        if (section.isGlyphMember()) {
                            continue;
                        }

                        int firstPos = section.getFirstPos();

                        if (firstPos < minY) {
                            iMin = i;

                            continue;
                        }

                        if (firstPos > maxY) {
                            break;
                        }

                        Point center = section.getCentroid();

                        if ((center.x >= minX) && (center.x <= maxX)) {
                            if (canIncludeSection(fil, section)) {
                                stickers.add(section);
                            }
                        }
                    }

                    // Actually include the retrieved stickers?
                    for (Section section : stickers) {
                        if (update) {
                            fil.addSection(section);
                        } else {
                            section.setGlyph(fil);
                        }
                    }
                }
            }

            barsRetriever.adjustStaffLines(system);
        }
    }

    //---------------------//
    // retrieveGlobalSlope //
    //---------------------//
    private double retrieveGlobalSlope ()
    {
        // Use the top longest filaments to determine slope
        final double ratio = params.topRatioForSlope;
        final int topCount = Math.max(
                1,
                (int) Math.rint(filaments.size() * ratio));
        double slopes = 0;
        Collections.sort(
                filaments,
                Glyphs.getReverseLengthComparator(HORIZONTAL));

        for (int i = 0; i < topCount; i++) {
            Filament fil = filaments.get(i);
            Point2D start = fil.getStartPoint(HORIZONTAL);
            Point2D stop = fil.getStopPoint(HORIZONTAL);
            slopes += ((stop.getY() - start.getY()) / (stop.getX()
                                                       - start.getX()));
        }

        return slopes / topCount;
    }

    //----------------//
    // setVipSections //
    //----------------//
    private void setVipSections ()
    {
        // Debug sections VIPs
        for (int id : params.vipSections) {
            Section sect = hLag.getVertexById(id);

            if (sect != null) {
                sect.setVip();
                logger.info("Horizontal vip section: {}", sect);
            }
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Constant.Ratio topRatioForSlope = new Constant.Ratio(
                0.1,
                "Percentage of top filaments used to retrieve global slope");

        // Constants for building horizontal sections 
        // ------------------------------------------
        final Constant.Ratio maxLengthRatio = new Constant.Ratio(
                1.5,
                "Maximum ratio in length for a run to be combined with an existing section");

        final Constant.Ratio maxLengthRatioShort = new Constant.Ratio(
                3.0,
                "Maximum ratio in length for a short run to be combined with an existing section");

        // Constants specified WRT *maximum* line thickness (scale.getmaxFore())
        // ----------------------------------------------
        // Should be 1.0, unless ledgers are thicker than staff lines
        final Constant.Ratio ledgerThickness = new Constant.Ratio(
                1.2, // 2.0, 
                "Ratio of ledger thickness vs staff line MAXIMUM thickness");

        final Constant.Ratio stickerThickness = new Constant.Ratio(
                1.0, //1.2,
                "Ratio of sticker thickness vs staff line MAXIMUM thickness");

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        //
        final Scale.LineFraction maxStickerGap = new Scale.LineFraction(
                0.5,
                "Maximum vertical gap between sticker and closest line side");

        final Scale.LineFraction maxStickerExtension = new Scale.LineFraction(
                1.2,
                "Maximum vertical sticker extension from line");

        final Scale.AreaFraction maxThinStickerWeight = new Scale.AreaFraction(
                0.06,
                "Maximum weight for a thin sticker (w/o impact on line geometry)");

        // Constants specified WRT mean interline
        // --------------------------------------
        final Scale.Fraction minRunLength = new Scale.Fraction(
                1.0,
                "Minimum length for a horizontal run to be considered");

        // Constants for display
        // ---------------------
        final Constant.Boolean showHorizontalLines = new Constant.Boolean(
                true,
                "Should we display the horizontal lines?");

        final Scale.Fraction tangentLg = new Scale.Fraction(
                1,
                "Typical length to display tangents at ending points");

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        final Constant.Boolean showTangents = new Constant.Boolean(
                false,
                "Should we show filament ending tangents?");

        //
        final Constant.Boolean showCombs = new Constant.Boolean(
                false,
                "Should we show staff lines combs?");

        // Constants for debugging
        // -----------------------
        final Constant.String horizontalVipSections = new Constant.String(
                "",
                "(Debug) Comma-separated list of VIP sections");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants
     * related to horizontal frames.
     */
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        /** Maximum vertical run length (to exclude too long vertical runs) */
        final int maxVerticalRunLength;

        /** Minimum run length for horizontal lag */
        final int minRunLength;

        /** Used for section junction policy */
        final double maxLengthRatio;

        /** Used for section junction policy for short sections */
        final double maxLengthRatioShort;

        /** Percentage of top filaments used to retrieve global slope */
        final double topRatioForSlope;

        /** Maximum sticker thickness */
        final int maxStickerThickness;

        /** Maximum sticker extension */
        final int maxStickerExtension;

        /** Maximum vertical gap between a sticker and the closest line side */
        final double maxStickerGap;

        /** Maximum weight for a thin sticker */
        final int maxThinStickerWeight;

        // Debug
        final List<Integer> vipSections;

        //~ Constructors -------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            // Special parameters
            maxVerticalRunLength = (int) Math.rint(
                    scale.getMaxFore() * constants.ledgerThickness.getValue());
            maxStickerThickness = (int) Math.rint(
                    scale.getMaxFore() * constants.stickerThickness.getValue());

            // Others
            minRunLength = scale.toPixels(constants.minRunLength);
            maxLengthRatio = constants.maxLengthRatio.getValue();
            maxLengthRatioShort = constants.maxLengthRatioShort.getValue();
            topRatioForSlope = constants.topRatioForSlope.getValue();
            maxStickerGap = scale.toPixelsDouble(constants.maxStickerGap);
            maxThinStickerWeight = scale.toPixels(
                    constants.maxThinStickerWeight);
            maxStickerExtension = (int) Math.ceil(
                    scale.toPixelsDouble(constants.maxStickerExtension));

            // VIPs
            vipSections = VipUtil.decodeIds(
                    constants.horizontalVipSections.getValue());

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }

            if (!vipSections.isEmpty()) {
                logger.info("Horizontal VIP sections: {}", vipSections);
            }
        }
    }
}
