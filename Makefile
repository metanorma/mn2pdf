#!make
ifeq ($(OS),Windows_NT)
SHELL ?= cmd
else
SHELL ?= /bin/bash
endif

#JAR_VERSION := $(shell mvn -q -Dexec.executable="echo" -Dexec.args='$${project.version}' --non-recursive exec:exec -DforceStdout)
JAR_VERSION := 1.73
JAR_FILE := mn2pdf-$(JAR_VERSION).jar

all: target/$(JAR_FILE)

target/$(JAR_FILE):
	mvn --settings settings.xml -DskipTests clean package shade:shade

test: target/$(JAR_FILE)
	mvn --settings settings.xml test surefire-report:report

deploy:
	mvn --settings settings.xml -Dmaven.test.skip=true clean deploy shade:shade

clean:
	mvn clean


.PHONY: all clean test deploy version target/$(JAR_FILE)
