//----------------------------------------------------------------------------//
//                                                                            //
//                                 G l y p h                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.check.Checkable;
import omr.check.Result;
import omr.check.SuccessResult;

import omr.lag.Section;
import omr.lag.SectionView;

import omr.math.Moments;

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
 * <p>A Glyph is basically a collection of sections. It can be split into
 * smaller glyphs, which may later be re-assembled into another instance of
 * glyph. There is a means, based on a simple signature (weight and bounding
 * box), to detect if the glyph at hand is identical to a previous one, which is
 * then reused.
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
     * Sections that compose this glyph. The collection is kept sorted
     * on GlyphSection order
     */
    @XmlElement(name = "section")
    protected SortedSet<GlyphSection> members = new TreeSet<GlyphSection>();

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

    // Below are properties to be retrieved in the glyph original if any

    /** Contained leaves and stems */
    protected Set<Glyph> leaves = new HashSet<Glyph>();

    /** Link to the glyph this one is a leaf of */
    protected Glyph leafOf;

    /** Contained parts, if this glyph is a compound */
    protected Set<Glyph> parts = new HashSet<Glyph>();

    /** Link to the compound this one is a part of */
    protected Glyph partOf;

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

    //----------//
    // isActive //
    //----------//
    /**
     * Tests whether this glyph is active (its member sections point to it) or
     * not
     *
     * @return true if glyph is active
     */
    public boolean isActive ()
    {
        // Some sanity check, to be removed later
        if (true) {
            boolean foundMembers = false;
            boolean foundAliens = false;

            for (GlyphSection section : members) {
                if (section.getGlyph() == this) {
                    foundMembers = true;
                } else {
                    foundAliens = true;
                }

                if (foundAliens && foundMembers) {
                    logger.warning("Mixed aliens & members in " + this);
                }
            }
        }

        return members.first()
                      .getGlyph() == this;
    }

    //-------//
    // isBar //
    //-------//
    /**
     * Convenient method which tests if the glyph is a Bar line
     *
     * @return true if glyph shape is a bar
     */
    public boolean isBar ()
    {
        return Shape.Barlines.contains(getShape());
    }

    //-----------------//
    // getFirstSection //
    //-----------------//
    /**
     * Report the first section in the ordered collection of glyph members
     *
     * @return the first section of the glyph
     */
    public GlyphSection getFirstSection ()
    {
        return members.first();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Convenient method, to build a string with just the ids of the glyph
     * collection
     *
     * @param glyphs the collection of glyphs
     * @return the string built
     */
    public static String toString (Collection<?extends Glyph> glyphs)
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
    /**
     * Return the bounding rectangle of the glyph
     *
     * @return the bounds
     */
    public Rectangle getBounds ()
    {
        if (bounds == null) {
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

        // Default
        return getAreaCenter();
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
        if (centroid == null) {
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
        return doubt;
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
        this.interline = interline;
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
        return interline;
    }

    //---------//
    // isKnown //
    //---------//
    /**
     * A glyph is considered as known if it has a registered shape other than
     * NOISE (Notice that CLUTTER as well as NO_LEGAL_SHAPE are considered as
     * being known).
     *
     * @return true if known
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
        this.leftStem = leftStem;
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
        return leftStem;
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
    public Collection<GlyphSection> getMembers ()
    {
        return members;
    }

    //------------//
    // setMoments //
    //------------//
    public void setMoments (Moments moments)
    {
        this.moments = moments;
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
        if (moments == null) {
            computeMoments();
        }

        return moments;
    }

    //---------------------//
    // getNormalizedWeight //
    //---------------------//
    public double getNormalizedWeight ()
    {
        return getMoments()
                   .getWeight();
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
        this.pitchPosition = pitchPosition;
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
        return pitchPosition;
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
        this.rightStem = rightStem;
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
        return rightStem;
    }

    //----------//
    // setShape //
    //----------//
    /**
     * Setter for the glyph shape, assumed to be based on structural data
     *
     * @param shape the assigned shape, which may be null
     */
    @XmlAttribute(name = "shape")
    public void setShape (Shape shape)
    {
        setShape(shape, Evaluation.NO_DOUBT);
    }

    //----------//
    // setShape //
    //----------//
    /**
     * Setter for the glyph shape, with related doubt
     *
     * @param shape the assigned shape
     * @param doubt the related doubt
     */
    public void setShape (Shape  shape,
                          double doubt)
    {
        // Blacklist the previous shape if any
        if (getShape() != null) {
            forbidShape(getShape());
        }

        // Now remove the assigned shape from the blacklist if any
        allowShape(shape);

        this.shape = shape;
        this.doubt = doubt;
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
        return shape;
    }

    //------------------//
    // isShapeForbidden //
    //------------------//
    public boolean isShapeForbidden (Shape shape)
    {
        return (forbiddenShapes != null) && forbiddenShapes.contains(shape);
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
        this.stemNumber = stemNumber;
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
        return stemNumber;
    }

    //--------------//
    // isSuccessful //
    //--------------//
    public boolean isSuccessful ()
    {
        return result instanceof SuccessResult;
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
        this.withLedger = withLedger;
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
        return withLedger;
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
     * @param link While adding a section to this glyph members, should we also
     *             set the link from section back to the glyph ?
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
        if (forbiddenShapes != null) {
            forbiddenShapes.remove(shape);
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
     * @return the result of ordering
     */
    @Implement(Comparable.class)
    public int compareTo (Glyph other)
    {
        Point ref = this.getContourBox()
                        .getLocation();
        Point otherRef = other.getContourBox()
                              .getLocation();

        // Are x values different?
        int dx = ref.x - otherRef.x;

        if (dx != 0) {
            return dx;
        }

        // Vertically aligned, so use ordinates
        int dy = ref.y - otherRef.y;

        if (dy != 0) {
            return dy;
        }

        // Finally, use id ...
        return this.getId() - other.getId();
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
        try {
            setMoments(new Moments(pos, coord, weight, getInterline()));
        } catch (Exception ex) {
            logger.warning(
                "Glyph #" + id + " Cannot compute moments with unit set to 0");
        }
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

        //        // We do not destroy the sections, just the glyph which must be
        //        // removed from its containing lag.
        //        if (lag != null) {
        //            lag.removeGlyph(this);
        //        }
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
        System.out.println("   lag=" + getLag());
        System.out.println("   members=" + getMembers());
        System.out.println("   parts=" + parts);
        System.out.println("   partOf=" + partOf);
        System.out.println("   leaves=" + leaves);
        System.out.println("   leafOf=" + leafOf);
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
        System.out.println("   forbiddenShapes=" + forbiddenShapes);
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

        if (leafOf != null) {
            sb.append(" leafOf #")
              .append(leafOf.getId());
        }

        if (leaves.size() > 0) {
            sb.append(" leaves")
              .append(toString(leaves));
        }

        if (partOf != null) {
            sb.append(" partOf#")
              .append(partOf.getId());
        }

        if (parts.size() > 0) {
            sb.append(" parts")
              .append(toString(parts));
        }

        if (centroid != null) {
            sb.append(" centroid=[")
              .append(centroid.x)
              .append(",")
              .append(centroid.y)
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

    protected void setDoubt (double doubt)
    {
        this.doubt = doubt;
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

    //-----------//
    // setPartOf //
    //-----------//
    /**
     * Record the link from this glyph as part of a larger compound
     *
     * @param compound the containing compound
     */
    void setPartOf (Glyph compound)
    {
        partOf = compound;
    }

    //----------//
    // setParts //
    //----------//
    /**
     * Record the parts that compose this compound gmyph
     *
     * @param parts the contained parts
     */
    void setParts (Collection<?extends Glyph> parts)
    {
        if (this.parts != parts) {
            this.parts.clear();
            this.parts.addAll(parts);
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
        if (forbiddenShapes == null) {
            forbiddenShapes = new HashSet<Shape>();
        }

        forbiddenShapes.add(shape);
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    private void invalidateCache ()
    {
        centroid = null;
        moments = null;
        bounds = null;
        contourBox = null;
        weight = null;
        signature = null;
    }
}
