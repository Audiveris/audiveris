//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       F i l t e r K i n d                                      //
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
package org.audiveris.omr.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code FilterKind} handles the various kinds of {@link PixelFilter}
 * implementations.
 */
public enum FilterKind
{
    GLOBAL("Basic filter using a global threshold", GlobalFilter.class),
    ADAPTIVE("Adaptive filter using a local threshold", AdaptiveFilter.getImplementationClass());

    /** Description. */
    public final String description;

    /** Implementing class. */
    public final Class<?> classe;

    private static final Logger logger = LoggerFactory.getLogger(FilterKind.class);

    //------------//
    // FilterKind //
    //------------//
    FilterKind (String description,
                Class<?> classe)
    {
        this.description = description;
        this.classe = classe;
    }

    //----------//
    // Constant //
    //----------//
    /**
     * Class {@code Constant} is a {@link org.audiveris.omr.constant.Constant},
     * meant to store a {@link FilterKind} value.
     */
    public static class Constant
            extends org.audiveris.omr.constant.Constant
    {

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the default FilterKind value
         * @param description  the semantic of the constant
         */
        public Constant (FilterKind defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
        }

        /**
         * Set a new value to the constant
         *
         * @param val the new FilterKind value
         */
        public void setValue (FilterKind val)
        {
            setTuple(val.toString(), val);
        }

        @Override
        public void setValue (java.lang.String string)
        {
            setValue(decode(string));
        }

        /**
         * Retrieve the current constant value
         *
         * @return the current FilterKind value
         */
        public FilterKind getValue ()
        {
            return (FilterKind) getCachedValue();
        }

        @Override
        protected FilterKind decode (java.lang.String str)
        {
            return FilterKind.valueOf(str);
        }
    }
}
