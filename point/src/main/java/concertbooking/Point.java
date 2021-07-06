package concertbooking;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Point_table")
public class Point {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long customerId;
    private Long bookingId;
    private Integer savedPoint;
    private Integer pointTotal;

     @PostPersist
     public void onPostPersist(){
         PointEarned pointEarned = new PointEarned();
         BeanUtils.copyProperties(this, pointEarned);
         pointEarned.publishAfterCommit();


    //     PointDeducted pointDeducted = new PointDeducted();
    //     BeanUtils.copyProperties(this, pointDeducted);
    //     pointDeducted.publishAfterCommit();


     }

     @PostUpdate
     public void onPostUpdate(){   
        PointEarned pointEarned = new PointEarned();
        BeanUtils.copyProperties(this, pointEarned);
        pointEarned.publishAfterCommit();
     }


    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getBookingId() {
        return bookingId;
    }

    public void setBookingId(Long bookingId) {
        this.bookingId = bookingId;
    }

    public Integer getSavedPoint() {
        return savedPoint;
    }

    public void setSavedPoint(Integer savedPoint) {
        this.savedPoint = savedPoint;
    }

    public Integer getPointTotal() {
        return pointTotal;
    }

    public void setPointTotal(Integer pointTotal) {
        this.pointTotal = pointTotal;
    }




}
