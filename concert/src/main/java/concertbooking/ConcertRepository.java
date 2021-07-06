package concertbooking;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="concerts", path="concerts")
public interface ConcertRepository extends PagingAndSortingRepository<Concert, Long>{


}
