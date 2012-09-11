//----------------------------------------------------------------------------//
//                                                                            //
//                       S c o r e P a r a m e t e r s                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.constant.ConstantSet;

import omr.text.Language;

import omr.log.Logger;

import omr.run.AdaptiveDescriptor;
import omr.run.FilterDescriptor;
import omr.run.FilterKind;
import omr.run.GlobalDescriptor;

import omr.score.Score;
import omr.score.entity.ScorePart;
import omr.score.entity.SlotPolicy;
import omr.score.midi.MidiAbstractions;

import omr.script.ParametersTask;
import omr.script.ScriptActions;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.step.Step;
import omr.step.Steps;

import omr.text.OCR.UnavailableOcrException;
import omr.text.TextBuilder;

import omr.ui.FileDropHandler;
import omr.ui.field.LDoubleField;
import omr.ui.field.LIntegerField;
import omr.ui.field.LTextField;
import omr.ui.field.SpinnerUtilities;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import java.awt.event.ActionEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.Action;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code ScoreParameters} is a dialog that manages information
 * as both a display and possible input from user for major Score/Sheet
 * parameters such as:
 * <ul>
 * <li>Call-stack printed on exception</li>
 * <li>Prompt for saving script on closing</li>
 * <li>Step triggerred by drag and drop</li>
 * <li>Binarization parameters</li>
 * <li>Policy and abscissa margin for time slots</li>
 * <li>Text language</li>
 * <li>Parts name and instrument</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class ScoreParameters
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
            ScoreParameters.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The swing component of this panel */
    private final Panel component;

    /** The related score */
    private final Score score;

    /** Collection of individual data panes */
    private final List<Pane> panes = new ArrayList<>();

    /** Related script task */
    private ParametersTask task;

    //~ Constructors -----------------------------------------------------------
    //
    //-----------------//
    // ScoreParameters //
    //-----------------//
    /**
     * Create a ScoreParameters object.
     *
     * @param score the related score
     */
    public ScoreParameters (Score score)
    {
        this.score = score;

        component = new Panel();

        // Sequence of Pane instances, w/ or w/o a score
        panes.add(new StackPane());
        panes.add(new ScriptPane());
        panes.add(new DnDPane());
        panes.add(new BinarizationPane());
        panes.add(new SlotPane());

        // Caution: The language pane needs Tesseract up & running
        try {
            panes.add(new LanguagePane());
        } catch (UnavailableOcrException ex) {
            logger.info("No language pane for lack of OCR");
        } catch (Throwable ex) {
            logger.warning("Error creating language pane", ex);
        }

        if ((score != null) && (score.getPartList() != null)) {
            // Part by part information
            panes.add(new ScorePane());
        }

        // Layout
        defineLayout();
    }

    //~ Methods ----------------------------------------------------------------
    //
    //--------//
    // commit //
    //--------//
    /**
     * Check the values and commit them if all are OK.
     *
     * @param sheet the related sheet
     * @return true if committed, false otherwise
     */
    public boolean commit (Sheet sheet)
    {
        if (dataIsValid()) {
            try {
                // Just launch the prepared task
                if (sheet != null) {
                    task.launch(sheet);
                }

                // Also carry out the actions not covered by the task
                for (Pane pane : panes) {
                    pane.commit();
                }
            } catch (Exception ex) {
                logger.warning("Could not run ParametersTask", ex);

                return false;
            }

            return true;
        } else {
            return false;
        }
    }

    //--------------//
    // getComponent //
    //--------------//
    /**
     * Report the UI component.
     *
     * @return the concrete component
     */
    public JPanel getComponent ()
    {
        return component;
    }

    //-------------//
    // dataIsValid //
    //-------------//
    /**
     * Make sure every user-entered data is valid, and while doing so,
     * feed a ParametersTask to be run on the related score/sheet
     *
     * @return true if everything is OK, false otherwise
     */
    private boolean dataIsValid ()
    {
        task = new ParametersTask();

        for (Pane pane : panes) {
            if (!pane.isValid()) {
                task = null; // Cleaner

                return false;
            }
        }

        return true;
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        // Compute the total number of logical rows
        int logicalRowCount = 0;

        for (Pane pane : panes) {
            logicalRowCount += pane.getLogicalRowCount();
        }

        FormLayout layout = Panel.makeFormLayout(logicalRowCount, 3);
        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();
        int r = 1;

        for (Pane pane : panes) {
            if (pane.getLabel() != null) {
                builder.addSeparator(pane.getLabel(), cst.xyw(1, r, 11));
                r += 2;
            }

            r = pane.defineLayout(builder, cst, r);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //
    //-------------//
    // BooleanPane //
    //-------------//
    /**
     * A template for pane with just one global boolean.
     */
    private abstract static class BooleanPane
            extends Pane
    {
        //~ Instance fields ----------------------------------------------------

        /** CheckBox for boolean */
        protected final JCheckBox promptBox = new JCheckBox();

        //~ Constructors -------------------------------------------------------
        public BooleanPane (String label,
                            String text,
                            String tip,
                            boolean selected)
        {
            super(label);

            promptBox.setText(text);
            promptBox.setToolTipText(tip);
            promptBox.setSelected(selected);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            builder.add(promptBox, cst.xyw(3, r, 3));

            return r + 2;
        }
    }

    //-------------//
    // DefaultPane //
    //-------------//
    /**
     * A pane with a checkBox to set parameter as a global default.
     */
    private abstract class DefaultPane
            extends Pane
    {
        //~ Instance fields ----------------------------------------------------

        /** Set as default? */
        protected final JCheckBox defaultBox = new JCheckBox();

        //~ Constructors -------------------------------------------------------
        public DefaultPane (String label)
        {
            super(label);

            defaultBox.setText("Set as default");
            defaultBox.setToolTipText(
                    "Check to set parameter as global default");

            // If no current score, push for a global setting
            defaultBox.setSelected(score == null);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            builder.add(defaultBox, cst.xyw(3, r, 3));

            return r;
        }
    }

    //----------//
    // SpinData //
    //----------//
    /**
     * A line with a checkBox and a parameter handled by a spinner.
     */
    private class SpinData
    {

        /** Set as default? */
        protected final JCheckBox box;

        protected final JLabel label;

        protected final JSpinner spinner;

        public SpinData (String label,
                         String tip,
                         double value,
                         double minimum,
                         double maximum,
                         double stepSize,
                         boolean withBox)
        {
            if (withBox) {
                box = new JCheckBox();
                box.setText("Set as default");
                box.setToolTipText(
                        "Check to set parameter as global default");

                // If no current score, push for a global setting
                box.setSelected(score == null);
            } else {
                box = null;
            }

            this.label = new JLabel(label, SwingConstants.RIGHT);

            SpinnerNumberModel model = new SpinnerNumberModel(
                    value, minimum, maximum, stepSize);
            spinner = new JSpinner(model);
            SpinnerUtilities.setRightAlignment(spinner);
            SpinnerUtilities.setEditable(spinner, true);
            spinner.setToolTipText(tip);
        }

        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            if (box != null) {
                builder.add(box, cst.xyw(3, r, 3));
            }
            builder.add(label, cst.xyw(7, r, 3));
            builder.add(spinner, cst.xyw(11, r, 1));

            r += 2;
            return r;
        }

        public void setVisible (boolean bool)
        {
            if (box != null) {
                box.setVisible(bool);
            }
            label.setVisible(bool);
            spinner.setVisible(bool);
        }
    }

    //------------------//
    // BinarizationPane //
    //------------------//
    /**
     * Pane to define the pixel binarization parameters.
     */
    private class BinarizationPane
            extends Pane
    {

        /** ComboBox for filter */
        final JComboBox<FilterKind> filterCombo;

        /** Set as default? */
        final JCheckBox defaultBox = new JCheckBox();

        /** Handling of entered / selected values */
        private final Action paramAction;

        // Data for global
        final SpinData globalData;

        // Data for local
        final SpinData localDataMean;

        final SpinData localDataDev;

        //~ Constructors -------------------------------------------------------
        public BinarizationPane ()
        {
            super("Binarization");

            FilterDescriptor desc =
                    ((score != null) && score.hasFilterDescriptor())
                    ? score.getFilterDescriptor()
                    : FilterDescriptor.getDefault();

            // Global data
            GlobalDescriptor globalDesc = (desc instanceof GlobalDescriptor)
                    ? (GlobalDescriptor) desc
                    : GlobalDescriptor.getDefault();
            globalData = new SpinData("Threshold",
                    "Global threshold for foreground pixels",
                    globalDesc.threshold, 0, 255, 1, false);
            globalData.setVisible(false);

            // Local data
            AdaptiveDescriptor localDesc = (desc instanceof AdaptiveDescriptor)
                    ? (AdaptiveDescriptor) desc
                    : AdaptiveDescriptor.getDefault();
            localDataMean = new SpinData("Coeff for Mean",
                    "Coefficient for mean pixel value",
                    localDesc.meanCoeff, 0.5, 1.5, 0.1, false);
            localDataMean.setVisible(false);

            localDataDev = new SpinData("Coeff for StdDev",
                    "Coefficient for standard deviation value",
                    localDesc.stdDevCoeff, 0.2, 1.5, 0.1, false);
            localDataDev.setVisible(false);

            // Filter default box
            defaultBox.setSelected(score == null);
            defaultBox.setText("Set as default");
            defaultBox.setToolTipText(
                    "Check to set parameter as global default");

            // ComboBox for filter
            filterCombo = new JComboBox<>(FilterKind.values());
            filterCombo.setToolTipText("Specific filter on image pixels");

            // At the end, so that proper lines are made visible
            paramAction = new ParamAction();
            filterCombo.addActionListener(paramAction);
            filterCombo.setSelectedItem(desc.getKind());
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void commit ()
        {
            if (defaultBox.isSelected()) {
                FilterDescriptor desc = task.getFilter();
                if (!FilterDescriptor.getDefault().equals(desc)) {
                    FilterDescriptor.setDefault(desc);
                    logger.info("Default pixel filter is now {0}", desc);
                }
            }
        }

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            builder.add(defaultBox, cst.xyw(3, r, 3));

            JLabel filterLabel = new JLabel("Filter", SwingConstants.RIGHT);
            builder.add(filterLabel, cst.xyw(5, r, 3));
            builder.add(filterCombo, cst.xyw(9, r, 3));
            r += 2;

            // Set global and local data as mutual overlays
            globalData.defineLayout(builder, cst, r);
            r = localDataMean.defineLayout(builder, cst, r);
            r = localDataDev.defineLayout(builder, cst, r);

            return r;
        }

        @Override
        public int getLogicalRowCount ()
        {
            return 4;
        }

        @Override
        public boolean isValid ()
        {
            FilterDescriptor desc = null;
            FilterKind kind = filterCombo.getItemAt(filterCombo.getSelectedIndex());
            switch (kind) {
            case GLOBAL:
                desc = new GlobalDescriptor((int) Math.rint(
                        (Double) globalData.spinner.getValue()));
                break;
            case ADAPTIVE:
                desc = new AdaptiveDescriptor(
                        (Double) localDataMean.spinner.getValue(),
                        (Double) localDataDev.spinner.getValue());
                break;
            default:
            }

            task.setFilter(desc);

            return true;
        }

        //-------------//
        // ParamAction //
        //-------------//
        private class ParamAction
                extends AbstractAction
        {
            // Method run whenever user make a combo selection

            @Override
            public void actionPerformed (ActionEvent e)
            {
                FilterKind kind = filterCombo.getItemAt(
                        filterCombo.getSelectedIndex());
                switch (kind) {
                case GLOBAL:
                    globalData.setVisible(true);
                    localDataMean.setVisible(false);
                    localDataDev.setVisible(false);
                    break;

                case ADAPTIVE:
                    globalData.setVisible(false);
                    localDataMean.setVisible(true);
                    localDataDev.setVisible(true);
                    break;
                default:
                }
            }
        }
    }

    //------//
    // Pane //
    //------//
    /**
     * A pane is a sub-component of the ScoreParameters, able to host
     * data, check data validity and apply the requested modifications.
     */
    private abstract static class Pane
    {
        //~ Instance fields ----------------------------------------------------

        /** String used as pane label */
        protected final String label;

        //~ Constructors -------------------------------------------------------
        public Pane (String label)
        {
            this.label = label;
        }

        //~ Methods ------------------------------------------------------------
        /**
         * Commit the modifications, for the items that are not handled
         * by the ParametersTask, which means all actions related to
         * default values.
         */
        public void commit ()
        {
        }

        /**
         * Build the related user interface
         *
         * @param builder the shared panel builder
         * @param cst     the cell constraints
         * @param r       initial row value
         * @return final row value
         */
        public abstract int defineLayout (PanelBuilder builder,
                                          CellConstraints cst,
                                          int r);

        /** Report the separator label if any. */
        public String getLabel ()
        {
            return label;
        }

        /**
         * Report the count of needed logical rows.
         * Typically 2 (the label separator plus 1 line of data)
         */
        public int getLogicalRowCount ()
        {
            return 2;
        }

        /**
         * Check whether all the pane data are valid, and feed the
         * ParametersTask accordingly
         *
         * @return true if everything is OK, false otherwise
         */
        public boolean isValid ()
        {
            return true; // By default
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Maximum value for slot margin */
        final Scale.Fraction maxSlotMargin = new Scale.Fraction(
                2.5,
                "Maximum value for slot margin");

    }

    //----------//
    // PartPane //
    //----------//
    /**
     * Pane for details of one part
     */
    private class PartPane
            extends Panel
    {
        //~ Static fields/initializers -----------------------------------------

        public static final int logicalRowCount = 3;

        //~ Instance fields ----------------------------------------------------
        /** The underlying model */
        private final ScorePart scorePart;

        /** Id of the part */
        private final LTextField id = new LTextField(
                false,
                "Id",
                "Id of the score part");

        /** Name of the part */
        private LTextField name = new LTextField(
                "Name",
                "Name for the score part");

        /** Midi Instrument */
        private JComboBox<String> midiBox = new JComboBox<>(
                MidiAbstractions.getProgramNames());

        //~ Constructors -------------------------------------------------------
        //----------//
        // PartPane //
        //----------//
        public PartPane (ScorePart scorePart)
        {
            this.scorePart = scorePart;

            // Let's impose the id!
            id.setText(scorePart.getPid());

            // Initial setting for part name
            name.setText(
                    (scorePart.getName() != null) ? scorePart.getName()
                    : scorePart.getDefaultName());

            // Initial setting for part midi program
            int prog = (scorePart.getMidiProgram() != null)
                    ? scorePart.getMidiProgram()
                    : scorePart.getDefaultProgram();
            midiBox.setSelectedIndex(prog - 1);
        }

        //~ Methods ------------------------------------------------------------
        //-----------//
        // checkPart //
        //-----------//
        public boolean checkPart ()
        {
            // Part name
            if (name.getText().trim().length() == 0) {
                logger.warning("Please supply a non empty part name");

                return false;
            } else {
                task.addPart(name.getText(), midiBox.getSelectedIndex() + 1);

                return true;
            }
        }

        //--------------//
        // defineLayout //
        //--------------//
        private int defineLayout (PanelBuilder builder,
                                  CellConstraints cst,
                                  int r)
        {
            builder.addSeparator(
                    "Part #" + scorePart.getId(),
                    cst.xyw(1, r, 11));

            r += 2; // --

            builder.add(id.getLabel(), cst.xy(5, r));
            builder.add(id.getField(), cst.xy(7, r));

            builder.add(name.getLabel(), cst.xy(9, r));
            builder.add(name.getField(), cst.xy(11, r));

            r += 2; // --

            builder.add(new JLabel("Midi"), cst.xy(5, r));
            builder.add(midiBox, cst.xyw(7, r, 5));

            return r;
        }
    }

    //------------//
    // ScriptPane //
    //------------//
    /**
     * Should we prompt the user for saving the script when sheet is
     * closed?.
     */
    private static class ScriptPane
            extends BooleanPane
    {
        //~ Constructors -------------------------------------------------------

        public ScriptPane ()
        {
            super(
                    "Script",
                    "Prompt for save",
                    "Should we prompt for saving the script on score closing",
                    ScriptActions.isConfirmOnClose());
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void commit ()
        {
            ScriptActions.setConfirmOnClose(promptBox.isSelected());
        }
    }

    //-----------//
    // StackPane //
    //-----------//
    /**
     * Should we print the call-stack when a warning with exception
     * occurs.
     */
    private static class StackPane
            extends BooleanPane
    {
        //~ Constructors -------------------------------------------------------

        public StackPane ()
        {
            super(
                    "On error",
                    "Print call-stack",
                    "Should we print the call-stack when an exception occurs",
                    Logger.isPrintStackOnWarning());
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void commit ()
        {
            Logger.setPrintStackOnWarning(promptBox.isSelected());
        }
    }

    //---------//
    // DnDPane //
    //---------//
    /**
     * Which step should we trigger on Drag n' Drop?.
     */
    private class DnDPane
            extends Pane
    {
        //~ Instance fields ----------------------------------------------------

        /** ComboBox for desired step */
        final JComboBox<Step> stepCombo;

        //~ Constructors -------------------------------------------------------
        public DnDPane ()
        {
            super("Drag n' Drop");

            // ComboBox for triggered step
            stepCombo = new JComboBox<>(
                    Steps.values().toArray(new Step[0]));
            stepCombo.setToolTipText("Step to trigger on Drag n' Drop");
            stepCombo.setSelectedItem(FileDropHandler.getDefaultStep());
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void commit ()
        {
            /** Since this info is not registered in the ParametersTask */
            Step step = stepCombo.getItemAt(
                    stepCombo.getSelectedIndex());
            FileDropHandler.setDefaultStep(step);
        }

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            JLabel stepLabel = new JLabel(
                    "Triggered step",
                    SwingConstants.RIGHT);
            builder.add(stepLabel, cst.xyw(5, r, 3));
            builder.add(stepCombo, cst.xyw(9, r, 3));

            return r + 2;
        }
    }

    //--------------//
    // LanguagePane //
    //--------------//
    /**
     * Pane to set the dominant text language.
     */
    private class LanguagePane
            extends DefaultPane
    {
        //~ Instance fields ----------------------------------------------------

        /** ComboBox for text language */
        final JComboBox<String> langCombo;

        //~ Constructors -------------------------------------------------------
        public LanguagePane ()
        {
            super("Text");

            // ComboBox for text language
            langCombo = createLangCombo();
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void commit ()
        {
            if (defaultBox.isSelected()) {
                // Make the selected language the global default
                String item = langCombo.getItemAt(langCombo.getSelectedIndex());
                String code = codeOf(item);

                if (!Language.getDefaultLanguage().equals(code)) {
                    logger.info("Default language is now ''{0}''", code);
                    Language.setDefaultLanguage(code);
                }
            }
        }

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            r = super.defineLayout(builder, cst, r);

            JLabel textLabel = new JLabel("Language", SwingConstants.RIGHT);
            builder.add(textLabel, cst.xyw(5, r, 3));
            builder.add(langCombo, cst.xyw(9, r, 3));

            return r + 2;
        }

        @Override
        public boolean isValid ()
        {
            String item = langCombo.getItemAt(langCombo.getSelectedIndex());
            task.setLanguage(codeOf(item));

            return true;
        }

        /** Report the code out of a label */
        private String codeOf (String label)
        {
            return label.substring(0, 3);
        }

        /** Create a combo box filled with supported language items */
        private JComboBox<String> createLangCombo ()
        {
            // Build the item list, only with the supported languages
            List<String> items = new ArrayList<>();

            for (String code : new TreeSet<>(
                    TextBuilder.getOcr().getLanguages())) {
                items.add(itemOf(code));
            }

            JComboBox<String> combo = new JComboBox<>(
                    items.toArray(new String[items.size()]));
            combo.setToolTipText("Dominant language for textual items");

            final String code = ((score != null) && score.hasLanguage())
                    ? score.getLanguage()
                    : Language.getDefaultLanguage();
            combo.setSelectedItem(itemOf(code));

            return combo;
        }

        /** Report an item made of code and full name */
        private String itemOf (String code)
        {
            String fullName = Language.nameOf(code);

            if (fullName != null) {
                return code + " (" + fullName + ")";
            } else {
                return code;
            }
        }
    }

    //-----------//
    // ScorePane //
    //-----------//
    /**
     * Pane to define the details for each part.
     */
    private class ScorePane
            extends Pane
    {
        //~ Instance fields ----------------------------------------------------

        /** Map of score part panes */
        private final Map<ScorePart, PartPane> partPanes = new HashMap<>();

        //~ Constructors -------------------------------------------------------
        public ScorePane ()
        {
            super(null);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            for (ScorePart scorePart : score.getPartList()) {
                PartPane partPane = new PartPane(scorePart);
                r = partPane.defineLayout(builder, cst, r);
                partPanes.put(scorePart, partPane);
                builder.add(partPane, cst.xy(1, r));
                r += 2;
            }

            return r;
        }

        @Override
        public int getLogicalRowCount ()
        {
            return PartPane.logicalRowCount * score.getPartList().size();
        }

        @Override
        public boolean isValid ()
        {
            // Each score part
            for (ScorePart scorePart : score.getPartList()) {
                PartPane partPane = partPanes.get(scorePart);

                if ((partPane != null) && !partPane.checkPart()) {
                    return false;
                }
            }

            return true;
        }
    }

    //------------//
    // SliderPane //
    //------------//
    /**
     * Abstract Pane to handle a slider + numeric value and a
     * SetAsDefault box.
     */
    private class SliderPane
            extends DefaultPane
            implements ChangeListener
    {
        //~ Static fields/initializers -----------------------------------------

        protected static final int FACTOR = 100;

        //~ Instance fields ----------------------------------------------------
        /** Slider */
        protected final JSlider slider;

        /** Numeric value kept in sync */
        protected final LTextField numValue;

        //~ Constructors -------------------------------------------------------
        public SliderPane (String separator,
                           LDoubleField numValue,
                           int minNumValue,
                           int maxNumValue,
                           double initValue)
        {
            super(separator);

            // Slider
            slider = new JSlider(
                    JSlider.HORIZONTAL,
                    minNumValue,
                    maxNumValue,
                    (int) (initValue * FACTOR));

            // Numeric value
            this.numValue = numValue;
            numValue.setValue(dblValue());

            commonInit();
        }

        public SliderPane (String separator,
                           LIntegerField numValue,
                           int minNumValue,
                           int maxNumValue,
                           int initValue)
        {
            super(separator);

            // Slider
            slider = new JSlider(
                    JSlider.HORIZONTAL,
                    minNumValue,
                    maxNumValue,
                    initValue);

            // Numeric value
            this.numValue = numValue;
            numValue.setValue(intValue());

            commonInit();
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            r = super.defineLayout(builder, cst, r);
            builder.add(numValue.getLabel(), cst.xy(9, r));
            builder.add(numValue.getField(), cst.xy(11, r));

            r += 2;
            builder.add(slider, cst.xyw(3, r, 9));

            return r + 2;
        }

        @Override
        public int getLogicalRowCount ()
        {
            return 3;
        }

        @Override
        public void stateChanged (ChangeEvent e)
        {
            if (numValue instanceof LDoubleField) {
                ((LDoubleField) numValue).setValue(dblValue());
            }

            if (numValue instanceof LIntegerField) {
                ((LIntegerField) numValue).setValue(intValue());
            }
        }

        protected double dblValue ()
        {
            return (double) slider.getValue() / FACTOR;
        }

        protected int intValue ()
        {
            return slider.getValue();
        }

        private void commonInit ()
        {
            // Numeric value is kept in sync with Slider
            slider.addChangeListener(this);
        }
    }

    //----------//
    // SlotPane //
    //----------//
    /**
     * Pane to define the abscissa margin around a common time slot.
     */
    private class SlotPane
            extends Pane
    {
        //~ Instance fields ----------------------------------------------------

        /** ComboBox for Slot policy */
        final JComboBox<SlotPolicy> policyCombo;

        /** Set as default? */
        final JCheckBox policyDefaultBox = new JCheckBox();

        /** Margin data */
        final SpinData marginData;

        //~ Constructors -------------------------------------------------------
        public SlotPane ()
        {
            super("Time Slots");

            policyCombo = createPolicyCombo();
            policyDefaultBox.setText("Set as default");
            policyDefaultBox.setToolTipText(
                    "Check to set parameter as global default");
            policyDefaultBox.setSelected(score == null);

            marginData = new SpinData("Margin",
                    "Horizontal margin around Slots, in interline fractions",
                    ((score != null) && score.hasSlotMargin())
                    ? score.getSlotMargin()
                    : Score.getDefaultSlotMargin(),
                    0, constants.maxSlotMargin.getValue(), 0.1,
                    true);
            marginData.box.setSelected(score == null);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void commit ()
        {
            SlotPolicy policy = policyCombo.getItemAt(
                    policyCombo.getSelectedIndex());

            if (policyDefaultBox.isSelected()
                && (policy != Score.getDefaultSlotPolicy())) {
                logger.info("Default slot policy is now {0}", policy);
                Score.setDefaultSlotPolicy(policy);
            }

            double val = (Double) marginData.spinner.getValue();

            if (marginData.box.isSelected()
                && (Math.abs(Score.getDefaultSlotMargin() - val) > .001)) {
                logger.info("Default slot margin is now {0}", val);
                Score.setDefaultSlotMargin(val);
            }
        }

        @Override
        public int defineLayout (PanelBuilder builder,
                                 CellConstraints cst,
                                 int r)
        {
            builder.add(policyDefaultBox, cst.xyw(3, r, 3));

            JLabel policyLabel = new JLabel("Policy", SwingConstants.RIGHT);
            builder.add(policyLabel, cst.xyw(5, r, 3));
            builder.add(policyCombo, cst.xyw(9, r, 3));
            r += 2;

            return marginData.defineLayout(builder, cst, r);
        }

        @Override
        public int getLogicalRowCount ()
        {
            return 3;
        }

        @Override
        public boolean isValid ()
        {
            task.setSlotPolicy(policyCombo.getItemAt(policyCombo.getSelectedIndex()));
            task.setSlotMargin((Double) marginData.spinner.getValue());

            return true;
        }

        /** Create a combo box filled with Slot Policy items */
        private JComboBox<SlotPolicy> createPolicyCombo ()
        {
            JComboBox<SlotPolicy> combo = new JComboBox<>(
                    SlotPolicy.values());
            combo.setToolTipText("Policy to determine time slots");

            combo.setSelectedItem(
                    ((score != null) && score.hasSlotPolicy())
                    ? score.getSlotPolicy()
                    : Score.getDefaultSlotPolicy());

            return combo;
        }
    }
}
