//----------------------------------------------------------------------------//
//                                                                            //
//                      S y m b o l G l y p h B o a r d                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Shape;
import omr.glyph.ShapeSet;
import omr.glyph.facets.Glyph;
import omr.text.TextRole;

import omr.score.entity.Text.CreatorText.CreatorType;
import omr.score.entity.TimeRational;
import omr.score.entity.TimeSignature;

import omr.selection.GlyphEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.UserEvent;

import omr.sheet.ui.SheetsController;

import omr.text.TextRoleInfo;
import omr.text.TextWord;

import omr.ui.field.LComboBox;
import omr.ui.field.LDoubleField;
import omr.ui.field.LIntegerField;
import omr.ui.field.LTextField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

/**
 * Class {@code SymbolGlyphBoard} defines an extended glyph board,
 * with characteristics (pitch position, stem number, etc) that are
 * specific to a symbol, and an additional symbol glyph spinner.
 * <ul>
 * <li>A <b>symbolSpinner</b> to browse through all glyphs that are
 * considered as symbols, that is built from aggregation of contiguous
 * sections, or by combination of other symbols.
 * Glyphs whose shape is set to {@link omr.glyph.Shape#NOISE}, that is too
 * small glyphs, are not included in this spinner.</ul>
 *
 * <h4>Layout of an instance of SymbolGlyphBoard:<br/>
 * <img src="doc-files/SymbolGlyphBoard.png"/>
 * </h4>
 *
 * @author Hervé Bitteur
 */
