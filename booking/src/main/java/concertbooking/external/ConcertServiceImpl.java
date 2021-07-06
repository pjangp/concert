
package concertbooking.external;


import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;


@Service
public class ConcertServiceImpl implements ConcertService {

    
    public boolean checkAndBookStock(@RequestParam("ccId") Long ccId , @RequestParam("qty") int qty){

       System.out.println("================ BookStock change delay ================");
        return true;
    }

}