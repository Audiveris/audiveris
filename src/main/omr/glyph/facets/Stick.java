//----------------------------------------------------------------------------//
//                                                                            //
//                                 S t i c k                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;


/**
 * Interface {@code } describes a stick, a special kind of glyph, either
 * horizontal or vertical, as an aggregation of sections. Besides usual
 * positions and coordinates, a stick exhibits its approximating Line which is
 * the least-square fitted line on all points contained in the stick.
 *
 * <ul> <li> Staff lines, ledgers, alternate ends are examples of horizontal
 * sticks </li>
 *
 * <li> Bar lines, stems are examples of vertical sticks </li> </ul>
 *
 * @author Hervé Bitteur
 */
public interface Stick
    extends Glyph, GlyphAlignment
{
}
