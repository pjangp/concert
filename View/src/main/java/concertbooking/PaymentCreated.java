
package concertbooking;

public class PaymentCreated extends AbstractEvent {

    private Long payId;
    private Long bookingId;
    private Long ccId;
    private String ccName;
    private String bookingStatus;
    private String paymentStatus;
    private Integer price;
    private Integer usedPoint;
    private Integer savedPoint;
    private Long customerId;

    public Long getId() {
        return payId;
    }

    public void setId(Long payId) {
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

    public Integer getSavedPoint() {
        return savedPoint;
    }

    public void setSavedPoint(Integer savedPoint) {
        this.savedPoint = savedPoint;
    }


    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }
}

