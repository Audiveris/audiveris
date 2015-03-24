//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   C l a s s i f i e r K i n d                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

/**
 * Enum {@code ClassifierKind} defines the kinds of glyph classifiers.
 *
 * @author Hervé Bitteur
 */
public enum ClassifierKind
{

    /** Neural network classifier. */
    NEURAL,
    /** Naive bayesian classifier. */
    BAYESIAN;

    public static class Constant
            extends omr.constant.Constant
    {

        public Constant (ClassifierKind defaultValue,
                         java.lang.String description)
        {
            super(null, defaultValue.toString(), description);
        }

        public ClassifierKind getValue ()
        {
            return (ClassifierKind) getCachedValue();
        }

        public void setValue (ClassifierKind val)
        {
            setTuple(val.toString(), val);
        }

        @Override
        public void setValue (java.lang.String string)
        {
            setValue(decode(string));
        }

        @Override
        protected ClassifierKind decode (java.lang.String str)
        {
            return ClassifierKind.valueOf(str);
        }
    }
}
