//----------------------------------------------------------------------------//
//                                                                            //
//                          L i n e F i l a m e n t                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.grid;

import omr.log.Logger;

import omr.sheet.Scale;

import omr.stick.Filament;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Class {@code LineFilament} is a {@link Filament} used as (part of) a
 * candidate staff line.
 */
public class LineFilament
    extends Filament
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(LineFilament.class);

    //~ Instance fields --------------------------------------------------------

    /** Patterns where this filament appears. map (column -> pattern) */
    private SortedMap<Integer, FilamentPattern> patterns;

    /** The line cluster this filament is part of, if any */
    private LineCluster cluster;

    /** Relative position in cluster (relevant only if cluster is not null) */
    private int clusterPos;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // LineFilament //
    //--------------//
    /**
     * Creates a new LineFilament object.
     *
     * @param scale scaling data
     */
    public LineFilament (Scale scale)
    {
        super(scale);
    }

    //~ Methods ----------------------------------------------------------------

    //-------------//
    // getAncestor //
    //-------------//
    @Override
    public LineFilament getAncestor ()
    {
        return (LineFilament) super.getAncestor();
    }

    //------------//
    // setCluster //
    //------------//
    /**
     * Assign this filament to a line cluster
     * @param cluster the containing cluster
     * @param pos the relative line position within the cluster
     */
    public void setCluster (LineCluster cluster,
                            int         pos)
    {
        this.cluster = cluster;
        clusterPos = pos;
    }

    //------------//
    // getCluster //
    //------------//
    /**
     * Report the line cluster, if any, this filament is part of
     * @return the containing cluster, or null
     */
    public LineCluster getCluster ()
    {
        return cluster;
    }

    //---------------//
    // getClusterPos //
    //---------------//
    /**
     * @return the clusterPos
     */
    public int getClusterPos ()
    {
        return clusterPos;
    }

    //-----------//
    // getParent //
    //-----------//
    @Override
    public LineFilament getParent ()
    {
        return (LineFilament) super.getParent();
    }

    //-------------//
    // getPatterns //
    //-------------//
    /**
     * @return the patterns
     */
    public SortedMap<Integer, FilamentPattern> getPatterns ()
    {
        if (patterns != null) {
            return patterns;
        } else {
            return new TreeMap<Integer, FilamentPattern>();
        }
    }

    //------------//
    // addPattern //
    //------------//
    /**
     * Add a pattern where this filament appears
     * @param column the sheet column index of the pattern
     * @param pattern the pattern which contains this filament
     */
    public void addPattern (int             column,
                            FilamentPattern pattern)
    {
        if (patterns == null) {
            patterns = new TreeMap<Integer, FilamentPattern>();
        }

        patterns.put(column, pattern);
    }

    //------//
    // dump //
    //------//
    @Override
    public void dump ()
    {
        super.dump();
        System.out.println("   cluster=" + cluster);
        System.out.println("   clusterPos=" + clusterPos);
        System.out.println("   patterns=" + patterns);
    }

    //---------//
    // include //
    //---------//
    /**
     * Include a whole other filament into this one
     * @param that the filament to swallow
     */
    public void include (LineFilament that)
    {
        super.include(that);

        that.cluster = this.cluster;
        that.clusterPos = this.clusterPos;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        if (cluster != null) {
            sb.append(" cluster:")
              .append(cluster.getId())
              .append("p")
              .append(clusterPos);
        }

        return sb.toString();
    }
}
