//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        P e a k G r a p h                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.dynamic.Filament;

import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;

import omr.math.AreaUtil;
import omr.math.GeoPath;
import omr.math.LineUtil;
import static omr.run.Orientation.HORIZONTAL;

import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import omr.sheet.Staff;
import omr.sheet.StaffManager;
import omr.sheet.SystemInfo;
import omr.sheet.SystemManager;
import static omr.sheet.grid.StaffPeak.Attribute.BRACE;

import omr.step.StepException;

import omr.util.Dumping;
import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.LEFT;
import static omr.util.HorizontalSide.RIGHT;
import omr.util.Navigable;
import omr.util.StopWatch;
import omr.util.VerticalSide;
import static omr.util.VerticalSide.BOTTOM;
import static omr.util.VerticalSide.TOP;

import ij.process.ByteProcessor;

import org.jgrapht.Graphs;
import org.jgrapht.graph.SimpleDirectedGraph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code PeakGraph} handles the graph of all StaffPeak instances in a sheet,
 * linked by alignment/connection relationships.
 *
 * @author Hervé Bitteur
 */
public class PeakGraph
        extends SimpleDirectedGraph<StaffPeak, BarAlignment>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(PeakGraph.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Scale-dependent constants. */
    private final Parameters params;

    /** Related staff manager. */
    private final StaffManager staffManager;

    /** Staff projectors. (sequence parallel to staves in sheet) */
    private final List<StaffProjector> projectors;

    /** Specific builder for peak-based filaments. */
    private final BarFilamentBuilder filamentBuilder;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PeakGraph} object.
     *
     * @param sheet      the sheet to process
     * @param projectors the projector for each staff
     */
    public PeakGraph (Sheet sheet,
                      List<StaffProjector> projectors)
    {
        super(BarAlignment.class);
        this.sheet = sheet;
        this.projectors = projectors;

        params = new Parameters(sheet.getScale());

        staffManager = sheet.getStaffManager();
        filamentBuilder = new BarFilamentBuilder(sheet);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // buildSystems //
    //--------------//
    /**
     * Retrieve all bar peaks found in staves projection, organize them in a graph
     * of peaks linked by alignment/connection relations, and infer the systems.
     *
     * @throws StepException
     */
    public void buildSystems ()
            throws StepException
    {
        final StopWatch watch = new StopWatch("PeakGraph.buildSystems");

        watch.start("findBarPeaks");
        findBarPeaks(); // Individual staff analysis to find raw peaks on x-axis projection

        watch.start("buildBarSticks");
        buildBarSticks(); // Build core filament for each peak

        watch.start("detectCurvedPeaks");
        detectCurvedPeaks(); // Flag any peak that looks like a curved entity (very weak filter!)

        watch.start("findAllAlignments");
        findAllAlignments(); // Find all peak alignments across staves
        //
        //        watch.start("alignGroups");
        //        alignGroups(); // Rectify alignments between groups of peaks

        watch.start("findConnections");
        findConnections(); // Find all concrete connections across staves

        watch.start("splitMergedGroups");
        splitMergedGroups(); // Split thick peaks that result from merged peaks

        watch.start("purgeAlignments");
        purgeAlignments(); // Purge conflicting connections & alignments

        watch.start("createSystems");

        // Create systems out of bar connections
        SystemManager mgr = sheet.getSystemManager();
        mgr.setSystems(createSystems(getSystemTops()));

        if (mgr.getSystems().isEmpty()) {
            logger.warn("No system found");
            sheet.getStub().invalidate();
            throw new StepException("No system found");
        }

        logger.info("Systems: {}", mgr.getSystemsString());

        watch.start("purgeCrossAlignments");
        purgeCrossAlignments(); // Purge peak alignments across systems, they are not relevant

        if (constants.printWatch.isSet()) {
            watch.print();
        }
    }

    //----------------//
    // checkAlignment //
    //----------------//
    /**
     * Check whether two peaks (first in top staff, second in bottom staff) are aligned.
     * <p>
     * We check vertical alignment, taking sheet slope into account.
     *
     * @param topPeak    peak in top staff
     * @param botPeak    peak in bottom staff
     * @param checkSlope true for slope check, false for no check
     * @param checkWidth true for dWidth check, false for no check
     * @return the BarAlignment or null
     */
    public BarAlignment checkAlignment (StaffPeak topPeak,
                                        StaffPeak botPeak,
                                        boolean checkSlope,
                                        boolean checkWidth)
    {
        if (topPeak.isVip() && botPeak.isVip()) {
            logger.info("VIP running checkAlignment for {} & {}", topPeak, botPeak);
        }

        final double sheetVertSlope = -sheet.getSkew().getSlope();

        // Slopes on left and on right, take the smallest
        double y1 = topPeak.getOrdinate(BOTTOM);
        double y2 = botPeak.getOrdinate(TOP);
        double lSlope = LineUtil.getInvertedSlope(topPeak.getStart(), y1, botPeak.getStart(), y2);
        double diffLeft = Math.abs(lSlope - sheetVertSlope);
        double rSlope = LineUtil.getInvertedSlope(topPeak.getStop(), y1, botPeak.getStop(), y2);
        double diffRight = Math.abs(rSlope - sheetVertSlope);
        double minSlope = Math.min(diffLeft, diffRight);

        if (checkSlope && (minSlope > params.maxAlignmentSlope)) {
            if (topPeak.isVip() && botPeak.isVip()) {
                logger.info("VIP large slope {} between {} & {}", minSlope, topPeak, botPeak);
            }

            return null;
        }

        // Width consistency
        final double dWidth = botPeak.getWidth() - topPeak.getWidth();

        if (checkWidth && (Math.abs(dWidth) > params.maxAlignmentDeltaWidth)) {
            return null;
        }

        final double alignImpact = 1 - (Math.abs(minSlope) / params.maxAlignmentSlope);
        final double widthImpact = 1 - (Math.abs(dWidth) / params.maxAlignmentDeltaWidth);

        return new BarAlignment(
                topPeak,
                botPeak,
                minSlope,
                dWidth,
                new BarAlignment.Impacts(alignImpact, widthImpact));
    }

    //---------------------//
    // checkBraceAlignment //
    //---------------------//
    /**
     * A rudimentary check of alignment between two candidate brace peaks.
     *
     * @param topPeak peak in top staff
     * @param botPeak peak in bottom staff
     * @return true if OK
     */
    public boolean checkBraceAlignment (StaffPeak topPeak,
                                        StaffPeak botPeak)
    {
        final Skew skew = sheet.getSkew();
        final int topMid = (topPeak.getStart() + topPeak.getStop()) / 2;
        final double topDsk = skew.deskewed(new Point(topMid, topPeak.getOrdinate(BOTTOM))).getX();
        final int botMid = (botPeak.getStart() + botPeak.getStop()) / 2;
        final double botDsk = skew.deskewed(new Point(botMid, botPeak.getOrdinate(TOP))).getX();
        final double dx = botDsk - topDsk;

        return Math.abs(dx) <= params.maxAlignmentBraceDx;
    }

    //-------------------//
    // areRightConnected //
    //-------------------//
    /**
     * Report whether the two provided staves are connected on their last peak.
     *
     * @param top    top staff
     * @param bottom bottom staff
     * @return true if right connected
     */
    private boolean areRightConnected (Staff top,
                                       Staff bottom)
    {
        StaffPeak p1 = projectorOf(top).getLastPeak();
        StaffPeak p2 = projectorOf(bottom).getLastPeak();

        if ((p1 != null) && (p2 != null)) {
            BarAlignment align = this.getEdge(p1, p2);

            if (align instanceof BarConnection) {
                return true;
            }
        }

        return false;
    }

    //
    //    //-------------//
    //    // alignGroups //
    //    //-------------//
    //    /**
    //     * For groups of peaks of identical size between two staves, make sure alignments
    //     * are consistent with each peak position within the group.
    //     */
    //    private void alignGroups ()
    //    {
    //        List<List<StaffPeak>> groups1 = null;
    //
    //        for (StaffProjector projector : projectors) {
    //            final List<List<StaffPeak>> groups2 = getGroupsOf(projector);
    //
    //            if (groups1 != null) {
    //                int i2Min = 0;
    //
    //                for (int i1 = 0; i1 < groups1.size(); i1++) {
    //                    final List<StaffPeak> g1 = groups1.get(i1);
    //                    final double start1 = g1.get(0).getDeskewedCenter().getX();
    //                    final double stop1 = g1.get(g1.size() - 1).getDeskewedCenter().getX();
    //
    //                    for (int i2 = i2Min; i2 < groups2.size(); i2++) {
    //                        final List<StaffPeak> g2 = groups2.get(i2);
    //                        final List<StaffPeak> partners = getConnectedPeaks(g2.get(0), TOP);
    //                        partners.retainAll(g1);
    //
    //                        if (!partners.isEmpty()) {
    //                            // We have some alignment, check the pair of groups
    //                            if (g1.size() == g2.size()) {
    //                                pruneGroupPair(g1, g2);
    //                            }
    //                        } else {
    //                            // Speed up a bit
    //                            final double start2 = g2.get(0).getDeskewedCenter().getX();
    //
    //                            if (start2 > (stop1 + params.maxAlignmentSlope)) {
    //                                break;
    //                            }
    //
    //                            final double stop2 = g2.get(g2.size() - 1).getDeskewedCenter().getX();
    //
    //                            if ((stop2 + params.maxAlignmentSlope) < start1) {
    //                                i2Min = i1 + 1;
    //                            }
    //                        }
    //                    }
    //                }
    //            }
    //
    //            groups1 = groups2;
    //        }
    //    }
    //
    //----------------//
    // buildBarSticks //
    //----------------//
    /**
     * Build the underlying stick of every peak.
     * <p>
     * These sticks are needed to detect those peaks which go past staff height above,
     * below, or both, and may rather be stems.
     * They are used also to detect curly peaks that are due to brace portions (not reliable).
     * <p>
     * For each peak, we take a vertical "slice" of the relevant sections using a lookup area.
     * We then run a dedicated factory on the sections and make it focus on the bar core area.
     */
    private void buildBarSticks ()
    {
        // Preselect sections of proper max width
        final int maxWidth = getMaxPeaksWidth();
        final List<Section> allSections = getSectionsByWidth(maxWidth);
        logger.debug("sections:{}", allSections.size());

        for (StaffProjector projector : projectors) {
            List<StaffPeak> toRemove = new ArrayList<StaffPeak>();

            for (StaffPeak peak : projector.getPeaks()) {
                // Build filament from proper slice of sections for this peak
                Filament filament = filamentBuilder.buildFilament(
                        peak,
                        params.bracketLookupExtension,
                        allSections);

                if (filament != null) {
                    peak.setFilament(filament);
                    logger.debug("{}", peak);
                } else {
                    toRemove.add(peak);
                }
            }

            if (!toRemove.isEmpty()) {
                logger.debug("Staff#{} no filament {}", projector.getStaff().getId(), toRemove);
                projector.removePeaks(toRemove);
            }
        }
    }

    //-----------------//
    // checkConnection //
    //-----------------//
    /**
     * Check whether the provided alignment is a true connection, that is with concrete
     * foreground pixels on the alignment line.
     * <p>
     * For this, we define a spline which goes through top & bottom points of each of the aligned
     * peaks and look for pixels in the area.
     *
     * @param alignment the alignment to check.
     * @return the connection if OK, null otherwise
     */
    private BarConnection checkConnection (BarAlignment alignment)
    {
        ByteProcessor pixelFilter = sheet.getPicture().getSource(
                Picture.SourceKey.BINARY);
        StaffPeak p1 = alignment.topPeak;
        StaffPeak p2 = alignment.bottomPeak;
        final boolean vip = p1.isVip() && p2.isVip();

        // Theoretical lines on left and right sides
        final GeoPath leftLine = new GeoPath(
                new Line2D.Double(
                        new Point2D.Double(p1.getStart(), p1.getBottom()),
                        new Point2D.Double(p2.getStart(), p2.getTop())));
        final GeoPath rightLine = new GeoPath(
                new Line2D.Double(
                        new Point2D.Double(p1.getStop(), p1.getBottom()),
                        new Point2D.Double(p2.getStop(), p2.getTop())));
        final AreaUtil.CoreData data = AreaUtil.verticalCore(pixelFilter, leftLine, rightLine);

        if (vip) {
            logger.info("VIP running checkConnection {} and {} {}", p1, p2, data);
        }

        if ((data.gap <= params.maxConnectionGap)
            && (data.whiteRatio <= params.maxConnectionWhiteRatio)) {
            BarConnection connection = new BarConnection(alignment);

            if (logger.isDebugEnabled() || vip) {
                logger.info("VIP {}", connection);
            }

            StaffPeak source = getEdgeSource(alignment);
            StaffPeak target = getEdgeTarget(alignment);
            removeEdge(alignment);
            addEdge(source, target, connection);

            return connection;
        }

        if (vip) {
            logger.info("VIP no connection between {} and {}", p1, p2);
        }

        return null;
    }

    //---------------//
    // checkForSplit //
    //---------------//
    /**
     * Check whether the provided peak is candidate for a split.
     * <p>
     * The peak must not be part of a group and it must have a connection with partners.
     * This is meant to limit such split to real cases.
     * <p>
     * It must be "aligned" with 2 partners above or 2 partners below, which would be compatible
     * with this peak, in terms of width and span.
     * <p>
     * The problem with a thick/thin in one staff and a thick in the other staff is that there may
     * be no "alignment" between thin to thick, simply because of width difference.
     * Hence we cannot rely only on "established" alignments, but have to consider a kind of "group
     * to group" alignment.
     *
     * @param peak the peak to check
     * @return the map of partners, above & below, empty if not candidate
     */
    private Map<VerticalSide, List<StaffPeak>> checkForSplit (StaffPeak peak)
    {
        if (peak.isVip()) {
            logger.info("VIP running checkForSplit for {}", peak);
        }

        final Map<VerticalSide, List<StaffPeak>> map = new EnumMap<VerticalSide, List<StaffPeak>>(
                VerticalSide.class);
        final List<StaffPeak> peakGroup = groupOf(Arrays.asList(peak));

        if (peakGroup.size() > 1) {
            return map;
        }

        for (VerticalSide side : VerticalSide.values()) {
            List<StaffPeak> partners = groupOf(getConnectedPeaks(peak, side));

            if (partners.size() != 2) {
                continue; // We can accommodate only 2 partners
            }

            final StaffPeak p1 = partners.get(0);
            final StaffPeak p2 = partners.get(partners.size() - 1);
            final int maxWidth = Math.max(p1.getWidth(), p2.getWidth());

            if (peak.getWidth() <= (maxWidth + 2)) {
                continue;
            }

            // Check gap between partners
            final int gap = p2.getStart() - p1.getStop() + 1;

            if (gap > params.maxCloseGap) {
                continue;
            }

            int width = peak.getWidth();

            // Total width for partners
            int total = totalWidth(partners);

            // Span width for partners
            int span = p2.getStop() - p1.getStart() + 1;

            // Width difference ratio
            int dTotal = Math.abs(total - width);
            double rTotal = dTotal / (double) Math.max(total, width);
            int dSpan = Math.abs(span - width);
            double rSpan = dSpan / (double) Math.max(span, width);
            double minRatio = Math.min(rTotal, rSpan);

            // TODO: check that rSpan is acceptable !!!!! (not 60% !!!)
            if (minRatio <= params.maxWidthRatio) {
                final Scale scale = sheet.getScale();

                if (logger.isDebugEnabled()) {
                    logger.info(
                            String.format(
                                    "%s width:%d %s dTotal:%dpx/%.2f(%d%%) dSpan:%dpx/%.2f(%d%%)",
                                    peak.toString(),
                                    width,
                                    side.toString(),
                                    dTotal,
                                    scale.pixelsToFrac(dTotal),
                                    (int) (rTotal * 100),
                                    dSpan,
                                    scale.pixelsToFrac(dSpan),
                                    (int) (rSpan * 100)));

                    for (StaffPeak partner : partners) {
                        logger.info("   {} width:{} ", partner, partner.getWidth());
                    }
                }

                // Check whether split would make sense
                int w1 = p1.getWidth();
                int w2 = p2.getWidth();
                double ratio = (double) w1 / (w1 + w2);
                int mid = peak.getStart() + (int) Math.rint(peak.getWidth() * ratio);

                if ((mid <= (peak.getStart() + 1)) || (mid >= (peak.getStop() - 1))) {
                    logger.debug("split of {} not feasible", peak);

                    continue; // Give up the split
                }

                // OK, let's go for a split
                map.put(side, partners);
            }
        }

        return map;
    }

    //---------------//
    // createSubPeak //
    //---------------//
    private StaffPeak createSubPeak (StaffPeak peak,
                                     int mid,
                                     Set<StaffPeak> impacted,
                                     HorizontalSide side)
    {
        final Staff staff = peak.getStaff();
        final StaffProjector projector = projectorOf(staff);
        final List<Section> allSections = new ArrayList<Section>(peak.getFilament().getMembers());

        StaffPeak p = new StaffPeak(
                staff,
                peak.getTop(),
                peak.getBottom(),
                (side == LEFT) ? peak.getStart() : (mid + 1),
                (side == LEFT) ? (mid - 1) : peak.getStop(),
                peak.getImpacts());

        Filament filament = filamentBuilder.buildFilament(
                p,
                params.bracketLookupExtension,
                allSections);

        if (filament == null) {
            return null;
        }

        p.setFilament(filament);

        return p;
    }

    //---------------//
    // createSystems //
    //---------------//
    /**
     * Build SystemInfo for each detected system.
     *
     * @param systemTops systems starting staves
     * @return the sequence of systems
     */
    private List<SystemInfo> createSystems (Integer[] systemTops)
    {
        final List<SystemInfo> newSystems = new ArrayList<SystemInfo>();
        Integer staffTop = null;
        int systemId = 0;
        SystemInfo system = null;

        for (int i = 0; i < staffManager.getStaffCount(); i++) {
            Staff staff = staffManager.getStaff(i);

            // System break?
            if ((staffTop == null) || (staffTop < systemTops[i])) {
                // Start of a new system
                staffTop = systemTops[i];

                system = new SystemInfo(++systemId, sheet, staffManager.getRange(staff, staff));
                newSystems.add(system);
            } else {
                // Continuing current system
                system.setStaves(staffManager.getRange(system.getFirstStaff(), staff));
            }
        }

        return newSystems;
    }

    //-------------------//
    // detectCurvedPeaks //
    //-------------------//
    /**
     * Detect curved filaments (brace portions or garbage) within peaks.
     * Wrong bar line peaks may result from mistakes on brace portion.
     * Such brace portions are characterized with:
     * - Short average curvature (we use this!)
     * - Low derivative
     * - Location on left side of the staff
     * - Small no-staff blank separation from rest of staff (but perhaps reduced to nothing)
     * - Significant thickness (well, not all)
     */
    private void detectCurvedPeaks ()
    {
        for (StaffProjector projector : projectors) {
            ///List<StaffPeak> toRemove = new ArrayList<StaffPeak>();
            for (StaffPeak peak : projector.getPeaks()) {
                Filament fil = (Filament) peak.getFilament();
                double curvature = fil.getMeanCurvature();

                if (curvature < params.minBarCurvature) {
                    if (peak.isVip()) {
                        logger.info("VIP removing curved {}", peak);
                    }

                    ///toRemove.add(peak);
                    peak.set(BRACE);
                }
            }

            //
            //            if (!toRemove.isEmpty()) {
            //                logger.debug("Staff#{} removing curved {}", projector.getStaff().getId(), toRemove);
            //                projector.removePeaks(toRemove);
            //            }
        }
    }

    //---------------------//
    // findAlignmentsAbove //
    //---------------------//
    /**
     * Lookup in the provided staff for one or several peaks compliant with (de-skewed)
     * peak abscissa.
     *
     * @param peak       the reference peak
     * @param staffAbove the staff above to be browsed for alignment with peak
     * @return the collection of alignments created
     */
    private List<BarAlignment> findAlignmentsAbove (StaffPeak peak,
                                                    Staff staffAbove)
    {
        List<BarAlignment> alignments = new ArrayList<BarAlignment>();

        for (StaffPeak peakAbove : projectorOf(staffAbove).getPeaks()) {
            BarAlignment alignment = checkAlignment(peakAbove, peak, true, true);

            if (alignment != null) {
                ///logger.debug("{}", alignment);
                addEdge(peakAbove, peak, alignment);
                alignments.add(alignment);
            }
        }

        return alignments;
    }

    //--------------------------------//
    // findAlignmentsAndConnectionsOf //
    //--------------------------------//
    private void findAlignmentsAndConnectionsOf (StaffPeak peak)
    {
        final Staff staff = peak.getStaff();

        // Above
        final List<Staff> stavesAbove = staffManager.vertNeighbors(staff, TOP);

        if (!stavesAbove.isEmpty() && (stavesAbove.get(0).isShort() == staff.isShort())) {
            for (Staff staffAbove : stavesAbove) {
                List<BarAlignment> alignments = findAlignmentsAbove(peak, staffAbove);

                for (BarAlignment alignment : alignments) {
                    checkConnection(alignment);
                }
            }
        }

        // Below
        final List<Staff> stavesBelow = staffManager.vertNeighbors(staff, BOTTOM);

        if (!stavesBelow.isEmpty() && (stavesBelow.get(0).isShort() == staff.isShort())) {
            for (Staff staffBelow : stavesBelow) {
                List<BarAlignment> alignments = findAlignmentsBelow(peak, staffBelow);

                for (BarAlignment alignment : alignments) {
                    checkConnection(alignment);
                }
            }
        }
    }

    //---------------------//
    // findAlignmentsBelow //
    //---------------------//
    /**
     * Lookup in the provided staff for one or several peaks compliant with (de-skewed)
     * peak abscissa.
     *
     * @param peak       the reference peak
     * @param staffBelow the staff below to be browsed for alignment with peak
     * @return the collection of alignments created
     */
    private List<BarAlignment> findAlignmentsBelow (StaffPeak peak,
                                                    Staff staffBelow)
    {
        List<BarAlignment> alignments = new ArrayList<BarAlignment>();

        for (StaffPeak peakBelow : projectorOf(staffBelow).getPeaks()) {
            BarAlignment alignment = checkAlignment(peak, peakBelow, true, true);

            if (alignment != null) {
                logger.debug("{}", alignment);
                addEdge(peak, peakBelow, alignment);
                alignments.add(alignment);
            }
        }

        return alignments;
    }

    //-------------------//
    // findAllAlignments //
    //-------------------//
    /**
     * Find all peak alignments across staves.
     */
    private void findAllAlignments ()
    {
        // Check for peaks aligned across staves
        for (StaffProjector projector : projectors) {
            final Staff staff = projector.getStaff();
            final List<Staff> stavesBelow = staffManager.vertNeighbors(staff, BOTTOM);

            // Make sure there are other staves on this side and they are "short-wise compatible"
            // with current staff
            if (stavesBelow.isEmpty() || (stavesBelow.get(0).isShort() != staff.isShort())) {
                continue;
            }

            for (StaffPeak peak : projector.getPeaks()) {
                for (Staff staffBelow : stavesBelow) {
                    findAlignmentsBelow(peak, staffBelow);
                }
            }
        }
    }

    //--------------//
    // findBarPeaks //
    //--------------//
    /**
     * Use individual staff projections to retrieve bar peaks.
     */
    private void findBarPeaks ()
    {
        // Analysis staff per staff
        for (Staff staff : staffManager.getStaves()) {
            StaffProjector projector = new StaffProjector(sheet, staff, this);
            projectors.add(projector);
            projector.process();
            Graphs.addAllVertices(this, projector.getPeaks());
        }
    }

    //-----------------//
    // findConnections //
    //-----------------//
    /**
     * Find all peak concrete connections across staves.
     */
    private void findConnections ()
    {
        // Check among the alignments for peaks connected across staves
        for (BarAlignment alignment : new ArrayList<BarAlignment>(edgeSet())) {
            // Look for concrete connection
            checkConnection(alignment);
        }
    }

    //-------------------//
    // getConnectedPeaks //
    //-------------------//
    /**
     * Report the peaks connected with the provided peak in the next staff on desired
     * direction.
     *
     * @param peak the provided peaks
     * @param side which staff to browse
     * @return the sequence of connected peaks, perhaps empty
     */
    private List<StaffPeak> getConnectedPeaks (StaffPeak peak,
                                               VerticalSide side)
    {
        int degree = (side == TOP) ? inDegreeOf(peak) : outDegreeOf(peak);

        if (degree > 0) {
            SortedSet<StaffPeak> others = new TreeSet<StaffPeak>();
            Set<BarAlignment> edges = (side == TOP) ? incomingEdgesOf(peak) : outgoingEdgesOf(
                    peak);

            for (BarAlignment edge : edges) {
                if (edge instanceof BarConnection) {
                    others.add((side == TOP) ? getEdgeSource(edge) : getEdgeTarget(edge));
                }
            }

            return new ArrayList<StaffPeak>(others);
        }

        return Collections.EMPTY_LIST;
    }

    //----------------//
    // getConnections //
    //----------------//
    private List<BarConnection> getConnections ()
    {
        List<BarConnection> list = new ArrayList<BarConnection>();

        for (BarAlignment align : edgeSet()) {
            if (align instanceof BarConnection) {
                list.add((BarConnection) align);
            }
        }

        Collections.sort(list);

        return list;
    }

    //-------------//
    // getGroupsOf //
    //-------------//
    /**
     * Report the sequence of (close) groups of peaks found in provided projector.
     *
     * @param projector the staff projector to process
     * @return the detected groups
     */
    private List<List<StaffPeak>> getGroupsOf (StaffProjector projector)
    {
        final List<List<StaffPeak>> groups = new ArrayList<List<StaffPeak>>();
        final List<StaffPeak> peaks = projector.getPeaks();
        int ig = -1; // Index of start of group

        for (int i = 1; i < peaks.size(); i++) {
            final int gap = peaks.get(i).getStart() - peaks.get(i - 1).getStop() - 1;

            if (gap <= params.maxCloseGap) {
                if (ig == -1) {
                    ig = i - 1; // Group start
                }
            } else if (ig != -1) {
                groups.add(peaks.subList(ig, i)); // Group has ended
                ig = -1;
            }
        }

        if (ig != -1) {
            groups.add(peaks.subList(ig, peaks.size())); // Group end
        }

        return groups;
    }

    //------------------//
    // getMaxPeaksWidth //
    //------------------//
    /**
     * Retrieve the maximum width among all peaks.
     *
     * @return the maximum width
     */
    private int getMaxPeaksWidth ()
    {
        int maxWidth = 0;

        for (StaffProjector projector : projectors) {
            for (StaffPeak peak : projector.getPeaks()) {
                if (!(peak.isBrace())) {
                    maxWidth = Math.max(maxWidth, peak.getWidth());
                }
            }
        }

        return maxWidth;
    }

    //-----------------//
    // getPeaksToSplit //
    //-----------------//
    /**
     * Among the provided peaks, detect the ones for which a split would make sense.
     *
     * @param peaks the peaks to inspect
     * @return the peaks to split
     */
    private Set<StaffPeak> getPeaksToSplit (Collection<StaffPeak> peaks)
    {
        Set<StaffPeak> toSplit = new LinkedHashSet<StaffPeak>();

        for (StaffPeak peak : peaks) {
            Map<VerticalSide, List<StaffPeak>> map = checkForSplit(peak);

            if (!map.isEmpty()) {
                toSplit.add(peak);
            }
        }

        logger.debug("toSplit: {}", toSplit);

        return toSplit;
    }

    //--------------------//
    // getSectionsByWidth //
    //--------------------//
    /**
     * Select relevant sections for bar sticks.
     * <p>
     * Both vertical and horizontal sections are OK if they are not wider than the maximum allowed.
     * The global collection is sorted on abscissa.
     *
     * @param maxWidth maximum section horizontal width
     * @return the abscissa-sorted list of compliant sections
     */
    private List<Section> getSectionsByWidth (int maxWidth)
    {
        List<Section> sections = new ArrayList<Section>();
        Lag hLag = sheet.getLagManager().getLag(Lags.HLAG);
        Lag vLag = sheet.getLagManager().getLag(Lags.VLAG);

        for (Lag lag : Arrays.asList(vLag, hLag)) {
            for (Section section : lag.getEntities()) {
                if (section.getLength(HORIZONTAL) <= maxWidth) {
                    sections.add(section);
                }
            }
        }

        Collections.sort(sections, Section.byAbscissa);

        return sections;
    }

    //---------------//
    // getSystemTops //
    //---------------//
    /**
     * Use connections across staves to gather staves into systems.
     *
     * @return the system top, per staff
     */
    private Integer[] getSystemTops ()
    {
        // For each staff, gives the staff that starts the containing system
        final Integer[] systemTops = new Integer[staffManager.getStaffCount()];

        // Connections are ordered per top staff then per abscissa.
        for (BarConnection connection : getConnections()) {
            logger.debug("{}", connection);

            final StaffPeak p1 = connection.topPeak;
            final StaffPeak p2 = connection.bottomPeak;
            final int top = p1.getStaff().getId();
            final int bottom = p2.getStaff().getId();

            if (systemTops[top - 1] == null) {
                systemTops[top - 1] = top;
            }

            if (systemTops[bottom - 1] == null) {
                // First connection ever between the 2 staves
                // Check it is not located too far on right after staff left abscissa
                // TODO: What if very first connection is missing but we have more on right?
                // Answer: check connection on staff right side as a second chance...
                final int xOffset = p2.getStart() - p2.getStaff().getAbscissa(LEFT);

                if (xOffset > params.maxFirstConnectionXOffset) {
                    if ((p2 == projectorOf(p2.getStaff()).getLastPeak())
                        || !areRightConnected(p1.getStaff(), p2.getStaff())) {
                        continue;
                    }
                }
            }

            systemTops[bottom - 1] = systemTops[top - 1];
        }

        // Complete assignments
        for (int i = 1; i <= systemTops.length; i++) {
            if (systemTops[i - 1] == null) {
                systemTops[i - 1] = i;
            }
        }

        final int[] ids = new int[staffManager.getStaffCount()];

        for (int i = 0; i < ids.length; i++) {
            ids[i] = i + 1;
        }

        logger.debug("Staves:  {}", ids);
        logger.debug("Systems: {}", (Object) systemTops);

        return systemTops;
    }

    //---------//
    // groupOf //
    //---------//
    /**
     * Report the sequence of close peaks, the provided peaks are part of.
     *
     * @param peaks the provided peaks
     * @return the sequence these peak(s) belong to
     */
    private List<StaffPeak> groupOf (List<StaffPeak> peaks)
    {
        if (peaks.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        final StaffPeak first = peaks.get(0);
        final List<StaffPeak> all = projectorOf(first.getStaff()).getPeaks(); // All peaks in staff
        final int i1 = all.indexOf(first);
        int iMin = i1;
        StaffPeak prevPeak = first;

        for (int i = i1 - 1; i >= 0; i--) {
            StaffPeak p = all.get(i);
            int gap = prevPeak.getStart() - p.getStop() + 1;

            if (gap > params.maxCloseGap) {
                break;
            }

            iMin = i;
            prevPeak = p;
        }

        final StaffPeak last = peaks.get(peaks.size() - 1);
        final int i2 = all.indexOf(last);
        int iMax = i2;

        for (int i = i2 + 1; i < all.size(); i++) {
            StaffPeak p = all.get(i);
            int gap = p.getStart() - prevPeak.getStop() + 1;

            if (gap > params.maxCloseGap) {
                break;
            }

            iMax = i;
            prevPeak = p;
        }

        return all.subList(iMin, iMax + 1);
    }

    //-------------//
    // projectorOf //
    //-------------//
    private StaffProjector projectorOf (Staff staff)
    {
        return projectors.get(staff.getId() - 1);
    }

    //----------------//
    // pruneGroupPair //
    //----------------//
    /**
     * Given two (roughly) aligned groups, of identical size, make sure their individual
     * connections are OK.
     *
     * @param g1 group in top staff
     * @param g2 group in bottom staff
     */
    private void pruneGroupPair (List<StaffPeak> g1,
                                 List<StaffPeak> g2)
    {
        logger.debug("groupPair");
        logger.debug("      top:{}", g1);
        logger.debug("   bottom:{}", g2);

        for (int i = 0; i < g1.size(); i++) {
            StaffPeak p1 = g1.get(i);
            StaffPeak p2 = g2.get(i);

            for (BarAlignment align : new ArrayList<BarAlignment>(outgoingEdgesOf(p1))) {
                if (getEdgeTarget(align) != p2) {
                    removeEdge(align);
                }
            }

            for (BarAlignment align : new ArrayList<BarAlignment>(incomingEdgesOf(p2))) {
                if (getEdgeSource(align) != p1) {
                    removeEdge(align);
                }
            }
        }
    }

    //-----------------//
    // purgeAlignments //
    //-----------------//
    /**
     * Purge the alignments and connections.
     * <p>
     * In set of alignment / connections a peak should appear at most once as top and at most once
     * as bottom.
     * <ul>
     * <li>Remove any alignment that conflicts with a connection.
     * Any connection is given priority against conflicting alignment (simply because connection was
     * validated by presence of enough black pixels in the inter-staff region).
     * <li>Remove duplicates: in the collection of alignments, a peak should appear at most once as
     * top and at most once as bottom. In case of conflict, use alignment quality to disambiguate.
     * </ul>
     */
    private void purgeAlignments ()
    {
        Set<BarAlignment> toRemove = new HashSet<BarAlignment>();

        for (StaffPeak peak : vertexSet()) {
            if (peak.isVip()) {
                logger.info("VIP running purgeAlignments for {}", peak);
            }

            if (inDegreeOf(peak) > 1) {
                List<BarAlignment> edges = new ArrayList<BarAlignment>(incomingEdgesOf(peak));
                edges.remove(BarAlignment.bestOf(edges, TOP));
                toRemove.addAll(edges);
            }

            if (outDegreeOf(peak) > 1) {
                List<BarAlignment> edges = new ArrayList<BarAlignment>(outgoingEdgesOf(peak));
                edges.remove(BarAlignment.bestOf(edges, BOTTOM));
                toRemove.addAll(edges);
            }
        }

        if (!toRemove.isEmpty()) {
            logger.debug("Purging alignments {}", toRemove);
            removeAllEdges(toRemove);
        }
    }

    //----------------------//
    // purgeCrossAlignments //
    //----------------------//
    /**
     * Only alignments within a system are meaningful.
     * So, alignments across systems must be removed.
     */
    private void purgeCrossAlignments ()
    {
        final Set<BarAlignment> toRemove = new HashSet<BarAlignment>();

        for (BarAlignment alignment : edgeSet()) {
            final SystemInfo s1 = alignment.getPeak(TOP).getStaff().getSystem();
            final SystemInfo s2 = alignment.getPeak(BOTTOM).getStaff().getSystem();

            if (s1 != s2) {
                toRemove.add(alignment);
            }
        }

        if (!toRemove.isEmpty()) {
            logger.debug("Purging cross-system alignments{}", toRemove);
            removeAllEdges(toRemove);
        }
    }

    //-------------------//
    // rectifyAlignments //
    //-------------------//
    /**
     * Remove any alignment between the two groups of same size, and link each peak in
     * top group with the corresponding peak in bottom group.
     *
     * @param topGroup    group of peaks in top staff
     * @param bottomGroup group of peaks in bottom staff
     */
    private void rectifyAlignments (List<StaffPeak> topGroup,
                                    List<StaffPeak> bottomGroup)
    {
        for (int i = 0; i < topGroup.size(); i++) {
            StaffPeak top = topGroup.get(i);
            StaffPeak bottom = bottomGroup.get(i);
            removeAllEdges(new ArrayList<BarAlignment>(outgoingEdgesOf(top)));
            removeAllEdges(new ArrayList<BarAlignment>(incomingEdgesOf(bottom)));
            addEdge(top, bottom, checkAlignment(top, bottom, false, false));
        }
    }

    //-----------------//
    // solveAlignments //
    //-----------------//
    /**
     * Solve conflicting alignments that result from merged peaks.
     *
     * @return the number of split performed
     */
    private int splitMergedGroups ()
    {
        int count = 0;
        Set<StaffPeak> toSplit = getPeaksToSplit(new ArrayList<StaffPeak>(vertexSet()));

        while (!toSplit.isEmpty()) {
            Set<StaffPeak> impacted = new LinkedHashSet<StaffPeak>();

            for (StaffPeak peak : toSplit) {
                if (splitPeak(peak, impacted)) {
                    count++;
                }
            }

            logger.debug("impacted: {}", impacted);
            toSplit = getPeaksToSplit(impacted);
        }

        return count;
    }

    //-----------//
    // splitPeak //
    //-----------//
    /**
     * If the provided peak is still relevant for a split, perform the split and
     * populate the 'impacted' set with the peaks somehow impacted by the split.
     *
     * @param peak     the peak to split
     * @param impacted (output) to be populated by impacted peaks
     * @return true if split was actually performed
     */
    private boolean splitPeak (StaffPeak peak,
                               Set<StaffPeak> impacted)
    {
        // Check this peak is still relevant for a split
        Map<VerticalSide, List<StaffPeak>> map = checkForSplit(peak);

        if (map.isEmpty()) {
            return false;
        }

        // Determine split ratio (we assume exactly 2 partners)
        Double splitRatio = null;

        for (List<StaffPeak> partners : map.values()) {
            impacted.addAll(partners);

            int w1 = partners.get(0).getWidth();
            int w2 = partners.get(partners.size() - 1).getWidth();
            double ratio = (double) w1 / (w1 + w2);

            if (splitRatio == null) {
                splitRatio = ratio;
            } else {
                splitRatio = (splitRatio + ratio) / 2;
            }
        }

        impacted.remove(peak);

        // Split old peak into 2 new sub-peaks
        int mid = peak.getStart() + (int) Math.rint(peak.getWidth() * splitRatio);
        StaffPeak p1 = createSubPeak(peak, mid, impacted, LEFT); // May fail
        StaffPeak p2 = createSubPeak(peak, mid, impacted, RIGHT); // May fail

        if ((p1 == null) || (p2 == null)) {
            return false;
        }

        final Staff staff = peak.getStaff();
        final StaffProjector projector = projectorOf(staff);

        for (StaffPeak p : new StaffPeak[]{p1, p2}) {
            p.computeDeskewedCenter(sheet.getSkew());
            projector.insertPeak(p, peak);
            findAlignmentsAndConnectionsOf(p);
            impacted.add(p);

            for (BarAlignment edge : incomingEdgesOf(p)) {
                impacted.add(getEdgeSource(edge));
            }

            for (BarAlignment edge : outgoingEdgesOf(p)) {
                impacted.add(getEdgeTarget(edge));
            }
        }

        // Purge connections within peak groups
        final List<StaffPeak> newPeaks = Arrays.asList(p1, p2);

        for (Entry<VerticalSide, List<StaffPeak>> entry : map.entrySet()) {
            VerticalSide side = entry.getKey();
            List<StaffPeak> partners = entry.getValue();

            if (side == TOP) {
                pruneGroupPair(partners, newPeaks);
            } else {
                pruneGroupPair(newPeaks, partners);
            }
        }

        logger.info("Split {} into {} and {}", peak, p1, p2);

        // This is the end...
        projector.removePeak(peak);

        return true;
    }

    //------------//
    // totalWidth //
    //------------//
    /**
     * Cumulate the width of the provided peaks.
     *
     * @param peaks the provided peaks
     * @return the cumulated width
     */
    private int totalWidth (List<StaffPeak> peaks)
    {
        int total = 0;

        for (StaffPeak peak : peaks) {
            total += peak.getWidth();
        }

        return total;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch?");

        private final Constant.Ratio maxAlignmentSlope = new Constant.Ratio(
                0.06, //0.04,
                "Max slope for bar alignment");

        private final Scale.Fraction maxAlignmentDeltaWidth = new Scale.Fraction(
                0.6,
                "Max delta width for bar alignment");

        private final Scale.Fraction maxAlignmentBraceDx = new Scale.Fraction(
                0.75,
                "Max abscissa shift for brace alignment");

        private final Scale.Fraction maxConnectionGap = new Scale.Fraction(
                2.0,
                "Max vertical gap when connecting bar lines");

        private final Constant.Ratio maxConnectionWhiteRatio = new Constant.Ratio(
                0.35,
                "Max white ratio when connecting bar lines");

        private final Constant.Ratio minConnectionGrade = new Constant.Ratio(
                0.5,
                "Minimum grade for a true connection");

        private final Scale.Fraction minBarCurvature = new Scale.Fraction(
                10,
                "Minimum mean curvature radius for a bar line");

        private final Constant.Ratio maxWidthRatio = new Constant.Ratio(
                0.3,
                "Max width difference ratio between aligned peaks");

        private final Constant.Ratio maxTotalDiffRatio = new Constant.Ratio(
                0.3,
                "Max width difference ratio between aligned groups of peaks");

        private final Scale.Fraction bracketLookupExtension = new Scale.Fraction(
                2.0,
                "Lookup height for bracket end above or below staff line");

        private final Scale.Fraction maxCloseGap = new Scale.Fraction(
                0.4,
                "Max horizontal gap between two close members of a double bar");

        private final Scale.Fraction maxFirstConnectionXOffset = new Scale.Fraction(
                2.0,
                "Max horizontal offset between staff start and first connection");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final double maxAlignmentSlope;

        final int maxAlignmentDeltaWidth;

        final int maxAlignmentBraceDx;

        final int minBarCurvature;

        final int maxConnectionGap;

        final double maxConnectionWhiteRatio;

        final double minConnectionGrade;

        final double maxWidthRatio;

        final double maxTotalDiffRatio;

        final int bracketLookupExtension;

        final int maxCloseGap;

        final int maxFirstConnectionXOffset;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            maxAlignmentSlope = constants.maxAlignmentSlope.getValue();
            maxAlignmentDeltaWidth = scale.toPixels(constants.maxAlignmentDeltaWidth);
            maxAlignmentBraceDx = scale.toPixels(constants.maxAlignmentBraceDx);
            minBarCurvature = scale.toPixels(constants.minBarCurvature);
            maxConnectionGap = scale.toPixels(constants.maxConnectionGap);
            maxConnectionWhiteRatio = constants.maxConnectionWhiteRatio.getValue();
            minConnectionGrade = constants.minConnectionGrade.getValue();
            maxWidthRatio = constants.maxWidthRatio.getValue();
            maxTotalDiffRatio = constants.maxTotalDiffRatio.getValue();
            bracketLookupExtension = scale.toPixels(constants.bracketLookupExtension);
            maxCloseGap = scale.toPixels(constants.maxCloseGap);
            maxFirstConnectionXOffset = scale.toPixels(constants.maxFirstConnectionXOffset);

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
