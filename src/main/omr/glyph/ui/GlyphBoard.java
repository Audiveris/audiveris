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
import omr.selection.Selection;
import omr.selection.SelectionHint;
import omr.ui.Board;
import omr.ui.util.Panel;
import omr.ui.field.SField;
import omr.ui.field.SpinnerUtilities;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>GlyphBoard</code> defines a board dedicated to the display
 * of {@link Glyph} information, with several spinners : <ol>
 *
 * <li>The universal <b>id</b> spinner, to browse through <i>all</i> glyphs
 * currently defined in the lag (note that glyphs can be dynamically
 * created or destroyed). This includes all the various (vertical) sticks
 * (which are special glyphs) built during the previous steps, for example
 * the bar lines.
 *
 * <li> The <b>known</b> spinner for known symbols. This is a subset of
 * the previous one.
 *
 * </ol> The ids handled by each of these spinners can dynamically vary,
 * since glyphs can change their status.
 *
 * <p> Any spinner can also be used to select a glyph by directly entering
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

    /** A dump action */
    protected final JButton dump = new JButton("Dump");

    /** Input / Output : spinner of glyph id */
    protected JSpinner gid;

    /** Input / Output : spinner of known glyphs */
    protected JSpinner known;

    /** Input : Deassign button */
    protected DeassignAction deassignAction = new DeassignAction();
    protected JButton deassignButton = new JButton(deassignAction);

    /** Output : shape of the glyph */
    protected final JTextField shape = new SField
        (false, "Assigned shape for this glyph");

    /** The JGoodies/Form layout to be used by all subclasses  */
    protected FormLayout layout = Panel.makeFormLayout(4, 3);

    /** The JGoodies/Form builder to be used by all subclasses  */
    protected PanelBuilder builder;

    /** The JGoodies/Form constraints to be used by all subclasses  */
    protected CellConstraints cst = new CellConstraints();

    // To avoid loop, indicate that update() method is being processed
    protected boolean updating = false;
    protected boolean idSelecting = false;

    //~ Constructors ------------------------------------------------------

    //------------//
    // GlyphBoard //
    //------------//
    /**
     * Create a Glyph Board
     *
     * @param unitName name of the owning unit
     * @param maxGlyphId the upper bound for glyph id
     * @param knownIds   the extended list of ids for known glyphs
     * @param glyphSelection input glyph selection
     * @param glyphIdSelection output glyph Id selection
     */
    public GlyphBoard (String        unitName,
                       int           maxGlyphId,
                       List<Integer> knownIds,
                       Selection     glyphSelection,
                       Selection     glyphIdSelection)
    {
        this(unitName + "-GlyphBoard", maxGlyphId, 
                glyphSelection, glyphIdSelection);

        if (logger.isFineEnabled()) {
            logger.fine("knownIds=" + knownIds);
        }

        known.setModel(new SpinnerListModel(knownIds));
        SpinnerUtilities.setRightAlignment(known);
        SpinnerUtilities.fixIntegerList(known); // For swing bug fix
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
     */
    protected GlyphBoard (String    unitName,
                          int       maxGlyphId,
                          Selection glyphSelection,
                          Selection glyphIdSelection)
    {
        this(unitName);

        setInputSelection(glyphSelection);
        setOutputSelection(glyphIdSelection);

        // Model for id spinner
        gid = new JSpinner();
        gid.setToolTipText("General spinner for any glyph id");
        gid.setModel(new SpinnerNumberModel(0, 0, maxGlyphId, 1));
        gid.addChangeListener(this);

        // Model for known spinner
        known = new JSpinner();
        known.setToolTipText("Specific spinner for relevant known glyphs");
        known.addChangeListener(this);

        // Layout
        int r = 3;                      // --------------------------------
        builder.addLabel("Id",          cst.xy (1,  r));
        builder.add(gid,                cst.xy (3,  r));

        builder.addLabel("Known",       cst.xy (5,  r));
        builder.add(known,              cst.xy (7,  r));
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
                        Selection input = GlyphBoard.this.inputSelection;
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
        builder.addSeparator("Glyph",   cst.xyw(1,  r, 9));
        builder.add(dump,               cst.xy (11, r));

        r += 2;                         // --------------------------------
        r += 2;                         // --------------------------------

        builder.add(deassignButton,     cst.xyw(1, r, 3));
        builder.add(shape,              cst.xyw(5, r, 7));

        deassignButton.setHorizontalTextPosition(SwingConstants.LEFT);
        deassignButton.setHorizontalAlignment(SwingConstants.RIGHT);
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
//        logger.info("GlyphBoard " + selection.getTag()
//                    + " updating=" + updating + " idSelecting=" + idSelecting
//                    + " : " + entity);

        switch (selection.getTag()) {
        case VERTICAL_GLYPH :
        case HORIZONTAL_GLYPH :

            if (updating) {
                ///logger.warning("double updating");
                return;
            }

            Glyph glyph = (Glyph) entity;
            dump.setEnabled(glyph != null);
            deassignAction.setEnabled(glyph != null && glyph.isKnown());
            updating = true;

            if (glyph != null) {
                if (gid != null) {
                    gid.setValue(glyph.getId());
                }

                if (glyph.getShape() != null) {
                    shape.setText(glyph.getShape().toString());
                    deassignButton.setIcon(glyph.getShape().getIcon());
                } else {
                    shape.setText("");
                    deassignButton.setIcon(null);
                }

                // Set known field if shape is one of the desired ones
                if (known != null) {
                    if (glyph.isKnown() &&
                        ((SpinnerListModel) known.getModel())
                        .getList().contains(new Integer(glyph.getId()))) {
                        known.setValue(glyph.getId());
                    } else {
                        known.setValue(NO_VALUE);
                    }
                }
            } else {
                if (gid != null) {
                    gid.setValue(NO_VALUE);
                }

                if (known != null) {
                    known.setValue(NO_VALUE);
                }

                shape.setText("");
                deassignButton.setIcon(null);
            }

            updating = false;
            break;

        default :
            logger.severe("Unexpected selection event from " + selection);
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * CallBack triggered by a change in one of the spinners
     *
     * @param e the change event, this allows to retrieve the originating
     *          spinner
     */
    public void stateChanged(ChangeEvent e)
    {
        if (!updating) {
            if (outputSelection != null) {
                JSpinner spinner = (JSpinner) e.getSource();
                int glyphId = (Integer) spinner.getValue();
                if (logger.isFineEnabled()) {
                        logger.fine("glyphId=" + glyphId);
                }
                idSelecting = true;
                outputSelection.setEntity(glyphId, SelectionHint.GLYPH_INIT);
                idSelecting = false;
            }
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
            Selection input = GlyphBoard.this.inputSelection;
            Glyph glyph = (Glyph) input.getEntity();

            if (glyph != null && glyph.isKnown()) {
                ///////////////// TBD glyphFocus.deassignGlyph(glyph);
            }
        }
    }
}
