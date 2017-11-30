//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          S y m b o l                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

/**
 * Interface {@code Symbol} handles the display of a symbol.
 * A Symbol can provide several features:<ul>
 *
 * <li>It can be used as an <b>icon</b> for buttons, menus, etc. For that purpose, {@code Symbol}
 * implements the {@link Icon} interface.</li>
 *
 * <li>It can be used as an <b>image</b> for precise drawing on score views, whatever the desired
 * scale and display ratio. See {@link #buildImage} and {@link #paintSymbol} methods.</li>
 *
 * <li>It may also be used to <b>train</b> the glyph classifier when we don't have enough "real"
 * glyphs available.</li>
 *
 * <li>It may also be used to convey the <b>reference point</b> of that shape.
 * Most shapes have no reference point, and thus we use their area center, which is the center of
 * their bounding box.
 * However, a few shapes (e.g. clefs to precisely position them on the staff) need a very precise
 * reference center (actually the y ordinate) which is somewhat different from the area center. See
 * {@link #getRefPoint}.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public interface Symbol
        extends Icon
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Paint the symbol that represents the related shape, using the scaled font and
     * context, the symbol being aligned at provided location.
     *
     * @param g         graphic context
     * @param font      properly-scaled font (for interline &amp; zoom)
     * @param location  where to paint the shape with provided alignment
     * @param alignment the way the symbol is aligned WRT the location
     */
    public void paintSymbol (Graphics2D g,
                             MusicFont font,
                             Point location,
                             Alignment alignment);

    /**
     * Build the image that represents the related shape, using the scaled font.
     * The main difficulty is to determine up-front the size of the image to allocate.
     *
     * @param font properly-scaled font (for interline &amp; zoom)
     * @return the image built, or null if failed
     */
    SymbolImage buildImage (MusicFont font);

    /**
     * Report the symbol mass center.
     *
     * @param box the contour box of the entity (symbol or glyph)
     * @return the mass center
     */
    Point getCentroid (Rectangle box);

    /**
     * Report the bounding dimension of this symbol for the provided font.
     *
     * @param font (scaled) music font
     * @return the bounding dimension
     */
    Dimension getDimension (MusicFont font);

    /**
     * Report the icon image, suitable for icon display.
     *
     * @return the image meant for icon display
     */
    BufferedImage getIconImage ();

    /**
     * Report the symbol reference point, which is usually the area center, but somewhat
     * different for some symbols (such as flats).
     *
     * @param area the contour box of the entity (symbol or glyph)
     * @return the reference point
     */
    Point getRefPoint (Rectangle area);
}
