//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I n t e g e r P a r a m                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class <code>IntegerParam</code> is a param for integer.
 *
 * @author Hervé Bitteur
 */
public class IntegerParam
        extends Param<Integer>
{
    //~ Constructors -------------------------------------------------------------------------------

    public IntegerParam (Object scope)
    {
        super(scope);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    /**
     * JAXB adapter for IntegerParam type.
     */
    public static class JaxbAdapter
            extends XmlAdapter<Integer, IntegerParam>
    {
        @Override
        public Integer marshal (IntegerParam param)
            throws Exception
        {
            if (param == null) {
                return null;
            }

            return param.getSpecific();
        }

        @Override
        public IntegerParam unmarshal (Integer value)
            throws Exception
        {
            if (value == null) {
                return null;
            }

            final IntegerParam param = new IntegerParam(null);
            param.setSpecific(value);

            return param;
        }
    }
}
