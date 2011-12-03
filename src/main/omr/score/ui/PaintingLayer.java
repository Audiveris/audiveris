//----------------------------------------------------------------------------//
//                                                                            //
//                         P a i n t i n g L a y e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;


/**
 * Enum {@code PaintingLayer} defines layers to be painted
 */
public enum PaintingLayer {
    /** Input data: image or glyphs */
    INPUT,
    /** Both input and output */
    INPUT_OUTPUT, 
    /** Output data: score entities */
    OUTPUT;
    //--------------------------------------------------------------------------

    //----------//
    // Constant //
    //----------//
    /**
     * Class {@code Constant} is a subclass of
     * {@link omr.constant.Constant}, meant to store a {@link PaintingLayer}
     * value.
     */
    public static class Constant
        extends omr.constant.Constant
    {
        /**
         * Normal constructor
         *
         * @param unit         the enclosing unit
         * @param name         the constant name
         * @param defaultValue the default Layer value
         * @param description  the semantic of the constant
         */
        public Constant (java.lang.String unit,
                         java.lang.String name,
                         PaintingLayer    defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
            setUnitAndName(unit, name);
        }

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later
         *
         * @param defaultValue the default Layer value
         * @param description  the semantic of the constant
         */
        public Constant (PaintingLayer    defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
        }

        /**
         * Set a new value to the constant
         *
         * @param val the new Layer value
         */
        public void setValue (PaintingLayer val)
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
         * @return the current Layer value
         */
        public PaintingLayer getValue ()
        {
            return (PaintingLayer) getCachedValue();
        }

        @Override
        protected PaintingLayer decode (java.lang.String str)
        {
            return PaintingLayer.valueOf(str);
        }
    }
}
