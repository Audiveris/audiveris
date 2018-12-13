//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S p i n n e r I d M o d e l                                  //
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

import org.audiveris.omr.util.Entity;
import org.audiveris.omr.util.EntityIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import javax.swing.AbstractSpinnerModel;

/**
 * Class {@code SpinnerIdModel} is an ID spinner model backed by a {@link EntityIndex}.
 *
 * @author Hervé Bitteur
 * @param <E> precise type for handled entity
 */
public class SpinnerIdModel<E extends Entity>
        extends AbstractSpinnerModel
{

    private static final Logger logger = LoggerFactory.getLogger(SpinnerIdModel.class);

    /** Underlying entity index. */
    private final EntityIndex<E> index;

    /** Current entity id value. */
    private Integer currentId;

    /**
     * Creates a new {@code SpinnerIdModel} object.
     *
     * @param index the underlying entity index
     */
    public SpinnerIdModel (EntityIndex<E> index)
    {
        this.index = index;
    }

    @Override
    public Integer getNextValue ()
    {
        final int next = index.getIdAfter(currentId);

        return (next != 0) ? next : null;
    }

    @Override
    public Integer getPreviousValue ()
    {
        final int prev = index.getIdBefore(currentId);

        return (prev != 0) ? prev : null;
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
        } else if (index != null) {
            E entity = index.getEntity(id);

            if (entity != null) {
                ok = true;
            }
        } else {
            ok = true;
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
