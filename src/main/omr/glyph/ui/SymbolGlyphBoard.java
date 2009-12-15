//----------------------------------------------------------------------------//
//                                                                            //
//                      S y m b o l G l y p h B o a r d                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.text.TextInfo;
import omr.glyph.text.TextRole;

import omr.log.Logger;

import omr.math.Moments;
import omr.math.Rational;

import omr.score.entity.Text.CreatorText.CreatorType;
import omr.score.entity.TimeSignature;

import omr.selection.GlyphEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.UserEvent;

import omr.sheet.ui.SheetsController;

import omr.ui.field.LComboBox;
import omr.ui.field.LDoubleField;
import omr.ui.field.LIntegerField;
import omr.ui.field.LTextField;

import omr.util.Implement;
import omr.util.Predicate;
import static omr.util.Synchronicity.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import javax.swing.*;

/**
 * Class <code>SymbolGlyphBoard</code> defines an extended glyph board, with
 * characteristics (pitch position, stem number, etc) that are specific to a
 * symbol, and an additional symbol glyph spinner : <ul>
 *
 * <li>A <b>symbolSpinner</b> to browse through all glyphs that are considered
 * as symbols, that is built from aggregation of contiguous sections, or by
 * combination of other symbols. Glyphs whose shape is set to {@link
 * omr.glyph.Shape#NOISE}, that is too small glyphs, are not included in this
 * spinner. The symbolSpinner is thus a subset of the knownSpinner (which is
 * itself a subset of the globalSpinner). </ul>
 *
 * <h4>Layout of an instance of SymbolGlyphBoard:<br/>
 *    <img src="doc-files/SymbolGlyphBoard.png"/>
 * </h4>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
class SymbolGlyphBoard
    extends GlyphBoard
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SymbolGlyphBoard.class);

    //~ Instance fields --------------------------------------------------------

    /** Numerator of time signature */
    private LIntegerField timeNum;

    /** Denominator of time signature */
    private LIntegerField timeDen;

    /** ComboBox for text role */
    private LComboBox roleCombo;

    /** ComboBox for text role type */
    private LComboBox typeCombo;

    /** Input/Output : textual content */
    protected final LTextField textField = new LTextField(
        true,
        "Text",
        "Content of a textual glyph");

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

    /** Glyph id for the very first symbol */
    private int firstSymbolId;

    /** Predicate for symbol glyphs */
    private Predicate<Glyph> symbolPredicate = new Predicate<Glyph>() {
        @Implement(Predicate.class)
        public boolean check (Glyph glyph)
        {
            return (glyph != null) && (glyph.getId() >= firstSymbolId) &&
                   (glyph.getShape() != Shape.NOISE);
        }
    };

    /** Handling of entered / selected values */
    private final Action paramAction;

    /** To avoid unwanted events */
    private boolean selfUpdatingText;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // SymbolGlyphBoard //
    //------------------//
    /**
     * Create the symbol glyph board
     *
     *
     * @param unitName name of the owning unit
     * @param glyphsController the companion which handles glyph (de)assignments
     * @param firstSymbolId id of the first glyph made as a symbol (as opposed
     *                      to sticks/glyphs elaborated during previous steps)
     */
    public SymbolGlyphBoard (String           unitName,
                             GlyphsController glyphsController,
                             int              firstSymbolId)
    {
        // For all glyphs
        super(unitName, glyphsController, null);

        // Cache info
        this.firstSymbolId = firstSymbolId;

        // Additional combo for text role
        paramAction = new ParamAction();
        roleCombo = new LComboBox(
            "Role",
            "Role of the Text",
            TextRole.values());
        roleCombo.addActionListener(paramAction);

        // Additional combo for text type
        typeCombo = new LComboBox(
            "Type",
            "Type of the Text",
            CreatorType.values());
        typeCombo.addActionListener(paramAction);

        // Text field
        textField.getField()
                 .setHorizontalAlignment(JTextField.LEFT);

        // Time signature
        timeNum = new LIntegerField("Num", "");
        timeDen = new LIntegerField("Den", "");

        defineSpecificLayout(true); // use of spinners

        // Needed to process user input when RETURN/ENTER is pressed
        getComponent()
            .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke("ENTER"), "TextAction");
        getComponent()
            .getActionMap()
            .put("TextAction", paramAction);
    }

    //------------------//
    // SymbolGlyphBoard //
    //------------------//
    /**
     * Create a simplified symbol glyph board (used by {@link GlyphBrowser})
     * @param unitName name of the owning unit
     * @param glyphsController the companion which handles glyph (de)assignments
     */
    public SymbolGlyphBoard (String           unitName,
                             GlyphsController glyphsController)
    {
        super(unitName, glyphsController);
        paramAction = null;

        defineSpecificLayout(false); // no use of spinners
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Glyph Selection has been modified
     *
     * @param event the (Glyph or glyph set) Selection
     */
    @Override
    public void onEvent (UserEvent event)
    {
        ///logger.info("SymbolGlyphBoard event:" + event);
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            super.onEvent(event);

            if (event instanceof GlyphEvent) {
                selfUpdating = true;

                GlyphEvent glyphEvent = (GlyphEvent) event;
                Glyph      glyph = glyphEvent.getData();
                Shape      shape = (glyph != null) ? glyph.getShape() : null;

                // Fill symbol characteristics
                if (glyph != null) {
                    pitchPosition.setValue(glyph.getPitchPosition());
                    ledger.setText(Boolean.toString(glyph.isWithLedger()));
                    stems.setValue(glyph.getStemNumber());

                    Moments moments = glyph.getMoments();
                    weight.setValue(moments.getWeight()); // Normalized
                    width.setValue(moments.getWidth());
                    height.setValue(moments.getHeight());
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
                        textField.setVisible(true);
                        roleCombo.setVisible(true);
                        typeCombo.setVisible(false);

                        roleCombo.setEnabled(true);
                        textField.setEnabled(true);

                        TextInfo textInfo = glyph.getTextInfo();

                        if (textInfo.getContent() != null) {
                            textField.setText(glyph.getTextInfo().getContent());
                        } else {
                            textField.setText("");
                        }

                        if (textInfo.getTextRole() != null) {
                            roleCombo.setSelectedItem(textInfo.getTextRole());

                            if (textInfo.getTextRole() == TextRole.Creator) {
                                typeCombo.setVisible(true);
                                typeCombo.setSelectedItem(
                                    textInfo.getCreatorType());
                            }
                        } else {
                            roleCombo.setSelectedItem(TextRole.Unknown);
                        }

                        selfUpdatingText = false;
                    } else {
                        textField.setVisible(false);
                        roleCombo.setVisible(false);
                        typeCombo.setVisible(false);
                    }
                }

                // Time Signature info
                if (timeNum != null) {
                    if (ShapeRange.Times.contains(shape)) {
                        timeNum.setVisible(true);
                        timeDen.setVisible(true);

                        timeNum.setEnabled(
                            shape == Shape.CUSTOM_TIME_SIGNATURE);
                        timeDen.setEnabled(
                            shape == Shape.CUSTOM_TIME_SIGNATURE);

                        Rational rational = (shape == Shape.CUSTOM_TIME_SIGNATURE)
                                            ? glyph.getRational()
                                            : TimeSignature.rationalOf(shape);

                        if (rational != null) {
                            timeNum.setValue(rational.num);
                            timeDen.setValue(rational.den);
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
            logger.warning(getClass().getName() + " onEvent error", ex);
        }
    }

    //----------------------//
    // defineSpecificLayout //
    //----------------------//
    /**
     * Define a specific layout for this Symbol GlyphBoard
     *
     * @param useSpinners true if spinners must be created
     */
    protected void defineSpecificLayout (boolean useSpinners)
    {
        int r = 1; // --------------------------------
                   // Glyph ---

        r += 2; // --------------------------------
                // Spinners

        if (useSpinners) {
            builder.addLabel("Id", cst.xy(1, r));
            builder.add(globalSpinner, cst.xy(3, r));

            builder.addLabel("Known", cst.xy(5, r));
            builder.add(knownSpinner, cst.xy(7, r));
        }

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

        builder.add(textField.getLabel(), cst.xyw(1, r, 1));
        builder.add(textField.getField(), cst.xyw(3, r, 9));

        // or time signature parameters
        builder.add(timeNum.getLabel(), cst.xy(5, r));
        builder.add(timeNum.getField(), cst.xy(7, r));

        builder.add(timeDen.getLabel(), cst.xy(9, r));
        builder.add(timeDen.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
                // Text information, second line

        if (roleCombo != null) {
            builder.add(roleCombo.getLabel(), cst.xyw(1, r, 1));
            builder.add(roleCombo.getField(), cst.xyw(3, r, 3));

            builder.add(typeCombo.getLabel(), cst.xyw(7, r, 1));
            builder.add(typeCombo.getField(), cst.xyw(9, r, 3));
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
        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            // Discard irrelevant action events
            if (selfUpdatingText) {
                return;
            }

            // Get current glyph set
            GlyphSetEvent glyphsEvent = (GlyphSetEvent) selectionService.getLastEvent(
                GlyphSetEvent.class);
            Set<Glyph>    glyphs = (glyphsEvent != null)
                                   ? glyphsEvent.getData() : null;

            if ((glyphs != null) && !glyphs.isEmpty()) {
                // Read shape information
                String shapeName = shapeField.getText();

                if (shapeName.equals("")) {
                    return;
                }

                Shape shape = Shape.valueOf(shapeName);

                // Text?
                if (shape.isText()) {
                    if (logger.isFineEnabled()) {
                        logger.fine(
                            "Text='" + textField.getText().trim() + "' Role=" +
                            roleCombo.getSelectedItem());
                    }

                    TextRole role = (TextRole) roleCombo.getSelectedItem();
                    SheetsController.selectedSheet()
                                    .getSymbolsController()
                                    .asyncAssignTexts(
                        glyphs,
                        ((role == TextRole.Creator)
                         ? (CreatorType) typeCombo.getSelectedItem() : null),
                        role,
                        textField.getText());
                } else
                // Custom time sig?
                if (shape == Shape.CUSTOM_TIME_SIGNATURE) {
                    int num = timeNum.getValue();
                    int den = timeDen.getValue();

                    if ((num != 0) && (den != 0)) {
                        SheetsController.selectedSheet()
                                        .getSymbolsController()
                                        .asyncAssignRationals(
                            glyphs,
                            new Rational(num, den));
                    } else {
                        logger.warning("Invalid time signature parameters");
                    }
                }
            }
        }
    }
}
