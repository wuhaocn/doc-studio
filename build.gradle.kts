plugins {
    java
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4" apply false
}

allprojects {
    group = "com.docstudio"
    version = "1.0.0"
    
    repositories {
        mavenCentral()
    }
    
    // 统一管理依赖版本
    extra["mybatis-plus.version"] = "3.5.8"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}

