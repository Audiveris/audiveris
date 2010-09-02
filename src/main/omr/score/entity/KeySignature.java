//----------------------------------------------------------------------------//
//                                                                            //
//                          K e y S i g n a t u r e                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import static omr.score.entity.Note.Step.*;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.TreeNode;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class <code>KeySignature</code> encapsulates a key signature, which may be
 * composed of one or several glyphs (all sharp-based or all flat-based).
 *
 * @author Hervé Bitteur
 */
public class KeySignature
    extends MeasureNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(KeySignature.class);

    /** Standard (in G clef) pitch position for the members of the sharp keys */
    private static final double[] sharpPositions = new double[] {
                                                       -4, // F - Fa
    -1, // C - Do
    -5, // G - Sol
    -2, // D - Ré
    +1, // A - La
    -3, // E - Mi
    0 // B - Si
                                                   };

    /** Standard (in G clef) pitch position of any sharp key signature */
    private static final double[] sharpKeyPositions = new double[1 + 7];

    static {
        for (int k = 0; k < sharpKeyPositions.length; k++) {
            sharpKeyPositions[k] = getStandardPosition(k);
        }
    }

    /** Note steps according to sharp key */
    private static final Note.Step[] sharpSteps = new Note.Step[] {
                                                      F, C, G, D, A, E, B
                                                  };

    /** Standard(in G clef) pitch position  for the members of the flat keys */
    private static final double[] flatPositions = new double[] {
                                                      0, // B - Si
    -3, // E - Mi
    +1, // A - La
    -2, // D - Ré
    +2, // G - Sol
    -1, // C - Do
    +3 // F - Fa
                                                  };

    /** Standard (in G clef) pitch position of any flat key signature */
    private static final double[] flatKeyPositions = new double[1 + 7];

    static {
        for (int k = 0; k < flatKeyPositions.length; k++) {
            flatKeyPositions[k] = getStandardPosition(-k);
        }
    }

    /** Note steps according to flat key */
    private static final Note.Step[] flatSteps = new Note.Step[] {
                                                     B, E, A, D, G, C, F
                                                 };

    /** Unique Id (for debugging). Having a static variable is stupid !!! TODO */
    private static AtomicInteger globalId = new AtomicInteger(0);

    //~ Instance fields --------------------------------------------------------

    /** Unique Id (debugging) */
    private final int id;

    /** Precise key signature. 0 for none, +n for n sharps, -n for n flats */
    private Integer key;

    /** Global pitch position */
    private Double pitchPosition;

    /** Related shape for drawing */
    private Shape shape;

    /** Center of mass of the key sig */
    private PixelPoint centroid;

    /** Related Clef kind (G, F or C) */
    private Shape clefKind;

    //~ Constructors -----------------------------------------------------------

    //--------------//
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
        id = newId();

        if (logger.isFineEnabled()) {
            logger.fine(getContextString() + " KeySignature created: " + this);
        }
    }

    //--------------//
    // KeySignature //
    //--------------//
    /**
     * Create a key signature, with containing measure by cloning an other one
     *
     * @param measure the containing measure
     * @param staff the related staff
     * @param other the key sig to clone
     */
    public KeySignature (Measure      measure,
                         Staff        staff,
                         KeySignature other)
    {
        super(measure);
        setStaff(staff);
        id = newId();
        key = other.getKey();
        pitchPosition = other.getPitchPosition();
        shape = other.getShape();
        clefKind = other.clefKind;

        // Nota: Center.y is irrelevant
        setCenter(new SystemPoint(other.getCenter().x, 0));

        if (logger.isFineEnabled()) {
            logger.fine(getContextString() + " KeySignature cloned: " + this);
        }
    }

    //~ Methods ----------------------------------------------------------------

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
            clefKind = getClefKind();

            if (clefKind == null) {
                clefKind = G_CLEF;
            }

            pitchPosition = getStandardPosition(getKey()) +
                            clefToDelta(clefKind);
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
            switch (getKey()) {
            case -1 :
                return KEY_FLAT_1;

            case -2 :
                return KEY_FLAT_2;

            case -3 :
                return KEY_FLAT_3;

            case -4 :
                return KEY_FLAT_4;

            case -5 :
                return KEY_FLAT_5;

            case -6 :
                return KEY_FLAT_6;

            case -7 :
                return KEY_FLAT_7;

            case 1 :
                return KEY_SHARP_1;

            case 2 :
                return KEY_SHARP_2;

            case 3 :
                return KEY_SHARP_3;

            case 4 :
                return KEY_SHARP_4;

            case 5 :
                return KEY_SHARP_5;

            case 6 :
                return KEY_SHARP_6;

            case 7 :
                return KEY_SHARP_7;

            default :
                return null;
            }
        }

        return shape;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //-----------------//
    // createDummyCopy //
    //-----------------//
    public KeySignature createDummyCopy (Measure     measure,
                                         SystemPoint center)
    {
        KeySignature dummy = new KeySignature(measure, null);

        dummy.key = this.key;
        dummy.setCenter(center);

        return dummy;
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

        ScoreSystem system = measure.getSystem();
        SystemInfo  systemInfo = system.getInfo();

        // Make sure the glyph pitch position is within the bounds
        double pitchPosition = staff.pitchPositionOf(center);

        if ((pitchPosition < -5) || (pitchPosition > 5)) {
            if (logger.isFineEnabled()) {
                logger.fine("Glyph not within vertical bounds");
            }

            return false;
        }

        // Make sure we have no note nearby
        // Use a enlarged rectangular box around the glyph, and check what's in
        // Check for lack of stem symbols (beam, beam hook, note head, flags),
        // or stand-alone note (THIS IS TOO RESTRICTIVE!!!)
        PixelRectangle glyphFatBox = glyph.getContourBox();
        glyphFatBox.grow(
            measure.getScale().toPixels(constants.xMargin),
            measure.getScale().toPixels(constants.yMargin));

        List<Glyph> neighbors = systemInfo.lookupIntersectedGlyphs(
            glyphFatBox,
            glyph);

        for (Glyph g : neighbors) {
            Shape shape = g.getShape();

            if (ShapeRange.StemSymbols.contains(shape) ||
                ShapeRange.Notes.getShapes()
                                .contains(shape)) {
                if (logger.isFineEnabled()) {
                    logger.fine("Cannot accept " + shape + " as neighbor");
                }

                return false;
            }
        }

        // Do we have a key signature just before in the same measure & staff?
        SystemRectangle fatBox = system.toSystemRectangle(glyphFatBox);
        KeySignature    keysig = null;
        boolean         found = false;

        for (TreeNode node : measure.getKeySignatures()) {
            keysig = (KeySignature) node;

            if (keysig.getCenter().x > center.x) {
                break;
            }

            // Check distance
            if (!fatBox.intersects(keysig.getBox())) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Glyph " + glyph.getId() + " too far from " + keysig);
                }

                continue;
            } else if (((glyph.getShape().isSharpBased()) &&
                       (keysig.getKey() < 0)) ||
                       ((glyph.getShape().isFlatBased()) &&
                       (keysig.getKey() > 0))) {
                // Check sharp or flat key sig, wrt current glyph
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Cannot extend opposite key signature with glyph " +
                        glyph.getId());
                }

                return false;
            } else {
                // Everything is OK
                found = true;

                if (logger.isFineEnabled()) {
                    logger.fine("Extending " + keysig);
                }

                break;
            }
        }

        // If not found create a brand new one
        if (!found) {
            // Check pitch position
            Clef clef = measure.getClefBefore(
                measure.computeGlyphCenter(glyph));

            if (!checkPitchPosition(glyph, center, staff, clef)) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Cannot start a new key signature with glyph " +
                        glyph.getId());
                }

                return false;
            }

            keysig = new KeySignature(measure, staff);
        }

        // Extend the keysig with this glyph
        keysig.addGlyph(glyph);
        keysig.getKey();
        glyph.setTranslation(keysig);

        if (logger.isFineEnabled()) {
            logger.fine("OK: " + keysig);
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
        sb.append("{KeySignature#")
          .append(id);

        try {
            sb.append(" key=")
              .append(key);
            sb.append(" center=")
              .append(getCenter());
            sb.append(" box=")
              .append(getBox());
            sb.append(" pitch=")
              .append(getPitchPosition());
            sb.append(" ")
              .append(Glyphs.toString(glyphs));
        } catch (Exception e) {
            sb.append("INVALID");
        }

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
    @SuppressWarnings("unchecked")
    public static void verifySystemKeys (ScoreSystem system)
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

                            try {
                                ks.copyKey(keysig);
                            } catch (Exception ex) {
                                logger.warning("Cannot copy key", ex);
                                ks.addError("Cannot copy key");
                            }

                            // TODO deassign glyphs that do not contribute to the key ?
                        }
                    }
                }
            }
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, so that it gets lazily recomputed when needed
     */
    @Override
    protected void reset ()
    {
        super.reset();

        setCenter(null);
        key = null;
        pitchPosition = null;
        shape = null;
        centroid = null;
        clefKind = null;
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

        case PERCUSSION_CLEF :
            return PERCUSSION_CLEF;

        default :
            return null;
        }
    }

    //------------------//
    // getContextString //
    //------------------//
    private static String getContextString (ScoreSystem system,
                                            int         measureIndex,
                                            int         systemStaffIndex)
    {
        return system.getContextString() + "M" + (measureIndex + 1) + "F" +
               staffOf(system, systemStaffIndex)
                   .getId();
    }

    //--------------//
    // getMeasureOf //
    //--------------//
    private static Measure getMeasureOf (ScoreSystem system,
                                         int         staffIndex,
                                         int         measureIndex)
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
     * @param clef current clef, if known
     * @return true if OK, false otherwise
     */
    private static boolean checkPosition (Glyph       glyph,
                                          SystemPoint center,
                                          Staff       staff,
                                          double[]    positions,
                                          int         index,
                                          Clef        clef)
    {
        int[] deltas = (clef != null)
                       ? new int[] { clefToDelta(clef.getShape()) }
                       : new int[] { 0, 2, 1 };

        for (int delta : deltas) {
            double dif = staff.pitchPositionOf(center) - positions[index] -
                         delta;

            if (Math.abs(dif) <= (constants.keyYMargin.getValue() * 2)) {
                if (logger.isFineEnabled()) {
                    logger.fine(
                        "Correct pitch position for glyph " + glyph.getId());
                }

                return true;
            }
        }

        if (logger.isFineEnabled()) {
            logger.fine("No valid pitch position for glyph " + glyph.getId());
        }

        return false;
    }

    //-------------//
    // clefToDelta //
    //-------------//
    /**
     * Report the delta in pitch position (wrt standard G_CLEF positions)
     * according to a given clef
     *
     * @param clef the clef
     * @return the delta in pitch position
     */
    private static int clefToDelta (Shape clef)
    {
        switch (getClefKind(clef)) {
        case F_CLEF :
            return 2;

        case C_CLEF :
            return 1;

        default :
        case G_CLEF :
            return 0;
        }
    }

    //-------//
    // keyOf //
    //-------//
    private static Integer keyOf (Shape shape)
    {
        switch (shape) {
        case KEY_FLAT_1 :
            return -1;

        case KEY_FLAT_2 :
            return -2;

        case KEY_FLAT_3 :
            return -3;

        case KEY_FLAT_4 :
            return -4;

        case KEY_FLAT_5 :
            return -5;

        case KEY_FLAT_6 :
            return -6;

        case KEY_FLAT_7 :
            return -7;

        case KEY_SHARP_1 :
            return 1;

        case KEY_SHARP_2 :
            return 2;

        case KEY_SHARP_3 :
            return 3;

        case KEY_SHARP_4 :
            return 4;

        case KEY_SHARP_5 :
            return 5;

        case KEY_SHARP_6 :
            return 6;

        case KEY_SHARP_7 :
            return 7;

        default :
            return null;
        }
    }

    //-------//
    // newId //
    //-------//
    private static int newId ()
    {
        return globalId.incrementAndGet();
    }

    //---------//
    // staffOf //
    //---------//
    private static Staff staffOf (ScoreSystem system,
                                  int         systemStaffIndex)
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

    //---------------------//
    // getStandardPosition //
    //---------------------//
    /**
     * Compute the standard mean pitch position of the provided key
     *
     * @param k the provided key value
     * @return the corresponding standard mean pitch position
     */
    private static double getStandardPosition (int k)
    {
        if (k == 0) {
            return 0;
        }

        double sum = 0;

        if (k > 0) {
            for (int i = 0; i < k; i++) {
                sum += sharpPositions[i];
            }
        } else {
            for (int i = 0; i > k; i--) {
                sum -= flatPositions[-i];
            }
        }

        return sum / k;
    }

    //---------------------//
    // getStandardPosition //
    //---------------------//
    /**
     * Return the standard (assuming G clef) mean pitch position, based on known
     * key value
     *
     * @return the standard mean pitch position
     */
    private double getStandardPosition ()
    {
        if (getKey() >= 0) {
            return sharpKeyPositions[key];
        } else {
            return flatKeyPositions[key];
        }
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
     * @param clef clef at this location, if known
     * @return true if OK, false otherwise
     */
    private static boolean checkPitchPosition (Glyph       glyph,
                                               SystemPoint center,
                                               Staff       staff,
                                               Clef        clef)
    {
        Shape glyphShape = glyph.getShape();

        if (glyphShape == SHARP) {
            return checkPosition(glyph, center, staff, sharpPositions, 0, clef);
        } else if (glyphShape.isSharpBased()) {
            return checkPosition(
                glyph,
                center,
                staff,
                sharpKeyPositions,
                keyOf(glyphShape),
                clef);
        }

        if (glyphShape == FLAT) {
            return checkPosition(glyph, center, staff, flatPositions, 0, clef);
        } else if (glyphShape.isFlatBased()) {
            return checkPosition(
                glyph,
                center,
                staff,
                flatKeyPositions,
                -keyOf(glyphShape),
                clef);
        }

        return false;
    }

    //-------------//
    // getClefKind //
    //-------------//
    /**
     * Report the current clef kind, if known
     * @return the current clef kind, or null
     */
    private Shape getClefKind ()
    {
        if (clefKind == null) {
            // First is there a clef right before, within the same measure?
            Clef clef = getMeasure()
                            .getMeasureClefBefore(getCenter(), null);

            if (clef != null) {
                return clefKind = clefKindOf(clef.getShape());
            }

            // Second, guess the clef based on key position
            clefKind = guessClefKind();
        }

        return clefKind;
    }

    //------------//
    // clefKindOf //
    //------------//
    /**
     * Report the kind of the provided clef
     * @param clef the provided clef
     * @return the kind of the clef
     */
    private Shape clefKindOf (Shape clef)
    {
        switch (clef) {
        case G_CLEF :
        case G_CLEF_OTTAVA_ALTA :
        case G_CLEF_OTTAVA_BASSA :
            return G_CLEF;

        case C_CLEF :
            return C_CLEF;

        case F_CLEF :
        case F_CLEF_OTTAVA_ALTA :
        case F_CLEF_OTTAVA_BASSA :
            return F_CLEF;

        case PERCUSSION_CLEF :
            return null;

        default :
            return null;
        }
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
        //        if (logger.isFineEnabled()) {
        //            logger.fine("Copying " + ks + " to " + this);
        //        }

        // Nota: We don't touch to contour, nor centroid

        // key
        key = ks.getKey();

        // Beware of different clef kinds. What if we ignore a clef?
        Measure measure = getMeasure();
        Clef    clef = measure.getClefBefore(ks.getCenter(), getStaff());
        Shape   kind = (clef != null) ? getClefKind(clef.getShape())
                       : Shape.G_CLEF;

        Measure ms = ks.getMeasure();
        Clef    c = ms.getClefBefore(ks.getCenter());
        Shape   k = (c != null) ? getClefKind(c.getShape()) : Shape.G_CLEF;

        int     delta = clefToDelta(kind) - clefToDelta(k);

        // center
        setCenter(
            new SystemPoint(
                ks.getCenter().x,
                ks.getCenter().y + Staff.pitchToUnit(delta)));

        // pitchPosition
        pitchPosition = ks.getPitchPosition() + delta;
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
            return G_CLEF;

        case 1 :
            return C_CLEF;

        case 2 :
            return F_CLEF;

        default :
            return null;
        }
    }

    //---------------//
    // guessClefKind //
    //---------------//
    /**
     * Guess what the current clef kind (G, F or C) is, based on the pitch
     * positions of the member glyphs
     *
     * @return the kind of clef
     */
    private Shape guessClefKind ()
    {
        double theoPos = getStandardPosition(getKey());
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

        Shape kind = deltaToClef(delta);

        if (kind == null) {
            if (logger.isFineEnabled()) {
                logger.fine("Cannot guess Clef from Key signature");
            }
        }

        return kind;
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
        if ((glyphs != null) && !glyphs.isEmpty()) {
            // Check we have only sharps or only flats
            Shape kind = null;
            int   k = 0;

            for (Glyph glyph : glyphs) {
                Shape glyphShape = glyph.getShape();

                if (glyphShape.isFlatBased()) {
                    if (kind == SHARP) {
                        if (logger.isFineEnabled()) {
                            logger.fine("Inconsistent key signature " + this);
                        }

                        return;
                    } else {
                        kind = FLAT;
                    }
                }

                if (glyphShape.isSharpBased()) {
                    if (kind == FLAT) {
                        if (logger.isFineEnabled()) {
                            logger.fine("Inconsistent key signature " + this);
                        }

                        return;
                    } else {
                        kind = SHARP;
                    }
                }

                // Update key value
                if (glyphShape == SHARP) {
                    k += 1;
                } else if (glyphShape == FLAT) {
                    k -= 1;
                } else {
                    k += keyOf(glyphShape);
                }
            }

            key = k;
        } else {
            addError("Empty key signature");
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

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
