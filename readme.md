# MARU_PAY

MARU_PAY는 Vert.x 기반의 결제 처리 서버로, 다양한 결제/정산 API를 제공합니다. 상점 단말 등록, 결제, 환불, 정산, 가상계좌 처리 등 결제 라이프사이클 전반에 필요한 기능을 포함합니다.

## 아키텍처 개요
- **진입점 및 라우팅**: `com.pgmate.pay.main.VertXServerNew`가 Vert.x HTTP 서버를 열고, `com.pgmate.pay.main.Api`에서 URI별 라우팅 및 요청 검증을 수행합니다.
- **프로세서 계층**: `src/com/pgmate/pay/proc` 하위의 `ProcPay`, `ProcRefund`, `ProcSettle*`, `Vact*` 등 프로세서가 결제, 환불, 정산, 가상계좌 시나리오별 비즈니스 로직을 처리합니다. 외부 PG/VAN 연동 웹훅(`ProcWebHook*`)과 모바일 리턴 처리(`*Return`, `*MobileReturn`)도 이 계층에서 담당합니다.
- **데이터/설정 계층**: `src/com/pgmate/pay/dao`가 트랜잭션/정산 데이터를 DB에 기록하며, `conf/`의 `galaxiaconfig.ini`, `*.json` 등을 통해 DB 커넥션과 서비스 파라미터를 주입합니다. JDBC 풀(HikariCP)과 MariaDB 드라이버를 사용합니다.
- **유틸리티 및 공통 구성**: `src/com/pgmate/pay/util`의 암호화·서명·포맷 유틸과 `bean`, `conf`, `firm`, `van` 패키지가 요청 DTO, 정산/밴더별 설정, 정산 계약 정보를 제공합니다. 로깅은 Logback/SLF4J를 사용합니다.
- **빌드/배포**: Ant 기반 빌드로 `lib/MARU_pay.jar`를 생성하며, `bin/start.sh`/`stop.sh` 스크립트가 실행·종료를 표준화합니다.

## 기술 스택
- **언어/런타임**: Java 8
- **프레임워크**: Vert.x 3.3.x (Netty 기반 비동기 HTTP 라우팅)
- **빌드/도구**: Apache Ant, JUnit 4
- **데이터/연결**: HikariCP, MariaDB JDBC Driver
- **로깅/유틸**: SLF4J + Logback, Apache HttpClient, Guava, Gson, JSON-Simple
- **결제 연동 라이브러리**: NICEPAY, Allat, Toss, KICC, Daou 등 외부 VAN/PG 클라이언트 JAR ( `lib/` 참고 )

## 폴더 구조
- `src/com/pgmate/pay` : 결제·정산 처리 로직, API 라우팅(`VertXServerNew`, `Api` 등)과 개별 프로세스 구현이 위치합니다.
- `src/com/pgmate/test` : 내부 테스트 및 샘플 호출 코드가 포함된 패키지입니다.
- `test/` : JUnit 기반 단위 테스트 모음.
- `lib/` : 빌드와 실행에 필요한 외부 라이브러리 JAR.
- `conf/` : 서비스 환경 설정(`*.json`, `logback.xml`, `galaxiaconfig.ini` 등)이 저장됩니다.
- `bin/` : Ant 빌드 스크립트(`build.xml`)와 실행 스크립트(`start.sh`, `stop.sh`, `start.bat`).
- `war/` : 실행 시 클래스패스에 포함되는 리소스 번들.

## 요구 사항
- **JDK 8** 이상
- **Apache Ant** (빌드에 사용)
- Linux/Unix 또는 Windows 실행 환경

## 빌드 방법
1. 저장소 루트에서 다음 명령으로 컴파일합니다.
   ```bash
   ant -f bin/build.xml
   ```
   결과 JAR(`MARU_pay.jar`)이 `lib/`에 생성되고, `classes/` 디렉터리에 컴파일 산출물이 배치됩니다.

## 실행 방법
1. `conf/` 경로에 서비스 설정 파일을 준비합니다.
2. Linux/Unix 환경에서 서버를 실행합니다.
   ```bash
   cd bin
   ./start.sh
   ```
   - `start.sh`는 JVM 인코딩을 UTF-8로 설정하고 `conf/logback.xml`을 로깅 설정으로 사용합니다.
   - 프로세스 PID는 `bin/apio.pid`에 기록됩니다.
3. 서버를 중지하려면 다음을 실행합니다.
   ```bash
   cd bin
   ./stop.sh
   ```
   - `stop.sh`는 `apio.pid`를 이용해 프로세스를 종료합니다.

Windows 환경에서는 `bin/start.bat`을 참고하여 동일한 JVM 옵션을 적용할 수 있습니다.

## 테스트
- `test/` 디렉터리의 JUnit 테스트는 IDE나 개별 런너에서 실행할 수 있습니다.
- 데이터베이스 및 상점/단말 정보가 필요한 테스트가 있으므로, 실행 전 관련 설정을 맞춰주세요.

## 주요 엔트리포인트
- `com.pgmate.pay.main.VertXServerNew` : Vert.x HTTP 서버를 시작하여 API 라우팅을 초기화합니다.
- `com.pgmate.pay.main.Api` : 요청 URI에 따라 결제, 환불, 정산, 가상계좌 등 각 프로세스 클래스로 위임합니다.
