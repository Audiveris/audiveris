//----------------------------------------------------------------------------//
//                                                                            //
//                       S c o r e P a r a m e t e r s                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.constant.ConstantSet;

import omr.glyph.text.Language;

import omr.log.Logger;

import omr.score.Score;
import omr.score.entity.MeasureId;
import omr.score.entity.MeasureId.MeasureRange;
import omr.score.entity.MeasureId.ScoreBased;
import omr.score.entity.ScorePart;
import omr.score.entity.SlotPolicy;
import omr.score.midi.MidiAbstractions;

import omr.script.ParametersTask;
import omr.script.ScriptActions;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.step.Step;
import omr.step.Steps;

import omr.ui.FileDropHandler;
import omr.ui.field.LDoubleField;
import omr.ui.field.LIntegerField;
import omr.ui.field.LTextField;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code ScoreParameters} is a dialog that manages information
 * as both a display and possible input from user for major Score/Sheet
 * parameters such as:<ul>
 * <li>Call-stack printed on exception</li>
 * <li>Prompt for saving script on closing</li>
 * <li>Step trigerred by drag and drop</li>
 * <li>Max value for foreground pixels</li>
 * <li>Histogram threshold for staff lines detection</li>
 * <li>Abscissa margin for time slots</li>
 * <li>Text language</li>
 * <li>Midi volume and tempo</li>
 * <li>Parts name and instrument</li>
 * <li>Measure range selection</li>
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

    /** The swing component of this panel */
    protected final Panel component;

    /** The related score */
    private final Score score;

    /** Collection of individual data panes */
    private final List<Pane> panes = new ArrayList<Pane>();

    /** Related script task */
    private ParametersTask task;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // ScoreParameters //
    //-----------------//
    /**
     * Create a ScoreParameters object.
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
        panes.add(new ForegroundPane());
        panes.add(new SlotPane());
        panes.add(new LanguagePane());
        panes.add(new VolumePane());
        panes.add(new TempoPane());

        if ((score != null) && (score.getPartList() != null)) {
            // Part by part information
            panes.add(new ScorePane());

            // Add measure pane iff we have measures
            if (!score.getFirstPage()
                      .getFirstSystem()
                      .getFirstPart()
                      .getMeasures()
                      .isEmpty()) {
                panes.add(new MeasurePane());
            }
        }

        // Layout
        defineLayout();
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // commit //
    //--------//
    /**
     * Check the values and commit them if all are OK
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
     * Report the UI component
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

        FormLayout   layout = Panel.makeFormLayout(logicalRowCount, 3);
        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();
        int             r = 1;

        for (Pane pane : panes) {
            if (pane.getLabel() != null) {
                builder.addSeparator(pane.getLabel(), cst.xyw(1, r, 11));
                r += 2;
            }

            r = pane.defineLayout(builder, cst, r);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

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

    //-------------//
    // BooleanPane //
    //-------------//
    /**
     * A template for pane with just one global boolean
     */
    private abstract static class BooleanPane
        extends Pane
    {
        //~ Instance fields ----------------------------------------------------

        /** CheckBox for boolean */
        protected final JCheckBox promptBox = new JCheckBox();

        //~ Constructors -------------------------------------------------------

        public BooleanPane (String  label,
                            String  text,
                            String  tip,
                            boolean selected)
        {
            super(label);

            promptBox.setText(text);
            promptBox.setToolTipText(tip);
            promptBox.setSelected(selected);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
        {
            builder.add(promptBox, cst.xyw(3, r, 3));

            return r + 2;
        }
    }

    //------------//
    // ScriptPane //
    //------------//
    /**
     * Should we prompt the user for saving the script when sheet is closed?
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
     * Should we print the call-stack when a warning with exception occurs
     */
    private static class StackPane
        extends BooleanPane
    {
        //~ Constructors -------------------------------------------------------

        public StackPane ()
        {
            super(
                "Call-Stack",
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
     * Which step should we trigger on Drag n' Drop?
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
            super("Drag 'n Drop");

            // ComboBox for triggered step
            stepCombo = new JComboBox<Step>(
                Steps.values().toArray(new Step[0]));
            stepCombo.setToolTipText("Step to trigger on Drag 'n Drop");
            stepCombo.setSelectedItem(FileDropHandler.getDefaultStep());
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void commit ()
        {
            /** Since this info is not registered in the ParametersTask */
            Step step = (Step) stepCombo.getItemAt(
                stepCombo.getSelectedIndex());
            FileDropHandler.setDefaultStep(step);
        }

        @Override
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
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
     * Pane to set the dominant text language
     */
    private class LanguagePane
        extends DefaultPane
    {
        //~ Instance fields ----------------------------------------------------

        /** ComboBox for text language */
        final JComboBox langCombo;

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
                String item = (String) langCombo.getItemAt(
                    langCombo.getSelectedIndex());
                String code = codeOf(item);

                if (!Language.getDefaultLanguage()
                             .equals(code)) {
                    logger.info("Default language is now '" + code + "'");
                    Language.setDefaultLanguage(code);
                }
            }
        }

        @Override
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
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
            String item = (String) langCombo.getItemAt(
                langCombo.getSelectedIndex());
            task.setLanguage(codeOf(item));

            return true;
        }

        /** Report the code out of a label */
        private String codeOf (String label)
        {
            return label.substring(0, 3);
        }

        /** Create a combo box filled with supported language items */
        private JComboBox createLangCombo ()
        {
            // Build the item list, only with the supported languages
            List<String> items = new ArrayList<String>();

            for (String code : new TreeSet<String>(
                Language.getSupportedLanguages())) {
                items.add(itemOf(code));
            }

            JComboBox<String> combo = new JComboBox<String>(
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
                return code + " - " + fullName;
            } else {
                return code;
            }
        }
    }

    //------//
    // Pane //
    //------//
    /**
     * A pane is a sub-component of the ScoreParameters, able to host data, check
     * data validity and apply the requested modifications.
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
         * Build the related user interface
         * @param builder the shared panel builder
         * @param cst the cell constraints
         * @param r initial row value
         * @return final row value
         */
        public abstract int defineLayout (PanelBuilder    builder,
                                          CellConstraints cst,
                                          int             r);

        /**
         * Commit the modifications, for the items that are not handled by the
         * ParametersTask, which means all actions related to default values.
         */
        public void commit ()
        {
        }

        /** Report the separator label if any */
        public String getLabel ()
        {
            return label;
        }

        /**
         * Report the count of needed logical rows
         * Typically 2 (the label separator plus 1 line of data)
         */
        public int getLogicalRowCount ()
        {
            return 2;
        }

        /**
         * Check whether all the pane data are valid, and feed the
         * ParametersTask accordingly
         * @return true if everything is OK, false otherwise
         */
        public boolean isValid ()
        {
            return true; // By default
        }
    }

    //-------------//
    // DefaultPane //
    //-------------//
    /**
     * A pane with a checkBox to set parameter as a global default
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
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
        {
            builder.add(defaultBox, cst.xyw(3, r, 3));

            return r;
        }
    }

    //----------------//
    // ForegroundPane //
    //----------------//
    /**
     * Pane to define the upper limit for foreground pixels
     */
    private class ForegroundPane
        extends SliderPane
    {
        //~ Constructors -------------------------------------------------------

        public ForegroundPane ()
        {
            super(
                "Foreground Pixels",
                new LIntegerField(
                    false,
                    "Level",
                    "Max level for foreground pixels"),
                0,
                255,
                ((score != null) &&
                                score.getFirstPage()
                                     .getSheet()
                                     .hasMaxForeground())
                                ? score.getFirstPage().getSheet().getMaxForeground()
                                : Sheet.getDefaultMaxForeground());
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void commit ()
        {
            if (defaultBox.isSelected() &&
                (intValue() != Sheet.getDefaultMaxForeground())) {
                Sheet.setDefaultMaxForeground(intValue());
                logger.info("Default max foreground is now " + intValue());
            }
        }

        @Override
        public int getLogicalRowCount ()
        {
            return 3;
        }

        @Override
        public boolean isValid ()
        {
            task.setForeground(intValue());

            return true;
        }
    }

    //-------------//
    // MeasurePane //
    //-------------//
    private class MeasurePane
        extends Pane
        implements ItemListener
    {
        //~ Instance fields ----------------------------------------------------

        /** Range selection */
        final JCheckBox rangeBox = new JCheckBox();

        /** First measure Id */
        final LTextField firstId = new LTextField(
            true,
            "first Id",
            "First measure id of measure range");

        /** Last measure Id */
        final LTextField lastId = new LTextField(
            true,
            "last Id",
            "Last measure id of measure range");

        //~ Constructors -------------------------------------------------------

        public MeasurePane ()
        {
            super("Measure range");

            // rangeBox
            rangeBox.setText("Select");
            rangeBox.setToolTipText("Check to enable measure selection");
            rangeBox.addItemListener(this);

            // Default measure range bounds
            MeasureId.MeasureRange range = score.getMeasureRange();

            if (range != null) {
                rangeBox.setSelected(true);
                firstId.setEnabled(true);
                firstId.setText(range.getFirstId().toString());
                lastId.setEnabled(true);
                lastId.setText(range.getLastId().toString());
            } else {
                rangeBox.setSelected(false);
                firstId.setEnabled(false);
                lastId.setEnabled(false);

                try {
                    firstId.setText(
                        score.getFirstPage().getFirstSystem().getFirstPart().getFirstMeasure().getScoreId());
                    lastId.setText(
                        score.getLastPage().getLastSystem().getLastPart().getLastMeasure().getScoreId());
                } catch (Exception ex) {
                    logger.warning("Error on score measure range", ex);
                    rangeBox.setEnabled(false); // Safer
                }
            }
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void commit ()
        {
            /** Since this info is not registered in the ParametersTask */
            if (rangeBox.isSelected()) {
                score.setMeasureRange(
                    new MeasureRange(
                        score,
                        firstId.getText().trim(),
                        lastId.getText().trim()));
            } else {
                score.setMeasureRange(null);
            }
        }

        @Override
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
        {
            builder.add(rangeBox, cst.xy(3, r));
            builder.add(firstId.getLabel(), cst.xy(5, r));
            builder.add(firstId.getField(), cst.xy(7, r));
            builder.add(lastId.getLabel(), cst.xy(9, r));
            builder.add(lastId.getField(), cst.xy(11, r));

            return r + 2;
        }

        @Override
        public boolean isValid ()
        {
            // Measure range
            if (rangeBox.isSelected() &&
                (score.getLastPage()
                      .getLastSystem() != null)) {
                ScoreBased minId = new ScoreBased(
                    score.getFirstPage().getFirstSystem().getFirstPart().getFirstMeasure().getPageId());
                ScoreBased maxId = new ScoreBased(
                    score.getLastPage().getLastSystem().getLastPart().getLastMeasure().getPageId());

                // Check values are valid
                ScoreBased first = null;

                try {
                    first = MeasureId.createScoreBased(
                        score,
                        firstId.getText());
                } catch (Exception ex) {
                    logger.warning(
                        "Illegal first measure id: '" + firstId.getText() +
                        "'",
                        ex);

                    return false;
                }

                ScoreBased last = null;

                try {
                    last = MeasureId.createScoreBased(score, lastId.getText());
                } catch (NumberFormatException ex) {
                    logger.warning(
                        "Illegal last measure id: '" + lastId.getText() + "'",
                        ex);

                    return false;
                }

                // First Measure
                if ((first.compareTo(minId) < 0) ||
                    (first.compareTo(maxId) > 0)) {
                    logger.warning(
                        "First measure Id is not within [" + minId + ".." +
                        maxId + "]: " + first);

                    return false;
                }

                // Last Measure
                if ((last.compareTo(minId) < 0) || (last.compareTo(maxId) > 0)) {
                    logger.warning(
                        "Last measure Id is not within [" + minId + ".." +
                        maxId + "]: " + last);

                    return false;
                }

                // First & last consistency
                if (first.compareTo(last) > 0) {
                    logger.warning(
                        "First measure Id " + first +
                        " is greater than last measure Id " + last);

                    return false;
                }
            }

            return true;
        }

        public void itemStateChanged (ItemEvent e)
        {
            if (rangeBox.isSelected()) {
                firstId.setEnabled(true);
                lastId.setEnabled(true);
            } else {
                firstId.setEnabled(false);
                lastId.setEnabled(false);
            }
        }
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

        public static final int   logicalRowCount = 3;

        //~ Instance fields ----------------------------------------------------

        /** The underlying model  */
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
        private JComboBox<String> midiBox = new JComboBox<String>(
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
            if (name.getText()
                    .trim()
                    .length() == 0) {
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
        private int defineLayout (PanelBuilder    builder,
                                  CellConstraints cst,
                                  int             r)
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

    //-----------//
    // ScorePane //
    //-----------//
    /**
     * Pane to define the details for each part
     */
    private class ScorePane
        extends Pane
    {
        //~ Instance fields ----------------------------------------------------

        /** Map of score part panes */
        private final Map<ScorePart, PartPane> partPanes = new HashMap<ScorePart, PartPane>();

        //~ Constructors -------------------------------------------------------

        public ScorePane ()
        {
            super(null);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
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
            return PartPane.logicalRowCount * score.getPartList()
                                                   .size();
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
     * Abstract Pane to handle a slider + numeric value and a SetAsDefault box
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

        public SliderPane (String       separator,
                           LDoubleField numValue,
                           int          minNumValue,
                           int          maxNumValue,
                           double       initValue)
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

        public SliderPane (String        separator,
                           LIntegerField numValue,
                           int           minNumValue,
                           int           maxNumValue,
                           int           initValue)
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
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
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
     * Pane to define the abscissa margin around a common time slot
     */
    private class SlotPane
        extends SliderPane
    {
        //~ Instance fields ----------------------------------------------------

        /** ComboBox for Slot policy */
        final JComboBox policyCombo;

        /** Set as default? */
        final JCheckBox policyDefaultBox = new JCheckBox();

        //~ Constructors -------------------------------------------------------

        public SlotPane ()
        {
            super(
                "Time Slots",
                new LDoubleField(
                    false,
                    "Margin",
                    "Horizontal margin around Slots, in interline fractions",
                    "%.2f"),
                0,
                (int) (constants.maxSlotMargin.getValue() * 100),
                ((score != null) && score.hasSlotMargin())
                                ? score.getSlotMargin()
                                : Score.getDefaultSlotMargin());
            policyCombo = createPolicyCombo();
            policyDefaultBox.setText("Set as default");
            policyDefaultBox.setToolTipText(
                "Check to set parameter as global default");
            defaultBox.setSelected(score == null);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void commit ()
        {
            SlotPolicy policy = (SlotPolicy) policyCombo.getSelectedItem();

            if (policyDefaultBox.isSelected()) {
                logger.info("Default slot policy is now " + policy);
                Score.setDefaultSlotPolicy(policy);
            }

            double val = dblValue();

            if (defaultBox.isSelected() &&
                (Math.abs(Score.getDefaultSlotMargin() - val) > .001)) {
                logger.info("Default slot margin is now " + val);
                Score.setDefaultSlotMargin(val);
            }
        }

        @Override
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
        {
            builder.add(policyDefaultBox, cst.xyw(3, r, 3));

            JLabel policyLabel = new JLabel("Policy", SwingConstants.RIGHT);
            builder.add(policyLabel, cst.xyw(5, r, 3));
            builder.add(policyCombo, cst.xyw(9, r, 3));
            r += 2;

            return super.defineLayout(builder, cst, r);
        }

        @Override
        public int getLogicalRowCount ()
        {
            return 4;
        }

        @Override
        public boolean isValid ()
        {
            task.setSlotPolicy((SlotPolicy) policyCombo.getSelectedItem());
            task.setSlotMargin(dblValue());

            return true;
        }

        /** Create a combo box filled with Slot Policy items */
        private JComboBox createPolicyCombo ()
        {
            JComboBox<SlotPolicy> combo = new JComboBox<SlotPolicy>(
                SlotPolicy.values());
            combo.setToolTipText("Policy to determine time slots");

            combo.setSelectedItem(
                ((score != null) && score.hasSlotPolicy())
                                ? score.getSlotPolicy()
                                : Score.getDefaultSlotPolicy());

            return combo;
        }
    }

    //-----------//
    // TempoPane //
    //-----------//
    /**
     * Pane for MIDI tempo
     */
    private class TempoPane
        extends DefaultPane
    {
        //~ Instance fields ----------------------------------------------------

        /** Tempo */
        final LIntegerField tempo = new LIntegerField(
            "Tempo",
            "Tempo value in number of quarters per minute");

        //~ Constructors -------------------------------------------------------

        public TempoPane ()
        {
            super("Midi Tempo");

            tempo.setValue(
                ((score != null) && score.hasTempo()) ? score.getTempo()
                                : Score.getDefaultTempo());
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void commit ()
        {
            int val = tempo.getValue();

            if (defaultBox.isSelected() && (val != Score.getDefaultTempo())) {
                Score.setDefaultTempo(val);
                logger.info("Default MIDI tempo is now " + val);
            }
        }

        @Override
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
        {
            r = super.defineLayout(builder, cst, r);
            builder.add(tempo.getLabel(), cst.xy(9, r));
            builder.add(tempo.getField(), cst.xy(11, r));

            return r + 2;
        }

        @Override
        public boolean isValid ()
        {
            int val = tempo.getValue();

            if ((val < 0) || (val > 1000)) {
                logger.warning("Tempo value should be in 0..1000 range");

                return false;
            }

            task.setTempo(val);

            return true;
        }
    }

    //------------//
    // VolumePane //
    //------------//
    /**
     * Pane for MIDI volume
     */
    private class VolumePane
        extends SliderPane
    {
        //~ Constructors -------------------------------------------------------

        public VolumePane ()
        {
            super(
                "Midi Volume",
                new LIntegerField(false, "Volume", "Volume for playback"),
                0,
                255,
                ((score != null) && score.hasVolume()) ? score.getVolume()
                                : Score.getDefaultVolume());
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void commit ()
        {
            int val = intValue();

            if (defaultBox.isSelected() && (val != Score.getDefaultVolume())) {
                Score.setDefaultVolume(val);
                logger.info("Default MIDI volume is now " + val);
            }
        }

        @Override
        public boolean isValid ()
        {
            int val = intValue();

            if ((val < 0) || (val > 127)) {
                logger.warning("Volume value should be in 0..127 range");

                return false;
            }

            task.setVolume(val);

            return true;
        }
    }
}
