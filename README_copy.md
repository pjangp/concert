# GasStation Project
- 주유소 판매/물류 프로젝트입니다
<p align="left"><img src="https://user-images.githubusercontent.com/76420081/120092243-5b65b700-c14c-11eb-8356-03083e54f0c2.png"></p>

# Table of contents

- [GasStation Project (주유소 판매시스템)](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [체크포인트](#체크포인트)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [폴리글랏 프로그래밍](#폴리글랏-프로그래밍)
    - [동기식 호출 과 Fallback 처리](#동기식-호출-과-Fallback-처리)
    - [비동기식 호출 과 Eventual Consistency](#비동기식-호출-과-Eventual-Consistency)
  - [운영](#운영)
    - [CI/CD 설정](#cicd설정)
    - [동기식 호출 / 서킷 브레이킹 / 장애격리](#동기식-호출-서킷-브레이킹-장애격리)
    - [오토스케일 아웃](#오토스케일-아웃)
    - [무정지 재배포](#무정지-재배포)
  - [신규 개발 조직의 추가](#신규-개발-조직의-추가)

# 서비스 시나리오

개요
1. 주유소 관련 주문/주문취소/상품마스터/판매 업무처리를 한다
2. 시스템
    - 주유소 점포시스템: BOS - BackOffice System
    - 주유소 판매시스템: POS - Point Of Sale
    - 주문시스템: order Market
    - 물류시스템: Logistics 
      - 본래는 배차업무와 배송은 별개업무임
      - 배송회사(예:글로비스)와 물류회사(예:송유관공사)는 별개로 존재함
      - 그러나 구현모델을 간단하게 하기 위해서 합친다
    - 구현범위 외( 현 구현범위 아님)
      - 위틱수수료정산
      - 배차
      - 본사ERP
      - 비고
        - 구 시스템들도 MSMQ로 시스템 격리되어 있으며, 옛날기술도 구현된 MSA로 볼 수도 있다
        - 타 시스템들이 장애생겨도, 각각 독립운영가능하다

![image](https://user-images.githubusercontent.com/76420081/120093071-af739a00-c152-11eb-89cf-e3232023e7bd.png)


기능적 요구사항
1. 유류입고
    - 점포담당자는 주문시스템을 통해서 주문한다
    - 주문시스템에선 물류시스템에 배차와 배송을 동시에 요구하나, 여기선 하나로 합쳐서 배송으로 처리한다
    - 배차된 유류차는 물류기지에서 점포까지 배송하며, 이는 배송(shipment)로 표현한다
    - 배송처리가 되면 주유소에선 입고예정자료를 받아볼 수 있다
    - 유류차가 도착하여 배송물량을 확인하고 입고되면, 점포시스템 최종 입고확정하며, 입고재고로 확정된다
2. 상품정보
    - 상품정보는 상품원장 or 상품마스터하며 판매자료의 근거가 된다
    - 상품원장은 본래 ERP 등 본사스템에서 관리하나, 여기에서는 주문시스템이 본사시스템을 겸한다
    - 상품원장의 가격정보가 변경되면, 주문시스템->점포시스템->POS 순으로 변경정보가 반영된다
3. 판매
    - POS시스템에서 고객과 대면하며 판매처리한다.
    - 판매처리되면, 즉시 재고감소와 판매집계를 해야한다. (Req/Res 테스트를 위한 동기처리)
    - 판매취소할 수 있으며, 판매취소되면 재고는 다시 증가, 판매집계에선 제외되야 한다.
        - 금융에선 이를 보상처리, 물류에선 자료보정처리라고 한다.
4. 계정(추후구현범위, 현범위 아님)
    - 점포에 있는 계정은 외상장부의 표현이다. 
    - 계정은 외상계정이며 외상의 금액(잔액)은 곧 고객에게 받을돈=미수금=채권이다.
    - 매출은 곧 외상의 발생이다. 
    - 외상계정의 외상잔액은 입금처리하면 감소한다
    - 현금또한 외상계정으로 처리하며, 현금은 외상발생과 동시에 입금처리된다

비기능적 요구사항
1. 트랜잭션
    - 판매처리와 동시에, 점포시스템의 재고감소처리한다
    - 판매취소와 동시에, 점포시스템의 재고증가처리한다
1. 장애격리
    - 점포시스템은 본사시스템과 별개로 운영가능하다
    - 주문/배송 또한 별개 시스템
1. 성능
    - 주문자는 주문 진행상태를 수시로 확인한다 (CQRS)
    - 점포매니저는 재고총계/판매총계을 수시로 확인한다 (CQRS)


# 체크포인트

- 분석 설계
  - 이벤트스토밍: 
    - 스티커 색상별 객체의미를 제대로 이해하여 헥사고날 아키텍처의 연계설계에 반영하고 있는가?
    - 각 도메인 이벤트가 의미있게 정의되었는가?
    - 어그리게잇: Command와 Event를 ACID 트랜잭션 단위의 Aggregate로 제대로 묶었는가?
    - 기능요구사항과 비기능요구사항을 누락 없이 반영하였는가?    
  - 서브 도메인, 바운디드 컨텍스트 분리
    - 항목 
      - 팀별 KPI 와 관심사, 상이한 배포주기 등에 따른  
      - Sub-domain이나 Bounded Context를 분리하였고 그 분리기준이 합리적인가?
      - 3개 이상 서비스 분리
    - 폴리글랏 설계: 
      - 각 마이크로 서비스들의 구현목표와 기능특성에 따른 각자의 기술 Stack과 저장소 구조를 다양하게 채택하여 설계하였는가?
    - 서비스 시나리오 중 ACID 트랜잭션이 크리티컬한 Use 케이스에 대하여 무리하게 서비스가 과다하게 조밀히 분리되지 않았는가?
  - 컨텍스트 매핑 / 이벤트 드리븐 아키텍처 
    - 업무 중요성과 도메인간 서열을 구분할 수 있는가? (Core, Supporting, General Domain)
    - Request-Response 방식과 이벤트 드리븐 방식을 구분하여 설계할 수 있는가?
    - 장애격리: 서포팅 서비스를 제거 하여도 기존 서비스에 영향이 없도록 설계하였는가?
    - 신규 서비스를 추가 하였을때 기존 서비스의 데이터베이스에 영향이 없도록 설계(열려있는 아키택처)할 수 있는가?
    - 이벤트와 폴리시를 연결하기 위한 Correlation-key 연결을 제대로 설계하였는가?
  - 헥사고날 아키텍처
    - 설계 결과에 따른 헥사고날 아키텍처 다이어그램을 제대로 그렸는가?
    
- 구현
  - [DDD] 분석단계에서의 스티커별 색상과 헥사고날 아키텍처에 따라 구현체가 매핑되게 개발되었는가?
    - Entity Pattern과 Repository Pattern을 적용하여 JPA를 사용한 데이터 접근 어댑터를 개발하였는가?
    - [헥사고날 아키텍처] 
      - REST Inbound adaptor 이외에 gRPC 등의 Inbound Adaptor 를 추가함에 있어서 
      - 도메인 모델의 손상을 주지 않고 새로운 프로토콜에 기존 구현체를 적응시킬 수 있는가?
    - 분석단계에서의 유비쿼터스 랭귀지(업무현장에서 쓰는 용어)를 사용하여 소스코드가 서술되었는가?
  - Request-Response 방식의 서비스 중심 아키텍처 구현
    - 마이크로 서비스간 Request-Response 호출에 있어 대상 서비스를 어떠한 방식으로 찾아서 호출 하였는가? 
      - Service Discovery, REST, FeignClient
    - 서킷브레이커를 통하여  장애를 격리시킬 수 있는가?
  - 이벤트 드리븐 아키텍처의 구현
    - 카프카를 이용하여 PubSub 으로 하나 이상의 서비스가 연동되었는가?
    - Correlation-key
      - 각 이벤트 건 (메시지)가 어떠한 폴리시를 처리할때 어떤 건에 연결된 처리건인지를 구별하기 위한 
      - Correlation-key 연결을 제대로 구현 하였는가?
    - Message Consumer 마이크로서비스가 장애상황에서 수신받지 못했던 기존 이벤트들을 다시 수신받아 처리하는가?
    - Scaling-out
      - Message Consumer 마이크로서비스의 Replica 를 추가했을때 중복없이 이벤트를 수신할 수 있는가
    - CQRS
      - Materialized View 를 구현하여
      - 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이)도 
      - 내 서비스의 화면 구성과 잦은 조회가 가능한가?
  - 폴리글랏 플로그래밍
    - 각 마이크로 서비스들이 하나이상의 각자의 기술 Stack 으로 구성되었는가?
    - 각 마이크로 서비스들이 각자의 저장소 구조를 자율적으로 채택하고 각자의 저장소 유형을 선택하여 구현하였는가?
      - RDB, NoSQL, File System 등
  - API 게이트웨이
    - API GW를 통하여 마이크로 서비스들의 집입점을 통일할 수 있는가?
    - 게이트웨이와 인증서버(OAuth), JWT 토큰 인증을 통하여 마이크로서비스들을 보호할 수 있는가?
- 운영
  - SLA 준수
    - 셀프힐링
      - Liveness Probe 를 통하여 어떠한 서비스의 health 상태가 지속적으로 저하됨에 따라 
      - 어떠한 임계치에서 pod 가 재생되는 것을 증명할 수 있는가?
    - 서킷브레이커, 레이트리밋 등을 통한 장애격리와 성능효율을 높힐 수 있는가?
    - 오토스케일러 (HPA) 를 설정하여 확장적 운영이 가능한가?
    - 모니터링, 앨럿팅: 
  - 무정지 운영 CI/CD (10)
    - Readiness Probe 의 설정과 Rolling update을 통하여 
      - 신규 버전이 완전히 서비스를 받을 수 있는 상태일때 신규버전의 서비스로 전환됨을 siege 등으로 증명 
    - Contract Test :  자동화된 경계 테스트를 통하여 구현 오류나 API 계약위반를 미리 차단 가능한가?

# 분석/설계

## AS-IS 조직 (Horizontally-Aligned)
![as_is](https://user-images.githubusercontent.com/76420081/120093786-886b9700-c157-11eb-9775-fd865b4cb781.png)

## TO-BE 조직 (Vertically-Aligned)
![to_be](https://user-images.githubusercontent.com/76420081/120093800-9ae5d080-c157-11eb-8058-99b87b60bddd.png)

## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  
  - [GasStation.zip](https://github.com/longsawyer/GasStation/files/6565788/GasStation.zip)

### 이벤트
![Event](https://user-images.githubusercontent.com/76420081/120093917-4e4ec500-c158-11eb-9764-8414454df296.png)

### 어그리게잇
![Policy](https://user-images.githubusercontent.com/76420081/120093951-7807ec00-c158-11eb-9b06-b73fe786264d.png)
- 처리내역
  - 주문, 재고, 계정, 판매, 배송 등을 정의함

### 폴리시,커맨드
![Command](https://user-images.githubusercontent.com/76420081/120093981-a38ad680-c158-11eb-8291-0f90f28e126a.png)
![Command](https://user-images.githubusercontent.com/76420081/120094000-b9989700-c158-11eb-99dc-9461762d92e7.png)

### 액터
![actor](https://user-images.githubusercontent.com/76420081/120094009-c5845900-c158-11eb-86c0-d35a72043817.png)

### 바운디드 컨텍스트, 컨텍스트 매핑 (파란색점선 Pub/Sub, 빨간색실선 Req/Res)
![BoundedContext](https://user-images.githubusercontent.com/76420081/120094015-d2a14800-c158-11eb-9431-7ae2b46f8779.png)
-처리내역
  - 도메인 서열 분리 : 점포->주문-> 물류->판매 순으로 정리
       
### 1차 완료
![firstDesign](https://user-images.githubusercontent.com/76420081/120094067-22800f00-c159-11eb-8572-0c2be3148bdc.png)

### 2차 수정
![2ndDesign](https://user-images.githubusercontent.com/76420081/120094078-375ca280-c159-11eb-9585-e9b75b84611f.png)
    - 판매취소에 대한 보상처리 추가

### 요구사항 검증 (기능적/비기능적 )
#### 주문(기능)
![1stReview](https://user-images.githubusercontent.com/76420081/120095402-cf11bf00-c160-11eb-81cc-8c05c45744dc.png)
- 처리내역
    - 주문에 따른 입고예정까지 잘 오는지 확인
    - 최종 입고확정시, 주문시스템의 주문상태 변경되는지 확인

#### 상품마스터(기능)
![2ndReview](https://user-images.githubusercontent.com/76420081/120095417-ddf87180-c160-11eb-9845-bbde16214946.png)
- 처리내역
    - 본사(주문)시스템에서 가격정책 변경시, 점포=>판매시스템까지 잘 변경되는지 확인
    
#### 상품마스터(기능)
![2ndReview](https://user-images.githubusercontent.com/76420081/120094350-ccac6680-c15a-11eb-8c51-75855ca1eb5b.png)
- 처리내역
    - 본사(주문)시스템에서 가격정책 변경시, 점포=>판매시스템까지 잘 변경되는지 확인
    
#### 판매처리(비기능)
![3rdReview](https://user-images.githubusercontent.com/76420081/120095251-edc38600-c15f-11eb-85c8-51ee546f242e.png)
- 처리내역
    - 판매 즉시 재고에 반영
    - 점포시스템등은 본사와 별개로 운영가능

#### 판매취소에 따른 보상처리
![3rdReview](https://user-images.githubusercontent.com/76420081/120095305-33804e80-c160-11eb-9f47-63e337db2c01.png)
- 처리내역
    - 판매취소되면, 보상처리로 취소된 판매분만큼 재고를 증가시킨다
    - 판매취소되면, 보상처리로 취소된 판매분만큼 매출집계에서 제외한다

## 헥사고날 아키텍처 다이어그램 도출
![hexagonal1](https://user-images.githubusercontent.com/76420081/120095897-7c85d200-c163-11eb-868d-a802b71a1386.png)


## 신규 서비스 추가 시 기존 서비스에 영향이 없도록 열린 아키택처 설계
- 신규 개발 조직 추가시
  - 기존의 마이크로 서비스에 수정이 발생하지 않도록 Inbund 요청을 REST 가 아닌 Event를 Subscribe 하는 방식으로 구현하였다.
- 기존 마이크로 서비스에 대하여 아키텍처, 데이터베이스 구조와 관계 없이 추가할 수 있다.
- 예시는, 위탁수수료 정산시스템이다
  - 매장의 종류에는 가맹점과 직영점이 있다
  - 직영점의 경우 본사로부터 운영을 위탁맡은 관리인이 운영한다
  - 위탁관리인은 판매내역에 근거하여, 매장운영에 대한 대가를 위탁수수료를 정산받는다

![hexagonal2](https://user-images.githubusercontent.com/76420081/120096055-4a28a480-c164-11eb-86d6-4fc52c86db3e.png)

# 구현:
분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 8083 이다)

  - Local
```
cd Order
mvn spring-boot:run

cd Station
mvn spring-boot:run

cd POS
mvn spring-boot:run

cd Logistics
mvn spring-boot:run

- EKS : CI/CD 통해 빌드/배포 ("운영 > CI-CD 설정" 부분 참조)
```

## DDD 의 적용

- 각 서비스내에 도출된 핵심 Aggregate Root 객체를 Entity 로 선언하였다
  - Order, Shipment, StockFlow, Sale, Product, Account
- Order(주문) 마이크로서비스 예시

```
package gasstation;
...
import gasstation.event.Ordered;

/**
 * 주문
 */
@Entity
@Table(name="T_ORDER")
public class Order {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long 	orderId;
    private String 	productId;		// 유종
    private String 	productName;	// 유종명
    private Double 	qty;			// 수량			
    private String 	destAddr;		// 배송지
    private String 	orderDate;		// 주문일자
    
    @PostPersist
    public void onPostPersist(){
        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();
    }
    ...
}
```

REST API 테스트
1. 카프카 기동
    - zookeeper
      - zookeeper-server-start.bat ../../config/zookeeper.properties
    - kafka
      - kafka-server-start.bat ../../config/server.properties --override delete.topic.enable=true
    - MQ모니터링
      - kafka-console-consumer.bat --bootstrap-server http://localhost:9092 --topic gasstation --from-beginning
2. 서버기동(Order:주문)
    - 서버<br>
      ![image](https://user-images.githubusercontent.com/76420081/120097803-772d8500-c16d-11eb-8f22-f2690f0cf473.png)
    - Kafka MQ
      ![image](https://user-images.githubusercontent.com/76420081/120097876-cb386980-c16d-11eb-9202-e80d1098e595.png)
    - Mongo DB
      ![image](https://user-images.githubusercontent.com/76420081/120097908-ef944600-c16d-11eb-9210-568d957c6add.png)
3. 서버기동(Station:점포)
    - 서버<br>
      ![image](https://user-images.githubusercontent.com/76420081/120097957-397d2c00-c16e-11eb-96be-11bac161ce9d.png)
    - Kafka MQ
      ![image](https://user-images.githubusercontent.com/76420081/120097960-48fc7500-c16e-11eb-8b20-5094dec3fc6d.png)
    - H2 Console
      ![image](https://user-images.githubusercontent.com/76420081/120097991-7cd79a80-c16e-11eb-9923-37c002b24826.png)
4. 서버기동(POS:판매)
    - 서버<br>
      ![image](https://user-images.githubusercontent.com/76420081/120098048-abee0c00-c16e-11eb-9d15-655dc9b2c7b4.png)
    - Kafka MQ
      ![image](https://user-images.githubusercontent.com/76420081/120098056-b9a39180-c16e-11eb-8773-c00643492a46.png)   
    - H2 Console
      ![image](https://user-images.githubusercontent.com/76420081/120098074-db047d80-c16e-11eb-819a-ce08ab83da00.png)
4. 서비기동(물류)
    - 서버<br>
      ![image](https://user-images.githubusercontent.com/76420081/120098113-05eed180-c16f-11eb-867d-600740325c12.png)
    - Kafka MQ
      ![image](https://user-images.githubusercontent.com/76420081/120098118-0dae7600-c16f-11eb-90c9-bc946a66e35f.png)
    - H2 Console
      ![image](https://user-images.githubusercontent.com/76420081/120098137-29198100-c16f-11eb-9b6d-92c66bb06642.png)
5. 주문(전)
    - 주문<br>
      ![image](https://user-images.githubusercontent.com/76420081/120098187-6bdb5900-c16f-11eb-8747-962404a0441e.png)
      ![image](https://user-images.githubusercontent.com/76420081/120098202-7d246580-c16f-11eb-85ff-9e0895025fd5.png)
    - 물류
      ![image](https://user-images.githubusercontent.com/76420081/120098223-93cabc80-c16f-11eb-9634-8ddaeaf1d725.png)
    - 점포
      ![image](https://user-images.githubusercontent.com/76420081/120098240-a6dd8c80-c16f-11eb-8e8d-efedcf2b8b66.png)
      ![image](https://user-images.githubusercontent.com/76420081/120098249-b9f05c80-c16f-11eb-808a-0e7668492a81.png)
6. 주문(후)
    - http -f POST http://localhost:8083/orders/placeOrder productId=CD1001 qty=20000 destAddr="SK Imme Station" <br>
      ![image](https://user-images.githubusercontent.com/76420081/120098287-050a6f80-c170-11eb-8486-5383b4b0fd12.png)
      ![image](https://user-images.githubusercontent.com/76420081/120099730-1ce5f180-c178-11eb-99aa-7abc9c1b775e.png)
    - 주문<br>
      ![image](https://user-images.githubusercontent.com/76420081/120098323-3be08580-c170-11eb-917d-b6164fae6ee7.png)
      ![image](https://user-images.githubusercontent.com/76420081/120098946-ae069980-c173-11eb-8d60-fb424f49a1e5.png)
    - 물류<br>
      ![image](https://user-images.githubusercontent.com/76420081/120099271-8e707080-c175-11eb-81e7-100289eab8cb.png)
    - 점포<br>
      ![image](https://user-images.githubusercontent.com/76420081/120098382-88c45c00-c170-11eb-92b3-c3b434c6627e.png)
7. 주문확정
    - http -f POST http://localhost:8082/stocks/confirmStock orderId=1 <br>
    ![image](https://user-images.githubusercontent.com/76420081/120098546-7e569200-c171-11eb-89c8-371fbc49550e.png)
    ![image](https://user-images.githubusercontent.com/76420081/120099755-32f3b200-c178-11eb-9d45-c66ede426b45.png)
    - 점포<br>
    ![image](https://user-images.githubusercontent.com/76420081/120098566-9b8b6080-c171-11eb-8109-3e4498ef481d.png)
    - 주문<br>
    ![image](https://user-images.githubusercontent.com/76420081/120099773-4e5ebd00-c178-11eb-8bf1-cbcab534980b.png)


## 폴리글랏 퍼시스턴스
- Order,Station,POS,Logistics 서비스 모두 H2 메모리DB를 적용하였다.  
- Order의 경우 로컬개발환경에서 MongoDB를 추가하였다 (상품마스터만 활용)
- 다양한 데이터소스 유형 (RDB or NoSQL) 적용 시 데이터 객체에 @Entity 가 아닌 @Document로 마킹 후, 
  - 기존의 Entity Pattern / Repository Pattern 적용과 데이터베이스 제품의 설정 (application.yml) 만으로 가능하다.

order시스템 설정예시

- application.yml - mongodb 설정
```
spring:
  profiles: default
  ...
          
  #H2콘솔창 http://localhost:8083/h2-console
  h2:
    console:
      enabled: true 
      
  #MongoDB
  data:
    mongodb:
      uri: mongodb://localhost:27017/tutorial
```

- Repository 설정
```
...
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="product", path="product")
//public interface ProductRepository extends PagingAndSortingRepository<Product, Long>{
//}

public interface ProductRepository extends MongoRepository<Product, String>{
	Optional<Product> findByProductId( @Param("product_id") String productId);
}
```

- VO설정
```
...
import org.springframework.data.mongodb.core.mapping.Document;

//@Entity
//@Table(name="T_PRODUCT_M")
@Document(collection = "T_PRODUCT_M")
public class Product {

    @Id
    private String id;
    //@GeneratedValue(strategy=GenerationType.AUTO)
    private String 	productId;		// 상품ID
    private String 	productName;	// 상품명
    private Long 	price;			// 가격

    /**
     * 상품정보 생성
     */
    @PostPersist
    public void onPostPersist(){
        ProductChanged productChanged = new ProductChanged();
        BeanUtils.copyProperties(this, productChanged);
        productChanged.publishAfterCommit();
    }
    
    /**
     * 상품정보 변경
     */
    @PostUpdate
    public void onPostUpdate() {
    	 ProductChanged productChanged = new ProductChanged();
         BeanUtils.copyProperties(this, productChanged);
         productChanged.publishAfterCommit();
    }
    
    /**
     * 강제전송용
     */
    public void fireEvent() {
    	 ProductChanged productChanged = new ProductChanged();
         BeanUtils.copyProperties(this, productChanged);
         productChanged.publish();
    }
    ...
}

```
![image](https://user-images.githubusercontent.com/76420081/120100025-81ee1700-c179-11eb-83b4-b5b674793f44.png)

## 동기식 호출과 Fallback 처리

- POS에서 매출처리시, 즉시 점포시스템(BOS)에서 즉시 재고감소처리를 한다.
  - 동기식 처리
    - 이부분은 POS에 심어진 FeignClient를 이용하여 점포시스템의 재고 REST서비스를 초출하도록 한다. 
- POS에서 매출취소처리하면, 이에 대한 보정처리로 보상거래를 태운다(fallback처리)

설치 서비스를 호출하기 위하여 Stub과 (FeignClient) 를 이용하여 Service 대행 인터페이스 (Proxy) 를 구현
- POS - StationService.java
```
...
@FeignClient(name="Station", url="${external.url}")
public interface StationService {
    @RequestMapping(path="/stockFlows", method= RequestMethod.POST)
    public boolean outcome(@RequestBody StockFlow stockFlow);
}
```

- POS - application.yml
```
server:
  port: 8080
---

spring:
  profiles: default
  ...
server:
  port: 8081
external:
  url: http://localhost:8082
---

spring:
  profiles: docker
  ...
external:
  url: http://Station:8080
```

- POS 매출취소에 대한, 점포시 재고보정
```
package gasstation.policy;
...

@Service
public class PolicyHandler{
    private Logger logger = LoggerFactory.getLogger(getClass());
	
    @Autowired StockFlowRepository stockFlowRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired ProductMasterRepository productMasterRepository;
	...
    
    /**
     * 주문이 취소되면, 재고를 다시 증가시킨다
     * @param canceledSold
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceledSold_cancelStock(@Payload CanceledSold canceledSold){

        if(!canceledSold.validate()) return;

        logger.info("\n\n##### listener CanceledSold : " + canceledSold.toJson() + "\n\n");

        // 재고흐름 추가
        StockFlow stockFlow = new StockFlow();
        BeanUtils.copyProperties(canceledSold, stockFlow);
        
        // 입고처리 ( 판매는 마이너스처리이므로...)
        stockFlowRepository.save(stockFlow);
    }
	...
}
```

- POS 매출취소에 대한, 점포시 매출집계보정
```
package gasstation.policy;
...

/**
 * 판매뷰 핸들러
 *
 */
@Service
public class SalesSummaryViewHandler {
	private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SalesSummaryRepository salesSummaryRepository;
     ...
    
    /**
     * 주문이 취소되면, 판매감소한다
     * @param canceledSold
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceledSold_cancelStock(@Payload CanceledSold canceledSold){

    	if(!canceledSold.validate()) return;

        logger.info("\n\n##### listener ChangeSalesSummary : " + canceledSold.toJson() + "\n\n");
        
        Optional<SalesSummary> opt =salesSummaryRepository.findById(canceledSold.getProductId());
        if( opt.isPresent()) {
        	SalesSummary salesSummary =opt.get();
        	salesSummary.addQty(	-1* canceledSold.getQty());
        	salesSummary.addAmount(	-1* canceledSold.getAmount());
        	salesSummaryRepository.save(salesSummary);
        } else {
        	logger.error("감소시길 재고집계내역이 없음");
        }
    }
    ...
}

```

- POS 매출취소에 대한, 점포시 재고집계보정
```
package gasstation.policy;
...
@Service
public class StockSummaryViewHandler {
    private Logger logger =LoggerFactory.getLogger(getClass());
    @Autowired	private StockSummaryRepository stockSummaryRepository;
    @Autowired	private StockFlowController stockFlowController;
    
    /**
     * 주문이 취소되면, 재고를 다시 증가시킨다
     * @param canceledSold
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCanceledSold_cancelStock(@Payload CanceledSold canceledSold){
        if(!canceledSold.validate()) return;
        logger.info("\n\n##### listener CanceledSold : " + canceledSold.toJson() + "\n\n");

        // 재고흐름 추가
        StockFlow stockFlow = new StockFlow();
        BeanUtils.copyProperties(canceledSold, stockFlow);
        
        // DAO를 쓰지 않고, 재고관련 프로세스가 있는 cmd controll을 쓴다
        stockFlowController.outcome(stockFlow);
    }
    ...
}
```

## 비동기식 호출 / 시간적 디커플링 / 장애격리 / 최종 (Eventual) 일관성 테스트

주문이 이루어진후, 물류시스템에 비동기로 알려준다. 
- 비동기식으로 처리되므로 물류 배송처리를 위해서 주문이 블로킹되지 않는다
- 이를 위해서 주문이 이루어진후, 주문 도메인이벤트를 카프카로 송출한다.

구현
- 주문
```
@Entity
@Table(name="T_ORDER")
public class Order {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long 	orderId;
    private String 	productId;		// 유종
    private String 	productName;	// 유종명
    private Double 	qty;			// 수량			
    private String 	destAddr;		// 배송지
    private String 	orderDate;		// 주문일자
    
    @PostPersist
    public void onPostPersist(){
        Ordered ordered = new Ordered();
        BeanUtils.copyProperties(this, ordered);
        ordered.publishAfterCommit();
    }
    ...
}
```

- 물류시스템은 주문됨 이벤트를 PolicyHandler로 수신한다
```
@Service
public class PolicyHandler{
    private Logger logger =Logger.getGlobal();
    @Autowired ShipmentRepository shipmentRepository;

    /**
     * 주문이 발생했을때, 자동으로 배송발생
     * @param ordered
     */
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverOrdered_RequestOrder(@Payload Ordered ordered){

        if(!ordered.validate()) return;

        logger.info("\n\n##### listener RequestOrder : " + ordered.toJson() + "\n\n");

        Shipment shipment = new Shipment();
        BeanUtils.copyProperties(ordered, shipment);
        // 차량번호는 임의생성
        shipment.setCarNumber("CAR#" + Math.round( Math.random()*1000));
        shipmentRepository.save(shipment);
    }
    ...
}
```

- 물류시스템은 배송처리되면, 배송됨 이벤트를 카프카에 송출한다
```
@Entity
@Table(name="T_SHIPMENT")
public class Shipment {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long shipId;
    private Long orderId;
    private String carNumber;
    private String destAddr;
    private String productId;
    private String productName;
    private Double qty;

    @PostPersist
    public void onPostPersist(){
        Shipped shipped = new Shipped();
        BeanUtils.copyProperties(this, shipped);
        shipped.publishAfterCommit();
    }
    ...
}
```

- 점포시스템은 배송됨 이벤트를 PolicyHandler로 수신한다
```
@Service
public class PolicyHandler{
    private Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired StockFlowRepository stockFlowRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired ProductMasterRepository productMasterRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverShipped_ReserveIncome(@Payload Shipped shipped){
        if(!shipped.validate()) return;

        logger.info("\n\n##### listener ReserveIncome : " + shipped.toJson() + "\n\n");

        // 재고흐름 추가
        StockFlow stockFlow = new StockFlow();
        BeanUtils.copyProperties(shipped, stockFlow);
        
        // 아직 입고확정을 하지 않는다
        // 입고확정은 사용자에게 받는다
        stockFlow.setQtyToBe( stockFlow.getQty());
        stockFlow.setQty( 0.0);
        stockFlowRepository.save(stockFlow);
    }
    ...
}
```

주문신청은 주문,물류,점포 시스템과 분리되어 있으며, 중간 MQ이벤트 수신에 따라처리된다. 
- 물류/점포 시스템이 내려가도 주문처리에 이상없다
- 물류/주문 시스템이 내려가도 점포의 판매/재고처리에 영향받지 않는다

## CQRS
주문상태 조회서비스를 CQRS 패턴으로 구현하였다
- Order,Station,Logistics 개별 aggregate 통합 조회로 인한 성능 저하를 막을 수 있다.
- 모든 정보는 비동기 방식으로처리된다. 즉 발행된 이벤트를 수신하여 처리된다.
- 설계: MSAEz 설계의 view 매핑 설정 참조

1. 주문(전)
    - 주문<br>
      ![image](https://user-images.githubusercontent.com/76420081/120098202-7d246580-c16f-11eb-85ff-9e0895025fd5.png)
    - 점포
2. 주문(후)
    - http -f POST http://localhost:8083/orders/placeOrder productId=CD1001 qty=20000 destAddr="SK Imme Station" <br>
      ![image](https://user-images.githubusercontent.com/76420081/120098287-050a6f80-c170-11eb-8486-5383b4b0fd12.png)
      ![image](https://user-images.githubusercontent.com/76420081/120099730-1ce5f180-c178-11eb-99aa-7abc9c1b775e.png)
    - 주문<br>
      ![image](https://user-images.githubusercontent.com/76420081/120098323-3be08580-c170-11eb-917d-b6164fae6ee7.png)
      ![image](https://user-images.githubusercontent.com/76420081/120098946-ae069980-c173-11eb-8d60-fb424f49a1e5.png)
    - 물류<br>
    - 점포<br>
3. 주문확정
    - http -f POST http://localhost:8082/stocks/confirmStock orderId=1 <br>
    ![image](https://user-images.githubusercontent.com/76420081/120099755-32f3b200-c178-11eb-9d45-c66ede426b45.png)
    - 점포<br>
    - 주문<br>
    ![image](https://user-images.githubusercontent.com/76420081/120099773-4e5ebd00-c178-11eb-8bf1-cbcab534980b.png)
    

## API Gateway

API Gateway를 통하여, 마이크로 서비스들 진입점을 통일한다.
- application.yml 파일에 라우팅 경로 설정
```
spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: pos
          uri: http://pos:8080
          predicates:
            - Path=/sales/**,/prodcutMenus/**,cancelSales/**
        - id: station
          uri: http://station:8080
          predicates:
            - Path=/stockFlows/**,/accounts/**,/productMasters/**,/stockSummaries/**,/salesSummaries/**
        - id: order
          uri: http://order:8080
          predicates:
            - Path=/orders/**,/prodcuts/**,/orderStatuses/**
        - id: logistics
          uri: http://logistics:8080
          predicates:
            - Path=/shipments/** 
      globalcors:
        corsConfigurations:
          '[/**]':
            allowedOrigins:
              - "*"
            allowedMethods:
              - "*"
            allowedHeaders:
              - "*"
            allowCredentials: true

server:
  port: 8080
```

- EKS에 배포 시, MSA는 Service type을 ClusterIP(default)로 설정하여, 클러스터 내부에서만 호출 가능하도록 한다.
- API Gateway는 Service type을 LoadBalancer로 설정하여 외부 호출에 대한 라우팅을 처리한다.

# 운영(작성)
- AWS: https://github.com/longsawyer/gasstation/blob/main/aws.md
- minikube: https://github.com/longsawyer/gasstation/blob/main/minikube.md

# 운영
## CI/CD 설정
### 빌드/배포(AWS)
각 프로젝트 jar를 Dockerfile을 통해 Docker Image 만들어 ECR저장소에 올린다.   
EKS 클러스터에 접속한 뒤, 각 서비스의 deployment.yaml, service.yaml을 kuectl명령어로 서비스를 배포한다.   
  - 코드 형상관리 : https://github.com/longsawyer/gasstation 하위 repository에 각각 구성   
  - 운영 플랫폼 : AWS의 EKS(Elastic Kubernetes Service)   
  - Docker Image 저장소 : AWS의 ECR(Elastic Container Registry)
  
##### 배포 명령어(AWS)
```
cd /home/project/gasstation;
git pull;
kubectl delete deploy,svc,pod --all
	
cd /home/project/gasstation/Order;mvn package -B;
cd /home/project/gasstation/Order;docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-order:v1 .;
cd /home/project/gasstation/Order;docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-order:v1;
cd /home/project/gasstation/Order/kubernetes/;
kubectl apply -f deployment.yml;
kubectl apply -f service.yaml;

cd /home/project/gasstation/Logistics;mvn package -B;
cd /home/project/gasstation/Logistics;docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-logistics:v1 .;
cd /home/project/gasstation/Logistics;docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-logistics:v1;
cd /home/project/gasstation/Logistics/kubernetes/;
kubectl apply -f deployment.yml;
kubectl apply -f service.yaml;

cd /home/project/gasstation/POS;mvn package -B;
cd /home/project/gasstation/POS;docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-pos:v1 .;
cd /home/project/gasstation/POS;docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-pos:v1;
cd /home/project/gasstation/POS/kubernetes/;
kubectl apply -f deployment.yml;
kubectl apply -f service.yaml;

cd /home/project/gasstation/Station;mvn package -B;
cd /home/project/gasstation/Station;docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-station:v1 .;
cd /home/project/gasstation/Station;docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-station:v1;
cd /home/project/gasstation/Station/kubernetes/;
kubectl apply -f deployment.yml;
kubectl apply -f service.yaml;

cd /home/project/gasstation/gateway;mvn package -B;
cd /home/project/gasstation/gateway;docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-gateway:v1 .;
cd /home/project/gasstation/gateway;docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-gateway:v1;
kubectl create deploy gateway --image=879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-gateway:v1
kubectl expose deployment gateway --type=LoadBalancer --port=8080
```

##### 배포 결과(AWS)
![image](https://user-images.githubusercontent.com/76420081/120580317-ef44c500-c463-11eb-9e8c-4cc108b576b7.png)


## 동기식호출 /서킷브레이킹 /장애격리
- 서킷 브레이킹 프레임워크의 선택
  - Spring FeignClient + Hystrix 옵션을 사용하여 구현할 경우, 도메인 로직과 부가 기능 로직이 서비스에 같이 구현된다.
  - istio를 사용해서 서킷 브레이킹 적용이 가능하다.


- istio 설치
```
cd /home/project
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.7.1 TARGET_ARCH=x86_64 sh -
cd istio-1.7.1
export PATH=$PWD/bin:$PATH
istioctl install --set profile=demo --set hub=gcr.io/istio-release
kubectl label namespace default istio-injection=enabled 
배포다시

확인
kubectl get all -n istio-system 
```
![image](https://user-images.githubusercontent.com/76420081/120580372-0be0fd00-c464-11eb-8c78-f155f1f96b7b.png)


- kiali 설치<br>
```
vi samples/addons/kiali.yaml
	4라인의 apiVersion: 
		apiextensions.k8s.io/v1beta1을 apiVersion: apiextensions.k8s.io/v1으로 수정

kubectl apply -f samples/addons
	kiali.yaml 오류발생시, 아래 명령어 실행
		kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.7/samples/addons/kiali.yaml

kubectl edit svc kiali -n istio-system
	:%s/ClusterIP/LoadBalancer/g
	:wq!
EXTERNAL-IP 확인
	kubectl get all -n istio-system 
모니터링 시스템(kiali) 접속 
	EXTERNAL-IP:20001 (admin/admin)
```
![image](https://user-images.githubusercontent.com/76420081/120581869-7f840980-c466-11eb-8b89-d45f6be6dbd0.png)<br>
![image](https://user-images.githubusercontent.com/76420081/120581856-7a26bf00-c466-11eb-8f8c-b733ecd01ac1.png)
![image](https://user-images.githubusercontent.com/76420081/120586763-0fc64c80-c46f-11eb-9510-e145e2632da7.png)
![image](https://user-images.githubusercontent.com/76420081/120582008-b5c18900-c466-11eb-96a4-b0e140c4edb0.png)
![image](https://user-images.githubusercontent.com/76420081/120581769-55324c00-c466-11eb-9244-8088cfabeb70.png)


- istio 에서 서킷브레이커 설정(DestinationRule)
```
cat <<EOF | kubectl apply -f -
apiVersion: networking.istio.io/v1alpha3
kind: DestinationRule
metadata:
  name: order
spec:
  host: order
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 1           # 목적지로 가는 HTTP, TCP connection 최대 값. (Default 1024)
      http:
        http1MaxPendingRequests: 1  # 연결을 기다리는 request 수를 1개로 제한 (Default 
        maxRequestsPerConnection: 1 # keep alive 기능 disable
        maxRetries: 3               # 기다리는 동안 최대 재시도 수(Default 1024)
    outlierDetection:
      consecutiveErrors: 5          # 5xx 에러가 5번 발생하면
      interval: 1s                  # 1초마다 스캔 하여
      baseEjectionTime: 30s         # 30 초 동안 circuit breaking 처리   
      maxEjectionPercent: 100       # 100% 로 차단
EOF

```

- 부하테스터 siege 툴을 통한 서킷 브레이커 동작을 확인한다.
  - 동시사용자 100명
  - 60초 동안 실시
  - 결과 화면
![image](https://user-images.githubusercontent.com/76420081/120583151-a6433f80-c468-11eb-81f7-f9a022c23148.png)
![image](https://user-images.githubusercontent.com/76420081/120583180-ae9b7a80-c468-11eb-97cc-5c6368d8bf29.png)

```
siege -c100 -t60S  -v 'http://a532a43b1b8b845799bc8adb11b6f8ec-234283.ap-northeast-2.elb.amazonaws.com:8080/orders/placeOrder POST productId=CD1001&qty=20000&destAddr=SK_Imme_Station'
```

- 서킷 브레이커 DestinationRule 삭제 
  - management에 적용된 서킷 브레이커 DestinationRule을 삭제하고 다시 부하를 주어 결과를 확인한다.    
```
kubectl delete dr --all;
siege -c100 -t60S  -v 'http://a532a43b1b8b845799bc8adb11b6f8ec-234283.ap-northeast-2.elb.amazonaws.com:8080/orders/placeOrder POST productId=CD1001&qty=20000&destAddr=SK_Imme_Station'
```

  - 아래와 같이 management서비스에서 모든 요청을 처리하여 200응답을 주는것을 확인하였다.
  ![image](https://user-images.githubusercontent.com/76420081/120588830-d55eae80-c472-11eb-8b7a-847d23d94a8e.png)
  ![image](https://user-images.githubusercontent.com/76420081/120588943-0b039780-c473-11eb-8641-a325aafe2676.png)

### Liveness
pod의 container가 정상적으로 기동되는지 확인하여, 비정상 상태인 경우 pod를 재기동하도록 한다.   

아래의 값으로 liveness를 설정한다.
- 재기동 제어값 : /tmp/healthy 파일의 존재를 확인
- 기동 대기 시간 : 3초
- 재기동 횟수 : 5번까지 재시도

이때, 재기동 제어값인 /tmp/healthy파일을 강제로 지워 liveness가 pod를 비정상 상태라고 판단하도록 하였다.    
5번 재시도 후에도 파드가 뜨지 않았을 경우 CrashLoopBackOff 상태가 됨을 확인하였다.   

##### order에 Liveness 적용한 내용

yaml
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
  labels:
    app: order
spec:
...
    spec:
      containers:
        - name: order
          image: 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-order:v1
          args:
          - /bin/sh
          - -c
          - touch /tmp/healthy; sleep 10; rm -rf /tmp/healthy; sleep 600;
          ports:
            - containerPort: 8080
          livenessProbe:
            exec:
              command:
              - cat
              - /tmp/healthy
            initialDelaySeconds: 3
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 5
          env:
        ...

```

- 확인
```
kubectl get pods -w
```
![image](https://user-images.githubusercontent.com/76420081/120587508-726c1800-c470-11eb-8327-2d84f896c839.png)
![image](https://user-images.githubusercontent.com/76420081/120587737-e3abcb00-c470-11eb-82ca-745ccd031651.png)

```
kubectl get pods
```
![image](https://user-images.githubusercontent.com/76420081/120587885-28376680-c471-11eb-976d-cfd865c817e5.png)



### 오토스케일 아웃
- 주문 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다.
- 설정은 CPU 사용량이 10프로를 넘어서면 replica 를 10개까지 늘려준다.
  - 쿠버 
```
kubectl autoscale deploy order --min=1 --max=10 --cpu-percent=10
```

  - deployment.yml
```
apiVersion: apps/v1
kind: Deployment
...
    spec:
      containers:
        - name: order
          resources:
            limits: 
              cpu: 500m
            requests:
              cpu: 200m
          image: 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user03-order:v1
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어준다.
```
kubectl get deploy order -w
kubectl get hpa order -w
```

- 사용자 50명으로 워크로드를 3분 동안 걸어준다.
```
siege -r 2000 -c 200 -v -v 'http://a532a43b1b8b845799bc8adb11b6f8ec-234283.ap-northeast-2.elb.amazonaws.com:8080/orders/placeOrder POST  productId=CD1001&qty=20000&destAddr=SK_Imme_Station'
```

- 오토스케일 발생하지 않음(siege 실행 결과 오류 없이 수행됨 : Availability 100%)
- autoscale 대상이 unknown이며, 확인결과 메트릭스 서버 설치필요함
![image](https://user-images.githubusercontent.com/76420081/120590433-7babb380-c475-11eb-9cf7-2def459901b4.png)


- 매트릭스 서버설치후
```
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
kubectl get deployment metrics-server -n kube-system
```
![image](https://user-images.githubusercontent.com/76420081/120592242-7c921480-c478-11eb-8018-e88b4e3d6b89.png)
![image](https://user-images.githubusercontent.com/76420081/120592349-ac411c80-c478-11eb-8b8e-dc123a237b61.png)
![image](https://user-images.githubusercontent.com/76420081/120592438-d85c9d80-c478-11eb-86f7-cac0ca6f6373.png)
![image](https://user-images.githubusercontent.com/76420081/120592581-122da400-c479-11eb-8ac7-2b8051bc22bb.png)

기타명령(필요시)
```
kubectl get hpa
kubectl delete hpa NAME-OF-HPA
```
![image](https://user-images.githubusercontent.com/76420081/120591176-b104d100-c476-11eb-8f8e-128cfd100474.png)


## 무정지 재배포
* 먼저 무정지 재배포가 100% 되는 것인지 확인하기 위해서 Autoscaler 이나 서킷브레이커 설정을 제거함
- siege 로 배포작업 직전에 워크로드를 모니터링 한다.
```
siege -c50 -t180S  -v 'http://a532a43b1b8b845799bc8adb11b6f8ec-234283.ap-northeast-2.elb.amazonaws.com:8080/orders/placeOrder POST  productId=CD1001&qty=20000&destAddr=SK_Imme_Station'
```
- readinessProbe, livenessProbe 설정되지 않은 상태로 buildspec.yml을 수정한다.
- Github에 buildspec.yml 수정 발생으로 CodeBuild 자동 빌드/배포 수행된다.
- siege 수행 결과 : 

- readinessProbe, livenessProbe 설정하고 buildspec.yml을 수정한다.
- Github에 buildspec.yml 수정 발생으로 CodeBuild 자동 빌드/배포 수행된다.
- siege 수행 결과 : 


## ConfigMap적용
- 설정을 외부주입하여 변경할 수 있다
- order에서 사용할 상점코드(주유소코드)를 넣는다

```
## configmap.yml
apiVersion: v1
kind: ConfigMap
metadata:
  name: order
data:
  stationCode: "ST0001"
```

## Secret적용
- username, password와 같은 민감한 정보는 ConfigMap이 아닌 Secret을 적용한다.
- etcd에 암호화 되어 저장되어, ConfigMap 보다 안전하다.
- value는 base64 인코딩 된 값으로 지정한다. (echo root | base64)
- order에서 사용할 상점명(주유소명)를 넣는다

```
echo -n 'SK Imme' | base64
LW4gJ1NLIEltbWUnIA0K
```

```
## secret.yml
apiVersion: v1
kind: Secret
metadata:
  name: order
type: Opaque
data:
  stationName: LW4gJ1NLIEltbWUnIA0K
```

## ConfigMap/Secret 적용내용

- deployment.yml
```
apiVersion: apps/v1
kind: Deployment
metadata:
  name: order
  labels:
    app: order
spec:
  replicas: 1
  selector:
    matchLabels:
      app: order
  template:
    metadata:
      labels:
        app: order
    spec:
      containers:
        - name: order
          image: laios/order:3
          imagePullPolicy: Never
          ports:
            - containerPort: 8080
          env:
          - name: station_nm
            valueFrom:
              secretKeyRef:
                name: order
                key: stationName
          - name: station_cd
            valueFrom:
              configMapKeyRef:
                name: order
                key: stationCode
```

- 테스트코드
```
/**
 * 점포명 출력
 * @return
 */
@RequestMapping(value = "/orders/station", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
public String station() {
	logger.info("### 점포=" + System.getenv().get("station_nm") + ", " + System.getenv().get("station_cd"));
	return System.getenv().get("station_nm") + ", " + System.getenv().get("station_cd");
}
```

- 설정적용: minikube에서 테스트한 내역
```
cd D:\projects\gasstation\kube
kubectl apply -f .\secret.yml
kubectl apply -f .\configmap.yml

http -f POST http://127.0.0.1:8080/orders/station
```
![image](https://user-images.githubusercontent.com/76420081/120335516-7db62b00-c32c-11eb-9441-4b74b4b4c16d.png)<br>
![image](https://user-images.githubusercontent.com/76420081/120338870-9542e300-c32f-11eb-8ca9-6b290be4f719.png)



## 운영 모니터링

### 쿠버네티스 구조
쿠버네티스는 Master Node(Control Plane)와 Worker Node로 구성된다.
![image](https://user-images.githubusercontent.com/64656963/86503139-09a29880-bde6-11ea-8706-1bba1f24d22d.png)


### 1. Master Node(Control Plane) 모니터링
Amazon EKS 제어 플레인 모니터링/로깅은 Amazon EKS 제어 플레인에서 계정의 CloudWatch Logs로 감사 및 진단 로그를 직접 제공한다.

- 사용할 수 있는 클러스터 제어 플레인 로그 유형은 다음과 같다.
```
  - Kubernetes API 서버 컴포넌트 로그(api)
  - 감사(audit) 
  - 인증자(authenticator) 
  - 컨트롤러 관리자(controllerManager)
  - 스케줄러(scheduler)

출처 : https://docs.aws.amazon.com/ko_kr/eks/latest/userguide/logging-monitoring.html
```

- 제어 플레인 로그 활성화 및 비활성화
```
기본적으로 클러스터 제어 플레인 로그는 CloudWatch Logs로 전송되지 않습니다. 
클러스터에 대해 로그를 전송하려면 각 로그 유형을 개별적으로 활성화해야 합니다. 
CloudWatch Logs 수집, 아카이브 스토리지 및 데이터 스캔 요금이 활성화된 제어 플레인 로그에 적용됩니다.

출처 : https://docs.aws.amazon.com/ko_kr/eks/latest/userguide/control-plane-logs.html
```

### 2. Worker Node 모니터링

- 쿠버네티스 모니터링 솔루션 중에 가장 인기 많은 것은 Heapster와 Prometheus 이다.
- Heapster는 쿠버네티스에서 기본적으로 제공이 되며, 클러스터 내의 모니터링과 이벤트 데이터를 수집한다.
- Prometheus는 CNCF에 의해 제공이 되며, 쿠버네티스의 각 다른 객체와 구성으로부터 리소스 사용을 수집할 수 있다.

- 쿠버네티스에서 로그를 수집하는 가장 흔한 방법은 fluentd를 사용하는 Elasticsearch 이며, fluentd는 node에서 에이전트로 작동하며 커스텀 설정이 가능하다.

- 그 외 오픈소스를 활용하여 Worker Node 모니터링이 가능하다. 아래는 istio, mixer, grafana, kiali를 사용한 예이다.

```
아래 내용 출처: https://bcho.tistory.com/1296?category=731548

```
- 마이크로 서비스에서 문제점중의 하나는 서비스가 많아 지면서 어떤 서비스가 어떤 서비스를 부르는지 의존성을 알기가 어렵고, 각 서비스를 개별적으로 모니터링 하기가 어렵다는 문제가 있다. Istio는 네트워크 트래픽을 모니터링함으로써, 서비스간에 호출 관계가 어떻게 되고, 서비스의 응답 시간, 처리량등의 다양한 지표를 수집하여 모니터링할 수 있다.

![image](https://user-images.githubusercontent.com/64656963/86347967-ff738380-bc99-11ea-9b5e-6fb94dd4107a.png)

- 서비스 A가 서비스 B를 호출할때 호출 트래픽은 각각의 envoy 프록시를 통하게 되고, 호출을 할때, 응답 시간과 서비스의 처리량이 Mixer로 전달된다. 전달된 각종 지표는 Mixer에 연결된 Logging Backend에 저장된다.

- Mixer는 위의 그림과 같이 플러그인이 가능한 아답터 구조로, 운영하는 인프라에 맞춰서 로깅 및 모니터링 시스템을 손쉽게 변환이 가능하다.  쿠버네티스에서 많이 사용되는 Heapster나 Prometheus에서 부터 구글 클라우드의 StackDriver 그리고, 전문 모니터링 서비스인 Datadog 등으로 저장이 가능하다.

![image](https://user-images.githubusercontent.com/64656963/86348023-14501700-bc9a-11ea-9759-a40679a6a61b.png)

- 이렇게 저장된 지표들은 여러 시각화 도구를 이용해서 시각화 될 수 있는데, 아래 그림은 Grafana를 이용해서 서비스의 지표를 시각화 한 그림이다.

![image](https://user-images.githubusercontent.com/64656963/86348092-25992380-bc9a-11ea-9d7b-8a7cdedc11fc.png)

- 그리고 근래에 소개된 오픈소스 중에서 흥미로운 오픈 소스중의 하나가 Kiali (https://www.kiali.io/)라는 오픈소스인데, Istio에 의해서 수집된 각종 지표를 기반으로, 서비스간의 관계를 아래 그림과 같이 시각화하여 나타낼 수 있다.  아래는 그림이라서 움직이는 모습이 보이지 않지만 실제로 트래픽이 흘러가는 경로로 에니메이션을 이용하여 표현하고 있고, 서비스의 각종 지표, 처리량, 정상 여부, 응답 시간등을 손쉽게 표현해 준다.

![image](https://user-images.githubusercontent.com/64656963/86348145-3a75b700-bc9a-11ea-8477-e7e7178c51fe.png)


# 시연
 1. 주문/재고주문신청 -> 물류/배송처리 -> 점포/입고예정처리
 2. 점포/입고완료처리 -> 주문/배송완료(received)처리
 3. POS/판매처리 -> 점포/재고감소,판매집계
 4. POS/판매취소 -> 점포/보상처리:재고증가,판매집계취소
 5. EDA 구현
   - Order(본사)시스템 장애상황에서 POS판매처리 정상처리
   - Order(본사)시스템 정상 전환시, 수신받지 못한 이벤트 처리 예)가격변경
 6. 무정지 재배포
 7. 오토 스케일링
