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
import omr.glyph.facets.Glyph;

import omr.lag.JunctionRatioPolicy;
import omr.lag.SectionsBuilder;

import omr.log.Logger;

import omr.run.Orientation;
import static omr.run.Orientation.*;
import omr.run.Run;
import omr.run.RunsTable;
import omr.run.RunsTableFactory;

import omr.score.ui.PagePainter;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;

import omr.stick.StickSection;
import static omr.util.HorizontalSide.*;
import omr.util.Predicate;
import omr.util.StopWatch;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;

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

    /** Color for patterns of main interline */
    private static final Color mainPatternColor = new Color(200, 255, 200);

    /** Color for patterns of second interline */
    private static final Color secondPatternColor = new Color(200, 200, 0);

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

    /** Global slope of the sheet */
    private double globalSlope;

    /** Companion in charge of clusters of main interline */
    private ClustersRetriever clustersRetriever;

    /** Companion in charge of clusters of second interline, if any */
    private ClustersRetriever secondClustersRetriever;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // LinesRetriever //
    //----------------//
    /**
     * Retrieve the frames of all staff lines
     *
     * @param sheet the sheet to process
     */
    public LinesRetriever (Sheet sheet)
    {
        this.sheet = sheet;

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

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Organize the long and thin horizontal sections into filaments (glyphs)
     * that will be good candidates for staff lines.
     * <p>First, retrieve long horizontal sections and merge them into
     * filaments.</p>
     * <p>Second, detect patterns of filaments regularly spaced and aggregate
     * them into clusters of lines (staff candidates). </p>
     */
    public void buildInfo ()
    {
        StopWatch watch = new StopWatch("LinesRetriever");

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
                mainPatternColor);
            watch.start("clustersRetriever");

            List<LineFilament> discarded = clustersRetriever.buildInfo();

            // Check for a second interline
            Integer secondInterline = scale.secondInterline();

            if (secondInterline != null) {
                secondFilaments = discarded;
                Collections.sort(secondFilaments, Glyph.idComparator);
                logger.info(
                    sheet.getLogPrefix() +
                    "Searching clusters with secondInterline: " +
                    secondInterline);
                secondClustersRetriever = new ClustersRetriever(
                    sheet,
                    secondFilaments,
                    secondInterline,
                    secondPatternColor);
                watch.start("secondClustersRetriever");
                secondClustersRetriever.buildInfo();
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

    //----------//
    // buildLag //
    //----------//
    /**
     * Build the underlying lag, out of the provided runs table
     * @param wholeVertTable the provided runs table
     * @param showRuns true to create views on runs
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
            sheet.getAssembly()
                 .addRunsTab(purgedVertTable);

            // Add a view on runs table (debug)
            sheet.getAssembly()
                 .addRunsTab(shortVertTable);
        }

        // Build table of long horizontal runs
        RunsTable wholeHoriTable = new RunsTableFactory(
            HORIZONTAL,
            shortVertTable.getBuffer(),
            sheet.getPicture().getMaxForeground(),
            0).createTable("whole-hori");

        RunsTable longHoriTable = wholeHoriTable.clone("long-hori")
                                                .purge(
            new Predicate<Run>() {
                    public final boolean check (Run run)
                    {
                        return run.getLength() < params.minRunLength;
                    }
                });

        if (showRuns) {
            // Add a view on runs table (debug)
            sheet.getAssembly()
                 .addRunsTab(longHoriTable);
        }

        // Build the horizontal hLag
        SectionsBuilder sectionsBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
            hLag,
            new JunctionRatioPolicy(params.maxLengthRatio));
        sectionsBuilder.createSections(longHoriTable);

        // Debug sections VIPs
        for (int id : params.vipSections) {
            GlyphSection sect = hLag.getVertexById(id);

            if (sect != null) {
                sect.setVip();
            }
        }

        // Purge hLag of too short sections
        //        final int minSectionLength = factory.getMinSectionLength();
        //        hLag.purgeSections(
        //            new Predicate<GlyphSection>() {
        //                    public final boolean check (GlyphSection section)
        //                    {
        //                        return section.getLength() < minSectionLength;
        //                    }
        //                });

        //
        return purgedVertTable;
    }

    //--------------//
    // isSectionFat //
    //--------------//
    /**
     * Detect if the provided section is a thick one
     * @param section the section to check
     * @return true if fat
     */
    boolean isSectionFat (GlyphSection section)
    {
        return factory.isSectionFat(section);
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render the filaments, their ending tangents, their patterns
     * @param g graphics context
     */
    void renderItems (Graphics2D g)
    {
        List<LineFilament> allFils = new ArrayList<LineFilament>(filaments);

        if (secondFilaments != null) {
            allFils.addAll(secondFilaments);
        }

        // Draw filaments
        g.setColor(PagePainter.musicColor);

        Stroke oldStroke = g.getStroke();
        g.setStroke(splineStroke);

        for (Filament filament : allFils) {
            filament.renderLine(g);
        }

        // Draw tangent at each ending point
        g.setColor(Color.BLACK);

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

        g.setStroke(oldStroke);

        // Patterns stuff        
        if (clustersRetriever != null) {
            clustersRetriever.renderItems(g);
        }

        if (secondClustersRetriever != null) {
            secondClustersRetriever.renderItems(g);
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

        // Constants specified WRT mean line thickness
        // -------------------------------------------
        Scale.LineFraction maxVerticalRunLength = new Scale.LineFraction(
            1.75, // 2.0
            "Maximum vertical run length WRT mean line height");

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

        /** Percentage of top filaments used to retrieve global slope */
        final double topRatioForSlope;

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
            topRatioForSlope = constants.topRatioForSlope.getValue();

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
}
