<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" omit-xml-declaration="yes"/>

  <!-- catch-all template -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!-- the release-version parameter must be passed as stringparam when calling xsltproc -->
  <xsl:template match="document/body">
      <xsl:element name="body">
          <xsl:element name="release">
            <xsl:attribute name="version"><xsl:value-of select="$release-version"/></xsl:attribute>
            <xsl:attribute name="date">TBD</xsl:attribute>
            <xsl:attribute name="description">TBD</xsl:attribute>
          </xsl:element>
          <xsl:apply-templates/>
      </xsl:element>
  </xsl:template>

</xsl:stylesheet>
