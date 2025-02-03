package com.prorunner.prorunner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // meta-annotations --> combines 3 annotations
public class ProRunnerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProRunnerApplication.class, args);
	}

}

/* @SpringBootApplication --->( meta-annotations )--> combines 3 annotations
*  1. @Configuration – Allows defining beans using (@Bean) annotation.
*  2. @EnableAutoConfiguration – Automatically configures beans based on dependencies.
*  3. @ComponentScan – Scans the package and registers components (controllers, services, etc.).
* */
