//----------------------------------------------------------------------------//
//                                                                            //
//                                  S l u r                                   //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.SlurGlyph;

import omr.math.Circle;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.PixelPoint;
import omr.sheet.Scale;

import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.geom.*;
import java.util.*;

/**
 * Class <code>Slur</code> encapsulates a slur (a curve) in a system
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Slur
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Slur.class);

    /** To order slurs vertically within a measure */
    private static final Comparator<Slur> slurComparator = new Comparator<Slur>() {
        public int compare (Slur s1,
                            Slur s2)
        {
            return Double.compare(s1.getCurve().getY1(), s2.getCurve().getY1());
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Underlying glyph */
    private final Glyph glyph;

    /** Underlying curve, where points are SystemPoint instances (in Double) */
    private final CubicCurve2D curve;

    /** Note on left side */
    private final Note leftNote;

    /** Note on right side */
    private final Note rightNote;

    /** Extension on left side */
    private Slur leftExtension;

    /** Extension on right side */
    private Slur rightExtension;

    /** Placement / orientation */
    private final boolean below;

    /** Is a Tie (else a plain slur) */
    private boolean isTie;

    //~ Constructors -----------------------------------------------------------

    //------//
    // Slur //
    //------//
    /**
     * Create a slur with all the specified parameters
     *
     * @param part  the containing system part
     * @param glyph the underlying glyph
     * @param curve the underlying bezier curve
     * @param below true if below, false if above
     * @param leftNote the note on the left
     * @param rightNote the note on the right
     */
    public Slur (SystemPart   part,
                 Glyph        glyph,
                 CubicCurve2D curve,
                 boolean      below,
                 Note         leftNote,
                 Note         rightNote)
    {
        super(part);
        this.glyph = glyph;
        this.curve = curve;
        this.below = below;
        this.leftNote = leftNote;
        this.rightNote = rightNote;

        // Link embraced notes to this slur instance
        if (leftNote != null) {
            leftNote.addSlur(this);
        }

        if (rightNote != null) {
            rightNote.addSlur(this);
        }

        // Tie ?
        isTie = haveSameHeight(leftNote, rightNote);
    }

    //~ Methods ----------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // getCurve //
    //----------//
    /**
     * Report the curve of the slur
     *
     * @return the curve to draw
     */
    public CubicCurve2D getCurve ()
    {
        return curve;
    }

    //------------------//
    // getLeftExtension //
    //------------------//
    /**
     * Report the slur (if any) at the end of previous system that could be
     * considered as an extension of this slur
     *
     * @return the connected slur on left, or null if none
     */
    public Slur getLeftExtension ()
    {
        return leftExtension;
    }

    //-------------//
    // getLeftNote //
    //-------------//
    /**
     * Report the note embraced by the left side of this slur
     * @return the embraced note
     */
    public Note getLeftNote ()
    {
        return leftNote;
    }

    //-------------------//
    // getRightExtension //
    //-------------------//
    /**
     * Report the slur (if any) at the beginning of next system that could be
     * considered as an extension of this slur
     *
     * @return the connected slur on right, or null if none
     */
    public Slur getRightExtension ()
    {
        return rightExtension;
    }

    //--------------//
    // getRightNote //
    //--------------//
    /**
     * Report the note embraced by the right side of this slur
     * @return the embraced note
     */
    public Note getRightNote ()
    {
        return rightNote;
    }

    //-------//
    // isTie //
    //-------//
    /**
     * Report whether this slur is actually a isTie
     *
     *
     * @return true if a isTie
     */
    public boolean isTie ()
    {
        return isTie;
    }

    //----------//
    // populate //
    //----------//
    /**
     * Given a glyph (potentially representing a Slur), allocate the Slur
     * entity that corresponds to this glyph.
     *
     * @param glyph The glyph to process
     * @param system The system which will contain the allocate Slur
     */
    public static void populate (Glyph  glyph,
                                 System system)
    {
        // Compute (and check) the approximating circle
        Circle circle = SlurGlyph.computeCircle(glyph);

        if (!circle.isValid(SlurGlyph.getMaxCircleDistance())) {
            system.addError(glyph, "Still spurious slur #" + glyph.getId());

            if (SlurGlyph.fixSpuriousSlur(glyph, system.getInfo())) {
                logger.info("Slur fixed  ...");

//                system.getScore()
//                      .getSheet()
//                      .getGlyphsBuilder()
//                      .extractNewSystemGlyphs(system.getInfo());
//
//                throw new ScoreBuilder.RebuildException("Slur");
            }
        } else {
            // Build a curve using system-based coordinates
            CubicCurve2D      pixelCurve = circle.getCurve();
            SystemPoint       p1 = system.toSystemPoint(
                new PixelPoint(
                    (int) Math.rint(pixelCurve.getX1()),
                    (int) Math.rint(pixelCurve.getY1())));
            SystemPoint       c1 = system.toSystemPoint(
                new PixelPoint(
                    (int) Math.rint(pixelCurve.getCtrlX1()),
                    (int) Math.rint(pixelCurve.getCtrlY1())));
            SystemPoint       c2 = system.toSystemPoint(
                new PixelPoint(
                    (int) Math.rint(pixelCurve.getCtrlX2()),
                    (int) Math.rint(pixelCurve.getCtrlY2())));
            SystemPoint       p2 = system.toSystemPoint(
                new PixelPoint(
                    (int) Math.rint(pixelCurve.getX2()),
                    (int) Math.rint(pixelCurve.getY2())));
            CubicCurve2D      curve = new CubicCurve2D.Double(
                p1.x,
                p1.y,
                c1.x,
                c1.y,
                c2.x,
                c2.y,
                p2.x,
                p2.y);

            // Retrieve and sort notes
            List<MeasureNode> leftNodes = new ArrayList<MeasureNode>();
            List<MeasureNode> rightNodes = new ArrayList<MeasureNode>();

            boolean           below = retrieveEmbracedNotes(
                glyph,
                system,
                curve,
                leftNodes,
                rightNodes);

            MeasureNode       leftNode = null;
            Note              leftNote = null;
            MeasureNode       rightNode = null;
            Note              rightNote = null;

            if (!leftNodes.isEmpty()) {
                leftNode = leftNodes.get(0);

                if (leftNode instanceof Note) {
                    leftNote = (Note) leftNode;
                } else {
                    Chord chord = (Chord) leftNode;
                    // Take the first note (closest to the tail)
                    leftNote = (Note) chord.getNotes()
                                           .get(0);
                }
            }

            if (!rightNodes.isEmpty()) {
                rightNode = rightNodes.get(0);

                if (rightNode instanceof Note) {
                    rightNote = (Note) rightNode;
                } else {
                    Chord chord = (Chord) rightNode;
                    // Take the first note (closest to the tail)
                    rightNote = (Note) chord.getNotes()
                                            .get(0);
                }
            }

            if ((leftNote != null) || (rightNote != null)) {
                SystemPart part = (leftNote != null) ? leftNode.getPart()
                                  : rightNote.getPart();
                Slur       slur = new Slur(
                    part,
                    glyph,
                    curve,
                    below,
                    leftNote,
                    rightNote);
                glyph.setTranslation(slur);

                if (logger.isFineEnabled()) {
                    //                    logger.fine(
                    //                        String.format(
                    //                            "Glyph %d dist=%g %s",
                    //                            glyph.getId(),
                    //                            circle.getDistance(),
                    //                            circle.toString()));
                    logger.finest(slur.toString());
                }
            } else {
                system.addError(glyph,
                    "Slur " + glyph.getId() + " with no embraced notes");
            }
        }
    }

    //---------//
    // isBelow //
    //---------//
    /**
     * Report whether the placement of this slur is below
     *
     * @return true if below, false if above
     */
    public boolean isBelow ()
    {
        return below;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description for this slur
     * @return a string with all slur parameters
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        if (isTie) {
            sb.append("{Tie");
        } else {
            sb.append("{Slur");
        }

        sb.append("#")
          .append(glyph.getId());

        sb.append(" P1[")
          .append((int) Math.rint(curve.getX1()))
          .append(",")
          .append((int) Math.rint(curve.getY1()))
          .append("]");

        sb.append(" C1[")
          .append((int) Math.rint(curve.getCtrlX1()))
          .append(",")
          .append((int) Math.rint(curve.getCtrlY1()))
          .append("]");

        sb.append(" C2[")
          .append((int) Math.rint(curve.getCtrlX2()))
          .append(",")
          .append((int) Math.rint(curve.getCtrlY2()))
          .append("]");

        sb.append(" P2[")
          .append((int) Math.rint(curve.getX2()))
          .append(",")
          .append((int) Math.rint(curve.getY2()))
          .append("]");

        if (leftNote != null) {
            sb.append(" L=")
              .append(leftNote.getStep())
              .append(leftNote.getOctave());
        } else if ((leftExtension != null) && (leftExtension.leftNote != null)) {
            sb.append(" LE=")
              .append(leftExtension.leftNote.getStep())
              .append(leftExtension.leftNote.getOctave());
        }

        if (rightNote != null) {
            sb.append(" R=")
              .append(rightNote.getStep())
              .append(rightNote.getOctave());
        } else if ((rightExtension != null) &&
                   (rightExtension.rightNote != null)) {
            sb.append(" RE=")
              .append(rightExtension.rightNote.getStep())
              .append(rightExtension.rightNote.getOctave());
        }

        sb.append("}");

        return sb.toString();
    }

    //-------------------------//
    // retrieveSlurConnections //
    //-------------------------//
    /**
     * Retrieve the connections between the (orphan) slurs at the beginning of
     * this system and the (orphan) slurs at the end of the previous system
     *
     * @param system the current system
     */
    static void retrieveSlurConnections (System system)
    {
        // Part by part,
        // Check whether orphan slurs in first measure can be connected to
        // corresponding orphans slurs in last leasure of previous system
        System prevSystem = (System) system.getPreviousSibling();

        if (prevSystem == null) {
            return;
        }

        // Do we have slurs with no left note in current system ?
        for (TreeNode pNode : system.getParts()) {
            SystemPart part = (SystemPart) pNode;
            List<Slur> orphans = new ArrayList<Slur>();

            for (TreeNode sNode : part.getSlurs()) {
                Slur slur = (Slur) sNode;

                if (slur.getLeftNote() == null) {
                    // Check we are in first measure
                    Point2D p1 = slur.getCurve()
                                     .getP1();
                    Measure measure = part.getMeasureAt(
                        new SystemPoint(p1.getX(), p1.getY()));

                    if (measure == part.getFirstMeasure()) {
                        orphans.add(slur);
                    }
                }
            }

            if (orphans.size() == 0) {
                continue;
            }

            Collections.sort(orphans, slurComparator);

            // Get orphans in previous measure
            SystemPart prevPart = (SystemPart) prevSystem.getParts()
                                                         .get(part.getId() - 1);
            List<Slur> prevOrphans = new ArrayList<Slur>();

            for (TreeNode sNode : prevPart.getSlurs()) {
                Slur slur = (Slur) sNode;

                if (slur.getRightNote() == null) {
                    // Check we are in last measure
                    Point2D p2 = slur.getCurve()
                                     .getP2();
                    Measure measure = prevPart.getMeasureAt(
                        new SystemPoint(p2.getX(), p2.getY()));

                    if (measure == prevPart.getLastMeasure()) {
                        prevOrphans.add(slur);
                    }
                }
            }

            Collections.sort(prevOrphans, slurComparator);

            // Connect the orphans as much as possible
            SlurLoop:
            for (int i = 0; i < orphans.size(); i++) {
                Slur slur = orphans.get(i);

                for (int j = 0; j < prevOrphans.size(); j++) {
                    Slur prevSlur = prevOrphans.get(j);

                    if ((prevSlur.rightExtension == null) &&
                        arePositionCompatible(prevPart, prevSlur, part, slur)) {
                        connectSlurs(prevSlur, slur);

                        continue SlurLoop;
                    }
                }

                // No connection for this orphan
                part.addError(slur.glyph, " Could not connect slur #" +
                    slur.glyph.getId());
            }

            // Check previous orphans
            for (int j = 0; j < prevOrphans.size(); j++) {
                Slur prevSlur = prevOrphans.get(j);

                if (prevSlur.rightExtension == null) {
                    prevPart.addError(prevSlur.glyph,
                        " Could not connect slur #" + prevSlur.glyph.getId());
                }
            }
        }
    }

    //-----------------------//
    // arePositionCompatible //
    //-----------------------//
    /**
     * Check whether two slurs to-be-connected are roughly compatible with each other
     *
     * @param prevPart the part containing the previous slur
     * @param prevSlur the previous slur
     * @param part the part containing the following slur
     * @param slur the following slur
     * @return true if found compatible
     */
    private static boolean arePositionCompatible (SystemPart prevPart,
                                                  Slur       prevSlur,
                                                  SystemPart part,
                                                  Slur       slur)
    {
        // Retrieve prev position
        SystemPoint prevPt = new SystemPoint(
            prevSlur.curve.getX2(),
            prevSlur.curve.getY2());
        Staff       prevStaff = prevPart.getStaffAt(prevPt);
        double      prevPp = prevStaff.pitchPositionOf(prevPt);

        // Retrieve position
        SystemPoint pt = new SystemPoint(
            slur.curve.getX1(),
            slur.curve.getY1());
        Staff       staff = part.getStaffAt(pt);
        double      pp = staff.pitchPositionOf(pt);

        // Compare pitch positions (very roughly)
        double deltaPitch = pp - prevPp;

        if (logger.isFineEnabled()) {
            logger.finest("deltaPitch=" + deltaPitch);
        }

        return (prevStaff.getId() == staff.getId()) &&
               (Math.abs(deltaPitch) <= (constants.maxDeltaY.getValue() * 2));
    }

    //--------------//
    // connectSlurs //
    //--------------//
    /**
     * Actually connect two slur chunks over a system border
     *
     * @param prevSlur slur at the end of previous system
     * @param slur slur at the beginning of the following system
     */
    private static void connectSlurs (Slur prevSlur,
                                      Slur slur)
    {
        // Cross-extensions
        slur.leftExtension = prevSlur;
        prevSlur.rightExtension = slur;

        // Tie ?
        boolean tie = haveSameHeight(prevSlur.leftNote, slur.rightNote);
        prevSlur.isTie = tie;
        slur.isTie = tie;

        if (logger.isFineEnabled()) {
            logger.fine(
                (tie ? "Tie" : "Slur") + " connection #" +
                prevSlur.glyph.getId() + " -> #" + slur.glyph.getId());
        }
    }

    //----------------//
    // haveSameHeight //
    //----------------//
    /**
     * Check whether two notes represent the same pitch (same octave, same step,
     * same alteration). This is needed to detects tie slurs.
     *
     * @param n1 one note
     * @param n2 the other note
     * @return true if the notes are equivalent.
     */
    private static boolean haveSameHeight (Note n1,
                                           Note n2)
    {
        return (n1 != null) && (n2 != null) && (n1.getStep() == n2.getStep()) &&
               (n1.getOctave() == n2.getOctave());

        // TBD: what about alteration, if we have not processed them yet ???
    }

    //-----------------------//
    // retrieveEmbracedNotes //
    //-----------------------//
    /**
     * Retrieve the notes that ar embraced on the left side and on the right
     * side of a slur glyph. Actually, we could retrieve just the first note on
     * left and first note on right. TBD.
     *
     * @param glyph the given slur glyph
     * @param system the containing system
     * @param curve the slur underlying curve
     * @param leftNodes output: the ordered list of notes found on left side
     * @param rightNodes output: the ordered list of notes found on right side
     * @return true if the placement is 'below'
     */
    private static boolean retrieveEmbracedNotes (Glyph             glyph,
                                                  System            system,
                                                  CubicCurve2D      curve,
                                                  List<MeasureNode> leftNodes,
                                                  List<MeasureNode> rightNodes)
    {
        // Determine arc orientation (above or below)
        final double          DX = curve.getX2() - curve.getX1();
        final double          DY = curve.getY2() - curve.getY1();
        final double          power = (curve.getCtrlX1() * DY) -
                                      (curve.getCtrlY1() * DX) -
                                      (curve.getX1() * DY) +
                                      (curve.getY1() * DX);
        final boolean         below = power < 0;

        // Determine left and right search areas
        final Scale           scale = system.getScale();
        final int             dx = scale.toUnits(constants.areaDx);
        final int             dy = scale.toUnits(constants.areaDy);
        final int             xMg = scale.toUnits(constants.areaXMargin);
        final SystemRectangle leftRect = new SystemRectangle(
            (int) Math.rint(curve.getX1() - dx),
            (int) Math.rint(curve.getY1()),
            dx + xMg,
            dy);
        final SystemRectangle rightRect = new SystemRectangle(
            (int) Math.rint(curve.getX2() - xMg),
            (int) Math.rint(curve.getY2()),
            dx + xMg,
            dy);

        if (below) {
            leftRect.y -= dy;
            rightRect.y -= dy;
        }

        // System > Part > Measure > Chord > Note
        for (TreeNode pNode : system.getParts()) {
            SystemPart part = (SystemPart) pNode;

            for (TreeNode mNode : part.getMeasures()) {
                Measure measure = (Measure) mNode;

                for (TreeNode cNode : measure.getChords()) {
                    Chord chord = (Chord) cNode;

                    if (leftRect.contains(chord.getTailLocation())) {
                        leftNodes.add(chord);
                    }

                    if (rightRect.contains(chord.getTailLocation())) {
                        rightNodes.add(chord);
                    }

                    for (TreeNode nNode : chord.getNotes()) {
                        Note note = (Note) nNode;

                        if (leftRect.contains(note.getCenter())) {
                            leftNodes.add(note);
                        }

                        if (rightRect.contains(note.getCenter())) {
                            rightNodes.add(note);
                        }
                    }
                }
            }
        }

        // Sort the collections of notes
        Collections.sort(
            leftNodes,
            new NodeComparator(curve.getX1(), curve.getY1()));
        Collections.sort(
            rightNodes,
            new NodeComparator(curve.getX2(), curve.getY2()));

        return below;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Abscissa extension when looking for embraced notes */
        Scale.Fraction areaDx = new Scale.Fraction(
            2,
            "Abscissa extension when looking for embraced notes");

        /** Abscissa margin when looking for embraced notes */
        Scale.Fraction areaXMargin = new Scale.Fraction(
            1,
            "Abscissa margin when looking for embraced notes");

        /** Ordinate extension when looking for embraced notes */
        Scale.Fraction areaDy = new Scale.Fraction(
            6,
            "Ordinate extension when looking for embraced notes");

        /** Maximum difference in vertical position between connecting slurs */
        Scale.Fraction maxDeltaY = new Scale.Fraction(
            4,
            "Maximum difference in vertical position between connecting slurs");
    }

    //----------------//
    // NodeComparator //
    //----------------//
    /**
     * Class <code>NodeComparator</code> implements a Slur comparator, where
     * slurs are ordered according to the ordinate of the left point (whether
     * its'a Note or a chord tail location, from top to bottom.
     */
    private static final class NodeComparator
        implements Comparator<MeasureNode>
    {
        final SystemPoint ref;

        public NodeComparator (double x,
                               double y)
        {
            ref = new SystemPoint(x, y);
        }

        public int compare (MeasureNode n1,
                            MeasureNode n2)
        {
            SystemPoint p1 = (n1 instanceof Chord)
                             ? ((Chord) n1).getTailLocation() : n1.getCenter();
            SystemPoint p2 = (n2 instanceof Chord)
                             ? ((Chord) n2).getTailLocation() : n2.getCenter();

            return Double.compare(p1.distance(ref), p2.distance(ref));
        }
    }
}
