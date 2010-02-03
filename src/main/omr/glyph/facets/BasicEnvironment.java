//----------------------------------------------------------------------------//
//                                                                            //
//                      B a s i c E n v i r o n m e n t                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.facets;

import omr.glyph.GlyphSection;

import omr.util.Predicate;

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

    //-------------//
    // setLeftStem //
    //-------------//
    public void setLeftStem (Glyph leftStem)
    {
        this.leftStem = leftStem;
    }

    //-------------//
    // getLeftStem //
    //-------------//
    public Glyph getLeftStem ()
    {
        return leftStem;
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

    //--------------//
    // setRightStem //
    //--------------//
    public void setRightStem (Glyph rightStem)
    {
        this.rightStem = rightStem;
    }

    //--------------//
    // getRightStem //
    //--------------//
    public Glyph getRightStem ()
    {
        return rightStem;
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
        for (GlyphSection section : glyph.getMembers()) {
            for (GlyphSection sct : section.getTargets()) {
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
        for (GlyphSection section : glyph.getMembers()) {
            for (GlyphSection sct : section.getSources()) {
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
        setLeftStem(other.getLeftStem());
        setRightStem(other.getRightStem());
        setStemNumber(other.getStemNumber());
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        System.out.println("   stemNumber=" + getStemNumber());
        System.out.println("   leftStem=" + getLeftStem());
        System.out.println("   rightStem=" + getRightStem());
        System.out.println("   pitchPosition=" + getPitchPosition());
        System.out.println("   withLedger=" + isWithLedger());
    }
}
