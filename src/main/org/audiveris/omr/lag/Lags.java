//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             L a g s                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.lag;

import ij.process.ByteProcessor;

import org.audiveris.omr.util.ByteUtil;

import java.awt.Point;

/**
 * Class {@code Lags} gathers utilities for lags.
 *
 * @author Hervé Bitteur
 */
public class Lags
{

    /** Horizontal (partial) lag. It complements vLag. */
    public static final String HLAG = "hLag";

    /** Vertical (partial) lag. It complements hLag. */
    public static final String VLAG = "vLag";

    /** Horizontal out-of-staves lag. (for ledgers) */
    public static final String LEDGER_LAG = "ledgerLag";

    /** Spot lag. (for beams) */
    public static final String SPOT_LAG = "spotLag";

    /** Symbol lag. (for symbols) */
    public static final String SYMBOL_LAG = "symLag";

    //-------------//
    // buildBuffer //
    //-------------//
    /**
     * Populate a buffer with the content of all provided lags.
     *
     * @param width  width of the target buffer
     * @param height height of the target buffer
     * @param lags   the contributing lags
     * @return the populated buffer
     */
    public static ByteProcessor buildBuffer (int width,
                                             int height,
                                             Lag... lags)
    {
        final Point offset = new Point(0, 0);
        final ByteProcessor buf = new ByteProcessor(width, height);
        ByteUtil.raz(buf); // buf.invert();

        for (Lag lag : lags) {
            for (Section section : lag.getEntities()) {
                section.fillBuffer(buf, offset);
            }
        }

        return buf;
    }
}
