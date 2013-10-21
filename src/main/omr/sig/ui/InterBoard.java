//----------------------------------------------------------------------------//
//                                                                            //
//                             I n t e r B o a r d                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.selection.InterIdEvent;
import omr.selection.InterListEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import omr.selection.UserEvent;

import omr.sheet.Sheet;

import omr.sig.Inter;
import omr.sig.SigManager;

import omr.ui.Board;
import omr.ui.PixelCount;
import omr.ui.field.LCheckBox;
import omr.ui.field.LDoubleField;
import omr.ui.field.LTextField;
import omr.ui.field.SpinnerUtil;
import static omr.ui.field.SpinnerUtil.NO_VALUE;
import omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code InterBoard} defines a UI board for {@link Inter}
 * information.
 *
 * @author Hervé Bitteur
 */
public class InterBoard
        extends Board
        implements ChangeListener, ActionListener
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            InterBoard.class);

    /** Events this board is interested in. */
    private static final Class<?>[] eventClasses = new Class<?>[]{
        InterListEvent.class
    };

    //~ Instance fields --------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Input: Dump. */
    private final JButton dump;

    /** Output : shape icon. */
    private final JLabel shapeIcon = new JLabel();

    /** Input / Output : spinner of all inter id's. */
    private JSpinner idSpinner;

    /** Output : grade. */
    private final LDoubleField grade = new LDoubleField(
            false,
            "Grade",
            "Probability",
            "%.2f");

    /** Input / Output : VIP flag. */
    private final LCheckBox vip = new LCheckBox(
            "Vip",
            "Is this glyph flagged as VIP?");

    /** Output : shape. */
    private final LTextField shapeField = new LTextField(
            "",
            "Shape for this interpretation");

    /** Output : grade details. */
    private final LTextField details = new LTextField(
            "Impacts",
            "Grade details");

    /** The JGoodies/Form constraints to be used by all subclasses. */
    protected final CellConstraints cst = new CellConstraints();

    /** The JGoodies/Form layout to be used by all subclasses. */
    protected final FormLayout layout = Panel.makeFormLayout(4, 3);

    /** The JGoodies/Form builder to be used by all subclasses. */
    protected final PanelBuilder builder;

    /** To delete/deassign. */
    private final DeassignAction deassignAction = new DeassignAction();

    /**
     * We have to avoid endless loop, due to related modifications.
     * When an Inter selection is notified, the id spinner is changed, and When
     * an id spinner is changed, the Inter selection is notified
     */
    private boolean selfUpdating = false;

    //~ Constructors -----------------------------------------------------------
    //------------//
    // InterBoard //
    //------------//
    /**
     * Creates a new InterBoard object.
     *
     * @param sheet the related sheet
     */
    public InterBoard (Sheet sheet)
    {
        super(
                Board.INTER,
                sheet.getLocationService(),
                eventClasses,
                true, // withDump
                true); // initially expanded
        this.sheet = sheet;

        // Dump
        dump = getDumpButton();
        dump.setToolTipText("Dump this interpretation");
        dump.addActionListener(
                new ActionListener()
                {
                    @Override
                    public void actionPerformed (ActionEvent e)
                    {
                        // Retrieve current inter selection
                        InterListEvent interListEvent = (InterListEvent) getSelectionService()
                        .getLastEvent(
                                InterListEvent.class);
                        List<Inter> interList = interListEvent.getData();

                        if ((interList != null) && !interList.isEmpty()) {
                            Inter inter = interList.get(interList.size() - 1);
                            logger.info(inter.dumpOf());
                        }
                    }
                });
        // Until a glyph selection is made
        dump.setEnabled(false);

        // Listener for VIP
        vip.addActionListener(this);

        // Force a constant height for the shapeIcon field, despite the
        // variation in size of the icon
        Dimension dim = new Dimension(
                constants.shapeIconWidth.getValue(),
                constants.shapeIconHeight.getValue());
        shapeIcon.setPreferredSize(dim);
        shapeIcon.setMaximumSize(dim);
        shapeIcon.setMinimumSize(dim);

        builder = new PanelBuilder(layout, getBody());
        builder.setDefaultDialogBorder();

        // Initial status
        defineLayout();
        vip.setEnabled(false);
        grade.setEnabled(false);
        details.setEnabled(false);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------------//
    // actionPerformed //
    //-----------------//
    /**
     * Triggered by VIP check box.
     *
     * @param e
     */
    @Override
    public void actionPerformed (ActionEvent e)
    {
        if (vip.getField() == e.getSource()) {
            final JCheckBox box = vip.getField();
            final Inter inter = getSelectedInter();

            if (inter != null) {
                if (!inter.isVip()) {
                    inter.setVip();
                    box.setEnabled(false);
                    logger.info("{} flagged as VIP", inter);
                }
            }
        }
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Inter Selection has been modified
     *
     * @param event of current inter list
     */
    @Override
    public void onEvent (UserEvent event)
    {
        logger.debug("InterBoard event:{}", event);

        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            if (event instanceof InterListEvent) {
                // Display inter parameters (while preventing circular updates)
                selfUpdating = true;
                handleEvent((InterListEvent) event);
                selfUpdating = false;
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

        if (spinner == idSpinner) {
            // Nota: this method is automatically called whenever the spinner
            // value is changed, including when an inter selection notification
            // is received leading to such selfUpdating. Hence the check.
            if (!selfUpdating) {
                // Notify the new inter id
                getSelectionService()
                        .publish(
                                new InterIdEvent(
                                        this,
                                        SelectionHint.INTER_INIT,
                                        null,
                                        (Integer) spinner.getValue()));
            }
        } else {
            logger.error("No known spinner");
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
        // Model for idSpinner
        idSpinner = makeInterSpinner(sheet.getSigManager());
        idSpinner.setName("idSpinner");
        idSpinner.setToolTipText("General spinner for any glyph id");

        // Layout
        int r = 1; // --------------------------------

        builder.addLabel("Id", cst.xy(1, r));
        builder.add(idSpinner, cst.xy(3, r));

        // Shape Icon (start, spans several rows) + layer + Deassign button
        builder.add(shapeIcon, cst.xywh(1, r, 1, 5));

        builder.add(grade.getLabel(), cst.xy(5, r));
        builder.add(grade.getField(), cst.xy(7, r));

        JButton deassignButton = new JButton(deassignAction);
        deassignButton.setHorizontalTextPosition(SwingConstants.LEFT);
        deassignButton.setHorizontalAlignment(SwingConstants.RIGHT);
        deassignAction.setEnabled(false);
        builder.add(deassignButton, cst.xyw(9, r, 3));

        r += 2; // --------------------------------
        // Count + Active + Shape name

        //        builder.add(count, cst.xy(3, r, "right, center"));
        //
        //        builder.add(active, cst.xy(5, r));
        builder.add(vip.getLabel(), cst.xy(1, r));
        builder.add(vip.getField(), cst.xy(3, r));

        builder.add(shapeField.getField(), cst.xyw(7, r, 5));

        r += 2; // --------------------------------

        builder.add(details.getLabel(), cst.xy(1, r));
        builder.add(details.getField(), cst.xyw(3, r, 9));
    }

    //------------------//
    // makeInterSpinner //
    //------------------//
    /**
     * Convenient method to allocate an inter-based spinner
     *
     * @param sigManager the underlying SIG manager
     * @return the spinner built
     */
    protected JSpinner makeInterSpinner (SigManager sigManager)
    {
        JSpinner spinner = new JSpinner();
        spinner.setModel(new SpinnerInterIdModel(sigManager));
        spinner.addChangeListener(this);
        SpinnerUtil.setRightAlignment(spinner);
        SpinnerUtil.setEditable(spinner, true);

        return spinner;
    }

    //------------------//
    // getSelectedInter //
    //------------------//
    private Inter getSelectedInter ()
    {
        final List<Inter> interList = (List<Inter>) getSelectionService()
                .getSelection(
                        InterListEvent.class);

        if ((interList != null) && !interList.isEmpty()) {
            return interList.get(interList.size() - 1);
        } else {
            return null;
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in InterList
     *
     * @param InterListEvent
     */
    private void handleEvent (InterListEvent interListEvent)
    {
        List<Inter> interList = interListEvent.getData();

        final Inter inter;

        if ((interList != null) && !interList.isEmpty()) {
            inter = interList.get(interList.size() - 1);
        } else {
            inter = null;
        }

        // Dump button and deassign button
        dump.setEnabled(inter != null);
        deassignAction.setEnabled((inter != null) && !inter.isDeleted());

        // Shape text and icon
        Shape shape = (inter != null) ? inter.getShape() : null;

        if (shape != null) {
            shapeField.setText(shape.toString());
            shapeIcon.setIcon(shape.getDecoratedSymbol());
        } else {
            shapeField.setText("");
            shapeIcon.setIcon(null);
        }

        // Id Spinner
        if (idSpinner != null) {
            if (inter != null) {
                idSpinner.setValue(inter.getId());
            } else {
                idSpinner.setValue(NO_VALUE);
            }
        }

        // Inter characteristics
        if (inter != null) {
            vip.getLabel()
                    .setEnabled(true);
            vip.getField()
                    .setEnabled(!inter.isVip());
            vip.getField()
                    .setSelected(inter.isVip());

            grade.setValue(inter.getGrade());
            details.setText(inter.getDetails());
            deassignAction.putValue(
                    Action.NAME,
                    inter.isDeleted() ? "deleted" : "Deassign");
        } else {
            vip.setEnabled(false);
            vip.getField()
                    .setSelected(false);

            grade.setText("");
            details.setText("");
            deassignAction.putValue(Action.NAME, " ");
        }

        grade.setEnabled(inter != null);
        details.setEnabled(inter != null);
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
            this.putValue(Action.SHORT_DESCRIPTION, "Deassign inter");
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public void actionPerformed (ActionEvent e)
        {
            logger.info("Not yet implemented");
        }
    }
}
