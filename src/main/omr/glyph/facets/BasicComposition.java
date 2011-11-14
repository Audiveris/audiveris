//----------------------------------------------------------------------------//
//                                                                            //
//                      B a s i c C o m p o s i t i o n                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.check.Result;
import omr.check.SuccessResult;

import omr.glyph.Shape;

import omr.lag.Section;

import omr.log.Logger;

import omr.score.common.PixelPoint;

import omr.sheet.SystemInfo;

import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class {@code BasicComposition} implements the composition facet of a glyph
 * made of sections. These member sections may belong to different lags.
 *
 * @author Hervé Bitteur
 */
class BasicComposition
    extends BasicFacet
    implements GlyphComposition
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        BasicComposition.class);

    //~ Instance fields --------------------------------------------------------

    /**
     * Sections that compose this glyph. The collection is kept sorted
     * on natural Section order (abscissa then ordinate, even with mixed
     * section orientations).
     */
    private final SortedSet<Section> members = new TreeSet<Section>();

    //    /** Contained parts, if this glyph is a compound */
    //    private final Set<Glyph> parts = new LinkedHashSet<Glyph>();

    /** Link to the compound, if any, this one is a part of */
    private Glyph partOf;

    /** Result of analysis wrt this glyph */
    private Result result;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // BasicComposition //
    //------------------//
    /**
     * Create a new BasicComposition object.
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
        if (glyph.getShape() == Shape.GLYPH_PART) {
            return false;
        }

        for (Section section : members) {
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
        for (Section section : members) {
            if (section.getSystem() != system) {
                return section.getSystem();
            }
        }

        //
        //        // Parts if any, recursively
        //        for (Glyph part : getParts()) {
        //            SystemInfo alien = part.getAlienSystem(system);
        //
        //            if (alien != null) {
        //                return alien;
        //            }
        //        }

        // No other system found
        return null;
    }

    //-------------//
    // getAncestor //
    //-------------//
    public Glyph getAncestor ()
    {
        Glyph glyph = this.glyph;

        while (glyph.getPartOf() != null) {
            glyph = glyph.getPartOf();
        }

        return glyph;
    }

    //-----------------//
    // getFirstSection //
    //-----------------//
    public Section getFirstSection ()
    {
        return members.first();
    }

    //------------//
    // getMembers //
    //------------//
    public SortedSet<Section> getMembers ()
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

    //------------//
    // addSection //
    //------------//
    public void addSection (Section section,
                            Linking link)
    {
        if (section == null) {
            throw new IllegalArgumentException("Cannot add a null section");
        }

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
    public void addSections (Glyph   other,
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
    public boolean containsSection (int id)
    {
        for (Section section : getMembers()) {
            if (section.getId() == id) {
                return true;
            }
        }

        return false;
    }

    //-------------//
    // cutSections //
    //-------------//
    public void cutSections ()
    {
        for (Section section : members) {
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
        System.out.println("   partOf=" + partOf);
        System.out.println("   result=" + getResult());
    }

    //---------//
    // include //
    //---------//
    public void include (Glyph that)
    {
        for (Section section : that.getMembers()) {
            addSection(section, Linking.LINK_BACK);
        }

        that.setPartOf(glyph);
    }

    //-----------------//
    // linkAllSections //
    //-----------------//
    public void linkAllSections ()
    {
        for (Section section : getMembers()) {
            section.setGlyph(glyph);
        }
    }

    //-----------//
    // translate //
    //-----------//
    /**
     * Apply the provided translation vector to all composing sections.
     * @param vector the provided translation vector
     */
    public void translate (PixelPoint vector)
    {
        for (Section section : getMembers()) {
            section.translate(vector);
        }
    }
}
