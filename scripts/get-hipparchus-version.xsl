<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:p="http://maven.apache.org/POM/4.0.0" version="1.0">
  <xsl:output method="xml" omit-xml-declaration="yes" />
  <xsl:template match="node()|@*">
      <xsl:apply-templates select="node()|@*"/>
  </xsl:template>
  <xsl:template match="/p:project/p:properties">
      <xsl:value-of select="p:orekit.hipparchus.version/text()"/>
  </xsl:template>
</xsl:stylesheet>
