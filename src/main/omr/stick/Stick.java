//-----------------------------------------------------------------------//
//                                                                       //
//                               S t i c k                               //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.stick;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.lag.Lag;
import omr.lag.Run;
import omr.lag.SectionView;
import omr.math.Line;
import omr.util.Logger;
import omr.ui.Zoom;

import java.awt.*;
import java.util.List;

/**
 * Class <code>Stick</code> describes a stick, a special kind of glyph,
 * either horizontal or vertical, as an aggregation of sections. Besides
 * usual positions and coordinates, a stick exhibits its approximating Line
 * which is the least-square fitted line on all points contained in the
 * stick.
 *
 * <ul> <li> Staff lines, ledgers, alternate ends are examples of
 * horizontal sticks </li>
 *
 * <li> Bar lines, stems are examples of vertical sticks </li> </ul>
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Stick
        extends Glyph
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(Stick.class);

    //~ Instance variables ------------------------------------------------

    // Best line equation
    private Line line;

    //~ Constructors ------------------------------------------------------


    /**
     * (For Castor?) Create a stick with a provided id
     *
     * @param id the given stick id
     */
    public Stick (int id)
    {
        super(id);
    }

    /**
     * Default constructor
     */
    public Stick ()
    {
    }

    //~ Methods -----------------------------------------------------------

    //------------------//
    // getAlienPixelsIn //
    //------------------//

    /**
     * Report the number of pixels found in the specified rectangle that do
     * not belong to the stick.
     *
     * @param area the rectangular area to investigate, in (coord, pos)
     *             form of course!
     *
     * @return the number of alien pixels found
     */
    public int getAlienPixelsIn (Rectangle area)
    {
        int count = 0;
        final int posMin = area.y;
        final int posMax = (area.y + area.height) - 1;
        final List<GlyphSection> neighbors = lag.getSectionsIn(area);

        for (GlyphSection section : neighbors) {
            // Keep only sections that are not part of the stick
            if (section.getGlyph() != this) {
                int pos = section.getFirstPos() - 1; // Ordinate for horizontal, Abscissa for vertical

                for (Run run : section.getRuns()) {
                    pos++;

                    if (pos > posMax) {
                        break;
                    }

                    if (pos < posMin) {
                        continue;
                    }

                    int stop = run.getStop();
                    int coordMin = Math.max(area.x, run.getStart());
                    int coordMax = Math.min((area.x + area.width) - 1,
                                            run.getStop());

                    if (coordMax >= coordMin) {
                        count += (coordMax - coordMin + 1);
                    }
                }
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Stick" + id + " " + area + " getAlienPixelsIn="
                         + count);
        }

        return count;
    }

    //------------------//
    // getAliensAtStart //
    //------------------//

    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     * +-------+
     * |       |
     * +=======+==================================+
     * |       |
     * +-------+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    public int getAliensAtStart (int dCoord,
                                 int dPos)
    {
        return getAlienPixelsIn(new Rectangle(getStart(),
                                              getStartingPos() - dPos,
                                              dCoord, 2 * dPos));
    }

    //-----------------------//
    // getAliensAtStartFirst //
    //-----------------------//

    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     * +-------+
     * |       |
     * +=======+==================================+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    public int getAliensAtStartFirst (int dCoord,
                                      int dPos)
    {
        return getAlienPixelsIn(new Rectangle(getStart(),
                                              getStartingPos() - dPos,
                                              dCoord, dPos));
    }

    //----------------------//
    // getAliensAtStartLast //
    //----------------------//

    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     * +=======+==================================+
     * |       |
     * +-------+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    public int getAliensAtStartLast (int dCoord,
                                     int dPos)
    {
        return getAlienPixelsIn(new Rectangle(getStart(), getStartingPos(),
                                              dCoord, dPos));
    }

    //-----------------//
    // getAliensAtStop //
    //-----------------//

    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     *                                    +-------+
     *                                    |       |
     * +==================================+=======+
     *                                    |       |
     *                                    +-------+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    public int getAliensAtStop (int dCoord,
                                int dPos)
    {
        return getAlienPixelsIn(new Rectangle(getStop() - dCoord,
                                              getStoppingPos() - dPos,
                                              dCoord, 2 * dPos));
    }

    //----------------------//
    // getAliensAtStopFirst //
    //----------------------//

    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     *                                    +-------+
     *                                    |       |
     * +==================================+=======+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    public int getAliensAtStopFirst (int dCoord,
                                     int dPos)
    {
        return getAlienPixelsIn(new Rectangle(getStop() - dCoord,
                                              getStoppingPos() - dPos,
                                              dCoord, dPos));
    }

    //---------------------//
    // getAliensAtStopLast //
    //---------------------//

    /**
     * Count alien pixels in the following rectangle...
     * <pre>
     * +==================================+=======+
     *                                    |       |
     *                                    +-------+
     * </pre>
     *
     * @param dCoord rectangle size along stick length
     * @param dPos   retangle size along stick thickness
     *
     * @return the number of alien pixels found
     */
    public int getAliensAtStopLast (int dCoord,
                                    int dPos)
    {
        return getAlienPixelsIn(new Rectangle(getStop() - dCoord,
                                              getStoppingPos(), dCoord, dPos));
    }

    //-----------//
    // getAspect //
    //-----------//
    /**
     * Report the ratio of thickness over length
     *
     * @return the "fatness" of the stick
     */
    public double getAspect ()
    {
        return (double) getThickness() / (double) getLength();
    }

    //------------//
    // getDensity //
    //------------//

    /**
     * Report the density of the stick, that is its weight divided by the
     * area of its bounding rectangle
     *
     * @return the density
     */
    public double getDensity ()
    {
        Rectangle rect = getBounds();
        int surface = (rect.width + 1) * (rect.height + 1);

        return (double) getWeight() / (double) surface;
    }

    //-------------//
    // getFirstPos //
    //-------------//

    /**
     * Return the first position (ordinate for stick of horizontal
     * sections, abscissa for stick of vertical sections and runs)
     *
     * @return the position at the beginning
     */
    public int getFirstPos ()
    {
        return getBounds().y;
    }

    //---------------//
    // getFirstStuck //
    //---------------//
    /**
     * Compute the number of pixels stuck on first side of the stick
     *
     * @return the number of pixels
     */
    public int getFirstStuck ()
    {
        int stuck = 0;

        for (GlyphSection section : members) {
            for (GlyphSection sct : section.getSources()) {
                if (!sct.isMember() || (sct.getGlyph() != this)) {
                    stuck += sct.getLastRun().getLength();
                }
            }
        }

        return stuck;
    }

    //------------//
    // getLastPos //
    //------------//
    /**
     * Return the last position (maximum ordinate for a horizontal stick,
     * maximum abscissa for a vertical stick)
     *
     * @return the position at the end
     */
    public int getLastPos ()
    {
        return (getFirstPos() + getThickness()) - 1;
    }

    //--------------//
    // getLastStuck //
    //--------------//

    /**
     * Compute the nb of pixels stuck on last side of the stick
     *
     * @return the number of pixels
     */
    public int getLastStuck ()
    {
        int stuck = 0;

        for (GlyphSection section : members) {
            for (GlyphSection sct : section.getTargets()) {
                if (!sct.isMember() || (sct.getGlyph() != this)) {
                    stuck += sct.getFirstRun().getLength();
                }
            }
        }

        return stuck;
    }

    //-----------//
    // getLength //
    //-----------//
    /**
     * Report the length of the stick
     *
     * @return the stick length in pixels
     */
    public int getLength ()
    {
        return getBounds().width;
    }

    //---------//
    // getLine //
    //---------//

    /**
     * Return the approximating line computed on the stick.
     *
     * @return The line
     */
    public Line getLine ()
    {
        if (line == null) {
            computeLine();
        }

        return line;
    }

    //---------//
    // setLine //
    //---------//

    /**
     * For Castor
     *
     * @param line The line of the stick
     */
    public void setLine (Line line)
    {
        this.line = line;
    }

    //-----------//
    // getMidPos //
    //-----------//

    /**
     * Return the position (ordinate for horizontal stick, abscissa for
     * vertical stick) at the middle of the stick
     *
     * @return the position of the midle of the stick
     */
    public int getMidPos ()
    {
        return (int) Math.rint
            (getLine().yAt((double) (getStart() + getStop()) / 2));
    }

    //----------//
    // getStart //
    //----------//

    /**
     * Return the beginning of the stick (xmin for horizontal, ymin for
     * vertical)
     *
     * @return The starting coordinate
     */
    public int getStart ()
    {
        return getBounds().x;
    }

    //----------------//
    // getStartingPos //
    //----------------//

    /**
     * Return the best pos value at starting of the stick
     *
     * @return mean pos value at stick start
     */
    public int getStartingPos ()
    {
        if (getThickness() >= 2) {
            return getLine().yAt(getStart());
        } else {
            return getFirstPos() + (getThickness() / 2);
        }
    }

    //---------//
    // getStop //
    //---------//

    /**
     * Return the end of the stick (xmax for horizontal, ymax for vertical)
     *
     * @return The ending coordinate
     */
    public int getStop ()
    {
        return (getStart() + getLength()) - 1;
    }

    //----------------//
    // getStoppingPos //
    //----------------//

    /**
     * Return the best pos value at the stopping end of the stick
     *
     * @return mean pos value at stick stop
     */
    public int getStoppingPos ()
    {
        if (getThickness() >= 2) {
            return getLine().yAt(getStop());
        } else {
            return getFirstPos() + (getThickness() / 2);
        }
    }

    //--------------//
    // getThickness //
    //--------------//
    /**
     * Report the stick thickness
     *
     * @return the thickness in pixels
     */
    public int getThickness ()
    {
        return getBounds().height;
    }

    //------------//
    // addSection //
    //------------//
    /**
     * Add a section as a member of this stick.
     *
     * @param section The section to be included
     */
    public void addSection (StickSection section,
                            boolean      link)
    {
        super.addSection(section, /* link => */ true);

        // Include the section points
        getLine().include(section.getLine());
    }

    //----------//
    // colorize //
    //----------//

    /**
     * Set the display color of all sections that compose this stick.
     *
     * @param viewIndex index in the view list
     * @param color     color for the whole stick
     */
    public void colorize (Lag lag,
                          int viewIndex,
                          Color color)
    {
        if (lag == this.lag) {
            colorize(viewIndex, members, color);
        }
    }

    //----------//
    // colorize //
    //----------//

    /**
     * Set the display color of all sections gathered by the provided list
     *
     * @param viewIndex the proper view index
     * @param list      the collection of sections
     * @param color     the display color
     */
    public void colorize (int viewIndex,
                          List<GlyphSection> list,
                          Color color)
    {
        for (GlyphSection section : list) {
            SectionView view = (SectionView) section.getViews().get(viewIndex);
            view.setColor(color);
        }
    }

    //------------------//
    // computeDensities //
    //------------------//

    /**
     * Computes the densities around the stick mean line
     */
    public void computeDensities (int nWidth,
                                  int nHeight)
    {
        final int maxDist = 20; // TBD of course

        // Allocate and initialize density histograms
        int[] histoLeft = new int[maxDist];
        int[] histoRight = new int[maxDist];

        for (int i = maxDist - 1; i >= 0; i--) {
            histoLeft[i] = 0;
            histoRight[i] = 0;
        }

        // Compute (horizontal) distances
        Line line = getLine();

        for (GlyphSection section : members) {
            int pos = section.getFirstPos(); // Abscissa for vertical

            for (Run run : section.getRuns()) {
                int stop = run.getStop();

                for (int coord = run.getStart(); coord <= stop; coord++) {
                    int dist = (int) Math.rint(line.distanceOf(coord, pos));

                    if ((dist < 0) && (dist > -maxDist)) {
                        histoRight[-dist] += 1;
                    }

                    if ((dist >= 0) && (dist < maxDist)) {
                        histoLeft[dist] += 1;
                    }
                }

                pos++;
            }
        }

        System.out.println("computeDensities for Stick #" + id);

        int length = getLength();
        boolean started = false;
        boolean stopped = false;

        for (int i = maxDist - 1; i >= 0; i--) {
            if (histoLeft[i] != 0) {
                started = true;
            }

            if (started) {
                System.out.println(i + " : "
                                   + ((histoLeft[i] * 100) / length));
            }
        }

        for (int i = -1; i > -maxDist; i--) {
            if (histoRight[-i] == 0) {
                stopped = true;
            }

            if (!stopped) {
                System.out.println(i + " : "
                                   + ((histoRight[-i] * 100) / length));
            }
        }

        // Retrieve sections in the neighborhood
        Rectangle neighborhood = new Rectangle(getBounds());
        neighborhood.grow(nWidth, nHeight);

        List<GlyphSection> neighbors = lag.getSectionsIn(neighborhood);

        for (GlyphSection section : neighbors) {
            // Keep only sections that are not part of the stick
            if (section.getGlyph() != this) {
                System.out.println(section.toString());
            }
        }
    }

    //-------------//
    // computeLine //
    //-------------//

    /**
     * Computes the least-square fitted line among all the section points
     * of the stick.
     */
    public void computeLine ()
    {
        line = new Line();

        for (GlyphSection section : members) {
            StickSection ss = (StickSection) section;
            line.include(ss.getLine());
        }

        if (logger.isDebugEnabled()) {
            logger.debug(line + " pointNb=" + line.getPointNb()
                         + " meanDistance=" + (float) line.getMeanDistance());
        }
    }

    //------//
    // dump //
    //------//

    /**
     * Dump the stick as well as its contained sections is so desired
     *
     * @param withContent Flag to specify the dump of contained sections
     */
    public void dump (boolean withContent)
    {
        if (withContent) {
            System.out.println();
        }

        System.out.println(toString() + " pointNb=" + line.getPointNb()
                           + " start=" + getStart() + " stop=" + getStop()
                           + " midPos=" + getMidPos());

        if (withContent) {
            System.out.println("-members:" + members.size());

            for (GlyphSection sct : members) {
                System.out.println(" " + sct.toString());
            }
        }
    }

    //-------------//
    // renderChunk //
    //-------------//

    /**
     * Render the chunk area at each end of the stick
     *
     * @param g the graphic context
     * @param z the display zoom
     */
    public void renderChunk (Graphics g,
                             Zoom z,
                             int length,
                             int thickness)
    {
        Rectangle box = z.scaled(getContourBox());

        if (box.intersects(g.getClipBounds())) {
            Line line = getLine();
            Rectangle rect = new Rectangle();
            Rectangle rect1 = new Rectangle(z.scaled(getStart()),
                                            z.scaled(line.yAt(getStart())
                                                     - thickness),
                                            z.scaled(length),
                                            z.scaled(2 * thickness));
            lag.switchRef(rect1, rect);
            g.drawRect(rect.x, rect.y, rect.width, rect.height);

            Rectangle rect2 = new Rectangle(z.scaled((getStop() + 1) - length),
                                            z.scaled(line.yAt(getStop() + 1)
                                                     - thickness),
                                            z.scaled(length),
                                            z.scaled(2 * thickness));
            lag.switchRef(rect2, rect);
            g.drawRect(rect.x, rect.y, rect.width, rect.height);
        }
    }

    //------------//
    // renderLine //
    //------------//

    /**
     * Render the main guiding line of the stick, using the current
     * foreground color.
     *
     * @param g the graphic context
     * @param z the display zoom
     */
    public void renderLine (Graphics g,
                            Zoom z)
    {
        Rectangle box = z.scaled(getContourBox());

        if (box.intersects(g.getClipBounds())) {
            Line line = getLine();
            Point start = lag.switchRef
                    (new Point(z.scaled(getStart()),
                               z.scaled(line.yAt((double) getStart()) + 0.5)),
                     null);
            Point stop = lag.switchRef
                    (new Point(z.scaled(getStop() + 1),
                               z.scaled(line.yAt((double) getStop() + 1) + 0.5)),
                     null);
            g.drawLine(start.x, start.y, stop.x, stop.y);
        }
    }

    //----------//
    // toString //
    //----------//

    /**
     * A readable image of the Stick
     *
     * @return The image string
     */
    public String toString ()
    {
        StringBuffer sb = new StringBuffer(128);
        sb.append("{Stick#");
        sb.append(id);

        if (result != null) {
            sb.append(" ").append(result);
        }

        sb.append(" th=").append(getThickness());
        sb.append(" lg=").append(getLength());
        sb.append(" t/l=").append((int) (100 * getAspect())).append("%");
        sb.append(" fa=").append((100 * getFirstStuck()) / getLength())
                .append("%");
        sb.append(" la=").append((100 * getLastStuck()) / getLength())
                .append("%");
        sb.append("}");

        if (line != null) {
            sb.append(" ").append(line);
        }

        return sb.toString();
    }

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
        return "Stick";
    }
}
