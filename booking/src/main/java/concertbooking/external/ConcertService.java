
package concertbooking.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;


@FeignClient(name="concert", url="http://localhost:8081" , fallback = ConcertServiceImpl.class)
public interface ConcertService {

    @RequestMapping(method= RequestMethod.GET, path="/checkAndBookStock" )
    public boolean checkAndBookStock(@RequestParam("ccId") Long ccId , @RequestParam("qty") int qty);

}