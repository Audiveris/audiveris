//----------------------------------------------------------------------------//
//                                                                            //
//                                D u m p e r                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import omr.util.Dumping.Relevance;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

/**
 * Class {@code Dumper} is a debugging utility that reports, in a brute
 * force manner, any internal data of a class instance.
 *
 * <p> When used on a class instance, all class internal fields which are
 * considered as "relevant" are printed using their toString() method, then we
 * walk up the inheritance tree and repeat the same actions, until there is no
 * more superclass or until the superclass we have reached is considered as
 * non-relevant. </p>
 *
 * <p> A (super)class is considered "relevant" if the static method
 * {@code isClassRelevant(class)} returns true. This method can be
 * overridden in a subclass of Dumper to adapt to local needs. </p>
 *
 * <p> A field is considered "relevant" if the following condition if the method
 * {@code isFieldRelevant(field)} returns true. Similarly, the behavior of
 * this predicate can be customized by subclassing the Dumper class. </p>
 *
 * <p> There are several kinds of print outs available through subclassing. Each
 * of them export two public methods: {@code dump()} which prints the
 * result on default output stream, and {@code dumpOf()} which simply
 * returns the generated dump string.
 *
 * <ul> <li> <b>Column</b> a dump with one line per field </li>
 *
 * <li> <b>Row</b> a dump with all information on one row </li>
 *
 * <li> <b>Html</b> an Html stream with fields arranged in tables </li>
 *
 * </ul>
 *
 * Here are some examples of use:
 * <pre>
 * // Using the predefined static helper methods
 * Dumper.dump(myinstance);
 * Dumper.dump(myinstance, "My Title");
 * Dumper.dump(myinstance, "My Title", 2);
 * System.out.println(Dumper.dumpOf(myinstance));
 * System.out.println(Dumper.htmlDumpOf(myinstance));
 *
 *  // Using directly the Dumper subclasses
 * new Dumper.Column(myinstance).print();
 * System.out.println(new Dumper.Row(myinstance).toString());
 * display(new Dumper.Html(myinstance).toString());
 * </pre>
 *
 * @author Hervé Bitteur
 */
public class Dumper
{
    //~ Static fields/initializers ---------------------------------------------

    /** Maximum number of collection items printed */
    private static final int MAX_COLLECTION_INDEX = 9;

    //~ Instance fields --------------------------------------------------------
    /** To filter classes and fields */
    protected final Relevance relevance;

    /**
     * The object to be dumped
     */
    protected final Object object;

    /**
     * The string buffer used as output
     */
    protected final StringBuilder sb;

    /**
     * Can we use HTML directives?
     */
    protected final boolean useHtml;

    /**
     * Class (beware, this variable is updated as we walk up the inheritance
     * tree)
     */
    protected Class<?> classe;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new Dumper.
     *
     * @param relevance the relevance filter
     * @param object    the object instance to be dumped.
     * @param useHtml   ????
     */
    public Dumper (Relevance relevance,
                   Object object,
                   boolean useHtml)
    {
        this.relevance = relevance;

        // (re)Allocate the string buffer
        sb = new StringBuilder(1024);

        // Cache the object & the related class
        this.object = object;
        this.useHtml = useHtml;
        classe = object.getClass();
    }

    //~ Methods ----------------------------------------------------------------
    //-------//
    // print //
    //-------//
    /**
     * Print the dump string onto the standard output
     */
    public void print ()
    {
        System.out.println(this);
    }

    //----------//
    // toString //
    //----------//
    /**
     * Return the string buffer content
     *
     * @return the dump of the object as a string
     */
    @Override
    public String toString ()
    {
        // Do the processing
        processObject();

        // Return the final content of string buffer
        return sb.toString();
    }

    //------------------//
    // printClassEpilog //
    //------------------//
    /**
     * To be overridden so as to print the epilog of class data
     */
    protected void printClassEpilog ()
    {
    }

    //------------------//
    // printClassProlog //
    //------------------//
    /**
     * To be overridden so as to print the prolog of class data
     */
    protected void printClassProlog ()
    {
    }

    //----------------------//
    // printCollectionValue //
    //----------------------//
    protected void printCollectionValue (Collection<?> collection)
    {
        sb.append("[");

        int i = 0;

        for (Object obj : collection) {
            if (i++ > 0) {
                sb.append(useHtml ? ",<br/>" : ",");
            }

            // Safeguard action when the object is a big collection
            if (i > MAX_COLLECTION_INDEX) {
                sb.append(" ... ")
                        .append(collection.size())
                        .append(" items");

                break;
            } else {
                sb.append(obj);
            }
        }

        sb.append("]");
    }

    //------------//
    // printField //
    //------------//
    /**
     * Basic printing of field name and value. The method can of course be
     * overridden.
     *
     * @param name  the field name
     * @param value the field value, which may be null
     */
    protected void printField (String name,
                               Object value)
    {
        if (value == null) {
            sb.append("null");
        } else {
            if (value instanceof Collection) {
                printCollectionValue((Collection) value);
            } else if (value instanceof Map) {
                printCollectionValue(((Map) value).entrySet());
            } else {
                sb.append(value.toString());
            }
        }
    }

