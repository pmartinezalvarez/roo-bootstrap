This is a Spring ROO Plugin fon include Twitter Bootstrap Dojo Implementation provided by @xsokev (http://dojobootstrap.com) on your ROO Projects.

To get it running download the maven project and package it to get com.roo.bootstrap-0.1.0.BUILD-SNAPSHOT.jar artifact.

Add the module to your ROO project running console using:

osgi start --url file://${PATH_TO_YOUR_ROO_BOOTSTRAP_JAR}/com.roo.bootstrap-0.1.0.BUILD-SNAPSHOT.jar

Then you can use it in a Spring MVC web project using the command:

web mvc install bootstrap

This is a sample ROO script that builds a demo application:

project --topLevelPackage com.roo.bootstrap.demo --projectName roo-bootstrap-demo --java 6 --packaging JAR
persistence setup --database HYPERSONIC_PERSISTENT --provider HIBERNATE 
entity jpa --class com.roo.bootstrap.demo.domain.Foo 
field string --fieldName foo
osgi start --url file://${PATH_TO_YOUR_ROO_BOOTSTRAP_JAR}/com.roo.bootstrap-0.1.0.BUILD-SNAPSHOT.jar
web mvc setup
web mvc install bootstrap
web mvc all --package com.roo.bootstrap.demo.web