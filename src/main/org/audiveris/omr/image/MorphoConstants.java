//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M o r p h o C o n s t a n t s                               //
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
package org.audiveris.omr.image;

/**
 * Interface {@code MorphoConstants}
 *
 * @author ?
 */
public interface MorphoConstants
{

    public static final int FREE = -1;

    public static final int CIRCLE = 0;

    public static final int DIAMOND = 1;

    public static final int LINE = 2;

    public static final int VLINE = 3;

    public static final int HLINE = 4;

    public static final int CLINE = 5;

    public static final int HPOINTS = 6;

    public static final int VPOINTS = 5;

    public static final int SQARE = 7;

    public static final int RING = 8;

    public static final int[] OFFSET0 = {0, 0};

    public static final int[] NGRAD = {0, 1};

    public static final int[] SGRAD = {0, -1};

    public static final int[] WGRAD = {1, 0};

    public static final int[] EGRAD = {-1, 0};

    public static final int[] NEGRAD = {-1, -1};

    public static final int[] NWGRAD = {1, -1};

    public static final int[] SEGRAD = {-1, -1};

    public static final int[] SWGRAD = {1, 1};

    public static final int MAXSIZE = 151;

    public static final int HPLUS = 1;

    public static final int HMINUS = -1;

    public static final int PERIM = -16;

    public static final int FULLAREA = -32;

    public static final int ERODE = 32;

    public static final int DILATE = 64;

    public static final double cor3 = 1.5 - Math.sqrt(3.0);
}
