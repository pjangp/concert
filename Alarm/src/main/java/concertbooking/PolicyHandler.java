package concertbooking;

import concertbooking.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired AlarmRepository alarmRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBooked_EventReceive(@Payload Booked booked){

        if(!booked.validate()) return;

        System.out.println("\n\n##### listener EventReceive : " + booked.toJson() + "\n\n");

        // Sample Logic //
        Alarm alarm = new Alarm();
        alarmRepository.save(alarm);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBookingCancelled_EventReceive(@Payload BookingCancelled bookingCancelled){

        if(!bookingCancelled.validate()) return;

        System.out.println("\n\n##### listener EventReceive : " + bookingCancelled.toJson() + "\n\n");

        // Sample Logic //
        Alarm alarm = new Alarm();
        alarmRepository.save(alarm);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCreated_EventReceive(@Payload PaymentCreated paymentCreated){

        if(!paymentCreated.validate()) return;

        System.out.println("\n\n##### listener EventReceive : " + paymentCreated.toJson() + "\n\n");

        // Sample Logic //
        Alarm alarm = new Alarm();
        alarmRepository.save(alarm);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentCancelled_EventReceive(@Payload PaymentCancelled paymentCancelled){

        if(!paymentCancelled.validate()) return;

        System.out.println("\n\n##### listener EventReceive : " + paymentCancelled.toJson() + "\n\n");

        // Sample Logic //
        Alarm alarm = new Alarm();
        alarmRepository.save(alarm);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPointEarned_EventReceive(@Payload PointEarned pointEarned){

        if(!pointEarned.validate()) return;

        System.out.println("\n\n##### listener EventReceive : " + pointEarned.toJson() + "\n\n");

        // Sample Logic //
        Alarm alarm = new Alarm();
        alarmRepository.save(alarm);
            
    }
    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPointDeducted_EventReceive(@Payload PointDeducted pointDeducted){

        if(!pointDeducted.validate()) return;

        System.out.println("\n\n##### listener EventReceive : " + pointDeducted.toJson() + "\n\n");

        // Sample Logic //
        Alarm alarm = new Alarm();
        alarmRepository.save(alarm);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
