//----------------------------------------------------------------------------//
//                                                                            //
//                                 G l y p h                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.check.Checkable;

/**
 * Interface {@code Glyph} represents any glyph found, such as stem, ledger,
 * accidental, note head, etc...
 *
 * <p>A Glyph is basically a collection of sections. It can be split into
 * smaller glyphs, which may later be re-assembled into another instance of
 * glyph. There is a means, based on a simple signature (weight and bounding
 * box), to detect if the glyph at hand is identical to a previous one, which is
 * then reused.
 *
 * <p>A Glyph can be stored on disk and reloaded in order to train a glyph
 * evaluator.
 *
 * @author Hervé Bitteur
 */
public interface Glyph
    extends Checkable, GlyphAdministration, GlyphComposition, GlyphDisplay,
                GlyphEnvironment, GlyphGeometry, GlyphRecognition,
                GlyphTranslation
{
}
