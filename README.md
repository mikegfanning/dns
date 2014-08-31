# dns

## General

This is a simple set of DNS server components written in Java and built to play pranks on my brother. Basically I'm planning to stick it on a Raspberry Pi, point the DHCP on my home network at it and make every web site redirect to some obnoxious YTMND page.

In a hilarious reversal of fortune, my wireless router doesn't allow me to configure the DNS server provided via DHCP - it always lists itself as the DNS server and relays to an external server. Time to start working on a DHCP implementation I guess...

## Requirements

This library uses [slf4j](http://www.slf4j.org/), which requires a runtime logging framework, as described [here](http://www.slf4j.org/manual.html).

* Java SDK 1.7 or above
* Maven 3.something
* slf4j logging framework

## Installation

The product of this project is JAR that is intended to be used by other projects. You can do `mvn install` from the root directory to install it in your local maven repo. If this ever becomes a real thing, I should prolly publish it to a public repo.

## Test App

There is a test application that sets up a simple DNS server and embedded [Tomcat](http://tomcat.apache.org/) instance with a redirect servlet. It is located at `src/test/java/org/code_revue/dns/TestDnsApp.java` and binds to ports 1053 (DNS) and 1080 (HTTP redirect). You will have to run this as a superuser to bind to the actual DNS and HTTP ports, or use `iptables` or `pfctl` or something to map the ports otherwise.
