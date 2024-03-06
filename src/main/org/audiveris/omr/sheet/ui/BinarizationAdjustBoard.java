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
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JButton;

/**
 * Class <code>BinarizationAdjustBoard</code> allows users to
 * adjust binarization settings and filters on the image.
 *
 * @author Mike Porter
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

    private final LIntegerField globalThresholdValue = 
        new LIntegerField("Threshold", "Gray threshold");
    
    private final LDoubleField adaptiveMeanValue = 
        new LDoubleField("Mean", "Coefficient for mean value", "%.3f");
    
    private final LDoubleField adaptiveStdDevValue = 
        new LDoubleField("Std Dev", "Coefficient for standard deviation value", "%.3f");

    private final JButton applyButton = new JButton("Apply");

    private final JButton resetButton = new JButton("Reset");

    private final LRadioButton globalFilterRadioButton = 
        new LRadioButton("Use global filter", "Converts to black and white by using one value for the entire image");

    private final LRadioButton adaptiveFilterRadioButton = 
        new LRadioButton(
            "Use adaptive filter (recommended)", 
            "Converts a pixel to black and white by looking at its surrounding pixels"
        );
    

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

        applyButton.addActionListener((e) -> changeFilterSettings());
        resetButton.addActionListener(this);

        adaptiveFilterRadioButton.addActionListener(this);
        globalFilterRadioButton.addActionListener(this);

        // Set initial values for input fields and which radio button is initially selected
        if (sheet.getStub().getBinarizationFilter() instanceof AdaptiveDescriptor ad) {

            adaptiveMeanValue.setValue(ad.meanCoeff);
            adaptiveStdDevValue.setValue(ad.stdDevCoeff);
            adaptiveFilterRadioButton.getField().setSelected(true);

            globalThresholdValue.setValue(GlobalDescriptor.getDefaultThreshold());

        } else if (sheet.getStub().getBinarizationFilter() instanceof GlobalDescriptor gd) {

            globalThresholdValue.setValue(gd.threshold);
            globalFilterRadioButton.getField().setSelected(true);

            adaptiveMeanValue.setValue(AdaptiveDescriptor.getDefaultMeanCoeff());
            adaptiveStdDevValue.setValue(AdaptiveDescriptor.getDefaultStdDevCoeff());

        }

        ButtonGroup imageButtonGroup = new ButtonGroup();
        imageButtonGroup.add(adaptiveFilterRadioButton.getField());
        imageButtonGroup.add(globalFilterRadioButton.getField());

        
        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {

        final FormLayout layout = new FormLayout(
            "right:pref, 4dlu, left:pref, 4dlu, fill:pref, 4dlu, left:pref", 
            "4dlu, center:pref, 4dlu, center:pref, 4dlu, center:pref, 4dlu, center:pref, 4dlu");

        layout.setColumnGroups(new int[][] {{1, 5}, {2, 6}, {3, 7}});
        layout.setColumnSpec(1, new ColumnSpec(Sizes.dluX(32)));
        layout.setColumnSpec(2, new ColumnSpec(Sizes.dluX(4)));

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
        builder.addRaw(resetButton).xyw(5, r, 3);

        // After adding all of the input fields, disable the ones we don't need
        addAdaptiveFilterInput();

    }


    private void addGlobalFilterInput() {
   
        adaptiveMeanValue.getLabel().setVisible(false);
        adaptiveMeanValue.getField().setVisible(false);
        adaptiveStdDevValue.getLabel().setVisible(false);
        adaptiveStdDevValue.getField().setVisible(false);

        globalThresholdValue.getLabel().setVisible(true);
        globalThresholdValue.getField().setVisible(true);

    }

    
    private void addAdaptiveFilterInput() {

        adaptiveMeanValue.getLabel().setVisible(true);
        adaptiveMeanValue.getField().setVisible(true);
        adaptiveStdDevValue.getLabel().setVisible(true);
        adaptiveStdDevValue.getField().setVisible(true);

        globalThresholdValue.getLabel().setVisible(false);
        globalThresholdValue.getField().setVisible(false);

    }

    private void changeFilterSettings() {

        FilterParam filterParam = new FilterParam(this);

        if (adaptiveFilterRadioButton.getField().isSelected()) {
            double mean = adaptiveMeanValue.getValue();
            double stdDev = adaptiveStdDevValue.getValue();
            filterParam.setSpecific(new AdaptiveDescriptor(mean, stdDev));

        } else if (globalFilterRadioButton.getField().isSelected()) {
            int value = globalThresholdValue.getValue();
            filterParam.setSpecific(new GlobalDescriptor(value));
        }

        sheet.getStub().setBinarizationFilterParam(filterParam);

        BinaryStep.runBinarizationFilter(sheet);

        // Force repaint of the SheetView
        sheet.getStub().getAssembly().getCurrentView().getComponent().repaint();
    }



    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == applyButton) {
            changeFilterSettings();
        }

        else if (e.getSource() == resetButton) {
            adaptiveMeanValue.setValue(AdaptiveDescriptor.getDefaultMeanCoeff());
            adaptiveStdDevValue.setValue(AdaptiveDescriptor.getDefaultStdDevCoeff());
            globalThresholdValue.setValue(GlobalDescriptor.getDefaultThreshold());
            changeFilterSettings();
        }

        else if (e.getSource() == adaptiveFilterRadioButton.getField()) {
            addAdaptiveFilterInput();
            changeFilterSettings();
        } 
        
        else if (e.getSource() == globalFilterRadioButton.getField()) {
            addGlobalFilterInput();
            changeFilterSettings();
        }

    }



    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event) {}

}
