//----------------------------------------------------------------------------//
//                                                                            //
//                         T e x t I n s p e c t o r                          //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.text;

import omr.glyph.Glyph;

import omr.lag.HorizontalOrientation;

import omr.log.Logger;

import omr.sheet.SystemInfo;

import java.util.SortedSet;

/**
 * Class <code>TextInspector</code> handles the inspection of textual items in
 * a system
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class TextInspector
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(TextInspector.class);

    //~ Instance fields --------------------------------------------------------

    /** Dedicated system */
    private final SystemInfo system;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new TextInspector object.
     *
     * @param system The dedicated system
     */
    public TextInspector (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // alignTextGlyphs //
    //-----------------//
    /**
     * Align the various text glyphs in horizontal text lines
     * @return the number of recognized textual items
     */
    public int alignTextGlyphs ()
    {
        int modifs = 0;

        try {
            // Keep the previous work! No textLines.clear();
            for (Glyph glyph : system.getGlyphs()) {
                if ((glyph.getShape() != null) && glyph.getShape()
                                                       .isText()) {
                    if (feedLine(glyph, system.getTextLines())) {
                        modifs++;
                    }
                }
            }

            // (Re)assign an id to each line
            if (logger.isFineEnabled()) {
                logger.fine("System#" + system.getId());
            }

            int index = 0;

            for (TextGlyphLine line : system.getTextLines()) {
                line.setId(++index);

                if (logger.isFineEnabled()) {
                    logger.fine(line.toString());
                }

                line.processGlyphs();
            }
        } catch (Error error) {
            logger.warning("Error in TextInspector.alignTextGlyphs: " + error);
        } catch (Exception ex) {
            logger.warning("Exception in TextInspector.alignTextGlyphs", ex);
        }

        return modifs;
    }

    //--------------------//
    // retrieveTextGlyphs //
    //--------------------//
    /**
     * Retrieve the various glyphs and series of glyphs that could represent
     * text portions in the system at hand
     * @return the number of text glyphs built
     */
    public int retrieveTextGlyphs ()
    {
        TextArea area = new TextArea(
            system,
            null,
            system.getSheet().getVerticalLag().createAbsoluteRoi(
                system.getBounds()),
            new HorizontalOrientation());

        // Subdivide the area, to find and build text glyphs (words most likely)
        area.subdivide(system.getSheet());

        // Process alignments of text items
        return alignTextGlyphs();
    }

    //----------//
    // feedLine //
    //----------//
    /**
     * Populate a Text line with this text glyph
     *
     * @param item the text item to host in a text line
     * @param lines the collections of text glyph lines
     * @return true if the glyph was really added
     */
    private boolean feedLine (Glyph                    item,
                              SortedSet<TextGlyphLine> lines)
    {
        boolean added = false;

        if (logger.isFineEnabled()) {
            logger.fine("Feeding a GlyphTextLine with " + item);
        }

        // First look for a suitable existing text line
        final int maxDy = system.getScoreSystem()
                                .getScale()
                                .toPixels(TextGlyphLine.getMaxItemDy());

        for (TextGlyphLine line : lines) {
            if (line.isAlignedWith(item.getLocation(), maxDy)) {
                added = line.addItem(item);

                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Inserted glyph #" + item.getId() + " into " + line);
                }

                return added;
            }
        }

        // No compatible line, so create a brand new one
        TextGlyphLine line = new TextGlyphLine(system);
        added = line.addItem(item);
        lines.add(line);

        if (logger.isFineEnabled()) {
            logger.fine("Created new " + line);
        }

        return added;
    }
}
