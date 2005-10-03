//-----------------------------------------------------------------------//
//                                                                       //
//                    V e r t i c a l s B u i l d e r                    //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.Main;
import omr.ProcessingException;
import omr.check.Check;
import omr.check.CheckSuite;
import omr.check.FailureResult;
import omr.check.SuccessResult;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.glyph.Glyph;
import omr.glyph.GlyphDirectory;
import omr.glyph.GlyphLag;
import omr.glyph.Shape;
import omr.glyph.ui.GlyphBoard;
import omr.stick.Stick;
import omr.stick.StickSection;
import omr.stick.StickView;
import omr.ui.BoardsPane;
import omr.ui.FilterBoard;
import omr.ui.PixelBoard;
import omr.ui.ScrollLagView;
import omr.ui.SectionBoard;
import omr.ui.Zoom;
import omr.util.Logger;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

/**
 * Class <code>VerticalsBuilder</code> is in charge of retrieving all the
 * vertical sticks of all systems in the sheet at hand. Bars are assumed to
 * have been already recognized, so this accounts for stems, vertical edges
 * of endings, and potentially parts of alterations (sharp, natural, flat).
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class VerticalsBuilder
    implements GlyphDirectory
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(VerticalsBuilder.class);

    // Success codes
    private static final SuccessResult STEM = new SuccessResult("Stem");

    // Failure codes
    private static final FailureResult TOO_LIMITED        = new FailureResult("Stem-TooLimited");
    private static final FailureResult TOO_SHORT          = new FailureResult("Stem-TooShort");
    private static final FailureResult TOO_FAT            = new FailureResult("Stem-TooFat");
    private static final FailureResult TOO_HIGH_ADJACENCY = new FailureResult("Stem-TooHighAdjacency");
    private static final FailureResult OUTSIDE_SYSTEM     = new FailureResult("Stem-OutsideSystem");
    private static final FailureResult TOO_HOLLOW         = new FailureResult("Stem-TooHollow");
    private static final FailureResult NO_CHUNK           = new FailureResult("Stem-NoChunk");

    //~ Instance variables ------------------------------------------------

    // The containing sheet
    private final Sheet sheet;

    // Lag of vertical runs
    private final GlyphLag vLag;
    private final VerticalArea verticalsArea;

    private MyView view;

    //~ Constructors ------------------------------------------------------

    //------------------//
    // VerticalsBuilder //
    //------------------//
    public VerticalsBuilder (Sheet sheet)
        throws ProcessingException
    {
        this.sheet = sheet;

        Scale scale = sheet.getScale();

        // We re-use the same vertical lag (but not the sticks) from Bars.
        vLag = sheet.getVerticalLag();

        // We cannot reuse the sticks, since thick sticks are allowed for
        // bars but not for stems. So, let's rebuild the stick area from
        // the initial lag.
        verticalsArea = new VerticalArea
            (sheet, vLag,
             scale.fracToPixels(constants.maxStemThickness));

        // Split these candidates per system
        SystemSplit.splitVerticalSticks(sheet, verticalsArea.getSticks());

        // Now process each system area on turn
        int totalStemNb = 0;

        // Iterate on systems
        for (SystemInfo system : sheet.getSystems()) {
            totalStemNb += retrieveVerticals(system);
        }

        if (constants.displayFrame.getValue() && Main.getJui() != null) {
            displayFrame();
        }

        logger.info(totalStemNb + " stem(s) found");
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // getEntity //
    //-----------//
    public Glyph getEntity (Integer id)
    {
        return vLag.getGlyph(id);
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        // Specific rubber display
        view = new MyView();
        view.colorize();

        // Ids of recognized glyphs
        List<Integer> knownIds = new ArrayList<Integer>();
        knownIds.add(GlyphBoard.NO_VALUE);
        for (SystemInfo system : sheet.getSystems()) {
            for (Glyph glyph : system.getGlyphs()) {
                if (glyph.isStem()) {
                    knownIds.add(new Integer(glyph.getId()));
                }
            }
        }

        // Create a hosting frame for the view
        sheet.getAssembly().addViewTab
            ("Verticals", new ScrollLagView(view),
             new BoardsPane
             (view,
              new PixelBoard(),
              new SectionBoard(vLag.getLastVertexId()),
              new GlyphBoard(vLag.getLastGlyphId(), knownIds),
              new FilterBoard()));
    }

    //-------------------//
    // retrieveVerticals //
    //-------------------//
    private int retrieveVerticals (SystemInfo system)
            throws ProcessingException
    {
        // Define the suite of Checks
        StemCheckSuite suite = new StemCheckSuite(system);

        double minResult = constants.minCheckResult.getValue();
        int stemNb = 0;
        List<Stick> sticks = system.getVerticalSticks();

        if (logger.isDebugEnabled()) {
            logger.debug("Searching verticals among " + sticks.size()
                         + " sticks from " + system);
        }

        for (Stick stick : sticks) {
            // Run the various Checks
            double res = suite.pass(stick);

            if (logger.isDebugEnabled()) {
                logger.debug("suite=> " + res + " for " + stick);
            }

            if (res >= minResult) {
                stick.setResult(STEM);
                stick.setShape(Shape.COMBINING_STEM);
                system.getGlyphs().add(stick);
                stemNb++;
            } else {
                stick.setResult(TOO_LIMITED);
            }
        }

        // (Re)Sort the modified list of glyphs, by abscissa
        system.sortGlyphs();

        if (logger.isDebugEnabled()) {
            logger.debug("Found " + stemNb + " stems");
        }

        return stemNb;
    }

    //~ Classes -----------------------------------------------------------

    //--------//
    // MyView //
    //--------//
    private class MyView
        extends StickView
    {
        //~ Constructors --------------------------------------------------

        public MyView ()
        {
            super(vLag, null, VerticalsBuilder.this);
        }

        //~ Methods -------------------------------------------------------

        //----------//
        // colorize //
        //----------//
        public void colorize ()
        {
            super.colorize();

            final int viewIndex = lag.getViews().indexOf(this);

            // All remaining vertical sticks clutter (HB TRYING)
            for (Stick stick : verticalsArea.getSticks()) {
                stick.colorize(lag, viewIndex, Color.red);
            }

            // Use light gray color for past successful entities
            sheet.colorize(lag, viewIndex, Color.lightGray);

            // Iterate on systems
            for (SystemInfo system : sheet.getSystems()) {
                // Use bright yellow color for recognized stems
                for (Glyph glyph : system.getGlyphs()) {
                    if (glyph.isStem()) {
                        Stick stick = (Stick) glyph;
                        stick.colorize(lag, viewIndex, Color.yellow); // TBD,
                                                                      // use
                                                                      // glyph.colorize ???
                    }
                }
            }
        }

        //-------------//
        // renderItems //
        //-------------//
        public void renderItems (Graphics g)
        {
            Zoom z = getZoom();

            // Render all physical info known so far
            sheet.render(g, z);

            Scale scale = sheet.getScale();
            int nWidth = scale.fracToPixels(constants.chunkWidth);
            int nHeight = scale.fracToPixels(constants.chunkHeight);

            // Render the contour of the verticals
            for (SystemInfo system : sheet.getSystems()) {

                for (Glyph glyph : system.getGlyphs()) {
                    if (glyph.isStem()) {
                        Stick stick = (Stick) glyph;
                        stick.renderContour(g, z);
                        //stick.renderChunk(g, z, nHeight, nWidth);
                        stick.renderLine(g, z);
                    }
                }
            }

            // TBD Render the contour of the unlucky candidates also?
        }

        //---------------//
        // glyphSelected //
        //---------------//
        @Override
            protected void glyphSelected (Glyph glyph,
                                          Point pt)
        {
            try {
                if (glyph instanceof Stick) {
                    SystemInfo systemInfo = sheet.getSystemAtY(pt.y);
                    StemCheckSuite suite = new StemCheckSuite(systemInfo);
                    Stick stick = (Stick) glyph;
                    filterMonitor.tellHtml(suite.passHtml(null, stick));
                }
            } catch (ProcessingException ex) {
            }
        }

        //---------------//
        // deassignGlyph //
        //---------------//
        @Override
            public void deassignGlyph (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            switch (shape) {
            case COMBINING_STEM :
                logger.info("Deassign a " + shape);
                sheet.getGlyphPane()
                    .cancelStems(Collections.singletonList(glyph));
                break;

            default :
            }
        }
    }

    //----------------//
    // MinLengthCheck //
    //----------------//
    private class MinLengthCheck
            extends Check<Stick>
    {
        //~ Constructors --------------------------------------------------

        protected MinLengthCheck ()
                throws ProcessingException
        {
            super("MinLength", constants.minStemLengthLow.getValue(),
                  constants.minStemLengthHigh.getValue(), true, TOO_SHORT);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the length data
        protected double getValue (Stick stick)
        {
            return sheet.getScale().pixelsToFrac(stick.getLength());
        }
    }

    //----------------//
    // MaxAspectCheck //
    //----------------//
    private static class MaxAspectCheck
            extends Check<Stick>
    {
        //~ Constructors --------------------------------------------------

        protected MaxAspectCheck ()
        {
            super("MaxAspect", constants.maxStemAspectLow.getValue(),
                  constants.maxStemAspectHigh.getValue(), false, TOO_FAT);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the ratio thickness / length
        protected double getValue (Stick stick)
        {
            return stick.getAspect();
        }
    }

    //---------------------//
    // FirstAdjacencyCheck //
    //---------------------//
    private static class FirstAdjacencyCheck
            extends Check<Stick>
    {
        //~ Constructors --------------------------------------------------

        protected FirstAdjacencyCheck ()
        {
            super("LeftAdj", constants.maxStemAdjacencyLow.getValue(),
                  constants.maxStemAdjacencyHigh.getValue(), false,
                  TOO_HIGH_ADJACENCY);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the adjacency value
        protected double getValue (Stick stick)
        {
            int length = stick.getLength();

            return (double) stick.getFirstStuck() / (double) length;
        }
    }

    //--------------------//
    // LastAdjacencyCheck //
    //--------------------//
    private static class LastAdjacencyCheck
            extends Check<Stick>
    {
        //~ Constructors --------------------------------------------------

        protected LastAdjacencyCheck ()
        {
            super("RightAdj", constants.maxStemAdjacencyLow.getValue(),
                  constants.maxStemAdjacencyHigh.getValue(), false,
                  TOO_HIGH_ADJACENCY);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the adjacency value
        protected double getValue (Stick stick)
        {
            int length = stick.getLength();

            return (double) stick.getLastStuck() / (double) length;
        }
    }

    //-----------//
    // LeftCheck //
    //-----------//
    private class LeftCheck
            extends Check<Stick>
    {
        //~ Constructors --------------------------------------------------

        protected LeftCheck (int val)
                throws ProcessingException
        {
            super("LeftLimit", val, val, true, OUTSIDE_SYSTEM);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the stick abscissa
        protected double getValue (Stick stick)
        {

            return stick.getFirstPos();
        }
    }

    //------------//
    // RightCheck //
    //------------//
    private class RightCheck
            extends Check<Stick>
    {
        //~ Constructors --------------------------------------------------

        protected RightCheck (int val)
                throws ProcessingException
        {
            super("RightLimit", val, val, false, OUTSIDE_SYSTEM);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the stick abscissa
        protected double getValue (Stick stick)
        {

            return stick.getFirstPos();
        }
    }

    //-----------------//
    // MinDensityCheck //
    //-----------------//
    private class MinDensityCheck
            extends Check<Stick>
    {
        //~ Constructors --------------------------------------------------

        protected MinDensityCheck ()
        {
            super("MinDensity", constants.minDensityLow.getValue(),
                  constants.minDensityHigh.getValue(), true, TOO_HOLLOW);
        }

        //~ Methods -------------------------------------------------------

        // Retrieve the density
        protected double getValue (Stick stick)
        {
            Rectangle rect = stick.getBounds();
            double area = rect.width * rect.height;

            return (double) stick.getWeight() / area;
        }
    }

//     //---------------//
//     // MinChunkCheck //
//     //---------------//
//     /**
//      * Class <code>MinChunkCheck</code> checks for presence of a chunk (beam
//      * or note head) at top or bottom
//      */
//     private class MinChunkCheck
//             extends Check<Stick>
//     {
//         //~ Instance variables --------------------------------------------

