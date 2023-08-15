//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       E n t i t y V i e w                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.LocationEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.view.RubberPanel;
import org.audiveris.omr.util.Entities;
import org.audiveris.omr.util.Entity;
import org.audiveris.omr.util.EntityIndex;
import org.audiveris.omr.util.WeakPropertyChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Class <code>EntityView</code> is a basic graphical view for an entity type.
 *
 * @param <E> precise entity type
 * @author Hervé Bitteur
 */
public class EntityView<E extends Entity>
        extends RubberPanel
        implements PropertyChangeListener
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(EntityView.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Underlying entity service. */
    protected final EntityService<E> entityService;

    /** Underlying entity index. */
    protected final EntityIndex<E> entityIndex;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>EntityView</code> object.
     *
     * @param entityService the underlying entity service
     */
    public EntityView (EntityService<E> entityService)
    {
        this.entityService = entityService;
        entityIndex = entityService.getIndex();

        setName("EntityView");

        // (Weakly) listening on ViewParameters and PaintingParameters
        PropertyChangeListener listener = new WeakPropertyChangeListener(this);
        ViewParameters.getInstance().addPropertyChangeListener(listener);

        // Subscribe to entity
        entityService.subscribeStrongly(EntityListEvent.class, this);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------------------//
    // handleEntityListEvent //
    //-----------------------//
    /**
     * Interest in EntityList to display entities contour.
     *
     * @param listEvent list of entities
     */
    protected final void handleEntityListEvent (EntityListEvent<E> listEvent)
    {
        List<E> list = listEvent.getData();

        if (!list.isEmpty() && (listEvent.hint != SelectionHint.ENTITY_TRANSIENT)) {
            locationService.publish(
                    new LocationEvent(
                            this,
                            SelectionHint.ENTITY_TRANSIENT,
                            null,
                            Entities.getBounds(list)));
        }
    }

    //---------//
    // onEvent //
    //---------//
    @Override
    @SuppressWarnings("unchecked")
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            super.onEvent(event); // Default view behavior (locationEvent -> focus)

            if (event instanceof EntityListEvent) {
                handleEntityListEvent((EntityListEvent<E>) event);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //----------------//
    // propertyChange //
    //----------------//
    @Override
    public void propertyChange (PropertyChangeEvent evt)
    {
        repaint(); // For any property change, we simply repaint the view
    }
}
