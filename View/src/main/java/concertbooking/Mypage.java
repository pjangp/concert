package concertbooking;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name="Mypage_table")
public class Mypage {

        @Id
        @GeneratedValue(strategy=GenerationType.AUTO)
        private Long bookingId;
        private Long ccId;
        private String ccName;
        private Integer qty;
        private String status;
        private String bookingStatus;
        private String paymentStatus;
        private Integer price;
        private Integer usedPoint;
        private Integer savedPoint;


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
        public Integer getQty() {
            return qty;
        }

        public void setQty(Integer qty) {
            this.qty = qty;
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
    
        public Integer getSavedPoint() {
            return savedPoint;
        }
    
        public void setSavedPoint(Integer savedPoint) {
            this.savedPoint = savedPoint;
        }

}
