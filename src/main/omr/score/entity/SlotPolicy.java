//----------------------------------------------------------------------------//
//                                                                            //
//                            S l o t P o l i c y                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;


/**
 * Class {@code SlotPolicy} defines the policy to be used for determining time
 * slots within a measure
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum SlotPolicy {
    /** Use Stems as key for slot position */
    STEM_BASED("Stem based"),
    /** Use heads as key for slot position */
    HEAD_BASED("Head based");
    //
    /** For user reading */
    private final String description;

    //------------//
    // SlotPolicy //
    //------------//
    /**
     * Create a new SlotPolicy object
     *
     * @param description the readable description
     */
    SlotPolicy (String description)
    {
        this.description = description;
    }

    //----------//
    // Constant //
    //----------//
    /**
     * Class {@code Constant} is a subclass of
     * {@link omr.constant.Constant}, meant to store a {@link SlotPolicy} value.
     */
    public static class Constant
        extends omr.constant.Constant
    {
        /**
         * Normal constructor
         *
         * @param unit         the enclosing unit
         * @param name         the constant name
         * @param defaultValue the default SlotPolicy value
         * @param description  the semantic of the constant
         */
        public Constant (java.lang.String unit,
                         java.lang.String name,
                         SlotPolicy       defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
            setUnitAndName(unit, name);
        }

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the default SlotPolicy value
         * @param description  the semantic of the constant
         */
        public Constant (SlotPolicy       defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
        }

        /**
         * Set a new value to the constant
         *
         * @param val the new SlotPolicy value
         */
        public void setValue (SlotPolicy val)
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
         * @return the current SlotPolicy value
         */
        public SlotPolicy getValue ()
        {
            return (SlotPolicy) getCachedValue();
        }

        @Override
        protected SlotPolicy decode (java.lang.String str)
        {
            return SlotPolicy.valueOf(str);
        }
    }
}
