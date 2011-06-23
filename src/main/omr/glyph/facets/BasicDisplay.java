//----------------------------------------------------------------------------//
//                                                                            //
//                          B a s i c D i s p l a y                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.GlyphSection;
import omr.glyph.Shape;

import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.ui.SectionView;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Class {@code BasicDisplay} is the basic implementation of a display facet
 *
 * @author Hervé Bitteur
 */
class BasicDisplay
    extends BasicFacet
    implements GlyphDisplay
{
    //~ Static fields/initializers ---------------------------------------------

    // Empty map always returned when there is no attachment
    protected static Map<String, java.awt.Shape> EMPTY_MAP = Collections.unmodifiableMap(
        new HashMap<String, java.awt.Shape>());

    //~ Instance fields --------------------------------------------------------

    // Map for potential attachments
    protected Map<String, java.awt.Shape> attachments;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // BasicDisplay //
    //--------------//
    /**
     * Create a new BasicDisplay object
     *
     * @param glyph our glyph
     */
    public BasicDisplay (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getAttachments //
    //----------------//
    public Map<String, java.awt.Shape> getAttachments ()
    {
        if (attachments != null) {
            return Collections.unmodifiableMap(attachments);
        } else {
            return EMPTY_MAP;
        }
    }

    //----------//
    // getColor //
    //----------//
    public Color getColor ()
    {
        if (glyph.getShape() == null) {
            return Shape.missedColor;
        } else {
            return glyph.getShape()
                        .getColor();
        }
    }

    //----------//
    // getImage //
    //----------//
    public BufferedImage getImage ()
    {
        // Determine the bounding box
        Rectangle     box = glyph.getContourBox();
        BufferedImage image = new BufferedImage(
            box.width,
            box.height,
            BufferedImage.TYPE_BYTE_GRAY);

        for (Section section : glyph.getMembers()) {
            section.fillImage(image, box);
        }

        return image;
    }

    //---------------//
    // addAttachment //
    //---------------//
    public void addAttachment (String         id,
                               java.awt.Shape attachment)
    {
        if (attachments == null) {
            attachments = new HashMap<String, java.awt.Shape>();
        }

        attachments.put(id, attachment);
    }

    //----------//
    // colorize //
    //----------//
    public void colorize (int   viewIndex,
                          Color color)
    {
        for (GlyphSection section : glyph.getMembers()) {
            SectionView view = (SectionView) section.getView(viewIndex);
            view.setColor(color);
        }
    }

    //----------//
    // colorize //
    //----------//
    public void colorize (Lag   lag,
                          int   viewIndex,
                          Color color)
    {
        if (lag == glyph.getLag()) {
            colorize(viewIndex, glyph.getMembers(), color);
        }
    }

    //----------//
    // colorize //
    //----------//
    public void colorize (int                      viewIndex,
                          Collection<GlyphSection> sections,
                          Color                    color)
    {
        for (GlyphSection section : sections) {
            SectionView view = (SectionView) section.getView(viewIndex);
            view.setColor(color);
        }
    }

    //-----------//
    // drawAscii //
    //-----------//
    public void drawAscii ()
    {
        System.out.println(glyph.toString());
        
        // Determine the bounding box
        Rectangle box = glyph.getContourBox();

        if (box == null) {
            return; // Safer
        }

        // Allocate the drawing table
        char[][] table = Section.allocateTable(box);

        // Register each glyph & section in turn
        fill(table, box);

        // Draw the result
        Section.drawTable(table, box);
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        if (attachments != null) {
            System.out.println("   attachments=" + getAttachments());
        }
    }

    //------------//
    // recolorize //
    //------------//
    public void recolorize (int viewIndex)
    {
        for (GlyphSection section : glyph.getMembers()) {
            SectionView view = (SectionView) section.getView(viewIndex);
            view.resetColor();
        }
    }

    //------//
    // fill //
    //------//
    private void fill (char[][]  table,
                       Rectangle box)
    {
        for (Section section : glyph.getMembers()) {
            section.fillTable(table, box);
        }
    }
}
