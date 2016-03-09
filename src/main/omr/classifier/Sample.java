//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S a m p l e                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.classifier;

import omr.glyph.BasicGlyph;
import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.run.RunTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

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

    /** For comparing Sample instances by shape. */
    public static final Comparator<Sample> byShape = new Comparator<Sample>()
    {
        @Override
        public int compare (Sample s1,
                            Sample s2)
        {
            return Integer.compare(s1.getShape().ordinal(), s2.getShape().ordinal());
        }
    };

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
    protected final int interline;

    private boolean symbol;

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
                   int id,
                   Shape shape)
    {
        super(left, top, runTable);
        this.id = id;
        this.shape = shape;
        this.interline = interline;
    }

    /**
     * Creates a new {@code Sample} object from a glyph.
     *
     * @param glyph     the originating glyph
     * @param interline sheet interline
     * @param shape     assigned shape
     */
    public Sample (Glyph glyph,
                   int interline,
                   Shape shape)
    {
        this(glyph.getLeft(), glyph.getTop(), glyph.getRunTable(), interline, glyph.getId(), shape);
    }

    /**
     * No-arg constructor needed for JAXB unmarshalling.
     */
    public Sample ()
    {
        this(0, 0, null, 0, 0, null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    public int getInterline ()
    {
        return interline;
    }

    public Shape getShape ()
    {
        return shape;
    }

    /**
     * @return the symbol
     */
    public boolean isSymbol ()
    {
        return symbol;
    }

    /**
     * @param symbol the symbol to set
     */
    public void setSymbol (boolean symbol)
    {
        this.symbol = symbol;
    }

    //--------------------//
    // getRecordableShape //
    //--------------------//
    /**
     * Report the shape to record for the provided shape.
     * <p>
     * For example shapes WHOLE_REST and HALF_REST differ only by their pitch position, so they both
     * end up to HW_REST_set physical shape.
     *
     * @param shape the provided shape
     * @return the recordable shape to use, or null
     */
    public static Shape getRecordableShape (Shape shape)
    {
        if (shape == null) {
            return null;
        }

        Shape physicalShape = shape.getPhysicalShape();

        if (physicalShape.isTrainable() && (physicalShape != Shape.NOISE)) {
            return physicalShape;
        } else {
            return null;
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if ((groups != null) && !groups.isEmpty()) {
            sb.append(' ').append(groups);
        }

        if (getShape() != null) {
            sb.append(" ").append(getShape());

            if (getShape().getPhysicalShape() != getShape()) {
                sb.append(" physical=").append(getShape().getPhysicalShape());
            }
        }

        return sb.toString();
    }
}
