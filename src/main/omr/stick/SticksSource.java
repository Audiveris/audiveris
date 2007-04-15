//----------------------------------------------------------------------------//
//                                                                            //
//                          S t i c k s S o u r c e                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.stick;

import omr.glyph.GlyphSection;

import omr.util.Predicate;

import java.util.*;

/**
 * Class <code>Source</code> allows to formalize the way relevant sections are
 * made available to the area to be scanned.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SticksSource
{
    //~ Instance fields --------------------------------------------------------

    /** the predicate to check whether section is to be processed */
    protected final Predicate<GlyphSection> predicate;

    /** the section iterator for the source */
    protected ListIterator<GlyphSection> vi;

    /** the section currently visited */
    protected GlyphSection section;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // SticksSource //
    //--------------//
    /**
     * Create a SticksBuilder source on a given collection of glyph sections,
     * with default predicate
     *
     * @param collection the provided sections
     */
    public SticksSource (Collection<GlyphSection> collection)
    {
        this(collection, new UnknownSectionPredicate());
    }

    //--------------//
    // SticksSource //
    //--------------//
    /**
     * Create a SticksBuilder source on a given collection of glyph sections,
     * with a specific predicate for section
     *
     * @param collection the provided sections
     * @param predicate the predicate to check for candidate sections
     */
    public SticksSource (Collection<GlyphSection> collection,
                         Predicate<GlyphSection>  predicate)
    {
        this.predicate = predicate;

        if (collection != null) {
            ArrayList<GlyphSection> list = new ArrayList<GlyphSection>(
                collection);
            vi = list.listIterator();
        }
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // isInArea //
    //----------//
    /**
     * Check whether a given section lies entirely within the scanned area
     *
     * @param section The section to be checked
     *
     * @return The boolean result of the test
     */
    public boolean isInArea (GlyphSection section)
    {
        // Default behavior : no filtering
        return true;
    }

    //--------//
    // backup //
    //--------//
    public void backup ()
    {
        // void
    }

    //---------//
    // hasNext //
    //---------//
    /**
     * Check whether we have more sections to scan
     *
     * @return the boolean result of the test
     */
    public boolean hasNext ()
    {
        while (vi.hasNext()) {
            // Update cached data
            section = (StickSection) vi.next();

            if (predicate.check(section)) {
                if (section instanceof StickSection) {
                    StickRelation relation = ((StickSection) section).getRelation();

                    if (relation != null) {
                        relation.role = null; // Safer ?
                    }
                }

                section.setGlyph(null); // Safer ?

                return true;
            }
        }

        return false;
    }

    //------//
    // next //
    //------//
    /**
     * Return the next relevant section in Area, if any
     *
     * @return the next section
     */
    public GlyphSection next ()
    {
        return section;
    }

    //-------//
    // reset //
    //-------//
    public void reset ()
    {
        // void
    }
}
