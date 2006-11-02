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

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.lag.JunctionAllPolicy;
import omr.lag.SectionsBuilder;
import omr.lag.VerticalOrientation;

import omr.score.PagePoint;
import omr.score.ScoreConstants;

import omr.sheet.Picture;
import omr.sheet.PixelPoint;
import omr.sheet.Scale;

import omr.ui.icon.SymbolIcon;

import omr.util.Logger;

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
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(IconGlyph.class);

    /**
     * Reduction of icon image versus normal glyph size. The ascii descriptions
     * are half the size of their real glyph equivalent, so reduction is 2
     */
    private static final double descReduction = 2f;

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
            final int displayFactor = constants.displayFactor.getValue();

            // Build a picture
            Picture  picture = new Picture(icon.getImage(), 2f * displayFactor);

            // Build related vertical lag
            GlyphLag vLag = new GlyphLag(new VerticalOrientation());
            vLag.setName("iconLag");
            vLag.setVertexClass(GlyphSection.class);

            SectionsBuilder<GlyphLag, GlyphSection> lagBuilder;
            lagBuilder = new SectionsBuilder<GlyphLag, GlyphSection>(
                vLag,
                new JunctionAllPolicy()); // catch all
            lagBuilder.createSections(picture, 0); // minRunLength

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
            int interline = displayFactor * ScoreConstants.INTER_LINE;
            setInterline(interline);

            // Mass center
            PixelPoint centroid = getCentroid();
            Scale      scale = new Scale(interline, 1);
            PagePoint  pgCentroid = scale.toPagePoint(centroid);

            // Number of connected stems
            if (icon.getStemNumber() != null) {
                setStemNumber(icon.getStemNumber());
            }

            // Has a related ledger ?
            if (icon.hasLedger() != null) {
                setWithLedger(icon.hasLedger());
            }

            // Vertical position wrt staff
            if (icon.getPitchPosition() != null) {
                setPitchPosition(icon.getPitchPosition());
            }

            if (logger.isFineEnabled()) {
                dump();
            }
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

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** This ratio has no impact on glyph moments, it is meant only for
           display of the icon glyph in utilities such as the GlyphVerifier */
        Constant.Integer displayFactor = new Constant.Integer(
            4,
            "Scaling factor for IconGlyph display");
    }
}
