<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:c="http://maven.apache.org/changes/2.0.0"
                xmlns="http://www.w3.org/1999/xhtml">
<xsl:output
    method="text"
    encoding="UTF-8"/>

<xsl:template match="/">
<xsl:apply-templates select="c:document/c:body/c:release[1]"/>
</xsl:template>

<xsl:template match="c:document/c:body/c:release">
<xsl:text>Orekit version </xsl:text><xsl:value-of select="@version"/><xsl:text>. The main changes are:
</xsl:text>
<xsl:apply-templates select="c:action"/>
<xsl:text>
</xsl:text>
</xsl:template>

<xsl:template match="c:document/c:body/c:release/c:action">
* <xsl:value-of select="normalize-space()"/>
</xsl:template>

</xsl:stylesheet>
