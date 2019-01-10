//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  L i n e s R e t r i e v e r                                   //
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
package org.audiveris.omr.sheet.grid;

import ij.process.ByteProcessor;

import org.audiveris.omr.OMR;
import static org.audiveris.omr.WellKnowns.LINE_SEPARATOR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.dynamic.Compounds;
import org.audiveris.omr.glyph.dynamic.CurvedFilament;
import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.glyph.dynamic.FilamentFactory;
import org.audiveris.omr.lag.JunctionRatioPolicy;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.SectionFactory;
import org.audiveris.omr.lag.SectionTally;
import org.audiveris.omr.lag.Sections;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.NaturalSpline;
import org.audiveris.omr.math.Population;
import org.audiveris.omr.run.Orientation;
import static org.audiveris.omr.run.Orientation.*;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Skew;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffManager;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.ui.RunsViewer;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.util.ItemRenderer;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.Dumping;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.Predicate;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.TOP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class {@code LinesRetriever} retrieves the staff lines of a sheet.
 *
 * @author Hervé Bitteur
 */
public class LinesRetriever
        implements ItemRenderer
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(LinesRetriever.class);

    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Related scale. */
    private final Scale scale;

    /** Related staff manager. */
    private final StaffManager staffManager;

    /** Scale-dependent constants for horizontal stuff. */
    private final Parameters params;

    /** Lag of horizontal runs. */
    private Lag hLag;

    /** Long horizontal filaments found, non sorted. */
    private List<StaffFilament> filaments;

    /** Second collection of filaments. (for small staves) */
    private List<StaffFilament> secondFilaments;

    /** Sloped filaments. */
    private List<StaffFilament> slopedFilaments;

    /** Discarded filaments. */
    private List<StaffFilament> discardedFilaments;

    /** Global slope of the sheet. */
    private double globalSlope;

    /** Companion in charge of clusters of main interline. */
    private ClustersRetriever clustersRetriever;

    /** Companion in charge of clusters of second interline, if any. */
    private ClustersRetriever smallClustersRetriever;

    /** Too-short horizontal runs. */
    private RunTable shortHoriTable;

    /** Binary buffer. */
    private ByteProcessor binaryBuffer;

    /** Companion in charge of bar lines. */
    final BarsRetriever barsRetriever;

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

    //--------------------//
    // buildHorizontalLag //
    //--------------------//
    /**
     * Build the underlying horizontal lag, and first populate it with only the long
     * horizontal sections.
     * Short horizontal sections will be added later (via {@link #createShortSections()})
     *
     * @return the table of long vertical runs (a side effect of building the long horizontal ones)
     */
    public RunTable buildHorizontalLag ()
    {
        final RunsViewer runsViewer = (constants.displayRuns.isSet() && (OMR.gui != null))
                ? new RunsViewer(sheet) : null;

        RunTable sourceTable = sheet.getPicture().getTable(Picture.TableKey.BINARY);

        // Filter runs whose height is larger than line thickness
        RunTable longVertTable = new RunTable(VERTICAL, sheet.getWidth(), sheet.getHeight());
        RunTable horiTable = sheet.getLagManager().filterRuns(sourceTable, longVertTable);

        if (runsViewer != null) {
            runsViewer.display("long-vert", longVertTable);
        }

        // Split horizontal runs into short & long tables
        shortHoriTable = new RunTable(HORIZONTAL, sheet.getWidth(), sheet.getHeight());

        RunTable longHoriTable = horiTable.purge(new Predicate<Run>()
        {
            @Override
            public final boolean check (Run run)
            {
                return run.getLength() < params.minRunLength;
            }
        }, shortHoriTable);

        if (runsViewer != null) {
            runsViewer.display("short-hori", shortHoriTable);
            runsViewer.display("long-hori-snapshot", longHoriTable.copy());
        }

        // Populate the horizontal hLag with the long horizontal runs
        // (short horizontal runs will be added later via createShortSections())
        hLag = sheet.getLagManager().buildHorizontalLag(longHoriTable, null);

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
     * projection and barline handling).
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
     * <br>
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
     *      + includeStickers()
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

            // Browse sloped filaments and discarded filaments for possible inclusion
            watch.start("include discarded filaments");
            includeDiscardedFilaments();

            // Add intermediate points where needed (1)
            watch.start("fillHoles");
            fillHoles();

            // Dispatch short sections into thick & thin ones
            final List<Section> thickSections = new ArrayList<>();
            final List<Section> thinSections = new ArrayList<>();
            watch.start("dispatchShortSections");
            dispatchShortSections(thickSections, thinSections);

            // First, consider thick sections
            watch.start("include " + thickSections.size() + " thick stickers");
            includeSections(thickSections);

            // Second, consider thin sections
            watch.start("include " + thinSections.size() + " thin stickers");
            includeSections(thinSections);

            // Polish staff lines (TODO: to be improved)
            watch.start("polishCurvatures");
            polishCurvatures();

            // Add intermediate points where needed (2)
            watch.start("fillHoles");
            fillHoles();

            // Include isolated horizontal sticker sections
            watch.start("includeStickers");
            includeStickers(); // This may delete intermediate points

            // Add intermediate points where needed (3)
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
        sheet.getLagManager().setLongSectionMaxId(hLag.getLastId());

        // Complete the horizontal hLag with the short sections
        // (it already contains all the other (long) horizontal sections)
        SectionFactory sectionsFactory = new SectionFactory(hLag, JunctionRatioPolicy.DEFAULT);
        List<Section> shortSections = sectionsFactory.createSections(shortHoriTable, null, true);

        sheet.getLagManager().setVipSections(HORIZONTAL);

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

        // Combs stuff?
        if (constants.showCombs.isSet()) {
            if (clustersRetriever != null) {
                clustersRetriever.renderItems(g);
            }

            if (smallClustersRetriever != null) {
                smallClustersRetriever.renderItems(g);
            }
        }

        // Filament lines?
        if (constants.showHorizontalLines.isSet() && (filaments != null)) {
            List<StaffFilament> allFils = new ArrayList<>(filaments);

            if (secondFilaments != null) {
                allFils.addAll(secondFilaments);
            }

            final boolean showPoints = Staff.showDefiningPoints();
            final double pointWidth = scale.toPixelsDouble(Staff.getDefiningPointSize());
            g.setColor(Colors.ENTITY_MINOR);

            for (Filament filament : allFils) {
                filament.renderLine(g, showPoints, pointWidth);
            }

            // Draw tangent at each ending point?
            if (constants.showTangents.isSet()) {
                g.setColor(Colors.TANGENT);

                double dx = sheet.getScale().toPixels(constants.tangentLg);

                for (Filament filament : allFils) {
                    Point2D p = filament.getStartPoint();
                    double der = filament.getSlopeAt(p.getX(), HORIZONTAL);
                    g.draw(
                            new Line2D.Double(
                                    p.getX(),
                                    p.getY(),
                                    p.getX() - dx,
                                    p.getY() - (der * dx)));
                    p = filament.getStopPoint();
                    der = filament.getSlopeAt(p.getX(), HORIZONTAL);
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
     * Organize the long and thin horizontal sections into filaments that will be good
     * candidates for staff lines.
     * <ol>
     * <li>First, retrieve long horizontal sections and merge them into filaments.</li>
     * <li>Second, detect series of filaments regularly spaced vertically and aggregate them into
     * clusters of lines (as staff candidates).</li>
     * </ol>
     * <p>
     * <b>Synopsis:</b>
     * <br>
     * <pre>
     *      + filamentFactory.retrieveFilaments()
     *      + retrieveGlobalSlope()
     *      + clustersRetriever.buildInfo()
     *      + secondClustersRetriever.buildInfo()
     *      + buildStaves()
     * </pre>
     *
     * @throws StepException if processing failed at this step
     */
    public void retrieveLines ()
            throws StepException
    {
        StopWatch watch = new StopWatch("retrieveLines");

        try {
            // Retrieve filaments out of merged long sections
            watch.start("retrieveFilaments");

            // Create initial filaments
            FilamentFactory<StaffFilament> factory = new FilamentFactory<>(
                    scale,
                    sheet.getFilamentIndex(),
                    Orientation.HORIZONTAL,
                    StaffFilament.class);
            factory.dump("LinesRetriever factory");
            filaments = factory.retrieveFilaments(hLag.getEntities());

            // Purge curved filaments
            purgeCurvedFilaments();

            // Compute global slope out of longest filaments
            watch.start("retrieveGlobalSlope");
            globalSlope = retrieveGlobalSlope();
            sheet.setSkew(new Skew(globalSlope, sheet));
            logger.info("Global slope: {}", String.format("%.5f", globalSlope));

            // Purge sloped filaments
            slopedFilaments = purgeSlopedFilaments();

            // Retrieve regular patterns of filaments and pack them into clusters
            clustersRetriever = new ClustersRetriever(
                    sheet,
                    filaments,
                    scale.getInterlineScale(),
                    Colors.COMB);
            watch.start("clustersRetriever");

            Integer smallInterline = scale.getSmallInterline();
            discardedFilaments = clustersRetriever.buildInfo(smallInterline != null);

            // Check for a small interline
            if ((smallInterline != null) && !discardedFilaments.isEmpty()) {
                secondFilaments = discardedFilaments;
                Collections.sort(secondFilaments, Entities.byId);
                logger.info("Searching clusters with smallInterline: {}", smallInterline);
                smallClustersRetriever = new ClustersRetriever(
                        sheet,
                        secondFilaments,
                        scale.getSmallInterlineScale(),
                        Colors.COMB_MINOR);
                watch.start("smallClustersRetriever");
                discardedFilaments = smallClustersRetriever.buildInfo(false);
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Discarded filaments: {}", Entities.ids(discardedFilaments));
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
        List<LineCluster> allClusters = new ArrayList<>();
        allClusters.addAll(clustersRetriever.getClusters());

        Integer smallInterline = null;

        if (smallClustersRetriever != null) {
            allClusters.addAll(smallClustersRetriever.getClusters());
            smallInterline = Math.min(
                    clustersRetriever.getInterline(),
                    smallClustersRetriever.getInterline());
        }

        Collections.sort(allClusters, clustersRetriever.byLayout);

        // Populate the staff manager
        int staffId = 0;
        staffManager.reset();

        for (LineCluster cluster : allClusters) {
            logger.debug("{}", cluster);

            // Copy array of lines
            List<StaffFilament> lines = new ArrayList<>(cluster.getLines());

            // Determine rough abscissa values for left & right sides
            double left = Integer.MAX_VALUE;
            double right = Integer.MIN_VALUE;

            for (StaffFilament line : lines) {
                left = Math.min(left, line.getStartPoint().getX());
                right = Math.max(right, line.getStopPoint().getX());
            }

            // Allocate Staff instance
            List<LineInfo> infos = new ArrayList<>(lines.size());

            for (StaffFilament line : lines) {
                infos.add(line);
            }

            Staff staff = new Staff(++staffId, left, right, cluster.getInterline(), infos);
            staffManager.addStaff(staff);

            // Flag small staff if any (smaller height than others)
            if ((smallInterline != null) && (smallInterline == cluster.getInterline())) {
                staff.setSmall();
            }
        }

        // Flag short staves (side by side) if any
        staffManager.detectShortStaves();
    }

    //--------------------//
    // canIncludeFilament //
    //--------------------//
    /**
     * Check whether the staff line filament could include the candidate filament
     *
     * @param lineFilament the staff line filament
     * @param fil          the candidate filament
     * @return true if OK
     */
    private boolean canIncludeFilament (StaffFilament lineFilament,
                                        Filament fil)
    {
        final boolean isVip = fil.isVip();
        final Rectangle box = fil.getBounds();
        final int xMid = box.x + (box.width / 2);
        final int maxThickness = params.maxStickerThickness;
        final int maxExt = params.maxStickerExtension;

        // For VIP debugging
        String vips = null;

        if (isVip) {
            vips = "Fil#" + fil.getId() + ": "; // BP here!
        }

        // Check entity thickness
        double eThickness = fil.getMeanThickness(HORIZONTAL);

        if (eThickness > maxThickness) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info("{}Fil thickness:{} vs {}", vips, eThickness, maxThickness);
            }

            return false;
        }

        // Check entity center gap with theoretical line
        double yLine = lineFilament.getPositionAt(xMid, HORIZONTAL);
        double yFil = fil.getPositionAt(xMid, HORIZONTAL);
        double dy = Math.abs(yLine - yFil);
        double gap = dy - (scale.getFore() / 2.0);

        if (gap > params.maxStickerGap) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info(String.format("%s gap:%.2f vs %.2f", vips, gap, params.maxStickerGap));
            }

            return false;
        }

        // Check max extension from theoretical line on each horizontal side of fil
        Point2D start = fil.getStartPoint();
        Point2D stop = fil.getStopPoint();
        double dyStart = start.getY() - lineFilament.getPositionAt(start.getX(), HORIZONTAL);
        double dyStop = stop.getY() - lineFilament.getPositionAt(stop.getX(), HORIZONTAL);
        int ext = (int) Math.rint(Math.max(Math.abs(dyStart), Math.abs(dyStop)));

        if (ext > maxExt) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info(String.format("%s ext:%d vs %d", vips, ext, maxExt));
            }

            return false;
        }

        // Check resulting thickness
        double rThickness = Compounds.getThicknessAt(xMid, HORIZONTAL, scale, fil, lineFilament);

        if (rThickness > maxThickness) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info(
                        String.format(
                                "%sRes thickness:%.1f vs %d",
                                vips,
                                rThickness,
                                maxThickness));
            }

            return false;
        }

        if (logger.isDebugEnabled() || isVip) {
            logger.info("{}---", vips);
        }

        return true;
    }

    //-------------------//
    // canIncludeSection //
    //-------------------//
    /**
     * Check whether the staff line filament could include the candidate section
     *
     * @param filament the staff line filament
     * @param section  the candidate section
     * @return true if OK, false otherwise
     */
    private boolean canIncludeSection (StaffFilament filament,
                                       Section section)
    {
        final boolean isVip = section.isVip();
        final Rectangle box = section.getBounds();
        final int xMid = box.x + (box.width / 2);
        final int maxThickness = params.maxStickerThickness;
        final int maxExt = params.maxStickerExtension;

        // For VIP debugging
        String vips = null;

        if (isVip) {
            vips = "Sct#" + section.getId() + ": "; // BP here!
        }

        // Check entity thickness
        double eThickness = box.height;

        if (eThickness > maxThickness) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info("{}Sct thickness:{} vs {}", vips, eThickness, maxThickness);
            }

            return false;
        }

        // Check entity center gap with theoretical line
        double yFil = filament.getPositionAt(xMid, HORIZONTAL);
        double dy = Math.abs(yFil - section.getCentroid2D().getY());
        double gap = dy - (scale.getFore() / 2.0);

        if (gap > params.maxStickerGap) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info(String.format("%s gap:%.2f vs %.2f", vips, gap, params.maxStickerGap));
            }

            return false;
        }

        // Check max extension from theoretical line
        int ext = (int) Math.rint(
                Math.max(Math.abs(yFil - box.y), Math.abs((box.y + eThickness) - yFil)));

        if (ext > maxExt) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info(String.format("%s ext:%d vs %d", vips, ext, maxExt));
            }

            return false;
        }

        // Check resulting thickness
        double rThickness = Compounds.getThicknessAt(xMid, HORIZONTAL, scale, section, filament);

        if (rThickness > maxThickness) {
            if (logger.isDebugEnabled() || isVip) {
                logger.info(
                        String.format(
                                "%sRes thickness:%.1f vs %d",
                                vips,
                                rThickness,
                                maxThickness));
            }

            return false;
        }

        if (logger.isDebugEnabled() || isVip) {
            logger.info("{}---", vips);
        }

        return true;
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

            Map<HorizontalSide, List<Point2D>> endMap = new EnumMap<>(HorizontalSide.class);

            for (HorizontalSide side : HorizontalSide.values()) {
                endMap.put(side, retrieveEndPoints(staff, meanDy, side));
            }

            // Adjust left and right endings of each line in the staff
            for (int i = 0; i < staff.getLines().size(); i++) {
                StaffFilament line = (StaffFilament) staff.getLines().get(i);
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
        final int maxLongId = sheet.getLagManager().getLongSectionMaxId();

        for (Section section : hLag.getEntities()) {
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
            List<StaffFilament> fils = new ArrayList<>();

            for (LineInfo line : staff.getLines()) {
                fils.add((StaffFilament) line);
            }

            for (int pos = 0; pos < staff.getLines().size(); pos++) {
                StaffFilament line = (StaffFilament) staff.getLines().get(pos);
                line.fillHoles(pos, fils);
            }
        }
    }

    //----------------//
    // getAllStickers //
    //----------------//
    private List<Section> getAllStickers ()
    {
        List<Section> list = new ArrayList<>(hLag.getEntities());

        // Remove any (hori) section that is already part of a staff line
        for (Staff staff : staffManager.getStaves()) {
            for (LineInfo l : staff.getLines()) {
                StaffFilament line = (StaffFilament) l;
                list.removeAll(line.getMembers());
            }
        }

        Collections.sort(list, Section.byFullPosition);

        // Build pos-based index
        final SectionTally<Section> tally = new SectionTally<>(sheet.getHeight(), list);

        Set<Section> connected = new LinkedHashSet<>();

        // Detect sections with connections below
        for (int i = 0, iBreak = list.size(); i < iBreak; i++) {
            final Section source = list.get(i);
            final Run predRun = source.getLastRun();
            final int predStart = predRun.getStart();
            final int predStop = predRun.getStop();
            final int nextPos = source.getFirstPos() + source.getRunCount();

            if (nextPos < sheet.getHeight()) {
                int touching = 0; // Number of touching pixels with next run(s)

                for (Section target : tally.getSubList(nextPos)) {
                    final Run succRun = target.getFirstRun();

                    if (succRun.getStart() > predStop) {
                        break; // Since sublist is sorted on coord
                    }

                    if (succRun.getStop() >= predStart) {
                        int commonStart = Math.max(predStart, succRun.getStart());
                        int commonStop = Math.min(predStop, succRun.getStop());
                        touching += (commonStop - commonStart + 1);
                    }

                    if (touching > params.maxStickerConnectionLength) {
                        connected.add(source);
                    }
                }
            }
        }

        // Detect sections with connections above
        for (int i = 0, iBreak = list.size(); i < iBreak; i++) {
            final Section source = list.get(i);
            final Run predRun = source.getFirstRun();
            final int predStart = predRun.getStart();
            final int predStop = predRun.getStop();
            final int nextPos = source.getFirstPos() - 1;

            if (nextPos >= 0) {
                int touching = 0; // Number of touching pixels with next run(s)

                for (Section target : tally.getSubList(nextPos)) {
                    final Run succRun = target.getLastRun();

                    if (succRun.getStart() > predStop) {
                        break; // Since sublist is sorted on coord
                    }

                    if (succRun.getStop() >= predStart) {
                        int commonStart = Math.max(predStart, succRun.getStart());
                        int commonStop = Math.min(predStop, succRun.getStop());
                        touching += (commonStop - commonStart + 1);
                    }

                    if (touching > params.maxStickerConnectionLength) {
                        connected.add(source);
                    }
                }
            }
        }

        // Keep only sections that are 1-pixel high and have limited connection
        list.removeAll(connected);

        List<Section> stickers = new ArrayList<>();

        for (Section section : list) {
            if (section.getRunCount() == 1) {
                stickers.add(section);
            }
        }

        return stickers;
    }

    //---------------------------//
    // includeDiscardedFilaments //
    //---------------------------//
    /**
     * Last attempt to include discarded (and sloped) filaments to retrieved staff lines.
     */
    private void includeDiscardedFilaments ()
    {
        List<StaffFilament> candidates = new ArrayList<>();
        candidates.addAll(discardedFilaments);
        candidates.addAll(slopedFilaments);

        // Sort candidates filaments by top ordinate
        Collections.sort(candidates, Filament.topComparator);

        final int iMax = candidates.size() - 1;

        for (SystemInfo system : sheet.getSystems()) {
            // Systems may be side by side, so restart from top
            int iMin = 0;

            for (Staff staff : system.getStaves()) {
                for (LineInfo line : staff.getLines()) {
                    final StaffFilament filament = (StaffFilament) line;
                    final Point2D startPt = filament.getStartPoint();
                    final Point2D stopPt = filament.getStopPoint();
                    final double minX = startPt.getX();
                    final double maxX = stopPt.getX();
                    final Rectangle lineBox = filament.getBounds();
                    lineBox.grow(0, scale.getFore());

                    final int minY = lineBox.y;
                    final int maxY = lineBox.y + lineBox.height;
                    boolean filamentModified = false;

                    // Browse discarded filaments
                    for (int i = iMin; i <= iMax; i++) {
                        Filament fil = candidates.get(i);

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
                                filamentModified = true;
                            }
                        }
                    }

                    if (filamentModified) {
                        filament.setEndingPoints(startPt, stopPt); // Recompute line
                    }
                }
            }
        }
    }

    //-----------------//
    // includeSections //
    //-----------------//
    /**
     * Include horizontal "sticker" sections into their related lines, when applicable
     *
     * @param sections List of horizontal sections that are stickers candidates
     */
    private void includeSections (List<Section> sections)
    {
        // Use a temporary vector indexed by section ID
        final int idMax = hLag.getLastId();
        final boolean[] included = new boolean[idMax + 1];
        Arrays.fill(included, false);

        // Sections are sorted according to their top run (Y)
        Collections.sort(sections, Section.byPosition);

        final int iMax = sections.size() - 1;

        // Sections included so far
        for (SystemInfo system : sheet.getSystems()) {
            // Because of possible side by side systems, we must restart from top
            int iMin = 0;

            for (Staff staff : system.getStaves()) {
                for (LineInfo line : staff.getLines()) {
                    /*
                     * Inclusion on the fly would imply recomputation of filament at each section
                     * inclusion. So we need to retrieve all "stickers" for a given staff line, and
                     * perform a global inclusion at the end only.
                     */
                    final StaffFilament fil = (StaffFilament) line;
                    final Point2D startPoint = fil.getEndPoint(LEFT);
                    final Point2D stopPoint = fil.getEndPoint(RIGHT);
                    final Rectangle lineBox = fil.getBounds();
                    lineBox.grow(0, scale.getFore());

                    final double minX = fil.getStartPoint().getX();
                    final double maxX = fil.getStopPoint().getX();
                    final int minY = lineBox.y;
                    final int maxY = lineBox.y + lineBox.height;
                    final List<Section> stickers = new ArrayList<>();

                    for (int i = iMin; i <= iMax; i++) {
                        Section section = sections.get(i);

                        if (included[section.getId()]) {
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

                    // Actually include the retrieved stickers
                    for (Section section : stickers) {
                        fil.addSection(section); // Invalidates filament cache, including extrema
                        included[section.getId()] = true;
                    }

                    // Restore extrema points (keep abscissae, but recompute ordinates)
                    fil.setEndingPoints(
                            new Point2D.Double(startPoint.getX(), fil.yAt(startPoint.getX())),
                            new Point2D.Double(stopPoint.getX(), fil.yAt(stopPoint.getX())));
                }
            }
        }
    }

    //-----------------//
    // includeStickers //
    //-----------------//
    /**
     * Horizontal sections of just 1-pixel height, stuck to a staff-line, and not stuck
     * to any other (horizontal) section are considered as part of the staff-line.
     * Otherwise these useless tiny sections would impede later symbol recognition.
     */
    private void includeStickers ()
    {
        final List<Section> stickers = getAllStickers();
        final SectionTally<Section> tally = new SectionTally<>(sheet.getHeight(), stickers);

        for (Staff staff : staffManager.getStaves()) {
            int lineId = 0;

            for (LineInfo l : staff.getLines()) {
                lineId++;

                StaffFilament fil = (StaffFilament) l;
                Set<Section> toAdd = new LinkedHashSet<>();

                for (Section source : fil.getMembers()) {
                    for (VerticalSide side : VerticalSide.values()) {
                        final Run predRun = (side == TOP) ? source.getFirstRun()
                                : source.getLastRun();
                        final int predStart = predRun.getStart();
                        final int predStop = predRun.getStop();

                        final int nextPos = (side == TOP) ? (source.getFirstPos() - 1)
                                : (source.getLastPos() + 1);

                        for (Section target : tally.getSubList(nextPos)) {
                            final Run succRun = target.getFirstRun();

                            if (succRun.getStart() > predStop) {
                                break; // Since sublist is sorted on coord
                            }

                            if (succRun.getStop() >= predStart) {
                                toAdd.add(target);
                            }
                        }
                    }
                }

                if (!toAdd.isEmpty()) {
                    if (logger.isDebugEnabled()) {
                        logger.info(
                                "Staff#{} line#{} {}",
                                staff.getId(),
                                lineId,
                                Sections.toString(toAdd));
                    }

                    // Include sticker sections, while perserving line ending points
                    final Point2D startPoint = fil.getEndPoint(LEFT);
                    final Point2D stopPoint = fil.getEndPoint(RIGHT);

                    for (Section sticker : toAdd) {
                        fil.addSection(sticker);
                    }

                    fil.setEndingPoints(startPoint, stopPoint);
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
            for (LineInfo line : staff.getLines()) {
                ((CurvedFilament) line).polishCurvature(params.minRadius);
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
            throws StepException
    {
        List<Filament> toRemove = new ArrayList<>();

        for (StaffFilament fil : filaments) {
            Point2D start = fil.getStartPoint();
            Point2D stop = fil.getStopPoint();

            // Check if this filament is straight enough
            double xMid = (start.getX() + stop.getX()) / 2;
            NaturalSpline spline = fil.getSpline();
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
            logger.debug("Discarded curved line filaments: {}", toRemove.size());
            filaments.removeAll(toRemove);
        }

        if (filaments.size() < 5) {
            sheet.getStub().decideOnRemoval(
                    sheet.getId() + LINE_SEPARATOR + "Too few staff filaments: " + filaments.size()
                            + LINE_SEPARATOR + "This sheet does not seem to contain staff lines.",
                    false);
        }
    }

    //----------------------//
    // purgeSlopedFilaments //
    //----------------------//
    /**
     * Now that we know the global sheet slope, discard the filaments whose slope is too
     * far from sheet slope.
     * <p>
     * Short filaments (typically based on a single section) are very likely to be horizontal, and
     * thus may exhibit a significant delta slope when sheet absolute slope is high.
     * For these short filaments, we have to accept a slope within [0-, sheet slope] if sheet slope
     * is positive, and within [sheet slope, 0+] if sheet slope is negative.
     *
     * @return the collection of purged sloped filaments
     */
    private List<StaffFilament> purgeSlopedFilaments ()
    {
        final double sheetSlope = sheet.getSkew().getSlope();
        final double minShortSlope = (sheetSlope > 0) ? (-params.maxSlopeDiff / 2) : sheetSlope;
        final double maxShortSlope = (sheetSlope > 0) ? sheetSlope : (params.maxSlopeDiff / 2);
        final List<StaffFilament> toRemove = new ArrayList<>();

        for (StaffFilament fil : filaments) {
            if (fil.isVip()) {
                logger.info("running purgeSlopedFilaments for {}", fil);
            }

            Point2D start = fil.getStartPoint();
            Point2D stop = fil.getStopPoint();

            double filSlope = LineUtil.getSlope(start, stop);
            final double slopeDiff = Math.abs(sheetSlope - filSlope);

            if (slopeDiff > params.maxSlopeDiff) {
                if (fil.getLength(HORIZONTAL) < params.minLengthForSlopeCheck) {
                    // Case of short filement
                    if ((filSlope >= minShortSlope) && (filSlope <= maxShortSlope)) {
                        continue;
                    }
                }

                if (fil.isVip()) {
                    logger.info(
                            "VIP discarded {} for delta slope {}",
                            fil,
                            String.format("%.3f > %.3f", slopeDiff, params.maxSlopeDiff));
                }

                toRemove.add(fil);
            }
        }

        if (!toRemove.isEmpty()) {
            logger.debug("Discarded sloped filaments: {}", toRemove.size());
            filaments.removeAll(toRemove);
        }

        return toRemove;
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
     * @param staff  the staff to process
     * @param meanDy actual mean interline for the staff
     * @param side   left or right side
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
            StaffFilament line = (StaffFilament) staff.getLines().get(i);
            Point2D linePt = line.getEndPoint(side);
            double dx = staffX - linePt.getX();
            double dxAbs = Math.abs(dx);

            if (dxAbs <= params.maxEndingDx) {
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
                    scale.getFore(),
                    scale.getInterline());

            // Find the most probable upper left ordinate
            final double uly;

            if (tops.getCardinality() > 0) {
                uly = tops.getMeanValue();
            } else {
                // Extrapolate the line which ends closest to the staff end abscissa
                StaffFilament line = (StaffFilament) staff.getLines().get(bestIndex);
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
        Collections.sort(filaments, Compounds.byReverseLength(HORIZONTAL));

        for (int i = 0; i < topCount; i++) {
            Filament fil = filaments.get(i);
            Point2D start = fil.getStartPoint();
            Point2D stop = fil.getStopPoint();
            slopes += ((stop.getY() - start.getY()) / (stop.getX() - start.getX()));
        }

        double mean = slopes / topCount;

        if (Math.abs(mean) >= params.minSlope) {
            return mean;
        } else {
            return 0;
        }
    }

    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio topRatioForSlope = new Constant.Ratio(
                0.1,
                "Percentage of top filaments used to retrieve global slope");

        private final Constant.Double maxFilamentRotation = new Constant.Double(
                "radians",
                0.1,
                "Maximum central rotation for filaments");

        private final Constant.Double maxSlopeDiff = new Constant.Double(
                "radians",
                0.025,
                "Maximum delta slope between filament and sheet");

        private final Constant.Double minSlope = new Constant.Double(
                "tangent",
                0.0002,
                "Minimum absolute slope value to be worth noting");

        // Constants specified WRT *maximum* line thickness (scale.getmaxFore())
        // ----------------------------------------------
        private final Constant.Ratio stickerThickness = new Constant.Ratio(
                1.0,
                "Ratio of sticker thickness vs staff line MAXIMUM thickness");

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        //
        private final Scale.LineFraction maxStickerGap = new Scale.LineFraction(
                0.25,
                "Maximum vertical gap between sticker and closest line side");

        private final Scale.LineFraction maxStickerExtension = new Scale.LineFraction(
                1.2,
                "Maximum vertical sticker extension from line");

        private final Scale.AreaFraction maxThinStickerWeight = new Scale.AreaFraction(
                0.06,
                "Maximum weight for a thin sticker (w/o impact on line geometry)");

        // Constants specified WRT mean interline
        // --------------------------------------
        private final Scale.Fraction minRunLength = new Scale.Fraction(
                1.0,
                "Minimum length for a horizontal run to be considered");

        private final Scale.Fraction maxEndingDx = new Scale.Fraction(
                1.0,
                "Maximum abscissa delta between line end and staff end");

        private final Scale.Fraction patternWidth = new Scale.Fraction(
                1.0,
                "Width of probe for staff pattern");

        private final Scale.Fraction patternJitter = new Scale.Fraction(
                0.25,
                "Maximum ordinate jitter for staff pattern");

        private final Scale.Fraction minRadius = new Scale.Fraction(
                12,
                "Minimum acceptable radius of polished curvature");

        private final Scale.Fraction minLengthForSlopeCheck = new Scale.Fraction(
                4.0,
                "Minimum filament length for strict slope check");

        private final Scale.Fraction maxStickerConnectionLength = new Scale.Fraction(
                0.05,
                "Maximum connected pixels for a line sticker");

        private final Constant.Boolean showHorizontalLines = new Constant.Boolean(
                true,
                "Should we show the horizontal grid lines?");

        private final Scale.Fraction tangentLg = new Scale.Fraction(
                1,
                "Typical length to show tangents at ending points");

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Boolean showTangents = new Constant.Boolean(
                false,
                "Should we show filament ending tangents?");

        private final Constant.Boolean showCombs = new Constant.Boolean(
                false,
                "Should we show staff lines combs?");

        // Constants for display
        // ---------------------
        Constant.Boolean displayRuns = new Constant.Boolean(
                false,
                "Should we display all images on runs?");
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

        /** Minimum run length for horizontal lag */
        final int minRunLength;

        /** Percentage of top filaments used to retrieve global slope */
        final double topRatioForSlope;

        /** Maximum rotation angle for filaments used to retrieve global slope */
        final double maxFilamentRotation;

        /** Maximum delta slope between filament and sheet */
        final double maxSlopeDiff;

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

        /** Minimum absolute slope to be worth noting */
        final double minSlope;

        /** Minimum polished radius. */
        final int minRadius;

        final int minLengthForSlopeCheck;

        final int maxStickerConnectionLength;

        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        Parameters (Scale scale)
        {
            // Special parameters
            maxStickerThickness = (int) Math.rint(
                    scale.getMaxFore() * constants.stickerThickness.getValue());

            // Others
            minRunLength = scale.toPixels(constants.minRunLength);
            topRatioForSlope = constants.topRatioForSlope.getValue();
            maxFilamentRotation = constants.maxFilamentRotation.getValue();
            maxSlopeDiff = constants.maxSlopeDiff.getValue();
            maxStickerGap = scale.toPixelsDouble(constants.maxStickerGap);
            maxThinStickerWeight = scale.toPixels(constants.maxThinStickerWeight);
            maxEndingDx = scale.toPixels(constants.maxEndingDx);
            patternWidth = scale.toPixels(constants.patternWidth);
            patternJitter = scale.toPixels(constants.patternJitter);
            minRadius = scale.toPixels(constants.minRadius);
            minLengthForSlopeCheck = scale.toPixels(constants.minLengthForSlopeCheck);
            maxStickerConnectionLength = scale.toPixels(constants.maxStickerConnectionLength);
            maxStickerExtension = (int) Math.ceil(
                    scale.toPixelsDouble(constants.maxStickerExtension));
            minSlope = constants.minSlope.getValue();

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
