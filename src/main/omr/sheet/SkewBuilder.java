//----------------------------------------------------------------------------//
//                                                                            //
//                           S k e w B u i l d e r                            //
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
import omr.glyph.GlyphSectionsBuilder;
import omr.glyph.facets.BasicStick;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;

import omr.lag.JunctionRatioPolicy;
import omr.lag.ui.LagView;
import omr.lag.ui.ScrollLagView;
import omr.lag.ui.SectionBoard;
import omr.lag.ui.SectionView;

import omr.log.Logger;

import omr.run.Orientation;
import omr.run.RunBoard;

import omr.sheet.picture.ImageFormatException;
import omr.sheet.picture.Picture;
import omr.sheet.ui.PixelBoard;

import omr.step.Step;
import omr.step.StepException;

import omr.stick.StickSection;

import omr.ui.BoardsPane;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.WindowConstants;

/**
 * Class <code>SkewBuilder</code> computes the skew angle of a given sheet
 * picture.
 *
 * @see Skew
 *
 * @author Hervé Bitteur
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

    /** Maximum acceptable section thickness */
    private int maxSectionThickness;

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
        Picture picture = sheet.getPicture();
        Scale   scale = sheet.getScale();

        // Parameters
        minSectionLength = scale.toPixels(constants.minSectionLength);
        maxSectionThickness = scale.mainFore();

        if (logger.isFineEnabled()) {
            logger.fine(
                "minSectionLength:" + minSectionLength +
                " maxSectionThickness:" + maxSectionThickness);
        }

        // Retrieve the horizontal lag of runs
        sLag = new GlyphLag("sLag", StickSection.class, Orientation.HORIZONTAL);

        GlyphSectionsBuilder lagBuilder = new GlyphSectionsBuilder(
            sLag,
            new JunctionRatioPolicy(constants.maxLengthRatio.getValue()));
        lagBuilder.createSections(
            sLag.getName(),
            picture,
            scale.toPixels(constants.minRunLength));

        // Detect long sticks
        detectSticks();

        if (logger.isFineEnabled()) {
            logger.fine("angle=" + angle);
        }

        // Produce histogram of slopes
        if (constants.plotting.getValue()) {
            displayChart();
        }

        logger.info(
            sheet.getLogPrefix() + "Skew angle is " + (float) angle +
            " radians");

        // Display the resulting lag
        if (constants.displayFrame.getValue() && (Main.getGui() != null)) {
            displayFrame();
        }

        // De-skew the picture if necessary
        if (Math.abs(angle) > constants.maxSkewAngle.getValue()) {
            try {
                picture.rotate(-angle);
                picture.setMaxForeground(sheet.getMaxForeground());
                sheet.getBench()
                     .recordImageDimension(
                    picture.getWidth(),
                    picture.getHeight());
            } catch (ImageFormatException ex) {
                throw new StepException(ex);
            }
        } else {
            logger.fine("No image rotation needed.");
        }

        // Report the computed info
        return new Skew(slope, sheet);
    }

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Build and display the slope histogram of lengthy horizontal sticks
     */
    public void displayChart ()
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
                    stick.dump();
                }

                double slope = stick.getOrientedLine()
                                    .getSlope();
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
            sheet.getId() + " (Slope Histogram)", // Title
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
            sheet.getId() + " - Slope",
            chart,
            true);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    //--------------//
    // isMajorChunk //
    //--------------//
    private boolean isMajorChunk (StickSection section)
    {
        // Check section length
        // Check /quadratic mean/ section thickness
        int length = section.getLength();

        return (length >= minSectionLength) &&
               ((section.getWeight() / length) <= maxSectionThickness);
    }

    //---------------//
    // areCompatible //
    //---------------//
    private boolean areCompatible (StickSection left,
                                   StickSection right)
    {
        // Make sure that two sections can be combined into a stick.
        // Tests are based on the angle between them,
        return Math.abs(left.getLine().getSlope() - right.getLine().getSlope()) <= constants.maxDeltaSlope.getValue();
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
                    stick = new BasicStick(sheet.getInterline());

                    // Include this section in the stick list
                    stick.addSection(section, Glyph.Linking.LINK_BACK);

                    // Register the stick in containing lag
                    // Store this new stick into the stick table
                    sticks.add((Stick) sLag.addGlyph(stick));
                }

                // Now, starting from this stick section, look at following
                // connected chunks
                for (GlyphSection gs : section.getTargets()) {
                    StickSection lnkSection = (StickSection) gs;

                    if (isMajorChunk(lnkSection) &&
                        areCompatible(section, lnkSection)) {
                        // If this section is already part of (another) stick,
                        // then merge this other stick with ours
                        if (lnkSection.getGlyph() != null) {
                            // Add the content of the other stick
                            if (lnkSection.getGlyph() != stick) {
                                Glyph other = lnkSection.getGlyph();
                                stick.addGlyphSections(
                                    other,
                                    Glyph.Linking.LINK_BACK);
                                sticks.remove(other);
                            }
                        } else {
                            // Let's add this section to the stick
                            stick.addSection(
                                lnkSection,
                                Glyph.Linking.LINK_BACK);
                        }
                    }
                }

                if (logger.isFineEnabled()) {
                    stick.dump();
                }
            }
        }

        // Now process these sticks
        if (!sticks.isEmpty()) {
            // Sort the sticks on their length, longest first
            Collections.sort(sticks, Stick.reverseLengthComparator);

            // Length of longest stick
            Iterator<Stick> it = sticks.iterator();
            Stick           longest = it.next();
            lengthThreshold = (int) (longest.getLength() * constants.sizeRatio.getValue());

            double slopeSum = longest.getOrientedLine()
                                     .getSlope() * longest.getLength();
            double slopeNb = longest.getLength();

            // Compute on sticks w/ significant length
            while (it.hasNext()) {
                stick = it.next();

                if (stick.getLength() >= lengthThreshold) {
                    slopeSum += (stick.getOrientedLine()
                                      .getSlope() * stick.getLength());
                    slopeNb += stick.getLength();
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
        MyView        view = new MyView();

        // Create a hosting frame for the view
        final String  unit = sheet.getId() + ":SkewBuilder";
        ScrollLagView slv = new ScrollLagView(view);
        BoardsPane    boards = new BoardsPane(
            new PixelBoard(unit, sheet),
            new RunBoard(unit, sLag),
            new SectionBoard(unit, sLag));

        sheet.getAssembly()
             .addViewTab(Step.SKEW_TAB, slv, boards);
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
            false,
            "Should we display a frame on Lags found ?");
        Constant.Angle   maxDeltaSlope = new Constant.Angle(
            0.05,
            "Maximum difference in slope between two sections in the same stick");
        Constant.Ratio   maxLengthRatio = new Constant.Ratio(
            2.5,
            "Maximum ratio in length for a run to be combined with an existing section");
        Constant.Angle   maxSkewAngle = new Constant.Angle(
            0.001,
            "Maximum value for skew angle before a rotation is performed");
        Scale.Fraction   minRunLength = new Scale.Fraction(
            2,
            "Minimum length for a run to be considered");
        Scale.Fraction   minSectionLength = new Scale.Fraction(
            5,
            "Minimum length for a section to be considered in skew computation");
        Constant.Boolean plotting = new Constant.Boolean(
            false,
            "Should we produce a plot about computed skew data ?");
        Constant.Ratio   sizeRatio = new Constant.Ratio(
            0.5,
            "Only sticks with length larger than this threshold are used for final computation");
    }

    //--------//
    // MyView //
    //--------//
    private final class MyView
        extends LagView<GlyphLag, GlyphSection>
    {
        //~ Constructors -------------------------------------------------------

        public MyView ()
        {
            super(sLag, null, null, sheet.getSelectionService());
            setName("SkewBuilder-View");
            colorizeAllSections();
        }

        //~ Methods ------------------------------------------------------------

        //---------------------//
        // colorizeAllSections //
        //---------------------//
        @Override
        public void colorizeAllSections ()
        {
            int viewIndex = lag.viewIndexOf(this);

            // Default colors for lag sections
            super.colorizeAllSections();

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
