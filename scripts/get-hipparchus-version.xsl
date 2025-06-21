<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml" omit-xml-declaration="yes" />
  <xsl:template match="node()|@*">
      <xsl:apply-templates select="node()|@*"/>
  </xsl:template>
  <xsl:template match="/*[local-name()='project']/*[local-name()='properties']">
      <xsl:value-of select="*[local-name()='orekit.hipparchus.version']/text()"></xsl:value-of>
  </xsl:template>
</xsl:stylesheet>
