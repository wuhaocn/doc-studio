dependencies {
    // 依赖公共模块
    implementation(project(":doc-server-common"))
    
    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    // MyBatis Plus (兼容 Spring Boot 3.x) - 使用 Spring Boot 3 专用版本
    implementation("com.baomidou:mybatis-plus-spring-boot3-starter:3.5.9")
    // MyBatis Plus 分页插件依赖（3.5.9+ 版本需要单独引入）
    implementation("com.baomidou:mybatis-plus-jsqlparser:3.5.9")
    
    // H2 Database (内置数据库，用于快速开发)
    runtimeOnly("com.h2database:h2")
    
    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    
    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
}

