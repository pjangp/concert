package concertbooking;

import concertbooking.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional; 

@Service
public class PolicyHandler{
    @Autowired PointRepository pointRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCreated_EarnPoint(@Payload PaymentCreated paymentCreated){

        if(paymentCreated.validate()){

            System.out.println("\n\n##### listener EarnPoint : " + paymentCreated.toJson() + "\n\n");

            long customerId = paymentCreated.getCustomerId();
            long bookingId = paymentCreated.getBookingId();
            Integer savedPoint =0;
            savedPoint = (int) Math.round((paymentCreated.getPrice()-paymentCreated.getUsedPoint())*0.01);

            updatePoint(customerId, bookingId, savedPoint, -1*savedPoint);// Point Update
        }
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCancelled_DeductPoint(@Payload PaymentCancelled paymentCancelled){

        if(paymentCancelled.validate()){

            System.out.println("\n\n##### listener DeductPoint : " + paymentCancelled.toJson() + "\n\n");

            long customerId = paymentCancelled.getCustomerId();
            Integer usedPoint = paymentCancelled.getUsedPoint();
            Integer savedPoint =0;
            savedPoint = (int) Math.round((paymentCancelled.getPrice()-paymentCancelled.getUsedPoint())*0.01);

            updatePoint(customerId, '-', 0, savedPoint);
            updatePoint(customerId, '-', 0,-1*usedPoint);
        }    
    }


//    @StreamListener(KafkaProcessor.INPUT)
//    public void whatever(@Payload String eventString){}

    private void updatePoint(long customerId, long bookingId, Integer savedPoint, Integer usedPoint) {

        //////////////////////////////////////////////
        // 사용자의 Point 관리
        //////////////////////////////////////////////

        // Concert 테이블에서 ccId의 Data 조회
        Optional<Point> res = pointRepository.findById(customerId);
        Point point = res.get();

        System.out.println("customserId    : " + point.getCustomerId());
        System.out.println("Total Point : " + point.getPointTotal());

        point.setBookingId(bookingId);
        point.setSavedPoint(savedPoint);
        point.setPointTotal(point.getPointTotal() - usedPoint);

        /////////////
        // DB Update
        /////////////
        pointRepository.save(point);
    }
}
