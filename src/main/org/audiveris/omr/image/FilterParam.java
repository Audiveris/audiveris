//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      F i l t e r P a r a m                                     //
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
package org.audiveris.omr.image;

import org.audiveris.omr.util.param.Param;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code FilterParam} is a param on FilterDescriptor.
 *
 * @author Hervé Bitteur
 */
public class FilterParam
        extends Param<FilterDescriptor>
{
    //~ Inner Classes ------------------------------------------------------------------------------

    public static class Adapter
            extends XmlAdapter<Adapter.Value, FilterParam>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public Value marshal (FilterParam fp)
                throws Exception
        {
            if (fp == null) {
                return null;
            }

            FilterDescriptor specific = fp.getSpecific();

            if (specific == null) {
                return null;
            }

            Value value = new Value();
            value.filter = specific;

            return value;
        }

        @Override
        public FilterParam unmarshal (Value value)
                throws Exception
        {
            if (value == null) {
                return null;
            }

            FilterParam fp = new FilterParam();
            fp.setSpecific(value.filter);

            return fp;
        }

        //~ Inner Classes --------------------------------------------------------------------------
        protected static class Value
        {
            //~ Instance fields --------------------------------------------------------------------

            @XmlElementRefs({
                @XmlElementRef(type = GlobalDescriptor.class)
                , @XmlElementRef(type = AdaptiveDescriptor.class)
            })
            FilterDescriptor filter;
        }
    }
}
