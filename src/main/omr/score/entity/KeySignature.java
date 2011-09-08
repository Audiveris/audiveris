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

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.ShapeRange;
import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.math.Histogram;
import omr.math.Histogram.Pair;

import omr.run.Orientation;

import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import static omr.score.entity.Note.Step.*;
import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.TreeNode;

import java.util.ArrayList;
import java.util.List;

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
    private static final double[] sharpItemPositions = new double[] {
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
    private static final double[] flatItemPositions = new double[] {
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

    //~ Instance fields --------------------------------------------------------

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

    /** Sequence of items abscissa references */
    private List<Integer> refList;

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

        if (logger.isFineEnabled()) {
            logger.fine(getContextString() + " KeySignature created: " + this);
        }
    }

    //--------------//
    // KeySignature //
    //--------------//
    /**
     * Create a key signature, with containing measure, by cloning an other one
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
        key = other.getKey();
        pitchPosition = other.getPitchPosition();
        shape = other.getShape();
        clefKind = other.clefKind;

        // Nota: Center.y is irrelevant
        setCenter(new PixelPoint(other.getCenter().x, 0));

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

    //-----------------//
    // getItemPosition //
    //-----------------//
    /**
     * Report the pitch position of the nth item, within the given clef. 'n' is
     * negative for flats and positive for sharps, and start at 1 for sharps
     * (and at -1 for flats)
     * @param n the signed index (one-based) of the desired item
     * @param clefKind the kind (G_CLEF, F_CLEF or C_CLEF) of the active clef
     * @return the pitch position of the item (sharp or flat)
     */
    public static int getItemPosition (int   n,
                                       Shape clefKind)
    {
        if (clefKind == null) {
            clefKind = G_CLEF;
        }

        int stdPitch = (int) Math.rint(
            (n >= 0) ? sharpItemPositions[n - 1] : flatItemPositions[-n - 1]);

        return stdPitch + clefToDelta(clefKind);
    }

    //-------------//
    // getClefKind //
    //-------------//
    /**
     * Report the current clef kind, if known
     * @return the current clef kind, or null
     */
    public Shape getClefKind ()
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

    //----------------------//
    // getItemPixelAbscissa //
    //----------------------//
    /**
     * Report the actual reference abscissa for the nth item. The reference is
     * the left edge of stick for a flat item and the center of the two sticks
     * for a sharp item.
     * @param n the signed index (one-based) of the desired item
     * @return the absolute pixel abscissa of the item reference, or null if
     * not available
     */
    public Integer getItemPixelAbscissa (int n)
    {
        try {
            return getRefSequence()
                       .get(Math.abs(n) - 1);
        } catch (Exception ex) {
            addError("KeySignature with no items references");

            return null;
        }
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

    //----------------//
    // getRefSequence //
    //----------------//
    /**
     * Report the sequence of reference abscissae for signature items
     * @return the list of (pixel) abscissa references
     */
    public List<Integer> getRefSequence ()
    {
        if (refList == null) {
            List<Integer>      refs = new ArrayList<Integer>();
            Histogram<Integer> histo = getPage()
                                           .getSheet()
                                           .getVerticalLag()
                                           .getHistogram(
                Orientation.VERTICAL,
                getGlyphs());

            if (logger.isFineEnabled()) {
                histo.print(System.out);
            }

            List<Histogram.Pair<Integer>> pairs = histo.getPeaks(
                ((int) Math.rint(
                    histo.getMaxCount() * constants.heightRatio.getValue())));

            if (logger.isFineEnabled()) {
                for (Histogram.Pair<Integer> pair : pairs) {
                    logger.fine(pair.toString());
                }
            }

            key = getKey();

            if (key > 0) {
                // Sharps : use center of the two vertical sticks
                if (pairs.size() == (2 * key)) {
                    for (int i = 0; i < key; i++) {
                        Pair<Integer> left = pairs.get(2 * i);
                        Pair<Integer> right = pairs.get((2 * i) + 1);
                        refs.add(
                            (int) Math.rint(
                                (left.first + left.second + right.first +
                                                                right.second) / 4d));
                    }
                }
            } else {
                // Flats : use vertical stick on left
                if (pairs.size() == -key) {
                    for (Pair<Integer> pair : pairs) {
                        refs.add(pair.first);
                    }
                }
            }

            refList = refs;
        }

        return refList;
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

    //---------------------//
    // getStandardPosition //
    //---------------------//
    /**
     * Compute the standard mean pitch position of the provided key
     *
     * @param k the provided key value
     * @return the corresponding standard mean pitch position
     */
    public static double getStandardPosition (int k)
    {
        if (k == 0) {
            return 0;
        }

        double sum = 0;

        if (k > 0) {
            for (int i = 0; i < k; i++) {
                sum += sharpItemPositions[i];
            }
        } else {
            for (int i = 0; i > k; i--) {
                sum -= flatItemPositions[-i];
            }
        }

        return sum / k;
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
    public KeySignature createDummyCopy (Measure    measure,
                                         PixelPoint center)
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
    public static boolean populate (Glyph      glyph,
                                    Measure    measure,
                                    Staff      staff,
                                    PixelPoint center)
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
        KeySignature keysig = null;
        boolean      found = false;

        for (TreeNode node : measure.getKeySignatures()) {
            keysig = (KeySignature) node;

            if (keysig.getCenter().x > center.x) {
                break;
            }

            // Check distance
            if (!glyphFatBox.intersects(keysig.getBox())) {
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
                measure.computeGlyphCenter(glyph),
                staff);

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
        sb.append("{KeySignature");

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
        refList = null;
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
    private static boolean checkPitchPosition (Glyph      glyph,
                                               PixelPoint center,
                                               Staff      staff,
                                               Clef       clef)
    {
        Shape glyphShape = glyph.getShape();

        if (glyphShape == SHARP) {
            return checkPosition(
                glyph,
                center,
                staff,
                sharpItemPositions,
                0,
                clef);
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
            return checkPosition(
                glyph,
                center,
                staff,
                flatItemPositions,
                0,
                clef);
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
    private static boolean checkPosition (Glyph      glyph,
                                          PixelPoint center,
                                          Staff      staff,
                                          double[]   positions,
                                          int        index,
                                          Clef       clef)
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

    //------------//
    // clefKindOf //
    //------------//
    /**
     * Report the kind of the provided clef
     * @param clef the provided clef
     * @return the kind of the clef
     */
    private static Shape clefKindOf (Shape clef)
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
        Constant.Ratio heightRatio = new Constant.Ratio(
            0.5d,
            "Histogram ratio for detection of sharp/flat sticks ");
    }
}
