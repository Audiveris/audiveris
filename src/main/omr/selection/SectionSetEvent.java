//----------------------------------------------------------------------------//
//                                                                            //
//                       S e c t i o n S e t E v e n t                        //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.selection;

import omr.lag.Section;
import omr.lag.Sections;

import java.util.Set;

/**
 * Class <code>SectionSetEvent</code> represents a Section Set selection
 *
 * <dl>
 * <dt><b>Publishers:</b><dd>TODO
 * <dt><b>Subscribers:</b><dd>TODO
 * <dt><b>Readers:</b><dd>TODO
 * </dl>
 * @param <S> precise section type
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class SectionSetEvent<S extends Section>
    extends LagEvent
{
    //~ Instance fields --------------------------------------------------------

    /** The selected section set, which may be null */
    public final Set<S> sections;

    //~ Constructors -----------------------------------------------------------

    //-----------------//
    // SectionSetEvent //
    //-----------------//
    /**
     * Creates a new SectionSetEvent object.
     *
     * @param source the entity that created this event
     * @param hint hint about event origin (or null)
     * @param movement the mouse movement
     * @param sections the selected collection of sections (or null)
     */
    public SectionSetEvent (Object        source,
                            SelectionHint hint,
                            MouseMovement movement,
                            Set<S>        sections)
    {
        super(source, hint, movement);
        this.sections = sections;
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getEntity //
    //-----------//
    @Override
    public Set<S> getData ()
    {
        return sections;
    }

    //----------------//
    // internalString //
    //----------------//
    @Override
    protected String internalString ()
    {
        return Sections.toString(sections);
    }
}
