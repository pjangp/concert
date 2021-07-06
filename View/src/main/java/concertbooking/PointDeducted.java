
package concertbooking;

public class PointDeducted extends AbstractEvent {

    private Long customerId;
    private Integer pointTotal;

    public Long getId() {
        return customerId;
    }

    public void setId(Long customerId) {
        this.customerId = customerId;
    }
    public Integer getPointTotal() {
        return pointTotal;
    }

    public void setPointTotal(Integer pointTotal) {
        this.pointTotal = pointTotal;
    }
}

