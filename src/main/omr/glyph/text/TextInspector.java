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

    //-------------------//
    // retrieveTextLines //
    //-------------------//
    /**
     * Align the various text glyphs in horizontal text lines
     * @return the number of recognized textual items
     */
    public int retrieveTextLines ()
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
            int index = 0;

            for (TextLine line : system.getTextLines()) {
                line.setId(++index);

                if (logger.isFineEnabled()) {
                    logger.fine(this + " " + line.toString());
                }

                line.process();
            }
        } catch (Error error) {
            logger.warning(
                "Error in TextInspector.retrieveTextLines: " + error);
        } catch (Exception ex) {
            logger.warning("Exception in TextInspector.retrieveTextLines", ex);
        }

        return modifs;
    }

    //----------------//
    // runTextPattern //
    //----------------//
    /**
     * Besides the existing text-shaped glyphs, using system area subdivision,
     * try to retrieve additional series of glyphs that could represent text
     * portions in the system at hand
     * @return the number of text glyphs built
     */
    public int runTextPattern ()
    {
        // Create a TextArea on the whole system
        TextArea area = new TextArea(
            system,
            null,
            system.getSheet().getVerticalLag().createAbsoluteRoi(
                system.getBounds()),
            new HorizontalOrientation());

        // Subdivide the area, to find and build text glyphs (words most likely)
        area.subdivide();

        // Process alignments of text items
        return retrieveTextLines();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return getClass()
                   .getSimpleName() + " System#" + system.getId();
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
    private boolean feedLine (Glyph               item,
                              SortedSet<TextLine> lines)
    {
        boolean added = false;

        if (logger.isFineEnabled()) {
            logger.fine(this + " feedLine with " + item);
        }

        // First look for a suitable existing text line
        final int maxDy = system.getScoreSystem()
                                .getScale()
                                .toPixels(TextLine.getMaxItemDy());

        for (TextLine line : lines) {
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
        TextLine line = new TextLine(system);
        added = line.addItem(item);
        lines.add(line);

        if (logger.isFineEnabled()) {
            logger.fine("Created new " + line);
        }

        return added;
    }
}
