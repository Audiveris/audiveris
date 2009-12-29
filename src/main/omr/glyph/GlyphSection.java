//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h S e c t i o n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.lag.Section;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import omr.util.Implement;

import java.awt.Point;
import java.util.Comparator;

/**
 * Class <code>GlyphSection</code> implements a specific class of section, meant
 * for easy glyph elaboration.
 *
 * Such sections are defined as naturally ordered, first on their position
 * (abscissa for vertical sections), then on their coordinate (ordinate for
 * vertical sections).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class GlyphSection
    extends Section<GlyphLag, GlyphSection>
    implements Comparable<GlyphSection>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(GlyphSection.class);

    /** For comparing GlyphSection instances on their decreasing weight */
    public static final Comparator<GlyphSection> reverseWeightComparator = new Comparator<GlyphSection>() {
        public int compare (GlyphSection s1,
                            GlyphSection s2)
        {
            return Integer.signum(s2.getWeight() - s1.getWeight());
        }
    };

    /** For comparing GlyphSection instances on their start value */
    public static final Comparator<GlyphSection> startComparator = new Comparator<GlyphSection>() {
        public int compare (GlyphSection s1,
                            GlyphSection s2)
        {
            return s1.getStart() - s2.getStart();
        }
    };


    //~ Instance fields --------------------------------------------------------

    /**
     * Glyph this section belongs to. This reference is kept in sync with the
     * containing GlyphLag activeMap. Don't directly assign a value to 'glyph',
     * use the setGlyph() method instead.
     */
    private Glyph glyph;

    /** The containing system, if any */
    private SystemInfo system;

    /** Is this section a patch? */
    private boolean patch = false;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // GlyphSection //
    //--------------//
    /**
     * Creates a new GlyphSection.
     */
    public GlyphSection ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // setGlyph //
    //----------//
    /**
     * Assign the containing glyph
     *
     * @param glyph the containing glyph
     */
    public void setGlyph (Glyph glyph)
    {
        this.glyph = glyph;

        // Keep the activeMap of the containing GlyphLag in sync!
        getGraph()
            .mapSection(this, glyph);
    }

    //----------//
    // getGlyph //
    //----------//
    /**
     * Report the glyph the section belongs to, if any
     *
     * @return the glyph, which may be null
     */
    public Glyph getGlyph ()
    {
        return glyph;
    }

    //---------------//
    // isGlyphMember //
    //---------------//
    /**
     * Checks whether the section is already a member of a glyph, whether this
     * glyph has been successfully recognized or not.
     *
     * @return the result of the test
     */
    public boolean isGlyphMember ()
    {
        return glyph != null;
    }

    //---------//
    // isKnown //
    //---------//
    /**
     * Check that the section at hand is a member section, aggregated to a known
     * glyph.
     *
     * @return true if member of a known glyph
     */
    public boolean isKnown ()
    {
        return (glyph != null) &&
               (glyph.isSuccessful() || glyph.isWellKnown());
    }

    //----------//
    // setPatch //
    //----------//
    /**
     * Set this section as a patch
     * @param patch the patch to set
     */
    public void setPatch (boolean patch)
    {
        this.patch = patch;
    }

    //---------//
    // isPatch //
    //---------//
    /**
     * Report whether this section is a patch
     * @return the patch
     */
    public boolean isPatch ()
    {
        return patch;
    }

    //-----------//
    // setSystem //
    //-----------//
    /**
     * Assign a containing system
     * @param system the system to set
     */
    public void setSystem (SystemInfo system)
    {
        this.system = system;
    }

    //-----------//
    // getSystem //
    //-----------//
    /**
     * Report the containing system
     * @return the system (may be null)
     */
    public SystemInfo getSystem ()
    {
        return system;
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Needed to implement Comparable, sorting sections first by position, then
     * by coordinate.
     *
     * @param other the other section to compare to
     * @return the result of ordering
     */
    @Implement(Comparable.class)
    public int compareTo (GlyphSection other)
    {
        if (this == other) {
            return 0;
        }

        final Point ref = this.getContourBox()
                              .getLocation();
        final Point otherRef = other.getContourBox()
                                    .getLocation();

        // Are x values different?
        final int dx = ref.x - otherRef.x;

        if (dx != 0) {
            return dx;
        }

        // Vertically aligned, so use ordinates
        final int dy = ref.y - otherRef.y;

        if (dy != 0) {
            return dy;
        }

        // Finally, use id. Note this should return zero since different
        // sections cannot overlap
        return this.getId() - other.getId();
    }

    //----------//
    // toString //
    //----------//
    /**
     * A readable description of this entity
     *
     * @return the string
     */
    @Override
    public String toString ()
    {
        StringBuffer sb = new StringBuffer(256);

        sb.append(super.toString());

        if (isPatch()) {
            sb.append(" patch");
        }

        if (glyph != null) {
            sb.append(" glyph#")
              .append(glyph.getId());

            if (glyph.getShape() != null) {
                sb.append(":")
                  .append(glyph.getShape());
            }
        }

        if (system != null) {
            sb.append(" syst:")
              .append(system.getId());
        }

        if (this.getClass()
                .getName()
                .equals(GlyphSection.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }

    //-----------//
    // getPrefix //
    //-----------//
    /**
     * Report a distinct prefix
     *
     * @return a prefix string
     */
    @Override
    protected String getPrefix ()
    {
        return "GS";
    }
}
