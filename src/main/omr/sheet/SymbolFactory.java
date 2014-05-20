//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S y m b o l F a c t o r y                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.glyph.Evaluation;
import omr.glyph.Shape;
import static omr.glyph.ShapeSet.Accidentals;
import static omr.glyph.ShapeSet.Clefs;
import static omr.glyph.ShapeSet.Digits;
import static omr.glyph.ShapeSet.Flags;
import static omr.glyph.ShapeSet.FlagsUp;
import static omr.glyph.ShapeSet.FullTimes;
import static omr.glyph.ShapeSet.PartialTimes;
import static omr.glyph.ShapeSet.Rests;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.lag.Section;

import omr.math.GeoOrder;
import omr.math.LineUtil;
import static omr.run.Orientation.VERTICAL;

import omr.sig.AbstractNoteInter;
import omr.sig.AccidNoteRelation;
import omr.sig.AccidentalInter;
import omr.sig.BarlineInter;
import omr.sig.BraceInter;
import omr.sig.ClefInter;
import omr.sig.FingeringInter;
import omr.sig.FlagInter;
import omr.sig.FlagStemRelation;
import omr.sig.Inter;
import omr.sig.NumberInter;
import omr.sig.RestInter;
import omr.sig.SIGraph;
import omr.sig.StemPortion;

import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;

/**
 * Class {@code SymbolFactory} generates the inter instances corresponding to
 * to an acceptable symbol evaluation in a given system.
 * <p>
 * (Generally there is one inter instance per evaluation, an exception is the case of full time
 * signature which leads to upper plus lower number instances).
 *
 * @author Hervé Bitteur
 */
