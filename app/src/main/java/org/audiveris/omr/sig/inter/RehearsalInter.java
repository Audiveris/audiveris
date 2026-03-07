//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e h e a r s a l I n t e r                                  //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.text.TextRole;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>RehearsalInter</code> represents a rehearsal text sentence,
 * perhaps wrapped by a rectangular enclosure.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "rehearsal")
public class RehearsalInter
        extends SentenceInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(RehearsalInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent Data
    //----------------

    /** Rectangular enclosure, if any. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.RectangleAdapter.class)
    private Rectangle enclosure;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    @SuppressWarnings("unused")
    protected RehearsalInter ()
    {
    }

    /**
     * Creates a new <code>RehearsalInter</code> object.
     *
     * @param grade     quality
     * @param enclosure rectangular enclosure, perhaps null
     */
    public RehearsalInter (Double grade,
                           Rectangle enclosure)
    {
        super(null, grade, null, TextRole.Rehearsal);
        this.enclosure = enclosure;
    }

    /**
     * Create a new <code>RehearsalInter</code> object from a former SentenceInter.
     *
     * @param s the sentence to be "replaced"
     */
    public RehearsalInter (SentenceInter s)
    {
        super(s.getBounds(), s.getGrade(), s.getMeanFont(), TextRole.Rehearsal);
        shape = Shape.TEXT; // Useful???

        generateEnclosure();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    /**
     * Add in the containing measure.
     */
    @Override
    public void added ()
    {
        super.added();

        final Point center = GeoUtil.center(enclosure);
        final Measure measure = staff.getPart().getMeasureAt(center);

        if (measure != null) {
            measure.addInter(this);
        }
    }

    //-------------------//
    // generateEnclosure //
    //-------------------//
    /**
     * Generate an enclosure around the sentence text.
     */
    private void generateEnclosure ()
    {
        getBounds();

        final int dy = (int) Math.rint(
                bounds.height * (constants.enclosureVersusTextHeight.getValue() - 1) / 2.0);
        final int dx = dy;
        enclosure = new Rectangle(
                bounds.x - dx,
                bounds.y - dy,
                bounds.width + 2 * dx,
                bounds.height + 2 * dy);
    }

    //--------------//
    // getEnclosure //
    //--------------//
    /**
     * Report the rehearsal enclosure, if any.
     *
     * @return the enclosure, perhaps null
     */
    public Rectangle getEnclosure ()
    {
        return enclosure;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + ((enclosure != null) ? " enclosure:" + enclosure : "");
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    /**
     * (Re)generate the enclosure.
     */
    @Override
    public void invalidateCache ()
    {
        super.invalidateCache();

        generateEnclosure();
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove from containing measure.
     *
     * @param extensive true for non-manual removals only
     */
    @Override
    public void remove (boolean extensive)
    {
        if (isRemoved()) {
            return;
        }

        final Measure measure = staff.getPart().getMeasureAt(getCenter());

        if (measure != null) {
            measure.removeInter(this);
        }

        super.remove(extensive);
    }

    //--------------//
    // setEnclosure //
    //--------------//
    /**
     * Set the rehearsal enclosure.
     *
     * @param enclosure the enclosure, perhaps null
     */
    public void setEnclosure (Rectangle enclosure)
    {
        this.enclosure = enclosure;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Ratio enclosureVersusTextHeight = new Constant.Ratio( //
                1.7,
                "Enclosure height as ratio of sentence height");
    }
}
