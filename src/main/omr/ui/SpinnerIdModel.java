//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S p i n n e r I d M o d e l                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.util.Entity;
import omr.util.EntityIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import javax.swing.AbstractSpinnerModel;

/**
 * Class {@code SpinnerIdModel} is an ID spinner model backed by a {@link EntityIndex}.
 *
 * @author Hervé Bitteur
 *
 * @param <E> precise type for handled entity
 */
public class SpinnerIdModel<E extends Entity>
        extends AbstractSpinnerModel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SpinnerIdModel.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying entity index. */
    private final EntityIndex<E> index;

    /** Current entity id value. */
    private Integer currentId;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SpinnerIdModel} object.
     *
     * @param index the underlying entity index
     */
    public SpinnerIdModel (EntityIndex<E> index)
    {
        this.index = index;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public Integer getNextValue ()
    {
        return index.getIdValueAfter(currentId);
    }

    @Override
    public Integer getPreviousValue ()
    {
        return index.getIdValueBefore(currentId);
    }

    @Override
    public Integer getValue ()
    {
        return currentId;
    }

    @Override
    public void setValue (Object value)
    {
        Integer id = (Integer) value;

        if (id == null) {
            id = 0;
        }

        boolean ok = false;

        if (id == 0) {
            ok = true;
        } else {
            E entity = index.getEntity(index.getPrefix() + id);

            if (entity != null) {
                ok = true;
            }
        }

        if (ok) {
            if (!Objects.equals(currentId, id)) {
                currentId = id;
                fireStateChanged();
            }
        } else {
            logger.warn("Invalid entity id: {}", id);
        }
    }
}
