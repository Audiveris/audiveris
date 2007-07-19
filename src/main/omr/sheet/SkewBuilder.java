//----------------------------------------------------------------------------//
//                                                                            //
//                           S k e w B u i l d e r                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;

import omr.lag.HorizontalOrientation;
import omr.lag.JunctionRatioPolicy;
import omr.lag.LagView;
import omr.lag.RunBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;
import omr.lag.SectionView;
import omr.lag.SectionsBuilder;
import static omr.selection.SelectionTag.*;

import omr.step.StepException;

import omr.stick.Stick;
import omr.stick.StickSection;

import omr.ui.BoardsPane;
import omr.ui.PixelBoard;

import omr.util.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import java.awt.Color;
import java.util.*;

import javax.swing.WindowConstants;

/**
 * Class <code>SkewBuilder</code> computes the skew angle of a given sheet
 * picture.
 *
 * @see Skew
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SkewBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SkewBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** (Skewed) lag of horizontal significant runs */
    private GlyphLag sLag;

    /** Sticks */
    private List<Stick> sticks = new ArrayList<Stick>();

    /** Sheet for which computation is to be done */
    private Sheet sheet;

    /** The target of this computation */
    private double angle;

    /** Skew slope as computed */
    private double slope;

    /** Length Threshold, for slope computation */
    private int lengthThreshold;

    /** Maximum accetable section thickness */
    private int maxThickness;

    /** Minimum acceptable section length */
    private int minSectionLength;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SkewBuilder //
    //-------------//
    /**
     * This method is used to retrieve the skew angle in the hard way, by
     * processing the picture pixels.
     *
     * @param sheet the sheet to process
     */
    public SkewBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Compute the skew of the sheet picture
     *
     * @return the skew info
     * @exception StepException to stop processing if needed
     */
    public Skew buildInfo ()
        throws StepException
    {
        // Needed out for previous steps
        Picture picture = sheet.getPicture();
        Scale   scale = sheet.getScale();

        // Parameters
        minSectionLength = scale.toPixels(constants.minSectionLength);

        // Retrieve the horizontal lag of runs
        sLag = new GlyphLag("sLag", new HorizontalOrientation());
        sLag.setVertexClass(StickSection.class);

        SectionsBuilder<GlyphLag, GlyphSection> lagBuilder;
        lagBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
            sLag,
            new JunctionRatioPolicy(constants.maxHeightRatio.getValue())); // maxHeightRatio
        lagBuilder.createSections(
            picture,
            scale.toPixels(constants.minRunLength)); // minRunLength

        // Detect long sticks
        maxThickness = scale.mainFore();
        detectSticks();

        if (logger.isFineEnabled()) {
            logger.fine("angle=" + angle);
        }

        // Produce histogram of slopes
        if (constants.plotting.getValue()) {
            writePlot();
        }

        logger.info("Skew angle is " + (float) angle + " radians");

        // Display the resulting lag
        if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
            displayFrame();
        }

        // De-skew the picture if necessary
        if (Math.abs(angle) > constants.maxSkewAngle.getValue()) {
            try {
                picture.rotate(-angle);
            } catch (ImageFormatException ex) {
                throw new StepException(ex);
            }
        } else {
            logger.fine("No image rotation needed.");
        }

        // Report the computed info
        return new Skew(angle);
    }

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Build and display the slope histogram of lengthy horizontal sticks
     */
    public void displayChart ()
    {
        writePlot();
    }

    //---------------//
    // areCompatible //
    //---------------//
    private boolean areCompatible (StickSection left,
                                   StickSection right)
    {
        // Make sure that two sections can be combined into a stick.
        // Tests are based on the angle between them,
        boolean result = Math.abs(
            left.getLine().getSlope() - right.getLine().getSlope()) <= constants.maxDeltaSlope.getValue();

        return result;
    }

    //---------------//
    // detectSticks //
    //---------------//
    private void detectSticks ()
    {
        Stick stick;

        // Try to aggregate sections into sticks.  Visit all sections of the lag
        for (GlyphSection s : sLag.getVertices()) {
            StickSection section = (StickSection) s;

            if (logger.isFineEnabled()) {
                logger.fine(section.toString());
            }

            // We consider only significant chunks
            if (isMajorChunk(section)) {
                // Is this chunk already part of a stick ?
                if (section.getGlyph() != null) {
                    // If so, reuse the stick
                    stick = (Stick) section.getGlyph();
                } else {
                    // Otherwise, start a brand new stick
                    stick = new Stick(sheet.getInterline());

                    // Include this section in the stick list
                    stick.addSection(section, /* link => */
                                     true);

                    // Register the stick in containing lag
                    // Store this new stick into the stick table
                    sticks.add((Stick) sLag.addGlyph(stick));
                }

                // Now, from this stick section, Look at following connected
                // chunks
                for (GlyphSection gs : section.getTargets()) {
                    StickSection lnkSection = (StickSection) gs;

                    if (isMajorChunk(lnkSection) &&
                        areCompatible(section, lnkSection)) {
                        // If this section is already part of (another) stick,
                        // then merge this other stick with ours
                        if (lnkSection.getGlyph() != null) {
                            // Add the content of the other stick
                            if (lnkSection.getGlyph() != stick) {
                                stick.addGlyphSections(
                                    lnkSection.getGlyph(),
                                    /* linkSections => */ true);
                                sticks.remove(lnkSection.getGlyph());
                            }
                        } else {
                            // Let's add this section to the stick
                            stick.addSection(lnkSection, /* link => */
                                             true);
                        }
                    }
                }

                if (logger.isFineEnabled()) {
                    stick.dump(false);
                }
            }
        }

        // Now process these sticks
        if (sticks.size() > 0) {
            // Sort the sticks on their length, longest first (so the swap)
            Collections.sort(
                sticks,
                new Comparator<Stick>() {
                        public int compare (Stick s1,
                                            Stick s2)
                        {
                            return s2.getLength() - s1.getLength();
                        }
                    });

            // Length of longest stick
            Iterator<Stick> it = sticks.iterator();
            Stick           longest = it.next();
            lengthThreshold = (int) ((double) longest.getLength() * constants.sizeRatio.getValue());

            double slopeSum = longest.getLine()
                                     .getSlope() * longest.getLength();
            double slopeNb = longest.getLength();

            // Compute on sticks w/ significant length
            while (it.hasNext()) {
                stick = it.next();

                if (stick.getLength() >= lengthThreshold) {
                    slopeSum += (stick.getLine()
                                      .getSlope() * stick.getLength());
                    slopeNb += stick.getLength();

                    //stick.dump (false);
                } else {
                    break; // Remaining ones are shorter!
                }
            }

            slope = slopeSum / slopeNb;
            angle = Math.atan(slope);
        }
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame ()
    {
        // Create a view
        LagView view = new SkewLagView();
        view.colorize();

        // Create a hosting frame for the view
        final String unit = sheet.getRadix() + ":SkewBuilder";
        sheet.getAssembly()
             .addViewTab(
            "Skew",
            new ScrollLagView(view),
            new BoardsPane(
                sheet,
                view,
                new PixelBoard(unit),
                new RunBoard(unit, sheet.getSelection(SKEW_RUN)),
                new SectionBoard(
                    unit,
                    sLag.getLastVertexId(),
                    sheet.getSelection(SKEW_SECTION),
                    sheet.getSelection(SKEW_SECTION_ID))));
    }

    //--------------//
    // isMajorChunk //
    //--------------//
    private boolean isMajorChunk (StickSection section)
    {
        // Check section length
        // Check /quadratic mean/ section thickness
        int     length = section.getLength();
        boolean result = (length >= minSectionLength) &&
                         ((section.getWeight() / length) <= maxThickness);

        return result;
    }

    //-----------//
    // writePlot //
    //-----------//
    private void writePlot ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("Slope computation based on following sticks :");
        }

        final int RESOLUTION = 10000;

        // Range -0.4 .. +0.4 Radians (-24 .. +24 Degrees)
        final int MAX_INDEX = 400;
        double[]  histo = new double[MAX_INDEX];

        for (int i = MAX_INDEX - 1; i >= 0; i--) {
            histo[i] = 0;
        }

        for (Stick stick : sticks) {
            if (stick.getLength() >= lengthThreshold) {
                if (logger.isFineEnabled()) {
                    stick.dump(false);
                }

                double slope = stick.getLine()
                                    .getSlope();
                int    length = stick.getLength();
                int    index = (int) (slope * RESOLUTION) + (MAX_INDEX / 2);

                if ((index >= 0) && (index < MAX_INDEX)) {
                    histo[index] += stick.getLength();
                }
            } else {
                break;
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries           slopeSeries = new XYSeries("Slope");

        for (int i = 0; i < MAX_INDEX; i++) {
            slopeSeries.add(i - (MAX_INDEX / 2), histo[i]);
        }

        dataset.addSeries(slopeSeries);

        // Chart
        JFreeChart chart = ChartFactory.createXYLineChart(
            sheet.getRadix() + " (Slope Histogram)", // Title
            "Slope [" + (float) (RESOLUTION * angle) + " Radians/" +
            RESOLUTION + "]", // X-Axis label
            "Counts", // Y-Axis label
            dataset, // Dataset
            PlotOrientation.VERTICAL, // orientation,
            true, // Show legend
            false, // Show tool tips
            false // urls
        );

        // Hosting frame
        ChartFrame frame = new ChartFrame(
            sheet.getRadix() + " - Slope",
            chart,
            true);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        Constant.Boolean displayFrame = new Constant.Boolean(
            false,
            "Should we display a frame on Lags found ?");
        Constant.Angle   maxDeltaSlope = new Constant.Angle(
            0.05,
            "Maximum difference in slope between two sections in the same stick");
        Constant.Ratio   maxHeightRatio = new Constant.Ratio(
            2.5,
            "Maximum ratio in height for a run to be combined with an existing section");
        Constant.Angle   maxSkewAngle = new Constant.Angle(
            0.001,
            "Maximum value for skew angle before a rotation is performed");
        Scale.Fraction   minRunLength = new Scale.Fraction(
            1,
            "Minimum length for a run to be considered");
        Scale.Fraction   minSectionLength = new Scale.Fraction(
            5,
            "Minimum length for a section to be considered in skew computation");
        Constant.Boolean plotting = new Constant.Boolean(
            false,
            "Should we produce a GnuPlot about computed skew data ?");
        Constant.Ratio   sizeRatio = new Constant.Ratio(
            0.5,
            "Only sticks with length higher than this threshold are used for final computation");
    }

    //--------------//
    // SkewLagView //
    //--------------//
    private class SkewLagView
        extends LagView<GlyphLag, GlyphSection>
    {
        //-------------//
        // SkewLagView //
        //-------------//
        public SkewLagView ()
        {
            super(sLag, null, null);
            setName("SkewBuilder-View");

            // Inject selection dependencies into this hlag, only when this
            // display is activated, since there is no other consumer

            // Location input / output
            setLocationSelection(sheet.getSelection(SHEET_RECTANGLE));

            // Run input & Section input/output
            sLag.setLocationSelection(sheet.getSelection(SHEET_RECTANGLE));
            sLag.setRunSelection(sheet.getSelection(SKEW_RUN));
            sLag.setSectionSelection(sheet.getSelection(SKEW_SECTION));

            sheet.getSelection(SHEET_RECTANGLE)
                 .addObserver(sLag);
            sheet.getSelection(SKEW_SECTION)
                 .addObserver(sLag);
            sheet.getSelection(SKEW_SECTION_ID)
                 .addObserver(sLag);
        }

        //----------//
        // colorize //
        //----------//
        @Override
        public void colorize ()
        {
            if (logger.isFineEnabled()) {
                logger.fine("colorize");
            }

            // Default colors for lag sections
            super.colorize();

            // Colorize the sections of the sticks
            for (Stick stick : sticks) {
                // Determine suitable color
                Color color;

                if (stick.getLength() >= lengthThreshold) {
                    color = Color.red; // Sticks used for slope computation
                } else {
                    color = Color.pink; // Other sticks
                }

                for (GlyphSection section : stick.getMembers()) {
                    SectionView view = (SectionView) section.getView(viewIndex);
                    view.setColor(color);
                }
            }
        }
    }
}
