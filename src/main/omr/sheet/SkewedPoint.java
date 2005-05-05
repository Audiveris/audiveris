//-----------------------------------------------------------------------//
//                                                                       //
//                         S k e w e d P o i n t                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.sheet;

import java.awt.*;

/**
 * Class <code>SkewedPoint</code> is a simple Point that is meant to represent
 * a point in the original image : a raw, probably skewed page, with its
 * coordinates specified in pixels.
 * <p/>
 * <p/>
 * This specialization is used to take benefit of compiler checks, to prevent
 * the use of points with incorrect meaning or units. </p>
 */
public class SkewedPoint
        extends Point
{
}
