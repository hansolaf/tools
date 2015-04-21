package com.github.hansolaf.tools.xml;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class XML {

    private final Node node;

    private XML(Node node) {
        this.node = node;
    }

    /* Generating */

    /**
     * Wraps any {@link org.w3c.dom.Node}
     */
    public static XML node(Node node) {
        return new XML(node);
    }

    /**
     * Reads an xml document from InputStream
     */
    public static XML node(InputStream is) {
        return node(is, Charset.defaultCharset().name());
    }

    /**
     * Reads an xml document from InputStream using the given charset
     */
    public static XML node(InputStream is, String charset) {
        try {
            Document document = documentBuilder.get().parse(new InputSource(new InputStreamReader(is, charset)));
            return new XML(document.getDocumentElement());
        } catch (SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates an element named tagName, with the given children
     */
    public static XML node(String tagName, XML... children) {
        Document document = documentBuilder.get().newDocument();
        Node root = document.appendChild(document.createElement(tagName));
        return new XML(root).append(children);
    }

    /**
     * Creates an element named tagName and namespaceURI, with the given children
     */
    public static XML node(String tagName, String namespaceURI, XML... children) {
        Document document = documentBuilder.get().newDocument();
        Node root = document.appendChild(document.createElementNS(namespaceURI, tagName));
        return new XML(root).append(children);
    }

    /* Accessors */

    /**
     * Gets the underlying {@link org.w3c.dom.Node}
     */
    public Node node() {
        return node;
    }

    /**
     * Gets the ownerDocument of this node
     */
    public Document doc() {
        return node.getOwnerDocument();
    }

    /**
     * Gets the textContent of this node
     */
    public String text() {
        return node.getTextContent();
    }

    /**
     * Gets the attribute value (or null if it doesn't exist)
     */
    public String attribute(String name) {
        Node attrib = node.getAttributes().getNamedItem(name);
        return attrib == null ? null : attrib.getTextContent();
    }

    /* Mutators */

    /**
     * Adds an attribute
     */
    public XML attribute(String name, String value) {
        ((Element) node).setAttribute(name, value);
        return this;
    }

    /**
     * Adds an attribute with the given namespace
     */
    public XML attribute(String name, String value, String namespaceURI) {
        ((Element) node).setAttributeNS(namespaceURI, name, value);
        return this;
    }

    /**
     * Adds a CDATA section under this node
     */
    public XML cdata(String data) {
        node.appendChild(doc().createCDATASection(data));
        return this;
    }

    /**
     * Sets the textContent of this node
     */
    public XML text(String textContent) {
        node.setTextContent(textContent);
        return this;
    }

    /**
     * Appends children to this node
     */
    public XML append(XML... children) {
        return append(Arrays.asList(children));
    }

    /**
     * Appends children to this node
     */
    public XML append(Iterable<XML> children) {
        for (XML child : children)
            node.appendChild(doc().importNode(child.node(), true));
        return this;
    }

    /* Lookup */

    /**
     * Finds first immediate child of this node with the given tagName
     */
    public XML find(String tagName) {
        return find(tagName, null);
    }

    /**
     * Finds first immediate child of this node with the given tagName and namespaceURI.
     * If namespaceURI is null namespaces are ignored during the search.
     */
    public XML find(String tagName, String namespaceURI) {
        List<XML> matches = findAll(tagName, namespaceURI);
        return matches.isEmpty() ? null : matches.get(0);
    }

    /**
     * Finds all immediate children of this node with the given tagName
     */
    public List<XML> findAll(String tagName) {
        return findAll(tagName, null);
    }

    /**
     * Finds all immediate children of this node with the given tagName and namespaceURI.
     * If namespaceURI is null namespaces are ignored during the search.
     */
    public List<XML> findAll(String tagName, String namespaceURI) {
        NodeList children = node.getChildNodes();
        List<XML> matches = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            boolean namespaceMatch = namespaceURI == null || namespaceURI.equals(child.getNamespaceURI());
            String childname = child.getNamespaceURI() == null ? child.getNodeName() : child.getLocalName();
            if (namespaceMatch && tagName.equals(childname))
                matches.add(new XML(child));
        }
        return matches;
    }

    /**
     * @see com.github.hansolaf.tools.xml.XML#xpath(String, javax.xml.namespace.QName, java.util.Map)
     */
    public <T> T xpath(String xpathExpression, QName expectedType) {
        return xpath(xpathExpression, expectedType, null);
    }

    /**
     * Evaluates the given xpath expression.
     *
     * @param xpathExpression expression to be evaluated
     * @param expectedType expected return type, typically from {@link javax.xml.xpath.XPathConstants}
     * @param namespaceContext prefix -> namespaceURI mappings to be used
     * @return result of evaluating the xpath expression.
     *  NODE-results are wrapped in an XML-node
     *  NODELIST-results are returned as a list of XML-nodes
     */
    @SuppressWarnings("unchecked")
    public <T> T xpath(String xpathExpression, QName expectedType, final Map<String, String> namespaceContext) {
        XPath xpath = xpathfactory.get().newXPath();
        if (namespaceContext != null) {
            xpath.setNamespaceContext(new NamespaceContext() {
                public String getNamespaceURI(String prefix) { return namespaceContext.get(prefix); }
                public String getPrefix(String namespaceURI) { throw new UnsupportedOperationException(); }
                public Iterator getPrefixes(String namespaceURI) { throw new UnsupportedOperationException(); }
            });
        }
        try {
            Object res = xpath.compile(xpathExpression).evaluate(node, expectedType);
            if (res == null)
                return null;
            if (XPathConstants.NODE.equals(expectedType))
                return (T) node((Node) res);
            if (XPathConstants.NODESET.equals(expectedType)) {
                NodeList nodelist = (NodeList) res;
                List<XML> nodes = new ArrayList<>(nodelist.getLength());
                for (int i = 0; i < nodelist.getLength(); i++)
                    nodes.add(node(nodelist.item(i)));
                return (T) nodes;
            }
            return (T) res;
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    /* General stuff */

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof XML && toString().equals(obj.toString());
    }

    @Override
    public String toString() {
        return writeTo(new ByteArrayOutputStream(), false, true).toString();
    }

    /**
     * Writes this document to the stream (with xml-declaration and without indenting)
     */
    public OutputStream writeTo(OutputStream out) {
        return writeTo(out, false, false);
    }

    /**
     * Writes this document to the stream
     */
    public OutputStream writeTo(OutputStream out, boolean omitXmlDeclaration, boolean indent) {
        try {
            Transformer tf = transformer.get();
            tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration ? "yes" : "no");
            tf.setOutputProperty(OutputKeys.INDENT, indent ? "yes" : "no");
            tf.transform(new DOMSource(node), new StreamResult(out));
            return out;
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
    }

    /* ThreadLocal-caches */

    private static final ThreadLocal<DocumentBuilder> documentBuilder = new ThreadLocal<DocumentBuilder>() {
        protected DocumentBuilder initialValue() {
            try {
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                // Disable DTDs and external entities
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                dbf.setXIncludeAware(false);
                dbf.setExpandEntityReferences(false);
                return dbf.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
    };

    private static final ThreadLocal<Transformer> transformer = new ThreadLocal<Transformer>() {
        protected Transformer initialValue() {
            try {
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
                return transformer;
            } catch (TransformerConfigurationException e) {
                throw new RuntimeException(e);
            }
        }
    };

    private static final ThreadLocal<XPathFactory> xpathfactory = new ThreadLocal<XPathFactory>() {
        protected XPathFactory initialValue() {
            return XPathFactory.newInstance();
        }
    };

}
