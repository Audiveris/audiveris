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
import omr.lag.SectionsBuilder;
import omr.lag.VerticalOrientation;

import omr.log.Logger;

import omr.score.ui.ScoreConstants;

import omr.stick.StickSection;

import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.SymbolPicture;

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

    /** The underlying icon */
    private ShapeSymbol icon;

    //~ Constructors -----------------------------------------------------------

    //-------------//
    // SymbolGlyph //
    //-------------//
    /**
     * Build an (artificial) glyph out of a symbol icon. This construction is
     * meant to populate and train on glyph shapes for which we have no real
     * instance yet.
     *
     * @param symbol the appearance of the glyph
     * @param shape the corresponding shape
     */
    public SymbolGlyph (ShapeSymbol symbol,
                        Shape       shape)
    {
        super(ScoreConstants.INTER_LINE);

        try {
            /** Build a dedicated SymbolPicture */
            SymbolPicture picture = new SymbolPicture(symbol);

            /** Build related vertical lag */
            GlyphLag vLag = new GlyphLag(
                "iLag",
                StickSection.class,
                new VerticalOrientation());
            SectionsBuilder<GlyphLag, GlyphSection> lagBuilder;
            lagBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
                vLag,
                new JunctionAllPolicy()); // catch all
            lagBuilder.createSections(picture, 0); // minRunLength

            // Retrieve the whole glyph made of all sections
            setLag(vLag);

            for (GlyphSection section : vLag.getSections()) {
                addSection(section, Glyph.Linking.LINK_BACK);
            }

            // Glyph features
            setShape(shape, Evaluation.MANUAL);

            // Ordinate (approximate value)
            getContourBox();

            // Mass center
            getCentroid();

            // Number of connected stems
            if (symbol.getStemNumber() != null) {
                setStemNumber(symbol.getStemNumber());
            }

            // Has a related ledger ?
            if (symbol.isWithLedger() != null) {
                setWithLedger(symbol.isWithLedger());
            }

            // Vertical position wrt staff
            if (symbol.getPitchPosition() != null) {
                setPitchPosition(symbol.getPitchPosition());
            }

            if (logger.isFineEnabled()) {
                dump();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //-------------//
    // SymbolGlyph //
    //-------------//
    /**
     * Create a new SymbolGlyph object, using the symbol related to the provided
     * shape
     *
     * @param shape The related shape
     */
    public SymbolGlyph (Shape shape)
    {
        this(shape.getSymbol(), shape);
    }
}
