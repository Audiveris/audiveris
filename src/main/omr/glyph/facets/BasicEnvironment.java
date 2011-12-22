//----------------------------------------------------------------------------//
//                                                                            //
//                      B a s i c E n v i r o n m e n t                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.Shape;

import omr.lag.Lag;
import omr.lag.Section;

import omr.run.Run;

import omr.score.common.PixelRectangle;

import omr.util.HorizontalSide;
import omr.util.Predicate;

import java.awt.Rectangle;
import java.util.List;
import java.util.Set;

/**
 * Class {@code BasicEnvironment} is the basic implementation of an environment
 * facet
 *
 * @author Hervé Bitteur
 */
class BasicEnvironment
    extends BasicFacet
    implements GlyphEnvironment
{
    //~ Instance fields --------------------------------------------------------

    /** Position with respect to nearest staff. Key references are : 0 for
       middle line (B), -2 for top line (F) and +2 for bottom line (E)  */
    private double pitchPosition;

    /** Number of stems it is connected to (0, 1, 2) */
    private int stemNumber;

    /** Stem attached on left if any */
    private Glyph leftStem;

    /** Stem attached on right if any */
    private Glyph rightStem;

    /** Is there a ledger nearby ? */
    private boolean withLedger;

    //~ Constructors -----------------------------------------------------------

    //------------------//
    // BasicEnvironment //
    //------------------//
    /**
     * Create a new BasicEnvironment object
     *
     * @param glyph our glyph
     */
    public BasicEnvironment (Glyph glyph)
    {
        super(glyph);
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // getAlienPixelsFrom //
    //--------------------//
    public int getAlienPixelsFrom (Lag                lag,
                                   PixelRectangle     absRoi,
                                   Predicate<Section> predicate)
    {
        // Use lag orientation
        final Rectangle oRoi = lag.getOrientation()
                                  .oriented(absRoi);
        final int       posMin = oRoi.y;
        final int       posMax = (oRoi.y + oRoi.height) - 1;
        int             count = 0;

        for (Section section : lag.lookupSections(absRoi)) {
            // Exclude sections that are part of the glyph
            if (section.getGlyph() == glyph) {
                continue;
            }

            // Additional section predicate, if any
            if ((predicate != null) && !predicate.check(section)) {
                continue;
            }

            int pos = section.getFirstPos() - 1;

            for (Run run : section.getRuns()) {
                pos++;

                if (pos > posMax) {
                    break;
                }

                if (pos < posMin) {
                    continue;
                }

                int coordMin = Math.max(oRoi.x, run.getStart());
                int coordMax = Math.min(
                    (oRoi.x + oRoi.width) - 1,
                    run.getStop());

                if (coordMax >= coordMin) {
                    count += (coordMax - coordMin + 1);
                }
            }
        }

        return count;
    }

    //--------------//
    // getFirstStem //
    //--------------//
    public Glyph getFirstStem ()
    {
        for (HorizontalSide side : HorizontalSide.values()) {
            Glyph stem = getStem(side);

            if (stem != null) {
                return stem;
            }
        }

        return null;
    }

    //------------------//
    // setPitchPosition //
    //------------------//
    public void setPitchPosition (double pitchPosition)
    {
        this.pitchPosition = pitchPosition;
    }

    //------------------//
    // getPitchPosition //
    //------------------//
    public double getPitchPosition ()
    {
        return pitchPosition;
    }

    //---------//
    // setStem //
    //---------//
    public void setStem (Glyph          stem,
                         HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            leftStem = stem;
        } else {
            rightStem = stem;
        }
    }

    //---------//
    // getStem //
    //---------//
    public Glyph getStem (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return leftStem;
        } else {
            return rightStem;
        }
    }

    //---------------//
    // setStemNumber //
    //---------------//
    public void setStemNumber (int stemNumber)
    {
        this.stemNumber = stemNumber;
    }

    //---------------//
    // getStemNumber //
    //---------------//
    public int getStemNumber ()
    {
        return stemNumber;
    }

    //-----------------//
    // getSymbolsAfter //
    //-----------------//
    public void getSymbolsAfter (Predicate<Glyph> predicate,
                                 Set<Glyph>       goods,
                                 Set<Glyph>       bads)
    {
        for (Section section : glyph.getMembers()) {
            for (Section sct : section.getTargets()) {
                if (sct.isGlyphMember()) {
                    Glyph other = sct.getGlyph();

                    if (other != glyph) {
                        if (predicate.check(other)) {
                            goods.add(other);
                        } else {
                            bads.add(other);
                        }
                    }
                }
            }
        }
    }

    //------------------//
    // getSymbolsBefore //
    //------------------//
    public void getSymbolsBefore (Predicate<Glyph> predicate,
                                  Set<Glyph>       goods,
                                  Set<Glyph>       bads)
    {
        for (Section section : glyph.getMembers()) {
            for (Section sct : section.getSources()) {
                if (sct.isGlyphMember()) {
                    Glyph other = sct.getGlyph();

                    if (other != glyph) {
                        if (predicate.check(other)) {
                            goods.add(other);
                        } else {
                            bads.add(other);
                        }
                    }
                }
            }
        }
    }

    //---------------//
    // setWithLedger //
    //---------------//
    public void setWithLedger (boolean withLedger)
    {
        this.withLedger = withLedger;
    }

    //--------------//
    // isWithLedger //
    //--------------//
    public boolean isWithLedger ()
    {
        return withLedger;
    }

    //---------------------//
    // copyStemInformation //
    //---------------------//
    public void copyStemInformation (Glyph other)
    {
        for (HorizontalSide side : HorizontalSide.values()) {
            setStem(other.getStem(side), side);
        }

        setStemNumber(other.getStemNumber());
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        System.out.println("   stemNumber=" + getStemNumber());
        System.out.println("   leftStem=" + getStem(HorizontalSide.LEFT));
        System.out.println("   rightStem=" + getStem(HorizontalSide.RIGHT));
        System.out.println("   pitchPosition=" + getPitchPosition());
        System.out.println("   withLedger=" + isWithLedger());
    }
}
