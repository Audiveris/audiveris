//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        R e s t I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;

import omr.math.GeoUtil;

import omr.sheet.Scale;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.Measure;

import omr.sig.SIGraph;

import omr.util.HorizontalSide;
import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code RestInter} represents a rest.
 * TODO: Should be closer to AbstractNoteInter?
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "rest")
public class RestInter
        extends AbstractNoteInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(RestInter.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new RestInter object.
     *
     * @param glyph underlying glyph
     * @param shape rest shape
     * @param grade evaluation value
     * @param staff the related staff
     * @param pitch the rest pitch
     */
    public RestInter (Glyph glyph,
                      Shape shape,
                      double grade,
                      Staff staff,
                      double pitch)
    {
        super(glyph, null, shape, grade, staff, pitch);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private RestInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // create //
    //--------//
    /**
     * (Try to) create a Rest inter.
     * <p>
     * Most rests, whatever their shape, lie very close to staff middle line.
     * Rests can lie far from staff middle line only when they are horizontally inserted between
     * head-chords that also lie outside of staff height.
     * <p>
     * Also, a rest cannot be too close abscissa-wise to a head-chord.
     *
     * @param glyph            underlying glyph
     * @param shape            precise shape
     * @param grade            evaluation value
     * @param system           the related system
     * @param systemHeadChords abscissa-ordered list of head-chords in this system
     * @return the created instance or null if failed
     */
    public static RestInter create (Glyph glyph,
                                    Shape shape,
                                    double grade,
                                    SystemInfo system,
                                    List<Inter> systemHeadChords)
    {
        // Determine pitch according to glyph centroid
        final Point centroid = glyph.getCentroid();
        final Rectangle glyphBox = glyph.getBounds();

        // Rest positioning
        Staff restStaff = null;
        Double restPitch = null;
        List<Inter> measureChords = null;

        // First, check ordinate
        StaffLoop:
        for (final Staff staff : system.getStavesAround(centroid)) {
            final Measure measure = staff.getPart().getMeasureAt(centroid);

            if (measure == null) {
                continue;
            }

            // All head-chords in measure
            final int left = measure.getAbscissa(HorizontalSide.LEFT, staff);
            final int right = measure.getAbscissa(HorizontalSide.RIGHT, staff);
            measureChords = SIGraph.inters(
                    systemHeadChords,
                    new Predicate<Inter>()
            {
                @Override
                public boolean check (Inter inter)
                {
                    if (inter.getStaff() != staff) {
                        return false;
                    }

                    final Point center = inter.getCenter();

                    return (center.x >= left) && (center.x <= right);
                }
            });

            // Pitch value WRT staff
            final double measuredPitch = staff.pitchPositionOf(centroid);

            // Good rest, close to staff middle?
            if (Math.abs(measuredPitch) <= constants.suspiciousPitchPosition.getValue()) {
                //return new RestInter(glyph, shape, grade, staff, measuredPitch);
                restStaff = staff;
                restPitch = measuredPitch;

                break;
            }

            // Not so good rest, look for head chords nearby (in the same staff measure)
            for (Inter inter : measureChords) {
                if (GeoUtil.yOverlap(inter.getBounds(), glyphBox) > 0) {
                    restStaff = staff;
                    restPitch = measuredPitch;

                    break StaffLoop;
                }
            }
        }

        // Check vertical position is OK
        if (restStaff == null) {
            if (glyph.isVip() || logger.isDebugEnabled()) {
                logger.info("Discarded isolated rest candidate glyph#{}", glyph.getId());
            }

            return null; // Failure
        }

        // Check horizontal position WRT head-chords
        final int minDx = system.getSheet().getScale().toPixels(constants.minInterChordDx);
        final Point restCenter = GeoUtil.centerOf(glyphBox);
        final Rectangle fatBox = new Rectangle(glyphBox);
        fatBox.grow(minDx, 0);

        for (Inter chord : measureChords) {
            if (fatBox.intersects(chord.getBounds())) {
                int dx = chord.getCenter().x - restCenter.x;

                if (Math.abs(dx) < minDx) {
                    if (glyph.isVip() || logger.isDebugEnabled()) {
                        logger.info("Discarded stuck rest candidate glyph#{}", glyph.getId());
                    }

                    return null; // Failure
                }
            }
        }

        // Everything is OK
        RestInter restInter = new RestInter(glyph, shape, grade, restStaff, restPitch);
        restStaff.addNote(restInter);

        return restInter;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //----------//
    // getChord //
    //----------//
    @Override
    public RestChordInter getChord ()
    {
        return (RestChordInter) getEnsemble();
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + " " + shape;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Double suspiciousPitchPosition = new Constant.Double(
                "PitchPosition",
                2.0,
                "Maximum absolute pitch position for a rest to avoid additional checks");

        private final Scale.Fraction minInterChordDx = new Scale.Fraction(
                0.5,
                "Minimum horizontal delta between two chords");
    }
}
