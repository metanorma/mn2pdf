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
	
	<xsl:param name="if_xml"/>
	
	<xsl:variable name="if_xml_flatten_">
		<!-- for command line debug: <xsl:apply-templates select="document($if_xml)" mode="if_flat"/> -->
		<xsl:apply-templates select="xalan:nodeset($if_xml)" mode="if_flat"/>
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
	<!-- ===================================================================== -->
	<!-- flatten Apache FOP intermediate format: only 'text' and 'id' elements -->
	<!-- ===================================================================== -->
	
	
	<!-- Review text coordinates calculation based on values from Apache FOP Intermediate Format -->
	
	<!-- Output xml:
		<annotations>
			<annotation id="_" reviewer="ISO" date="2017-01-01T00:00:00Z" from="foreword" to="foreword">
				<data>
					<body xmlns="http://www.w3.org/1999/xhtml">
						<p dir="ltr">A Foreword shall appear in each document. The generic text is shown here. It does not contain requirements, recommendations or permissions.</p>
						<p dir="ltr">For further information on the Foreword, see <span style="font-weight:bold">ISO/IEC Directives</span>, Part 2, 2016, Clause 12..</p>
					</body>
				</data>
				<page>5</page>
				<text x="70.866" y="146.218" foi:struct-ref="125" xmlns:foi="http://xmlgraphics.apache.org/fop/internal"> </text>
			</annotation>
			...
		</annotations>
	-->
	
	<xsl:variable name="hair_space">&#x200A;</xsl:variable>

	<xsl:template match="/">
		
		<xsl:element name="annotations">
		
			<xsl:for-each select="//*[local-name() = 'review']">
				<xsl:variable name="id_from">
					<xsl:choose>
						<xsl:when test="normalize-space(@from) != ''"><xsl:value-of select="@from"/></xsl:when>
						<xsl:otherwise><xsl:value-of select="@id"/></xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<xsl:variable name="id_to" select="@to"/>
			
				<xsl:element name="annotation">
					<xsl:copy-of select="@*" />
					
					<xsl:element name="data">
						<xsl:apply-templates mode="pdf_richtext"/>
						<!-- <xsl:copy-of select="node()"/> -->
					</xsl:element>
					
					<xsl:variable name="texts_">
					
						<xsl:variable name="element_from_" select="$if_xml_flatten//*[local-name() = 'id'][@name = $id_from]/following-sibling::*[local-name() = 'text'][1]"/>
						<xsl:variable name="element_from" select="xalan:nodeset($element_from_)"/>
						
						<xsl:variable name="page" select="count($element_from/preceding-sibling::*[local-name() = 'page'])"/>
						
						<xsl:element name="text">
							<xsl:copy-of select="$element_from/@*"/>
							<xsl:attribute name="page"><xsl:value-of select="$page"/></xsl:attribute>
							<xsl:copy-of select="$element_from/node()"/>
						</xsl:element>
						
						<xsl:if test="$id_to != ''">
							<xsl:for-each select="$element_from/following-sibling::*[local-name() = 'text'][not(preceding-sibling::*[local-name() = 'id'][@name = $id_to])]">
								<xsl:variable name="page_to" select="count(preceding-sibling::*[local-name() = 'page'])"/>
								
								<xsl:element name="text">
									<xsl:copy-of select="@*"/>
									<xsl:attribute name="page"><xsl:value-of select="$page_to"/></xsl:attribute>
									<xsl:copy-of select="node()"/>
								</xsl:element>
								
							</xsl:for-each>
						</xsl:if>
					</xsl:variable>
					
					<xsl:variable name="texts" select="xalan:nodeset($texts_)"/>
					
					<xsl:choose>
						<xsl:when test="count($texts//text) &gt; 1">
							<xsl:for-each select="xalan:nodeset($texts)//text[normalize-space() != $hair_space]">
								<xsl:copy-of select="."/>
							</xsl:for-each>
						</xsl:when>
						<xsl:otherwise>
							<xsl:copy-of select="$texts"/>
						</xsl:otherwise>
					</xsl:choose>
					
				</xsl:element>
			</xsl:for-each>
		</xsl:element>
	</xsl:template>

	<!-- ==================== -->
	<!-- PDF rich text format -->
	<!-- ==================== -->
	<xsl:variable name="namespace_xhtml">http://www.w3.org/1999/xhtml</xsl:variable>
	<xsl:template match="@*|*" mode="pdf_richtext">
		<xsl:apply-templates select="@*|node()" mode="pdf_richtext"/>
	</xsl:template>
	<xsl:template match="text()" mode="pdf_richtext">
		<xsl:value-of select="."/>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'strong']" mode="pdf_richtext">
		<xsl:element name="span" namespace="{$namespace_xhtml}">
			<xsl:attribute name="style">font-weight:bold</xsl:attribute>
			<xsl:apply-templates mode="pdf_richtext"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'em']" mode="pdf_richtext">
		<xsl:element name="span" namespace="{$namespace_xhtml}">
			<xsl:attribute name="style">font-style:italic</xsl:attribute>
			<xsl:apply-templates mode="pdf_richtext"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'underline']" mode="pdf_richtext">
		<xsl:element name="span" namespace="{$namespace_xhtml}">
			<xsl:attribute name="style">text-decoration:underline</xsl:attribute>
			<xsl:apply-templates mode="pdf_richtext"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'p']" mode="pdf_richtext">
		<xsl:element name="{local-name()}" namespace="{$namespace_xhtml}">
			<xsl:attribute name="dir">ltr</xsl:attribute>
			<xsl:apply-templates mode="pdf_richtext"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'sub' or local-name() = 'sup']" mode="pdf_richtext">
		<xsl:element name="{local-name()}" namespace="{$namespace_xhtml}">
			<xsl:apply-templates mode="pdf_richtext"/>
		</xsl:element>
	</xsl:template>
	
	<!-- ==================== -->
	<!-- END: PDF rich text format -->
	<!-- ==================== -->
	
</xsl:stylesheet>
