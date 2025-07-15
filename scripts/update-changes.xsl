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
  <!-- the release-version, release-date and release-description parameters
         must be passed as stringparam when calling xsltproc -->
  <xsl:template match="c:document/c:body/c:release">
    <!-- we use <xsl:if> to work around XSLT 1.0 limitation that forbids use of variables in match clauses -->
    <xsl:if test="@version=$release-version">
      <!-- this is the version we want to update -->
      <xsl:copy>
        <xsl:attribute name="version"><xsl:value-of select="$release-version"/></xsl:attribute>
        <xsl:attribute name="date"><xsl:value-of select="$release-date"/></xsl:attribute>
        <xsl:attribute name="description"><xsl:value-of select="$release-description"/></xsl:attribute>
        <xsl:apply-templates/>
      </xsl:copy>
    </xsl:if>
    <xsl:if test="not(@version=$release-version)">
      <!-- this is another version we want to keep as is -->
      <xsl:copy>
        <xsl:apply-templates select="node()|@*"/>
      </xsl:copy>
    </xsl:if>
  </xsl:template>

</xsl:stylesheet>
