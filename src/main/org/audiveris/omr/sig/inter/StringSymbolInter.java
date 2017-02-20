//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S t r i n g S y m b o l I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.sig.inter;

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
 * <li>Word '3' and DIGIT_3 (and TUPLET_THREE?)</li>
 * <li>Word '4' and DIGIT_4</li>
 * <li>Word '6' and TUPLET_SIX?</li>
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
