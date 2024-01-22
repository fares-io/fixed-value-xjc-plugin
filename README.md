# Fixed Value XJC Plugin

This plugin provides functionality to generate Java Bean properties defaulted to the values defined by the `xsd:fixed` attribute for XSD elements. **Note:** the fixed value setting for attributes works out-of-the-box in JAXB.

Most of the logic of this plugin is from https://github.com/fbdo/jaxb2-fixed-value which unfortunately is no longer maintained and not compatible with JAXB 3+.

## Usage

The plugin can be used with any JAXB compiler that is capable of registering XJC plugins. The plugin jar needs to be made available to the XJC compiler classpath. In maven this is not the project classpath but the classpath of the plugin that generates code from one or more XML schema.

Example configuration for the JAXB compiler:

```xml
<plugin>
  <groupId>org.jvnet.jaxb</groupId>
  <artifactId>jaxb-maven-plugin</artifactId>
  <configuration>
    <extension>true</extension>
    <plugins>
      <plugin>
        <groupId>io.fares.bind.xjc.plugins</groupId>
        <artifactId>fixed-value-xjc-plugin</artifactId>
        <version>1.0.3</version>
      </plugin>
    </plugins>
    <args>
      <arg>-Xfixed-value</arg>
    </args>
  </configuration>
</plugin>
```

## Default Elements Example

The following schema will default the values of the generated class. It also demonstrates unboxing of primitives with `rank` defined as `xsd:int`.

```xml
<xsd:schema targetNamespace="urn:testfixedelement"
            xmlns:tns="urn:testfixedelement"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            elementFormDefault="qualified">

  <xsd:complexType name="Example">
    <xsd:sequence>
      <xsd:element name="unit" type="tns:UnitOfMeasurement" fixed="lbs"/>
      <xsd:element name="rank" type="xsd:int" fixed="5"/>
      <xsd:element name="list" type="xsd:positiveInteger" fixed="201"/>
    </xsd:sequence>
  </xsd:complexType>

  <xsd:simpleType name="UnitOfMeasurement">
    <xsd:restriction base="xsd:string">
      <xsd:enumeration value="kg"/>
      <xsd:enumeration value="lbs"/>
    </xsd:restriction>
  </xsd:simpleType>

</xsd:schema>
```

will generate

```java
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Product")
public class Product {

  @XmlElement(required = true)
  protected String name;
  @XmlElement(required = true)
  @XmlSchemaType(name = "string")
  protected UnitOfMeasurement unit = UnitOfMeasurement.LBS;
  @XmlElement(required = true)
  @XmlSchemaType(name = "string")
  protected ProductGroup group = ProductGroup.BOOKS;
  @XmlElement(required = true)
  protected String category = "reading";
  protected int rank = 5;
  @XmlElement(required = true)
  @XmlSchemaType(name = "positiveInteger")
  protected BigInteger list;

  ...

}
```
