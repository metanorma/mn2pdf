<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns="http://xmlgraphics.apache.org/fop/intermediate" 
	xmlns:im="http://xmlgraphics.apache.org/fop/intermediate" 
	xmlns:fo="http://www.w3.org/1999/XSL/Format" 
	xmlns:foi="http://xmlgraphics.apache.org/fop/internal"   
	xmlns:fox="http://xmlgraphics.apache.org/fop/extensions"
	xmlns:math="http://www.w3.org/1998/Math/MathML"
	xmlns:xalan="http://xml.apache.org/xalan"  
	xmlns:jeuclid="http://jeuclid.sf.net/ns/ext"
	xmlns:java="http://xml.apache.org/xalan/java"
	exclude-result-prefixes="im xalan fo fox math java"
	version="1.0">

	<xsl:output version="1.0" method="xml" encoding="UTF-8" indent="yes"/>
	
	<xsl:template match="@*|node()">
		<xsl:copy>
				<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:variable name="x_shift">-30000</xsl:variable>
	
	
	<xsl:template match="im:page/im:content/im:viewport[not(@region-type = 'Header' or @region-type = 'Footer')][.//im:text[@x = '0']]" priority="2">
		<xsl:copy-of select="."/>
	
		<xsl:copy>
			<xsl:copy-of select="@*"/>
		
		
			<xsl:variable name="texts_">
				<xsl:for-each select=".//im:text[@x = '0'][not(preceding-sibling::im:font[1][number(@size) &lt;= 8000][preceding-sibling::*[1][self::im:line] or ancestor::im:viewport[1]/preceding-sibling::im:line])]">
					<xsl:element name="text" namespace="http://xmlgraphics.apache.org/fop/intermediate">
						<xsl:attribute name="x"><xsl:value-of select="$x_shift"/></xsl:attribute>
						<xsl:attribute name="y"><xsl:value-of select="@y"/></xsl:attribute>
						
						<xsl:attribute name="count"><xsl:value-of select="count(ancestor::im:viewport)"/></xsl:attribute>
						
						<xsl:if test="count(ancestor::im:viewport) &gt; 1">
							<xsl:variable name="viewport_">
								<xsl:apply-templates select="ancestor::*[local-name() = 'viewport'][1]" mode="shift_y"/>
							</xsl:variable>
							<xsl:variable name="viewport" select="xalan:nodeset($viewport_)"/>
							
							<xsl:attribute name="y"><xsl:value-of select="($viewport/*[local-name() = 'viewport']/*[local-name() = 'y'] + @y)"/></xsl:attribute>
							
							<!-- <xsl:copy-of select="$viewport"/> -->
							
						</xsl:if>
						
					</xsl:element>
				</xsl:for-each>
			</xsl:variable>
		
			<xsl:variable name="texts" select="xalan:nodeset($texts_)"/>
		
			<xsl:if test="count($texts/*) &gt; 0">
				<xsl:element name="font" namespace="http://xmlgraphics.apache.org/fop/intermediate">
					<xsl:attribute name="family">Times New Roman</xsl:attribute>
					<xsl:attribute name="weight">400</xsl:attribute>
					<xsl:attribute name="size">11000</xsl:attribute>
					
					<xsl:for-each select="$texts/*">
					
						<xsl:copy>
							<xsl:copy-of select="@*"/>
							<xsl:number /> <!-- count="im:text[@x = '0']" -->
						
						</xsl:copy>
					</xsl:for-each>
					
					
					
				</xsl:element>
			</xsl:if>
		</xsl:copy>
	
	</xsl:template>
	
	
	<xsl:template match="*[local-name() = 'viewport']" mode="shift_y">
		<xsl:param name="y" select="0"/>
		
		<xsl:choose>
			<xsl:when test="starts-with(@transform, 'translate')">
			
				<xsl:variable name="translate_value" select="substring-before(substring-after(@transform, 'translate('), ')')"/>
				
				<xsl:variable name="value_y">
					<xsl:choose>
						<xsl:when test="contains($translate_value, ',')"><xsl:value-of select="substring-after($translate_value, ',')"/></xsl:when> <!-- Example: transform="translate(70866,36000)" -->
						<xsl:otherwise>0</xsl:otherwise> <!-- Example: transform="translate(70866)" -->
					</xsl:choose>
				</xsl:variable>
				
				<xsl:variable name="new_y" select="$y + number($value_y)"/>
				
				<xsl:choose>
					<xsl:when test="count(ancestor::*[local-name = 'viewport']) &gt; 1">
						<xsl:apply-templates select="ancestor::*[local-name = 'viewport'][1]" mode="shift_y">
							<xsl:with-param name="y" select="$new_y"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<viewport>
							<y><xsl:value-of select="$new_y"/></y>
						</viewport>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="count(ancestor::*[local-name = 'viewport']) &gt; 1">
						<xsl:apply-templates select="ancestor::*[local-name = 'viewport'][1]" mode="shift_y">
							<xsl:with-param name="y" select="$y"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<viewport>
							<y><xsl:value-of select="$y"/></y>
						</viewport>
					</xsl:otherwise>
				</xsl:choose>
					
			</xsl:otherwise>
		</xsl:choose>
		
	</xsl:template>
	
	<xsl:template match="im:viewport2[not(@region-type = 'Header' or @region-type = 'Footer')]/im:text[@x = '0'][not(preceding-sibling::im:font[@size = '7000'][preceding-sibling::*[1][self::im:line]])]">
		<xsl:element name="text" namespace="http://xmlgraphics.apache.org/fop/intermediate">
			<xsl:attribute name="x"><xsl:value-of select="$x_shift"/></xsl:attribute>
			<xsl:attribute name="y"><xsl:value-of select="@y"/></xsl:attribute>
			<xsl:number count="im:text[@x = '0']"/>
		</xsl:element>
		<xsl:copy-of select="."/>
	</xsl:template>

</xsl:stylesheet>