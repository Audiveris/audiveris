//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       I n t e r B o a r d                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.LyricItemInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.EntityBoard;
import org.audiveris.omr.ui.PixelCount;
import org.audiveris.omr.ui.field.LCheckBox;
import org.audiveris.omr.ui.field.LComboBox;
import org.audiveris.omr.ui.field.LIntegerField;
import org.audiveris.omr.ui.field.LLabel;
import org.audiveris.omr.ui.field.LTextField;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.util.Panel;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

/**
 * Class {@code InterBoard} defines a UI board for {@link Inter} information.
 *
 * @author Hervé Bitteur
 */
public class InterBoard
        extends EntityBoard<Inter>
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(InterBoard.class);

    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(InterBoard.class);

    /** Related sheet. */
    private final Sheet sheet;

    /** Output : shape icon. */
    private final JLabel shapeIcon = new JLabel();

    /** Output : grade (intrinsic/contextual). */
    private final LTextField grade = new LTextField(false,
                                                    resources.getString("grade.text"),
                                                    resources.getString("grade.toolTipText"));

    /** Output : implicit / manual. */
    private final JLabel specific = new JLabel("");

    /** Output : shape. */
    private final LLabel shapeName = new LLabel("", resources.getString("shapeName.toolTipText"));

    /** Output : lyric verse. */
    private final LIntegerField verse = new LIntegerField(false,
                                                          resources.getString("verse.text"),
                                                          resources.getString("verse.toolTipText"));

    /** Output : voice. */
    private final LIntegerField voice = new LIntegerField(false,
                                                          resources.getString("voice.text"),
                                                          resources.getString("voice.toolTipText"));

    /** Output : lyrics above or below related note line. */
    private final JLabel aboveBelow = new JLabel();

    /** To delete/de-assign. */
    private final DeassignAction deassignAction = new DeassignAction();

    /** To select ensemble. */
    private final ToEnsembleAction toEnsAction = new ToEnsembleAction();

    /** To set into Edit mode. */
    private final LCheckBox edit = new LCheckBox(resources.getString("edit.text"),
                                                 resources.getString("edit.toolTipText"));

    //    /** Numerator of time signature */
    //    private final LIntegerField timeNum;
    //
    //    /** Denominator of time signature */
    //    private final LIntegerField timeDen;
    //
    /** ComboBox for text role. */
    private final LComboBox<TextRole> roleCombo = new LComboBox<>(
            resources.getString("roleCombo.text"),
            resources.getString("roleCombo.toolTipText"),
            TextRole.valuesSansLyrics());

    /** Input/Output : textual content. */
    private final LTextField textField = new LTextField(
            true,
            resources.getString("textField.text"),
            resources.getString("textField.toolTipText"));

    /** Handling of entered / selected values. */
    private final Action paramAction;

    /** To avoid unwanted events. */
    private boolean selfUpdatingText;

    /**
     * Creates a new InterBoard object, pre-selected by default.
     *
     * @param sheet the related sheet
     */
    public InterBoard (Sheet sheet)
    {
        this(sheet, true);
    }

    /**
     * Creates a new InterBoard object.
     *
     * @param sheet    the related sheet
     * @param selected true for pre-selected, false for collapsed
     */
    public InterBoard (Sheet sheet,
                       boolean selected)
    {
        super(Board.INTER, sheet.getInterIndex().getEntityService(), true);
        this.sheet = sheet;

        edit.addActionListener(this);

        paramAction = new ParamAction();

        defineLayout();

        aboveBelow.setToolTipText(resources.getString("aboveBelow.toolTipText"));

        // Initial status
        grade.setEnabled(false);
        specific.setEnabled(false);
        edit.setEnabled(false);

        voice.setVisible(false);
        verse.setVisible(false);
        aboveBelow.setVisible(false);
    }

    //-----------------//
    // actionPerformed //
    //-----------------//
    /**
     * Triggered by Edit check box (and by VIP check box and by dump button)
     *
     * @param e the event that triggered this action
     */
    @Override
    public void actionPerformed (ActionEvent e)
    {
        if (edit.getField() == e.getSource()) {
            final Inter inter = InterBoard.this.getSelectedEntity();

            if (edit.getField().isSelected()) {
                sheet.getSymbolsEditor().openEditMode(inter);
            } else {
                sheet.getSymbolsEditor().closeEditMode();
            }
        } else {
            super.actionPerformed(e); // VIP or DUMP
        }
    }

    //---------------------//
    // dumpActionPerformed //
    //---------------------//
    @Override
    protected void dumpActionPerformed (ActionEvent e)
    {
        final Inter inter = getSelectedEntity();

        // Compute contextual grade
        if ((inter.getSig() != null) && !inter.isRemoved()) {
            inter.getSig().computeContextualGrade(inter);
        }

        super.dumpActionPerformed(e);
    }

    //---------------//
    // getFormLayout //
    //---------------//
    @Override
    protected FormLayout getFormLayout ()
    {
        return Panel.makeFormLayout(5, 3);
    }

    //-----------------------//
    // handleEntityListEvent //
    //-----------------------//
    /**
     * Interest in InterList for shape, icon, text, role, etc... fields
     *
     * @param interListEvent the inter list event
     */
    @Override
    protected void handleEntityListEvent (EntityListEvent<Inter> interListEvent)
    {
        super.handleEntityListEvent(interListEvent);

        final Inter inter = interListEvent.getEntity();

        // Shape text and icon
        Shape shape = (inter != null) ? inter.getShape() : null;

        if (shape != null) {
            shapeName.setText(shape.toString());
            shapeIcon.setIcon(shape.getDecoratedSymbol());
        } else {
            shapeName.setText((inter != null) ? inter.shapeString() : "");
            shapeIcon.setIcon(null);
        }

        // Inter characteristics
        textField.setVisible(false);
        textField.setEnabled(false);
        roleCombo.setVisible(false);
        verse.setVisible(false);
        aboveBelow.setVisible(false);
        voice.setVisible(false);

        if (inter != null) {
            Double cp = inter.getContextualGrade();

            if (cp != null) {
                grade.setText(String.format("%.2f/%.2f", inter.getGrade(), cp));
            } else {
                grade.setText(String.format("%.2f", inter.getGrade()));
            }

            specific.setText(inter.isImplicit() ? "IMPLICIT" : (inter.isManual() ? "MANUAL" : ""));

            deassignAction.putValue(Action.NAME, inter.isRemoved()
                                    ? resources.getString("deassign.Action.deleted")
                                    : resources.getString("deassign.Action.text"));

            if (inter instanceof WordInter) {
                selfUpdatingText = true;

                WordInter word = (WordInter) inter;
                textField.setText(word.getValue());
                textField.setEnabled(true);
                textField.setVisible(true);
                selfUpdatingText = false;
            } else if (inter instanceof SentenceInter) {
                selfUpdatingText = true;

                SentenceInter sentence = (SentenceInter) inter;
                textField.setText(sentence.getValue());
                textField.setVisible(true);

                if (inter instanceof LyricLineInter) {
                    LyricLineInter lyric = (LyricLineInter) inter;
                    verse.setVisible(true);
                    verse.setValue(lyric.getNumber());

                    boolean isAbove = lyric.getStaff().pitchPositionOf(inter.getCenter()) < 0;
                    aboveBelow.setText(resources.getString(isAbove ? "above" : "below"));
                    aboveBelow.setVisible(true);

                    LyricItemInter firstNormalItem = lyric.getFirstNormalItem();
                    if (firstNormalItem != null) {
                        HeadChordInter firstChord = firstNormalItem.getHeadChord();
                        Voice theVoice = firstChord.getVoice();

                        if (theVoice != null) {
                            voice.setVisible(true);
                            voice.setValue(theVoice.getId());
                        }

                    }
                } else {
                    roleCombo.setSelectedItem(sentence.getRole());
                    roleCombo.setVisible(true);
                    roleCombo.setEnabled(!(sentence instanceof LyricLineInter));
                }
                selfUpdatingText = false;
            } else {
                // edit?
                Inter editedInter = sheet.getSymbolsEditor().getEditedInter();
                edit.getField().setSelected(inter == editedInter);
            }
        } else {
            grade.setText("");
            specific.setText("");
            deassignAction.putValue(Action.NAME, " ");
            edit.getField().setSelected(false);
        }

        deassignAction.setEnabled((inter != null) && !inter.isRemoved());
        grade.setEnabled(inter != null);
        shapeName.setEnabled(inter != null);
        edit.setEnabled((inter != null) && !inter.isRemoved() && inter.isEditable());
        toEnsAction.setEnabled((inter != null) && !inter.isRemoved() && (inter.getSig() != null)
                                       && (inter.getEnsemble() != null));
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout for InterBoard specific fields.
     */
    private void defineLayout ()
    {
        final CellConstraints cst = new CellConstraints();

        // Layout
        int r = 1; // -----------------------------

        // Shape Icon (start, spans several rows) + grade + Deassign button
        builder.add(shapeIcon, cst.xywh(1, r, 1, 7, CellConstraints.CENTER, CellConstraints.CENTER));

        // Grade
        builder.add(grade.getLabel(), cst.xy(5, r));
        builder.add(grade.getField(), cst.xy(7, r));

        // Deassign
        JButton deassignButton = new JButton(deassignAction);
        deassignButton.setHorizontalTextPosition(SwingConstants.LEFT);
        deassignButton.setHorizontalAlignment(SwingConstants.RIGHT);
        deassignAction.setEnabled(false);
        builder.add(deassignButton, cst.xyw(9, r, 3));

        r += 2; // --------------------------------

        // Edit
        JPanel editPane = new JPanel(new BorderLayout());
        edit.getLabel().setHorizontalAlignment(SwingConstants.CENTER);
        editPane.add(edit.getLabel(), BorderLayout.CENTER);
        editPane.add(edit.getField(), BorderLayout.EAST);
        builder.add(editPane, cst.xyw(3, r, 1));

        // Specific (MANUAL or IMPLICIT) if any
        builder.add(specific, cst.xyw(5, r, 4));

        // To ensemble
        JButton toEnsButton = new JButton(toEnsAction);
        toEnsButton.setHorizontalTextPosition(SwingConstants.LEFT);
        toEnsButton.setHorizontalAlignment(SwingConstants.RIGHT);
        toEnsAction.setEnabled(false);
        builder.add(toEnsButton, cst.xyw(9, r, 3));

        r += 2; // --------------------------------

        // Role
        roleCombo.getField().setMaximumRowCount(TextRole.valuesSansLyrics().length);
        roleCombo.addActionListener(paramAction);
        roleCombo.setVisible(false);
        builder.add(roleCombo.getField(), cst.xyw(3, r, 4));

        // Shape name
        builder.add(shapeName.getField(), cst.xyw(7, r, 5));

        r += 2; // --------------------------------

        // Text field
        textField.getField().setHorizontalAlignment(JTextField.LEFT);
        textField.setVisible(false);
        builder.add(textField.getField(), cst.xyw(3, r, 9));

        r += 2; // --------------------------------

        // Voice, Verse, Above
        builder.add(voice.getLabel(), cst.xyw(1, r, 1));
        builder.add(voice.getField(), cst.xyw(3, r, 1));

        builder.add(verse.getLabel(), cst.xyw(5, r, 1));
        builder.add(verse.getField(), cst.xyw(7, r, 1));

        builder.add(aboveBelow, cst.xyw(9, r, 3));

        // Needed to process user input when RETURN/ENTER is pressed
        getComponent().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke("ENTER"),
                "TextAction");
        getComponent().getActionMap().put("TextAction", paramAction);
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final PixelCount shapeIconHeight = new PixelCount(
                70,
                "Exact pixel height for the shape icon field");

        private final PixelCount shapeIconWidth = new PixelCount(
                50,
                "Exact pixel width for the shape icon field");
    }

    //----------------//
    // DeassignAction //
    //----------------//
    private class DeassignAction
            extends AbstractAction
    {

        public DeassignAction ()
        {
            super(resources.getString("deassign.Action.text"));
            this.putValue(Action.SHORT_DESCRIPTION,
                          resources.getString("deassign.Action.shortDescription"));
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Delete the inter
            final Inter inter = InterBoard.this.getSelectedEntity();
            logger.debug("Deleting {}", inter);

            sheet.getInterController().removeInters(Arrays.asList(inter));
        }
    }

    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
            extends AbstractAction
    {

        /**
         * Method run whenever user presses Return/Enter in one of the parameter fields
         *
         * @param e unused?
         */
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Discard irrelevant action events
            if (selfUpdatingText) {
                return;
            }

            // Current inter
            final Inter inter = getSelectedEntity();

            if (inter != null) {
                // Word or Sentence
                if (inter instanceof WordInter) {
                    WordInter word = (WordInter) inter;

                    // Change text value?
                    final String newValue = textField.getText().trim();

                    if (!newValue.equals(word.getValue())) {
                        logger.debug("Word=\"{}\"", newValue);
                        sheet.getInterController().changeWord(word, newValue);
                    }
                } else if (inter instanceof SentenceInter) {
                    SentenceInter sentence = (SentenceInter) inter;

                    // Change sentence role?
                    final TextRole newRole = roleCombo.getSelectedItem();

                    if (newRole != sentence.getRole()) {
                        logger.debug("Sentence=\"{}\" Role={}", textField.getText().trim(), newRole);
                        sheet.getInterController().changeSentence(sentence, newRole);
                    }
                }
            }
        }
    }

    //------------------//
    // ToEnsembleAction //
    //------------------//
    private class ToEnsembleAction
            extends AbstractAction
    {

        public ToEnsembleAction ()
        {
            super(resources.getString("ToEnsembleAction.text"));
            putValue(Action.SHORT_DESCRIPTION,
                     resources.getString("ToEnsembleAction.shortDescription"));
        }

        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Select the containing ensemble of current inter
            final Inter inter = InterBoard.this.getSelectedEntity();
            inter.getSig().publish(inter.getEnsemble(), SelectionHint.ENTITY_INIT);
        }
    }
}
