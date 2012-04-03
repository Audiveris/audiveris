//----------------------------------------------------------------------------//
//                                                                            //
//                         B a s i c G e o m e t r y                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
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

    /** Absolute display box */
    private Rectangle contourBox;

    /** A signature to retrieve this glyph */
    private GlyphSignature signature;

    /** Approximating circle, if any */
    private Circle circle;

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // BasicGeometry //
    //---------------//
    /**
     * Create a new BasicGeometry object.
     * @param glyph     our glyph
     * @param interline the interline scaling value
     */
    public BasicGeometry (Glyph glyph,
                          int   interline)
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
        System.out.println("   centroid=" + getCentroid());
        System.out.println("   contourBox=" + getContourBox());
        System.out.println("   interline=" + getInterline());
        System.out.println("   location=" + getLocation());
        System.out.println("   geoMoments=" + getGeometricMoments());
        System.out.println("   artMoments=" + getARTMoments());
        System.out.println("   signature=" + getSignature());
        System.out.println("   weight=" + getWeight());
        System.out.println("   circle=" + circle);
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
            PixelRectangle box = glyph.getContourBox();
            center = new PixelPoint(
                box.x + (box.width / 2),
                box.y + (box.height / 2));
        }

        return center;
    }

    //-------------//
    // getCentroid //
    //-------------//
    @Override
    public PixelPoint getCentroid ()
    {
        if (centroid == null) {
            centroid = getGeometricMoments()
                           .getCentroid();
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

    //---------------//
    // getContourBox //
    //---------------//
    @Override
    public PixelRectangle getContourBox ()
    {
        if (contourBox == null) {
            PixelRectangle box = null;

            for (Section section : glyph.getMembers()) {
                if (box == null) {
                    box = new PixelRectangle(section.getContourBox());
                } else {
                    box.add(section.getContourBox());
                }
            }

            contourBox = box;
        }

        if (contourBox != null) {
            return new PixelRectangle(contourBox); // Return a COPY
        } else {
            return null;
        }
    }

    //------------//
    // getDensity //
    //------------//
    @Override
    public double getDensity ()
    {
        Rectangle rect = getContourBox();
        int       surface = (rect.width + 1) * (rect.height + 1);

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
            return glyph.getTextInfo()
                        .getTextStart();
        }

        // Other shape: check with the related symbol if any
        ShapeSymbol symbol = shape.getSymbol();

        if (symbol != null) {
            return symbol.getRefPoint(getContourBox());
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
        return getGeometricMoments()
                   .getHeight();
    }

    //---------------------//
    // getNormalizedWeight //
    //---------------------//
    @Override
    public double getNormalizedWeight ()
    {
        return getGeometricMoments()
                   .getWeight();
    }

    //--------------------//
    // getNormalizedWidth //
    //--------------------//
    @Override
    public double getNormalizedWidth ()
    {
        return getGeometricMoments()
                   .getWidth();
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
        if (rectangle.intersects(glyph.getContourBox())) {
            // Then make sure at least one section intersects the rectangle
            for (Section section : glyph.getMembers()) {
                if (rectangle.intersects(section.getContourBox())) {
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
        center = null;
        centroid = null;
        contourBox = null;
        geometricMoments = null;
        signature = null;
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
        this.contourBox = contourBox;
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
                "Glyph #" + glyph.getId() +
                " Cannot compute moments with unit set to 0");
        }
    }
}
