//----------------------------------------------------------------------------//
//                                                                            //
//                         U n i t D i m e n s i o n                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.common;


/**
 * Class <code>UnitDimension</code> is a simple Dimension that is meant to
 * represent a dimension in a sheet, with its components (width and height)
 * specified in units, so the name.
 *
 * <p> This specialization is used to take benefit of compiler checks, to
 * prevent the use of dimensions with incorrect meaning or units. </p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class UnitDimension
    extends SimpleDimension
{
    //~ Constructors -----------------------------------------------------------

    //---------------//
    // UnitDimension //
    //---------------//
    /**
     * Creates an instance of <code>UnitDimension</code> with a width of zero
     * and a height of zero.
     */
    public UnitDimension ()
    {
    }

    //---------------//
    // UnitDimension //
    //---------------//
    /**
     * Constructs a <code>UnitDimension</code> and initializes it to the
     * specified width and specified height.
     *
     * @param width the specified width
     * @param height the specified height
     */
    public UnitDimension (int width,
                          int height)
    {
        super(width, height);
    }
}
