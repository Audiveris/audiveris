//----------------------------------------------------------------------------//
//                                                                            //
//                          S c a l e B u i l d e r                           //
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

import omr.lag.RunsBuilder;

import omr.step.StepException;

import omr.util.Implement;
import omr.util.Logger;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import java.awt.Rectangle;

import javax.swing.WindowConstants;

/**
 * Class <code>ScaleBuilder</code> encapsulates the computation of a sheet
 * scale. This is kept separate from the simple <code>Scale</code> class, to
 * save the loading of this when computation is not required.
 *
 * @see omr.sheet.Scale
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScaleBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScaleBuilder.class);

    //~ Instance fields --------------------------------------------------------

    /** Adapter for reading runs */
    private Adapter adapter;

    /** Related sheet */
    private Sheet sheet;

    /** Most frequent background vertical run */
    private int mainBack = -1;

    /**
     * Most frequent run lengths for foreground & background runs as read from
     * the sheet picture. They are initialized to -1, so as to detect if they
     * have been computed or not.
     */
    private int mainFore = -1;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScaleBuilder //
    //--------------//
    /**
     * (package private) constructor, to enable scale computation on a given
     * sheet
     *
     * @param sheet the sheet at hand
     */
    ScaleBuilder (Sheet sheet)
    {
        this.sheet = sheet;
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // displayChart //
    //--------------//
    /**
     * Build and display the scale histogram
     */
    public void displayChart ()
    {
        if (adapter != null) {
            adapter.writePlot();
        } else {
            logger.warning("No scale adapter available");
        }
    }

    //-------------//
    // getMainBack //
    //-------------//
    /**
     * A (package private) lazy method to retrieve the main length of background
     * runs of pixels
     *
     * @return the main back length
     * @throws StepException
     */
    int getMainBack ()
        throws StepException
    {
        if (mainBack == -1) {
            retrieveScale();
        }

        return mainBack;
    }

    //-------------//
    // getMainFore //
    //-------------//
    /**
     * A (package private) lazy method to retrieve the main length of foreground
     * runs of pixels
     *
     * @return the main fore length
     * @throws StepException
     */
    int getMainFore ()
        throws StepException
    {
        if (mainFore == -1) {
            retrieveScale();
        }

        return mainFore;
    }

    //---------------//
    // retrieveScale //
    //---------------//
    /**
     * Create a scale entity, by processing the provided sheet picture.
     *
     * @throws StepException
     */
    private void retrieveScale ()
        throws StepException
    {
        Picture picture = sheet.getPicture();
        adapter = new Adapter(sheet, picture.getHeight() - 1);

        // Read the picture runs
        RunsBuilder runsBuilder = new RunsBuilder(adapter);
        runsBuilder.createRuns(
            new Rectangle(0, 0, picture.getHeight(), picture.getWidth()));

        logger.info(
            "Scale black is " + mainFore + ", white is " + mainBack +
            ", interline is " + (mainFore + mainBack));

        // Check picture resolution
        if ((mainFore + mainBack) < constants.minInterline.getValue()) {
            String msg = "Picture resolution is too low: " +
                         (mainFore + mainBack);
            logger.warning(msg);

            if (Main.getGui() != null) {
                Main.getGui()
                    .displayWarning(msg);
            }

            throw new StepException(msg);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Adapter //          Needed for createRuns
    //---------//
    private class Adapter
        implements RunsBuilder.Reader
    {
        //~ Instance fields ----------------------------------------------------

        private Picture picture;
        private Sheet   sheet;
        private int[]   back;
        private int[]   fore;
        private int     maxForeground;

        //~ Constructors -------------------------------------------------------

        //---------//
        // Adapter //
        //---------//
        public Adapter (Sheet sheet,
                        int   hMax)
        {
            this.sheet = sheet;
            this.picture = sheet.getPicture();

            maxForeground = picture.getMaxForeground();

            // Allocate histogram counters
            fore = new int[hMax + 2];
            back = new int[hMax + 2];

            for (int i = fore.length - 1; i >= 0; i--) {
                fore[i] = 0;
                back[i] = 0;
            }
        }

        //~ Methods ------------------------------------------------------------

        //--------//
        // isFore //
        //--------//
        @Implement(RunsBuilder.Reader.class)
        public boolean isFore (int level)
        {
            // Assuming black=0, white=255
            return level <= maxForeground;
        }

        //----------//
        // getLevel //
        //----------//
        @Implement(RunsBuilder.Reader.class)
        public int getLevel (int coord,
                             int pos)
        {
            return picture.getPixel(pos, coord); // swap pos & coord
        }

        //---------//
        // backRun //
        //---------//
        @Implement(RunsBuilder.Reader.class)
        public void backRun (int w,
                             int h,
                             int length)
        {
            back[length]++;
        }

        //---------//
        // foreRun //
        //---------//
        @Implement(RunsBuilder.Reader.class)
        public void foreRun (int w,
                             int h,
                             int length,
                             int cumul)
        {
            fore[length]++;
        }

        //-----------//
        // terminate //
        //-----------//
        @Implement(RunsBuilder.Reader.class)
        public void terminate ()
        {
            // Determine the biggest buckets
            int maxFore = 0;
            int maxBack = 0;

            for (int i = fore.length - 1; i >= 0; i--) {
                if (fore[i] > maxFore) {
                    maxFore = fore[i];
                    mainFore = i;
                }

                if (back[i] > maxBack) {
                    maxBack = back[i];
                    mainBack = i;
                }
            }

            // Print plot if needed
            if (constants.plotting.getValue()) {
                writePlot();
            }
        }

        //-----------//
        // writePlot //
        //-----------//
        public void writePlot ()
        {
            XYSeriesCollection dataset = new XYSeriesCollection();
            int                upper = Math.min(
                fore.length,
                mainBack + (mainBack / 2));

            // Foreground
            XYSeries foreSeries = new XYSeries(
                "Foreground" + " [" + mainFore + "]");

            for (int i = 0; i < upper; i++) {
                foreSeries.add(i, fore[i]);
            }

            dataset.addSeries(foreSeries);

            // Background
            XYSeries backSeries = new XYSeries(
                "Background" + " [" + mainBack + "]");

            for (int i = 0; i < upper; i++) {
                backSeries.add(i, back[i]);
            }

            dataset.addSeries(backSeries);

            // Chart
            JFreeChart chart = ChartFactory.createXYLineChart(
                sheet.getRadix() + " (Run Lengths)", // Title
                "Lengths", // X-Axis label
                "Numbers", // Y-Axis label
                dataset, // Dataset
                PlotOrientation.VERTICAL, // orientation,
                true, // Show legend
                false, // Show tool tips
                false // urls
            );

            // Hosting frame
            ChartFrame frame = new ChartFrame(
                sheet.getRadix() + " - Runs",
                chart,
                true);
            frame.pack();
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            RefineryUtilities.centerFrameOnScreen(frame);
            frame.setVisible(true);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Should we produce a chart on computed scale data ? */
        Constant.Boolean plotting = new Constant.Boolean(
            false,
            "Should we produce a chart on computed scale data ?");

        /** Minimum number of pixels per interline */
        Constant.Integer minInterline = new Constant.Integer(
            "Pixels",
            15,
            "Minimum number of pixels per interline");
    }
}
