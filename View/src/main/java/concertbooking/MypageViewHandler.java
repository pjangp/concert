package concertbooking;

import concertbooking.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class MypageViewHandler {


    @Autowired
    private MypageRepository mypageRepository;

    // 예매 했을 때
    @StreamListener(KafkaProcessor.INPUT)
    public void whenbooked_then_CREATE(@Payload Booked booked) {
        try {

            if (!booked.validate()) return;

            // view 객체 생성
            Mypage mypage = new Mypage();
            // view 객체에 이벤트의 Value 를 set 함
            mypage.setBookingId(booked.getBookingId());
            mypage.setCcId(booked.getCcId());
            mypage.setCcName(booked.getCcName());
            mypage.setQty(booked.getQty());
            mypage.setBookingStatus(booked.getBookingStatus());
            // view 레파지 토리에 save
            mypageRepository.save(mypage);
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }    

    // 예매 취소 했을 때
    @StreamListener(KafkaProcessor.INPUT)
    public void whenbookingCancelled_then_UPDATE(@Payload BookingCancelled bookingCancelled) {
        try {

            if (!bookingCancelled.validate()) return;
            Optional<Mypage> mypageOptional = mypageRepository.findById(bookingCancelled.getBookingId());
            if( mypageOptional.isPresent()) {
                Mypage mypage = mypageOptional.get();
                
                mypage.setBookingStatus(bookingCancelled.getBookingStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
            }
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }  
    
    //결제 완료 했을 때 
    @StreamListener(KafkaProcessor.INPUT)
    public void whenpaymentCreated_then_CREATE(@Payload PaymentCreated paymentCreated) {
        try {

            if (!paymentCreated.validate()) return;
            Optional<Mypage> mypageOptional = mypageRepository.findById(paymentCreated.getBookingId());
            if( mypageOptional.isPresent()) {
                Mypage mypage = mypageOptional.get();
                
                mypage.setPaymentStatus(paymentCreated.getPaymentStatus());
                mypage.setPrice(paymentCreated.getPrice());
                mypage.setUsedPoint(paymentCreated.getUsedPoint());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
            }
        
        }catch (Exception e){
            e.printStackTrace();
        }
    }    

    // 결제 취소 했을 때
    @StreamListener(KafkaProcessor.INPUT)
    public void whenpaymentCancelled_then_UPDATE(@Payload PaymentCancelled paymentCancelled) {
        try {

            if (!paymentCancelled.validate()) return;
            Optional<Mypage> mypageOptional = mypageRepository.findById(paymentCancelled.getBookingId());
            if( mypageOptional.isPresent()) {
                Mypage mypage = mypageOptional.get();
                
                mypage.setPaymentStatus(paymentCancelled.getPaymentStatus());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
            }
        
        }catch (Exception e){
            e.printStackTrace();
        }
    } 

    // 포인트 적립 됐을 때
    @StreamListener(KafkaProcessor.INPUT)
    public void whenpointEarned_then_UPDATE(@Payload PointEarned pointEarned) {
        try {

            if (!pointEarned.validate()) return;
            Optional<Mypage> mypageOptional = mypageRepository.findById(pointEarned.getBookingId());
            if( mypageOptional.isPresent()) {
                Mypage mypage = mypageOptional.get();
                
                mypage.setSavedPoint(pointEarned.getSavedPoint());
                // view 레파지 토리에 save
                mypageRepository.save(mypage);
            }
        
        }catch (Exception e){
            e.printStackTrace();
        }
    } 

}