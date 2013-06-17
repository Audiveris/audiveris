//----------------------------------------------------------------------------//
//                                                                            //
//                         S t i c k R e l a t i o n                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.stick;

import java.awt.Color;

/**
 * Class {@code StickRelation} complements {@link omr.lag.Section}
 * for easy stick elaboration.
 *
 * @author Hervé Bitteur
 */
public class StickRelation
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The role of the section in the enclosing stick.
     * Not final, since it may be modified afterhand.
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
    //----------//
    // getColor //
    //----------//
    /**
     * Define a color, according to the data at hand, that is according
     * to the role of this section in the enclosing stick.
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

    //-------------//
    // isCandidate //
    //-------------//
    /**
     * Checks whether the section is a good candidate to be a member
     * of a stick
     *
     * @return the result of the test
     */
    public boolean isCandidate ()
    {
        return (role != null)
               && (role.ordinal() < SectionRole.BORDER.ordinal());
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
                           int layer,
                           int direction)
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
        StringBuilder sb = new StringBuilder(256);

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
