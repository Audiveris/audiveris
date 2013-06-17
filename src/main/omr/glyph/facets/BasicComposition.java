//----------------------------------------------------------------------------//
//                                                                            //
//                      B a s i c C o m p o s i t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.check.Result;
import omr.check.SuccessResult;

import omr.glyph.Shape;

import omr.lag.Section;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import omr.sheet.SystemInfo;

import java.awt.Point;
import java.util.Collections;
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

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(BasicComposition.class);

    /**
     * Sections that compose this glyph.
     * The collection is kept sorted on natural Section order (abscissa then
     * ordinate, even with mixed section orientations).
     */
    private final SortedSet<Section> members = new TreeSet<>();

    /** Unmodifiable view on members */
    private final SortedSet<Section> unmodifiableMembers = Collections.unmodifiableSortedSet(members);

    /** Link to the compound, if any, this one is a part of */
    private Glyph partOf;

    /** Result of analysis wrt this glyph */
    private Result result;

    //------------------//
    // BasicComposition //
    //------------------//
    /**
     * Create a new BasicComposition object.
     *
     * @param glyph our glyph
     */
    public BasicComposition (Glyph glyph)
    {
        super(glyph);
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
        if (link == Linking.LINK_BACK) {
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

        if (result != null) {
            sb.append(String.format("   result=%s%n", result));
        }

        return sb.toString();
    }

    //----------------//
    // getAlienSystem //
    //----------------//
    @Override
    public SystemInfo getAlienSystem (SystemInfo system)
    {
        // Direct members
        for (Section section : glyph.getMembers()) {
            if (section.getSystem() != system) {
                return section.getSystem();
            }
        }

        // No other system found
        return null;
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

    //-----------//
    // getResult //
    //-----------//
    @Override
    public Result getResult ()
    {
        return result;
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

    //--------------//
    // isSuccessful //
    //--------------//
    @Override
    public boolean isSuccessful ()
    {
        return result instanceof SuccessResult;
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
        if (link == Linking.LINK_BACK) {
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

    //-----------//
    // setResult //
    //-----------//
    @Override
    public void setResult (Result result)
    {
        this.result = result;
    }

    //---------------//
    // stealSections //
    //---------------//
    @Override
    public void stealSections (Glyph that)
    {
        for (Section section : that.getMembers()) {
            addSection(section, Linking.LINK_BACK);
        }

        that.setPartOf(glyph);
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply the provided translation vector to all composing sections.
     *
     * @param vector the provided translation vector
     */
    public void translate (Point vector)
    {
        for (Section section : glyph.getMembers()) {
            section.translate(vector);
        }
    }
}
