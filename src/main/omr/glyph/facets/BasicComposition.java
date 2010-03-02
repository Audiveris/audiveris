//----------------------------------------------------------------------------//
//                                                                            //
//                      B a s i c C o m p o s i t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.check.Result;
import omr.check.SuccessResult;

import omr.glyph.GlyphSection;

import omr.score.common.PixelPoint;

import omr.sheet.SystemInfo;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code BasicComposition} implements the composition facet of a glyph
 * made of sections
 *
 * @author Hervé Bitteur
 */
class BasicComposition
    extends BasicFacet
    implements GlyphComposition
{
    //~ Instance fields --------------------------------------------------------

    /**
     * Sections that compose this glyph. The collection is kept sorted
     * on GlyphSection order (TODO: check this statement)
     */
    private SortedSet<GlyphSection> members = new TreeSet<GlyphSection>();

    /** Contained parts, if this glyph is a compound */
    private Set<Glyph> parts = new LinkedHashSet<Glyph>();

    /** Link to the compound, if any, this one is a part of */
    private Glyph partOf;

    /** Result of analysis wrt this glyph */
    private Result result;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // BasicComposition //
    //------------------//
    /**
     * Create a new BasicComposition object
     *
     * @param glyph our glyph
     */
    public BasicComposition (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // isActive //
    //----------//
    public boolean isActive ()
    {
        for (GlyphSection section : members) {
            if (section.getGlyph() != glyph) {
                return false;
            }
        }

        return true;
    }

    //----------------//
    // getAlienSystem //
    //----------------//
    public SystemInfo getAlienSystem (SystemInfo system)
    {
        // Direct members
        for (GlyphSection section : members) {
            if (section.getSystem() != system) {
                return section.getSystem();
            }
        }

        // Parts if any, recursively
        for (Glyph part : getParts()) {
            SystemInfo alien = part.getAlienSystem(system);

            if (alien != null) {
                return alien;
            }
        }

        // No other system found
        return null;
    }

    //-----------------//
    // getFirstSection //
    //-----------------//
    public GlyphSection getFirstSection ()
    {
        return members.first();
    }

    //------------//
    // getMembers //
    //------------//
    public SortedSet<GlyphSection> getMembers ()
    {
        return members;
    }

    //-----------//
    // setPartOf //
    //-----------//
    public void setPartOf (Glyph compound)
    {
        partOf = compound;
    }

    //-----------//
    // getPartOf //
    //-----------//
    public Glyph getPartOf ()
    {
        return partOf;
    }

    //----------//
    // setParts //
    //----------//
    public void setParts (Collection<?extends Glyph> parts)
    {
        if (this.parts != parts) {
            this.parts.clear();
            this.parts.addAll(parts);
        }
    }

    //----------//
    // getParts //
    //----------//
    public Set<Glyph> getParts ()
    {
        return parts;
    }

    //-----------//
    // setResult //
    //-----------//
    public void setResult (Result result)
    {
        this.result = result;
    }

    //-----------//
    // getResult //
    //-----------//
    public Result getResult ()
    {
        return result;
    }

    //--------------//
    // isSuccessful //
    //--------------//
    public boolean isSuccessful ()
    {
        return result instanceof SuccessResult;
    }

    //------------------//
    // addGlyphSections //
    //------------------//
    public void addGlyphSections (Glyph   other,
                                  Linking linkSections)
    {
        // Update glyph info in other sections
        for (GlyphSection section : other.getMembers()) {
            addSection(section, linkSections);
        }
    }

    //------------//
    // addSection //
    //------------//
    public void addSection (GlyphSection section,
                            Linking      link)
    {
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
    // cutSections //
    //-------------//
    public void cutSections ()
    {
        for (GlyphSection section : members) {
            if (section.getGlyph() == glyph) {
                section.setGlyph(null);
            }
        }
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        System.out.println("   members=" + getMembers());
        System.out.println("   parts=" + parts);
        System.out.println("   partOf=" + partOf);
        System.out.println("   result=" + getResult());
    }

    //-----------------//
    // linkAllSections //
    //-----------------//
    public void linkAllSections ()
    {
        for (GlyphSection section : getMembers()) {
            section.setGlyph(glyph);
        }
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply the provided translation vector to all composing sections
     * @param vector the provided translation vector
     */
    public void translate (PixelPoint vector)
    {
        for (GlyphSection section : getMembers()) {
            section.translate(vector);
        }
    }
}
