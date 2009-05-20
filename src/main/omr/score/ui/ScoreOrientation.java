//----------------------------------------------------------------------------//
//                                                                            //
//                      S c o r e O r i e n t a t i o n                       //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.ui;


/**
 * Class <code>ScoreOrientation</code> defines the orientation used for systems
 * layout in the score view
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public enum ScoreOrientation {
    /** Systems displayed side by side */
    HORIZONTAL("Horizontal"),
    /** System displayed one above the other */
    VERTICAL("Vertical");
    //
    public final String description;

    //------------------//
    // ScoreOrientation //
    //------------------//
    private ScoreOrientation (String description)
    {
        this.description = description;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return description;
    }

    //----------//
    // Constant //
    //----------//
    /**
     * Class <code>Constant</code> is a subclass of
     * {@link omr.constant.Constant}, meant to persist a
     * {@link ScoreOrientation} value.
     */
    public static class Constant
        extends omr.constant.Constant
    {
        /**
         * Normal constructor
         *
         * @param unit         the enclosing unit
         * @param name         the constant name
         * @param defaultValue the default orientation value
         * @param description  the semantic of the constant
         */
        public Constant (java.lang.String unit,
                         java.lang.String name,
                         ScoreOrientation defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
            setUnitAndName(unit, name);
        }

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the default orientation value
         * @param description  the semantic of the constant
         */
        public Constant (ScoreOrientation defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
        }

        /**
         * Set a new value to the constant
         *
         * @param val the new orientation value
         */
        public void setValue (ScoreOrientation val)
        {
            setTuple(val.toString(), val);
        }

        /**
         * Retrieve the current constant value
         *
         * @return the current orientation value
         */
        public ScoreOrientation getValue ()
        {
            return (ScoreOrientation) getCachedValue();
        }

        @Override
        protected ScoreOrientation decode (java.lang.String str)
        {
            return ScoreOrientation.valueOf(str.toUpperCase());
        }

        /**
         * Set a new value to the constant
         *
         * @param string the new orientation value
         */
        @Override
        public void setValue (java.lang.String string)
        {
            setValue(decode(string));
        }
    }
}
