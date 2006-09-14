//-----------------------------------------------------------------------//
//                                                                       //
//                          G l y p h B o a r d                          //
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
import omr.selection.SelectionHint;
import omr.ui.Board;
import omr.ui.field.SField;
import omr.ui.field.SpinnerUtilities;
import omr.ui.util.Panel;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>GlyphBoard</code> defines a board dedicated to the display
 * of {@link Glyph} information, with several spinners : <ol>
 *
 * <li>The universal <b>globalSpinner</b>, to browse through <i>all</i>
 * glyphs currently defined in the lag (note that glyphs can be dynamically
 * created or destroyed). This includes all the various (vertical) sticks
 * (which are special glyphs) built during the previous steps, for example
 * the bar lines. For other instances (such as for HorizontalsBuilder),
 * these would be horizontal sticks.
 *
 * <li>The <b>knownSpinner</b> for known symbols (that is with a defined
 * shape) that are of interest for the board (e.g. just the lines for
 * LinesBuider, and just the stems for VerticalsBuilder). This spinner is a
 * subset of the globalSpinner.
 *
 * </ol>The ids handled by each of these spinners can dynamically vary,
 * since glyphs can change their status.
 *
 * <p>Any spinner can also be used to select a glyph by directly entering
 * the glyph id value into the spinner field
 *
 * <dl>
 * <dt><b>Selection Inputs:</b></dt><ul>
 * <li>*_GLYPH
 * </ul>
 *
 * <dt><b>Selection Outputs:</b></dt><ul>
 * <li>*_GLYPH_ID (flagged with GLYPH_INIT hint)
 * </ul>
 * </dl>
 *
 *
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphBoard
    extends Board
    implements ChangeListener   // For all spinners
{
    //~ Static variables/initializers -------------------------------------

    private static Logger logger = Logger.getLogger(GlyphBoard.class);

    //~ Instance variables ------------------------------------------------

    /** Counter of glyph selection */
    protected final JLabel count = new JLabel("");

    /** A dump action */
    protected final JButton dump = new JButton("Dump");

    /** Input / Output : spinner of all glyphs */
    protected JSpinner globalSpinner;

    /** Input / Output : spinner of known glyphs */
    protected JSpinner knownSpinner;

    /** Input : Deassign button */
    protected DeassignAction deassignAction = new DeassignAction();
    protected JButton deassignButton = new JButton(deassignAction);

    /** Output : shape of the glyph */
    protected final JTextField shapeField = new SField
        (false, "Assigned shape for this glyph");

    /** The JGoodies/Form layout to be used by all subclasses  */
    protected FormLayout layout = Panel.makeFormLayout(4, 3);

    /** The JGoodies/Form builder to be used by all subclasses  */
    protected PanelBuilder builder;

    /** The JGoodies/Form constraints to be used by all subclasses  */
    protected CellConstraints cst = new CellConstraints();

    // We have to avoid endless loop, due to related modifications :
    // - When a GLYPH selection is notified, the id spinner is changed
    // - When an id spinner is changed, the GLYPH selection is notified
    protected boolean selfUpdating = false;

    //~ Constructors ------------------------------------------------------

    //------------//
    // GlyphBoard //
    //------------//
    /**
     * Create a Glyph Board
     *
     *
     * @param unitName name of the owning unit
     * @param maxGlyphId the upper bound for glyph id
     * @param knownIds   the extended list of ids for known glyphs
     * @param glyphSelection input glyph selection
     * @param glyphIdSelection output glyph Id selection
     * @param glyphSetSelection input glyph set selection
     */
    public GlyphBoard (String        unitName,
                       int           maxGlyphId,
                       List<Integer> knownIds,
                       Selection     glyphSelection,
                       Selection     glyphIdSelection,
                       Selection     glyphSetSelection)
    {
        this(unitName + "-GlyphBoard", maxGlyphId,
                glyphSelection, glyphIdSelection, glyphSetSelection);

        if (logger.isFineEnabled()) {
            logger.fine("knownIds=" + knownIds);
        }

        knownSpinner.setModel(new SpinnerListModel(knownIds));
        SpinnerUtilities.setRightAlignment(knownSpinner);
        SpinnerUtilities.fixIntegerList(knownSpinner); // For swing bug fix
    }

    //------------//
    // GlyphBoard //
    //------------//
    /**
     * Create a Glyph Board
     *
     * @param unitName name of the owning unit
     * @param maxGlyphId the upper bound for glyph id
     * @param glyphSelection input glyph selection
     * @param glyphIdSelection output glyph Id selection
     * @param glyphSetSelection input glyph set selection
     */
    protected GlyphBoard (String    unitName,
                          int       maxGlyphId,
                          Selection glyphSelection,
                          Selection glyphIdSelection,
                          Selection glyphSetSelection)
    {
        this(unitName);

        ArrayList<Selection> inputs = new ArrayList<Selection>();
        if (glyphSelection != null) {
            inputs.add(glyphSelection);
        }
        if (glyphSetSelection != null) {
            inputs.add(glyphSetSelection);
        }
        setInputSelectionList(inputs);
        setOutputSelection(glyphIdSelection);

        // Model for globalSpinner
        globalSpinner = new JSpinner();
        globalSpinner.setName("globalSpinner");
        globalSpinner.setToolTipText("General spinner for any glyph id");
        globalSpinner.setModel(new SpinnerNumberModel(0, 0, maxGlyphId, 1));
        globalSpinner.addChangeListener(this);

        // Model for knownSpinner
        knownSpinner = new JSpinner();
        knownSpinner.setName("knownSpinner");
        knownSpinner.setToolTipText("Specific spinner for relevant known glyphs");
        knownSpinner.addChangeListener(this);

        // Layout
        int r = 3;                      // --------------------------------
        builder.addLabel("Id",          cst.xy (1,  r));
        builder.add(globalSpinner,      cst.xy (3,  r));

        builder.addLabel("Known",       cst.xy (5,  r));
        builder.add(knownSpinner,       cst.xy (7,  r));
    }

    //------------//
    // GlyphBoard //
    //------------//
    /**
     * Basic constructor, to set common characteristics
     *
     * @param name the name assigned to this board instance
     */
    protected GlyphBoard (String name)
    {
        super(Board.Tag.GLYPH, name);

        // Dump action
        dump.setToolTipText("Dump this glyph");
        dump.addActionListener
            (new ActionListener()
                {
                    public void actionPerformed (ActionEvent e)
                    {
                        // retrieve current glyph selection
                        Selection input
                                = GlyphBoard.this.inputSelectionList.get(0);
                        Glyph glyph = (Glyph) input.getEntity();
                        if (glyph != null) {
                            glyph.dump();
                        }
                    }
                });
        dump.setEnabled(false); // Until a glyph selection is made

        // Precise layout
        layout.setColumnGroups(new int[][]{{1, 5, 9}, {3, 7, 11}});

        builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        defineLayout();
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout for common fields of all GlyphBoard classes
     */
    protected void defineLayout()
    {
        int r = 1;                      // --------------------------------
        builder.addSeparator("Glyph",   cst.xyw(1,  r, 7));
        builder.add(count,              cst.xy (9, r));
        builder.add(dump,               cst.xy (11, r));

        r += 2;                         // --------------------------------
        r += 2;                         // --------------------------------

        builder.add(deassignButton,     cst.xyw(1, r, 3));
        builder.add(shapeField,         cst.xyw(5, r, 7));

        deassignButton.setHorizontalTextPosition(SwingConstants.LEFT);
        deassignButton.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    //---------------//
    // trySetSpinner //
    //---------------//
    /**
     * Assign value to an id spinner, after checking the id is part of the
     * spinner model
     *
     * @param spinner the spinner whose value is to be set
     * @param id the id value
     */
    protected void trySetSpinner(JSpinner spinner,
                                 int      id)
    {
        // Make sure we have a spinner entity
        if (spinner == null) {
            return;
        }

        SpinnerModel model = spinner.getModel();
        if (model instanceof SpinnerListModel) {
            SpinnerListModel listModel = (SpinnerListModel) model;
            if (listModel.getList().contains(new Integer(id))) {
                spinner.setValue(id);
            } else {
                logger.warning(getName() + " " + spinner.getName() +
                               ": no list slot for id " + id);
                spinner.setValue(NO_VALUE);
            }
        } else if (model instanceof SpinnerNumberModel) {
            SpinnerNumberModel numberModel = (SpinnerNumberModel) model;
            if (numberModel.getMaximum().compareTo(id) >= 0) {
                spinner.setValue(id);
            } else {
                logger.warning(getName() + " " + spinner.getName() +
                               ": no number slot for id " + id);
                spinner.setValue(NO_VALUE);
            }
        } else {
            logger.warning(spinner.getName() + ": no known model !!!");
            spinner.setValue(id);
        }
    }

    //--------------//
    // alterSpinner //
    //--------------//
    protected void alterSpinner (JSpinner spinner,
                                 Glyph    glyph,
                                 boolean  modified)
    {
        // Make sure we have a spinner entity
        if (spinner == null) {
            return;
        }

        SpinnerModel model = spinner.getModel();
        if (model instanceof SpinnerListModel) {
            SpinnerListModel listModel = (SpinnerListModel) model;
//            if (listModel.getList().contains(glyph.getId())) {
//                spinner.setValue(id);
//            } else if (modified) {
//                listModel.getList().add(glyph.getId());
//            } else {
//                logger.warning(getName() + " " + spinner.getName() +
//                               ": no list slot for id " + id);
//                spinner.setValue(NO_VALUE);
//            }
        } else if (model instanceof SpinnerNumberModel) {
            SpinnerNumberModel numberModel = (SpinnerNumberModel) model;
//            if (numberModel.getMaximum().compareTo(id) >= 0) {
//                spinner.setValue(id);
//            } else {
//                logger.warning(getName() + " " + spinner.getName() +
//                               ": no number slot for id " + id);
//                spinner.setValue(NO_VALUE);
//            }
        } else {
            logger.warning(spinner.getName() + ": no known model !!!");
//            spinner.setValue(id);
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * CallBack triggered by a change in one of the spinners.
     *
     * @param e the change event, this allows to retrieve the originating
     *          spinner
     */
    public void stateChanged(ChangeEvent e)
    {
        JSpinner spinner = (JSpinner) e.getSource();

        //  Nota: this method is automatically called whenever the spinner
        //  value is changed, including when a GLYPH selection notification
        //  is received leading to such selfUpdating. So the check.
        if (!selfUpdating) {
            ///logger.info("GB stateChanged. proceeding... " + spinner.getName());
            if (outputSelection != null) {
                // Notify the new glyph id
                outputSelection.setEntity((Integer) spinner.getValue(),
                                          SelectionHint.GLYPH_INIT);
            }
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
        Object entity = selection.getEntity();
//      logger.info("GlyphBoard " + selection.getTag()
//                  + " selfUpdating=" + selfUpdating
//                  + " : " + entity);
        switch (selection.getTag()) {
        case VERTICAL_GLYPH :
        case HORIZONTAL_GLYPH :
            // Display Glyph parameters (while preventing circular updates)
            selfUpdating = true;
            Glyph glyph = (Glyph) entity;

            // Dump button and deassign button
            dump.setEnabled(glyph != null);
            deassignAction.setEnabled(glyph != null && glyph.isKnown());

            // Shape text and icon
            Shape shape = (glyph != null) ? glyph.getShape() : null;
            if (shape != null) {
                shapeField.setText(shape.toString());
                deassignButton.setIcon(shape.getIcon());
            } else {
                shapeField.setText("");
                deassignButton.setIcon(null);
            }

            // Global & Known Spinners
            if (glyph != null) {
                // Update the models ?
                if (hint == SelectionHint.GLYPH_MODIFIED) {
                    ///alterSpinner(globalSpinner, glyph);
                    if (glyph.isKnown()) {

                    } else {

                    }
                }

                // Beware Stem glyph Id is not known ?
                trySetSpinner(globalSpinner, glyph.getId());

                // Set knownSpinner field if shape is one of the desired ones
                trySetSpinner(knownSpinner,
                        glyph.isKnown() ? glyph.getId() : NO_VALUE);
            } else {
                if (globalSpinner != null) {
                    globalSpinner.setValue(NO_VALUE);
                }

                if (knownSpinner != null) {
                    knownSpinner.setValue(NO_VALUE);
                }
            }

            selfUpdating = false;
            break;

        case GLYPH_SET :
            // Display count of glyphs in the glyph set
            List<Glyph> glyphs = (List<Glyph>) entity;
            if (glyphs != null && glyphs.size() > 0) {
                count.setText(Integer.toString(glyphs.size()));
            } else {
                count.setText("");
            }
            break;

        default :
            logger.severe("Unexpected selection event from " + selection);
        }
    }

    //-------------------//
    // getDeassignButton //
    //-------------------//
    /**
     * Give access to the Deassign Button, to modify its properties
     *
     * @return the deassign button
     */
    public JButton getDeassignButton()
    {
        return deassignButton;
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
            Selection glyphSelection = GlyphBoard.this.inputSelectionList.get(0);
            Glyph glyph = (Glyph) glyphSelection.getEntity();

            if (glyph != null && glyph.isKnown()) {
                // Notify the new glyph info
                glyph.setShape(null);
                glyphSelection.setEntity(glyph, SelectionHint.GLYPH_MODIFIED);
            }
        }
    }
}
