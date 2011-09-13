//----------------------------------------------------------------------------//
//                                                                            //
//                        L i n e s R e t r i e v e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.GlyphSectionsBuilder;
import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;

import omr.lag.JunctionRatioPolicy;

import omr.log.Logger;

import omr.run.Orientation;
import static omr.run.Orientation.*;
import omr.run.Run;
import omr.run.RunBoard;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;
import omr.run.RunsTableView;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import omr.sheet.ui.PixelBoard;

import omr.stick.StickSection;

import omr.ui.BoardsPane;
import omr.ui.Colors;
import omr.ui.view.RubberPanel;
import omr.ui.view.ScrollView;
import static omr.util.HorizontalSide.*;
import omr.util.Predicate;
import omr.util.StopWatch;

import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Class <code>LinesRetriever</code> retrieves the horizontal filaments
 * of the systems grid in a sheet.
 *
 * @author Hervé Bitteur
 */
public class LinesRetriever
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LinesRetriever.class);

    /** Stroke for drawing filaments curves */
    private static final Stroke splineStroke = new BasicStroke(
        (float) constants.splineThickness.getValue(),
        BasicStroke.CAP_ROUND,
        BasicStroke.JOIN_ROUND);

    //~ Instance fields --------------------------------------------------------

    /** related sheet */
    private final Sheet sheet;

    /** Related scale */
    private final Scale scale;

    /** Scale-dependent constants for horizontal stuff */
    private final Parameters params;

    /** Lag of horizontal runs */
    private GlyphLag hLag;

    /** Filaments factory */
    private FilamentsFactory factory;

    /** Long filaments found, non sorted */
    private final List<LineFilament> filaments = new ArrayList<LineFilament>();

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
    private RunsTable purgedHoriTable;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // LinesRetriever //
    //----------------//
    /**
     * Retrieve the frames of all staff lines
     *
     * @param sheet the sheet to process
     */
    public LinesRetriever (Sheet         sheet,
                           BarsRetriever barsRetriever)
    {
        this.sheet = sheet;
        this.barsRetriever = barsRetriever;

        scale = sheet.getScale();
        params = new Parameters(scale);
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // getLag //
    //--------//
    /**
     * Report the horizontal lag
     * @return horizontal lag
     */
    public GlyphLag getLag ()
    {
        return hLag;
    }

    //----------//
    // buildLag //
    //----------//
    /**
     * Build the underlying lag, out of the provided runs table
     * @param wholeVertTable the provided table of all (vertical) runs
     * @param showRuns true to create views on runs
     * @return the vertical runs too long to be part of any staff line
     */
    public RunsTable buildLag (RunsTable wholeVertTable,
                               boolean   showRuns)
    {
        hLag = new GlyphLag("hLag", StickSection.class, Orientation.HORIZONTAL);

        // Create filament factory
        try {
            factory = new FilamentsFactory(scale, hLag, LineFilament.class);

            // Debug VIP sticks
            factory.setVipGlyphs(params.vipSticks);
        } catch (Exception ex) {
            logger.warning("Cannot create lines filament factory", ex);
        }

        // To record the purged runs
        RunsTable purgedVertTable = new RunsTable(
            "purged-vert",
            VERTICAL,
            new Dimension(sheet.getWidth(), sheet.getHeight()));

        // Remove runs whose height is somewhat greater than line thickness
        RunsTable shortVertTable = wholeVertTable.clone("short-vert")
                                                 .purge(
            new Predicate<Run>() {
                    public final boolean check (Run run)
                    {
                        return run.getLength() > params.maxVerticalRunLength;
                    }
                },
            purgedVertTable);

        if (showRuns) {
            // Add a view on purged table (debug)
            addRunsTab(purgedVertTable);

            // Add a view on runs table (debug)
            addRunsTab(shortVertTable);
        }

        // Build table of long horizontal runs
        RunsTable wholeHoriTable = new RunsTableFactory(
            HORIZONTAL,
            shortVertTable.getBuffer(),
            sheet.getPicture().getMaxForeground(),
            0).createTable("whole-hori");

        // To record the purged horizontal runs
        purgedHoriTable = new RunsTable(
            "purged-hori",
            HORIZONTAL,
            new Dimension(sheet.getWidth(), sheet.getHeight()));

        RunsTable longHoriTable = wholeHoriTable.clone("long-hori")
                                                .purge(
            new Predicate<Run>() {
                    public final boolean check (Run run)
                    {
                        return run.getLength() < params.minRunLength;
                    }
                },
            purgedHoriTable);

        if (showRuns) {
            // Add a view on purged table (debug)
            addRunsTab(purgedHoriTable);

            // Add a view on runs table (debug)
            addRunsTab(longHoriTable);
        }

        // Build the horizontal hLag
        GlyphSectionsBuilder sectionsBuilder = new GlyphSectionsBuilder(
            hLag,
            new JunctionRatioPolicy(params.maxLengthRatio));
        sectionsBuilder.createSections(longHoriTable);

        setVipSections();

        return purgedVertTable;
    }

    //---------------//
    // completeLines //
    //---------------//
    /**
     * Complete the retrieved staff lines whenever possible with filaments and
     * short sections left over.
     */
    public List<GlyphSection> completeLines ()
    {
        StopWatch watch = new StopWatch("completeLines");

        try {
            // Browse discarded filaments for possible inclusion
            watch.start("include discarded filaments");
            includeDiscardedFilaments();

            // Build sections out of purgedHoriTable (too short horizontal runs)
            watch.start("create shortSections");

            List<GlyphSection> shortSections = createShortSections();

            // Dispatch sections into thick & thin ones
            watch.start(
                "dispatching " + shortSections.size() + " thick / thin");

            List<GlyphSection> thickSections = new ArrayList<GlyphSection>();
            List<GlyphSection> thinSections = new ArrayList<GlyphSection>();

            for (GlyphSection section : shortSections) {
                if (section.getWeight() > params.maxThinStickerWeight) {
                    thickSections.add(section);
                } else {
                    thinSections.add(section);
                }
            }

            // First, consider thick sections and update geometry
            watch.start("include " + thickSections.size() + " thick stickers");
            includeStickers(thickSections, true);

            // Second, consider thin sections w/o updating the geometry
            watch.start("include " + thinSections.size() + " thin stickers");
            includeStickers(thinSections, false);

            return shortSections;
        } finally {
            if (constants.printWatch.getValue()) {
                watch.print();
            }
        }
    }

    //---------------//
    // retrieveLines //
    //---------------//
    /**
     * Organize the long and thin horizontal sections into filaments (glyphs)
     * that will be good candidates for staff lines.
     * <ol>
     * <li>First, retrieve long horizontal sections and merge them into
     * filaments.</li>
     * <li>Second, detect series of filaments regularly spaced and aggregate
     * them into clusters of lines (as staff candidates). </li>
     * </ol>
     */
    public void retrieveLines ()
    {
        StopWatch watch = new StopWatch("retrieveLines");

        try {
            // Retrieve filaments out of merged long sections
            watch.start("retrieveFilaments");

            for (Filament fil : factory.retrieveFilaments(
                hLag.getSections(),
                true)) {
                filaments.add((LineFilament) fil);
            }

            // Compute global slope out of longest filaments
            watch.start("retrieveGlobalSlope");
            globalSlope = retrieveGlobalSlope();
            sheet.setSkew(new Skew(globalSlope, sheet));
            logger.info(
                sheet.getLogPrefix() + "Global slope: " + (float) globalSlope);

            // Retrieve regular patterns of filaments and pack them into clusters
            clustersRetriever = new ClustersRetriever(
                sheet,
                filaments,
                scale.interline(),
                Colors.COMB);
            watch.start("clustersRetriever");

            discardedFilaments = clustersRetriever.buildInfo();

            // Check for a second interline
            Integer secondInterline = scale.secondInterline();

            if (secondInterline != null) {
                secondFilaments = discardedFilaments;
                Collections.sort(secondFilaments, Glyph.idComparator);
                logger.info(
                    sheet.getLogPrefix() +
                    "Searching clusters with secondInterline: " +
                    secondInterline);
                secondClustersRetriever = new ClustersRetriever(
                    sheet,
                    secondFilaments,
                    secondInterline,
                    Colors.COMB_MINOR);
                watch.start("secondClustersRetriever");
                discardedFilaments = secondClustersRetriever.buildInfo();
            }

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Discarded filaments: " +
                    Glyphs.toString(discardedFilaments));
            }

            // Convert clusters into staves
            watch.start("BuildStaves");
            buildStaves();
        } finally {
            if (constants.printWatch.getValue()) {
                watch.print();
            }
        }
    }

    //--------------------//
    // getStafflineGlyphs //
    //--------------------//
    Iterable<Glyph> getStafflineGlyphs ()
    {
        List<Glyph> glyphs = new ArrayList<Glyph>();

        if (clustersRetriever != null) {
            glyphs.addAll(clustersRetriever.getStafflineGlyphs());

            if (secondClustersRetriever != null) {
                glyphs.addAll(secondClustersRetriever.getStafflineGlyphs());
            }
        }

        return glyphs;
    }

    //------------//
    // addRunsTab //
    //------------//
    /**
     * Add a view tab in sheet assembly for the provided runs table
     * @param table the runs table to display
     */
    void addRunsTab (RunsTable table)
    {
        if (Main.getGui() == null) {
            return;
        }

        RubberPanel view = new MyRunsTableView(table);
        view.setName(table.getName());
        view.setPreferredSize(table.getDimension());

        final String unit = sheet.getId() + ":" + table.getName();

        BoardsPane   boards = new BoardsPane(
            new PixelBoard(unit, sheet),
            new RunBoard(unit, table.getSelectionService(), true));

        sheet.getAssembly()
             .addViewTab(table.getName(), new ScrollView(view), boards);
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render the filaments, their ending tangents, their combs
     * @param g graphics context
     * @param showTangents true to display tangents at filament ends
     * @param showCombs true to display cluster combs
     */
    void renderItems (Graphics2D g,
                      boolean    showTangents,
                      boolean    showCombs)
    {
        List<LineFilament> allFils = new ArrayList<LineFilament>(filaments);

        if (secondFilaments != null) {
            allFils.addAll(secondFilaments);
        }

        // Draw filaments
        g.setColor(Colors.MUSIC);

        Stroke oldStroke = g.getStroke();
        g.setStroke(splineStroke);

        for (Filament filament : allFils) {
            filament.renderLine(g);
        }

        // Draw tangent at each ending point?
        if (showTangents) {
            g.setColor(Colors.TANGENT);

            double dx = sheet.getScale()
                             .toPixels(constants.tangentLg);

            for (Filament filament : allFils) {
                Point2D p = filament.getStartPoint();
                double  der = filament.slopeAt(p.getX());
                g.draw(
                    new Line2D.Double(
                        p.getX(),
                        p.getY(),
                        p.getX() - dx,
                        p.getY() - (der * dx)));
                p = filament.getStopPoint();
                der = filament.slopeAt(p.getX());
                g.draw(
                    new Line2D.Double(
                        p.getX(),
                        p.getY(),
                        p.getX() + dx,
                        p.getY() + (der * dx)));
            }
        }

        g.setStroke(oldStroke);

        // Combs stuff?
        if (showCombs) {
            if (clustersRetriever != null) {
                clustersRetriever.renderItems(g);
            }

            if (secondClustersRetriever != null) {
                secondClustersRetriever.renderItems(g);
            }
        }
    }

    //----------------//
    // setVipSections //
    //----------------//
    private void setVipSections ()
    {
        // Debug sections VIPs
        for (int id : params.vipSections) {
            GlyphSection sect = hLag.getVertexById(id);

            if (sect != null) {
                sect.setVip();
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
        List<LineCluster> allClusters = new ArrayList<LineCluster>();
        allClusters.addAll(clustersRetriever.getClusters());

        if (secondClustersRetriever != null) {
            allClusters.addAll(secondClustersRetriever.getClusters());
        }

        Collections.sort(allClusters, clustersRetriever.ordinateComparator);

        // Populate the staff manager
        StaffManager staffManager = sheet.getStaffManager();
        int          staffId = 0;
        staffManager.reset();

        for (LineCluster cluster : allClusters) {
            if (logger.isFineEnabled()) {
                logger.fine(cluster.toString());
            }

            List<LineInfo> lines = new ArrayList<LineInfo>(cluster.getLines());
            double         left = Integer.MAX_VALUE;
            double         right = Integer.MIN_VALUE;

            for (LineInfo line : lines) {
                left = Math.min(left, line.getEndPoint(LEFT).getX());
                right = Math.max(right, line.getEndPoint(RIGHT).getX());
            }

            StaffInfo staff = new StaffInfo(
                ++staffId,
                left,
                right,
                new Scale(cluster.getInterline(), scale.mainFore()),
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
     * Check whether the provided staff linbe filament could include the
     * candidate filament
     * @param filament the staff line filament
     * @param fil the candidate filament
     * @return true if OK
     */
    private boolean canInclude (LineFilament filament,
                                Filament     fil)
    {
        // For VIP debugging
        final boolean isVip = fil.isVip();
        String        vips = null;

        if (isVip) {
            vips = "Fil#" + fil.getId() + ": "; // BP here!
        }

        // Check fil thickness
        PixelRectangle box = fil.getContourBox();
        int            height = box.height;

        if (height > params.maxStickerThickness) {
            if (logger.isFineEnabled() || isVip) {
                logger.info(
                    vips + "TTT height:" + height + " vs " +
                    params.maxStickerThickness);
            }

            return false;
        }

        // Check fil centroid gap with theoretical line
        PixelPoint center = fil.getCentroid();
        double     yFil = filament.getPositionAt(center.x);
        double     dy = Math.abs(yFil - center.y);
        double     gap = ((2 * dy) - scale.mainFore()) / 2;

        if (gap > params.maxStickerGap) {
            if (logger.isFineEnabled() || isVip) {
                logger.info(
                    vips + "GGG gap:" + (float) gap + " vs " +
                    (float) params.maxStickerGap);
            }

            return false;
        }

        // Check max extension from theoretical line
        double extension = Math.max(
            Math.abs(yFil - box.y),
            Math.abs((box.y + height) - yFil));

        if (extension > params.maxStickerExtension) {
            if (logger.isFineEnabled() || isVip) {
                logger.info(
                    vips + "XXX ext:" + (float) extension + " vs " +
                    params.maxStickerExtension);
            }

            return false;
        }

        if (logger.isFineEnabled() || isVip) {
            logger.info(vips + "---");
        }

        return true;
    }

    //--------------//
    // checkSticker //
    //--------------//
    /**
     * Check if the staff line filament can accept the provided sticker section
     * @param fil the staff line filament
     * @param section the candidate sticker
     * @return true if OK, false otherwise
     */
    private boolean checkSticker (LineFilament fil,
                                  GlyphSection section)
    {
        // For VIP debugging
        final boolean isVip = section.isVip();
        String        vips = null;

        if (isVip) {
            vips = "Sct#" + section.getId() + ": "; // BP here!
        }

        // Check section thickness
        PixelRectangle box = section.getContourBox();
        int            height = box.height;

        if (height > params.maxStickerThickness) {
            if (logger.isFineEnabled() || isVip) {
                logger.info(
                    vips + "TTT height:" + height + " vs " +
                    params.maxStickerThickness);
            }

            return false;
        }

        // Check section centroid gap with theoretical line
        PixelPoint center = section.getCentroid();
        double     yFil = fil.getPositionAt(center.x);
        double     dy = Math.abs(yFil - center.y);
        double     gap = ((2 * dy) - scale.mainFore()) / 2;

        if (gap > params.maxStickerGap) {
            if (logger.isFineEnabled() || isVip) {
                logger.info(
                    vips + "GGG gap:" + (float) gap + " vs " +
                    (float) params.maxStickerGap);
            }

            return false;
        }

        // Check max extension from theoretical line
        double extension = Math.max(
            Math.abs(yFil - box.y),
            Math.abs((box.y + height) - yFil));

        if (extension > params.maxStickerExtension) {
            if (logger.isFineEnabled() || isVip) {
                logger.info(
                    vips + "XXX ext:" + (float) extension + " vs " +
                    params.maxStickerExtension);
            }

            return false;
        }

        if (logger.isFineEnabled() || isVip) {
            logger.info(vips + "---");
        }

        return true;
    }

    //---------------------//
    // createShortSections //
    //---------------------//
    /**
     * Build horizontal sections out of purgedHoriTable runs
     * @return the list of created sections
     */
    private List<GlyphSection> createShortSections ()
    {
        // Augment the horizontal hLag
        GlyphSectionsBuilder sectionsBuilder = new GlyphSectionsBuilder(
            hLag,
            new JunctionRatioPolicy(params.maxLengthRatioShort));
        List<GlyphSection>   shortSections = sectionsBuilder.createSections(
            purgedHoriTable);

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

        for (SystemFrame system : sheet.getSystemManager()
                                       .getSystems()) {
            for (StaffInfo staff : system.getStaves()) {
                for (LineInfo l : staff.getLines()) {
                    FilamentLine   line = (FilamentLine) l;
                    LineFilament   filament = line.fil;
                    PixelRectangle lineBox = filament.getContourBox();
                    lineBox.grow(0, scale.mainFore());

                    double         minX = filament.getStartPoint()
                                                  .getX();
                    double         maxX = filament.getStopPoint()
                                                  .getX();
                    int            minY = lineBox.y;
                    int            maxY = lineBox.y + lineBox.height;
                    List<Filament> added = new ArrayList<Filament>();

                    for (int i = iMin; i <= iMax; i++) {
                        Filament fil = discardedFilaments.get(i);

                        if (fil.getPartOf() != null) {
                            continue;
                        }

                        int firstPos = fil.getContourBox().y;

                        if (firstPos < minY) {
                            iMin = i;

                            continue;
                        }

                        if (firstPos > maxY) {
                            break;
                        }

                        PixelPoint center = fil.getCentroid();

                        if ((center.x >= minX) && (center.x <= maxX)) {
                            if (canInclude(filament, fil)) {
                                filament.include(fil);
                            }
                        }
                    }
                }
            }

            barsRetriever.adjustStaffLines(system);
        }
    }

    //-----------------//
    // includeStickers //
    //-----------------//
    /**
     * Include "sticker" sections into their related lines, when applicable
     * @param sections List of sections that are stickers candidates
     * @param update should we update the line geometry with stickers (this
     * should be limited to large sections).
     */
    private void includeStickers (List<GlyphSection> sections,
                                  boolean            update)
    {
        // Sections are sorted according to their top run (Y first and X second)
        int iMin = 0;
        int iMax = sections.size() - 1;

        // Inclusion on the fly would imply recomputation of filament at each
        // section inclusion. So we need to retrieve all "stickers" for a given
        // staff line, and perform a global inclusion at the end only.
        for (SystemFrame system : sheet.getSystemManager()
                                       .getSystems()) {
            for (StaffInfo staff : system.getStaves()) {
                for (LineInfo l : staff.getLines()) {
                    FilamentLine   line = (FilamentLine) l;
                    LineFilament   fil = line.fil;
                    PixelRectangle lineBox = fil.getContourBox();
                    lineBox.grow(0, scale.mainFore());

                    double             minX = fil.getStartPoint()
                                                 .getX();
                    double             maxX = fil.getStopPoint()
                                                 .getX();
                    int                minY = lineBox.y;
                    int                maxY = lineBox.y + lineBox.height;
                    List<GlyphSection> stickers = new ArrayList<GlyphSection>();

                    for (int i = iMin; i <= iMax; i++) {
                        GlyphSection section = sections.get(i);

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

                        PixelPoint center = section.getCentroid();

                        if ((center.x >= minX) && (center.x <= maxX)) {
                            if (checkSticker(fil, section)) {
                                stickers.add(section);
                            }
                        }
                    }

                    // Actually include the retrieved stickers?
                    for (GlyphSection section : stickers) {
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
        final int    topCount = Math.max(
            1,
            (int) Math.rint(filaments.size() * ratio));
        double       slopes = 0;
        Collections.sort(filaments, LineFilament.reverseLengthComparator);

        for (int i = 0; i < topCount; i++) {
            Filament fil = filaments.get(i);
            Point2D  start = fil.getStartPoint();
            Point2D  stop = fil.getStopPoint();
            slopes += ((stop.getY() - start.getY()) / (stop.getX() -
                                                      start.getX()));
        }

        return slopes / topCount;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio     maxLengthRatio = new Constant.Ratio(
            1.5,
            "Maximum ratio in length for a run to be combined with an existing section");
        Constant.Ratio     maxLengthRatioShort = new Constant.Ratio(
            3.0,
            "Maximum ratio in length for a short run to be combined with an existing section");

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        Scale.LineFraction maxVerticalRunLength = new Scale.LineFraction(
            1.75, // 2.0
            "Maximum vertical run length WRT mean line height");
        Scale.LineFraction maxStickerGap = new Scale.LineFraction(
            0.5,
            "Maximum vertical gap between sticker and closest line side");
        Scale.LineFraction maxStickerExtension = new Scale.LineFraction(
            1.2,
            "Maximum vertical sticker extension from line");
        Scale.LineFraction maxStickerThickness = new Scale.LineFraction(
            1.4,
            "Maximum sticker thickness");
        Scale.AreaFraction maxThinStickerWeight = new Scale.AreaFraction(
            0.1,
            "Maximum weight for a thin sticker");

        // Constants specified WRT mean interline
        // --------------------------------------
        Scale.Fraction   minRunLength = new Scale.Fraction(
            1.0,
            "Minimum length for a horizontal run to be considered");
        Constant.Ratio   topRatioForSlope = new Constant.Ratio(
            0.1,
            "Percentage of top filaments used to retrieve global slope");

        // Constants for display
        //
        Constant.Boolean showLinesOnRuns = new Constant.Boolean(
            false,
            "Should we display lines on top of runs?");
        Constant.Double  splineThickness = new Constant.Double(
            "thickness",
            0.5,
            "Stroke thickness to draw filaments curves");
        Scale.Fraction   tangentLg = new Scale.Fraction(
            1,
            "Typical length to display tangents at ending points");
        Constant.Boolean printWatch = new Constant.Boolean(
            false,
            "Should we print out the stop watch?");

        // Constants for debugging
        //
        Constant.String horizontalVipSections = new Constant.String(
            "",
            "(Debug) Comma-separated list of VIP sections");
        Constant.String horizontalVipSticks = new Constant.String(
            "",
            "(Debug) Comma-separated list of VIP sticks");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all constants related to horizontal frames
     */
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        /** Minimum run length for horizontal lag */
        final int minRunLength;

        /** Maximum vertical run length (to exclude too long vertical runs) */
        final int maxVerticalRunLength;

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
        final List<Integer> vipSticks;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            minRunLength = scale.toPixels(constants.minRunLength);
            maxVerticalRunLength = scale.toPixels(
                constants.maxVerticalRunLength);
            maxLengthRatio = constants.maxLengthRatio.getValue();
            maxLengthRatioShort = constants.maxLengthRatioShort.getValue();
            topRatioForSlope = constants.topRatioForSlope.getValue();
            maxStickerGap = scale.toPixelsDouble(constants.maxStickerGap);
            maxStickerExtension = scale.toPixels(constants.maxStickerExtension);
            maxStickerThickness = scale.toPixels(constants.maxStickerThickness);
            maxThinStickerWeight = scale.toPixels(
                constants.maxThinStickerWeight);

            // VIPs
            vipSections = decode(constants.horizontalVipSections.getValue());
            vipSticks = decode(constants.horizontalVipSticks.getValue());

            if (logger.isFineEnabled()) {
                Main.dumping.dump(this);
            }

            if (!vipSections.isEmpty()) {
                logger.info("Horizontal VIP sections: " + vipSections);
            }

            if (!vipSticks.isEmpty()) {
                logger.info("Horizontal VIP sticks: " + vipSticks);
            }
        }

        //~ Methods ------------------------------------------------------------

        private List<Integer> decode (String str)
        {
            List<Integer>   ids = new ArrayList<Integer>();

            // Retrieve the list of ids
            StringTokenizer st = new StringTokenizer(str, ",");

            while (st.hasMoreTokens()) {
                try {
                    ids.add(Integer.decode(st.nextToken().trim()));
                } catch (Exception ex) {
                    logger.warning("Illegal id", ex);
                }
            }

            return ids;
        }
    }

    //-----------------//
    // MyRunsTableView //
    //-----------------//
    /**
     * A specific runs view, which displays retrieved lines on top of the runs
     */
    private class MyRunsTableView
        extends RunsTableView
    {
        //~ Constructors -------------------------------------------------------

        public MyRunsTableView (RunsTable table)
        {
            super(table, sheet.getSelectionService());
        }

        //~ Methods ------------------------------------------------------------

        @Override
        protected void renderItems (Graphics2D g)
        {
            if (constants.showLinesOnRuns.getValue()) {
                LinesRetriever.this.renderItems(g, false, false);
            }
        }
    }
}
