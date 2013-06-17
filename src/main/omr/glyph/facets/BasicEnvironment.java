//----------------------------------------------------------------------------//
//                                                                            //
//                      B a s i c E n v i r o n m e n t                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.lag.Lag;
import omr.lag.Section;

import omr.run.Run;

import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import omr.util.Predicate;

import java.awt.Rectangle;
import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code BasicEnvironment} is the basic implementation of an
 * environment facet.
 *
 * @author Hervé Bitteur
 */
class BasicEnvironment
        extends BasicFacet
        implements GlyphEnvironment
{
    //~ Instance fields --------------------------------------------------------

    /** Position with respect to nearest staff. Key references are : 0 for
     * middle line (B), -2 for top line (F) and +2 for bottom line (E) */
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
    //---------------------//
    // copyStemInformation //
    //---------------------//
    @Override
    public void copyStemInformation (Glyph other)
    {
        for (HorizontalSide side : HorizontalSide.values()) {
            setStem(other.getStem(side), side);
        }

        setStemNumber(other.getStemNumber());
    }

    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        StringBuilder sb = new StringBuilder();

        if (stemNumber > 0) {
            sb.append(String.format("   stemNumber=%s%n", stemNumber));
        }

        if (leftStem != null) {
            sb.append(String.format("   leftStem=%s%n", leftStem));
        }

        if (rightStem != null) {
            sb.append(String.format("   rightStem=%s%n", rightStem));
        }

        sb.append(String.format("   pitchPosition=%s%n", getPitchPosition()));

        if (withLedger) {
            sb.append(String.format("   withLedger%n"));
        }

        return sb.toString();
    }

    //--------------------//
    // getAlienPixelsFrom //
    //--------------------//
    @Override
    public int getAlienPixelsFrom (Lag lag,
                                   Rectangle absRoi,
                                   Predicate<Section> predicate)
    {
        // Use lag orientation
        final Rectangle oRoi = lag.getOrientation()
                .oriented(absRoi);
        final int posMin = oRoi.y;
        final int posMax = (oRoi.y + oRoi.height) - 1;
        int count = 0;

        for (Section section : lag.lookupIntersectedSections(absRoi)) {
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

    //-----------------------//
    // getConnectedNeighbors //
    //-----------------------//
    @Override
    public Set<Glyph> getConnectedNeighbors ()
    {
        Set<Section> sections = glyph.getMembers();

        // Retrieve sections connected to this glyph
        Set<Section> connectedSections = new HashSet<>();

        for (Section section : sections) {
            for (Section s : section.getSources()) {
                if (!sections.contains(s)) {
                    connectedSections.add(s);
                }
            }

            for (Section s : section.getTargets()) {
                if (!sections.contains(s)) {
                    connectedSections.add(s);
                }
            }

            for (Section s : section.getOppositeSections()) {
                if (!sections.contains(s)) {
                    connectedSections.add(s);
                }
            }
        }

        // Retrieve their containing glyphs
        Set<Glyph> connectedGlyphs = new HashSet<>();

        for (Section s : connectedSections) {
            Glyph g = s.getGlyph();

            if (g != null) {
                connectedGlyphs.add(g);
            }
        }

        return connectedGlyphs;
    }

    //--------------//
    // getFirstStem //
    //--------------//
    @Override
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
    // getPitchPosition //
    //------------------//
    @Override
    public double getPitchPosition ()
    {
        return pitchPosition;
    }

    //---------//
    // getStem //
    //---------//
    @Override
    public Glyph getStem (HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            return leftStem;
        } else {
            return rightStem;
        }
    }

    //---------------//
    // getStemNumber //
    //---------------//
    @Override
    public int getStemNumber ()
    {
        return stemNumber;
    }

    //-----------------//
    // getSymbolsAfter //
    //-----------------//
    @Override
    public void getSymbolsAfter (Predicate<Glyph> predicate,
                                 Set<Glyph> goods,
                                 Set<Glyph> bads)
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
    @Override
    public void getSymbolsBefore (Predicate<Glyph> predicate,
                                  Set<Glyph> goods,
                                  Set<Glyph> bads)
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

    //-----------//
    // getSystem //
    //-----------//
    @Override
    public SystemInfo getSystem ()
    {
        return glyph.getFirstSection()
                .getSystem();
    }

    //--------------//
    // isWithLedger //
    //--------------//
    @Override
    public boolean isWithLedger ()
    {
        return withLedger;
    }

    //------------------//
    // setPitchPosition //
    //------------------//
    @Override
    public void setPitchPosition (double pitchPosition)
    {
        this.pitchPosition = pitchPosition;
    }

    //---------//
    // setStem //
    //---------//
    @Override
    public void setStem (Glyph stem,
                         HorizontalSide side)
    {
        if (side == HorizontalSide.LEFT) {
            leftStem = stem;
        } else {
            rightStem = stem;
        }
    }

    //---------------//
    // setStemNumber //
    //---------------//
    @Override
    public void setStemNumber (int stemNumber)
    {
        this.stemNumber = stemNumber;
    }

    //---------------//
    // setWithLedger //
    //---------------//
    @Override
    public void setWithLedger (boolean withLedger)
    {
        this.withLedger = withLedger;
    }
}
