
package concertbooking;

public class PointDeducted extends AbstractEvent {

    private Long customerId;
    private Long bookingId;
    private Integer savedPoint;
    private Integer pointTotal;

    public Long getId() {
        return customerId;
    }

    public void setId(Long customerId) {
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

