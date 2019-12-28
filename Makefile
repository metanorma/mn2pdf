#!make
SHELL := /bin/bash
JAR_FILE := mn2pdf-1.0.jar

all: target/mn2pdf-1.0.jar

target/mn2pdf-1.0.jar:
	mvn clean package shade:shade

test: target/mn2pdf-1.0.jar tests/pdf_fonts_config.xml
	java -jar target/mn2pdf-1.0.jar tests/pdf_fonts_config.xml tests/G.191.xml tests/itu.recommendation.xsl tests/G.191.pdf

tests/pdf_fonts_config.xml: tests/pdf_fonts_config.xml.in
	MN_PDF_FONT_PATH=${MN_PDF_FONT_PATH}; \
	envsubst < tests/pdf_fonts_config.xml.in > tests/pdf_fonts_config.xml

clean:
	rm -rf target
	rm -rf tests/*.pdf tests/pdf_fonts_config.xml

.PHONY: all clean test
