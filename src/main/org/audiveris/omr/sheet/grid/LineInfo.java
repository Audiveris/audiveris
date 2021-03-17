//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L i n e I n f o                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.math.NaturalSpline;
import org.audiveris.omr.sheet.StaffLine;
import org.audiveris.omr.util.HorizontalSide;

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

    /**
     * Report the same line translated vertically.
     *
     * @param dy amount of vertical translation
     * @return
     */
    LineInfo yTranslated (double dy);
}
