# concert

# Table of contents

- [콘서트 예매 시스템](#---)
  - [서비스 시나리오](#서비스-시나리오)
  - [분석/설계](#분석설계)
  - [구현:](#구현-)
    - [DDD 의 적용](#ddd-의-적용)
    - [Gateway 적용](#Gateway-적용)
    - [CQRS](#CQRS)
    - [폴리글랏 퍼시스턴스](#폴리글랏-퍼시스턴스)
    - [동기식 호출과 Fallback 처리](#동기식-호출과-Fallback-처리)
  - [운영](#운영)
    - [Deploy/Pipeline](#Deploy-Pipeline)
    - [Autoscale(HPA)](#Autoscale)
    - [Circuit Breaker](#Circuit-Breaker)
    - [Zero-Downtime deploy(Readiness Probe)](#Zero-Downtime-deploy(Readiness-Probe))
    - [Self-healing(Liveness Probe)](#Self-healing)

# 서비스 시나리오

# 기능적 요구사항
  1.  콘서트 관리자는 콘서트 정보를 등록한다. 
  2.  고객은 콘서트를 예매한다.
  3.  고객은 콘서트 예매를 취소할 수 있다.
  4.  콘서트 좌석을 초과하여 예매할 수 없다.
  5.  고객이 예매를 하면, 예매가능한 티켓 수량이 감소한다.
  6.  고객이 예매를 취소하면, 티겟 수량이 증가한다.
  7.  ~~결제가 완료되면 티켓을 배송한다.(팀과제)~~
  8.  ~~결제가 취소되면, 배송이 취소된다.(팀과제)~~
  9. 결제가 완료되면 포인트가 적립된다.(개인과제 추가)
  10. 결제가 취소되면 포인트가 차감된다.(개인과제 추가)
  11. 결제시 포인트로 결제가 가능하며 사용한 포인트만큼 포인트가 차감된다.(개인과제 추가)
  12. 포인트가 포함된 결제가 취소되면 사용한 포인트만큼 포인트가 재적립된다.(개인과제 추가)
  13.  이벤트가 발생하면, 고객에게 알림 메시지가 간다.
  14. 고객은 예매정보를 확인 할 수 있다.

# 비기능적 요구사항
1. 트랜잭션
    1. 예매 수량은 좌석 수량을 초과하여 예약 할 수 없다. (Sync 호출, 팀과제)
    2. 결제시 사용 포인트는 적립된 포인트를 초과할 수 없다.(Sync 호출, 개인과제)
1. 장애격리
    1. 배송 시스템이 수행되지 않더라도 예매 기능은 365일 24시간 받을 수 있어야 한다. Async (event-driven), Eventual Consistency
    1. 예약시스템이 과중 되면 사용자를 잠시동안 받지 않고 예약을 잠시후에 하도록 유도한다. Circuit breaker, fallback
1. 성능
    1. 고객은 MyPage에서 본인 예매 상태를 확인 할 수 있어야 한다. (CQRS)
      - 팀과제 : 예약, 예약 취소 상태까지만 구현
      - 개인과제 : 결제 여부, 마일리지 적립 현황까지 추가 구현


# 분석/설계
## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/wCwpUDxVtrWKc54qAaeMyXoCFoT2/mine/c10d211cdd3c6d198eb891fede354ae4


### 이벤트 도출
![이벤트도출](https://user-images.githubusercontent.com/82200734/124548304-7ffa3080-de68-11eb-9495-c87686178494.PNG)

### 부적격 이벤트 제거
![부적격이벤트제거](https://user-images.githubusercontent.com/82200734/124548344-8c7e8900-de68-11eb-9047-6ab1b9f35bd8.PNG)

### 액터/커맨드 부착
![커맨드부착](https://user-images.githubusercontent.com/82200734/124548436-ae780b80-de68-11eb-920c-e060d08d09c2.PNG)

### 어그리게잇으로 묶기
![어그리겟](https://user-images.githubusercontent.com/82200734/124548468-b9cb3700-de68-11eb-9e58-3d5801173409.PNG)

### 바운디드 컨텍스트로 묶기
![컨텍스트](https://user-images.githubusercontent.com/82200734/124548522-cc457080-de68-11eb-8b45-a8179b67a00d.PNG)

### 이벤트스토밍 최종 결과
![최종](https://user-images.githubusercontent.com/82200734/124548542-d5364200-de68-11eb-83c2-5328e4ec05ab.PNG)

### 시나리오 요구사항 check
![시나리오체크](https://user-images.githubusercontent.com/82200734/124548711-0dd61b80-de69-11eb-91fe-7599d7eed76d.PNG)

### 헥사고날 아키텍처 다이어그램 도출
![헥사고날](https://user-images.githubusercontent.com/82200734/124916396-98677800-e02d-11eb-954c-4d68e8fd571b.PNG)



# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd view
mvn spring-boot:run

cd booking
mvn spring-boot:run 

cd concert
mvn spring-boot:run  

cd payment
mvn spring-boot:run  

cd point
mvn spring-boot:run  

```

## DDD 의 적용
이벤트 스토밍을 통해 도출된 Micro Service 는 총 6개이나, 5개만 구현하였으며 그 중 View는 CQRS를 위한 서비스이다.

|과제유형|MSA|기능|port|URL|
| :--: | :--: | :--: | :--: | :--: |
|팀과제|concert| 콘서트정보 관리 |8081|http://localhost:8081/concerts|
|팀과제|booking| 티켓예매 관리 |8082|http://localhost:8082/bookings|
|팀과제|view| 콘서트 예매내역 조회 |8086|http://localhost:8086/mypages|
|개인과제|payment| 결제 처리 |8085|http://localhost:8085/payments|
|개인과제|point| point 관리 |8084|http://localhost:8084/points|

- concert 서비스의 콘서드 정보 등록
![콘서트 등록](https://user-images.githubusercontent.com/82200734/124563887-6c0bfa00-de7b-11eb-941c-3eb491025eae.PNG)

- booking 서비스의 예매
![예매](https://user-images.githubusercontent.com/82200734/124564061-a2497980-de7b-11eb-98ed-77d5d95f898b.PNG)

- payment 서비스의 결제
![결제](https://user-images.githubusercontent.com/82200734/124563982-87770500-de7b-11eb-9b76-4b18d42e13f8.PNG)

- point 서비스의 point 적립
![포인트 등록](https://user-images.githubusercontent.com/82200734/124564137-b7260d00-de7b-11eb-8a58-3849fff921e9.PNG)

- view 서비스의 mypage 조회
![마이페이지 결제후](https://user-images.githubusercontent.com/82200734/124567923-60bacd80-de7f-11eb-952e-62a74a2effc2.PNG)


Gateway 적용
API GateWay를 통하여 마이크로 서비스들의 진입점을 통일할 수 있다. 
다음과 같이 GateWay를 적용하였다.

```yaml
server:
  port: 8088

---

spring:
  profiles: default
  cloud:
    gateway:
      routes:
        - id: concert
          uri: http://localhost:8081
          predicates:
            - Path=/concerts/** 
        - id: booking
          uri: http://localhost:8082
          predicates:
            - Path=/bookings/** 
        - id: alarm
          uri: http://localhost:8083
          predicates:
            - Path=/alarms/** 
        - id: point
          uri: http://localhost:8084
          predicates:
            - Path=/points/**, /pointcheck/** 
        - id: payment
          uri: http://localhost:8085
          predicates:
            - Path=/payments/** 
        - id: view
          uri: http://localhost:8086
          predicates:
            - Path= /mypages/**
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


---

spring:
  profiles: docker
  cloud:
    gateway:
      routes:
        - id: concert
          uri: http://concert:8080
          predicates:
            - Path=/concerts/** 
        - id: booking
          uri: http://booking:8080
          predicates:
            - Path=/bookings/** 
        - id: alarm
          uri: http://alarm:8080
          predicates:
            - Path=/alarms/** 
        - id: point
          uri: http://point:8080
          predicates:
            - Path=/points/**, /pointcheck/** 
        - id: payment
          uri: http://payment:8080
          predicates:
            - Path=/payments/** 
        - id: view
          uri: http://view:8080
          predicates:
            - Path= /mypages/**
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
EKS에 배포 시, MSA는 Service type을 ClusterIP(default)로 설정하여, 클러스터 내부에서만 호출 가능하도록 한다.
API Gateway는 Service type을 LoadBalancer로 설정하여 외부 호출에 대한 라우팅을 처리한다.
![Gateway](https://user-images.githubusercontent.com/82200734/124864790-7bfb1980-dff4-11eb-89e5-df5f27cab903.PNG)


## CQRS
Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현해 두었다.
본 프로젝트에서 Mypage 역할은 view 서비스가 수행한다.

모든 정보는 비동기 방식으로 발행된 이벤트(팀과제 : 예매, 예매 취소 , 개인과제 결제 : 결제, 결제 취소, 포인트 적립)를 수신하여 처리된다.


예매(Booked) 실행
![예매](https://user-images.githubusercontent.com/82200734/124564704-4b906f80-de7c-11eb-9348-4a82def41198.PNG)

카푸카 메시지
![카푸 예매](https://user-images.githubusercontent.com/82200734/124564749-564b0480-de7c-11eb-8999-203d18b8321d.PNG)

예매(Booked) 실행 후 mypage 화면
![마이페이지 예매후](https://user-images.githubusercontent.com/82200734/124567985-6dd7bc80-de7f-11eb-9f64-e420fe122fc5.PNG)

결제(PaymentCreated) 실행
![결제](https://user-images.githubusercontent.com/82200734/124564990-8eeade00-de7c-11eb-970d-67172b68aa2f.PNG)

카푸카메시지
![카푸 결제](https://user-images.githubusercontent.com/82200734/124565045-9ad6a000-de7c-11eb-87e5-105232fc13f6.PNG)

결제(PaymentCreated) 실행 후 mypage 화면
![마이페이지 결제후](https://user-images.githubusercontent.com/82200734/124568244-abd4e080-de7f-11eb-9e50-4dcdca18b471.PNG)

  
## 폴리글랏 퍼시스턴스
concert, payment 서비스의 DB 를 HSQL 로 설정하여 MSA간 서로 다른 종류의 DB간에도 문제 없이 동작하여 다형성을 만족하는지 확인하였다.

|과제유형|서비스|DB|pom.xml|
| :--: | :--: | :--: | :--: |
|팀 과제|concert| HSQL |![concert_hsqldb](https://user-images.githubusercontent.com/85874443/122845192-15aca080-d33e-11eb-8dc8-79974d3b77e6.PNG)|
|팀 과제|booking| H2 |![booking_h2db](https://user-images.githubusercontent.com/85874443/122845208-1c3b1800-d33e-11eb-998c-e6bf5ada128a.PNG)|
|팀 과제|view| H2 |![booking_h2db](https://user-images.githubusercontent.com/85874443/122845208-1c3b1800-d33e-11eb-998c-e6bf5ada128a.PNG)|
|개인 과제|payment| HSQL |![concert_hsqldb](https://user-images.githubusercontent.com/85874443/122845192-15aca080-d33e-11eb-8dc8-79974d3b77e6.PNG)|
|개인 과제|point| H2 |![booking_h2db](https://user-images.githubusercontent.com/85874443/122845208-1c3b1800-d33e-11eb-998c-e6bf5ada128a.PNG)|


## 동기식 호출과 Fallback 처리
팀과제 : 콘서트 티켓 예약수량은 등록된 티켓 수량을 초과 할 수 없으며 예약(Booking)->콘서트(Concert) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다.

개인 과제 : 결제 시 Point를 사용할 수 있는데, 적립된 Point를 초과할 수 없으며 결제(payment)->포인트(point)간의  호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다.

호출 프로토콜은 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.

point 서비스 내 PointController.java 파일 서비스

```java
@RestController
 public class PointController {
    
    @Autowired
    PointRepository pointRepository;
    
    @RequestMapping(value = "/pointcheck/checkAndDeductPoint",
        method = RequestMethod.GET,
        produces = "application/json;charset=UTF-8")

        public boolean checkAndDeductPoint(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
                // Parameter로 받은 customerId 추출
                long customerId = Long.valueOf(request.getParameter("customerId"));
                Integer usedPoint = Integer.valueOf(request.getParameter("usedPoint"));
                System.out.println("######################## checkAndDeductPoint customerId : " + customerId);
                System.out.println("######################## checkAndDeductPoint use Point : " + usedPoint);

                // Point 데이터 조회
                Optional<Point> res = pointRepository.findById(customerId);
                Point point = res.get();
                System.out.println("######################## checkAndDeductPoint Saved Point : " + usedPoint);

                //point 체크
                boolean result = false;
                 if(point.getPointTotal() >= usedPoint ) {
                    point.setPointTotal(point.getPointTotal() - usedPoint);
                    pointRepository.save(point);
                        
                    result = true;
                 } 

                System.out.println("######################## checkAndDeductPoint Return : " + result);
                return result;
        }
 }
```

Payment 서비스  내 external.PointService.java 파일

```java
package concertbooking.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

@FeignClient(name="point", url="${prop.room.url}" , fallback = PointServiceImpl.class)
public interface PointService {

    @RequestMapping(method= RequestMethod.GET, path="pointcheck/checkAndDeductPoint")
    public boolean checkAndDeductPoint(@RequestParam("customerId") long customerId, @RequestParam("usedPoint") Integer usedPoint);

}
}
```

Payment 서비스 내 payment.java 파일

```java
    @PostPersist
    public void onPostPersist(){     

        boolean result = PaymentApplication.applicationContext.getBean(concertbooking.external.PointService.class)
        .checkAndDeductPoint(this.getCustomerId(), this.getUsedPoint());
        System.out.println("######## Check Result : " + result);

        if(result) {             
            PaymentCreated paymentCreated = new PaymentCreated();
            BeanUtils.copyProperties(this, paymentCreated);
            paymentCreated.setPaymentStatus("payCompleted");
            paymentCreated.publishAfterCommit();
        }
    }
```



# 운영

## Deploy/Pipeline
각 구현체들은 각자의 source repository 에 구성되었고, 각 서비스별로 빌드를 하여, aws ecr에 등록 후 deployment.yaml 통해 EKS에 배포함.

- git에서 소스 가져오기

```
git clone -b master https://github.com/pjangp/concert.git
```

- Build 하기

```bash
각 서비스(concert/booking/payment/point/view) 별로 build
cd 서비스명
mvn package -B
```

- aws ECS의 Repository 생성
![ECR](https://user-images.githubusercontent.com/82200734/124867231-a7800300-dff8-11eb-807a-24f8c8981b68.PNG)



- ECR, EKS 에 적용 
```yml

cd 서비스명
docker build -t 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user05-booking:v1 .;
docker push 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user05-booking:v1;

cd 서비스명/kubernetes
kubectl apply -f deployment.yml;
kubectl apply -f service.yaml;

```	  


***



## Autoscale

- metric 서버를 설치한다.

```sh
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.3.7/components.yaml
kubectl get deployment metrics-server -n kube-system
```

- 포인트 서비스에 리소스에 대한 사용량을 정의한다.resources.requests.cpu: "200m" 추가
  point/kubernetes/deployment.yml</code>

```yml
          resources:
            requests:
              cpu: "200m"  
```

- 포인트 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 20프로를 넘어서면 replica 를 3개까지 늘려준다:

```sh
$ kubectl autoscale deployment point --cpu-percent=20 --min=1 --max=3
```

- siege를 이용하여 워크로드를 걸어준다.

```sh
siege -c20 -t40S -v http://a5cb5ea9f93da4ef3b97d5048a02b76a-1240042388.ap-northeast-2.elb.amazonaws.com:8080/points
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:

```sh
$ kubectl get deploy point -w
```

* siege 부하테스트 중

![pod늘어남](https://user-images.githubusercontent.com/82200734/124875383-ca63e480-e003-11eb-8214-bdfa432a40b5.PNG)

```sh
$ kubectl get hpa
```
* siege 부하테스트 전

![hpa 부하전](https://user-images.githubusercontent.com/82200734/124875638-1a42ab80-e004-11eb-982d-ccd73f5f744a.PNG)

* siege 부하테스트 후

![hpa 늘어나고나서](https://user-images.githubusercontent.com/82200734/124875661-1f9ff600-e004-11eb-9157-98f54f332761.PNG)



## Circuit Breaker

  * 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 설치
  * 팀과제 : 시나리오는 예약(booking) >> 콘서트(concert) 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 예약 요청이 과도할 경우 CB 를 통하여 장애격리
  * 개인과제 : 결제(payment) >> 포인트(point) 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 포인트(point) 서비스에 문제가 있을 경우 CB 를 통하여 장애격리
  * Hystrix 설정

```yml
# application.yml

feign:
  hystrix:
    enabled: true

hystrix:
  command:
    default:
      execution.isolation.thread.timeoutInMilliseconds: 610
```

- 부하 테스트 수행
```sh
$ siege -c2 -t30S -v --content-type "application/json" 'http://localhost:8085/payments POST {"bookingId":1,"ccId":1,"ccName":"BRAVEGIRLS_1","price":50000,"usedPoint":0,"customerId":1}'
```

- fallback 설정
  PointService.java  
  ![fall1](https://user-images.githubusercontent.com/82200734/124917528-e8930a00-e02e-11eb-9ae6-e35567d50104.PNG)

  PointServiceImpl.java  
  ![fall2](https://user-images.githubusercontent.com/82200734/124917543-ed57be00-e02e-11eb-9eda-efd6af25c6ef.PNG)

- 부하를 줬을 때 500 에러가 발생하지 않고, fallback 함수에서 처리한 메시지가 발생되며, 100% 처리됨
  ![CB_siege](https://user-images.githubusercontent.com/82200734/124917704-1aa46c00-e02f-11eb-9d5c-291cdf73d501.PNG)
  ![CB_2](https://user-images.githubusercontent.com/82200734/124917720-1f692000-e02f-11eb-8067-d74ffd39d311.PNG)
 

***

## Zero-Downtime deploy (Readiness Probe)

- 포인트 서비스의 deployment.yml에 정상 적용되어 있는 readinessProbe  
```yml
readinessProbe:
  httpGet:
    path: '/actuator/health'
    port: 8080
  initialDelaySeconds: 10
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 10
```

- deployment.yml에서 readiness 설정 제거 후, 배포중 siege 테스트 진행
- kubectl delete deploy --all
- kubectl apply -f deployment.yml
- kubectl apply -f service.yaml

- readiness 적용 전. 포인트 서비스 배포되는 중  
![readiness  미적용](https://user-images.githubusercontent.com/82200734/124885894-b5408300-e00e-11eb-8980-c425159bf181.PNG)


- 다시 readiness 정상 적용 후, Availability 100% 확인  
![readiness  적용](https://user-images.githubusercontent.com/82200734/124885921-bc679100-e00e-11eb-9f1d-59351a8b27eb.PNG)



    
## Self-healing (Liveness Probe)

- deployment.yml에 정상 적용되어 있는 livenessProbe  
  
  livenessProbe에 'cat /tmp/healthy' 검증도록 함

```yml
    spec:
      containers:
        - name: point
          image: 879772956301.dkr.ecr.ap-northeast-2.amazonaws.com/user05-point:v2
          imagePullPolicy: Always
          args:
            - /bin/sh
            - -c
            - touch /tmp/healthy; sleep 10; rm -rf /tmp/healthy; sleep 600;
          ports:
            - containerPort: 8080
          resources:
            requests:
              cpu: "200m"  
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:            
            # httpGet:
            #   path: '/actuator/health'
            #   port: 8080
            exec:
              command:
                - cat
                - /tmp/healthy        
```

- Container 실행 이 후, /tmp/healthy 파일 삭제되고, LivenessProbe 실패 리턴함. 
    - container의 상태가 비정상이라고 판단하여 Pod를 재시작함  
    ![liveness](https://user-images.githubusercontent.com/82200734/124897471-4ddc0080-e019-11eb-84bf-0a47006c7229.PNG)


