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
	
	<xsl:variable name="if_xml" select="document($if_xml_file)"/>
	
	<xsl:variable name="if_xml_flatten_">
		<xsl:apply-templates select="document($if_xml_file)" mode="if_flat"/>
	</xsl:variable>
	<xsl:variable name="if_xml_flatten" select="xalan:nodeset($if_xml_flatten_)"/>
	
	<!-- ===================================================================== -->
	<!-- flatten Apache FOP intermediate format: only 'text' and 'id' elements -->
	<!-- ===================================================================== -->
	<xsl:template match="/" mode="if_flat">
		<xsl:copy>
			<xsl:apply-templates mode="if_flat"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'text']" mode="if_flat">
		<xsl:element name="text">
			<xsl:copy-of select="@*"/>
			
			<xsl:variable name="viewport_">
				<xsl:apply-templates select="ancestor::*[local-name() = 'viewport'][1]"/>
			</xsl:variable>
			<xsl:variable name="viewport" select="xalan:nodeset($viewport_)"/>
			
			<xsl:attribute name="x"><xsl:value-of select="($viewport/viewport/x + @x) div 1000"/></xsl:attribute>
			<xsl:attribute name="y"><xsl:value-of select="($viewport/viewport/y + @y) div 1000"/></xsl:attribute>
			<xsl:copy-of select="node()"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'id']" mode="if_flat">
		<xsl:element name="id">
			<xsl:copy-of select="@*"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'page']" mode="if_flat">
		<xsl:element name="page">
			<xsl:copy-of select="@*"/>
		</xsl:element>
		<xsl:apply-templates mode="if_flat"/>
	</xsl:template>
	
	<!-- remove text from header/footer -->
	<!-- Example: <viewport width="488976" height="13200" region-type="Footer"> -->
	<xsl:template match="*[local-name() = 'viewport'][@region-type = 'Footer' or @region-type = 'Header']" mode="if_flat" priority="2"/>
	
	
	<xsl:template match="*[local-name() != 'text' and local-name() != 'id' and local-name() != 'page']" mode="if_flat">
		<xsl:apply-templates mode="if_flat"/>
	</xsl:template>
	<xsl:template match="*[local-name() != 'text']/text()" mode="if_flat"/>
	
	<xsl:template match="@*|node()" mode="if_flat">
		<xsl:copy>
				<xsl:apply-templates select="@*|node()" mode="if_flat"/>
		</xsl:copy>
	</xsl:template>
	<!-- ===================================================================== -->
	<!-- flatten Apache FOP intermediate format: only 'text' and 'id' elements -->
	<!-- ===================================================================== -->
	
	
	<!-- Review text coordinates calculation based on values from Apache FOP Intermediate Format -->
	
	<!-- Output xml:
		<annotations>
			<annotation id="_" reviewer="ISO" date="2017-01-01T00:00:00Z" from="foreword" to="foreword">
				<data>
					<p xmlns="https://www.metanorma.org/ns/iso" id="_">A Foreword shall appear in each document. The generic text is shown here. It does not contain requirements, recommendations or permissions.</p>
					<p xmlns="https://www.metanorma.org/ns/iso" id="_">For further information on the Foreword, see <strong>ISO/IEC Directives, Part 2, 2016, Clause 12.</strong>
					</p>
				</data>
				<page>5</page>
				<text x="70.866" y="146.218" foi:struct-ref="125" xmlns:foi="http://xmlgraphics.apache.org/fop/internal"> </text>
			</annotation>
			...
		</annotations>
	-->
	
	<xsl:template match="/">
		
		<xsl:element name="annotations">
		
			<xsl:for-each select="//*[local-name() = 'review']">
				<xsl:variable name="id_from" select="@from"/>
				<xsl:variable name="id_to" select="@to"/>
			
				<xsl:element name="annotation">
					<xsl:copy-of select="@*" />
					
					<xsl:element name="data">
						<xsl:copy-of select="node()"/>
					</xsl:element>
					
					<xsl:variable name="element_from_" select="$if_xml_flatten//*[local-name() = 'id'][@name = $id_from]/following-sibling::*[local-name() = 'text'][1]"/>
					<xsl:variable name="element_from" select="xalan:nodeset($element_from_)"/>
					
					
					<xsl:element name="page">
						<xsl:value-of select="count($element_from/preceding-sibling::*[local-name() = 'page'])"/>
					</xsl:element>
					
					
					<xsl:element name="text">
						<xsl:copy-of select="$element_from/@*"/>
						<xsl:copy-of select="$element_from/node()"/>
					</xsl:element>
					
					<xsl:if test="$id_to != ''">
						<xsl:for-each select="$element_from/following-sibling::*[local-name() = 'text'][not(preceding-sibling::*[local-name() = 'id'][@name = $id_to])]">
						
							<xsl:element name="text">
								<xsl:copy-of select="@*"/>
								<xsl:copy-of select="node()"/>
							</xsl:element>
							
						</xsl:for-each>
					</xsl:if>
					
				</xsl:element>
			</xsl:for-each>
		</xsl:element>
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
