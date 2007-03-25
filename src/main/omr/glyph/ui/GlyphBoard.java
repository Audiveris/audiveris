//----------------------------------------------------------------------------//
//                                                                            //
//                            G l y p h B o a r d                             //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphModel;
import omr.glyph.Shape;

import omr.selection.Selection;
import omr.selection.SelectionHint;

import omr.sheet.Sheet;

import omr.ui.Board;
import omr.ui.field.SField;
import omr.ui.field.SpinnerUtilities;
import static omr.ui.field.SpinnerUtilities.*;
import omr.ui.util.Panel;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.Predicate;

import com.jgoodies.forms.builder.*;
import com.jgoodies.forms.layout.*;

import java.awt.Dimension;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>GlyphBoard</code> defines a board dedicated to the display of
 * {@link Glyph} information, with several spinners : <ol>
 *
 * <li>The universal <b>globalSpinner</b>, to browse through <i>all</i> glyphs
 * currently defined in the lag (note that glyphs can be dynamically created or
 * destroyed). This includes all the various (vertical) sticks (which are
 * special glyphs) built during the previous steps, for example the bar
 * lines. For other instances (such as for HorizontalsBuilder), these would be
 * horizontal sticks.
 *
 * <li>The <b>knownSpinner</b> for known symbols (that is with a defined shape).
 * This spinner is a subset of the globalSpinner.
 *
 * </ol>The ids handled by each of these spinners can dynamically vary, since
 * glyphs can change their status.
 *
 * <p>Any spinner can also be used to select a glyph by directly entering the
 * glyph id value into the spinner field
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
    implements ChangeListener // For all spinners

