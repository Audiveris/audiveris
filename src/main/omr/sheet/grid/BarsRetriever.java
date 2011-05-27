//----------------------------------------------------------------------------//
//                                                                            //
//                         B a r s R e t r i e v e r                          //
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
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;

import omr.lag.JunctionRatioPolicy;
import omr.lag.SectionsBuilder;

import omr.log.Logger;

import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunsTable;
import omr.run.RunsTableView;

import omr.score.common.PixelPoint;

import omr.sheet.BarsChecker;
import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.step.StepException;

import omr.stick.StickSection;

import omr.ui.view.ScrollView;

import omr.util.Predicate;

import java.util.*;
import java.util.List;

/**
 * Class <code>BarsRetriever</code> focuses on the retrieval of vertical bars
 * to determine system frames.
 *
 * @author Hervé Bitteur
 */
public class BarsRetriever
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BarsRetriever.class);

    //~ Instance fields --------------------------------------------------------

    /** Related sheet */
    private final Sheet sheet;

    /** Related scale */
    private final Scale scale;

    /** Scale-dependent constants for vertical stuff */
    private final Parameters params;

    /** Lag of vertical runs */
    private GlyphLag vLag;

    /** Sequence of systems */
    private List<SystemFrame> systems;

    /** Related staff manager */
    private final StaffManager staffManager;

    /** Companion in charge of physical bar checking */
    private BarsChecker barsChecker;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // BarsRetriever //
    //---------------//
    /**
     * Retrieve the frames of all staff lines
     *
     * @param sheet the sheet to process
     */
    public BarsRetriever (Sheet sheet)
    {
        this.sheet = sheet;

        scale = sheet.getScale();
        params = new Parameters(scale);
        staffManager = sheet.getStaffManager();
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // getLag //
    //--------//
    public GlyphLag getLag ()
    {
        return vLag;
    }

    //------------//
    // getSystems //
    //------------//
    /**
     * @return the systems
     */
    public List<SystemFrame> getSystems ()
    {
        return systems;
    }

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Compute and display the system frames of the sheet picture
     */
    public void buildInfo ()
        throws StepException
    {
        // Retrieve the barlines
        buildBarlines();
    }

    //----------//
    // buildLag //
    //----------//
    public void buildLag (RunsTable wholeVertTable)
    {
        vLag = new GlyphLag("vLag", StickSection.class, Orientation.VERTICAL);

        RunsTable longVertTable = wholeVertTable.clone("long-vert")
                                                .purge(
            new Predicate<Run>() {
                    public final boolean check (Run run)
                    {
                        return run.getLength() < params.minRunLength;
                    }
                });

        // Add a view on runs table
        sheet.getAssembly()
             .addRunsTab(longVertTable);

        SectionsBuilder sectionsBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
            vLag,
            new JunctionRatioPolicy(params.maxLengthRatio));
        sectionsBuilder.createSections(longVertTable);
    }

    //-----------//
    // isLongBar //
    //-----------//
    boolean isLongBar (Stick stick)
    {
        return stick.getLength() >= params.minLongLength;
    }

    //---------//
    // getBars //
    //---------//
    private List<Stick> getBars ()
    {
        List<Stick> bars = new ArrayList<Stick>();

        for (Glyph glyph : vLag.getActiveGlyphs()) {
            Shape shape = glyph.getShape();

            if ((shape == Shape.THICK_BARLINE) ||
                (shape == Shape.THIN_BARLINE)) {
                bars.add((Stick) glyph);
            }
        }

        return bars;
    }

    //---------------//
    // buildBarlines //
    //---------------//
    /**
     * Use the long vertical sections to retrieve the barlines that limit the
     * staves
     */
    private void buildBarlines ()
        throws StepException
    {
        // Use the longest sections aggregates as barline candidates
        barsChecker = new BarsChecker(sheet, vLag, true); // Rough
        barsChecker.retrieveCandidates();

        // Connect bars and staves into systems
        connectBars();
    }

    //-------------------//
    // buildSystemFrames //
    //-------------------//
    /**
     * Build the frame of each system
     * @param tops the startin staff for each system
     * @return the sequence of system physical frames
     */
    private List<SystemFrame> buildSystemFrames (Integer[] tops)
    {
        List<SystemFrame> systems = new ArrayList<SystemFrame>();
        Integer           staffTop = null;
        int               systemId = 0;
        SystemFrame       systemFrame = null;

        for (int i = 0; i < staffManager.getStaffCount(); i++) {
            StaffInfo staff = staffManager.getStaff(i);

            // System break?
            if ((staffTop == null) || (staffTop < tops[i])) {
                // Start of a new system
                staffTop = tops[i];

                systemFrame = new SystemFrame(
                    ++systemId,
                    staffManager.getRange(staff, staff));
                systems.add(systemFrame);
            } else {
                // Continuing current system
                systemFrame.setStaves(
                    staffManager.getRange(systemFrame.getFirstStaff(), staff));
            }

            if (logger.isFineEnabled()) {
                logger.fine("i:" + i + " tops:" + tops[i]);
            }
        }

        return systems;
    }

    //-------------//
    // connectBars //
    //-------------//
    /**
     * Connect bars and staves, using the skeletons of vertical glyphs (bars)
     * and non-finished staves.
     */
    private void connectBars ()
    {
        // Bars are available as vertical glyphs
        // Staves are available through the StaffManager

        // Sort bars starting from the longest
        // Each such bar can connect several staves as a system (or just a part)
        List<Stick>                 bars = getBars();

        // Retrieve the staves that start systems
        Map<StaffInfo, List<Stick>> staffBars = new HashMap<StaffInfo, List<Stick>>();
        Integer[]                   tops = retrieveSystemTops(bars, staffBars);

        // Retrieve bar(s) at left and right side of each staff
        retrieveStaffSides(staffBars);

        // Create system frames
        systems = buildSystemFrames(tops);

        // Retrieve the frame left side for each system
        for (SystemFrame system : systems) {
            // Retrieve system left barline
            system.getLeftBar();
            // Adjust line left points accordingly
            system.alignEndings();
            logger.info(system.toString());
        }
    }

    //---------------------//
    // retrieveLeftSideBar //
    //---------------------//
    private void retrieveLeftSideBar (StaffInfo   staff,
                                      List<Stick> staffSticks)
    {
        BarInfo bar = null;
        int     xMax = staff.getLeft() + params.maxDistanceFromStaffSide;

        for (Stick stick : staffSticks) {
            int x = stick.getMidPos(); // TODO: refine

            if (x > xMax) {
                break;
            }

            if (isLongBar(stick)) {
                if (bar == null) {
                    bar = new BarInfo(stick);
                } else {
                    // Perhaps a pack of bars
                    if ((x - bar.getLeftStick()
                                .getMidPos()) <= params.maxLeftBarPackWidth) {
                        bar.appendStick(stick);
                    }
                }
            }
        }

        staff.setLeftBar(bar);
    }

    //----------------------//
    // retrieveRightSideBar //
    //----------------------//
    private void retrieveRightSideBar (StaffInfo   staff,
                                       List<Stick> staffSticks)
    {
        BarInfo bar = null;
        int     xMin = staff.getRight() - params.maxDistanceFromStaffSide;

        for (int i = staffSticks.size() - 1; i >= 0; i--) {
            Stick stick = staffSticks.get(i);
            int   x = stick.getMidPos(); // TODO: refine

            if (x < xMin) {
                break;
            }

            if (bar == null) {
                bar = new BarInfo(stick);
            } else {
                // Perhaps a pack of bars
                if ((bar.getRightStick()
                        .getMidPos() - x) <= params.maxRightBarPackWidth) {
                    bar.prependStick(stick);
                }
            }
        }

        staff.setRightBar(bar);
    }

    //--------------------//
    // retrieveStaffSides //
    //--------------------//
    /**
     * Retrieve the bars that start and the bars that end each staff
     * @param staffBars the bar candidates that intersect each staff
     */
    private void retrieveStaffSides (Map<StaffInfo, List<Stick>> staffBars)
    {
        for (StaffInfo staff : staffManager.getStaves()) {
            List<Stick> staffSticks = staffBars.get(staff);
            // TODO: Should use abscissa at staff ordinate
            Collections.sort(staffSticks, Stick.midPosComparator);
            retrieveLeftSideBar(staff, staffSticks);
            retrieveRightSideBar(staff, staffSticks);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "staff#" + staff.getId() + " leftBar:" +
                    staff.getLeftBar() + " rightBar:" + staff.getRightBar() +
                    Glyphs.toString(" bars", staffSticks));
            }
        }
    }

    //--------------------//
    // retrieveSystemTops //
    //--------------------//
    /**
     * Retrieve for each staff the staff that starts its containing system
     * @param bars input: the collection of bar candidates
     * @param staffBars output: the collection of bars that intersect each staff
     * @return the system starting staff for each staff
     */
    private Integer[] retrieveSystemTops (List<Stick>                 bars,
                                          Map<StaffInfo, List<Stick>> staffBars)
    {
        Collections.sort(bars, Stick.reverseLengthComparator);

        Integer[] tops = new Integer[staffManager.getStaffCount()];

        for (Stick stick : bars) {
            PixelPoint start = stick.getStartPoint();
            StaffInfo  topStaff = staffManager.getStaffAt(start);
            PixelPoint stop = stick.getStopPoint();
            StaffInfo  botStaff = staffManager.getStaffAt(stop);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Bar#" + stick.getId() + " top:" + topStaff.getId() +
                    " bot:" + botStaff.getId());
            }

            int top = topStaff.getId() - 1;
            int bot = botStaff.getId() - 1;

            for (int i = top; i <= bot; i++) {
                StaffInfo   staff = staffManager.getStaff(i);
                List<Stick> staffSticks = staffBars.get(staff);

                if (staffSticks == null) {
                    staffSticks = new ArrayList<Stick>();
                    staffBars.put(staff, staffSticks);
                }

                staffSticks.add(stick);

                if ((tops[i] == null) || (top < tops[i])) {
                    tops[i] = top;
                }
            }
        }

        return tops;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Ratio maxLengthRatio = new Constant.Ratio(
            1.5,
            "Maximum ratio in length for a run to be combined with an existing section");

        // Constants specified WRT mean interline
        // --------------------------------------
        Scale.Fraction minRunLength = new Scale.Fraction(
            1.5, // 1.5,
            "Minimum length for a vertical run to be considered");
        Scale.Fraction minLongLength = new Scale.Fraction(
            8,
            "Minimum length for a long vertical bar");
        Scale.Fraction maxDistanceFromStaffSide = new Scale.Fraction(
            2,
            "Max abscissa delta when looking for left or right side bars");
        Scale.Fraction maxLeftBarPackWidth = new Scale.Fraction(
            1.5,
            "Max width of a pack of vertical barlines");
        Scale.Fraction maxRightBarPackWidth = new Scale.Fraction(
            0.5,
            "Max width of a pack of vertical barlines");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all constants related to vertical frames
     */
    private static class Parameters
    {
        //~ Static fields/initializers -----------------------------------------

        /** Specific application parameters */
        private static final Constants constants = new Constants();

        /** Usual logger utility */
        private static final Logger logger = Logger.getLogger(Parameters.class);

        //~ Instance fields ----------------------------------------------------

        /** Minimum run length for vertical lag */
        final int minRunLength;

        /** Used for section junction policy */
        final double maxLengthRatio;

        /** Minimum for long vertical stick bars */
        final int minLongLength;

        /** Maximum distance between a bar and the staff side */
        final int maxDistanceFromStaffSide;

        /** Maximum width for a pack of bars on left side */
        final int maxLeftBarPackWidth;

        /** Maximum width for a pack of bars on right side*/
        final int maxRightBarPackWidth;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Parameters object.
         *
         * @param scale the scaling factor
         */
        public Parameters (Scale scale)
        {
            minRunLength = scale.toPixels(constants.minRunLength);
            maxLengthRatio = constants.maxLengthRatio.getValue();
            minLongLength = scale.toPixels(constants.minLongLength);
            maxDistanceFromStaffSide = scale.toPixels(
                constants.maxDistanceFromStaffSide);
            maxLeftBarPackWidth = scale.toPixels(constants.maxLeftBarPackWidth);
            maxRightBarPackWidth = scale.toPixels(
                constants.maxRightBarPackWidth);

            if (logger.isFineEnabled()) {
                Main.dumping.dump(this);
            }
        }
    }
}
