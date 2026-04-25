<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:mn="https://www.metanorma.org/ns/standoc" xmlns:mnx="https://www.metanorma.org/ns/xslt" xmlns:mathml="http://www.w3.org/1998/Math/MathML" xmlns:xalan="http://xml.apache.org/xalan" xmlns:fox="http://xmlgraphics.apache.org/fop/extensions" xmlns:pdf="http://xmlgraphics.apache.org/fop/extensions/pdf" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:java="http://xml.apache.org/xalan/java" xmlns:barcode="http://barcode4j.krysalis.org/ns" xmlns:redirect="http://xml.apache.org/xalan/redirect" exclude-result-prefixes="java" extension-element-prefixes="redirect" version="1.0">

	<xsl:output method="xml" encoding="UTF-8" indent="no"/>

	<xsl:param name="add_math_as_text">true</xsl:param>
	<xsl:param name="lang"/>
	
	<xsl:template match="/">
		<fo:root xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:barcode="http://barcode4j.krysalis.org/ns" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:pdf="http://xmlgraphics.apache.org/fop/extensions/pdf" xmlns:fox="http://xmlgraphics.apache.org/fop/extensions" xmlns:xalan="http://xml.apache.org/xalan" xmlns:mathml="http://www.w3.org/1998/Math/MathML" xmlns:mnx="https://www.metanorma.org/ns/xslt" xmlns:mn="https://www.metanorma.org/ns/standoc" xmlns:jeuclid="http://jeuclid.sf.net/ns/ext" font-family="Noto Sans JP, Times New Roman, Cambria Math" font-size="11pt">
			<xsl:attribute name="xml:lang"><xsl:value-of select="$lang"/></xsl:attribute>
			<fo:layout-master-set>
				<fo:simple-page-master master-name="document" page-width="210mm" page-height="297mm">
					<fo:region-body margin-top="20mm" margin-bottom="20mm" margin-left="20mm" margin-right="20mm"/>
				</fo:simple-page-master>
			</fo:layout-master-set>
			<fo:declarations>
				<pdf:catalog>
					<pdf:dictionary key="ViewerPreferences" type="normal">
						<pdf:boolean key="DisplayDocTitle">true</pdf:boolean>
					</pdf:dictionary>
				</pdf:catalog>
				<x:xmpmeta xmlns:x="adobe:ns:meta/">
					<rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
						<rdf:Description xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:pdf="http://ns.adobe.com/pdf/1.3/" rdf:about="">
							<dc:title>
								<rdf:Alt>
									<rdf:li xml:lang="x-default">Cereals and pulses — Specifications and test methods — Locality.part 1: Rice (Final)</rdf:li>
								</rdf:Alt>
							</dc:title>
							<dc:creator>
								<rdf:Seq>
									<rdf:li>International Organization for Standardization</rdf:li>
								</rdf:Seq>
							</dc:creator>
							<pdf:Keywords>keyword1, keyword2, keyword3.</pdf:Keywords>
						</rdf:Description>
						<rdf:Description xmlns:xmp="http://ns.adobe.com/xap/1.0/" rdf:about="">
							<xmp:CreatorTool/>
						</rdf:Description>
					</rdf:RDF>
				</x:xmpmeta>
			</fo:declarations>
			<fo:page-sequence initial-page-number="1" force-page-count="no-force" master-reference="document">
				<fo:flow flow-name="xsl-region-body">
					<fo:block>
						<xsl:apply-templates/>
					</fo:block>
				</fo:flow>
			</fo:page-sequence>
		</fo:root>
	</xsl:template>
	
	<xsl:template match="mn:p">
		<fo:block>
			<xsl:copy-of select="@*"/>
			<xsl:apply-templates/>
		</fo:block>
	</xsl:template>
	
</xsl:stylesheet>