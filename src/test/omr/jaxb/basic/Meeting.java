//
package omr.jaxb.basic;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * DOCUMENT ME!
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Meeting
{
    //~ Instance fields --------------------------------------------------------

    @XmlAttribute
    public int              start;
    @XmlAttribute
    public int              stop;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new Meeting object.
     *
     * @param start DOCUMENT ME!
     * @param stop DOCUMENT ME!
     */
    public Meeting (int start,
                    int stop)
    {
        this.start = start;
        this.stop = stop;
    }
    public Meeting (){}
}
