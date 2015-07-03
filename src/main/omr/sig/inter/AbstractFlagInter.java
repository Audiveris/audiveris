//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t F l a g I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;
import static omr.glyph.ShapeSet.FlagsUp;
import static omr.glyph.ShapeSet.SmallFlags;
import omr.glyph.facets.Glyph;

import omr.math.GeoOrder;
import omr.math.LineUtil;
import static omr.run.Orientation.VERTICAL;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.Voice;

import omr.sig.SIGraph;
import omr.sig.relation.FlagStemRelation;
import omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Class {@code AbstractFlagInter} is the basis for (standard) FlagInter as well as
 * SmallFlagInter for grace notes.
 *
 * @author Hervé Bitteur
 */
public class AbstractFlagInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractFlagInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Value of this flag (compound?) in terms of individual flags. */
    protected final int value;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AbstractFlagInter} object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    protected AbstractFlagInter (Glyph glyph,
                                 Shape shape,
                                 double grade)
    {
        super(glyph, null, shape, grade);
        value = getFlagValue(shape);
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

    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a Flag inter (either standard FlagInter or SmallFlagInter).
     * <p>
     * At this time note heads have already been validated (with their attached stem).
     * So, a flag is created only if it can be related to stems with standard size head(s),
     * and if it is correctly located WRT note heads on the stem.
     *
     * @param glyph       the flag glyph
     * @param shape       flag shape
     * @param grade       the interpretation quality
     * @param system      the related system
     * @param systemStems ordered collection of stems in system
     * @return the created instance or null
     */
    public static AbstractFlagInter create (Glyph glyph,
                                            Shape shape,
                                            double grade,
                                            SystemInfo system,
                                            List<Inter> systemStems)
    {
        AbstractFlagInter flag = null;
        final SIGraph sig = system.getSig();
        final Scale scale = system.getSheet().getScale();
        final int maxStemFlagGapY = scale.toPixels(FlagStemRelation.getYGapMaximum());

        // Look for stems nearby, using the lowest (for up) or highest (for down) third of height
        final boolean isFlagUp = FlagsUp.contains(shape);
        final boolean isSmall = SmallFlags.contains(shape);
        final int stemWidth = system.getSheet().getScale().getMaxStem();
        final Rectangle flagBox = glyph.getBounds();
        final int footHeight = (int) Math.rint(flagBox.height / 3.0);

        // We need a flag ref point to compute x and y distances to stem
        final Point refPt = new Point(
                flagBox.x,
                isFlagUp ? ((flagBox.y + flagBox.height) - footHeight) : (flagBox.y + footHeight));
        final int y = isFlagUp ? ((flagBox.y + flagBox.height) - footHeight
                                  - maxStemFlagGapY) : (flagBox.y + maxStemFlagGapY);
        final int midFootY = isFlagUp ? (refPt.y + (footHeight / 2))
                : (refPt.y - (footHeight / 2));

        //TODO: -1 is used to cope with stem margin when erased (To be improved)
        final Rectangle luBox = new Rectangle(
                (flagBox.x - 1) - stemWidth,
                y,
                2 * stemWidth,
                footHeight);
        glyph.addAttachment("fs", luBox);

        List<Inter> stems = SIGraph.intersectedInters(systemStems, GeoOrder.BY_ABSCISSA, luBox);

        for (Inter inter : stems) {
            StemInter stem = (StemInter) inter;

            // Make sure stem is linked to consistent head size (small flag w/ small head)
            if (stem.isGraceStem() != isSmall) {
                continue;
            }

            Glyph stemGlyph = stem.getGlyph();
            Point2D start = stemGlyph.getStartPoint(VERTICAL);
            Point2D stop = stemGlyph.getStopPoint(VERTICAL);
            Point2D crossPt = LineUtil.intersectionAtY(start, stop, refPt.getY());
            final double xGap = refPt.getX() - crossPt.getX();
            final double yGap;

            if (refPt.getY() < start.getY()) {
                yGap = start.getY() - refPt.getY();
            } else if (refPt.getY() > stop.getY()) {
                yGap = refPt.getY() - stop.getY();
            } else {
                yGap = 0;
            }

            FlagStemRelation fRel = new FlagStemRelation();
            fRel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

            if (fRel.getGrade() >= fRel.getMinGrade()) {
                fRel.setAnchorPoint(
                        LineUtil.intersectionAtY(
                                start,
                                stop,
                                isFlagUp ? ((flagBox.y + flagBox.height) - 1) : flagBox.y));

                // Check consistency between flag direction and vertical position on stem
                // As well as stem direction as indicated by heads on stem
                double midStemY = (start.getY() + stop.getY()) / 2;

                if (isFlagUp) {
                    if (midFootY <= midStemY) {
                        continue;
                    }

                    if (stem.getDirection() == -1) {
                        continue;
                    }
                } else {
                    if (midFootY >= midStemY) {
                        continue;
                    }

                    if (stem.getDirection() == 1) {
                        continue;
                    }
                }

                if (flag == null) {
                    flag = isSmall ? new SmallFlagInter(glyph, shape, grade)
                            : new FlagInter(glyph, shape, grade);
                    sig.addVertex(flag);
                }

                sig.addEdge(flag, stem, fRel);

                if (flag.getStaff() == null) {
                    flag.setStaff(stem.getStaff());
                } else if (flag.getStaff() != stem.getStaff()) {
                    logger.warn("Different staves for {} & {}", flag, stem);
                }
            }
        }

        return flag;
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
        switch (shape) {
        case FLAG_1:
        case FLAG_1_UP:
        case SMALL_FLAG:
        case SMALL_FLAG_SLASH:
            return 1;

        case FLAG_2:
        case FLAG_2_UP:
            return 2;

        case FLAG_3:
        case FLAG_3_UP:
            return 3;

        case FLAG_4:
        case FLAG_4_UP:
            return 4;

        case FLAG_5:
        case FLAG_5_UP:
            return 5;
        }

        logger.error("Illegal flag shape: {}", shape);

        return 0;
    }
}
