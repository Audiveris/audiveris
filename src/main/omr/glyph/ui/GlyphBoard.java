//----------------------------------------------------------------------------//
//                                                                            //
//                            G l y p h B o a r d                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.constant.ConstantSet;

import omr.glyph.Nest;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.selection.GlyphEvent;
import omr.selection.GlyphIdEvent;
import omr.selection.GlyphSetEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import omr.selection.UserEvent;

import omr.ui.Board;
import omr.ui.PixelCount;
import omr.ui.field.LTextField;
import omr.ui.field.SpinnerUtil;
import static omr.ui.field.SpinnerUtil.*;
import omr.ui.util.Panel;

import omr.util.BasicTask;
import omr.util.Predicate;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.jdesktop.application.Task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code GlyphBoard} defines a UI board dedicated to the display
 * of {@link Glyph} information.
 *
 * <p>The universal <b>globalSpinner</b> addresses <i>all</i> glyphs
 * currently defined in the nest (note that glyphs can be dynamically created or
 * destroyed).
 *
 * <p>The spinner can be used to select a glyph by directly entering the
 * glyph id value into the spinner field
 *
 * @author Hervé Bitteur
 */
public class GlyphBoard
        extends Board
        implements ChangeListener // For all spinners
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            GlyphBoard.class);

    /** Events this board is interested in */
    private static final Class<?>[] eventClasses = new Class<?>[]{
        GlyphEvent.class,
        GlyphSetEvent.class
    };

    /** Predicate for known glyphs */
    protected static final Predicate<Glyph> knownPredicate = new Predicate<Glyph>()
    {
        @Override
        public boolean check (Glyph glyph)
        {
            return (glyph != null) && glyph.isKnown();
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** The related glyph model */
    protected final GlyphsController controller;

    /** An active label */
    protected final JLabel active = new JLabel("", SwingConstants.CENTER);

    /** Input: Dump */
    protected final JButton dump;

    /** Counter of glyph selection */
    protected final JLabel count = new JLabel("");

    /** Input : Deassign action */
    protected Action deassignAction;

    /** Output : glyph shape icon */
    protected final JLabel shapeIcon = new JLabel();

    /** Input / Output : spinner of all glyphs */
    protected JSpinner globalSpinner;

    /** Input / Output : spinner of known glyphs */
    protected JSpinner knownSpinner;

    /** Output : shape of the glyph */
    protected final LTextField shapeField = new LTextField(
            "",
            "Assigned shape for this glyph");

    /** The JGoodies/Form constraints to be used by all subclasses */
    protected final CellConstraints cst = new CellConstraints();

    /** The JGoodies/Form layout to be used by all subclasses */
    protected final FormLayout layout = Panel.makeFormLayout(6, 3);

    /** The JGoodies/Form builder to be used by all subclasses */
    protected final PanelBuilder builder;

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
        super(
                Board.GLYPH,
                controller.getNest().getGlyphService(),
                eventClasses,
                true, // Dump
                expanded);

        this.controller = controller;

        // Dump
        dump = getDumpButton();
        dump.setToolTipText("Dump this glyph");
        dump.addActionListener(
                new ActionListener()
        {
            @Override
            public void actionPerformed (ActionEvent e)
            {
                // Retrieve current glyph selection
                GlyphEvent glyphEvent = (GlyphEvent) getSelectionService()
                        .getLastEvent(
                        GlyphEvent.class);
                Glyph glyph = glyphEvent.getData();

                if (glyph != null) {
                    logger.info(glyph.dumpOf());
                }
            }
        });
        // Until a glyph selection is made
        dump.setEnabled(false);
        getDeassignAction()
                .setEnabled(false);

        // Force a constant height for the shapeIcon field, despite the
        // variation in size of the icon
        Dimension dim = new Dimension(
                constants.shapeIconWidth.getValue(),
                constants.shapeIconHeight.getValue());
        shapeIcon.setPreferredSize(dim);
        shapeIcon.setMaximumSize(dim);
        shapeIcon.setMinimumSize(dim);

        //         Precise layout
        //        layout.setColumnGroups(
        //            new int[][] {
        //                { 1, 5, 9 },
        //                { 3, 7, 11 }
        //            });
        builder = new PanelBuilder(layout, getBody());
        builder.setDefaultDialogBorder();

        defineLayout();

        if (useSpinners) {
            // Model for globalSpinner
            globalSpinner = makeGlyphSpinner(controller.getNest(), null);
            globalSpinner.setName("globalSpinner");
            globalSpinner.setToolTipText("General spinner for any glyph id");

            // Layout
            int r = 1; // --------------------------------

            if (globalSpinner != null) {
                builder.addLabel("Id", cst.xy(1, r));
                builder.add(globalSpinner, cst.xy(3, r));
            }
        }
    }

    //~ Methods ----------------------------------------------------------------
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

            logger.debug(
                    "GlyphBoard selfUpdating={} : {}",
                    selfUpdating,
                    event);

            if (event instanceof GlyphEvent) {
                // Display Glyph parameters (while preventing circular updates)
                selfUpdating = true;
                handleEvent((GlyphEvent) event);
                selfUpdating = false;
            } else if (event instanceof GlyphSetEvent) {
                // Display count of glyphs in the glyph set
                handleEvent((GlyphSetEvent) event);
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
     * @param e the change event, this allows to retrieve the originating
     *          spinner
     */
    @Override
    public void stateChanged (ChangeEvent e)
    {
        JSpinner spinner = (JSpinner) e.getSource();

        //  Nota: this method is automatically called whenever the spinner value
        //  is changed, including when a GLYPH selection notification is
        //  received leading to such selfUpdating. So the check.
        if (!selfUpdating) {
            // Notify the new glyph id
            getSelectionService()
                    .publish(
                    new GlyphIdEvent(
                    this,
                    SelectionHint.GLYPH_INIT,
                    null,
                    (Integer) spinner.getValue()));
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
        // Shape Icon (start, spans several rows) + count + active + Deassign button

        builder.add(shapeIcon, cst.xywh(1, r, 1, 5));

        builder.add(count, cst.xy(5, r));

        builder.add(active, cst.xy(7, r));

        JButton deassignButton = new JButton(getDeassignAction());
        deassignButton.setHorizontalTextPosition(SwingConstants.LEFT);
        deassignButton.setHorizontalAlignment(SwingConstants.RIGHT);
        builder.add(deassignButton, cst.xyw(9, r, 3));

        r += 2; // --------------------------------
        // Shape name

        builder.add(shapeField.getField(), cst.xyw(7, r, 5));
    }

    //------------------//
    // makeGlyphSpinner //
    //------------------//
    /**
     * Convenient method to allocate a glyph-based spinner
     *
     * @param nest      the underlying glyph nest
     * @param predicate a related glyph predicate, if any
     * @return the spinner built
     */
    protected JSpinner makeGlyphSpinner (Nest nest,
                                         Predicate<Glyph> predicate)
    {
        JSpinner spinner = new JSpinner();
        spinner.setModel(new SpinnerGlyphModel(nest, predicate));
        spinner.addChangeListener(this);
        SpinnerUtil.setRightAlignment(spinner);
        SpinnerUtil.setEditable(spinner, true);

        return spinner;
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Glyph
     *
     * @param GlyphEvent
     */
    private void handleEvent (GlyphEvent glyphEvent)
    {
        // Display Glyph parameters
        Glyph glyph = glyphEvent.getData();

        // Active ?
        if (glyph != null) {
            if (glyph.isActive()) {
                if (glyph.isVirtual()) {
                    active.setText("Virtual");
                } else {
                    active.setText("Active");
                }
            } else {
                active.setText("Non Active");
            }
        } else {
            active.setText("");
        }

        // Dump button and deassign button
        dump.setEnabled(glyph != null);
        getDeassignAction()
                .setEnabled((glyph != null) && glyph.isKnown());

        // Shape text and icon
        Shape shape = (glyph != null) ? glyph.getShape() : null;

        if (shape != null) {
            if ((shape == Shape.GLYPH_PART) && (glyph.getPartOf() != null)) {
                shapeField.setText(shape + " of #" + glyph.getPartOf().getId());
            } else {
                shapeField.setText(shape.toString());
            }

            shapeIcon.setIcon(shape.getDecoratedSymbol());
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
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in GlyphSet
     *
     * @param GlyphSetEvent
     */
    private void handleEvent (GlyphSetEvent glyphSetEvent)
    {
        // Display count of glyphs in the glyph set
        Set<Glyph> glyphs = glyphSetEvent.getData();

        if ((glyphs != null) && !glyphs.isEmpty()) {
            count.setText(Integer.toString(glyphs.size()));
        } else {
            count.setText("");
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Exact pixel height for the shape icon field */
        PixelCount shapeIconHeight = new PixelCount(
                70,
                "Exact pixel height for the shape icon field");

        /** Exact pixel width for the shape icon field */
        PixelCount shapeIconWidth = new PixelCount(
                50,
                "Exact pixel width for the shape icon field");

    }

    //----------------//
    // DeassignAction //
    //----------------//
    private class DeassignAction
            extends AbstractAction
    {
        //~ Constructors -------------------------------------------------------

        public DeassignAction ()
        {
            super("Deassign");
            this.putValue(Action.SHORT_DESCRIPTION, "Deassign shape");
        }

        //~ Methods ------------------------------------------------------------
        @SuppressWarnings("unchecked")
        @Override
        public void actionPerformed (ActionEvent e)
        {
            List<Class<?>> classes = Arrays.asList(eventClasses);

            if ((controller != null) && !classes.isEmpty()) {
                // Do we have selections for glyph set, or just for glyph?
                if (classes.contains(GlyphEvent.class)) {
                    final Glyph glyph = (Glyph) getSelectionService()
                            .getSelection(
                            GlyphEvent.class);

                    if (classes.contains(GlyphSetEvent.class)) {
                        final Set<Glyph> glyphs = (Set<Glyph>) getSelectionService()
                                .getSelection(
                                GlyphSetEvent.class);

                        boolean noVirtuals = true;

                        for (Glyph g : glyphs) {
                            if (g.isVirtual()) {
                                noVirtuals = false;

                                break;
                            }
                        }

                        if (noVirtuals) {
                            new BasicTask()
                            {
                                @Override
                                protected Void doInBackground ()
                                        throws Exception
                                {
                                    // Following actions must be done in sequence
                                    Task task = controller.asyncDeassignGlyphs(
                                            glyphs);

                                    if (task != null) {
                                        task.get();

                                        // Update focus on current glyph,
                                        // even if reused in a compound
                                        Glyph newGlyph = glyph.getFirstSection()
                                                .getGlyph();
                                        getSelectionService()
                                                .publish(
                                                new GlyphEvent(
                                                this,
                                                SelectionHint.GLYPH_INIT,
                                                null,
                                                newGlyph));
                                    }

                                    return null;
                                }
                            }.execute();
                        } else {
                            new BasicTask()
                            {
                                @Override
                                protected Void doInBackground ()
                                        throws Exception
                                {
                                    // Following actions must be done in sequence
                                    Task task = controller.asyncDeleteVirtualGlyphs(
                                            glyphs);

                                    if (task != null) {
                                        task.get();

                                        // Null publication
                                        getSelectionService()
                                                .publish(
                                                new GlyphEvent(
                                                this,
                                                SelectionHint.GLYPH_INIT,
                                                null,
                                                null));
                                    }

                                    return null;
                                }
                            }.execute();
                        }
                    } else {
                        // We have selection for glyph only
                        if (glyph.isVirtual()) {
                            controller.asyncDeleteVirtualGlyphs(
                                    Collections.singleton(glyph));
                        } else {
                            controller.asyncDeassignGlyphs(
                                    Collections.singleton(glyph));
                        }
                    }
                }
            }
        }
    }
}
