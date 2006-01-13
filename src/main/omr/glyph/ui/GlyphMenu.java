//-----------------------------------------------------------------------//
//                                                                       //
//                           G l y p h M e n u                           //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.util.Dumper;

import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import java.util.ArrayList;

/**
 * Class <code>GlyphMenu</code> defines the popup menu which is linked to
 * the current selection of either one or several glyphs
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class GlyphMenu
{
    //~ Instance variables ------------------------------------------------

    // Concrete popup menu
    private final JPopupMenu popup;

    private final GlyphPane  pane;
    private final ShapeFocus focus;

    private final ConfirmAction confirmAction = new ConfirmAction();
    private final JMenuItem     confirmItem;

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
     * @param pane the top companion
     * @param focus the current shape focus
     */
    public GlyphMenu (final GlyphPane  pane,
                      ShapeFocus focus)
    {
        popup = new JPopupMenu();

        this.pane  = pane;
        this.focus = focus;

        // Confirm current guess
        confirmItem = popup.add(confirmAction);
        confirmItem.setToolTipText("Confirm current guess");

        popup.addSeparator();

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
                    pane.assignShape(shape,
                                     /* asGuessed => */ false,
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
                     pane.assignShape(shape,
                                      /* asGuessed => */ false,
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
                     pane.assignShape(shape,
                                      /* asGuessed => */ false,
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

        // Confirm or Assign
        if (glyph.getShape() == null) {
            if (glyph.getGuess() == null) {
                confirmAction.setEnabled(false);
                confirmItem.setIcon(null);
                confirmItem.setText("Confirm");
                confirmItem.setToolTipText("No guess for this glyph");
            } else {
                confirmAction.setTargetShape(glyph.getGuess());
                confirmAction.setEnabled(true);
                confirmItem.setIcon(glyph.getGuess().getIcon());
                if (focus.getCurrent() == glyph.getGuess()) {
                    confirmItem.setText("Confirm " + glyph.getGuess());
                    confirmItem.setToolTipText("Confirm this glyph is a " + glyph.getGuess());
                } else {
                    confirmItem.setText("Assign " + glyph.getGuess());
                    confirmItem.setToolTipText("Assign this glyph as a " + glyph.getGuess());
                }
            }
        } else {
            if (focus.getCurrent() == null) {
                confirmAction.setEnabled(false);
                confirmItem.setIcon(null);
                confirmItem.setText("Confirm");
                confirmItem.setToolTipText("No focus defined");
            } else {
                confirmAction.setTargetShape(glyph.getGuess());
                if (glyph.getGuess() != null) {
                    confirmItem.setIcon(glyph.getGuess().getIcon());
                }
                if (focus.getCurrent() == glyph.getGuess()) {
                    confirmAction.setEnabled(true);
                    confirmItem.setText("Confirm " + glyph.getGuess());
                    confirmItem.setToolTipText("Confirm this glyph is a " + glyph.getGuess());
                } else {
                    confirmAction.setEnabled(true);
                    confirmItem.setText("Force " + glyph.getGuess());
                    confirmItem.setToolTipText("Force this glyph as a " + glyph.getGuess());
                }
            }
        }
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
            if (glyph.getGuess() == null) {
                similarAction.setEnabled(false);
                similarItem.setText("Show similar");
            } else {
                similarAction.setEnabled(true);
                similarItem.setText("Show similar " + glyph.getGuess() + "'s");
            }
        } else {
            similarAction.setEnabled(true);
            similarItem.setText("Show similar " + glyph.getShape() + "'s");
        }
    }

    //-----------------//
    // updateForGlyphs //
    //-----------------//
    /**
     * Update the popup menu when there are several glyphs selected (more
     * than one)
     *
     * @param glyphs the collection of current glyphs
     */
    public void updateForGlyphs (List<Glyph> glyphs)
    {
        // Check that some candidate glyphs are consistent with current
        // focus
        int consistentNb = 0;
        if (focus.getCurrent() != null) {
            for (Glyph glyph : glyphs) {
                if (!glyph.isKnown() &&
                    (glyph.getGuess() == focus.getCurrent())) {
                    consistentNb++;
                }
            }
        }

        if (consistentNb > 0) {
            confirmAction.setEnabled(true);
            confirmAction.setTargetShape(focus.getCurrent());
            confirmItem.setText("Confirm " + consistentNb + " " +
                                focus.getCurrent());
            confirmItem.setToolTipText("Confirm " + consistentNb +
                                       " glyphs as " + focus.getCurrent());
        } else {
            confirmAction.setEnabled(false);
            confirmItem.setText("No candidate");
            confirmItem.setToolTipText
                ("No glyph consistent with current focus on "
                 + focus.getCurrent());
        }

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
        for (Glyph glyph : pane.getCurrentGlyphs()) {
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
        if (pane.getLatestShapeAssigned() != null) {
            latestAssign.setEnabled(true);
            latestAssign.setText(pane.getLatestShapeAssigned().toString());
        } else {
            latestAssign.setEnabled(false);
            latestAssign.setText("no shape");
        }
    }

    //~ Classes -----------------------------------------------------------

    //---------------//
    // ConfirmAction //
    //---------------//
    private class ConfirmAction
            extends AbstractAction
    {
        // Shape that could be assigned or confirmed
        private Shape targetShape;

        //~ Constructors --------------------------------------------------

        public ConfirmAction ()
        {
            super("Confirm Guess");
        }

        //~ Methods -------------------------------------------------------

        public void setTargetShape (Shape targetShape)
        {
            this.targetShape = targetShape;
        }

        public void actionPerformed (ActionEvent e)
        {
            pane.assignShape(targetShape,
                             /* asGuessed => */ true,
                             /* compound => */ false);
        }
    }

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
            // First phase, putting the stems apart
            List<Glyph> stems = new ArrayList<Glyph>();
            for (Glyph glyph : pane.getCurrentGlyphs()) {
                if (glyph.getShape() == Shape.COMBINING_STEM) {
                    stems.add(glyph);
                } else {
                    if (glyph.isKnown()) {
                        pane.setShape(glyph, null, /* UpdateUI => */ true);
                    }
                }
            }

            // Second phase dedicated to stems, if any
            if (stems.size() > 0) {
                pane.cancelStems(stems);
            }

            focus.colorizeAllGlyphs();  // TBI
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
            for (Glyph glyph : pane.getCurrentGlyphs()) {
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
            List<Glyph> glyphs = pane.getCurrentGlyphs();
            if (glyphs.size() == 1) {
                Glyph glyph = glyphs.get(0);
                if (glyph.getShape() != null) {
                    focus.setCurrent(glyph.getShape());
                } else {
                    focus.setCurrent(glyph.getGuess());
                }
            }
        }
    }
}
