//----------------------------------------------------------------------------//
//                                                                            //
//                            B a s i c S t i c k                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.math.Line;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;

import omr.stick.StickSection;

import java.awt.*;

/**
 * Class {@code BasicStick} is the Stick implementation for a standard glyph
 * with focus on alignment
 *
 * @author Hervé Bitteur
 */
public class BasicStick
    extends BasicGlyph
    implements Stick
{
    //~ Instance fields --------------------------------------------------------

    /** GlyphAlignment facet */
    final GlyphAlignment alignment;

    /** Best line equation */
    protected Line line;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // BasicStick //
    //------------//
    /**
     * Create a stick with the related interline value
     * @param interline the very important scaling information
     */
    public BasicStick (int interline)
    {
        super(interline);
        addFacet(alignment = new BasicAlignment(this));
    }

    //~ Methods ----------------------------------------------------------------

    public int getAlienPixelsIn (Rectangle area)
    {
        return alignment.getAlienPixelsIn(area);
    }

    public int getAliensAtStart (int dCoord,
                                 int dPos)
    {
        return alignment.getAliensAtStart(dCoord, dPos);
    }

    public int getAliensAtStartFirst (int dCoord,
                                      int dPos)
    {
        return alignment.getAliensAtStartFirst(dCoord, dPos);
    }

    public int getAliensAtStartLast (int dCoord,
                                     int dPos)
    {
        return alignment.getAliensAtStartLast(dCoord, dPos);
    }

    public int getAliensAtStop (int dCoord,
                                int dPos)
    {
        return alignment.getAliensAtStop(dCoord, dPos);
    }

    public int getAliensAtStopFirst (int dCoord,
                                     int dPos)
    {
        return alignment.getAliensAtStopFirst(dCoord, dPos);
    }

    public int getAliensAtStopLast (int dCoord,
                                    int dPos)
    {
        return alignment.getAliensAtStopLast(dCoord, dPos);
    }

    public double getAspect ()
    {
        return alignment.getAspect();
    }

    public boolean isExtensionOf (Stick  other,
                                  int    maxDeltaCoord,
                                  int    maxDeltaPos,
                                  double maxDeltaSlope)
    {
        return alignment.isExtensionOf(
            other,
            maxDeltaCoord,
            maxDeltaPos,
            maxDeltaSlope);
    }

    public int getFirstPos ()
    {
        return alignment.getFirstPos();
    }

    public int getFirstStuck ()
    {
        return alignment.getFirstStuck();
    }

    public int getLastPos ()
    {
        return alignment.getLastPos();
    }

    public int getLastStuck ()
    {
        return alignment.getLastStuck();
    }

    public int getLength ()
    {
        return alignment.getLength();
    }

    public Line getLine ()
    {
        return alignment.getLine();
    }

    public int getMidPos ()
    {
        return alignment.getMidPos();
    }

    public int getStart ()
    {
        return alignment.getStart();
    }

    public PixelPoint getStartPoint ()
    {
        return alignment.getStartPoint();
    }

    public int getStartingPos ()
    {
        return alignment.getStartingPos();
    }

    public int getStop ()
    {
        return alignment.getStop();
    }

    public PixelPoint getStopPoint ()
    {
        return alignment.getStopPoint();
    }

    public int getStoppingPos ()
    {
        return alignment.getStoppingPos();
    }

    public int getThickness ()
    {
        return alignment.getThickness();
    }

    //------------//
    // addSection //
    //------------//
    /**
     * Add a section as a member of this stick.
     *
     * @param section The section to be included
     * @param link should the section point back to this stick?
     */
    public void addSection (StickSection section,
                            Linking      link)
    {
        super.addSection(section, link);

        // Include the section points
        getLine()
            .includeLine(section.getLine());
    }

    public void computeLine ()
    {
        alignment.computeLine();
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        super.invalidateCache();
        line = null;
    }

    public boolean overlapsWith (Stick other)
    {
        return alignment.overlapsWith(other);
    }

    public void renderLine (Graphics g)
    {
        alignment.renderLine(g);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, typically for inclusion
     * in a toString. The overriding methods, if any, should return a string
     * that begins with a " " followed by some content.
     *
     * @return the string of internals
     */
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(25);
        sb.append(super.internalsString());

        if (getResult() != null) {
            sb.append(" ")
              .append(getResult());
        }

        if (!getMembers()
                 .isEmpty()) {
            sb.append(" th=")
              .append(getThickness());
            sb.append(" lg=")
              .append(getLength());
            sb.append(" l/t=")
              .append(String.format("%.2f", getAspect()));
            sb.append(" fa=")
              .append((100 * getFirstStuck()) / getLength())
              .append("%");
            sb.append(" la=")
              .append((100 * getLastStuck()) / getLength())
              .append("%");
        }

        if ((line != null) && (line.getNumberOfPoints() > 1)) {
            try {
                sb.append(" start[");

                PixelPoint start = getStartPoint();
                sb.append(start.x)
                  .append(",")
                  .append(start.y);
            } catch (Exception ignored) {
                sb.append("INVALID");
            } finally {
                sb.append("]");
            }

            try {
                sb.append(" stop[");

                PixelPoint stop = getStopPoint();
                sb.append(stop.x)
                  .append(",")
                  .append(stop.y);
            } catch (Exception ignored) {
                sb.append("INVALID");
            } finally {
                sb.append("]");
            }
        }

        return sb.toString();
    }
}
