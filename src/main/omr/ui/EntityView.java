//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       E n t i t y V i e w                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.ui;

import omr.score.ui.PaintingParameters;

import omr.ui.selection.EntityListEvent;
import omr.ui.selection.EntityService;
import omr.ui.selection.LocationEvent;
import omr.ui.selection.MouseMovement;

import static omr.ui.selection.SelectionHint.ENTITY_INIT;

import omr.ui.selection.UserEvent;
import omr.ui.view.RubberPanel;

import omr.util.Entities;
import omr.util.Entity;
import omr.util.EntityIndex;
import omr.util.WeakPropertyChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

/**
 * Class {@code EntityView} is a basic graphical view for an entity type.
 *
 * @param <E> precise entity type
 *
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
     * Creates a new {@code EntityView} object.
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
        PaintingParameters.getInstance().addPropertyChangeListener(listener);

        // Subscribe to entity
        entityService.subscribeStrongly(EntityListEvent.class, this);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            super.onEvent(event); // Default view behavior (locationEvent -> focus)

            if (event instanceof EntityListEvent) {
                handleEvent((EntityListEvent<E>) event);
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

    //-------------//
    // handleEvent //
    //-------------//
    private void handleEvent (EntityListEvent<E> event)
    {
        if (event.hint == ENTITY_INIT) {
            List<E> list = event.getData();

            if ((list != null) && !list.isEmpty()) {
                // Display entities contour
                locationService.publish(
                        new LocationEvent(this, event.hint, null, Entities.getBounds(list)));
            }
        }
    }
}
