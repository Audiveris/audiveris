//----------------------------------------------------------------------------//
//                                                                            //
//                         R e s i z e d S y m b o l                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.symbol;

import omr.glyph.Shape;

import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;

/**
 * Class {@code ResizedSymbol} is a {@link ShapeSymbol} with a ratio
 * different from 1.
 * This is meant for shapes like G_CLEF_SMALL and F_CLEF_SMALL.
 *
 * @author Hervé Bitteur
 */
public class ResizedSymbol
        extends ShapeSymbol
{
    //~ Instance fields --------------------------------------------------------

    /** The display ratio */
    protected final double ratio;

    //~ Constructors -----------------------------------------------------------
    /**
     * Create a non decorated standard ResizedSymbol with the provided
     * shape and codes.
     *
     * @param shape the related shape
     * @param ratio the resizing ratio
     * @param codes the codes for MusicFont characters
     */
    public ResizedSymbol (Shape shape,
                          double ratio,
                          int... codes)
    {
        this(false, shape, ratio, false, codes);
    }

    /**
     * Create a ResizedSymbol with the provided shape and codes
     *
     * @param isIcon    true for an icon
     * @param shape     the related shape
     * @param ratio     the resizing ratio
     * @param decorated true if the symbol uses decoration around the shape
     * @param codes     the codes for MusicFont characters
     */
    public ResizedSymbol (boolean isIcon,
                          Shape shape,
                          double ratio,
                          boolean decorated,
                          int... codes)
    {
        super(isIcon, shape, decorated, codes);
        this.ratio = ratio;
    }

    //~ Methods ----------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ResizedSymbol createIcon ()
    {
        return new ResizedSymbol(true, shape, ratio, decorated, codes);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        sb.append(" ratio:")
                .append((float) ratio);

        return sb.toString();
    }

    //--------//
    // layout //
    //--------//
    /**
     * Report a single layout, based on symbol codes if they exist.
     * This feature can work only with a single "line" of music codes.
     *
     * @param font the specifically-scaled font to use
     * @return the layout ready to be drawn, or null
     */
    @Override
    protected TextLayout layout (MusicFont font)
    {
        AffineTransform at = AffineTransform.getScaleInstance(ratio, ratio);

        return font.layout(shape, at);
    }
}
