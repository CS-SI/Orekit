<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" 
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://www.w3.org/1999/xhtml">
<xsl:output
    method="text"
    encoding="UTF-8"/>

<xsl:template match="/">
<xsl:apply-templates select="document/body/release[1]"/>
</xsl:template>

<xsl:template match="document/body/release">
<xsl:text>Version </xsl:text><xsl:value-of select="@version"/><xsl:text> is a patch release of Orekit. The main changes are:
</xsl:text>
<xsl:apply-templates select="action"/>
<xsl:text>
</xsl:text>
</xsl:template>

<xsl:template match="document/body/release/action">
* <xsl:value-of select="normalize-space()"/>
</xsl:template>

</xsl:stylesheet>