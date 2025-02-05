//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      C o d e d S y m b o l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.ui.symbol.MusicFont.TINY_INTERLINE;
import static org.audiveris.omr.ui.symbol.OmrFont.defaultImageColor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;

/**
 * Class <code>CodedSymbol</code> is a {@link ShapeSymbol} defined by a sequence of code points.
 *
 * @author Hervé Bitteur
 */
public class CodedSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(CodedSymbol.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Sequence of point codes. */
    public final int[] codes;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a <code>CodedSymbol</code> object.
     *
     * @param shape  the related shape
     * @param family the selected MusicFont family
     * @param codes  the point codes
     */
    public CodedSymbol (Shape shape,
                        MusicFamily family,
                        int[] codes)
    {
        super(shape, family);
        this.codes = codes;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // getHexaString //
    //---------------//
    /**
     * Report the symbol code(s) as an hexadecimal string
     *
     * @return the codes hexadecimal value
     */
    public String getHexaString ()
    {
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < codes.length; i++) {
            if (i > 0) {
                sb.append(',');
            }

            sb.append(Long.toHexString(((Number) codes[i]).longValue()));
        }

        return sb.toString();
    }

    //-----------//
    // getParams //
    //-----------//
    /**
     * Report the specific Params object to draw this symbol.
     *
     * @param font scaled music font
     * @return the specific Params object
     */
    @Override
    protected Params getParams (MusicFont font)
    {
        // Select the font that corresponds to symbol family and thus its code
        if (musicFamily != font.getMusicFamily()) {
            font = MusicFont.getMusicFont(musicFamily, font.getSize());
        }

        Params p = new Params();

        p.layout = font.layout(getString()); // Use the font-related codes
        p.rect = p.layout.getBounds();

        return p;
    }

    //-----------//
    // getString //
    //-----------//
    /**
     * Report the String defined by Unicode characters
     *
     * @return the resulting String
     */
    public final String getString ()
    {
        return new String(codes, 0, codes.length);
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        final StringBuilder sb = new StringBuilder(super.internals());

        if (codes != null) {
            sb.append('/').append(getHexaString());
        }

        return sb.toString();
    }

    //-----------//
    // paintIcon //
    //-----------//
    /**
     * Implements Icon interface paintIcon() method.
     *
     * @param c containing component
     * @param g graphic context
     * @param x abscissa
     * @param y ordinate
     */
    @Override
    public void paintIcon (Component c,
                           Graphics g,
                           int x,
                           int y)
    {
        logger.trace("CodedSymbol.paintIcon {} family: {}", this, musicFamily);
        final MusicFont font = MusicFont.getBaseFont(musicFamily, TINY_INTERLINE);
        final Graphics2D g2 = (Graphics2D) g;
        g.setColor(logger.isDebugEnabled() ? Color.RED : defaultImageColor);
        paint(g2, getParams(font), new Point(x, y), Alignment.TOP_LEFT);
    }
}
