#!make
SHELL ?= /bin/bash
JAR_VERSION := $(shell mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
JAR_FILE := mn2pdf-$(JAR_VERSION).jar

all: target/$(JAR_FILE)

target/$(JAR_FILE):
	mvn -DskipTests clean package shade:shade

test: target/$(JAR_FILE)
	mvn test surefire-report:report

clean:
	rm -rf target
	rm -rf tests/*.pdf

.PHONY: all clean test
