//----------------------------------------------------------------------------//
//                                                                            //
//                        S e c t i o n I d E v e n t                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.selection;


/**
 * Class <code>SectionIdEvent</code> represents a Section Id selection
 *
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>SectionBoard
 * <dt><b>Subscribers:</b><dd>GlyphLagView, Lag, LagView
 * <dt><b>Readers:</b><dd>
 * </dl>
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SectionIdEvent
    extends LagEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected section id, which may be null */
    public final Integer id;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new SectionIdEvent object.
     *
     * @param source the entity that created this event
     * @param hint hint about event origin (or null)
     * @param id the selected section id (or null)
     */
    public SectionIdEvent (Object        source,
                           SelectionHint hint,
                           Integer       id)
    {
        super(source, hint);
        this.id = id;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getEntity //
    //-----------//
    @Override
    public Integer getData ()
    {
        return id;
    }
}
