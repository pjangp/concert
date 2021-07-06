package concertbooking;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

 @RestController
 public class PointController {
    
    @Autowired
    PointRepository pointRepository;
    
    @RequestMapping(value = "/pointcheck/checkAndDeductPoint",
        method = RequestMethod.GET,
        produces = "application/json;charset=UTF-8")

        public boolean checkAndDeductPoint(HttpServletRequest request, HttpServletResponse response)
        throws Exception {
                // Parameter로 받은 customerId 추출
                long customerId = Long.valueOf(request.getParameter("customerId"));
                Integer usedPoint = Integer.valueOf(request.getParameter("usedPoint"));
                System.out.println("######################## checkAndDeductPoint customerId : " + customerId);
                System.out.println("######################## checkAndDeductPoint use Point : " + usedPoint);

                // Point 데이터 조회
                Optional<Point> res = pointRepository.findById(customerId);
                Point point = res.get();
                System.out.println("######################## checkAndDeductPoint Saved Point : " + usedPoint);

                //point 체크
                boolean result = false;
                 if(point.getPointTotal() >= usedPoint ) {
                    point.setPointTotal(point.getPointTotal() - usedPoint);
                    pointRepository.save(point);
                        
                    result = true;
                 } 

                System.out.println("######################## checkAndDeductPoint Return : " + result);
                return result;
        }
 }
