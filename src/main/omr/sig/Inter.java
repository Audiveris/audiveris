//----------------------------------------------------------------------------//
//                                                                            //
//                                  I n t e r                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.util.Vip;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.util.Comparator;

/**
 * Interface {@code Inter} defines a possible interpretation.
 *
 * @author Hervé Bitteur
 */
public interface Inter
        extends VisitableInter, Vip
{
    //~ Static fields/initializers ---------------------------------------------

    /**
     * For comparing interpretations by id.
     */
    public static final Comparator<Inter> byId = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter i1,
                            Inter i2)
        {
            return Integer.compare(i1.getId(), i2.getId());
        }
    };

    /**
     * For comparing interpretations by abscissa.
     */
    public static final Comparator<Inter> byAbscissa = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter i1,
                            Inter i2)
        {
            return Integer.compare(i1.getBounds().x, i2.getBounds().x);
        }
    };

    /**
     * For comparing interpretations by ordinate.
     */
    public static final Comparator<Inter> byOrdinate = new Comparator<Inter>()
    {
        @Override
        public int compare (Inter i1,
                            Inter i2)
        {
            return Integer.compare(i1.getBounds().y, i2.getBounds().y);
        }
    };

    //~ Methods ----------------------------------------------------------------
    /**
     * Delete this instance, and remove it from its containing SIG.
     */
    void delete ();

    /**
     * Report a complete dump for this interpretation.
     *
     * @return a complete string dump
     */
    String dumpOf ();

    /**
     * Report the precise defining area
     *
     * @return the inter area, if any
     */
    Area getArea ();

    /**
     * Report the bounding box for this interpretation.
     *
     * @return the bounding box
     */
    Rectangle getBounds ();

    /**
     * Report the contextual grade, (0..1 probability) computed for
     * interpretation.
     *
     * @return the contextual grade, if any
     */
    Double getContextualGrade ();

    /**
     * Report the core box for this interpretation.
     *
     * @return a small core box
     */
    Rectangle2D getCoreBounds ();

    /**
     * Details for tip.
     *
     * @return informations for a tip
     */
    String getDetails ();

    /**
     * Report the glyph, if any, which is concerned by this interpretation.
     *
     * @return the underlying glyph, or null
     */
    Glyph getGlyph ();

    /**
     * Report the intrinsic grade (0..1 probability) assigned to
     * interpretation
     *
     * @return the intrinsic grade
     */
    double getGrade ();

    /**
     * Report the interpretation id (for debugging)
     *
     * @return the id or 0 if not yet identified
     */
    int getId ();

    /**
     * Report details about the final grade
     *
     * @return the grade details
     */
    GradeImpacts getImpacts ();

    /**
     * Report the shape related to interpretation.
     *
     * @return the shape
     */
    Shape getShape ();

    /**
     * Report the sig which hosts this interpretation.
     *
     * @return the containing sig
     */
    SIGraph getSig ();

    /**
     * Report whether this instance has been deleted.
     *
     * @return true if deleted
     */
    boolean isDeleted ();

    /**
     * Report whether the interpretation has a good grade.
     *
     * @return true if grade is good
     */
    boolean isGood ();

    /**
     * Report whether this interpretation represents the same thing
     * as that interpretation
     *
     * @param that the other inter to check
     * @return true if identical, false otherwise
     */
    boolean isSameAs (Inter that);

    /**
     * Check whether this inter instance overlaps that inter instance.
     *
     * @param that the other instance
     * @return true if overlap is detected
     */
    boolean overlaps (Inter that);

    /**
     * Assign the bounding box for this interpretation.
     * The assigned bounds may be different from the underlying glyph bounds.
     *
     * @param box the bounding box
     */
    void setBounds (Rectangle box);

    /**
     * Assign the contextual grade, (0..1 probability) computed for
     * interpretation.
     *
     * @param value the contextual grade value
     */
    void setContextualGrade (double value);

    /**
     * Assign an id to the interpretation
     *
     * @param id the inter id
     */
    void setId (int id);

    /**
     * Assign details about the final grade
     *
     * @param impacts the grade impacts
     */
    void setImpacts (GradeImpacts impacts);

    /**
     * Assign the containing SIG
     *
     * @param sig the containing SIG
     */
    void setSig (SIGraph sig);
}