{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphBoard.class);

    //~ Instance fields --------------------------------------------------------

    /** The related glyph model */
    protected final GlyphModel glyphModel;

    /** An active label */
    protected final JLabel active = new JLabel("");

    /** A dump action */
    protected final JButton dump = new JButton("Dump");

    /** Counter of glyph selection */
    protected final JLabel count = new JLabel("");

    /** Input : Deassign action */
    protected DeassignAction deassignAction = new DeassignAction();

    /** Button to trigger deassignAction */
    protected JButton deassignButton = new JButton(deassignAction);

    /** Output : glyph shape icon */
    protected JLabel shapeIcon = new JLabel();

    /** Input / Output : spinner of all glyphs */
    protected JSpinner globalSpinner;

    /** Input / Output : spinner of known glyphs */
    protected JSpinner knownSpinner;

    /** Predicate for known glyphs */
    protected Predicate<Glyph> knownPredicate = new Predicate<Glyph>() {
        public boolean check (Glyph glyph)
        {
            return (glyph != null) && glyph.isKnown();
        }
    };

    /** Output : shape of the glyph */
    protected final JTextField shapeField = new SField(
        false,
        "Assigned shape for this glyph");

    /** The JGoodies/Form constraints to be used by all subclasses  */
    protected CellConstraints cst = new CellConstraints();

    /** The JGoodies/Form layout to be used by all subclasses  */
    protected FormLayout layout = Panel.makeFormLayout(5, 3);

    /** The JGoodies/Form builder to be used by all subclasses  */
    protected PanelBuilder builder;

    /**
     * We have to avoid endless loop, due to related modifications : When a
     * GLYPH selection is notified, the id spinner is changed, and When an id
     * spinner is changed, the GLYPH selection is notified
     */
    protected boolean selfUpdating = false;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // GlyphBoard //
    //------------//
    /**
     * Create a Glyph Board
     *
     * @param unitName name of the owning unit
     * @param glyphModel the underlying glyph model
     * @param specificGlyphs additional collection of glyphs, or null
     * @param glyphSelection input glyph selection
     * @param glyphIdSelection output glyph Id selection
     * @param glyphSetSelection input glyph set selection
     */
    public GlyphBoard (String                     unitName,
                       GlyphModel                 glyphModel,
                       Collection<?extends Glyph> specificGlyphs,
                       Selection                  glyphSelection,
                       Selection                  glyphIdSelection,
                       Selection                  glyphSetSelection)
    {
        this(unitName, glyphModel);

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
        globalSpinner = makeGlyphSpinner(
            glyphModel.getLag(),
            specificGlyphs,
            null);
        globalSpinner.setName("globalSpinner");
        globalSpinner.setToolTipText("General spinner for any glyph id");

        // Model for knownSpinner
        knownSpinner = makeGlyphSpinner(
            glyphModel.getLag(),
            specificGlyphs,
            knownPredicate);
        knownSpinner.setName("knownSpinner");
        knownSpinner.setToolTipText("Specific spinner for known glyphs");

        // Layout
        int r = 3; // --------------------------------
        builder.addLabel("Id", cst.xy(1, r));
        builder.add(globalSpinner, cst.xy(3, r));

        builder.addLabel("Known", cst.xy(5, r));
        builder.add(knownSpinner, cst.xy(7, r));
    }

    //------------//
    // GlyphBoard //
    //------------//
    /**
     * Basic constructor, to set common characteristics
     *
     * @param name the name assigned to this board instance
     * @param glyphModel the related glyph model, if any
     */
    protected GlyphBoard (String     name,
                          GlyphModel glyphModel)
    {
        super(Board.Tag.GLYPH, name);

        this.glyphModel = glyphModel;

        // Dump action
        dump.setToolTipText("Dump this glyph");
        dump.addActionListener(
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        // Retrieve current glyph selection
                        Selection input = inputSelectionList.get(0);
                        Glyph     glyph = (Glyph) input.getEntity();

                        if (glyph != null) {
                            glyph.dump();
                        }
                    }
                });

        // Until a glyph selection is made
        dump.setEnabled(false);
        deassignButton.setEnabled(false);

        // Force a constant height for the shapeIcon field, despite the
        // variation in size of the icon
        Dimension dim = new Dimension(
            constants.shapeIconWidth.getValue(),
            constants.shapeIconHeight.getValue());
        shapeIcon.setPreferredSize(dim);
        shapeIcon.setMaximumSize(dim);
        shapeIcon.setMinimumSize(dim);

        // Precise layout
        layout.setColumnGroups(
            new int[][] {
                { 1, 5, 9 },
                { 3, 7, 11 }
            });

        builder = new PanelBuilder(layout, getComponent());
        builder.setDefaultDialogBorder();

        defineLayout();
    }

    //~ Methods ----------------------------------------------------------------

    //-------------------//
    // getDeassignButton //
    //-------------------//
    /**
     * Give access to the Deassign Button, to modify its properties
     *
     * @return the deassign button
     */
    public JButton getDeassignButton ()
    {
        return deassignButton;
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
    @Implement(ChangeListener.class)
    public void stateChanged (ChangeEvent e)
    {
        JSpinner spinner = (JSpinner) e.getSource();

        //  Nota: this method is automatically called whenever the spinner value
        //  is changed, including when a GLYPH selection notification is
        //  received leading to such selfUpdating. So the check.
        if (!selfUpdating && (outputSelection != null)) {
            // Notify the new glyph id
            outputSelection.setEntity(
                (Integer) spinner.getValue(),
                SelectionHint.GLYPH_INIT);
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
    public void update (Selection     selection,
                        SelectionHint hint)
    {
        Object entity = selection.getEntity();

        if (logger.isFineEnabled()) {
            logger.info(
                "GlyphBoard " + selection.getTag() + " selfUpdating=" +
                selfUpdating + " : " + entity);
        }

        switch (selection.getTag()) {
        case VERTICAL_GLYPH :
        case HORIZONTAL_GLYPH :
            // Display Glyph parameters (while preventing circular updates)
            selfUpdating = true;

            Glyph glyph = (Glyph) entity;

            // Active ?
            if (glyph != null) {
                if (glyph.isActive()) {
                    active.setText("Active");
                } else {
                    active.setText("Non Active");
                }
            } else {
                active.setText("");
            }

            // Dump button and deassign button
            dump.setEnabled(glyph != null);
            deassignButton.setEnabled((glyph != null) && glyph.isKnown());

            // Shape text and icon
            Shape shape = (glyph != null) ? glyph.getShape() : null;

            if (shape != null) {
                shapeField.setText(shape.toString());
                shapeIcon.setIcon(shape.getIcon());
            } else {
                shapeField.setText("");
                shapeIcon.setIcon(null);
            }

            // Global Spinner
            if (globalSpinner != null) {
                if (glyph != null) {
                    globalSpinner.setValue(glyph.getId());
                } else {
                    globalSpinner.setValue(NO_VALUE);
                }
            }

            // Known Spinner
            if (knownSpinner != null) {
                if (glyph != null) {
                    knownSpinner.setValue(
                        knownPredicate.check(glyph) ? glyph.getId() : NO_VALUE);
                } else {
                    knownSpinner.setValue(NO_VALUE);
                }
            }

            selfUpdating = false;

            break;

        case GLYPH_SET :

            // Display count of glyphs in the glyph set
            List<Glyph> glyphs = (List<Glyph>) entity; // Compiler warning

            if ((glyphs != null) && (glyphs.size() > 0)) {
                count.setText(Integer.toString(glyphs.size()));
            } else {
                count.setText("");
            }

            break;

        default :
            logger.severe("Unexpected selection event from " + selection);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout for common fields of all GlyphBoard classes
     */
    protected void defineLayout ()
    {
        int r = 1; // --------------------------------
        builder.addSeparator("Glyph", cst.xyw(1, r, 6));
        builder.add(active, cst.xy(7, r));
        builder.add(count, cst.xy(9, r));
        builder.add(dump, cst.xy(11, r));

        r += 2; // --------------------------------
        r += 2; // --------------------------------

        builder.add(shapeIcon, cst.xy(1, r));
        builder.add(deassignButton, cst.xy(3, r));
        builder.add(shapeField, cst.xyw(5, r, 7));

        deassignButton.setHorizontalTextPosition(SwingConstants.LEFT);
        deassignButton.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    //------------------//
    // makeGlyphSpinner //
    //------------------//
    /**
     * Convenient method to allocate a glyph-based spinner
     *
     * @param lag the underlying glyph lag
     * @param specificGlyphs additional specific glyph collection, or null
     * @param predicate a related glyph predicate, if any
     * @return the spinner built
     */
    protected JSpinner makeGlyphSpinner (GlyphLag                   lag,
                                         Collection<?extends Glyph> specificGlyphs,
                                         Predicate<Glyph>           predicate)
    {
        JSpinner spinner = new JSpinner();
        spinner.setModel(new SpinnerGlyphModel(lag, specificGlyphs, predicate));
        spinner.addChangeListener(this);
        SpinnerUtilities.setRightAlignment(spinner);
        SpinnerUtilities.setEditable(spinner, true);

        return spinner;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Exact pixel height for the shape icon field */
        Constant.Integer shapeIconHeight = new Constant.Integer(
            70,
            "Exact pixel height for the shape icon field");

        /** Exact pixel width for the shape icon field */
        Constant.Integer shapeIconWidth = new Constant.Integer(
            50,
            "Exact pixel width for the shape icon field");
    }

    //----------------//
    // DeassignAction //
    //----------------//
    private class DeassignAction
        extends AbstractAction
    {
        public DeassignAction ()
        {
            super("Deassign");
        }

        @Implement(ChangeListener.class)
        public void actionPerformed (ActionEvent e)
        {
            if (glyphModel != null) {
                if (inputSelectionList.size() > 1) {
                    Selection   glyphSelection = inputSelectionList.get(0);
                    Glyph       glyph = (Glyph) glyphSelection.getEntity();
                    Selection   glyphSetSelection = inputSelectionList.get(1);
                    List<Glyph> glyphs = (List<Glyph>) glyphSetSelection.getEntity();
                    glyphModel.deassignSetShape(glyphs);

                    Sheet sheet = glyphModel.getSheet();

                    if (sheet != null) {
                        sheet.updateSteps();
                    }

                    // Update focus on current glyph, if reused in a compound
                    Glyph newGlyph = glyph.getFirstSection()
                                          .getGlyph();

                    if (glyph != newGlyph) {
                        glyphSelection.setEntity(
                            newGlyph,
                            SelectionHint.GLYPH_INIT);
                    }
                } else if (inputSelectionList.size() == 1) {
                    glyphModel.deassignGlyphShape(
                        (Glyph) inputSelectionList.get(0).getEntity());

                    Sheet sheet = glyphModel.getSheet();

                    if (sheet != null) {
                        sheet.updateSteps();
                    }
                }
            }
        }
    }
}
