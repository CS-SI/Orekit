<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="text"/>
  <xsl:template match="node()|@*">
      <xsl:apply-templates select="node()|@*"/>
  </xsl:template>
  <xsl:template match="/*[local-name()='settings']/*[local-name()='servers']/*[local-name()='server']/*[local-name()='id'][text()='central']">
      <xsl:value-of select="../*[local-name()='username']/text()"/>
  </xsl:template>
</xsl:stylesheet>
