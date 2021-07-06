package concertbooking;

import concertbooking.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PolicyHandler{

    @Autowired ConcertRepository concertRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverBookingCancelled_ModifyStock(@Payload BookingCancelled bookingCancelled){

        if(!bookingCancelled.validate()) return;

        System.out.println("\n\n##### listener ModifyStock : " + bookingCancelled.toJson() + "\n\n");

    // BIZ로직 
    Optional<Concert> concert = concertRepository.findById(bookingCancelled.getCcId()); 

    if(concert.isPresent()){

            Concert concertValue = concert.get();
                    
            concertValue.setStock(concertValue.getStock() + bookingCancelled.getQty()); 
            System.out.println("##### change  stock: " + concertValue.getStock() + "#####" +  bookingCancelled.getQty());
            concertRepository.save(concertValue);
            
        }
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
