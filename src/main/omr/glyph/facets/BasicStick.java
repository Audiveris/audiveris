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

import omr.log.Logger;

import omr.math.Line;

import omr.score.common.PixelPoint;

import omr.stick.StickSection;

import java.awt.*;
import java.lang.reflect.Constructor;

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
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BasicStick.class);

    //~ Instance fields --------------------------------------------------------

    /** GlyphAlignment facet */
    private final GlyphAlignment alignment;

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

    //------------//
    // BasicStick //
    //------------//
    /**
     * Create a stick with the interline value and specific alignment class
     * @param interline the very important scaling information
     * @param alignmentClass the specific alignment class
     */
    protected BasicStick (int                            interline,
                          Class<?extends GlyphAlignment> alignmentClass)
    {
        super(interline);

        GlyphAlignment theAlignment = null;

        try {
            Constructor constructor = alignmentClass.getConstructor(
                new Class[] { Glyph.class });
            theAlignment = (GlyphAlignment) constructor.newInstance(
                new Object[] { this });
        } catch (Exception ex) {
            logger.severe(
                "Cannot instantiate BasicStick with " + alignmentClass +
                " ex:" + ex);
        }

        addFacet(alignment = theAlignment);
    }

    //~ Methods ----------------------------------------------------------------

    public Line getAbsoluteLine ()
    {
        return alignment.getAbsoluteLine();
    }

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

    public void setEndingPoints (PixelPoint pStart,
                                 PixelPoint pStop)
    {
        alignment.setEndingPoints(pStart, pStop);
    }

    public boolean isExtensionOf (Stick other,
                                  int   maxDeltaCoord,
                                  int   maxDeltaPos)
    {
        return alignment.isExtensionOf(other, maxDeltaCoord, maxDeltaPos);
    }

    public int getFirstPos ()
    {
        return alignment.getFirstPos();
    }

    public int getFirstStuck ()
    {
        return alignment.getFirstStuck();
    }

    public int getIntPositionAt (double coord)
    {
        return alignment.getIntPositionAt(coord);
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

    public double getMeanDistance ()
    {
        return alignment.getMeanDistance();
    }

    public int getMidPos ()
    {
        return alignment.getMidPos();
    }

    public Line getOrientedLine ()
    {
        return alignment.getOrientedLine();
    }

    public double getPositionAt (double coord)
    {
        return alignment.getPositionAt(coord);
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

    public double getThicknessAt (int coord)
    {
        return alignment.getThicknessAt(coord);
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
        getOrientedLine()
            .includeLine(section.getLine());
    }

    public boolean overlapsWith (Stick other)
    {
        return alignment.overlapsWith(other);
    }

    public void renderLine (Graphics2D g)
    {
        alignment.renderLine(g);
    }

    //--------------//
    // getAlignment //
    //--------------//
    protected GlyphAlignment getAlignment ()
    {
        return alignment;
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

        //        if ((line != null) && (line.getNumberOfPoints() > 1)) {
        //            try {
        //                sb.append(" start[");
        //
        //                PixelPoint start = getStartPoint();
        //                sb.append(start.x)
        //                  .append(",")
        //                  .append(start.y);
        //            } catch (Exception ignored) {
        //                sb.append("INVALID");
        //            } finally {
        //                sb.append("]");
        //            }
        //
        //            try {
        //                sb.append(" stop[");
        //
        //                PixelPoint stop = getStopPoint();
        //                sb.append(stop.x)
        //                  .append(",")
        //                  .append(stop.y);
        //            } catch (Exception ignored) {
        //                sb.append("INVALID");
        //            } finally {
        //                sb.append("]");
        //            }
        //        }
        return sb.toString();
    }
}
