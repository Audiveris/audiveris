//----------------------------------------------------------------------------//
//                                                                            //
//                             S c o r e P a r t                              //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * Class <code>ScorePart</code> defines a part at score level. It is
 * instantiated in each System by a SystemPart.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@XmlRootElement
public class ScorePart
{
    //~ Instance fields --------------------------------------------------------

    /**
     * Distinguished id for this part (the same id is used by the corresponding
     * SystemPart in each System)
     */
    private Integer id;

    /** Name for this part */
    private String name;

    /** List of staff indices */
    private List<Integer> indices = new ArrayList<Integer>();

    //~ Constructors -----------------------------------------------------------

    /** Meant for JAXB only */
    private ScorePart()
    {
    }

    //-----------//
    // ScorePart //
    //-----------//
    /** Creates a new instance of ScorePart, built from a SystemPart
     *
     * @param systemPart the concrete SystemPart
     */
    public ScorePart (SystemPart systemPart)
    {
        for (Staff staff : systemPart.getStaves()) {
            indices.add(staff.getStafflink());
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // setId //
    //-------//
    /**
     * Assign an id to this part
     *
     * @param id the assigned id
     */
    public void setId (int id)
    {
        this.id = id;
    }

    //-------//
    // getId //
    //-------//
    /**
     * Report the id of this part
     *
     * @return the part id
     */
    public int getId ()
    {
        return id;
    }

    //------------//
    // getIndices //
    //------------//
    /**
     * Report the staff indices for this part
     *
     * @return the list of staff indices
     */
    public List<Integer> getIndices ()
    {
        return indices;
    }

    //---------//
    // setName //
    //---------//
    /**
     * Assign a name to this part
     *
     * @param name the new part name
     */
    public void setName (String name)
    {
        this.name = name;
    }

    //---------//
    // getName //
    //---------//
    /**
     * Report the assigned name
     *
     * @return the part name
     */
    public String getName ()
    {
        return name;
    }

    //--------//
    // getPid //
    //--------//
    /**
     * Report a pid string, using format "Pn", where 'n' is the id
     *
     * @return the Pid
     */
    public String getPid ()
    {
        return "P" + id;
    }

    //--------//
    // equals //
    //--------//
    /**
     * Check whether the list of indices are identical
     *
     * @param obj the object to compare to
     * @return true if equal
     */
    @Override
    public boolean equals (Object obj)
    {
        if (obj instanceof ScorePart) {
            ScorePart sp = (ScorePart) obj;

            if (sp.indices.size() != indices.size()) {
                return false;
            }

            for (int i = 0; i < indices.size(); i++) {
                if (!(sp.indices.get(i).equals(indices.get(i)))) {
                    return false;
                }
            }

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
        StringBuilder sb = new StringBuilder();
        sb.append("{ScorePart");

        if (id != null) {
            sb.append(" id=")
              .append(id);
        }

        if (name != null) {
            sb.append(" name=")
              .append(name);
        }

        sb.append(" [");

        for (Integer i : indices) {
            sb.append(i + " ");
        }

        sb.append("]}");

        return sb.toString();
    }
}
