//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            P a r a m                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.util;

/**
 * Class {@code Param} handles the context of operations performed on score and/or pages.
 *
 * @param <E> type of parameter handled
 *
 * @author Hervé Bitteur
 */
public class Param<E>
{
    //~ Instance fields ----------------------------------------------------------------------------

    //
    /** Parent param, if any, to inherit from. */
    protected final Param<E> parent;

    /** Specifically set parameter, if any. */
    protected E specific;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a Param object, with no parent.
     */
    public Param ()
    {
        this(null);
    }

    /**
     * Creates a Param object.
     *
     * @param parent parent context, or null
     */
    public Param (Param<E> parent)
    {
        this.parent = parent;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // getSpecific //
    //-------------//
    public E getSpecific ()
    {
        return specific;
    }

    //-----------//
    // getTarget //
    //-----------//
    public E getTarget ()
    {
        if (getSpecific() != null) {
            return getSpecific();
        } else if (parent != null) {
            return parent.getTarget();
        } else {
            return null;
        }
    }

    //
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
        if ((getSpecific() == null) || !getSpecific().equals(specific)) {
            this.specific = specific;

            return true;
        } else {
            return false;
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");
        sb.append(internalsString());
        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // internalsString //
    //-----------------//
    /**
     * Return the string of the internals of this class, typically for
     * inclusion in a toString.
     *
     * @return the string of internals
     */
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder();

        if (parent != null) {
            sb.append("parent:").append(parent);
        }

        if (getSpecific() != null) {
            sb.append(" specific:").append(getSpecific());
        }

        return sb.toString();
    }
}
