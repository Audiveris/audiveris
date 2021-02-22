//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S t e m I t e m                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet.stem;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.stem.HeadLinker.SLinker.CLinker;
import org.audiveris.omr.sheet.stem.BeamLinker.BLinker;
import org.audiveris.omr.sheet.stem.BeamLinker.BLinker.VLinker;
import org.audiveris.omr.sig.inter.AbstractBeamInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class {@code StemItem} is an abstract class to formalize any item used when trying to
 * build a stem (glyph, beam-based linker, head-based linker, gap).
 *
 * @author Hervé Bitteur
 */
public abstract class StemItem
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(StemItem.class);

    // Trick to check equality of double values
    private static final double EPS = 0.01;

    //~ Instance fields ----------------------------------------------------------------------------
    /** Start and stop points, oriented top down. */
    final Line2D line;

    /** Seed or plain chunk or stump, if any. */
    final Glyph glyph;

    /** Positive height contribution to stem. */
    final int contrib;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a {@code StemItem} object.
     *
     * @param line    segment from top to bottom, can be degenerated to a single refPt
     * @param glyph   underlying glyph is any
     * @param contrib contribution to stem length
     */
    protected StemItem (Line2D line,
                        Glyph glyph,
                        int contrib)
    {
        this.line = line;
        this.glyph = glyph;
        this.contrib = contrib;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append('{');
        sb.append("Lg:").append(contrib);

        if (glyph != null) {
            sb.append(' ').append(glyph);
            sb.append(String.format(" %.1f-%.1f",
                                    glyph.getStartPoint(Orientation.VERTICAL).getY(),
                                    glyph.getStopPoint(Orientation.VERTICAL).getY()));
        }

        return sb.append('}').toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // GapItem //
    //---------//
    /**
     * A "virtual" StemItem to formalize a vertical gap in stem.
     */
    public static class GapItem
            extends StemItem
    {

        /**
         * Create a {@code GapItem} object;
         *
         * @param line the top down gap line
         */
        public GapItem (Line2D line)
        {
            super(line, null, line.getBounds().height);
        }

        @Override
        public boolean equals (Object obj)
        {
            if (obj instanceof GapItem) {
                final GapItem gap = (GapItem) obj;

                // We care about y values only
                return (Math.abs(gap.line.getY1() - this.line.getY1()) < EPS)
                               && (Math.abs(gap.line.getY2() - this.line.getY2()) < EPS);
            }

            return super.equals(obj);
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            return hash;
        }

        @Override
        public String toString ()
        {
            return new StringBuilder("GAP{").append(contrib)
                    .append(String.format(" %.1f-%.1f", line.getY1(), line.getY2()))
                    .append('}').toString();
        }
    }

    //-----------//
    // GlyphItem //
    //-----------//
    /**
     * A StemItem composed of a plain glyph.
     */
    public static class GlyphItem
            extends StemItem
    {

        /**
         * Create a {@code GlyphItem} object.
         *
         * @param glyph   underlying glyph (non null)
         * @param contrib positive contribution to stem length
         */
        public GlyphItem (Glyph glyph,
                          int contrib)
        {
            super(glyph.getCenterLine(), glyph, contrib);
        }

        @Override
        public boolean equals (Object obj)
        {
            if (obj instanceof GlyphItem) {
                final GlyphItem ge = (GlyphItem) obj;

                return ge.contrib == contrib && ge.glyph == glyph;
            }

            return super.equals(obj);
        }

        @Override
        public int hashCode ()
        {
            int hash = 7;
            return hash;
        }
    }

    //------------//
    // LinkerItem //
    //------------//
    /**
     * A StemItem composed of a beam or head linker.
     */
    public static class LinkerItem
            extends StemItem
    {

        /**
         * Related (head or beam) linker.
         * This can be:
         * <ul>
         * <li>A {@link HalfLinkerItem} to a beam {@link VLinker} or a head {@link CLinker}.
         * <li>A {@link LinkerItem} to a beam {@link BLinker}.
         * </ul>
         */
        final StemLinker linker;

        /**
         * Create a {@code LinkerItem} object.
         *
         * @param linker the underlying head or beam linker
         */
        public LinkerItem (StemLinker linker)
        {
            this(linker,
                 (linker.getStump() != null) ? linker.getStump().getBounds().height : 0);
        }

        public LinkerItem (StemLinker linker,
                           int contrib)
        {
            super(lineOf(linker),
                  linker.getStump(),
                  contrib);
            this.linker = linker;
        }

        @Override
        public String toString ()
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(getClass().getSimpleName()).append('{');
            sb.append("Lg:").append(contrib);

            sb.append(' ').append(linker);

            if ((linker instanceof StemHalfLinker) && (linker.getStump() != null)) {
                sb.append('/').append(linker.getStump());
            }

            return sb.append('}').toString();
        }

        private static Line2D lineOf (StemLinker linker)
        {
            final Glyph stump = linker.getStump();

            if (stump != null) {
                return stump.getCenterLine();
            }

            final Point2D refPt = linker.getReferencePoint();

            if (linker instanceof BLinker) { // Applies to VLinker as well
                AbstractBeamInter beam = ((BLinker) linker).getSource();
                final double halfHeight = beam.getHeight() / 2.0;
                final double rx = refPt.getX();
                final double ry = refPt.getY();
                return new Line2D.Double(rx, ry - halfHeight, rx, ry + halfHeight);
            }

            return new Line2D.Double(refPt, refPt);
        }
    }

    //----------------//
    // HalfLinkerItem //
    //----------------//
    /**
     * A LinkerItem pointing up or down.
     * <p>
     * Either a beam {@link VLinker} or a head {@link CLinker}
     */
    public static class HalfLinkerItem
            extends LinkerItem
    {

        public HalfLinkerItem (StemHalfLinker linker,
                               int contrib)
        {
            super(linker, contrib);
        }
    }
}
