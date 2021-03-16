//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       A l i g n m e n t                                        //
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
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.math.PointUtil;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code Alignment} defines how a location is to be understood (vertically and
 * horizontally) with respect to symbol rectangular bounds.
 *
 * @author Hervé Bitteur
 */
public class Alignment
{

    /** Pre-defined alignment on top left of symbol */
    public static final Alignment TOP_LEFT = new Alignment(Vertical.TOP, Horizontal.LEFT);

    /** Pre-defined alignment on top center of symbol */
    public static final Alignment TOP_CENTER = new Alignment(Vertical.TOP, Horizontal.CENTER);

    /** Pre-defined alignment on top left of symbol */
    public static final Alignment TOP_RIGHT = new Alignment(Vertical.TOP, Horizontal.RIGHT);

    /** Pre-defined alignment on middle left of symbol */
    public static final Alignment MIDDLE_LEFT = new Alignment(Vertical.MIDDLE, Horizontal.LEFT);

    /** Pre-defined alignment on middle center of symbol */
    public static final Alignment AREA_CENTER = new Alignment(Vertical.MIDDLE, Horizontal.CENTER);

    /** Pre-defined alignment on middle right of symbol */
    public static final Alignment MIDDLE_RIGHT = new Alignment(Vertical.MIDDLE, Horizontal.RIGHT);

    /** Pre-defined alignment on baseline left of symbol (for text) */
    public static final Alignment BASELINE_LEFT = new Alignment(Vertical.BASELINE, Horizontal.LEFT);

    /** Pre-defined alignment on baseline center of symbol */
    public static final Alignment BASELINE_CENTER = new Alignment(Vertical.BASELINE,
                                                                  Horizontal.CENTER);

    /** Pre-defined alignment on baseline right of symbol */
    public static final Alignment BASELINE_RIGHT
            = new Alignment(Vertical.BASELINE, Horizontal.RIGHT);

    /** Pre-defined alignment on baseline origin of symbol (for text) */
    public static final Alignment BASELINE_XORIGIN = new Alignment(Vertical.BASELINE,
                                                                   Horizontal.XORIGIN);

    /** Pre-defined alignment on bottom left of symbol */
    public static final Alignment BOTTOM_LEFT = new Alignment(Vertical.BOTTOM, Horizontal.LEFT);

    /** Pre-defined alignment on bottom center of symbol */
    public static final Alignment BOTTOM_CENTER = new Alignment(Vertical.BOTTOM, Horizontal.CENTER);

    /** Pre-defined alignment on bottom right of symbol */
    public static final Alignment BOTTOM_RIGHT = new Alignment(Vertical.BOTTOM, Horizontal.RIGHT);

    /** The vertical alignment */
    public final Vertical vertical;

    /** The horizontal alignment */
    public final Horizontal horizontal;

    /**
     * Create an Alignment instance
     *
     * @param vertical   vertical part
     * @param horizontal horizontal part
     */
    public Alignment (Vertical vertical,
                      Horizontal horizontal)
    {
        if ((vertical == null) || (horizontal == null)) {
            throw new IllegalArgumentException("Cannot create Alignment with null members");
        }

        this.vertical = vertical;
        this.horizontal = horizontal;
    }

