//----------------------------------------------------------------------------//
//                                                                            //
//                          K e y S i g n a t u r e                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import static omr.score.Note.Step.*;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.PixelPoint;
import omr.sheet.PixelRectangle;
import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.Logger;
import omr.util.TreeNode;

import java.util.*;

/**
 * Class <code>KeySignature</code> encapsulates a key signature, which may be
 * composed of one or several glyphs (all sharps or all flats).
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class KeySignature
    extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(KeySignature.class);

    /** Pitch position for the members of the sharp keys assuming a G clef */
    private static final double[] sharpPositions = new double[] {
                                                       -4, // F - Fa
    -1, // C - Do
    -5, // G - Sol
    -2, // D - R�
    +1, // A - La
    -3, // E - Mi
    0 // B - Si
                                                   };

    /** Note steps according to sharp key */
    private static final Note.Step[] sharpSteps = new Note.Step[] {
                                                      F, C, G, D, A, E, B
                                                  };

    /** Pitch position for the members of the flat keys assuming a G clef */
    private static final double[] flatPositions = new double[] {
                                                      0, // B - Si
    -3, // E - Mi
    +1, // A - La
    -2, // D - R�
    +2, // G - Sol
    -1, // C - Do
    +3 // F - Fa
                                                  };

    /** Note steps according to flat key */
    private static final Note.Step[] flatSteps = new Note.Step[] {
                                                     B, E, A, D, G, C, F
                                                 };

    //~ Instance fields --------------------------------------------------------

    /** Precise key signature. 0 for none, +n for n sharps, -n for n flats */
    private Integer key;

    /** Global pitch position */
    private Double pitchPosition;

    /** Related shape for drawing */
    private Shape shape;

    /** Global contour box */
    private PixelRectangle contour;

    /** Center of mass of the key sig */
    private PixelPoint centroid;

    /** Related Clef kind (G, F or C) */
    private Shape clefKind;

    /**
     * The glyph(s) that compose the key signature, a collection which is kept
     * sorted on glyph abscissa.
     */
    private SortedSet<Glyph> glyphs = new TreeSet<Glyph>();

    /** Unlucky glyphs candidates (not really used TBD) */
    private SortedSet<Glyph> parias = new TreeSet<Glyph>();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // KeySignature //
    //--------------//
    /**
     * Create a key signature, with containing measure
     *
     * @param measure the containing measure
     * @param staff the related staff
     */
    public KeySignature (Measure measure,
                         Staff   staff)
    {
        super(measure);
        setStaff(staff);
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

    //-------------//
    // getAlterFor //
    //-------------//
    public int getAlterFor (Note.Step step)
    {
        getKey();

        if (key > 0) {
            for (int k = 0; k < key; k++) {
                if (step == sharpSteps[k]) {
                    return 1;
                }
            }
        } else {
            for (int k = 0; k < -key; k++) {
                if (step == flatSteps[k]) {
                    return -1;
                }
            }
        }

        return 0;
    }

    //
    //    //--------------//
    //    // KeySignature //
    //    //--------------//
    //    /**
    //     * Entry for generating a dummy keysig, please ignore.
    //     */
    //    public KeySignature (Measure measure,
    //                         int     key)
    //    {
    //        super(measure);
    //
    //        this.key = key;
    //        center = new SystemPoint(
    //            measure.getLeftX() + (measure.getWidth() / 2),
    //            0);
    //    }

    //--------//
    // getKey //
    //--------//
    /**
     * Report the key signature
     *
     * @return the (lazily determined) key
     */
    public Integer getKey ()
    {
        if (key == null) {
            retrieveKey();
        }

        return key;
    }

    //------------------//
    // getPitchPosition //
    //------------------//
    /**
     * Report the pitch position for the global key symbol
     *
     * @return the pitch position of the signature symbol
     */
    public Double getPitchPosition ()
    {
        if (pitchPosition == null) {
            Shape guess = guessClefKind();

            if (guess == null) {
                guess = G_CLEF;
            }

            pitchPosition = getTheoreticalPosition() + clefToDelta(guess);
        }

        return pitchPosition;
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the related symbol
     *
     * @return related symbol
     */
    public Shape getShape ()
    {
        if (shape == null) {
            getKey();

            if (key > 0) {
                shape = Shape.values()[(Shape.KEY_SHARP_1.ordinal() + key) - 1];
            } else {
                shape = Shape.values()[(Shape.KEY_FLAT_1.ordinal() + key) + 1];
            }
        }

        return shape;
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate the score with a key signature built from the provided glyph
     *
     * @param glyph the source glyph
     * @param measure containing measure
     * @param staff related staff
     * @param center glyph center wrt system
     *
     * @return true if population is successful, false otherwise
     */
    public static boolean populate (Glyph       glyph,
                                    Measure     measure,
                                    Staff       staff,
                                    SystemPoint center)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Populating keysig for " + glyph);
        }

        System         system = measure.getSystem();
        SystemInfo     systemInfo = system.getInfo();

        // Make sure we have no note nearby
        // Use a enlarged rectangular box around the glyph, and check what's in
        PixelRectangle box = glyph.getContourBox();
        final int      dx = measure.getScale()
                                   .toPixels(constants.xMargin);
        final int      dy = measure.getScale()
                                   .toPixels(constants.yMargin);
        PixelRectangle glyphFatBox = new PixelRectangle(
            box.x - dx,
            box.y - dy,
            box.width + (2 * dx),
            box.height + (2 * dy));
        List<Glyph>    neighbors = systemInfo.lookupIntersectedGlyphs(
            glyphFatBox,
            glyph);

        // Check for lack of stem symbols (beam, beam hook, note head, flags),
        // or stand-alone note
        for (Glyph g : neighbors) {
            Shape shape = g.getShape();

            if (Shape.StemSymbols.contains(shape) ||
                Shape.Notes.getShapes()
                           .contains(shape)) {
                if (logger.isFineEnabled()) {
                    logger.fine("Cannot accept " + shape + " as neighbor");
                }

                return false;
            }
        }

        // Do we have a key signature just before in the same measure ?
        KeySignature keysig = null;
        boolean      found = false;

        for (TreeNode node : measure.getKeySignatures()) {
            keysig = (KeySignature) node;

            if (keysig.getCenter().x > center.x) {
                break;
            }

            // Check distance
            if (!glyphFatBox.intersects(keysig.getContour())) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Glyph " + glyph.getId() +
                        " too far from previous key signature");
                }

                keysig.parias.add(glyph);

                continue;
            } else if (((glyph.getShape() == SHARP) && (keysig.getKey() < 0)) ||
                       ((glyph.getShape() == FLAT) && (keysig.getKey() > 0))) {
                // Check sharp or flat key sig, wrt current glyph
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Cannot extend key signature with glyph " +
                        glyph.getId());
                }

                return false;
            } else {
                // Everything is OK
                found = true;

                break;
            }
        }

        // If not found create a brand new one
        if (!found) {
            // Check pitch position
            Shape clefShape = null;
            Clef  clef = measure.getClefBefore(
                measure.computeGlyphCenter(glyph));

            if (clef != null) {
                clefShape = clef.getShape();
            } else {
                // Assume G clef
                clefShape = G_CLEF;
            }

            if (!checkPitchPosition(
                glyph,
                center,
                staff,
                0,
                getClefKind(clefShape))) {
                return false;
            }

            keysig = new KeySignature(measure, staff);
        }

        // Extend the keysig with this glyph
        keysig.addGlyph(glyph);
        glyph.setTranslation(keysig);

        if (logger.isFineEnabled()) {
            logger.fine("key=" + keysig.getKey());
        }

        return true;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return description
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{KeySignature");

        sb.append(" key=")
          .append(key);

        sb.append(" center=")
          .append(getCenter());

        sb.append(" pitch=")
          .append(getPitchPosition());

        sb.append(" glyphs[");

        for (Glyph glyph : glyphs) {
            sb.append("#")
              .append(glyph.getId());
        }

        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    //------------------//
    // verifySystemKeys //
    //------------------//
    /**
     * Perform verifications (and corrections when possible) when all keysigs
     * have been generated for the system: The key signature must be the same
     * (in terms of fifths) for the same measure index in all parts and staves.
     *
     * @param system the system to be verified
     */
    public static void verifySystemKeys (System system)
    {
        ///logger.info("verifySystemKeys for " + system);

        // Total number of staves in the system
        final int staffNb = system.getInfo()
                                  .getStaves()
                                  .size();

        // Number of measures in the system
        final int measureNb = system.getFirstPart()
                                    .getMeasures()
                                    .size();

        // Verify each measure index on turn
        for (int im = 0; im < measureNb; im++) {
            ///logger.info("measure index =" + im);

            // Retrieve the key list for this measure in each staff
            List[] keyMatrix = new List[staffNb];
            int    staffOffset = 0;

            for (int is = 0; is < staffNb; is++) {
                keyMatrix[is] = new ArrayList<TreeNode>();
            }

            for (TreeNode node : system.getParts()) {
                SystemPart part = (SystemPart) node;
                Measure    measure = (Measure) part.getMeasures()
                                                   .get(im);

                for (TreeNode ksnode : measure.getKeySignatures()) {
                    KeySignature ks = (KeySignature) ksnode;

                    keyMatrix[ks.getStaff()
                                .getId() - 1 + staffOffset].add(ks);
                }

                staffOffset += part.getStaves()
                                   .size();
            }

            // Same number of keys in each measure/staff ?
            int maxKeyNb = -1;

            for (List<TreeNode> list : keyMatrix) {
                maxKeyNb = Math.max(maxKeyNb, list.size());
            }

            if (logger.isFineEnabled()) {
                logger.fine(
                    system.getContextString() + "M" + im + " maxKeyNb = " +
                    maxKeyNb);
            }

            for (int iStaff = 0; iStaff < keyMatrix.length; iStaff++) {
                List keyList = keyMatrix[iStaff];

                if (logger.isFineEnabled()) {
                    logger.fine(
                        getContextString(system, im, iStaff) + " keyNb=" +
                        keyList.size());
                }

                if (keyList.size() < maxKeyNb) {
                    // Determine the containing measure
                    getMeasureOf(system, iStaff, im)
                        .addError("Missing key signature");

                    // Need to add a key here
                    staffOffset = 0;

                    for (TreeNode node : system.getParts()) {
                        SystemPart part = (SystemPart) node;
                        int        partStaffNb = part.getStaves()
                                                     .size();
                        staffOffset += partStaffNb;

                        if (iStaff < staffOffset) {
                            KeySignature ks = new KeySignature(
                                (Measure) part.getMeasures().get(im),
                                (Staff) part.getStaves().get(
                                    (partStaffNb + iStaff) - staffOffset));
                            ks.key = 0; // Specific compatible dummy value
                            keyList.add(ks);

                            break;
                        }
                    }
                }
            }

            // Compatible keys ?
            for (int ik = 0; ik < maxKeyNb; ik++) {
                // Browse all staves for sharp/flat compatibility
                // If not, give up
                // If compatible, adjust all keysigs to the longest
                boolean      compatible = true;
                boolean      adjustment = false;
                KeySignature keysig = null;

                for (List<TreeNode> list : keyMatrix) {
                    KeySignature ks = (KeySignature) list.get(ik);

                    if (keysig == null) {
                        keysig = ks;
                    } else if (!keysig.getKey()
                                      .equals(ks.getKey())) {
                        if (logger.isFineEnabled()) {
                            logger.fine("Key signatures will need adjustment");
                        }

                        adjustment = true;

                        if ((ks.getKey() * keysig.getKey()) < 0) {
                            if (logger.isFineEnabled()) {
                                logger.fine("Non compatible key signatures");
                            }

                            compatible = false;

                            break;
                        } else if (Math.abs(keysig.getKey()) < Math.abs(
                            ks.getKey())) {
                            // Keep longest key
                            keysig = ks;
                        }
                    }
                }

                // Force key signatures to this value, if compatible
                if (compatible && adjustment) {
                    for (List<TreeNode> list : keyMatrix) {
                        KeySignature ks = (KeySignature) list.get(ik);

                        if (!keysig.getKey()
                                   .equals(ks.getKey())) {
                            logger.fine(
                                ks.getContextString() +
                                " Forcing key signature to " + keysig.getKey());
                            ks.copyKey(keysig);

                            // TBD deassign glyphs that do not contribute to the key ?
                        }
                    }
                }
            }
        }
    }

    //---------------//
    // computeCenter //
    //---------------//
    @Override
    protected void computeCenter ()
    {
        setCenter(computeGlyphsCenter(glyphs));
    }

    //-------------//
    // clefToDelta //
    //-------------//
    /**
     * Report the delta in pitch position (wrt standard G_CLEF positions)
     * according to a given clef
     *
     * @param clefKind the kind of clef
     * @return the delta in pitch position
     */
    private static int clefToDelta (Shape clefKind)
    {
        switch (clefKind) {
        case G_CLEF :
            return 0;

        case F_CLEF :
            return 2;

        case C_CLEF :
            return 1;

        default :
            return 0; // Not correct TBD
        }
    }

    //-------------//
    // getClefKind //
    //-------------//
    /**
     * Classify clefs by clef kinds, since for example the same key is
     * represented with idebtical pitch positions for G_CLEF, G_CLEF_OTTAVA_ALTA
     * and G_CLEF_OTTAVA_BASSA.
     *
     * @param shape the precise clef shape
     * @return the clef kind
     */
    private static Shape getClefKind (Shape shape)
    {
        switch (shape) {
        case G_CLEF :
        case G_CLEF_OTTAVA_ALTA :
        case G_CLEF_OTTAVA_BASSA :
            return G_CLEF;

        case F_CLEF :
        case F_CLEF_OTTAVA_ALTA :
        case F_CLEF_OTTAVA_BASSA :
            return F_CLEF;

        case C_CLEF :
            return C_CLEF;

        default :
            return null;
        }
    }

    //------------------//
    // getContextString //
    //------------------//
    private static String getContextString (System system,
                                            int    measureIndex,
                                            int    systemStaffIndex)
    {
        return system.getContextString() + "M" + (measureIndex + 1) + "T" +
               staffOf(system, systemStaffIndex)
                   .getId();
    }

    //--------------//
    // getMeasureOf //
    //--------------//
    private static Measure getMeasureOf (System system,
                                         int    staffIndex,
                                         int    measureIndex)
    {
        int staffOffset = 0;

        for (TreeNode node : system.getParts()) {
            SystemPart part = (SystemPart) node;
            staffOffset += part.getStaves()
                               .size();

            if (staffIndex < staffOffset) {
                return (Measure) part.getMeasures()
                                     .get(measureIndex);
            }
        }

        logger.severe("Illegal systemStaffIndex: " + staffIndex);

        return null;
    }

    //---------//
    // staffOf //
    //---------//
    private static Staff staffOf (System system,
                                  int    systemStaffIndex)
    {
        int staffOffset = 0;

        for (TreeNode node : system.getParts()) {
            SystemPart part = (SystemPart) node;
            int        partStaffNb = part.getStaves()
                                         .size();
            staffOffset += partStaffNb;

            if (systemStaffIndex < staffOffset) {
                return (Staff) part.getStaves()
                                   .get(
                    (partStaffNb + systemStaffIndex) - staffOffset);
            }
        }

        logger.severe("Illegal systemStaffIndex: " + systemStaffIndex);

        return null;
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a new glyph as part of this key signature
     *
     * @param glyph the new component glyph
     */
    private void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);
        reset();
    }

    //--------------------//
    // checkPitchPosition //
    //--------------------//
    /**
     * Check that the glyph is at the correct pitch position, knowing its index
     * in the signature, its shape (sharp or flat) and the current clef kind at
     * this location
     *
     * @param glyph the glyph to check
     * @param center the (flat-corrected) glyph center
     * @param the containing staff
     * @param index index in signature
     * @param clefKind kind of clef at this location
     * @return true if OK, false otherwise
     */
    private static boolean checkPitchPosition (Glyph       glyph,
                                               SystemPoint center,
                                               Staff       staff,
                                               int         index,
                                               Shape       clefKind)
    {
        if (glyph.getShape() == SHARP) {
            return checkPosition(
                glyph,
                center,
                staff,
                sharpPositions,
                index,
                clefToDelta(clefKind));
        } else {
            return checkPosition(
                glyph,
                center,
                staff,
                flatPositions,
                index,
                clefToDelta(clefKind));
        }
    }

    //---------------//
    // checkPosition //
    //---------------//
    /**
     * Check the pitch position of the glyph, knowing the theoretical positions
     * based on glyph shape
     *
     * @param glyph the glyph to check
     * @param center the (flat-corrected) glyph center
     * @param the containing staff
     * @param positions the array of positions (for G clef kind)
     * @param index index of glyph within signature
     * @param delta delta pitch position (based on clef kind)
     * @return true if OK, false otherwise
     */
    private static boolean checkPosition (Glyph       glyph,
                                          SystemPoint center,
                                          Staff       staff,
                                          double[]    positions,
                                          int         index,
                                          int         delta)
    {
        double dif = staff.pitchPositionOf(center) - positions[index] - delta;

        if (Math.abs(dif) > (constants.keyYMargin.getValue() * 2)) {
            if (logger.isFineEnabled()) {
                logger.fine(
                    "Invalid pitch position for glyph " + glyph.getId());
            }

            return false;
        } else if (logger.isFineEnabled()) {
            logger.fine("Correct pitch position for glyph " + glyph.getId());
        }

        return true;
    }

    //---------//
    // copyKey //
    //---------//
    /**
     * Force parameters (key, center, pitchPosition) of this key signature, by
     * deriving parameters of another keysig (from another staff)
     *
     * @param ks the key signature to replicate here
     */
    private void copyKey (KeySignature ks)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Copying " + ks + " to " + this);
        }

        // Nota: We don't touch to contour, nor centroid

        // key
        key = ks.getKey();

        // Beware of different clef kinds
        Measure measure = (Measure) getParent()
                                        .getParent();
        Clef    clef = measure.getClefBefore(ks.getCenter());
        Measure ms = (Measure) ks.getParent()
                                 .getParent();
        Clef    c = ms.getClefBefore(ks.getCenter());
        Shape   kind = getClefKind(clef.getShape());
        Shape   k = getClefKind(c.getShape());
        int     delta = clefToDelta(kind) - clefToDelta(k);

        // center
        setCenter(
            new SystemPoint(
                ks.getCenter().x,
                ks.getCenter().y + getStaff().pitchToUnit(delta)));

        // pitchPosition
        pitchPosition = new Double(ks.getPitchPosition() + delta);
    }

    //-------------//
    // deltaToClef //
    //-------------//
    /**
     * Determine clef kind, based on delta pitch position
     *
     * @param delta the delta in pitch position between the actual glyphs
     *              position and the theoretical position based on key
     * @return the kind of clef
     */
    private Shape deltaToClef (int delta)
    {
        switch (delta) {
        case 0 :
            return clefKind = G_CLEF;

        case 1 :
            return clefKind = C_CLEF;

        case 2 :
            return clefKind = F_CLEF;

        default :
            return null;
        }
    }

    //-------------//
    // getCentroid //
    //-------------//
    /**
     * Report the actual center of mass of the glyphs that compose the signature
     *
     * @return the PixelPoint that represent the center of mass
     */
    private PixelPoint getCentroid ()
    {
        if (centroid == null) {
            centroid = new PixelPoint();

            double totalWeight = 0;

            for (Glyph glyph : glyphs) {
                PixelPoint c = glyph.getCentroid();
                double     w = glyph.getWeight();
                centroid.x += (c.x * w);
                centroid.y += (c.y * w);
                totalWeight += w;
            }

            centroid.x /= totalWeight;
            centroid.y /= totalWeight;
        }

        return centroid;
    }

    //------------//
    // getContour //
    //------------//
    /**
     * Report the actual contour of the global key signature, which is the union
     * of all member glyphs
     *
     * @return the global contour box, as a PixelRectangle
     */
    private PixelRectangle getContour ()
    {
        if (contour == null) {
            for (Glyph glyph : glyphs) {
                if (contour == null) {
                    contour = new PixelRectangle(glyph.getContourBox());
                } else {
                    contour = contour.union(glyph.getContourBox());
                }
            }
        }

        return contour;
    }

    //------------------------//
    // getTheoreticalPosition //
    //------------------------//
    /**
     * Compute the theoretical mean pitch position, based on key value
     *
     * @return the mean pitch position (for the signature symbol, for example)
     */
    private double getTheoreticalPosition ()
    {
        double sum = 0;
        getKey();

        if (key > 0) {
            for (int k = 0; k < key; k++) {
                sum += sharpPositions[k];
            }
        } else {
            for (int k = 0; k > key; k--) {
                sum -= flatPositions[-k];
            }
        }

        return sum / key;
    }

    //---------------//
    // guessClefKind //
    //---------------//
    /**
     * Guess what the current clef kind (G,F or C) is, based on the pitch
     * positions of the member glyphs
     *
     * @return the kind of clef
     */
    private Shape guessClefKind ()
    {
        if (clefKind == null) {
            double theoPos = getTheoreticalPosition();
            double realPos = getStaff()
                                 .pitchPositionOf(getCentroid());

            // Correction for flats
            if (glyphs.first()
                      .getShape() == Shape.FLAT) {
                realPos += 0.75;
            }

            int delta = (int) Math.rint(realPos - theoPos);

            if (logger.isFineEnabled()) {
                logger.fine(
                    "theoPos=" + theoPos + " realPos=" + realPos + " delta=" +
                    delta);
            }

            clefKind = deltaToClef(delta);

            if (clefKind == null) {
                if (logger.isFineEnabled()) {
                    logger.fine("Cannot guess Clef from Key signature");
                }
            }
        }

        return clefKind;
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, so that it gets lazily recomputed when needed
     */
    private void reset ()
    {
        setCenter(null);
        key = null;
        shape = null;
        contour = null;
        centroid = null;
        clefKind = null;
    }

    //-------------//
    // retrieveKey //
    //-------------//
    /**
     * Compute the key of this signature, based on the member glyphs (shape and
     * number)
     */
    private void retrieveKey ()
    {
        if (glyphs.size() > 0) {
            // Check we have only sharps or only flats
            Shape shape = null;

            for (Glyph glyph : glyphs) {
                if ((shape != null) && (glyph.getShape() != shape)) {
                    if (logger.isFineEnabled()) {
                        logger.fine("Inconsistent key signature " + this);
                    }

                    return;
                } else {
                    shape = glyph.getShape();
                }
            }

            // Number and shape determine key signature
            if (shape == SHARP) {
                key = glyphs.size();
            } else if (shape == FLAT) {
                key = -glyphs.size();
            } else {
                addError("Weird key signature " + this);
            }
        } else {
            addError("Empty key signature " + this);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /**
         * Abscissa margin when looking up for glyph neighbors
         */
        Scale.Fraction xMargin = new Scale.Fraction(
            1d,
            "Abscissa margin when looking up for glyph neighbors");

        /**
         * Ordinate margin when looking up for glyph neighbors
         */
        Scale.Fraction yMargin = new Scale.Fraction(
            1d,
            "Ordinate margin when looking up for glyph neighbors");

        /**
         * Margin when checking single-glyph key ordinate
         */
        Scale.Fraction keyYMargin = new Scale.Fraction(
            0.25d,
            "Margin when checking vertical position of single-glyph key");
    }
}
