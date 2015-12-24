//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S y m b o l G l y p h B o a r d                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Glyph;

import omr.selection.EntityListEvent;
import omr.selection.MouseMovement;
import omr.selection.UserEvent;

import omr.ui.field.LDoubleField;

import com.jgoodies.forms.layout.CellConstraints;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code SymbolGlyphBoard} defines an extended glyph board, with characteristics
 * (pitch position, stem number, etc) that are specific to a symbol, and an additional
 * symbol glyph spinner.
 * <ul>
 * <li>A <b>symbolSpinner</b> to browse through all glyphs that are considered as symbols, that is
 * built from aggregation of contiguous sections, or by combination of other symbols.
 * Glyphs whose shape is set to {@link omr.glyph.Shape#NOISE}, that is too small glyphs, are not
 * included in this spinner.</ul>
 *
 * <p>
 * <img alt="Image of SymbolGlyphBoard" src="doc-files/SymbolGlyphBoard.png">
 *
 * @author Hervé Bitteur
 */
public class SymbolGlyphBoard
        extends GlyphBoard
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolGlyphBoard.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //    /** Numerator of time signature */
    //    private final LIntegerField timeNum;
    //
    //    /** Denominator of time signature */
    //    private final LIntegerField timeDen;
    //
    //    /** ComboBox for text role */
    //    private final LComboBox<TextRole> roleCombo;
    //
    //    /** Output : textual confidence */
    //    protected LDoubleField confField;
    //
    //    /** Input/Output : textual content */
    //    protected LTextField textField;
    //
    /** Glyph characteristics : position wrt staff. */
    private final LDoubleField pitchPosition = new LDoubleField(
            false,
            "Pitch",
            "Logical pitch position",
            "%.3f");

    /** Glyph characteristics : normalized weight. */
    private final LDoubleField weight = new LDoubleField(
            false,
            "Weight",
            "Normalized weight",
            "%.3f");

    /** Glyph characteristics : normalized width. */
    private final LDoubleField width = new LDoubleField(false, "Width", "Normalized width", "%.3f");

    /** Glyph characteristics : normalized height. */
    private final LDoubleField height = new LDoubleField(
            false,
            "Height",
            "Normalized height",
            "%.3f");

    //~ Constructors -------------------------------------------------------------------------------
    //
    //    /** Handling of entered / selected values */
    //    private final Action paramAction;
    //
    //    /** To avoid unwanted events */
    //    private boolean selfUpdatingText;
    /**
     * Create the symbol glyph board.
     *
     * @param glyphsController the companion which handles glyph (de)assignments
     * @param useSpinners      true for use of spinners
     * @param expanded         true to expand this board
     */
    public SymbolGlyphBoard (GlyphsController glyphsController,
                             boolean useSpinners,
                             boolean expanded)
    {
        // For all glyphs
        super(glyphsController, useSpinners, true);

        // Initial status
        width.setEnabled(false);
        height.setEnabled(false);
        pitchPosition.setEnabled(false);
        weight.setEnabled(false);
        //
        //        // Additional combo for text role
        //        paramAction = new ParamAction();
        //        roleCombo = new LComboBox<TextRole>("Role", "Role of the Text", TextRole.values());
        //        roleCombo.getField().setMaximumRowCount(TextRole.values().length);
        //        roleCombo.addActionListener(paramAction);
        //
        //        // Confidence and Text fields
        //        confField = new LDoubleField(false, "Conf", "Confidence in text value", "%.2f");
        //        textField = new LTextField(true, "Text", "Content of a textual glyph");
        //        textField.getField().setHorizontalAlignment(JTextField.LEFT);
        //
        //        // Time signature
        //        timeNum = new LIntegerField("Num", "");
        //        timeDen = new LIntegerField("Den", "");
        //
        defineLayout();

        //
        //        // Needed to process user input when RETURN/ENTER is pressed
        //        getComponent().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
        //                KeyStroke.getKeyStroke("ENTER"),
        //                "TextAction");
        //        getComponent().getActionMap().put("TextAction", paramAction);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Glyph Selection has been modified.
     *
     * @param event the (Glyph or glyph set) Selection
     */
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            super.onEvent(event);

            if (event instanceof EntityListEvent) {
                handleEvent((EntityListEvent<Glyph>) event);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define a specific layout for this Symbol GlyphBoard
     */
    private void defineLayout ()
    {
        final CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------
        // Glyph ---

        r += 2; // --------------------------------
        // shape

        r += 2; // --------------------------------
        // Glyph characteristics, first line

        builder.add(width.getLabel(), cst.xy(5, r));
        builder.add(width.getField(), cst.xy(7, r));

        builder.add(pitchPosition.getLabel(), cst.xy(9, r));
        builder.add(pitchPosition.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        // Glyph characteristics, second line

        builder.add(height.getLabel(), cst.xy(5, r));
        builder.add(height.getField(), cst.xy(7, r));

        builder.add(weight.getLabel(), cst.xy(9, r));
        builder.add(weight.getField(), cst.xy(11, r));

        //
        //        r += 2; // --------------------------------
        //        // Text information, first line
        //
        //        if (textField != null) {
        //            builder.add(confField.getLabel(), cst.xyw(1, r, 1));
        //            builder.add(confField.getField(), cst.xyw(3, r, 1));
        //            confField.setVisible(false);
        //            builder.add(textField.getLabel(), cst.xyw(5, r, 1));
        //            builder.add(textField.getField(), cst.xyw(7, r, 5));
        //            textField.setVisible(false);
        //        }
        //
        //
        //        // or time signature parameters
        //        if (timeNum != null) {
        //            builder.add(timeNum.getLabel(), cst.xy(5, r));
        //            builder.add(timeNum.getField(), cst.xy(7, r));
        //            timeNum.setVisible(false);
        //
        //            builder.add(timeDen.getLabel(), cst.xy(9, r));
        //            builder.add(timeDen.getField(), cst.xy(11, r));
        //            timeDen.setVisible(false);
        //        }
        //
        //        r += 2; // --------------------------------
        //        // Text information, second line
        //
        //        if (roleCombo != null) {
        //            builder.add(roleCombo.getLabel(), cst.xyw(1, r, 1));
        //            builder.add(roleCombo.getField(), cst.xyw(3, r, 3));
        //            roleCombo.setVisible(false);
        //        }
    }

    //    //-------------//
    //    // ParamAction //
    //    //-------------//
    //    private class ParamAction
    //            extends AbstractAction
    //    {
    //        //~ Methods --------------------------------------------------------------------------------
    //
    //        // Method run whenever user presses Return/Enter in one of the parameter
    //        // fields
    //        @Override
    //        public void actionPerformed (ActionEvent e)
    //        {
    //            // Discard irrelevant action events
    //            if (selfUpdatingText) {
    //                return;
    //            }
    //
    //            // Get current glyph set
    //            GlyphSetEvent glyphsEvent = (GlyphSetEvent) getSelectionService()
    //                    .getLastEvent(GlyphSetEvent.class);
    //            Set<Glyph> glyphs = (glyphsEvent != null) ? glyphsEvent.getData() : null;
    //
    //            if ((glyphs != null) && !glyphs.isEmpty()) {
    //                // Read shape information
    //                String shapeName = shapeField.getText();
    //
    //                if (shapeName.isEmpty()) {
    //                    return;
    //                }
    //
    //                Shape shape = Shape.valueOf(shapeName);
    ////
    ////                // Text?
    ////                if (shape.isText()) {
    ////                    logger.debug(
    ////                            "Text=''{}'' Role={}",
    ////                            textField.getText().trim(),
    ////                            roleCombo.getSelectedItem());
    ////
    ////                    TextRole role = roleCombo.getSelectedItem();
    ////                    SheetsController.getCurrentSheet().getSymbolsController().asyncAssignTexts(
    ////                            glyphs,
    ////                            role,
    ////                            textField.getText());
    ////                } else if (shape == Shape.CUSTOM_TIME) {
    ////                    int num = timeNum.getValue();
    ////                    int den = timeDen.getValue();
    ////
    ////                    if ((num != 0) && (den != 0)) {
    ////                        SheetsController.getCurrentSheet().getSymbolsController().asyncAssignRationals(
    ////                                glyphs,
    ////                                new TimeRational(num, den));
    ////                    } else {
    ////                        logger.warn("Invalid time signature parameters");
    ////                    }
    ////                }
    //            }
    //        }
    //    }
    //-------------//
    // handleEvent //
    //-------------//
    private void handleEvent (EntityListEvent<Glyph> listEvent)
    {
        final Glyph glyph = listEvent.getEntity();

        // Fill symbol characteristics
        if (glyph != null) {
            Double pitch = glyph.getPitchPosition();

            if (pitch != null) {
                pitchPosition.setValue(pitch);
            } else {
                pitchPosition.setText("");
            }

            int interline = controller.getModel().getSheet().getScale().getInterline();
            weight.setValue(glyph.getNormalizedWeight(interline));
            width.setValue((double) glyph.getWidth() / interline);
            height.setValue((double) glyph.getHeight() / interline);

//            if (glyph.getGroup() != null) {
//                groupSpinner.setValue(glyph.getGroup()); //TODO: judicious???
//            }
        } else {
            pitchPosition.setText("");
            weight.setText("");
            width.setText("");
            height.setText("");
        }

        width.setEnabled(glyph != null);
        height.setEnabled(glyph != null);
        pitchPosition.setEnabled(glyph != null);
        weight.setEnabled(glyph != null);

        //                // Text info
        //                if (roleCombo != null) {
        //                    if ((shape != null) && shape.isText()) {
        //                        selfUpdatingText = true;
        //                        confField.setVisible(false);
        //                        textField.setVisible(true);
        //                        roleCombo.setVisible(true);
        //
        //                        roleCombo.setEnabled(true);
        //                        textField.setEnabled(true);
        //
        //                        if (glyph.getTextValue() != null) {
        //                            textField.setText(glyph.getTextValue());
        //
        //                            // Related word?
        //                            TextWord word = glyph.getTextWord();
        //
        //                            if (word != null) {
        //                                confField.setValue(word.getConfidence());
        //                                confField.setVisible(true);
        //                            }
        //                        } else {
        //                            textField.setText("");
        //                        }
        //
        //                        if (glyph.getTextRole() != null) {
        //                            roleCombo.setSelectedItem(glyph.getTextRole());
        //                        } else {
        //                            roleCombo.setSelectedItem(TextRole.UnknownRole);
        //                        }
        //
        //                        selfUpdatingText = false;
        //                    } else {
        //                        confField.setVisible(false);
        //                        textField.setVisible(false);
        //                        roleCombo.setVisible(false);
        //                    }
        //                }
        //
        //                // Time Signature info
        //                if (timeNum != null) {
        //                    if (ShapeSet.Times.contains(shape)) {
        //                        timeNum.setVisible(true);
        //                        timeDen.setVisible(true);
        //
        //                        timeNum.setEnabled(shape == Shape.CUSTOM_TIME);
        //                        timeDen.setEnabled(shape == Shape.CUSTOM_TIME);
        //
        //                        TimeRational timeRational = (shape == Shape.CUSTOM_TIME)
        //                                ? glyph.getTimeRational()
        //                                : TimeInter.rationalOf(shape);
        //
        //                        if (timeRational != null) {
        //                            timeNum.setValue(timeRational.num);
        //                            timeDen.setValue(timeRational.den);
        //                        } else {
        //                            timeNum.setText("");
        //                            timeDen.setText("");
        //                        }
        //                    } else {
        //                        timeNum.setVisible(false);
        //                        timeDen.setVisible(false);
        //                    }
        //                }
    }
}
