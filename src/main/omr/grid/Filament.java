//----------------------------------------------------------------------------//
//                                                                            //
//                              F i l a m e n t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.Main;

import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.GlyphComposition.Linking;

import omr.lag.Section;

import omr.log.Logger;

import omr.run.Orientation;

import omr.sheet.Scale;

import java.util.Comparator;

/**
 * Class {@code Filament} represents a long glyph that can be far from being a
 * straight line.
 * It is used to handle candidate staff lines and bar lines.
 */
public class Filament
    extends BasicGlyph
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Filament.class);

    /**
     * For comparing Filament instances on their starting point
     */
    public static final Comparator<Filament> startComparator = new Comparator<Filament>() {
        public int compare (Filament f1,
                            Filament f2)
        {
            // Sort on start
            return Double.compare(
                f1.getStartPoint().getX(),
                f2.getStartPoint().getX());
        }
    };

    /**
     * For comparing Filament instances on their stopping point
     */
    public static final Comparator<Filament> stopComparator = new Comparator<Filament>() {
        public int compare (Filament f1,
                            Filament f2)
        {
            // Sort on stop
            return Double.compare(
                f1.getStopPoint().getX(),
                f2.getStopPoint().getX());
        }
    };

    /**
     * For comparing Filament instances on their top ordinate
     */
    public static final Comparator<Filament> topComparator = new Comparator<Filament>() {
        public int compare (Filament f1,
                            Filament f2)
        {
            // Sort on top ordinate
            return Integer.signum(f1.getContourBox().y - f2.getContourBox().y);
        }
    };

    /**
     * For comparing Filament instances on distance from reference axis
     */
    public static final Comparator<Filament> distanceComparator = new Comparator<Filament>() {
        public int compare (Filament f1,
                            Filament f2)
        {
            // Sort on distance from top edge
            return Integer.signum(f1.refDist - f2.refDist);
        }
    };


    //~ Instance fields --------------------------------------------------------

    /** Related scale */
    private final Scale scale;

    /** Distance from reference axis */
    private Integer refDist;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // Filament //
    //----------//
    /**
     * Creates a new Filament object.
     *
     * @param scale scaling data
     */
    public Filament (Scale scale)
    {
        this(scale, FilamentAlignment.class);
    }

    //----------//
    // Filament //
    //----------//
    /**
     * Creates a new Filament object.
     *
     * @param scale scaling data
     */
    public Filament (Scale                             scale,
                     Class<?extends FilamentAlignment> alignmentClass)
    {
        super(scale.interline(), alignmentClass);
        this.scale = scale;
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // getMeanCurvature //
    //------------------//
    public double getMeanCurvature ()
    {
        return getAlignment()
                   .getMeanCurvature();
    }

    //----------------//
    // setRefDistance //
    //----------------//
    /**
     * Remember the filament distance to reference axis
     * @param refDist the orthogonal distance to reference axis
     */
    public void setRefDistance (int refDist)
    {
        this.refDist = refDist;
    }

    //----------------//
    // getRefDistance //
    //----------------//
    /**
     * Report the orthogonal distance from the filament to the reference axis
     * @return distance from axis that takes global slope into acount
     */
    public Integer getRefDistance ()
    {
        return refDist;
    }

    //------------//
    // addSection //
    //------------//
    public void addSection (Section section)
    {
        addSection(section, Linking.LINK_BACK);
    }

    //------------//
    // addSection //
    //------------//
    @Override
    public void addSection (Section section,
                            Linking link)
    {
        getComposition()
            .addSection(section, link);
    }

    //----------//
    // deepDump //
    //----------//
    public void deepDump ()
    {
        Main.dumping.dump(this);
        Main.dumping.dump(getAlignment());
    }

    //---------//
    // include //
    //---------//
    /**
     * Include a whole other glyph into this one
     * @param that the filament or basic glyph to swallow
     */
    public void include (Glyph that)
    {
        for (Section section : that.getMembers()) {
            addSection(section);
        }

        that.setPartOf(this);
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        super.invalidateCache();

        refDist = null;
    }

    //-----------------//
    // polishCurvature //
    //-----------------//
    /**
     * Polish the filament by looking at local curvatures and removing sections
     * when necessary.
     */
    public void polishCurvature ()
    {
        getAlignment()
            .polishCurvature();
    }

    //---------------//
    // getPositionAt //
    //---------------//
    /**
     * Report the precise filament position for the provided coordinate .
     * @param coord the coord value (x for horizontal, y for vertical)
     * @param orientation the reference orientation
     * @return the pos value (y for horizontal, x for vertical)
     */
    public double positionAt (double      coord,
                              Orientation orientation)
    {
        return getAlignment()
                   .getPositionAt(coord, orientation);
    }

    //---------//
    // slopeAt //
    //---------//
    public double slopeAt (double      coord,
                           Orientation orientation)
    {
        return getAlignment()
                   .slopeAt(coord, orientation);
    }

    //------------//
    // trueLength //
    //------------//
    /**
     * Report an evaluation of how this filament is filled by sections
     * @return how solid this filament is
     */
    public int trueLength ()
    {
        return (int) Math.rint((double) getWeight() / scale.mainFore());
    }

    //--------------//
    // getAlignment //
    //--------------//
    @Override
    protected FilamentAlignment getAlignment ()
    {
        return (FilamentAlignment) super.getAlignment();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        //        sb.append(" lg:")
        //          .append(getLength());
        sb.append(" start[x=")
          .append((float) getStartPoint().getX())
          .append(",y=")
          .append((float) getStartPoint().getY())
          .append("]");

        sb.append(" stop[x=")
          .append((float) getStopPoint().getX())
          .append(",y=")
          .append((float) getStopPoint().getY())
          .append("]");

        //        sb.append(" meanDist:")
        //          .append((float) getMeanDistance());
        //
        if (getPartOf() != null) {
            sb.append(" anc:")
              .append(getAncestor());
        }

        //        if (refDist != null) {
        //            sb.append(" refDist:")
        //              .append(refDist);
        //        }
        if (getShape() != null) {
            sb.append(" ")
              .append(getShape());
        }

        return sb.toString();
    }
}
