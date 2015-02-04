//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        F l a g I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;
import static omr.glyph.ShapeSet.FlagsUp;
import omr.glyph.facets.Glyph;

import omr.lag.Section;

import omr.math.GeoOrder;
import omr.math.LineUtil;
import static omr.run.Orientation.VERTICAL;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;
import omr.sheet.Voice;

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
 * Class {@code FlagInter} represents one or several flags.
 *
 * @author Hervé Bitteur
 */
public class FlagInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(FlagInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Value of this flag (compound?) in terms of individual flags. */
    private final int value;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new FlagInter object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     */
    private FlagInter (Glyph glyph,
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
     * (Try to) create a Flag inter.
     * A flag is created only if it can be related to one or several stems.
     *
     * @param glyph       the flag glyph
     * @param shape       flag shape
     * @param grade       the interpretation quality
     * @param system      the related system
     * @param systemStems ordered collection of stems in system
     * @return the created instance or null
     */
    public static FlagInter create (Glyph glyph,
                                    Shape shape,
                                    double grade,
                                    SystemInfo system,
                                    List<Inter> systemStems)
    {
        FlagInter flag = null;
        final SIGraph sig = system.getSig();
        final Scale scale = system.getSheet().getScale();
        final int maxStemFlagGapY = scale.toPixels(FlagStemRelation.getYGapMaximum());

        // Look for stems nearby, using the lowest (for up) or highest (for down) third of height
        final boolean isFlagUp = FlagsUp.contains(shape);
        final int stemWidth = system.getSheet().getMaxStem();
        final Rectangle flagBox = glyph.getBounds();
        final int height = (int) Math.rint(flagBox.height / 3.0);
        final int y = isFlagUp ? ((flagBox.y + flagBox.height) - height - maxStemFlagGapY)
                : (flagBox.y + maxStemFlagGapY);

        // We need a flag ref point to compute x and y distances to stem
        final Section section = glyph.getFirstSection();
        final Point refPt = new Point(
                flagBox.x,
                isFlagUp ? section.getStartCoord() : section.getStopCoord());
        final int midFlagY = (section.getStartCoord() + section.getStopCoord()) / 2;

        //TODO: -1 is used to cope with stem margin when erased (To be improved)
        final Rectangle luBox = new Rectangle((flagBox.x - 1) - stemWidth, y, stemWidth, height);
        glyph.addAttachment("fs", luBox);

        final List<Inter> stems = sig.intersectedInters(systemStems, GeoOrder.BY_ABSCISSA, luBox);

        for (Inter stem : stems) {
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
                double midStemY = (start.getY() + stop.getY()) / 2;

                if (isFlagUp) {
                    if (midFlagY <= midStemY) {
                        continue;
                    }
                } else {
                    if (midFlagY >= midStemY) {
                        continue;
                    }
                }

                if (flag == null) {
                    sig.addVertex(flag = new FlagInter(glyph, shape, grade));
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
