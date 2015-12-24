//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E n t i t y B o a r d                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.selection.EntityListEvent;
import omr.selection.EntityService;
import omr.selection.IdEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import omr.selection.UserEvent;

import omr.ui.field.LCheckBox;
import omr.ui.field.SpinnerUtil;
import omr.ui.util.Panel;

import omr.util.Entity;
import omr.util.EntityIndex;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import omr.util.IdUtil;

/**
 * Class {@code EntityBoard} is a basic board related to an entity type.
 *
 * @param <E> precise entity type
 *
 * @author Hervé Bitteur
 */
public class EntityBoard<E extends Entity>
        extends Board
        implements ChangeListener, ActionListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(EntityBoard.class);

    /** Events this board is interested in */
    protected static final Class<?>[] eventsRead = new Class<?>[]{EntityListEvent.class};

    //~ Instance fields ----------------------------------------------------------------------------
    /** Counter of entities selection. */
    protected JLabel count;

    /** Input / Output : VIP flag. */
    protected final LCheckBox vip;

    /** Button for entity dump. */
    protected final JButton dump;

    /** Input / Output : spinner of all ID's. */
    protected JSpinner idSpinner;

    /** The JGoodies/Form layout to be used by all subclasses. */
    protected final FormLayout layout = getFormLayout();

    /** The JGoodies/Form builder to be used by all subclasses. */
    protected final PanelBuilder builder;

    /** To avoid loop, indicate that update() method id being processed. */
    protected boolean selfUpdating = false;

    /** Prefix for entity IDs. */
    protected String prefix;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code EntityBoard} object.
     *
     * @param desc          board descriptor
     * @param entityService the underlying entity service
     * @param expanded      true for expanded, false for collapsed
     */
    public EntityBoard (Desc desc,
                        EntityService entityService,
                        boolean expanded)
    {
        super(desc, entityService, eventsRead, true, true, true, expanded);

        prefix = entityService.getIndex().getPrefix();

        // Count
        count = getCountField();
        count.setToolTipText("Count of selected entities");

        // VIP
        vip = getVipBox();
        vip.addActionListener(this);
        vip.setEnabled(false);

        // Dump
        dump = getDumpButton();
        dump.setToolTipText("Dump this entity");
        dump.setEnabled(false);
        dump.addActionListener(this);

        //        dump.addActionListener(
        //                new ActionListener()
        //                {
        //                    @Override
        //                    public void actionPerformed (ActionEvent e)
        //                    {
        //                        // Retrieve current entity selection
        //                        final E entity = ((EntityService<E>) getSelectionService()).getSelectedEntity();
        //
        //                        if (entity != null) {
        //                            logger.info(entity.dumpOf());
        //                        }
        //                    }
        //                });
        // Model for idSpinner
        idSpinner = makeIdSpinner(entityService.getIndex());
        idSpinner.setName("idSpinner");
        idSpinner.setToolTipText("Spinner for any entity id");

        builder = new PanelBuilder(layout, getBody());
        //        builder.setDefaultDialogBorder();
        //
        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // actionPerformed //
    //-----------------//
    /**
     * Triggered by VIP check box and by dump button
     *
     * @param e
     */
    @Override
    public void actionPerformed (ActionEvent e)
    {
        if (vip.getField() == e.getSource()) {
            vipActionPerformed(e);
        } else if (dump == e.getSource()) {
            dumpActionPerformed(e);
        }
    }

    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when user Selection has been modified
     *
     * @param event of current inter list
     */
    @Override
    public void onEvent (UserEvent event)
    {
        logger.debug("EntityBoard event:{}", event);

        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            if (event instanceof EntityListEvent) {
                // Display inter parameters (while preventing circular updates)
                selfUpdating = true;
                handleEvent((EntityListEvent<E>) event);
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
     * @param e the change event, this allows to retrieve the originating spinner
     */
    @Override
    public void stateChanged (ChangeEvent e)
    {
        JSpinner spinner = (JSpinner) e.getSource();

        if (spinner == idSpinner) {
            // Nota: this method is automatically called whenever the spinner
            // value is changed, including when an entity selection notification
            // is received leading to such selfUpdating. Hence the check.
            if (!selfUpdating) {
                // Notify the new entity id
                getSelectionService().publish(
                        new IdEvent(
                                this,
                                SelectionHint.ENTITY_INIT,
                                null,
                                prefix + spinner.getValue()));
            }
        } else {
            logger.error("No known spinner");
        }
    }

    //---------------------//
    // dumpActionPerformed //
    //---------------------//
    /**
     * Override-able action performed for 'dump'.
     *
     * @param e
     */
    protected void dumpActionPerformed (ActionEvent e)
    {
        final E entity = getSelectedEntity();
        logger.info(entity.dumpOf());
    }

    //---------------//
    // getFormLayout //
    //---------------//
    /**
     * Override-able method to provide layout.
     *
     * @return the proper FormLayout
     */
    protected FormLayout getFormLayout ()
    {
        return Panel.makeFormLayout(4, 3);
    }

    //-------------------//
    // getSelectedEntity //
    //-------------------//
    protected E getSelectedEntity ()
    {
        final List<E> list = (List<E>) getSelectionService().getSelection(EntityListEvent.class);

        if ((list != null) && !list.isEmpty()) {
            return list.get(list.size() - 1);
        } else {
            return null;
        }
    }

    //--------------------//
    // vipActionPerformed //
    //--------------------//
    /**
     * Override-able action performed for 'vip'.
     *
     * @param e
     */
    protected void vipActionPerformed (ActionEvent e)
    {
        final E entity = getSelectedEntity();
        entity.setVip(vip.getField().isSelected());

        if (entity.isVip()) {
            logger.info("{} flagged as VIP", entity);
        } else {
            logger.info("{} no longer VIP", entity);
        }
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout for common fields of all EntityBoard (sub)classes.
     * Layout for others (count, vip, dump) is defined in super Board class.
     */
    private void defineLayout ()
    {
        CellConstraints cst = new CellConstraints();

        // Layout
        int r = 1; // --------------------------------

        builder.addLabel("Id", cst.xy(1, r));
        builder.add(idSpinner, cst.xy(3, r));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in EntityList
     *
     * @param EntityListEvent
     */
    private void handleEvent (EntityListEvent<E> listEvent)
    {
        // Count
        final List<E> entities = listEvent.getData();

        if ((entities != null) && !entities.isEmpty()) {
            count.setText(Integer.toString(entities.size()));
        } else {
            count.setText("");
        }

        final E entity = listEvent.getEntity();

        if (entity != null) {
            // VIP
            vip.getLabel().setEnabled(true);
            vip.getField().setEnabled(!entity.isVip());
            vip.getField().setSelected(entity.isVip());
            // Dump
            dump.setEnabled(true);
            // Id
            final Integer idValue = IdUtil.getIntValue(entity.getId());
            idSpinner.setValue(idValue != null ? idValue : 0);
        } else {
            // VIP
            vip.setEnabled(false);
            vip.getField().setSelected(false);
            // Dump
            dump.setEnabled(false);
            // Id
            idSpinner.setValue(0);
        }
    }

    //---------------//
    // makeIdSpinner //
    //---------------//
    /**
     * Convenient method to allocate an entity-based spinner
     *
     * @param index the underlying entity index
     * @return the spinner built
     */
    private JSpinner makeIdSpinner (EntityIndex index)
    {
        JSpinner spinner = new JSpinner(new SpinnerIdModel(index));
        spinner.setValue(0); // Initial value before listener is set!
        spinner.addChangeListener(this);
        spinner.setLocale(Locale.ENGLISH);
        SpinnerUtil.setRightAlignment(spinner);
        SpinnerUtil.setEditable(spinner, true);

        return spinner;
    }
}
