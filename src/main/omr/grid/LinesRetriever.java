//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  L i n e s R e t r i e v e r                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLayer;
import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;

import omr.lag.BasicLag;
import omr.lag.JunctionRatioPolicy;
import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;
import omr.lag.SectionFactory;

import omr.math.LineUtil;
import omr.math.NaturalSpline;
import omr.math.Population;

import omr.run.Orientation;

import static omr.run.Orientation.*;

import omr.run.Run;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import omr.sheet.Staff;
import omr.sheet.StaffManager;
import omr.sheet.SystemInfo;
import omr.sheet.ui.RunsViewer;

import omr.ui.Colors;
import omr.ui.util.ItemRenderer;
import omr.ui.util.UIUtil;

import omr.util.HorizontalSide;

import static omr.util.HorizontalSide.*;

import omr.util.IntUtil;
import omr.util.Navigable;
import omr.util.Predicate;
import omr.util.StopWatch;

import ij.process.ByteProcessor;

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
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code LinesRetriever} retrieves the staff lines of a sheet.
 *
 * @author Hervé Bitteur
 */
public class LinesRetriever
        implements ItemRenderer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LinesRetriever.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    /** related sheet */
    @Navigable(false)
    private final Sheet sheet;

    /** Related scale */
    private final Scale scale;

    /** Related staff manager. */
    private final StaffManager staffManager;

    /** Scale-dependent constants for horizontal stuff */
    private final Parameters params;

    /** Lag of horizontal runs */
    private Lag hLag;

    /** Filaments factory */
    private FilamentsFactory factory;

    /** Long horizontal filaments found, non sorted */
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
    final BarsRetriever barsRetriever;

    /** Too-short horizontal runs */
    private RunTable shortHoriTable;

    /** Binary buffer. */
    private ByteProcessor binaryBuffer;

    //~ Constructors -------------------------------------------------------------------------------
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

        staffManager = sheet.getStaffManager();
        scale = sheet.getScale();
        params = new Parameters(scale);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // buildHorizontalLag //
    //--------------------//
    /**
     * Build the underlying horizontal lag, and first populate it with only the long
     * horizontal sections.
     * Short horizontal sections will be added later (via {@link #createShortSections()})
     *
     * @param wholeVertTable the provided table of all (vertical) runs
     * @return the table of long vertical runs (a side effect of building the long horizontal ones)
     */
    public RunTable buildHorizontalLag (RunTable wholeVertTable)
    {
        final RunsViewer runsViewer = (constants.displayRuns.isSet() && (Main.getGui() != null))
                ? new RunsViewer(sheet) : null;
        hLag = new BasicLag(Lags.HLAG, Orientation.HORIZONTAL);

        // Create filament factory
        factory = new FilamentsFactory(
                scale,
                sheet.getNest(),
                GlyphLayer.DEFAULT,
                Orientation.HORIZONTAL,
                LineFilament.class);
        factory.dump("LinesRetriever factory");

        // To record the purged vertical runs
        RunTable longVertTable = new RunTable(
                "long-vert",
                VERTICAL,
                sheet.getWidth(),
                sheet.getHeight());

        // Remove runs whose height is larger than line thickness
        RunTable shortVertTable = wholeVertTable.copy("short-vert").purge(
                new Predicate<Run>()
                {
                    @Override
                    public final boolean check (Run run)
                    {
                        return run.getLength() > params.maxVerticalRunLength;
                    }
                },
                longVertTable);

        if (runsViewer != null) {
            runsViewer.display(longVertTable);
            runsViewer.display(shortVertTable);
        }

        // Build table of long horizontal runs
        RunTableFactory runFactory = new RunTableFactory(HORIZONTAL);
        RunTable wholeHoriTable = runFactory.createTable(
                "whole-hori",
                shortVertTable.getBuffer());

        // To record the purged horizontal runs
        shortHoriTable = new RunTable(
                "short-hori",
                HORIZONTAL,
                sheet.getWidth(),
                sheet.getHeight());

        RunTable longHoriTable = wholeHoriTable.copy("long-hori").purge(
                new Predicate<Run>()
                {
                    @Override
                    public final boolean check (Run run)
                    {
                        return run.getLength() < params.minRunLength;
                    }
                },
                shortHoriTable);

        if (runsViewer != null) {
            runsViewer.display(shortHoriTable);
            runsViewer.display(longHoriTable.copy("long-hori-snapshot"));
        }

        // Populate the horizontal hLag with the long horizontal runs
        // (short horizontal runs will be added later via createShortSections())
        SectionFactory sectionsBuilder = new SectionFactory(hLag, new JunctionRatioPolicy());
        sectionsBuilder.createSections(longHoriTable, null, true);

        sheet.setLag(Lags.HLAG, hLag);

        setVipSections();

        return longVertTable;
    }

    //---------------//
    // completeLines //
    //---------------//
    /**
     * Complete the retrieved staff lines whenever possible with filaments and short
     * sections left over.
     * <p>
     * When this method is called, the precise staff abscissa endings are known (thanks to staff
     * projection and bar line handling).
     * Lines must be completed accordingly.
     * Ending points are determined by searching the best vertical fit for a staff pattern of 5 line
     * segments.
     * Then filaments and sections are added to the theoretical lines.
     * <p>
     * To decide on inclusion of filament or section, the line geometry must be rather precise.
     * But geometry will be impacted by inclusions.
     * Hence, line geometry must be recomputed on each major update.
     * Line geometry is computed by sampling on abscissa and retrieving ordinate barycenter of glyph
     * sections within each abscissa sample.
     * <p>
     * <b>Synopsis:</b>
     * <pre>
     *      + defineEndPoints()
     *      + includeDiscardedFilaments()
     *          + canIncludeFilament(fil1, fil2)
     *          + fil1.stealSections(fil2)
     *      + fillHoles()
     *      + includeSections()
     *          + canIncludeSection(fil, sct)
     *          + fil.addSection(sct)
     *      + polishCurvature()
     *      + fillHoles()
     * </pre>
     */
    public void completeLines ()
    {
        StopWatch watch = new StopWatch("completeLines");
        binaryBuffer = sheet.getPicture().getSource(Picture.SourceKey.BINARY);

        try {
            // Define the precise end points for every staff line
            watch.start("defineEndPoints");
            defineEndPoints();

            // Browse discarded filaments for possible inclusion
            watch.start("include discarded filaments");
            includeDiscardedFilaments();

            // Add intermediate points when needed
            watch.start("fillHoles");
            fillHoles();

            // Dispatch short sections into thick & thin ones
            final List<Section> thickSections = new ArrayList<Section>();
            final List<Section> thinSections = new ArrayList<Section>();
            watch.start("dispatchShortSections");
            dispatchShortSections(thickSections, thinSections);

            // First, consider thick sections
            watch.start("include " + thickSections.size() + " thick stickers");
            includeSections(thickSections, true);

            // Second, consider thin sections
            watch.start("include " + thinSections.size() + " thin stickers");
            includeSections(thinSections, true);

            // Polish staff lines (TODO: to be improved)
            watch.start("polishCurvatures");
            polishCurvatures();

            // Add intermediate points when needed
            watch.start("fillHoles");
            fillHoles();
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //---------------------//
    // createShortSections //
    //---------------------//
    /**
     * Build horizontal sections out of shortHoriTable runs
     *
     * @return the list of created sections
     */
    public List<Section> createShortSections ()
    {
        // Note the current section id
        sheet.setLongSectionMaxId(hLag.getLastVertexId());

        // Complete the horizontal hLag with the short sections
        // (it already contains all the other (long) horizontal sections)
        SectionFactory sectionsBuilder = new SectionFactory(
                hLag,
                new JunctionRatioPolicy(params.maxLengthRatioShort));
        List<Section> shortSections = sectionsBuilder.createSections(shortHoriTable, null, true);

        setVipSections();

        return shortSections;
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
            List<LineFilament> allFils = new ArrayList<LineFilament>(filaments);

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
                            new Line2D.Double(p.getX(), p.getY(), p.getX() - dx, p.getY() - (der * dx)));
                    p = filament.getStopPoint(HORIZONTAL);
                    der = filament.slopeAt(p.getX(), HORIZONTAL);
                    g.draw(
                            new Line2D.Double(p.getX(), p.getY(), p.getX() + dx, p.getY() + (der * dx)));
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
     * Organize the long and thin horizontal sections into filaments that will be good
     * candidates for staff lines.
     * <ol>
     * <li>First, retrieve long horizontal sections and merge them into filaments.</li>
     * <li>Second, detect series of filaments regularly spaced and aggregate them into clusters of
     * lines (as staff candidates). </li>
     * </ol>
     * <p>
     * <b>Synopsis:</b>
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

            for (Glyph fil : factory.retrieveFilaments(hLag.getSections())) {
                filaments.add((LineFilament) fil);
            }

            // Purge curved filaments
            purgeCurvedFilaments();

            // Compute global slope out of longest filaments
            watch.start("retrieveGlobalSlope");
            globalSlope = retrieveGlobalSlope();
            sheet.setSkew(new Skew(globalSlope, sheet));
            logger.info("{}Sheet slope: {}", sheet.getLogPrefix(), (float) globalSlope);

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

            if ((secondInterline != null) && !discardedFilaments.isEmpty()) {
                secondFilaments = discardedFilaments;
                Collections.sort(secondFilaments, Glyph.byId);
                logger.info(
                        "{}Searching clusters with secondInterline: {}",
                        sheet.getLogPrefix(),
                        secondInterline);
                secondClustersRetriever = new ClustersRetriever(
                        sheet,
                        secondFilaments,
                        secondInterline,
                        Colors.COMB_MINOR);
                watch.start("secondClustersRetriever");
                discardedFilaments = secondClustersRetriever.buildInfo();
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Discarded filaments: {}", Glyphs.toString(discardedFilaments));
            }

            // Convert clusters into staves
            watch.start("BuildStaves");
            buildStaves();
        } finally {
            if (constants.printWatch.isSet()) {
                watch.print();
            }
        }
    }

    //-------------//
    // buildStaves //
    //-------------//
    /**
     * Register line clusters as staves.
     * <p>
     * At this point, all clusters have been constructed and trimmed to the right number of lines
     * per cluster.
     * Each cluster can now give birth to a staff, with preliminary values, since we don't know yet
     * precisely the starting and ending abscissae of each staff.
     * This will be refined later, using staff projection to retrieve major bar lines as well as
     * staff side limits.
     */
    private void buildStaves ()
    {
        // Accumulate all clusters, and sort them by layout
        List<LineCluster> allClusters = new ArrayList<LineCluster>();
        allClusters.addAll(clustersRetriever.getClusters());

        if (secondClustersRetriever != null) {
            allClusters.addAll(secondClustersRetriever.getClusters());
        }

        Collections.sort(allClusters, clustersRetriever.byLayout);

        // Populate the staff manager
        int staffId = 0;
        staffManager.reset();

        for (LineCluster cluster : allClusters) {
            logger.debug(cluster.toString());

            List<FilamentLine> lines = new ArrayList<FilamentLine>(cluster.getLines());
            double left = Integer.MAX_VALUE;
            double right = Integer.MIN_VALUE;

            for (LineInfo line : lines) {
                left = Math.min(left, line.getEndPoint(LEFT).getX());
                right = Math.max(right, line.getEndPoint(RIGHT).getX());
            }

            Staff staff = new Staff(
                    ++staffId,
                    left,
                    right,
                    new Scale(cluster.getInterline(), scale.getMainFore()),
                    lines);
            staffManager.addStaff(staff);
        }

        // Flag short staves (side by side) if any
        staffManager.detectShortStaves();

        sheet.getBench().recordStaveCount(staffManager.getStaffCount());
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
                logger.info("{}SSS height:{} vs {}", vips, height, params.maxStickerThickness);
            }

            return false;
        }

        // Check entity center gap with theoretical line
        double yFil = filament.getPositionAt(center.x, HORIZONTAL);
        double dy = Math.abs(yFil - center.y);
        double gap = dy - (scale.getMainFore() / 2.0);

        if (gap > params.maxStickerGap) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info("{}GGG gap:{} vs {}", vips, (float) gap, (float) params.maxStickerGap);
            }

            return false;
        }

        // Check max extension from theoretical line
        double extension = Math.max(Math.abs(yFil - box.y), Math.abs((box.y + height) - yFil));

        if (extension > params.maxStickerExtension) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info(
                        "{}XXX ext:{} vs {}",
                        vips,
                        (float) extension,
                        params.maxStickerExtension);
            }

            return false;
        }

        // Check resulting thickness
        double thickness = 0;

        if (candidate instanceof Section) {
            thickness = Glyphs.getThicknessAt(center.x, HORIZONTAL, (Section) candidate, filament);
        } else if (candidate instanceof Glyph) {
            thickness = Glyphs.getThicknessAt(center.x, HORIZONTAL, (Glyph) candidate, filament);
        }

        if (thickness > params.maxStickerThickness) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info(
                        "{}RRR thickness:{} vs {}",
                        vips,
                        (float) thickness,
                        params.maxStickerExtension);
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

    //-----------------//
    // defineEndPoints //
    //-----------------//
    /**
     * Knowing precise abscissa endings for each staff, determine precise ending points
     * for each staff line.
     */
    private void defineEndPoints ()
    {
        for (Staff staff : staffManager.getStaves()) {
            double meanDy = staff.getMeanInterline();

            Map<HorizontalSide, List<Point2D>> endMap = new EnumMap<HorizontalSide, List<Point2D>>(
                    HorizontalSide.class);

            for (HorizontalSide side : HorizontalSide.values()) {
                endMap.put(side, retrieveEndPoints(staff, meanDy, side));
            }

            // Adjust left and right endings of each line in the staff
            for (int i = 0; i < staff.getLines().size(); i++) {
                FilamentLine line = staff.getLines().get(i);
                line.setEndingPoints(endMap.get(LEFT).get(i), endMap.get(RIGHT).get(i));
            }
        }
    }

    //-----------------------//
    // dispatchShortSections //
    //-----------------------//
    /**
     * Dispatch short horizontal sections into thick and thin collections.
     *
     * @param thickSections (output) thick sections
     * @param thinSections  (output) thin sections
     */
    private void dispatchShortSections (List<Section> thickSections,
                                        List<Section> thinSections)
    {
        final int maxLongId = sheet.getLongSectionMaxId();

        for (Section section : hLag.getSections()) {
            // Skip long sections
            if (section.getId() <= maxLongId) {
                continue;
            }

            if (section.getWeight() > params.maxThinStickerWeight) {
                thickSections.add(section);
            } else {
                thinSections.add(section);
            }
        }
    }

    //-----------//
    // fillHoles //
    //-----------//
    /**
     * Staff by staff, check the intermediate line points.
     */
    private void fillHoles ()
    {
        for (Staff staff : staffManager.getStaves()) {
            logger.debug("{}", staff);

            // Insert line intermediate points, if so needed
            List<LineFilament> fils = new ArrayList<LineFilament>();

            for (FilamentLine line : staff.getLines()) {
                fils.add(line.fil);
            }

            for (int pos = 0; pos < staff.getLines().size(); pos++) {
                FilamentLine line = staff.getLines().get(pos);
                line.fil.fillHoles(fils, pos);
            }
        }
    }

    //---------------//
    // getLineEnding //
    //---------------//
    /**
     * Report the precise point where a given line should end.
     * This is based on extrapolation only, and may provide wrong results if missing abscissa range
     * is too large.
     *
     * @param system the system to process
     * @param staff  containing staff
     * @param line   the line at hand
     * @param side   the desired ending
     * @return the computed ending point
     */
    @Deprecated
    private Point2D getLineEnding (SystemInfo system,
                                   Staff staff,
                                   LineInfo line,
                                   HorizontalSide side)
    {
        double slope = staff.getEndingSlope(side);
        Point2D linePt = line.getEndPoint(side);
        int staffX = staff.getAbscissa(side);
        double y = linePt.getY() + ((staffX - linePt.getX()) * slope);

        return new Point2D.Double(staffX, y);
    }

    //---------------------------//
    // includeDiscardedFilaments //
    //---------------------------//
    /**
     * Last attempt to include discarded filaments to retrieved staff lines.
     */
    private void includeDiscardedFilaments ()
    {
        // Sort these discarded filaments by top ordinate
        Collections.sort(discardedFilaments, Filament.topComparator);

        final int iMax = discardedFilaments.size() - 1;

        for (SystemInfo system : sheet.getSystems()) {
            // Systems may be side by side, so restart from top
            int iMin = 0;

            for (Staff staff : system.getStaves()) {
                for (FilamentLine line : staff.getLines()) {
                    LineFilament filament = line.fil;
                    Rectangle lineBox = filament.getBounds();
                    lineBox.grow(0, scale.getMainFore());

                    double minX = filament.getStartPoint(HORIZONTAL).getX();
                    double maxX = filament.getStopPoint(HORIZONTAL).getX();
                    int minY = lineBox.y;
                    int maxY = lineBox.y + lineBox.height;

                    // Browse discarded filaments
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

                    line.checkLine(); // Recompute line if needed
                }
            }
        }
    }

    //-----------------//
    // includeSections //
    //-----------------//
    /**
     * Include "sticker" sections into their related lines, when applicable
     *
     * @param sections       List of sections that are stickers candidates
     * @param updateGeometry should we update the line geometry with stickers
     *                       (this should be limited to large sections).
     */
    private void includeSections (List<Section> sections,
                                  boolean updateGeometry)
    {
        // Sections are sorted according to their top run (Y)
        Collections.sort(sections, Section.posComparator);

        final int iMax = sections.size() - 1;

        for (SystemInfo system : sheet.getSystems()) {
            // Because of possible side by side systems, we must restart from top
            int iMin = 0;

            for (Staff staff : system.getStaves()) {
                for (FilamentLine line : staff.getLines()) {
                    /*
                     * Inclusion on the fly would imply recomputation of filament at each section
                     * inclusion. So we need to retrieve all "stickers" for a given staff line, and
                     * perform a global inclusion at the end only.
                     */
                    LineFilament fil = line.fil;
                    Rectangle lineBox = fil.getBounds();
                    lineBox.grow(0, scale.getMainFore());

                    double minX = fil.getStartPoint(HORIZONTAL).getX();
                    double maxX = fil.getStopPoint(HORIZONTAL).getX();
                    int minY = lineBox.y;
                    int maxY = lineBox.y + lineBox.height;
                    List<Section> stickers = new ArrayList<Section>();

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
                            break; // Since sections are sorted on pos (Y)
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
                        if (updateGeometry) {
                            // This invalidates glyph cache, including extrema
                            fil.addSection(section);
                        } else {
                            section.setGlyph(fil);
                        }
                    }

                    line.checkLine(); // Recompute line if needed
                }
            }
        }
    }

    //------------------//
    // polishCurvatures //
    //------------------//
    private void polishCurvatures ()
    {
        for (Staff staff : staffManager.getStaves()) {
            for (FilamentLine line : staff.getLines()) {
                line.fil.polishCurvature();
            }
        }
    }

    //----------------------//
    // purgeCurvedFilaments //
    //----------------------//
    /**
     * Discard all filaments that exhibit a too strong curvature.
     */
    private void purgeCurvedFilaments ()
    {
        List<Filament> toRemove = new ArrayList<Filament>();

        for (LineFilament filament : filaments) {
            Filament fil = filament;
            Point2D start = fil.getStartPoint(HORIZONTAL);
            Point2D stop = fil.getStopPoint(HORIZONTAL);

            // Check if this filament is straight enough
            double xMid = (start.getX() + stop.getX()) / 2;
            NaturalSpline spline = (NaturalSpline) fil.getLine();
            double yMid = spline.yAtX(xMid);
            Point2D mid = new Point2D.Double(xMid, yMid);
            double rot = LineUtil.rotation(start, stop, mid);

            if (rot > params.maxFilamentRotation) {
                if (fil.isVip()) {
                    logger.info(
                            "VIP curved {} rotation:{} (vs {} radians)",
                            fil,
                            String.format("%.3f", rot),
                            params.maxFilamentRotation);
                }

                toRemove.add(fil);
            }
        }

        if (!toRemove.isEmpty()) {
            logger.info("Discarded curved line filaments: {}", toRemove.size());
            filaments.removeAll(toRemove);
        }
    }

    //-------------------//
    // retrieveEndPoints //
    //-------------------//
    /**
     * Retrieve the best end point for each line of the provided staff on desired side.
     * <p>
     * We know the precise ending abscissa of the staff, but not the precise lines ordinates.
     * If a line end abscissa is close enough to staff end abscissa, we can simply extrapolate
     * end ordinate using staff mean slope at end of concrete line.
     * Otherwise, we have to use a staff pattern and find its best vertical fit.
     *
     * @param staff the staff to process
     * @param side  left or right side
     * @return the sequence of end points, from top to bottom
     */
    private List<Point2D> retrieveEndPoints (Staff staff,
                                             double meanDy,
                                             HorizontalSide side)
    {
        final int staffX = staff.getAbscissa(side);
        final Point2D[] endings = new Point2D[staff.getLineCount()];
        final double slope = staff.getEndingSlope(side);
        final Population tops = new Population();
        int bestIndex = 0;
        double bestDx = Double.MAX_VALUE;
        boolean missing = false;

        // First, look for close ending lines
        for (int i = 0; i < endings.length; i++) {
            FilamentLine line = staff.getLines().get(i);
            Point2D linePt = line.getEndPoint(side);
            double dx = staffX - linePt.getX();
            double dxAbs = Math.abs(dx);

            if (Math.abs(dx) <= params.maxEndingDx) {
                double y = linePt.getY() + (dx * slope);
                endings[i] = new Point2D.Double(staffX, y);
                tops.includeValue(y - (i * meanDy));
            } else {
                missing = true;

                if (dxAbs < bestDx) {
                    bestDx = dxAbs;
                    bestIndex = i;
                }
            }
        }

        if (missing) {
            // Use a staff pattern to compute missing ordinates
            StaffPattern pattern = new StaffPattern(
                    staff.getLineCount(),
                    params.patternWidth,
                    scale.getMainFore(),
                    scale.getInterline());

            // Find the most probable upper left ordinate
            final double uly;

            if (tops.getCardinality() > 0) {
                uly = tops.getMeanValue();
            } else {
                // Extrapolate the line which ends closest to the staff end abscissa
                FilamentLine line = staff.getLines().get(bestIndex);
                Point2D linePt = line.getEndPoint(side);
                double dx = staffX - linePt.getX();
                uly = (linePt.getY() + (dx * slope)) - (bestIndex * meanDy);
            }

            final int patternX = (side == LEFT) ? staffX : (staffX - params.patternWidth);
            final int iterMax = 1 + (2 * ((params.patternJitter + 1) / 2));
            int dy = 0;
            int bestDy = 0;
            double bestRatio = 0;

            for (int iter = 1; iter <= iterMax; iter++) {
                Point2D ul = new Point2D.Double(patternX, uly + dy);
                double ratio = pattern.evaluate(ul, binaryBuffer);
                logger.debug("{} iter:{} dy:{} ratio:{}", side, iter, dy, ratio);

                if (ratio > bestRatio) {
                    bestRatio = ratio;
                    bestDy = dy;
                }

                if (dy == 0) {
                    dy = 1;
                } else {
                    dy += (Integer.signum(-dy) * iter);
                }
            }

            logger.debug("{} bestDy:{} bestRatio:{}", side, bestDy, bestRatio);

            // Fill the missing points
            for (int i = 0; i < endings.length; i++) {
                if (endings[i] == null) {
                    endings[i] = new Point2D.Double(staffX, uly + bestDy + (i * meanDy));
                }
            }
        }

        logger.debug("Staff#{} {} {}", staff.getId(), side, endings);

        return Arrays.asList(endings);
    }

    //---------------------//
    // retrieveGlobalSlope //
    //---------------------//
    private double retrieveGlobalSlope ()
    {
        // Use the top longest filaments to determine slope
        final double ratio = params.topRatioForSlope;
        final int topCount = Math.max(1, (int) Math.rint(filaments.size() * ratio));
        double slopes = 0;
        Collections.sort(filaments, Glyphs.byReverseLength(HORIZONTAL));

        for (int i = 0; i < topCount; i++) {
            Filament fil = filaments.get(i);
            Point2D start = fil.getStartPoint(HORIZONTAL);
            Point2D stop = fil.getStopPoint(HORIZONTAL);
            slopes += ((stop.getY() - start.getY()) / (stop.getX() - start.getX()));
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Ratio topRatioForSlope = new Constant.Ratio(
                0.1,
                "Percentage of top filaments used to retrieve global slope");

        final Constant.Double maxFilamentRotation = new Constant.Double(
                "radians",
                0.1,
                "Maximum central rotation for filaments");

        // Constants for building horizontal sections
        // ------------------------------------------
        final Constant.Ratio maxLengthRatio = new Constant.Ratio(
                1.5,
                "Maximum ratio in length for a run to be combined with an existing section");

        final Constant.Ratio maxLengthRatioShort = new Constant.Ratio(
                2.5, //3.0,
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

        final Scale.Fraction maxEndingDx = new Scale.Fraction(
                1.0,
                "Maximum abscissa delta between line end and staff end");

        final Scale.Fraction patternWidth = new Scale.Fraction(
                1.0,
                "Width of probe for staff pattern");

        final Scale.Fraction patternJitter = new Scale.Fraction(
                0.25,
                "Maximum ordinate jitter for staff pattern");

        // Constants for display
        // ---------------------
        Constant.Boolean displayRuns = new Constant.Boolean(
                false,
                "Should we display all images on runs?");

        final Constant.Boolean showHorizontalLines = new Constant.Boolean(
                true,
                "Should we show the horizontal grid lines?");

        final Scale.Fraction tangentLg = new Scale.Fraction(
                1,
                "Typical length to show tangents at ending points");

        final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        final Constant.Boolean showTangents = new Constant.Boolean(
                false,
                "Should we show filament ending tangents?");

        final Constant.Boolean showCombs = new Constant.Boolean(
                false,
                "Should we show staff lines combs?");

        // Constants for debugging
        // -----------------------
        final Constant.String horizontalVipSections = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP horizontal sections IDs");
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
        //~ Instance fields ------------------------------------------------------------------------

        /** Maximum vertical run length (to exclude too long vertical runs) */
        final int maxVerticalRunLength;

        /** Minimum run length for horizontal lag */
        final int minRunLength;

        /** Used for section junction policy for short sections */
        final double maxLengthRatioShort;

        /** Percentage of top filaments used to retrieve global slope */
        final double topRatioForSlope;

        /** Maximum rotation angle for filaments used to retrieve global slope */
        final double maxFilamentRotation;

        /** Maximum sticker thickness */
        final int maxStickerThickness;

        /** Maximum sticker extension */
        final int maxStickerExtension;

        /** Maximum vertical gap between a sticker and the closest line side */
        final double maxStickerGap;

        /** Maximum weight for a thin sticker */
        final int maxThinStickerWeight;

        /** Maximum abscissa delta between concrete line end and staff end */
        final int maxEndingDx;

        /** Width used for staff pattern */
        final int patternWidth;

        /** Maximum ordinate jitter for staff pattern */
        final int patternJitter;

        // Debug
        final List<Integer> vipSections;

        //~ Constructors ---------------------------------------------------------------------------
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
            maxLengthRatioShort = constants.maxLengthRatioShort.getValue();
            topRatioForSlope = constants.topRatioForSlope.getValue();
            maxFilamentRotation = constants.maxFilamentRotation.getValue();
            maxStickerGap = scale.toPixelsDouble(constants.maxStickerGap);
            maxThinStickerWeight = scale.toPixels(constants.maxThinStickerWeight);
            maxEndingDx = scale.toPixels(constants.maxEndingDx);
            patternWidth = scale.toPixels(constants.patternWidth);
            patternJitter = scale.toPixels(constants.patternJitter);
            maxStickerExtension = (int) Math.ceil(
                    scale.toPixelsDouble(constants.maxStickerExtension));

            // VIPs
            vipSections = IntUtil.parseInts(constants.horizontalVipSections.getValue());

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }

            if (!vipSections.isEmpty()) {
                logger.info("Horizontal VIP sections: {}", vipSections);
            }
        }
    }
}
