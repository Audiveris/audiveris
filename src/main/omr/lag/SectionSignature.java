//----------------------------------------------------------------------------//
//                                                                            //
//                      S e c t i o n S i g n a t u r e                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import java.awt.Rectangle;

/**
 * Class {@code SectionSignature} defines a signature for a section
 *
 * @author Hervé Bitteur
 */
public class SectionSignature
{
    //~ Instance fields --------------------------------------------------------

    /** Section weight */
    private final int weight;

    /** Section bounds */
    private Rectangle bounds;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new SectionSignature object.
     *
     * @param weight the section weight
     * @param bounds the section bounds
     */
    public SectionSignature (int weight,
                             Rectangle bounds)
    {
        this.weight = weight;
        this.bounds = bounds;
    }

    //~ Methods ----------------------------------------------------------------
    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof SectionSignature) {
            SectionSignature that = (SectionSignature) obj;

            return (weight == that.weight) && (bounds.x == that.bounds.x)
                   && (bounds.y == that.bounds.y)
                   && (bounds.width == that.bounds.width)
                   && (bounds.height == that.bounds.height);
        } else {
            return false;
        }
    }

    //----------//
    // hashCode //
    //----------//
    @Override
    public int hashCode ()
    {
        int hash = 7;
        hash = (41 * hash) + this.weight;

        return hash;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{SSig");
        sb.append(" weight=")
                .append(weight);

        if (bounds != null) {
            sb.append(" Rectangle[x=")
                    .append(bounds.x)
                    .append(",y=")
                    .append(bounds.y)
                    .append(",width=")
                    .append(bounds.width)
                    .append(",height=")
                    .append(bounds.height)
                    .append("]");
        }

        sb.append("}");

        return sb.toString();
    }
}
