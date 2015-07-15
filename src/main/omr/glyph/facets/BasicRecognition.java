//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                B a s i c R e c o g n i t i o n                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.Shape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code BasicRecognition} is the basic implementation of a recognition facet.
 *
 * @author Hervé Bitteur
 */
class BasicRecognition
        extends BasicFacet
        implements GlyphRecognition
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BasicRecognition.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Current shape, if any */
    private Shape shape;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicRecognition object.
     *
     * @param glyph our glyph
     */
    public BasicRecognition (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        if (shape != null) {
            sb.append(String.format("   %s%n", shape));
        }

        Shape physical = (getShape() != null) ? getShape().getPhysicalShape() : null;

        if (physical != null) {
            sb.append(String.format("   physical=%s%n", physical));
        }

        return sb.toString();
    }

    //----------//
    // getShape //
    //----------//
    @Override
    public Shape getShape ()
    {
        return shape;
    }

    //----------//
    // setShape //
    //----------//
    @Override
    public void setShape (Shape shape)
    {
        this.shape = shape;

        if (glyph.isVip()) {
            logger.info("VIP {} assigned {}", glyph.idString(), shape);
        }
    }
}
