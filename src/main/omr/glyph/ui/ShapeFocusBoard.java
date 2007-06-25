//----------------------------------------------------------------------------//
//                                                                            //
//                       S h a p e F o c u s B o a r d                        //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphModel;
import omr.glyph.Shape;

import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.selection.SelectionTag;
import static omr.selection.SelectionTag.*;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.ui.Board;
import omr.ui.Board.Tag;
import omr.ui.field.SpinnerUtilities;
import static omr.ui.field.SpinnerUtilities.*;
import omr.ui.util.Panel;

import omr.util.Implement;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>ShapeFocusBoard</code> handles the shape that receives current
 * focus, and all glyphs whose shape corresponds to the focus (for example all
 * treble clefs glyphs if such is the focus)
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>*GLYPH (if GLYPH_MODIFIED)
 * </ul>
 *
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>VERTICAL_GLYPH_ID (flagged with GLYPH_INIT hint)
 * </ul>
 * </dl>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
class ShapeFocusBoard
    extends Board
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ShapeFocusBoard.class);

    //~ Enumerations -----------------------------------------------------------

    /** Filter on which symbols should be displayed */
    public static enum Filter {
        /** Display all symbols */
        ALL,
        /** Display only known symbols */
        KNOWN, 
        /** Display only unknown symbols */
        UNKNOWN, 
        /** Display only translated symbols */
        TRANSLATED, 
        /** Display only untranslated symbols */
        UNTRANSLATED;
    }

    //~ Instance fields --------------------------------------------------------

    private final GlyphLagView view;
    private final GlyphModel   glyphModel;
    private final Sheet        sheet;

    /** Counter on symbols assigned to the current shape */
    private Counter assignedCounter = new Counter();

    /** Button to select the shape focus */
    private JButton selectButton = new JButton();

    /** Filter for known / unknown symbol display */
    private JComboBox filterButton = new JComboBox(Filter.values());

    /** Popup menu to allow shape selection */
    private JPopupMenu pm = new JPopupMenu();

    /** The current shape used for display focus */
    private Shape currentShape;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // ShapeFocusBoard //
    //-----------------//
    /**
     * Create the instance to handle the shape focus, with pointers to needed
     * companions
     *
     * @param sheet the related sheet
     * @param view the displayed lag view
     * @param glyphModel the related glyph model
     * @param filterListener the action linked to filter button
     */
    public ShapeFocusBoard (Sheet          sheet,
                            GlyphLagView   view,
                            GlyphModel     glyphModel,
                            ActionListener filterListener)
    {
        super(Tag.CUSTOM, "ShapeFocusBoard");

        this.sheet = sheet;
        this.view = view;
        this.glyphModel = glyphModel;

        // Tool Tips
        selectButton.setToolTipText("Select candidate shape");
        selectButton.setHorizontalAlignment(SwingConstants.LEFT);
        selectButton.addActionListener(
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        pm.show(
                            selectButton,
                            selectButton.getX(),
                            selectButton.getY());
                    }
                });

        // Filter
        filterButton.addActionListener(filterListener);
        filterButton.setToolTipText(
            "Select displayed glyphs according to their current state");

        // Popup menu for shape selection
        JMenuItem noFocus = new JMenuItem("No Focus");
        noFocus.setToolTipText("Cancel any focus");
        noFocus.addActionListener(
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        setCurrentShape(null);
                    }
                });
        pm.add(noFocus);
        Shape.addShapeItems(
            pm,
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        JMenuItem source = (JMenuItem) e.getSource();
                        setCurrentShape(Shape.valueOf(source.getText()));
                    }
                });

        defineLayout();

        // Output on glyph id selection
        setOutputSelection(sheet.getSelection(VERTICAL_GLYPH_ID));

        // Input on Glyph selection
        setInputSelectionList(
            Collections.singletonList(
                sheet.getSelection(SelectionTag.VERTICAL_GLYPH)));

        // Initially, no focus
        setCurrentShape(null);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // setCurrentShape //
    //-----------------//
    /**
     * Define the new current shape
     *
     * @param currentShape the shape to be considered as current
     */
    public void setCurrentShape (Shape currentShape)
    {
        this.currentShape = currentShape;
        assignedCounter.resetIds();

        if (currentShape != null) {
            // Update the shape button
            selectButton.setText(currentShape.toString());
            selectButton.setIcon(currentShape.getIcon());

            // Count the number of glyphs assigned to current shape
            for (Glyph glyph : sheet.getActiveGlyphs()) {
                if (glyph.getShape() == currentShape) {
                    assignedCounter.addId(glyph.getId());
                }
            }
        } else {
            // Void the shape button
            selectButton.setText("- No Focus -");
            selectButton.setIcon(null);
        }

        assignedCounter.refresh();
    }

    //--------//
    // update //
    //--------//
    /**
     * Notification about selection objects (the shape of a just modified glyph,
     * if not null, is used as the new shape focus)
     *
     * @param selection the notified selection
     * @param hint the processing hint if any
     */
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        switch (selection.getTag()) {
        case VERTICAL_GLYPH :

            if (hint == SelectionHint.GLYPH_MODIFIED) {
                // Use glyph assigned shape as current shape, if not null
                Glyph glyph = (Glyph) selection.getEntity();

                if (glyph.getShape() != null) {
                    setCurrentShape(glyph.getShape());

                    //                } else if (glyph.getOldShape() == currentShape) {
                    //                    setCurrentShape(currentShape);
                }
            }

            break;

        default :
        }
    }

    //-----------//
    // getFilter //
    //-----------//
    /**
     * Report the current filter in action
     *
     * @return current filter value
     */
    Filter getFilter ()
    {
        return (Filter) filterButton.getSelectedItem();
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        final String buttonWidth = Panel.getButtonWidth();
        final String fieldInterval = Panel.getFieldInterval();
        final String fieldInterline = Panel.getFieldInterline();

        FormLayout   layout = new FormLayout(
            buttonWidth + "," + fieldInterval + "," + buttonWidth + "," +
            fieldInterval + "," + buttonWidth + "," + fieldInterval + "," +
            buttonWidth,
            "pref," + fieldInterline + "," + "pref");

        PanelBuilder builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        CellConstraints cst = new CellConstraints();

        int             r = 1; // --------------------------------
        builder.addSeparator("Focus", cst.xyw(1, r, 1));
        builder.add(selectButton, cst.xyw(3, r, 5));

        r += 2; // --------------------------------
        builder.add(filterButton, cst.xy(1, r));

        builder.add(assignedCounter.val, cst.xy(5, r));
        builder.add(assignedCounter.spinner, cst.xy(7, r));
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // Counter //
    //---------//
    private class Counter
        implements ChangeListener
    {
        // Spinner on these glyphs
        ArrayList<Integer> ids = new ArrayList<Integer>();

        // Number of glyphs
        JLabel   val = new JLabel("", SwingConstants.RIGHT);
        JSpinner spinner = new JSpinner(new SpinnerListModel());

        //---------//
        // Counter //
        //---------//
        public Counter ()
        {
            resetIds();
            spinner.addChangeListener(this);
            SpinnerUtilities.setList(spinner, ids);
            refresh();
        }

        //-------//
        // addId //
        //-------//
        public void addId (int id)
        {
            ids.add(id);
        }

        //---------//
        // refresh //
        //---------//
        public void refresh ()
        {
            if (ids.size() > 1) { // To skip first NO_VALUE item
                val.setText(Integer.toString(ids.size() - 1));
                spinner.setEnabled(true);
            } else {
                val.setText("");
                spinner.setEnabled(false);
            }

            spinner.setValue(NO_VALUE);
        }

        //----------//
        // resetIds //
        //----------//
        public void resetIds ()
        {
            ids.clear();
            ids.add(NO_VALUE);
        }

        //--------------//
        // stateChanged //
        //--------------//
        @Implement(ChangeListener.class)
        public void stateChanged (ChangeEvent e)
        {
            int id = (Integer) spinner.getValue();

            if (id != NO_VALUE) {
                outputSelection.setEntity(id, SelectionHint.GLYPH_INIT);
            }
        }
    }
}
