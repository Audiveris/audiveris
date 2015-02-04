//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 A n c h o r e d T e m p l a t e                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import omr.image.Anchored.Anchor;

/**
 * Class {@code AnchoredTemplate} is a Template handled through a specific Anchor.
 *
 * @author Hervé Bitteur
 */
public class AnchoredTemplate
{
    //~ Instance fields ----------------------------------------------------------------------------

    public final Anchor anchor;

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
        StringBuilder sb = new StringBuilder("{");

        sb.append(template.getShape());
        sb.append(" ").append(template.getInterline());

        if (anchor != null) {
            sb.append(" ").append(anchor);
        }

        sb.append("}");

        return sb.toString();
    }
}
