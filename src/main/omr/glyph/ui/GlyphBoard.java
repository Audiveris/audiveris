//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G l y p h B o a r d                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Symbol.Group;

import omr.ui.selection.EntityListEvent;
import omr.ui.selection.GroupEvent;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.UserEvent;

import omr.ui.Board;
import omr.ui.EntityBoard;
import omr.ui.PixelCount;
import omr.ui.util.Panel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;

/**
 * Class {@code GlyphBoard} defines a UI board dedicated to the display of {@link Glyph}
 * information.
 *
 * @author Hervé Bitteur
 */
public class GlyphBoard
        extends EntityBoard<Glyph>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(GlyphBoard.class);

    /** Events this board is interested in.
     * TODO: not correctly used, need GroupEvent!!!!!!!!!!!!!!!!!!!!!! */
    private static final Class<?>[] eventClasses = new Class<?>[]{
        EntityListEvent.class, GroupEvent.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related glyph model. */
    protected final GlyphsController controller;

    /** Input : group selection. */
    protected final JSpinner groupSpinner;

    /** Input : Deassign action. */
    protected Action deassignAction;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Basic constructor, to set common characteristics.
     *
     * @param controller  the related glyphs controller, if any
     * @param useSpinners true for use of spinners
     * @param expanded    true if board must be initially expanded
     */
    public GlyphBoard (GlyphsController controller,
                       boolean useSpinners,
                       boolean expanded)
    {
        super(Board.GLYPH, controller.getNest().getEntityService(), expanded);

        this.controller = controller;

        getDeassignAction().setEnabled(false);

        // Force a constant height for the shapeIcon field, despite variation in size of the icon
        //        Dimension dim = new Dimension(
        //                constants.shapeIconWidth.getValue(),
        //                constants.shapeIconHeight.getValue());
        //        shapeIcon.setPreferredSize(dim);
        //        shapeIcon.setMaximumSize(dim);
        //        shapeIcon.setMinimumSize(dim);
        //
        //         Precise layout
        //        layout.setColumnGroups(
        //            new int[][] {
        //                { 1, 5, 9 },
        //                { 3, 7, 11 }
        //            });
        // Group spinner
        groupSpinner = new JSpinner(new SpinnerListModel(Group.values()));
        groupSpinner.addChangeListener(this);
        groupSpinner.setName("groupSpinner");
        groupSpinner.setToolTipText("Selection of glyph group");

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------------//
    // getDeassignAction //
    //-------------------//
    /**
     * Give access to the Deassign Action, to modify its properties
     *
     * @return the deassign action
     */
    public Action getDeassignAction ()
    {
        if (deassignAction == null) {
            deassignAction = new DeassignAction();
        }

        return deassignAction;
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Glyph Selection has been modified
     *
     * @param event of current glyph or glyph set
     */
    @Override
    public void onEvent (UserEvent event)
    {
        logger.debug("GlyphBoard event:{}", event);

        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            super.onEvent(event); // count, vip, dump, id

            if (event instanceof GroupEvent) {
                // Change group
                handleEvent((GroupEvent) event);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //--------------//
    // stateChanged //
    //--------------//
    /**
     * CallBack triggered by a change in one of the spinners.
     *
     * @param e the change event, this allows to retrieve the originating spinner
     */
    @Override
    public void stateChanged (ChangeEvent e)
    {
        JSpinner spinner = (JSpinner) e.getSource();

        if (spinner == groupSpinner) {
//            getSelectionService().publish(
//                    new GroupEvent(this, SelectionHint.ENTITY_INIT, null, (Group) spinner.getValue()));
        } else {
            super.stateChanged(e);
        }
    }

    //---------------//
    // getFormLayout //
    //---------------//
    @Override
    protected FormLayout getFormLayout ()
    {
        return Panel.makeFormLayout(6, 3);
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout for common fields of all GlyphBoard classes
     */
    private void defineLayout ()
    {
        final CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------
        // Shape Icon (start, spans several rows) + layer + Deassign button

        ///builder.add(shapeIcon, cst.xywh(1, r, 1, 5));
        builder.add(groupSpinner, cst.xyw(5, r, 3));

        JButton deassignButton = new JButton(getDeassignAction());
        deassignButton.setHorizontalTextPosition(SwingConstants.LEFT);
        deassignButton.setHorizontalAlignment(SwingConstants.RIGHT);
        builder.add(deassignButton, cst.xyw(9, r, 3));

        r += 2; // --------------------------------
    }

    //
    //    //-------------//
    //    // handleEvent //
    //    //-------------//
    //    /**
    //     * Interest in GlyphList
    //     *
    //     * @param glyphsEvent
    //     */
    //    private void handleEvent (EntityListEvent<Glyph> glyphsEvent)
    //    {
    //        List<Glyph> glyphs = glyphsEvent.getData();
    //
    //        // Display count of glyphs in the glyph list
    //        if ((glyphs != null) && !glyphs.isEmpty()) {
    //            count.setText(Integer.toString(glyphs.size()));
    //
    //            // Display Glyph parameters
    //            Glyph glyph = glyphs.get(glyphs.size() - 1);
    //
    //            ///getDeassignAction().setEnabled((glyph != null) && glyph.isKnown());
    //            //
    //            //        // Shape text and icon
    //            //        Shape shape = (glyph != null) ? glyph.getShape() : null;
    //            //
    //            //        if (shape != null) {
    //            //            if ((shape == Shape.GLYPH_PART) && (glyph.getPartOf() != null)) {
    //            //                shapeField.setText(shape + " of #" + glyph.getPartOf().getId());
    //            //            } else {
    //            //                shapeField.setText(shape.toString());
    //            //            }
    //            //
    //            //            shapeIcon.setIcon(shape.getDecoratedSymbol());
    //            //        } else {
    //            //            shapeField.setText("");
    //            //            shapeIcon.setIcon(null);
    //            //        }
    //        } else {
    //            count.setText("");
    //            dump.setEnabled(false);
    //
    //            if (idSpinner != null) {
    //                idSpinner.setValue(NO_VALUE);
    //            }
    //        }
    //    }
    //
    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Group
     *
     * @param GroupEvent
     */
    private void handleEvent (GroupEvent groupEvent)
    {
        // Display new group
        Group group = groupEvent.getData();
        groupSpinner.setValue(group);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final PixelCount shapeIconHeight = new PixelCount(
                70,
                "Exact pixel height for the shape icon field");

        private final PixelCount shapeIconWidth = new PixelCount(
                50,
                "Exact pixel width for the shape icon field");
    }

    //----------------//
    // DeassignAction //
    //----------------//
    private class DeassignAction
            extends AbstractAction
    {
        //~ Constructors ---------------------------------------------------------------------------

        public DeassignAction ()
        {
            super("Deassign");
            putValue(Action.SHORT_DESCRIPTION, "Deassign shape");
        }

        //~ Methods --------------------------------------------------------------------------------
        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed (ActionEvent e)
        {
            logger.warn("HB. To be implemented");

            //            List<Class<?>> classes = Arrays.asList(eventClasses);
            //
            //            if ((controller != null) && !classes.isEmpty()) {
            //                // Do we have selections for glyph set, or just for glyph?
            //                if (classes.contains(GlyphEvent.class)) {
            //                    final Glyph glyph = (Glyph) getSelectionService().getSelection(
            //                            GlyphEvent.class);
            //
            //                    if (classes.contains(GlyphSetEvent.class)) {
            //                        final Set<Glyph> glyphs = (Set<Glyph>) getSelectionService().getSelection(
            //                                GlyphSetEvent.class);
            //
            //                        boolean noVirtuals = true;
            //
            //                        for (Glyph g : glyphs) {
            //                            if (g.isVirtual()) {
            //                                noVirtuals = false;
            //
            //                                break;
            //                            }
            //                        }
            //
            //                        if (noVirtuals) {
            //                            new BasicTask()
            //                            {
            //                                @Override
            //                                protected Void doInBackground ()
            //                                        throws Exception
            //                                {
            //                                    // Following actions must be done in sequence
            //                                    Task task = controller.asyncDeassignGlyphs(glyphs);
            //
            //                                    if (task != null) {
            //                                        task.get();
            //
            //                                        throw new RuntimeException("HB. To be implemented");
            //
            //                                            //                                        // Update focus on current glyph,
            //                                        //                                                // even if reused in a compound
            //                                        //
            //                                        //                                        Glyph newGlyph = glyph.getFirstSection().getCompound();
            //                                        //                                        getSelectionService().publish(
            //                                        //                                                new GlyphEvent(
            //                                        //                                                        this,
            //                                        //                                                        SelectionHint.GLYPH_INIT,
            //                                        //                                                        null,
            //                                        //                                                        newGlyph));
            //                                    }
            //
            //                                    return null;
            //                                }
            //                            }.execute();
            //                        } else {
            //                            new BasicTask()
            //                            {
            //                                @Override
            //                                protected Void doInBackground ()
            //                                        throws Exception
            //                                {
            //                                    // Following actions must be done in sequence
            //                                    Task task = controller.asyncDeleteVirtualGlyphs(glyphs);
            //
            //                                    if (task != null) {
            //                                        task.get();
            //
            //                                        // Null publication
            //                                        getSelectionService().publish(
            //                                                new GlyphEvent(
            //                                                        this,
            //                                                        SelectionHint.GLYPH_INIT,
            //                                                        null,
            //                                                        null));
            //                                    }
            //
            //                                    return null;
            //                                }
            //                            }.execute();
            //                        }
            //                    } else {
            //                        // We have selection for glyph only
            //                        if (glyph.isVirtual()) {
            //                            controller.asyncDeleteVirtualGlyphs(Collections.singleton(glyph));
            //                        } else {
            //                            controller.asyncDeassignGlyphs(Collections.singleton(glyph));
            //                        }
            //                    }
            //                }
            //            }
        }
    }
}
