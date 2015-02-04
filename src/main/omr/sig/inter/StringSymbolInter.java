//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S t r i n g S y m b o l I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
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
 * <li>Word 'p' & PLUCK_P</li>
 * <li>Word 'i' & PLUCK_I</li>
 * <li>Word 'm' & PLUCK_M & DYNAMICS_CHAR_M</li>
 * <li>Word 'a' & PLUCK_A</li>
 * <li>Word 'f' & DYNAMICS_F</li>
 * <li>Word 'P' & DYNAMICS_P</li>
 * <li>Word '0' & DIGIT_0</li>
 * <li>Word '1' & DIGIT_1</li>
 * <li>Word '2' & DIGIT_2</li>
 * <li>Word '3' & DIGIT_3 (& TUPLET_3?)</li>
 * <li>Word '4' & DIGIT_4</li>
 * <li>Word '6' & TUPLET_6?</li>
 * <li>Word 'I' & ROMAN_I</li>
 * <li>Word 'II' & ROMAN_II</li>
 * <li>Word 'III' & ROMAN_III</li>
 * <li>Word 'IV' & ROMAN_IV</li>
 * <li>Word 'V' & ROMAN_V</li>
 * <li>Word 'VI' & ROMAN_VI</li>
 * <li>Word 'VII' & ROMAN_VII</li>
 * <li>Word 'VIII' & ROMAN_VIII</li>
 * <li>Word 'IX' & ROMAN_IX</li>
 * <li>Word 'X' & ROMAN_X</li>
 * <li>Word 'XI' & ROMAN_XI</li>
 * <li>Word 'XII' & ROMAN_XII</li>
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
