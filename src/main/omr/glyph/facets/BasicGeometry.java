//----------------------------------------------------------------------------//
//                                                                            //
//                         B a s i c G e o m e t r y                          //
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
import omr.glyph.GlyphSignature;
import omr.glyph.Shape;

import omr.lag.Section;

import omr.log.Logger;

import omr.math.Moments;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.ui.icon.SymbolIcon;

import java.awt.Point;
import java.awt.Rectangle;

/**
 * Class {@code BasicGeometry} is the basic implementation of the geometry
 * facet
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

    /**
     * Bounding rectangle, defined as union of all member section bounds so
     * this implies that it has the same orientation as the sections
     */
    private Rectangle bounds;

    /** Computed moments of this glyph */
    private Moments moments;

    /**  Mass center coordinates */
    private PixelPoint centroid;

    /**  Box center coordinates */
    private PixelPoint center;

    /**
     * Display box (always properly oriented), so that rectangle width is
     * aligned with display horizontal and rectangle height with display
     * vertical
     */
    private Rectangle contourBox;

    /** A signature to retrieve this glyph */
    private GlyphSignature signature;

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
            PixelRectangle box = getContourBox();
            center = new PixelPoint(
                box.x + (box.width / 2),
                box.y + (box.height / 2));
        }

        return center;
    }

    //-----------//
    // getBounds //
    //-----------//
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            for (Section section : glyph.getMembers()) {
                if (bounds == null) {
                    bounds = new Rectangle(section.getBounds());
                } else {
                    bounds.add(section.getBounds());
                }
            }
        }

        return bounds;
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

    //---------------//
    // getContourBox //
    //---------------//
    public PixelRectangle getContourBox ()
    {
        if (contourBox == null) {
            Rectangle box = null;

            for (Section section : glyph.getMembers()) {
                if (box == null) {
                    box = new Rectangle(section.getContourBox());
                } else {
                    box.add(section.getContourBox());
                }
            }

            contourBox = box;
        }

        if (contourBox != null) {
            return new PixelRectangle(contourBox);
        } else {
            return null;
        }
    }

    //------------//
    // getDensity //
    //------------//
    public double getDensity ()
    {
        Rectangle rect = getBounds();
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

        // Other shape: check with the related icon if any
        SymbolIcon icon = (SymbolIcon) shape.getIcon();

        if (icon != null) {
            Point iconRefPoint = icon.getRefPoint();

            if (iconRefPoint != null) {
                double         refRatio = (double) iconRefPoint.y / icon.getIconHeight();
                PixelRectangle box = getContourBox();

                return new PixelPoint(
                    getAreaCenter().x,
                    (int) Math.rint(box.y + (box.height * refRatio)));
            }
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

            for (GlyphSection section : glyph.getMembers()) {
                weight += section.getWeight();
            }
        }

        return weight;
    }

    //----------------//
    // computeMoments //
    //----------------//
    /**
     * Compute all the moments for this glyph
     */
    public void computeMoments ()
    {
        // First cumulate point from member sections
        weight = getWeight();

        int[] coord = new int[weight];
        int[] pos = new int[weight];

        // Append recursively all points
        cumulatePoints(coord, pos, 0);

        // Then compute the moments, swapping pos & coord since the lag is
        // vertical
        try {
            moments = new Moments(pos, coord, weight, getInterline());
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
        System.out.println("   contourBox=" + getContourBox());
        System.out.println("   weight=" + getWeight());
        System.out.println("   signature=" + getSignature());
        System.out.println("   interline=" + getInterline());
        System.out.println("   moments=" + getMoments());
        System.out.println("   location=" + getLocation());
        System.out.println("   centroid=" + getCentroid());
        System.out.println("   bounds=" + getBounds());
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        centroid = null;
        moments = null;
        bounds = null;
        contourBox = null;
        weight = null;
        signature = null;
    }

    //-------//
    // shift //
    //-------//
    public void shift (PixelPoint vector)
    {
        // Compute current value for all the dependent items
        getBounds();
        getContourBox();
        getAreaCenter();
        getMoments();
        getCentroid();

        // Translate all the dependent items
        bounds.translate(vector.x, vector.y);
        contourBox.translate(vector.x, vector.y);
        center.translate(vector);
        centroid.translate(vector);

        moments.getValues()[17] += vector.x; // Beurk
        moments.getValues()[18] += vector.y; // Beurk

        signature = new GlyphSignature(glyph);
    }

    //----------------//
    // cumulatePoints //
    //----------------//
    private int cumulatePoints (int[] coord,
                                int[] pos,
                                int   nb)
    {
        for (Section section : glyph.getMembers()) {
            nb = section.cumulatePoints(coord, pos, nb);
        }

        return nb;
    }
}
