//----------------------------------------------------------------------------//
//                                                                            //
//                      B a s i c T r a n s l a t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code BasicTranslation} is the basic implementation of the
 * translation facet
 *
 * @author Hervé Bitteur
 */
class BasicTranslation
    extends BasicFacet
    implements GlyphTranslation
{
    //~ Instance fields --------------------------------------------------------

    /** Set of translation(s) of this glyph on the score side */
    private Set<Object> translations = new HashSet<>();

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // BasicTranslation //
    //------------------//
    /**
     * Create a new BasicTranslation object
     *
     * @param glyph our glyph
     */
    public BasicTranslation (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // addTranslation //
    //----------------//
    @Override
    public void addTranslation (Object entity)
    {
        translations.add(entity);
    }

    //-------------------//
    // clearTranslations //
    //-------------------//
    @Override
    public void clearTranslations ()
    {
        translations.clear();
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        if (!translations.isEmpty()) {
            System.out.println("   translations=" + translations);
        }
    }

    //-----------------//
    // getTranslations //
    //-----------------//
    @Override
    public Collection<Object> getTranslations ()
    {
        return translations;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        clearTranslations();
    }

    //--------------//
    // isTranslated //
    //--------------//
    @Override
    public boolean isTranslated ()
    {
        return !translations.isEmpty();
    }

    //----------------//
    // setTranslation //
    //----------------//
    @Override
    public void setTranslation (Object entity)
    {
        translations.clear();
        addTranslation(entity);
    }
}
