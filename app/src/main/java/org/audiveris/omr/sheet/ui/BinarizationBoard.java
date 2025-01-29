//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                B i n a r i z a t i o n B o a r d                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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
import static org.audiveris.omr.image.FilterKind.ADAPTIVE;
import static org.audiveris.omr.image.FilterKind.GLOBAL;
import org.audiveris.omr.image.FilterParam;
import org.audiveris.omr.image.GlobalDescriptor;
import org.audiveris.omr.sheet.Picture.SourceKey;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.step.BinaryStep;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.field.LCheckBox;
import org.audiveris.omr.ui.field.LDoubleField;
import org.audiveris.omr.ui.field.LIntegerField;
import org.audiveris.omr.ui.field.LRadioButton;
import org.audiveris.omr.ui.field.LTextField;
import org.audiveris.omr.ui.selection.LocationEvent;
import static org.audiveris.omr.ui.selection.MouseMovement.PRESSING;
import static org.audiveris.omr.ui.selection.SelectionHint.LOCATION_INIT;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Class <code>BinarizationBoard</code> allows users to
 * adjust binarization filters and settings to operate on the gray image.
 * <p>
 * To avoid useless loading of GRAY image, the evaluation of 'noGraySource' boolean is
 * differred until the board is actually selected.
 * <p>
 * This class was renamed from BinarizationAdjustBoard and has replaced
 * the former BinarizationBoard now renamed BinaryBoard.
 *
 * @author Mike Porter, Hervé Bitteur
 */
