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
  2.  콘서트 관리자는 티켓을 등록한다.
  3.  고객은 콘서트를 예매한다.
  4.  고객은 콘서트 예매를 취소할 수 있다.
  5.  콘서트 좌석을 초과하여 예매할 수 없다.
  6.  고객이 예매를 하면, 예매가능한 티켓 수량이 감소한다.
  7.  결제가 완료되면 티켓을 배송한다.
  8.  결제가 취소되면, 배송이 취소된다. 
  9.  이벤트가 발생하면, 고객에게 알림 메시지가 간다.
  10. 고객은 예매정보를 확인 할 수 있다.

# 비기능적 요구사항
1. 트랜잭션
    1. 예매 수량은 좌석 수량을 초과하여 예약 할 수 없다. (Sync 호출)
1. 장애격리
    1. 배송 시스템이 수행되지 않더라도 예매 기능은 365일 24시간 받을 수 있어야 한다. Async (event-driven), Eventual Consistency
    1. 예약시스템이 과중 되면 사용자를 잠시동안 받지 않고 예약을 잠시후에 하도록 유도한다. Circuit breaker, fallback
1. 성능
    1. 고객은 MyPage에서 본인 예매 상태를 확인 할 수 있어야 한다. (CQRS)


# 분석/설계
## Event Storming 결과
* MSAEz 로 모델링한 이벤트스토밍 결과:  http://www.msaez.io/#/storming/wCwpUDxVtrWKc54qAaeMyXoCFoT2/mine/72cb437f5c71c03d9ed514225311dda7


