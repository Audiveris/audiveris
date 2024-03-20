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
import org.audiveris.omr.ui.field.LCheckBox;
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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
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
    private final JButton applyToWholeBook = new JButton("Apply to whole book");

    /** Checkbox for overwritting sheets */
    private final LCheckBox overwriteCheckbox = new LCheckBox("Overwrite sheets?", "When applying to the whole book, overwrite individual sheets' binarization settings");

    /** Reference to the last filter used on this board.  */
    private FilterDescriptor previousFilter;

    /** Equal to false if this sheet has no gray source image to binarize. */
    private boolean noGraySource;

    /** Label for when no gray source image is present */
    private LLabel noGraySourceLabel = new LLabel("No source image available for binarization", "No source image available to run binarization on");
    
    /** True if this sheet is part of a book with multiple sheets */
    private boolean isMultiSheetBook;

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
        
        isMultiSheetBook = sheet.getStub().getBook().isMultiSheet();
        previousFilter = sheet.getStub().getBook().getBinarizationParam().getSpecific();
        noGraySource = sheet.getPicture().getSource(SourceKey.GRAY) == null;

        applyButton.addActionListener(this);
        resetButton.addActionListener(this);
        applyToWholeBook.addActionListener(this);

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
    private void applyFilterSettings(FilterParam filterParam) {

        if (adaptiveFilterRadioButton.getField().isSelected()) {

            double mean = adaptiveMeanValue.getValue();
            double stdDev = adaptiveStdDevValue.getValue();

            if (mean < AdaptiveDescriptor.MINMEAN || mean > AdaptiveDescriptor.MAXMEAN) {
                mean = Math.max(Math.min(mean, AdaptiveDescriptor.MAXMEAN), AdaptiveDescriptor.MINMEAN);
                adaptiveMeanValue.setValue(mean);
            }

            if (stdDev < AdaptiveDescriptor.MINSTDDEV || stdDev > AdaptiveDescriptor.MAXSTDDEV) {
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
    }

    //---------------------//
    // applyFilterSettings //
    //---------------------//
    /** 
     * Applies new filter to the proper scope and calls runBinarizationFilter
     * to display changes.
     * 
     */
    private void applyAndRunFilter() {
        
        FilterParam filterParam = isMultiSheetBook ? 
            sheet.getStub().getBinarizationFilterParam() : 
            sheet.getStub().getBook().getBinarizationParam();

        applyFilterSettings(filterParam);
        runBinarizationFilter(sheet);
        
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
        if (!sheet.getStub().getBinarizationFilter().equals(previousFilter)) {
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
            "left:180dlu", 
            "6dlu");

        final FormBuilder builder = FormBuilder.create().layout(layout).panel(getBody());

        int buttonHeight = applyButton.getPreferredSize().height;

        int r = 2;

        // Add a message if no gray source if available
        if (noGraySource) {
            builder.appendRows("center: pref");
            builder.addRaw(noGraySourceLabel.getLabel()).xy(1, r);
            
            r += 1;
        }

        builder.appendRows("center:14dlu, center:14dlu, center:pref, center:pref, center:10dlu, center:pref");

        JPanel radioButtonRow1 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        radioButtonRow1.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        radioButtonRow1.add(adaptiveFilterRadioButton.getField());
        radioButtonRow1.add(adaptiveFilterRadioButton.getLabel());
        builder.addRaw(radioButtonRow1).xy(1, r++);


        JPanel radioButtonRow2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
        radioButtonRow2.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
        radioButtonRow2.add(globalFilterRadioButton.getField());
        radioButtonRow2.add(globalFilterRadioButton.getLabel());
        builder.addRaw(radioButtonRow2).xy(1, r++);

        JPanel inputRow = new JPanel(new FlowLayout(FlowLayout.LEADING));
        inputRow.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
        inputRow.add(globalThresholdValue.getLabel());
        inputRow.add(globalThresholdValue.getField());
        inputRow.add(adaptiveMeanValue.getLabel());
        inputRow.add(adaptiveMeanValue.getField());
        inputRow.add(adaptiveStdDevValue.getLabel());
        inputRow.add(adaptiveStdDevValue.getField());
        builder.addRaw(inputRow).xy(1, r++);

        JPanel buttonRow1 = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        buttonRow1.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        applyButton.setPreferredSize(new Dimension(124, buttonHeight));
        resetButton.setPreferredSize(new Dimension(100, buttonHeight));
        buttonRow1.add(applyButton);
        buttonRow1.add(resetButton);
        builder.addRaw(buttonRow1).xy(1, r++);

        if (isMultiSheetBook) {

            JSeparator separator = new JSeparator();
            separator.setPreferredSize(new Dimension(320, 8));
            builder.addRaw(separator).xy(1, r++);
    
            JPanel buttonRow2 = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
            buttonRow2.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            applyToWholeBook.setPreferredSize(new Dimension(168, buttonHeight));
            buttonRow2.add(applyToWholeBook);
    
            JPanel overwritePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 3, 0));
            overwritePanel.add(overwriteCheckbox.getField());
            overwritePanel.add(overwriteCheckbox.getLabel());
            buttonRow2.add(overwritePanel);
    
            builder.addRaw(buttonRow2).xy(1, r++);

        }

        if (noGraySource) {
            disableBoard(getBody());
            noGraySourceLabel.setEnabled(true);
        }

    }

    //--------------//
    // disableBoard //
    //--------------//
    /** 
     * Disables this board.
     */
    private void disableBoard(Component comp) {

        comp.setEnabled(false);

        if (comp instanceof Container container) {
            for (Component child : container.getComponents()) {
                disableBoard(child);
            }
        }

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

    //----------------//
    // inputIsInRange //
    //----------------//
    /**
     * Tests whether the input in a field is within the range of min and max.
     * 
     * @param field the field to test.
     * @param min the minimum valid value.
     * @param max the maximum valid value.
     * @return true if this field's number is within min and max.
      */
    private boolean inputIsInRange(LTextField field, double min, double max) {

        try {

            double value = Double.parseDouble(field.getText().trim());
            return value > min && value < max;
            
        } catch (NumberFormatException e) {
            return false;
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
        adaptiveStdDevValue.setVisible(true);

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
        adaptiveStdDevValue.setVisible(false);

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
            applyAndRunFilter();
        }

        else if (e.getSource() == applyToWholeBook) {
            
            // Optionally erase every sheet's specific binarization setting
            if (overwriteCheckbox.getField().isSelected()) {
                sheet.getStub().getBook().getStubs().forEach((stub) -> {
                    stub.getBinarizationFilterParam().setSpecific(null);
                });
            }

            // Apply current filter settings to this book's FilterParam
            applyFilterSettings(sheet.getStub().getBook().getBinarizationParam());

            // Run binarization for this sheet only
            runBinarizationFilter(sheet);
        }

        else if (e.getSource() == resetButton) {

            if (isMultiSheetBook) {

                FilterDescriptor resetFilterDesc = sheet.getStub().getBook().getBinarizationParam().getValue();
                initiatizeInputValues(resetFilterDesc);

                sheet.getStub().getBinarizationFilterParam().setSpecific(null);

            } else {

                FilterDescriptor resetFilterDesc = switch(FilterDescriptor.getDefaultKind()) {
                    case GLOBAL -> GlobalDescriptor.getDefault();
                    case ADAPTIVE -> AdaptiveDescriptor.getDefault();};
                initiatizeInputValues(resetFilterDesc);

                sheet.getStub().getBook().getBinarizationParam().setSpecific(null);

            }

            runBinarizationFilter(sheet);

        }

        else if (e.getSource() == adaptiveFilterRadioButton.getField()) {
            showAdaptiveFilterInput();
            applyAndRunFilter();
        } 
        
        else if (e.getSource() == globalFilterRadioButton.getField()) {
            showGlobalFilterInput();
            applyAndRunFilter();
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

            boolean currentlyValid = inputIsInRange(field, MIN, MAX);

            // If there has been a change in validity
            if (wasValid != currentlyValid) {

                if (currentlyValid) {
                    field.getField().setBackground(Color.WHITE);
                } else {
                    field.getField().setBackground(new Color(0xFCA5A5));
                }

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
            applyAndRunFilter();
        }
    }

}