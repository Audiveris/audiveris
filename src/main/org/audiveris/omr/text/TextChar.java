//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T e x t C h a r                                         //
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
package org.audiveris.omr.text;

import java.awt.Rectangle;

/**
 * Class {@code TextChar} manages information about a OCR-decoded character.
 *
 * @author Hervé Bitteur
 */
public class TextChar
        extends TextItem
{

    /**
     * Creates a new TextChar object.
     *
     * @param bounds the bounding box of this character WRT the decoded image
     * @param value  the character string value
     */
    public TextChar (Rectangle bounds,
                     String value)
    {
        super(bounds, value);
    }

    @Override
    public void setValue (String value)
    {
        super.setValue(value);
    }
}
