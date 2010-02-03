//----------------------------------------------------------------------------//
//                                                                            //
//                        B a s i c A l i g n m e n t                         //
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

import omr.lag.Run;

import omr.log.Logger;

import omr.math.BasicLine;
import omr.math.Line;

import omr.score.common.PixelPoint;

import omr.stick.StickSection;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Class {@code BasicAlignment} implements a basic handling of Alignment facet
 *
 * @author Hervé Bitteur
 */
class BasicAlignment
    extends BasicFacet
    implements GlyphAlignment
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BasicAlignment.class);

    //~ Instance fields --------------------------------------------------------

    /** Best line equation */
    private Line line;

    //~ Constructors -----------------------------------------------------------

    /**
     * Create a new BasicAlignment object
     *
     * @param glyph our glyph
     */
    public BasicAlignment (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // getAlienPixelsIn //
    //------------------//
    public int getAlienPixelsIn (Rectangle area)
    {
        int                      count = 0;
        final int                posMin = area.y;
        final int                posMax = (area.y + area.height) - 1;
        final List<GlyphSection> neighbors = glyph.getLag()
                                                  .getSectionsIn(area);

        for (GlyphSection section : neighbors) {
            // Keep only non-patch sections that are not part of the stick
            if (!section.isPatch() && (section.getGlyph() != glyph)) {
                int pos = section.getFirstPos() - 1; // Ordinate for horizontal,
                                                     // Abscissa for vertical

                for (Run run : section.getRuns()) {
                    pos++;

                    if (pos > posMax) {
                        break;
                    }

                    if (pos < posMin) {
                        continue;
                    }

                    int coordMin = Math.max(area.x, run.getStart());
                    int coordMax = Math.min(
                        (area.x + area.width) - 1,
                        run.getStop());

                    if (coordMax >= coordMin) {
                        count += (coordMax - coordMin + 1);
                    }
                }
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                "Stick" + glyph.getId() + " " + area + " getAlienPixelsIn=" +
                count);
        }

        return count;
    }

    //------------------//
    // getAliensAtStart //
    //------------------//
    public int getAliensAtStart (int dCoord,
                                 int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(
                getStart(),
                getStartingPos() - dPos,
                dCoord,
                2 * dPos));
    }

    //-----------------------//
    // getAliensAtStartFirst //
    //-----------------------//
    public int getAliensAtStartFirst (int dCoord,
                                      int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(getStart(), getStartingPos() - dPos, dCoord, dPos));
    }

    //----------------------//
    // getAliensAtStartLast //
    //----------------------//
    public int getAliensAtStartLast (int dCoord,
                                     int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(getStart(), getStartingPos(), dCoord, dPos));
    }

    //-----------------//
    // getAliensAtStop //
    //-----------------//
    public int getAliensAtStop (int dCoord,
                                int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(
                getStop() - dCoord,
                getStoppingPos() - dPos,
                dCoord,
                2 * dPos));
    }

    //----------------------//
    // getAliensAtStopFirst //
    //----------------------//
    public int getAliensAtStopFirst (int dCoord,
                                     int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(
                getStop() - dCoord,
                getStoppingPos() - dPos,
                dCoord,
                dPos));
    }

    //---------------------//
    // getAliensAtStopLast //
    //---------------------//
    public int getAliensAtStopLast (int dCoord,
                                    int dPos)
    {
        return getAlienPixelsIn(
            new Rectangle(getStop() - dCoord, getStoppingPos(), dCoord, dPos));
    }

    //-----------//
    // getAspect //
    //-----------//
    public double getAspect ()
    {
        return (double) getLength() / (double) getThickness();
    }

    //---------------//
    // isExtensionOf //
    //---------------//
    public boolean isExtensionOf (Stick  other,
                                  int    maxDeltaCoord,
                                  int    maxDeltaPos,
                                  double maxDeltaSlope)
    {
        // Check that a pair of start/stop is compatible
        if ((Math.abs(other.getStart() - getStop()) <= maxDeltaCoord) ||
            (Math.abs(other.getStop() - getStart()) <= maxDeltaCoord)) {
            // Check that a pair of positions is compatible
            if ((Math.abs(
                other.getLine().yAt(other.getStart()) -
                getLine().yAt(other.getStop())) <= maxDeltaPos) ||
                (Math.abs(
                other.getLine().yAt(other.getStop()) -
                getLine().yAt(other.getStart())) <= maxDeltaPos)) {
                // Check that slopes are compatible (a useless test ?)
                if (Math.abs(other.getLine().getSlope() - getLine().getSlope()) <= maxDeltaSlope) {
                    return true;
                } else if (logger.isFineEnabled()) {
                    logger.fine("isExtensionOf:  Incompatible slopes");
                }
            } else if (logger.isFineEnabled()) {
                logger.fine("isExtensionOf:  Incompatible positions");
            }
        } else if (logger.isFineEnabled()) {
            logger.fine("isExtensionOf:  Incompatible coordinates");
        }

        return false;
    }

    //-------------//
    // getFirstPos //
    //-------------//
    public int getFirstPos ()
    {
        return glyph.getBounds().y;
    }

    //---------------//
    // getFirstStuck //
    //---------------//
    public int getFirstStuck ()
    {
        int stuck = 0;

        for (GlyphSection section : glyph.getMembers()) {
            Run sectionRun = section.getFirstRun();

            for (GlyphSection sct : section.getSources()) {
                if (!sct.isGlyphMember() || (sct.getGlyph() != glyph)) {
                    stuck += sectionRun.getCommonLength(sct.getLastRun());
                }
            }
        }

        return stuck;
    }

    //------------//
    // getLastPos //
    //------------//
    public int getLastPos ()
    {
        return (getFirstPos() + getThickness()) - 1;
    }

    //--------------//
    // getLastStuck //
    //--------------//
    public int getLastStuck ()
    {
        int stuck = 0;

        for (GlyphSection section : glyph.getMembers()) {
            Run sectionRun = section.getLastRun();

            for (GlyphSection sct : section.getTargets()) {
                if (!sct.isGlyphMember() || (sct.getGlyph() != glyph)) {
                    stuck += sectionRun.getCommonLength(sct.getFirstRun());
                }
            }
        }

        return stuck;
    }

    //-----------//
    // getLength //
    //-----------//
    public int getLength ()
    {
        return glyph.getBounds().width;
    }

    //---------//
    // getLine //
    //---------//
    public Line getLine ()
    {
        if (line == null) {
            computeLine();
        }

        return line;
    }

    //-----------//
    // getMidPos //
    //-----------//
    public int getMidPos ()
    {
        if (getLine()
                .isVertical()) {
            // Fall back value
            return (int) Math.rint((getFirstPos() + getLastPos()) / 2.0);
        } else {
            return (int) Math.rint(
                getLine().yAt((getStart() + getStop()) / 2.0));
        }
    }

    //----------//
    // getStart //
    //----------//
    public int getStart ()
    {
        return glyph.getBounds().x;
    }

    //---------------//
    // getStartPoint //
    //---------------//
    public PixelPoint getStartPoint ()
    {
        Point start = glyph.getLag()
                           .switchRef(
            new Point(getStart(), line.yAt(getStart())),
            null);

        return new PixelPoint(start.x, start.y);
    }

    //----------------//
    // getStartingPos //
    //----------------//
    public int getStartingPos ()
    {
        if ((getThickness() >= 2) && !getLine()
                                          .isVertical()) {
            return getLine()
                       .yAt(getStart());
        } else {
            return getFirstPos() + (getThickness() / 2);
        }
    }

    //---------//
    // getStop //
    //---------//
    public int getStop ()
    {
        return (getStart() + getLength()) - 1;
    }

    //--------------//
    // getStopPoint //
    //--------------//
    public PixelPoint getStopPoint ()
    {
        Point stop = glyph.getLag()
                          .switchRef(
            new Point(getStop(), line.yAt(getStop())),
            null);

        return new PixelPoint(stop.x, stop.y);
    }

    //----------------//
    // getStoppingPos //
    //----------------//
    public int getStoppingPos ()
    {
        if ((getThickness() >= 2) && !getLine()
                                          .isVertical()) {
            return getLine()
                       .yAt(getStop());
        } else {
            return getFirstPos() + (getThickness() / 2);
        }
    }

    //--------------//
    // getThickness //
    //--------------//
    public int getThickness ()
    {
        return glyph.getBounds().height;
    }

    //-------------//
    // computeLine //
    //-------------//
    public void computeLine ()
    {
        line = new BasicLine();

        for (GlyphSection section : glyph.getMembers()) {
            StickSection ss = (StickSection) section;
            line.includeLine(ss.getLine());
        }

        if (logger.isFineEnabled()) {
            logger.fine(
                line + " pointNb=" + line.getNumberOfPoints() +
                " meanDistance=" + (float) line.getMeanDistance());
        }
    }

    //--------------//
    // overlapsWith //
    //--------------//
    public boolean overlapsWith (Stick other)
    {
        return Math.max(getStart(), other.getStart()) < Math.min(
            getStop(),
            other.getStop());
    }

    //------------//
    // renderLine //
    //------------//
    public void renderLine (Graphics g)
    {
        if (glyph.getContourBox()
                 .intersects(g.getClipBounds())) {
            getLine(); // To make sure the line has been computed

            Point start = glyph.getLag()
                               .switchRef(
                new Point(
                    getStart(),
                    (int) Math.rint(line.yAt((double) getStart()))),
                null);
            Point stop = glyph.getLag()
                              .switchRef(
                new Point(
                    getStop() + 1,
                    (int) Math.rint(line.yAt((double) getStop() + 1))),
                null);
            g.drawLine(start.x, start.y, stop.x, stop.y);
        }
    }

    //    //------//
    //    // dump //
    //    //------//
    //    /**
    //     * Print out glyph internal data
    //     */
    //    @Override
    //    public void dump ()
    //    {
    //        super.dump();
    //        System.out.println("   line=" + getLine());
    //    }
    //
    //    //------//
    //    // dump //
    //    //------//
    //    /**
    //     * Dump the stick as well as its contained sections is so desired
    //     *
    //     * @param withContent Flag to specify the dump of contained sections
    //     */
    //    public void dump (boolean withContent)
    //    {
    //        if (withContent) {
    //            System.out.println();
    //        }
    //
    //        StringBuilder sb = new StringBuilder(toString());
    //
    //        if (line != null) {
    //            sb.append(" pointNb=")
    //              .append(line.getNumberOfPoints());
    //        }
    //
    //        sb.append(" start=")
    //          .append(getStart());
    //        sb.append(" stop=")
    //          .append(getStop());
    //        sb.append(" midPos=")
    //          .append(getMidPos());
    //        System.out.println(sb);
    //
    //        if (withContent) {
    //            System.out.println("-members:" + getMembers().size());
    //
    //            for (GlyphSection sct : getMembers()) {
    //                System.out.println(" " + sct.toString());
    //            }
    //        }
    //    }

    //----------//
    // toString //
    //----------//
    /**
     * A readable image of the Stick
     *
     * @return The image string
     */
    @Override
    public String toString ()
    {
        StringBuffer sb = new StringBuffer(256);
        sb.append(super.toString());

        if (glyph.getResult() != null) {
            sb.append(" ")
              .append(glyph.getResult());
        }

        if (!glyph.getMembers()
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

        if (this.getClass()
                .getName()
                .equals(Stick.class.getName())) {
            sb.append("}");
        }

        return sb.toString();
    }
}
