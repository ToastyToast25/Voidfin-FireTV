plugins {
	alias(libs.plugins.android.application) apply false
	alias(libs.plugins.android.library) apply false
	alias(libs.plugins.detekt)
	java
}

buildscript {
	dependencies {
		classpath(libs.kotlin.gradle)
	}
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(libs.versions.java.jdk.get()))
	}
}

detekt {
	buildUponDefaultConfig.set(true)
	ignoreFailures.set(true)
	config.setFrom(files("$rootDir/detekt.yaml"))
	basePath.set(rootDir)
	parallel.set(true)

	source.setFrom(fileTree(projectDir) {
		include("**/*.kt", "**/*.kts")
	})
}

tasks.withType<dev.detekt.gradle.Detekt> {
	reports {
		sarif.required.set(true)
	}
}

tasks.withType<Test> {
	// Ensure Junit emits the full stack trace when a unit test fails through gradle
	useJUnit()

	testLogging {
		events(
			org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
			org.gradle.api.tasks.testing.logging.TestLogEvent.STANDARD_ERROR,
			org.gradle.api.tasks.testing.logging.TestLogEvent.SKIPPED
		)
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		showExceptions = true
		showCauses = true
		showStackTraces = true
	}
}
