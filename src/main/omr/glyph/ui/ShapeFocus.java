//-----------------------------------------------------------------------//
//                                                                       //
//                          S h a p e F o c u s                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;
import omr.ui.Panel;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.debug.*;
import com.jgoodies.forms.layout.*;

import static omr.ui.Board.NO_VALUE;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

/**
 * Class <code>ShapeFocus</code> handles the shape that receives current
 * focus, and all glyphs whose shape or guess correspond to the focus
 * (for example all treble clefs glyphs if such is the focus)
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ShapeFocus
    extends Panel
{
    //~ Static variables/initializers ----------------------------------

    private static final Logger logger = Logger.getLogger(ShapeFocus.class);

    // Predefined Colors
    /** Color for candidate glyphs whose guess is similar to the current
        shape focus */
    public static final Color focusCandidateColor= Color.yellow;

    /** Color for hiding unknown glyphs when filter is ON */
    public static final Color hiddenColor = Color.white;

    //~ Instance variables ------------------------------------------------

    private final GlyphPane     pane;
    private final Sheet         sheet;
    private final SymbolGlyphView view;

    // Display filter
    private enum Filter
    {
            ALL,                        // Display all glyphs
            KNOWN,                      // Display only known glyphs
            UNKNOWN;                    // Display only unknown glyphs
    }
    private Filter filter = Filter.ALL;

    private FilterAction filterAction = new FilterAction();
    private JButton filterButton = new JButton(filterAction);

    private Shape current;  // The current shape used for display focus

    private SelectAction selectAction = new SelectAction();
    private JButton selectButton = new JButton(selectAction);
    private DeassignAction deassignAction = new DeassignAction();
    private JButton deassignButton = new JButton(deassignAction);
    private AssignedCounter assignedCounter = new AssignedCounter();

    private ConfirmAllAction confirmAllAction = new ConfirmAllAction();
    private JButton confirmAllButton = new JButton(confirmAllAction);
    private ConfirmAction confirmAction = new ConfirmAction();
    private JButton confirmButton = new JButton(confirmAction);
    private CandidateCounter candidateCounter = new CandidateCounter();

    // Specific popup menu to allow shape selection
    private JPopupMenu pm = new JPopupMenu();

    //~ Constructors ------------------------------------------------------

    //------------//
    // ShapeFocus //
    //------------//
    /**
     * Create the instance to handle the shape focus, with pointers to
     * needed companions
     *
     * @param sheet the related sheet
     * @param view the displayed lag view
     * @param pane the master component
     */
    public ShapeFocus (Sheet           sheet,
                       SymbolGlyphView view,
                       GlyphPane       pane)
    {
        this.sheet = sheet;
        this.view  = view;
        this.pane  = pane;

        setNoInsets();

        // Tool Tips
        filterButton.setToolTipText("Toggle display of all/known/unknown" +
                                    " glyphs");
        selectButton.setToolTipText("Select candidate shape");
        confirmAllButton.setToolTipText("Confirm all proposed glyphs as "
                                       + current);
        confirmButton.setToolTipText("Confirm this glyph as " + current);

        // Popup menu
        JMenuItem noFocus  = new JMenuItem("No Focus");
        noFocus.setToolTipText("Cancel any focus");
        noFocus.addActionListener
            (new ActionListener() {
                 public void actionPerformed(ActionEvent e) {
                     setCurrent(null);
                 }
                });
        pm.add(noFocus);
        Shape.addShapeItems(pm, new ActionListener()
            {
                public void actionPerformed (ActionEvent e)
                {
                    JMenuItem source = (JMenuItem) e.getSource();
                    setCurrent(Shape.valueOf(source.getText()));
                }
            });

        // Initially, no focus
        setCurrent(null);

        defineLayout();
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout()
    {
        final String buttonWidth    = Panel.getButtonWidth();
        final String fieldInterval  = Panel.getFieldInterval();
        final String fieldInterline = Panel.getFieldInterline();

        FormLayout layout = new FormLayout
            (buttonWidth + "," + fieldInterval + "," +
             buttonWidth + "," + fieldInterval + "," +
             buttonWidth + "," + fieldInterval + "," +
             buttonWidth,
             "pref," + fieldInterline + "," +
             "pref," + fieldInterline + "," +
             "pref");

        PanelBuilder builder = new PanelBuilder(layout, this);
        builder.setDefaultDialogBorder();
        CellConstraints cst = new CellConstraints();

        int r = 1;                      // --------------------------------
        builder.addSeparator("Focus",   cst.xyw(1,  r, 1));
        builder.add(selectButton,       cst.xyw(3,  r, 5));

        r += 2;                         // --------------------------------
        builder.add(filterButton,       cst.xy (1,  r));
        builder.add(deassignButton,     cst.xy (3,  r));

        builder.add(assignedCounter.val,     cst.xy (5,  r));
        builder.add(assignedCounter.spinner, cst.xy (7,  r));

        r += 2;                         // --------------------------------
        builder.add(confirmAllButton,  cst.xy (1,  r));
        builder.add(confirmButton,     cst.xy (3,  r));

        builder.add(candidateCounter.val,     cst.xy (5,  r));
        builder.add(candidateCounter.spinner, cst.xy (7, r));
    }

    //------------//
    // getCurrent //
    //------------//
    /**
     * Report what the current shape is (which may be null)
     *
     * @return the current shape
     */
    public Shape getCurrent ()
    {
        return current;
    }

    //------------//
    // setCurrent //
    //------------//
    /**
     * Define the new current shape
     *
     * @param current the shape to be considered as current
     */
    public void setCurrent (Shape current)
    {
        this.current = current;
        if (current != null) {
            selectButton.setText(current.toString());
        } else {
            selectButton.setText("No Focus");
            assignedCounter.val.setText("");
            candidateCounter.val.setText("");
        }

        colorizeAllGlyphs();
    }

    //-------------------//
    // colorizeAllGlyphs //
    //-------------------//
    /**
     * Colorize all the glyphs of the sheet
     */
    public void colorizeAllGlyphs ()
    {
        assignedCounter.resetIds();
        candidateCounter.resetIds();

        // Display all glyphs guessed as current focus
        if (current == null) {
            // Normal glyphs display, since there is no focus
            for (SystemInfo system : sheet.getSystems()) {
                for (Glyph glyph : system.getGlyphs()) {
                    colorizeGlyph(glyph);
                }
            }
        } else {
            for (SystemInfo system : sheet.getSystems()) {
                for (Glyph glyph : system.getGlyphs()) {
                    if (glyph.getShape() == null &&
                        glyph.getGuess() == current) {
                        candidateCounter.addId(glyph.getId());
                        view.colorizeGlyph(glyph, focusCandidateColor);
                    } else {
                        if (glyph.getShape() == current) {
                            assignedCounter.addId(glyph.getId());
                        }
                        colorizeGlyph(glyph);
                    }
                }
            }
        }

        assignedCounter.refresh();
        deassignAction.setEnabled(assignedCounter.getId() != NO_VALUE);

        candidateCounter.refresh();
        confirmAllAction.setEnabled(candidateCounter.getIds().size() > 1);
        confirmAction.setEnabled(candidateCounter.getId() != NO_VALUE);

        view.repaint();
    }

    //---------------//
    // colorizeGlyph //
    //---------------//
    private void colorizeGlyph (Glyph glyph)
    {
        switch (filter) {
        case ALL:
            view.colorizeGlyph(glyph);
            break;
        case KNOWN:
            if (glyph.isKnown()) {
                view.colorizeGlyph(glyph);
            } else {
                view.colorizeGlyph(glyph, hiddenColor);
            }
            break;
        case UNKNOWN:
            if (glyph.isKnown()) {
                view.colorizeGlyph(glyph, hiddenColor);
            } else {
                view.colorizeGlyph(glyph);
            }
            break;
        }
    }

    //~ Classes -----------------------------------------------------------

    //---------//
    // Counter //
    //---------//
    private class Counter
        implements ChangeListener

    {
        //~ Instance variables --------------------------------------------

        // Number of glyphs
        JLabel val = new JLabel("", SwingConstants.CENTER);

        // Spinner on these glyphs
        ArrayList<Integer> ids = new ArrayList<Integer>();
        JSpinner spinner = new JSpinner();

        //~ Constructors --------------------------------------------------

        //---------//
        // Counter //
        //---------//
        public Counter()
        {
            spinner.addChangeListener(this);
        }

        //~ Methods -------------------------------------------------------

        //--------------//
        // stateChanged //
        //--------------//
        public void stateChanged (ChangeEvent e)
        {
            int id = (Integer) spinner.getValue();
            if (id != NO_VALUE) {
                view.setFocusGlyph(id);
                Glyph glyph = pane.getEntity(id);
                pane.getEvaluatorsPanel().evaluate(glyph);
            }
        }

        //----------//
        // resetIds //
        //----------//
        public void resetIds()
        {
            ids.clear();
            ids.add(NO_VALUE);
        }

        //-------//
        // addId //
        //-------//
        public void addId (int id)
        {
            ids.add(id);
        }

        //-------//
        // getId //
        //-------//
        public int getId()
        {
            return (Integer) spinner.getValue();
        }

        //--------//
        // getIds //
        //--------//
        public ArrayList<Integer> getIds()
        {
            return ids;
        }

        //---------//
        // refresh //
        //---------//
        public void refresh()
        {
            val.setText(Integer.toString(ids.size() -1));
            spinner.setModel(new SpinnerListModel(ids));
        }
    }

    //-----------------//
    // AssignedCounter //
    //-----------------//
    private class AssignedCounter
        extends Counter
    {
        //--------------//
        // stateChanged //
        //--------------//
        public void stateChanged (ChangeEvent e)
        {
            super.stateChanged(e);
            deassignAction.setEnabled(getId() != NO_VALUE);
        }

        //----------//
        // deassign //
        //----------//
        public void deassign()
        {
            int id = getId();
            Integer nextId = (Integer) spinner.getNextValue();
            if (id != NO_VALUE) {
                Glyph glyph = pane.getEntity(id);
                pane.setShape(glyph, null, /* UpdateUI => */ true);
                colorizeAllGlyphs();
                val.setText(Integer.toString(ids.size() -1));

                // Move to next id if any
                if (nextId != null) {
                    spinner.setValue(nextId);
                }
            }
        }
    }

    //------------------//
    // CandidateCounter //
    //------------------//
    private class CandidateCounter
        extends Counter
    {
        //--------------//
        // stateChanged //
        //--------------//
        public void stateChanged (ChangeEvent e)
        {
            super.stateChanged(e);
            confirmAction.setEnabled(getId() != NO_VALUE);
        }

        //---------//
        // confirm //
        //---------//
        public void confirm()
        {
            int id = getId();
            Integer nextId = (Integer) spinner.getNextValue();
            if (id != NO_VALUE) {
                Glyph glyph = pane.getEntity(id);
                pane.setShape(glyph, current, /* UpdateUI => */ true);
                val.setText(Integer.toString(ids.size() -1));

                // Move to next id if any
                if (nextId != null) {
                    spinner.setValue(nextId);
                }
            }
        }

        //------------//
        // confirmAll //
        //------------//
        public void confirmAll()
        {
            for (int id : ids) {
                if (id != NO_VALUE) {
                    Glyph glyph = pane.getEntity(id);
                    pane.setShape(glyph, current, /* UpdateUI => */ false);
                }
            }
            logger.info((ids.size() -1) + " candidates confirmed");
            colorizeAllGlyphs();
        }
    }

    //--------------//
    // FilterAction //
    //--------------//
    private class FilterAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public FilterAction()
        {
            super(filter.name());
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed(ActionEvent e)
        {
            // Determine next value for the display filter
            Filter[] values = Filter.values();
            int ord = filter.ordinal();
            ord++;
            if (ord >= values.length) {
                ord = 0;
            }
            filter = values[ord];

            // Update filter button accordingly
            filterButton.setText(filter.name());

            // Update the display
            colorizeAllGlyphs();
        }
    }

    //--------------//
    // SelectAction //
    //--------------//
    private class SelectAction
        extends AbstractAction
    {
        //~ Methods -------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            pm.show(ShapeFocus.this,
                    selectButton.getX(), selectButton.getY());
        }
    }

    //----------------//
    // DeassignAction //
    //----------------//
    private class DeassignAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public DeassignAction()
        {
            super("Deassign");
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed(ActionEvent e)
        {
            assignedCounter.deassign();
        }
    }

    //---------------//
    // ConfirmAction //
    //---------------//
    private class ConfirmAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public ConfirmAction()
        {
            super("Confirm");
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed(ActionEvent e)
        {
            candidateCounter.confirm();
        }
    }

    //------------------//
    // ConfirmAllAction //
    //------------------//
    private class ConfirmAllAction
        extends AbstractAction
    {
        //~ Constructors --------------------------------------------------

        public ConfirmAllAction()
        {
            super("Confirm All");
        }

        //~ Methods -------------------------------------------------------

        public void actionPerformed(ActionEvent e)
        {
            candidateCounter.confirmAll();
        }
    }
}
