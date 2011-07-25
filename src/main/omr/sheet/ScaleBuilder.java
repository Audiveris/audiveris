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

import omr.log.Logger;

import omr.math.Histogram;

import omr.run.Orientation;
import omr.run.RunsRetriever;

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

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import omr.score.common.PixelRectangle;

/**
 * Class <code>ScaleBuilder</code> encapsulates the computation of a sheet
 * scale, by adding the most frequent foreground run length to the most frequent
 * background run length, since this gives the average interline value.
 *
 * <p>If we have not been able to retrieve the main run length for background
 * or for foreground, then we suspect a wrong image format. In that case,
 * the safe action is to stop the processing, by throwing a StepException.</p>
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

    /** Second frequent length of background runs found in the sheet picture */
    private Integer secondBack;

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
    // getSecondBack //
    //---------------//
    /**
     * A (package private) lazy method to retrieve the second length of
     * background runs of pixels
     *
     * @return the second back length (perhaps null)
     * @throws StepException thrown if background runs could not be measured
     */
    Integer getSecondBack ()
        throws StepException
    {
        if (mainBack == null) {
            retrieveScale();
        }

        return secondBack;
    }

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
        RunsRetriever runsBuilder = new RunsRetriever(
            Orientation.VERTICAL,
            adapter);
        runsBuilder.retrieveRuns(
            new PixelRectangle(0, 0, picture.getWidth(), picture.getHeight()));

        // Report results to the user
        StringBuilder sb = new StringBuilder(sheet.getLogPrefix());
        sb.append("Scale main black is ")
          .append(mainFore);

        sb.append(", white is ")
          .append(mainBack);

        if (secondBack != null) {
            sb.append(", second white is ")
              .append(secondBack);
        }

        if ((mainFore != null) && (mainBack != null)) {
            sb.append(", main interline is ")
              .append(mainFore + mainBack);

            if (secondBack != null) {
                sb.append(", second interline is ")
                  .append(mainFore + secondBack);
            }
        }

        logger.info(sb.toString());
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Adapter //          Needed for retrieveRuns
    //---------//
    private class Adapter
        implements RunsRetriever.Adapter
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

            // Useful? 
            Arrays.fill(fore, 0);
            Arrays.fill(back, 0);
        }

        //~ Methods ------------------------------------------------------------

        //--------//
        // isFore //
        //--------//
        @Implement(RunsRetriever.Adapter.class)
        public boolean isFore (int level)
        {
            // Assuming black=0, white=255
            return level <= maxForeground;
        }

        //----------//
        // getLevel //
        //----------//
        @Implement(RunsRetriever.Adapter.class)
        public int getLevel (int coord,
                             int pos)
        {
            return picture.getPixel(pos, coord); // swap pos & coord
        }

        //---------//
        // backRun //
        //---------//
        @Implement(RunsRetriever.Adapter.class)
        public void backRun (int w,
                             int h,
                             int length)
        {
            back[length]++;
        }

        //---------//
        // foreRun //
        //---------//
        @Implement(RunsRetriever.Adapter.class)
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
        @Implement(RunsRetriever.Adapter.class)
        public void terminate ()
        {
            if (logger.isFineEnabled()) {
                logger.info("fore: " + Arrays.toString(fore));
                logger.info("back: " + Arrays.toString(back));
            }

            final double                  quorum = constants.minExtremaRatio.getValue();
            Histogram<Integer>            foreHisto = createHistogram(fore);
            List<Entry<Integer, Integer>> foreList = foreHisto.getMaxima(
                quorum);

            if (logger.isFineEnabled()) {
                logger.fine("Foreground peaks: " + foreList);
            }

            Histogram<Integer>            backHisto = createHistogram(back);
            List<Entry<Integer, Integer>> backList = backHisto.getMaxima(
                quorum);

            if (logger.isFineEnabled()) {
                logger.fine("Background peaks: " + backList);
            }

            // Remember the biggest buckets
            mainFore = foreList.get(0)
                               .getKey();
            mainBack = backList.get(0)
                               .getKey();

            if (backList.size() > 1) {
                secondBack = backList.get(1)
                                     .getKey();
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
                "Background" + " [" + mainBack +
                ((secondBack != null) ? (" & " + secondBack) : "") + "]");

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

        //-----------------//
        // createHistogram //
        //-----------------//
        private Histogram<Integer> createHistogram (int... vals)
        {
            Histogram<Integer> histo = new Histogram<Integer>();

            for (int i = 0; i < vals.length; i++) {
                histo.increaseCount(i, vals[i]);
            }

            return histo;
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
        final Constant.Boolean plotting = new Constant.Boolean(
            false,
            "Should we produce a chart on computed scale data ?");

        /** Minimum number of pixels per interline */
        final Constant.Integer minInterline = new Constant.Integer(
            "Pixels",
            11,
            "Minimum number of pixels per interline");

        /** Minimum ratio of pixels for extrema acceptance */
        final Constant.Ratio minExtremaRatio = new Constant.Ratio(
            0.1,
            "Minimum ratio of pixels for extrema acceptance ");
    }
}
