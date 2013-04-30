//----------------------------------------------------------------------------//
//                                                                            //
//                              F i l a m e n t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.grid;

import omr.Main;

import omr.glyph.facets.BasicGlyph;
import omr.glyph.facets.GlyphComposition.Linking;

import omr.lag.Section;

import omr.run.Orientation;

import omr.sheet.Scale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;

/**
 * Class {@code Filament} represents a long glyph that can be far from
 * being a straight line.
 * It is used to handle candidate staff lines and bar lines.
 */
public class Filament
        extends BasicGlyph
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            Filament.class);

    /**
     * For comparing Filament instances on their top ordinate
     */
    public static final Comparator<Filament> topComparator = new Comparator<Filament>()
    {
        @Override
        public int compare (Filament f1,
                            Filament f2)
        {
            // Sort on top ordinate
            return Integer.signum(f1.getBounds().y - f2.getBounds().y);
        }
    };

    //~ Instance fields --------------------------------------------------------
    /** Related scale */
    private final Scale scale;

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
    public Filament (Scale scale,
                     Class<? extends FilamentAlignment> alignmentClass)
    {
        super(scale.getInterline(), alignmentClass);
        this.scale = scale;
    }

    //~ Methods ----------------------------------------------------------------
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

    //------------------//
    // getMeanCurvature //
    //------------------//
    public double getMeanCurvature ()
    {
        return getAlignment()
                .getMeanCurvature();
    }

    //----------//
    // getScale //
    //----------//
    /**
     * Report the scale that governs this filament.
     *
     * @return the related scale
     */
    public Scale getScale ()
    {
        return scale;
    }

    //-----------------//
    // polishCurvature //
    //-----------------//
    /**
     * Polish the filament by looking at local curvatures and removing
     * sections when necessary.
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
     *
     * @param coord       the coord value (x for horizontal, y for vertical)
     * @param orientation the reference orientation
     * @return the pos value (y for horizontal, x for vertical)
     */
    public double positionAt (double coord,
                              Orientation orientation)
    {
        return getAlignment()
                .getPositionAt(coord, orientation);
    }

    //---------//
    // slopeAt //
    //---------//
    public double slopeAt (double coord,
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
     *
     * @return how solid this filament is
     */
    public int trueLength ()
    {
        return (int) Math.rint((double) getWeight() / scale.getMainFore());
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

        if (getPartOf() != null) {
            sb.append(" anc:")
                    .append(getAncestor());
        }

        if (getShape() != null) {
            sb.append(" ")
                    .append(getShape());
        }

        return sb.toString();
    }
}
