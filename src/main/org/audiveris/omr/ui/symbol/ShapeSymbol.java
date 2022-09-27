//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S h a p e S y m b o l                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
import static org.audiveris.omr.glyph.Shape.*;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.ui.ObjectUIModel;

import java.awt.AlphaComposite;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Class <code>ShapeSymbol</code> extends the {@link BasicSymbol} with the handling of a
 * related {@link Shape}.
 * <p>
 * A ShapeSymbol can thus be used:
 * <ul>
 * <li>For dedicated shape assignment buttons.
 * <li>For Drag n' Drop operations, since it implements the {@link Transferable} interface.
 * <li>To <b>train</b> the glyph classifier when we don't have enough "real" glyphs available.
 * </ul>
 * Beside the plain shape image, a ShapeSymbol may also provide a <b>decorated</b> version
 * whose image represents the shape within a larger decoration.
 *
 * @author Hervé Bitteur
 */
public class ShapeSymbol
        extends BasicSymbol
        implements Transferable
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** The symbol meta data. */
    public static final DataFlavor DATA_FLAVOR = new DataFlavor(SymbolIcon.class, "shape-symbol");

    /** Composite used for decoration. */
    protected static final AlphaComposite decoComposite = AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER,
            0.15f);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related shape. */
    protected final Shape shape;

    /** Is this a decorated symbol. (shape with additional decoration) */
    protected boolean isDecorated;

    /** Decorated version if any. */
    protected ShapeSymbol decoratedVersion;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a standard ShapeSymbol with the provided shape and codes.
     *
     * @param shape the related shape
     * @param codes the codes for MusicFont characters
     */
    public ShapeSymbol (Shape shape,
                        int... codes)
    {
        super(codes);
        this.shape = shape;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------------//
    // getDecoratedSymbol //
    //--------------------//
    /**
     * Report the (preferably decorated) version of this ShapeSymbol.
     *
     * @return the decorated version if any, otherwise the plain version
     */
    public ShapeSymbol getDecoratedVersion ()
    {
        if (isDecorated) {
            return this;
        }

        if (!supportsDecoration()) {
            return this; // Fallback using the non-decorated version
        } else {
            if (decoratedVersion == null) {
                decoratedVersion = createDecoratedVersion();
            }

            return decoratedVersion;
        }
    }

    //----------//
    // getModel //
    //----------//
    /**
     * Get inter geometry from this (dropped) symbol.
     *
     * @param font     properly scaled font
     * @param location dropping location
     * @return the data model for inter being shaped
     * @throws UnsupportedOperationException if operation is not explicitly supported by the symbol
     */
    public ObjectUIModel getModel (MusicFont font,
                                   Point location)
    {
        throw new UnsupportedOperationException(); // By default
    }

    //-------------//
    // updateModel //
    //-------------//
    /**
     * Tell the symbol that it can update its model with staff informations.
     * <p>
     * This is useful when the dragged item enters a staff, since it can adapt itself to
     * staff informations (such as the typical beam thickness for small staff).
     *
     * @param staff underlying staff
     */
    public void updateModel (Staff staff)
    {
        // Void, by default
    }

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

    //--------//
    // getTip //
    //--------//
    /**
     * Report the tip text to display for this symbol.
     *
     * @return the tip text
     */
    public String getTip ()
    {
        return shape.toString(); // By default, use the shape name
    }

    //-----------------//
    // getTransferData //
    //-----------------//
    @Override
    public Object getTransferData (DataFlavor flavor)
            throws UnsupportedFlavorException,
                   IOException
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
     * Tell whether the image represents the shape with additional decorations.
     *
     * @return true if decorated
     */
    public boolean isDecorated ()
    {
        return isDecorated;
    }

    //-------------//
    // updateModel //
    //-------------//
    /**
     * Tell the symbol that it can update its model with sheet informations.
     * <p>
     * This is useful when the dragged item enters a sheet view, since it can adapt itself to
     * sheet informations (such as the typical beam thickness).
     *
     * @param sheet underlying sheet
     */
    public void updateModel (Sheet sheet)
    {
        // Void, by default
    }

    //------------------------//
    // createDecoratedVersion //
    //------------------------//
    /**
     * Create symbol with proper decorations.
     * To be redefined by each subclass that does provide a specific decorated version
     *
     * @return the decorated version, which may be the same symbol if no decoration exists
     */
    protected ShapeSymbol createDecoratedVersion ()
    {
        if (!supportsDecoration()) {
            return null; // No decoration by default
        }

        try {
            final ShapeSymbol clone = (ShapeSymbol) clone();
            clone.setDecorated();

            return clone;
        } catch (Exception ex) {
            logger.warn("No decorated glyph for code: {} shape: {}",
                        getHexaString(), shape);

            return null;
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(" ").append(shape);

        if (isDecorated) {
            sb.append(" DECORATED");
        }

        return sb.toString();
    }

    //--------------------//
    // supportsDecoration //
    //--------------------//
    /**
     * Report whether this symbol class can have a decorated version.
     * <p>
     * Answer is false by default and thus must be overridden by any ShapeSymbol subclass that does
     * provide support for decoration.
     *
     * @return true if so
     */
    protected boolean supportsDecoration ()
    {
        return false; // By default
    }

    //--------------//
    // setDecorated //
    //--------------//
    private void setDecorated ()
    {
        isDecorated = true;
        computeImage(); // Recompute image and dimension as well
    }
}
