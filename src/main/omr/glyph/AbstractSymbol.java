//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   A b s t r a c t S y m b o l                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.run.Orientation;
import static omr.run.Orientation.HORIZONTAL;

import omr.ui.util.AttachmentHolder;
import omr.ui.util.BasicAttachmentHolder;

import omr.util.AbstractEntity;

import java.awt.Graphics2D;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code AbstractSymbol}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "symbol")
public abstract class AbstractSymbol
        extends AbstractEntity
        implements Symbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final EnumSet<Group> NO_GROUP = EnumSet.noneOf(Group.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Assigned groups, if any. */
    @XmlList
    @XmlElement
    protected EnumSet<Group> groups = EnumSet.noneOf(Group.class);

    // Transient data
    //---------------
    //
    /** Potential attachments. */
    protected AttachmentHolder attachments;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code AbstractSymbol} object.
     */
    protected AbstractSymbol ()
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
    public void addGroup (Group group)
    {
        if (group != null) {
            if (groups == null) {
                groups = EnumSet.noneOf(Group.class);
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
    public EnumSet<Group> getGroups ()
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
        return (double) getWeight() / interline * interline;
    }

    @Override
    public boolean hasGroup (Group group)
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
