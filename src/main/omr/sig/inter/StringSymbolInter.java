//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S t r i n g S y m b o l I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

/**
 * Interface {@code StringSymbolInter} flags a Symbol Inter which can be mistaken for a
 * word, and vice versa.
 * Examples of such shapes:
 * <ul>
 * <li>Word 'p' and PLUCK_P</li>
 * <li>Word 'i' and PLUCK_I</li>
 * <li>Word 'm' and PLUCK_M and DYNAMICS_CHAR_M</li>
 * <li>Word 'a' and PLUCK_A</li>
 * <li>Word 'f' and DYNAMICS_F</li>
 * <li>Word 'P' and DYNAMICS_P</li>
 * <li>Word '0' and DIGIT_0</li>
 * <li>Word '1' and DIGIT_1</li>
 * <li>Word '2' and DIGIT_2</li>
 * <li>Word '3' and DIGIT_3 (and TUPLET_3?)</li>
 * <li>Word '4' and DIGIT_4</li>
 * <li>Word '6' and TUPLET_6?</li>
 * <li>Word 'I' and ROMAN_I</li>
 * <li>Word 'II' and ROMAN_II</li>
 * <li>Word 'III' and ROMAN_III</li>
 * <li>Word 'IV' and ROMAN_IV</li>
 * <li>Word 'V' and ROMAN_V</li>
 * <li>Word 'VI' and ROMAN_VI</li>
 * <li>Word 'VII' and ROMAN_VII</li>
 * <li>Word 'VIII' and ROMAN_VIII</li>
 * <li>Word 'IX' and ROMAN_IX</li>
 * <li>Word 'X' and ROMAN_X</li>
 * <li>Word 'XI' and ROMAN_XI</li>
 * <li>Word 'XII' and ROMAN_XII</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public interface StringSymbolInter
        extends Inter
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the string that resembles the symbol.
     *
     * @return the symbol string
     */
    String getSymbolString ();
}
