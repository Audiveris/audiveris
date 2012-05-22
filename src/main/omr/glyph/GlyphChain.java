//----------------------------------------------------------------------------//
//                                                                            //
//                            G l y p h C h a i n                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2012. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.Glyph;

import java.util.Collection;
import java.util.SortedSet;

/**
 * Interface {@code GlyphChain} defines a dynamic chain of active
 * glyphs called the chain items.
 * <p>A glyph can be shared by several GlyphChain instances at a time.</p>
 * <p>One can dynamically add or remove items to a GlyphChain.</p>
 *
 * @author Hervé Bitteur
 */
public interface GlyphChain
{
    //~ Methods ----------------------------------------------------------------

    void addAllItems (Collection<?extends Glyph> glyphs);

    /**
     * Add a glyph item to the chain.
     * @param glyph the glyph to add
     * @return true if glyph did not already exist in the chain
     */
    boolean addItem (Glyph glyph);

    /**
     * Report the single compound glyph that gathers all sections.
     * @return the single compound glyph (which is the single item if there
     * is only one item, or a compound of all items otherwise).
     * This compound is a registered glyph, therefore it is unique.
     */
    Glyph getCompound ();

    /**
     * Report the first item of the chain
     * @return chain first item
     */
    Glyph getFirstItem ();

    /**
     * Report the item right after the provided one in the chain of
     * items.
     * @param start the item whose successor is desired, or null if the very
     * first item is desired
     * @return the item that follow 'start', or null if none exists
     */
    Glyph getItemAfter (Glyph start);

    /**
     * Report the item right before the provided stop item in the chain
     * of items.
     * @param stop the item whose preceding instance is desired
     * @return the very last item found before the 'stop' item, or null
     */
    Glyph getItemBefore (Glyph stop);

    /**
     * Report the current number of items in the chain.
     * @return the current items nuumber
     */
    int getItemCount ();

    /**
     * Report the items, if any, that compose this compound.
     * @return the set of items, perhaps empty, but never null
     */
    SortedSet<Glyph> getItems ();

    /**
     * Report the last item of the chain
     * @return chain last item
     */
    Glyph getLastItem ();

    /**
     * Remove the provided item from this chain.
     * @param item the glyph to remove
     * @return true if the provided item was actually removed
     */
    boolean removeItem (Glyph item);

    /**
     * Record the items that compose this compound glyph.
     * @param items the contained items, perhaps empty but not null.
     */
    void setItems (Collection<?extends Glyph> items);
}
