//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 A n c h o r e d T e m p l a t e                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.image;

import org.audiveris.omr.image.Anchored.Anchor;

/**
 * Class {@code AnchoredTemplate} is a Template handled through a specific Anchor.
 *
 * @author Hervé Bitteur
 */
public class AnchoredTemplate
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Specific anchor. */
    public final Anchor anchor;

    /** Related template. */
    public final Template template;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AnchoredTemplate} object.
     *
     * @param anchor   anchor WRT template
     * @param template the template
     */
    public AnchoredTemplate (Anchor anchor,
                             Template template)
    {
        this.anchor = anchor;
        this.template = template;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("{");

        sb.append(template.getShape());
        sb.append(" ").append(template.getPointSize());

        if (anchor != null) {
            sb.append(" ").append(anchor);
        }

        sb.append("}");

        return sb.toString();
    }
}
