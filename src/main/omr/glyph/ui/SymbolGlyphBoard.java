//----------------------------------------------------------------------------//
//                                                                            //
//                      S y m b o l G l y p h B o a r d                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.text.TextInfo;
import omr.glyph.text.TextRole;

import omr.log.Logger;

import omr.math.Moments;

import omr.selection.GlyphEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.UserEvent;

import omr.sheet.ui.SheetsController;

import omr.ui.field.LDoubleField;
import omr.ui.field.LField;
import omr.ui.field.LIntegerField;
import omr.ui.field.SField;
import static omr.ui.field.SpinnerUtilities.*;

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
 *    <img src="doc-files/SymbolGlyphBoard.jpg"/>
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

    /** Spinner just for symbol glyphs */
    private JSpinner symbolSpinner;

    /** ComboBox for text role */
    private JComboBox roleCombo;

    /** Input/Output : textual content */
    protected final JTextField textField = new SField(
        true,
        "Content of a textual glyph");

    /** Glyph characteristics : position wrt staff */
    private LDoubleField pitchPosition = new LDoubleField(
        false,
        "Pitch",
        "Logical pitch position",
        "%.3f");

    /** Glyph characteristics : is there a ledger */
    private LField ledger = new LField(
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

        // Additional spinner for symbols
        if (glyphsController != null) {
            symbolSpinner = makeGlyphSpinner(
                glyphsController.getLag(),
                null, // Specific glyphs
                symbolPredicate);
            symbolSpinner.setName("symbolSpinner");
            symbolSpinner.setToolTipText("Specific spinner for symbol glyphs");
        }

        // Additional combo for text type
        paramAction = new ParamAction();
        roleCombo = new JComboBox(TextRole.values());
        roleCombo.addActionListener(paramAction);
        roleCombo.setToolTipText("Role of the Text");

        // Text field
        textField.setHorizontalAlignment(JTextField.LEFT);

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
     * Create a simplified symbol glyph board
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

                // Set symbolSpinner accordingly
                if (symbolSpinner != null) {
                    symbolSpinner.setValue(
                        symbolPredicate.check(glyph) ? glyph.getId() : NO_VALUE);
                }

                // Text Information
                if (roleCombo != null) {
                    selfUpdatingText = true;
                    roleCombo.setSelectedItem(TextRole.Unknown);

                    if ((glyph != null) &&
                        (glyph.getShape() != null) &&
                        (glyph.getShape().isText())) {
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
                        }
                    } else {
                        roleCombo.setEnabled(false);
                        textField.setEnabled(false);
                        textField.setText("");
                    }

                    selfUpdatingText = false;
                }

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

            builder.addLabel("Symb", cst.xy(9, r));
            builder.add(symbolSpinner, cst.xy(11, r));
        }

        r += 2; // --------------------------------
                // Deassign, shape

        r += 2; // --------------------------------
                // For text information

        if (roleCombo != null) {
            builder.add(roleCombo, cst.xyw(3, r, 1));
            builder.add(textField, cst.xyw(5, r, 7));
        }

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
                // Read text information
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Text='" + textField.getText().trim() + "' Role=" +
                        roleCombo.getSelectedItem());
                }

                SheetsController.selectedSheet()
                                .getSymbolsController()
                                .asyncAssignText(
                    glyphs,
                    (TextRole) roleCombo.getSelectedItem(),
                    textField.getText());
            }
        }
    }
}
