//-----------------------------------------------------------------------//
//                                                                       //
//                       F o n t S h a p e I c o n                       //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 * Class <code>FontShapeIcon</code> handles an icon suitable to represent a
 * given {@link Shape}, to be used in menu items for example. This
 * implementation is based on the use of a music font named <b>Musette</b>,
 * for which code and height ratio are provided.
 *
 * <p><b>Nota</b><i>we have to find a more usable music font.</i>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class FontShapeIcon
    implements Icon
{
    //~ Static variables/initializers -------------------------------------

    private static final Constants constants = new Constants();

    // The font used to draw the icons
    private static final Font mf = new Font("musette",
                                            Font.PLAIN,
                                            constants.fontSize.getValue());

    // Same width for all
    private static final int ww =
        (int) Math.rint(constants.fontSize.getValue() *
                        constants.globalFontWidthRatio.getValue());
    private static final int xx = 0;

    //~ Instance variables ------------------------------------------------

    // The character string used to draw the icon, since we use a specific
    // font (the "Musette" font for the time being) to draw the icon
    private final String string;

    // Height ratio wrt to mean height
    private final double heightRatio;

    // Height varies widely, so it depends on the relatedshape
    private final int hh;
    private final int yy;

    //~ Constructors ------------------------------------------------------

    //---------------//
    // FontShapeIcon //
    //---------------//
    /**
     * Create the proper icon for a given shape
     *
     * @param code the point code
     * @param heightRatio specific height ratio
     */
    public FontShapeIcon(int code,
                         double heightRatio)
    {
        this.string = new String(Character.toChars(code));
        this.heightRatio = heightRatio;
        hh = (int) Math.rint(constants.fontSize.getValue() *
                             constants.globalFontHeightRatio.getValue() *
                             heightRatio);
        yy = (int) Math.rint(constants.fontSize.getValue() *
                             constants.globalFontHeightRatio.getValue() *
                             heightRatio *
                             constants.globalFontBaseRatio.getValue());
    }

    //---------------//
    // FontShapeIcon //
    //---------------//
    /**
     * Create the proper icon for a given shape, with default height ratio
     *
     * @param code the point code
     */
    public FontShapeIcon(int code)
    {
        this(code, 1d);
    }

    //~ Methods -----------------------------------------------------------

    //--------------//
    // getIconWidth //
    //--------------//
    /**
     * Report the width of the icon
     *
     * @return the icon width in pixels
     */
    public int getIconWidth()
    {
        return ww;
    }

    //---------------//
    // getIconHeight //
    //---------------//
    /**
     * Report the height of the icon
     *
     * @return the icon height in pixels
     */
    public int getIconHeight()
    {
        return hh;
    }

    //-----------//
    // paintIcon //
    //-----------//
    /**
     * Used by Swing to properly render the icon
     *
     * @param c the (containing?) component
     * @param g the graphic context
     * @param x ???
     * @param y ???
     */
    public void paintIcon(Component c,
                          Graphics g,
                          int x,
                          int y)
    {
        Font oldFont = g.getFont();
        g.setColor(Color.black);
        g.setFont(mf);
        g.drawString(string, x + xx, y + yy);
        g.setFont(oldFont);
    }

    //~ Classes -----------------------------------------------------------

    private static class Constants
            extends ConstantSet
    {
        Constant.Integer fontSize = new Constant.Integer
                (36,
                 "Size in points for Shape music font");

        Constant.Double globalFontHeightRatio = new Constant.Double
                (1d,
                 "Global ratio between font height (in pixels)" +
                 " and font size (in points)");

        Constant.Double globalFontWidthRatio = new Constant.Double
                (0.8d,
                 "Global ratio between font width (in pixels)" +
                 " and font size (in points)");

        Constant.Double globalFontBaseRatio = new Constant.Double
                (0.6d,
                 "Global ratio between font base line and font height");

        Constants ()
        {
            initialize();
        }
    }
}
