//----------------------------------------------------------------------------//
//                                                                            //
//                          B a s i c D i s p l a y                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.ui.AttachmentHolder;
import omr.glyph.ui.BasicAttachmentHolder;

import omr.lag.BasicSection;
import omr.lag.Section;

import omr.ui.Colors;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Class {@code BasicDisplay} is the basic implementation of a display
 * facet.
 *
 * @author Hervé Bitteur
 */
class BasicDisplay
    extends BasicFacet
    implements GlyphDisplay
{
    //~ Instance fields --------------------------------------------------------

    /** Potential attachments, lazily allocated */
    protected AttachmentHolder attachments;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // BasicDisplay //
    //--------------//
    /**
     * Create a new BasicDisplay object.
     * @param glyph our glyph
     */
    public BasicDisplay (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // addAttachment //
    //---------------//
    @Override
    public void addAttachment (String         id,
                               java.awt.Shape attachment)
    {
        if (attachment != null) {
            if (attachments == null) {
                attachments = new BasicAttachmentHolder();
            }

            attachments.addAttachment(id, attachment);
        }
    }

    //----------//
    // colorize //
    //----------//
    @Override
    public void colorize (Color color)
    {
        colorize(glyph.getMembers(), color);
    }

    //----------//
    // colorize //
    //----------//
    @Override
    public void colorize (Collection<Section> sections,
                          Color               color)
    {
        for (Section section : sections) {
            section.setColor(color);
        }
    }

    //-----------//
    // drawAscii //
    //-----------//
    @Override
    public void drawAscii ()
    {
        System.out.println(glyph.toString());

        // Determine the bounding box
        Rectangle box = glyph.getContourBox();

        if (box == null) {
            return; // Safer
        }

        // Allocate the drawing table
        char[][] table = BasicSection.allocateTable(box);

        // Register each section in turn
        for (Section section : glyph.getMembers()) {
            section.fillTable(table, box);
        }

        // Draw the result
        BasicSection.drawTable(table, box);
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

    //----------------//
    // getAttachments //
    //----------------//
    @Override
    public Map<String, java.awt.Shape> getAttachments ()
    {
        if (attachments != null) {
            return attachments.getAttachments();
        } else {
            return Collections.emptyMap();
        }
    }

    //----------//
    // getColor //
    //----------//
    @Override
    public Color getColor ()
    {
        if (glyph.getShape() == null) {
            return Colors.SHAPE_UNKNOWN;
        } else {
            return glyph.getShape()
                        .getColor();
        }
    }

    //----------//
    // getImage //
    //----------//
    @Override
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

    //------------//
    // recolorize //
    //------------//
    @Override
    public void recolorize ()
    {
        for (Section section : glyph.getMembers()) {
            section.resetColor();
        }
    }

    //-------------------//
    // renderAttachments //
    //-------------------//
    @Override
    public void renderAttachments (Graphics2D g)
    {
        if (attachments != null) {
            attachments.renderAttachments(g);
        }
    }
}
