# concert

# Table of contents

- [콘서트 예매 시스템](#---)
  - [서비스 시나리오](#서비스-시나리오)

# 서비스 시나리오
- [기능적 요구사항](#기능적-요구사항)
- [비기능적 요구사항](#비기능적-요구사항)

# 기능적 요구사항
  1.  콘서트 관리자는 콘서트 정보를 등록한다.
  2.  콘서트 관리자는 티켓을 등록한다.
  3.  고객은 콘서트를 예매한다.
  4.  고객은 콘서트 예매를 취소할 수 있다.
  5.  콘서트 좌석을 초과하여 예매할 수 없다.
  6.  고객이 예매를 하면, 예매가능한 티켓 수량이 감소한다.
  7.  고객이 콘서트 현장에서 티켓 발급 또한, 우편 수령 후 티켓 발급 완료 처리가 된다.
  8.  고객은 예매정보를 확인 할 수 있다.
  9.  결제가 완료되면 티켓을 배송한다.
  10. 결제가 취소되면, 배달이 취소된다.
  11. 고객에게 알림 메시지가 간다.

# 비기능적 요구사항
1. 트랜잭션
    1. 예매 수량은 좌석 수량을 초과하여 예약 할 수 없다. (Sync 호출)
1. 장애격리
    1. 배송 시스템이 수행되지 않더라도 예매 기능은 365일 24시간 받을 수 있어야 한다. Async (event-driven), Eventual Consistency
    1. 예약시스템이 과중 되면 사용자를 잠시동안 받지 않고 예약을 잠시후에 하도록 유도한다. Circuit breaker, fallback
1. 성능
    1. 고객은 MyPage에서 본인 예매 상태를 확인 할 수 있어야 한다. (CQRS)
    

