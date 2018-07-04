//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               C o n s t a n t B a s e d P a r a m                              //
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
package org.audiveris.omr.util.param;

import org.audiveris.omr.constant.Constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ConstantBasedParam} is a {@link Param} backed by an application constant
 * as is the case for many default Param.
 *
 * @author Hervé Bitteur
 *
 * @param <E> type for value
 * @param <C> type for value constant
 */
public class ConstantBasedParam<E, C extends Constant>
        extends Param<E>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ConstantBasedParam.class);

    //~ Instance fields ----------------------------------------------------------------------------
    private final C cst;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ConstantBasedParam} object.
     *
     * @param constant the underlying constant
     */
    public ConstantBasedParam (C constant)
    {
        this.cst = constant;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public E getSourceValue ()
    {
        return (E) cst.getSourceValue();
    }

    @Override
    public E getSpecific ()
    {
        if (cst.isSourceValue()) {
            return null;
        } else {
            return (E) cst.getValue();
        }
    }

    @Override
    public E getValue ()
    {
        return (E) cst.getValue();
    }

    @Override
    public boolean isSpecific ()
    {
        return !cst.isSourceValue();
    }

    @Override
    public boolean setSpecific (E specific)
    {
        if (!getValue().equals(specific)) {
            if (specific == null) {
                if (!cst.isSourceValue()) {
                    cst.resetToSource();
                    logger.info(
                            "Default " + cst.getDescription() + " reset to {}",
                            cst.getSourceValue());
                }
            } else {
                cst.setValue(specific);
                logger.info("Default " + cst.getDescription() + " set to {}", cst.getValue());
            }

            return true;
        }

        return false;
    }
}
