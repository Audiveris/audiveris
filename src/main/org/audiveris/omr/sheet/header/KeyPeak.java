//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          K e y P e a k                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet.header;

import org.audiveris.omr.math.Range;

import java.util.Comparator;

/**
 * Formalizes a peak while browsing key signature abscissa range.
 *
 * @author Hervé Bitteur
 */
public class KeyPeak
        extends Range
{

    /** To sort peaks by their area value. */
    public static final Comparator<KeyPeak> byArea = new Comparator<KeyPeak>()
    {
        @Override
        public int compare (KeyPeak p1,
                            KeyPeak p2)
        {
            if (p1 == p2) {
                return 0;
            }

            return Integer.compare(p1.area, p2.area);
        }
    };

    /** Max cumulated height. */
    protected int height;

    /** Weight of peak. */
    protected int area;

    /**
     * Creates a new {@code KeyPeak} object.
     *
     * @param start  x at peak start
     * @param main   x at max y
     * @param stop   x at peak stop
     * @param height max y
     * @param area   sum of y on peak range
     */
    public KeyPeak (int start,
                    int main,
                    int stop,
                    int height,
                    int area)
    {
        super(start, main, stop);
        this.height = height;
        this.area = area;
    }

    /**
     * Report peak center abscissa
     *
     * @return the abscissa of peak center
     */
    public double getCenter ()
    {
        return 0.5 * (min + max);
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Peak{");
        sb.append(min);
        sb.append("-");
        sb.append(max);
        sb.append("/");
        sb.append("h:").append(height);
        sb.append(",a:").append(area);
        sb.append("}");

        return sb.toString();
    }
}
