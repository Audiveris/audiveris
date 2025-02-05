//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S c a l e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.sheet;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.Range;
import org.audiveris.omr.sheet.note.HeadSeedScale;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.param.ConstantBasedParam;
import org.audiveris.omr.util.param.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>Scale</code> encapsulates what drives the scale of a sheet, starting by the
 * distance between two staff lines (center to center).
 * <ol>
 * <li>Primary informations: This data is always detected, otherwise the current page is detected as
 * not being a music page and thus flagged as <i>invalid</i>.
 * <ul>
 * <li><b>Staff interline</b>:
 * Typical vertical distance measured from line center to line center.
 * <li><b>Staff line thickness</b>:
 * Typical vertical run length, the number of contiguous black pixels in staff line height.
 * </ul>
 * <li>Secondary informations: This data is always made available, either based on detected value or
 * derived from other information.
 * <ul>
 * <li><b>Beam thickness</b>:
 * A second peak in the histogram of vertical black runs signals the presence of beams.
 * Otherwise a guess value is computed as a ratio of main white length between staff lines.
 * <li><b>Stem thickness</b>: Computed during STEM_SEEDS step.
 * </ul>
 * <li>Optional informations: This data may exist or not, according to the sheet at hand.
 * <ul>
 * <li><b>Black head</b>: Typical width and height measured for black heads.
 * <li><b>Music font</b>: Precise point size determined for music font rendering of heads.
 * <li><b>Small interline</b>: If a second peak is detected in the histogram of staff interlines,
 * it signals the presence of staves with a different interline value.
 * <br>
 * In that case, interline is assigned the larger value and small-interline the smaller value.
 * <li><b>Second beam thickness</b>: If a third peak is detected in the histogram of vertical black
 * runs, it provides the thickness of a second population of beams.
 * <br>
 * In that case, beam is assigned the larger value and small-beam the smaller value.
 * <li><b>Head-Seed</b>: Typical horizontal distance between note head bounds and stem seed,
 * per head shape and head side.
 * This info is retrieved in HEADS step and used in STEMS step to precisely detect stem
 * candidates around each head even in poor-quality sheets.
 * </ul>
 * </ol>
 * <p>
 * This class also provides methods for converting values based on what the interline and the line
 * thickness are actually worth.
 * <br>
 * There are three different measurements: pixels, lines and interlines.
 * <table style="border: 1px solid black; border-collapse: collapse;">
 * <caption>Measurements table</caption>
 * <tr>
 * <th style="padding: 10px; text-align:center; border: 1px solid black;">Measurement</th>
 * <th style="padding: 10px; text-align:center; border: 1px solid black;">Value</th>
 * </tr>
 * <tr>
 * <td style="padding: 10px; border: 1px solid black;">Pixel</td>
 * <td style="padding: 10px; border: 1px solid black;">This is simply an absolute number (or
 * fraction) of pixels.</td>
 * </tr>
 * <tr>
 * <td style="padding: 10px; border: 1px solid black;">(interline) Fraction</td>
 * <td style="padding: 10px; border: 1px solid black;">This is a number (or fraction) of interlines.
 * <br>
 * Typical unit value for interline is around 20 pixels.</td>
 * </tr>
 * <tr>
 * <td style="padding: 10px; border: 1px solid black;">(interline) AreaFraction</td>
 * <td style="padding: 10px; border: 1px solid black;">This is a number (or fraction) of square
 * interlines,
 * meant to measure glyph area or weight.
 * <br>
 * Typical unit value for interline area is around 400 square pixels.</td>
 * </tr>
 * <tr>
 * <td style="padding: 10px; border: 1px solid black;">LineFraction</td>
 * <td style="padding: 10px; border: 1px solid black;">This is a number (or fraction) of line
 * thickness.
 * <br>
 * Typical unit value for line is around 4 pixels.</td>
 * </tr>
 * </table>
 * <h2>Example of marshalled Scale element</h2>
 *
 * <pre>
 * &lt;scale&gt;
 *      &lt;line min="3" main="4" max="6"/&gt;
 *      &lt;interline min="17" main="19" max="20"/&gt;
 *      &lt;small-interline min="13" main="14" max="15"/&gt;
 *      &lt;beam main-thickness="17"/&gt;
 *      &lt;small-beam main-thickness="12"/&gt;
 *      &lt;stem main-thickness="3" max-thickness="4"/&gt;
 *      &lt;black-head mean-width="23.8" sigma-width="0.4" mean-height="25" sigma-height="1.3"/&gt;
 *      &lt;music-font name="MusicalSymbols" point-size="80"/&gt;
 *      &lt;head-seeds&gt;
 *          &lt;head-seed shape="NOTEHEAD_BLACK" side="LEFT" dx="0.3"/&gt;
 *          &lt;head-seed shape="NOTEHEAD_BLACK" side="RIGHT" dx="0.5"/&gt;
 *          &lt;head-seed shape="NOTEHEAD_VOID" side="LEFT" dx="0"/&gt;
 *      &lt;/head-seeds&gt;
 * &lt;/scale&gt;
 * </pre>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "scale")