    //--------------//
    // processClass //
    //--------------//
    private void processClass ()
    {
        // Class Prolog
        printClassProlog();

        // Process the class Fields
        for (Field field : classe.getDeclaredFields()) {
            processField(field);
        }

        // Class Epilog
        printClassEpilog();
    }

    //--------------//
    // processField //
    //--------------//
    private void processField (Field field)
    {
        // Check that we are really interested in printing this field out
        if (relevance.isFieldRelevant(field)) {
            // Override any access limitation
            field.setAccessible(true);

            try {
                // Retrieve field value in the object instance
                Object value = field.get(object);

                // Print the field value as requested
                printField(field.getName(), value);
            } catch (IllegalAccessException ex) {
                // Cannot occur in fact, thanks to setAccessible
            }
        }
    }

    //---------------//
    // processObject //
    //---------------//
    private void processObject ()
    {
        do {
            // Process the class at hand
            processClass();

            // Walk up the inheritance tree
            classe = classe.getSuperclass();
        } while (relevance.isClassRelevant(classe));
    }

    //~ Inner Classes ----------------------------------------------------------
    //--------//
    // Column //
    //--------//
    /**
     * Class {@code Column} implements a Dumper where all fields are
     * presented in one column, each field on a separate line. The column can be
     * left indented, according to the specified indentation level.
     */
    public static class Column
            extends Dumper
    {
        //~ Static fields/initializers -----------------------------------------

        private static final String MEMBER_GAP = "   ";

        private static final String INDENT_GAP = ".  ";

        //~ Instance fields ----------------------------------------------------
        private final String title;

        private final StringBuilder prefix;

        //~ Constructors -------------------------------------------------------
        public Column (Relevance relevance,
                       Object object,
                       String title,
                       int level)
        {
            super(relevance, object, false);

            // Cache the title
            if (title != null) {
                this.title = title;
            } else {
                this.title = "";
            }

            // Prepare indent prefix
            prefix = new StringBuilder(level * INDENT_GAP.length());

            for (int i = level; i > 0; i--) {
                prefix.append(INDENT_GAP);
            }
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected void printClassProlog ()
        {
            // We print the class name only for the lowest class in
            // heritance hierarchy
            if (object.getClass() == classe) {
                sb.append("\n");
                sb.append(prefix)
                        .append(classe.getName());
                sb.append(" ")
                        .append(title)
                        .append(":");
            }
        }

        @Override
        protected void printField (String name,
                                   Object value)
        {
            sb.append("\n");
            sb.append(prefix)
                    .append(MEMBER_GAP);
            sb.append(name)
                    .append("=");
            super.printField(name, value);
        }
    }

    //------//
    // Html //
    //------//
    /**
     * Class {@code Html} implements a Dumper using HTML tags to present
     * fields in a table.
     */
    public static class Html
            extends Dumper
    {
        //~ Constructors -------------------------------------------------------

        public Html (Relevance relevance,
                     Object object)
        {
            super(relevance, object, true);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public String toString ()
        {
            // Style
            sb.append("<style> td {")
                    .append(" font-family: Lucida Console, Verdana, sans-serif;")
                    .append(" font-size: 9px;")
                    .append(" font-style: normal;")
                    .append("} </style>");

            // Table begin
            sb.append("<table border=0 cellpadding=3>");

            // The object
            super.processObject();

            // Table end
            sb.append("</table>");

            // Return the final content of string buffer
            return sb.toString();
        }

        @Override
        protected void printClassProlog ()
        {
            // Class name
            sb.append("<tr><td colspan=2><font color='BLUE'>")
                    .append(classe.getName())
                    .append("</font></td></tr>");
        }

        @Override
        protected void printField (String name,
                                   Object value)
        {
            // One table row per field
            sb.append("<tr>");

            // First the field name
            sb.append("<td align='right'><font color='RED'>")
                    .append(name)
                    .append("</font></td>");

            // Then the field value
            sb.append("<td>");
            super.printField(name, value);

            sb.append("</td>")
                    .append("</tr>");
        }
    }

    //-----//
    // Row //
    //-----//
    /**
     * Class {@code Row} implements a Dumper where all fields are
     * presented on the same line.
     */
    public static class Row
            extends Dumper
    {
        //~ Constructors -------------------------------------------------------

        public Row (Relevance relevance,
                    Object object)
        {
            super(relevance, object, false);
        }

        //~ Methods ------------------------------------------------------------
        @Override
        protected void printClassEpilog ()
        {
            sb.append("}");
        }

        @Override
        protected void printClassProlog ()
        {
            // Class name
            sb.append("{");

            // Special annotation for superclass
            if (object.getClass() != classe) {
                sb.append("from ");
            }

            sb.append(classe.getName())
                    .append(":");
        }

        @Override
        protected void printField (String name,
                                   Object value)
        {
            sb.append(" ");
            sb.append(name)
                    .append("=");
            super.printField(name, value);
        }
    }
}
