//----------------------------------------------------------------------------//
//                                                                            //
//                          S c a l e B u i l d e r                           //
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

import omr.lag.RunsBuilder;

import omr.log.Logger;

import omr.score.Score;

import omr.sheet.picture.Picture;
import omr.sheet.ui.SheetsController;

import omr.step.StepException;

import omr.util.Implement;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import java.awt.Rectangle;

import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

/**
 * Class <code>ScaleBuilder</code> encapsulates the computation of a sheet
 * scale, by adding the most frequent foreground run length to the most frequent
 * background run length, since this gives the average interline value.
 *
 * <p>If we have been unable to retrieve the main run length for background
 * or for foreground, then we suspect a wrong image format. In that case,
 * the safe action is to stop the processing, by throwing a StepException</p>
 *
 * <p>Otherwise, if the main interline value is below a certain threshold
 * (see constants.minInterline), then we suspect that the picture is not
 * a music sheet (it may rather be an image, a page of text, ...). In that
 * case and if the sheet at hand is part of a multi-page score, we propose to
 * simply discard this sheet.</p>
 *
 * <p>This class is a companion of {@link Scale} class. It is kept separate
 * to save the loading of this class when computation is not required.</p>
 *
 * @see omr.sheet.Scale
 *
 * @author Hervé Bitteur
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

    /** Most frequent length of background runs found in the sheet picture */
    private Integer mainBack;

    /** Most frequent length of foreground runs found in the sheet picture */
    private Integer mainFore;

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

    //----------------//
    // checkInterline //
    //----------------//
    /**
     * Check global interline value, to detect pictures with too low
     * resolution or pictures which do not represent music staves
     * @param interline the retrieved interline value
     */
    void checkInterline (int interline)
        throws StepException
    {
        if (interline < constants.minInterline.getValue()) {
            Score score = sheet.getScore();

            if (score.isMultiPage()) {
                String msg = sheet.getId() + "\nMain interline value is " +
                             interline + " pixels" +
                             "\nThis sheet does not seem to contain staff lines";
                logger.warning(msg);

                if (Main.getGui() != null) {
                    // Ask user for confirmation
                    SheetsController.getInstance()
                                    .showAssembly(sheet);

                    if (Main.getGui()
                            .displayModelessConfirm(
                        msg + "\nOK for discarding this sheet?") == JOptionPane.OK_OPTION) {
                        sheet.remove();
                        logger.info("Removed sheet " + sheet.getId());
                        throw new StepException("Sheet removed");
                    }
                } else {
                    // No user available, let's remove this page
                    sheet.remove();
                    logger.info("Removed sheet " + sheet.getId());
                    throw new StepException("Sheet removed");
                }
            } else {
                String msg = sheet.getId() + "\nWith only " + interline +
                             " pixels between two staff lines," +
                             " your picture resolution is too low!" +
                             "\nPlease rescan at higher resolution (300dpi should be OK)";
                logger.warning(msg);

                if (Main.getGui() != null) {
                    Main.getGui()
                        .displayWarning(msg);
                }
            }
        }
    }

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
     * @throws StepException thrown if background runs could not be measured
     */
    int getMainBack ()
        throws StepException
    {
        if (mainBack == null) {
            retrieveScale();
        }

        if (mainBack == null) {
            logger.warning(
                "Unable to retrieve white runs, check your image format");
            throw new StepException("Illegal background values");
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
     * @throws StepException thrown if foreground runs could not be measured
     */
    int getMainFore ()
        throws StepException
    {
        if (mainFore == null) {
            retrieveScale();
        }

        if (mainFore == null) {
            logger.warning(
                "Unable to retrieve black runs, check your image format");
            throw new StepException("Illegal foreground values");
        }

        return mainFore;
    }

    //---------------//
    // retrieveScale //
    //---------------//
    /**
     * Retrieve the global scale values by processing the provided picture runs
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

        // Report results to the user
        StringBuilder sb = new StringBuilder(sheet.getLogPrefix());
        sb.append("Scale black is ")
          .append(mainFore);
        sb.append(", white is ")
          .append(mainBack);

        if ((mainFore != null) && (mainBack != null)) {
            sb.append(", interline is ")
              .append(mainFore + mainBack);
        }

        logger.info(sb.toString());
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
            picture = sheet.getPicture();

            if (picture.getMaxForeground() != -1) {
                maxForeground = picture.getMaxForeground();
            } else {
                maxForeground = sheet.getMaxForeground();
            }

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
                sheet.getId() + " (Run Lengths)", // Title
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
                sheet.getId() + " - Runs",
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
        //~ Static fields/initializers -----------------------------------------

        /** Should we produce a chart on computed scale data ? */
        static final Constant.Boolean plotting = new Constant.Boolean(
            false,
            "Should we produce a chart on computed scale data ?");

        /** Minimum number of pixels per interline */
        static final Constant.Integer minInterline = new Constant.Integer(
            "Pixels",
            13,
            "Minimum number of pixels per interline");
    }
}
