//-----------------------------------------------------------------------//
//                                                                       //
//                       T i m e S i g n a t u r e                       //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.score;

import omr.glyph.Glyph;
import omr.glyph.Shape.Range;
import omr.glyph.Shape;
import omr.sheet.Scale;
import omr.ui.icon.SymbolIcon;
import omr.ui.view.Zoom;
import omr.util.Logger;

import static omr.glyph.Shape.*;

import java.awt.Graphics;
import java.util.*;

/**
 * Class <code>TimeSignature</code> encapsulates a time signature, which
 * may be composed of one or several sticks.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class TimeSignature
    extends StaffNode
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(TimeSignature.class);

    // Specific sub-ranges for time signature
    public static final Range SingleTimes = new Range
        (EnumSet.range(TIME_ZERO, TIME_SIXTEEN));
    public static final Range MultiTimes  = new Range
        (EnumSet.range(TIME_FOUR_FOUR, CUT_TIME));

    //~ Instance variables ------------------------------------------------

    // Sheet global scale
    private Scale scale;

    // The glyph(s) that compose the time signature, a collection which is
    // kept sorted on glyph abscissa
    // This can be just one : e.g. TIME_SIX_EIGHT for 6/8
    // Or several : e.g. TIME_SIX  + TIME_TWELVE for 6/12
    private SortedSet<Glyph> glyphs = new TreeSet<Glyph>();

    // Location of the time signature center WRT staff top-left corner
    private StaffPoint center;

    // Rational components
    private Integer numerator;
    private Integer denominator;

    // Precise time signature shape (if any, since we may have no redefined
    // shape for complex time signatures)
    private Shape shape;

    //~ Constructors ------------------------------------------------------

    //---------------//
    // TimeSignature //
    //--------------//
    /**
     * Create a time signature, with related sheet scale and containing staff
     *
     * @param scale the sheet global scale
     * @param staff the containing staff
     */
    public TimeSignature(Measure measure,
                         Staff   staff,
                         Scale   scale)
    {
        super(measure, staff);
        this.scale = scale;
    }

    //~ Methods -----------------------------------------------------------

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, so that it gets lazily recomputed when
     * needed
     */
    public void reset()
    {
        center      = null;
        shape       = null;
        numerator   = null;
        denominator = null;
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape of this time signature
     *
     * @return the (lazily determined) shape
     */
    public Shape getShape()
    {
        if (shape == null) {
            computeRational();
        }

        return shape;
    }

    //--------------//
    // getNumerator //
    //--------------//
    /**
     * Report the top part of the time signature
     *
     * @return the top part
     */
    public Integer getNumerator()
    {
        if (numerator == null) {
            computeRational();
        }

        return numerator;
    }

    //----------------//
    // getDenominator //
    //----------------//
    /**
     * Report the bottom part of the time signature
     *
     * @return the bottom part
     */
    public Integer getDenominator()
    {
        if (denominator == null) {
            computeRational();
        }

        return denominator;
    }

    //-----------//
    // getCenter //
    //-----------//
    /**
     * Report the bounding center of the time signature
     *
     * @return the bounding center (in units wrt staff topleft)
     */
    public StaffPoint getCenter()
    {
        if (center == null) {
            center = staff.computeGlyphsCenter(glyphs, scale);
        }

        return center;
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
        public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{TimeSignature");

        if (getNumerator() != null) {
            sb
                .append(" ").append(getNumerator())
                .append("/").append(getDenominator());
        }

        sb
            .append(" ").append(getShape())
            .append(" center=").append(getCenter());


        sb.append(" glyphs[");
        for (Glyph glyph : glyphs) {
            sb.append("#").append(glyph.getId());
        }
        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    //----------//
    // addGlyph //
    //----------//
    /**
     * Add a new glyph as part of this time signature
     *
     * @param glyph the new component glyph
     */
    public void addGlyph (Glyph glyph)
    {
        glyphs.add(glyph);
        reset();
    }

    //-----------//
    // paintNode //
    //-----------//
    @Override
        protected boolean paintNode (Graphics  g,
                                     Zoom      zoom)
    {
        // Draw the time symbol
        Shape shape =  getShape();
        if (shape != null) {
            switch (shape) {
            case TIME_FOUR_FOUR :
            case TIME_TWO_TWO :
            case TIME_TWO_FOUR :
            case TIME_THREE_FOUR :
            case TIME_SIX_EIGHT :
            case COMMON_TIME :
            case CUT_TIME :
                staff.paintSymbol(g, zoom,
                                  (SymbolIcon) shape.getIcon(),
                                  getCenter());
                break;

            default:
                logger.warning("Weird time signature shape : " + shape);
            }
        } else {
            // Perhaps a multi-part signature
            for (Glyph glyph : glyphs) {
                Shape s = glyph.getShape();
                if (s != null) {
                    StaffPoint center = staff.computeGlyphCenter(glyph, scale);
                    int pitch = staff.unitToPitch(center.y);
                    staff.paintSymbol(g, zoom,
                            (SymbolIcon) s.getIcon(),
                            center, pitch);
                }
            }
        }

        return true;
    }

    //~ Methods private ---------------------------------------------------

    //-----------------//
    // computeRational //
    //-----------------//
    private void computeRational()
    {
        if (glyphs.size() == 0) {       // No component
            return;
        } else if (glyphs.size() == 1) { // Just one component
            Shape shape = glyphs.first().getShape();
            if (shape != null) {
                switch (shape) {

                case TIME_FOUR_FOUR :
                    this.shape = shape;
                    numerator   = 4;
                    denominator = 4;
                    return;

                case TIME_TWO_TWO :
                    this.shape = shape;
                    numerator   = 2;
                    denominator = 2;
                    return;

                case TIME_TWO_FOUR :
                    this.shape = shape;
                    numerator   = 2;
                    denominator = 4;
                    return;

                case TIME_THREE_FOUR :
                    this.shape = shape;
                    numerator   = 3;
                    denominator = 4;
                    return;

                case TIME_SIX_EIGHT :
                    this.shape = shape;
                    numerator   = 6;
                    denominator = 8;
                    return;

                case COMMON_TIME :
                    this.shape = shape;
                    numerator   = 4;
                    denominator = 4;
                    return;

                case CUT_TIME :
                    this.shape = shape;
                    numerator   = 2;
                    denominator = 4;
                    return;

                default :
                    logger.warning("Weird single time component : " + shape +
                            " for glyph#" + glyphs.first().getId());
                }
            }
        } else {                        // Several components
            // Dispatch glyphs on top and bottom parts
            numerator = denominator = null;
            for (Glyph glyph : glyphs) {
                int pitch = staff.unitToPitch
                    (staff.computeGlyphCenter(glyph, scale).y);
                Integer value = getNumericValue(glyph);
                if (logger.isFineEnabled()) {
                    logger.fine("pitch=" + pitch + " value=" + value +
                                " glyph=" + glyph);
                }
                if (value != null) {
                    if (pitch < 0) {
                        if (numerator == null) {
                            numerator = 0;
                        }
                        numerator = 10 * numerator + value;
                    } else if (pitch > 0) {
                        if (denominator == null) {
                            denominator = 0;
                        }
                        denominator = 10 * denominator + value;
                    } else {
                        logger.warning("Multi-part time signature" +
                                       " with a component of pitch position 0");
                    }
                } else {
                    logger.warning("Time signature component" +
                                   " with no numeric value");
                }
            }
        }
        if (logger.isFineEnabled()) {
            logger.fine("numerator="  + numerator +
                    " denominator=" + denominator);
        }
    }

    //-----------------//
    // getNumericValue //
    //-----------------//
    private Integer getNumericValue (Glyph glyph)
    {
        Shape shape = glyph.getShape();
        if (shape != null) {
            switch (shape) {
            case TIME_ZERO      : return  0;
            case TIME_ONE       : return  1;
            case TIME_TWO       : return  2;
            case TIME_THREE     : return  3;
            case TIME_FOUR      : return  4;
            case TIME_FIVE      : return  5;
            case TIME_SIX       : return  6;
            case TIME_SEVEN     : return  7;
            case TIME_EIGHT     : return  8;
            case TIME_NINE      : return  9;
            case TIME_TWELVE    : return 12;
            case TIME_SIXTEEN   : return 16;

            default             : return null;
            }
        }

        return null;
    }
}
