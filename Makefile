#!make
SHELL ?= /bin/bash

#JAR_VERSION := $(shell mvn -q -Dexec.executable="echo" -Dexec.args='$${project.version}' --non-recursive exec:exec -DforceStdout)
JAR_VERSION := 1.6
JAR_FILE := mn2pdf-$(JAR_VERSION).jar

all: target/$(JAR_FILE)

target/$(JAR_FILE): settings.xml
	mvn -DskipTests clean package shade:shade

test: target/$(JAR_FILE)
	mvn test surefire-report:report

settings.xml:
	PAT_TOKEN=${PAT_TOKEN}; \
	PAT_USERNAME=${PAT_USERNAME}; \
	envsubst < settings.xml.in > ~/.m2/settings.xml
	cat ~/.m2/settings.xml

clean:
	mvn clean

version:
	echo "${JAR_VERSION}"

.PHONY: all clean test version
