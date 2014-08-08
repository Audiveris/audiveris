//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                G l y p h C o m p o s i t i o n                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.check.Checkable;
import omr.check.Failure;

import omr.lag.Section;

import java.util.Set;
import java.util.SortedSet;

/**
 * Interface {@code GlyphComposition} defines the facet that handles the way a glyph is
 * composed of sections members, as well as the relationships with sub-glyphs (parts)
 * if any.
 *
 * @author Hervé Bitteur
 */
public interface GlyphComposition
        extends GlyphFacet, Checkable
{
    //~ Enumerations -------------------------------------------------------------------------------

    /** Tells whether a section must point back to a containing glyph.
     * <<p>
     * A section may point back to its containing glyph, making it <b>active</b>.
     * There may be several containing glyph instances for a given section, but at most one of them
     * can be active.
     * This allows to easily detect which sections and which glyph instances are
     * active.
     * Sections are often displayed with a color specific to the shape of their
     * active containing glyph (example: staff lines during grid building).
     * <p>
     * TODO: Is this still useful?
     * When building glyph instances from a collection of sections, we should always be able to
     * filter the collection beforehand.
     * The feature is convenient for color display, however staff lines sections get quickly
     * removed, and we now focus on Inter display rather than Glyph display.
     * So let's keep the feature for a while but use it only for sections whose role is determined
     * once for all, like staff lines.
     */
    enum Linking
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        /** Pointing back to glyph */
        LINK,
        /** No pointing back to glyph */
        NO_LINK;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Report the top ancestor of this glyph.
     * This is this glyph itself, when it has no parent (i.e. not been included
     * into another one)
     *
     * @return the glyph ancestor
     */
    public Glyph getAncestor ();

    /**
     * Report the containing compound, if any, which has "stolen" the
     * sections of this glyph.
     *
     * @return the containing compound if any
     */
    public Glyph getPartOf ();

    /**
     * Record the link to the compound which has "stolen" the sections
     * of this glyph.
     *
     * @param compound the containing compound, if any
     */
    public void setPartOf (Glyph compound);

    /**
     * Add a section as a member of this glyph.
     *
     * @param section The section to be included
     * @param link    While adding a section to this glyph members, should we
     *                also
     *                set the link from section back to the glyph?
     */
    void addSection (Section section,
                     Linking link);

    /**
     * Debug function that returns true if this glyph contains the
     * section whose ID is provided.
     *
     * @param id the ID of interesting section
     * @return true if such section exists among glyph sections
     */
    boolean containsSection (int id);

    /**
     * Cut the link to this glyph from its member sections, only if the
     * sections actually point to this glyph.
     */
    void cutSections ();

    /**
     * Report the failures found during analyses of this glyph.
     *
     * @return the collection of failures
     */
    Set<Failure> getFailures ();

    /**
     * Report the first section in the ordered collection of members.
     *
     * @return the first section of the glyph
     */
    Section getFirstSection ();

    /**
     * Report the set of member sections.
     *
     * @return member sections
     */
    SortedSet<Section> getMembers ();

    /**
     * Tests whether this glyph is active.
     * (all its member sections point to it)
     *
     * @return true if glyph is active, false otherwise
     */
    boolean isActive ();

    /**
     * Make all the glyph's sections point back to this glyph.
     */
    void linkAllSections ();

    /**
     * Remove a section from the glyph members
     *
     * @param section the section to remove
     * @param link    should we update the link from section to glyph?
     * @return true if the section was actually found and removed
     */
    boolean removeSection (Section section,
                           Linking link);

    /**
     * Include the sections from another glyph into this one, and make
     * its sections point into this one.
     * Doing so, the other glyph becomes inactive.
     *
     * @param that the glyph to swallow
     */
    void stealSections (Glyph that);

    /**
     * Test whether glyph touches the provided section
     * @param section provided section
     * @return true if there is contact
     */
    boolean touches (Section section);
}
