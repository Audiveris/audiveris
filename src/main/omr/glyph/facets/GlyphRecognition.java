//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                G l y p h R e c o g n i t i o n                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.Shape;

/**
 * Interface {@code GlyphRecognition} defines a facet that deals with
 * the shape recognition of a glyph.
 *
 * @author Hervé Bitteur
 */
interface GlyphRecognition
        extends GlyphFacet
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the registered glyph shape.
     *
     * @return the glyph shape, which may be null
     */
    Shape getShape ();

    /**
     * Setter for the glyph shape (Algorithm assumed).
     *
     * @param shape the assigned shape, which may be null
     */
    void setShape (Shape shape);
}
