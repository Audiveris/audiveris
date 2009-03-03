//----------------------------------------------------------------------------//
//                                                                            //
//                              L i n e I n f o                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.glyph.GlyphSection;

import java.awt.Graphics;
import java.util.Collection;

/**
 * Interface <code>LineInfo</code> describes the handling of one staff line.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface LineInfo
{
    //~ Methods ----------------------------------------------------------------

    //---------//
    // cleanup //
    //---------//
    /**
     * Cleanup the line, by removing the sections that compose the line and by
     * extending the external crossing sections
     */
    public void cleanup ();

    //--------//
    // render //
    //--------//
    /**
     * Paint the computed line on the provided environment.
     *
     * @param g     the graphics context
     * @param left  the imposed (for clean alignment) left abscissa
     * @param right the imposed (for clean alignment) right abscissa
     */
    public void render (Graphics g,
                        int      left,
                        int      right);

    //---------//
    // getLeft //
    //---------//
    /**
     * Selector for the left abscissa of the line
     *
     * @return left abscissa, in pixels
     */
    int getLeft ();

    //----------//
    // getRight //
    //----------//
    /**
     * Selector for the right abscissa of the line
     *
     * @return right abscissa, in pixels
     */
    int getRight ();

    //-------------//
    // getSections //
    //-------------//
    /**
     * Report the lag sections that compose the staff line
     *
     * @return a collection of the line sections
     */
    Collection<GlyphSection> getSections ();

    //-----//
    // yAt //
    //-----//
    /**
     * Retrieve the staff line ordinate at given abscissa x, using int values
     *
     * @param x the given abscissa
     *
     * @return the corresponding y value
     */
    int yAt (int x);

    //-----//
    // yAt //
    //-----//
    /**
     * Retrieve the staff line ordinate at given abscissa x, using double values
     *
     * @param x the given abscissa
     *
     * @return the corresponding y value
     */
    double yAt (double x);
}
