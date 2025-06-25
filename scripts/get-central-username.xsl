<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:s="http://maven.apache.org/SETTINGS/1.1.0" version="1.0">
  <xsl:output method="text"/>
  <xsl:template match="node()|@*">
      <xsl:apply-templates select="node()|@*"/>
  </xsl:template>
  <xsl:template match="/s:settings/s:servers/s:server/s:id[text()='central']">
      <xsl:value-of select="../s:username/text()"/>
  </xsl:template>
</xsl:stylesheet>
