<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns="http://xmlgraphics.apache.org/fop/intermediate" 
	xmlns:im="http://xmlgraphics.apache.org/fop/intermediate" 
	xmlns:fo="http://www.w3.org/1999/XSL/Format" 
	xmlns:foi="http://xmlgraphics.apache.org/fop/internal"   
	xmlns:fox="http://xmlgraphics.apache.org/fop/extensions"
	xmlns:xalan="http://xml.apache.org/xalan"  
	xmlns:jeuclid="http://jeuclid.sf.net/ns/ext"
	xmlns:java="http://xml.apache.org/xalan/java"
	xmlns:math="http://exslt.org/math"
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
				<xsl:for-each select=".//im:text">
				
					<xsl:variable name="y_current" select="number(@y)"/>
				
					<xsl:variable name="process">
						<xsl:choose>
							<!-- <xsl:when test="preceding-sibling::im:font[1][number(@size) &lt;= 8000][preceding-sibling::*[1][self::im:line] or ancestor::im:viewport[1]/preceding-sibling::im:line]"></xsl:when> --> <!-- skip -->
							<xsl:when test="preceding-sibling::im:id[starts-with(@name,'footnote_')] or ancestor::im:viewport/im:id[starts-with(@name,'footnote_')]"><!-- skip --></xsl:when>
							<xsl:when test="@x = '0'">true</xsl:when>
							<xsl:when test="@x != '0' and preceding-sibling::im:text[math:abs(number(@y) - $y_current) &lt; 10000]"><!-- skip --></xsl:when>
							<!-- <xsl:when test="@x != '0' and not(preceding-sibling::im:text[@y = $y_current]) and preceding-sibling::im:text[(number(@y) - number($y_current)) &gt; 10000 or (number($y_current) - number(@y)) &gt; 10000]">true2</xsl:when> -->
							<xsl:otherwise>true</xsl:otherwise>
							<!-- not(preceding-sibling::im:text[@y = current()/@y and @x = '0']) -->
						</xsl:choose>
					</xsl:variable>
					
					<xsl:if test="contains(normalize-space($process), 'true')">
						<xsl:element name="text" namespace="http://xmlgraphics.apache.org/fop/intermediate">
							<xsl:attribute name="x"><xsl:value-of select="$x_shift"/></xsl:attribute>
							<xsl:attribute name="y"><xsl:value-of select="@y"/></xsl:attribute>
							
							<!-- <xsl:attribute name="process"><xsl:value-of select="$process"/></xsl:attribute> -->
							
							<xsl:variable name="curr_y" select="@y"/>
							
							<!-- <xsl:for-each select="preceding-sibling::im:text[1]">
								<xsl:attribute name="diff"><xsl:value-of select="math:abs(number(@y) - number($curr_y))"/></xsl:attribute>
							</xsl:for-each> -->
							
							<xsl:attribute name="count"><xsl:value-of select="count(ancestor::im:viewport)"/></xsl:attribute>
							
							<xsl:if test="count(ancestor::im:viewport) + count(ancestor::im:g) &gt; 1">
								<xsl:variable name="viewport_">
									<xsl:apply-templates select="ancestor::*[local-name() = 'viewport' or local-name() = 'g'][1]" mode="shift_y"/>
								</xsl:variable>
								<!-- <xsl:variable name="viewport" select="xalan:nodeset($viewport_)"/> -->
								
								<!-- <xsl:attribute name="y"><xsl:value-of select="($viewport/*[local-name() = 'viewport']/*[local-name() = 'y'] + @y)"/></xsl:attribute> -->
								<xsl:attribute name="y"><xsl:value-of select="$viewport_ + @y"/></xsl:attribute>
								
								<!-- <xsl:copy-of select="$viewport"/> -->
								
							</xsl:if>
							
						</xsl:element>
					</xsl:if>
					
				</xsl:for-each>
			</xsl:variable>
		
			<xsl:variable name="texts" select="xalan:nodeset($texts_)"/>
		
			<!-- <xsl:copy-of select="$texts"/> -->
		
			<xsl:variable name="texts_unique_">
				<xsl:for-each select="$texts/*">
					<xsl:if test="not(preceding-sibling::*[@y = current()/@y])"> <!-- condition for table cells in one row -->
						<xsl:copy-of select="."/>
					</xsl:if>
				</xsl:for-each>
			</xsl:variable>
			<xsl:variable name="texts_unique" select="xalan:nodeset($texts_unique_)"/>
		
			<xsl:copy-of select="$texts_unique"/>
		
			<xsl:if test="count($texts_unique/*) &gt; 0">
				<xsl:element name="font" namespace="http://xmlgraphics.apache.org/fop/intermediate">
					<xsl:attribute name="family">Times New Roman</xsl:attribute>
					<xsl:attribute name="weight">400</xsl:attribute>
					<xsl:attribute name="style">normal</xsl:attribute>
					<xsl:attribute name="size">11000</xsl:attribute>
					<xsl:attribute name="color">black</xsl:attribute>
					<xsl:for-each select="$texts_unique/*">
						<xsl:sort select="@y" data-type="number"/>
						<xsl:copy>
							<xsl:copy-of select="@*"/>
							
							<xsl:value-of select="position()"/>
							<!-- <xsl:copy-of select="."/> -->
						</xsl:copy>
					</xsl:for-each>
				</xsl:element>
			</xsl:if>
		</xsl:copy>
	
	</xsl:template>
	
	
	<xsl:template match="*[local-name() = 'viewport' or local-name() = 'g']" mode="shift_y">
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
				
				<!-- <ancestor_count1><xsl:value-of select="count(ancestor::im:viewport)"/></ancestor_count1>
				<transform><xsl:value-of select="@transform"/></transform>
				<new_y1><xsl:value-of select="$new_y"/></new_y1> -->
				
				<xsl:choose>
					<xsl:when test="count(ancestor::*[local-name() = 'viewport' or local-name() = 'g']) &gt; 1">
						<xsl:apply-templates select="ancestor::*[local-name() = 'viewport' or local-name() = 'g'][1]" mode="shift_y">
							<xsl:with-param name="y" select="$new_y"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<!-- <viewport>
							<y><xsl:value-of select="$new_y"/></y>
						</viewport> -->
						<xsl:value-of select="$new_y"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>
			
			<xsl:otherwise>
			
				<!-- <ancestor_count2><xsl:value-of select="count(ancestor::im:viewport)"/></ancestor_count2>
				<new_y2><xsl:value-of select="$y"/></new_y2> -->
			
				<xsl:choose>
				
					<xsl:when test="count(ancestor::*[local-name() = 'viewport' or local-name() = 'g']) &gt; 1">
						<xsl:apply-templates select="ancestor::*[local-name() = 'viewport' or local-name() = 'g'][1]" mode="shift_y">
							<xsl:with-param name="y" select="$y"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<!-- <viewport>
							<y><xsl:value-of select="$y"/></y>
						</viewport> -->
						<xsl:value-of select="$y"/>
					</xsl:otherwise>
				</xsl:choose>
					
			</xsl:otherwise>
		</xsl:choose>
		
	</xsl:template>
	
</xsl:stylesheet>
