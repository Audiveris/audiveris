//----------------------------------------------------------------------------//
//                                                                            //
//                         B a r s R e t r i e v e r                          //
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

import omr.glyph.GlyphLayer;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.lag.BasicLag;
import omr.lag.JunctionShiftPolicy;
import omr.lag.Lag;
import omr.lag.Lags;
import omr.lag.Section;
import omr.lag.SectionsBuilder;

import omr.math.AreaUtil;
import omr.math.AreaUtil.CoreData;
import omr.math.BasicLine;
import omr.math.GeoPath;
import omr.math.Histogram;
import omr.math.NaturalSpline;
import static omr.run.Orientation.*;
import omr.run.RunsTable;

import omr.sheet.PartInfo;
import omr.sheet.Picture;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Skew;
import omr.sheet.SystemInfo;

import omr.sig.BarConnectionInter;
import omr.sig.BarlineInter;
import omr.sig.GradeImpacts;
import omr.sig.SIGraph;

import omr.step.StepException;

import omr.ui.Colors;
import omr.ui.util.ItemRenderer;
import omr.ui.util.UIUtil;

import omr.util.Navigable;
import omr.util.StopWatch;
import omr.util.VerticalSide;
import static omr.util.VerticalSide.*;
import omr.util.VipUtil;
import omr.util.WrappedInteger;

