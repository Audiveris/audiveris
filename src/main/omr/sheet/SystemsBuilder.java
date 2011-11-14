//----------------------------------------------------------------------------//
//                                                                            //
//                        S y s t e m s B u i l d e r                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.ConstantSet;

import omr.glyph.GlyphsModel;

import omr.grid.StaffInfo;
import omr.grid.SystemManager;

import omr.log.Logger;

import omr.selection.GlyphEvent;

import omr.step.Step;
import omr.step.StepException;
import omr.step.Steps;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class <code>SystemsBuilder</code> is in charge of retrieving the systems
 * (SystemInfo instances) and parts (PartInfo instances) in the provided sheet
 * and to allocate the corresponding instances on the Score side (the Score
 * instance, and the various instances of ScoreSystem, SystemPart and Staff).
 * The result is visible in the ScoreView.
 *
 * <p>Is does so automatically by using barlines glyphs that embrace staves,
 * parts and systems.  It also allows the user to interactively modify the
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
    private static final Logger logger = Logger.getLogger(SystemsBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** Companion physical stick barsChecker */
    private final BarsChecker barsChecker;

    /** Sheet retrieved systems */
    private final List<SystemInfo> systems;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // SystemsBuilder //
    //----------------//
    /**
     * Creates a new SystemsBuilder object.
     * @param sheet the related sheet
     */
    public SystemsBuilder (Sheet sheet)
    {
        super(sheet, sheet.getNest(), Steps.valueOf(Steps.SPLIT));

        systems = sheet.getSystems();

        // BarsChecker companion, in charge of purely physical tests
        barsChecker = new BarsChecker(sheet, false);
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
        try {
            doBuildSystems();
        } catch (Throwable ex) {
            logger.warning("Error in buildSystems", ex);
        } finally {
            // Provide use checkboard for barlines
            if (Main.getGui() != null) {
                sheet.getAssembly()
                     .addBoard(Step.DATA_TAB, barsChecker.getCheckBoard());
            }
        }
    }

    //-------------------//
    // rebuildAllSystems //
    //-------------------//
    public void rebuildAllSystems ()
    {
        // Update the retrieved systems
        try {
            doBuildSystems();
        } catch (StepException ex) {
            logger.warning("Error rebuilding systems info", ex);
        }
    }

    //---------------//
    // useBoundaries //
    //---------------//
    public void useBoundaries ()
    {
        // Split the entities (horizontals sections, vertical sections,
        // vertical sticks) to the system they belong to.
        splitSystemEntities();
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
        sheet.getPage()
             .resetSystems();

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
        final SystemManager systemManager = sheet.getSystemManager();
        final Integer[]     partTops = systemManager.getPartTops();

        for (SystemInfo system : systemManager.getSystems()) {
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

        // TODO   // Specific degraded case, just one staff, no bar stick
        //        if (systems.isEmpty() && (staffNb == 1)) {
        //            StaffInfo singleStaff = staffManager.getFirstStaff();
        //            system = new SystemInfo(++id, sheet);
        //            systems.add(system);
        //            system.addStaff(singleStaff);
        //            part = new PartInfo();
        //            system.addPart(part);
        //            part.addStaff(singleStaff);
        //            logger.warning("Created one system, one part, one staff");
        //        }
        if (logger.isFineEnabled()) {
            for (SystemInfo systemInfo : systems) {
                Main.dumping.dump(systemInfo);

                int i = 0;

                for (PartInfo partInfo : systemInfo.getParts()) {
                    Main.dumping.dump(partInfo, "Part #" + ++i, 1);
                }
            }
        }
    }

    //----------------//
    // doBuildSystems //
    //----------------//
    private void doBuildSystems ()
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
        sheet.computeSystemBoundaries();

        useBoundaries();
    }

    //---------------//
    // reportResults //
    //---------------//
    private void reportResults ()
    {
        StringBuilder sb = new StringBuilder();
        int           partNb = 0;

        for (SystemInfo system : sheet.getSystems()) {
            partNb = Math.max(partNb, system.getParts().size());
        }

        int sysNb = systems.size();

        if (partNb > 0) {
            sb.append(partNb)
              .append(" part");

            if (partNb > 1) {
                sb.append("s");
            }
        } else {
            sb.append("no part found");
        }

        sheet.getBench()
             .recordPartCount(partNb);

        if (sysNb > 0) {
            sb.append(", ")
              .append(sysNb)
              .append(" system");

            if (sysNb > 1) {
                sb.append("s");
            }
        } else {
            sb.append(", no system found");
        }

        sheet.getBench()
             .recordSystemCount(sysNb);

        logger.info(sheet.getLogPrefix() + sb.toString());
    }

    //---------------------//
    // splitSystemEntities //
    //---------------------//
    /**
     * Split horizontals, vertical sections, glyphs per system
     * @return the set of modified systems
     */
    private SortedSet<SystemInfo> splitSystemEntities ()
    {
        // Split everything, including horizontals, per system
        SortedSet<SystemInfo> modifiedSystems = new TreeSet<SystemInfo>();
        ///modifiedSystems.addAll(sheet.splitHorizontals());
        modifiedSystems.addAll(sheet.splitHorizontalSections());
        modifiedSystems.addAll(sheet.splitVerticalSections());
        modifiedSystems.addAll(sheet.splitBarSticks(nest.getAllGlyphs()));

        if (!modifiedSystems.isEmpty()) {
            StringBuilder sb = new StringBuilder("[");

            for (SystemInfo system : modifiedSystems) {
                sb.append("#")
                  .append(system.getId());
            }

            sb.append("]");

            if (logger.isFineEnabled()) {
                logger.info(sheet.getLogPrefix() + "Impacted systems: " + sb);
            }
        }

        return modifiedSystems;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction maxDeltaLength = new Scale.Fraction(
            0.2,
            "Maximum difference in run length to be part of the same section");

        //
        Scale.Fraction maxBarThickness = new Scale.Fraction(
            1.0,
            "Maximum thickness of an interesting vertical stick");
    }
}
