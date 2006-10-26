//----------------------------------------------------------------------------//
//                                                                            //
//                          G l y p h S e c t i o n                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.check.SuccessResult;

import omr.lag.Section;

import omr.util.Implement;

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
    //~ Instance fields --------------------------------------------------------

    /**
     * Glyph this section belongs to
     */
    protected Glyph glyph;

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
        return isGlyphMember() &&
               (((glyph.getResult() != null) &&
                glyph.getResult() instanceof SuccessResult) ||
               glyph.isWellKnown());
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
        // Are pos values different?
        int dPos = getCentroid().y - other.getCentroid().y;

        if (dPos != 0) {
            return dPos;
        }

        // Use coordinates
        return getCentroid().x - other.getCentroid().x;
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
            sb.append(" glyph=")
              .append(glyph.getId());
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
