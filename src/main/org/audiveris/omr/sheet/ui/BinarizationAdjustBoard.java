//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               B i n a r i z a t i o n B o a r d                                //
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
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.field.LDoubleField;
import org.audiveris.omr.ui.field.LIntegerField;
import org.audiveris.omr.ui.field.LRadioButton;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.UserEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;

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

    /** Events this entity is interested in */
    private static final Class<?>[] eventClasses = new Class<?>[]
    { LocationEvent.class };

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
    private final JButton applyButton = new JButton("Apply");

    /** Reset button.  */
    private final JButton resetButton = new JButton("Reset");

    /** Apply to all pages button.  */
    private final JButton applyToBookButton = new JButton("Apply to all pages");

    /** Reference to the last filter used on this board.  */
    private FilterDescriptor previousFilter;
    

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
                eventClasses,
                false,
                false,
                false,
                false);

        this.sheet = sheet;
        previousFilter = sheet.getStub().getBinarizationFilter();

        applyButton.addActionListener(this);
        resetButton.addActionListener(this);
        applyToBookButton.addActionListener(this);

        adaptiveFilterRadioButton.addActionListener(this);
        globalFilterRadioButton.addActionListener(this);

        // Make radio buttons act as one group
        ButtonGroup imageButtonGroup = new ButtonGroup();
        imageButtonGroup.add(adaptiveFilterRadioButton.getField());
        imageButtonGroup.add(globalFilterRadioButton.getField());

        defineLayout();

        initiatizeInputValues();
        
    }

    //~ Methods ------------------------------------------------------------------------------------

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
        builder.addRaw(applyToBookButton).xy(7, r);

    }

    //-----------------------//
    // initializeInputValues //
    //-----------------------//
    /** 
     * Sets controls back to initial (source-code specified) values.
     */
    private void initiatizeInputValues() {

        if (sheet.getStub().getBinarizationFilter() instanceof AdaptiveDescriptor ad) {

            adaptiveMeanValue.setValue(ad.meanCoeff);
            adaptiveStdDevValue.setValue(ad.stdDevCoeff);
            adaptiveFilterRadioButton.getField().setSelected(true);

            globalThresholdValue.setValue(GlobalDescriptor.getDefaultThreshold());

            showAdaptiveFilterInput();

        } else if (sheet.getStub().getBinarizationFilter() instanceof GlobalDescriptor gd) {

            globalThresholdValue.setValue(gd.threshold);
            globalFilterRadioButton.getField().setSelected(true);

            adaptiveMeanValue.setValue(AdaptiveDescriptor.getDefaultMeanCoeff());
            adaptiveStdDevValue.setValue(AdaptiveDescriptor.getDefaultStdDevCoeff());

            showGlobalFilterInput();
        
        }
    }

    //-----------------------//
    // showGlobalFilterInput //
    //-----------------------//
    /** 
     * Makes global filter inputs visible.
     */
    private void showGlobalFilterInput() {
   
        adaptiveMeanValue.getLabel().setVisible(false);
        adaptiveMeanValue.getField().setVisible(false);
        adaptiveStdDevValue.getLabel().setVisible(false);
        adaptiveStdDevValue.getField().setVisible(false);

        globalThresholdValue.getLabel().setVisible(true);
        globalThresholdValue.getField().setVisible(true);

    }

    //-------------------------//
    // showAdaptiveFilterInput //
    //-------------------------//
    /** 
     * Makes adaptive filter inputs visible.
     */
    private void showAdaptiveFilterInput() {

        adaptiveMeanValue.getLabel().setVisible(true);
        adaptiveMeanValue.getField().setVisible(true);
        adaptiveStdDevValue.getLabel().setVisible(true);
        adaptiveStdDevValue.getField().setVisible(true);

        globalThresholdValue.getLabel().setVisible(false);
        globalThresholdValue.getField().setVisible(false);

    }

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

        if (adaptiveFilterRadioButton.getField().isSelected()) {
            double mean = adaptiveMeanValue.getValue();
            double stdDev = adaptiveStdDevValue.getValue();
            filterParam.setSpecific(new AdaptiveDescriptor(mean, stdDev));

        } else if (globalFilterRadioButton.getField().isSelected()) {
            int value = globalThresholdValue.getValue();
            filterParam.setSpecific(new GlobalDescriptor(value));
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
        sheetToFilter.getStub().getAssembly().getCurrentView().getComponent().repaint();

    }

    //---------//
    // connect //
    //---------//
    /** 
     * Calls parent method, then checks for whether the binarization filter settings
     * have changed since the board was last disconnected. If so, it runs
     * binarization with the values from the sheet.
     */
    @Override
    public void connect() {
        super.connect();

        // Re-binarize image if the filter has changed
        if (sheet.getStub().getBinarizationFilter() != previousFilter) {
            initiatizeInputValues();
            runBinarizationFilter(sheet);
        }
 
    }

    //------------//
    // disconnect //
    //------------//
    /** 
     * Calls parent method, then saves a reference to the current sheet's
     * binarization filter settings (FilterDescriptor).
     */
    @Override
    public void disconnect() {
        super.disconnect();

        // Save a reference to this filter, in case it gets changed by 
        // another sheet's BinarizationAdjustBoard later.
        previousFilter = sheet.getStub().getBinarizationFilter();
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

        else if (e.getSource() == applyToBookButton) {

            // Apply settings to every sheet in this book
            sheet.getStub().getBook().getStubs().forEach((stub) -> {
                applyFilterSettings(stub.getSheet());
            });

            // Run binarization for this sheet only
            runBinarizationFilter(sheet);
        }

        else if (e.getSource() == resetButton) {
            adaptiveMeanValue.setValue(AdaptiveDescriptor.getDefaultMeanCoeff());
            adaptiveStdDevValue.setValue(AdaptiveDescriptor.getDefaultStdDevCoeff());
            globalThresholdValue.setValue(GlobalDescriptor.getDefaultThreshold());
            applyFilterSettings(sheet);
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

}
