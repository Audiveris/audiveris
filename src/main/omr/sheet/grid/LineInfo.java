//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L i n e I n f o                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.glyph.Glyph;

import omr.math.NaturalSpline;

import omr.sheet.StaffLine;

import omr.util.HorizontalSide;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Interface {@code LineInfo} describes the handling of one staff line.
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(StaffLine.Adapter.class)
public interface LineInfo
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Report the absolute contour rectangle
     *
     * @return a copy of the contour box (with minimum height of 1)
     */
    Rectangle getBounds ();

    /**
     * Selector for the left or right ending point of the line
     *
     * @param side proper horizontal side
     * @return left point
     */
    Point2D getEndPoint (HorizontalSide side);

    /**
     * Selector for the underlying glyph
     *
     * @return the underlying glyph
     */
    Glyph getGlyph ();

    /**
     * Selector for the underlying spline.
     *
     * @return the underlying spline
     */
    NaturalSpline getSpline ();

    /**
     * Report the mean line thickness.
     *
     * @return the line thickness
     */
    double getThickness ();

    /**
     * Paint the computed line on the provided graphic environment.
     *
     * @param g          the graphics context
     * @param showPoints true to show the defining points
     * @param pointWidth width for any displayed defining point
     */
    void renderLine (Graphics2D g,
                     boolean showPoints,
                     double pointWidth);

    /**
     * Retrieve the staff line ordinate at given abscissa x, using int values
     *
     * @param x the given abscissa
     * @return the corresponding y value
     */
    int yAt (int x);

    /**
     * Retrieve the staff line ordinate at given abscissa x, using double values
     *
     * @param x the given abscissa
     * @return the corresponding y value
     */
    double yAt (double x);
}
