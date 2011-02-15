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

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
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

    //-----------------//
    // getAbsoluteLine //
    //-----------------//
    public Line getAbsoluteLine ()
    {
        return glyph.getLag()
                    .switchRef(getOrientedLine());
    }

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
                other.getOrientedLine().yAt(other.getStart()) -
                getOrientedLine().yAt(other.getStop())) <= maxDeltaPos) ||
                (Math.abs(
                other.getOrientedLine().yAt(other.getStop()) -
                getOrientedLine().yAt(other.getStart())) <= maxDeltaPos)) {
                // Check that slopes are compatible (a useless test ?)
                if (Math.abs(
                    other.getOrientedLine().getSlope() -
                    getOrientedLine().getSlope()) <= maxDeltaSlope) {
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

    //-----------//
    // getMidPos //
    //-----------//
    public int getMidPos ()
    {
        if (getOrientedLine()
                .isVertical()) {
            // Fall back value
            return (int) Math.rint((getFirstPos() + getLastPos()) / 2.0);
        } else {
            return (int) Math.rint(
                getOrientedLine().yAt((getStart() + getStop()) / 2.0));
        }
    }

    //-----------------//
    // getOrientedLine //
    //-----------------//
    public Line getOrientedLine ()
    {
        if (line == null) {
            computeLine();
        }

        return line;
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
        if ((getThickness() >= 2) && !getOrientedLine()
                                          .isVertical()) {
            return getOrientedLine()
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
        if ((getThickness() >= 2) && !getOrientedLine()
                                          .isVertical()) {
            return getOrientedLine()
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

    //------//
    // dump //
    //------//
    /**
     * Print out glyph internal data
     */
    @Override
    public void dump ()
    {
        super.dump();
        System.out.println("   line=" + getOrientedLine());
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
    public void renderLine (Graphics2D g)
    {
        if (glyph.getContourBox()
                 .intersects(g.getClipBounds())) {
            getOrientedLine(); // To make sure the line has been computed

            int    halfLine = 0;
            Stroke stroke = g.getStroke();

            if (stroke instanceof BasicStroke) {
                halfLine = (int) Math.rint(
                    ((BasicStroke) stroke).getLineWidth() / 2);
            }

            Point start = glyph.getLag()
                               .switchRef(
                new Point(
                    getStart() + halfLine,
                    (int) Math.rint(line.yAt((double) getStart()))),
                null);
            Point stop = glyph.getLag()
                              .switchRef(
                new Point(
                    (getStop() + 1) - halfLine,
                    (int) Math.rint(line.yAt((double) getStop() + 1))),
                null);
            g.drawLine(start.x, start.y, stop.x, stop.y);
        }
    }
}
