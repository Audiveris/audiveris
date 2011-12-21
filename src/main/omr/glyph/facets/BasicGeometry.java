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
import omr.math.Moments;
import omr.math.PointsCollector;

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

    /** Computed moments of this glyph */
    private Moments moments;

    /**  Mass center coordinates */
    private PixelPoint centroid;

    /**  Box center coordinates */
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
     * Create a new BasicGeometry object
     *
     * @param glyph our glyph
     * @param interline the interline scaling value
     */
    public BasicGeometry (Glyph glyph,
                          int   interline)
    {
        super(glyph);
        this.interline = interline;
    }

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getAreaCenter //
    //---------------//
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
    public PixelPoint getCentroid ()
    {
        if (centroid == null) {
            centroid = getMoments()
                           .getCentroid();
        }

        return centroid;
    }

    //-----------//
    // setCircle //
    //-----------//
    public void setCircle (Circle circle)
    {
        this.circle = circle;
    }

    //-----------//
    // getCircle //
    //-----------//
    public Circle getCircle ()
    {
        return circle;
    }

    //---------------//
    // setContourBox //
    //---------------//
    public void setContourBox (PixelRectangle contourBox)
    {
        this.contourBox = contourBox;
    }

    //---------------//
    // getContourBox //
    //---------------//
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
    public double getDensity ()
    {
        Rectangle rect = getContourBox();
        int       surface = (rect.width + 1) * (rect.height + 1);

        return (double) getWeight() / (double) surface;
    }

    //--------------//
    // getInterline //
    //--------------//
    public int getInterline ()
    {
        return interline;
    }

    //-------------//
    // getLocation //
    //-------------//
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

    //------------//
    // getMoments //
    //------------//
    public Moments getMoments ()
    {
        if (moments == null) {
            computeMoments();
        }

        return moments;
    }

    //---------------------//
    // getNormalizedHeight //
    //---------------------//
    public double getNormalizedHeight ()
    {
        return getMoments()
                   .getHeight();
    }

    //---------------------//
    // getNormalizedWeight //
    //---------------------//
    public double getNormalizedWeight ()
    {
        return getMoments()
                   .getWeight();
    }

    //--------------------//
    // getNormalizedWidth //
    //--------------------//
    public double getNormalizedWidth ()
    {
        return getMoments()
                   .getWidth();
    }

    //--------------//
    // getSignature //
    //--------------//
    /**
     * Report a signature that should allow to detect glyph identity
     *
     * @return the glyph signature
     */
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

    //----------------//
    // computeMoments //
    //----------------//
    /**
     * Compute all the moments for this glyph, knowing that it can be a mix of
     * vertical sections and horizontal sections.
     */
    public void computeMoments ()
    {
        // First cumulate point from member sections
        weight = getWeight();

        PointsCollector collector = new PointsCollector(null, weight);

        // Append all points, whatever section orientation
        for (Section section : glyph.getMembers()) {
            section.cumulate(collector);
        }

        // Then compute the moments with this collector
        try {
            moments = new Moments(
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
        System.out.println("   moments=" + getMoments());
        System.out.println("   signature=" + getSignature());
        System.out.println("   weight=" + getWeight());
        System.out.println("   circle=" + circle);
    }

    //------------//
    // intersects //
    //------------//
    public boolean intersects (PixelRectangle rectangle)
    {
        // First make a rough test
        if (rectangle.intersects(glyph.getContourBox())) {
            // Make sure at least one section intersects the rectangle
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
        moments = null;
        signature = null;
        weight = null;
        circle = null;
    }

    //-----------//
    // translate //
    //-----------//
    public void translate (PixelPoint vector)
    {
        for (Section section : glyph.getMembers()) {
            section.translate(vector);
        }

        glyph.invalidateCache();
    }
}
