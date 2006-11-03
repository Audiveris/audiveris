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

import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.score.visitor.Visitor;

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

    //~ Instance fields --------------------------------------------------------

    /** Precise key signature. 0 for none, +n for n sharps, -n for n flats */
    private Integer key;

    /** Related shape for drawing */
    private Shape shape;

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

    /**
     * Dummy entry for test, please ignore
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
            computekey();
        }

        return key;
    }

    public double getPitchPosition ()
    {
        switch (getKey()) {
        case -1 :
            return 0.0;

        case -2 :
            return -3.0 / 2;

        case -3 :
            return -2.0 / 3;

        case -4 :
            return -4.0 / 4;

        case -5 :
            return -9.0 / 5;

        case -6 :
            return -10.0 / 6;

        case -7 :
            return -14.0 / 7;

        case 1 :
            return -4.0;

        case 2 :
            return -5.0 / 2;

        case 3 :
            return -10.0 / 3;

        case 4 :
            return -12.0 / 4;

        case 5 :
            return -11.0 / 5;

        case 6 :
            return -14.0 / 6;

        case 7 :
            return -14.0 / 7;
        }

        return 0;
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
                shape = Shape.values()[(Shape.KEY_FLAT_1.ordinal() - key) + 1];
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

        // Make sure we have no note nearby (test already done ?)
        // Use a widened rectangular box around the glyph, and check what's in
        PixelRectangle box = glyph.getContourBox();
        final int      dx = scale.toPixels(constants.xMargin);
        final int      dy = scale.toPixels(constants.yMargin);
        PixelRectangle rect = new PixelRectangle(
            box.x - dx,
            box.y - dy,
            box.width + (2 * dx),
            box.height + (2 * dy));
        List<Glyph>    neighbors = systemInfo.lookupIntersectedGlyphs(
            rect,
            glyph);

        for (Glyph g : neighbors) {
            logger.info("  Neighbor " + g.getId() + " " + g.getShape());
        }

        // Check for lack of stem symbols (beam, beam hook, note head, flags),
        // or stand-alone note
        for (Glyph g : neighbors) {
            Shape shape = g.getShape();

            if (Shape.StemSymbols.contains(shape) ||
                Shape.Notes.getShapes()
                           .contains(shape)) {
                logger.info("*** Cannot accept " + shape);

                return false;
            }
        }

        // Do we have a (beginning of) key signature nearby ?
        KeySignature keysig = null;

        for (TreeNode node : measure.getKeySignatures()) {
            keysig = (KeySignature) node;

            // Check distance TBD
            // Check sharp or flat, wrt to current glyph
        }

        // If so, just try to extend it, else create a brand new one
        if (keysig == null) {
            keysig = new KeySignature(measure, scale);
        }

        keysig.addGlyph(glyph);
        logger.info(">>>>>>>>>>>>>>> key=" + keysig.getKey());

        // Then, processing
        return true;
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
    }
}
