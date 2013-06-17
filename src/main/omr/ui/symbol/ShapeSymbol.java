//----------------------------------------------------------------------------//
//                                                                            //
//                           S h a p e S y m b o l                            //
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
import static omr.glyph.Shape.*;

import java.awt.AlphaComposite;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Class {@code ShapeSymbol} extends the {@link BasicSymbol} with the
 * handling of a related {@link Shape}.
 * A ShapeSymbol thus adds several features:<ul>
 *
 * <li>It can be used for Drag & Drop operations, since it implements the
 * {@link Transferable} interface.</li>
 *
 * <li>It can be used to <b>train</b> the glyph evaluator when we don't
 * have enough "real" glyphs available.</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class ShapeSymbol
        extends BasicSymbol
        implements Symbol, Transferable
{
    //~ Static fields/initializers ---------------------------------------------

    /** The symbol meta data */
    public static DataFlavor DATA_FLAVOR = new DataFlavor(
            Symbol.class,
            "shape-symbol");

    /** Composite used for decoration */
    protected static AlphaComposite decoComposite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            0.15f);

    //~ Instance fields --------------------------------------------------------
    /** Related shape */
    protected final Shape shape;

    /** Is this a decorated symbol (shape with additional stuff) */
    protected final boolean decorated;

    //~ Constructors -----------------------------------------------------------
    //-------------//
    // ShapeSymbol //
    //-------------//
    /**
     * Create a ShapeSymbol with the provided shape and codes
     *
     * @param isIcon    true for an icon
     * @param shape     the related shape
     * @param decorated true if the symbol uses decoration around the shape
     * @param codes     the codes for MusicFont characters
     */
    public ShapeSymbol (boolean isIcon,
                        Shape shape,
                        boolean decorated,
                        int... codes)
    {
        super(isIcon, codes);
        this.shape = shape;
        this.decorated = decorated;
    }

    //-------------//
    // ShapeSymbol //
    //-------------//
    /**
     * Create a non decorated standard ShapeSymbol with the provided
     * shape and codes.
     *
     * @param shape the related shape
     * @param codes the codes for MusicFont characters
     */
    public ShapeSymbol (Shape shape,
                        int... codes)
    {
        this(false, shape, false, codes);
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape of the symbol.
     *
     * @return the shape
     */
    public Shape getShape ()
    {
        return shape;
    }

    //-----------------//
    // getTransferData //
    //-----------------//
    @Override
    public Object getTransferData (DataFlavor flavor)
            throws UnsupportedFlavorException, IOException
    {
        if (isDataFlavorSupported(flavor)) {
            return this;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    //------------------------//
    // getTransferDataFlavors //
    //------------------------//
    @Override
    public DataFlavor[] getTransferDataFlavors ()
    {
        return new DataFlavor[]{DATA_FLAVOR};
    }

    //-----------------------//
    // isDataFlavorSupported //
    //-----------------------//
    @Override
    public boolean isDataFlavorSupported (DataFlavor flavor)
    {
        return flavor == DATA_FLAVOR;
    }

    //-------------//
    // isDecorated //
    //-------------//
    /**
     * Tell whether the image represents the shape with additional
     * decorations.
     *
     * @return true if decorated
     */
    public boolean isDecorated ()
    {
        return decorated;
    }

    //-------------//
    // numberCodes //
    //-------------//
    public static int[] numberCodes (int number)
    {
        ShapeSymbol symbol = Symbols.getSymbol(TIME_ZERO);
        int base = symbol.codes[0];
        int[] numberCodes = (number > 9) ? new int[2] : new int[1];
        int index = 0;

        if (number > 9) {
            numberCodes[index++] = base + (number / 10);
        }

        numberCodes[index] = base + (number % 10);

        return numberCodes;
    }

    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new ShapeSymbol(true, shape, decorated, codes);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        sb.append(" ")
                .append(shape);

        return sb.toString();
    }
}
