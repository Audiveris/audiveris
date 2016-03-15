//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   F i x e d W i d t h I c o n                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import java.awt.Component;
import java.awt.Graphics;
import java.util.Objects;

import javax.swing.Icon;

/**
 * Force an icon to be painted within a given width.
 */
public class FixedWidthIcon
        implements Icon
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final int width = 35; // TODO: use an application constant

    //~ Instance fields ----------------------------------------------------------------------------
    private final Icon icon;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code FixedIcon} object.
     *
     * @param icon the underlying icon
     */
    public FixedWidthIcon (Icon icon)
    {
        Objects.requireNonNull(icon, "FixedWidthIcon needs a non-null icon");
        this.icon = icon;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public int getIconHeight ()
    {
        return icon.getIconHeight();
    }

    @Override
    public int getIconWidth ()
    {
        return width;
    }

    @Override
    public void paintIcon (Component c,
                           Graphics g,
                           int x,
                           int y)
    {
        int w = icon.getIconWidth();
        icon.paintIcon(c, g, x + ((width - w) / 2), y);
    }
}
