//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S y m b o l G l y p h                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;

import omr.lag.JunctionAllPolicy;
import omr.lag.Section;
import omr.lag.SectionFactory;

import omr.run.Orientation;

import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.util.List;

/**
 * Class {@code SymbolGlyph} is an artificial glyph, built from a symbol.
 * It is used to generate glyph instances for training, when no real glyph
 * (glyph retrieved from scanned sheet) is available.
 *
 * @author Hervé Bitteur
 */
public class SymbolGlyph
        extends BasicGlyph
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolGlyph.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Build an (artificial) glyph out of a symbol icon.
     * This construction is meant to populate and train on glyph shapes for
     * which we have no real instance yet.
     *
     * @param shape      the corresponding shape
     * @param symbol     the related drawing
     * @param interline  the related interline scaling value
     * @param layer      the layer for glyph
     * @param descriptor additional features, if any
     */
    public SymbolGlyph (Shape shape,
                        ShapeSymbol symbol,
                        int interline,
                        GlyphLayer layer,
                        SymbolGlyphDescriptor descriptor)
    {
        super(interline, layer);

        BufferedImage image = symbol.buildImage(MusicFont.getFont(interline));

        /** Build a dedicated SymbolPicture */
        ByteProcessor buffer = createBuffer(image);

        List<Section> sections = new SectionFactory(
                Orientation.VERTICAL,
                JunctionAllPolicy.INSTANCE).createSections(buffer, null);

        // Retrieve the whole glyph made of all sections
        for (Section section : sections) {
            addSection(section, Glyph.Linking.LINK);
        }

        // Glyph features
        setShape(shape);

        // Use descriptor if any is provided
        if (descriptor != null) {
            // Vertical position wrt staff
            if (descriptor.getPitchPosition() != null) {
                setPitchPosition(descriptor.getPitchPosition());
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug(dumpOf());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // createBuffer //
    //--------------//
    private ByteProcessor createBuffer (BufferedImage img)
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

        //TODO: binarize?
        buf.threshold(216);

        return buf;
    }
}
