package com.hyetaekon.hyetaekon.common.publicdata.mongodb.repository;

import com.hyetaekon.hyetaekon.common.publicdata.mongodb.document.PublicData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface PublicDataMongoRepository extends MongoRepository<PublicData, String> {
    Optional<PublicData> findByPublicServiceId(String publicServiceId);
    List<PublicData> findAllByPublicServiceId(String publicServiceId);
    List<PublicData> findAllByPublicServiceIdIn(Collection<String> publicServiceIds);

    @Query(value = "{}", fields = "{ 'publicServiceId' : 1 }")
    List<String> findAllPublicServiceIds();
}
