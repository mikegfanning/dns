# dns

## General

This is a simple DNS server written in Java and built to play pranks on my brother. Basically I'm planning to stick it on a Raspberry Pi, point the DHCP on my home network at it and make every web site redirect to some obnoxious YTMND page.

## Requirements

* Java SDK 1.7 or above
* Maven 3.something

## Installation

Currently this project is configured to build a jar that can be invoked via the Java Runtime Environment. At some point I should probably add some shell scripts and configuration files to make deployment simpler.

1. Clone project
2. Run `mvn clean package`
3. Copy dns-X.XX-SNAPSHOT.jar wherever it makes you feel all warm and fuzzy

## Quick Start

> You will need root access to use port 53 (the default DNS port) on *nix and *nix-like systems

1. Make sure `tomcat-embed-core-X.X.X.jar` and `tomcat-embed-logging-juli-X.X.X.jar` are on your classpath
2. Run `java -cp $CLASSPATH:dns-X.XX-SNAPSHOT.jar org.code_revue.dns.DnsApp`