//         // Half-dimensions for window at top and bottom, checking for
//         // chunks
//         private final int nWidth;
//         private final int nHeight;

//         //~ Constructors --------------------------------------------------

//         protected MinChunkCheck ()
//                 throws ProcessingException
//         {
//             super("MinChunk", 0, 0, true, NO_CHUNK);

//             // Adjust chunk window according to system scale (problem, we
//             // have sheet scale and stave scale, not system scale...)
//             Scale scale = sheet.getScale();
//             nWidth = scale.fracToPixels(constants.chunkWidth);
//             nHeight = scale.fracToPixels(constants.chunkHeight);

//             int area = 2 * nWidth * nHeight;
//             setLowHigh(area * constants.chunkRatioLow.getValue(),
//                        area * constants.chunkRatioHigh.getValue());

//             if (logger.isDebugEnabled()) {
//                 logger.debug("MinPixLow=" + getLow() + ", MinPixHigh="
//                              + getHigh());
//             }
//         }

//         //~ Methods -------------------------------------------------------

//         protected double getValue (Stick stick)
//         {

//             // Retrieve the biggest stick chunk either at top or bottom
//             int res = 0;
//             res = Math.max(res, stick.getAliensAtStartFirst(nHeight, nWidth));
//             res = Math.max(res, stick.getAliensAtStartLast(nHeight, nWidth));
//             res = Math.max(res, stick.getAliensAtStopFirst(nHeight, nWidth));
//             res = Math.max(res, stick.getAliensAtStopLast(nHeight, nWidth));

