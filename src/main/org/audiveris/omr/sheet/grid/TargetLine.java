//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T a r g e t L i n e                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.grid;

import static org.audiveris.omr.util.HorizontalSide.*;

import java.awt.geom.Point2D;

/**
 * Class {@code TargetLine} is an immutable perfect destination object for a staff line.
 *
 * @author Hervé Bitteur
 */
public class TargetLine
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Related raw information */
    public final LineInfo info;

    /** Ordinate in containing page */
    public final double y;

    /** Containing staff */
    public final TargetStaff staff;

    /** Sine of raw line angle */
    private final double sin;

    /** Cosine of raw line angle */
    private final double cos;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TargetLine object.
     *
     * @param info  the physical information
     * @param y     ordinate in containing sheet
     * @param staff the containing staff
     */
    public TargetLine (LineInfo info,
                       double y,
                       TargetStaff staff)
    {
        this.info = info;
        this.y = y;
        this.staff = staff;

        // Compute sin & cos values
        Point2D left = info.getEndPoint(LEFT);
        Point2D right = info.getEndPoint(RIGHT);
        double dx = right.getX() - left.getX();
        double dy = right.getY() - left.getY();
        double hypot = Math.hypot(dx, dy);
        sin = dy / hypot;
        cos = dx / hypot;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // sourceOf //
    //----------//
    /**
     * Report the source point that corresponds to a destination point dst
     * above or below this line
     *
     * @param dst the given destination point
     * @return the corresponding source point
     */
    public Point2D sourceOf (Point2D dst)
    {
        // Use orthogonal projection to line
        double dist = dst.getY() - y;
        Point2D projSrc = sourceOf(dst.getX());
        double dx = -dist * sin;
        double dy = dist * cos;

        return new Point2D.Double(projSrc.getX() + dx, projSrc.getY() + dy);
    }

    //----------//
    // sourceOf //
    //----------//
    /**
     * Report the source point that corresponds to a destination point at
     * abscissa dstX on this line
     *
     * @param dstX the given destination abscissa
     * @return the corresponding source point
     */
    public Point2D sourceOf (double dstX)
    {
        double left = staff.system.left;
        double right = staff.system.right;
        double xRatio = (dstX - left) / (right - left);
        double srcX = ((1 - xRatio) * info.getEndPoint(LEFT).getX())
                      + (xRatio * info.getEndPoint(RIGHT).getX());
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
        sb.append(" y:").append(y);
        sb.append("}");

        return sb.toString();
    }
}