public class SymbolGlyphBoard
        extends GlyphBoard
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            SymbolGlyphBoard.class);

    //~ Instance fields --------------------------------------------------------
    /** Numerator of time signature */
    private LIntegerField timeNum;

    /** Denominator of time signature */
    private LIntegerField timeDen;

    /** ComboBox for text role */
    private LComboBox<TextRole> roleCombo;

    /** ComboBox for text role type */
    private LComboBox<CreatorType> typeCombo;

    /** Output : textual confidence */
    protected LIntegerField confField;

    /** Input/Output : textual content */
    protected LTextField textField;

    /** Glyph characteristics : position wrt staff */
    private LDoubleField pitchPosition = new LDoubleField(
            false,
            "Pitch",
            "Logical pitch position",
            "%.3f");

    /** Glyph characteristics : is there a ledger */
    private LTextField ledger = new LTextField(
            false,
            "Ledger",
            "Does this glyph intersect a ledger?");

    /** Glyph characteristics : how many stems */
    private LIntegerField stems = new LIntegerField(
            false,
            "Stems",
            "Number of stems connected to this glyph");

    /** Glyph characteristics : normalized weight */
    private LDoubleField weight = new LDoubleField(
            false,
            "Weight",
            "Normalized weight",
            "%.3f");

    /** Glyph characteristics : normalized width */
    private LDoubleField width = new LDoubleField(
            false,
            "Width",
            "Normalized width",
            "%.3f");

    /** Glyph characteristics : normalized height */
    private LDoubleField height = new LDoubleField(
            false,
            "Height",
            "Normalized height",
            "%.3f");

    /** Handling of entered / selected values */
    private final Action paramAction;

    /** To avoid unwanted events */
    private boolean selfUpdatingText;

    //~ Constructors -----------------------------------------------------------
    //------------------//
    // SymbolGlyphBoard //
    //------------------//
    /**
     * Create the symbol glyph board.
     *
     * @param glyphsController the companion which handles glyph (de)assignments
     * @param useSpinners      true for use of spinners
     * @param expanded         true to initially expand this board
     */
    public SymbolGlyphBoard (GlyphsController glyphsController,
                             boolean useSpinners,
                             boolean expanded)
    {
        // For all glyphs
        super(glyphsController, useSpinners, true);

        // Additional combo for text role
        paramAction = new ParamAction();
        roleCombo = new LComboBox<>(
                "Role",
                "Role of the Text",
                TextRole.values());
        roleCombo.getField().setMaximumRowCount(TextRole.values().length);
        roleCombo.addActionListener(paramAction);

        // Additional combo for text type
        typeCombo = new LComboBox<>(
                "Type",
                "Type of the Text",
                CreatorType.values());
        typeCombo.addActionListener(paramAction);

        // Confidence and Text fields
        confField = new LIntegerField(false, "Conf", "Confidence in text value");
        textField = new LTextField(true, "Text", "Content of a textual glyph");
        textField.getField().setHorizontalAlignment(JTextField.LEFT);

        // Time signature
        timeNum = new LIntegerField("Num", "");
        timeDen = new LIntegerField("Den", "");

        defineSpecificLayout();

        // Needed to process user input when RETURN/ENTER is pressed
        getComponent().
                getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).
                put(KeyStroke.getKeyStroke("ENTER"), "TextAction");
        getComponent().getActionMap().put("TextAction", paramAction);
    }

    //~ Methods ----------------------------------------------------------------
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

            if (event instanceof GlyphEvent) {
                selfUpdating = true;

                GlyphEvent glyphEvent = (GlyphEvent) event;
                Glyph glyph = glyphEvent.getData();
                Shape shape = (glyph != null) ? glyph.getShape() : null;

                // Fill symbol characteristics
                if (glyph != null) {
                    pitchPosition.setValue(glyph.getPitchPosition());
                    ledger.setText(Boolean.toString(glyph.isWithLedger()));
                    stems.setValue(glyph.getStemNumber());

                    weight.setValue(glyph.getNormalizedWeight());
                    width.setValue(glyph.getNormalizedWidth());
                    height.setValue(glyph.getNormalizedHeight());
                } else {
                    ledger.setText("");
                    pitchPosition.setText("");
                    stems.setText("");

                    weight.setText("");
                    width.setText("");
                    height.setText("");
                }

                // Text info
                if (roleCombo != null) {
                    if ((shape != null) && shape.isText()) {
                        selfUpdatingText = true;
                        confField.setVisible(false);
                        textField.setVisible(true);
                        roleCombo.setVisible(true);
                        typeCombo.setVisible(false);

                        roleCombo.setEnabled(true);
                        textField.setEnabled(true);

                        if (glyph.getTextValue() != null) {
                            textField.setText(glyph.getTextValue());
                            // Related word?
                            TextWord word = glyph.getTextWord();
                            if (word != null) {
                                confField.setValue(word.getConfidence());
                                confField.setVisible(true);
                            }
                        } else {
                            textField.setText("");
                        }


                        if (glyph.getTextRole() != null) {
                            roleCombo.setSelectedItem(glyph.getTextRole().role);

                            if (glyph.getTextRole().role == TextRole.Creator) {
                                typeCombo.setVisible(true);
                                typeCombo.setSelectedItem(
                                        glyph.getTextRole().creatorType);
                            }
                        } else {
                            roleCombo.setSelectedItem(TextRole.UnknownRole);
                        }

                        selfUpdatingText = false;
                    } else {
                        confField.setVisible(false);
                        textField.setVisible(false);
                        roleCombo.setVisible(false);
                        typeCombo.setVisible(false);
                    }
                }

                // Time Signature info
                if (timeNum != null) {
                    if (ShapeSet.Times.contains(shape)) {
                        timeNum.setVisible(true);
                        timeDen.setVisible(true);

                        timeNum.setEnabled(
                                shape == Shape.CUSTOM_TIME);
                        timeDen.setEnabled(
                                shape == Shape.CUSTOM_TIME);

                        TimeRational timeRational = (shape == Shape.CUSTOM_TIME)
                                ? glyph.getTimeRational()
                                : TimeSignature.rationalOf(
                                shape);

                        if (timeRational != null) {
                            timeNum.setValue(timeRational.num);
                            timeDen.setValue(timeRational.den);
                        } else {
                            timeNum.setText("");
                            timeDen.setText("");
                        }
                    } else {
                        timeNum.setVisible(false);
                        timeDen.setVisible(false);
                    }
                }

                selfUpdating = false;
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //----------------------//
    // defineSpecificLayout //
    //----------------------//
    /**
     * Define a specific layout for this Symbol GlyphBoard
     */
    private void defineSpecificLayout ()
    {
        int r = 1; // --------------------------------
        // Glyph ---

        r += 2; // --------------------------------
        // shape

        r += 2; // --------------------------------
        // Glyph characteristics, first line

        builder.add(pitchPosition.getLabel(), cst.xy(1, r));
        builder.add(pitchPosition.getField(), cst.xy(3, r));

        builder.add(ledger.getLabel(), cst.xy(5, r));
        builder.add(ledger.getField(), cst.xy(7, r));

        builder.add(stems.getLabel(), cst.xy(9, r));
        builder.add(stems.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        // Glyph characteristics, second line

        builder.add(weight.getLabel(), cst.xy(1, r));
        builder.add(weight.getField(), cst.xy(3, r));

        builder.add(width.getLabel(), cst.xy(5, r));
        builder.add(width.getField(), cst.xy(7, r));

        builder.add(height.getLabel(), cst.xy(9, r));
        builder.add(height.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        // Text information, first line

        if (textField != null) {
            builder.add(confField.getLabel(), cst.xyw(1, r, 1));
            builder.add(confField.getField(), cst.xyw(3, r, 1));
            confField.setVisible(false);
            builder.add(textField.getLabel(), cst.xyw(5, r, 1));
            builder.add(textField.getField(), cst.xyw(7, r, 5));
            textField.setVisible(false);
        }

        // or time signature parameters
        if (timeNum != null) {
            builder.add(timeNum.getLabel(), cst.xy(5, r));
            builder.add(timeNum.getField(), cst.xy(7, r));
            timeNum.setVisible(false);

            builder.add(timeDen.getLabel(), cst.xy(9, r));
            builder.add(timeDen.getField(), cst.xy(11, r));
            timeDen.setVisible(false);
        }

        r += 2; // --------------------------------
        // Text information, second line

        if (roleCombo != null) {
            builder.add(roleCombo.getLabel(), cst.xyw(1, r, 1));
            builder.add(roleCombo.getField(), cst.xyw(3, r, 3));
            roleCombo.setVisible(false);

            builder.add(typeCombo.getLabel(), cst.xyw(7, r, 1));
            builder.add(typeCombo.getField(), cst.xyw(9, r, 3));
            typeCombo.setVisible(false);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-------------//
    // ParamAction //
    //-------------//
    private class ParamAction
            extends AbstractAction
    {
        //~ Methods ------------------------------------------------------------

        // Method run whenever user presses Return/Enter in one of the parameter
        // fields
        @Override
        public void actionPerformed (ActionEvent e)
        {
            // Discard irrelevant action events
            if (selfUpdatingText) {
                return;
            }

            // Get current glyph set
            GlyphSetEvent glyphsEvent = (GlyphSetEvent) getSelectionService().
                    getLastEvent(
                    GlyphSetEvent.class);
            Set<Glyph> glyphs = (glyphsEvent != null)
                    ? glyphsEvent.getData() : null;

            if ((glyphs != null) && !glyphs.isEmpty()) {
                // Read shape information
                String shapeName = shapeField.getText();

                if (shapeName.isEmpty()) {
                    return;
                }

                Shape shape = Shape.valueOf(shapeName);

                // Text?
                if (shape.isText()) {
                    logger.debug("Text=''{}'' Role={}",
                            textField.getText().trim(),
                            roleCombo.getSelectedItem());

                    CreatorType type = null;
                    TextRole role = roleCombo.getSelectedItem();
                    if (role == TextRole.Creator) {
                        type = typeCombo.getSelectedItem();
                    }
                    TextRoleInfo roleInfo = new TextRoleInfo(role, type);
                    SheetsController.getCurrentSheet().getSymbolsController().
                            asyncAssignTexts(
                            glyphs,
                            roleInfo,
                            textField.getText());
                } else // Custom time sig?
                if (shape == Shape.CUSTOM_TIME) {
                    int num = timeNum.getValue();
                    int den = timeDen.getValue();

                    if ((num != 0) && (den != 0)) {
                        SheetsController.getCurrentSheet().
                                getSymbolsController().asyncAssignRationals(
                                glyphs,
                                new TimeRational(num, den));
                    } else {
                        logger.warn("Invalid time signature parameters");
                    }
                }
            }
        }
    }
}
