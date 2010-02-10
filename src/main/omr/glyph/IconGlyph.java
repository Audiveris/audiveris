//----------------------------------------------------------------------------//
//                                                                            //
//                             I c o n G l y p h                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;

import omr.lag.JunctionAllPolicy;
import omr.lag.SectionsBuilder;
import omr.lag.VerticalOrientation;

import omr.log.Logger;

import omr.score.ui.ScoreConstants;

import omr.ui.icon.SymbolIcon;
import omr.ui.icon.SymbolPicture;

/**
 * Class <code>IconGlyph</code> is an articial glyph, built from an icon. It is
 * used to generate glyphs for training, when no real glyph (glyph retrieved
 * from scanned sheet) is available.
 *
 * @author Hervé Bitteur
 */
public class IconGlyph
    extends BasicGlyph
    implements Glyph
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(IconGlyph.class);

    /**
     * Reduction of icon image versus normal glyph size. The ascii descriptions
     * of SymbolIcon are half the size of their real glyph equivalent, so
     * reduction is 2.
     */
    private static final int descReduction = 2;

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
        super(ScoreConstants.INTER_LINE);

        try {
            /** Build a dedicated SymbolPicture */
            SymbolPicture picture = new SymbolPicture(icon, descReduction);

            /** Build related vertical lag */
            GlyphLag vLag = new GlyphLag(
                "iLag",
                GlyphSection.class,
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

            //vLag.dump("Icon Lag");
            //glyph.drawAscii();

            // Glyph features
            setShape(shape, Evaluation.MANUAL);

            // Ordinate (approximate value)
            getContourBox();

            // Mass center
            getCentroid();

            // Number of connected stems
            if (icon.getStemNumber() != null) {
                setStemNumber(icon.getStemNumber());
            }

            // Has a related ledger ?
            if (icon.isWithLedger() != null) {
                setWithLedger(icon.isWithLedger());
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

    //-----------//
    // IconGlyph //
    //-----------//
    /**
     * Create a new IconGlyph object, using the icon related to the provided
     * shape
     *
     * @param shape The related shape
     */
    public IconGlyph (Shape shape)
    {
        this((SymbolIcon) shape.getIcon(), shape);
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
