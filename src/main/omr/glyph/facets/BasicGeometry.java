//----------------------------------------------------------------------------//
//                                                                            //
//                         B a s i c G e o m e t r y                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur 2000-2012. All rights reserved.                 //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.GlyphSignature;
import omr.glyph.Shape;

import omr.lag.Section;

import omr.log.Logger;

import omr.math.Circle;
import omr.math.PointsCollector;

import omr.moments.ARTMoments;
import omr.moments.BasicARTExtractor;
import omr.moments.BasicARTMoments;
import omr.moments.GeometricMoments;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.ui.symbol.ShapeSymbol;

import java.awt.Rectangle;
import java.util.logging.Level;

/**
 * Class {@code BasicGeometry} is the basic implementation of the
 * geometry facet.
 *
 * @author Hervé Bitteur
 */
class BasicGeometry
        extends BasicFacet
        implements GlyphGeometry
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BasicGeometry.class);

    //~ Instance fields --------------------------------------------------------
    /** Interline of the containing staff (or sheet) */
    private final int interline;

    /** Total weight of this glyph */
    private Integer weight;

    /** Computed ART Moments of this glyph */
    private ARTMoments artMoments;

    /** Computed geometric Moments of this glyph */
    private GeometricMoments geometricMoments;

    /** Mass center coordinates */
    private PixelPoint centroid;

    /** Box center coordinates */
    private PixelPoint center;

    /** Absolute contour box */
    private Rectangle bounds;

    /** Current signature */
    private GlyphSignature signature;

    /** Signature used for registration */
    private GlyphSignature registeredSignature;

    /** Approximating circle, if any */
    private Circle circle;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // BasicGeometry //
    //---------------//
    /**
     * Create a new BasicGeometry object.
     *
     * @param glyph     our glyph
     * @param interline the interline scaling value
     */
    public BasicGeometry (Glyph glyph,
                          int interline)
    {
        super(glyph);
        this.interline = interline;
    }

    //~ Methods ----------------------------------------------------------------
    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        System.out.println("   bounds=" + getBounds());
        System.out.println("   centroid=" + getCentroid());
        System.out.println("   interline=" + getInterline());
        System.out.println("   location=" + getLocation());
        System.out.println("   geoMoments=" + getGeometricMoments());
        System.out.println("   artMoments=" + getARTMoments());
        System.out.println("   weight=" + getWeight());

        if (circle != null) {
            System.out.println("   circle=" + circle);
        }
    }

    //---------------//
    // getARTMoments //
    //---------------//
    @Override
    public ARTMoments getARTMoments ()
    {
        if (artMoments == null) {
            computeARTMoments();
        }

        return artMoments;
    }

    //---------------//
    // getAreaCenter //
    //---------------//
    @Override
    public PixelPoint getAreaCenter ()
    {
        if (center == null) {
            PixelRectangle box = glyph.getBounds();
            center = new PixelPoint(
                    box.x + (box.width / 2),
                    box.y + (box.height / 2));
        }

        return center;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public PixelRectangle getBounds ()
    {
        if (bounds == null) {
            PixelRectangle box = null;

            for (Section section : glyph.getMembers()) {
                if (box == null) {
                    box = section.getBounds();
                } else {
                    box.add(section.getBounds());
                }
            }

            bounds = box;
        }

        if (bounds != null) {
            return new PixelRectangle(bounds); // Return a COPY
        } else {
            return null;
        }
    }

    //-------------//
    // getCentroid //
    //-------------//
    @Override
    public PixelPoint getCentroid ()
    {
        if (centroid == null) {
            centroid = getGeometricMoments().getCentroid();
        }

        return centroid;
    }

    //-----------//
    // getCircle //
    //-----------//
    @Override
    public Circle getCircle ()
    {
        return circle;
    }

    //------------//
    // getDensity //
    //------------//
    @Override
    public double getDensity ()
    {
        Rectangle rect = getBounds();
        int surface = (rect.width + 1) * (rect.height + 1);

        return (double) getWeight() / (double) surface;
    }

    //---------------------//
    // getGeometricMoments //
    //---------------------//
    @Override
    public GeometricMoments getGeometricMoments ()
    {
        if (geometricMoments == null) {
            computeGeometricMoments();
        }

        return geometricMoments;
    }

    //--------------//
    // getInterline //
    //--------------//
    @Override
    public int getInterline ()
    {
        return interline;
    }

    //-------------//
    // getLocation //
    //-------------//
    @Override
    public PixelPoint getLocation ()
    {
        Shape shape = glyph.getShape();

        // No shape: use area center
        if (shape == null) {
            return getAreaCenter();
        }

        // Text shape: use specific reference
        if (shape.isText()) {
            PixelPoint loc = glyph.getTextLocation();
            if (loc != null) {
                return loc;
            }
        }

        // Other shape: check with the related symbol if any
        ShapeSymbol symbol = shape.getSymbol();

        if (symbol != null) {
            return symbol.getRefPoint(getBounds());
        }

        // Default: use area center
        return getAreaCenter();
    }

    //---------------------//
    // getNormalizedHeight //
    //---------------------//
    @Override
    public double getNormalizedHeight ()
    {
        return getGeometricMoments().getHeight();
    }

    //---------------------//
    // getNormalizedWeight //
    //---------------------//
    @Override
    public double getNormalizedWeight ()
    {
        return getGeometricMoments().getWeight();
    }

    //--------------------//
    // getNormalizedWidth //
    //--------------------//
    @Override
    public double getNormalizedWidth ()
    {
        return getGeometricMoments().getWidth();
    }

    //--------------------//
    // getPointsCollector //
    //--------------------//
    @Override
    public PointsCollector getPointsCollector ()
    {
        // Cumulate point from member sections
        PointsCollector collector = new PointsCollector(null, getWeight());

        // Append all points, whatever section orientation
        for (Section section : glyph.getMembers()) {
            section.cumulate(collector);
        }

        return collector;
    }

    //------------------------//
    // getRegisteredSignature //
    //------------------------//
    @Override
    public GlyphSignature getRegisteredSignature ()
    {
        return registeredSignature;
    }

    //--------------//
    // getSignature //
    //--------------//
    @Override
    public GlyphSignature getSignature ()
    {
        if (signature == null) {
            signature = new GlyphSignature(glyph);
        }

        return signature;
    }

    //-----------//
    // getWeight //
    //-----------//
    @Override
    public int getWeight ()
    {
        if (weight == null) {
            weight = 0;

            for (Section section : glyph.getMembers()) {
                weight += section.getWeight();
            }
        }

        return weight;
    }

    //------------//
    // intersects //
    //------------//
    @Override
    public boolean intersects (PixelRectangle rectangle)
    {
        // First make a rough test
        if (rectangle.intersects(glyph.getBounds())) {
            // Then make sure at least one section intersects the rectangle
            for (Section section : glyph.getMembers()) {
                if (rectangle.intersects(section.getBounds())) {
                    return true;
                }
            }
        }

        return false;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        signature = null;
        center = null;
        centroid = null;
        bounds = null;
        artMoments = null;
        geometricMoments = null;
        weight = null;
        circle = null;
    }

    //-----------//
    // setCircle //
    //-----------//
    @Override
    public void setCircle (Circle circle)
    {
        this.circle = circle;
    }

    //---------------//
    // setContourBox //
    //---------------//
    @Override
    public void setContourBox (PixelRectangle contourBox)
    {
        this.bounds = contourBox;
    }

    //------------------------//
    // setRegisteredSignature //
    //------------------------//
    @Override
    public void setRegisteredSignature (GlyphSignature sig)
    {
        registeredSignature = sig;
    }

    //-----------//
    // translate //
    //-----------//
    @Override
    public void translate (PixelPoint vector)
    {
        for (Section section : glyph.getMembers()) {
            section.translate(vector);
        }

        glyph.invalidateCache();
    }

    //-------------------//
    // computeARTMoments //
    //-------------------//
    private void computeARTMoments ()
    {
        // Retrieve glyph foreground points
        PointsCollector collector = glyph.getPointsCollector();

        // Then compute the ART moments with this collector
        artMoments = new BasicARTMoments();

        BasicARTExtractor extractor = new BasicARTExtractor();
        extractor.setDescriptor(artMoments);
        extractor.extract(
                collector.getXValues(),
                collector.getYValues(),
                collector.getSize());
    }

    //-------------------------//
    // computeGeometricMoments //
    //-------------------------//
    private void computeGeometricMoments ()
    {
        // Retrieve glyph foreground points
        PointsCollector collector = glyph.getPointsCollector();

        // Then compute the geometric moments with this collector
        try {
            geometricMoments = new GeometricMoments(
                    collector.getXValues(),
                    collector.getYValues(),
                    collector.getSize(),
                    getInterline());
        } catch (Exception ex) {
            logger.warning(
                    "Glyph #{0} Cannot compute moments with unit set to 0",
                    glyph.getId());
        }
    }
}
