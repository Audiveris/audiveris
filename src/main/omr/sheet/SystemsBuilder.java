//----------------------------------------------------------------------------//
//                                                                            //
//                        S y s t e m s B u i l d e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.GlyphsModel;
import omr.glyph.facets.Glyph;

import omr.grid.LineInfo;
import omr.grid.StaffInfo;

import omr.math.BasicLine;
import omr.math.Line;

import omr.step.StepException;
import omr.step.Steps;

import omr.util.BrokenLine;
import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code SystemsBuilder} is in charge of retrieving the systems
 * (SystemInfo instances) and parts (PartInfo instances) in the
 * provided sheet and to allocate the corresponding instances on the
 * Score side (the Score instance, and the various instances of
 * ScoreSystem, SystemPart and Staff).
 *
 * <p>Is does so automatically by using barlines glyphs that embrace staves,
 * parts and systems. It also allows the user to interactively modify the
 * retrieved information.</p>
 *
 * <p>Systems define their own area, which may be more complex than a simple
 * ordinate range, in order to precisely define which glyph belongs to which
 * system. The user has the ability to interactively modify the broken line
 * that defines the limit between two adjacent systems.</p>
 *
 * <p>This class has close relationships with {@link MeasuresBuilder} in charge
 * of building and checking the measures, because barlines are used both to
 * define systems and parts, and to define measures.</p>
 *
 * <p>From the related view, the user has the ability to assign or to deassign
 * a barline glyph, with subsequent impact on the related measures.</p>
 *
 * <p>TODO: Implement a way for the user to tell whether a bar glyph is or not
 * a BAR_PART_DEFINING (i.e. if it is anchored on top and bottom).</p>
 *
 * @author Hervé Bitteur
 */