    //--------//
    // equals //
    //--------//
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof Alignment) {
            Alignment that = (Alignment) obj;

            return (this.horizontal == that.horizontal) && (this.vertical == that.vertical);
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
        hash = (73 * hash) + vertical.hashCode();
        hash = (73 * hash) + horizontal.hashCode();

        return hash;
    }

    //---------//
    // toPoint //
    //---------//
    /**
     * Report the vector needed to translate from this alignment around
     * a rectangle to that alignment
     *
     * @param that the desired alignment
     * @param rect the symbol bounds
     * @return the translation vector
     */
    public Point toPoint (Alignment that,
                          Rectangle rect)
    {
        return new Point(horizontal.dxToPoint(that.horizontal, rect), vertical.dyToPoint(
                         that.vertical, rect));
    }

    //---------//
    // toPoint //
    //---------//
    /**
     * Report the vector needed to translate from this alignment around
     * a rectangle to that alignment
     *
     * @param that the desired alignment
     * @param rect the symbol bounds
     * @return the translation vector
     */
    public Point2D toPoint (Alignment that,
                            Rectangle2D rect)
    {
        return new Point2D.Double(horizontal.dxToPoint(that.horizontal, rect), vertical.dyToPoint(
                                  that.vertical, rect));
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "[" + vertical + "," + horizontal + "]";
    }

    //--------------//
    // toTextOrigin //
    //--------------//
    /**
     * Report the vector needed to translate from this alignment around
     * a rectangle to the text origin [BASELINE,XORIGIN]
     *
     * @param rect the symbol bounds
     * @return the translation vector
     */
    public Point toTextOrigin (Rectangle rect)
    {
        return new Point(horizontal.dxToTextOrigin(rect), vertical.dyToTextOrigin(rect));
    }

    //--------------//
    // toTextOrigin //
    //--------------//
    /**
     * Report the vector needed to translate from this alignment around
     * a rectangle to the text origin [BASELINE,XORIGIN]
     *
     * @param rect the symbol bounds
     * @return the translation vector
     */
    public Point2D toTextOrigin (Rectangle2D rect)
    {
        return new Point2D.Double(horizontal.dxToTextOrigin(rect), vertical.dyToTextOrigin(rect));
    }

    //-----------------//
    // translatedPoint //
    //-----------------//
    /**
     * Report the proper translated location, aligned with that alignment
     *
     * @param that     the desired alignment
     * @param rect     the symbol bounds
     * @param location the provided location
     * @return the translated point
     */
    public Point2D translatedPoint (Alignment that,
                                    Rectangle2D rect,
                                    Point2D location)
    {
        if (!this.equals(that)) {
            Point2D toOrigin = toPoint(that, rect);
            PointUtil.add(toOrigin, location);

            return toOrigin;
        } else {
            return new Point2D.Double(location.getX(), location.getY());
        }
    }

    //----------//
    // Vertical //
    //----------//
    /**
     * The reference y line for this symbol.
     */
    public static enum Vertical
    {
        TOP,
        MIDDLE,
        BOTTOM,
        BASELINE;

        //-----------//
        // dyToPoint //
        //-----------//
        public int dyToPoint (Vertical that,
                              Rectangle rect)
        {
            if (this == BASELINE) {
                if (that == BASELINE) {
                    return 0;
                } else {
                    return rect.y + (((that.ordinal() - TOP.ordinal()) * rect.height) / 2);
                }
            } else if (that == BASELINE) {
                return -rect.y + (((TOP.ordinal() - this.ordinal()) * rect.height) / 2);
            } else {
                return ((that.ordinal() - this.ordinal()) * rect.height) / 2;
            }
        }

        //-----------//
        // dyToPoint //
        //-----------//
        public double dyToPoint (Vertical that,
                                 Rectangle2D rect)
        {
            if (this == BASELINE) {
                if (that == BASELINE) {
                    return 0;
                } else {
                    return rect.getY() + (((that.ordinal() - TOP.ordinal()) * rect.getHeight()) / 2);
                }
            } else if (that == BASELINE) {
                return -rect.getY() + (((TOP.ordinal() - this.ordinal()) * rect.getHeight()) / 2);
            } else {
                return ((that.ordinal() - this.ordinal()) * rect.getHeight()) / 2;
            }
        }

        //----------------//
        // dyToTextOrigin //
        //----------------//
        public int dyToTextOrigin (Rectangle rect)
        {
            return dyToPoint(BASELINE, rect);
        }

        //----------------//
        // dyToTextOrigin //
        //----------------//
        public double dyToTextOrigin (Rectangle2D rect)
        {
            return dyToPoint(BASELINE, rect);
        }
    }

    //------------//
    // Horizontal //
    //------------//
    /**
     * The reference x line for this symbol.
     */
    public static enum Horizontal
    {
        LEFT,
        CENTER,
        RIGHT,
        XORIGIN;

        //-----------//
        // dxToPoint //
        //-----------//
        public int dxToPoint (Horizontal that,
                              Rectangle rect)
        {
            if (this == XORIGIN) {
                if (that == XORIGIN) {
                    return 0;
                } else {
                    return rect.x + (((that.ordinal() - LEFT.ordinal()) * rect.width) / 2);
                }
            } else if (that == XORIGIN) {
                return -rect.x + (((LEFT.ordinal() - this.ordinal()) * rect.width) / 2);
            } else {
                return ((that.ordinal() - this.ordinal()) * rect.width) / 2;
            }
        }

        //-----------//
        // dxToPoint //
        //-----------//
        public double dxToPoint (Horizontal that,
                                 Rectangle2D rect)
        {
            if (this == XORIGIN) {
                if (that == XORIGIN) {
                    return 0;
                } else {
                    return rect.getX() + (((that.ordinal() - LEFT.ordinal()) * rect.getWidth()) / 2);
                }
            } else if (that == XORIGIN) {
                return -rect.getX() + (((LEFT.ordinal() - this.ordinal()) * rect.getWidth()) / 2);
            } else {
                return ((that.ordinal() - this.ordinal()) * rect.getWidth()) / 2;
            }
        }

        //----------------//
        // dxToTextOrigin //
        //----------------//
        public int dxToTextOrigin (Rectangle rect)
        {
            return dxToPoint(XORIGIN, rect);
        }

        //----------------//
        // dxToTextOrigin //
        //----------------//
        public double dxToTextOrigin (Rectangle2D rect)
        {
            return dxToPoint(XORIGIN, rect);
        }
    }
}
