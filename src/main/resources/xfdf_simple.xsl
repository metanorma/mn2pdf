<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
											xmlns:xalan="http://xml.apache.org/xalan" 
											xmlns:java="http://xml.apache.org/xalan/java"
											exclude-result-prefixes="java xalan"
											version="1.0">

	<xsl:output method="xml" encoding="UTF-8" indent="no"/>
	
	<xsl:param name="if_xml"/>
	
	<xsl:variable name="if_xml_flatten" select="xalan:nodeset($if_xml)"/>
	<!-- <xsl:variable name="if_xml_flatten" select="document($if_xml)"/> for debug from the xalan command line only -->
	
	<!-- Review text coordinates calculation based on values from Apache FOP Intermediate Format -->
	
	<!-- Output xml example:
		<?xml version="1.0" encoding="UTF-8"?>
			<xfdf xmlns="http://ns.adobe.com/xfdf/">
				<annots>
					<text color="#FFC333" opacity="0.600000" flags="nozoom,norotate" date="20220422T000000" page="4" rect="70.866,556.946" title="Reese Plews">
						<contents-richtext>
							<body xmlns="http://www.w3.org/1999/xhtml">
								<p dir="ltr">Propose to </p>
							</body>
						</contents-richtext>
						<popup flags="nozoom,norotate" open="yes" page="4" rect=""/>
					</text>
					<text color="#FFC333" opacity="0.600000" flags="nozoom,norotate" date="20220422T000000" page="4" rect="474.26,702.244" title="Reese Plews">
						<contents-richtext>
							<body xmlns="http://www.w3.org/1999/xhtml">
								<p dir="ltr">Proposed revision shown above.</p>
							</body>
						</contents-richtext>
						<popup flags="nozoom,norotate" open="yes" page="4" rect=""/>
					</text>
					<highlight color="#FFC333" opacity="0.300000" date="" page="4" coords="" rect="474.315,702.244" Highlight="" title="">
						<popup flags="print,nozoom,norotate" open="no" page="4" rect="474.315,702.244"/>
					</highlight>
				</annots>
			</xfdf>
	-->
	
	<xsl:variable name="hair_space">&#x200A;</xsl:variable>

	<xsl:variable name="color_annotation">#FFC333</xsl:variable>
	<xsl:variable name="opacity_popup">0.600000</xsl:variable>
	<xsl:variable name="opacity_highlight">0.300000</xsl:variable>

	<xsl:template match="/">
		
		<xfdf xmlns="http://ns.adobe.com/xfdf/">
		
			<xsl:element name="annots">
			
			
				<xsl:for-each select="//*[local-name() = 'review']">
					<xsl:variable name="id_from">
						<xsl:choose>
							<xsl:when test="normalize-space(@from) != ''"><xsl:value-of select="@from"/></xsl:when>
							<xsl:otherwise><xsl:value-of select="@id"/></xsl:otherwise>
						</xsl:choose>
					</xsl:variable>
					<xsl:variable name="id_to" select="@to"/>
			
					<xsl:variable name="reviewer" select="@reviewer"/>
					<xsl:variable name="date" select="@date"/>
				
					<!-- add Post-It popup -->
					<!-- Example:
						<text color="#FFC333" opacity="0.600000" flags="nozoom,norotate" date="D:20220422000000+03'00'" name="d4e12061-3dbc-4b51-9b7d-89dcdad52d34" page="4" rect="70.865997,289.942993,88.865997,309.942993" title="Reese Plews">
							<contents-richtext>
								<body xmlns="http://www.w3.org/1999/xhtml">
									<p dir="ltr">Propose to deprecate this entry as a definition built upon the <span style="font-style:italic">concepts</span> from
				the SAG MRS has already been proposed by ISO.</p>
								</body>
							</contents-richtext>
							<popup flags="nozoom,norotate" open="yes" page="4" rect="595.275024,195.942993,799.275024,309.942993"/>
						</text>
					-->
					
					<xsl:variable name="element_from_" select="$if_xml_flatten//*[local-name() = 'id'][@name = $id_from]/following-sibling::*[local-name() = 'text'][1]"/>
					<xsl:variable name="element_from" select="xalan:nodeset($element_from_)"/>
					
					<xsl:variable name="page" select="count($element_from/preceding-sibling::*[local-name() = 'page'])"/>
					
					<!-- <debug>
						<id_from><xsl:value-of select="$id_from"/></id_from>
						<element><xsl:copy-of select="$if_xml_flatten//*[local-name() = 'id'][@name = $id_from]/following-sibling::*[local-name() = 'text'][1]"/></element>
						<element_from><xsl:copy-of select="$element_from"/></element_from>
					</debug> -->
					
					<text>
						<xsl:attribute name="color"><xsl:value-of select="$color_annotation"/></xsl:attribute>
						<xsl:attribute name="opacity"><xsl:value-of select="$opacity_popup"/></xsl:attribute>
						<xsl:attribute name="flags">nozoom,norotate</xsl:attribute>
						<xsl:attribute name="date"><xsl:value-of select="$date"/></xsl:attribute>
						<xsl:attribute name="page"><xsl:value-of select="$page - 1"/></xsl:attribute>
						<xsl:attribute name="rect"><xsl:value-of select="concat($element_from/@x,',',$element_from/@y)"/></xsl:attribute>
						<xsl:attribute name="title"><xsl:value-of select="$reviewer"/></xsl:attribute>
					
						<xsl:element name="contents-richtext">
							<body xmlns="http://www.w3.org/1999/xhtml">
								<xsl:apply-templates mode="pdf_richtext"/>
							</body>
						</xsl:element>
						
						<!-- <popup flags="nozoom,norotate" open="yes" page="4" rect="595.275024,195.942993,799.275024,309.942993"/> -->
						<popup>
							<xsl:attribute name="flags">nozoom,norotate</xsl:attribute>
							<xsl:attribute name="open">yes</xsl:attribute>
							<xsl:attribute name="page"><xsl:value-of select="$page - 1"/></xsl:attribute>
							<xsl:attribute name="rect"></xsl:attribute>
						</popup>
						
					</text>
					
					
					
					<!-- add text highlight -->
					
					<!-- Example:
						<highlight color="#FFC333" opacity="0.300000" date="D:20220422000000+03'00'" name="921c0030-b204-4ce8-a2f1-89eae80cc9b1" page="4" coords="474.315000,148.304960,533.885000,148.304960,474.315000,137.644960,533.885000,137.644960" rect="474.315000,139.644960,533.885000,161.304960" subject="Highlight" title="Reese Plews">
							<popup flags="print,nozoom,norotate" open="no" page="4" rect="595.275024,34.304962,799.275024,148.304962"/>
						</highlight>
					-->
					
					<xsl:if test="$id_to != ''">
					
					
						<xsl:variable name="highlight_sequence_">
					
							<xsl:for-each select="$element_from/following-sibling::*[local-name() = 'text'][not(preceding-sibling::*[local-name() = 'id'][@name = $id_to])]">
								<xsl:variable name="page_to" select="count(preceding-sibling::*[local-name() = 'page'])"/>
								
								
								<highlight>
									<xsl:attribute name="color"><xsl:value-of select="$color_annotation"/></xsl:attribute>
									<xsl:attribute name="opacity"><xsl:value-of select="$opacity_highlight"/></xsl:attribute>
									<xsl:attribute name="date"><xsl:value-of select="$date"/></xsl:attribute>
									<xsl:attribute name="page"><xsl:value-of select="$page_to - 1"/></xsl:attribute>
									<xsl:attribute name="coords"></xsl:attribute>
									<xsl:attribute name="rect"><xsl:value-of select="concat(@x,',',@y)"/></xsl:attribute>
									<xsl:attribute name="subject">Highlight</xsl:attribute>
									<xsl:attribute name="title"><xsl:value-of select="$reviewer"/></xsl:attribute>
									
									<popup>
									 <xsl:attribute name="flags">print,nozoom,norotate</xsl:attribute>
									 <xsl:attribute name="open">no</xsl:attribute>
									 <xsl:attribute name="page"><xsl:value-of select="$page_to - 1"/></xsl:attribute>
									 <xsl:attribute name="rect"><xsl:value-of select="concat(@x,',',@y)"/></xsl:attribute>
									</popup>
									
									<hightlighttext><xsl:copy-of select="node()"/></hightlighttext>
									
								</highlight>
								
							</xsl:for-each>
						</xsl:variable>
						
						<xsl:variable name="highlight_sequence" select="xalan:nodeset($highlight_sequence_)"/>
								
						<!-- <xsl:copy-of select="$highlight_sequence"/> -->
						
						<xsl:variable name="pages">
							<xsl:for-each select="$highlight_sequence/*[local-name() = 'highlight']">
								<xsl:if test="not(preceding-sibling::*[local-name() = 'highlight'][@page = current()/@page])">
									<page><xsl:value-of select="@page"/></page>
								</xsl:if>
							</xsl:for-each>
						</xsl:variable>
						
						<!-- <xsl:copy-of select="$pages"/> -->
						
						<!-- group of highlight sequence -->
						<xsl:for-each select="xalan:nodeset($pages)//*[local-name() = 'page']">
							<xsl:variable name="page" select="."/>
							
							<xsl:for-each select="$highlight_sequence/*[local-name() = 'highlight'][@page = $page][1]">
							
								<highlight>
									<xsl:copy-of select="@*"/>
									<xsl:copy-of select="node()"/>
									
									<xsl:for-each select="following-sibling::*[local-name() = 'highlight'][@page = $page]">
										<next_highlight>
											<xsl:copy-of select="@*"/>
											<xsl:copy-of select="node()"/>
										</next_highlight>
									</xsl:for-each>
								
								</highlight>
							
							</xsl:for-each>
							
						</xsl:for-each>
						
					</xsl:if>
					
				</xsl:for-each>
			</xsl:element>
		</xfdf>
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
		<xsl:if test="following-sibling::*"><xsl:text>&#xa;</xsl:text></xsl:if>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'ul'] | *[local-name() = 'ol']" mode="pdf_richtext">
		<xsl:apply-templates mode="pdf_richtext"/>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'ul' or local-name() = 'ol']/*" mode="pdf_richtext">
		<xsl:apply-templates mode="pdf_richtext"/>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'ul' or local-name() = 'ol']/*/*[local-name() = 'p']" mode="pdf_richtext">
		<xsl:element name="p" namespace="{$namespace_xhtml}">
			<xsl:attribute name="dir">ltr</xsl:attribute>
			<xsl:variable name="level" select="count(ancestor-or-self::*[local-name() = 'ul' or local-name() = 'ol'])"/>
			<xsl:call-template name="repeat">
				<xsl:with-param name="count" select="($level - 1) * 5"/>
			</xsl:call-template>
			<xsl:choose>
				<xsl:when test="$level mod 2 = 0">• </xsl:when>
				<xsl:otherwise>&#x2014; </xsl:otherwise>
			</xsl:choose>
			
			<xsl:apply-templates mode="pdf_richtext"/>
		</xsl:element>
		<xsl:text>&#xa;</xsl:text>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'dt']" mode="pdf_richtext">
		<xsl:apply-templates mode="pdf_richtext"/>
		<xsl:text>&#xa0;&#xa0;&#xa0;&#xa0;</xsl:text>
		<xsl:apply-templates select="following-sibling::*[local-name() = 'dd'][1]" mode="pdf_richtext">
			<xsl:with-param name="process">true</xsl:with-param>
		</xsl:apply-templates>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'dd']" mode="pdf_richtext">
		<xsl:param name="process">false</xsl:param>
		<xsl:if test="$process = 'true'">
			<xsl:apply-templates mode="pdf_richtext"/>
			<xsl:if test="following-sibling::*">
				<xsl:text>&#xa;</xsl:text>
			</xsl:if>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="repeat">
		<xsl:param name="char" select="'&#xa0;'"/>
		<xsl:param name="count" />
		<xsl:if test="$count &gt; 0">
			<xsl:value-of select="$char"/>
			<xsl:call-template name="repeat">
				<xsl:with-param name="char" select="$char" />
				<xsl:with-param name="count" select="$count - 1" />
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	
	<xsl:template match="*[local-name() = 'sub' or local-name() = 'sup']" mode="pdf_richtext">
		<xsl:element name="{local-name()}" namespace="{$namespace_xhtml}">
			<xsl:apply-templates mode="pdf_richtext"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="text()" mode="pdf_richtext">
		<!-- remove new line and tab characters -->
		<xsl:variable name="text" select="translate(.,'&#x09;&#x0a;&#x0d;','')"/>
		<!-- replace two and more space to one space -->
		<xsl:value-of select="java:replaceAll(java:java.lang.String.new($text),' {2,}',' ')"/>
	</xsl:template>
	
	<!-- ==================== -->
	<!-- END: PDF rich text format -->
	<!-- ==================== -->
	
</xsl:stylesheet>
