//----------------------------------------------------------------------------//
//                                                                            //
//                                  S l u r                                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.SlurInspector;

import omr.log.Logger;

import omr.math.Circle;

import omr.score.common.PixelPoint;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.util.Predicate;
import omr.util.TreeNode;

import java.awt.geom.*;
import java.io.Serializable;
import java.util.*;

/**
 * Class <code>Slur</code> encapsulates a slur (a curve) in a system.
 * A slur is used for a tie (2 notes with he same octave & step) or for
 * just a phrase embracing several notes.
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
    public static final Comparator<Slur> verticalComparator = new Comparator<Slur>() {
        public int compare (Slur s1,
                            Slur s2)
        {
            return Double.compare(s1.getCurve().getY1(), s2.getCurve().getY1());
        }
    };

    /** Predicate for a orphan slur at the end of its system/part */
    public static final Predicate<Slur> isEndingOrphan = new Predicate<Slur>() {
        public boolean check (Slur slur)
        {
            if (slur.getRightNote() == null) {
                // Check we are in last measure
                Point2D p2 = slur.getCurve()
                                 .getP2();
                Measure measure = slur.getPart()
                                      .getMeasureAt(
                    new SystemPoint(p2.getX(), p2.getY()));

                if (measure == slur.getPart()
                                   .getLastMeasure()) {
                    // Nullify a potential link to zombie slurs
                    slur.rightExtension = null;

                    return true;
                }
            }

            return false;
        }
    };

    /** Predicate for a orphan slur at the beginning of its system/part */
    public static final Predicate<Slur> isBeginningOrphan = new Predicate<Slur>() {
        public boolean check (Slur slur)
        {
            if (slur.getLeftNote() == null) {
                // Check we are in first measure
                Point2D p1 = slur.getCurve()
                                 .getP1();
                Measure measure = slur.getPart()
                                      .getMeasureAt(
                    new SystemPoint(p1.getX(), p1.getY()));

                if (measure == slur.getPart()
                                   .getFirstMeasure()) {
                    // Nullify a potential link to zombie slurs
                    slur.leftExtension = null;

                    return true;
                }
            }

            return false;
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Underlying glyph */
    private final Glyph glyph;

    /** Underlying curve, where points are SystemPoint instances (in Double) */
    private final CubicCurve2D curve;

    /** Note on left side, if any */
    private final Note leftNote;

    /** Note on right side, if any */
    private final Note rightNote;

    /** Extension on left side, if any */
    private Slur leftExtension;

    /** Extension on right side, if any */
    private Slur rightExtension;

    /** Placement / orientation */
    private final boolean below;

    /** Is a Tie (else a plain slur) */
    private boolean tie;

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
        tie = haveSameHeight(leftNote, rightNote);
    }

    //~ Methods ----------------------------------------------------------------

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

    //-------//
    // getId //
    //-------//
    /**
     * Report the slur id (as the id of the underlying glyph)
     * @return the id of the underlying glyph
     */
    public int getId ()
    {
        return glyph.getId();
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
     * Report the note (if any) embraced by the left side of this slur
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
     * Report the note (if any) embraced by the right side of this slur
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
     * Report whether this slur is actually a tie (a slur between similar notes)
     *
     * @return true if is a Tie, false otherwise
     */
    public boolean isTie ()
    {
        return tie;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addError //
    //----------//
    @Override
    public void addError (String text)
    {
        super.addError(glyph, text);
    }

    //-----------//
    // canExtend //
    //-----------//
    /**
     * Check whether this slur can extend the prevSlur of the preceding system
     * @param prevSlur the slur candidate in the preceding system
     * @return true if connection is possible
     */
    public boolean canExtend (Slur prevSlur)
    {
        return (this.leftExtension == null) &&
               (prevSlur.rightExtension == null) &&
               this.isCompatibleWith(prevSlur);
    }

    //----------//
    // populate //
    //----------//
    /**
     * Given a glyph (potentially representing a Slur), allocate the Slur
     * entity that corresponds to this glyph.
     *
     * @param glyph The glyph to process
     * @param system The system which will contain the allocated Slur
     */
    public static void populate (Glyph       glyph,
                                 ScoreSystem system)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Populating slur glyph#" + glyph.getId());
        }

        // Compute the approximating circle
        // Build a curve using system-based coordinates
        Circle            circle = SlurInspector.computeCircle(glyph);
        CubicCurve2D      curve = computeCurve(circle, system);

        // Retrieve & sort nodes (notes or chords) on both ends of the slur
        List<MeasureNode> leftNodes = new ArrayList<MeasureNode>();
        List<MeasureNode> rightNodes = new ArrayList<MeasureNode>();

        boolean           below = retrieveEmbracedNotes(
            system,
            curve,
            leftNodes,
            rightNodes);

        // Now choose the most relevant note, if any, on each slur side
        Side leftSide = null;
        Side rightSide = null;

        switch (leftNodes.size()) {
        case 0 :

            switch (rightNodes.size()) {
            case 0 :
                break;

            case 1 :
                rightSide = new Side(rightNodes.get(0));

                break;

            default :
                rightSide = new Side(rightNodes.get(0)); // Why not?
            }

            break;

        case 1 :
            leftSide = new Side(leftNodes.get(0));

            switch (rightNodes.size()) {
            case 0 :
                break;

            case 1 :
                rightSide = new Side(rightNodes.get(0));

                break;

            default :

                for (MeasureNode node : rightNodes) {
                    rightSide = new Side(node);

                    if (leftSide.stemDir == rightSide.stemDir) {
                        break;
                    }
                }
            }

            break;

        default :

            switch (rightNodes.size()) {
            case 0 :
                leftSide = new Side(leftNodes.get(0)); // Why not?

                break;

            case 1 :
                rightSide = new Side(rightNodes.get(0));

                for (MeasureNode node : leftNodes) {
                    leftSide = new Side(node);

                    if (leftSide.stemDir == rightSide.stemDir) {
                        break;
                    }
                }

                break;

            default : // N left & P right
                leftSide = new Side(leftNodes.get(0)); // Why not?
                rightSide = new Side(rightNodes.get(0)); // Why not?
            }
        }

        // Should we allocate the slur entity?
        if ((leftSide != null) || (rightSide != null)) {
            SystemPart part = (leftSide != null) ? leftSide.note.getPart()
                              : rightSide.note.getPart();
            Slur       slur = new Slur(
                part,
                glyph,
                curve,
                below,
                (leftSide != null) ? leftSide.note : null,
                (rightSide != null) ? rightSide.note : null);
            glyph.setTranslation(slur);

            if (logger.isFineEnabled()) {
                logger.finest(slur.toString());
            }
        } else {
            system.addError(
                glyph,
                "Slur " + glyph.getId() + " with no embraced notes");
        }
    }

    //---------//
    // isBelow //
    //---------//
    /**
     * Report whether the placement of this slur is below the embraced notes
     *
     * @return true if below, false if above
     */
    public boolean isBelow ()
    {
        return below;
    }

    //-----------//
    // connectTo //
    //-----------//
    /**
     * Make the connection with another slur in the previous system
     *
     * @param prevSlur slur at the end of previous system
     */
    public void connectTo (Slur prevSlur)
    {
        // Cross-extensions
        this.leftExtension = prevSlur;
        prevSlur.rightExtension = this;

        // Tie ?
        boolean isATie = haveSameHeight(prevSlur.leftNote, this.rightNote);
        prevSlur.tie = isATie;
        this.tie = isATie;

        if (logger.isFineEnabled()) {
            logger.fine(
                (isATie ? "Tie" : "Slur") + " connection #" +
                prevSlur.glyph.getId() + " -> #" + this.glyph.getId());
        }
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

        if (tie) {
            sb.append("{Tie");
        } else {
            sb.append("{Slur");
        }

        try {
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
            } else if ((leftExtension != null) &&
                       (leftExtension.leftNote != null)) {
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
        } catch (NullPointerException e) {
            sb.append(" INVALID");
        }

        sb.append("}");

        return sb.toString();
    }

    //---------//
    // isBelow //
    //---------//
    /**
     * Report whether the provided curve is below the notes (turned upwards)
     * or above the notes (turned downwards)
     * @param curve the provided curve to check
     * @return true if below, false if above
     */
    private static boolean isBelow (CubicCurve2D curve)
    {
        // Determine arc orientation (above or below)
        final double DX = curve.getX2() - curve.getX1();
        final double DY = curve.getY2() - curve.getY1();
        final double power = (curve.getCtrlX1() * DY) -
                             (curve.getCtrlY1() * DX) - (curve.getX1() * DY) +
                             (curve.getY1() * DX);

        return power < 0;
    }

    //------------------//
    // isCompatibleWith //
    //------------------//
    /**
     * Check whether two slurs to-be-connected are roughly compatible with each
     * other (same staff id, and pitch positions not too different)
     *
     * @param prevSlur the previous slur
     * @return true if found compatible
     */
    private boolean isCompatibleWith (Slur prevSlur)
    {
        // Retrieve prev staff, using the left point of the prev slur
        Staff prevStaff = prevSlur.getPart()
                                  .getStaffAt(
            new SystemPoint(prevSlur.curve.getX1(), prevSlur.curve.getY1()));

        // Retrieve staff, using the right point of the slur
        Staff staff = getPart()
                          .getStaffAt(
            new SystemPoint(curve.getX2(), curve.getY2()));

        if (prevStaff.getId() != staff.getId()) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "prevSlur#" + prevSlur.getId() + " prevStaff:" +
                    prevStaff.getId() + " slur#" + this.getId() + " staff:" +
                    staff.getId() + " different staff id");
            }

            return false;
        }

        // Retrieve prev position, using the right point of the prev slur
        double      prevPp = prevStaff.pitchPositionOf(
            new SystemPoint(prevSlur.curve.getX2(), prevSlur.curve.getY2()));

        // Retrieve position, using the left point of the slur
        SystemPoint pt = new SystemPoint(curve.getX1(), curve.getY1());
        double      pp = staff.pitchPositionOf(pt);

        // Compare staves and pitch positions (very roughly)
        double  deltaPitch = pp - prevPp;
        boolean res = Math.abs(deltaPitch) <= (constants.maxDeltaY.getValue() * 2);

        if (logger.isFineEnabled()) {
            logger.fine(
                "prevSlur#" + prevSlur.getId() + " slur#" + this.getId() +
                " deltaPitch:" + deltaPitch + " res:" + res);
        }

        return res;
    }

    //--------------//
    // computeCurve //
    //--------------//
    /**
     *  Build the slur curve from its circle using system-based coordinates
     *
     * @param circle the approximating circle
     * @param system the containing system
     * @return the resulting curve
     */
    private static CubicCurve2D computeCurve (Circle      circle,
                                              ScoreSystem system)
    {
        CubicCurve2D pixelCurve = circle.getCurve();
        SystemPoint  p1 = system.toSystemPoint(
            new PixelPoint(
                (int) Math.rint(pixelCurve.getX1()),
                (int) Math.rint(pixelCurve.getY1())));
        SystemPoint  c1 = system.toSystemPoint(
            new PixelPoint(
                (int) Math.rint(pixelCurve.getCtrlX1()),
                (int) Math.rint(pixelCurve.getCtrlY1())));
        SystemPoint  c2 = system.toSystemPoint(
            new PixelPoint(
                (int) Math.rint(pixelCurve.getCtrlX2()),
                (int) Math.rint(pixelCurve.getCtrlY2())));
        SystemPoint  p2 = system.toSystemPoint(
            new PixelPoint(
                (int) Math.rint(pixelCurve.getX2()),
                (int) Math.rint(pixelCurve.getY2())));

        return new CubicCurve2D.Double(
            p1.x,
            p1.y,
            c1.x,
            c1.y,
            c2.x,
            c2.y,
            p2.x,
            p2.y);
    }

    //-------------//
    // filterNodes //
    //-------------//
    /**
     * Keep in the provided collection of nodes the very first ones that
     * cannot be separated via the normal node comparator
     *
     * @param nodes the collection of nodes found in the neighborhood
     * @param ref the reference point to compare distance from
     */
    private static void filterNodes (List<MeasureNode> nodes,
                                     SystemPoint       ref)
    {
        if (nodes.size() > 1) {
            NodeComparator comparator = new NodeComparator(ref);
            Collections.sort(nodes, comparator);

            // Keep only the minimum number of nodes
            MeasureNode prevNode = null;

            for (Iterator<MeasureNode> it = nodes.iterator(); it.hasNext();) {
                MeasureNode currNode = it.next();

                if (prevNode != null) {
                    if (comparator.compare(prevNode, currNode) != 0) {
                        // Discard this node
                        it.remove();

                        // And the following ones as well
                        for (; it.hasNext();) {
                            it.next();
                            it.remove();
                        }

                        return;
                    }
                }

                prevNode = currNode;
            }
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

        // TODO: what about alteration, if we have not processed them yet ???
    }

    //-----------------------//
    // retrieveEmbracedNotes //
    //-----------------------//
    /**
     * Retrieve the notes that are embraced on the left side and on the right
     * side of a slur glyph.
     *
     * @param system the containing system
     * @param curve the slur underlying curve
     * @param leftNodes output: the ordered list of notes found on left side
     * @param rightNodes output: the ordered list of notes found on right side
     * @return true if the placement is 'below'
     */
    private static boolean retrieveEmbracedNotes (ScoreSystem       system,
                                                  CubicCurve2D      curve,
                                                  List<MeasureNode> leftNodes,
                                                  List<MeasureNode> rightNodes)
    {
        boolean               below = isBelow(curve);

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

        // Sort the collections of nodes, and keep only the closest ones
        filterNodes(leftNodes, new SystemPoint(curve.getX1(), curve.getY1()));
        filterNodes(rightNodes, new SystemPoint(curve.getX2(), curve.getY2()));

        return below;
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

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
     * Class <code>NodeComparator</code> implements a Node comparator, where
     * nodes are ordered according to the ordinate of the left point (whether
     * its'a Note or a chord tail location, from top to bottom.
     */
    private static final class NodeComparator
        implements Comparator<MeasureNode>, Serializable
    {
        //~ Instance fields ----------------------------------------------------

        final SystemPoint ref;

        //~ Constructors -------------------------------------------------------

        public NodeComparator (SystemPoint ref)
        {
            this.ref = ref;
        }

        //~ Methods ------------------------------------------------------------

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

    //------//
    // Side //
    //------//
    /**
     *  Note information on one side of a slur
     */
    private static class Side
    {
        //~ Instance fields ----------------------------------------------------

        // The precise note embraced by the slur on this side
        final Note note;

        // The related chord stem direction
        final int stemDir;

        //~ Constructors -------------------------------------------------------

        public Side (MeasureNode node)
        {
            if (node instanceof Note) {
                note = (Note) node;
            } else {
                Chord chord = (Chord) node;
                // Take the last note (closest to the tail)
                note = (Note) chord.getNotes()
                                   .get(chord.getNotes().size() - 1);
            }

            stemDir = note.getChord()
                          .getStemDir();
        }
    }
}
