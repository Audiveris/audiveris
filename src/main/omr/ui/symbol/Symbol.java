//----------------------------------------------------------------------------//
//                                                                            //
//                                S y m b o l                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.score.common.PixelPoint;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.Icon;

/**
 * Interface {@code Symbol} handles the display of a symbol.
 * A Symbol can provide several features:<ul>
 *
 * <li>It can be used as an <b>icon</b> for buttons, menus, etc. For that
 * purpose, {@code Symbol} implements the {@link Icon} interface.</li>
 *
 * <li>It can be used as an <b>image</b> for precise drawing on score views,
 * whatever the desired scale and display ratio. See {@link #buildImage} and
 * {@link #paintSymbol} methods.</li>
 *
 * <li>It may also be used to <b>train</b> the glyph evaluator when we don't
 * have enough "real" glyphs available.</li>
 *
 * <li>It may also be used to convey the <b>reference point</b> of that shape.
 * Most of shapes have no reference point, and thus we use their area center,
 * which is the center of their bounding box.
 * However, a few shapes (clefs to precisely position them  on the staff,
 * head/flags combos to handle the precise position of the head part) need a
 * very precise reference center (actually the y ordinate) which is somewhat
 * different from the area center. See {@link #getRefPoint}.</li>
 *
 * @author Hervé Bitteur
 */
public interface Symbol
    extends Icon
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Report the height of the related icon
     * @return the height of the icon image in pixels
     */
    public int getIconHeight ();

    /**
     * Report the width of the icon (used by swing when painting)
     * @return the icon image width in pixels
     */
    public int getIconWidth ();

    /**
     * Paint the symbol that represents the related shape, using the scaled
     * font and context, the symbol being aligned at provided location
     * @param g graphic context
     * @param font properly-scaled font (for interline & zoom)
     * @param location where to paint the shape with provided alignment
     * @param alignment the way the symbol is aligned wrt the location
     */
    public void paintSymbol (Graphics2D g,
                             MusicFont  font,
                             PixelPoint location,
                             Alignment  alignment);

    /**
     * Report the icon image, suitable for icon display
     * @return the image meant for icon display
     */
    BufferedImage getIconImage ();

    /**
     * Report the symbol reference point, which is usually the area center,
     * but somewhat different for some symbols (such as flats)
     * @param area the contour box of the entity (symbol or glyph)
     * @return the reference point
     */
    PixelPoint getRefPoint (Rectangle area);

    /**
     * Build the image that represents the related shape, using the scaled
     * font. The main difficulty is to determine up-front the size of the image
     * to allocate.
     * @param font properly-scaled font (for interline & zoom)
     * @return the image built, or null if failed
     */
    SymbolImage buildImage (MusicFont font);
}
