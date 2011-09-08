//----------------------------------------------------------------------------//
//                                                                            //
//                           S y m b o l G l y p h                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.BasicStick;
import omr.glyph.facets.Glyph;

import omr.lag.JunctionAllPolicy;

import omr.log.Logger;

import omr.run.Orientation;

import omr.stick.StickSection;

import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.SymbolPicture;

import java.awt.image.BufferedImage;

/**
 * Class <code>SymbolGlyph</code> is an articial glyph, built from a symbol.
 * It is used to generate glyphs for training, when no real glyph (glyph
 * retrieved from scanned sheet) is available.
 *
 * @author Hervé Bitteur
 */
public class SymbolGlyph
    extends BasicStick
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolGlyph.class);

    //~ Instance fields --------------------------------------------------------

    /** The underlying symbol, with generic size */
    private final ShapeSymbol symbol;

    /** The underlying image, properly sized */
    private final BufferedImage image;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SymbolGlyph //
    //-------------//
    /**
     * Build an (artificial) glyph out of a symbol icon. This construction is
     * meant to populate and train on glyph shapes for which we have no real
     * instance yet.
     *
     * @param shape the corresponding shape
     * @param symbol the related drawing
     * @param interline the related interline scaling value
     * @param descriptor additional features, if any
     */
    public SymbolGlyph (Shape                 shape,
                        ShapeSymbol           symbol,
                        int                   interline,
                        SymbolGlyphDescriptor descriptor)
    {
        super(interline);
        this.symbol = symbol;
        image = symbol.buildImage(MusicFont.getFont(interline));

        /** Build a dedicated SymbolPicture */
        SymbolPicture picture = new SymbolPicture(image);

        /** Build related vertical lag */
        GlyphLag vLag = new GlyphLag(
            "iLag",
            StickSection.class,
            Orientation.VERTICAL);

        new GlyphSectionsBuilder(vLag, new JunctionAllPolicy()) // catch all
        .createSections("symbol", picture, 0); // minRunLength

        // Retrieve the whole glyph made of all sections
        setLag(vLag);

        for (GlyphSection section : vLag.getSections()) {
            addSection(section, Glyph.Linking.LINK_BACK);
        }

        // Glyph features
        setShape(shape, Evaluation.MANUAL);

        // Use descriptor if any is provided
        if (descriptor != null) {
            // Number of connected stems
            if (descriptor.getStemNumber() != null) {
                setStemNumber(descriptor.getStemNumber());
            }

            // Has a related ledger ?
            if (descriptor.isWithLedger() != null) {
                setWithLedger(descriptor.isWithLedger());
            }

            // Vertical position wrt staff
            if (descriptor.getPitchPosition() != null) {
                setPitchPosition(descriptor.getPitchPosition());
            }
        }

        if (logger.isFineEnabled()) {
            dump();
        }
    }
}
