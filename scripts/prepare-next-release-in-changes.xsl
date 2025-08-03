<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:c="http://maven.apache.org/changes/2.0.0" version="1.0">
  <xsl:output method="xml" omit-xml-declaration="yes"/>

  <!-- catch-all template -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!-- specific template for releases -->
  <xsl:template match="c:document/c:body/c:release">
    <!-- we use <xsl:if> to work around XSLT 1.0 limitation that forbids use of variables in match clauses -->
    <!-- the release-version parameter must be passed as stringparam when calling xsltproc -->
    <xsl:if test="@version=$release-version">
      <!-- prepend a new entry before first one -->
      <xsl:element name="release" xmlns="http://maven.apache.org/changes/2.0.0">
        <!-- the release-version parameter must be passed as stringparam when calling xsltproc -->
        <xsl:attribute name="version"><xsl:value-of select="$next-version"/></xsl:attribute>
        <xsl:attribute name="date">TBD</xsl:attribute>
        <xsl:attribute name="description">TBD</xsl:attribute>
      </xsl:element>
    </xsl:if>
    <!-- copy all existing elements -->
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

</xsl:stylesheet>
