//-----------------------------------------------------------------------//
//                                                                       //
//                          G l y p h B o a r d                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.GlyphFocus;
import omr.glyph.GlyphObserver;
import omr.glyph.Shape;
import omr.ui.Board;
import omr.ui.SField;
import omr.util.Logger;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.List;
import java.util.ArrayList;

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
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class GlyphBoard
    extends Board
    implements GlyphObserver,
               ChangeListener
{
    //~ Static variables/initializers -------------------------------------

    private static Logger logger = Logger.getLogger(GlyphBoard.class);

    //~ Instance variables ------------------------------------------------

    /** The glyph displayed */
    protected Glyph glyph;

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

    /** Glyph Focus if any */
    protected GlyphFocus glyphFocus;

    /**
     * To differentiate between an id as selected from the id spinner
     * (which triggers a focus on related glyph), and the simple display of
     * glyph info (with no explicit glyph focus)
     */
    protected volatile boolean focusWanted = true;


    /** The JGoodies/Form layout to be used by all subclasses  */
    protected FormLayout layout = makeFormLayout(5, 3);

    /** The JGoodies/Form builder to be used by all subclasses  */
    protected PanelBuilder builder;

    /** The JGoodies/Form constraints to be used by all subclasses  */
    protected CellConstraints cst = new CellConstraints();

    //~ Constructors ------------------------------------------------------

    //------------//
    // GlyphBoard //
    //------------//
    /**
     * Create a Glyph Board
     *
     * @param maxGlyphId the upper bound for glyph id
     * @param knownIds the extended list of ids for known glyphs
     */
    public GlyphBoard (int           maxGlyphId,
                       List<Integer> knownIds)
    {
        this(maxGlyphId);

        if (logger.isDebugEnabled()) {
            logger.debug("knownIds=" + knownIds);
        }

        known.setModel(new SpinnerListModel(knownIds));
    }

    //------------//
    // GlyphBoard //
    //------------//
    /**
     * Create a Glyph Board
     *
     * @param maxGlyphId the upper bound for glyph id
     */
    protected GlyphBoard (int maxGlyphId)
    {
        this();

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
     */
    protected GlyphBoard()
    {
        super(Board.Tag.GLYPH);

        // Dump action
        dump.setToolTipText("Dump this glyph");
        dump.addActionListener
            (new ActionListener()
                {
                    public void actionPerformed (ActionEvent e)
                    {
                        if (glyph != null) {
                            glyph.dump();
                        }
                    }
                });
        dump.setEnabled(glyph != null);

        // Precise layout
        layout.setColumnGroups(new int[][]{{1, 5, 9}, {3, 7, 11}});

        builder = new PanelBuilder(layout, this);
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
    }

    //---------------//
    // setGlyphFocus //
    //---------------//
    /**
     * Connect an entity to be later notified of glyph focus, as input by a
     * user (when a glyph ID is entered)
     *
     * @param glyphFocus
     */
    public void setGlyphFocus (GlyphFocus glyphFocus)
    {
        this.glyphFocus = glyphFocus;
    }

    //------------//
    // setIdModel //
    //------------//
    /**
     * Change the model for the id spinner
     *
     * @param model the new model
     */
    public void setIdModel (SpinnerModel model)
    {
        gid.setModel(model);
    }

    //--------//
    // update //
    //--------//
    /**
     * Display info about the glyph at hand (its id, its shape, etc)
     *
     * @param glyph the glyph at hand
     */
    public void update (Glyph glyph)
    {
        this.glyph = glyph;
        dump.setEnabled(glyph != null);
        deassignAction.setEnabled(glyph != null && glyph.isKnown());

        focusWanted = false;
        if (glyph != null) {
            if (gid != null) {
                gid.setValue(glyph.getId());
            }

            if (glyph.getShape() != null) {
                shape.setText(glyph.getShape().toString());
            } else {
                shape.setText("");
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
        }

        focusWanted = true;
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * CallBack triggered by a change in one of the spinners
     *
     * @param e the change event, this allows to retrieve the originating
     * spinner
     */
    public void stateChanged(ChangeEvent e)
    {
        if (glyphFocus != null && focusWanted) {
            JSpinner spinner = (JSpinner) e.getSource();
            int glyphId = (Integer) spinner.getValue();

            if (logger.isDebugEnabled()) {
                logger.debug("glyphId=" + glyphId);
            }

            if (glyphId != NO_VALUE) {
                glyphFocus.setFocusGlyph(glyphId);
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
            if (glyphFocus != null && glyph != null && glyph.isKnown()) {
                glyphFocus.deassignGlyph(glyph);
            }
        }
    }
}
