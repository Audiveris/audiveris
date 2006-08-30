//-----------------------------------------------------------------------//
//                                                                       //
//                    S y m b o l G l y p h B o a r d                    //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.Shape;
import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.ui.field.LField;
import omr.ui.field.LIntegerField;
import omr.ui.field.SpinnerUtilities;
import omr.util.Logger;

import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>SymbolGlyphBoard</code> defines an extended glyph board,
 * with an additional symbol glyph spinner : <ul>
 *
 * <li> A spinner to browse through all glyphs that are considered as
 * <b>symbols</b>, that is built from aggregation of contiguous sections,
 * or by combination of other symbols. This is a subset of the previous
 * one. Glyphs whose shape is set to {@link omr.glyph.Shape#NOISE}, that is
 * too small glyphs, are not included in this spinner.
 * </ul>
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
public class SymbolGlyphBoard
    extends GlyphBoard
{
    //~ Static variables/initializers -------------------------------------

    private static Logger logger = Logger.getLogger(SymbolGlyphBoard.class);

    //~ Instance variables ------------------------------------------------

    private GlyphPane pane;
    private GlyphLag vLag;

    // Spinner just for symbol glyphs
    private JSpinner symbolSpinner;

    // Glyph id for the very first symbol
    private int firstSymbolId;

    // Lists for spinner models
    private List<Integer> glyphIds  = new ArrayList<Integer>();
    private List<Integer> symbolIds = new ArrayList<Integer>();
    private List<Integer> knownIds  = new ArrayList<Integer>();

    // Glyph characteristics
    private LField ledger       = new LField
        (false, "Ledger", "Does this glyph intersect a legder");
    private LIntegerField pitchPosition  = new LIntegerField
        (false, "Pitch", "Logical pitch position");
    private LIntegerField stems     = new LIntegerField
        (false, "Stems", "Number of stems connected to this glyph");

    //~ Constructors ------------------------------------------------------

    //------------------//
    // SymbolGlyphBoard //
    //------------------//
    /**
     * Create the symbol glyph board
     *
     * @param pane the companion pane which handles the other UI entities
     * @param firstSymbolId id of the first glyph made as a symbol (as
     *                      opposed to sticks/glyphs elaborated during
     *                      previous steps)
     * @param vLag the related vertical glyph lag
     * @param glyphSelection glyph selection as input
     * @param glyphIdSelection glyph_id selection as output
     */
    public SymbolGlyphBoard (GlyphPane pane,
                             int       firstSymbolId,
                             GlyphLag  vLag,
                             Selection glyphSelection,
                             Selection glyphIdSelection,
                             Selection glyphSetSelection)
    {
        // For all glyphs
        super("SymbolBoard",
              vLag.getLastGlyphId(),
              glyphSelection,
              glyphIdSelection,
              glyphSetSelection);

        // Cache info
        this.pane          = pane;
        this.firstSymbolId = firstSymbolId;
        this.vLag          = vLag;

        // Change spinner model for glyph id
        glyphIds.add(NO_VALUE);
        globalSpinner.setModel(new SpinnerListModel(glyphIds));
        SpinnerUtilities.setRightAlignment(globalSpinner);
        SpinnerUtilities.fixIntegerList(globalSpinner); // Waiting for swing bug fix

        // Change spinner model for knownSpinner
        knownIds.add(NO_VALUE);
        knownSpinner.setModel(new SpinnerListModel(knownIds));
        SpinnerUtilities.setRightAlignment(knownSpinner);
        SpinnerUtilities.fixIntegerList(knownSpinner); // Waiting for swing bug fix

        // For symbols
        symbolSpinner = makeSpinner(symbolIds);
        symbolSpinner.setName("symbolSpinner");
        symbolSpinner.setToolTipText("Specific spinner for symbol glyphs");

        defineSpecificLayout();

        // Initially populate the various spinners models
        resetSpinners();
    }

    //------------------//
    // SymbolGlyphBoard //
    //------------------//
    /**
     * Create a simplified symbol glyph board
     */
    public SymbolGlyphBoard()
    {
        super("SymbolSimpleBoard");
        defineSpecificLayout();
    }

    //~ Methods -----------------------------------------------------------

    //----------------------//
    // defineSpecificLayout //
    //----------------------//
    /**
     * Define a specific layout for this Symbol GlyphBoard
     */
    protected void defineSpecificLayout()
    {
        int r = 1;                      // --------------------------------

        r += 2;                         // --------------------------------
        if (pane != null) {
            builder.addLabel("Id",      cst.xy (1,  r));
            builder.add(globalSpinner,  cst.xy (3,  r));

            builder.addLabel("Known",   cst.xy (5,  r));
            builder.add(knownSpinner,   cst.xy (7,  r));

            builder.addLabel("Symb",    cst.xy (9,  r));
            builder.add(symbolSpinner,  cst.xy (11, r));
        }

        r += 2;                         // --------------------------------

        // For glyph characteristics
        r += 2;                         // --------------------------------
        builder.add(pitchPosition.getLabel(), cst.xy (1, r));
        builder.add(pitchPosition.getField(), cst.xy (3, r));

        builder.add(ledger.getLabel(),  cst.xy (5,  r));
        builder.add(ledger.getField(),  cst.xy (7,  r));

        builder.add(stems.getLabel(),   cst.xy (9,  r));
        builder.add(stems.getField(),   cst.xy (11, r));
    }

    //-------------//
    // assignGlyph //
    //-------------//
    /**
     * This glyph is now known (assigned to a true shape)
     *
     * @param id the glyph id
     */
    public void assignGlyph (int id)
    {
        if (logger.isFineEnabled()) {
            logger.fine ("assign id=" + id);
        }

        Integer iden = new Integer(id);

        // Add in the known ones, if not already there
        if (!knownIds.contains(iden)) {
            knownIds.add(iden);
        }
    }

    //---------------//
    // deassignGlyph //
    //---------------//
    /**
     * This glyph is explicitly flagged as not interesting
     *
     * @param id the glyph id
     */
    public void deassignGlyph (int id)
    {
        if (logger.isFineEnabled()) {
            logger.fine ("deassign id=" + id);
        }

        Integer iden = new Integer(id);

        // Just in case, remove from knownSpinner model
        knownIds.remove(iden);
    }

    //------------//
    // addGlyphId //
    //------------//
    /**
     * Update the spinners with the glyph at hand
     *
     * @param glyph the glyph whose id is to be inserted into various spinner
     * models
     */
    public void addGlyphId (Glyph glyph)
    {
        final int id = glyph.getId();

        if (logger.isFineEnabled()) {
            logger.fine ("add id=" + id  + " glyph=" + glyph);
        }

        // Universal id spinner
        glyphIds.add(id);

        if (id >= firstSymbolId) {
            // Symbol spinner
            if (glyph.getShape() != Shape.NOISE) {
                symbolIds.add(id);
            }

            // Known spinner
            if (glyph.isKnown()) {
                knownIds.add(id);
            }
        }
    }

    //---------------//
    // removeGlyphId //
    //--------------//
    /**
     * Update the spinners with the glyph at hand
     *
     * @param id the glyph id
     */
    public void removeGlyphId (int id)
    {
        if (logger.isFineEnabled()) {
            logger.fine ("remove id=" + id);
        }

        Integer integerId = new Integer(id);

        // Universal id spinner
        glyphIds.remove(integerId);

        if (id >= firstSymbolId) {
            // Symbol spinner
            symbolIds.remove(integerId);
            // Just in case
            knownIds.remove(integerId);
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * Callback triggered by a change in one of the spinners.
     *
     * @param e the change event, this allows to retrieve the originating
     * spinner
     */
    @Override
        public void stateChanged (ChangeEvent e)
    {
        super.stateChanged(e);

        // Symbol evaluation ?
        JSpinner spinner = (JSpinner) e.getSource();
        int glyphId = (Integer) spinner.getValue();
        if (glyphId != NO_VALUE) {
            Glyph glyph = null;
            if (glyphId >= firstSymbolId) {
                glyph = pane.getEntity(glyphId);
            }
            ///pane.getEvaluatorsPanel().evaluate(glyph);
        }
    }

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
    public void update (Selection selection,
                        SelectionHint hint)
    {
//        logger.info("SymbolGlyphBoard " + selection.getTag()
//                    + " updating=" + updating + " idSelecting=" + idSelecting);

        switch (selection.getTag()) {
        case VERTICAL_GLYPH :

            if (updating) {
                ///logger.warning("double updating");
                return;
            }

            // For normal glyph id and shape
            try {
                super.update(selection, hint);
            } catch (IllegalArgumentException ex) {
                logger.warning("IllegalArgumentException");
                return;
            }

            updating = true;
            Glyph glyph = (Glyph) selection.getEntity();

            // Update spinners
            int id = 0;
            if (glyph != null) {
                id = glyph.getId();
            }

            // Set symbol id accordingly
            trySetSpinner(symbolSpinner,
                          (id >= firstSymbolId && glyph.getShape() != Shape.NOISE)
                          ? id
                          : NO_VALUE);

            // Set knownSpinner accordingly
            trySetSpinner(knownSpinner,
                          (id >= firstSymbolId && glyph.isKnown())
                          ? id
                          : NO_VALUE);

            // Fill characteristics
            if (glyph != null) {
                pitchPosition.setValue((int) Math.rint(glyph.getPitchPosition()));
                ledger.setText(Boolean.toString(glyph.hasLedger()));
                stems.setValue(glyph.getStemNumber());
            } else {
                ledger.setText("");
                pitchPosition.setText("");
                stems.setText("");
            }
            updating = false;
            break;

        case GLYPH_SET :
            super.update(selection, hint);
            break;

        default :
        }
    }

    //-------------//
    // makeSpinner //
    //-------------//
    private JSpinner makeSpinner (List<Integer> ids)
    {
        JSpinner spinner = new JSpinner();
        ids.add(NO_VALUE);
        spinner.setModel(new SpinnerListModel(ids));
        spinner.addChangeListener(this);
        SpinnerUtilities.setRightAlignment(spinner);
        SpinnerUtilities.fixIntegerList(spinner); // Waiting for swing bug fix

        return spinner;
    }

    //---------------//
    // resetSpinners //
    //---------------//
    /**
     * Reset the data models for all contained spinners, according to the
     * current population of glyphs
     */
    void resetSpinners()
    {
        // Clean up all lists
        glyphIds.clear();  glyphIds.add(new Integer(NO_VALUE));
        symbolIds.clear(); symbolIds.add(new Integer(NO_VALUE));
        knownIds.clear();  knownIds.add(new Integer(NO_VALUE));

        // Add the ids of all glyphs
        for (Glyph glyph : vLag.getGlyphs()) {
            addGlyphId(glyph);
        }
    }
}
