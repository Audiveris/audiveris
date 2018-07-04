//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           A b s t r a c t W e i g h t e d E n t i t y                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.glyph;

import org.audiveris.omr.run.Orientation;
import static org.audiveris.omr.run.Orientation.HORIZONTAL;
import org.audiveris.omr.ui.util.AttachmentHolder;
import org.audiveris.omr.ui.util.BasicAttachmentHolder;
import org.audiveris.omr.util.AbstractEntity;

import java.awt.Graphics2D;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AbstractWeightedEntity}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "symbol")
public abstract class AbstractWeightedEntity
        extends AbstractEntity
        implements WeightedEntity
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final EnumSet<GlyphGroup> NO_GROUP = EnumSet.noneOf(GlyphGroup.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Assigned groups, if any. */
    @XmlList
    @XmlAttribute
    protected EnumSet<GlyphGroup> groups = EnumSet.noneOf(GlyphGroup.class);

    // Transient data
    //---------------
    //
    /** Potential attachments. */
    protected AttachmentHolder attachments;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AbstractSymbol} object.
     */
    protected AbstractWeightedEntity ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void addAttachment (String id,
                               java.awt.Shape attachment)
    {
        if (attachment != null) {
            if (attachments == null) {
                attachments = new BasicAttachmentHolder();
            }

            attachments.addAttachment(id, attachment);
        }
    }

    @Override
    public void addGroup (GlyphGroup group)
    {
        if (group != null) {
            if (groups == null) {
                groups = EnumSet.noneOf(GlyphGroup.class);
            }

            groups.add(group);
        }
    }

    @Override
    public double getAspect (Orientation orientation)
    {
        if (orientation == HORIZONTAL) {
            return getWidth() / (double) getHeight();
        } else {
            return getHeight() / (double) getWidth();
        }
    }

    @Override
    public Map<String, java.awt.Shape> getAttachments ()
    {
        if (attachments != null) {
            return attachments.getAttachments();
        } else {
            return Collections.emptyMap();
        }
    }

    @Override
    public EnumSet<GlyphGroup> getGroups ()
    {
        if (groups != null) {
            return groups;
        } else {
            return NO_GROUP;
        }
    }

    @Override
    public double getMeanThickness (Orientation orientation)
    {
        return (double) getWeight() / getLength(orientation);
    }

    @Override
    public double getNormalizedWeight (int interline)
    {
        return (double) getWeight() / (interline * interline);
    }

    @Override
    public boolean hasGroup (GlyphGroup group)
    {
        if (groups == null) {
            return false;
        }

        return groups.contains(group);
    }

    @Override
    public int removeAttachments (String prefix)
    {
        if (attachments != null) {
            return attachments.removeAttachments(prefix);
        } else {
            return 0;
        }
    }

    @Override
    public void renderAttachments (Graphics2D g)
    {
        if (attachments != null) {
            attachments.renderAttachments(g);
        }
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before the marshalling of this object begins.
     */
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        // Nullify 'groups', so that no empty element appears in XML output.
        if ((groups != null) && groups.isEmpty()) {
            groups = null;
        }
    }
}
