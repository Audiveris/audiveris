//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S i m p l e S h e e t P a i n t e r                              //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.sheet.Sheet;

import java.awt.Graphics2D;

/**
 * Interface {@code SimpleSheetPainter} is a simple painter working on a sheet.
 *
 * @author Hervé Bitteur
 */
public interface SimpleSheetPainter
{

    /**
     * Paint some sheet-related data to the provided graphics context.
     *
     * @param sheet the related sheet
     * @param g     the output graphics context
     */
    void paint (Sheet sheet,
                Graphics2D g);
}
