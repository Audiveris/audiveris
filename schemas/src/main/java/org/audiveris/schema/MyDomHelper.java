//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      M y D o m H e l p e r                                     //
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
package org.audiveris.schema;

import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.location.ClassLocation;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.location.FieldLocation;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.location.MethodLocation;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.SortedMap;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.JavaDocData;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.JavaDocRenderer;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.SortableLocation;

/**
 * Helper class stashing commonly used algorithms to work with DOM documents.
 * <p>
 * HB: This is a modified version of class
 * org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.DomHelper
 * in order to process ref elements.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 * @since 2.3
 */
public class MyDomHelper
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final String NAME_ATTRIBUTE = "name";

    private static final String REF_ATTRIBUTE = "ref";

    private static final String VALUE_ATTRIBUTE = "value";

    /**
     * The name of the annotation element.
     */
    public static final String ANNOTATION_ELEMENT_NAME = "annotation";

    /**
     * The name of the documentation element.
     */
    public static final String DOCUMENTATION_ELEMENT_NAME = "documentation";

    /**
     * The namespace schema prefix for the URI {@code http://www.w3.org/2001/XMLSchema}
     * (i.e. {@code XMLConstants.W3C_XML_SCHEMA_NS_URI}).
     *
     * @see javax.xml.XMLConstants#W3C_XML_SCHEMA_NS_URI
     */
    public static final String XSD_SCHEMA_NAMESPACE_PREFIX = "xs";

    /**
     * The names of DOM Elements corresponding to Java class Fields or Methods.
     */
    public static final List<String> CLASS_FIELD_METHOD_ELEMENT_NAMES = Arrays.asList("element",
                                                                                      "attribute");

    /**
     * The names of DOM Elements corresponding to Java enum Fields or Methods.
     */
    public static final List<String> ENUMERATION_FIELD_METHOD_ELEMENT_NAMES = Collections
            .singletonList("enumeration");

    //~ Constructors -------------------------------------------------------------------------------

    /*
     * Hide constructor for utility classes
     */
    private MyDomHelper ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Retrieves the value of the {@code name} attribute of the supplied Node.
     *
     * @param aNode A DOM Node.
     * @return the value of the {@code name} attribute of the supplied Node/Element.
     */
    public static String getNameAttribute (final Node aNode)
    {
        return getNamedAttribute(aNode, NAME_ATTRIBUTE);
    }

    /**
     * Retrieves the value of the {@code value} attribute of the supplied Node.
     *
     * @param aNode A DOM Node.
     * @return the value of the {@code value} attribute of the supplied Node/Element.
     */
    public static String getValueAttribute (final Node aNode)
    {
        return getNamedAttribute(aNode, VALUE_ATTRIBUTE);
    }

    public static String getRefAttribute (final Node aNode)
    {
        return getNamedAttribute(aNode, "ref");
    }

    /**
     * Checks if the supplied DOM Node is a DOM Element having a defined "name" attribute.
     *
     * @param aNode A DOM Node.
     * @return {@code true} if the supplied aNode is an Element having a defined "name" attribute.
     */
    public static boolean isNamedElement (final Node aNode)
    {

        final boolean isElementNode = aNode != null && aNode.getNodeType() == Node.ELEMENT_NODE;

        return isElementNode
                       && getNamedAttribute(aNode, NAME_ATTRIBUTE) != null
                       && !getNamedAttribute(aNode, NAME_ATTRIBUTE).isEmpty();
    }

    /**
     * Checks if the supplied DOM Node is a DOM Element having a defined "ref" attribute.
     *
     * HB: method added
     *
     * @param aNode A DOM Node.
     * @return {@code true} if the supplied aNode is an Element having a defined "ref" attribute.
     */
    public static boolean isRefElement (final Node aNode)
    {

        final boolean isElementNode = aNode != null && aNode.getNodeType() == Node.ELEMENT_NODE;

        return isElementNode
                       && getNamedAttribute(aNode, REF_ATTRIBUTE) != null
                       && !getNamedAttribute(aNode, REF_ATTRIBUTE).isEmpty();
    }

    /**
     * Retrieves the TagName for the supplied Node if it is an Element, and null otherwise.
     *
     * @param aNode A DOM Node.
     * @return The TagName of the Node if it is an Element, and null otherwise.
     */
    public static String getElementTagName (final Node aNode)
    {

        if (aNode != null && aNode.getNodeType() == Node.ELEMENT_NODE) {

            final Element theElement = (Element) aNode;
            return theElement.getTagName();
        }

        // The Node was not an Element.
        return null;
    }

    /**
     * <p>
     * Adds the given formattedDocumentation within an XML documentation annotation under the
     * supplied Node.
     * Only adds the documentation annotation if the formattedDocumentation is non-null and
     * non-empty. The
     * documentation annotation is on the form:</p>
     * <pre>
     *     <code>
     *         &lt;xs:annotation&gt;
     *             &lt;xs:documentation&gt;(JavaDoc here, within a CDATA section)&lt;/xs:documentation&gt;
     *         &lt;/xs:annotation&gt;
     *     </code>
     * </pre>
     *
     * @param aNode                  The non-null Node to which an XML documentation annotation
     *                               should be added.
     * @param formattedDocumentation The documentation text to add.
     */
    public static void addXmlDocumentAnnotationTo (final Node aNode,
                                                   final String formattedDocumentation)
    {

        if (aNode != null && formattedDocumentation != null && !formattedDocumentation.isEmpty()) {

            // Add the new Elements, as required.
            final Document doc = aNode.getOwnerDocument();
            final Element annotation = doc.createElementNS(
                    XMLConstants.W3C_XML_SCHEMA_NS_URI, ANNOTATION_ELEMENT_NAME);
            final Element docElement = doc.createElementNS(
                    XMLConstants.W3C_XML_SCHEMA_NS_URI, DOCUMENTATION_ELEMENT_NAME);
            final CDATASection xsdDocumentation = doc.createCDATASection(formattedDocumentation);

            // Set the prefixes
            annotation.setPrefix(XSD_SCHEMA_NAMESPACE_PREFIX);
            docElement.setPrefix(XSD_SCHEMA_NAMESPACE_PREFIX);

            // Inject the formattedDocumentation into the CDATA section.
            annotation.appendChild(docElement);
            final Node firstChildOfCurrentNode = aNode.getFirstChild();
            if (firstChildOfCurrentNode == null) {
                aNode.appendChild(annotation);
            } else {
                aNode.insertBefore(annotation, firstChildOfCurrentNode);
            }

            // All Done.
            docElement.appendChild(xsdDocumentation);
        }
    }

    /**
     * Retrieves the XPath for the supplied Node within its document.
     *
     * @param aNode The DOM Node for which the XPath should be retrieved.
     * @return The XPath to the supplied DOM Node.
     */
    public static String getXPathFor (final Node aNode)
    {

        List<String> nodeNameList = new ArrayList<String>();

        for (Node current = aNode; current != null; current = current.getParentNode()) {

            final String currentNodeName = current.getNodeName();
            final String nameAttribute = getNameAttribute(current);

            if (currentNodeName.toLowerCase().endsWith("enumeration")) {

                // We should print the "value" attribute here.
                nodeNameList.add(currentNodeName + "[@value='" + getValueAttribute(current) + "']");

            } else if (nameAttribute == null) {

                // Just emit the node's name.
                nodeNameList.add(current.getNodeName());

            } else {

                // We should print the "name" attribute here.
                nodeNameList.add(current.getNodeName() + "[@name='" + nameAttribute + "']");
            }
        }

        StringBuilder builder = new StringBuilder();
        for (ListIterator<String> it = nodeNameList.listIterator(nodeNameList.size()); it
                .hasPrevious();) {
            builder.append(it.previous());
            if (it.hasPrevious()) {
                builder.append("/");
            }
        }

        return builder.toString();
    }

    /**
     * Retrieves the ClassLocation for the supplied aNode.
     *
     * @param aNode          A non-null DOM Node.
     * @param classLocations The set of known ClassLocations, extracted from the JavaDocs.
     * @return the ClassLocation matching the supplied Node
     */
    public static ClassLocation getClassLocation (final Node aNode,
                                                  final Set<ClassLocation> classLocations)
    {

        if (aNode != null) {

            // The LocalName of the supplied DOM Node should be either "complexType" or "simpleType".
            final String nodeLocalName = aNode.getLocalName();
            final boolean acceptableType = "complexType".equalsIgnoreCase(nodeLocalName)
                                                   || "simpleType".equalsIgnoreCase(nodeLocalName);

            if (acceptableType) {

                final String nodeClassName = getNameAttribute(aNode);
                for (ClassLocation current : classLocations) {

                    // TODO: Ensure that the namespace of the supplied aNode matches the expected namespace.
                    // Issue #25: Handle XML Type renaming.
                    final String effectiveClassName = current.getAnnotationRenamedTo() == null
                            ? current.getClassName()
                            : current.getAnnotationRenamedTo();
                    if (effectiveClassName.equalsIgnoreCase(nodeClassName)) {
                        return current;
                    }
                }
            }
        }

        // Nothing found
        return null;
    }

    /**
     * Finds the MethodLocation within the given Set, which corresponds to the supplied DOM Node.
     *
     * @param aNode           A DOM Node.
     * @param methodLocations The Set of all found/known MethodLocation instances.
     * @return The MethodLocation matching the supplied Node - or {@code null} if no match was
     *         found.
     */
    public static MethodLocation getMethodLocation (final Node aNode,
                                                    final Set<MethodLocation> methodLocations)
    {

        MethodLocation toReturn = null;

        if (aNode != null && CLASS_FIELD_METHOD_ELEMENT_NAMES.contains(aNode.getLocalName()
                .toLowerCase())) {

            final MethodLocation validLocation = getFieldOrMethodLocationIfValid(
                    aNode, getContainingClassOrNull(aNode), methodLocations);

            // The MethodLocation should represent a normal getter; no arguments should be present.
            if (validLocation != null
                        && MethodLocation.NO_PARAMETERS.equalsIgnoreCase(validLocation
                            .getParametersAsString())) {
                toReturn = validLocation;
            }
        }

        // All done.
        return toReturn;
    }

    /**
     * Retrieves a FieldLocation from the supplied Set, provided that the FieldLocation matches the
     * supplied Node.
     *
     * @param aNode          The non-null Node.
     * @param fieldLocations The Set of known/found FieldLocation instances.
     * @return The FieldLocation corresponding to the supplied DOM Node.
     */
    public static FieldLocation getFieldLocation (final Node aNode,
                                                  final Set<FieldLocation> fieldLocations)
    {

        FieldLocation toReturn = null;

        if (aNode != null) {

            if (CLASS_FIELD_METHOD_ELEMENT_NAMES.contains(aNode.getLocalName().toLowerCase())) {

                // This is a ComplexType which correspond to a Java class.
                toReturn = getFieldOrMethodLocationIfValid(aNode, getContainingClassOrNull(aNode),
                                                           fieldLocations);
            } else if (ENUMERATION_FIELD_METHOD_ELEMENT_NAMES.contains(aNode.getLocalName()
                    .toLowerCase())) {

                // This is a SimpleType which correspond to a Java enum.
                toReturn = getFieldOrMethodLocationIfValid(aNode, getContainingClassOrNull(aNode),
                                                           fieldLocations);
            }
        }

        // All done.
        return toReturn;
    }

    /**
     * Retrieves a FieldLocation or MethodLocation from the supplied Set of Field- or
     * MethodLocations, provided that
     * the supplied Node has the given containing Node corresponding to a Class or an Enum.
     *
     * @param aNode               A non-null DOM Node.
     * @param containingClassNode A Non-null DOM Node corresponding to a Class or Enum.
     * @param locations           A Set containing known/found Field- and MethodLocations.
     * @param <T>                 The FieldLocation type.
     * @return The Matching Field- or MethodLocation.
     */
    @SuppressWarnings("unchecked")
    public static <T extends FieldLocation> T getFieldOrMethodLocationIfValid (
            final Node aNode,
            final Node containingClassNode,
            final Set<? extends FieldLocation> locations)
    {

        T toReturn = null;

        if (containingClassNode != null) {

            // Do we have a FieldLocation corresponding to the supplied Node?
            for (FieldLocation current : locations) {

                // Validate that the field and class names match the FieldLocation's corresponding values,
                // minding that annotations such as XmlType, XmlElement and XmlAttribute may override the
                // reflective Class, Field and Method names.
                //
                // Note that we cannot match package names here, as the generated XSD does not contain package
                // information directly. Instead, we must get the Namespace for the generated Class, and compare
                // it to the effective Namespace of the current Node.
                //
                // However, this is a computational-expensive operation, implying we would rather
                // do it at processing time when the number of nodes are (considerably?) reduced.
                // Issue #25: Handle XML Type renaming.
                final String fieldName = current.getAnnotationRenamedTo() == null
                        ? current.getMemberName()
                        : current.getAnnotationRenamedTo();
                final String className = current.getClassName();

                try {

                    //
                    // Fields in XML enums are rendered on the form
                    // <xs:enumeration value="LACTO_VEGETARIAN"/>, implying that
                    // we must retrieve the 'value' attribute's value.
                    //
                    // Fields in XML classes are rendered on the form
                    // <xsd:element name="Line1" type="xsd:string"/>, implying that
                    // we must retrieve the 'name' attribute's value.
                    //
//                    final String attributeValue = getNameAttribute(aNode) == null
//                            ? getValueAttribute(aNode)
//                            : getNameAttribute(aNode);
                    // HB: Added retrieval of ref attribute
                    final String attributeValue = getNameAttribute(aNode) != null
                            ? getNameAttribute(aNode)
                            : (getRefAttribute(aNode) != null
                            ? getRefAttribute(aNode)
                            : getValueAttribute(aNode));
                    if (fieldName.equalsIgnoreCase(attributeValue)
                                && className.equalsIgnoreCase(getNameAttribute(
                                    containingClassNode))) {

                        toReturn = (T) current;
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(
                            "Could not acquire FieldLocation for fieldName ["
                                    + fieldName + "] and className [" + className + "]", e);
                }
            }
        }

        // All done.
        return toReturn;
    }

    /**
     * Processes the supplied DOM Node, inserting XML Documentation annotations if applicable.
     *
     * @param aNode          The DOM Node to process.
     * @param classJavaDocs  A Map relating {@link ClassLocation}s to {@link JavaDocData}.
     * @param fieldJavaDocs  A Map relating {@link FieldLocation}s to {@link JavaDocData}.
     * @param methodJavaDocs A Map relating {@link MethodLocation}s to {@link JavaDocData}.
     * @param renderer       A non-null {@link JavaDocRenderer}.
     */
    public static void insertXmlDocumentationAnnotationsFor (
            final Node aNode,
            final SortedMap<ClassLocation, JavaDocData> classJavaDocs,
            final SortedMap<FieldLocation, JavaDocData> fieldJavaDocs,
            final SortedMap<MethodLocation, JavaDocData> methodJavaDocs,
            final JavaDocRenderer renderer)
    {

        JavaDocData javaDocData = null;
        SortableLocation location = null;

        // Insert the documentation annotation into the current Node.
        final ClassLocation classLocation = getClassLocation(aNode, classJavaDocs.keySet());
        if (classLocation != null) {
            javaDocData = classJavaDocs.get(classLocation);
            location = classLocation;
        } else {

            final FieldLocation fieldLocation = getFieldLocation(aNode, fieldJavaDocs
                                                                 .keySet());
            if (fieldLocation != null) {
                javaDocData = fieldJavaDocs.get(fieldLocation);
                location = fieldLocation;
            } else {

                final MethodLocation methodLocation = getMethodLocation(aNode,
                                                                        methodJavaDocs
                                                                                .keySet());
                if (methodLocation != null) {
                    javaDocData = methodJavaDocs.get(methodLocation);
                    location = methodLocation;
                }
            }
        }

        // We should have a JavaDocData here.
        if (javaDocData == null) {

            final String nodeName = aNode.getNodeName();
            String humanReadableName = getNameAttribute(aNode);

            if (humanReadableName == null && nodeName.toLowerCase().endsWith("enumeration")) {
                humanReadableName = "enumeration#" + getValueAttribute(aNode);
            }

            throw new IllegalStateException("Could not find JavaDocData for XSD node ["
                                                    + humanReadableName + "] with XPath ["
                                                    + getXPathFor(aNode) + "]");
        }

        // Add the XML documentation annotation.
        final String processedJavaDoc = renderer.render(javaDocData, location).trim();
        addXmlDocumentAnnotationTo(aNode, processedJavaDoc);
    }

    //
    // Private helpers
    //
    private static Node getContainingClassOrNull (final Node aNode)
    {

        for (Node current = aNode.getParentNode(); current != null; current = current
                .getParentNode()) {

            final String localName = current.getLocalName();
            final boolean foundClassMatch = "complexType".equalsIgnoreCase(localName)
                                                    || "simpleType".equalsIgnoreCase(localName);

            if (foundClassMatch) {
                return current;
            }
        }

        // No parent Node found.
        return null;
    }

    private static String getNamedAttribute (final Node aNode,
                                             final String attributeName)
    {

        // Fail fast
        if (aNode == null) {
            return null;
        }

        final NamedNodeMap attributes = aNode.getAttributes();
        if (attributes != null) {

            final Node nameNode = attributes.getNamedItem(attributeName);
            if (nameNode != null) {
                return nameNode.getNodeValue().trim();
            }
        }

        // Not found.
        return null;
    }
}
