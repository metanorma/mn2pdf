#!make
SHELL ?= /bin/bash
ifdef ComSpec
	PATHSEP2 := \\
else
	PATHSEP2 := /
endif
PATHSEP := $(strip $(PATHSEP2))

#JAR_VERSION := $(shell mvn -q -Dexec.executable="echo" -Dexec.args='$${project.version}' --non-recursive exec:exec -DforceStdout)
JAR_VERSION := 1.6
JAR_FILE := mn2pdf-$(JAR_VERSION).jar

all: target/$(JAR_FILE)

target/$(JAR_FILE):
	mvn -DskipTests clean package shade:shade

test: target/$(JAR_FILE)
	mvn test surefire-report:report

clean:
	mvn clean

version:
	echo "${JAR_VERSION}"

.PHONY: all clean test version
