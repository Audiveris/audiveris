//----------------------------------------------------------------------------//
//                                                                            //
//                         S c o r e L o c a t i o n                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.common;


/**
 * Class <code>ScoreLocation</code> represents a location within the Score
 * space, in a way which is independent of the orientation of any related score
 * view (whether horizontal or vertical).
 *
 * @author Hervé Bitteur
 */
public class ScoreLocation
{
    //~ Instance fields --------------------------------------------------------

    /** The ID of the containing system */
    public final int systemId;

    /**
     * The PixelRectangle, relative to the containing system, and which can be
     * degenerated to a point when both width and height values equal zero
     */
    public final PixelRectangle rectangle;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // ScoreLocation //
    //---------------//
    /**
     * Creates a new ScoreLocation object, with value for each final data
     *
     * @param systemId the containing system
     * @param rectangle the rectangle within the containing system
     */
    public ScoreLocation (int             systemId,
                          PixelRectangle rectangle)
    {
        this.systemId = systemId;
        this.rectangle = rectangle;
    }

    //---------------//
    // ScoreLocation //
    //---------------//
    /**
     * Creates a new ScoreLocation object, with value for each final data
     *
     * @param systemId the containing system
     * @param sysPt the point within the containing system
     */
    public ScoreLocation (int         systemId,
                          PixelPoint sysPt)
    {
        this.systemId = systemId;
        this.rectangle = new PixelRectangle(sysPt);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{ScoreLocation");
        sb.append(" S#")
          .append(systemId);
        sb.append(" ")
          .append(rectangle);
        sb.append("}");

        return sb.toString();
    }
}
