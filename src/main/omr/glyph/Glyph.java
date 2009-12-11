//----------------------------------------------------------------------------//
//                                                                            //
//                                 G l y p h                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.check.Checkable;
import omr.check.Result;
import omr.check.SuccessResult;

import omr.glyph.text.TextInfo;

import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.ui.SectionView;

import omr.log.Logger;

import omr.math.Moments;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.sheet.SystemInfo;

import omr.ui.icon.SymbolIcon;

import omr.util.Implement;
import omr.util.Predicate;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;

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
    implements Checkable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Glyph.class);

    //~ Instance fields --------------------------------------------------------

    /** The containing glyph lag */
    protected GlyphLag lag;

    /** Instance identifier (Unique in the containing GlyphLag) */
    @XmlAttribute
    private int id;

    /** Interline of the containing staff (or sheet) */
    @XmlAttribute
    private final int interline;

    /**
     * Sections that compose this glyph. The collection is kept sorted
     * on GlyphSection order
     */
    @XmlElement(name = "section")
    private SortedSet<GlyphSection> members = new TreeSet<GlyphSection>();

    /**
     * Display box (always properly oriented), so that rectangle width is
     * aligned with display horizontal and rectangle height with display
     * vertical
     */
    private Rectangle contourBox;

    /** Total weight of this glyph */
    private Integer weight;

    /** Result of analysis wrt this glyph */
    private Result result;

    /** A signature to retrieve this glyph */
    private GlyphSignature signature;

    /** Contained parts, if this glyph is a compound */
    private Set<Glyph> parts = new LinkedHashSet<Glyph>();

    /** Link to the compound, if any, this one is a part of */
    private Glyph partOf;

    /** Current recognized shape of this glyph */
    private Shape shape;

    /** Doubt in the assigned shape */
    private double doubt = Evaluation.ALGORITHM;

    /** Number of stems it is connected to (0, 1, 2) */
    private int stemNumber;

    /** Stem attached on left if any */
    private Glyph leftStem;

    /** Stem attached on right if any */
    private Glyph rightStem;

    /** Position with respect to nearest staff. Key references are : 0 for
       middle line (B), -2 for top line (F) and +2 for bottom line (E)  */
    private double pitchPosition;

    /** Is there a ledger nearby ? */
    private boolean withLedger;

    /** Computed moments of this glyph */
    private Moments moments;

    /**  Mass center coordinates */
    private PixelPoint centroid;

    /**  Box center coordinates */
    private PixelPoint center;

    /**
     * Bounding rectangle, defined as union of all member section bounds so
     * this implies that it has the same orientation as the sections
     */
    private Rectangle bounds;

    /** Related textual information, if any */
    private TextInfo textInfo;

    /** Set of forbidden shapes, if any */
    private Set<Shape> forbiddenShapes;

    /** Set of translation(s) of this glyph on the score side */
    private Collection<Object> translations = new ArrayList<Object>();

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Glyph //
    //-------//
    /**
     * Constructor with setting of scaling information
     * @param interline the related interline value for scaling
     */
    public Glyph (int interline)
    {
        this.interline = interline;
    }

    //-------//
    // Glyph //
    //-------//
    /**
     * No-arg constructor to please JAXB
     */
    protected Glyph ()
    {
        interline = 0; // Dummy
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // isActive //
    //----------//
    /**
     * Tests whether this glyph is active (all its member sections point to it)
     *
     * @return true if glyph is active, false otherwise
     */
    public boolean isActive ()
    {
        for (GlyphSection section : members) {
            if (section.getGlyph() != this) {
                return false;
            }
        }

        return true;
    }

    //----------------//
    // getAlienSystem //
    //----------------//
    /**
     * Check whether all the glyph sections belong to the same system
     * @param system the supposed containing system
     * @return the alien system found, or null if OK
     */
    public SystemInfo getAlienSystem (SystemInfo system)
    {
        // Direct members
        for (GlyphSection section : members) {
            if (section.getSystem() != system) {
                return section.getSystem();
            }
        }

        // Parts if any, recursively
        for (Glyph part : getParts()) {
            SystemInfo alien = part.getAlienSystem(system);

            if (alien != null) {
                return alien;
            }
        }

        // No other system found
        return null;
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
        return ShapeRange.Barlines.contains(getShape());
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

    //--------//
    // isClef //
    //--------//
    /**
     * Convenient method which tests if the glyph is a Clef
     *
     * @return true if glyph shape is a Clef
     */
    public boolean isClef ()
    {
        return ShapeRange.Clefs.contains(getShape());
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
     * Return a copy of the display bounding box of the display contour.
     * Useful to quickly check if the glyph needs to be repainted.
     *
     * @return a COPY of the bounding contour rectangle box
     */
    public PixelRectangle getContourBox ()
    {
        if (contourBox == null) {
            Rectangle box = null;

            for (Section section : members) {
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

    //----------//
    // getImage //
    //----------//
    /**
     * Report an image of the glyph (which can be handed to the OCR)
     * @return a black & white image (contour box size )
     */
    public BufferedImage getImage ()
    {
        // Determine the bounding box
        Rectangle     box = getContourBox();
        BufferedImage image = new BufferedImage(
            box.width,
            box.height,
            BufferedImage.TYPE_BYTE_GRAY);

        for (Section section : members) {
            section.fillImage(image, box);
        }

        return image;
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
    public int getInterline ()
    {
        return interline;
    }

    //---------//
    // isKnown //
    //---------//
    /**
     * A glyph is considered as known if it has a registered shape other than
     * NOISE (Notice that CLUTTER as well as NO_LEGAL_TIME and GLYPH_PART are
     * considered as being known).
     *
     * @return true if known
     */
    public boolean isKnown ()
    {
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

    //-------------//
    // getLocation //
    //-------------//
    /**
     * Report the glyph (reference) location, which is the equivalent of the
     * icon reference point if one such point exists, or the glyph area center
     * otherwise. The point is lazily evaluated.
     *
     * @return the reference center point
     */
    public PixelPoint getLocation ()
    {
        // No shape: use area center
        if (shape == null) {
            return getAreaCenter();
        }

        // Text shape: use specific reference
        if (shape.isText()) {
            return getTextInfo()
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
        return getDoubt() == Evaluation.MANUAL;
    }

    //------------//
    // getMembers //
    //------------//
    /**
     * Report the set of member sections.
     *
     * @return member sections
     */
    public SortedSet<GlyphSection> getMembers ()
    {
        return members;
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
    // getNormalizedHeight //
    //---------------------//
    /**
     * Report the height of this glyph, after normalization to sheet interline
     * @return the height value, expressed as an interline fraction
     */
    public double getNormalizedHeight ()
    {
        return getMoments()
                   .getHeight();
    }

    //---------------------//
    // getNormalizedWeight //
    //---------------------//
    /**
     * Report the weight of this glyph, after normalization to sheet interline
     * @return the weight value, expressed as an interline square fraction
     */
    public double getNormalizedWeight ()
    {
        return getMoments()
                   .getWeight();
    }

    //--------------------//
    // getNormalizedWidth //
    //--------------------//
    /**
     * Report the width of this glyph, after normalization to sheet interline
     * @return the width value, expressed as an interline fraction
     */
    public double getNormalizedWidth ()
    {
        return getMoments()
                   .getWidth();
    }

    //-----------//
    // setPartOf //
    //-----------//
    /**
     * Record the link from this glyph as part of a larger compound
     *
     * @param compound the containing compound
     */
    public void setPartOf (Glyph compound)
    {
        partOf = compound;
    }

    //-----------//
    // getPartOf //
    //-----------//
    /**
     * Report the containing compound, if any
     *
     * @return compound the containing compound if any
     */
    public Glyph getPartOf ()
    {
        return partOf;
    }

    //----------//
    // setParts //
    //----------//
    /**
     * Record the parts that compose this compound gmyph
     *
     * @param parts the contained parts
     */
    public void setParts (Collection<?extends Glyph> parts)
    {
        if (this.parts != parts) {
            this.parts.clear();
            this.parts.addAll(parts);
        }
    }

    //----------//
    // getParts //
    //----------//
    /**
     * Report the parts, if any, that compose this compound
     * @return the set of glyphs, perhaps empty, but never null
     */
    public Set<Glyph> getParts ()
    {
        return parts;
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
    public void setShape (Shape shape)
    {
        setShape(shape, Evaluation.ALGORITHM);
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
        // Blacklist the old shape if any
        Shape oldShape = getShape();

        if (oldShape != null) {
            forbidShape(oldShape);
        }

        if (shape == null) {
            // Set the part shape to null as well (rather than GLYPH_PART)
            for (Glyph part : this.getParts()) {
                part.setShape(null, doubt);
            }
        } else {
            // Remove the new shape from the blacklist if any
            allowShape(shape);
        }

        // Remember the new shape
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
    /**
     * Check whether a shape is forbidden for this glyph
     * @param shape the shape to check
     * @return true if the provided shape is one of the forbidden shapes for
     * this glyph
     */
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
    /**
     * Convenient method to check whether the glyph is successfully recognized
     * @return true if the glyph is successfully recognized
     */
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

    //--------//
    // isText //
    //--------//
    /**
     * Check whether the glyph shape is a text (or a character)
     *
     * @return true if text or character
     */
    public boolean isText ()
    {
        return (shape != null) && shape.isText();
    }

    //-------------//
    // getTextInfo //
    //-------------//
    /**
     * Report the textual information for this glyph
     * @return the glyph textual info, or null if none
     */
    public TextInfo getTextInfo ()
    {
        if (textInfo == null) {
            textInfo = new TextInfo(this);
        }

        return textInfo;
    }

    //--------//
    // isTime //
    //--------//
    /**
     * Convenient method which tests if the glyph is a (part of) time signature
     *
     * @return true if glyph shape is a time
     */
    public boolean isTime ()
    {
        return ShapeRange.SingleTimes.contains(getShape()) ||
               ShapeRange.MultiTimes.contains(getShape());
    }

    //------------------//
    // setTrainingShape //
    //------------------//
    /**
     * Assign the training shape of the registered glyph shape (NB: this is
     * meant for JAXB)
     *
     * @param shape the glyph shape
     */
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

    //-------------//
    // isTransient //
    //-------------//
    /**
     * Test whether the glyph is transient (not yet inserted into lag)
     * @return true if transient
     */
    public boolean isTransient ()
    {
        return id == 0;
    }

    //--------------//
    // isTranslated //
    //--------------//
    /**
     * Report whether this glyph is translated to a score entity
     * @return true if this glyph is translated to score
     */
    public boolean isTranslated ()
    {
        return !translations.isEmpty();
    }

    //----------------//
    // setTranslation //
    //----------------//
    /**
     * Assign a unique score translation for this glyph
     * @param entity the score entity that is a translation of this glyph
     */
    public void setTranslation (Object entity)
    {
        translations.clear();
        addTranslation(entity);
    }

    //-----------------//
    // getTranslations //
    //-----------------//
    /**
     * Report the collection of score entities this glyph contributes to
     * @return the collection of entities that are translations of this glyph
     */
    public Collection<Object> getTranslations ()
    {
        return translations;
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
     * A glyph is considered as well known if it has a registered well known
     * shape
     *
     * @return true if so
     */
    public boolean isWellKnown ()
    {
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
     *             set the link from section back to the glyph?
     */
    public void addSection (GlyphSection section,
                            boolean      link)
    {
        // Nota: We must include the section in the glyph members before
        // linking back the section to the containing glyph.
        // Otherwise, there is a risk of using the glyph box (which depends on
        // its member sections) before the section is in the glyph members.
        // This phenomenum was sometimes observed when using parallelism.

        /** First, update glyph data */
        members.add(section);

        /** Second, update section data, if so desired */
        if (link) {
            section.setGlyph(this);
        }

        invalidateCache();
    }

    //----------------//
    // addTranslation //
    //----------------//
    /**
     * Add a score entity as a translation for this glyph
     * @param entity the counterpart of this glyph on the score side
     */
    public void addTranslation (Object entity)
    {
        translations.add(entity);
    }

    //------------//
    // allowShape //
    //------------//
    /**
     * Remove the provided shape from the collection of forbidden shaped, if any
     * @param shape the shape to allow
     */
    public void allowShape (Shape shape)
    {
        if (forbiddenShapes != null) {
            forbiddenShapes.remove(shape);
        }
    }

    //-------------------//
    // clearTranslations //
    //-------------------//
    /**
     * Remove all the links to score entities
     */
    public void clearTranslations ()
    {
        translations.clear();
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
            SectionView view = (SectionView) section.getView(viewIndex);
            view.setColor(color);
        }
    }

    //----------//
    // colorize //
    //----------//
    /**
     * Set the display color of all sections that compose this stick.
     *
     * @param lag the containing lag
     * @param viewIndex index in the view list
     * @param color     color for the whole stick
     */
    public void colorize (Lag   lag,
                          int   viewIndex,
                          Color color)
    {
        if (lag == this.lag) {
            colorize(viewIndex, getMembers(), color);
        }
    }

    //----------//
    // colorize //
    //----------//
    /**
     * Set the display color of all sections gathered by the provided list
     *
     * @param viewIndex the proper view index
     * @param sections  the collection of sections
     * @param color     the display color
     */
    public void colorize (int                      viewIndex,
                          Collection<GlyphSection> sections,
                          Color                    color)
    {
        for (GlyphSection section : sections) {
            SectionView view = (SectionView) section.getView(viewIndex);
            view.setColor(color);
        }
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
            setMoments(new Moments(pos, coord, weight, getInterline()));
        } catch (Exception ex) {
            logger.warning(
                "Glyph #" + id + " Cannot compute moments with unit set to 0");
        }
    }

    //-------------//
    // cutSections //
    //-------------//
    /**
     * Cut the link to this glyph from its member sections, only if the sections
     * actually point to this glyph
     */
    public void cutSections ()
    {
        for (GlyphSection section : members) {
            if (section.getGlyph() == this) {
                section.setGlyph(null);
            }
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
        System.out.println("   @" + Integer.toHexString(hashCode()));
        System.out.println("   id=" + getId());
        System.out.println("   isActive=" + isActive());
        System.out.println("   lag=" + getLag());
        System.out.println("   members=" + getMembers());
        System.out.println("   parts=" + parts);
        System.out.println("   partOf=" + partOf);
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
        System.out.println("   location=" + getLocation());
        System.out.println("   centroid=" + getCentroid());
        System.out.println("   bounds=" + getBounds());
        System.out.println("   forbiddenShapes=" + forbiddenShapes);
        System.out.println("   textInfo=" + textInfo);
        System.out.println("   translations=" + translations);
    }

    //-----------------//
    // linkAllSections //
    //-----------------//
    /**
     * Make all the glyph's sections point back to this glyph
     */
    public void linkAllSections ()
    {
        for (GlyphSection section : getMembers()) {
            section.setGlyph(this);
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
            SectionView view = (SectionView) section.getView(viewIndex);
            view.resetColor();
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

        if (shape != null) {
            sb.append(" shape=")
              .append(shape)
              .append("(")
              .append(String.format("%.3f", getDoubt()))
              .append(")");

            if (getTrainingShape() != shape) {
                sb.append(" training=")
                  .append(getTrainingShape());
            }

            if (shape.isText()) {
                String textContent = getTextInfo()
                                         .getContent();

                if (textContent != null) {
                    sb.append(" \"")
                      .append(textContent)
                      .append("\"");
                }
            }
        }

        if (partOf != null) {
            sb.append(" partOf#")
              .append(partOf.getId());
        }

        if (!parts.isEmpty()) {
            sb.append(Glyphs.toString(" parts", parts));
        }

        if (centroid != null) {
            sb.append(" centroid=[")
              .append(centroid.x)
              .append(",")
              .append(centroid.y)
              .append("]");
        }

        if (isTranslated()) {
            sb.append(" trans=[")
              .append(translations)
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

    //-----------------//
    // linkAllSections //
    //-----------------//
    /**
     * Make all the glyph's sections point back to the provided owner glyph, and
     * transitively / recursively for all sections of the glyph parts if any
     * @param owner the glyph that is to be pointed back by the sections
     */
    protected void linkAllSections (Glyph owner)
    {
        // First the direct members of this glyph
        for (GlyphSection section : getMembers()) {
            section.setGlyph(owner);
        }

        // Then, same thing for the parts if any
        for (Glyph part : getParts()) {
            part.linkAllSections(owner);
        }
    }

    //---------------------//
    // copyStemInformation //
    //---------------------//
    /**
     * Forward stem-related information from the provided glyph
     * @param glyph the glyph whose stem information has to be used
     */
    void copyStemInformation (Glyph glyph)
    {
        setLeftStem(glyph.getLeftStem());
        setRightStem(glyph.getRightStem());
        setStemNumber(glyph.getStemNumber());
    }

    //------------//
    // setMoments //
    //------------//
    private void setMoments (Moments moments)
    {
        this.moments = moments;
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
        clearTranslations();
    }
}
