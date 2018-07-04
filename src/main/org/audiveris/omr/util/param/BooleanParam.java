//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B o o l e a n P a r a m                                    //
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

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code BooleanParam} is a Param for Boolean.
 *
 * @author Hervé Bitteur
 */
public class BooleanParam
        extends Param<Boolean>
{
    //~ Inner Classes ------------------------------------------------------------------------------

    public static class Adapter
            extends XmlAdapter<String, BooleanParam>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String marshal (BooleanParam val)
                throws Exception
        {
            if (val == null) {
                return null;
            }

            Boolean spc = val.getSpecific();

            if (spc == null) {
                return null;
            }

            return spc.toString();
        }

        @Override
        public BooleanParam unmarshal (String str)
                throws Exception
        {
            if ((str == null) || str.trim().isEmpty()) {
                return null;
            }

            BooleanParam b = new BooleanParam();
            b.setSpecific(Boolean.parseBoolean(str));

            return b;
        }
    }
}
