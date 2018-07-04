//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E n t i t y B o a r d                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.ui.field.LCheckBox;
import org.audiveris.omr.ui.field.LLabel;
import org.audiveris.omr.ui.field.SpinnerUtil;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.IdEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.util.Entity;
import org.audiveris.omr.util.EntityIndex;

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

    //~ Enumerations -------------------------------------------------------------------------------
    /** To select precise ID option. */
    public static enum IdOption
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        ID_NONE,
        ID_LABEL,
        ID_SPINNER;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Counter of entities selection. */
    protected JLabel count;

    /** Input / Output : VIP flag. */
    protected LCheckBox vip;

    /** Button for entity dump. */
    protected JButton dump;

    /** Input / Output : spinner of all ID's. (exclusive of idLabel) */
    protected JSpinner idSpinner;

    /** Output : ID value. (exclusive of idSpinner). */
    protected LLabel idLabel;

    /** The JGoodies/Form layout to be used by all subclasses. */
    protected final FormLayout layout = getFormLayout();

    /** The JGoodies/Form builder to be used by all subclasses. */
    protected final PanelBuilder builder;

    /** To avoid loop, indicate that update() method id being processed. */
    protected boolean selfUpdating = false;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code EntityBoard} object, with all entity fields by default.
     *
     * @param desc          board descriptor
     * @param entityService the underlying entity service
     * @param selected      true for pre-selected, false for collapsed
     */
    public EntityBoard (Desc desc,
                        EntityService<E> entityService,
                        boolean selected)
    {
        this(desc, entityService, selected, true, true, true, IdOption.ID_SPINNER);
    }

    /**
     * Creates a new {@code EntityBoard} object, with selected entity fields.
     *
     * @param desc          board descriptor
     * @param entityService the underlying entity service
     * @param selected      true for pre-selected, false for collapsed
     * @param useCount      true for use of count
     * @param useVip        true for use of VIP
     * @param useDump       true for use of dump
     * @param idOption      option for ID
     */
    public EntityBoard (Desc desc,
                        EntityService<E> entityService,
                        boolean selected,
                        boolean useCount,
                        boolean useVip,
                        boolean useDump,
                        IdOption idOption)
    {
        super(desc, entityService, eventsRead, selected, useCount, useVip, useDump);

        // Count
        if (useCount) {
            count = getCountField();
            count.setToolTipText("Count of selected entities");
        }

        // VIP
        if (useVip) {
            vip = getVipBox();
            vip.addActionListener(this);
            vip.setEnabled(false);
        }

        // Dump
        if (useDump) {
            dump = getDumpButton();
            dump.setToolTipText("Dump this entity");
            dump.setEnabled(false);
            dump.addActionListener(this);
        }

        // Id
        switch (idOption) {
        case ID_LABEL:
            idLabel = new LLabel("Id:", "Entity id");

            break;

        case ID_SPINNER:
            idSpinner = makeIdSpinner(entityService.getIndex());
            idSpinner.setName("idSpinner");
            idSpinner.setToolTipText("Spinner for any entity id");

            break;

        default:
        case ID_NONE:
        }

        builder = new PanelBuilder(layout, getBody());

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // actionPerformed //
    //-----------------//
    /**
     * Triggered by VIP check box and by dump button
     *
     * @param e the event that triggered this action
     */
    @Override
    public void actionPerformed (ActionEvent e)
    {
        if ((vip != null) && (vip.getField() == e.getSource())) {
            vipActionPerformed(e);
        } else if ((dump != null) && (dump == e.getSource())) {
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
                // Display entity parameters (while preventing circular updates)
                selfUpdating = true;
                handleEntityListEvent((EntityListEvent<E>) event);
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
        if ((idSpinner != null) && (idSpinner == e.getSource())) {
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
                                (Integer) idSpinner.getValue()));
            }
        }
    }

    //---------------------//
    // dumpActionPerformed //
    //---------------------//
    /**
     * Override-able action performed for 'dump'.
     *
     * @param e the event that triggered this action
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
     * Overridable method to provide layout of the body part of the board.
     * (not including the top board line: title + dump button)
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
    @SuppressWarnings("unchecked")
    protected E getSelectedEntity ()
    {
        final List<E> list = (List<E>) getSelectionService().getSelection(EntityListEvent.class);

        if ((list != null) && !list.isEmpty()) {
            return list.get(0); // Use first
            ///return list.get(list.size() - 1); // Use last
        } else {
            return null;
        }
    }

    //-----------------------//
    // handleEntityListEvent //
    //-----------------------//
    /**
     * Interest in EntityList for Vip, Dump, Id fields
     *
     * @param listEvent EntityListEvent
     */
    protected void handleEntityListEvent (EntityListEvent<E> listEvent)
    {
        // Count
        final List<E> entities = listEvent.getData();

        if (count != null) {
            if (!entities.isEmpty()) {
                count.setText(Integer.toString(entities.size()));
            } else {
                count.setText("");
            }
        }

        final E entity = listEvent.getEntity();

        if (entity != null) {
            // VIP
            if (vip != null) {
                vip.getLabel().setEnabled(true);
                vip.getField().setEnabled(!entity.isVip());
                vip.getField().setSelected(entity.isVip());
            }

            // Dump
            if (dump != null) {
                dump.setEnabled(true);
            }

            // Id
            if (idSpinner != null) {
                idSpinner.setValue(entity.getId());
            } else if (idLabel != null) {
                idLabel.setText(Integer.toString(entity.getId()));
            }
        } else {
            // VIP
            if (vip != null) {
                vip.setEnabled(false);
                vip.getField().setSelected(false);
            }

            // Dump
            if (dump != null) {
                dump.setEnabled(false);
            }

            // Id
            if (idSpinner != null) {
                idSpinner.setValue(0);
            } else if (idLabel != null) {
                idLabel.setText("");
            }
        }
    }

    //--------------------//
    // vipActionPerformed //
    //--------------------//
    /**
     * Override-able action performed for 'vip'.
     *
     * @param e the event that triggered this action
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

        if (idSpinner != null) {
            builder.addLabel("Id", cst.xy(1, r));
            builder.add(idSpinner, cst.xy(3, r));
        }

        if (idLabel != null) {
            builder.add(idLabel.getLabel(), cst.xy(1, r));
            builder.add(idLabel.getField(), cst.xy(3, r));
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
    private JSpinner makeIdSpinner (EntityIndex<E> index)
    {
        JSpinner spinner = new JSpinner(new SpinnerIdModel<E>(index));
        spinner.setValue(0); // Initial value before listener is set!
        spinner.addChangeListener(this);
        spinner.setLocale(Locale.ENGLISH);
        SpinnerUtil.setRightAlignment(spinner);
        SpinnerUtil.setEditable(spinner, true);

        return spinner;
    }
}
