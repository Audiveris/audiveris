//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  E n t i t y L i s t E v e n t                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.util.Entity;

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
