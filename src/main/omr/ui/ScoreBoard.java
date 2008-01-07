//----------------------------------------------------------------------------//
//                                                                            //
//                            S c o r e B o a r d                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.ui;

import omr.score.MeasureRange;
import omr.score.Score;
import omr.score.entity.ScorePart;
import omr.score.midi.MidiAbstractions;

import omr.ui.field.LField;
import omr.ui.field.LIntegerField;
import omr.ui.util.Panel;

import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
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
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreBoard.class);

    //~ Instance fields --------------------------------------------------------

    /** The related score */
    private final Score score;

    /** Tempo */
    private final LIntegerField tempo = new LIntegerField(
        "Tempo",
        "Tempo value in number of quarters per minute");

    /** Velocity */
    private final LIntegerField velocity = new LIntegerField(
        "Velocity",
        "Volume");

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
     */
    public ScoreBoard (String unitName,
                       Score  score)
    {
        super(Board.Tag.SCORE, unitName + "-ScoreBoard");
        this.score = score;

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
            firstId.setValue(range.getFirstId());
            lastId.setValue(range.getLastId());
        } else {
            firstId.setValue(
                score.getFirstSystem().getFirstPart().getFirstMeasure().getId());
            lastId.setValue(
                score.getLastSystem().getLastPart().getLastMeasure().getId());
        }
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // commitPart //
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
            score.setMeasureRange(
                new MeasureRange(score, firstId.getValue(), lastId.getValue()));

            return true;
        } else {
            return false;
        }
    }

    //---------------//
    // getGlobalPane //
    //---------------//
    private Panel getGlobalPane ()
    {
        Panel        panel = new Panel();
        FormLayout   layout = Panel.makeFormLayout(2, 2);
        PanelBuilder builder = new PanelBuilder(layout, panel);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();
        int             r = 1;
        builder.addSeparator("Global Data");
        r += 2;
        builder.add(tempo.getLabel(), cst.xy(1, r));
        builder.add(tempo.getField(), cst.xy(3, r));
        builder.add(velocity.getLabel(), cst.xy(5, r));
        builder.add(velocity.getField(), cst.xy(7, r));

        return panel;
    }

    //--------------//
    // getRangePane //
    //--------------//
    private Panel getRangePane ()
    {
        Panel        panel = new Panel();
        FormLayout   layout = Panel.makeFormLayout(2, 2);
        PanelBuilder builder = new PanelBuilder(layout, panel);
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();
        int             r = 1;
        builder.addSeparator("Measure range");
        r += 2;
        builder.add(firstId.getLabel(), cst.xy(1, r));
        builder.add(firstId.getField(), cst.xy(3, r));
        builder.add(lastId.getLabel(), cst.xy(5, r));
        builder.add(lastId.getField(), cst.xy(7, r));

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
        if ((firstId.getValue() < 1) ||
            (firstId.getValue() > score.getLastSystem()
                                       .getLastPart()
                                       .getLastMeasure()
                                       .getId())) {
            logger.warning("Illegal first measure Id");

            return false;
        }

        // Last Measure
        if ((lastId.getValue() < 0) ||
            (lastId.getValue() > score.getLastSystem()
                                      .getLastPart()
                                      .getLastMeasure()
                                      .getId())) {
            logger.warning("Illegal last measure Id");

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
                    .isEmpty()) {
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
            FormLayout   layout = Panel.makeFormLayout(3, 2);

            PanelBuilder builder = new PanelBuilder(layout, this);
            builder.setDefaultDialogBorder();

            CellConstraints cst = new CellConstraints();

            int             r = 1; // --
            builder.addSeparator("Part #" + scorePart.getId());

            r += 2; // --

            builder.add(id.getLabel(), cst.xy(1, r));
            builder.add(id.getField(), cst.xy(3, r));

            builder.add(name.getLabel(), cst.xy(5, r));
            builder.add(name.getField(), cst.xy(7, r));

            r += 2; // --

            builder.add(new JLabel("Midi"), cst.xy(1, r));
            builder.add(midiBox, cst.xyw(3, r, 5));
        }
    }
}
