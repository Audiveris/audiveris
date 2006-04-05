//-----------------------------------------------------------------------//
//                                                                       //
//                        S t i c k S e c t i o n                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.stick;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.lag.Lag;
import omr.lag.Run;
import omr.math.BasicLine;
import omr.math.Line;

import java.awt.*;

import static omr.stick.SectionRole.*;

/**
 * Class <code>StickSection</code> implements a specific class of
 * {@link GlyphSection}, meant for easy stick elaboration.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StickSection
        extends GlyphSection
{
    //~ Instance variables ------------------------------------------------

    /**
     * The role of the section in the enclosing stick. Not final, since it
     * may be modified afterhand
     */
    public SectionRole role;

    /**
     * Layer of this section in the stick
     */
    public int layer;

    /**
     * Position with respect to line core center
     */
    public int direction;

    /**
     * Approximating line for this section
     */
    protected Line line;

    //~ Constructors ---------------------------------------------------------

    //--------------//
    // StickSection //
    //--------------//
    /**
     * Creates a new StickSection.
     */
    public StickSection ()
    {
    }

    //~ Methods -----------------------------------------------------------

    //-----------//
    // setParams //
    //-----------//
    /**
     * Assign major parameters (kind, layer and direction), since the
     * enclosing stick may be assigned later.
     *
     * @param role      the role of this section in stick elaboration
     * @param layer     the layer from stick core
     * @param direction the direction when departing from the stick core
     */
    public void setParams (SectionRole role,
                           int         layer,
                           int         direction)
    {
        this.role = role;
        this.layer = layer;
        this.direction = direction;
    }

    //----------//
    // getColor //
    //----------//
    /**
     * Define a color, according to the data at hand, that is according to
     * the role of this section in the enclosing stick.
     *
     * @return the related color
     */
    public Color getColor ()
    {
        if (role != null) {
            return role.getColor();
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
    // getPrefix //
    //-----------//
    @Override
    protected String getPrefix ()
    {
        return "SS";
    }

    //-------------//
    // isCandidate //
    //-------------//
    /**
     * Checks whether the section is a good candidate to be a member of a
     * stick
     *
     * @return the result of the test
     */
    public boolean isCandidate ()
    {
        return (role != null) &&
               (role.ordinal() < BORDER.ordinal());
    }

    //--------------//
    // isAggregable //
    //--------------//
    /**
     * Check that the section at hand is a candidate section not yet
     * aggregated to a recognized stick.
     *
     * @return true if aggregable (but not yet aggregated)
     */
    public boolean isAggregable ()
    {
        if (!isCandidate()) {
            return false;
        }

        return !isKnown();
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

        sb.append(" L=").append(layer);
        sb.append(" D=").append(direction);

        if (role != null) {
            sb.append(" ").append(role);
        }

        if (this.getClass().getName().equals 
                (StickSection.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }
}
