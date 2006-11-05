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

    /** Related Clef shape (G, F or C) */
    private Shape clefShape;

    /**
     * The glyph(s) that compose the key signature, a collection which is kept
     * sorted on glyph abscissa.
     */
    private SortedSet<Glyph> glyphs = new TreeSet<Glyph>();

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

    //~ Methods ----------------------------------------------------------------

    //
    //    /**
    //     * Dummy entry for test, please ignore
    //     */
    //    public KeySignature (Measure measure,
    //                         int     key)
    //    {
    //        super(measure, measure.getStaff());
    //        this.key = key;
    //        center = new StaffPoint(
    //            measure.getLeftX() + (measure.getWidth() / 2),
    //            0);
    //    }
    //

    //-------------//
    // getCentroid //
    //-------------//
    /**
     * Report the actual center of mass of the glyphs that compose the signature
     *
     * @return the PixelPoint that represent the center of mass
     */
    public PixelPoint getCentroid ()
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
    public PixelRectangle getContour ()
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
            computekey();
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
            Shape guess = guessClefShape();

            if (guess == null) {
                guess = G_CLEF;
            }

            switch (guess) {
            case G_CLEF :
            case G_CLEF_OTTAVA_ALTA :
            case G_CLEF_OTTAVA_BASSA :
                return pitchPosition = getMeanPosition();

            case F_CLEF :
            case F_CLEF_OTTAVA_ALTA :
            case F_CLEF_OTTAVA_BASSA :
                return pitchPosition = 2 + getMeanPosition();

            case C_CLEF : // Assuming Alto
                return pitchPosition = 1 + getMeanPosition();

            default :
            }
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
    // addGlyph //
    //----------//
    /**
     * Add a new glyph as part of this key signature
     *
     * @param glyph the new component glyph
     */
    public void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);
        reset();
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, so that it gets lazily recomputed when needed
     */
    public void reset ()
    {
        center = null;
        key = null;
        shape = null;
        contour = null;
        centroid = null;
        clefShape = null;
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

        sb.append(" glyphs[");

        for (Glyph glyph : glyphs) {
            sb.append("#")
              .append(glyph.getId());
        }

        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // computeCenter //
    //---------------//
    @Override
    protected void computeCenter ()
    {
        center = computeGlyphsCenter(glyphs);
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
    static boolean populate (Glyph   glyph,
                             Measure measure,
                             Scale   scale)
    {
        logger.info("Keysig for " + glyph);

        System         system = measure.getStaff()
                                       .getSystem();
        SystemInfo     systemInfo = system.getInfo();
        StaffPoint     center = measure.computeGlyphCenter(glyph);

        // Make sure we have no note nearby (test already done ?)
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
                logger.info("Cannot accept " + shape + " as neighbor");

                return false;
            }
        }

        // Do we have a (beginning of) key signature just before ?
        KeySignature keysig = null;
        boolean      found = false;

        for (TreeNode node : measure.getKeySignatures()) {
            keysig = (KeySignature) node;

            if (keysig.getCenter().x > center.x) {
                break;
            }

            // Check distance
            if (!glyphFatBox.intersects(keysig.getContour())) {
                logger.warning(
                    "Glyph " + glyph.getId() +
                    " too far from previous key signature");

                continue;
            } else {
                // Check sharp or flat key sig, wrt to current glyph
                if (((glyph.getShape() == SHARP) && (keysig.getKey() < 0)) ||
                    ((glyph.getShape() == FLAT) && (keysig.getKey() > 0))) {
                    logger.warning(
                        "Cannot extend key signature with glyph " +
                        glyph.getId());

                    return false;
                }

                // Check pitch positions

                // Everything is OK
                found = true;

                break;
            }
        }

        // If so, just try to extend it, else create a brand new one
        if (!found) {
            // Check pitch position
            Shape clefShape = null;
            Clef  clef = measure.getLastClef();

            if (clef == null) {
                clef = measure.getPreviousClef();
            }

            if (clef != null) {
                clefShape = clef.getShape();
            } else {
                // Assume G clef
                clefShape = G_CLEF;
            }

            if (glyph.getShape() == SHARP) {
                if ((clefShape == G_CLEF) ||
                    (clefShape == G_CLEF_OTTAVA_ALTA) ||
                    (clefShape == G_CLEF_OTTAVA_BASSA)) {
                    if (Math.abs(glyph.getPitchPosition() - sharpPositions[0]) > constants.pitchMargin.getValue()) {
                        logger.warning(
                            "Invalid pitch position for glyph " +
                            glyph.getId());

                        return false;
                    } else {
                        logger.info(
                            "Correct G pitch position for glyph " +
                            glyph.getId());
                    }
                } else if ((clefShape == Shape.F_CLEF) ||
                           (clefShape == Shape.F_CLEF_OTTAVA_ALTA) ||
                           (clefShape == Shape.F_CLEF_OTTAVA_BASSA)) {
                    if (Math.abs(
                        glyph.getPitchPosition() - sharpPositions[0] - 2) > constants.pitchMargin.getValue()) {
                        logger.warning(
                            "Invalid pitch position for glyph " +
                            glyph.getId());

                        return false;
                    } else {
                        logger.info(
                            "Correct F pitch position for glyph " +
                            glyph.getId());
                    }
                } else if (clefShape == Shape.C_CLEF) {
                    if (Math.abs(
                        glyph.getPitchPosition() - sharpPositions[0] - 1) > constants.pitchMargin.getValue()) {
                        logger.warning(
                            "Invalid pitch position for glyph " +
                            glyph.getId());

                        return false;
                    } else {
                        logger.info(
                            "Correct C pitch position for glyph " +
                            glyph.getId());
                    }
                }
            }

            keysig = new KeySignature(measure, scale);
        }

        keysig.addGlyph(glyph);
        logger.info("key=" + keysig.getKey());

        return true;
    }

    //-----------------//
    // getMeanPosition //
    //-----------------//
    private double getMeanPosition ()
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

    //------------//
    // computekey //
    //------------//
    private void computekey ()
    {
        if (glyphs.size() > 0) {
            // Check we have only sharps or only flats
            Shape shape = null;

            for (Glyph glyph : glyphs) {
                if ((shape != null) && (glyph.getShape() != shape)) {
                    logger.warning("Inconsistent key signature " + this);

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

    //----------------//
    // guessClefShape //
    //----------------//
    private Shape guessClefShape ()
    {
        if (clefShape == null) {
            double theoPos = getMeanPosition();
            double realPos = getStaff()
                                 .pitchPositionOf(getCentroid());
            int    delta = (int) Math.rint(realPos - theoPos);
            logger.info(
                this + " theoPos=" + theoPos + " realPos=" + realPos +
                " delta=" + delta);

            switch (delta) {
            case 0 :
                return clefShape = G_CLEF;

            case 1 :
                return clefShape = C_CLEF;

            case 2 :
                return clefShape = F_CLEF;
            }

            logger.warning("Cannot guess Clef from Key signature");

            return clefShape = null;
        }

        return Shape.G_CLEF; ///// PAS BEAU !!!!!
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
