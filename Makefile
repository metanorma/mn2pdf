#!make
SHELL ?= /bin/bash
JAR_VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
JAR_FILE := mn2pdf-$(JAR_VERSION).jar

FONTS := \
	SourceCodePro-Black.ttf \
	SourceCodePro-BlackItalic.ttf \
	SourceCodePro-Bold.ttf \
	SourceCodePro-BoldItalic.ttf \
	SourceCodePro-ExtraLight.ttf \
	SourceCodePro-ExtraLightItalic.ttf \
	SourceCodePro-Italic.ttf \
	SourceCodePro-Light.ttf \
	SourceCodePro-LightItalic.ttf \
	SourceCodePro-Medium.ttf \
	SourceCodePro-MediumItalic.ttf \
	SourceCodePro-Regular.ttf \
	SourceCodePro-SemiBold.ttf \
	SourceCodePro-SemiBoldItalic.ttf \
	SourceSansPro-Black.ttf \
	SourceSansPro-BlackItalic.ttf \
	SourceSansPro-Bold.ttf \
	SourceSansPro-BoldItalic.ttf \
	SourceSansPro-ExtraLight.ttf \
	SourceSansPro-ExtraLightItalic.ttf \
	SourceSansPro-Italic.ttf \
	SourceSansPro-Light.ttf \
	SourceSansPro-LightItalic.ttf \
	SourceSansPro-Regular.ttf \
	SourceSansPro-SemiBold.ttf \
	SourceSerifPro-Black.ttf \
	SourceSerifPro-Bold.ttf \
	SourceSerifPro-ExtraLight.ttf \
	SourceSerifPro-Light.ttf \
	SourceSerifPro-Regular.ttf \
	SourceSerifPro-SemiBold.ttf \
	SourceHanSans-Bold.ttc \
	SourceHanSans-ExtraLight.ttc \
	SourceHanSans-Heavy.ttc \
	SourceHanSans-Light.ttc \
	SourceHanSans-Medium.ttc \
	SourceHanSans-Normal.ttc \
	SourceHanSans-Regular.ttc

FONTS := $(addprefix src/main/resources/fonts/,$(FONTS))

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

src/main/resources/fonts/SourceSansPro-%: | src/main/resources/fonts
	curl -sSL -o $@ https://github.com/google/fonts/raw/master/ofl/sourcesanspro/$(notdir $@)

src/main/resources/fonts/SourceSerifPro-%: | src/main/resources/fonts
	curl -sSL -o $@ https://github.com/google/fonts/raw/master/ofl/sourceserifpro/$(notdir $@)

src/main/resources/fonts/SourceCodePro-%: | src/main/resources/fonts
	curl -sSL -o $@ https://github.com/google/fonts/raw/master/ofl/sourcecodepro/$(notdir $@)

tmp:
	mkdir -p $@

tmp/SourceHanSans.7z: | tmp
	curl -ssL -o $@ https://github.com/Pal3love/Source-Han-TrueType/raw/master/SourceHanSans.7z

tmp/SourceHanSans-%.ttc: tmp/SourceHanSans.7z
	7za e -y $< -otmp
	touch tmp/*.ttc

src/main/resources/fonts/SourceHanSans-%.ttc: tmp/SourceHanSans-%.ttc
	cp $< $@
