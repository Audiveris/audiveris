//----------------------------------------------------------------------------//
//                                                                            //
//                       S c o r e P a r a m e t e r s                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.glyph.text.Language;

import omr.log.Logger;

import omr.score.MeasureRange;
import omr.score.Score;
import omr.score.entity.ScorePart;
import omr.score.midi.MidiAbstractions;

import omr.script.ParametersTask;
import omr.script.ScriptActions;

import omr.sheet.Sheet;
import omr.sheet.picture.Picture;

import omr.step.Step;

import omr.ui.FileDropHandler;
import omr.ui.field.LDoubleField;
import omr.ui.field.LField;
import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>ScoreParameters</code> is a dialog that manages score information
 * as both a display and possible input from user (text language, midi
 * parameters, parts name and instrument, measure range selection).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreParameters
{
    //~ Static fields/initializers ---------------------------------------------

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
     * Create a ScoreParameters object
     *
     * @param score the related score
     */
    public ScoreParameters (Score score)
    {
        this.score = score;

        component = new Panel();
        component.setNoInsets();

        // Sequence of Pane instances
        panes.add(new DnDPane());
        panes.add(new ScriptPane());
        panes.add(new ForegroundPane());
        panes.add(new HistoPane());
        panes.add(new SlotPane());
        panes.add(new LanguagePane());

        if (score != null) {
            panes.add(new MidiPane());
            panes.add(new ScorePane());

            // Add measure pane iff we have measures
            if (!score.getSystems()
                      .isEmpty() &&
                !score.getFirstSystem()
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

    //-------------//
    // dataIsValid //
    //-------------//
    /**
     * Make sure every user-entered data is valid
     * @return true if every entry is valid
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
        int logRowCount = 0;

        for (Pane pane : panes) {
            logRowCount += pane.getLogicalRowCount();
        }

        FormLayout   layout = Panel.makeFormLayout(logRowCount, 3);
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

        /** Report the separator label if any */
        public String getLabel ()
        {
            return label;
        }

        /**
         * Commit the modifications
         * (for the items that are not handled by the ParametersTask)
         */
        public void commit ()
        {
        }

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
         * Report the count of needed logical rows
         * Typically 2 (the label separator plus 1 line of data)
         */
        public int getLogicalRowCount ()
        {
            return 2;
        }

        /** Are all the pane data valid? */
        public boolean isValid ()
        {
            return true; // By default
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
        final JComboBox stepCombo;

        //~ Constructors -------------------------------------------------------

        public DnDPane ()
        {
            super("Drag 'n Drop");

            // ComboBox for triggered step
            stepCombo = createStepCombo();
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

        /** Create a combo box filled with possible steps */
        private JComboBox createStepCombo ()
        {
            JComboBox combo = new JComboBox(Step.values());
            combo.setToolTipText("Step to trigger on Drag 'n Drop");

            combo.setSelectedItem(FileDropHandler.getDefaultStep());

            return combo;
        }
    }

    //------------//
    // DoublePane //
    //------------//
    /**
     * Abstract Pane to handle a slider for double value and a SetAsDefault box
     */
    private class DoublePane
        extends DefaultPane
        implements ChangeListener
    {
        //~ Static fields/initializers -----------------------------------------

        protected static final int   FACTOR = 100;

        //~ Instance fields ----------------------------------------------------

        /** Slider */
        protected final JSlider slider;

        /** Numeric value kept in sync */
        protected final LDoubleField numValue;

        //~ Constructors -------------------------------------------------------

        public DoublePane (String separator,
                           String label,
                           String tip,
                           int    minNumValue,
                           int    maxNumValue,
                           double initValue)
        {
            super(separator);

            // Slider
            slider = new JSlider(
                JSlider.HORIZONTAL,
                minNumValue,
                maxNumValue,
                (int) (initValue * FACTOR));
            slider.setMajorTickSpacing(50);
            slider.setMinorTickSpacing(10);
            slider.setPaintTicks(true);

            // Numeric value
            numValue = new LDoubleField(false, label, tip, "%.2f");
            numValue.setValue(dblValue());

            // Numeric value is kept in sync with Slider
            slider.addChangeListener(this);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public int getLogicalRowCount ()
        {
            return 3;
        }

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

        public void stateChanged (ChangeEvent e)
        {
            numValue.setValue(dblValue());
        }

        protected double dblValue ()
        {
            return (double) slider.getValue() / FACTOR;
        }
    }

    //----------------//
    // ForegroundPane //
    //----------------//
    /**
     * Pane to define the upper limit for foreground pixels
     */
    private class ForegroundPane
        extends DefaultPane
        implements ChangeListener
    {
        //~ Instance fields ----------------------------------------------------

        /** Slider for max foreground value */
        final JSlider pixSlider;

        /** Numeric value kept in sync */
        final LIntegerField pixValue = new LIntegerField(
            false,
            "Level",
            "Max level for foreground pixels");

        //~ Constructors -------------------------------------------------------

        public ForegroundPane ()
        {
            super("Foreground");

            int initValue = (score != null)
                            ? score.getSheet()
                                   .getPicture()
                                   .getMaxForeground()
                            : Picture.getDefaultMaxForeground();

            pixSlider = new JSlider(JSlider.HORIZONTAL, 0, 255, initValue);
            pixSlider.setMajorTickSpacing(50);
            pixSlider.setMinorTickSpacing(10);
            pixSlider.setPaintTicks(true);

            pixValue.setValue(initValue);

            // If no current score, push for a global setting
            defaultBox.setSelected(score == null);

            // Kept in sync
            pixSlider.addChangeListener(this);
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public int getLogicalRowCount ()
        {
            return 3;
        }

        @Override
        public boolean isValid ()
        {
            task.setForeground(pixSlider.getValue());

            return true;
        }

        @Override
        public void commit ()
        {
            if (defaultBox.isSelected()) {
                Picture.setDefaultMaxForeground(pixSlider.getValue());
            }
        }

        @Override
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
        {
            r = super.defineLayout(builder, cst, r);
            builder.add(pixValue.getLabel(), cst.xy(9, r));
            builder.add(pixValue.getField(), cst.xy(11, r));

            r += 2;
            builder.add(pixSlider, cst.xyw(3, r, 9));

            return r + 2;
        }

        public void stateChanged (ChangeEvent e)
        {
            pixValue.setValue(pixSlider.getValue());

            if (!pixSlider.getValueIsAdjusting()) {
                //logger.info("New maxForeground: " + histoSlider.getValue());
            }
        }
    }

    //-----------//
    // HistoPane //
    //-----------//
    /**
     * Pane to define the histogram threshold used for staff retrieval
     */
    private class HistoPane
        extends DoublePane
        implements ChangeListener
    {
        //~ Constructors -------------------------------------------------------

        public HistoPane ()
        {
            super(
                "Staff Lines",
                "Ratio",
                "Ratio of horizontal histogram for staff lines",
                0,
                100,
                (score != null) ? score.getSheet().getHistoRatio()
                                : Sheet.getDefaultHistoRatio());
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public boolean isValid ()
        {
            task.setHistoRatio(dblValue());

            return true;
        }

        @Override
        public void commit ()
        {
            if (defaultBox.isSelected()) {
                Sheet.setDefaultHistoRatio(dblValue());
            }
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
        public boolean isValid ()
        {
            String item = (String) langCombo.getItemAt(
                langCombo.getSelectedIndex());
            task.setLanguage(codeOf(item));

            return true;
        }

        @Override
        public void commit ()
        {
            if (defaultBox.isSelected()) {
                // Make the selected language the global default
                String item = (String) langCombo.getItemAt(
                    langCombo.getSelectedIndex());
                Language.setDefaultLanguage(codeOf(item));
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

            for (String code : Language.getSupportedLanguages()) {
                items.add(itemOf(code));
            }

            JComboBox combo = new JComboBox(items.toArray(new String[0]));
            combo.setToolTipText("Dominant language for textual items");

            final String code = ((score != null) &&
                                (score.getLanguage() != null))
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
        final LIntegerField firstId = new LIntegerField(
            "first Id",
            "First measure id of measure range");

        /** Last measure Id */
        final LIntegerField lastId = new LIntegerField(
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
            MeasureRange range = score.getMeasureRange();

            if (range != null) {
                rangeBox.setSelected(true);
                firstId.setEnabled(true);
                firstId.setValue(range.getFirstId());
                lastId.setEnabled(true);
                lastId.setValue(range.getLastId());
            } else {
                rangeBox.setSelected(false);
                firstId.setEnabled(false);
                lastId.setEnabled(false);

                try {
                    firstId.setValue(
                        score.getFirstSystem().getFirstPart().getFirstMeasure().getId());
                    lastId.setValue(
                        score.getLastSystem().getLastPart().getLastMeasure().getId());
                } catch (Exception ex) {
                    logger.warning("Error on score measure range", ex);
                    rangeBox.setEnabled(false); // Safer
                }
            }
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public boolean isValid ()
        {
            // Measure range
            if (rangeBox.isSelected() && (score.getLastSystem() != null)) {
                // First Measure
                int maxMeasureId = score.getLastSystem()
                                        .getLastPart()
                                        .getLastMeasure()
                                        .getId();

                if ((firstId.getValue() < 1) ||
                    (firstId.getValue() > maxMeasureId)) {
                    logger.warning(
                        "First measure Id is not within [1.." + maxMeasureId +
                        "]: " + firstId.getValue());

                    return false;
                }

                // Last Measure
                if ((lastId.getValue() < 1) ||
                    (lastId.getValue() > maxMeasureId)) {
                    logger.warning(
                        "Last measure Id is not within [1.." + maxMeasureId +
                        "]: " + lastId.getValue());

                    return false;
                }

                // First & last consistency
                if (firstId.getValue() > lastId.getValue()) {
                    logger.warning(
                        "First measure Id is greater than last measure Id");

                    return false;
                }
            }

            return true;
        }

        @Override
        public void commit ()
        {
            /** Since this info is not registered in the ParametersTask */
            if (rangeBox.isSelected()) {
                score.setMeasureRange(
                    new MeasureRange(
                        score,
                        firstId.getValue(),
                        lastId.getValue()));
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
    // MidiPane //
    //----------//
    private class MidiPane
        extends Pane
    {
        //~ Instance fields ----------------------------------------------------

        /** Tempo */
        final LIntegerField tempo = new LIntegerField(
            "Tempo",
            "Tempo value in number of quarters per minute");

        /** Volume */
        final LIntegerField volume = new LIntegerField("Volume", "Volume");

        //~ Constructors -------------------------------------------------------

        public MidiPane ()
        {
            super("Midi");

            tempo.setValue(
                (score.getTempo() != null) ? score.getTempo()
                                : score.getDefaultTempo());
            volume.setValue(
                (score.getVolume() != null) ? score.getVolume()
                                : score.getDefaultVolume());
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public boolean isValid ()
        {
            if ((tempo.getValue() < 0) || (tempo.getValue() > 1000)) {
                logger.warning("Tempo value should be in 0..1000 range");

                return false;
            }

            if ((volume.getValue() < 0) || (volume.getValue() > 127)) {
                logger.warning("Volume value should be in 0..127 range");

                return false;
            }

            task.setTempo(tempo.getValue());
            task.setVolume(volume.getValue());

            return true;
        }

        @Override
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
        {
            builder.add(tempo.getLabel(), cst.xy(5, r));
            builder.add(tempo.getField(), cst.xy(7, r));

            builder.add(volume.getLabel(), cst.xy(9, r));
            builder.add(volume.getField(), cst.xy(11, r));

            return r + 2;
        }
    }

    //----------//
    // PartPane //
    //----------//
    private class PartPane
        extends Panel
    {
        //~ Static fields/initializers -----------------------------------------

        public static final int logicalRowCount = 3;

        //~ Instance fields ----------------------------------------------------

        /** The underlying model  */
        private final ScorePart scorePart;

        /** Id of the part */
        private final LField id = new LField(
            false,
            "Id",
            "Id of the score part");

        /** Name of the part */
        private LField name = new LField("Name", "Name for the score part");

        /** Midi Instrument */
        private JComboBox midiBox = new JComboBox(
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
    }

    //------------//
    // ScriptPane //
    //------------//
    /**
     * Should we prompt the user for saving the script when sheet is closed?
     */
    private class ScriptPane
        extends Pane
    {
        //~ Instance fields ----------------------------------------------------

        /** CheckBox for boolean */
        private final JCheckBox promptBox = new JCheckBox();

        //~ Constructors -------------------------------------------------------

        public ScriptPane ()
        {
            super("Script");

            promptBox.setText("Prompt for save");
            promptBox.setToolTipText(
                "Should we prompt for saving the script on score closing");
            promptBox.setSelected(ScriptActions.isConfirmOnClose());
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void commit ()
        {
            ScriptActions.setConfirmOnClose(promptBox.isSelected());
        }

        @Override
        public int defineLayout (PanelBuilder    builder,
                                 CellConstraints cst,
                                 int             r)
        {
            builder.add(promptBox, cst.xyw(3, r, 3));

            return r + 2;
        }
    }

    //----------//
    // SlotPane //
    //----------//
    /**
     * Pane to define the abscissa margin around a common time slot
     */
    private class SlotPane
        extends DoublePane
        implements ChangeListener
    {
        //~ Constructors -------------------------------------------------------

        public SlotPane ()
        {
            super(
                "Time Slots",
                "Margin",
                "Horizontal margin around Slots, in interline fractions",
                0,
                200,
                ((score != null) && (score.getSlotMargin() != null))
                                ? score.getSlotMargin().doubleValue()
                                : Score.getDefaultSlotMargin().doubleValue());
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public boolean isValid ()
        {
            task.setSlotMargin(dblValue());

            return true;
        }

        @Override
        public void commit ()
        {
            if (defaultBox.isSelected()) {
                Score.setDefaultSlotMargin(dblValue());
            }
        }
    }
}
