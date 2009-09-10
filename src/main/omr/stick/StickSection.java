//----------------------------------------------------------------------------//
//                                                                            //
//                          S t i c k S e c t i o n                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.stick;

import omr.glyph.GlyphSection;

import omr.lag.Run;

import omr.math.BasicLine;
import omr.math.Line;
import static omr.stick.SectionRole.*;

import java.awt.*;

/**
 * Class <code>StickSection</code> implements a specific class of {@link
 * GlyphSection}, meant for easy stick elaboration.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StickSection
    extends GlyphSection
{
    //~ Instance fields --------------------------------------------------------

    /** Relation between section and stick */
    protected StickRelation relation;

    /** Approximating line for this section */
    protected Line line;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // StickSection //
    //--------------//
    /**
     * Creates a new StickSection.
     */
    public StickSection ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //--------------//
    // isAggregable //
    //--------------//
    /**
     * Check that the section at hand is a candidate section not yet aggregated
     * to a recognized stick.
     *
     * @return true if aggregable (but not yet aggregated)
     */
    public boolean isAggregable ()
    {
        if ((relation == null) || !relation.isCandidate()) {
            return false;
        }

        return !isKnown();
    }

    //----------//
    // getColor //
    //----------//
    /**
     * Define a color, according to the data at hand, that is according to the
     * role of this section in the enclosing stick.
     *
     * @return the related color
     */
    public Color getColor ()
    {
        if (relation != null) {
            return relation.getColor();
        } else {
            return null;
        }
    }

    //---------//
    // getLine //
    //---------//
    /**
     * Report the line which best approximates the stick
     *
     * @return the fitted line
     */
    public Line getLine ()
    {
        if (line == null) {
            // Compute the section line
            line = new BasicLine();

            int y = getFirstPos();

            for (Run run : getRuns()) {
                int stop = run.getStop();

                for (int x = run.getStart(); x <= stop; x++) {
                    line.includePoint((double) x, (double) y);
                }

                y++;
            }
        }

        return line;
    }

    //-----------//
    // setParams //
    //-----------//
    /**
     * Assign major parameters (kind, layer and direction), since the enclosing
     * stick may be assigned later.
     *
     * @param role      the role of this section in stick elaboration
     * @param layer     the layer from stick core
     * @param direction the direction when departing from the stick core
     */
    public void setParams (SectionRole role,
                           int         layer,
                           int         direction)
    {
        if (relation == null) {
            relation = new StickRelation();
        }

        relation.setParams(role, layer, direction);
    }

    //-------------//
    // getRelation //
    //-------------//
    public StickRelation getRelation ()
    {
        return relation;
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

        if (relation != null) {
            sb.append(" ")
              .append(relation);
        }

        if (this.getClass()
                .getName()
                .equals(StickSection.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }

    //-----------//
    // getPrefix //
    //-----------//
    @Override
    protected String getPrefix ()
    {
        return "SS";
    }
}
