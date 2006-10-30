//----------------------------------------------------------------------------//
//                                                                            //
//                      S y m b o l G l y p h B o a r d                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphModel;
import omr.glyph.Shape;

import omr.math.Moments;

import omr.selection.Selection;
import omr.selection.SelectionHint;

import omr.ui.field.LDoubleField;
import omr.ui.field.LField;
import omr.ui.field.LIntegerField;
import static omr.ui.field.SpinnerUtilities.*;

import omr.util.Logger;
import omr.util.Predicate;

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
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>VERTICAL_GLYPH
 * </ul>
 * </dl>
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
        "Does this glyph intersect a legder");

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
        public boolean check (Glyph glyph)
        {
            return (glyph != null) && (glyph.getId() >= firstSymbolId) &&
                   (glyph.getShape() != Shape.NOISE);
        }
    };


    //~ Constructors -----------------------------------------------------------

    //------------------//
    // SymbolGlyphBoard //
    //------------------//
    /**
     * Create the symbol glyph board
     *
     *
     * @param unitName name of the owning unit
     * @param glyphModel the companion which handles glyph (de)assignments
     * @param firstSymbolId id of the first glyph made as a symbol (as opposed
     *                      to sticks/glyphs elaborated during previous steps)
     * @param glyphSelection glyph selection as input
     * @param glyphIdSelection glyph_id selection as output
     * @param glyphSetSelection input glyph set selection
     */
    public SymbolGlyphBoard (String     unitName,
                             GlyphModel glyphModel,
                             int        firstSymbolId,
                             Selection  glyphSelection,
                             Selection  glyphIdSelection,
                             Selection  glyphSetSelection)
    {
        // For all glyphs
        super(
            unitName,
            glyphModel,
            glyphSelection,
            glyphIdSelection,
            glyphSetSelection);

        // Cache info
        this.firstSymbolId = firstSymbolId;

        // Additional spinner for symbols
        if (glyphModel != null) {
            symbolSpinner = makeGlyphSpinner(
                glyphModel.getLag(),
                symbolPredicate);
            symbolSpinner.setName("symbolSpinner");
            symbolSpinner.setToolTipText("Specific spinner for symbol glyphs");
        }

        defineSpecificLayout(true); // use of spinners
    }

    //------------------//
    // SymbolGlyphBoard //
    //------------------//
    /**
     * Create a simplified symbol glyph board
     * @param unitName name of the owning unit
     * @param glyphModel the companion which handles glyph (de)assignments
     */
    public SymbolGlyphBoard (String     unitName,
                             GlyphModel glyphModel)
    {
        super(unitName, glyphModel);

        defineSpecificLayout(false); // no use of spinners
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // update //
    //--------//
    /**
     * Call-back triggered when Glyph Selection has been modified
     *
     * @param selection the (Glyph) Selection
     * @param hint potential notification hint
     */
    @Override
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        //        logger.info("SymbolGlyphBoard " + selection.getTag()
        //                    + " selfUpdating=" + selfUpdating);
        super.update(selection, hint);

        switch (selection.getTag()) {
        case VERTICAL_GLYPH :
            selfUpdating = true;

            Glyph glyph = (Glyph) selection.getEntity();

            // Set symbolSpinner accordingly
            if (symbolSpinner != null) {
                symbolSpinner.setValue(
                    symbolPredicate.check(glyph) ? glyph.getId() : NO_VALUE);
            }

            // Fill symbol characteristics
            if (glyph != null) {
                pitchPosition.setValue(glyph.getPitchPosition());
                ledger.setText(Boolean.toString(glyph.hasLedger()));
                stems.setValue(glyph.getStemNumber());

                Moments moments = glyph.getMoments();
                weight.setValue(moments.getWeight());
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

            break;

        default :
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

        r += 2; // --------------------------------

        if (useSpinners) {
            builder.addLabel("Id", cst.xy(1, r));
            builder.add(globalSpinner, cst.xy(3, r));

            builder.addLabel("Known", cst.xy(5, r));
            builder.add(knownSpinner, cst.xy(7, r));

            builder.addLabel("Symb", cst.xy(9, r));
            builder.add(symbolSpinner, cst.xy(11, r));
        }

        r += 2; // --------------------------------

        // For glyph characteristics, first line
        r += 2; // --------------------------------
        builder.add(pitchPosition.getLabel(), cst.xy(1, r));
        builder.add(pitchPosition.getField(), cst.xy(3, r));

        builder.add(ledger.getLabel(), cst.xy(5, r));
        builder.add(ledger.getField(), cst.xy(7, r));

        builder.add(stems.getLabel(), cst.xy(9, r));
        builder.add(stems.getField(), cst.xy(11, r));

        // For glyph characteristics, second line
        r += 2; // --------------------------------
        builder.add(weight.getLabel(), cst.xy(1, r));
        builder.add(weight.getField(), cst.xy(3, r));

        builder.add(width.getLabel(), cst.xy(5, r));
        builder.add(width.getField(), cst.xy(7, r));

        builder.add(height.getLabel(), cst.xy(9, r));
        builder.add(height.getField(), cst.xy(11, r));
    }
}
