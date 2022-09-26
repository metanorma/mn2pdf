<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:im="http://xmlgraphics.apache.org/fop/intermediate" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:foi="http://xmlgraphics.apache.org/fop/internal" version="1.0">

	<xsl:output version="1.0" method="xml" encoding="UTF-8" indent="yes"/>
	
	<xsl:template match="/">
		<index>
			<xsl:for-each select="//im:text[preceding-sibling::*[1][local-name() = 'id']][number(text()) = text()]">
				<xsl:variable name="page_num"><xsl:value-of select="."/></xsl:variable>
				<xsl:variable name="id" select="preceding-sibling::*[local-name() = 'id'][1]/@name"/>
				<item id="{$id}"><xsl:value-of select="$page_num"/></item>
			</xsl:for-each>
		</index>
	</xsl:template>

</xsl:stylesheet>