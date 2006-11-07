//----------------------------------------------------------------------------//
//                                                                            //
//                          K e y S i g n a t u r e                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.score.visitor.Visitor;

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
    extends StaffNode
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
    -2, // D - Ré
    +1, // A - La
    -3, // E - Mi
    0 // B - Si
                                                   };

    /** Pitch position for the members of the flat keys assuming a G clef */
    private static final double[] flatPositions = new double[] {
                                                      0, // B - Si
    -3, // E - Mi
    +1, // A - La
    -2, // D - Ré
    +2, // G - Sol
    -1, // C - Do
    +3 // F - Fa
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

    /** Unlucky glyphs candidates */
    private SortedSet<Glyph> parias = new TreeSet<Glyph>();

    //~ Constructors -----------------------------------------------------------

    //---------------//
    // KeySignature //
    //--------------//
    /**
     * Create a key signature, with related sheet scale and containing staff
     *
     * @param measure the containing measure
     * @param scale the sheet global scale
     */
    public KeySignature (Measure measure,
                         Scale   scale)
    {
        super(measure, measure.getStaff());
    }

    //--------------//
    // KeySignature //
    //--------------//
    /**
     * Entry for generating a dummy keysig, please ignore.
     */
    public KeySignature (Measure measure,
                         int     key)
    {
        super(measure, measure.getStaff());

        this.key = key;
        center = new StaffPoint(
            measure.getLeftX() + (measure.getWidth() / 2),
            0);
    }

    //~ Methods ----------------------------------------------------------------

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

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (Visitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // populate //
    //----------//
    /**
     * Populate the score with a key signature built from the provided glyph
     *
     * @param glyph the source glyph
     * @param measure containing measure
     * @param scale sheet scale
     *
     * @return true if population is successful, false otherwise
     */
    public static boolean populate (Glyph   glyph,
                                    Measure measure,
                                    Scale   scale)
    {
        if (logger.isFineEnabled()) {
            logger.fine("Populating keysig for " + glyph);
        }

        System         system = measure.getStaff()
                                       .getSystem();
        SystemInfo     systemInfo = system.getInfo();
        StaffPoint     center = measure.computeGlyphCenter(glyph);

        // Make sure we have no note nearby
        // Use a enlarged rectangular box around the glyph, and check what's in
        PixelRectangle box = glyph.getContourBox();
        final int      dx = scale.toPixels(constants.xMargin);
        final int      dy = scale.toPixels(constants.yMargin);
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
                // Check sharp or flat key sig, wrt to current glyph
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

            if (!checkPitchPosition(glyph, 0, getClefKind(clefShape))) {
                return false;
            }

            keysig = new KeySignature(measure, scale);
        }

        // Extend the keysig with this glyph
        keysig.addGlyph(glyph);

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
     * have been generated for the system
     *
     * @param system the system to be verified
     */
    public static void verifySystemKeys (System system)
    {
        // Check key consistency across the various staves
        final int measureNb = system.getNumberOfMeasures();

        for (int im = 0; im < measureNb; im++) {
            // Retrieve the key list for this measure in each staff
            List<List<TreeNode>> keyMatrix = new ArrayList<List<TreeNode>>();

            for (TreeNode node : system.getStaves()) {
                Staff   staff = (Staff) node;
                Measure measure = (Measure) staff.getMeasures()
                                                 .get(im);
                keyMatrix.add(measure.getKeySignatures());
            }

            // Same number of keys in each measure ?
            int nb = -1;

            for (List<TreeNode> list : keyMatrix) {
                nb = Math.max(nb, list.size());
            }

            for (List<TreeNode> list : keyMatrix) {
                if (list.size() < nb) {
                    // Need to add a key here (which index?, which key?)
                    // To Be Implemented TBD
                }
            }

            // Compatible keys ?
            for (int ik = 0; ik < nb; ik++) {
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
                            ks.copyKey(keysig);
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
        center = computeGlyphsCenter(glyphs);
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
     * @param index index in signature
     * @param clefKind kind of clef at this location
     * @return true if OK, false otherwise
     */
    private static boolean checkPitchPosition (Glyph glyph,
                                               int   index,
                                               Shape clefKind)
    {
        if (glyph.getShape() == SHARP) {
            return checkPosition(
                glyph,
                sharpPositions,
                index,
                clefToDelta(clefKind));
        } else {
            return checkPosition(
                glyph,
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
     * @param positions the array of positions (for G clef kind)
     * @param index index of glyph within signature
     * @param delta delta pitch position (based on clef kind)
     * @return true if OK, false otherwise
     */
    private static boolean checkPosition (Glyph    glyph,
                                          double[] positions,
                                          int      index,
                                          int      delta)
    {
        double dif = glyph.getPitchPosition() - positions[index] - delta;

        if (Math.abs(dif) > constants.pitchMargin.getValue()) {
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
        Measure measure = (Measure) getContainer()
                                        .getContainer();
        Clef    clef = measure.getClefBefore(ks.getCenter());
        Measure ms = (Measure) ks.getContainer()
                                 .getContainer();
        Clef    c = ms.getClefBefore(ks.getCenter());
        Shape   kind = getClefKind(clef.getShape());
        Shape   k = getClefKind(c.getShape());
        int     delta = clefToDelta(kind) - clefToDelta(k);

        // center
        center = new StaffPoint(
            ks.getCenter().x,
            ks.getCenter().y + getStaff().pitchToUnit(delta));

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
            int    delta = (int) Math.rint(realPos - theoPos);

            if (logger.isFineEnabled()) {
                logger.fine(
                    this + " theoPos=" + theoPos + " realPos=" + realPos +
                    " delta=" + delta);
            }

            clefKind = deltaToClef(delta);

            if (clefKind == null) {
                if (logger.isFineEnabled()) {
                    logger.fine("Cannot guess Clef from Key signature " + this);
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
        center = null;
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
                logger.warning("Weird key signature " + this);
            }
        } else {
            logger.warning("Empty key signature " + this);
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
            "Abscissa margin (in interline fraction) when looking up for glyph neighbors");

        /**
         * Ordinate margin when looking up for glyph neighbors
         */
        Scale.Fraction yMargin = new Scale.Fraction(
            1d,
            "Ordinate margin (in interline fraction) when looking up for glyph neighbors");

        /**
         * Margin on pitch position
         */
        Constant.Double pitchMargin = new Constant.Double(
            0.5,
            "Margin on pitch position");
    }
}
