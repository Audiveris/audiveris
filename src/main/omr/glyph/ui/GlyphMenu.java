//-----------------------------------------------------------------------//
//                                                                       //
//                           G l y p h M e n u                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.selection.Selection;
import omr.util.Dumper;

import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import java.util.ArrayList;

/**
 * Class <code>GlyphMenu</code> defines the popup menu which is linked to
 * the current selection of either one or several glyphs
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphMenu
{
    //~ Instance variables ------------------------------------------------

    // Current selection of glyphs
    private final Selection glyphSetSelection;

    // Concrete popup menu
    private final JPopupMenu popup;

    private final SymbolsBuilder  symbolsBuilder;
    private final ShapeFocusBoard shapeFocus;

    private final DumpAction    dumpAction = new DumpAction();
    private final JMenuItem     dumpItem;

    private final DeassignAction deassignAction = new DeassignAction();
    private final JMenuItem      deassignItem;

    private final SimilarAction similarAction = new SimilarAction();
    private final JMenuItem     similarItem;

    private final JMenu assignMenu;
    private final JMenu compoundMenu;
    private final JMenuItem latestAssign;

    //~ Constructors ------------------------------------------------------

    //-----------//
    // GlyphMenu //
    //-----------//
    /**
     * Create the popup menu
     *
     * @param symbolsBuilder the top companion
     * @param shapeFocus the current shape focus
     * @param glyphSetSelection the currently selected glyphs
     */
    public GlyphMenu (final SymbolsBuilder symbolsBuilder,
                      ShapeFocusBoard      shapeFocus,
                      Selection            glyphSetSelection)
    {
        this.symbolsBuilder  = symbolsBuilder;
        this.shapeFocus = shapeFocus;
        this.glyphSetSelection = glyphSetSelection;

        popup = new JPopupMenu();

        // Deassign selected glyph(s)
        deassignItem = popup.add(deassignAction);
        deassignItem.setToolTipText("Deassign selected glyph(s)");

        // Manually assign a shape
        assignMenu = new JMenu("Force to");

        // Direct link to latest shape assigned
        latestAssign  = new JMenuItem("no shape", null);
        latestAssign.setToolTipText("Assign latest shape");
        latestAssign.addActionListener (new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    JMenuItem source = (JMenuItem) e.getSource();
                    Shape shape = Shape.valueOf(source.getText());
                    symbolsBuilder.assignSetShape(getCurrentGlyphs(),
                                                  shape,
                                                  /* compound => */ false);
                }
            });
        assignMenu.add(latestAssign);
        assignMenu.addSeparator();

        Shape.addShapeItems
            (assignMenu,
             new ActionListener()
             {
                 public void actionPerformed (ActionEvent e)
                 {
                     JMenuItem source = (JMenuItem) e.getSource();
                     Shape shape = Shape.valueOf(source.getText());
                     symbolsBuilder.assignSetShape(getCurrentGlyphs(),
                                                   shape,
                                                   /* compound => */ false);
                 }
             });
        assignMenu.setToolTipText("Manually force an assignment");
        popup.add(assignMenu);

        popup.addSeparator();

        // Build a compound
        compoundMenu = new JMenu("Build compound");
        Shape.addShapeItems
            (compoundMenu,
             new ActionListener()
             {
                 public void actionPerformed (ActionEvent e)
                 {
                     JMenuItem source = (JMenuItem) e.getSource();
                     Shape shape = Shape.valueOf(source.getText());
                     symbolsBuilder.assignSetShape(getCurrentGlyphs(),
                                                   shape,
                                                   /* compound => */ true);
                 }
             });
        compoundMenu.setToolTipText("Manually build a compound");
        popup.add(compoundMenu);

        popup.addSeparator();

        // Dump current glyph
        dumpItem = popup.add(dumpAction);
        dumpItem.setToolTipText("Dump this glyph");

        popup.addSeparator();

        // Display all glyphs of the same shape
        similarItem = popup.add(similarAction);
        similarItem.setToolTipText("Display all similar glyphs");
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // getPopup //
    //----------//
    /**
     * Report the concrete popup menu
     *
     * @return the popup menu
     */
    public JPopupMenu getPopup()
    {
        return popup;
    }

    //----------------//
    // updateForGlyph //
    //----------------//
    /**
     * Update the popup menu when one glyph is selected as current
     *
     * @param glyph the current glyph
     */
    public void updateForGlyph (Glyph glyph)
    {
        // No compound for a single glyph
        compoundMenu.setEnabled(false);

        assignMenu.setText("Force assignment to");
        updateLatestAssign();

        // Dump
        dumpItem.setText("Dump glyph");

        // Deassign
        deassignAction.setEnabled(glyph.isKnown());
        if (glyph.getShape() == Shape.COMBINING_STEM) {
            deassignItem.setText("Cancel this stem");
            deassignItem.setToolTipText("Remove this selected stem");
        } else {
            deassignItem.setText("Deassign");
            deassignItem.setToolTipText("Deassign selected glyph");
        }

        // Show similar
        if (glyph.getShape() == null) {
            similarAction.setEnabled(false);
            similarItem.setText("Show similar");
        } else {
            similarAction.setEnabled(true);
            similarItem.setText("Show similar " + glyph.getShape() + "'s");
        }
    }

    //-------------------//
    // updateForGlyphSet //
    //-------------------//
    /**
     * Update the popup menu when there are several glyphs selected (more
     * than one)
     *
     * @param glyphs the collection of current glyphs
     */
    public void updateForGlyphSet (List<Glyph> glyphs)
    {
        // Assign
        assignMenu.setText("Assign each of these " + glyphs.size() + " glyphs as");
        updateLatestAssign();

        // Compound
        if (glyphs.size() > 1) {
            compoundMenu.setEnabled(true);
            compoundMenu.setText("Build one " + glyphs.size() + "-glyph Compound as");
        } else {
            compoundMenu.setEnabled(false);
            compoundMenu.setText("No compound");
        }

        // Dump
        dumpItem.setText("Dump " + glyphs.size() + " glyphs");

        // Deassign, check what is to be deassigned
        int knownNb = 0;
        int stemNb = 0;
        for (Glyph glyph : (List<Glyph>) glyphSetSelection.getEntity()) {
            if (glyph.isKnown()) {
                knownNb++;
                if (glyph.getShape() == Shape.COMBINING_STEM) {
                    stemNb++;
                }
            }
        }
        if (knownNb > 0) {
            deassignAction.setEnabled(true);
            deassignItem.setText("Deassign " + knownNb + " glyphs" +
                                 ((stemNb > 0) ?
                                  " w/ " + stemNb + " stems": ""));
            deassignItem.setToolTipText("Deassign selected glyphs");
        } else {
            deassignAction.setEnabled(false);
            deassignItem.setText("Deassign");
            deassignItem.setToolTipText("No glyphs to deassign");
        }
    }

    //--------------------//
    // updateLatestAssign //
    //--------------------//
    private void updateLatestAssign()
    {
        if (symbolsBuilder.getLatestShapeAssigned() != null) {
            latestAssign.setEnabled(true);
            latestAssign.setText(symbolsBuilder.getLatestShapeAssigned().toString());
        } else {
            latestAssign.setEnabled(false);
            latestAssign.setText("no shape");
        }
    }

    //------------------//
    // getCurrentGlyphs //
    //------------------//
    private List<Glyph> getCurrentGlyphs()
    {
        return (List<Glyph>) glyphSetSelection.getEntity();
    }

    //~ Classes -----------------------------------------------------------

    //----------------//
    // DeassignAction //
    //----------------//
    private class DeassignAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public DeassignAction ()
        {
            super("Deassign");
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            symbolsBuilder.deassignSetShape(getCurrentGlyphs());
        }
    }


    //------------//
    // DumpAction //
    //------------//
    private class DumpAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public DumpAction ()
        {
            super("Dump Glyph");
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            for (Glyph glyph : (List<Glyph>) glyphSetSelection.getEntity()) {
                Dumper.dump(glyph);
            }
        }
    }

    //---------------//
    // SimilarAction //
    //---------------//
    private class SimilarAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public SimilarAction ()
        {
            super("Show Similars");
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity();
            if (glyphs != null && glyphs.size() == 1) {
                Glyph glyph = glyphs.get(0);
                if (glyph.getShape() != null) {
                    shapeFocus.setCurrentShape(glyph.getShape());
                }
            }
        }
    }
}
