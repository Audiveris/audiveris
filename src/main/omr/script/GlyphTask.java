//----------------------------------------------------------------------------//
//                                                                            //
//                             G l y p h T a s k                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.lag.LagOrientation;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.Stepping;
import omr.step.Steps;

import java.util.*;

import javax.xml.bind.annotation.*;

/**
 * Class {@code  GlyphTask} handles a collection of glyphs.
 * These glyphs may already exist (as in a plain {@link GlyphUpdateTask})
 * or remain to be created (as in {@link InsertTask})</p>
 *
 * <h4>Glyphs and sections in a script:<br/>
 *    <img src="doc-files/script.jpg"/>
 * </h4>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class GlyphTask
    extends SheetTask
{
    //~ Instance fields --------------------------------------------------------

    /** Lag orientation */
    @XmlAttribute
    protected final LagOrientation orientation;

    /** The collection of glyphs which are concerned by this task */
    protected Set<Glyph> glyphs;

    /** The set of (pre) impacted systems, using status before action */
    protected SortedSet<SystemInfo> initialSystems;

    //~ Constructors -----------------------------------------------------------

    //-----------//
    // GlyphTask //
    //-----------//
    /**
     * Creates a new GlyphTask object.
     *
     * @param sheet the sheet impacted
     * @param orientation orientation of the containing lag
     * @param glyphs the collection of glyphs concerned by this task
     */
    protected GlyphTask (Sheet             sheet,
                         LagOrientation    orientation,
                         Collection<Glyph> glyphs)
    {
        // Check parameters
        if (orientation == null) {
            throw new IllegalArgumentException(
                getClass().getSimpleName() + " needs a non-null orientation");
        }

        if ((glyphs == null) || glyphs.isEmpty()) {
            throw new IllegalArgumentException(
                getClass().getSimpleName() + " needs at least one glyph");
        }

        this.orientation = orientation;

        this.glyphs = new TreeSet<Glyph>(Glyph.globalComparator);
        this.glyphs.addAll(glyphs);
    }

    //-----------//
    // GlyphTask //
    //-----------//
    /**
     * Creates a new GlyphTask object, for vertical glyphs by default
     *
     * @param sheet the sheet impacted
     * @param glyphs the collection of glyphs concerned by this task
     */
    protected GlyphTask (Sheet             sheet,
                         Collection<Glyph> glyphs)
    {
        this(sheet, LagOrientation.VERTICAL, glyphs);
    }

    //-----------//
    // GlyphTask //
    //-----------//
    /**
     * Creates a new GlyphTask object, of specified orientation
     * @param sheet the sheet impacted
     * @param orientation the specified lag orientation
     */
    protected GlyphTask (Sheet          sheet,
                         LagOrientation orientation)
    {
        super(sheet);

        if (orientation == null) {
            throw new IllegalArgumentException(
                getClass().getSimpleName() + " needs a non-null orientation");
        } else {
            this.orientation = orientation;
        }
    }

    //-----------//
    // GlyphTask //
    //-----------//
    /**
     * Constructor needed by no-arg constructors of subclasses (for JAXB)
     */
    protected GlyphTask ()
    {
        orientation = null;
    }

    //~ Methods ----------------------------------------------------------------

    //--------------------//
    // getImpactedSystems //
    //--------------------//
    /**
     * Report the set of systems that are impacted by the action
     * @param sheet the containing sheet
     * @return the ordered set of impacted systems
     */
    public SortedSet<SystemInfo> getImpactedSystems (Sheet sheet)
    {
        SortedSet<SystemInfo> impactedSystems = new TreeSet<SystemInfo>();
        impactedSystems.addAll(initialSystems);
        impactedSystems.addAll(retrieveCurrentImpact(sheet));

        return impactedSystems;
    }

    //------------------//
    // getInitialGlyphs //
    //------------------//
    /**
     * Report the collection of initial glyphs
     * @return the impactedGlyphs
     */
    public Set<Glyph> getInitialGlyphs ()
    {
        return glyphs;
    }

    //--------//
    // epilog //
    //--------//
    @Override
    public void epilog (Sheet sheet)
    {
        switch (orientation) {
        case HORIZONTAL :
            Stepping.reprocessSheet(
                Steps.valueOf(Steps.SYSTEMS),
                sheet,
                null,
                false);

            break;

        case VERTICAL :
            Stepping.reprocessSheet(
                Steps.valueOf(Steps.PATTERNS),
                sheet,
                getImpactedSystems(sheet),
                false);
        }
    }

    //------------------//
    // impactAllSystems //
    //------------------//
    /**
     * Set the impacted systems as all systems
     */
    public void impactAllSystems ()
    {
        initialSystems.addAll(sheet.getSystems());
    }

    //--------//
    // prolog //
    //--------//
    @Override
    public void prolog (Sheet sheet)
    {
        super.prolog(sheet);
        this.sheet = sheet;

        // Make sure the concrete sections and glyphs are available
        if (glyphs == null) {
            retrieveGlyphs();
        }

        initialSystems = retrieveCurrentImpact(sheet);
    }

    //----------------//
    // retrieveGlyphs //
    //----------------//
    /**
     * This method is in charge of retrieving the glyphs to be handled, using
     * either their composing sections ids or their shape and locations.
     */
    protected abstract void retrieveGlyphs ();

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        if (glyphs != null) {
            sb.append(" ")
              .append(Glyphs.toString(glyphs));
        } else {
            sb.append(" no-glyphs");
        }

        return sb.toString();
    }

    //-----------//
    // remaining //
    //-----------//
    /**
     * Report the collection of systems that follow the provided one
     * @param system the one after which we pick any system
     * @return the remaining portion of the sequence of systems in the sheet
     */
    protected Collection<SystemInfo> remaining (SystemInfo system)
    {
        List<SystemInfo> all = sheet.getSystems();

        return all.subList(all.indexOf(system) + 1, all.size());
    }

    //-----------------------//
    // retrieveCurrentImpact //
    //-----------------------//
    /**
     * Report the set of systems that are impacted by the action, as determined
     * by the *current status* of the glyphs *currently* pointed by the sections
     * @param sheet the containing sheet
     * @return the ordered set of impacted systems
     */
    protected SortedSet<SystemInfo> retrieveCurrentImpact (Sheet sheet)
    {
        SortedSet<SystemInfo> impactedSystems = new TreeSet<SystemInfo>();

        for (Glyph glyph : glyphs) {
            SystemInfo system = sheet.getSystemOf(glyph);

            if (system != null) {
                impactedSystems.add(system);
            }

            if (glyph != null) {
                Shape shape = glyph.getShape();

                if ((shape != null) && shape.isPersistent()) {
                    // Include all following systems
                    impactedSystems.addAll(remaining(system));
                }
            }
        }

        return impactedSystems;
    }
}
