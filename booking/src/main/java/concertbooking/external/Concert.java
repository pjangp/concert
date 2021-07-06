package concertbooking.external;

public class Concert {

    private Long ccId;
    private String ccName;
    private String ccDate;
    private Long stock;

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
    public String getCcDate() {
        return ccDate;
    }
    public void setCcDate(String ccDate) {
        this.ccDate = ccDate;
    }
    public Long getStock() {
        return stock;
    }
    public void setStock(Long stock) {
        this.stock = stock;
    }

}
