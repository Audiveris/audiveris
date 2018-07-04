//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         A r c S h a p e                                        //
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
package org.audiveris.omr.sheet.curve;

/**
 * Shape detected for arc.
 */
public enum ArcShape
{
    /**
     * Not yet known.
     */
    UNKNOWN(false, false),
    /**
     * Short arc.
     * Can be tested as slur or wedge extension.
     */
    SHORT(true, true),
    /**
     * Long portion of slur.
     * Can be part of slur only.
     */
    SLUR(true, false),
    /**
     * Long straight line.
     * Can be part of wedge (and slur).
     */
    LINE(true, true),
    /**
     * Short portion of staff line.
     * Can be part of slur only.
     */
    STAFF_ARC(true, false),
    /**
     * Long arc, but no shape detected.
     * Cannot be part of slur/wedge
     */
    IRRELEVANT(false, false);

    private final boolean forSlur; // OK for slur

    private final boolean forWedge; // OK for wedge

    ArcShape (boolean forSlur,
              boolean forWedge)
    {
        this.forSlur = forSlur;
        this.forWedge = forWedge;
    }

    public boolean isSlurRelevant ()
    {
        return forSlur;
    }

    public boolean isWedgeRelevant ()
    {
        return forWedge;
    }
}
