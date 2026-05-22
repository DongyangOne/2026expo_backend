# 2026expo_backend

## 🇰🇷 프로젝트 정보

본 프로젝트는 2026년 ONE 동양미래 EXPO입니다.

### 권장 개발 환경

- Java 17
- Spring Boot 4.x
- Gradle
- MySql 8.0

### 프로젝트 구조

```
├───main
│   ├───java
│   │   └───one
│   │       └───2026expobackend
│   │           ├───domain   // 엔티티
│   │           └───global    // 애플리케이션 전역에서 공통으로 사용되는 설정, 예외 처리 등을 관리
│   │               ├───config 
│   │               │   ├───auth  // Jwt 관련 설정
│   │               │   ├───filter // 요청 로깅 등 로깅 필터 설정
│   │               │   └───swagger // 스웨거 설정
│   │               ├───dto   // 공통 response 객체
│   │               ├───entity  // 생성/수정 시간 자동 기록
│   │               ├───enums // 공통 에러 코드 및 공통 enums 관리
│   │               ├───exception  // 전역 예외 관리
│   │               ├───pagination  // 페이징 관련 설정 및 DTO
│   │               └───security  // SpringSecurity 관리
```




### 추가 팁

- http://localhost:8080/swagger-ui.html 로 스웨거를 확인해 볼 수 있습니다.
- 새로운 에러 상황이 발생하면 common/enums/ErrorCode.java에 에러 코드를 먼저 등록한 뒤 사용하세요.
- 새로운 Entity를 만들 때 BaseEntity를 상속 받으세요.

### 자주 발생하는 문제

- Java 버전 충돌: `JAVA_HOME`이 올바른지 확인하고, `java -version`을 통해 17인지 체크하세요.


