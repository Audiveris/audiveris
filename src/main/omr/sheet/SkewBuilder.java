//-----------------------------------------------------------------------//
//                                                                       //
//                         S k e w B u i l d e r                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.sheet;

import omr.Main;
import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.lag.HorizontalOrientation;
import omr.lag.JunctionRatioPolicy;
import omr.lag.LagBuilder;
import omr.lag.LagView;
import omr.lag.SectionView;
import omr.stick.Stick;
import omr.stick.StickSection;
import omr.ui.BoardsPane;
import omr.ui.PixelBoard;
import omr.lag.ScrollLagView;
import omr.lag.SectionBoard;
import omr.util.Clock;
import omr.util.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import java.awt.Color;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.*;
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
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(SkewBuilder.class);

    //~ Instance variables ------------------------------------------------

    // The target of this computation
    private double angle;

    // Sheet for which computation is to be done
    private Sheet sheet;

    // Skew slope as computed
    private double slope;

    // Length Threshold, for slope computation
    private int lengthThreshold;

    // Sticks
    private List<Stick> sticks = new ArrayList<Stick>();

    // Lag of horizontal significant runs
    private GlyphLag hLag;
    private int minSectionLength;
    private int maxThickness;

    //~ Constructors ------------------------------------------------------

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

    //~ Methods -----------------------------------------------------------

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Build and display the slope histogram of lengthy horizontal sticks
     */
    public void displayChart()
    {
        writePlot();
    }

    //-----------//
    // buildInfo //
    //-----------//
    /**
     * Compute the skew of the sheet picture
     *
     * @return the skew info
     * @exception omr.ProcessingException to stop processing if needed
     */
    public Skew buildInfo ()
        throws omr.ProcessingException
    {
        // Needed out for previous steps
        Picture picture = sheet.getPicture();
        Scale scale = sheet.getScale();

        // Parameters
        minSectionLength = scale.fracToPixels(constants.minSectionLength);

        // Retrieve the horizontal lag of runs
        hLag = new GlyphLag(new HorizontalOrientation());
        hLag.setVertexClass(StickSection.class);
        new LagBuilder<GlyphLag, GlyphSection>().rip
                (hLag,
                 picture,
                 scale.fracToPixels(constants.minRunLength), // minRunLength
                 new JunctionRatioPolicy(constants.maxHeightRatio.getValue())); // maxHeightRatio

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
        if (constants.displayFrame.getValue() &&
            Main.getJui() != null) {
            displayFrame();
//         } else {
//             houseKeeping();
        }

        // De-skew the picture if necessary
        if (Math.abs(angle) > constants.maxSkewAngle.getValue()) {
            picture.rotate(-angle);
        } else {
            logger.info ("No image rotation needed.");
        }

        // Report the computed info
        return new Skew(angle);
    }

    //--------------//
    // isMajorChunk //
    //--------------//
    private boolean isMajorChunk (StickSection section)
    {
        // Check section length
        // Check /quadratic mean/ section thickness
        int length = section.getLength();
        boolean result = (length >= minSectionLength)
                         && ((section.getWeight() / length) <= maxThickness);

        return result;
    }

    //---------------//
    // areCompatible //
    //---------------//
    private boolean areCompatible (StickSection left,
                                   StickSection right)
    {
        // Make sure that two sections can be combined into a stick.
        // Tests are based on the angle between them,
        boolean result = Math.abs(left.getLine().getSlope()
                                  - right.getLine().getSlope())
                         <= constants.maxDeltaSlope.getValue();

        return result;
    }

    //--------------//
    // displayFrame //
    //--------------//
    private void displayFrame()
    {
        // Create a view
        LagView view = new SkewLagView(hLag);
        view.colorize();

        // Create a hosting frame for the view
        sheet.getAssembly().addViewTab
            ("Skew",
             new ScrollLagView(view),
             new BoardsPane
             (view,
              new PixelBoard(),
              new SectionBoard(hLag.getLastVertexId())));
    }

    //---------------//
    // detectSticks //
    //---------------//
    private void detectSticks ()
    {
        Stick stick;

        // Try to aggregate sections into sticks.
        // Visit all sections of the lag
        for (GlyphSection s : hLag.getVertices()) {
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
                    stick = (Stick) hLag.createGlyph(Stick.class);

                    // Store this new stick into the stick table
                    sticks.add(stick);

                    // Include this section in the stick list
                    stick.addSection(section, /* link => */ true);
                }

                // Now, from this stick section,
                // Look at following connected chunks
                for (GlyphSection gs : section.getTargets()) {
                    StickSection lnkSection = (StickSection) gs;
                    if (isMajorChunk(lnkSection)
                        && areCompatible(section, lnkSection)) {
                        // If this section is already part of (another)
                        // stick, then merge this other stick with ours
                        if (lnkSection.getGlyph() != null) {
                            // Add the content of the other stick
                            if (lnkSection.getGlyph() != stick) {
                                stick.addGlyphSections(lnkSection.getGlyph(),
                                                       /* linkSections => */ true);
                                sticks.remove(lnkSection.getGlyph());
                            }
                        } else {
                            // Let's add this section to the stick
                            stick.addSection(lnkSection, /* link => */ true);
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
            Collections.sort(sticks,
                             new Comparator<Stick>()
                             {
                                 public int compare (Stick s1,
                                                     Stick s2)
                                 {
                                     return s2.getLength() - s1.getLength();
                                 }
                             });

            // Length of longest stick
            Iterator<Stick> it = sticks.iterator();
            Stick longest = it.next();
            lengthThreshold = (int) ((double) longest.getLength()
                                     * constants.sizeRatio.getValue());

            double slopeSum = longest.getLine().getSlope()
                              * longest.getLength();
            double slopeNb = longest.getLength();

            // Compute on sticks w/ significant length
            while (it.hasNext()) {
                stick = it.next();

                if (stick.getLength() >= lengthThreshold) {
                    slopeSum += (stick.getLine().getSlope()
                                 * stick.getLength());
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
    // houseKeeping //
    //--------------//
//     private void houseKeeping ()
//     {
//         sticks.clear();
//         sticks = null;
//         hLag = null;
//     }

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
        double[] histo = new double[MAX_INDEX];

        for (int i = MAX_INDEX - 1; i >= 0; i--) {
            histo[i] = 0;
        }

        for (Stick stick : sticks) {
            if (stick.getLength() >= lengthThreshold) {
                if (logger.isFineEnabled()) {
                    stick.dump(false);
                }

                double slope = stick.getLine().getSlope();
                int length = stick.getLength();
                int index = (int) (slope * RESOLUTION) + (MAX_INDEX / 2);

                if ((index >= 0) && (index < MAX_INDEX)) {
                    histo[index] += stick.getLength();
                }
            } else {
                break;
            }
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries slopeSeries = new XYSeries("Slope");
        for (int i = 0; i < MAX_INDEX; i++) {
            slopeSeries.add(i - (MAX_INDEX / 2), histo[i]);
        }
        dataset.addSeries(slopeSeries);

        // Chart
        JFreeChart chart = ChartFactory.createXYLineChart
            (sheet.getName() + " Slope Histogram", // Title
             "Slope [" + (float) (RESOLUTION * angle)
             + " Radians/" + RESOLUTION + "]", // X-Axis label
             "Counts",                 // Y-Axis label
             dataset,                   // Dataset
             PlotOrientation.VERTICAL,  // orientation,
             true,                      // Show legend
             false,                     // Show tool tips
             false                      // urls
             );

        // Hosting frame
        ChartFrame frame = new ChartFrame(sheet.getName() + " Slope",
                                          chart, true) ;
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    //~ Classes --------------------------------------------------------------

    //--------------//
    // SkewLagView //
    //--------------//
    private class SkewLagView
        extends LagView<GlyphLag, GlyphSection>
    {
        //~ Constructors -----------------------------------------------------

        //-------------//
        // SkewLagView //
        //-------------//
        public SkewLagView (GlyphLag lag)
        {
            super(lag, null);
        }

        //~ Methods -------------------------------------------------------

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
                    SectionView view = (SectionView) section.getViews().get(viewIndex);
                    view.setColor(color);
                }
            }
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        Constant.Double maxSkewAngle = new Constant.Double
                (0.001,
                 "Maximum value for skew angle before a rotation is performed");

        Scale.Fraction minRunLength = new Scale.Fraction
                (1,
                 "Minimum length for a run to be considered");

        Scale.Fraction minSectionLength = new Scale.Fraction
                (5,
                 "Minimum length for a section to be considered in skew computation");

        Constant.Double maxDeltaSlope = new Constant.Double
                (0.05,
                 "Maximum difference in slope between two sections in the same stick");

        Constant.Double sizeRatio = new Constant.Double
                (0.5,
                 "Only sticks with length higher than this threshold are used for final computation");

        Constant.Double maxHeightRatio = new Constant.Double
                (2.5,
                 "Maximum ratio in height for a run to be combined with an existing section");

        Constant.Boolean plotting = new Constant.Boolean
                (false,
                 "Should we produce a GnuPlot about computed skew data ?");

        Constant.Boolean displayFrame = new Constant.Boolean
                (false,
                 "Should we display a frame on Lags found ?");

        Constants ()
        {
            initialize();
        }
    }
}
