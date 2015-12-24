//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S a m p l e                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

import omr.glyph.BasicGlyph;
import omr.glyph.Shape;

import omr.run.RunTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Sample} represents a sample of a shape with the related glyph.
 * Such Sample instances are used to train classifiers.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "sample")
public class Sample
        extends BasicGlyph
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            Sample.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Assigned shape. */
    @XmlAttribute(name = "shape")
    protected final Shape shape;

    /** Scaling information. */
    @XmlAttribute(name = "interline")
    protected int interline;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ShapeSample} object.
     *
     * @param left      sheet-based abscissa of top-left corner
     * @param top       sheet-based ordinate of top-left corner
     * @param runTable  runs of pixels
     * @param interline scaling information
     * @param id        sample id
     * @param shape     assigned shape
     */
    public Sample (int left,
                   int top,
                   RunTable runTable,
                   int interline,
                   String id,
                   Shape shape)
    {
        super(left, top, runTable);
        this.id = id;
        this.shape = shape;
        this.interline = interline;
    }

    /**
     * No-arg constructor needed for JAXB unmarshalling.
     */
    public Sample ()
    {
        this(0, 0, null, 0, null, null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    public Shape getShape ()
    {
        return shape;
    }

    public int getInterline ()
    {
        return interline;
    }
}
