//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           T e m p o                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.score;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.util.param.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code Tempo} handles the default tempo value.
 *
 * @author Hervé Bitteur
 */
public abstract class Tempo
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Tempo.class);

    /** Default parameter. */
    public static final Param<Integer> defaultTempo = new Default();

    //~ Constructors -------------------------------------------------------------------------------
    /** Not meant to be instantiated. */
    private Tempo ()
    {
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer defaultTempo = new Constant.Integer(
                "QuartersPerMn",
                120,
                "Default tempo, stated in number of quarters per minute");
    }

    //---------//
    // Default //
    //---------//
    private static class Default
            extends Param<Integer>
    {

        @Override
        public Integer getSpecific ()
        {
            if (constants.defaultTempo.isSourceValue()) {
                return null;
            } else {
                return constants.defaultTempo.getValue();
            }
        }

        @Override
        public Integer getValue ()
        {
            return constants.defaultTempo.getValue();
        }

        @Override
        public boolean isSpecific ()
        {
            return !constants.defaultTempo.isSourceValue();
        }

        @Override
        public boolean setSpecific (Integer specific)
        {
            if (!getValue().equals(specific)) {
                constants.defaultTempo.setValue(specific);
                logger.info("Default tempo is now {}", specific);

                return true;
            } else {
                return false;
            }
        }
    }
}
