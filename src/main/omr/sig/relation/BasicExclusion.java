//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B a s i c E x c l u s i o n                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.relation;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code BasicExclusion}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "exclusion")
public class BasicExclusion
        extends AbstractRelation
        implements Exclusion
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    public final Cause cause;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicExclusion object.
     *
     * @param cause root cause of this exclusion
     */
    public BasicExclusion (Cause cause)
    {
        this.cause = cause;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BasicExclusion ()
    {
        this.cause = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String getName ()
    {
        return "Exclusion";
    }

    @Override
    protected String internals ()
    {
        return super.internals() + cause;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    static class Adapter
            extends XmlAdapter<BasicExclusion, Exclusion>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public Adapter ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public BasicExclusion marshal (Exclusion itf)
                throws Exception
        {
            return (BasicExclusion) itf;
        }

        @Override
        public Exclusion unmarshal (BasicExclusion impl)
                throws Exception
        {
            return impl;
        }
    }
}
