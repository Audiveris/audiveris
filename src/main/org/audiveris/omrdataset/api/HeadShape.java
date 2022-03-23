//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        H e a d S h a p e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omrdataset.api;

/**
 * Class {@code HeadShape} is a small subset of OmrShape, meant for Head processing.
 *
 * @see OmrShape
 *
 * @author Hervé Bitteur
 */
public enum HeadShape
{
    none,

    noteheadBlack,
    noteheadBlackSmall,
    noteheadHalf,
    noteheadHalfSmall,
    noteheadWhole,
    noteheadWholeSmall,
    noteheadDoubleWhole,
    noteheadDoubleWholeSmall,
    noteheadXBlack,
    noteheadXHalf,
    noteheadXWhole;

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // toOmrShape //
    //------------//
    public OmrShape toOmrShape ()
    {
        switch (this) {
        default:
        case none:
            return OmrShape.none;
        case noteheadBlack:
            return OmrShape.noteheadBlack;
        case noteheadBlackSmall:
            return OmrShape.noteheadBlackSmall;
        case noteheadHalf:
            return OmrShape.noteheadHalf;
        case noteheadHalfSmall:
            return OmrShape.noteheadHalfSmall;
        case noteheadWhole:
            return OmrShape.noteheadWhole;
        case noteheadWholeSmall:
            return OmrShape.noteheadWholeSmall;
        case noteheadDoubleWhole:
            return OmrShape.noteheadDoubleWhole;
        case noteheadDoubleWholeSmall:
            return OmrShape.noteheadDoubleWholeSmall;
        case noteheadXBlack:
            return OmrShape.noteheadXBlack;
        case noteheadXHalf:
            return OmrShape.noteheadXHalf;
        case noteheadXWhole:
            return OmrShape.noteheadXWhole;
        }
    }

    //-------------//
    // toHeadShape //
    //-------------//
    public static HeadShape toHeadShape (OmrShape omrShape)
    {
        if (omrShape == null) {
            return null;
        }

        switch (omrShape) {
        case none:
            return none;
        case noteheadBlack:
            return noteheadBlack;
        case noteheadBlackSmall:
            return noteheadBlackSmall;
        case noteheadHalf:
            return noteheadHalf;
        case noteheadHalfSmall:
            return noteheadHalfSmall;
        case noteheadWhole:
            return noteheadWhole;
        case noteheadWholeSmall:
            return noteheadWholeSmall;
        case noteheadDoubleWhole:
            return noteheadDoubleWhole;
        case noteheadDoubleWholeSmall:
            return noteheadDoubleWholeSmall;
        case noteheadXBlack:
            return noteheadXBlack;
        case noteheadXHalf:
            return noteheadXHalf;
        case noteheadXWhole:
            return noteheadXWhole;
        default:
            return null;
        }
    }
}
