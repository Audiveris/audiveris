//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h S e c t i o n                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.lag.Section;

import omr.log.Logger;

import omr.util.Implement;

import java.awt.Point;

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

    //~ Instance fields --------------------------------------------------------

    /**
     * Glyph this section belongs to. This reference is kept in sync with the
     * containing GlyphLag activeMap. Don't directly assign a value to 'glyph',
     * use the setGlyph() method instead.
     */
    private Glyph glyph;

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

        if (glyph != null) {
            sb.append(" glyph#")
              .append(glyph.getId());

            if (glyph.getShape() != null) {
                sb.append(":")
                  .append(glyph.getShape());
            }
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
