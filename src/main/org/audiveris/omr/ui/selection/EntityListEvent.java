//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  E n t i t y L i s t E v e n t                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.ui.selection;

import org.audiveris.omr.util.Entity;

import java.util.List;

/**
 * Class {@code EntityListEvent} represents a selected list of entities.
 * <p>
 * It is assumed to be a set actually (no duplication of items) but the last item in the list is
 * used for subscribers interested by a single entity (such as a GlyphBoard). Hence it is more
 * convenient to use a list than a true set.
 *
 * @param <E> precise type for entities handled
 *
 * @author Hervé Bitteur
 */
public class EntityListEvent<E extends Entity>
        extends UserEvent
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The selected entity list, which may be null. */
    private final List<E> entities;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code EntityListEvent} object.
     *
     * @param source   the entity that created this event
     * @param hint     hint about event origin (or null)
     * @param movement the user movement
     * @param entities the selected collection of entities (or null)
     */
    public EntityListEvent (Object source,
                            SelectionHint hint,
                            MouseMovement movement,
                            List<E> entities)
    {
        super(source, hint, movement);
        this.entities = entities;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getData //
    //---------//
    @Override
    public List<E> getData ()
    {
        return entities;
    }

    //-----------//
    // getEntity //
    //-----------//
    public E getEntity ()
    {
        if ((entities == null) || entities.isEmpty()) {
            return null;
        }

        return entities.get(entities.size() - 1);
    }

    //----------------//
    // internalString //
    //----------------//
    @Override
    protected String internalString ()
    {
        if (entities != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");

            for (Entity entity : entities) {
                sb.append(entity).append(" ");
            }

            sb.append("]");

            return sb.toString();
        } else {
            return "";
        }
    }
}
