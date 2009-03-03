//----------------------------------------------------------------------------//
//                                                                            //
//                            S c o r e B o a r d                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.ui;

import omr.glyph.text.TesseractOCR;

import omr.log.Logger;

import omr.score.MeasureRange;
import omr.score.Score;
import omr.score.entity.ScorePart;
import omr.score.midi.MidiAbstractions;

import omr.selection.UserEvent;

import omr.ui.*;
import omr.ui.field.LField;
import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.event.*;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;

/**
 * Class <code>ScoreBoard</code> is a board that manages score information as
 * both a display and possible input from user.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ScoreBoard
    extends Board
    implements ItemListener
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreBoard.class);

    //~ Instance fields --------------------------------------------------------

    /** The related score */
    private final Score score;

    /** Map of text languages */
    private final Map<String, String> langMap = TesseractOCR.getInstance()
                                                            .getSupportedLanguages();

    /** Array of language codes */
    String[] langCodes = langMap.keySet()
                                .toArray(new String[0]);

    /** ComboBox for text language */
    private JComboBox langCombo;

    /** Tempo */
    private final LIntegerField tempo = new LIntegerField(
        "Tempo",
        "Tempo value in number of quarters per minute");

    /** Velocity */
    private final LIntegerField velocity = new LIntegerField(
        "Velocity",
        "Volume");

    /** Range selection */
    private final JCheckBox rangeBox = new JCheckBox();

    /** First measure Id */
    private final LIntegerField firstId = new LIntegerField(
        "first Id",
        "First measure id of measure range");

    /** Last measure Id */
    private final LIntegerField lastId = new LIntegerField(
        "last Id",
        "Last measure id of measure range");

    /** Map of score part panes */
    private final Map<ScorePart, PartPane> panes = new HashMap<ScorePart, PartPane>();

    //~ Constructors -----------------------------------------------------------

    //------------//
    // ScoreBoard //
    //------------//
    /**
     * Create a ScoreBoard
     *
     * @param unitName name of the unit which declares a score board
     * @param score the related score
     */
    public ScoreBoard (String      unitName,
                       final Score score)
    {
        super(unitName + "-ScoreBoard", null, null);
        this.score = score;

        // ComboBox for text language
        langCombo = new JComboBox(langMap.values().toArray());
        langCombo.addActionListener(
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        score.setLanguage(
                            langCodes[langCombo.getSelectedIndex()]);
                    }
                });
        langCombo.setToolTipText("Dominant language for textual items");

        if (score.getLanguage() != null) {
            langCombo.setSelectedItem(langMap.get(score.getLanguage()));
        }

        // rangeBox
        rangeBox.setText("Select");
        rangeBox.setToolTipText("Check to enable measure selection");
        rangeBox.addItemListener(this);

        // Layout
        defineLayout();

        // Initial setting for tempo
        tempo.setValue(
            (score.getTempo() != null) ? score.getTempo()
                        : score.getDefaultTempo());

        // Initial setting for velocity
        velocity.setValue(
            (score.getVelocity() != null) ? score.getVelocity()
                        : score.getDefaultVelocity());

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
            firstId.setValue(
                score.getFirstSystem().getFirstPart().getFirstMeasure().getId());
            lastId.setEnabled(false);
            lastId.setValue(
                score.getLastSystem().getLastPart().getLastMeasure().getId());
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // commit //
    //--------//
    /**
     * Checks the values and commitPart them if they are OK
     *
     * @return true if committed, false otherwise
     */
    public boolean commit ()
    {
        if (checkData()) {
            // Each score part
            for (ScorePart scorePart : score.getPartList()) {
                panes.get(scorePart)
                     .commitPart();
            }

            // Score global data
            score.setTempo(tempo.getValue());
            score.setVelocity(velocity.getValue());

            // Measure range
            if (rangeBox.isSelected()) {
                score.setMeasureRange(
                    new MeasureRange(
                        score,
                        firstId.getValue(),
                        lastId.getValue()));
            } else {
                score.setMeasureRange(null);
            }

            return true;
        } else {
            return false;
        }
    }

    //------------------//
    // itemStateChanged //
    //------------------//
    public void itemStateChanged (ItemEvent e)
    {
        if (e.getItemSelectable() == rangeBox) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                firstId.setEnabled(true);
                lastId.setEnabled(true);
            } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                firstId.setEnabled(false);
                lastId.setEnabled(false);
            }
        }
    }

    //---------//
    // onEvent //
    //---------//
    public void onEvent (UserEvent event)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    //---------------//
    // getGlobalPane //
    //---------------//
    private Panel getGlobalPane ()
    {
        Panel        panel = new Panel();
        FormLayout   layout = Panel.makeFormLayout(2, 3);
        PanelBuilder builder = new PanelBuilder(layout, panel);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();
        int             r = 1;
        builder.addSeparator("Global Data");
        r += 2;
        builder.add(new JLabel("Lang"), cst.xy(1, r));
        builder.add(langCombo, cst.xy(3, r));

        builder.add(tempo.getLabel(), cst.xy(5, r));
        builder.add(tempo.getField(), cst.xy(7, r));

        builder.add(velocity.getLabel(), cst.xy(9, r));
        builder.add(velocity.getField(), cst.xy(11, r));

        return panel;
    }

    //--------------//
    // getRangePane //
    //--------------//
    private Panel getRangePane ()
    {
        Panel        panel = new Panel();
        FormLayout   layout = Panel.makeFormLayout(2, 3);
        PanelBuilder builder = new PanelBuilder(layout, panel);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();
        int             r = 1;
        builder.addSeparator("Measure range");
        r += 2;
        builder.add(rangeBox, cst.xy(3, r));
        builder.add(firstId.getLabel(), cst.xy(5, r));
        builder.add(firstId.getField(), cst.xy(7, r));
        builder.add(lastId.getLabel(), cst.xy(9, r));
        builder.add(lastId.getField(), cst.xy(11, r));

        return panel;
    }

    //-----------//
    // checkData //
    //-----------//
    private boolean checkData ()
    {
        // Tempo
        if ((tempo.getValue() < 0) || (tempo.getValue() > 1000)) {
            logger.warning("Illegal tempo value");

            return false;
        }

        // Velocity
        if ((velocity.getValue() < 0) || (velocity.getValue() > 127)) {
            logger.warning("Illegal velocity value");

            return false;
        }

        // First Measure
        int maxMeasureId = score.getLastSystem()
                                .getLastPart()
                                .getLastMeasure()
                                .getId();

        if ((firstId.getValue() < 1) || (firstId.getValue() > maxMeasureId)) {
            logger.warning(
                "First measure Id is not within [1.." + maxMeasureId + "]");

            return false;
        }

        // Last Measure
        if ((lastId.getValue() < 1) || (lastId.getValue() > maxMeasureId)) {
            logger.warning(
                "Last measure Id is not within [1.." + maxMeasureId + "]");

            return false;
        }

        // First & last consistency
        if (firstId.getValue() > lastId.getValue()) {
            logger.warning("First measure Id is greater than last measure Id");

            return false;
        }

        // Each score part
        for (ScorePart scorePart : score.getPartList()) {
            if (!panes.get(scorePart)
                      .checkPart()) {
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
        // Prepare constraints for rows
        StringBuilder sb = new StringBuilder();
        sb.append("pref");

        for (int i = 0; i < score.getPartList()
                                 .size(); i++) {
            sb.append(",1dlu,pref,1dlu,pref");
        }

        FormLayout   layout = new FormLayout("pref", sb.toString());
        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();
        int             r = 1;
        builder.add(getGlobalPane(), cst.xy(1, r));

        for (ScorePart scorePart : score.getPartList()) {
            r += 2;

            PartPane partPane = new PartPane(scorePart);
            panes.put(scorePart, partPane);
            builder.add(partPane, cst.xy(1, r));
        }

        r += 2;
        builder.add(getRangePane(), cst.xy(1, r));
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------//
    // PartPane //
    //----------//
    private class PartPane
        extends Panel
    {
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

            defineLayout();

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
            }

            return true;
        }

        //------------//
        // commitPart //
        //------------//
        public void commitPart ()
        {
            // Part name
            scorePart.setName(name.getText());

            // Part midi program
            scorePart.setMidiProgram(midiBox.getSelectedIndex() + 1);

            // Replicate the score tempo
            scorePart.setTempo(tempo.getValue());
        }

        //--------------//
        // defineLayout //
        //--------------//
        private void defineLayout ()
        {
            FormLayout   layout = Panel.makeFormLayout(3, 3);

            PanelBuilder builder = new PanelBuilder(layout, this);
            builder.setDefaultDialogBorder();

            CellConstraints cst = new CellConstraints();

            int             r = 1; // --
            builder.addSeparator("Part #" + scorePart.getId());

            r += 2; // --

            builder.add(id.getLabel(), cst.xy(5, r));
            builder.add(id.getField(), cst.xy(7, r));

            builder.add(name.getLabel(), cst.xy(9, r));
            builder.add(name.getField(), cst.xy(11, r));

            r += 2; // --

            builder.add(new JLabel("Midi"), cst.xy(5, r));
            builder.add(midiBox, cst.xyw(7, r, 5));
        }
    }
}
