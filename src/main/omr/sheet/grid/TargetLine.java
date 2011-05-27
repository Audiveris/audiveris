//----------------------------------------------------------------------------//
//                                                                            //
//                            T a r g e t L i n e                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import java.awt.geom.Point2D;

/**
 * Class {@code TargetLine} is an immutable perfect destination object for a
 * staff line.
 *
 * @author Hervé Bitteur
 */
public class TargetLine
{
    //~ Instance fields --------------------------------------------------------

    /** Related raw information */
    public final LineInfo info;

    /** Id for debug */
    public final int id;

    /** Ordinate in containing page */
    public final int y;

    /** Containing staff */
    public final TargetStaff staff;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // TargetLine //
    //------------//
    /**
     * Creates a new TargetLine object.
     *
     * @param info the physical information
     * @param y ordinate in containing pag
     * @param the containing staff
     */
    public TargetLine (LineInfo    info,
                       int         y,
                       TargetStaff staff)
    {
        this.info = info;
        this.y = y;
        this.staff = staff;

        id = info.getId();
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // sourceOf //
    //----------//
    /**
     * Report the source point that corresponds to a destination point dst
     * above or below this line
     * @param dst the given destination point
     * @return the corresponding source point
     */
    public Point2D sourceOf (Point2D dst)
    {
        // Use vertical translation
        Point2D src = sourceOf(dst.getX());

        return new Point2D.Double(src.getX(), src.getY() - y + dst.getY());
    }

    //----------//
    // sourceOf //
    //----------//
    /**
     * Report the source point that corresponds to a destination point at
     * abscissa dstX on this line
     * @param dstX the given destination abscissa
     * @return the corresponding source point
     */
    public Point2D sourceOf (double dstX)
    {
        int    left = staff.system.left;
        int    right = staff.system.right;
        double xRatio = (dstX - left) / (right - left);
        double srcX = ((1 - xRatio) * info.getLeftPoint().x) +
                      (xRatio * info.getRightPoint().x);
        double srcY = info.yAt(srcX);

        return new Point2D.Double(srcX, srcY);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Line");
        sb.append("#")
          .append(id);
        sb.append(" y:")
          .append(y);
        sb.append("}");

        return sb.toString();
    }
}
