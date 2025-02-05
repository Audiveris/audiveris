//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        I n p u t M o d e                                       //
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
package org.audiveris.omr.sheet;

/**
 * Class <code>InputMode</code> is meant to specify processing mode based on input quality.
 *
 * @author Hervé Bitteur
 */
public enum InputMode
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Standard quality. */
    STANDARD,

    /** Significant gaps in black pixels. */
    POOR,

    /** Almost no black pixels, meant for beam umbrella in poor mode. */
    VERY_POOR;
}
