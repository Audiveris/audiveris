//----------------------------------------------------------------------------//
//                                                                            //
//                                 G l y p h                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.check.Checkable;
import omr.check.Result;

import omr.lag.Section;
import omr.lag.SectionView;

import omr.math.Moments;

import omr.sheet.Picture;
import omr.sheet.PixelPoint;
import omr.sheet.PixelRectangle;

import omr.ui.icon.SymbolIcon;
import omr.ui.view.Zoom;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.Predicate;

import java.awt.*;
import java.util.*;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * Class <code>Glyph</code> represents any glyph found, such as stem, ledger,
 * accidental, note head, etc...
 *
 * <p>A Glyph is basically a collection of pixels. It can be split into smaller
 * glyphs, which may later be re-assembled into another instance of glyph. There
 * is a means, based on a simple signature (weight and bounding box), to link
 * the second instance to the first (original) one. Then any getter or setter
 * does transparently access the original glyph.
 *
 * <p>The Glyph class implements Comparable so that collections of glyphs
 * (generally at system level) are sorted first by abscissa then by ordinate.
 *
 * <p>A Glyph can be stored on disk and reloaded in order to train a glyph
 * evaluator, so this class uses JAXB annotations.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "glyph")
@XmlType(propOrder =  {
    "stemNumber", "withLedger", "pitchPosition", "members"}
)
public class Glyph
    implements Comparable<Glyph>, Checkable, java.io.Serializable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Glyph.class);

    //~ Instance fields --------------------------------------------------------

    /** Instance identifier (Unique in the containing GlyphLag) */
    @XmlAttribute
    protected int id;

    /** The containing glyph lag */
    protected GlyphLag lag;

    /**
     * Sections that compose this glyph. The collection should be kept sorted
     * on centroid abscissa then ordinate
     */
    @XmlElement(name = "section")
    protected List<GlyphSection> members = new ArrayList<GlyphSection>();

    /**
     * Display box (always properly oriented), so that rectangle width is
     * aligned with display horizontal and rectangle height with display
     * vertical
     */
    protected Rectangle contourBox;

    /** Total weight of this glyph */
    protected Integer weight;

    /** Result of analysis wrt this glyph */
    protected Result result;

    /** A signature to retrieve this glyph */
    private GlyphSignature signature;

    /** Original if any */
    protected Glyph original;

    // Below are properties to be retrieved in the glyph original if any

    /** Current recognized shape of this glyph */
    protected Shape shape;

    /** Doubt in the assigned shape */
    protected double doubt = Evaluation.NO_DOUBT;

    /** Interline of the containing staff (or sheet) */
    protected int interline;

    /** Number of stems it is connected to (0, 1, 2) */
    protected int stemNumber;

    /** Stem attached on left if any */
    protected Glyph leftStem;

    /** Stem attached on right if any */
    protected Glyph rightStem;

    /** Position with respect to nearest staff. Key references are : 0 for
       middle line (B), -2 for top line (F) and +2 for bottom line (E)  */
    protected double pitchPosition;

    /** Is there a ledger nearby ? */
    protected boolean withLedger;

    /** Computed moments of this glyph */
    protected Moments moments;

    /**  Centroid coordinates */
    protected PixelPoint centroid;

    /**  Box center coordinates */
    protected PixelPoint center;

    /**
     * Bounding rectangle, defined as union of all member section bounds so
     * this implies that it has the same orientation as the sections
     */
    protected Rectangle bounds;

    /** Set of forbidden shapes, if any */
    protected Set<Shape> forbiddenShapes;

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Glyph //
    //-------//
    /**
     * Constructor needed for Class.newInstance method
     */
    public Glyph ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    public static String toString(Collection<? extends Glyph> glyphs)
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" glyphs[");
        for (Glyph glyph : glyphs) {
            sb.append("#")
              .append(glyph.getId());
        }
        sb.append("]");

        return sb.toString();
    
    }
    //---------------//
    // getAreaCenter //
    //---------------//
    /**
     * Report the glyph area center. The point is lazily evaluated.
     *
     * @return the area center point
     */
    public PixelPoint getAreaCenter ()
    {
        if (original != null) {
            return original.getAreaCenter();
        } else if (center == null) {
            PixelRectangle box = getContourBox();
            center = new PixelPoint(
                box.x + (box.width / 2),
                box.y + (box.height / 2));
        }

        return center;
    }

    //-----------//
    // setBounds //
    //-----------//
    public void setBounds (Rectangle bounds)
    {
        if (original != null) {
            original.setBounds(bounds);
        } else {
            this.bounds = bounds;
        }
    }

    //-----------//
    // getBounds //
    //-----------//
    /**
     * Return the bounding rectangle of the glyph
     *
     * @return the bounds
     */
    public Rectangle getBounds ()
    {
        if (original != null) {
            return original.getBounds();
        } else if (bounds == null) {
            for (Section section : members) {
                if (bounds == null) {
                    bounds = new Rectangle(section.getBounds());
                } else {
                    bounds.add(section.getBounds());
                }
            }
        }

        return bounds;
    }

    //-----------//
    // setCenter //
    //-----------//
    public void setCenter (PixelPoint center)
    {
        if (original != null) {
            original.setCenter(center);
        } else {
            this.center = center;
        }
    }

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the glyph (reference) center, which is the equivalent of the icon
     * reference point if one such point exist, or the glyph area center
     * otherwise. The point is lazily evaluated.
     *
     * @return the reference center point
     */
    public PixelPoint getCenter ()
    {
        if (shape == null) {
            return getAreaCenter();
        }

        SymbolIcon icon = (SymbolIcon) shape.getIcon();
        Point      iconRefPoint = icon.getRefPoint();

        if (iconRefPoint != null) {
            double         refRatio = (double) iconRefPoint.y / icon.getIconHeight();
            PixelRectangle box = getContourBox();

            return new PixelPoint(
                getAreaCenter().x,
                (int) Math.rint(box.y + (box.height * refRatio)));
        } else {
            return getAreaCenter();
        }
    }

    //-------------//
    // setCentroid //
    //-------------//
    public void setCentroid (PixelPoint centroid)
    {
        if (original != null) {
            original.setCentroid(centroid);
        } else {
            this.centroid = centroid;
        }
    }

    //-------------//
    // getCentroid //
    //-------------//
    /**
     * Report the glyph centroid (mass center). The point is lazily evaluated.
     *
     * @return the mass center point
     */
    public PixelPoint getCentroid ()
    {
        if (original != null) {
            return original.getCentroid();
        } else if (centroid == null) {
            centroid = getMoments()
                           .getCentroid();
        }

        return centroid;
    }

    //----------//
    // getColor //
    //----------//
    /**
     * Report the color to be used to colorize the provided glyph, according to
     * the color policy which is based on the glyph shape
     *
     * @return the related shape color of the glyph, or the predefined {@link
     * Shape#missedColor} if the glyph has no related shape
     */
    public Color getColor ()
    {
        if (getShape() == null) {
            return Shape.missedColor;
        } else {
            return getShape()
                       .getColor();
        }
    }

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Return the display bounding box of the display contour. Useful to quickly
     * check if the glyph needs to be repainted.
     *
     * @return the bounding contour rectangle box
     */
    public PixelRectangle getContourBox ()
    {
        if (contourBox == null) {
            for (Section section : members) {
                if (contourBox == null) {
                    contourBox = new Rectangle(section.getContourBox());
                } else {
                    contourBox.add(section.getContourBox());
                }
            }
        }

        if (contourBox != null) {
            return new PixelRectangle(
                contourBox.x,
                contourBox.y,
                contourBox.width,
                contourBox.height);
        } else {
            return null;
        }
    }

    //------------------//
    // getGlyphIndexAtX //
    //------------------//
    /**
     * Retrieve the index of the very first glyph in the provided ordered list,
     * whose left abscissa is equal or greater than the provided x value.
     *
     * @param list the list to search, ordered by increasing abscissa
     * @param x the desired abscissa
     * @return the index of the first suitable glyph, or list.size() if no such
     *         glyph can be found.
     */
    public static int getGlyphIndexAtX (List<?extends Glyph> list,
                                        int                  x)
    {
        int low = 0;
        int high = list.size() - 1;

        while (low <= high) {
            int mid = (low + high) >> 1;
            int gx = list.get(mid)
                         .getContourBox().x;

            if (gx < x) {
                low = mid + 1;
            } else if (gx > x) {
                high = mid - 1;
            } else {
                // We are pointing to a glyph with desired x
                // Let's now pick the very first one
                for (mid = mid - 1; mid >= 0; mid--) {
                    if (list.get(mid)
                            .getContourBox().x < x) {
                        break;
                    }
                }

                return mid + 1;
            }
        }

        return low;
    }

    //----------//
    // getDoubt //
    //----------//
    /**
     * Report the doubt of the glyph shape
     *
     * @return the doubt related to glyph shape
     */
    public double getDoubt ()
    {
        if (original != null) {
            return original.getDoubt();
        } else {
            return doubt;
        }
    }

    //-------//
    // setId //
    //-------//
    /**
     * Assign a unique ID to the glyph
     *
     * @param id the unique id
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the unique glyph id
     *
     * @return the glyph id
     */
    public int getId ()
    {
        return id;
    }

    //--------------//
    // setInterline //
    //--------------//
    /**
     * Setter for the interline value
     *
     * @param interline the mean interline value of containing staff
     */
    public void setInterline (int interline)
    {
        if (original != null) {
            original.setInterline(interline);
        } else {
            this.interline = interline;
        }
    }

    //--------------//
    // getInterline //
    //--------------//
    /**
     * Report the interline value for the glyph containing staff, which is used
     * for some of the moments
     *
     * @return the interline value
     */
    @XmlAttribute
    public int getInterline ()
    {
        if (original != null) {
            return original.getInterline();
        } else {
            return interline;
        }
    }

    //---------//
    // isKnown //
    //---------//
    /**
     * A glyph is considered as known if it has a registered shape other than
     * noise (clutter is considered as known)
     *
     * @return true if so
     */
    public boolean isKnown ()
    {
        Shape shape = getShape();

        return (shape != null) && (shape != Shape.NOISE);
    }

    //--------//
    // setLag //
    //--------//
    /**
     * The setter for glyph lag. Used with care, by {@link GlyphLag} and {@link
     * omr.glyph.ui.GlyphVerifier}
     *
     * @param lag the containing lag
     */
    public void setLag (GlyphLag lag)
    {
        this.lag = lag;
    }

    //--------//
    // getLag //
    //--------//
    /**
     * Report the containing lag
     *
     * @return the containing lag
     */
    public GlyphLag getLag ()
    {
        return lag;
    }

    //-------------//
    // setLeftStem //
    //-------------//
    /**
     * Assign the stem on left
     *
     * @param leftStem stem glyph
     */
    public void setLeftStem (Glyph leftStem)
    {
        if (original != null) {
            original.setLeftStem(leftStem);
        } else {
            this.leftStem = leftStem;
        }
    }

    //-------------//
    // getLeftStem //
    //-------------//
    /**
     * Report the stem attached on left side, if any
     *
     * @return stem on left, or null
     */
    public Glyph getLeftStem ()
    {
        if (original != null) {
            return original.getLeftStem();
        } else {
            return leftStem;
        }
    }

    public Logger getLogger ()
    {
        return logger;
    }

    //---------------//
    // isManualShape //
    //---------------//
    /**
     * Report whether the shape of this glyph has been manually assigned (and
     * thus can only be modified by explicit user action)
     *
     * @return true if shape manually assigned
     */
    public boolean isManualShape ()
    {
        return getDoubt() == Evaluation.MANUAL_NO_DOUBT;
    }

    //------------//
    // getMembers //
    //------------//
    /**
     * Report the collection of member sections.
     *
     * @return member sections
     */
    public List<GlyphSection> getMembers ()
    {
        return members;
    }

    //------------//
    // setMoments //
    //------------//
    public void setMoments (Moments moments)
    {
        if (original != null) {
            original.setMoments(moments);
        } else {
            this.moments = moments;
        }
    }

    //------------//
    // getMoments //
    //------------//
    /**
     * Report the glyph moments, which are lazily computed
     *
     * @return the glyph moments
     */
    public Moments getMoments ()
    {
        if (original != null) {
            return original.getMoments();
        } else if (moments == null) {
            computeMoments();
        }

        return moments;
    }

    //-------------//
    // setOriginal //
    //-------------//
    public void setOriginal (Glyph original)
    {
        this.original = original;
    }

    //------------------//
    // setPitchPosition //
    //------------------//
    /**
     * Setter for the pitch position, with respect to the containing
     * staff
     *
     * @param pitchPosition the pitch position wrt the staff
     */
    public void setPitchPosition (double pitchPosition)
    {
        if (original != null) {
            original.setPitchPosition(pitchPosition);
        } else {
            this.pitchPosition = pitchPosition;
        }
    }

    //------------------//
    // getPitchPosition //
    //------------------//
    /**
     * Report the pitchPosition feature (position relative to the staff)
     *
     * @return the pitchPosition value
     */
    @XmlElement(name = "pitch-position")
    public double getPitchPosition ()
    {
        if (original != null) {
            return original.getPitchPosition();
        } else {
            return pitchPosition;
        }
    }

    //-----------//
    // setResult //
    //-----------//
    /**
     * Record the analysis result in the glyph itself
     *
     * @param result the assigned result
     */
    @Implement(Checkable.class)
    public void setResult (Result result)
    {
        this.result = result;
    }

    //-----------//
    // getResult //
    //-----------//
    /**
     * Report the result found during analysis of this glyph
     *
     * @return the analysis result
     */
    public Result getResult ()
    {
        return result;
    }

    //--------------//
    // setRightStem //
    //--------------//
    /**
     * Assign the stem on right
     *
     * @param rightStem stem glyph
     */
    public void setRightStem (Glyph rightStem)
    {
        if (original != null) {
            original.setRightStem(rightStem);
        } else {
            this.rightStem = rightStem;
        }
    }

    //--------------//
    // getRightStem //
    //--------------//
    /**
     * Report the stem attached on right side, if any
     *
     * @return stem on right, or null
     */
    public Glyph getRightStem ()
    {
        if (original != null) {
            return original.getRightStem();
        } else {
            return rightStem;
        }
    }

    //----------//
    // setShape //
    //----------//
    /**
     * Setter for the glyph shape, assumed to be based on structural data
     *
     * @param shape the assigned shape, which may be null
     */
    public void setShape (Shape shape)
    {
        setShape(shape, Evaluation.NO_DOUBT);
    }

    //----------//
    // setShape //
    //----------//
    /**
     * Setter for the glyph shape, with related grade
     *
     * @param shape the assigned shape
     * @param doubt the related doubt
     */
    public void setShape (Shape  shape,
                          double doubt)
    {
        if (original != null) {
            original.setShape(shape, doubt);
        } else {
            // Blacklist the previous shape if any
            if (getShape() != null) {
                forbidShape(getShape());
            }

            // Now remove the assigned shape from the blacklist if any
            allowShape(shape);

            this.shape = shape;
            this.doubt = doubt;
        }
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the registered glyph shape
     *
     * @return the glyph shape, which may be null
     */
    public Shape getShape ()
    {
        if (original != null) {
            return original.getShape();
        } else {
            return shape;
        }
    }

    //------------------//
    // isShapeForbidden //
    //------------------//
    public boolean isShapeForbidden (Shape shape)
    {
        return (getForbiddenShapes() != null) &&
               getForbiddenShapes()
                   .contains(shape);
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
            signature = new GlyphSignature(this);
        }

        return signature;
    }

    //--------//
    // isStem //
    //--------//
    /**
     * Convenient method which tests if the glyph is a Stem
     *
     * @return true if glyph shape is a Stem
     */
    public boolean isStem ()
    {
        return getShape() == Shape.COMBINING_STEM;
    }

    //---------------//
    // setStemNumber //
    //---------------//
    /**
     * Remember the number of stems near by
     *
     * @param stemNumber the number of stems
     */
    public void setStemNumber (int stemNumber)
    {
        if (original != null) {
            original.setStemNumber(stemNumber);
        } else {
            this.stemNumber = stemNumber;
        }
    }

    //---------------//
    // getStemNumber //
    //---------------//
    /**
     * Report the number of stems the glyph is close to
     *
     * @return the number of stems near by, typically 0, 1 or 2.
     */
    @XmlElement(name = "stem-number")
    public int getStemNumber ()
    {
        if (original != null) {
            return original.getStemNumber();
        } else {
            return stemNumber;
        }
    }

    //-----------------//
    // getSymbolsAfter //
    //-----------------//
    /**
     * Return the known glyphs stuck on last side of the stick (this is relevant
     * mainly for a stem glyph)
     *
     * @param predicate the predicate to apply on each glyph
     * @param goods the set of correct glyphs (perhaps empty)
     * @param bads the set of non-correct glyphs (perhaps empty)
     */
    public void getSymbolsAfter (Predicate<Glyph> predicate,
                                 Set<Glyph>       goods,
                                 Set<Glyph>       bads)
    {
        for (GlyphSection section : members) {
            for (GlyphSection sct : section.getTargets()) {
                if (sct.isGlyphMember()) {
                    Glyph glyph = sct.getGlyph();

                    if (glyph != this) {
                        if (predicate.check(glyph)) {
                            goods.add(glyph);
                        } else {
                            bads.add(glyph);
                        }
                    }
                }
            }
        }
    }

    //------------------//
    // getSymbolsBefore //
    //------------------//
    /**
     * Return the known glyphs stuck on first side of the stick (this is
     * relevant mainly for a stem glyph)
     *
     * @param predicate the predicate to apply on each glyph
     * @param goods the set of correct glyphs (perhaps empty)
     * @param bads the set of non-correct glyphs (perhaps empty)
     */
    public void getSymbolsBefore (Predicate<Glyph> predicate,
                                  Set<Glyph>       goods,
                                  Set<Glyph>       bads)
    {
        for (GlyphSection section : members) {
            for (GlyphSection sct : section.getSources()) {
                if (sct.isGlyphMember()) {
                    Glyph glyph = sct.getGlyph();

                    if (glyph != this) {
                        if (predicate.check(glyph)) {
                            goods.add(glyph);
                        } else {
                            bads.add(glyph);
                        }
                    }
                }
            }
        }
    }

    //------------------//
    // setTrainingShape //
    //------------------//
    public void setTrainingShape (Shape shape)
    {
        setShape(shape);
    }

    //------------------//
    // getTrainingShape //
    //------------------//
    /**
     * Report the training shape of the registered glyph shape
     *
     * @return the glyph shape, which may be null
     */
    @XmlAttribute(name = "shape")
    public Shape getTrainingShape ()
    {
        if (getShape() != null) {
            return getShape()
                       .getTrainingShape();
        } else {
            return null;
        }
    }

    //-----------//
    // getWeight //
    //-----------//
    /**
     * Report the total weight of this glyph, as the sum of section weights
     *
     * @return the total weight (number of pixels)
     */
    public int getWeight ()
    {
        if (weight == null) {
            weight = 0;

            for (GlyphSection section : members) {
                weight += section.getWeight();
            }
        }

        return weight;
    }

    //-------------//
    // isWellKnown //
    //-------------//
    /**
     * A glyph is considered as well known if it has a registered shape other
     * than noise and stucture
     *
     * @return true if so
     */
    public boolean isWellKnown ()
    {
        Shape shape = getShape();

        return (shape != null) && shape.isWellKnown();
    }

    //---------------//
    // setWithLedger //
    //---------------//
    /**
     * Remember info about ledger nearby
     *
     * @param withLedger true is there is such ledger
     */
    @XmlElement(name = "with-ledger")
    public void setWithLedger (boolean withLedger)
    {
        if (original != null) {
            original.setWithLedger(withLedger);
        } else {
            this.withLedger = withLedger;
        }
    }

    //--------------//
    // isWithLedger //
    //--------------//
    /**
     * Report whether the glyph touches a ledger
     *
     * @return true if there is a close ledger
     */
    public boolean isWithLedger ()
    {
        if (original != null) {
            return original.isWithLedger();
        } else {
            return withLedger;
        }
    }

    //------------------//
    // addGlyphSections //
    //------------------//
    /**
     * Add another glyph (with its sections of points) to this one
     *
     * @param other The merged glyph
     * @param linkSections Should we set the link from sections to glyph ?
     */
    public void addGlyphSections (Glyph   other,
                                  boolean linkSections)
    {
        // Update glyph info in other sections
        for (GlyphSection section : other.members) {
            addSection(section, linkSections);
        }

        invalidateCache();
    }

    //------------//
    // addSection //
    //------------//
    /**
     * Add a section as a member of this glyph.
     *
     * @param section The section to be included
     * @param link Should we set the link from section to glyph ?
     */
    public void addSection (GlyphSection section,
                            boolean      link)
    {
        if (link) {
            section.setGlyph(this);
        }

        members.add(section);

        invalidateCache();
    }

    //------------//
    // allowShape //
    //------------//
    public void allowShape (Shape shape)
    {
        Set<Shape> forbiddens = getForbiddenShapes();

        if (forbiddens != null) {
            forbiddens.remove(shape);
        }
    }

    //----------//
    // colorize //
    //----------//
    /**
     * Set the display color of all sections that compose this glyph.
     *
     * @param viewIndex index in the view list
     * @param color     color for the whole glyph
     */
    public void colorize (int   viewIndex,
                          Color color)
    {
        for (GlyphSection section : members) {
            SectionView view = (SectionView) section.getViews()
                                                    .get(viewIndex);
            view.setColor(color);
        }
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Needed to implement Comparable, sorting glyphs first by abscissa, then by
     * ordinate.
     *
     * @param other the other glyph to compare to
     * @return th result of ordering
     */
    @Implement(Comparable.class)
    public int compareTo (Glyph other)
    {
        // Are x values different?
        int dx = getCentroid().x - other.getCentroid().x;

        if (dx != 0) {
            return dx;
        }

        // Glyphs are vertically aligned, so use ordinates
        return getCentroid().y - other.getCentroid().y;
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
        int   weight = getWeight();

        int[] coord = new int[weight];
        int[] pos = new int[weight];

        // Append recursively all points
        cumulatePoints(coord, pos, 0);

        // Then compute the moments, swapping pos & coord since the lag is
        // vertical
        setMoments(new Moments(pos, coord, weight, getInterline()));
    }

    //---------//
    // destroy //
    //---------//
    /**
     * Delete the glyph from its containing lag
     *
     * @param cutSections if true, the glyph links in the member sections
     * are also nullified
     */
    public void destroy (boolean cutSections)
    {
        // Cut the link between this glyph and its member sections
        if (cutSections) {
            for (GlyphSection section : members) {
                section.setGlyph(null);
            }
        }

        // We do not destroy the sections, just the glyph which must be
        // removed from its containing lag.
        if (lag != null) {
            lag.removeGlyph(this);
        }
    }

    //-----------//
    // drawAscii //
    //-----------//
    /**
     * Draw a basic representation of the glyph, using ascii characters
     */
    public void drawAscii ()
    {
        // Determine the bounding box
        Rectangle box = getContourBox();

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
    /**
     * Print out glyph internal data
     */
    public void dump ()
    {
        System.out.println(getClass().getName());
        System.out.println("   id=" + getId());
        System.out.println("   original=" + original);
        System.out.println("   lag=" + getLag());
        System.out.println("   members=" + getMembers());
        System.out.println("   contourBox=" + getContourBox());
        System.out.println("   weight=" + getWeight());
        System.out.println("   result=" + getResult());
        System.out.println("   signature=" + getSignature());
        System.out.println("   shape=" + getShape());
        System.out.println("   doubt=" + getDoubt());
        System.out.println("   training=" + getTrainingShape());
        System.out.println("   interline=" + getInterline());
        System.out.println("   stemNumber=" + getStemNumber());
        System.out.println("   leftStem=" + getLeftStem());
        System.out.println("   rightStem=" + getRightStem());
        System.out.println("   pitchPosition=" + getPitchPosition());
        System.out.println("   withLedger=" + isWithLedger());
        System.out.println("   moments=" + getMoments());
        System.out.println("   center=" + getCenter());
        System.out.println("   centroid=" + getCentroid());
        System.out.println("   bounds=" + getBounds());
        System.out.println("   forbiddenShapes=" + getForbiddenShapes());
    }

    //-------------//
    // erasePixels //
    //-------------//
    /**
     * In the provided Picture, remove the pixels that correspond to the member
     * sections
     *
     * @param picture the provided picture
     */
    public void erasePixels (Picture picture)
    {
        for (GlyphSection section : members) {
            section.write(picture, Picture.BACKGROUND);
        }
    }

    //------------//
    // recolorize //
    //------------//
    /**
     * Reset the display color of all sections that compose this glyph.
     *
     * @param viewIndex index in the view list
     */
    public void recolorize (int viewIndex)
    {
        for (GlyphSection section : members) {
            SectionView view = (SectionView) section.getViews()
                                                    .get(viewIndex);
            view.resetColor();
        }
    }

    //---------------//
    // removeSection //
    //---------------//
    /**
     * Remove a section from this glyph.
     *
     * @param section The section to be removed
     */
    public void removeSection (GlyphSection section)
    {
        section.setGlyph(null);

        if (!members.remove(section)) {
            logger.warning("removeSection " + section + " not part of " + this);
        }

        invalidateCache();
    }

    //---------------//
    // renderBoxArea //
    //---------------//
    /**
     * Render the box area of the glyph, using inverted color
     *
     * @param g the graphic context
     * @param z the display zoom
     */
    public boolean renderBoxArea (Graphics g,
                                  Zoom     z)
    {
        // Check the clipping
        Rectangle box = new Rectangle(getContourBox());
        z.scale(box);

        if (box.intersects(g.getClipBounds())) {
            g.fillRect(box.x, box.y, box.width, box.height);

            return true;
        } else {
            return false;
        }
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a descriptive string
     *
     * @return the glyph description
     */
    @Override
    public String toString ()
    {
        StringBuffer sb = new StringBuffer(256);
        sb.append("{")
          .append(getPrefix());
        sb.append("#")
          .append(id);

        if (getShape() != null) {
            sb.append(" shape=")
              .append(getShape())
              .append("(")
              .append(String.format("%.3f", getDoubt()))
              .append(")");

            if (getTrainingShape() != getShape()) {
                sb.append(" training=")
                  .append(getTrainingShape());
            }
        }

        if (getCentroid() != null) {
            sb.append(" centroid=[")
              .append(getCentroid().x)
              .append(",")
              .append(getCentroid().y)
              .append("]");
        }

        // Is that all?
        if (this.getClass()
                .getName()
                .equals(Glyph.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }

    protected void setGrade (double grade)
    {
        if (original != null) {
            original.setGrade(grade);
        } else {
            this.doubt = grade;
        }
    }

    //-----------//
    // getPrefix //
    //-----------//
    /**
     * Return a distinctive string, to be used as a prefix in toString() for
     * example.
     *
     * @return the prefix string
     */
    protected String getPrefix ()
    {
        return "Glyph";
    }

    //--------------------//
    // setForbiddenShapes //
    //--------------------//
    private void setForbiddenShapes (Set<Shape> forbiddenShapes)
    {
        if (original != null) {
            original.setForbiddenShapes(forbiddenShapes);
        } else {
            this.forbiddenShapes = forbiddenShapes;
        }
    }

    //--------------------//
    // getForbiddenShapes //
    //--------------------//
    private Set<Shape> getForbiddenShapes ()
    {
        if (original != null) {
            return original.getForbiddenShapes();
        } else {
            return forbiddenShapes;
        }
    }

    //----------------//
    // cumulatePoints //
    //----------------//
    private int cumulatePoints (int[] coord,
                                int[] pos,
                                int   nb)
    {
        for (Section section : members) {
            nb = section.cumulatePoints(coord, pos, nb);
        }

        return nb;
    }

    //------//
    // fill //
    //------//
    private void fill (char[][]  table,
                       Rectangle box)
    {
        for (Section section : members) {
            section.fillTable(table, box);
        }
    }

    //-------------//
    // forbidShape //
    //-------------//
    private void forbidShape (Shape shape)
    {
        Set<Shape> forbiddens = getForbiddenShapes();

        if (forbiddens == null) {
            forbiddens = new HashSet<Shape>();
            setForbiddenShapes(forbiddens);
        }

        forbiddens.add(shape);
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    private void invalidateCache ()
    {
        setCentroid(null);
        setMoments(null);
        setBounds(null);
        contourBox = null;
        weight = null;
        signature = null;
    }
}
