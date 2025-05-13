//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   M u s i c F o n t T e s t                                    //
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
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.ui.symbol.Symbols.CodeRange;

import org.junit.Test;

/**
 * Class <code>MusicFontTest</code> generates a PDF file for each known font family,
 * with all non-degenerated symbols from each font.
 *
 * @author Hervé Bitteur
 */
public class MusicFontTest
{
    /**
     * Printout of each MusicFont family.
     */
    @Test
    public void printAllFamilies ()
    {
        for (MusicFamily family : MusicFamily.values()) {
            System.out.println("family: " + family);
            final MusicFont musicFont = MusicFont.getMusicFont(family, 64);
            final FontPrintOut fp = new FontPrintOut(musicFont);
            fp.start();

            for (CodeRange range : family.getSymbols().getCodeRanges()) {
                for (int i = range.start; i <= range.stop; i++) {
                    fp.printSymbol(i);
                }
            }

            fp.stop();
        }
    }
}