public class BinarizationBoard
        extends Board
        implements ActionListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BinarizationBoard.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The related sheet. */
    private final Sheet sheet;

    /** Radio button to select adaptive filter. */
    private final LRadioButton adaptiveFilterRadioButton = new LRadioButton(
            "Use adaptive filter (recommended)",
            "Converts a pixel to black and white by looking at its surrounding pixels");

    /** Radio button to select global filter. */
    private final LRadioButton globalFilterRadioButton = new LRadioButton(
            "Use global filter",
            "Converts to black and white by using one threshold value for the entire image");

    /** Input field for global filter's threshold. */
    private final LIntegerField globalThresholdValue = new LIntegerField(
            "Threshold",
            "Gray threshold");

    /** Input field for adaptive filter's mean. */
    private final LDoubleField adaptiveMeanValue = new LDoubleField(
            "Mean coef",
            "Coefficient for mean value",
            "%.2f");

    /** Input field for adaptive filter's standard deviation. */
    private final LDoubleField adaptiveStdDevValue = new LDoubleField(
            "Std Dev coef",
            "Coefficient for standard deviation value",
            "%.2f");

    /** Apply button. */
    private final JButton applyButton = new JButton("Apply (enter)");

    /** Reset button. */
    private final JButton resetButton = new JButton("Reset");

    /** Apply to all pages button. */
    private final JButton applyToWholeBook = new JButton("Apply to whole book");

    /** Checkbox for overwriting sheets. */
    private final LCheckBox overwriteCheckbox = new LCheckBox(
            "Overwrite sheets?",
            "When applying to the whole book, overwrite individual sheets' binarization settings");

    /** Reference to the last filter used on this board. */
    private FilterDescriptor previousFilter;

    /**
     * Initially null, then equal to false if this sheet has no gray source image to binarize.
     */
    private Boolean noGraySource;

    /** Label for when no gray source image is present. */
    private final JLabel noGraySourceLabel = new JLabel("No gray image available for binarization");

    /** True if this sheet is part of a book with multiple sheets. */
    private final boolean isMultiSheetBook;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>BinarizationBoard</code> object, pre-selected by default.
     *
     * @param sheet related sheet
     */
    public BinarizationBoard (Sheet sheet)
    {
        super(Board.BINARIZATION, sheet.getLocationService(), null, false, false, false, false);

        this.sheet = sheet;

        isMultiSheetBook = sheet.getStub().getBook().isMultiSheet();
        previousFilter = sheet.getStub().getBook().getBinarizationParam().getSpecific();

        applyButton.setToolTipText("Apply the filter & settings on the sheet gray image");
        applyButton.addActionListener(this);

        resetButton.setToolTipText("Reset to the default filter & settings for this sheet");
        resetButton.addActionListener(this);

        applyToWholeBook.setToolTipText("Define this filter & settings for the whole book");
        applyToWholeBook.addActionListener(this);

        noGraySourceLabel.setToolTipText(
                "There is no source image available to run binarization on");

        adaptiveMeanValue.getField().getDocument().addDocumentListener(
                new LFieldValidater(
                        adaptiveMeanValue,
                        AdaptiveDescriptor.MINMEAN,
                        AdaptiveDescriptor.MAXMEAN));
        adaptiveStdDevValue.getField().getDocument().addDocumentListener(
                new LFieldValidater(
                        adaptiveStdDevValue,
                        AdaptiveDescriptor.MINSTDDEV,
                        AdaptiveDescriptor.MAXSTDDEV));
        globalThresholdValue.getField().getDocument().addDocumentListener(
                new LFieldValidater(
                        globalThresholdValue,
                        GlobalDescriptor.MINTHRESHOLD,
                        GlobalDescriptor.MAXTHRESHOLD));

        adaptiveFilterRadioButton.addActionListener(this);
        globalFilterRadioButton.addActionListener(this);

        // Make radio buttons act as one group
        final ButtonGroup imageButtonGroup = new ButtonGroup();
        imageButtonGroup.add(adaptiveFilterRadioButton.getField());
        imageButtonGroup.add(globalFilterRadioButton.getField());

        // Needed to process user input when RETURN/ENTER is pressed
        getComponent().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"),
                "ParamAction");
        getComponent().getActionMap().put("ParamAction", new ParamAction());

        initializeInputValues(sheet.getStub().getBinarizationFilter());

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------------//
    // actionPerformed //
    //-----------------//
    /**
     * Handles the button and radio button clicks for this board.
     */
    @Override
    public void actionPerformed (ActionEvent e)
    {
        if (e.getSource() == applyButton) {
            applyAndRunFilter();
        } else if (e.getSource() == applyToWholeBook) {
            // Optionally erase every sheet's specific binarization setting
            if (overwriteCheckbox.getField().isSelected()) {
                sheet.getStub().getBook().getStubs().forEach( (stub) -> {
                    stub.getBinarizationFilterParam().setSpecific(null);
                });
            }

            // Apply current filter settings to this book's FilterParam
            applyFilterSettings(sheet.getStub().getBook().getBinarizationParam());

            // Run binarization for this sheet only
            runBinarizationFilter(sheet);

        } else if (e.getSource() == resetButton) {
            if (isMultiSheetBook) {
                FilterDescriptor resetFilterDesc = sheet.getStub().getBook().getBinarizationParam()
                        .getValue();
                initializeInputValues(resetFilterDesc);

                sheet.getStub().getBinarizationFilterParam().setSpecific(null);

            } else {
                FilterDescriptor resetFilterDesc = switch (FilterDescriptor.getDefaultKind()) {
                    case GLOBAL -> GlobalDescriptor.getDefault();
                    case ADAPTIVE -> AdaptiveDescriptor.getDefault();
                };
                initializeInputValues(resetFilterDesc);

                sheet.getStub().getBook().getBinarizationParam().setSpecific(null);
            }

            runBinarizationFilter(sheet);

        } else if (e.getSource() == adaptiveFilterRadioButton.getField()) {
            showAdaptiveFilterInput();
            applyAndRunFilter();
        } else if (e.getSource() == globalFilterRadioButton.getField()) {
            showGlobalFilterInput();
            applyAndRunFilter();
        }
    }

    //-------------------//
    // applyAndRunFilter //
    //-------------------//
    /**
     * Applies new filter to the proper scope and calls runBinarizationFilter
     * to display changes.
     */
    private void applyAndRunFilter ()
    {
        FilterParam filterParam = isMultiSheetBook ? sheet.getStub().getBinarizationFilterParam()
                : sheet.getStub().getBook().getBinarizationParam();

        applyFilterSettings(filterParam);
        runBinarizationFilter(sheet);
    }

    //---------------------//
    // applyFilterSettings //
    //---------------------//
    /**
     * Pulls the filter values from the input fields and sets
     * a new FilterDescriptor for this sheet based on those values.
     *
     * @param filterParam the sheet to apply the current values to.
     */
    private void applyFilterSettings (FilterParam filterParam)
    {
        if (adaptiveFilterRadioButton.getField().isSelected()) {
            double mean = adaptiveMeanValue.getValue();
            double stdDev = adaptiveStdDevValue.getValue();

            if (mean < AdaptiveDescriptor.MINMEAN || mean > AdaptiveDescriptor.MAXMEAN) {
                mean = Math.max(
                        Math.min(mean, AdaptiveDescriptor.MAXMEAN),
                        AdaptiveDescriptor.MINMEAN);
                adaptiveMeanValue.setValue(mean);
            }

            if (stdDev < AdaptiveDescriptor.MINSTDDEV || stdDev > AdaptiveDescriptor.MAXSTDDEV) {
                stdDev = Math.max(
                        Math.min(stdDev, AdaptiveDescriptor.MAXSTDDEV),
                        AdaptiveDescriptor.MINSTDDEV);
                adaptiveStdDevValue.setValue(stdDev);
            }

            filterParam.setSpecific(new AdaptiveDescriptor(mean, stdDev));

        } else if (globalFilterRadioButton.getField().isSelected()) {
            int threshold = globalThresholdValue.getValue();

            if (threshold < GlobalDescriptor.MINTHRESHOLD
                    || threshold > GlobalDescriptor.MAXTHRESHOLD) {
                threshold = Math.max(
                        Math.min(threshold, GlobalDescriptor.MAXTHRESHOLD),
                        GlobalDescriptor.MINTHRESHOLD);
                globalThresholdValue.setValue(threshold);
            }

            filterParam.setSpecific(new GlobalDescriptor(threshold));
        }
    }

    //---------//
    // connect //
    //---------//
    /**
     * Checks for whether the binarization filter settings
     * have changed since the board was last disconnected.
     * If so, it runs binarization with the values from the sheet.
     */
    @Override
    public void connect ()
    {
        if (noGraySource == null) {
            noGraySource = sheet.getPicture().getSource(SourceKey.GRAY) == null;

            if (noGraySource) {
                disableBoard(getBody());
                noGraySourceLabel.setVisible(true);
                noGraySourceLabel.setEnabled(true);
            }
        }

        if (noGraySource)
            return;

        // Re-binarize image if the filter has changed
        if (!sheet.getStub().getBinarizationFilter().equals(previousFilter)) {
            initializeInputValues(sheet.getStub().getBinarizationFilter());
            runBinarizationFilter(sheet);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        final int inset = UIUtil.adjustedSize(3);
        ((Panel) getBody()).setInsets(inset, 0, inset, inset); // TLBR

        final FormLayout layout = Panel.makeFormLayout(6, 3);
        final FormBuilder builder = FormBuilder.create().layout(layout).panel(getBody());

        int r = 1; // -----------------------------

        // Place holder: Room for a message if no gray source if available
        noGraySourceLabel.setVisible(false);
        builder.addRaw(noGraySourceLabel).xyw(2, r, 8);
        r += 2;

        builder.addRaw(adaptiveFilterRadioButton.getField()).xy(1, r);
        builder.addRaw(adaptiveFilterRadioButton.getLabel()).xyw(3, r, 6);
        adaptiveFilterRadioButton.getLabel().setHorizontalAlignment(SwingConstants.LEFT);

        r += 2; // --------------------------------

        builder.addRaw(globalFilterRadioButton.getField()).xy(1, r);
        builder.addRaw(globalFilterRadioButton.getLabel()).xyw(3, r, 6);
        globalFilterRadioButton.getLabel().setHorizontalAlignment(SwingConstants.LEFT);

        r += 2; // --------------------------------

        builder.addRaw(globalThresholdValue.getLabel()).xyw(1, r, 3);
        builder.addRaw(globalThresholdValue.getField()).xy(5, r);

        // Overlapping line
        builder.addRaw(adaptiveMeanValue.getLabel()).xyw(1, r, 3);
        builder.addRaw(adaptiveMeanValue.getField()).xy(5, r);

        builder.addRaw(adaptiveStdDevValue.getLabel()).xyw(7, r, 3);
        builder.addRaw(adaptiveStdDevValue.getField()).xy(11, r);

        r += 2; // --------------------------------

        builder.addRaw(applyButton).xyw(1, r, 4);
        builder.addRaw(resetButton).xyw(7, r, 4);

        if (isMultiSheetBook) {
            r += 2; // ----------------------------

            builder.addRaw(applyToWholeBook).xyw(1, r, 4);

            builder.addRaw(overwriteCheckbox.getField()).xyw(5, r, 1);
            builder.addRaw(overwriteCheckbox.getLabel()).xyw(7, r, 3);
        }
    }

    //--------------//
    // disableBoard //
    //--------------//
    /**
     * Disables this board.
     */
    private void disableBoard (Component comp)
    {
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
    public void disconnect ()
    {
        // Save a reference to this filter, in case it gets changed by
        // another sheet's BinarizationBoard later.
        previousFilter = sheet.getStub().getBinarizationFilter();
    }

    //-----------------------//
    // initializeInputValues //
    //-----------------------//
    /**
     * Sets controls back to initial (source-code specified) values.
     */
    private void initializeInputValues (FilterDescriptor desc)
    {
        switch (desc) {
            case AdaptiveDescriptor ad -> {
                adaptiveMeanValue.setValue(ad.meanCoeff);
                adaptiveStdDevValue.setValue(ad.stdDevCoeff);
                adaptiveFilterRadioButton.getField().setSelected(true);

                globalThresholdValue.setValue(GlobalDescriptor.getDefaultThreshold());

                showAdaptiveFilterInput();

            }
            case GlobalDescriptor gd -> {
                globalThresholdValue.setValue(gd.threshold);
                globalFilterRadioButton.getField().setSelected(true);

                adaptiveMeanValue.setValue(AdaptiveDescriptor.getDefaultMeanCoeff());
                adaptiveStdDevValue.setValue(AdaptiveDescriptor.getDefaultStdDevCoeff());

                showGlobalFilterInput();
            }
            default -> {}
        }
    }

    //----------------//
    // inputIsInRange //
    //----------------//
    /**
     * Tests whether the input in a field is within the range of min and max.
     *
     * @param field the field to test.
     * @param min   the minimum valid value.
     * @param max   the maximum valid value.
     * @return true if this field's number is within min and max.
     */
    private boolean inputIsInRange (LTextField field,
                                    double min,
                                    double max)
    {
        try {
            double value = Double.parseDouble(field.getText().trim());
            return value >= min && value <= max;

        } catch (NumberFormatException e) {
            return false;
        }
    }

    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
    }

    //-----------------------//
    // runBinarizationFilter //
    //-----------------------//
    /**
     * Runs binarization on a sheet.
     *
     * @param sheetToFilter the sheet to run binarization on.
     */
    private void runBinarizationFilter (Sheet sheetToFilter)
    {
        BinaryStep.runBinarizationFilter(sheetToFilter);

        // Force repaint of the SheetView
        SheetView sheetView = sheetToFilter.getStub().getAssembly().getCurrentView();
        if (sheetView != null)
            sheetView.getComponent().repaint();

        // Re-publish location event to update dependent artifacts (e.g. plain Binarization board)
        final SelectionService service = sheet.getLocationService();
        final LocationEvent loc = (LocationEvent) service.getLastEvent(LocationEvent.class);
        if (loc != null) {
            service.publish(new LocationEvent(this, LOCATION_INIT, PRESSING, loc.getData()));
        }
    }

    //-------------------------//
    // showAdaptiveFilterInput //
    //-------------------------//
    /**
     * Makes adaptive filter inputs visible.
     */
    private void showAdaptiveFilterInput ()
    {
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
    private void showGlobalFilterInput ()
    {
        adaptiveMeanValue.setVisible(false);
        adaptiveStdDevValue.setVisible(false);

        globalThresholdValue.setVisible(true);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------------//
    // LFieldValidater //
    //-----------------//
    /**
     * Check the value of an LTextField, and if it is not a valid number
     * within the specified range, turn the background red to inform the user.
     */
    private class LFieldValidater
            implements DocumentListener
    {
        public double MIN;

        public double MAX;

        public LTextField field;

        public boolean wasValid = true;

        public LFieldValidater (LTextField field,
                                double min,
                                double max)
        {
            this.MIN = min;
            this.MAX = max;
            this.field = field;
        }

        @Override
        public void insertUpdate (DocumentEvent e)
        {
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
        public void removeUpdate (DocumentEvent e)
        {
            insertUpdate(e);
        }

        @Override
        public void changedUpdate (DocumentEvent e)
        {
        }
    }

    //-------------//
    // ParamAction //
    //-------------//
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