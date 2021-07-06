package concertbooking;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Payment_table")
public class Payment {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long payId;
    private Long bookingId;
    private Long ccId;
    private String ccName;
    private String bookingStatus;
    private String paymentStatus;
    private Integer price;
    private Integer usedPoint;
//    private Integer savedPoint;
    private Long customerId;

    @PostPersist
    public void onPostPersist(){     

        boolean result = PaymentApplication.applicationContext.getBean(concertbooking.external.PointService.class)
        .checkAndDeductPoint(this.getCustomerId(), this.getUsedPoint());
        System.out.println("######## Check Result : " + result);

        if(result) { 
//            Integer savePoint = 0;
//            savePoint = (int) Math.round((this.getPrice()-this.getUsedPoint())*0.01);
            
            PaymentCreated paymentCreated = new PaymentCreated();
            BeanUtils.copyProperties(this, paymentCreated);
            paymentCreated.setPaymentStatus("payCompleted");
//            paymentCreated.setSavedPoint(savePoint);
            paymentCreated.publishAfterCommit();
        }
    }

    @PostUpdate
    public void onPostUpdate(){
    
        if(this.getPaymentStatus().equals("payCancelled")) {
 //           Integer savePoint = 0;
 //           savePoint = (int) Math.round((this.getPrice()-this.getUsedPoint())*0.01);
            PaymentCancelled paymentCancelled = new PaymentCancelled();
            BeanUtils.copyProperties(this, paymentCancelled);
//            paymentCancelled.setSavedPoint(savePoint);
            paymentCancelled.publishAfterCommit();
        }
    }
    
    
    public Long getPayId() {
        return payId;
    }

    public void setPayId(Long payId) {
        this.payId = payId;
    }
    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }
    public Long getCcId() {
        return ccId;
    }

    public void setCcId(Long ccId) {
        this.ccId = ccId;
    }
    public String getCcName() {
        return ccName;
    }

    public void setCcName(String ccName) {
        this.ccName = ccName;
    }
    public String getBookingStatus() {
        return bookingStatus;
    }

    public void setBookingStatus(String bookingStatus) {
        this.bookingStatus = bookingStatus;
    }
    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public Integer getPrice() {
        return price;
    }

    public void setPrice(Integer price) {
        this.price = price;
    }

    public Integer getUsedPoint() {
        return usedPoint;
    }

    public void setUsedPoint(Integer usedPoint) {
        this.usedPoint = usedPoint;
    }

    // public Integer getSavedPoint() {
    //     return savedPoint;
    // }

    // public void setSavedPoint(Integer savedPoint) {
    //     this.savedPoint = savedPoint;
    // }


    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

}
