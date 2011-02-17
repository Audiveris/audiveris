//----------------------------------------------------------------------------//
//                                                                            //
//                    S t a v e s F u z z y B u i l d e r                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.GlyphsModel;
import omr.glyph.facets.BasicStick;
import omr.glyph.ui.GlyphBoard;
import omr.glyph.ui.GlyphLagView;
import omr.glyph.ui.GlyphsController;

import omr.lag.HorizontalOrientation;
import omr.lag.JunctionRatioPolicy;
import omr.lag.SectionsBuilder;
import omr.lag.ui.RunBoard;
import omr.lag.ui.ScrollLagView;
import omr.lag.ui.SectionBoard;
import omr.lag.ui.SectionView;

import omr.log.Logger;

import omr.math.NaturalSpline;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.LinesBuilder.LinesParameters;
import omr.sheet.picture.Picture;
import omr.sheet.ui.PixelBoard;

import omr.step.Step;
import omr.step.StepException;
import omr.step.Steps;

import omr.stick.StickSection;

import omr.ui.BoardsPane;

import omr.util.Implement;
import omr.util.WeakPropertyChangeListener;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class {@code StavesFuzzyBuilder} implements {@link StavesBuilder} by using
 * an approach likely to handle non straight staff lines
 *
 * @author Hervé Bitteur
 */
