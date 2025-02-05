//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      F i l t e r P a r a m                                     //
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
package org.audiveris.omr.image;

import org.audiveris.omr.util.param.Param;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class <code>FilterParam</code> is a param on FilterDescriptor.
 *
 * @author Hervé Bitteur
 */
public class FilterParam
        extends Param<FilterDescriptor>
{
    //~ Constructors -------------------------------------------------------------------------------

    public FilterParam (Object scope)
    {
        super(scope);
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    /**
     * JAXB adapter.
     */
    public static class JaxbAdapter
            extends XmlAdapter<JaxbAdapter.FilterDescriptorValue, FilterParam>
    {
        @Override
        public FilterDescriptorValue marshal (FilterParam fp)
            throws Exception
        {
            if (fp == null) {
                return null;
            }

            FilterDescriptor specific = fp.getSpecific();

            if (specific == null) {
                return null;
            }

            FilterDescriptorValue value = new FilterDescriptorValue();
            value.filter = specific;

            return value;
        }

        @Override
        public FilterParam unmarshal (FilterDescriptorValue value)
            throws Exception
        {
            if (value == null) {
                return null;
            }

            FilterParam fp = new FilterParam(Param.GLOBAL_SCOPE);
            fp.setSpecific(value.filter);

            return fp;
        }

        /**
         * Class <code>FilterDescriptorValue</code> is meant to [un]marshal
         * the description of a binarization filter (global or adaptive).
         *
         * @see GlobalDescriptor
         * @see AdaptiveDescriptor
         */
        protected static class FilterDescriptorValue
        {
            @XmlElementRefs(
            {
                    @XmlElementRef(type = GlobalDescriptor.class),
                    @XmlElementRef(type = AdaptiveDescriptor.class) })
            FilterDescriptor filter;
        }
    }
}