public class Scale
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(Scale.class);

    public static final Param<Integer> defaultInterlineSpecification = new ConstantBasedParam<>(
            constants.defaultInterlineSpecification,
            Param.GLOBAL_SCOPE);

    public static final Param<Integer> defaultBeamSpecification = new ConstantBasedParam<>(
            constants.defaultBeamSpecification,
            Param.GLOBAL_SCOPE);

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * Typical thickness of staff lines.
     */
    @XmlElement(name = "line")
    private LineScale lineScale;

    /**
     * Typical vertical distance between a staff line center and the next line center
     * within the same staff.
     */
    @XmlElement(name = "interline")
    private InterlineScale interlineScale;

    /** Deprecated, replaced by <code>smallInterlineScale</code>. */
    @XmlElement(name = "small-staff")
    private Scale oldSmallScale;

    /** Typical vertical distance for small staves, if any. */
    @XmlElement(name = "small-interline")
    private InterlineScale smallInterlineScale;

    /** Typical thickness of beams and beam hooks. */
    @XmlElement(name = "beam")
    private BeamScale beamScale;

    /** Small typical thickness for beams and beam hooks, if any. */
    @XmlElement(name = "small-beam")
    private BeamScale smallBeamScale;

    /** Typical thickness of stems. */
    @XmlElement(name = "stem")
    private StemScale stemScale;

    /** Typical dimension of isolated black heads. */
    @XmlElement(name = "black-head")
    private BlackHeadScale blackHeadScale;

    /** Music font to fit typical black heads. */
    @XmlElement(name = "music-font")
    private MusicFontScale musicFontScale;

    /** Typical horizontal distances between note heads and stem seeds. */
    @XmlElement(name = "head-seeds")
    private HeadSeedScale headSeedScale;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create an empty Scale object.
     */
    public Scale ()
    {
    }

    /**
     * Create a Scale object, meant for a whole sheet.
     *
     * @param interlineScale      scale for (large) interline
     * @param lineScale           scale for line thickness
     * @param beamScale           scale for beams
     * @param smallInterlineScale scale for small interlines, perhaps null
     * @param smallBeamScale      scale for small beams, perhaps null
     */
    public Scale (InterlineScale interlineScale,
                  LineScale lineScale,
                  BeamScale beamScale,
                  InterlineScale smallInterlineScale,
                  BeamScale smallBeamScale)
    {
        this.interlineScale = interlineScale;
        this.lineScale = lineScale;
        this.beamScale = beamScale;
        this.smallInterlineScale = smallInterlineScale;
        this.smallBeamScale = smallBeamScale;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called immediately after unmarshalling of this object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller m,
                                 Object parent)
    {
        // Replace deprecated oldSmallScale by smallInterlineScale
        if (oldSmallScale != null) {
            smallInterlineScale = oldSmallScale.interlineScale;
            oldSmallScale = null;
        }
    }

    //--------------//
    // fracToPixels //
    //--------------//
    /**
     * Convert a fraction of interline to a number of pixels.
     *
     * @param val the fraction value
     * @return the (double) number of pixels
     */
    private double fracToPixels (double val)
    {
        return interlineScale.main * val;
    }

    //--------------//
    // getBeamScale //
    //--------------//
    /**
     * Report the beam scale.
     *
     * @return the beam scale
     */
    public BeamScale getBeamScale ()
    {
        return beamScale;
    }

    //------------------//
    // getBeamThickness //
    //------------------//
    /**
     * Report the main thickness for beams.
     *
     * @return main beam thickness, perhaps null
     */
    public Integer getBeamThickness ()
    {
        if (beamScale == null) {
            return null;
        }

        return beamScale.getMain();
    }

    //-------------------//
    // getBlackHeadScale //
    //-------------------//
    /**
     * Report the black head scale
     *
     * @return black head scale
     */
    public BlackHeadScale getBlackHeadScale ()
    {
        return blackHeadScale;
    }

    //---------//
    // getFore //
    //---------//
    /**
     * Report the line thickness this scale is based upon.
     *
     * @return the number of black pixels in a staff line, perhaps null
     */
    public Integer getFore ()
    {
        if (lineScale == null) {
            return null;
        }

        return lineScale.main;
    }

    //------------------//
    // getHeadSeedScale //
    //------------------//
    public HeadSeedScale getHeadSeedScale ()
    {
        return headSeedScale;
    }

    //--------------//
    // getInterline //
    //--------------//
    /**
     * Report the main interline value this scale is based upon.
     *
     * @return the number of pixels (black + white) from one line to the other, perhaps null
     */
    public Integer getInterline ()
    {
        if (interlineScale == null) {
            return null;
        }

        return interlineScale.main;
    }

    //-------------------//
    // getInterlineScale //
    //-------------------//
    /**
     * Report the (large) interline scale.
     *
     * @return the interlineScale
     */
    public InterlineScale getInterlineScale ()
    {
        return interlineScale;
    }

    //-------------------//
    // getInterlineScale //
    //-------------------//
    /**
     * Report the scale that applies for the provided (staff) interline value.
     *
     * @param interline the provided (staff specific) interline
     * @return the desired interlineScale
     */
    public InterlineScale getInterlineScale (int interline)
    {
        if (interlineScale.main == interline) {
            return interlineScale;
        }

        if ((smallInterlineScale != null) && (smallInterlineScale.main == interline)) {
            return smallInterlineScale;
        }

        throw new IllegalArgumentException("No interline scale for provided value " + interline);
    }

    //--------------//
    // getItemValue //
    //--------------//
    /**
     * Report the value of a specific item.
     *
     * @param item desired item
     * @return item value, perhaps null
     */
    public Integer getItemValue (Item item)
    {
        return switch (item) {
            case line -> getFore();
            case interline -> getInterline();
            case smallInterline -> getSmallInterline();
            case beam -> getBeamThickness();
            case smallBeam -> (smallBeamScale != null) ? smallBeamScale.getMain() : null;
            case stem -> getStemThickness();
        };
    }

    //------------------------//
    // getLargeInterlineScale //
    //------------------------//
    /**
     * Same as {@link #getInterlineScale()}, method defined for completeness.
     *
     * @return the (large) interlineScale
     */
    public InterlineScale getLargeInterlineScale ()
    {
        return interlineScale;
    }

    //--------------//
    // getLineScale //
    //--------------//
    /**
     * Report the line scale.
     *
     * @return the lineScale
     */
    public LineScale getLineScale ()
    {
        return lineScale;
    }

    //------------//
    // getMaxFore //
    //------------//
    /**
     * Report the maximum line thickness (using standard percentile).
     *
     * @return the max fore value
     */
    public int getMaxFore ()
    {
        return lineScale.max;
    }

    //-----------------//
    // getMaxInterline //
    //-----------------//
    /**
     * Report the maximum interline (using standard percentile).
     *
     * @return the maxInterline
     */
    public int getMaxInterline ()
    {
        return interlineScale.max;
    }

    //----------------------//
    // getMaxSmallInterline //
    //----------------------//
    /**
     * Report the maximum value of small interline if any (using standard percentile).
     *
     * @return the max smallInterline if any, otherwise null
     */
    public Integer getMaxSmallInterline ()
    {
        if (smallInterlineScale != null) {
            return smallInterlineScale.max;
        } else {
            return null;
        }
    }

    //------------//
    // getMaxStem //
    //------------//
    /**
     * Report the reasonable maximum stem width within the sheet.
     *
     * @return the reasonable maximum stem thickness
     */
    public int getMaxStem ()
    {
        return stemScale.getMax();
    }

    //------------//
    // getMinFore //
    //------------//
    /**
     * Report the minimum line thickness (using standard percentile).
     *
     * @return the min fore value
     */
    public int getMinFore ()
    {
        return lineScale.min;
    }

    //-------------------//
    // getMusicFontScale //
    //-------------------//
    /**
     * Report the music font scale information.
     *
     * @return the music font scale information
     */
    public MusicFontScale getMusicFontScale ()
    {
        return musicFontScale;
    }

    //-------------------//
    // getSmallBeamScale //
    //-------------------//
    /**
     * Report the small beam scale, if any
     *
     * @return the smallBeamScale, perhaps null
     */
    public BeamScale getSmallBeamScale ()
    {
        return smallBeamScale;
    }

    //-------------------//
    // getSmallInterline //
    //-------------------//
    /**
     * Report the small interline value, if any.
     *
     * @return the small interline, perhaps null.
     */
    public Integer getSmallInterline ()
    {
        if (smallInterlineScale != null) {
            return smallInterlineScale.main;
        } else {
            return null;
        }
    }

    //------------------------//
    // getSmallInterlineScale //
    //------------------------//
    /**
     * Report the small interline scale if any.
     *
     * @return the smallInterlineScale, perhaps null
     */
    public InterlineScale getSmallInterlineScale ()
    {
        return smallInterlineScale;
    }

    //--------------//
    // getStemScale //
    //--------------//
    /**
     * Report the stem scale.
     *
     * @return the stem scale
     */
    public StemScale getStemScale ()
    {
        return stemScale;
    }

    //------------------//
    // getStemThickness //
    //------------------//
    /**
     * Report the most frequent stem thickness (width).
     *
     * @return the most frequent stem thickness
     */
    public Integer getStemThickness ()
    {
        if (stemScale == null) {
            return null;
        }

        return stemScale.getMain();
    }

    //--------------------//
    // isBeamExtrapolated //
    //--------------------//
    /**
     * Tell whether beam scaling info is extrapolated (rather than measured).
     *
     * @return true for extrapolated, false for measured
     */
    public boolean isBeamExtrapolated ()
    {
        Objects.requireNonNull(beamScale, "This scale instance has no beam information");

        return beamScale.isExtrapolated();
    }

    //------------------//
    // pixelsToAreaFrac //
    //------------------//
    /**
     * Compute the interline area fraction that corresponds to the given number of pixels.
     *
     * @param pixels the equivalent in number of pixels
     * @return the interline area fraction
     * @see #toPixels
     */
    public double pixelsToAreaFrac (double pixels)
    {
        return pixels / (interlineScale.main * interlineScale.main);
    }

    //--------------//
    // pixelsToFrac //
    //--------------//
    /**
     * Compute the interline fraction that corresponds to the given number of pixels.
     *
     * @param pixels the equivalent in number of pixels
     * @return the interline fraction
     * @see #toPixels
     */
    public double pixelsToFrac (double pixels)
    {
        return pixels / interlineScale.main;
    }

    //------------------//
    // pixelsToLineFrac //
    //------------------//
    /**
     * Compute the line fraction that corresponds to the given number of pixels.
     *
     * @param pixels the equivalent in number of pixels
     * @return the line fraction
     * @see #toPixels
     */
    public double pixelsToLineFrac (double pixels)
    {
        return pixels / lineScale.main;
    }

    //-------------------//
    // setBlackHeadScale //
    //-------------------//
    /**
     * Remember black head scale.
     *
     * @param blackHeadScale the blackHeadScale to set
     */
    public void setBlackHeadScale (BlackHeadScale blackHeadScale)
    {
        this.blackHeadScale = blackHeadScale;
    }

    //------------------//
    // setHeadSeedScale //
    //------------------//
    public void setHeadSeedScale (HeadSeedScale headSeedScale)
    {
        this.headSeedScale = headSeedScale;
    }

    //--------------//
    // setItemValue //
    //--------------//
    /**
     * Assign a value to a specific item.
     *
     * @param item desired item
     * @param v    new value. If the new value is 0 or less, the scale object is set to null
     * @return the modified scale object
     */
    public Object setItemValue (Item item,
                                int v)
    {
        return switch (item) {
            case line -> lineScale = (v <= 0) ? null : new LineScale(v, v, v);
            case interline -> interlineScale = (v <= 0) ? null : new InterlineScale(v, v, v);
            case smallInterline -> smallInterlineScale = (v <= 0) ? null
                    : new InterlineScale(v, v, v);
            case beam -> beamScale = (v <= 0) ? null : new BeamScale(v, false);
            case smallBeam -> smallBeamScale = (v <= 0) ? null : new BeamScale(v, false);
            case stem -> stemScale = (v <= 0) ? null : new StemScale(v, v);
        };
    }

    //-------------------//
    // setMusicFontScale //
    //-------------------//
    /**
     * Remember music font scale.
     *
     * @param musicFontScale the musicFontScale to set
     */
    public void setMusicFontScale (MusicFontScale musicFontScale)
    {
        this.musicFontScale = musicFontScale;
    }

    //--------------//
    // setStemScale //
    //--------------//
    /**
     * Remember stem scaling information.
     *
     * @param stemScale stem scaling
     */
    public void setStemScale (StemScale stemScale)
    {
        this.stemScale = stemScale;
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Compute the squared-normalized number of pixels, according to the scale.
     *
     * @param areaFrac a measure based on interline (1 = one interline square)
     * @return the actual squared number of pixels with the current scale
     */
    public int toPixels (AreaFraction areaFrac)
    {
        return InterlineScale.toPixels(interlineScale.main, areaFrac);
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Compute the number of pixels that corresponds to the fraction of interline
     * provided, according to the scale.
     *
     * @param frac a measure based on interline (1 = one interline)
     * @return the actual number of pixels with the current scale
     */
    public int toPixels (Fraction frac)
    {
        return (int) Math.rint(toPixelsDouble(frac));
    }

    //----------//
    // toPixels //
    //----------//
    /**
     * Compute the number of pixels that corresponds to the fraction of line thickness
     * provided, according to the scale.
     *
     * @param lineFrac a measure based on line thickness (1 = one line height)
     * @return the actual number of pixels with the current scale
     */
    public int toPixels (LineFraction lineFrac)
    {
        return (int) Math.rint(toPixelsDouble(lineFrac));
    }

    //----------------//
    // toPixelsDouble //
    //----------------//
    /**
     * Convenient method, working directly on a constant of interline fraction.
     * Same as toPixels, but the result is a double instead of a rounded int.
     *
     * @param frac the interline fraction constant
     * @return the equivalent in number of pixels
     * @see #toPixels
     */
    public double toPixelsDouble (Fraction frac)
    {
        return fracToPixels(frac.getValue());
    }

    //----------------//
    // toPixelsDouble //
    //----------------//
    /**
     * Convenient method, working directly on a constant of line
     * fraction.
     * Same as toPixels, but the result is a double instead of a rounded int.
     *
     * @param lineFrac the line fraction constant
     * @return the equivalent in number of pixels
     * @see #toPixels
     */
    public double toPixelsDouble (LineFraction lineFrac)
    {
        return lineScale.main * lineFrac.getValue();
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return toString(Info.ALL);
    }

    //----------//
    // toString //
    //----------//
    /**
     * An extensible description of scale.
     *
     * @param info which info to display
     * @return scale description
     */
    public String toString (Info info)
    {
        final StringBuilder sb = new StringBuilder();

        if (info == Info.ALL) {
            sb.append("Scale{");
        }

        if (info == Info.COMBO || info == Info.ALL) {
            if (smallInterlineScale != null) {
                sb.append(" small_").append(smallInterlineScale);
            }

            if (interlineScale != null) {
                sb.append(" ").append(interlineScale);
            }
        }

        if (info == Info.BLACK || info == Info.ALL) {
            if (lineScale != null) {
                sb.append(" ").append(lineScale);
            }

            if (smallBeamScale != null) {
                sb.append(" small_").append(smallBeamScale);
            }

            if (beamScale != null) {
                sb.append(" ").append(beamScale);
            }
        }

        if (info == Info.ALL) {
            if (stemScale != null) {
                sb.append(" ").append(stemScale);
            }

            if (blackHeadScale != null) {
                sb.append(" ").append(blackHeadScale);
            }

            if (musicFontScale != null) {
                sb.append(" ").append(musicFontScale);
            }

            if (headSeedScale != null) {
                sb.append(" ").append(headSeedScale);
            }
        }

        if (info == Info.ALL) {
            sb.append("}");
        }

        return sb.toString();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //--------------//
    // AreaFraction //
    //--------------//
    /**
     * A subclass of Constant.Double, meant to store a fraction of interline-based area.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class AreaFraction
            extends Constant.Double
    {
        /**
         * Specific constructor, where 'unit' and 'name' are assigned later.
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public AreaFraction (double defaultValue,
                             java.lang.String description)
        {
            super("Interline**2", defaultValue, description);
        }
    }

    //-----------//
    // BeamScale //
    //-----------//
    /**
     * Class <code>BeamScale</code> keeps scaling information about beams in a sheet.
     * <p>
     * Generally, there are enough beam-based vertical runs in a sheet to produce a visible peak in
     * the sheet histogram of vertical run lengths.
     * In some cases, however, there are just a few beam-based vertical runs, and so the sheet
     * histogram reveals no significant beam peak.
     * The main value for beam thickness is then extrapolated as a pre-defined fraction of sheet
     * interline, and flagged as such in this BeamScale instance.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class BeamScale
    {
        /** This is the most frequent beam thickness value. */
        @XmlAttribute(name = "main-thickness")
        private final int main;

        /**
         * This indicates that, for lack of enough measurements, the typical beam
         * thickness had to be extrapolated.
         * <p>
         * <b>BEWARE</b>: An extrapolated value is much less reliable than a measured one.
         */
        @XmlAttribute(name = "extra")
        private final Boolean extra;

        /**
         * No-argument constructor needed for JAXB.
         */
        private BeamScale ()
        {
            this.main = 0;
            this.extra = null;
        }

        /**
         * Creates a new <code>BeamScale</code> object.
         *
         * @param main         most frequent beam thickness
         * @param extrapolated true if not enough measurements are available
         */
        public BeamScale (int main,
                          boolean extrapolated)
        {
            this.main = main;
            this.extra = extrapolated ? true : null;
        }

        /**
         * Report the most frequent beam distance
         *
         * @return the most frequent beam distance
         */
        public int getMain ()
        {
            return main;
        }

        /**
         * Tell whether beam thickness is extrapolated (rather than measured)
         *
         * @return true for extrapolated, false for measured
         */
        public boolean isExtrapolated ()
        {
            return extra != null;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("beam(");
            sb.append(main);

            if (extra != null) {
                sb.append(" extra");
            }

            sb.append(')');

            return sb.toString();
        }
    }

    //----------------//
    // BlackHeadScale //
    //----------------//
    /**
     * Class <code>BlackHeadScale</code> keeps scaling information about dimension of
     * single black heads in a sheet.
     * <p>
     * This data is used to derive MusicFontScale.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class BlackHeadScale
    {
        /** Mean value for black head width. */
        @XmlAttribute(name = "mean-width")
        @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double1Adapter.class)
        final double widthMean;

        /** Standard deviation value for black head width. */
        @XmlAttribute(name = "sigma-width")
        @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double1Adapter.class)
        final double widthStd;

        /** Mean value for black head height. */
        @XmlAttribute(name = "mean-height")
        @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double1Adapter.class)
        final double heightMean;

        /** Standard deviation value for black head height. */
        @XmlAttribute(name = "sigma-height")
        @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double1Adapter.class)
        final double heightStd;

        /**
         * No-argument constructor needed for JAXB.
         */
        private BlackHeadScale ()
        {
            this.widthMean = 0;
            this.widthStd = 0;
            this.heightMean = 0;
            this.heightStd = 0;
        }

        /**
         * Creates a new <code>BlackHeadScale</code> object.
         *
         * @param widthMean  width mean value
         * @param widthStd   width standard deviation
         * @param heightMean height mean value
         * @param heightStd  height standard deviation
         */
        public BlackHeadScale (double widthMean,
                               double widthStd,
                               double heightMean,
                               double heightStd)
        {
            this.widthMean = widthMean;
            this.widthStd = widthStd;
            this.heightMean = heightMean;
            this.heightStd = heightStd;
        }

        /**
         * @return the heightMean
         */
        public double getHeightMean ()
        {
            return heightMean;
        }

        /**
         * @return the heightStd
         */
        public double getHeightStd ()
        {
            return heightStd;
        }

        /**
         * @return the widthMean
         */
        public double getWidthMean ()
        {
            return widthMean;
        }

        /**
         * @return the widthStd
         */
        public double getWidthStd ()
        {
            return widthStd;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("blackHead(");
            String frm = "%s:%.1f~%.1f";
            sb.append(String.format(frm, "width", widthMean, widthStd));
            sb.append(" ");
            sb.append(String.format(frm, "height", heightMean, heightStd));
            sb.append(')');

            return sb.toString();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer defaultInterlineSpecification = new Constant.Integer(
                "pixels",
                0,
                "Default specification of interline (0 means no specification)");

        private final Constant.Integer defaultBeamSpecification = new Constant.Integer(
                "pixels",
                0,
                "Default specification of beam thickness (0 means no specification)");
    }

    //----------//
    // Fraction //
    //----------//
    /**
     * A subclass of Constant.Double, meant to store a fraction of interline, since many
     * distances on a music sheet are expressed as fraction of staff interline (as
     * opposed to {@link Scale.LineFraction} which stores a fraction of line thickness).
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class Fraction
            extends Constant.Double
    {
        /**
         * Fraction with value 0.
         */
        public static final Fraction ZERO = new Fraction(0, "zero");

        static {
            ZERO.setUnitAndName(Scale.class.getName(), "ZERO");
        }

        // Meant for JAXB
        private Fraction ()
        {
            this(0d, null);
        }

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later.
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public Fraction (double defaultValue,
                         java.lang.String description)
        {
            super("Interline", defaultValue, description);
        }
    }

    /**
     * Parts of scale to display.
     */
    public enum Info
    {
        BLACK,
        COMBO,
        ALL;
    }

    //----------------//
    // InterlineScale //
    //----------------//
    /**
     * Range of values for staff line vertical distance (center to center).
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class InterlineScale
            extends Range
    {
        /** Meant for JAXB. */
        protected InterlineScale ()
        {
            this(0, 0, 0);
        }

        /**
         * Create an InterlineScale object.
         *
         * @param min  minimum value
         * @param main most frequent value
         * @param max  maximum value
         */
        public InterlineScale (int min,
                               int main,
                               int max)
        {
            super(min, main, max);
        }

        /**
         * Create an InterlineScale object.
         *
         * @param range the underlying range
         */
        public InterlineScale (Range range)
        {
            this(range.min, range.main, range.max);
        }

        /**
         * Compute the interline fraction that corresponds to the given number of pixels.
         *
         * @param pixels the equivalent in number of pixels
         * @return the interline fraction
         * @see #toPixels
         */
        public double pixelsToFrac (double pixels)
        {
            return pixels / main;
        }

        /**
         * Compute the squared-normalized number of pixels.
         *
         * @param areaFrac a measure based on interline (1 = one interline square)
         * @return the actual squared number of pixels with the current scale
         */
        public int toPixels (AreaFraction areaFrac)
        {
            return toPixels(main, areaFrac);
        }

        /**
         * Compute the number of pixels that corresponds to the fraction of interline
         * provided, according to the scale.
         *
         * @param frac a measure based on interline (1 = one interline)
         * @return the actual number of pixels with the current scale
         */
        public int toPixels (Fraction frac)
        {
            return toPixels(main, frac);
        }

        /**
         * Convenient method, working directly on a constant of interline fraction.
         * Same as toPixels, but the result is a double instead of a rounded integer.
         *
         * @param frac the interline fraction constant
         * @return the equivalent in number of pixels
         * @see #toPixels
         */
        public double toPixelsDouble (Fraction frac)
        {
            return toPixelsDouble(main, frac);
        }

        @Override
        public String toString ()
        {
            return "interline" + super.toString();
        }

        /**
         * Compute the squared-normalized number of pixels, according to the provided
         * interline.
         *
         * @param interline provided interline value
         * @param areaFrac  a measure based on interline (1 = one interline square)
         * @return the actual squared number of pixels with the current scale
         */
        public static int toPixels (int interline,
                                    AreaFraction areaFrac)
        {
            return (int) Math.rint(interline * interline * areaFrac.getValue());
        }

        /**
         * Compute in pixels the provided Fraction, under the provided interline.
         *
         * @param interline the actual interline value
         * @param frac      the interline-based specification
         * @return the resulting integer value in pixels
         */
        public static int toPixels (int interline,
                                    Fraction frac)
        {
            return (int) Math.rint(toPixelsDouble(interline, frac));
        }

        /**
         * Compute in pixels the provided Fraction, under the provided interline.
         *
         * @param interline the actual interline value
         * @param frac      the interline-based specification
         * @return the resulting double value in pixels
         */
        public static double toPixelsDouble (int interline,
                                             Fraction frac)
        {
            return interline * frac.getValue();
        }
    }

    //------//
    // Item //
    //------//
    /**
     * Scale information kind.
     */
    public static enum Item
    {
        line("Line thickness"),
        interline("Interline"),
        smallInterline("Small interline"),
        beam("Beam thickness"),
        smallBeam("Small beam thickness"),
        stem("Stem thickness");

        private final String description;

        Item (String description)
        {
            this.description = description;
        }

        /**
         * Report item description
         *
         * @return description
         */
        public String getDescription ()
        {
            return description;
        }
    }

    //--------------//
    // LineFraction //
    //--------------//
    /**
     * A subclass of Constant.Double, meant to store a fraction of line thickness (as
     * opposed to {@link Scale.Fraction} which stores a fraction of interline).
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class LineFraction
            extends Constant.Double
    {
        // Meant for JAXB
        private LineFraction ()
        {
            this(0d, null);
        }

        /**
         * Specific constructor, where 'unit' and 'name' are assigned later.
         *
         * @param defaultValue the (double) default value
         * @param description  the semantic of the constant
         */
        public LineFraction (double defaultValue,
                             java.lang.String description)
        {
            super("Line", defaultValue, description);
        }
    }

    //-----------//
    // LineScale //
    //-----------//
    /**
     * Range of values for staff line thickness.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class LineScale
            extends Range
    {
        /** Meant for JAXB. */
        protected LineScale ()
        {
            this(0, 0, 0);
        }

        /**
         * Create a LineScale object.
         *
         * @param min  minimum value
         * @param main most frequent value
         * @param max  maximum value
         */
        public LineScale (int min,
                          int main,
                          int max)
        {
            super(min, main, max);
        }

        /**
         * Create a LineScale object.
         *
         * @param range the defining range
         */
        public LineScale (Range range)
        {
            this(range.min, range.main, range.max);
        }

        /**
         * Compute the number of pixels that corresponds to the fraction of line
         * thickness provided, according to the scale.
         *
         * @param lineFrac a measure based on line thickness (1 = one line height)
         * @return the actual number of pixels with the current scale
         */
        public int toPixels (LineFraction lineFrac)
        {
            return (int) Math.rint(toPixelsDouble(lineFrac));
        }

        /**
         * Convenient method, working directly on a constant of line fraction.
         * Same as toPixels, but the result is a double instead of a rounded integer.
         *
         * @param lineFrac the line fraction constant
         * @return the equivalent in number of pixels
         * @see #toPixels
         */
        public double toPixelsDouble (LineFraction lineFrac)
        {
            return main * lineFrac.getValue();
        }

        @Override
        public String toString ()
        {
            return "line" + super.toString();
        }
    }

    //----------------//
    // MusicFontScale //
    //----------------//
    /**
     * Class <code>MusicFontScale</code> keeps scaling information about music font in sheet.
     * <p>
     * It can optionally handle a small font size for small staves in sheet.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class MusicFontScale
    {
        /** This is the name of the music font. */
        @XmlAttribute(name = "name")
        final String name;

        /** This is the size of the music font, specified in typographic point value. */
        @XmlAttribute(name = "point-size")
        final int pointSize;

        /** Meant for JAXB. */
        private MusicFontScale ()
        {
            this.name = null;
            this.pointSize = 0;
        }

        /**
         * Creates a new <code>MusicFontScale</code> object.
         *
         * @param name      name of font, not null
         * @param pointSize point size for standard large staff, not null
         */
        public MusicFontScale (String name,
                               int pointSize)
        {
            this.name = name;
            this.pointSize = pointSize;
        }

        /**
         * @return the name
         */
        public String getName ()
        {
            return name;
        }

        /**
         * @return the pointSize
         */
        public int getPointSize ()
        {
            return pointSize;
        }

        @Override
        public String toString ()
        {
            return new StringBuilder("MusicFont{").append("name:").append(name).append(
                    " pointSize:").append(pointSize).append('}').toString();
        }
    }

    /**
     * Staff size kind.
     */
    public enum Size
    {
        /** Standard staff. */
        LARGE,
        /** Small staff. */
        SMALL;
    }

    //-----------//
    // StemScale //
    //-----------//
    /**
     * Class <code>StemScale</code> keeps scaling information about stems in a sheet.
     * <p>
     * It handles main and max values for stem thickness.
     * <p>
     * TODO: It could also handle stem length, which can be interesting for stem candidates (at
     * least their tail for those free of beam and flag).
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class StemScale
    {
        /** Most frequent stem thickness. */
        @XmlAttribute(name = "main-thickness")
        private final int main;

        /** Maximum stem thickness. */
        @XmlAttribute(name = "max-thickness")
        private final int max;

        /**
         * No-argument constructor needed for JAXB.
         */
        private StemScale ()
        {
            this.main = 0;
            this.max = 0;
        }

        /**
         * Creates a new <code>StemScale</code> object.
         *
         * @param main most frequent thickness
         * @param max  max thickness
         */
        public StemScale (int main,
                          int max)
        {
            this.main = main;
            this.max = max;
        }

        /**
         * Report the most frequent stem thickness
         *
         * @return the main value
         */
        public int getMain ()
        {
            return main;
        }

        /**
         * Report the reasonable maximum stem thickness.
         *
         * @return the max
         */
        public int getMax ()
        {
            return max;
        }

        @Override
        public String toString ()
        {
            StringBuilder sb = new StringBuilder("stem");
            sb.append('(');
            sb.append(main);
            sb.append(" max:").append(max);
            sb.append(')');

            return sb.toString();
        }
    }
}
