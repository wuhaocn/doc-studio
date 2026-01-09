dependencies {
    // 依赖公共模块
    implementation(project(":doc-server-common"))
    
    // 依赖管理模块
    implementation(project(":doc-server-manager"))
    
    // Spring Boot Starter
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    // MyBatis Plus
    implementation("com.baomidou:mybatis-plus-boot-starter:3.5.5")
    
    // H2 Database (内置数据库，用于快速开发)
    runtimeOnly("com.h2database:h2")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
}

