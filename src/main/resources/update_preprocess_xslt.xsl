<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

	<xsl:output version="1.0" method="xml" encoding="UTF-8" indent="no"/>
	
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
	
<!-- 	<xsl:template match="xsl:stylesheet">
		<xsl:apply-templates/>
	</xsl:template> -->
	
	<xsl:template match="xsl:stylesheet/*" />
	
	<xsl:template match="xsl:stylesheet/text()"/>
	
	<xsl:template match="xsl:stylesheet/xsl:template | xsl:stylesheet/xsl:variable">
		<xsl:text>&#xa;</xsl:text>
		<xsl:copy>
			<xsl:if test="local-name() = 'template'">
				<xsl:attribute name="priority">5</xsl:attribute>
				<xsl:attribute name="mode">update_xml_step1</xsl:attribute>
			</xsl:if>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
		<xsl:text>&#xa;&#xa;</xsl:text>
	</xsl:template>

	<xsl:template match="*">
		<xsl:element name="{local-name()}">
			<xsl:apply-templates select="@*"/>
			<xsl:apply-templates select="node()"/>
		</xsl:element>
	</xsl:template>

	<xsl:template match="xsl:*">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>

	<xsl:template match="@*[local-name() = 'match'][not(contains(., 'local-name()'))]">
		<xsl:attribute name="{local-name()}">
			<xsl:call-template name="split">
				<xsl:with-param name="pText" select="."/>
			</xsl:call-template>
		</xsl:attribute>
	</xsl:template>

	<!-- split string by separator -->
	<xsl:template name="split">
		<xsl:param name="pText" select="."/>
		<xsl:param name="sep" select="'/'"/>
		
		<xsl:if test="string-length($pText) &gt; 0">
			<item>
				<item>*[local-name() = '<xsl:value-of select="substring-before(concat($pText, $sep), $sep)"/>']</item>
			</item>
			<xsl:if test="contains($pText, $sep)"><item><xsl:value-of select="$sep"/></item></xsl:if>
			<xsl:call-template name="split">
				<xsl:with-param name="pText" select="substring-after($pText, $sep)"/>
				<xsl:with-param name="sep" select="$sep"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>
