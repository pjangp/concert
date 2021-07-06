
package concertbooking.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

@FeignClient(name="point", url="${prop.room.url}")
public interface PointService {

    @RequestMapping(method= RequestMethod.GET, path="pointcheck/checkAndDeductPoint")
    public boolean checkAndDeductPoint(@RequestParam("customerId") long customerId, @RequestParam("usedPoint") Integer usedPoint);

}