### 이벤트 도출
![image](https://user-images.githubusercontent.com/85874443/122223814-8000b380-ceee-11eb-93a3-3c69d431226b.png)

### 부적격 이벤트 제거
![image](https://user-images.githubusercontent.com/85874443/122864914-70ef8a80-d360-11eb-94ff-a5da8b2dc82a.png)

### 액터/커맨드 부착
![image](https://user-images.githubusercontent.com/85874443/122864929-751ba800-d360-11eb-8e06-0ec85ea7baca.png)

### 어그리게잇으로 묶기
![image](https://user-images.githubusercontent.com/85874443/122864938-7947c580-d360-11eb-9312-73744c6c9d01.png)

### 바운디드 컨텍스트로 묶기
![image](https://user-images.githubusercontent.com/85874443/122864950-7e0c7980-d360-11eb-8940-cc3573bac9e2.png)

### 이벤트스토밍 최종 결과
![image](https://user-images.githubusercontent.com/85874443/122227160-9ceab600-cef1-11eb-9dee-52b2f63dd9ac.png)

### 시나리오 요구사항 check
<img width="100%" height="100%" alt="hex" src="https://user-images.githubusercontent.com/85874443/122320354-0fdd4680-cf5d-11eb-9ab2-1bfcf9ac89e9.PNG">

### 헥사고날 아키텍처 다이어그램 도출
<img width="1447" alt="hex2" src="https://user-images.githubusercontent.com/85874443/122849190-fca7ed80-d345-11eb-8a40-654f41365d2a.PNG">


# 구현:

분석/설계 단계에서 도출된 헥사고날 아키텍처에 따라, 각 BC별로 대변되는 마이크로 서비스들을 스프링부트와 파이선으로 구현하였다. 구현한 각 서비스를 로컬에서 실행하는 방법은 아래와 같다 (각자의 포트넘버는 8081 ~ 808n 이다)

```
cd view
mvn spring-boot:run

cd booking
mvn spring-boot:run 

cd concert
mvn spring-boot:run  

```

## DDD 의 적용
이벤트 스토밍을 통해 도출된 Micro Service 는 총 6개이나, 3개만 구현하였으며 그 중 View는 CQRS를 위한 서비스이다.

|MSA|기능|port|URL|
| :--: | :--: | :--: | :--: |
|concert| 티켓정보 관리 |8081|http://localhost:8081/concerts|
|booking| 티켓예매 관리 |8082|http://localhost:8082/bookings|
|view| 콘서트 예매내역 조회 |8086|http://localhost:8086/mypages|


- AWS에 gateway 등록
![gateway](https://user-images.githubusercontent.com/85874443/122735509-0f74e080-d2ba-11eb-84ef-6438b66c62f2.PNG)


- concert 서비스의 티켓등록
![concert](https://user-images.githubusercontent.com/85874443/122735425-fc621080-d2b9-11eb-89a8-bb5f727ee13a.PNG)


- booking 서비스의 예매
![booking](https://user-images.githubusercontent.com/85874443/122735451-0257f180-d2ba-11eb-9194-a871828eb95b.PNG)



Gateway 적용
API GateWay를 통하여 마이크로 서비스들의 진입점을 통일할 수 있다. 
다음과 같이 GateWay를 적용하였다.

```yaml
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
        - id: Alarm
          uri: http://Alarm:8080
          predicates:
            - Path=/alarms/** 
        - id: Delivery
          uri: http://Delivery:8080
          predicates:
            - Path=/deliveries/** 
        - id: Payment
          uri: http://Payment:8080
          predicates:
            - Path=/payments/** 
        - id: View
          uri: http://View:8080
          predicates:
            - Path= /mypages/**
```  

## CQRS
Materialized View 를 구현하여, 타 마이크로서비스의 데이터 원본에 접근없이(Composite 서비스나 조인SQL 등 없이) 도 내 서비스의 화면 구성과 잦은 조회가 가능하게 구현해 두었다.
본 프로젝트에서 Mypage 역할은 view 서비스가 수행한다.

모든 정보는 비동기 방식으로 발행된 이벤트(예매, 예매 취소)를 수신하여 처리된다.

예매(Booked) 실행
 
![image](https://user-images.githubusercontent.com/85874443/122846091-17776380-d340-11eb-87e6-fb330d787236.PNG)

카프카 메시지

![ka1](https://user-images.githubusercontent.com/85874443/122853258-e6516000-d34c-11eb-9783-37814741be1c.PNG)

예매(Booked) 실행 후 mypage 화면

![image](https://user-images.githubusercontent.com/85874443/122846131-2a8a3380-d340-11eb-851e-be9df34e5cdf.PNG)
  
## 폴리글랏 퍼시스턴스
concert 서비스의 DB 를 HSQL 로 설정하여 MSA간 서로 다른 종류의 DB간에도 문제 없이 동작하여 다형성을 만족하는지 확인하였다.

|서비스|DB|pom.xml|
| :--: | :--: | :--: |
|concert| HSQL |![concert_hsqldb](https://user-images.githubusercontent.com/85874443/122845192-15aca080-d33e-11eb-8dc8-79974d3b77e6.PNG)|
|booking| H2 |![booking_h2db](https://user-images.githubusercontent.com/85874443/122845208-1c3b1800-d33e-11eb-998c-e6bf5ada128a.PNG)|
|view| H2 |![booking_h2db](https://user-images.githubusercontent.com/85874443/122845208-1c3b1800-d33e-11eb-998c-e6bf5ada128a.PNG)|


## 동기식 호출과 Fallback 처리
분석단계에서의 조건 중 하나로  콘서트 티켓 예약수량은 등록된 티켓 수량을 초과 할 수 없으며
예약(Booking)->콘서트(Concert) 간의 호출은 동기식 일관성을 유지하는 트랜잭션으로 처리하기로 하였다. 
호출 프로토콜은 Rest Repository 에 의해 노출되어있는 REST 서비스를 FeignClient 를 이용하여 호출하도록 한다.



Booking  내 external.ConcertService

```java
package concertbooking.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name="Concert", url="http://localhost:8081")
public interface ConcertService {

    @RequestMapping(method= RequestMethod.GET, path="/checkAndBookStock")
    public boolean checkAndBookStock(@RequestParam("ccId") Long ccId , @RequestParam("qty") int qty);

}
```

Booking 서비스 내 Req/Resp

```java
    @PostPersist
    public void onPostPersist() throws Exception{
        
        
        boolean rslt = BookingApplication.applicationContext.getBean(concertbooking.external.ConcertService.class)
            .checkAndBookStock(this.getCcId(), this.getQty());

            if (rslt) {
                Booked booked = new Booked();
                BeanUtils.copyProperties(this, booked);
                booked.publishAfterCommit();
            }  
            else{
                throw new Exception("Out of Stock Exception Raised.");
            }      
        

    }
```

Concert 서비스 내 Booking 서비스 Feign Client 요청 대상

```java
@RestController
public class ConcertController {

@Autowired
ConcertRepository concertRepository;

@RequestMapping(value = "/checkAndBookStock",
        method = RequestMethod.GET,
        produces = "application/json;charset=UTF-8")

public boolean checkAndBookStock(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
     
        System.out.println("##### /concert/checkAndBookStock  called #####");

        boolean status = false;
        
        Long ccId = Long.valueOf(request.getParameter("ccId"));
        int qty = Integer.parseInt(request.getParameter("qty"));

        System.out.println("##### ccid #####" + ccId +"##### qty" + qty);
        Optional<Concert> concert = concertRepository.findById(ccId);
        
        if(concert.isPresent()){

                Concert concertValue = concert.get();

                if (concertValue.getStock() >= qty) {
                        concertValue.setStock(concertValue.getStock() - qty);
                        concertRepository.save(concertValue);
                        status = true;
                        System.out.println("##### /concert/checkAndBookStock  qty check true ##### stock"+concertValue.getStock()+"### qty"+ qty);
                }

                System.out.println("##### /concert/checkAndBookStock  qty check false ##### stock"+concertValue.getStock()+"### qty"+ qty);
        }

        return status;
        }
        
 }
```

공연 정보를 등록함

![concert](https://user-images.githubusercontent.com/85874443/122849383-61634800-d346-11eb-8d6d-73c09867dc17.PNG)



티켓을 예매함
![booking](https://user-images.githubusercontent.com/85874443/122849272-252fe780-d346-11eb-8ee5-51469a470115.PNG)


티켓 예매를 취소함
![cancle](https://user-images.githubusercontent.com/85874443/122849246-1a755280-d346-11eb-9455-e7a4de36cf12.PNG)


# 운영

## Deploy/Pipeline
각 구현체들은 각자의 source repository 에 구성되었고, 각 서비스별로 빌드를 하여, aws ecr에 등록 후 deployment.yaml 통해 EKS에 배포함.

- git에서 소스 가져오기

```
git clone --recurse-submodules https://github.com/skteam4/concert/concertbooking.git
```

- Build 하기

```bash
cd /alarm
cd gateway
mvn package

cd ..
cd booking
mvn package

cd ..
cd concert
mvn package

cd ..
cd delivery
mvn package

cd ..
cd payment
mvn package
```

- aws 이미지 캡처

<img width="705" alt="aws_repository" src="https://user-images.githubusercontent.com/85874443/122850409-1f3b0600-d348-11eb-8ebd-e3653bafe919.PNG">


<img width="682" alt="aws_book_tag" src="https://user-images.githubusercontent.com/85874443/122850413-2235f680-d348-11eb-807f-b2aef08c24ff.PNG">


- concert/booking/kubernetes/deployment.yml 파일 

```yml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: booking
  labels:
    app: booking
spec:
  replicas: 1
  selector:
    matchLabels:
      app: booking
  template:
    metadata:
      labels:
        app: booking
    spec:
      containers:
        - name: booking
          image: xxxxxx.dkr.ecr.ca-central-1.amazonaws.com/booking:v4
          ports:
            - containerPort: 8080
          readinessProbe:
            httpGet:
              path: '/actuator/health'
              port: 8080
            initialDelaySeconds: 10
            timeoutSeconds: 2
            periodSeconds: 5
            failureThreshold: 10
          livenessProbe:
```	  


***



## Autoscale

- metric 서버를 설치한다.

```sh
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/download/v0.3.7/components.yaml
kubectl get deployment metrics-server -n kube-system
```

- 예약 서비스에 리소스에 대한 사용량을 정의한다.

<code>booking/kubernetes/deployment.yml</code>

```yml
  resources:
    requests:
      memory: "64Mi"
      cpu: "250m"
    limits:
      memory: "500Mi"
      cpu: "500m"
```

- 예약 서비스에 대한 replica 를 동적으로 늘려주도록 HPA 를 설정한다. 설정은 CPU 사용량이 20프로를 넘어서면 replica 를 3개까지 늘려준다:

```sh
$ kubectl autoscale deploy booking --min=1 --max=3 --cpu-percent=20
```

- CB 에서 했던 방식대로 워크로드를 걸어준다.

```sh
siege -c20 -t40S -v --content-type "application/json" 'http://localhost:8082/bookings POST {“ccId”:"1", "ccName":"mong", "ccDate:"20210621", “qty”:”2" ,”customerId”:"6007" , "bookingStatus":"success"}'
```

- 오토스케일이 어떻게 되고 있는지 모니터링을 걸어둔다:

```sh
$ kubectl get deploy booking -w
```

- 어느정도 시간이 흐른 후 스케일 아웃이 벌어지는 것을 확인할 수 있다:

* siege 부하테스트 - 후 1

![hpa](https://user-images.githubusercontent.com/85874443/122758180-76eb5a00-d2d3-11eb-9618-e2005145b0de.PNG)


* siege 부하테스트 - 후 2


![scaleout_최종](https://user-images.githubusercontent.com/85874443/122758323-a13d1780-d2d3-11eb-8687-fc39ef7008a5.PNG)


## Circuit Breaker

  * 서킷 브레이킹 프레임워크의 선택: Spring FeignClient + Hystrix 설치
  * 시나리오는 예약(booking) >> 콘서트(concert) 연결을 RESTful Request/Response 로 연동하여 구현이 되어있고, 예약 요청이 과도할 경우 CB 를 통하여 장애격리
  * Booking 서비스 내 XX에 FeignClient 에 적용
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
$ siege -c20 -t40S -v --content-type "application/json" 'http://localhost:8082/bookings POST {"ccId":1, "ccName":"mong", "ccDate":"20210621", "qty":2 ,"customerId":6007 ,"bookingStatus":"success"}'
```

- fallback 설정

![fallback설정](https://user-images.githubusercontent.com/85874443/122866266-9d0c0b00-d362-11eb-92ca-43179c843e30.PNG)
![fallback함수](https://user-images.githubusercontent.com/85874443/122866315-b4e38f00-d362-11eb-8437-dd24f46977eb.PNG)


- Hystrix 설정 + fallback 설정 전

  ![Hystrix설정후_fallback설정전](https://user-images.githubusercontent.com/85874443/122845849-899b7880-d33f-11eb-8f9b-e266db0afde1.PNG)

  
- Hystrix 설정 + fallback 설정 후

  ![Hystrix설정전_fallback설정후](https://user-images.githubusercontent.com/85874443/122845630-172a9880-d33f-11eb-9aec-5592f9a56ee3.PNG)

- 부하를 줬을 때 fallback 설정 전에는 500 에러가 발생했으나, fallback 설정 이후에는 100% 정상적으로 처리함

***

## Zero-Downtime deploy (Readiness Probe)

- deployment.yml에 정상 적용되어 있는 readinessProbe  
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

- readiness 적용 전. booking이 배포되는 중  
  ![update_version_80%](https://user-images.githubusercontent.com/85874443/122764789-c84b1780-d2da-11eb-951c-b6058f77b208.PNG)


- 다시 readiness 정상 적용 후, Availability 100% 확인  
  ![update_version_100%](https://user-images.githubusercontent.com/85874443/122764804-ce40f880-d2da-11eb-83fa-af8a85d8431b.PNG)


    
## Self-healing (Liveness Probe)

- deployment.yml에 정상 적용되어 있는 livenessProbe  

```yml
livenessProbe:
  httpGet:
    path: '/actuator/health'
    port: 8080
  initialDelaySeconds: 120
  timeoutSeconds: 2
  periodSeconds: 5
  failureThreshold: 5
```

- port 및 path 잘못된 값으로 변경 후, retry 시도 확인 
    - booking 에 있는 deployment.yml 수정  
        ![livenessProbe_yaml](https://user-images.githubusercontent.com/85874443/122760461-1c073200-d2d6-11eb-8db8-c25c6ef9abb4.png)


    - retry 시도 확인  
        ![livenessProbe](https://user-images.githubusercontent.com/85874443/122760301-ecf0c080-d2d5-11eb-9da5-bd39c7867e24.png)

