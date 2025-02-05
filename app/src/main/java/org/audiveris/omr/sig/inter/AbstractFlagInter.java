//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t F l a g I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.ShapeSet.FlagsUp;
import static org.audiveris.omr.glyph.ShapeSet.SmallFlagsUp;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class <code>AbstractFlagInter</code> is the basis for (standard) FlagInter as well as
 * SmallFlagInter for grace notes.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractFlagInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractFlagInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Value of this flag (compound?) in terms of individual flags.
     * (Lazily evaluated)
     */
    protected Integer value;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>AbstractFlagInter</code> object.
     */
    protected AbstractFlagInter ()
    {
    }

    /**
     * Creates a new <code>AbstractFlagInter</code> object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    protected AbstractFlagInter (Glyph glyph,
                                 Shape shape,
                                 Double grade)
    {
        super(glyph, null, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation)
    {
        // First call needed to get flag  bounds
        super.deriveFrom(symbol, sheet, font, dropLocation);

        if (staff != null) {
            // We try to snap abscissa on a suitable stem
            final Double x = getSnapAbscissa();

            if (x != null) {
                // Final call, now with refined dropLocation
                dropLocation.x = (int) Math.rint(x);
                super.deriveFrom(symbol, sheet, font, dropLocation);
            }
        }

        return true;
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //-----------------//
    // getSnapAbscissa //
    //-----------------//
    /**
     * Report the theoretical abscissa of flag center when correctly aligned with
     * a suitable stem.
     * <p>
     * Required properties: staff, shape, bounds
     *
     * @return the proper abscissa if any, null otherwise
     */
    private Double getSnapAbscissa ()
    {
        if (staff == null) {
            return null;
        }

        // Stems nearby?
        final Collection<Link> links = searchLinks(staff.getSystem());

        for (Link link : links) {
            // We can have at most one link, and on left side of flag
            StemInter stem = (StemInter) link.partner;
            double stemX = LineUtil.xAtY(stem.getMedian(), getCenter().y);
            double halfWidth = getBounds().width / 2.0;

            return stemX + halfWidth;
        }

        return null;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the count of individual flags represented by this inter shape.
     *
     * @return the corresponding count of individual flags
     */
    public int getValue ()
    {
        if (value == null) {
            value = getFlagValue(shape);
        }

        return value;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, FlagStemRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this Flag instance and a stem nearby.
     *
     * @param systemStems ordered collection of stems in system
     * @param profile     desired profile level
     * @return the link found or null
     */
    private Link lookupLink (List<Inter> systemStems,
                             int profile)
    {
        if (systemStems.isEmpty()) {
            return null;
        }

        final SystemInfo system = systemStems.get(0).getSig().getSystem();
        final Scale scale = system.getSheet().getScale();
        final int maxStemFlagGapY = scale.toPixels(FlagStemRelation.getYGapMaximum(profile));

        // Look for stems nearby, using the highest (for up) or lowest (for down) third of height
        final boolean isFlagUp = FlagsUp.contains(shape) || SmallFlagsUp.contains(shape);
        final boolean isSmall = shape.isSmallFlag();
        final int stemWidth = system.getSheet().getScale().getMaxStem();
        final Rectangle flagBox = getBounds();
        final int footHeight = (int) Math.rint(flagBox.height / 2.5);

        // We need a flag ref point to compute x and y distances to stem
        final Point refPt = new Point(
                flagBox.x,
                isFlagUp ? (flagBox.y + footHeight) : ((flagBox.y + flagBox.height) - footHeight));
        final int y = isFlagUp ? (flagBox.y + maxStemFlagGapY)
                : ((flagBox.y + flagBox.height) - footHeight - maxStemFlagGapY);
        final int midFootY = isFlagUp ? (refPt.y - (footHeight / 2)) : (refPt.y + (footHeight / 2));

        //TODO: -1 is used to cope with stem margin when erased (To be improved)
        final Rectangle luBox = new Rectangle(
                (flagBox.x - 1) - stemWidth,
                y,
                2 * stemWidth,
                footHeight);

        if (glyph != null) {
            glyph.addAttachment("fs", luBox);
        }

        final List<Inter> stems = Inters.intersectedInters(
                systemStems,
                GeoOrder.BY_ABSCISSA,
                luBox);

        for (Inter inter : stems) {
            StemInter stem = (StemInter) inter;

            // Make sure stem is linked to consistent head size (small flag w/ small head)
            if (stem.isGraceStem() != isSmall) {
                continue;
            }

            Point2D start = stem.getTop();
            Point2D stop = stem.getBottom();
            double crossX = LineUtil.xAtY(start, stop, refPt.getY());
            final double xGap = refPt.getX() - crossX;
            final double yGap;

            if (refPt.getY() < start.getY()) {
                yGap = start.getY() - refPt.getY();
            } else if (refPt.getY() > stop.getY()) {
                yGap = refPt.getY() - stop.getY();
            } else {
                yGap = 0;
            }

            FlagStemRelation fRel = new FlagStemRelation();
            fRel.setInOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), getProfile());

            if (fRel.getGrade() >= fRel.getMinGrade()) {
                fRel.setExtensionPoint(
                        LineUtil.intersectionAtY(
                                start,
                                stop,
                                isFlagUp ? flagBox.y : ((flagBox.y + flagBox.height) - 1)));

                // Check consistency between flag direction and vertical position on stem
                // As well as stem direction as indicated by heads on stem
                double midStemY = (start.getY() + stop.getY()) / 2;

                if (isFlagUp) {
                    if (midFootY >= midStemY) {
                        continue;
                    }

                    if (stem.computeDirection() == 1) {
                        continue;
                    }
                } else {
                    if (midFootY <= midStemY) {
                        continue;
                    }

                    if (stem.computeDirection() == -1) {
                        continue;
                    }
                }

                return new Link(stem, fRel, true);
            }
        }

        return null;
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final List<Inter> systemStems = system.getSig().inters(StemInter.class);
        Collections.sort(systemStems, Inters.byAbscissa);

        final int profile = Math.max(getProfile(), system.getProfile());
        final Link link = lookupLink(systemStems, profile);

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, FlagStemRelation.class);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create and add a Flag inter (either standard FlagInter or SmallFlagInter).
     * <p>
     * At this time note heads have already been validated (with their attached stem).
     * So, a flag is created only if it can be related to stems with consistent size head(s),
     * and if it is correctly located WRT note heads on the stem.
     *
     * @param glyph       the flag glyph
     * @param shape       flag shape
     * @param grade       the interpretation quality
     * @param system      the related system
     * @param systemStems ordered collection of stems in system
     * @return the created instance or null
     */
    public static AbstractFlagInter createValidAdded (Glyph glyph,
                                                      Shape shape,
                                                      double grade,
                                                      SystemInfo system,
                                                      List<Inter> systemStems)
    {
        final AbstractFlagInter flag = shape.isSmallFlag() ? new SmallFlagInter(glyph, shape, grade)
                : new FlagInter(glyph, shape, grade);
        final Link link = flag.lookupLink(systemStems, system.getProfile());

        if (link != null) {
            system.getSig().addVertex(flag);
            link.applyTo(flag);

            return flag;
        }

        return null;
    }

    //--------------//
    // getFlagValue //
    //--------------//
    /**
     * Report the number of individual flags that corresponds to the flag shape
     *
     * @param shape the given flag shape
     * @return the number of individual flags
     */
    private static int getFlagValue (Shape shape)
    {
        return switch (shape) {
            case FLAG_1, FLAG_1_DOWN, //
                    SMALL_FLAG, SMALL_FLAG_DOWN, SMALL_FLAG_SLASH, SMALL_FLAG_SLASH_DOWN -> 1;
            case FLAG_2, FLAG_2_DOWN -> 2;
            case FLAG_3, FLAG_3_DOWN -> 3;
            case FLAG_4, FLAG_4_DOWN -> 4;
            case FLAG_5, FLAG_5_DOWN -> 5;

            default -> {
                logger.error("Illegal flag shape: {}", shape);
                yield 0;
            }
        };
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a flag.
     * <p>
     * For a flag, we provide only one handle:
     * <ul>
     * <li>Middle handle, moving only vertically
     * </ul>
     */
    private static class Editor
            extends InterEditor
    {
        // Original data
        private final Rectangle originalBounds;

        // Latest data
        private final Rectangle latestBounds;

        public Editor (final AbstractFlagInter flag)
        {
            super(flag);

            originalBounds = flag.getBounds();
            latestBounds = flag.getBounds();

            // Middle handle: move vertically only
            handles.add(selectedHandle = new Handle(flag.getCenter())
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    if (dy == 0) {
                        return false;
                    }

                    // Data
                    latestBounds.y += (int) Math.rint(dy);

                    // Handle
                    for (Handle handle : handles) {
                        PointUtil.add(handle.getPoint(), 0, dy);
                    }

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            getInter().setBounds(latestBounds);
            super.doit(); // No more glyph
        }

        @Override
        public void undo ()
        {
            getInter().setBounds(originalBounds);
            super.undo();
        }
    }
}
