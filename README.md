# Tools

Helpers for some of the standard JDK APIs

## XML

XML is a wrapper for org.w3c.dom.Node. It includes methods for reading documents,
building them from scratch, and searching them. Implementation is based on the javax.xml package.

### Usage

```java
// Reading from an InputStream
XML myDoc = node(new FileInputStream("my-file.xml"));

// Building a document
XML doc =
    node("foo:bar", "http://foo.com/bar/2001",
        node("person",
            node("name").text("John"),
            node("age").text("40")));

// Finding by tagName
doc.find("person").find("age").text();                    // -> 40

// Finding by xpath
doc.xpath("//person/age/text()", XPathConstants.STRING);  // -> 40

// Printing
doc.writeTo(System.out);
```

The last line outputs the following:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<foo:bar xmlns:foo="http://foo.com/bar/2001">
  <person>
    <name>John</name>
    <age>40</age>
  </person>
</foo:bar>
```
