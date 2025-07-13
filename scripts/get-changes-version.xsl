<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:c="http://maven.apache.org/changes/2.0.0" version="1.0">
  <xsl:output method="text"/>
  <xsl:template match="node()|@*">
      <xsl:apply-templates select="node()|@*"/>
  </xsl:template>
  <xsl:template match="/c:document/c:body/c:release[1]/@version">
      <xsl:value-of select="."/>
  </xsl:template>
</xsl:stylesheet>
