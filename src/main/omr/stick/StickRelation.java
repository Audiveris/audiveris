//----------------------------------------------------------------------------//
//                                                                            //
//                         S t i c k R e l a t i o n                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.stick;

import omr.glyph.GlyphSection;

import java.awt.*;

/**
 * Class <code>StickRelation</code> implements a specific class of {@link
 * GlyphSection}, meant for easy stick elaboration.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StickRelation
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The role of the section in the enclosing stick. Not final, since it may
     * be modified afterhand
     */
    public SectionRole role;

    /**
     * Position with respect to line core center
     */
    public int direction;

    /**
     * Layer of this section in the stick
     */
    public int layer;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // StickRelation //
    //---------------//
    /**
     * Creates a new StickRelation.
     */
    public StickRelation ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // isCandidate //
    //-------------//
    /**
     * Checks whether the section is a good candidate to be a member of a stick
     *
     * @return the result of the test
     */
    public boolean isCandidate ()
    {
        return (role != null) &&
               (role.ordinal() < SectionRole.BORDER.ordinal());
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
        if (role != null) {
            return role.getColor();
        } else {
            return null;
        }
    }

    //-----------//
    // setParams //
    //-----------//
    /**
     * Assign the various parameters (kind, layer and direction)
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

        sb.append("[");

        sb.append("L=")
          .append(layer);
        sb.append(" D=")
          .append(direction);

        if (role != null) {
            sb.append(" ")
              .append(role);
        }

        sb.append("]");

        return sb.toString();
    }
}
