//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S y m b o l S a m p l e                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.classifier.Sample;
import omr.run.Orientation;
import omr.run.RunTable;
import omr.run.RunTableFactory;

import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;

/**
 * Class {@code SymbolSample} is an artificial symbol, built from a font icon.
 * It is used to generate glyph instances for training, when no real glyph
 * (glyph retrieved from scanned sheet) is available.
 *
 * @author Hervé Bitteur
 */
public class SymbolSample
        extends Sample
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolSample.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Build an (artificial) sample out of a symbol icon.
     * This is meant to populate and train on shapes for which we have no real sample yet.
     *
     * @param shape     the corresponding shape
     * @param interline the related interline scaling value
     * @param runTable  table of runs
     */
    protected SymbolSample (Shape shape,
                            int interline,
                            RunTable runTable)
    {
        super(0, 0, runTable, interline, null, shape);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    public static SymbolSample create (Shape shape,
                                       ShapeSymbol symbol,
                                       int interline)
    {
        // Build the corresponding runTable
        BufferedImage image = symbol.buildImage(MusicFont.getFont(interline));
        ByteProcessor buffer = createBuffer(image);
        RunTableFactory factory = new RunTableFactory(Orientation.VERTICAL);
        RunTable runTable = factory.createTable(buffer);

        // Allocate the sample
        SymbolSample sample = new SymbolSample(symbol.getShape(), interline, runTable);

        return sample;
    }

    //--------------//
    // createBuffer //
    //--------------//
    private static ByteProcessor createBuffer (BufferedImage img)
    {
        DataBuffer dataBuffer = img.getData().getDataBuffer();
        ByteProcessor buf = new ByteProcessor(img.getWidth(), img.getHeight());

        for (int y = 0, h = img.getHeight(); y < h; y++) {
            for (int x = 0, w = img.getWidth(); x < w; x++) {
                int index = x + (y * w);
                int elem = dataBuffer.getElem(index);

                // ShapeSymbol instances use alpha channel as the pixel level
                // With 0 as totally transparent so background (255)
                // And with 255 as totally opaque so foreground (0)
                int val = 255 - (elem >>> 24);
                buf.set(x, y, val);
            }
        }

        // binarize
        buf.threshold(216);

        return buf;
    }
}
