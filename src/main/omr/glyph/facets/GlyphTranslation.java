//----------------------------------------------------------------------------//
//                                                                            //
//                      G l y p h T r a n s l a t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.score.entity.PartNode;

import java.util.Collection;

/**
 * Interface {@code GlyphTranslation} defines a facet dealing with the
 * translation of a glyph into its score entity counter-part(s).
 *
 * @author Hervé Bitteur
 */
interface GlyphTranslation
        extends GlyphFacet
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Add a score entity as a translation for this glyph
     *
     * @param entity the counterpart of this glyph on the score side
     */
    void addTranslation (PartNode entity);

    /**
     * Remove all the links to score entities
     */
    void clearTranslations ();

    /**
     * Report the collection of score entities this glyph contributes to
     *
     * @return the collection of entities that are translations of this glyph
     */
    Collection<PartNode> getTranslations ();

    /**
     * Report whether this glyph is translated to a score entity
     *
     * @return true if this glyph is translated to score
     */
    boolean isTranslated ();

    /**
     * Assign a unique score translation for this glyph
     *
     * @param entity the score entity that is a translation of this glyph
     */
    void setTranslation (PartNode entity);
}
