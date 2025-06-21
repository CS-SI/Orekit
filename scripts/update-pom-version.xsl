<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" omit-xml-declaration="yes" />

  <!-- catch-all template -->
  <xsl:template match="node()|@*">
    <xsl:copy>
      <xsl:apply-templates select="node()|@*"/>
    </xsl:copy>
  </xsl:template>

  <!-- specific template for version number -->
  <!-- the release-version parameter must be passed as stringparam when calling xsltproc -->
  <xsl:template match="/*[local-name()='project']/*[local-name()='version']/text()">
      <xsl:value-of select="$release-version"/>
  </xsl:template>

</xsl:stylesheet>
