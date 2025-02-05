//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            P a r a m                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.util.param;

import org.audiveris.omr.util.Navigable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

/**
 * Class <code>Param</code> defines data value at global scope, book scope, sheet scope.
 * <p>
 * The {@link #getValue()} reports the current data value:
 * <ol>
 * <li>If the param instance has a non-null specific value, this specific value is returned.
 * <li>Otherwise, if this instance has a registered parent param, parent.getValue() is returned.
 * <li>Otherwise, null is returned.
 * </ol>
 * <p>
 * <img src="doc-files/Param.png" alt="Param UML">
 *
 * @param <E> type of parameter handled
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Param<E>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Unique object for global scope. */
    public static final String GLOBAL_SCOPE = "Global";

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Specific value, if any. */
    protected E specific;

    // Transient data
    //---------------

    /** Parent param, if any, to inherit from. */
    protected Param<E> parent;

    /** Scope of this param: GLOBAL_SCOPE, otherwise book or sheet/stub. */
    @Navigable(false)
    protected Object scope;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a <b>Param</b> instance with its related scope.
     *
     * @param scope the owning scope (GLOBAL_SCOPE, book or sheet/stub)
     */
    public Param (Object scope)
    {
        this.scope = scope;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // getScope //
    //----------//
    /**
     * Report the scope of this param.
     *
     * @return scope, perhaps null
     */
    public Object getScope ()
    {
        return scope;
    }

    //----------------//
    // getSourceValue //
    //----------------//
    /**
     * Report the source value if any, by default this return null.
     *
     * @return the source value
     */
    public E getSourceValue ()
    {
        return null;
    }

    //-------------//
    // getSpecific //
    //-------------//
    /**
     * Report the specific parameter value, if any.
     *
     * @return specific value or null
     */
    public E getSpecific ()
    {
        return specific;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the parameter value, which is the specific value if any, otherwise the
     * value of the parent.
     *
     * @return the parameter value
     */
    public E getValue ()
    {
        final boolean isSpecific = isSpecific();

        if (isSpecific) {
            return getSpecific();
        }

        if (parent != null) {
            return parent.getValue();
        }

        return null;
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, typically for inclusion in a toString.
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        final StringBuilder sb = new StringBuilder();

        sb.append("scope:").append(scope);

        if (getSpecific() != null) {
            sb.append(" specific:").append(getSpecific());
        }

        if (parent != null) {
            sb.append(" parent:").append(parent);
        }

        return sb.toString();
    }

    //------------//
    // isSpecific //
    //------------//
    /**
     * Report whether this Param instance holds a specific value.
     *
     * @return true id specific
     */
    public boolean isSpecific ()
    {
        return specific != null;
    }

    //-----------//
    // setParent //
    //-----------//
    /**
     * Assign a parent to be used as default.
     *
     * @param parent parent for default value
     */
    public void setParent (Param<E> parent)
    {
        this.parent = parent;
    }

    //----------//
    // setScope //
    //----------//
    /**
     * Setter for scope field, needed after unmarshalling.
     *
     * @param scope the scope to set
     */
    public void setScope (Object scope)
    {
        this.scope = scope;
    }

    //-------------//
    // setSpecific //
    //-------------//
    /**
     * Defines a (new) specific value
     *
     * @param specific the new specific value
     * @return true if the new value is actually different
     */
    public boolean setSpecific (E specific)
    {
        final E currentSpecific = getSpecific();

        if (currentSpecific == null) {
            if (specific == null) {
                return false;
            }
        } else {
            if (currentSpecific.equals(specific)) {
                return false;
            }
        }

        this.specific = specific;

        return true;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName()).append('{').append(internalsString())
                .append('}').toString();
    }
}
