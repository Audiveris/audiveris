//----------------------------------------------------------------------------//
//                                                                            //
//                                  B e a m                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;

import omr.math.BasicLine;
import omr.math.Line;

import omr.score.visitor.Visitor;

import omr.sheet.Scale;

import omr.stick.Stick;

import omr.util.Dumper;
import omr.util.Logger;
import omr.util.TreeNode;
import static java.lang.Math.*;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Class <code>Beam</code> represents a beam hook or a beam, that may be
 * composed of several beam glyphs, aligned one after the other.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class Beam
    extends StaffNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Beam.class);

    //~ Instance fields --------------------------------------------------------

    /** Unique id (within measure) of this beam */
    private int id;

    /** Glyphs that compose this beam */
    private SortedSet<Glyph> glyphs = new TreeSet<Glyph>();

    /** Ordered sequence of Chords that are linked by this beam */
    private SortedSet<Chord> chords = new TreeSet<Chord>();

    /** Line equation */
    private Line line;

    /** Left point of beam */
    private StaffPoint left;

    /** Right point of beam */
    private StaffPoint right;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Beam //
    //------//
    /** Creates a new instance of Beam */
    public Beam (StaffNode container)
    {
        super(container, container.getStaff());

        Measure measure = (Measure) getContainer()
                                        .getContainer();
        id = measure.getNextBeamId();
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // getId//
    //-------//
    public int getId ()
    {
        return id;
    }

    //---------//
    // getLeft //
    //---------//
    public StaffPoint getLeft ()
    {
        getLine();

        return left;
    }

    //---------//
    // getLine //
    //---------//
    public Line getLine ()
    {
        if ((line == null) && (glyphs.size() > 0)) {
            line = new BasicLine();

            // Take left side of first glyph, and right side of last glyph
            left = getLeftPoint(glyphs.first());
            line.includePoint(left.x, left.y);
            right = getRightPoint(glyphs.last());
            line.includePoint(right.x, right.y);
        }

        return line;
    }

    //----------//
    // getRight //
    //----------//
    public StaffPoint getRight ()
    {
        getLine();

        return right;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (Visitor visitor)
    {
        return visitor.visit(this);
    }

    //------//
    // dump //
    //------//
    public void dump ()
    {
        getLine();
        Dumper.dump(this);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Beam");

        sb.append(" #")
          .append(id);

        sb.append(" left=")
          .append(getLeft());

        sb.append(" right=")
          .append(getRight());

        sb.append(" glyphs[");

        for (Glyph glyph : glyphs) {
            sb.append("#")
              .append(glyph.getId());
        }

        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate a (or create a brand new) beam with this glyph
     *
     * @param glyph a beam glyph
     * @param measure the containing measure
     */
    static void populate (Glyph   glyph,
                          Measure measure)
    {
        ///logger.info("Populating " + glyph);
        Beam beam = null;

        // Browse existing beams, to check if this glyph can be appended
        for (TreeNode node : measure.getBeams()) {
            Beam b = (Beam) node;

            if (b.isCompatibleWith(glyph)) {
                beam = b;

                break;
            }
        }

        // If not, create a brand new beam entity
        if (beam == null) {
            beam = new Beam(measure);
        }

        beam.addGlyph(glyph);

        logger.info(beam.toString());
    }

    //------------------//
    // isCompatibleWith //
    //------------------//
    private boolean isCompatibleWith (Glyph glyph)
    {
        logger.info("Check glyph " + glyph.getId() + " with " + this);

        Scale      scale = getStaff()
                               .getInfo()
                               .getScale();

        // Check alignment
        StaffPoint gsp = computeGlyphCenter(glyph);
        double     dist = getLine()
                              .distanceOf(gsp.x, gsp.y);
        double     maxDistance = scale.toUnits(constants.maxDistance);
        logger.info("maxDistance=" + maxDistance + " dist=" + dist);

        if (abs(dist) > maxDistance) {
            return false;
        }

        // Check distance along the same alignment
        double maxGap = scale.toUnits(constants.maxGap);

        logger.info(
            "maxGap=" + maxGap + " leftGap=" +
            getRightPoint(glyph).distance(getLeft()) + " rightGap=" +
            getLeftPoint(glyph).distance(getRight()));

        if ((getRightPoint(glyph)
                 .distance(getLeft()) <= maxGap) ||
            (getLeftPoint(glyph)
                 .distance(getRight()) <= maxGap)) {
            return true;
        }

        return false;
    }

    //--------------//
    // getLeftPoint //
    //--------------//
    private StaffPoint getLeftPoint (Glyph glyph)
    {
        Stick stick = (Stick) glyph;
        int   lx = stick.getFirstPos();
        Scale scale = getStaff()
                          .getInfo()
                          .getScale();

        return new StaffPoint(
            scale.pixelsToUnits(lx) - staff.getTopLeft().x,
            (int) rint(
                scale.pixelsToUnitsDouble(stick.getLine().xAt((double) lx)) -
                staff.getTopLeft().y));
    }

    //---------------//
    // getRightPoint //
    //---------------//
    private StaffPoint getRightPoint (Glyph glyph)
    {
        Stick stick = (Stick) glyph;
        int   rx = stick.getLastPos();
        Scale scale = getStaff()
                          .getInfo()
                          .getScale();

        return new StaffPoint(
            scale.pixelsToUnits(rx) - staff.getTopLeft().x,
            (int) rint(
                scale.pixelsToUnitsDouble(stick.getLine().xAt((double) rx)) -
                staff.getTopLeft().y));
    }

    //----------//
    // addGlyph //
    //----------//
    private void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);
        reset();
    }

    //-------//
    // reset //
    //-------//
    private void reset ()
    {
        line = null;
        left = null;
        right = null;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /**
         * Maximum euclidian distance between glyph center and beam line
         */
        Scale.Fraction maxDistance = new Scale.Fraction(
            0.5,
            "Maximum euclidian distance between glyph center and beam line");

        /**
         * Maximum gap along alignment with beam left or right extremum
         */
        Scale.Fraction maxGap = new Scale.Fraction(
            0.5,
            "Maximum gap along alignment with beam left or right extremum");
    }
}
