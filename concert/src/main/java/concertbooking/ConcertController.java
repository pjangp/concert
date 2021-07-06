package concertbooking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



import java.util.Optional;

@RestController
public class ConcertController {

    @Autowired
    ConcertRepository concertRepository;

    @RequestMapping(value = "/checkAndBookStock", method = RequestMethod.GET, produces = "application/json;charset=UTF-8")

    public boolean checkAndBookStock(HttpServletRequest request, HttpServletResponse response) throws Exception {

        System.out.println("##### /concert/checkAndBookStock  called #####");

        boolean status = false;

        Long ccId = Long.valueOf(request.getParameter("ccId"));
        int qty = Integer.parseInt(request.getParameter("qty"));

        System.out.println("##### ccid #####" + ccId + "##### qty" + qty);
        Optional<Concert> concert = concertRepository.findById(ccId);

        if (concert.isPresent()) {

            Concert concertValue = concert.get();

                try {
                        Thread.currentThread();
                        Thread.sleep((long) (400 + Math.random() * 220));
                } catch (InterruptedException e) { }


                if (concertValue.getStock() >= qty) {
                        concertValue.setStock(concertValue.getStock() - qty);
                        concertRepository.save(concertValue);
                        status = true;
                        System.out.println("##### /concert/checkAndBookStock  qty check true ##### stock"+concertValue.getStock()+"### qty"+ qty);
                }

                System.out.println("##### /concert/checkAndBookStock  qty check false ##### stock"+concertValue.getStock()+"### qty"+ qty);
        }

        return status;
        }
        
 }
