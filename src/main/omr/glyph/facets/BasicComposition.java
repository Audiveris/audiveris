//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                B a s i c C o m p o s i t i o n                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.check.Failure;

import omr.glyph.Shape;

import omr.lag.Section;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code BasicComposition} implements the composition facet of
 * a glyph made of sections (and possibly of other sub-glyphs).
 * These member sections may belong to different lags.
 *
 * @author Hervé Bitteur
 */
class BasicComposition
        extends BasicFacet
        implements GlyphComposition
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BasicComposition.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /**
     * Sections that compose this glyph.
     * The collection is kept sorted on natural Section order (abscissa then
     * ordinate, even with mixed section orientations).
     */
    private final SortedSet<Section> members = new TreeSet<Section>();

    /** Unmodifiable view on members */
    private final SortedSet<Section> unmodifiableMembers = Collections.unmodifiableSortedSet(
            members);

    /** Failures found for this glyph */
    private final Set<Failure> failures = new LinkedHashSet<Failure>();

    /** Link to the compound, if any, this one is a part of */
    private Glyph partOf;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new BasicComposition object.
     *
     * @param glyph our glyph
     */
    public BasicComposition (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // addFailure //
    //------------//
    @Override
    public void addFailure (Failure failure)
    {
        failures.add(failure);
    }

    //------------//
    // addSection //
    //------------//
    @Override
    public void addSection (Section section,
                            Linking link)
    {
        if (section == null) {
            throw new IllegalArgumentException("Cannot add a null section");
        }

        //        if (!glyph.isTransient()) {
        //            logger.error("Adding section to registered glyph");
        //        }
        // Nota: We must include the section in the glyph members before
        // linking back the section to the containing glyph.
        // Otherwise, there is a risk of using the glyph box (which depends on
        // its member sections) before the section is in the glyph members.
        // This phenomenum was sometimes observed when using parallelism.
        /** First, update glyph data */
        members.add(section);

        /** Second, update section data, if so desired */
        if (link == Linking.LINK) {
            section.setGlyph(glyph);
        }

        glyph.invalidateCache();
    }

    //-------------//
    // addSections //
    //-------------//
    public void addSections (Glyph other,
                             Linking linkSections)
    {
        // Update glyph info in other sections
        for (Section section : other.getMembers()) {
            addSection(section, linkSections);
        }
    }

    //-----------------//
    // containsSection //
    //-----------------//
    @Override
    public boolean containsSection (int id)
    {
        for (Section section : glyph.getMembers()) {
            if (section.getId() == id) {
                return true;
            }
        }

        return false;
    }

    //-------------//
    // cutSections //
    //-------------//
    @Override
    public void cutSections ()
    {
        for (Section section : glyph.getMembers()) {
            if (section.getGlyph() == glyph) {
                section.setGlyph(null);
            }
        }
    }

    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("   members=%s%n", members));

        if (partOf != null) {
            sb.append(String.format("   partOf=%s%n", partOf));
        }

        for (Failure failure : failures) {
            sb.append(String.format("   %s%n", failure));
        }

        return sb.toString();
    }

    //-------------//
    // getAncestor //
    //-------------//
    @Override
    public Glyph getAncestor ()
    {
        Glyph g = this.glyph;

        while (g.getPartOf() != null) {
            g = g.getPartOf();
        }

        return g;
    }

    //-------------//
    // getFailures //
    //-------------//
    @Override
    public Set<Failure> getFailures ()
    {
        return failures;
    }

    //-----------------//
    // getFirstSection //
    //-----------------//
    @Override
    public Section getFirstSection ()
    {
        return glyph.getMembers().first();
    }

    //------------//
    // getMembers //
    //------------//
    @Override
    public SortedSet<Section> getMembers ()
    {
        return unmodifiableMembers;
    }

    //-----------//
    // getPartOf //
    //-----------//
    @Override
    public Glyph getPartOf ()
    {
        return partOf;
    }

    //----------//
    // isActive //
    //----------//
    @Override
    public boolean isActive ()
    {
        if (glyph.getShape() == Shape.GLYPH_PART) {
            return false;
        }

        for (Section section : glyph.getMembers()) {
            if (section.getGlyph() != glyph) {
                return false;
            }
        }

        return true;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return glyph.isVip();
    }

    //-----------------//
    // linkAllSections //
    //-----------------//
    @Override
    public void linkAllSections ()
    {
        for (Section section : glyph.getMembers()) {
            section.setGlyph(glyph);
        }
    }

    //---------------//
    // removeSection //
    //---------------//
    @Override
    public boolean removeSection (Section section,
                                  Linking link)
    {
        if (link == Linking.LINK) {
            section.setGlyph(null);
        }

        boolean bool = members.remove(section);
        glyph.invalidateCache();

        return bool;
    }

    //-----------//
    // setPartOf //
    //-----------//
    @Override
    public void setPartOf (Glyph compound)
    {
        partOf = compound;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip ()
    {
        glyph.setVip();
    }

    //---------------//
    // stealSections //
    //---------------//
    @Override
    public void stealSections (Glyph that)
    {
        for (Section section : that.getMembers()) {
            addSection(section, Linking.LINK);
        }

        that.setPartOf(glyph);
    }

    //---------//
    // touches //
    //---------//
    @Override
    public boolean touches (Section section)
    {
        for (Section member : members) {
            if (section.touches(member)) {
                return true;
            }
        }

        return false;
    }
}
