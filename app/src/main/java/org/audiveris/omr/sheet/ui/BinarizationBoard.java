//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                B i n a r i z a t i o n B o a r d                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
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
import org.audiveris.omr.ui.field.LTextField;
import org.audiveris.omr.ui.selection.LocationEvent;
import static org.audiveris.omr.ui.selection.MouseMovement.PRESSING;
import static org.audiveris.omr.ui.selection.SelectionHint.LOCATION_INIT;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UIUtil;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
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

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(BinarizationBoard.class);

    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(BinarizationBoard.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The related sheet. */
    private final Sheet sheet;

    /** Radio button to select the adaptive filter. */
    private final JRadioButton adaptiveFilter = new JRadioButton();

    /** Radio button to select the global filter. */
    private final JRadioButton globalFilter = new JRadioButton();

    /** Input field for global filter's threshold. */
    private final LIntegerField globalThreshold = new LIntegerField("globalThreshold");

    /** Input field for adaptive filter's mean. */
    private final LDoubleField adaptiveMean = new LDoubleField(true, "adaptiveMean", "%.2f");

    /** Input field for adaptive filter's standard deviation. */
    private final LDoubleField adaptiveStdDev = new LDoubleField(true, "adaptiveStdDev", "%.2f");

    /** Apply button. */
    private final JButton apply = new JButton();

    /** Reset button. */
    private final JButton reset = new JButton();

    /** Apply to all pages button. */
    private final JButton applyToWholeBook = new JButton();

    /** Checkbox for overwriting sheets. */
    private final LCheckBox overwrite = new LCheckBox("overwrite");

    /** Reference to the last filter used on this board. */
    private FilterDescriptor previousFilter;

    /**
     * Initially null, then equal to false if this sheet has no gray source image to binarize.
     */
    private Boolean noGraySource;

    /** Label for when no gray source image is present. */
    private final JLabel noGraySourceLabel = new JLabel();

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
        super(BoardDesc.BINARIZATION, sheet.getLocationService(), null, false, false, false, false);

        this.sheet = sheet;

        isMultiSheetBook = sheet.getStub().getBook().isMultiSheet();
        previousFilter = sheet.getStub().getBook().getBinarizationParam().getSpecific();

        apply.addActionListener(this);

        reset.addActionListener(this);

        applyToWholeBook.addActionListener(this);

        adaptiveMean.getField().getDocument().addDocumentListener(
                new LFieldValidater(
                        adaptiveMean,
                        AdaptiveDescriptor.MINMEAN,
                        AdaptiveDescriptor.MAXMEAN));
        adaptiveStdDev.getField().getDocument().addDocumentListener(
                new LFieldValidater(
                        adaptiveStdDev,
                        AdaptiveDescriptor.MINSTDDEV,
                        AdaptiveDescriptor.MAXSTDDEV));
        globalThreshold.getField().getDocument().addDocumentListener(
                new LFieldValidater(
                        globalThreshold,
                        GlobalDescriptor.MINTHRESHOLD,
                        GlobalDescriptor.MAXTHRESHOLD));

        adaptiveFilter.addActionListener(this);
        globalFilter.addActionListener(this);

        // Make radio buttons act as one group
        final ButtonGroup imageButtonGroup = new ButtonGroup();
        imageButtonGroup.add(adaptiveFilter);
        imageButtonGroup.add(globalFilter);

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
        if (e.getSource() == apply) {
            applyAndRunFilter();
        } else if (e.getSource() == applyToWholeBook) {
            // Optionally erase every sheet's specific binarization setting
            if (overwrite.getField().isSelected()) {
                sheet.getStub().getBook().getStubs().forEach( (stub) -> {
                    stub.getBinarizationFilterParam().setSpecific(null);
                });
            }

            // Apply current filter settings to this book's FilterParam
            applyFilterSettings(sheet.getStub().getBook().getBinarizationParam());

            // Run binarization for this sheet only
            runBinarizationFilter(sheet);

        } else if (e.getSource() == reset) {
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

        } else if (e.getSource() == adaptiveFilter) {
            showAdaptiveFilterInput();
            applyAndRunFilter();
        } else if (e.getSource() == globalFilter) {
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
        if (adaptiveFilter.isSelected()) {
            double mean = adaptiveMean.getValue();
            double stdDev = adaptiveStdDev.getValue();

            if (mean < AdaptiveDescriptor.MINMEAN || mean > AdaptiveDescriptor.MAXMEAN) {
                mean = Math.max(
                        Math.min(mean, AdaptiveDescriptor.MAXMEAN),
                        AdaptiveDescriptor.MINMEAN);
                adaptiveMean.setValue(mean);
            }

            if (stdDev < AdaptiveDescriptor.MINSTDDEV || stdDev > AdaptiveDescriptor.MAXSTDDEV) {
                stdDev = Math.max(
                        Math.min(stdDev, AdaptiveDescriptor.MAXSTDDEV),
                        AdaptiveDescriptor.MINSTDDEV);
                adaptiveStdDev.setValue(stdDev);
            }

            filterParam.setSpecific(new AdaptiveDescriptor(mean, stdDev));

        } else if (globalFilter.isSelected()) {
            int threshold = globalThreshold.getValue();

            if (threshold < GlobalDescriptor.MINTHRESHOLD
                    || threshold > GlobalDescriptor.MAXTHRESHOLD) {
                threshold = Math.max(
                        Math.min(threshold, GlobalDescriptor.MAXTHRESHOLD),
                        GlobalDescriptor.MINTHRESHOLD);
                globalThreshold.setValue(threshold);
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
        final int inset = UIUtil.adjustedSize(constants.inset.getValue());
        ((Panel) getBody()).setInsets(inset, 0, inset, inset); // TLBR

        final FormLayout layout = Panel.makeFormLayout(6, 3);
        final FormBuilder builder = FormBuilder.create().layout(layout).panel(getBody());

        int r = 1; // -----------------------------

        // Place holder: Room for a message if no gray source if available
        noGraySourceLabel.setVisible(false);
        builder.addRaw(noGraySourceLabel).xyw(2, r, 8);
        r += 2;

        builder.addRaw(adaptiveFilter).xyw(1, r, 8);
        adaptiveFilter.setHorizontalAlignment(SwingConstants.LEFT);

        r += 2; // --------------------------------

        builder.addRaw(globalFilter).xyw(1, r, 8);
        globalFilter.setHorizontalAlignment(SwingConstants.LEFT);

        r += 2; // --------------------------------

        builder.addRaw(globalThreshold.getLabel()).xyw(1, r, 3);
        builder.addRaw(globalThreshold.getField()).xy(5, r);

        // Overlapping line
        builder.addRaw(adaptiveMean.getLabel()).xyw(1, r, 3);
        builder.addRaw(adaptiveMean.getField()).xy(5, r);

        builder.addRaw(adaptiveStdDev.getLabel()).xyw(7, r, 3);
        builder.addRaw(adaptiveStdDev.getField()).xy(11, r);

        r += 2; // --------------------------------

        builder.addRaw(apply).xyw(1, r, 4);
        builder.addRaw(reset).xyw(7, r, 4);

        if (isMultiSheetBook) {
            r += 2; // ----------------------------

            builder.addRaw(applyToWholeBook).xyw(1, r, 4);

            overwrite.getField().setHorizontalAlignment(SwingConstants.RIGHT);
            builder.addRaw(overwrite.getField()).xyw(5, r, 1);
            builder.addRaw(overwrite.getLabel()).xyw(7, r, 5);
        }

        // Resources injection
        adaptiveFilter.setName("adaptiveFilter");
        globalFilter.setName("globalFilter");
        apply.setName("apply");
        reset.setName("reset");
        applyToWholeBook.setName("applyToWholeBook");
        noGraySourceLabel.setName("noGraySourceLabel");
        resources.injectComponents(getComponent());
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
                adaptiveMean.setValue(ad.meanCoeff);
                adaptiveStdDev.setValue(ad.stdDevCoeff);
                adaptiveFilter.setSelected(true);

                globalThreshold.setValue(GlobalDescriptor.getDefaultThreshold());

                showAdaptiveFilterInput();

            }
            case GlobalDescriptor gd -> {
                globalThreshold.setValue(gd.threshold);
                globalFilter.setSelected(true);

                adaptiveMean.setValue(AdaptiveDescriptor.getDefaultMeanCoeff());
                adaptiveStdDev.setValue(AdaptiveDescriptor.getDefaultStdDevCoeff());

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
            final NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
            final double value = nf.parse(field.getText().trim()).doubleValue();
            return value >= min && value <= max;
        } catch (ParseException e) {
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
        adaptiveMean.setVisible(true);
        adaptiveStdDev.setVisible(true);

        globalThreshold.setVisible(false);
    }

    //-----------------------//
    // showGlobalFilterInput //
    //-----------------------//
    /**
     * Makes global filter inputs visible.
     */
    private void showGlobalFilterInput ()
    {
        adaptiveMean.setVisible(false);
        adaptiveStdDev.setVisible(false);

        globalThreshold.setVisible(true);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer inset = new Constant.Integer(
                "pixels",
                3,
                "Body inset value");
    }

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