//             if (logger.isDebugEnabled()) {
//                 logger.debug("MinAliens= " + res + " for " + stick);
//             }

//             return res;
//         }
//     }

    //----------------//
    // StemCheckSuite //
    //----------------//
    private class StemCheckSuite
            extends CheckSuite<Stick>
    {
        //~ Constructors --------------------------------------------------

        public StemCheckSuite (SystemInfo info)
                throws ProcessingException
        {
            super("Stems " + info, constants.minCheckResult.getValue());
            add(1, new MinLengthCheck());
            add(1, new MaxAspectCheck());
            add(1, new FirstAdjacencyCheck());
            add(1, new LastAdjacencyCheck());
            add(0, new LeftCheck(info.getLeft()));
            add(0, new RightCheck(info.getLeft() + info.getWidth()));
            add(2, new MinDensityCheck());

            // This step is much too expensive (and not really useful ...)
            ////add(2, new MinChunkCheck());

            if (logger.isDebugEnabled()) {
                dump();
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        //~ Instance variables --------------------------------------------

        Constant.Boolean displayFrame = new Constant.Boolean
                (true,
                 "Should we display a frame on the stem sticks");

        Constant.Integer maxDeltaLength = new Constant.Integer
                (4,
                 "Maximum difference in run length to be part of the same section");

        Constant.Double maxStemAspectLow = new Constant.Double
                (0.08,
                 "Low Maximum aspect ratio for a stem stick");

        Constant.Double maxStemAspectHigh = new Constant.Double
                (0.10,
                 "High Maximum aspect ratio for a stem stick");

        Constant.Double maxStemAdjacencyLow = new Constant.Double
                (0.60,
                 "Low Maximum adjacency ratio for a stem stick");

        Constant.Double maxStemAdjacencyHigh = new Constant.Double
                (0.70,
                 "High Maximum adjacency ratio for a stem stick");

        Scale.Fraction maxStemThickness = new Scale.Fraction
                (0.3,
                 "Maximum thickness of an interesting vertical stick");

        Scale.Fraction minStemLengthLow = new Scale.Fraction
                (2.5,
                 "Low Minimum length for a stem");

        Scale.Fraction minStemLengthHigh = new Scale.Fraction
                (3.5,
                 "High Minimum length for a stem");

        Constant.Double minCheckResult = new Constant.Double
                (0.50,
                 "Minimum result for suite of check");

        Scale.Fraction minForeWeight = new Scale.Fraction
                (1.25,
                 "Minimum foreground weight for a section to be kept");

        Constant.Double minDensityLow = new Constant.Double
                (0.8,
                 "Low Minimum density for a stem");

        Constant.Double minDensityHigh = new Constant.Double
                (0.9,
                 "High Minimum density for a stem");

        Scale.Fraction chunkHeight = new Scale.Fraction
                (0.33,
                 "Height of half area to look for chunks");

        Constant.Double chunkRatioLow = new Constant.Double
                (0.25,
                 "LowMinimum ratio of alien pixels to detect chunks");

        Constant.Double chunkRatioHigh = new Constant.Double
                (0.25,
                 "HighMinimum ratio of alien pixels to detect chunks");

        Scale.Fraction chunkWidth = new Scale.Fraction
                (0.33,
                 "Width of half area to look for chunks");

        Constants ()
        {
            initialize();
        }
    }
}
