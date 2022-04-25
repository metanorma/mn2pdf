<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
											xmlns:fo="http://www.w3.org/1999/XSL/Format" 
											xmlns:iso="https://www.metanorma.org/ns/iso" 
											xmlns:mathml="http://www.w3.org/1998/Math/MathML" 
											xmlns:xalan="http://xml.apache.org/xalan" 
											xmlns:fox="http://xmlgraphics.apache.org/fop/extensions" 
											xmlns:pdf="http://xmlgraphics.apache.org/fop/extensions/pdf"
											xmlns:xlink="http://www.w3.org/1999/xlink"
											xmlns:java="http://xml.apache.org/xalan/java"
											exclude-result-prefixes="java"
											version="1.0">

	<xsl:output method="xml" encoding="UTF-8" indent="no"/>
	
	<xsl:param name="if_xml_file"/>
	
	<!-- Review text coordinates calculation based on values from Apache FOP Intermediate Format -->
	
	<xsl:template match="/">
		<xsl:variable name="if_xml" select="document($if_xml_file)"/>
		<annotations>
		<xsl:for-each select="//*[local-name() = 'review']">
			<xsl:variable name="id_from" select="@from"/>
			<xsl:variable name="id_to" select="@to"/>
		
			<annotation>
				<xsl:copy-of select="@*" />
				<data><xsl:copy-of select="node()"/></data>
				
				<xsl:variable name="element_from_" select="$if_xml//*[local-name() = 'id'][@name = $id_from]/following-sibling::*[local-name() = 'text'][1]"/>
				<xsl:variable name="element_from" select="xalan:nodeset($element_from_)"/>
				
				
				<page><xsl:value-of select="count($element_from/ancestor::*[local-name() = 'page']/preceding-sibling::*[local-name() = 'page']) + 1"/></page>
				
				<xsl:variable name="viewport_from_">
					<xsl:apply-templates select="$element_from/ancestor::*[local-name() = 'viewport'][1]"/>
				</xsl:variable>
				<xsl:variable name="viewport_from" select="xalan:nodeset($viewport_from_)"/>
				
				<text>
					<xsl:copy-of select="$element_from/@*"/>
					<xsl:attribute name="x"><xsl:value-of select="$viewport_from/viewport/x + $element_from/@x"/></xsl:attribute>
					<xsl:attribute name="y"><xsl:value-of select="$viewport_from/viewport/y + $element_from/@y"/></xsl:attribute>
					<xsl:copy-of select="$element_from/node()"/>
				</text>
				
				<xsl:if test="$id_to != ''">
					<xsl:for-each select="$element_from/following-sibling::*[local-name() = 'text']">
						
						<xsl:variable name="viewport_to_">
							<xsl:apply-templates select="ancestor::*[local-name() = 'viewport'][1]"/>
						</xsl:variable>
						<xsl:variable name="viewport_to" select="xalan:nodeset($viewport_to_)"/>
						
						<text>
							<xsl:copy-of select="@*"/>
							<xsl:attribute name="x"><xsl:value-of select="$viewport_to/viewport/x + @x"/></xsl:attribute>
							<xsl:attribute name="y"><xsl:value-of select="$viewport_to/viewport/y + @y"/></xsl:attribute>
							<xsl:copy-of select="node()"/>
						</text>
						
					</xsl:for-each>
					
					
				</xsl:if>
				
			</annotation>
		</xsl:for-each>
		</annotations>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'viewport']">
		<xsl:param name="x" select="0"/>
		<xsl:param name="y" select="0"/>
		
		<xsl:choose>
			<xsl:when test="starts-with(@transform, 'translate')">
			
				<xsl:variable name="translate_value" select="substring-before(substring-after(@transform, 'translate('), ')')"/>
				
				<xsl:variable name="value_x">
					<xsl:choose>
						<xsl:when test="contains($translate_value, ',')"><xsl:value-of select="substring-before($translate_value, ',')"/></xsl:when> <!-- Example: transform="translate(70866,36000)" -->
						<xsl:otherwise><xsl:value-of select="$translate_value"/></xsl:otherwise> <!-- Example: transform="translate(70866)" -->
					</xsl:choose>
				</xsl:variable>
				
				<xsl:variable name="value_y">
					<xsl:choose>
						<xsl:when test="contains($translate_value, ',')"><xsl:value-of select="substring-after($translate_value, ',')"/></xsl:when> <!-- Example: transform="translate(70866,36000)" -->
						<xsl:otherwise>0</xsl:otherwise> <!-- Example: transform="translate(70866)" -->
					</xsl:choose>
				</xsl:variable>
				
				<xsl:variable name="new_x" select="$x + number($value_x)"/>
				<xsl:variable name="new_y" select="$y + number($value_y)"/>
				
				<xsl:choose>
					<xsl:when test="ancestor::*[local-name = 'viewport']">
						<xsl:apply-templates select="ancestor::*[local-name = 'viewport'][1]">
							<xsl:with-param name="x" select="$new_x"/>
							<xsl:with-param name="y" select="$new_y"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<viewport>
							<x><xsl:value-of select="$new_x"/></x>
							<y><xsl:value-of select="$new_y"/></y>
						</viewport>
					</xsl:otherwise>
				</xsl:choose>

			</xsl:when>
			<xsl:otherwise>
			
				<xsl:choose>
					<xsl:when test="ancestor::*[local-name = 'viewport']">
						<xsl:apply-templates select="ancestor::*[local-name = 'viewport'][1]">
							<xsl:with-param name="x" select="$x"/>
							<xsl:with-param name="y" select="$y"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<viewport>
							<x><xsl:value-of select="$x"/></x>
							<y><xsl:value-of select="$y"/></y>
						</viewport>
					</xsl:otherwise>
				</xsl:choose>
					
			</xsl:otherwise>
		</xsl:choose>
		
		
	</xsl:template>
	
	
</xsl:stylesheet>
