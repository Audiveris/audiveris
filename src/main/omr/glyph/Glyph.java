//-----------------------------------------------------------------------//
//                                                                       //
//                               G l y p h                               //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.glyph;

import omr.check.Checkable;
import omr.check.Result;
import omr.lag.Lag;
import omr.lag.Section;
import omr.lag.SectionView;
import omr.math.Moments;
import omr.sheet.Picture;
import omr.util.Logger;
import omr.ui.Zoom;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>Glyph</code> represents any glyph found, such as stem,
 * ledger, accidental, note head, etc...
 *
 * <p> Collections of glyphs (generally at system level) are sorted first
 * by abscissa then by ordinate. To cope with glyphs nearly vertically
 * aligned, and which would thus be too dependent on the precise x value,
 * the y value could be used instead. But this would lead to non transitive
 * comparisons...
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

    /**  Centroid coordinates (within the system, in units?) */
    protected Point centroid;

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

    /** Interline of the containing stave (or sheet) */
    protected int interline;

    /** Within system width ? */
    protected boolean isWithinSystem;

    /** Has a ledger nearby ? */
    protected boolean hasLedger;

    /** Number of stems it is connected to (0, 1, 2) */
    protected int stemNumber;

    /** Position with respect to nearest staff */
    protected double stepLine;

    /** Computed moments of this glyph */
    protected Moments moments;

    //~ Constructors ------------------------------------------------------

    //-------//
    // Glyph // Needed for Class.newInstance method
    //-------//
    public Glyph()
    {
    }

    //-------//
    // Glyph // Needed for Castor
    //-------//
    public Glyph (int id)
    {
        setId(id);
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
    public int compareTo (Glyph other)
    {
        // Are x values different?
        int dx = centroid.x - other.centroid.x;

        if (dx != 0) {
            return dx;
        }

        // Glyphs are vertically aligned, so use ordinates
        return centroid.y - other.centroid.y;
    }

    //----------------//
    // computeMoments //
    //----------------//
    public void computeMoments ()
    {
        // First cumulate point from member sections
        int weight = getWeight();

        int[] coord = new int[weight];
        int[] pos = new int[weight];
        int nb = 0;

        // Append recursively all points
        nb = cumulatePoints(coord, pos, nb);

        // Then compute the moments, swapping pos & coord since the lag is
        // vertical
        moments = new Moments(pos, coord, weight, interline);
    }

    //---------//
    // destroy //
    //---------//
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
     * Draws a basic representation of the glyph, using ascii characters
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
    public void dump()
    {
        // Temporary
        omr.util.Dumper.dump(this);
    }

    //-------------//
    // erasePixels //
    //-------------//
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
    public Point getCentroid()
    {
        if (centroid == null) {
            getMoments();
             // Very dangerous
            centroid = new Point((int) Math.rint(moments.k[17]),
                                 (int) Math.rint(moments.k[18]));
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
    public Shape getGuess ()
    {
        return guess;
    }

    //-------//
    // getId //
    //-------//
    public int getId ()
    {
        return id;
    }

    //--------------//
    // getInterline //
    //--------------//
    public int getInterline ()
    {
        return interline;
    }

    //--------//
    // getLag //
    //--------//
    public GlyphLag getLag ()
    {
        return lag;
    }

    //------------//
    // getMembers // for Castor
    //------------//
    public List<GlyphSection> getMembers ()
    {
        return members;
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
    public Shape getShape ()
    {
        return shape;
    }

    //-------------//
    // getStepLine //
    //-------------//
    public double getStepLine ()
    {
        return stepLine;
    }

    //----------------//
    // getStringShape //
    //----------------//
    public String getStringShape ()
    {
        if (shape != null) {
            return shape.toString();
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
        int weight = 0;

        for (GlyphSection section : members) {
            weight += section.getWeight();
        }

        return weight;
    }

    //-----------//
    // hasLedger //
    //-----------//
    public boolean hasLedger ()
    {
        return hasLedger;
    }

    //---------------//
    // getStemNumber //
    //---------------//
    public int getStemNumber ()
    {
        return stemNumber;
    }

    //---------//
    // isKnown //
    //---------//
    public boolean isKnown ()
    {
        Shape shape = getShape();
        return
            shape != null &&
            shape != Shape.NOISE &&
            shape != Shape.CLUTTER;
    }

    //----------------//
    // isWithinSystem //
    //----------------//
    public boolean isWithinSystem ()
    {
        return isWithinSystem;
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
            logger.error("removeSection " + section +
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

    //-----------//
    // setBounds //
    //-----------//
    /**
     * For Castor, to define the bounds of the glyph
     *
     * @param bounds the bounds of the glyph
     */
    public void setBounds (Rectangle bounds)
    {
        this.bounds = bounds;
    }

    //---------------//
    // setContourBox //
    //---------------//
    /**
     * For Castor, TBD
     */
    public void setContourBox (Rectangle contourBox)
    {
        this.contourBox = contourBox;
    }

    //----------//
    // setGuess //
    //----------//
    public void setGuess (Shape guess)
    {
        this.guess = guess;
    }

    //--------------//
    // setHasLedger //
    //--------------//
    public void setHasLedger (boolean hasLedger)
    {
        this.hasLedger = hasLedger;
    }

    //-------------//
    // setLeftStem //
    //-------------//
    public void setStemNumber (int stemNumber)
    {
        this.stemNumber = stemNumber;
    }

    //-------//
    // setId //
    //-------//
    public void setId (int id)
    {
        this.id = id;
    }

    //--------------//
    // setInterline //
    //--------------//
    public void setInterline (int interline)
    {
        this.interline = interline;
    }

    //-------------------//
    // setIsWithinSystem //
    //-------------------//
    public void setIsWithinSystem (boolean isWithinSystem)
    {
        this.isWithinSystem = isWithinSystem;
    }

    //------------//
    // setMembers // for Castor
    //------------//
    public void setMembers (List<GlyphSection> members)
    {
        this.members = members;
        invalidateCache();              // Useful?
    }

    //------------//
    // setMoments // for Castor
    //------------//
    public void setMoments (Moments moments)
    {
        this.moments = moments;
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
    public void setShape (Shape shape)
    {
        this.shape = shape;
    }

    //-------------//
    // setStepLine //
    //-------------//
    public void setStepLine (double stepLine)
    {
        this.stepLine = stepLine;
    }

    //----------------//
    // setStringShape //
    //----------------//
    public void setStringShape (String shape)
    {
        this.shape = Shape.valueOf(shape);
    }

    //----------//
    // toString //
    //----------//
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
            sb.append(" center=[").append(centroid.x).append(",");
            sb.append(centroid.y).append("]");
        }

        //         if (moments != null)
        //             sb.append(" moments=" + moments);

        if (this.getClass().getName() == Glyph.class.getName()) {
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
        centroid = null;
        moments = null;
        bounds = null;
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
