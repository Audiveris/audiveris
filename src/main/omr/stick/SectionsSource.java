//----------------------------------------------------------------------------//
//                                                                            //
//                        S e c t i o n s S o u r c e                         //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.stick;

import omr.lag.Section;

import omr.util.Predicate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ListIterator;

/**
 * Class {@code Source} allows to formalize the way relevant sections
 * are made available to the area to be scanned.
 *
 * @author Hervé Bitteur
 */
public class SectionsSource
{
    //~ Instance fields --------------------------------------------------------

    /** the predicate to check whether section is to be processed */
    protected final Predicate<Section> predicate;

    /** Underlying list */
    protected ArrayList<Section> list;

    /** the section iterator for the source */
    protected ListIterator<Section> vi;

    /** the section currently visited */
    protected Section section;

    //~ Constructors -----------------------------------------------------------
    //----------------//
    // SectionsSource //
    //----------------//
    /**
     * Create a source on a given collection of glyph sections,
     * with default predicate
     *
     * @param collection the provided sections
     */
    public SectionsSource (Collection<Section> collection)
    {
        this(collection, new UnknownSectionPredicate());
    }

    //----------------//
    // SectionsSource //
    //----------------//
    /**
     * Create a source on a given collection of glyph sections,
     * with a specific predicate for section
     *
     * @param collection the provided sections
     * @param predicate  the predicate to check for candidate sections
     */
    public SectionsSource (Collection<Section> collection,
                           Predicate<Section> predicate)
    {
        this.predicate = predicate;

        if (collection != null) {
            list = new ArrayList<>(collection);
            reset();
        }
    }

    //~ Methods ----------------------------------------------------------------
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
     * Check whether we have more sections to scan.
     *
     * @return the boolean result of the test
     */
    public boolean hasNext ()
    {
        while (vi.hasNext()) {
            // Update cached data
            section = vi.next();

            if (predicate.check(section)) {
                StickRelation relation = section.getRelation();

                if (relation != null) {
                    relation.role = null; // Safer ?
                }

                section.setGlyph(null);

                return true;
            }
        }

        return false;
    }

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
    public boolean isInArea (Section section)
    {
        // Default behavior : no filtering
        return true;
    }

    //------//
    // next //
    //------//
    /**
     * Return the next relevant section in Area, if any
     *
     * @return the next section
     */
    public Section next ()
    {
        return section;
    }

    //-------//
    // reset //
    //-------//
    public void reset ()
    {
        vi = list.listIterator();
    }
}
