//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                B a s i c E n v i r o n m e n t                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.lag.Section;

import omr.run.Orientation;
import omr.run.Run;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code BasicEnvironment} is the basic implementation of an environment facet.
 *
 * @author Hervé Bitteur
 */
class BasicEnvironment
        extends BasicFacet
        implements GlyphEnvironment
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Position with respect to nearest staff. Key references are : 0 for
     * middle line (B), -2 for top line (F) and +2 for bottom line (E) */
    private double pitchPosition;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a new BasicEnvironment object
     *
     * @param glyph our glyph
     */
    public BasicEnvironment (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("   pitchPosition=%s%n", getPitchPosition()));

        return sb.toString();
    }

    //------------------//
    // getPitchPosition //
    //------------------//
    @Override
    public double getPitchPosition ()
    {
        return pitchPosition;
    }

    //------------//
    // intersects //
    //------------//
    @Override
    public boolean intersects (Glyph that)
    {
        // Very rough test
        final Rectangle thisBox = glyph.getBounds();
        final Rectangle thatBox = that.getBounds();

        if (!thisBox.intersects(thatBox)) {
            return false;
        }

        // Use only the sections of that glyph that do intersect this glyph box
        final List<Section> thatSections = new ArrayList<Section>();

        for (Section section : that.getMembers()) {
            if (section.intersects(thisBox)) {
                thatSections.add(section);
            }
        }

        // More precise tests
        for (Section section : glyph.getMembers()) {
            if (!section.intersects(thatBox)) {
                continue;
            }

            Orientation orientation = section.getOrientation();
            int pos = section.getFirstPos();

            for (Run run : section.getRuns()) {
                final int start = run.getStart();
                final Rectangle runBox = (orientation == Orientation.HORIZONTAL)
                        ? new Rectangle(start, pos, run.getLength(), 1)
                        : new Rectangle(pos, start, 1, run.getLength());

                for (Section s : thatSections) {
                    if (s.intersects(runBox)) {
                        return true;
                    }
                }

                pos++;
            }
        }

        return false;
    }

    //------------------//
    // setPitchPosition //
    //------------------//
    @Override
    public void setPitchPosition (double pitchPosition)
    {
        this.pitchPosition = pitchPosition;
    }

    //---------//
    // touches //
    //---------//
    @Override
    public boolean touches (Glyph that)
    {
        // Very rough test
        final Rectangle thisBox = glyph.getBounds();
        final Rectangle thatFatBox = that.getBounds();
        thatFatBox.grow(1, 1);

        if (!thisBox.intersects(thatFatBox)) {
            return false;
        }

        // Use only the sections of that glyph that do intersect this (enlarged) glyph box
        thisBox.grow(1, 1);

        final List<Section> thatSections = new ArrayList<Section>();

        for (Section section : that.getMembers()) {
            if (section.intersects(thisBox)) {
                thatSections.add(section);
            }
        }

        // More precise tests
        for (Section section : glyph.getMembers()) {
            if (!section.intersects(thatFatBox)) {
                continue;
            }

            for (Section thatSection : thatSections) {
                if (section.touches(thatSection)) {
                    return true;
                }
            }
        }

        return false;
    }
}
