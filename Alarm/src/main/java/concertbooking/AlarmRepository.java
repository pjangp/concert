package concertbooking;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="alarms", path="alarms")
public interface AlarmRepository extends PagingAndSortingRepository<Alarm, Long>{


}