public class SymbolFactory
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SymbolFactory.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** The related sheet. */
    @Navigable(false)
    private final Sheet sheet;

    /** Global scale. */
    private final Scale scale;

    /** The related SIG. */
    private final SIGraph sig;

    /** Scale-dependent global constants. */
    private final Parameters params;

    /** All system stems, ordered by abscissa. */
    private final List<Inter> systemStems;

    /** All system notes, ordered by abscissa. */
    private  List<Inter> systemNotes;

    /** All system rests, ordered by abscissa. */
    private  List<Inter> systemRests;

    /** All system bar lines, ordered by abscissa. */
    private  List<Inter> systemBars;

    /** Dot factory companion. */
    private final DotFactory dotFactory;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SymbolsFactory object.
     *
     * @param system the dedicated system
     */
    public SymbolFactory (SystemInfo system)
    {
        this.system = system;

        sig = system.getSig();
        sheet = system.getSheet();
        scale = sheet.getScale();
        params = new Parameters(scale);

        systemStems = sig.inters(Shape.STEM);
        Collections.sort(systemStems, Inter.byAbscissa);

        dotFactory = new DotFactory(this);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    /**
     * Create the proper inter instance(s) for the provided evaluated glyph.
     * <p>
     * TODO: method to be completed so that all inter classes are handled!!!!
     *
     * @param eval  evaluation result
     * @param glyph evaluated glyph
     * @param staff related staff
     */
    public void create (Evaluation eval,
                        Glyph glyph,
                        StaffInfo staff)
    {
        final Shape shape = eval.shape;
        final double grade = Inter.intrinsicRatio * eval.grade;

        if (glyph.isVip()) {
            logger.info("glyph#{} symbol created as {}", glyph.getId(), eval.shape);
        }

        if (Clefs.contains(shape)) {
            sig.addVertex(ClefInter.create(shape, glyph, grade, staff));
        } else if (Rests.contains(shape)) {
            sig.addVertex(RestInter.create(shape, glyph, grade));
        } else if (Accidentals.contains(shape)) {
            AccidentalInter accidInter = AccidentalInter.create(shape, glyph, grade);
            sig.addVertex(accidInter);
            detectAccidNoteRelation(accidInter);
        } else if (Flags.contains(shape)) {
            checkFlagStemRelation(FlagInter.create(shape, glyph, grade));
        } else if ((shape == Shape.BRACE) || (shape == Shape.BRACKET)) {
            sig.addVertex(BraceInter.create(shape, glyph, grade));
        } else if (PartialTimes.contains(shape)) {
            sig.addVertex(NumberInter.create(shape, glyph, grade));
        } else if (FullTimes.contains(shape)) {
            //            List<Inter> nd = TimeInter.create(shape, glyph, grade);
            //
            //            if (nd.size() > 1) {
            //                for (Inter inter : nd) {
            //                    sig.addVertex(inter);
            //                }
            //
            //                for (int i = 0; i < nd.size(); i++) {
            //                    Inter inter = nd.get(i);
            //
            //                    for (Inter other : nd.subList(i + 1, nd.size())) {
            //                        sig.addEdge(inter, other, new BasicSupport());
            //                    }
            //                }
            //            }
        } else if (Digits.contains(shape)) {
            sig.addVertex(FingeringInter.create(shape, glyph, grade));
        } else if (shape == Shape.DOT_set) {
            dotFactory.checkDot(eval, glyph);
        }
    }

    //-----------//
    // getSystem //
    //-----------//
    public SystemInfo getSystem ()
    {
        return system;
    }

    //---------------//
    // getSystemBars //
    //---------------//
    public List<Inter> getSystemBars ()
    {
        if (systemBars == null) {
            systemBars = sig.inters(BarlineInter.class);
            Collections.sort(systemBars, Inter.byAbscissa);
        }

        return systemBars;
    }

    //----------------//
    // getSystemNotes //
    //----------------//
    public List<Inter> getSystemNotes ()
    {
        if (systemNotes == null) {
            systemNotes = sig.inters(AbstractNoteInter.class);
            Collections.sort(systemNotes, Inter.byAbscissa);
        }

        return systemNotes;
    }

    //----------------//
    // getSystemRests //
    //----------------//
    public List<Inter> getSystemRests ()
    {
        if (systemRests == null) {
            systemRests = sig.inters(RestInter.class);
            Collections.sort(systemRests, Inter.byAbscissa);
        }

        return systemRests;
    }

    //-------------//
    // linkSymbols //
    //-------------//
    public void linkSymbols ()
    {
        dotFactory.pairRepeats();
        dotFactory.checkAugmentations();
    }

    //-----------------------//
    // checkFlagStemRelation //
    //-----------------------//
    /**
     * Check Flag/Stem adjacency for the provided flag and thus mutual support
     * (rather than exclusion).
     * <p>
     * If no stem is found at all, don't insert flag inter in sig.
     *
     * @param flag the provided flag
     */
    private void checkFlagStemRelation (FlagInter flag)
    {
        // Look for stems nearby, using the lowest (for up) or highest (for down) third of height
        final Shape shape = flag.getShape();
        final boolean isUp = FlagsUp.contains(shape);
        final int stemWidth = sheet.getMainStem();
        final Rectangle flagBox = flag.getBounds();
        final int height = (int) Math.rint(flagBox.height / 3.0);
        final int y = isUp ? ((flagBox.y + flagBox.height) - height - params.maxStemFlagGapY)
                : (flagBox.y + params.maxStemFlagGapY);

        // We need a flag ref point to compute x and y distances to stem
        final Glyph glyph = flag.getGlyph();
        final Section section = glyph.getFirstSection();
        final Point refPt = new Point(
                flagBox.x,
                isUp ? section.getStartCoord() : section.getStopCoord());
        final int midFlagY = (section.getStartCoord() + section.getStopCoord()) / 2;

        //TODO: -1 is used to cope with stem margin when erased (To be improved)
        final Rectangle box = new Rectangle((flagBox.x - 1) - stemWidth, y, stemWidth, height);
        glyph.addAttachment("fs", box);

        final List<Inter> stems = sig.intersectedInters(systemStems, GeoOrder.BY_ABSCISSA, box);
        boolean inserted = false;

        for (Inter stem : stems) {
            Glyph stemGlyph = stem.getGlyph();
            Point2D start = stemGlyph.getStartPoint(VERTICAL);
            Point2D stop = stemGlyph.getStopPoint(VERTICAL);
            Point2D crossPt = LineUtil.intersectionAtY(start, stop, refPt.getY());
            double xGap = refPt.getX() - crossPt.getX();
            double yGap;

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
                // Determine and check stem portion
                //TODO: this may be too strict, STEM_MIDDLE can happen with stack of flags?
                double midStemY = (start.getY() + stop.getY()) / 2;

                if (isUp) {
                    if (midFlagY > midStemY) {
                        fRel.setStemPortion(StemPortion.STEM_BOTTOM);
                    } else {
                        continue;
                    }
                } else {
                    if (midFlagY < midStemY) {
                        fRel.setStemPortion(StemPortion.STEM_TOP);
                    } else {
                        continue;
                    }
                }

                if (!inserted) {
                    sig.addVertex(flag);
                    inserted = true;
                }

                sig.addEdge(flag, stem, fRel);
            }
        }
    }

    //-------------------------//
    // detectAccidNoteRelation //
    //-------------------------//
    /**
     * Detect Accidental/Note relation for the provided accidental
     *
     * @param accid the provided accidental inter (sharp, flat or natural)
     */
    private void detectAccidNoteRelation (AccidentalInter accid)
    {
        // Look for notes nearby on the right side of accidental
        final int xGapMax = scale.toPixels(AccidNoteRelation.getXOutGapMaximum());
        final int yGapMax = scale.toPixels(AccidNoteRelation.getYGapMaximum());

        // Accid ref point is on accid right side and precise y depends on accid shape
        Shape shape = accid.getShape();
        Rectangle accidBox = accid.getBounds();
        Point accidPt = new Point(
                accidBox.x + accidBox.width,
                ((shape != Shape.FLAT) && (shape != Shape.DOUBLE_FLAT))
                ? (accidBox.y + (accidBox.height / 2))
                : (accidBox.y + ((3 * accidBox.height) / 4)));
        Rectangle luBox = new Rectangle(accidPt.x, accidPt.y - yGapMax, xGapMax, 2 * yGapMax);
        List<Inter> notes = sig.intersectedInters(getSystemNotes(), GeoOrder.BY_ABSCISSA, luBox);

        if (!notes.isEmpty()) {
            if (accid.getGlyph().isVip()) {
                logger.info("accid {} glyph#{} notes:{}", accid, accid.getGlyph().getId(), notes);
            }

            AccidNoteRelation bestRel = null;
            Inter bestNote = null;
            double bestYGap = Double.MAX_VALUE;

            for (Inter note : notes) {
                // Note ref point is on note left side and y is at note mid height
                // We are strict on pitch concordance (through yGapMax value)
                Point notePt = note.getCenterLeft();
                double xGap = notePt.x - accidPt.x;
                double yGap = Math.abs(notePt.y - accidPt.y);
                AccidNoteRelation rel = new AccidNoteRelation();
                rel.setDistances(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap));

                if (rel.getGrade() >= rel.getMinGrade()) {
                    if ((bestRel == null) || (bestYGap > yGap)) {
                        bestRel = rel;
                        bestNote = note;
                        bestYGap = yGap;
                    }
                }
            }

            if (bestRel != null) {
                sig.addEdge(accid, bestNote, bestRel);
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // Parameters //
    //------------//
    /**
     * Class {@code Parameters} gathers all pre-scaled constants.
     */
    private static class Parameters
    {
        //~ Instance fields ------------------------------------------------------------------------

        final int maxStemFlagGapY;

        //~ Constructors ---------------------------------------------------------------------------
        public Parameters (Scale scale)
        {
            maxStemFlagGapY = scale.toPixels(FlagStemRelation.getYGapMaximum());

            Main.dumping.dump(this);
        }
    }
}
