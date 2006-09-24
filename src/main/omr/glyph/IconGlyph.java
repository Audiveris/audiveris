//----------------------------------------------------------------------------//
//                                                                            //
//                             I c o n G l y p h                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.lag.JunctionAllPolicy;
import omr.lag.LagBuilder;
import omr.lag.VerticalOrientation;

import omr.score.PagePoint;
import omr.score.ScoreConstants;

import omr.sheet.Picture;
import omr.sheet.PixelPoint;
import omr.sheet.Scale;

import omr.ui.icon.SymbolIcon;

import java.awt.Rectangle;

/**
 * Class <code>IconGlyph</code> is an articial glyph, built from an icon. It is
 * used to generate glyphs for training, when no real glyph (glyph retrieved
 * from scanned sheet) is available.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class IconGlyph
    extends Glyph
{
    //~ Instance fields --------------------------------------------------------

    /** The underlying icon */
    private SymbolIcon icon;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // IconGlyph //
    //-----------//
    /**
     * Build an (artificial) glyph out of a symbol icon. This construction is
     * meant to populate and train on glyph shapes for which we have no real
     * instance yet.
     *
     * @param icon the appearance of the glyph
     * @param shape the corresponding shape
     */
    public IconGlyph (SymbolIcon icon,
                      Shape      shape)
    {
        try {
            // Build a picture
            Picture  picture = new Picture(icon.getImage(), 2.0f);

            // Build related vertical lag
            GlyphLag vLag = new GlyphLag(new VerticalOrientation());
            vLag.setName("iconLag");
            vLag.setVertexClass(GlyphSection.class);
            new LagBuilder<GlyphLag, GlyphSection>().rip(
                vLag,
                picture,
                0, // minRunLength
                new JunctionAllPolicy()); // catch all

            // Retrieve the whole glyph made of all sections
            setLag(vLag);

            for (GlyphSection section : vLag.getSections()) {
                addSection(section, /* link => */
                           true);
            }

            //vLag.dump("Icon Lag");
            //glyph.drawAscii();

            // Glyph features
            setShape(shape);

            // Ordinate (approximate value)
            Rectangle box = getContourBox();
            int       y = box.y;

            // Staff interline value
            setInterline(2 * ScoreConstants.INTER_LINE);

            // Mass center
            PixelPoint centroid = getCentroid();
            Scale      scale = new Scale(2 * ScoreConstants.INTER_LINE, 1);
            PagePoint  pgCentroid = scale.toPagePoint(centroid);

            // Number of connected stems
            setStemNumber(icon.getStemNumber());

            // Has a related ledger ?
            setHasLedger(icon.hasLedger());

            // Vertical position wrt staff
            setPitchPosition(icon.getPitchPosition());

            //glyph.dump();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // setIcon //
    //---------//
    /**
     * Assign the related icon
     *
     * @param val the icon
     */
    public void setIcon (SymbolIcon val)
    {
        this.icon = val;
    }

    //---------//
    // getIcon //
    //---------//
    /**
     * Report the related icon
     *
     * @return the icon
     */
    public SymbolIcon getIcon ()
    {
        return icon;
    }
}
