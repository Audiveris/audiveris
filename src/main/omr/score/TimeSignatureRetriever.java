//----------------------------------------------------------------------------//
//                                                                            //
//                T i m e S i g n a t u r e R e t r i e v e r                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.Evaluation;
import omr.glyph.GlyphNetwork;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Barline;
import omr.score.entity.Chord;
import omr.score.entity.Clef;
import omr.score.entity.KeySignature;
import omr.score.entity.Measure;
import omr.score.entity.Note;
import omr.score.entity.Page;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.visitor.AbstractScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import omr.util.Predicate;
import omr.util.TreeNode;
import omr.util.WrappedBoolean;

import java.util.Collection;

/**
 * Class <code>TimeSignatureRetriever</code> checks carefully the first
 * measure of each staff for a time signature.
 *
 * @author Hervé Bitteur
 */
public class TimeSignatureRetriever
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        TimeSignatureRetriever.class);

    /** Specific predicate to filter time sig shapes */
    private static final Predicate<Shape> timePredicate = new Predicate<Shape>() {
        public boolean check (Shape shape)
        {
            return ShapeRange.Times.contains(shape);
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** To flag a page modification */
    private final WrappedBoolean modified;

    //~ Constructors -----------------------------------------------------------

    //------------------------//
    // TimeSignatureRetriever //
    //------------------------//
    /**
     * Creates a new TimeSignatureRetriever object.
     * @param modified the output to set in case of modification
     */
    public TimeSignatureRetriever (WrappedBoolean modified)
    {
        this.modified = modified;
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // visit Page //
    //------------//
    /**
     * Page hierarchy (sole) entry point
     *
     * @param page the page to check
     * @return false
     */
    @Override
    public boolean visit (Page page)
    {
        try {
            // We simply consider the very first measure of every staff
            ScoreSystem system = page.getFirstSystem();
            Measure     firstMeasure = system.getFirstPart()
                                             .getFirstMeasure();

            // If we have some TS, then it's OK
            if (hasTimeSig(firstMeasure)) {
                return false;
            }

            // No TS found. Let's look where it would be, if there was one
            PixelRectangle roi = getRoi(firstMeasure);
            Scale          scale = system.getScale();
            int            timeSigWidth = scale.toPixels(
                constants.timeSigWidth);

            if (roi.width < timeSigWidth) {
                logger.info("No room for time sig: " + roi.width);

                return false;
            }

            int yOffset = scale.toPixels(constants.yOffset);

            for (Staff.SystemIterator sit = new Staff.SystemIterator(
                firstMeasure); sit.hasNext();) {
                Staff staff = sit.next();

                if (staff.isDummy()) {
                    continue;
                }

                // Define the inner box to intersect clef glyph(s)
                int            center = roi.x + (roi.width / 2);
                StaffInfo      staffInfo = staff.getInfo();
                PixelRectangle inner = new PixelRectangle(
                    center,
                    staffInfo.getFirstLine().yAt(center) +
                    (staffInfo.getHeight() / 2),
                    0,
                    0);
                inner.grow(
                    (timeSigWidth / 2),
                    (staffInfo.getHeight() / 2) - yOffset);

                // Draw the box, for visual debug
                SystemPart part = system.getPartAt(inner.getCenter());
                Barline    barline = part.getStartingBarline();
                Glyph      line = null;

                if (barline != null) {
                    line = Glyphs.firstOf(
                        barline.getGlyphs(),
                        Barline.linePredicate);

                    if (line != null) {
                        line.addAttachment(
                            "TimeSigInner#" + staff.getId(),
                            inner);
                    }
                }

                // We now must find a time sig out of these glyphs
                Collection<Glyph> glyphs = system.getInfo()
                                                 .lookupIntersectedGlyphs(
                    inner);

                if (checkTimeSig(glyphs, system)) {
                    modified.set(true);
                }
            }
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + page,
                ex);
        }

        return false; // No navigation
    }

    //--------//
    // getRoi //
    //--------//
    /**
     * Retrieve the free space where a time signature could be within the first
     * measure
     * @param firstMeasure the containing measure (whatever the part)
     * @return a rectangle to provide left and right bounds
     */
    private PixelRectangle getRoi (Measure firstMeasure)
    {
        ScoreSystem system = firstMeasure.getSystem();
        int         left = 0; // Min
        int         right = system.getTopLeft().x +
                            system.getDimension().width; // Max

        for (Staff.SystemIterator sit = new Staff.SystemIterator(firstMeasure);
             sit.hasNext();) {
            Staff staff = sit.next();

            if (staff.isDummy()) {
                continue;
            }

            int          staffId = staff.getId();
            Measure      measure = sit.getMeasure();

            // Before: clef? + key signature?
            KeySignature keySig = measure.getFirstMeasureKey(staffId);

            if (keySig != null) {
                PixelRectangle kBox = keySig.getBox();
                left = Math.max(left, kBox.x + kBox.width);
            } else {
                Clef clef = measure.getFirstMeasureClef(staffId);

                if (clef != null) {
                    PixelRectangle cBox = clef.getBox();
                    left = Math.max(left, cBox.x + cBox.width);
                }
            }

            // After: alteration? + chord?
            Chord chord = measure.getClosestChord(new PixelPoint(0, 0));

            for (TreeNode tn : chord.getNotes()) {
                Note  note = (Note) tn;
                Glyph accid = note.getAccidental();

                if (accid != null) {
                    right = Math.min(right, accid.getContourBox().x);
                } else {
                    right = Math.min(right, note.getBox().x);
                }
            }

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Staff:" + staffId + " left:" + left + " right:" + right);
            }
        }

        return new PixelRectangle(left, 0, right - left, 0);
    }

    //--------------//
    // checkTimeSig //
    //--------------//
    /**
     * Check that we are able to recognize a time signature from the provided
     * glyphs
     * @param glyphs the glyphs to build up the time sig
     * @param scoreSystem the containing system
     * @return true if the time sig has been recognized and inserted
     */
    private boolean checkTimeSig (Collection<Glyph> glyphs,
                                  ScoreSystem       scoreSystem)
    {
        SystemInfo system = scoreSystem.getInfo();
        Glyphs.purgeManuals(glyphs);

        if (glyphs.isEmpty()) {
            return false;
        }

        Glyph compound = system.buildTransientCompound(glyphs);
        system.computeGlyphFeatures(compound);

        // Check if a time sig appears in the top evaluations
        Evaluation vote = GlyphNetwork.getInstance()
                                      .topVote(
            compound,
            constants.timeSigMaxDoubt.getValue(),
            system,
            timePredicate);

        if (vote != null) {
            // We now have a time sig!
            if (logger.isFineEnabled()) {
                logger.fine(
                    vote.shape + " built from " + Glyphs.toString(glyphs));
            }

            compound = system.addGlyph(compound);
            compound.setShape(vote.shape, Evaluation.ALGORITHM);

            return true;
        } else {
            return false;
        }
    }

    //------------//
    // hasTimeSig //
    //------------//
    /**
     * Check whether the provided measure contains at least one explicit time
     * signature
     *
     * @param measure the provided measure (in fact we care only about the
     * measure id, regardless of the part)
     * @return true if a time sig exists in some staff of the measure
     */
    private boolean hasTimeSig (Measure measure)
    {
        for (Staff.SystemIterator sit = new Staff.SystemIterator(measure);
             sit.hasNext();) {
            Staff staff = sit.next();

            if (sit.getMeasure()
                   .getTimeSignature(staff) != null) {
                return true;
            }
        }

        return false;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction   timeSigWidth = new Scale.Fraction(
            2d,
            "Width of a time signature");
        Scale.Fraction   yOffset = new Scale.Fraction(
            0.5d,
            "Time signature vertical offset since staff line");
        Evaluation.Doubt timeSigMaxDoubt = new Evaluation.Doubt(
            300d,
            "Maximum doubt for time sig verification");
    }
}
