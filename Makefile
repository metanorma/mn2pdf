#!make
SHELL ?= /bin/bash
JAR_VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
JAR_FILE := mn2pdf-$(JAR_VERSION).jar

FONTS := \
	SourceCodePro-Black \
	SourceCodePro-BlackItalic \
	SourceCodePro-Bold \
	SourceCodePro-BoldItalic \
	SourceCodePro-ExtraLight \
	SourceCodePro-ExtraLightItalic \
	SourceCodePro-Italic \
	SourceCodePro-Light \
	SourceCodePro-LightItalic \
	SourceCodePro-Medium \
	SourceCodePro-MediumItalic \
	SourceCodePro-Regular \
	SourceCodePro-SemiBold \
	SourceCodePro-SemiBoldItalic \
	SourceSansPro-Black \
	SourceSansPro-BlackItalic \
	SourceSansPro-Bold \
	SourceSansPro-BoldItalic \
	SourceSansPro-ExtraLight \
	SourceSansPro-ExtraLightItalic \
	SourceSansPro-Italic \
	SourceSansPro-Light \
	SourceSansPro-LightItalic \
	SourceSansPro-Regular \
	SourceSansPro-SemiBold \
	SourceSerifPro-Black \
	SourceSerifPro-Bold \
	SourceSerifPro-ExtraLight \
	SourceSerifPro-Light \
	SourceSerifPro-Regular \
	SourceSerifPro-SemiBold

FONTS := $(addprefix src/main/resources/fonts/,$(addsuffix .ttf,$(FONTS)))

all: $(FONTS) target/$(JAR_FILE)

allfonts: $(FONTS)

target/$(JAR_FILE):
	mvn -DskipTests clean package shade:shade

test: target/$(JAR_FILE)
	mvn test surefire-report:report

clean:
	mvn clean

fontclean:
	rm -rf fonts

.PHONY: all clean test

src/main/resources/fonts:
	mkdir -p $@

src/main/resources/fonts/SourceSansPro-%.ttf: | src/main/resources/fonts
	curl -sSL -o $@ https://github.com/google/fonts/raw/master/ofl/sourcesanspro/$(notdir $@)

src/main/resources/fonts/SourceSerifPro-%.ttf: | src/main/resources/fonts
	curl -sSL -o $@ https://github.com/google/fonts/raw/master/ofl/sourceserifpro/$(notdir $@)

src/main/resources/fonts/SourceCodePro-%.ttf: | src/main/resources/fonts
	curl -sSL -o $@ https://github.com/google/fonts/raw/master/ofl/sourcecodepro/$(notdir $@)