public class SystemsBuilder
        extends GlyphsModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(SystemsBuilder.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** Sheet retrieved systems */
    private final List<SystemInfo> systems;

    //~ Constructors -----------------------------------------------------------
    //----------------//
    // SystemsBuilder //
    //----------------//
    /**
     * Creates a new SystemsBuilder object.
     *
     * @param sheet the related sheet
     */
    public SystemsBuilder (Sheet sheet)
    {
        super(sheet, sheet.getNest(), Steps.valueOf(Steps.SYSTEMS));

        systems = sheet.getSystems();
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // buildSystems //
    //--------------//
    /**
     * Process the sheet information produced by the GRID step and allocate the
     * related score information
     */
    public void buildSystems ()
            throws StepException
    {
        // Systems have been created by GRID step on sheet side
        // Build parts on sheet side
        buildParts();

        // Create score counterparts
        // Build systems, parts & measures on score side
        allocateScoreStructure();

        // Report number of systems retrieved
        reportResults();

        // Define precisely the systems boundaries
        computeBoundaries();

        // Split sections & glyphs per system
        splitSystemEntities();
    }

    //---------------------//
    // splitSystemEntities //
    //---------------------//
    /**
     * Split horizontals, vertical sections, glyphs per system
     */
    public void splitSystemEntities ()
    {
        // Split everything, including horizontals, per system
        ///sheet.splitHorizontals();
        sheet.splitHorizontalSections();
        sheet.splitVerticalSections();
        sheet.splitGlyphs();
    }

    //------------------------//
    // allocateScoreStructure //
    //------------------------//
    /**
     * For each SystemInfo, build the corresponding System entity with all its
     * depending Parts and Staves
     */
    private void allocateScoreStructure ()
            throws StepException
    {
        // Clear Score -> Systems
        sheet.getPage().resetSystems();

        for (SystemInfo system : systems) {
            system.allocateScoreStructure(); // ScoreSystem, Parts & Staves
        }
    }

    //------------//
    // buildParts //
    //------------//
    /**
     * Knowing the starting staff indice of each part, allocate the related
     * PartInfo instances in proper SystemInfo instances
     */
    private void buildParts ()
    {
        final Integer[] partTops = sheet.getStaffManager().getPartTops();

        for (SystemInfo system : sheet.getSystems()) {
            system.getParts().clear(); // Start from scratch
            int partTop = -1;
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

        // TODO   // Specific degraded case, just one staff, no bar stick
        //        if (systems.isEmpty() && (staffNb == 1)) {
        //            StaffInfo singleStaff = staffManager.getFirstStaff();
        //            system = new SystemInfo(++id, sheet);
        //            systems.add(system);
        //            system.addStaff(singleStaff);
        //            part = new PartInfo();
        //            system.addPart(part);
        //            part.addStaff(singleStaff);
        //            logger.warn("Created one system, one part, one staff");
        //        }
        if (logger.isDebugEnabled()) {
            for (SystemInfo systemInfo : systems) {
                Main.dumping.dump(systemInfo);

                int i = 0;

                for (PartInfo partInfo : systemInfo.getParts()) {
                    Main.dumping.dump(partInfo, "Part #" + ++i, 1);
                }
            }
        }
    }

    //-------------------//
    // computeBoundaries //
    //-------------------//
    /**
     * Compute the boundary of the related area of each system.
     */
    private void computeBoundaries ()
    {
        // Very first system top border
        SystemInfo prevSystem = null;
        BrokenLine prevBorder = new BrokenLine(
                new Point(0, 0),
                new Point(sheet.getWidth(), 0));

        BrokenLine border; // Top border of current system

        for (SystemInfo system : sheet.getSystems()) {
            if (prevSystem != null) {
                // Try the simplistic approach, defining top border as the 
                // middle between last line of last staff of previous system
                // and first line of first staff of current system
                Line line = new BasicLine();
                LineInfo topLine = prevSystem.getLastStaff().getLastLine();
                LineInfo botLine = system.getFirstStaff().getFirstLine();

                for (HorizontalSide side : HorizontalSide.values()) {
                    Point2D top = topLine.getEndPoint(side);
                    Point2D bot = botLine.getEndPoint(side);
                    line.includePoint(
                            (top.getX() + bot.getX()) / 2,
                            (top.getY() + bot.getY()) / 2);
                }

                border = new BrokenLine(
                        new Point(0, line.yAtX(0)),
                        new Point(sheet.getWidth(), line.yAtX(sheet.getWidth())));

                // Check if the border is acceptable and replace it if needed
                BrokenLine newBorder = refineBorder(border, prevSystem, system);

                if (newBorder != null) {
                    border = newBorder;
                } else {
                    // Use basic border as a temporary virtual border only
                }

                prevSystem.setBoundary(
                        new SystemBoundary(prevSystem, prevBorder, border));
                prevBorder = border;
            }

            prevSystem = system;
        }

        // Very last system
        if (prevSystem != null) {
            border = new BrokenLine(
                    new Point(0, sheet.getHeight()),
                    new Point(sheet.getWidth(), sheet.getHeight()));
            prevSystem.setBoundary(
                    new SystemBoundary(prevSystem, prevBorder, border));
        }

        sheet.setSystemBoundaries();
    }

    //--------------//
    // refineBorder //
    //--------------//
    private BrokenLine refineBorder (BrokenLine border,
                                     SystemInfo prevSystem,
                                     SystemInfo system)
    {
        // Define the inter-system yellow zone
        int yellowDy = sheet.getScale().toPixels(constants.yellowZoneHalfHeight);
        Polygon polygon = new Polygon();
        Point left = border.getPoint(0);
        Point right = border.getPoint(1);
        polygon.addPoint(left.x, left.y - yellowDy);
        polygon.addPoint(right.x, right.y - yellowDy);
        polygon.addPoint(right.x, right.y + yellowDy);
        polygon.addPoint(left.x, left.y + yellowDy);

        // Look for glyphs intersected by this yellow zone
        List<Glyph> intersected = new ArrayList<>();

        for (Glyph glyph : nest.getActiveGlyphs()) {
            if (polygon.intersects(glyph.getBounds())) {
                intersected.add(glyph);
            }
        }

        logger.debug("S#{}-{} : {}{}", prevSystem.getId(),
                system.getId(), polygon.getBounds(),
                Glyphs.toString(" inter:", intersected));

        // If the yellow zone is empty, keep the border
        // Otherwise, use the more complex approach
        if (intersected.isEmpty()) {
            return border;
        } else {
            return new BorderBuilder(sheet, prevSystem, system).buildBorder();
        }
    }

    //---------------//
    // reportResults //
    //---------------//
    private void reportResults ()
    {
        StringBuilder sb = new StringBuilder();
        int partNb = 0;

        for (SystemInfo system : sheet.getSystems()) {
            partNb = Math.max(partNb, system.getParts().size());
        }

        int sysNb = systems.size();

        if (partNb > 0) {
            sb.append(partNb).append(" part");

            if (partNb > 1) {
                sb.append("s");
            }
        } else {
            sb.append("no part found");
        }

        sheet.getBench().recordPartCount(partNb);

        if (sysNb > 0) {
            sb.append(", ").append(sysNb).append(" system");

            if (sysNb > 1) {
                sb.append("s");
            }
        } else {
            sb.append(", no system found");
        }

        sheet.getBench().recordSystemCount(sysNb);

        logger.info("{}{}", sheet.getLogPrefix(), sb.toString());
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction yellowZoneHalfHeight = new Scale.Fraction(
                0.1,
                "Half height of inter-system yellow zone");

    }
}
