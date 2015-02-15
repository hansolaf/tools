package com.github.hansolaf.tools.xml;

import org.junit.Test;

import javax.xml.xpath.XPathConstants;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static com.github.hansolaf.tools.xml.XML.node;

public class XMLTest {

    private static final String FOO_NS = "http://foobar.com/foo/bar";
    private static final String SEC_NS = "http://security.com/2011/06/";

    XML doc =
        node("foo:document", FOO_NS,
            node("foo:header", FOO_NS,
                node("security:Security", SEC_NS,
                    node("Credentials")
                        .attribute("type", "text")
                        .text("pw01"))),
            node("foo:body", FOO_NS,
                node("request",
                    node("id").text("15"),
                    node("id").text("333"),
                    node("data")
                        .cdata("random string <b>with tags</b>")
                        .attribute("ver", "v1", "some::namespace"))));

    String docString =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<foo:document xmlns:foo=\"http://foobar.com/foo/bar\">\n" +
        "  <foo:header>\n" +
        "    <security:Security xmlns:security=\"http://security.com/2011/06/\">\n" +
        "      <Credentials type=\"text\">pw01</Credentials>\n" +
        "    </security:Security>\n" +
        "  </foo:header>\n" +
        "  <foo:body>\n" +
        "    <request>\n" +
        "      <id>15</id>\n" +
        "      <id>333</id>\n" +
        "      <data xmlns:ns0=\"some::namespace\" ns0:ver=\"v1\"><![CDATA[random string <b>with tags</b>]]></data>\n" +
        "    </request>\n" +
        "  </foo:body>\n" +
        "</foo:document>\n";

    @Test
    public void buildingAndPrintingWorksAsExpected() {
        assertEquals(docString, doc.toString());
    }

    @Test
    public void printingAndBuildingPreservesHashCodeAndEquals() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.writeTo(out);
        XML copy = node(new ByteArrayInputStream(out.toByteArray()));
        assertEquals(doc, copy);
        assertEquals(doc.hashCode(), copy.hashCode());
    }

    @Test
    public void findFindsNodesCorrectly() {
        assertEquals("15", doc.find("body", FOO_NS).find("request").find("id").text());
    }

    @Test
    public void findWithoutNamespacesMatchesRegardlessOfNamespace() {
        assertEquals("15", doc.find("body").find("request").find("id").text());
    }

    @Test
    public void xpathFindsNodesCorrectly() {
        assertEquals("15", doc.xpath("//id/text()", XPathConstants.STRING));
        assertEquals(node("id").text("15"), doc.xpath("//id", XPathConstants.NODE));
    }

    @Test
    public void lookupsOfNonExistingPathsReturnNull() {
        assertNull(doc.find("non-existing"));
        assertNull(doc.find("non-existing", "myns"));
        assertNull(doc.xpath("//non-existing", XPathConstants.NODE));
    }

    @Test
    public void xpathWithNodesetFindsAllMatches() {
        List<XML> matches = doc.xpath("//id", XPathConstants.NODESET);
        assertEquals("15", matches.get(0).text());
        assertEquals("333", matches.get(1).text());
    }

    @Test
    public void xpathWithNamespacesMatchesCorrectly() {
        Map<String, String> nsMapping = new HashMap<String, String>() {{ put("s", FOO_NS); }};
        assertEquals("15", doc.xpath("/s:document/s:body/request/id/text()", XPathConstants.STRING, nsMapping));
        assertNull(doc.xpath("//non-existing", XPathConstants.NODE, nsMapping));
    }

}