import ij.process.ByteProcessor;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code BarsRetriever} focuses on the retrieval of vertical
 * bar lines.
 * <p>
 * Bar lines are used to:
 * <ul>
 * <li>Determine the gathering of staves into systems and parts</li>
 * <li>Determine measures</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class BarsRetriever
    implements ItemRenderer
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants       constants = new Constants();
    private static final Logger          logger = LoggerFactory.getLogger(
        BarsRetriever.class);

    //~ Instance fields --------------------------------------------------------

    //
    /** Related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Global sheet scale. */
    @Navigable(false)
    private final Scale scale;

    /** Scale-dependent constants. */
    private final Parameters params;

    /** Lag of vertical runs. */
    private Lag vLag;

    /** Related staff manager. */
    private final StaffManager staffManager;

    /** Staff projectors. */
    private final List<StaffProjector> projectors = new ArrayList<StaffProjector>();

    /** All alignments found between bars across staves. */
    private final Set<BarAlignment> alignments = new LinkedHashSet<BarAlignment>();

    /** All connections found between bars across staves. */
    private final Set<BarConnection> connections = new LinkedHashSet<BarConnection>();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // BarsRetriever //
    //---------------//
    /**
     * Retrieve the bar lines of all staves.
     *
     * @param sheet the sheet to process
     */
    public BarsRetriever (Sheet sheet)
    {
        this.sheet = sheet;

        scale = sheet.getScale();

        // Scale-dependent parameters
        params = new Parameters(scale);

        // Companions
        staffManager = sheet.getStaffManager();
    }

    //~ Methods ----------------------------------------------------------------

    //
    //------------------//
    // buildVerticalLag //
    //------------------//
    /**
     * Build the underlying vertical lag, from the provided runs table.
     * This method must be called before building info.
     *
     * @param longVertTable the provided table of (long) vertical runs
     */
    public void buildVerticalLag (RunsTable longVertTable)
    {
        vLag = new BasicLag(Lags.VLAG, VERTICAL);

        SectionsBuilder sectionsBuilder = new SectionsBuilder(
            vLag,
            new JunctionShiftPolicy(params.maxRunShift));
        sectionsBuilder.createSections(longVertTable, true);

        sheet.setLag(Lags.VLAG, vLag);

        setVipSections();
    }

    //------//
    // plot //
    //------//
    public void plot (StaffInfo staff)
    {
        final int index = staff.getId() - 1;

        if (index < projectors.size()) {
            projectors.get(index)
                      .plot();
        }
    }

    //-------------//
    // renderItems //
    //-------------//
    /**
     * Render the bar lines found.
     *
     * @param g graphics context
     */
    @Override
    public void renderItems (Graphics2D g)
    {
        if (!constants.showVerticalLines.isSet()) {
            return;
        }

        final Rectangle clip = g.getClipBounds();
        final Stroke    oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
        final Color     oldColor = g.getColor();
        g.setColor(Colors.ENTITY_MINOR);

        // Draw bar lines (only within staff height)
        for (StaffInfo staff : staffManager.getStaves()) {
            for (BarPeak peak : staff.getBarPeaks()) {
                Rectangle peakBox = new Rectangle(
                    peak.getStart(),
                    peak.getTop(),
                    peak.getWidth(),
                    peak.getBottom() - peak.getTop());

                if (clip.intersects(peakBox)) {
                    double xMid = (peak.getStart() + peak.getStop()) / 2d;
                    Line2D line = new Line2D.Double(
                        xMid,
                        peak.getTop(),
                        xMid,
                        peak.getBottom());
                    g.draw(line);
                }
            }
        }

        // Draw Connections (outside of staff height)
        for (BarConnection connection : connections) {
            Line2D median = connection.getMedian();

            if (median.intersects(clip)) {
                g.draw(median);
            }
        }

        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    //------------------//
    // retrieveBarlines //
    //------------------//
    /**
     * Retrieve all bar lines in the sheet and create systems and
     * parts indicated by bar lines that connect across staves.
     *
     * @throws StepException raised if processing must stop
     */
    public void retrieveBarlines ()
        throws StepException
    {
        // Individual staff analysis to find bar peaks
        findBarPeaks();

        // Build core glyph for each peak
        buildBarSticks();

        // Find all bar alignments (& connections) across staves
        findAlignments();

        // Purge alignments incompatible with connections
        purgeAlignments();

        // Purge long peaks that do not connect staves
        purgeLongPeaks();

        // Refine all staff side abscissae
        refineSides();

        // Create systems & parts from bar connections
        createSystemsAndParts();

        // Purge alignments across systems, they are not relevant
        purgeCrossAlignments();

        // Create barline interpretations within each system
        createBarlineInters();

        // Create bar connection across staves
        createBarConnectionInters();
    }

    //--------------//
    // alignedPeaks //
    //--------------//
    /**
     * Report the BarPeak instance(s) that are aligned with the
     * provided peak, looking in the provided vertical side.
     *
     * @param peak the peak to check from
     * @param side which side to look (from provided peak)
     * @return the collection of peaks found, perhaps empty
     */
    private List<BarPeak> alignedPeaks (BarPeak      peak,
                                        VerticalSide side)
    {
        final List<BarPeak> found = new ArrayList<BarPeak>();

        for (BarAlignment alignment : alignmentsOf(peak, side.opposite())) {
            found.add(alignment.getPeak(side));
        }

        return found;
    }

    //--------------//
    // alignmentsOf //
    //--------------//
    /**
     * Report the collection of alignments for which the provided peak
     * is involved on desired vertical side.
     *
     * @param peak the peak to check for
     * @param side the desired vertical side
     * @return the collection found, perhaps empty.
     */
    private Set<BarAlignment> alignmentsOf (BarPeak      peak,
                                            VerticalSide side)
    {
        Set<BarAlignment> found = new HashSet<BarAlignment>();

        for (BarAlignment alignment : alignments) {
            if (alignment.getPeak(side) == peak) {
                found.add(alignment);
            }
        }

        return found;
    }

    //----------------//
    // buildBarSticks //
    //----------------//
    /**
     * Bar line sticks are needed to detect those which go past staff
     * height above, below, or both.
     */
    private void buildBarSticks ()
    {
        WrappedInteger maxThickWidth = new WrappedInteger(0);
        WrappedInteger maxThinWidth = new WrappedInteger(0);
        getMaxWidths(maxThickWidth, maxThinWidth);

        if (maxThickWidth.value != 0) {
            buildSticks(maxThickWidth.value, false);
        }

        if (maxThinWidth.value != 0) {
            buildSticks(maxThinWidth.value, true);
        }
    }

    //-------------//
    // buildSticks //
    //-------------//
    /**
     * Build sticks corresponding to retrieved bar peaks of a given
     * kind (all thin peaks or all thick peaks).
     * <p>
     * For each peak, we take a vertical "slice" of the relevant sections.
     * We run the filament factory (with proper thickness value) on the sections
     * and make it focus on bar target line.
     *
     * @param maxWidth value to use as maximum width
     * @param isThin   true to process only thin bars, false to process only
     *                 thick ones
     */
    private void buildSticks (int     maxWidth,
                              boolean isThin)
    {
        StopWatch watch = new StopWatch(isThin ? "THIN" : "THICK");
        watch.start("buildSticks()");

        // Preselect sections of proper max width (thin or thick)
        List<Section> allSections = getSectionsByWidth(maxWidth);
        logger.debug("isThin:{} sections:{}", isThin, allSections.size());

        FilamentsFactory factory = new FilamentsFactory(
            sheet.getScale(),
            sheet.getNest(),
            GlyphLayer.DEFAULT,
            VERTICAL,
            Filament.class);

        // Factory parameters adjustment
        factory.setMaxCoordGap(constants.maxCoordGap);
        factory.setMaxPosGap(constants.maxPosGap);
        factory.setMaxOverlapSpace(constants.maxOverlapSpace);
        factory.setMaxOverlapDeltaPos(constants.maxOverlapDeltaPos);
        factory.setMaxThickness(maxWidth);

        for (StaffInfo staff : staffManager.getStaves()) {
            for (BarPeak peak : staff.getBarPeaks()) {
                if (peak.isThin() == isThin) {
                    // Take proper slice of sections for this peak
                    List<Section> sections = getPeakSections(peak, allSections);
                    double        xMid = (peak.getStart() + peak.getStop()) / 2d;
                    NaturalSpline line = NaturalSpline.interpolate(
                        new Point2D.Double(xMid, peak.getTop()),
                        new Point2D.Double(xMid, peak.getBottom()));
                    List<Glyph>   glyphs = factory.retrieveFilaments(
                        sections,
                        Arrays.asList(line));

                    // By construction we should have exactly 1 glyph built
                    if (!glyphs.isEmpty()) {
                        Glyph glyph = glyphs.get(0);
                        peak.setGlyph(glyph);

                        // Check whether the glyph gets above or below the staff
                        Rectangle glyphBox = glyph.getBounds();

                        if ((peak.getTop() - glyphBox.y) > params.maxBarExtension) {
                            peak.setAbove();
                        }

                        if (((glyphBox.y + glyphBox.height) - 1 -
                            peak.getBottom()) > params.maxBarExtension) {
                            peak.setBelow();
                        }
                    }

                    logger.debug("Staff#{} {}", staff.getId(), peak);
                }
            }
        }

        ///watch.print();
    }

    //-----------------//
    // checkConnection //
    //-----------------//
    /**
     * Check whether the provided alignment is a true connection, that
     * is with concrete foreground pixels on the alignment line.
     * <p>
     * For this, we define a spline which goes through top & bottom points of
     * each of the aligned peaks and look for pixels in the area.
     *
     * @param alignment the alignment to check.
     * @return the connection if OK, null otherwise
     */
    private BarConnection checkConnection (BarAlignment alignment)
    {
        ByteProcessor  pixelFilter = sheet.getPicture()
                                          .getSource(Picture.SourceKey.BINARY);
        BarPeak        p1 = alignment.topPeak;
        BarPeak        p2 = alignment.bottomPeak;

        // Theoretical lines on left and right sides
        final GeoPath  leftLine = new GeoPath(
            new Line2D.Double(
                new Point2D.Double(p1.getStart(), p1.getBottom()),
                new Point2D.Double(p2.getStart(), p2.getTop())));
        final GeoPath  rightLine = new GeoPath(
            new Line2D.Double(
                new Point2D.Double(p1.getStop(), p1.getBottom()),
                new Point2D.Double(p2.getStop(), p2.getTop())));

        final CoreData data = AreaUtil.verticalCore(
            pixelFilter,
            leftLine,
            rightLine);

        logger.debug("{}", data);

        if ((data.gap <= params.maxConnectionGap) &&
            (data.whiteRatio <= params.maxConnectionWhiteRatio)) {
            double       whiteImpact = 1 -
                                       (data.whiteRatio / params.maxConnectionWhiteRatio);
            double       gapImpact = 1 -
                                     ((double) data.gap / params.maxConnectionGap);
            double       alignImpact = alignment.getImpacts()
                                                .getGrade() / alignment.getImpacts()
                                                                       .getIntrinsicRatio();
            GradeImpacts impacts = new BarConnection.Impacts(
                alignImpact,
                whiteImpact,
                gapImpact);

            return new BarConnection(alignment, impacts);
        } else {
            return null;
        }
    }

    //---------------------------//
    // createBarConnectionInters //
    //---------------------------//
    private void createBarConnectionInters ()
    {
        for (BarConnection connection : connections) {
            BarPeak            peak = connection.topPeak;
            SystemInfo         system = peak.getStaff()
                                            .getSystem();
            SIGraph            sig = system.getSig();
            BarConnectionInter inter = new BarConnectionInter(
                connection,
                peak.isThin() ? Shape.THIN_CONNECTION : Shape.THICK_CONNECTION,
                connection.getImpacts());
            sig.addVertex(inter);
        }
    }

    //---------------------//
    // createBarlineInters //
    //---------------------//
    private void createBarlineInters ()
    {
        for (SystemInfo system : sheet.getSystems()) {
            SIGraph sig = system.getSig();

            for (StaffInfo staff : system.getStaves()) {
                for (BarPeak peak : staff.getBarPeaks()) {
                    BasicLine    median = new BasicLine(
                        new double[] {
                            (peak.getStart() + peak.getStop()) / 2d,
                            (peak.getStart() + peak.getStop()) / 2d
                        },
                        new double[] { peak.getTop(), peak.getBottom() });
                    BarlineInter inter = new BarlineInter(
                        peak.getGlyph(),
                        peak.isThin() ? Shape.THIN_BARLINE : Shape.THICK_BARLINE,
                        peak.getImpacts(),
                        median,
                        peak.getWidth());
                    sig.addVertex(inter);
                }
            }
        }
    }

    //-------------//
    // createParts //
    //-------------//
    /**
     * Create PartInfo for each part.
     *
     * @param partTops parts starting staves
     */
    private void createParts (Integer[] partTops)
    {
        for (SystemInfo system : sheet.getSystems()) {
            system.getParts()
                  .clear(); // Start from scratch

            int      partTop = -1;
            PartInfo part = null;

            for (StaffInfo staff : system.getStaves()) {
                int topId = partTops[staff.getId() - 1];

                if (topId != partTop) {
                    part = new PartInfo();
                    system.addPart(part);
                    partTop = topId;
                }

                part.addStaff(staff);
            }
        }
    }

    //---------------//
    // createSystems //
    //---------------//
    /**
     * Build SystemInfo of each system.
     *
     * @param systems starting staves
     * @return the sequence of systems
     */
    private List<SystemInfo> createSystems (Integer[] systemTops)
    {
        List<SystemInfo> newSystems = new ArrayList<SystemInfo>();
        Integer          staffTop = null;
        int              systemId = 0;
        SystemInfo       system = null;

        for (int i = 0; i < staffManager.getStaffCount(); i++) {
            StaffInfo staff = staffManager.getStaff(i);

            // System break?
            if ((staffTop == null) || (staffTop < systemTops[i])) {
                // Start of a new system
                staffTop = systemTops[i];

                system = new SystemInfo(
                    ++systemId,
                    sheet,
                    staffManager.getRange(staff, staff));
                newSystems.add(system);
            } else {
                // Continuing current system
                system.setStaves(
                    staffManager.getRange(system.getFirstStaff(), staff));
            }
        }

        return newSystems;
    }

    //-----------------------//
    // createSystemsAndParts //
    //-----------------------//
    /**
     * Gather staves per systems and parts and create the related
     * info instances.
     */
    private void createSystemsAndParts ()
    {
        final int       staffCount = staffManager.getStaffCount();

        // For each staff, gives the staff that starts the containing system
        final Integer[] systemTops = new Integer[staffCount];

        // For each staff, gives the staff that starts the containing part
        final Integer[] partTops = new Integer[staffCount];

        gatherStaves(systemTops, partTops);
        sheet.setSystems(createSystems(systemTops));
        createParts(partTops);
    }

    //----------------//
    // findAlignments //
    //----------------//
    private void findAlignments ()
    {
        // Check for bar peaks aligned across staves
        for (StaffInfo staff : staffManager.getStaves()) {
            for (VerticalSide side : VerticalSide.values()) {
                List<StaffInfo> otherStaves = staffManager.vertNeighbors(
                    staff,
                    side);

                // Make sure there are other staves on this side
                // and they are "short-wise compatible" with current staff
                if (otherStaves.isEmpty() ||
                    (otherStaves.get(0)
                                .isShort() != staff.isShort())) {
                    continue;
                }

                // Look for all alignment/connection relations
                for (BarPeak peak : staff.getBarPeaks()) {
                    // Look for a suitable partnering peak in stave(s) nearby
                    // Same peak kind: thin or thick
                    // Vertically aligned, taking sheet slope into account
                    for (StaffInfo otherStaff : otherStaves) {
                        lookupPeaks(peak, side, otherStaff);
                    }
                }
            }
        }

        // Check for bar peaks connected across staves
        for (Iterator<BarAlignment> it = alignments.iterator(); it.hasNext();) {
            BarAlignment  alignment = it.next();

            // Look for concrete connection
            BarConnection connection = checkConnection(alignment);

            if (connection != null) {
                connections.add(connection);
                // Remove the underlying alignment
                it.remove();
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
        for (StaffInfo staff : staffManager.getStaves()) {
            StaffProjector projector = new StaffProjector(sheet, staff);
            projectors.add(projector);
            projector.process();
        }
    }

    //--------------//
    // gatherStaves //
    //--------------//
    /**
     * Use connections across staves to gather staves into systems
     * and parts.
     * <p>
     * A first connection between two staves make them system partners.
     * A second connection between two staves makes them part partners, provided
     * that the second connection is sufficiently abscissa-shifted from the
     * first one.
     *
     * @param systemTops (output) systems starting staves
     * @param partTops   (output) parts starting staves
     */
    private void gatherStaves (Integer[] systemTops,
                               Integer[] partTops)
    {
        BarConnection prevConnection = null;

        // Connections are ordered per top staff then per abscissa.
        for (BarConnection connection : connections) {
            int top = connection.topPeak.getStaff()
                                        .getId();
            int bottom = connection.bottomPeak.getStaff()
                                              .getId();

            if (systemTops[top - 1] == null) {
                // First connection ever between the 2 staves
                systemTops[top - 1] = top;
            } else {
                // Is this a truely separate second connection?
                // Check horizontal gap with previous one
                int gap = connection.topPeak.getStart() -
                          prevConnection.topPeak.getStop() - 1;

                if (gap > params.maxDoubleBarGap) {
                    if (partTops[top - 1] == null) {
                        partTops[top - 1] = top;
                    }

                    partTops[bottom - 1] = partTops[top - 1];
                }
            }

            systemTops[bottom - 1] = systemTops[top - 1];
            prevConnection = connection;
        }

        // Complete assignments
        for (int i = 1; i <= systemTops.length; i++) {
            if (systemTops[i - 1] == null) {
                systemTops[i - 1] = i;
            }

            if (partTops[i - 1] == null) {
                partTops[i - 1] = i;
            }
        }

        final int[] ids = new int[staffManager.getStaffCount()];

        for (int i = 0; i < ids.length; i++) {
            ids[i] = i + 1;
        }

        logger.info("{}Staves:  {}", sheet.getLogPrefix(), ids);
        logger.info("{}Parts:   {}", sheet.getLogPrefix(), partTops);
        logger.info("{}Systems: {}", sheet.getLogPrefix(), systemTops);
    }

    //--------------//
    // getMaxWidths //
    //--------------//
    /**
     * Retrieve the maximum width of thick peaks and the maximum width
     * of thin peaks.
     *
     * @param maxThick (output) maximum width of thick peaks
     * @param maxThin  (output) maximum width of thin peaks
     */
    private void getMaxWidths (WrappedInteger maxThick,
                               WrappedInteger maxThin)
    {
        final Histogram<Integer> histo = new Histogram<Integer>();
        final SortedSet<Integer> thicks = new TreeSet<Integer>();
        final SortedSet<Integer> thins = new TreeSet<Integer>();

        for (StaffInfo staff : staffManager.getStaves()) {
            for (BarPeak peak : staff.getBarPeaks()) {
                int width = peak.getWidth();
                histo.increaseCount(width, 1);

                if (peak.isThin()) {
                    thins.add(width);
                } else {
                    thicks.add(width);
                }
            }
        }

        logger.debug("Peak width histo: {}", histo.dataString());
        logger.debug("Thick peak widths: {}", thicks);
        logger.debug("Thin  peak widths: {}", thins);

        if (!thicks.isEmpty()) {
            maxThick.value = thicks.last();
        }

        if (!thins.isEmpty()) {
            maxThin.value = thins.last();
        }
    }

    //-----------------//
    // getPeakSections //
    //-----------------//
    /**
     * Select proper candidate sections for the peak at hand.
     *
     * @param peak        the peak to process
     * @param allSections pre-filtered sheet-level collection
     * @return the sub-collection of relevant sections
     */
    private List<Section> getPeakSections (BarPeak       peak,
                                           List<Section> allSections)
    {
        final Rectangle peakBox = new Rectangle(
            peak.getStart(),
            peak.getTop(),
            peak.getWidth(),
            peak.getBottom() - peak.getTop() + 1);

        // These constants don't need precision and are large enough
        peakBox.grow(scale.getInterline() / 2, params.maxBarExtension);

        final int           xBreak = peakBox.x + peakBox.width;
        final List<Section> sections = new ArrayList<Section>();

        for (Section section : allSections) {
            final Rectangle sectionBox = section.getBounds();

            if (sectionBox.intersects(peakBox)) {
                sections.add(section);
            } else if (sectionBox.x >= xBreak) {
                break; // Since allSections are sorted by abscissa
            }
        }

        return sections;
    }

    //--------------------//
    // getSectionsByWidth //
    //--------------------//
    /**
     * Select relevant sections for specific kind of bar sticks.
     * <p>
     * Both vertical and horizontal sections are OK if they are not wider than
     * the maximum allowed.
     * The global collection is sorted on abscissa.
     * <p>
     * TODO: add some margin to maxWidth?
     *
     * @param maxWidth maximum section horizontal width
     * @return the abscissa-sorted list of compliant sections
     */
    private List<Section> getSectionsByWidth (int maxWidth)
    {
        List<Section> sections = new ArrayList<Section>();
        Lag           hLag = sheet.getLag(Lags.HLAG);

        for (Lag lag : Arrays.asList(vLag, hLag)) {
            for (Section section : lag.getSections()) {
                if (section.getLength(HORIZONTAL) <= maxWidth) {
                    sections.add(section);
                }
            }
        }

        Collections.sort(sections, Section.byAbscissa);

        return sections;
    }

    //-------------//
    // isConnected //
    //-------------//
    /**
     * Check whether the provided peak is connected on the provided
     * vertical side.
     *
     * @param peak the peak to check
     * @param side which vertical side to look for from peak
     * @return true if a compliant connection was found, false otherwise
     */
    private boolean isConnected (BarPeak      peak,
                                 VerticalSide side)
    {
        final VerticalSide opposite = side.opposite();

        for (BarConnection connection : connections) {
            if (connection.getPeak(opposite) == peak) {
                return true;
            }
        }

        return false;
    }

    //-------------//
    // lookupPeaks //
    //-------------//
    /**
     * Lookup in the provided staff for one or several peaks compliant
     * with (de-skewed) peak abscissa and peak kind.
     * This populates the 'alignments' set.
     *
     * @param peak       the reference peak
     * @param side       vertical side with respect to reference peak
     * @param otherStaff the other staff to be browsed for alignment with peak
     */
    private void lookupPeaks (BarPeak      peak,
                              VerticalSide side,
                              StaffInfo    otherStaff)
    {
        final Skew   skew = sheet.getSkew();
        final int    mid = (peak.getStart() + peak.getStop()) / 2;
        final double dsk = skew.deskewed(
            new Point(mid, peak.getOrdinate(side)))
                               .getX();

        for (BarPeak otherPeak : otherStaff.getBarPeaks()) {
            if (otherPeak.isThin() != peak.isThin()) {
                continue;
            }

            int    otherMid = (otherPeak.getStart() + otherPeak.getStop()) / 2;
            Point  otherPt = (side == TOP)
                             ? new Point(otherMid, otherPeak.getBottom())
                             : new Point(otherMid, otherPeak.getTop());
            double otherDsk = skew.deskewed(otherPt)
                                  .getX();
            double dx = scale.pixelsToFrac(otherDsk - dsk);

            if (Math.abs(dx) <= constants.maxAlignmentDx.getValue()) {
                double             alignImpact = 1 -
                                                 (Math.abs(dx) / constants.maxAlignmentDx.getValue());
                GradeImpacts       impacts = new BarAlignment.Impacts(
                    alignImpact);
                final BarAlignment alignment;

                if (side == TOP) {
                    alignment = new BarAlignment(otherPeak, peak, -dx, impacts);
                } else {
                    alignment = new BarAlignment(peak, otherPeak, dx, impacts);
                }

                alignments.add(alignment);
            }
        }
    }

    //-----------------//
    // purgeAlignments //
    //-----------------//
    /**
     * Purge the alignments collection.
     * <ul>
     * <li>Remove any alignment that conflicts with a connection.
     * Any connection is given priority against conflicting alignment (simply
     * because connection was validated by presence of enough black pixels in
     * the inter-staff region)</li>
     * <li>Remove duplicates: in the collection of alignments a peak can
     * appear at most once as top and at most once as bottom.
     * In case of conflict, use alignment quality to disambiguate.
     * TODO: A more complex approach to disambiguate could use detection of pair
     * of bars aligned with another pair of bars, and align left with left and
     * right with right. But is it worth the complexity?</li>
     * </ul>
     */
    private void purgeAlignments ()
    {
        // Purge alignments vs connections
        for (BarConnection connection : connections) {
            for (VerticalSide side : VerticalSide.values()) {
                for (BarAlignment alignment : alignmentsOf(
                    connection.getPeak(side),
                    side)) {
                    alignments.remove(alignment);
                    logger.info("Removed {}", alignment);
                }
            }
        }

        // Check duplicate alignments (looking to top & bottom)
        for (VerticalSide side : VerticalSide.values()) {
            Map<BarPeak, BarAlignment> map = new HashMap<BarPeak, BarAlignment>();
            Set<BarAlignment>          toRemove = new HashSet<BarAlignment>();

            for (BarAlignment alignment : alignments) {
                BarPeak      peak = alignment.getPeak(side);
                BarAlignment otherAlignment = map.get(peak);

                if (otherAlignment != null) {
                    // We have a conflict here, make a decision
                    logger.debug(
                        "Conflict {} vs {}",
                        alignment,
                        otherAlignment);

                    if (Math.abs(otherAlignment.dx) <= Math.abs(alignment.dx)) {
                        toRemove.add(alignment);
                    } else {
                        toRemove.add(otherAlignment);
                        map.put(peak, alignment);
                    }
                } else {
                    map.put(peak, alignment);
                }
            }

            if (!toRemove.isEmpty()) {
                logger.debug("Purging {}", toRemove);
                alignments.removeAll(toRemove);
            }
        }
    }

    //----------------------//
    // purgeCrossAlignments //
    //----------------------//
    /**
     * Only alignments within a system are meaningful.
     */
    private void purgeCrossAlignments ()
    {
        final Set<BarAlignment> toRemove = new HashSet<BarAlignment>();

        for (BarAlignment alignment : alignments) {
            final SystemInfo s1 = alignment.getPeak(TOP)
                                           .getStaff()
                                           .getSystem();
            final SystemInfo s2 = alignment.getPeak(BOTTOM)
                                           .getStaff()
                                           .getSystem();

            if (s1 != s2) {
                toRemove.add(alignment);
            }
        }

        if (!toRemove.isEmpty()) {
            logger.debug("Purging cross-system alignments{}", toRemove);
            alignments.removeAll(toRemove);
        }
    }

    //----------------//
    // purgeLongPeaks //
    //----------------//
    /**
     * Purge long thin bars (those getting above or below the related
     * staff) that do not connect staves.
     * Thick bars are not concerned by this test, because they cannot be
     * mistaken for stems and can appear to be extended because of brackets.
     * <p>
     * The check is relaxed for a bar which is aligned with another bar
     * that exhibits no such length problem.
     */
    private void purgeLongPeaks ()
    {
        final Set<BarPeak> toRemove = new LinkedHashSet<BarPeak>();

        for (StaffInfo staff : staffManager.getStaves()) {
            toRemove.clear();

            PeakLoop: 
            for (BarPeak peak : staff.getBarPeaks()) {
                // Thick bars are safe
                if (!peak.isThin()) {
                    continue;
                }

                for (VerticalSide side : VerticalSide.values()) {
                    if (peak.isBeyond(side) && !isConnected(peak, side)) {
                        List<BarPeak> partners = alignedPeaks(peak, side);

                        if (partners.size() == 1) {
                            BarPeak partner = partners.get(0);

                            if (!partner.isAbove() && !partner.isBelow()) {
                                // Consider this bar as safe
                                continue PeakLoop;
                            }
                        }

                        toRemove.add(peak);
                    }
                }
            }

            if (!toRemove.isEmpty()) {
                logger.debug("T{} deleting longs {}", staff.getId(), toRemove);
                staff.removeBarPeaks(toRemove);
            }
        }
    }

    //-------------//
    // refineSides //
    //-------------//
    private void refineSides ()
    {
        for (StaffProjector projector : projectors) {
            projector.refineStaffSides();
        }
    }

    //----------------//
    // setVipSections //
    //----------------//
    private void setVipSections ()
    {
        // Debug sections VIPs
        for (int id : params.vipSections) {
            Section sect = vLag.getVertexById(id);

            if (sect != null) {
                sect.setVip();
                logger.info("Vertical vip section: {}", sect);
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

        Scale.Fraction   maxAlignmentDx = new Scale.Fraction(
            0.5,
            "Max abscissa shift for bar alignment");
        Scale.Fraction   maxConnectionGap = new Scale.Fraction(
            2.0,
            "Max vertical gap when connecting bar lines");
        Constant.Ratio   maxConnectionWhiteRatio = new Constant.Ratio(
            0.20,
            "Max white ratio when connecting bar lines");
        Scale.Fraction   maxRunShift = new Scale.Fraction(
            0.05,
            "Max shift between two runs of vertical sections");
        Scale.Fraction   maxOverlapDeltaPos = new Scale.Fraction(
            0.4,
            "Maximum delta position between two overlapping filaments");
        Scale.Fraction   maxCoordGap = new Scale.Fraction(
            0,
            "Maximum delta ordinate for a gap between filaments");
        Scale.Fraction   maxPosGap = new Scale.Fraction(
            0.2,
            "Maximum delta abscissa for a gap between filaments");
        Scale.Fraction   maxOverlapSpace = new Scale.Fraction(
            0.1,
            "Maximum space between overlapping bar filaments");
        Scale.Fraction   maxBarExtension = new Scale.Fraction(
            0.25,
            "Max extension of bar above or below staff line");
        Scale.Fraction   maxDoubleBarGap = new Scale.Fraction(
            1.0,
            "Max horizontal gap between two members of a double bar");

        // Constants for display
        //
        Constant.Boolean showVerticalLines = new Constant.Boolean(
            false,
            "Should we display the vertical lines?");

        // Constants for debugging
        //
        Constant.String verticalVipSections = new Constant.String(
            "",
            "(Debug) Comma-separated list of VIP vertical sections");
    }

    //------------//
    // Parameters //
    //------------//
    private static class Parameters
    {
        //~ Instance fields ----------------------------------------------------

        final int           maxAlignmentDx;
        final int           maxRunShift;
        final int           maxBarExtension;
        final int           maxConnectionGap;
        final double        maxConnectionWhiteRatio;
        final int           maxDoubleBarGap;

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
            maxAlignmentDx = scale.toPixels(constants.maxAlignmentDx);
            maxRunShift = scale.toPixels(constants.maxRunShift);
            maxBarExtension = scale.toPixels(constants.maxBarExtension);
            maxConnectionGap = scale.toPixels(constants.maxConnectionGap);
            maxDoubleBarGap = scale.toPixels(constants.maxDoubleBarGap);
            maxConnectionWhiteRatio = constants.maxConnectionWhiteRatio.getValue();

            // VIPs
            vipSections = VipUtil.decodeIds(
                constants.verticalVipSections.getValue());

            if (logger.isDebugEnabled()) {
                Main.dumping.dump(this);
            }

            if (!vipSections.isEmpty()) {
                logger.info("Vertical VIP sections: {}", vipSections);
            }
        }
    }
}
//    //---------------//
//    // harmonizeEnds //
//    //---------------//
//    /**
//     * Make sure staves side ends are consistent across the whole page.
//     * <p>
//     * In a page, either all staves or none start with a left bar line.
//     * The same applies for potential right bar line.
//     * <p>
//     * With or without bar lines, staves starting and stopping abscissae are
//     * not random but organized in groups.
//     * Strategy: try to gather ends by rather vertical lines and detect abnormal
//     * points.
//     */
//    private void harmonizeEnds ()
//    {
//        Skew skew = sheet.getSkew();
//
//        // Check barline presence
//        for (HorizontalSide side : HorizontalSide.values()) {
//            List<Integer> withBar = new ArrayList<Integer>();
//            List<Integer> withoutBar = new ArrayList<Integer>();
//
//            for (StaffInfo staff : staffManager.getStaves()) {
//                int xStaff = staff.getAbscissa(side);
//                List<BarPeak> peaks = staff.getBarPeaks();
//                int index = (side == LEFT) ? 0 : (peaks.size() - 1);
//                BarPeak peak = peaks.get(index);
//
//                if ((xStaff >= peak.getStart()) && (xStaff <= peak.getStop())) {
//                    withBar.add(staff.getId());
//                } else {
//                    withoutBar.add(staff.getId());
//                }
//            }
//
//            logger.info("{} Bars:{} noBars:{}", side, withBar, withoutBar);
//        }
//
//        // Check staff start & stop abscissa
//        for (HorizontalSide side : HorizontalSide.values()) {
//            List<BasicLine> lines = new ArrayList<BasicLine>();
//            BasicLine line = null;
//            Double prevX = null;
//
//            for (StaffInfo staff : staffManager.getStaves()) {
//                int x = staff.getAbscissa(side);
//                FilamentLine staffLine = staff.getLines()
//                        .get(2);
//                Point2D end = new Point2D.Double(
//                        x,
//                        staffLine.getEndPoint(side).getY());
//                Point2D dskEnd = skew.deskewed(end);
//                double dskX = dskEnd.getX();
//                boolean shift = (prevX != null)
//                                && (Math.abs(dskX - prevX) > params.maxAlignmentDx);
//                logger.info(
//                        "{} staff#{} x:{} dskEnd:{} {}",
//                        side,
//                        staff.getId(),
//                        x,
//                        String.format("%.0f", dskX),
//                        shift ? String.format("SHIFT_DETECTED: %.0f", dskX - prevX)
//                        : "");
//
//                if (shift) {
//                    // We cannot keep on with the same line
//                    // Reuse another one or create a brand new one?
//                    line = null;
//
//                    for (ListIterator<BasicLine> it = lines.listIterator(
//                            lines.size() - 1); it.hasPrevious();) {
//                        BasicLine ln = it.previous();
//                        final double delta;
//
//                        if (ln.getNumberOfPoints() > 1) {
//                            delta = ln.distanceOf(end);
//                        } else {
//                            delta = ln.getMinAbscissa() - end.getX();
//                        }
//
//                        if (Math.abs(delta) <= (2 * params.maxAlignmentDx)) {
//                            line = ln;
//
//                            break;
//                        }
//                    }
//                }
//
//                if (line == null) {
//                    lines.add(line = new BasicLine());
//                }
//
//                line.includePoint(end);
//                prevX = dskX;
//            }
//
//            logger.info("{} lines: {}", lines.size(), lines);
//        }
//    }
//
