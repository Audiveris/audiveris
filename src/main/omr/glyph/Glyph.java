//-----------------------------------------------------------------------//
//                                                                       //
//                               G l y p h                               //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

import omr.check.Checkable;
import omr.check.Result;
import omr.lag.Section;
import omr.lag.SectionView;
import omr.math.Moments;
import omr.sheet.Picture;
import omr.util.Logger;
import omr.util.Predicate;
import omr.ui.view.Zoom;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import omr.sheet.PixelPoint;

/**
 * Class <code>Glyph</code> represents any glyph found, such as stem,
 * ledger, accidental, note head, etc...
 *
 * <p> Collections of glyphs (generally at system level) are sorted first
 * by abscissa then by ordinate. To cope with glyphs nearly vertically
 * aligned, and which would thus be too dependent on the precise x value,
 * the y value could be used instead. But this would lead to non transitive
 * comparisons...
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Glyph
    implements Comparable<Glyph>,
                   Checkable,
                   java.io.Serializable
{
    //~ Static variables/initializers -------------------------------------

    private static Logger logger = Logger.getLogger(Glyph.class);

    //~ Instance variables ------------------------------------------------

    /** Unique instance identifier (in the containing GlyphLag) */
    protected int id;

    /**  Centroid coordinates */
    protected PixelPoint centroid;

    /** Sections that compose this glyph */
    protected List<GlyphSection> members = new ArrayList<GlyphSection>();

    /** The containing lag */
    protected GlyphLag lag;

    /** Bounding rectangle */
    protected Rectangle bounds;

    /** Display box (properly oriented) */
    protected Rectangle contourBox;

    /** Result of analysis wrt this glyph */
    protected Result result;

    /** Recognized shape of this glyph */
    protected Shape shape;

    /** Guessed shape of this glyph */
    protected Shape guess;

    /** Interline of the containing staff (or sheet) */
    protected int interline;

    /** Measure margin on left side (in interline fraction) */
    protected double leftMargin;

    /** Measure margin on right side (in interline fraction) */
    protected double rightMargin;

    /** Has a ledger nearby ? */
    protected boolean hasLedger;

    /** Number of stems it is connected to (0, 1, 2) */
    protected int stemNumber;

    /** Position with respect to nearest staff. Key references are : 0 for
        middle line (B), -2 for top line (F) and +2 for bottom line (E)  */
    protected double pitchPosition;

    /** Computed moments of this glyph */
    protected Moments moments;

    //~ Constructors ------------------------------------------------------

    //-------//
    // Glyph //
    //-------//
    /**
     * Constructor needed for Class.newInstance method
     */
    public Glyph()
    {
    }

    //~ Methods -----------------------------------------------------------

    //------------------//
    // addGlyphSections //
    //------------------//
    /**
     * Add another glyph (with its sections of points) to this one
     *
     * @param other The merged glyph
     * @param linkSections Should we set the link from sections to glyph ?
     */
    public void addGlyphSections (Glyph other,
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

    //----------//
    // colorize //
    //----------//
    /**
     * Set the display color of all sections that compose this glyph.
     *
     * @param viewIndex index in the view list
     * @param color     color for the whole glyph
     */
    public void colorize (int viewIndex,
                          Color color)
    {
        for (GlyphSection section : members) {
            SectionView view =
                (SectionView) section.getViews().get(viewIndex);
            view.setColor(color);
        }
    }

    //-----------//
    // compareTo //
    //-----------//
    /**
     * Needed to implement Comparable, sorting glyphs first by abscissa,
     * then by ordinate.
     *
     * @param other the other glyph to compare to
     * @return th result of ordering
     */
    public int compareTo (Glyph other)
    {
        // Are x values different?
        int dx = getCentroid().x - other.getCentroid().x;

        if (dx != 0) {
            return dx;
        }

        // Glyphs are vertically aligned, so use ordinates
        return centroid.y - other.centroid.y;
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
        int weight = getWeight();

        int[] coord = new int[weight];
        int[] pos = new int[weight];

        // Append recursively all points
        cumulatePoints(coord, pos, 0);

        // Then compute the moments, swapping pos & coord since the lag is
        // vertical
        moments = new Moments(pos, coord, weight, interline);
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
    public void dump()
    {
        // Temporary
        omr.util.Dumper.dump(this);
    }

    //-------------//
    // erasePixels //
    //-------------//
    /**
     * In the provided Picture, remove the pixels that correspond to the
     * member sections
     *
     * @param picture the provided picture
     */
    public void erasePixels (Picture picture)
    {
        for (GlyphSection section : members) {
            section.write(picture, Picture.BACKGROUND);
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
     * Report the glyph centroid (mass center). The point is lasily evaluated.
     *
     * @return the mass center point
     */
    public PixelPoint getCentroid()
    {
        if (centroid == null) {
            centroid = getMoments().getCentroid();
        }

        return centroid;
    }

    //----------//
    // getColor //
    //----------//
    /**
     * Report the color to be used to colorize the provided glyph,
     * according to the color policy which is based on the glyph shape
     *
     * @return the related shape color of the glyph, or the predefined
     * {@link Shape#missedColor} if the glyph has no related shape
     */
    public Color getColor()
    {
        if (getShape() == null) {
            return Shape.missedColor;
        } else {
            return getShape().getColor();
        }
    }

    //---------------//
    // getContourBox //
    //---------------//
    /**
     * Return the display bounding box of the display contour. Useful to
     * quickly check if the glyph needs to be repainted.
     *
     * @return the bounding contour rectangle box
     */
    public Rectangle getContourBox ()
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

        return contourBox;
    }

    //----------//
    // getGuess //
    //----------//
    /**
     * Report the stored guessed shape, if any, for the glyph
     *
     * @return the guessed shape, or null
     */
    public Shape getGuess ()
    {
        return guess;
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
    // getInterline //
    //--------------//
    /**
     * Report the interline value for the glyph containing staff, which is
     * used for some of the moments
     *
     * @return the interline value
     */
    public int getInterline ()
    {
        return interline;
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
    // getPitchPosition //
    //------------------//
    /**
     * Report the pitchPosition feature (position relative to the staff)
     *
     *
     * @return the pitchPosition value
     */
    public double getPitchPosition ()
    {
        return pitchPosition;
    }

    //-----------------//
    // getSymbolsAfter //
    //-----------------//
    /**
     * Return the known glyphs stuck on last side of the stick
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
                if (sct.isMember()) {
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
     * Return the known glyphs stuck on first side of the stick
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
                if (sct.isMember()) {
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
        int weight = 0;

        for (GlyphSection section : members) {
            weight += section.getWeight();
        }

        return weight;
    }

    //-----------//
    // hasLedger //
    //-----------//
    /**
     * Report whether the glyph touches a ledger
     *
     * @return true if there is a close ledger
     */
    public boolean hasLedger ()
    {
        return hasLedger;
    }

    //---------------//
    // getStemNumber //
    //---------------//
    /**
     * Report the number of stems the glyph is close to
     *
     * @return the number of stems near by, typically 0, 1 or 2.
     */
    public int getStemNumber ()
    {
        return stemNumber;
    }

    //---------//
    // isKnown //
    //---------//
    /**
     * A glyph is considered as known if it has a registered shape other
     * than noise (clutter is considered as known)
     *
     * @return true if so
     */
    public boolean isKnown ()
    {
        Shape shape = getShape();
        return
            shape != null &&
            shape != Shape.NOISE;
    }

    //-------------//
    // isWellKnown //
    //-------------//
    /**
     * A glyph is considered as well known if it has a registered shape
     * other than noise and stucture
     *
     * @return true if so
     */
    public boolean isWellKnown ()
    {
        Shape shape = getShape();
        return
            shape != null &&
            shape.isWellKnown();
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
        return shape == Shape.COMBINING_STEM;
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
            SectionView view
                = (SectionView) section.getViews().get(viewIndex);
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
            logger.warning("removeSection " + section +
                         " not part of " + this);
        }

        invalidateCache();
    }

    //---------------//
    // renderContour //
    //---------------//
    /**
     * Render the contour box of the glyph, using the current foreground
     * color
     *
     * @param g the graphic context
     * @param z the display zoom
     */
    public boolean renderContour (Graphics g,
                                  Zoom z)
    {
        // Check the clipping
        Rectangle box = new Rectangle(getContourBox());
        z.scale(box);

        if (box.intersects(g.getClipBounds())) {
            g.drawRect(box.x, box.y, box.width, box.height);
            return true;
        } else {
            return false;
        }
    }

    //----------//
    // setGuess //
    //----------//
    /**
     * Register a shape as glyph guess
     *
     * @param guess the guessed shape to register
     */
    public void setGuess (Shape guess)
    {
        this.guess = guess;
    }

    //--------------//
    // setHasLedger //
    //--------------//
    /**
     * Remember info about ledger near by
     *
     * @param hasLedger true is there is such ledger
     */
    public void setHasLedger (boolean hasLedger)
    {
        this.hasLedger = hasLedger;
    }

    //-------------//
    // setLeftStem //
    //-------------//
    /**
     * Remember the number of stems near by
     *
     * @param stemNumber the number of stems
     */
    public void setStemNumber (int stemNumber)
    {
        this.stemNumber = stemNumber;
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

    //---------------//
    // setLeftMargin //
    //---------------//
    /**
     * Set the left margin within the measure
     *
     * @param leftMargin measure left margin in interline fraction
     */
    public void setLeftMargin (double leftMargin)
    {
        this.leftMargin = leftMargin;
    }

    //---------------//
    // getLeftMargin //
    //---------------//
    /**
     * Report the measure left margin (in interline fraction) before the glyph
     *
     * @return the left margin
     */
    public double getLeftMargin()
    {
        return leftMargin;
    }

    //----------------//
    // setRightMargin //
    //----------------//
    /**
     * Set the right margin within the measure
     *
     * @param rightMargin measure right margin in interline fraction
     */
    public void setRightMargin (double rightMargin)
    {
        this.rightMargin = rightMargin;
    }

    //----------------//
    // getRightMargin //
    //----------------//
    /**
     * Report the measure right margin (in interline fraction) after the glyph
     *
     * @return the right margin
     */
    public double getRightMargin()
    {
        return rightMargin;
    }

    //-----------//
    // setResult //
    //-----------//
    /**
     * Record the analysis result in the glyph itself
     *
     * @param result the assigned result
     */
    public void setResult (Result result)
    {
        this.result = result;
    }

    //----------//
    // setShape //
    //----------//
    /**
     * Setter for the glyph shape
     *
     * @param shape the assigned shape, which may be null
     */
    public void setShape (Shape shape)
    {
        this.shape = shape;
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

    //----------------//
    // setStringShape //
    //----------------//
    /**
     * Setter for the shape, knowing the name of the shape
     *
     * @param shape the NAME of the shape
     */
    public void setStringShape (String shape)
    {
        this.shape = Shape.valueOf(shape);
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
        sb.append("{").append(getPrefix());
        sb.append("#").append(id);

        if (shape != null) {
            sb.append(" shape=").append(shape);
        }

        if (guess != null) {
            sb.append(" guess=").append(guess);
        }

        if (centroid != null) {
            sb.append(" centroid=[").append(centroid.x).append(",");
            sb.append(centroid.y).append("]");
        }

        //         if (moments != null)
        //             sb.append(" moments=" + moments);

        if (this.getClass().getName().equals (Glyph.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }

    //~ Methods protected -------------------------------------------------

    //-----------//
    // getPrefix //
    //-----------//
    /**
     * Return a distinctive string, to be used as a prefix in toString()
     * for example.
     *
     * @return the prefix string
     */
    protected String getPrefix ()
    {
        return "Glyph";
    }

    //~ Methods package private -------------------------------------------

    //--------//
    // setLag // // For access from GlyphLag only !!!
    //--------//
    /**
     * The setter for glyph lag. Used with care, by {@link GlyphLag} and
     * {@link omr.glyph.ui.GlyphVerifier}
     *
     * @param lag the containing lag
     */
    public void setLag (GlyphLag lag)
    {
        this.lag = lag;
    }

    //~ Methods private ---------------------------------------------------

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
    private void fill (char[][] table,
                       Rectangle box)
    {
        for (Section section : members) {
            section.fillTable(table, box);
        }
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    private void invalidateCache ()
    {
        centroid   = null;
        moments    = null;
        bounds     = null;
        contourBox = null;
    }

    //------------------//
    // getGlyphIndexAtX //
    //------------------//
    /**
     * Retrieve the index of the very first glyph in the provided ordered
     * list, whose left abscissa is equal or greater than the provided x
     * value.
     *
     * @param list the list to search, ordered by increasing abscissa
     * @param x the desired abscissa
     * @return the index of the first suitable glyph, or list.size() if no
     * such glyph can be found.
     */
    public static int getGlyphIndexAtX (List<? extends Glyph> list,
                                        int x)
    {
        int low = 0;
        int high = list.size()-1;
        while (low <= high) {
            int mid = (low + high) >> 1;
            int gx = list.get(mid).getContourBox().x;

            if (gx < x) {
                low = mid + 1;
            } else if (gx > x) {
                high = mid - 1;
            } else {
                // We are pointing to a glyph with desired x
                // Let's now pick the very first one
                for (mid = mid -1; mid >= 0; mid--) {
                    if (list.get(mid).getContourBox().x < x)
                        break;
                }

                return mid +1;
            }
        }

        return low;
    }
}