public class StavesFuzzyBuilder
    extends GlyphsModel
    implements StavesBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        StavesFuzzyBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** Sequence of retrieved staves */
    private List<StaffInfo> staves = new ArrayList<StaffInfo>();

    /** Related scale */
    private Scale scale;

    /** Lag view on staff lines, if so desired */
    private GlyphLagView lagView;

    /** Long filaments found */
    private List<Filament> filaments = new ArrayList<Filament>();

    /** Maximum acceptable section thickness */
    private int maxSectionThickness;

    /** Minimum acceptable section length */
    private int minSectionLength;

    //~ Constructors -----------------------------------------------------------

    //--------------------//
    // StavesFuzzyBuilder //
    //--------------------//
    /**
     * Creates a new StavesFuzzyBuilder object.
     *
     * @param sheet The sheet to process
     */
    public StavesFuzzyBuilder (Sheet sheet)
    {
        super(
            sheet,
            new GlyphLag(
                "hLag",
                StickSection.class,
                new HorizontalOrientation()),
            Steps.valueOf(Steps.LINES));
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getStaves //
    //-----------//
    @Implement(StavesBuilder.class)
    public List<StaffInfo> getStaves ()
    {
        return staves;
    }

    //-----------//
    // buildInfo //
    //-----------//
    @Implement(StavesBuilder.class)
    public void buildInfo ()
        throws StepException
    {
        Picture picture = sheet.getPicture();
        scale = sheet.getScale();

        // Parameters
        minSectionLength = scale.toPixels(constants.minSectionLength);
        maxSectionThickness = scale.mainFore();

        SectionsBuilder<GlyphLag, GlyphSection> lagBuilder;
        lagBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
            getLag(),
            new JunctionRatioPolicy(constants.maxLengthRatio.getValue()));
        lagBuilder.createSections(
            picture,
            scale.toPixels(constants.minRunLength));

        sheet.setHorizontalLag(lag);

        // This is the heart of staff lines detection ...
        try {
            buildFilaments();

            //            retrieveStaves(retrievePeaks(sheet.getPicture().getHeight()));
            //
            //            // Clean up the staff lines in the found staves.
            //            cleanup();
            //
            //            // Determine limits in ordinate for each staff area
            //            if (!staves.isEmpty()) {
            //                computeStaffLimits();
            //            }

            // User feedback
            if (staves.size() > 1) {
                logger.info(staves.size() + " staves");
            } else if (!staves.isEmpty()) {
                logger.info(staves.size() + " staff");
            } else {
                logger.warning("No staves found!");
            }

            sheet.getBench()
                 .recordStaveCount(staves.size());
        } finally {
            // Display the resulting lag if so asked for
            if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
                displayFrame();
            }
        }

        if (staves.isEmpty()) {
            logger.warning("Cannot proceed without staves");

            //throw new StepException("Cannot proceed without staves");
        }
    }

    //--------------//
    // displayChart //
    //--------------//
    @Implement(StavesBuilder.class)
    public void displayChart ()
    {
        logger.warning(
            "No display chart implemented by StavesFuzzyBuilder yet");
    }

    //--------------//
    // isMajorChunk //
    //--------------//
    private boolean isMajorChunk (GlyphSection section)
    {
        // Check section length
        // Check /quadratic mean/ section thickness
        int length = section.getLength();

        return (length >= minSectionLength) &&
               ((section.getWeight() / length) <= maxSectionThickness);
    }

    //----------------//
    // buildFilaments //
    //----------------//
    private void buildFilaments ()
    {
        // Sort sections by decreasing length
        List<GlyphSection> sections = new ArrayList<GlyphSection>(
            getLag().getSections());
        Collections.sort(sections, GlyphSection.reverseLengthComparator);

        SectionLoop: 
        for (GlyphSection section : sections) {
            // Limit to main sections for the time being
            if (!isMajorChunk(section)) {
                continue;
            }

            // Can we aggregate this section to a filament stick?
            for (Filament filament : filaments) {
                if (filament.include(section)) {
                    continue SectionLoop;
                }
            }

            // Start a brand new filament with this section
            Filament filament = new Filament(scale.interline());
            filament.include(section);
            filaments.add(filament);
        }
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        // Sections that, as members of staff lines, will be treated as specific
        List<GlyphSection> members = new ArrayList<GlyphSection>();

        /*
         * Populate the specific sections, to hide or display the removed
         * line sections. This assumes StraightLineInfo implementation (?)
         */

        // Browse StaffInfos
        for (StaffInfo staff : staves) {
            // Browse LineInfos
            for (LineInfo line : staff.getLines()) {
                members.addAll(line.getSections());
            }
        }

        GlyphsController controller = new GlyphsController(this);
        lagView = new MyView(lag, members, controller);

        final String  unit = sheet.getId() + ":StavesFuzzyBuilder";
        BoardsPane    boardsPane = new BoardsPane(
            new PixelBoard(unit, sheet),
            new RunBoard(unit, lag),
            new SectionBoard(unit, lag.getLastVertexId(), lag),
            new GlyphBoard(unit, controller, null));

        // Create a hosting frame for the view
        ScrollLagView slv = new ScrollLagView(lagView);
        sheet.getAssembly()
             .addViewTab(Step.LINES_TAB, slv, boardsPane);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Boolean displayFrame = new Constant.Boolean(
            true,
            "Should we display a frame on Lags found ?");

        /** Should we display original staff lines */
        Constant.Boolean displayOriginalStaffLines = new Constant.Boolean(
            false,
            "Should we display original staff lines?");
        Constant.Ratio   maxLengthRatio = new Constant.Ratio(
            2.5,
            "Maximum ratio in length for a run to be combined with an existing section");
        Scale.Fraction   minRunLength = new Scale.Fraction(
            0,
            "Minimum length for a run to be considered");
        Scale.Fraction   minSectionLength = new Scale.Fraction(
            5,
            "Minimum length for a section to be considered in line retrieval");
    }

    //----------//
    // Filament //
    //----------//
    /**
     * Represents a candidate staff line
     */
    private static class Filament
        extends BasicStick
    {
        //~ Static fields/initializers -----------------------------------------

        private static int          SEGMENT = 200;

        /**
         * For comparing Filament instances on position then coordinate
         */
        static Comparator<Filament> comparator = new Comparator<Filament>() {
            public int compare (Filament s1,
                                Filament s2)
            {
                if (s1 == s2) {
                    return 0;
                }

                // Sort on vertical position first
                int dPos = s1.getMidPos() - s2.getMidPos();

                if (dPos != 0) {
                    return dPos;
                }

                // Sort on horizontal coordinate second
                int dStart = s1.getStart() - s2.getStart();

                if (dStart != 0) {
                    return dStart;
                } else {
                    throw new RuntimeException(
                        "Overlapping filaments " + s1 + " & " + s2);
                }
            }
        };


        //~ Instance fields ----------------------------------------------------

        /** Interpolating curve */
        private NaturalSpline curve;

        //~ Constructors -------------------------------------------------------

        public Filament (int interline)
        {
            super(interline);
        }

        //~ Methods ------------------------------------------------------------

        public NaturalSpline getCurve ()
        {
            if (curve == null) {
                try {
                    GlyphSection   section = getFirstSection();

                    PixelRectangle box = section.getContourBox();
                    PixelRectangle r = new PixelRectangle(box);
                    r.width = 5;

                    List<PixelPoint> points = new ArrayList<PixelPoint>();
                    PixelPoint       pStart = section.getRectangleCentroid(r);
                    pStart.x = box.x;
                    points.add(pStart);

                    for (int i = SEGMENT; i < (box.width - SEGMENT);
                         i += SEGMENT) {
                        r.x = box.x + i;

                        PixelPoint pt = section.getRectangleCentroid(r);

                        if (pt != null) {
                            points.add(pt);
                        }
                    }

                    r.x = (box.x + box.width) - r.width;

                    PixelPoint pStop = section.getRectangleCentroid(r);
                    pStop.x = (box.x + box.width) - 1;
                    points.add(pStop);

                    curve = NaturalSpline.interpolate(
                        points.toArray(new PixelPoint[0]));
                    addAttachment("SPLINE", curve);
                } catch (Exception ex) {
                    logger.warning("Cannot getCurve", ex);
                }
            }

            return curve;
        }

        public boolean include (GlyphSection section)
        {
            if (getMembers()
                    .isEmpty()) {
                addSection(section, Linking.LINK_BACK);
                reset();

                return true;
            } else {
                return false;
            }
        }

        @Override
        protected String internalsString ()
        {
            return super.internalsString();
        }

        protected void reset ()
        {
            curve = null;
        }
    }

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends GlyphLagView
    {
        //~ Constructors -------------------------------------------------------

        //--------//
        // MyView //
        //--------//
        public MyView (GlyphLag           lag,
                       List<GlyphSection> specifics,
                       GlyphsController   controller)
        {
            super(
                lag,
                specifics,
                constants.displayOriginalStaffLines,
                controller,
                null);
            setName("StavesFuzzyBuilder-View");

            // (Weakly) listening on LineParameters properties
            LinesParameters.getInstance()
                           .addPropertyChangeListener(
                new WeakPropertyChangeListener(this));
        }

        //~ Methods ------------------------------------------------------------

        //---------------------//
        // colorizeAllSections //
        //---------------------//
        @Override
        public void colorizeAllSections ()
        {
            Color veryLightGray = new Color(240, 240, 250);
            int   viewIndex = lag.viewIndexOf(this);

            // Default colors for lag sections
            super.colorizeAllSections();

            // Colorize the long sections
            for (GlyphSection section : getLag()
                                            .getSections()) {
                // Determine suitable color
                Color color;

                if (isMajorChunk(section)) {
                    color = Color.pink; // Long filaments
                } else {
                    color = veryLightGray; // Other sticks
                }

                SectionView view = (SectionView) section.getView(viewIndex);
                view.setColor(color);
            }
        }

        //-------------//
        // renderItems //
        //-------------//
        @Override
        protected void renderItems (Graphics2D g)
        {
            // Draw the line info, lineset by lineset
            g.setColor(Color.yellow);

            for (Filament filament : filaments) {
                NaturalSpline curve = filament.getCurve();

                if (curve != null) {
                    g.draw(curve);
                }
            }

            for (StaffInfo staff : staves) {
                staff.render(g);
            }
        }
    }
}
