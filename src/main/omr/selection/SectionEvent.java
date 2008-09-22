//----------------------------------------------------------------------------//
//                                                                            //
//                          S e c t i o n E v e n t                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.selection;

import omr.lag.Section;

/**
 * Class <code>SectionEvent</code> represents a Section selection
 *
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>GlyphLag, Lag, LagView
 * <dt><b>Subscribers:</b><dd>GlyphLag, Lag
 * <dt><b>Readers:</b><dd>GlyphLagView, SectionBoard
 * </dl>
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SectionEvent<S extends Section>
    extends LagEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected section, which may be null */
    public final S section;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // SectionEvent //
    //--------------//
    /**
     * Creates a new SectionEvent object.
     *
     * @param source the entity that created this event
     * @param hint hint about event origin (or null)
     * @param section the selected section (or null)
     */
    public SectionEvent (Object        source,
                         SelectionHint hint,
                         S             section)
    {
        super(source, hint);
        this.section = section;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getEntity //
    //-----------//
    @Override
    public S getData ()
    {
        return section;
    }
}
