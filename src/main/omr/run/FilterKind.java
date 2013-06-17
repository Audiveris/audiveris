//----------------------------------------------------------------------------//
//                                                                            //
//                             F i l t e r K i n d                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code FilterKind} handles the various kinds of
 * {@link PixelFilter} implementations.
 */
public enum FilterKind
{

    GLOBAL("Basic filter using a global threshold", GlobalFilter.class),
    ADAPTIVE(
    "Adaptive filter using a local threshold",
    AdaptiveFilter.getImplementationClass());

    /** Description. */
    public final String description;

    /** Implementing class. */
    public final Class<?> classe;

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            FilterKind.class);

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
     * Class {@code Constant} is a {@link omr.constant.Constant},
     * meant to store a {@link FilterKind} value.
     */
    public static class Constant
            extends omr.constant.Constant
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
