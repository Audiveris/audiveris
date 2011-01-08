//----------------------------------------------------------------------------//
//                                                                            //
//                       T e x t A r e a B r o w s e r                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.GlyphLag;
import omr.glyph.facets.Glyph;
import omr.glyph.text.TextArea;

import omr.lag.HorizontalOrientation;
import omr.lag.Oriented;
import omr.lag.VerticalOrientation;

import omr.log.Logger;

import omr.math.Histogram;

import omr.score.ui.ScoreDependent;

import omr.selection.SheetLocationEvent;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.sheet.ui.SheetsController;

import omr.step.Stepping;
import omr.step.Steps;

import omr.util.BasicTask;

import org.jdesktop.application.Action;
import org.jdesktop.application.Task;

import org.jfree.chart.*;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.*;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;

import java.awt.Rectangle;
import java.util.Map;

import javax.swing.WindowConstants;

/**
 * Class <code>TextAreaBrowser</code> is a user interface to interact with
 * potential text areas
 *
 * @author Hervé Bitteur
 */
public class TextAreaBrowser
    extends ScoreDependent
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        TextAreaBrowser.class);

    /** Singleton */
    private static volatile TextAreaBrowser INSTANCE;

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getInstance //
    //-------------//
    public static TextAreaBrowser getInstance ()
    {
        if (INSTANCE == null) {
            INSTANCE = new TextAreaBrowser();
        }

        return INSTANCE;
    }

    //------------//
    // alignTexts //
    //------------//
    public static void alignTexts (Sheet sheet)
    {
        TextArea area = new TextArea(
            null,
            null,
            sheet.getVerticalLag().createAbsoluteRoi(
                new Rectangle(sheet.getDimension())),
            new HorizontalOrientation());
        // subdivide
        area.subdivide();

        // Align text glyphs
        for (SystemInfo info : sheet.getSystems()) {
            info.retrieveSentences();
        }
    }

    //-------------------//
    // retrieveTextAreas //
    //-------------------//
    /**
     * Action that allows to launch the retrieval of all potential text areas in
     * the current sheet
     * @return the asynchronous task to execute
     */
    @Action(enabledProperty = SCORE_AVAILABLE)
    public Task retrieveTextAreas ()
    {
        return new TextAreasTask();
    }

    //-------------------------//
    // showHorizontalHistogram //
    //-------------------------//
    /**
     * Action that allows to display the histogram of horizontal projection of
     * foreground pixels found in the selected rectangle
     */
    @Action
    public void showHorizontalHistogram ()
    {
        processDesiredRectangle(new HorizontalOrientation());
    }

    //-----------------------//
    // showVerticalHistogram //
    //-----------------------//
    /**
     * Action that allows to display the histogram of vertical projection of
     * foreground pixels found in the selected rectangle
     */
    @Action
    public void showVerticalHistogram ()
    {
        processDesiredRectangle(new VerticalOrientation());
    }

    //---------//
    // addLine //
    //---------//
    private void addLine (XYSeriesCollection dataset,
                          TextArea           area,
                          String             title,
                          int                bucket)
    {
        XYSeries series = new XYSeries(title + "[" + bucket + "]");
        series.add(-bucket, 0);
        series.add(-bucket, area.getMaxHistoValue());
        dataset.addSeries(series);
    }

    //-------------------------//
    // processDesiredRectangle //
    //-------------------------//
    private void processDesiredRectangle (Oriented orientation)
    {
        Sheet sheet = SheetsController.getCurrentSheet();

        if (sheet == null) {
            return;
        }

        // Selected glyph, if any
        Glyph              glyph = null;

        // Check for a selected rectangle
        SheetLocationEvent sheetLocation = (SheetLocationEvent) sheet.getSelectionService()
                                                                     .getLastEvent(
            SheetLocationEvent.class);
        GlyphLag           lag = sheet.getVerticalLag();

        if (lag == null) {
            return;
        }

        Rectangle rect = (sheetLocation != null) ? sheetLocation.rectangle : null;

        if ((rect == null) || (rect.width == 0)) {
            // No real rectangle, so check for a selected glyph
            glyph = lag.getSelectedGlyph();

            if (glyph != null) {
                rect = glyph.getContourBox();
            }
        }

        if (rect == null) {
            return;
        }

        TextArea area = new TextArea(
            null,
            null,
            lag.createAbsoluteRoi(rect),
            orientation);

        showHistogram(area, orientation, glyph);
    }

    //---------------//
    // showHistogram //
    //---------------//
    private void showHistogram (TextArea area,
                                Oriented orientation,
                                Glyph    glyph)
    {
        Histogram<Integer> histo = area.getHistogram(orientation, glyph);

        if (logger.isFineEnabled()) {
            histo.print(System.out);
        }

        boolean   vertical = orientation.isVertical();
        Rectangle rect = area.getAbsoluteContour();

        // Projection data
        XYSeries dataSeries = new XYSeries("Foreground Pixels");

        for (Map.Entry<Integer, Integer> entry : histo.entrySet()) {
            dataSeries.add(
                vertical ? entry.getKey() : (-entry.getKey()),
                entry.getValue());
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(dataSeries);

        // Chart
        JFreeChart chart;

        if (vertical) {
            chart = ChartFactory.createXYAreaChart(
                "Vertical Projections", // Title
                "Abscissa",
                "Cumulated Pixels",
                dataset, // Dataset
                PlotOrientation.VERTICAL, // orientation,
                false, // Show legend
                false, // Show tool tips
                false // urls
            );
        } else {
            // Thresholds
            addLine(dataset, area, "Base", area.getBaseline());
            addLine(dataset, area, "Median", area.getMedianLine());
            addLine(dataset, area, "Top", area.getTopline());

            chart = ChartFactory.createXYLineChart(
                "Horizontal Projections top:" + area.getTopline() + " median:" +
                area.getMedianLine() + " base:" + area.getBaseline(), // Title
                "Ordinate",
                "Cumulated Pixels",
                dataset, // Dataset
                PlotOrientation.HORIZONTAL, // orientation,
                true, // Show legend
                true, // Show tool tips
                false // urls
            );
        }

        // Hosting frame
        ChartFrame frame = new ChartFrame("Histogram of " + rect, chart, true);
        frame.pack();
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------------//
    // TextAreasTask //
    //---------------//
    private static class TextAreasTask
        extends BasicTask
    {
        //~ Methods ------------------------------------------------------------

        @Override
        protected Void doInBackground ()
            throws InterruptedException
        {
            Sheet sheet = SheetsController.getCurrentSheet();
            alignTexts(sheet);
            Stepping.reprocessSheet(
                Steps.valueOf(Steps.PATTERNS),
                sheet,
                null,
                true);

            return null;
        }
    }
}
