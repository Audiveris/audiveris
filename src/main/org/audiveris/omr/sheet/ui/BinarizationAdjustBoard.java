//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                         B i n a r i z a t i o n A d j u s t B o a r d                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.image.AdaptiveDescriptor;
import org.audiveris.omr.image.FilterDescriptor;
import org.audiveris.omr.image.FilterParam;
import org.audiveris.omr.image.GlobalDescriptor;
import org.audiveris.omr.step.BinaryStep;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Picture.SourceKey;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.field.LDoubleField;
import org.audiveris.omr.ui.field.LIntegerField;
import org.audiveris.omr.ui.field.LLabel;
import org.audiveris.omr.ui.field.LRadioButton;
import org.audiveris.omr.ui.field.LTextField;
import org.audiveris.omr.ui.selection.UserEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.NoSuchElementException;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class <code>BinarizationAdjustBoard</code> allows users to
 * adjust binarization settings and filters on the image.
 *
 * @author Mike Porter, Hervé Bitteur
 */
public class BinarizationAdjustBoard
        extends Board
        implements ActionListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BinarizationAdjustBoard.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The related sheet. */
    private final Sheet sheet;

    /** Radio button to select adaptive filter.  */
    private final LRadioButton adaptiveFilterRadioButton = 
    new LRadioButton(
        "Use adaptive filter (recommended)", 
        "Converts a pixel to black and white by looking at its surrounding pixels"
    );

    /** Radio button to select global filter.  */
    private final LRadioButton globalFilterRadioButton = 
    new LRadioButton("Use global filter", "Converts to black and white by using one value for the entire image");

    /** Input field for global filter's threshold.  */
    private final LIntegerField globalThresholdValue = 
        new LIntegerField("Threshold", "Gray threshold");
    
    /** Input field for adaptive filter's mean.  */
    private final LDoubleField adaptiveMeanValue = 
        new LDoubleField("Mean", "Coefficient for mean value", "%.3f");
    
    /** Input field for adaptive filter's standard deviation.  */
    private final LDoubleField adaptiveStdDevValue = 
        new LDoubleField("Std Dev", "Coefficient for standard deviation value", "%.3f");

    /** Apply button.  */
    private final JButton applyButton = new JButton("Apply (enter)");

    /** Reset button.  */
    private final JButton resetButton = new JButton("Reset");

    /** Apply to all pages button.  */
    private final JButton applyToAllSheetsButton = new JButton("Apply to all sheets");

    /** Reference to the last filter used on this board.  */
    private FilterDescriptor previousFilter;

    /** Equal to false if this sheet has no gray source image to binarize. */
    private boolean noGraySource;

    /** Label for when no gray source image is present */
    private LLabel noGraySourceLabel = new LLabel("No source image available for binarization", "No source image available to run binarization on");
    

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new BinarizationAdjustBoard object.
     *
     * @param sheet related sheet
     */
    public BinarizationAdjustBoard (Sheet sheet)
    {
        super(
                Board.BINARIZATIONADJUST,
                sheet.getLocationService(),
                null,
                false,
                false,
                false,
                false);


        this.sheet = sheet;

        previousFilter = sheet.getStub().getBook().getBinarizationParam().getSpecific();
        noGraySource = sheet.getPicture().getSource(SourceKey.GRAY) == null;

        applyButton.addActionListener(this);
        resetButton.addActionListener(this);
        applyToAllSheetsButton.addActionListener(this);

        adaptiveMeanValue.getField().getDocument().addDocumentListener(
            new LFieldValidater(adaptiveMeanValue, AdaptiveDescriptor.MINMEAN, AdaptiveDescriptor.MAXMEAN)
        );

        adaptiveStdDevValue.getField().getDocument().addDocumentListener(
            new LFieldValidater(adaptiveStdDevValue, AdaptiveDescriptor.MINSTDDEV, AdaptiveDescriptor.MAXSTDDEV)
        );

        globalThresholdValue.getField().getDocument().addDocumentListener(
            new LFieldValidater(globalThresholdValue, GlobalDescriptor.MINTHRESHOLD, GlobalDescriptor.MAXTHRESHOLD)
        );

        adaptiveFilterRadioButton.addActionListener(this);
        globalFilterRadioButton.addActionListener(this);

        // Make radio buttons act as one group
        ButtonGroup imageButtonGroup = new ButtonGroup();
        imageButtonGroup.add(adaptiveFilterRadioButton.getField());
        imageButtonGroup.add(globalFilterRadioButton.getField());

                // Needed to process user input when RETURN/ENTER is pressed
        getComponent().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"),
                "ParamAction");
        getComponent().getActionMap().put("ParamAction", new ParamAction());

        initiatizeInputValues(sheet.getStub().getBinarizationFilter());

        defineLayout();
        
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------------//
    // applyFilterSettings //
    //---------------------//
    /** 
     * Pulls the filter values from the input fields and sets
     * a new FilterDescriptor for this sheet based on those values.
     * 
     * @param sheetToFilter the sheet to apply the current values to.
     */
    private void applyFilterSettings(Sheet sheetToFilter) {

        FilterParam filterParam = sheetToFilter.getStub().getBinarizationFilterParam();

        try {
            if (adaptiveFilterRadioButton.getField().isSelected()) {

                double mean = adaptiveMeanValue.getValue();
                double stdDev = adaptiveStdDevValue.getValue();

                if (mean < AdaptiveDescriptor.MINMEAN || mean > AdaptiveDescriptor.MAXMEAN) {
                    mean = Math.max(Math.min(mean, AdaptiveDescriptor.MAXMEAN), AdaptiveDescriptor.MINMEAN);
                    adaptiveMeanValue.setValue(mean);
                }

                if (mean < AdaptiveDescriptor.MINMEAN || mean > AdaptiveDescriptor.MAXMEAN) {
                    stdDev = Math.max(Math.min(stdDev, AdaptiveDescriptor.MAXSTDDEV), AdaptiveDescriptor.MINSTDDEV);
                    adaptiveStdDevValue.setValue(stdDev);
                }

                filterParam.setSpecific(new AdaptiveDescriptor(mean, stdDev));
    
            } else if (globalFilterRadioButton.getField().isSelected()) {

                int threshold = globalThresholdValue.getValue();

                if (threshold < AdaptiveDescriptor.MINMEAN || threshold > AdaptiveDescriptor.MAXMEAN) {
                    threshold = Math.max(Math.min(threshold, GlobalDescriptor.MAXTHRESHOLD), GlobalDescriptor.MINTHRESHOLD);
                    globalThresholdValue.setValue(threshold);
                }

                filterParam.setSpecific(new GlobalDescriptor(threshold));
            }
        } catch (Exception ex) {
            // Do nothing For invalid input
        }
    }

    //---------//
    // connect //
    //---------//
    /** 
     * Checks for whether the binarization filter settings
     * have changed since the board was last disconnected. If so, it runs
     * binarization with the values from the sheet.
     */
    @Override
    public void connect() {
        if (noGraySource) return;

        // Re-binarize image if the filter has changed
        if (!sheet.getStub().getBinarizationFilter().equals(previousFilter)
        ) {
            initiatizeInputValues(sheet.getStub().getBinarizationFilter());
            runBinarizationFilter(sheet);
        }
 
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {

        final FormLayout layout = new FormLayout(
            "right:32dlu, 4dlu, left:pref, 4dlu, fill:pref, 4dlu, left:pref", 
            "4dlu, center:pref, 4dlu, center:pref, 4dlu, center:pref, 4dlu, center:pref, 4dlu");

        final FormBuilder builder = FormBuilder.create().layout(layout).panel(getBody());

        int r = 2;

        // Add a message if no gray source if available
        if (noGraySource) {
            builder.appendRows("center:pref, 4dlu");
            builder.addRaw(noGraySourceLabel.getLabel()).xyw(1, r, 7);

            r += 2;
        }

        builder.addRaw(adaptiveFilterRadioButton.getField()).xy(1, r, "r, c");
        builder.addRaw(adaptiveFilterRadioButton.getLabel()).xyw(3, r, 5, "l, c");

        r += 2;

        builder.addRaw(globalFilterRadioButton.getField()).xy(1, r, "r, c");
        builder.addRaw(globalFilterRadioButton.getLabel()).xyw(3, r, 5, "l, c");

        r += 2;

        builder.addRaw(adaptiveMeanValue.getLabel()).xy(1, r);
        builder.addRaw(adaptiveMeanValue.getField()).xy(3, r, "l, c");

        builder.addRaw(adaptiveStdDevValue.getLabel()).xy(5, r);
        builder.addRaw(adaptiveStdDevValue.getField()).xy(7, r, "l, c");

        builder.addRaw(globalThresholdValue.getLabel()).xy(1, r);
        builder.addRaw(globalThresholdValue.getField()).xy(3, r, "l, c");

        r += 2;

        builder.addRaw(applyButton).xyw(1, r, 3);
        builder.addRaw(resetButton).xy(5, r);
        builder.addRaw(applyToAllSheetsButton).xy(7, r);

        if (noGraySource) disableBoard();

    }

    //--------------//
    // disableBoard //
    //--------------//
    /** 
     * Disables this board.
     */
    private void disableBoard() {
        for (Component comp : getBody().getComponents()) {
            comp.setEnabled(false);
        }
        noGraySourceLabel.setEnabled(true);
    }

    //------------//
    // disconnect //
    //------------//
    /** 
     * Saves a reference to the current sheet's
     * binarization filter settings (FilterDescriptor).
     */
    @Override
    public void disconnect() {

        // Save a reference to this filter, in case it gets changed by 
        // another sheet's BinarizationAdjustBoard later.
        previousFilter = sheet.getStub().getBinarizationFilter();
    }

    //-----------------------//
    // initializeInputValues //
    //-----------------------//
    /** 
     * Sets controls back to initial (source-code specified) values.
     */
    private void initiatizeInputValues(FilterDescriptor desc) {

        if (desc instanceof AdaptiveDescriptor ad) {

            adaptiveMeanValue.setValue(ad.meanCoeff);
            adaptiveStdDevValue.setValue(ad.stdDevCoeff);
            adaptiveFilterRadioButton.getField().setSelected(true);

            globalThresholdValue.setValue(GlobalDescriptor.getDefaultThreshold());

            showAdaptiveFilterInput();

        } else if (desc instanceof GlobalDescriptor gd) {

            globalThresholdValue.setValue(gd.threshold);
            globalFilterRadioButton.getField().setSelected(true);

            adaptiveMeanValue.setValue(AdaptiveDescriptor.getDefaultMeanCoeff());
            adaptiveStdDevValue.setValue(AdaptiveDescriptor.getDefaultStdDevCoeff());

            showGlobalFilterInput();
        
        }
    }

    //-----------------------//
    // runBinarizationFilter //
    //-----------------------//
    /** 
     * Runs binarization on a sheet.
     * 
     * @param sheetToFilter the sheet to run binarization on.
     */
    private void runBinarizationFilter(Sheet sheetToFilter) {

        BinaryStep.runBinarizationFilter(sheetToFilter);

        // Force repaint of the SheetView
        SheetView sheetView = sheetToFilter.getStub().getAssembly().getCurrentView();
        if (sheetView != null) sheetView.getComponent().repaint();

    }

    //-------------------------//
    // showAdaptiveFilterInput //
    //-------------------------//
    /** 
     * Makes adaptive filter inputs visible.
     */
    private void showAdaptiveFilterInput() {

        adaptiveMeanValue.setVisible(true);
        adaptiveMeanValue.setVisible(true);
        adaptiveStdDevValue.setVisible(true);
        adaptiveStdDevValue.setVisible(true);

        globalThresholdValue.setVisible(false);
        globalThresholdValue.setVisible(false);

    }

    //-----------------------//
    // showGlobalFilterInput //
    //-----------------------//
    /** 
     * Makes global filter inputs visible.
     */
    private void showGlobalFilterInput() {
   
        adaptiveMeanValue.setVisible(false);
        adaptiveMeanValue.setVisible(false);
        adaptiveStdDevValue.setVisible(false);
        adaptiveStdDevValue.setVisible(false);

        globalThresholdValue.setVisible(true);
        globalThresholdValue.setVisible(true);

    }

    //-----------------//
    // actionPerformed //
    //-----------------//
    /** 
     * Handles the button and radio button clicks for this board.
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == applyButton) {
            applyFilterSettings(sheet);
            runBinarizationFilter(sheet);
        }

        else if (e.getSource() == applyToAllSheetsButton) {

            // Apply settings to every sheet in this book
            sheet.getStub().getBook().getStubs().forEach((stub) -> {
                applyFilterSettings(stub.getSheet());
            });

            // Run binarization for this sheet only
            runBinarizationFilter(sheet);
        }

        else if (e.getSource() == resetButton) {
            
            FilterDescriptor bookFilterDesc = sheet.getStub().getBook().getBinarizationParam().getValue();

            // Reset this sheet back to the defaults of its book
            initiatizeInputValues(bookFilterDesc);

            sheet.getStub().getBinarizationFilterParam().setSpecific(null);
            runBinarizationFilter(sheet);
        }

        else if (e.getSource() == adaptiveFilterRadioButton.getField()) {
            showAdaptiveFilterInput();
            applyFilterSettings(sheet);
            runBinarizationFilter(sheet);
        } 
        
        else if (e.getSource() == globalFilterRadioButton.getField()) {
            showGlobalFilterInput();
            applyFilterSettings(sheet);
            runBinarizationFilter(sheet);
        }

    }


    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event) {}



    //~ Inner Classes ------------------------------------------------------------------------------

    /** Check the value of an LTextField, and if it is not a valid number
     * within the specified range, turn the background red to inform the user.
     */
    private class LFieldValidater
        implements DocumentListener
    {
        
        public double MIN;

        public double MAX;

        public LTextField field;

        public boolean wasValid = true;

        public LFieldValidater(LTextField field, double min, double max) {
            this.MIN = min;
            this.MAX = max;
            this.field = field;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {

            boolean currentlyValid = true;

            try {

                double value = Double.parseDouble(field.getText().trim());

                if (value < MIN || value > MAX) {
                    field.getField().setBackground(new Color(0xFCA5A5));
                } else {
                    field.getField().setBackground(Color.WHITE);
                }

            } catch (NumberFormatException | NoSuchElementException ex) {
                field.getField().setBackground(new Color(0xFCA5A5));
                currentlyValid = false;



            }

            // If there has been a change in validity
            if (wasValid != currentlyValid) {

                applyButton.setEnabled(currentlyValid);
                applyToAllSheetsButton.setEnabled(currentlyValid);

            }

            wasValid = currentlyValid;

        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            insertUpdate(e);
        }

        @Override
        public void changedUpdate(DocumentEvent e) {}
        
    }




    private class ParamAction
            extends AbstractAction
    {
        // Method runs whenever user presses Return/Enter in one of the parameter fields
        @Override
        public void actionPerformed (ActionEvent e)
        {
            applyFilterSettings(sheet);
            runBinarizationFilter(sheet);
        }
    }

}